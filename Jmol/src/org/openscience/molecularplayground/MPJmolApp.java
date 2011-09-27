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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.openscience.jmol.app.jmolpanel.BannerFrame;
import org.openscience.jmol.app.jmolpanel.JsonNioClient;
import org.openscience.jmol.app.jmolpanel.JsonNioService;
import org.openscience.jmol.app.jmolpanel.KioskFrame;

public class MPJmolApp implements JsonNioClient {

  protected JmolViewer jmolViewer;

  //static final String magicWord = "{\"magic\":\"JmolApp\"\r\n";

  public static void main(String args[]) {
    new MPJmolApp(args.length > 1 ? Integer.parseInt(args[1]) : 31416);
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

    /*
     * Separate sockets are used for incoming and outgoing messages,
     * since communication to/from the Hub is completely asynchronous
     *
     * All messages are sent as JSON strings, terminated by a CR/LF.
     */

    try {
      setBannerLabel("click below and type exitJmol[enter] to quit");
      jmolViewer
          .script("set allowKeyStrokes;set zoomLarge false;set frank off;set antialiasdisplay off");
      String path = System.getProperty("user.dir").replace('\\', '/')
          + "/Content-Cache/%ID%/%ID%.json";
      jmolViewer.script("NIOcontentPath=\"" + path + "\";NIOterminatorMessage='MP_DONE'");

      service = new JsonNioService();
      service.startService(port, this, jmolViewer);

      // Bob's demo model
      jmolViewer
          .script("load http://chemapps.stolaf.edu/jmol/docs/examples-12/data/caffeine.xyz");

    } catch (Throwable e) {
      e.printStackTrace();
      if (service == null)
        nioClosed();
      else
        service.close();
    }
  }

  /// JsonNiosClient ///

  public void setBannerLabel(String label) {
    bannerFrame.setLabel(label);
  }

  public void nioClosed() {
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

  class KioskPanel extends JPanel {

    private final Dimension currentSize = new Dimension();

    KioskPanel() {
      jmolViewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(),
          null, null, null, ""/*-multitouch-mp"*/, null);
    }

    @Override
    public void paint(Graphics g) {
      getSize(currentSize);
      jmolViewer.renderScreenImage(g, currentSize.width, currentSize.height);
    }

  }

}
