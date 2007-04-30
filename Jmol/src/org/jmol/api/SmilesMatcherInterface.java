package org.jmol.api;

import java.util.BitSet;
import org.jmol.viewer.Viewer;

public interface SmilesMatcherInterface {

  public abstract void setViewer(Viewer viewer);
  public abstract BitSet getSubstructureSet(String smiles)
      throws Exception;
}
