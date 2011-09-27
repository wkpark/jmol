package org.openscience.jmol.app.jmolpanel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.vecmath.Point3f;

import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolViewer;
import org.jmol.constant.EnumCallback;
import org.jmol.util.TextFormat;

import com.json.JSONObject;
import com.json.JSONTokener;

import naga.NIOService;
import naga.NIOSocket;
import naga.SocketObserver;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;

public class JsonNioService extends NIOService implements JmolCallbackListener {

  protected NIOSocket inSocket;
  protected NIOSocket outSocket;
  protected JmolViewer jmolViewer;
  protected JsonNioClient client;

  protected boolean halt;
  protected boolean isPaused;
  protected long lastMoveTime;
  protected boolean wasSpinOn;

  protected String contentPath = "./%ID%/%ID%.json";
  protected String terminatorMessage = "NEXT_SCRIPT";

  /*
   * When Jmol gets the terminator message, we tell the Hub that we're done
   * with the script and need the name of the next one to load.
   */

  public JsonNioService() throws IOException {
    super();
  }

  public void setContentPath(String path) {
    contentPath = path;
  }

  public void setTerminatorMessage(String msg) {
    terminatorMessage = msg;
  }

  public void startService(int port, JsonNioClient client, JmolViewer jmolViewer)
      throws IOException {

    this.client = client;
    this.jmolViewer = jmolViewer;
    jmolViewer.setJmolCallbackListener(this);

    System.out.println("JsonNioService using port " + port);
    System.out.println("contentPath=" + contentPath);

    jmolViewer.script(";sync on;sync slave");

    inSocket = openSocket("127.0.0.1", port);
    outSocket = openSocket("127.0.0.1", port);
    inSocket.setPacketReader(new AsciiLinePacketReader());
    inSocket.setPacketWriter(new RawPacketWriter());
    outSocket.setPacketReader(new AsciiLinePacketReader());
    outSocket.setPacketWriter(new RawPacketWriter());
    selectNonBlocking();

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

    thread = new Thread(new JsonNioThread(), "JsonNiosThread");
    thread.start();
  }

  Thread thread;

  @Override
  public void close() {
    try {
      if (thread != null) {
        thread.interrupt();
        thread = null;
      }
      inSocket.close();
      outSocket.close();
      super.close();
    } catch (Throwable e) {
      //
    }
    client.nioClosed();
  }

  class JsonNioThread implements Runnable {

    public void run() {
      try {
        while (!halt) {

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
        //
      }
      close();
    }

  }

  protected void processMessage(byte[] packet) {
    try {
      JSONObject json = new JSONObject(new String(packet));
      switch (("move......" + "command..." + "content..." + "quit......"
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
        System.out.println("JsonNiosService quitting");
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

  protected void sendMessage(JSONObject json) {
    try {
      String jsonString = json.toString() + "\r\n";
      outSocket.write(jsonString.getBytes("UTF-8"));
    } catch (Throwable e) {
      e.printStackTrace();
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
    switch (type) {
    case SCRIPT:
      String msg = (String) data[1];
      if (msg.equals(terminatorMessage)) {
        try {
          JSONObject json = new JSONObject();
          json.put("type", "script");
          json.put("event", "done");
          sendMessage(json);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      break;
    default:
      break;
    }
  }

  public void setCallbackFunction(String callbackType, String callbackFunction) {
    // unused
  }

}
