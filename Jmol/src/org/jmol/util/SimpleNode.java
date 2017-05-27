package org.jmol.util;


import javajs.util.P3;

/**
 * Just the bare minimum to accomplish CIP and non-BioSMILES
 */
public interface SimpleNode {

  void setCIPChirality(int rs);

  int getIsotopeNumber();

  String getAtomName();

  // could include hydrogen bonds
  int getBondCount();

  int getCovalentBondCount();

  SimpleEdge[] getEdges();

  int getElementNumber();

  int getFormalCharge();

  int getIndex();

  int getNominalMass();

  int getValence();

  P3 getXYZ();


}
