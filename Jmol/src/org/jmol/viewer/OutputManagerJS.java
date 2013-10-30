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

import javajs.util.OutputChannel;

public class OutputManagerJS extends OutputManager {

  public OutputManagerJS() {
    // by reflection only
  }

  @Override
  protected String getLogPath(String fileName) {
    return fileName;
  }

  @Override
  String clipImageOrPasteText(String text) {
    return "Clipboard not available";
  }

  @Override
  String getClipboardText() {
    return "Clipboard not available";
  }

  @Override
  OutputChannel openOutputChannel(double privateKey, String fileName,
                                      boolean asWriter, boolean asAppend) {
    return (new OutputChannel())
        .setParams(viewer.fileManager, fileName, asWriter, null);
  }

  @Override
  protected String createSceneSet(String sceneFile, String type, int width,
                                int height) {
    return "ERROR: Not Available";
  }

}
