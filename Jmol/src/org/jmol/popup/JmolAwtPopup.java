/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
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
package org.jmol.popup;

import java.awt.Component;

import javajs.awt.SC;

import javax.swing.JPopupMenu;

public class JmolAwtPopup extends JmolGenericPopup {

  public JmolAwtPopup() {
    helper = new AwtSwingPopupHelper(this);
  }

  @Override
  protected void menuShowPopup(SC popup, int x, int y) {
    try {
      ((JPopupMenu)((AwtSwingComponent)popup).jc).show((Component) vwr.display, x, y);
    } catch (Exception e) {
      // ignore
    }
  }
  
  @Override
  public String menuSetCheckBoxOption(SC item, String name, String what, boolean TF) {
    // n/a
    return null;
  }
  
  @Override
  protected Object getImageIcon(String fileName) {
    // n/a
    return null;
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // n/a ?? 
  }


}
