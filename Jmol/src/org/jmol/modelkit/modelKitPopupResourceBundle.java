/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-04-29 07:22:46 -0500 (Thu, 29 Apr 2010) $
 * $Revision: 12980 $
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

import org.jmol.i18n.GT;
import org.jmol.popup.PopupResource;

class ModelKitPopupResourceBundle extends PopupResource {

  ModelKitPopupResourceBundle() {
    super(null, null);
  }

  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    setStructure(menuStructure);
  }
    
  private static String[][] menuContents = {
    { "modelkitMenu", "atomMenu bondMenu - new center addh minimize saveFileSIGNED saveStateSIGNED exit" },
    { "atomMenu" , "assignAtom_XP!RD dragAtomP!RD dragMinimizeP!RD " +
    		"- assignAtom_CP!RD assignAtom_HP!RD assignAtom_NP!RD assignAtom_OP!RD " +
    		"_??P!RD _??P!RD _??P!RD " +
    		"moreAtomMenu - assignAtom_PlP!RD assignAtom_MiP!RD" },
    { "moreAtomMenu", "clearQ - _??P!RD _??P!RD _??P!RD _??P!RD _??P!RD _??P!RD " },
    { "bondMenu", "assignBond_0P!RD assignBond_1P!RD assignBond_2P!RD assignBond_3P!RD - assignBond_pP!RD assignBond_mP!RD - rotateBondP!RD" },
  };
  
  private static String[][] structureContents = {
    { "new" , "zap" },
    { "minimize" , "minimize" },
    { "center" , "center visible" },
    { "addh" , "delete hydrogens; calculate hydrogens" },
    { "saveFileSIGNED", "write COORD '?jmol.mol'" },
    { "saveStateSIGNED", "write '?jmol.jpg'" },
    { "clearQ", "clearQ" },
    { "exit", "set modelkitMode false" },
  };
  
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.getDoTranslate();
    if (!wasTranslating)
      GT.setDoTranslate(true);
    String[] words = new String[] {
        "atomMenu", GT._("atoms"),
        "moreAtomMenu", GT._("more..."),
        "bondMenu", GT._("bonds"),
        "new", GT._("new"),
        "center", GT._("center"),
        "minimize", GT._("minimize"),
        "clearQ", GT._("clear"),
        "saveFileSIGNED", GT._("save file"),
        "saveStateSIGNED", GT._("save state"),
        "addh", GT._("add hydrogens"),
        "assignAtom_XP!RD" , GT._("delete atom"),
        "dragAtomP!RD" , GT._("drag atom"),
        "dragMinimizeP!RD" , GT._("drag-minimize"),
        "assignAtom_CP!RD" , "C",
        "assignAtom_HP!RD" , "H",
        "assignAtom_NP!RD" , "N",
        "assignAtom_OP!RD" , "O",
        "_??P!RD", "??",
        "assignAtom_PlP!RD", GT._("increase charge"),
        "assignAtom_MiP!RD", GT._("decrease charge"),
        "assignBond_0P!RD" , GT._("delete bond"),
        "assignBond_1P!RD", GT._("single"),
        "assignBond_2P!RD", GT._("double"),
        "assignBond_3P!RD", GT._("triple"),
        "assignBond_pP!RD", GT._("increase order"),
        "assignBond_mP!RD", GT._("decrease order"),
        "rotateBondP!RD", GT._("rotate bond"),
        "exit", GT._("exit modelkit mode"),
    };
 
    if (!wasTranslating)
      GT.setDoTranslate(wasTranslating);

    return words;
  }

}
