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
    { "modelkitMenu", "atomMenu bondMenu " +
    		"- new center addh minimize " +
    		"- saveFileSIGNED saveStateSIGNED exit" },
    { "atomMenu" , "assignAtom_XP!CB dragMoleculeP!CB dragAtomP!CB dragMinimizeP!CB " +
    		"- assignAtom_CP!CB assignAtom_HP!CB assignAtom_NP!CB assignAtom_OP!CB " +
    		"_??P!CB _??P!CB _??P!CB " +
    		"moreAtomMenu - assignAtom_PlP!CB assignAtom_MiP!CB" },
    { "moreAtomMenu", "clearQ - _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB " },
    { "bondMenu", "assignBond_0P!CB assignBond_1P!CB assignBond_2P!CB assignBond_3P!CB - assignBond_pP!CB assignBond_mP!CB - rotateBondP!CB" },
  };
  
  private static String[][] structureContents = {
    { "new" , "zap" },
    { "minimize" , "minimize" },
    { "center" , "center visible" },
    { "addh" , "calculate hydrogens {model=_lastframe}" },
    { "saveFileSIGNED", "select visible;write COORD '?jmol.mol'" },
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
        "assignAtom_XP!CB" , GT._("delete atom"),
        "dragMoleculeP!CB" , GT._("drag molecule"),
        "dragAtomP!CB" , GT._("drag atom"),
        "dragMinimizeP!CB" , GT._("drag-minimize"),
        "assignAtom_CP!CB" , "C",
        "assignAtom_HP!CB" , "H",
        "assignAtom_NP!CB" , "N",
        "assignAtom_OP!CB" , "O",
        "_??P!CB", "??",
        "assignAtom_PlP!CB", GT._("increase charge"),
        "assignAtom_MiP!CB", GT._("decrease charge"),
        "assignBond_0P!CB" , GT._("delete bond"),
        "assignBond_1P!CB", GT._("single"),
        "assignBond_2P!CB", GT._("double"),
        "assignBond_3P!CB", GT._("triple"),
        "assignBond_pP!CB", GT._("increase order"),
        "assignBond_mP!CB", GT._("decrease order"),
        "rotateBondP!CB", GT._("rotate bond"),
        "exit", GT._("exit modelkit mode"),
    };
 
    if (!wasTranslating)
      GT.setDoTranslate(wasTranslating);

    return words;
  }

}
