/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;

import java.io.File;

public class JmolFileFilter extends javax.swing.filechooser.FileFilter {

  private String endMask = ".";
  private String name = "";
  private boolean acceptNoDots = false;

  /**Creates a filter which will accept only files ending in
   * mask. If acceptNoDot is true then files without an extension
   * are also accepted.
   * @param mask String in which accepted files will end
   * @param typeName Name of this type and colon, eg "Xmol", use null none for none
   * @param acceptNoDot If true then files without dots (eg Unix files) will be accepted.
   **/
  public JmolFileFilter(String mask, String typeName, boolean acceptNoDot) {

    endMask = mask.toLowerCase();
    if (typeName != null) {
      name = typeName + " (" + "*" + endMask + ")";
    } else {
      name = "*" + endMask;
    }
    acceptNoDots = acceptNoDot;
  }

  /**Overrides accept() in
   * javax.swing.filechooser.FileFilter. Always accepts
   * directories.
   **/
  public boolean accept(File f) {

    String fname = f.getName();
    if (f.isDirectory()) {
      return true;
    } else if (fname.indexOf(".") == -1) {
      return acceptNoDots;
    } else {
      return (fname.toLowerCase().endsWith(endMask));
    }
  }

  /**Overrides getDescription() in
   *  javax.swing.filechooser.FileFilter. Returns the current
   *  mask.**/
  public String getDescription() {
    return (name);
  }
}
