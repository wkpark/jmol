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
package org.jmol.modelkit;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.jmol.api.JmolPopupInterface;
import org.jmol.i18n.GT;
import org.jmol.popup.SwingPopup;
import org.jmol.util.Elements;
import org.jmol.viewer.Viewer;

public class ModelKitPopup extends SwingPopup implements JmolPopupInterface {

  public ModelKitPopup() {
    // required by reflection
  }
  
  public Object getJMenu() {
    return null;
  }

  public String getMenu(String string) {
    return null;
  }
  
  public void initialize(Viewer viewer, boolean doTranslate, String menu,
                         boolean asPopup) {
    GT.setDoTranslate(true);
    imagePath = "org/jmol/modelkit/images/"; 
    initialize(viewer, "modelkitMenu", new ModelKitPopupResourceBundle(), false);
    GT.setDoTranslate(doTranslate);
  }
    
  public void show(int x, int y) {
    show(x, y, false);
  }

  public void updateComputedMenus() {    
  }
  
  @Override
  public void checkMenuClick(Object source, String script) {
    if (script.equals("clearQ")) {
      for (Object o : htCheckbox.values()) {
        JMenuItem item = (JMenuItem) o;
        if (item.getActionCommand().indexOf(":??") < 0)
          continue;        
        setLabel(item, "??");
        item.setActionCommand("_??P!:");
        item.setSelected(false);
        item.setArmed(false);
      }
      viewer.evalStringQuiet("set picking assignAtom_C");
      return;
    }
    super.checkMenuClick(source, script);  
  }
  
  @Override
  protected String setCheckBoxOption(Object item, String name, String what) {
    // atom type
    String element = JOptionPane.showInputDialog(GT._("Element?"), "");
    if (element == null || Elements.elementNumberFromSymbol(element, true) == 0)
      return null;
    setLabel(item, element);
    ((JMenuItem)item).setActionCommand("assignAtom_" + element + "P!:??");
    return "set picking assignAtom_" + element;
  }

}
