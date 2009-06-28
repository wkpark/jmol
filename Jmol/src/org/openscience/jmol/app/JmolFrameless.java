/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.openscience.jmol.app;

import java.awt.Dimension;
import java.util.Hashtable;

import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.export.image.ImageCreator;
import org.jmol.i18n.GT;
import org.jmol.util.TextFormat;

public class JmolFrameless {
  
  /*
   * no Java Swing to be found. No implementation of any graphics or 
   * containers at all. 
   * 
   * Just a great little answer machine that can load models, 
   * do scripted analysis of their structures, and spit out text and
   * JPG or PNG images.
   * 
   */

  public JmolApp jmolApp;
  public JmolViewer viewer;
  
  public static JmolFrameless getJmol(int width, int height, String commandOptions) {
    JmolApp jmolApp = new JmolApp();
    jmolApp.haveDisplay = false;
    jmolApp.startupHeight = height;
    jmolApp.startupWidth = width;
    String[] args = TextFormat.split(commandOptions, ' '); // doesn't allow for double-quoted 
    jmolApp.parseCommandLine(args);
    return new JmolFrameless(jmolApp);
  }

  public JmolFrameless(JmolApp jmolApp) {
    this.jmolApp = jmolApp;
    viewer = JmolViewer.allocateViewer(null, null, 
        null, null, null, jmolApp.commandOptions, 
        new MyStatusListenerFrameless());
    viewer.setScreenDimension(new Dimension(jmolApp.startupWidth, jmolApp.startupHeight));
    jmolApp.startViewer(viewer, null);
  }
  
  public static void main(String[] args) {
    JmolApp jmolApp = new JmolApp();
    jmolApp.haveDisplay = false;
    jmolApp.exitUponCompletion = true;
    jmolApp.parseCommandLine(args);    
    new JmolFrameless(jmolApp);
  }
  
  String createImageStatus(String fileName, String type, Object text_or_bytes,
                           int quality) {
    return (String) (new ImageCreator(viewer)).createImage(fileName,
        type, text_or_bytes, quality);
  }

  class MyStatusListenerFrameless implements JmolStatusListener {

    // we should just need these two methods:

    public String createImage(String fileName, String type, Object text_or_bytes,
                              int quality) {
      return createImageStatus(fileName, type, text_or_bytes, quality);
    }

    public void setCallbackFunction(String callbackType, String callbackFunction) {
      if (callbackType.equalsIgnoreCase("language")) {
        new GT(callbackFunction);
      }
    }
    
    // all the rest are unneeded in a frameless environment
    
    public boolean notifyEnabled(int type) {
      return false;
    }

    public void notifyCallback(int type, Object[] data) {
    }

    public String eval(String strEval) {
      return "# 'eval' is implemented only for the applet.";
    }

    public void handlePopupMenu(int x, int y) {
    }

    public void showUrl(String url) {
    }

    public void showConsole(boolean showConsole) {
    }

    public float[][] functionXY(String functionName, int nX, int nY) {
      nX = Math.abs(nX);
      nY = Math.abs(nY);
      float[][] f = new float[nX][nY];
      return f;
    }

    public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
      nX = Math.abs(nX);
      nY = Math.abs(nY);
      nZ = Math.abs(nZ);
      float[][][] f = new float[nX][nY][nZ];
      return f;
    }

    public Hashtable getRegistryInfo() {
      return null;
    }

    public String dialogAsk(String type, String fileName) {
      return null;
    }
  }

}  

