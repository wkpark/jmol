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

class BondPopupResourceBundle extends PopupResource {

  BondPopupResourceBundle() {
    super(null, null);
  }

  protected void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    setStructure(menuStructure);
  }
    
  private static String[][] menuContents = {
    { "bondMenu", "delete 1 2 3 + -" }
  };
  
  private static String[][] structureContents = {
    { "delete" , "set picking assignBond_0" },
    { "1", "set picking assignBond_1" },
    { "2", "set picking assignBond_2" },
    { "3", "set picking assignBond_3" },
    { "+", "set picking assignBond_+" },
    { "-", "set picking assignBond_-" },
  };
  
  protected String[] getWordContents() {
    
    boolean wasTranslating = GT.getDoTranslate();
    if (!wasTranslating)
      GT.setDoTranslate(true);
    String[] words = new String[] {
        "delete" , GT._("delete"),
        "1", GT._("single"),
        "2", GT._("double"),
        "3", GT._("triple"),
        "+", GT._("increase"),
        "-", GT._("decrease"),
    };
 
    if (!wasTranslating)
      GT.setDoTranslate(wasTranslating);

    return words;
  }

}
