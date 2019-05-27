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

import java.awt.Component;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.jmol.api.PlatformViewer;
import org.jmol.i18n.GT;
import org.jmol.popup.AwtSwingComponent;
import org.jmol.popup.AwtSwingPopupHelper;
import org.jmol.popup.JmolGenericPopup;
import org.jmol.popup.PopupResource;
import org.jmol.util.Elements;
import org.jmol.viewer.Viewer;

import javajs.awt.SC;
import javajs.util.P3;

public class ModelKitPopup extends JmolGenericPopup {

  private boolean hasUnitCell;
  private int state;

  public ModelKitPopup() {
    helper = new AwtSwingPopupHelper(this);
  }
  
  @Override
  public void jpiInitialize(PlatformViewer vwr, String menu) {
    updateMode = UPDATE_NEVER;
    boolean doTranslate = GT.setDoTranslate(true);
    PopupResource bundle = new ModelKitPopupResourceBundle(null, null);
    initialize((Viewer) vwr, bundle, bundle.getMenuName());
    GT.setDoTranslate(doTranslate);
  }
  
  @Override
  public void jpiUpdateComputedMenus() {
    hasUnitCell = vwr.getCurrentUnitCell() != null;
    SC menu = htMenus.get("xtalMenu");
    menu.setEnabled(hasUnitCell);
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
    if (name.startsWith("mk")) {
      int pt = name.indexOf("??");
      name = name.substring(2, pt);
      jpiSetProperty(name.substring(pt + 2), Boolean.valueOf(TF));
      return null;
    }
    
    // atom type
    String element = JOptionPane.showInputDialog(GT.$("Element?"), "");
    if (element == null || Elements.elementNumberFromSymbol(element, true) == 0)
      return null;
    menuSetLabel(item, element);
    
    ((AwtSwingComponent) item).setActionCommand("assignAtom_" + element + "P!:??");
    return "set picking assignAtom_" + element;
  }

  @Override
  public void menuClickCallback(SC source, String script) {
    if (script.equals("clearQ")) {
      for (SC item : htCheckbox.values()) {
        if (item.getActionCommand().indexOf(":??") < 0)
          continue;        
        menuSetLabel(item, "??");
        item.setActionCommand("_??P!:");
        item.setSelected(false);
        //item.setArmed(false);
      }
      script = "set picking assignAtom_C";
    }
    super.menuClickCallback(source, script);  
  }

  @Override
  protected Object getImageIcon(String fileName) {
    String imageName = "org/jmol/modelkit/images/" + fileName;
    URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
    return (imageUrl == null ? null : new ImageIcon(imageUrl));
  }

  @Override
  public void menuFocusCallback(String name, String actionCommand, boolean b) {
    // n/a
  }

  // xtal model kit only
  
  public final static int STATE_BITS_VIEWEDIT   = 0x03;//0b00000011;
  public final static int STATE_VIEW            = 0x00;//0b00000000;
  public final static int STATE_EDIT            = 0x01;//0b00000001;
  
  public final static int STATE_BITS_SYM        = 0x1c;//0b00011100;
  public final static int STATE_SYM_NONE        = 0x00;//0b00000000;
  public final static int STATE_SYM_UNITIZE     = 0x0c;//0b00001100;
  public final static int STATE_SYM_APPLYLOCAL  = 0x04;//0b00000100; // edit only
  public final static int STATE_SYM_RETAINLOCAL = 0x08;//0b00001000; // edit only
  public final static int STATE_SYM_APPLYFULL   = 0x10;//0b00010000;

  public final static int STATE_BITS_PACKING    = 0x60;//0b01100000;
  public final static int STATE_PACK_UC         = 0x20;//0b00100000;
  public final static int STATE_PACK_EXTEND     = 0x40;//0b01000000;

  public P3 atom1, atom2, offset;
  
  public String symop;  
  
  
  private void setSym(int bits) {
    state = (state & ~STATE_BITS_SYM) | bits;
  }

  private void setViewEdit(int bits) {
    state = (state & ~STATE_BITS_VIEWEDIT) | bits;
  }

  private void setPacking(int bits) {
    state = (state & ~STATE_BITS_PACKING) | bits;
  }

//  { "xtalSymmetryMenu", "mknoSymmetry??P!RD mkretainLocal??P!RD mkapplyLocal??P!RD mkapplyFull??P!RD" },
//  { "xtalPackingMenu", "mkextendCell??P!RD mkpackCell??P!RD" },
//  { "xtalOptionsMenu", "mkallAtoms??P!RD mkasymmetricUnit??P!RD mkallowElementReplacement??P!CB" }


  @Override
  public void jpiSetProperty(String name, Object value) {
    name = name.toLowerCase().intern();
    
    if (name == "view") {
      setViewEdit(STATE_VIEW);
      return;
    }
    if (name == "edit") {
      setViewEdit(STATE_EDIT);
      return;
    }

    if (name == "symop") {
      symop = (String) value;
      return;
    }
    if (name == "atom1") {
      atom1 = (P3) value;
      return;
    }
    if (name == "atom2") {
      atom2 = (P3) value;
      return;
    }
    if (name == "offset") {
      offset = (P3) value;
      return;
    }
    
    if (name == "nosymmetry") {
      setSym(STATE_SYM_NONE);
      return;
    }
    if (name == "applylocal") {
      setSym(STATE_SYM_APPLYLOCAL);
      return;
    }
    if (name == "retainlocal") {
      setSym(STATE_SYM_RETAINLOCAL);
      return;
    }
    if (name == "applyfull") {
      setSym(STATE_SYM_APPLYFULL);
      return;
    }

    if (name == "pack") {
      setPacking(STATE_PACK_UC);
      return;
    }
    if (name == "extend") {
      setPacking(STATE_PACK_EXTEND);
      return;
    }
    if (name == "addConstraint") {
      // TODO
    }
    
    if (name == "removeConstraint") {
      // TODO
    }
    
    if (name == "removeAllConstraints") {
      // TODO
    }
    
    System.err.println("ModelKitPopup.setProperty? " + name + " " + value);
    
  }

}
