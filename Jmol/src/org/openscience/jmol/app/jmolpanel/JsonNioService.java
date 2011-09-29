/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-19 08:25:14 -0500 (Wed, 19 May 2010) $
 * $Revision: 13133 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openscience.jmol.app.jmolpanel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Calendar;

import javax.vecmath.Point3f;

import org.jmol.api.JmolViewer;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import com.json.JSONException;
import com.json.JSONObject;
import com.json.JSONTokener;

import naga.ConnectionAcceptor;
import naga.NIOServerSocket;
import naga.NIOService;
import naga.NIOSocket;
import naga.SocketObserverAdapter;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserver;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;

/**
 * listens over a port on the local host for instructions on what to display. 
 * Instructions come in over the port as JSON strings.
 * 
 * This class uses the Naga asynchronous socket network I/O package, 
 * the JSON.org JSON package and Jmol.
 * 
 * http://code.google.com/p/naga/
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
 *   Content is assumed to be in a location determined by the Jmol variable
 *   nioContentPath, with %ID% being replaced by some sort of ID number of tag provided by
 *   the other half of the system. That file contains more JSON code:
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
public class JsonNioService extends NIOService {

  protected NIOSocket inSocket;
  protected NIOSocket outSocket;
  protected JmolViewer jmolViewer;
  protected JsonNioClient client;

  protected boolean halt;
  protected boolean isPaused;
  protected long lastMoveTime;
  protected boolean wasSpinOn;
  
  private int port;
  protected String myName;

  protected String contentPath = "./%ID%.json";
  protected String terminatorMessage = "NEXT_SCRIPT";

  /*
   * When Jmol gets the terminator message, we tell the Hub that we're done
   * with the script and need the name of the next one to load.
   */

  public JsonNioService() throws IOException {
    super();
  }

  public void startService(int port, JsonNioClient client, JmolViewer jmolViewer, String name)
      throws IOException {

    this.port = port;
    this.client = client;
    this.jmolViewer = jmolViewer;
    myName = (name == null ? "" : name);
    
    if (port < 0) {
      startServerService();
      return;
    }
    if (name != null) {
      jmolViewer.script(";sync on;sync slave");
      String s = getJmolValue("NIOcontentPath");
      if (s != null)
        contentPath = s;
      s = getJmolValue("NIOterminatorMessage");
      if (s != null)
        terminatorMessage = s;
      Logger.info("contentPath=" + contentPath);
      Logger.info("terminatorMessage=" + terminatorMessage);
    }
    Logger.info("JsonNioService" + myName + " using port " + port);
    inSocket = openSocket("127.0.0.1", port);
    outSocket = openSocket("127.0.0.1", port);
    inSocket.setPacketReader(new AsciiLinePacketReader());
    inSocket.setPacketWriter(new RawPacketWriter());
    outSocket.setPacketReader(new AsciiLinePacketReader());
    outSocket.setPacketWriter(new RawPacketWriter());

    inSocket.listen(new SocketObserver() {

      public void connectionOpened(NIOSocket nioSocket) {
        initialize("out", nioSocket);
      }

      public void packetReceived(NIOSocket nioSocket, byte[] packet) {
        processMessage(packet);
      }

      public void connectionBroken(NIOSocket nioSocket, Exception exception) {
        halt = true;
        Logger.info(Thread.currentThread().getName() + " connectionBroken");
      }
    });

    outSocket.listen(new SocketObserver() {

      public void connectionOpened(NIOSocket nioSocket) {
        initialize("in", outSocket);
      }

      public void packetReceived(NIOSocket nioSocket, byte[] packet) {
        System.out.println(Thread.currentThread().getName() + " outSocket packetRecieved " + (new String(packet)));
      }

      public void connectionBroken(NIOSocket nioSocket, Exception exception) {
        System.out.println("connectionBroken");
      }
    });

    thread = new Thread(new JsonNioThread(), "JsonNiosThread" + myName);
    thread.start();
  }

  protected void initialize(String role, NIOSocket nioSocket) {
    try {
      JSONObject json = new JSONObject();
      json.put("magic", "JmolApp");
      json.put("role", role);
      sendMessage(json, null, nioSocket);
    } catch (JSONException e) {
      // TODO
    }
  }

  private NIOServerSocket serverSocket;
  private Thread serverThread;
  
  class JsonNioServerThread implements Runnable {

    public void run() {
      // Keep reading IO forever.
      try {
        while (!halt) {
          selectBlocking();
        }
      } catch (IOException e) {
        // TODO
      }

    }
    
  }

  private void startServerService() {
    port = -port;
    try {
      serverSocket = openServerSocket(port);
      Logger.info(Thread.currentThread().getName() + " on port " + port);
      serverSocket.listen(new ServerSocketObserverAdapter() {
        
        
        @Override
        public void newConnection(NIOSocket nioSocket) {
          Logger.info(Thread.currentThread().getName() + " Received connection: " + nioSocket);

          nioSocket.setPacketReader(new AsciiLinePacketReader());

          nioSocket.setPacketWriter(new RawPacketWriter());

          // Listen on the connection.
          nioSocket.listen(new SocketObserverAdapter() {
            @Override
            public void packetReceived(NIOSocket socket, byte[] packet) {
              Logger.info(Thread.currentThread().getName() + " received " + new String(packet));
              processMessage(packet);
              //socket.close();
            }

            @Override
            public void connectionBroken(NIOSocket arg0, Exception arg1) {
              // TODO
              System.out.println("connectionBroken");

            }

            @Override
            public void connectionOpened(NIOSocket arg0) {
              System.out.println("connectionOpened");

            }
          });
        }
      });

      serverSocket.setConnectionAcceptor(new ConnectionAcceptor() {
        public boolean acceptConnection(InetSocketAddress arg0) {
          boolean         isOK = arg0.getAddress().isLoopbackAddress();
          return isOK;
        }
      });

    } catch (IOException e) {
      // TODO
    }

    if (serverThread != null)
      serverThread.interrupt();
    serverThread = new Thread(new JsonNioServerThread(), "JsonNioServerThread" + myName);
    serverThread.start();
  }

  private String getJmolValue(String var) {
    if (jmolViewer == null)
      return "";
    String s = (String) jmolViewer.scriptWaitStatus("print " + var, "output");
    return (s.indexOf("\n") <= 1 ? null : s.substring(0, s.lastIndexOf("\n")));
  }

  Thread thread;

  @Override
  public void close() {
    try {
      super.close();
      halt = true;
      if (thread != null) {
        thread.interrupt();
        thread = null;
      }
      if (serverThread != null) {
        serverThread.interrupt();
        serverThread = null;
      }
      if (inSocket != null)
        inSocket.close();
      if (outSocket != null)
        outSocket.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    if (client != null)
      client.nioClosed(this);
  }

  class JsonNioThread implements Runnable {

    public void run() {
      try {
        while (!halt) {

          selectNonBlocking();

          long now = Calendar.getInstance().getTimeInMillis();

          // No commands for 5 seconds = unpause/restore Jmol
          if (isPaused && now - lastMoveTime > 5000) {
            jmolViewer
                .script("restore rotation 'JsonNios-save' 1; resume; spin "
                    + wasSpinOn);
            isPaused = false;
            wasSpinOn = false;
          }
          Thread.sleep(50);
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
      close();
    }

  }

  protected void processMessage(byte[] packet) {
    try {
      String msg = new String(packet);
      if (jmolViewer == null) {
        Logger.info("JNIOS " + Thread.currentThread().getName() + " received " + msg);
        return;
      }
      JSONObject json = new JSONObject(msg);
      if (!json.has("type"))
        return;
      switch (("move......" 
          + "command..." 
          + "content..." 
          + "quit......"
          + "touch.....").indexOf(json.getString("type"))) {
      case 0: // move
        if (!isPaused) {
          // Pause the script and save the state when interaction starts
          wasSpinOn = jmolViewer.getBooleanProperty("spinOn");
          jmolViewer
              .script("pause; save orientation 'JsonNios-save'; spin off");
          isPaused = true;
        }
        lastMoveTime = Calendar.getInstance().getTimeInMillis();
        switch (("sync......" + "rotate...." + "translate." + "zoom......")
            .indexOf(json.getString("style"))) {
        case 0: // sync
          jmolViewer.syncScript("Mouse: " + json.getString("sync"), "~", 0);
          break;
        case 10: // rotate
          jmolViewer.syncScript("Mouse: rotateXYBy " + json.getString("x")
              + " " + json.getString("y"), "~", 0);
          break;
        case 20: // translate
          jmolViewer.syncScript("Mouse: translateXYBy " + json.getString("x")
              + " " + json.getString("y"), "~", 0);
          break;
        case 30: // zoom
          float zoomFactor = (float) (json.getDouble("scale") / (jmolViewer
              .getZoomPercentFloat() / 100.0f));
          jmolViewer.syncScript("Mouse: zoomByFactor " + zoomFactor, "~", 0);
          break;
        }
        break;
      case 10: // command
        jmolViewer.script(json.getString("command"));
        break;
      case 20: // content
        String id = json.getString("id");
        String path = TextFormat.simpleReplace(contentPath, "%ID%", id)
            .replace('\\', '/');
        File f = new File(path);
        FileInputStream jsonFile = new FileInputStream(f);
        System.out.println("JsonNiosService Setting path to "
            + f.getAbsolutePath());
        int pt = path.lastIndexOf('/');
        if (pt >= 0)
          path = path.substring(0, pt);
        else
          path = ".";
        JSONObject contentJSON = new JSONObject(new JSONTokener(jsonFile));
        String script = contentJSON.getString("startup_script");
        System.out.println("JsonNiosService startup_script=" + script);
        client.setBannerLabel("<html></html>");
        jmolViewer.script("exit");
        jmolViewer.script("zap;cd \"" + path + "\";script " + script);
        String bannerText = contentJSON.getString("banner_text");
        if (contentJSON.getString("banner").equals("off") || bannerText == null) {
          client.setBannerLabel(null);
        } else {
          client.setBannerLabel("<html><center>" + bannerText
              + "</center></html>");
        }
        break;
      case 30: // quit
        halt = true;
        Logger.info("JsonNiosService quitting");
        break;
      case 40: // touch
        // raw touch event
        jmolViewer.processEvent(0, json.getInt("eventType"), json
            .getInt("touchID"), json.getInt("iData"), new Point3f((float) json
            .getDouble("x"), (float) json.getDouble("y"), (float) json
            .getDouble("z")), json.getLong("time"));
        break;

      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(JSONObject json, String msg, NIOSocket socket) {
    try {
      if (json != null) {
        msg = json.toString() + "\r\n";
      } else if (msg != null && msg.indexOf("{") != 0){
        json = new JSONObject();
        json.put("type", "command");
        json.put("command", msg);
        msg = json.toString() + "\r\n";
      }
      if (socket == null)
        socket = outSocket;
      Logger.info(Thread.currentThread().getName() + " sending " + msg);
      socket.write(msg.getBytes("UTF-8"));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void scriptCallback(String msg) {
    if (msg.equals(terminatorMessage)) {
      try {
        JSONObject json = new JSONObject();
        json.put("type", "script");
        json.put("event", "done");
        sendMessage(json, null, outSocket);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public void send(int port, String msg) {
    try {
      if (port != this.port) {
        if (inSocket != null) {
          inSocket.close();
          outSocket.close();
        }
        if (thread != null) {
          thread.interrupt();
          thread = null;
        }
        startService(port, client, jmolViewer, myName);
      }
      sendMessage(null, msg, null);
    } catch (IOException e) {
      // ignore
    }
  }

}
