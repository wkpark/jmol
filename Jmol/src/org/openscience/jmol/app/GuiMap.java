/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;

import java.util.Hashtable;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBox;

class GuiMap {

  Hashtable map = new Hashtable();

  String getLabel(String key) {
    String label =
      JmolResourceHandler.getStringX(key+"Label");
    if (label == null)
      label = key;
    return label;
  }

  JMenu newJMenu(String key) {
    return new KeyJMenu(key, getLabel(key));
  }

  JMenuItem newJMenuItem(String key) {
    return new KeyJMenuItem(key, getLabel(key));
  }
  JCheckBoxMenuItem newJCheckBoxMenuItem(String key, boolean isChecked) {
    return new KeyJCheckBoxMenuItem(key, getLabel(key), isChecked);
  }
  JRadioButtonMenuItem newJRadioButtonMenuItem(String key) {
    return new KeyJRadioButtonMenuItem(key, getLabel(key));
  }
  JCheckBox newJCheckBox(String key, boolean isChecked) {
    return new KeyJCheckBox(key, getLabel(key), isChecked);
  }

  Object get(String key) {
    return map.get(key);
  }

  String getKey(Object obj) {
    return (((GetKey)obj).getKey());
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
    KeyJMenu(String key, String label) {
      super(label);
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJMenuItem extends JMenuItem implements GetKey {
    String key;
    KeyJMenuItem(String key, String label) {
      super(label);
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJCheckBoxMenuItem
    extends JCheckBoxMenuItem implements GetKey {
    String key;
    KeyJCheckBoxMenuItem(String key, String label, boolean isChecked) {
      super(label, isChecked);
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJRadioButtonMenuItem
    extends JRadioButtonMenuItem implements GetKey {
    String key;
    KeyJRadioButtonMenuItem(String key, String label) {
      super(label);
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJCheckBox
    extends JCheckBox implements GetKey {
    String key;
    KeyJCheckBox(String key, String label, boolean isChecked) {
      super(label, isChecked);
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }
}

