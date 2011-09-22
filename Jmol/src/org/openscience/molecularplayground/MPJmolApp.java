/*
 * Copyright 2011 University of Massachusetts
 *
 * File: MPJmolApp.java
 * Description: Molecular Playground Jmol interface component/application
 * Author: Adam Williams
 *
 * This class uses the Naga asynchronous socket IO package, the JSON.org JSON package and 
 * Jmol (currently 11.8.2). 
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

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
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

  JmolViewer jmolViewer;
  NIOService service;
  NIOSocket inSocket;
  NIOSocket outSocket;
  int port;
  boolean halt;
  JLabel bannerLabel;
  JFrame bannerFrame;
  boolean isPaused;
  long lastMoveTime;
  boolean wasSpinOn;

  static final String magicWord = "{\"magic\":\"JmolApp\"\r\n";
  static final String contentPath = "/Content-Cache/";

  public static void main(String args[]) {
    int portArg = 31416;
    if (args.length > 1) {
      portArg = Integer.parseInt(args[1]);
    }
    new MPJmolApp(portArg);
  }

  public MPJmolApp(int port) {
    halt = false;
    this.port = port;

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
    jmolViewer.script("set frank off;set antialiasdisplay off;sync on;sync slave");

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
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      service.close();
    }
  }

  void processMessage(byte[] packet) {
    try {
      String msg = new String(packet);
      JSONObject json = new JSONObject(msg);

      if (json.getString("type").equals("move")) {

        // Pause the script and save the state when interaction starts
        if (!isPaused) {
          wasSpinOn = jmolViewer.getBooleanProperty("spinOn");
          jmolViewer.script("pause; save orientation 'playground-save'; spin off");
          isPaused = true;
        }

        lastMoveTime = Calendar.getInstance().getTimeInMillis();

        String style = json.getString("style");

        if (style.equals("zoom")) {
          float zoomFactor = jmolViewer.getZoomPercentFloat() / 100.0f;
          zoomFactor = ((float) json.getDouble("scale")) / zoomFactor;
          jmolViewer.syncScript("Mouse: zoomByFactor " + zoomFactor, "~");
        } else if (style.equals("rotate")) {
          jmolViewer.syncScript("Mouse: rotateXYBy " + json.getString("x") + " " + json.getString("y"), "~");
        } else if (style.equals("translate")) {
          jmolViewer.syncScript("Mouse: translateXYBy " + json.getString("x") + " " + json.getString("y"), "~");
        } else if (style.equals("sync")) {
          jmolViewer.syncScript("Mouse: " + json.getString("sync"), "~");
        }
      } else if (json.getString("type").equals("command")) {
        String command = json.getString("command");
        jmolViewer.script(command);
      } else if (json.getString("type").equals("content")) {
        int id = json.getInt("id");
        String path = System.getProperty("user.dir") + contentPath + id + "/"
            + id + ".json";
        FileInputStream jsonFile = new FileInputStream(path);
        JSONObject contentJSON = new JSONObject(new JSONTokener(jsonFile));
        bannerLabel.setText("<html></html>");
        jmolViewer.script("exit");
        jmolViewer.script("zap");
        jmolViewer.script("cd " + System.getProperty("user.dir") + contentPath
            + id);
        jmolViewer.script("script " + contentJSON.getString("startup_script"));
        if (contentJSON.getString("banner").equals("off")) {
          bannerFrame.setVisible(false);
        } else {
          bannerFrame.setVisible(true);
          bannerLabel.setText("<html><center>"
              + contentJSON.getString("banner_text") + "</center></html>");
        }
      } else if (json.getString("type").equals("quit")) {
        halt = true;
        System.out.println("I should quit");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void sendMessage(JSONObject json) {
    try {
      String jsonString = json.toString() + "\r\n";
      outSocket.write(jsonString.getBytes("UTF-8"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class JmolPanel extends JPanel {
    JmolViewer viewer;
    JmolAdapter adapter;

    JmolPanel(JmolCallbackListener mpJmolApp) {
      adapter = new SmarterJmolAdapter();
      viewer = JmolViewer.allocateViewer(this, adapter);
      viewer.setJmolCallbackListener(mpJmolApp);
    }

    public JmolViewer getViewer() {
      return viewer;
    }

    final Dimension currentSize = new Dimension();
    final Rectangle rectClip = new Rectangle();

    @Override
    public void paint(Graphics g) {
      getSize(currentSize);
      g.getClipBounds(rectClip);
      viewer.renderScreenImage(g, currentSize, rectClip);
    }
  }

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
