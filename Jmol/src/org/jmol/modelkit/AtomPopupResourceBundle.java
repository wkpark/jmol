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

class AtomPopupResourceBundle extends PopupResource {

  AtomPopupResourceBundle() {
    super(null, null);
  }

  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    setStructure(menuStructure);
  }
    
  private static String[][] menuContents = {
    { "atomMenu" , "delete dragAtom dragMinimize sproutC sproutH sproutN sproutO + -" }
  };
  
  private static String[][] structureContents = {
    { "delete" ,  "set picking assignAtom_X" },
    { "dragAtom", "set picking dragAtom" },
    { "dragMinimize", "set picking dragMinimize" },
    { "sproutC", "set picking assignAtom_C" },
    { "sproutH", "set picking assignAtom_H" },
    { "sproutN", "set picking assignAtom_N" },
    { "sproutO", "set picking assignAtom_O" },
    { "+", "set picking assignAtom_+" },
    { "-", "set picking assignAtom_-" },
  };
  
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.getDoTranslate();
    if (!wasTranslating)
      GT.setDoTranslate(true);
    String[] words = new String[] {
        "delete" , GT._("delete atom"),
        "dragAtom" , GT._("drag atom"),
        "dragMinimize" , GT._("drag-minimize"),
        "sproutC" , "C",
        "sproutH" , "H",
        "sproutN" , "N",
        "sproutO" , "O",
        "+", GT._("increase charge"),
        "-", GT._("decrease charge"),
    };
 
    if (!wasTranslating)
      GT.setDoTranslate(wasTranslating);

    return words;
  }

}
