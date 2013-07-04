package org.jmol.api;

import org.jmol.modelset.Atom;
import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.Tensor;
import org.jmol.util.V3;
import org.jmol.viewer.Viewer;

public interface JmolNMRInterface {

  public JmolNMRInterface setViewer(Viewer viewer);

  /**
   * Quadrupolar constant, directly proportional to Vzz and dependent on the
   * quadrupolar moment of the isotope considered
   * 
   * @param efg
   * @return float value
   */
  public float getQuadrupolarConstant(Tensor efg);

  /**
   * If t is null, then a1, a2, and type are used to find the appropriate
   * tensor.
   * 
   * @param a1
   * @param a2
   * @param type
   * @param t
   * @return 0 if not found
   */
  public float getJCouplingHz(Atom a1, Atom a2, String type, Tensor t);

  /**
   * 
   * @param a1
   * @param a2
   * @return desired constant
   */
  public float getDipolarConstantHz(Atom a1, Atom a2);

  /**
   * 
   * @param a1
   * @param a2
   * @param vField
   * @return projected value
   */
  public float getDipolarCouplingHz(Atom a1, Atom a2, V3 vField);

  /**
   * Finds a set of interaction tensors based on a set of atoms -- all within the
   * set if bs.cardinality() > 1; all for this atom when bs.cardinality() == 1.
   * 
   * @param type
   * @param bs
   * @return list of matching tensors
   */
  public JmolList<Tensor> getInteractionTensorList(String type, BS bs);

  /**
   * An attempt to find unique atoms using tensors.
   * 
   * @param bs
   * @return bitset of atoms
   */
  public BS getUniqueTensorSet(BS bs);

}
