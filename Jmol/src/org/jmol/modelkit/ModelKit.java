/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-30 18:58:33 -0500 (Tue, 30 Jun 2009) $
 * $Revision: 11158 $
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
package org.jmol.modelkit;

import org.jmol.api.JmolModelKitInterface;
import org.jmol.constant.modelKitPopupResourceBundle;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class ModelKit implements JmolModelKitInterface {

  private ModelKitPopup modelkitMenu;
  private final static String IMAGE_PATH = "org/jmol/modelkit/images/";

  public ModelKit() {
    // necessary for reflection
  }

  /* (non-Javadoc)
   * @see org.jmol.modelkit.JmolModelKitInterface#getModelKit(org.jmol.viewer.Viewer, Object)
   */
  public JmolModelKitInterface getModelKit(Viewer viewer) {
    return new ModelKit(viewer);
  }

  /**
   * @param viewer
   */
  private ModelKit(Viewer viewer) {
    GT.setDoTranslate(true);
    try {
      modelkitMenu = new ModelKitPopup(viewer, "modelkitMenu",
          new modelKitPopupResourceBundle(), IMAGE_PATH);
    } catch (Exception e) {
      Logger.error("Modelkit menus not loaded");
    }
    GT.setDoTranslate(true);
  }

  /* (non-Javadoc)
   * @see org.jmol.modelkit.JmolModelKitInterface#show(int, int, java.lang.String)
   */
  public void show(int x, int y, char type) {
    // type 'a' atom 'b' bond 'm' main -- ignored
    if (modelkitMenu != null)
      modelkitMenu.show(x, y);
  }  
}
