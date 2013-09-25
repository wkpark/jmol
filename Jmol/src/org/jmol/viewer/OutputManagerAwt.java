/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-06-02 12:14:13 -0500 (Sat, 02 Jun 2007) $
 * $Revision: 7831 $
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

package org.jmol.viewer;

import java.awt.Image;
import java.io.BufferedWriter;
import java.util.Date;

import org.jmol.awt.ImageSelection;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.Viewer.ACCESS;

public class OutputManagerAwt extends OutputManagerAll {

  public OutputManagerAwt() {
    // by reflection only
  }
  
  @Override
  String clipImageOrPasteText(String text) {
    String msg;
    try {
      if (text == null) {
        Image image = (Image) viewer.getScreenImageBuffer(null, true);
        ImageSelection.setClipboard(image);
        msg = "OK image to clipboard: "
            + (image.getWidth(null) * image.getHeight(null));
      } else {
        ImageSelection.setClipboard(text);
        msg = "OK text to clipboard: " + text.length();
      }
    } catch (Error er) {
      msg = viewer.getErrorMessage();
    } finally {
      if (text == null)
        viewer.releaseScreenImage();
    }
    return msg;
  }

  @Override
  String getClipboardText() {    
    return ImageSelection.getClipboardText();
  }

  @Override
  String setLogFile(String value) {
    String path = null;
    String logFilePath = viewer.getLogFilePath();
    if (logFilePath == null || value.indexOf("\\") >= 0
        || value.indexOf("/") >= 0) {
      value = null;
    } else if (value.length() > 0) {
      if (!value.startsWith("JmolLog_"))
        value = "JmolLog_" + value;
      path = viewer.getAbsolutePath(privateKey, logFilePath + value);
    }
    if (path == null)
      value = null;
    else
      Logger.info(GT._("Setting log file to {0}", path));
    if (value == null || !viewer.haveAccess(ACCESS.ALL)) {
      Logger.info(GT._("Cannot set log file path."));
      value = null;
    } else {
      viewer.logFileName = path;
      viewer.global.setS("_logFile", viewer.isApplet() ? value : path);
    }
    return value;
  }

  @Override
  void logToFile(String data) {
    try {
      boolean doClear = (data.equals("$CLEAR$"));
      if (data.indexOf("$NOW$") >= 0)
        data = TextFormat.simpleReplace(data, "$NOW$", (new Date()).toString());
      if (viewer.logFileName == null) {
        System.out.println(data);
        return;
      }
      BufferedWriter out = (BufferedWriter) viewer.openLogFile(privateKey,
          viewer.logFileName, !doClear);
      if (!doClear) {
        int ptEnd = data.indexOf('\0');
        if (ptEnd >= 0)
          data = data.substring(0, ptEnd);
        out.write(data);
        if (ptEnd < 0)
          out.write("\n");
      }
      out.close();
    } catch (Exception e) {
      if (Logger.debugging)
        Logger.debug("cannot log " + data);
    }
  }  

}
