package org.jmol.adapter.readers.pymol;

import org.jmol.adapter.smarter.Atom;
import org.jmol.util.BS;

class PyMOLAtom extends Atom {
  String label;
  BS bsReps;
  int cartoonType;
  int flags;
  boolean bonded;
  int uniqueID = -1;
}
