package org.jmol.api;


import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.BitSet;



public interface MinimizerInterface {

  public abstract boolean minimize(int steps, double crit, BitSet bsSelected, 
                                   BitSet bsFixed, boolean haveFixed, 
                                   boolean isSilent, String ff) throws Exception;
  public abstract void setProperty(String propertyName, Object propertyValue);
  public abstract Object getProperty(String propertyName, int param);
  public abstract void calculatePartialCharges(Bond[] bonds, int bondCount, Atom[] atoms, BitSet bsAtoms);
  public abstract boolean startMinimization();
  public abstract boolean stepMinimization();
  public abstract void endMinimization();
  public abstract boolean minimizationOn();
}
