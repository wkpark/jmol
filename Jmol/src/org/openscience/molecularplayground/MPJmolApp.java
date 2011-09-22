/*
 * Copyright 2011 University of Massachusetts
 *
 * File: MPJmolApp.java
 * Description: Molecular Playground Jmol interface component/application
 * Author: Adam Williams
 *
 * See http://molecularplayground.org/
 * 
 * A Java application that listens over a port on the local host for 
 * instructions on what to display. Instructions come in over the port as JSON strings.
 * 
 * This class uses the Naga asynchronous socket IO package, the JSON.org JSON package and Jmol.
 * 
 * Adapted by Bob Hanson for Jmol 12.2
 *  
 * Sent from Jmol: 
 * 
 *   {"magic" : "JmolApp", "role" : "out"}  (socket initialization for messages TO jmol)
 *   {"magic" : "JmolApp", "role" : "in"}   (socket initialization for messages FROM jmol)
 *   {"type" : "script", "event" : "done"}  (script completed)
 *   
 * Sent to Jmol:
 * 
 *   {"type" : "quit" }                          (shut down request)
 *   {"type" : "move", "style" : (see below) }   (mouse command request)
 *   {"type" : "command", "command" : command }  (script command request)
 *   {"type" : "content", "id" : id }            (load content request)
 *   {"type" : "touch",                          (a raw touch event)
 *        "eventType" : eventType,
 *        "touchID"   : touchID,
 *        "iData"     : idata,
 *        "time"      : time,
 *        "x" : x, "y" : y, "z" : z }
 *    
 *   For details on the "touch" type, see org.jmol.viewer.ActionManagerMT::processEvent
 *   Content is assumed to be in System.getProperty("user.dir") + "/Content-Cache/<id>/<id>.json";
 *   This file contains more JSON code:
 *   
 *   {"startup_script" : scriptFileName, "banner_text" : text } 
 *   
 *   An additional option "banner" : "off" turns off the title banner.
 *   The startup script must be in the same directory as the .json file, typically as a .spt file
 *   
 *   Move commands include:
 *   
 *   {"type" : "move", "style" : "rotate", "x" : deltaX, "y", deltaY }
 *   {"type" : "move", "style" : "translate", "x" : deltaX, "y", deltaY }
 *   {"type" : "move", "style" : "zoom", "scale" : scale }  (1.0 = 100%)
 *   {"type" : "move", "style" : "sync", "sync" : syncText }
 *   
 *   Note that all these moves utilize the Jmol sync functionality originally intended for
 *   applets. So any valid sync command may be used with the "sync" style. These include 
 *   essentially all the actions that a user can make with a mouse, including the
 *   following, where the notation <....> represents a number of a given type. These
 *   events interrupt any currently running script, just as with typical mouse actions.
 *   
 *   "centerAt <int:x> <int:y> <float:ptx> <float:pty> <float:ptz>"
 *      -- set {ptx,pty,ptz} at screen (x,y)
 *   "rotateMolecule <float:deltaX> <float:deltaY>"
 *   "rotateXYBy <float:deltaX> <float:deltaY>"
 *   "rotateZBy <int:degrees>"
 *   "rotateZBy <int:degrees> <int:x> <int:y>" (with center reset)
 *   "rotateArcBall <int:x> <int:y> <float:factor>"
 *   "spinXYBy <int:x> <int:y> <float:speed>"
 *      -- a "flick" gesture
 *   "translateXYBy <float:deltaX, float:deltaY>"
 *   "zoomBy <int:pixels>"
 *   "zoomByFactor <float:factor>"
 *   "zoomByFactor <float:factor> <int:x> <int:y>" (with center reset)
 *   
 */
package org.openscience.molecularplayground;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.FileInputStream;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.vecmath.Point3f;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolViewer;
import org.jmol.constant.EnumCallback;
import org.openscience.molecularplayground.json.JSONObject;
import org.openscience.molecularplayground.json.JSONTokener;

import naga.NIOService;
import naga.NIOSocket;
import naga.SocketObserver;
import naga.packetwriter.RawPacketWriter;
import naga.packetreader.AsciiLinePacketReader;

public class MPJmolApp implements JmolCallbackListener {

  private static String defaultScript = "set frank off;set antialiasdisplay off";
  private static final String contentPath = System.getProperty("user.dir").replace('\\','/') + "/Content-Cache/";
  
  private JmolViewer jmolViewer;
  private NIOService service;
  private NIOSocket inSocket;
  private NIOSocket outSocket;
  private boolean halt;
  private JLabel bannerLabel;
  private JFrame bannerFrame;
  private boolean isPaused;
  private long lastMoveTime;
  private boolean wasSpinOn;

