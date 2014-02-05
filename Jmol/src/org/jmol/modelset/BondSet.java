package org.jmol.modelset;

import javajs.util.BS;
import org.jmol.util.BSUtil;

public class BondSet extends BS {

  public BondSet() {
  }

  private int[] associatedAtoms;
  
  public int[] getAssociatedAtoms() {
    return associatedAtoms;
  }

  public BondSet(BS bs) {
    BSUtil.copy2(bs, this);
  }

  public BondSet(BS bs, int[] atoms) {
    this(bs);
    associatedAtoms = atoms;
  }
}