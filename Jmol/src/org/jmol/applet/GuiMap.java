/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-08-05 12:32:28 -0500 (Tue, 05 Aug 2008) $
 * $Revision: 9669 $
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
package org.jmol.applet;

import java.util.Hashtable;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.AbstractButton;

import org.jmol.i18n.GT;

class GuiMap {

  Hashtable map = new Hashtable();
  
  private Hashtable labels = null;
  
  private Hashtable setupLabels() {
      Hashtable labels = new Hashtable();
      labels.put("help", GT._("&Help"));
      labels.put("search", GT._("&Search..."));
      labels.put("commands", GT._("&Commands"));
      labels.put("functions", GT._("Math &Functions"));
      labels.put("parameters", GT._("Set &Parameters"));
      labels.put("more", GT._("&More"));
      return labels;
  }

  String getLabel(String key) {
    if (labels == null) {
      labels = setupLabels();
    }
    return (String)labels.get(key);
  }

  JMenu newJMenu(String key) {
    String label = getLabel(key);
    return new KeyJMenu(key, getLabelWithoutMnemonic(label), getMnemonic(label));
  }
  
  JMenuItem newJMenuItem(String key) {
    String label = getLabel(key);
    return new KeyJMenuItem(key, getLabelWithoutMnemonic(label), getMnemonic(label));
  }

  Object get(String key) {
    return map.get(key);
  }

  static String getKey(Object obj) {
    return (((GetKey)obj).getKey());
  }

  static String getLabelWithoutMnemonic(String label) {
    if (label == null) {
      return null;
    }
    int index = label.indexOf('&');
    if (index == -1) {
      return label;
    }
    return label.substring(0, index) +
      ((index < label.length() - 1) ? label.substring(index + 1) : "");
  }
  
  static char getMnemonic(String label) {
    if (label == null) {
      return ' ';
    }
    int index = label.indexOf('&');
    if ((index == -1) || (index == label.length() - 1)){
      return ' ';
    }
    return label.charAt(index + 1);
  }
  
  void setSelected(String key, boolean b) {
    ((AbstractButton)get(key)).setSelected(b);
  }

  boolean isSelected(String key) {
    return ((AbstractButton)get(key)).isSelected();
  }


  interface GetKey {
    public String getKey();
  }

  class KeyJMenu extends JMenu implements GetKey {
    String key;
    KeyJMenu(String key, String label, char mnemonic) {
      super(label);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJMenuItem extends JMenuItem implements GetKey {
    String key;
    KeyJMenuItem(String key, String label, char mnemonic) {
      super(label);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }
}

