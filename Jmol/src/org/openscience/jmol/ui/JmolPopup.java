/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2003  The Jmol Development Team
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
package org.openscience.jmol.ui;
import org.openscience.jmol.viewer.JmolViewer;
import java.awt.Component;
import java.util.ResourceBundle;

public class JmolPopup {
  JmolPopupSwing popupSwing;
  JmolPopupAwt popupAwt;
  boolean useSwing;

  public JmolPopup(JmolViewer viewer, Component parent) {
    ResourceBundle rbStructure =
      ResourceBundle.getBundle("org.openscience.jmol.ui." +
                               "JmolPopupStructure");
    ResourceBundle rbWords =
      ResourceBundle.getBundle("org.openscience.jmol.ui." +
                               "JmolPopupWords");
    useSwing = viewer.jvm12orGreater;
    if (useSwing)
      popupSwing = new JmolPopupSwing(viewer, parent, rbStructure, rbWords);
    else
      popupAwt = new JmolPopupAwt(viewer, parent, rbStructure, rbWords);
    rbWords = null;
  }

  public void show(int x, int y) {
    if (useSwing)
      popupSwing.showSwing(x, y);
    else
      popupAwt.showAwt(x, y);
  }
}
