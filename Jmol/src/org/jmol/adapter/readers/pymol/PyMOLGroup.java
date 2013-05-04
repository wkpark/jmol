package org.jmol.adapter.readers.pymol;

import org.jmol.util.BS;
import org.jmol.util.JmolList;

class PyMOLGroup {
  String name;
  String branchNameID;
  JmolList<PyMOLGroup> list = new JmolList<PyMOLGroup>();
  JmolList<Object> branch;
  boolean visible = true;
  boolean occluded = false;
  BS bsAtoms;
  int type;

  PyMOLGroup parent;
  
  PyMOLGroup(String name) {
    this.name = name;
  }

  void addList(PyMOLGroup child) {
    list.addLast(child);
    child.parent = this;
  }
  
  void set() {
    if (parent != null)
      return;
    
  }
}
