package org.jmol.api;

import java.util.BitSet;

import org.jmol.viewer.Viewer;

public interface MinimizerInterface {

  public abstract boolean minimize(Viewer viewer, int steps, double crit, 
                                   BitSet bsSelected, BitSet bsFix)
      throws Exception;
  public abstract void setProperty(String propertyName, Object propertyValue);
  public abstract Object getProperty(String propertyName, int param);
}
