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

import org.jmol.api.*;

import java.awt.*;
import java.io.File;

import javax.swing.JFileChooser;

import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;

class Jvm12 {

  private JmolViewer viewer;
  private Component awtComponent;
  Console console;

  Jvm12(Component awtComponent, JmolViewer viewer) {
    this.awtComponent = awtComponent;
    this.viewer = viewer;
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

  private JFileChooser exportChooser, openChooser, saveChooser;
  final private static String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM" };
  final private static String[] imageExtensions = { "jpg", "png", "gif", "PPM" };

  public String dialogAsk(JmolSaveDialogInterface sd, String type,
                          String fileName) {
    if (type.equals("load"))
      return getOpenFileNameFromDialog(fileName);
    if (sd == null)
      sd = (JmolSaveDialogInterface) Interface
          .getOptionInterface("export.__SaveDialog");
    if (type.equals("save")) {
      if (saveChooser == null)
        saveChooser = new JFileChooser();
      return sd.getSaveFileNameFromDialog(saveChooser, viewer, fileName, null);
    }
    if (type.startsWith("saveImage")) {
      if (exportChooser == null)
        exportChooser = new JFileChooser();
      return sd.getImageFileNameFromDialog(exportChooser, viewer, fileName,
          imageType, imageChoices, imageExtensions, qualityJPG, qualityPNG);
    }
    return null;
  }

  public String getOpenFileNameFromDialog(String fileName) {
    if (openChooser == null) {
      openChooser = new JFileChooser();
    }
    //    String ext = null;
    if (fileName != null) {
      if (fileName.length() > 0)
        openChooser.setSelectedFile(new File(fileName));
      if (fileName.indexOf(":") < 0)
        openChooser.setCurrentDirectory(FileManager.getLocalDirectory(viewer));
      //      int pt;
      //      if ((pt = fileName.indexOf(".")) >= 0) {
      //        ext = fileName.substring(pt + 1);
      //    }
    }
    int ret = openChooser.showOpenDialog(awtComponent);
    if (ret != JFileChooser.APPROVE_OPTION)
      return null;
    File file = openChooser.getSelectedFile();
    if (file == null)
      return null;
    viewer.setStringProperty("currentLocalPath", file.getParent());
    return file.getAbsolutePath();
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
      JmolSaveDialogInterface sd = (JmolSaveDialogInterface) Interface
          .getOptionInterface("export.__SaveDialog");
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