  //static final String magicWord = "{\"magic\":\"JmolApp\"\r\n";

  public static void main(String args[]) {
    int portArg = 31416;
    if (args.length > 1) {
      portArg = Integer.parseInt(args[1]);
    }
    new MPJmolApp(portArg);
  }

  public MPJmolApp(int port) {
    halt = false;
    //this.port = port;
    System.out.println("MPJmolApp using port " + port);
    System.out.println("defaultScript=" + defaultScript);
    System.out.println("contentPath=" + contentPath);
    isPaused = false;
    lastMoveTime = 0;
    wasSpinOn = false;

    JFrame appFrame = new JFrame("MPJmolApp");
    appFrame.setUndecorated(true);
    appFrame.setBackground(new Color(0, 0, 0, 0));
    Container contentPane = appFrame.getContentPane();
    JmolPanel jmolPanel = new JmolPanel(this);

    contentPane.add(jmolPanel);
    appFrame.setSize(1024, 768);
    appFrame.setBounds(0, 0, 1024, 768);
    appFrame.setVisible(true);

    bannerFrame = new JFrame("Banner");
    bannerFrame.setUndecorated(true);
    bannerFrame.setBackground(Color.WHITE);
    bannerFrame.setSize(1024, 75);
    bannerFrame.setBounds(0, 0, 1024, 75);
    bannerLabel = new JLabel("<html></html>", SwingConstants.CENTER);
    bannerLabel.setPreferredSize(bannerFrame.getSize());
    bannerLabel.setFont(new Font("Helvetica", Font.BOLD, 30));
    bannerFrame.getContentPane().add(bannerLabel, BorderLayout.CENTER);
    bannerFrame.setVisible(true);
    bannerFrame.setAlwaysOnTop(true);

    jmolViewer = jmolPanel.getViewer();
    jmolViewer.script(defaultScript  + ";sync on;sync slave");

    /*
     * Separate sockets are used for incoming and outgoing messages,
     * since communication to/from the Hub is completely asynchronous
     *
     * All messages are sent as JSON strings, terminated by a CR/LF.
     */
    try {
      service = new NIOService();
      inSocket = service.openSocket("127.0.0.1", port);
      outSocket = service.openSocket("127.0.0.1", port);
      inSocket.setPacketReader(new AsciiLinePacketReader());
      inSocket.setPacketWriter(new RawPacketWriter());
      outSocket.setPacketReader(new AsciiLinePacketReader());
      outSocket.setPacketWriter(new RawPacketWriter());

      inSocket.listen(new SocketObserver() {

        public void connectionOpened(NIOSocket nioSocket) {
          try {
            JSONObject json = new JSONObject();
            json.put("magic", "JmolApp");
            json.put("role", "out");
            String jsonString = json.toString() + "\r\n";
            nioSocket.write(jsonString.getBytes("UTF-8"));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        public void packetReceived(NIOSocket nioSocket, byte[] packet) {
          processMessage(packet);
        }

        public void connectionBroken(NIOSocket nioSocket, Exception exception) {
        }
      });

      outSocket.listen(new SocketObserver() {

        public void connectionOpened(NIOSocket nioSocket) {
          try {
            JSONObject json = new JSONObject();
            json.put("magic", "JmolApp");
            json.put("role", "in");
            sendMessage(json);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        public void packetReceived(NIOSocket nioSocket, byte[] packet) {
        }

        public void connectionBroken(NIOSocket nioSocket, Exception exception) {
        }
      });

      while (!halt) {
        service.selectNonBlocking();

        long now = Calendar.getInstance().getTimeInMillis();

        // No commands for 5 seconds = unpause/restore Jmol
        if (isPaused && now - lastMoveTime > 5000) {
          jmolViewer.script("restore rotation 'playground-save' 1; resume; spin " + wasSpinOn);
          isPaused = false;
          wasSpinOn = false;
        }
        Thread.sleep(50);
      }
      inSocket.close();
      outSocket.close();
    } catch (Exception e) {
      e.printStackTrace();
      service.close();
    }
    System.exit(0);
  }

  protected void processMessage(byte[] packet) {
    try {
      JSONObject json = new JSONObject(new String(packet));
      switch (
           ("move......" 
          + "command..." 
          + "content..." 
          + "quit......"
          + "touch.....").indexOf(json.getString("type"))) {
      case 0: // move
        if (!isPaused) {
          // Pause the script and save the state when interaction starts
          wasSpinOn = jmolViewer.getBooleanProperty("spinOn");
          jmolViewer
              .script("pause; save orientation 'playground-save'; spin off");
          isPaused = true;
        }
        lastMoveTime = Calendar.getInstance().getTimeInMillis();
        switch (
             ("sync......" 
            + "rotate...." 
            + "translate." 
            + "zoom......").indexOf(json.getString("style"))) {
        case 0: // sync
          jmolViewer.syncScript("Mouse: " + json.getString("sync"), "~");
          break;
        case 10: // rotate
          jmolViewer.syncScript("Mouse: rotateXYBy " + json.getString("x")
              + " " + json.getString("y"), "~");
          break;
        case 20: // translate
          jmolViewer.syncScript("Mouse: translateXYBy " + json.getString("x")
              + " " + json.getString("y"), "~");
          break;
        case 30: // zoom
          float zoomFactor = (float) (json.getDouble("scale") / (jmolViewer
              .getZoomPercentFloat() / 100.0f));
          jmolViewer.syncScript("Mouse: zoomByFactor " + zoomFactor, "~");
          break;
        }
        break;
      case 10: // command
        jmolViewer.script(json.getString("command"));
        break;
      case 20: // content
        int id = json.getInt("id");
        String path = contentPath + id;
        FileInputStream jsonFile = new FileInputStream(path + "/" + id
            + ".json");
        JSONObject contentJSON = new JSONObject(new JSONTokener(jsonFile));
        bannerLabel.setText("<html></html>");
        jmolViewer.script("exit");
        jmolViewer.script("zap;cd " + path + ";script "
            + contentJSON.getString("startup_script"));
        String bannerText = contentJSON.getString("banner_text");
        if (contentJSON.getString("banner").equals("off") || bannerText == null) {
          bannerFrame.setVisible(false);
        } else {
          bannerFrame.setVisible(true);
          bannerLabel.setText("<html><center>" + bannerText
              + "</center></html>");
        }
        break;
      case 30: // quit
        halt = true;
        System.out.println("I should quit");
        break;
      case 40: // touch
        // raw touch event
        jmolViewer.processEvent(0, json.getInt("eventType"), json
            .getInt("touchID"), json.getInt("iData"), new Point3f((float) json
            .getDouble("x"), (float) json.getDouble("y"), (float) json
            .getDouble("z")), json.getLong("time"));
        break;

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void sendMessage(JSONObject json) {
    try {
      String jsonString = json.toString() + "\r\n";
      outSocket.write(jsonString.getBytes("UTF-8"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class JmolPanel extends JPanel {
    private JmolViewer viewer;

    JmolPanel(JmolCallbackListener mpJmolApp) {
      viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
          null, null, null, "-multitouch-mp", null);
      viewer.setJmolCallbackListener(mpJmolApp);
    }

    JmolViewer getViewer() {
      return viewer;
    }

    private final Dimension currentSize = new Dimension();
    private final Rectangle rectClip = new Rectangle();

    @Override
    public void paint(Graphics g) {
      getSize(currentSize);
      g.getClipBounds(rectClip);
      viewer.renderScreenImage(g, currentSize, rectClip);
    }
  }

  ///  JmolCallbackListener Interface ///
  
  public boolean notifyEnabled(EnumCallback type) {
    switch (type) {
    case SCRIPT:
      return true;
    default:
      return false;
    }
  }

  public void notifyCallback(EnumCallback type, Object[] data) {
    // this method as of 11.5.23 gets all the callback notifications for
    // any embedding application or for the applet.
    // see org.jmol.applet.Jmol.java and org.jmol.openscience.app.Jmol.java

    // data is an object set up by org.jmol.viewer.StatusManager
    // see that class for details.
    // data[0] is always blank -- for inserting htmlName
    // data[1] is either String (main message) or int[] (animFrameCallback only)
    // data[2] is optional supplemental information such as status info
    //         or sometimes an Integer value
    // data[3] is more optional supplemental information, either a String or Integer
    // etc. 

    /*
     * MP_DONE is the end-of-script marker.
     * When Jmol gets to this message, we tell the Hub that we're done
     * with the script and need the name of the next one to load.
     */
    switch (type) {
    case SCRIPT:
      String msg = (String) data[1];
      if (msg.equals("MP_DONE")) {
        try {
          JSONObject json = new JSONObject();
          json.put("type", "script");
          json.put("event", "done");
          sendMessage(json);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      break;
    default:
      break;
    }
  }

  public void setCallbackFunction(String callbackType, String callbackFunction) {
    // TODO    
  }

}
