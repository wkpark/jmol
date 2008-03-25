package org.jmol.api;

import java.util.BitSet;

import org.jmol.viewer.Viewer;

public interface MinimizerInterface {

  public abstract boolean minimize(Viewer viewer, BitSet bsSelected, BitSet bsFix, BitSet bsIgnore)
      throws Exception;
  public abstract void setProperty(String propertyName, Object propertyValue);
}
