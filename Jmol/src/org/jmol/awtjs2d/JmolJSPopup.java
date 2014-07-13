/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-10-14 11:28:38 -0500 (Fri, 14 Oct 2011) $
 * $Revision: 16354 $
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
package org.jmol.awtjs2d;

import javajs.api.PlatformViewer;
import javajs.api.SC;
import javajs.awt.Component;
import javajs.swing.JPopupMenu;

import org.jmol.i18n.GT;
import org.jmol.popup.JSSwingPopupHelper;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.MainPopupResourceBundle;
import org.jmol.popup.PopupResource;
import org.jmol.viewer.Viewer;

public class JmolJSPopup extends JmolGenericPopup {
  

  /**
   * The main popup window for the applet -- as JavaScript
   * 
   */
  public JmolJSPopup() {
    helper = new JSSwingPopupHelper(this);
  }

  @Override
  public void jpiInitialize(PlatformViewer vwr, String menu) {
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = new MainPopupResourceBundle(strMenuStructure = menu,
        menuText);
    initialize((Viewer) vwr, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }

  @Override
  public void menuShowPopup(SC popup, int x, int y) {
    try {
      ((JPopupMenu) popup).show(isTainted ? (Component) vwr.getHTML5Applet() : null, x, y);
    } catch (Exception e) {
      // ignore
    }
    isTainted = false;
  }

  @Override
  protected String menuSetCheckBoxOption(SC item, String name,
                                         String what) {
    // ModelKit popup only
    return null;
  }
  
  @Override
  protected Object getImageIcon(String fileName) {
    // ModelKit menu only
    return null;
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // TODO
  }




}
