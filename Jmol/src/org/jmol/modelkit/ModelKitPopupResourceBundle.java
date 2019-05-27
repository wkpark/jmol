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

import java.util.Properties;

import org.jmol.i18n.GT;
import org.jmol.popup.PopupResource;

public class ModelKitPopupResourceBundle extends PopupResource {

  public ModelKitPopupResourceBundle(String menuStructure,
      Properties menuText) {
    super(menuStructure, menuText);
  }

  private final static String MENU_NAME = "modelkitMenu";

  @Override
  public String getMenuName() {
    return MENU_NAME; 
  }

  @Override
  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    if (menuStructure != null)
      setStructure(menuStructure, new GT());
  }
  
  private static String[][] menuContents = {
    { MENU_NAME, "atomMenu bondMenu xtalMenu optionsMenu" },
    { "optionsMenu", "new center addh minimize hmin " +
    		" - undo redo - SIGNEDsaveFile SIGNEDsaveState exit" },
    { "atomMenu" , "assignAtom_XP!CB assignAtom_XxP!CB dragAtomP!CB dragMinimizeP!CB dragMoleculeP!CB dragMinimizeMoleculeP!CB " +
    		"invertStereoP!CB - assignAtom_CP!CB assignAtom_HP!CB assignAtom_NP!CB assignAtom_OP!CB assignAtom_FP!CB assignAtom_ClP!CB assignAtom_BrP!CB " +
    		"_??P!CB _??P!CB _??P!CB " +
    		"moreAtomMenu - assignAtom_PlP!CB assignAtom_MiP!CB" },
    { "moreAtomMenu", "clearQ - _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB _??P!CB " },
    { "bondMenu", "assignBond_0P!CB assignBond_1P!CB assignBond_2P!CB assignBond_3P!CB - assignBond_pP!CB assignBond_mP!CB - rotateBondP!CB" },
    { "xtalMenu", "xtalSymmetryMenu xtalPackingMenu xtalOptionsMenu" },
    { "xtalSymmetryMenu", "mknoSymmetry??P!RD mkretainLocal??P!RD mkapplyLocal??P!RD mkapplyFull??P!RD" },
    { "xtalPackingMenu", "mkextendCell??P!RD mkpackCell??P!RD" },
    { "xtalOptionsMenu", "mkallAtoms??P!RD mkasymmetricUnit??P!RD mkallowElementReplacement??P!CB" }

  };
  
  private static String[][] structureContents = {
      { "new" , "zap" },
    { "center" , "zoomto 0 {visible} 0/1.5" },
    { "addh" , "calculate hydrogens {model=_lastframe}" },
    { "minimize" , "minimize" },
    { "hmin" , "delete hydrogens and model=_lastframe; minimize addhydrogens" },
    { "SIGNEDsaveFile", "select visible;write COORD '?jmol.mol'" },
    { "SIGNEDsaveState", "write '?jmol.jpg'" },
    { "clearQ", "clearQ" },
    { "undo" , "!UNDO" },
    { "redo" , "!REDO" },
    { "exit", "set modelkitMode false" },
  };
  
  @Override
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.setDoTranslate(true);
    String[] words = new String[] {
        "atomMenu", "<atoms.png>",//GT.$("atoms"),
        "moreAtomMenu", "<dotdotdot.png>",//GT.$("more..."),
        "bondMenu", "<bonds.png>",//GT.$("bonds"),
        "optionsMenu", "<dotdotdot.png>",//GT.$("atoms"),
        "xtalMenu", "<xtal.png>",
        "xtalSymmetryMenu", "symmetry",
        "xtalOptionsmenu", "options",
        "xtalPackingMenu", "packing",
        "mknoSymmetry??P!RD", GT.$("none"),
        "mkretainLocal??P!RD", GT.$("retain local"),
        "mkapplyLocal??P!RD", GT.$("apply local"),
        "mkapplyFull??P!RD", GT.$("apply full"),
        "mkextendCell??P!RD", GT.$("extend cell"),
        "mkpackCell??P!RD", GT.$("pack cell"),
        "mkasymmetricUnit??P!RD", GT.$("asymmetric unit"),
        "mkallAtoms??P!RD", GT.$("all atoms"),
        "new" , "zap",
        "new", GT.$("new"),
        "undo", GT.$("undo (CTRL-Z)"),
        "redo", GT.$("redo (CTRL-Y)"),
        "center", GT.$("center"),
        "addh", GT.$("add hydrogens"),
        "minimize", GT.$("minimize"),
        "hmin", GT.$("fix hydrogens and minimize"),
        "clearQ", GT.$("clear"),
        "SIGNEDsaveFile", GT.$("save file"),
        "SIGNEDsaveState", GT.$("save state"),
        "invertStereoP!CB", GT.$("invert ring stereochemistry"),
        "assignAtom_XP!CB" , GT.$("delete atom"),
        "assignAtom_XxP!CB" , GT.$("drag to bond"),
        "dragAtomP!CB" , GT.$("drag atom"),
        "dragMinimizeP!CB" , GT.$("drag atom (and minimize)"),
        "dragMoleculeP!CB" , GT.$("drag molecule (ALT to rotate)"),
        "dragMinimizeMoleculeP!CB" , GT.$("drag and minimize molecule (docking)"),
        "assignAtom_CP!CB" , "C",
        "assignAtom_HP!CB" , "H",
        "assignAtom_NP!CB" , "N",
        "assignAtom_OP!CB" , "O",
        "assignAtom_FP!CB" , "F",
        "assignAtom_ClP!CB" , "Cl",
        "assignAtom_BrP!CB" , "Br",
        "_??P!CB", "??",
        "assignAtom_PlP!CB", GT.$("increase charge"),
        "assignAtom_MiP!CB", GT.$("decrease charge"),
        "assignBond_0P!CB" , GT.$("delete bond"),
        "assignBond_1P!CB", GT.$("single"),
        "assignBond_2P!CB", GT.$("double"),
        "assignBond_3P!CB", GT.$("triple"),
        "assignBond_pP!CB", GT.$("increase order"),
        "assignBond_mP!CB", GT.$("decrease order"),
        "rotateBondP!CB", GT.$("rotate bond (SHIFT-DRAG)"),
        "exit", GT.$("exit modelkit mode"),
    };
 
    GT.setDoTranslate(wasTranslating);

    return words;
  }

  @Override
  public String getMenuAsText(String title) {
    return getStuctureAsText(title, menuContents, structureContents);
  }

}
