package org.jmol.adapter.smarter;

import java.util.Map;

import org.jmol.api.SymmetryInterface;

import javajs.util.Matrix;


/**
 * Modulated Structure Reader Interface
 * 
 */
public interface MSInterface {

  // methods called from org.jmol.adapters.readers.xtal.JanaReader
  
  void addModulation(Map<String, double[]> map, String id, double[] pt, int iModel);

  void addSubsystem(String code, Matrix w);

  void finalizeModulation();

  double[] getMod(String key);

  int initialize(AtomSetCollectionReader r, String data) throws Exception;

  void setModulation(boolean isPost);

  SymmetryInterface getAtomSymmetry(Atom a, SymmetryInterface symmetry);

}
