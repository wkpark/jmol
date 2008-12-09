/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.jmol.applet;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jmol.api.*;
import org.jmol.util.Logger;

class Jvm12 {

  protected JmolViewer viewer;
  private Component awtComponent;
  Console console;
  protected String appletContext;

  Jvm12(Component awtComponent, JmolViewer viewer, String appletContext) {
    this.awtComponent = awtComponent;
    this.viewer = viewer;
    this.appletContext = appletContext;
    try {
      UIManager
          .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception exc) {
      System.err.println("Error loading L&F: " + exc);
    }
  }

  private final Rectangle rectClip = new Rectangle();
  private final Dimension dimSize = new Dimension();

  Rectangle getClipBounds(Graphics g) {
    return g.getClipBounds(rectClip);
  }

  Dimension getSize() {
    return awtComponent.getSize(dimSize);
  }

  void showConsole(boolean showConsole) {
    if (!showConsole) {
      if (console != null) {
        console.setVisible(false);
        console = null;
      }
      return;
    }
    getConsole();
    if (console != null)
      console.setVisible(true);
  }

  void consoleMessage(String message) {
    console.output(message);
  }

  boolean haveConsole() {
    return (console != null);
  }

  Console getConsole() {
    if (console == null) {
      try {
        console = new Console(awtComponent, viewer, this);
      } catch (Exception e) {
        Logger.debug("Jvm12/console exception");
      }
    }
    if (console == null) {
      try { //try again -- Java 1.6.0 bug? When "console" is given in a script, but not the menu
        console = new Console(awtComponent, viewer, this);
      } catch (Exception e) {
      }
    }
    return console;
  }

  String getConsoleMessage() {
    return console.getText();
  }

  final protected static String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM" };
  final protected static String[] imageExtensions = { "jpg", "png", "gif", "ppm" };

  static JmolDialogInterface newDialog(boolean forceNewTranslation) {
    JmolDialogInterface sd = (JmolDialogInterface) Interface
        .getOptionInterface("export.dialog.Dialog");
    sd.setupUI(forceNewTranslation);
    return sd;
  }
  
  String inputFileName;
  String outputFileName;
  String dialogType;
  
  public String dialogAsk(String type, String fileName) {
    inputFileName = fileName;
    dialogType = type;
    //System.out.println("Jvm12 thread: " + Thread.currentThread().getName());
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          if (dialogType.equals("load")) {
            outputFileName = newDialog(false).getOpenFileNameFromDialog(
                appletContext, viewer, inputFileName, null, null, false);
            return;
          }
          JmolDialogInterface sd = newDialog(false);
          if (dialogType.equals("save")) {
            outputFileName = sd.getSaveFileNameFromDialog(viewer,
                inputFileName, null);
            return;
          }
          if (dialogType.startsWith("saveImage")) {
            outputFileName = sd.getImageFileNameFromDialog(viewer,
                inputFileName, imageType, imageChoices, imageExtensions,
                qualityJPG, qualityPNG);
            qualityJPG = sd.getQuality("JPG");
            qualityPNG = sd.getQuality("PNG");
            String sType = sd.getType();
            if (sType != null)
              imageType = sType;
            int iQuality = sd.getQuality(sType);
            if (iQuality >= 0)
              imageQuality = iQuality;
            return;
          }
          outputFileName = null;
        }
      });
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    } catch (InvocationTargetException e) {
      System.out.println(e.getMessage());
    }
    return outputFileName;
  }

  int qualityJPG = -1;
  int qualityPNG = -1;
  String imageType;
  int imageQuality;

  /**
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return          null (canceled) or a message starting with OK or an error message
   */
  String createImage(String fileName, String type, Object text_or_bytes,
                     int quality) {
    if (quality == Integer.MIN_VALUE) {
      // text or bytes
      fileName = dialogAsk("save", fileName);
    } else {
      imageType = type.toUpperCase();
      imageQuality = quality;
      fileName = dialogAsk("saveImage+" + type, fileName);
      quality = imageQuality;
      type = imageType;
    }
    if (fileName == null)
      return null;
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
        .getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    return (String) c.createImage(fileName, type, text_or_bytes, quality);
  }

  String getClipboardText() {
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
        .getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    return c.getClipboardText();
  }

}
