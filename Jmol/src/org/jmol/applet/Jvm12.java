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

import org.jmol.api.*;
import org.jmol.util.Logger;

class Jvm12 {

  private JmolViewer viewer;
  JmolAdapter modelAdapter;
  private Component awtComponent;
  Console console;

  Jvm12(Component awtComponent, JmolViewer viewer, JmolAdapter modelAdapter) {
    this.awtComponent = awtComponent;
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
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

  final private static String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM" };
  final private static String[] imageExtensions = { "jpg", "png", "gif", "PPM" };

  static JmolDialogInterface newDialog(boolean forceNewTranslation) {
    JmolDialogInterface sd = (JmolDialogInterface) Interface
        .getOptionInterface("export.dialog.Dialog");
    sd.setupUI(forceNewTranslation);
    return sd;
  }
  
  public String dialogAsk(JmolDialogInterface sd, String type,
                          String fileName) {
    if (type.equals("load"))
      return getOpenFileNameFromDialog(fileName);
    if (sd == null)
      sd = newDialog(false);
    if (type.equals("save")) {
      return sd.getSaveFileNameFromDialog(viewer, fileName, null);
    }
    if (type.startsWith("saveImage")) {
      return sd.getImageFileNameFromDialog(viewer, fileName,
          imageType, imageChoices, imageExtensions, qualityJPG, qualityPNG);
    }
    return null;
  }

  public String getOpenFileNameFromDialog(String fileName) {
    return newDialog(false).getOpenFileNameFromDialog(modelAdapter, viewer, fileName, null, null, false);
  }

  int qualityJPG = -1;
  int qualityPNG = -1;
  String imageType;

  String createImage(String fileName, String type, Object text_or_bytes,
                     int quality) {
    String sType = null;
    int iQuality = quality;
    if (quality == Integer.MIN_VALUE) {
      // text or bytes
      fileName = dialogAsk(null, "save", fileName);
    } else {
      // image
      JmolDialogInterface sd = newDialog(false);
      fileName = dialogAsk(sd, "saveImage+" + type, fileName);
      qualityJPG = sd.getQuality("JPG");
      qualityPNG = sd.getQuality("PNG");
      sType = imageType = sd.getType();
      if (sType == null)
        sType = type.toUpperCase();
      iQuality = sd.getQuality(sType);
      if (iQuality < 0)
        iQuality = quality;
    }
    if (fileName == null)
      return null;
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
        .getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    return c.createImage(fileName, sType, text_or_bytes, iQuality);
  }

  void clipImage() {
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
        .getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    c.clipImage(null);
  }

  String getClipboardText() {
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
        .getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    return c.getClipboardText();
  }

}
