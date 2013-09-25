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

import java.io.IOException;
import java.util.Map;

import org.jmol.io.JmolOutputChannel;
import org.jmol.util.Logger;

public class OutputManagerJS extends OutputManagerAll {

  public OutputManagerJS() {
      // by reflection only
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
  protected boolean getImageBytes2(Object objImage, String type,
                                   JmolOutputChannel out,
                                   Map<String, Object> params, String[] errRet)
      throws IOException {
    errRet[0] = "image type " + type + " not available on this platform";
    return false;
  }

  @Override
  void logToFile(String data) {
    Logger.info(data);
  }

  @Override
  String setLogFile(String name) {
    Logger.info("cannot set LogFile name");
    return null;
  }

}
