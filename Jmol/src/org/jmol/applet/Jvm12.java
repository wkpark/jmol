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
    return (console!= null);
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

  private JFileChooser exportChooser;
  final private static String[] imageChoices = { "JPEG", "PNG", "GIF" };
  final private static String[] imageExtensions = { "jpg", "png", "gif" };
  
  String createImage(String fileName, String type, Object text_or_bytes, int quality) {
    File file = null;
    String pathName;
    if (exportChooser == null)
      exportChooser = new JFileChooser();
    JmolImageTyperInterface it = (JmolImageTyperInterface) Interface
        .getOptionInterface("export.image.ImageTyper");
    if (fileName == null) {
      fileName = viewer.getModelSetFileName();
      pathName = viewer.getModelSetPathName();
      it.createPanel(exportChooser, imageChoices, imageExtensions, 0);
      if ((fileName != null) && (pathName != null)) {
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart != -1) {
          fileName = fileName.substring(0, extensionStart) + "."
              + it.getExtension();
        }
        file = new File(pathName, fileName);
      }
    } else {
      int n;
      for (n = 0; n < imageExtensions.length; n++)
        if (fileName.indexOf(imageExtensions[n]) >= 0)
          break;
      it.createPanel(exportChooser, imageChoices, imageExtensions, n
          % imageExtensions.length);
      file = new File(fileName);
    }

    exportChooser.setSelectedFile(file);
    if (exportChooser.showSaveDialog(awtComponent) != 0)
      return null;
    it.memorizeDefaultType();
    file = exportChooser.getSelectedFile();
    if (file == null)
      return null;
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
        .getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    String sType = it.getType();
    if (sType == null)
      sType = type.toUpperCase();
    else {
      text_or_bytes = null;
      quality = 75;
    }
    int iQuality = it.getQuality(sType);
    if (iQuality < 0)
      iQuality = quality;
    return c.createImage(file.getAbsolutePath(), sType, text_or_bytes, iQuality);
  }

  void clipImage() {
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface.getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    c.clipImage(null);
  }
  
  String getClipboardText() {
    JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface.getOptionInterface("export.image.ImageCreator");
    c.setViewer(viewer);
    return c.getClipboardText();
  }
}
