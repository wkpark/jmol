/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

/**
 * JFileChooser with possibility to fix size and location
 */
public class FileChooser extends JFileChooser {

  private Point dialogLocation = null;
  private Dimension dialogSize = null;
  private JDialog dialog = null;

  /* (non-Javadoc)
   * @see javax.swing.JFileChooser#createDialog(java.awt.Component)
   */
  protected JDialog createDialog(Component parent) {
    dialog = super.createDialog(parent);
    if (dialog != null) {
      if (dialogLocation != null) {
        dialog.setLocation(dialogLocation);
      }
      if (dialogSize != null) {
        dialog.setSize(dialogSize);
      }
    }
    return dialog;
  }

  /**
   * @param p Location of the JDialog
   */
  public void setDialogLocation(Point p) {
  	dialogLocation = p;
  }
  
  /**
   * @param d Size of the JDialog
   */
  public void setDialogSize(Dimension d) {
    dialogSize = d;
  }
  
  /**
   * @return Dialog containing the JFileChooser
   */
  public JDialog getDialog() {
    return dialog;
  }
}
