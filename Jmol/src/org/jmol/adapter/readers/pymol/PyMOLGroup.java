package org.jmol.adapter.readers.pymol;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.BS;
import org.jmol.util.JmolList;

class PyMOLGroup {
  String name;
  String objectNameID;
  Map<String, PyMOLGroup> list = new Hashtable<String, PyMOLGroup>();
  JmolList<Object> object;
  boolean visible = true;
  boolean occluded = false;
  BS bsAtoms;
  int type;

  PyMOLGroup parent;
  
  PyMOLGroup(String name) {
    this.name = name;
  }

  void addList(PyMOLGroup child) {
    PyMOLGroup group = list.get(child.name);
    if (group != null)
      return;
    list.put(child.name, child);
    child.parent = this;
  }
  
  void set() {
    if (parent != null)
      return;    
  }
  
  @Override
  public String toString() {
    return this.name;
  }
}
