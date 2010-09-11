/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-25 02:42:30 -0500 (Thu, 25 Jun 2009) $
 * $Revision: 11113 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, www.jmol.org
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
package org.jmol.console;


import javax.swing.JDialog;
import javax.swing.JFrame;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;

public class JmolConsoleDialog extends JDialog {

  JmolConsole jmolConsole;

  public JmolConsoleDialog() {
  }
  
  public JmolConsoleDialog(JmolConsole jmolConsole, JFrame frame) {
    super(frame, getTitleText(), false);
    this.jmolConsole = jmolConsole;
  }

  protected static String getTitleText() {
    return GT._("Jmol Script Console") + " " + Viewer.getJmolVersion();
  }
  

}
