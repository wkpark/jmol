package org.jmol.api;

import java.util.BitSet;

import org.jmol.modelset.Atom;



public interface MinimizerInterface {

  public abstract boolean minimize(int steps, double crit, BitSet bsSelected, 
                                   BitSet bsFixed, boolean haveFixed, boolean isSilent) throws Exception;
  public abstract void setProperty(String propertyName, Object propertyValue);
  public abstract Object getProperty(String propertyName, int param);
  public abstract void calculatePartialCharges(Atom[] atoms, BitSet bsAtoms, SmilesMatcherInterface smilesMatcherInterface);
}
