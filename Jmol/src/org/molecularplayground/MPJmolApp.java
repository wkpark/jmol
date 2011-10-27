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
 * see JsonNioService for details.
 *   
 */
package org.molecularplayground;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolViewer;
import org.jmol.constant.EnumCallback;
import org.openscience.jmol.app.jsonkiosk.BannerFrame;
import org.openscience.jmol.app.jsonkiosk.JsonNioClient;
import org.openscience.jmol.app.jsonkiosk.JsonNioService;
import org.openscience.jmol.app.jsonkiosk.KioskFrame;

/*
 * Jmol 12 implementation of the Molecular Playground
 * 
 * includes message "banner:xxxxx" intercept to 
 * display xxxxx on the banner, thus allowing that to 
 * be modified by a running script.
 * 
 */
public class MPJmolApp implements JsonNioClient {

  protected JmolViewer jmolViewer;

  public static void main(String args[]) {
    new MPJmolApp(args.length > 1 ? Integer.parseInt(args[1]) : 31416);
  }

  public MPJmolApp() {
    this(31416);
  }
  
  public MPJmolApp(int port) {
    startJsonNioKiosk(port);
  }

  private JsonNioService service;
  private BannerFrame bannerFrame;
  private KioskFrame kioskFrame;
  
  private void startJsonNioKiosk(int port) {
    KioskPanel kioskPanel = new KioskPanel();
    bannerFrame = new BannerFrame(1024, 75);
    kioskFrame = new KioskFrame(0, 75, 1024, 768 - 75, kioskPanel);
    try {
      setBannerLabel("click below and type exitJmol[enter] to quit");
      jmolViewer
          .script("set debugScript;set allowgestures;set allowKeyStrokes;set zoomLarge false;set frank off;set antialiasdisplay off");
      String path = System.getProperty("user.dir").replace('\\', '/')
          + "/Content-Cache/%ID%/%ID%.json";
      jmolViewer.script("NIOcontentPath=\"" + path + "\";NIOterminatorMessage='MP_DONE'");

      service = new JsonNioService();
      service.startService(port, this, jmolViewer, "-MP");

      // Bob's demo model
      jmolViewer
          .script("load http://chemapps.stolaf.edu/jmol/docs/examples-12/data/caffeine.xyz");

    } catch (Throwable e) {
      e.printStackTrace();
      if (service == null)
        nioClosed(null);
      else
        service.close();
    }
  }

  /// JsonNiosClient ///

  public void setBannerLabel(String label) {
    bannerFrame.setLabel(label);
  }

  public void nioClosed(JsonNioService jns) {
    try {
      jmolViewer.setModeMouse(-1);
      bannerFrame.dispose();
      kioskFrame.dispose();
    } catch (Throwable e) {
      //
    }
    System.exit(0);
  }


  ////////////////////////

  class KioskPanel extends JPanel implements JmolCallbackListener {

    private final Dimension currentSize = new Dimension();

    KioskPanel() {
      jmolViewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(),
          null, null, null, ""/*-multitouch-mp"*/, null);
      jmolViewer.setJmolCallbackListener(this);
    }

    @Override
    public void paint(Graphics g) {
      getSize(currentSize);
      jmolViewer.renderScreenImage(g, currentSize.width, currentSize.height);
    }

    // / JmolCallbackListener interface ///
    public boolean notifyEnabled(EnumCallback type) {
      switch (type) {
      case ECHO:
      case MESSAGE:
        return true;
      case ANIMFRAME:
      case LOADSTRUCT:
      case MEASURE:
      case PICK:
      case SCRIPT:
      case EVAL:
      case ATOMMOVED:
      case CLICK:
      case ERROR:
      case HOVER:
      case MINIMIZATION:
      case RESIZE:
      case SYNC:
      case APPLETREADY:
        // applet only (but you could change this for your listener)
        break;
      }
      return false;
    }

    public void notifyCallback(EnumCallback type, Object[] data) {
      
      String strInfo = (data == null || data[1] == null ? null : data[1]
                                                                      .toString());
      if (strInfo != null && strInfo.startsWith("banner:")) {
        setBannerLabel(strInfo.substring(7).trim());  
      }
        

      
      JmolCallbackListener appConsole = (JmolCallbackListener) jmolViewer
          .getProperty("DATA_API", "getAppConsole", null);
      if (appConsole != null)
        appConsole.notifyCallback(type, data);
      
    }

    public void setCallbackFunction(String callbackType, String callbackFunction) {
      // ignore
    }

  }

}
