package org.jmol.adapter.smarter;

import java.util.Map;

import org.jmol.api.SymmetryInterface;

import javajs.util.P3;


/**
 * Modulated Structure Reader Interface
 * 
 */
public interface MSInterface {

  // methods called from org.jmol.adapters.readers.xtal.JanaReader
  
  void addModulation(Map<String, P3> map, String id, P3 pt, int iModel);

  void addSubsystem(String code, int[][] wmatrix);

  void finalizeModulation();

  P3 getMod(String key);

  int initialize(AtomSetCollectionReader r, String data) throws Exception;

  void setModulation(boolean isPost);

  SymmetryInterface getAtomSymmetry(Atom a, SymmetryInterface symmetry);

}
