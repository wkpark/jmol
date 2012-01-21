package org.jmol.api;

import java.util.BitSet;

import org.jmol.viewer.Viewer;

public interface JmolPeerInterface {

  public abstract void setViewer(Viewer viewer);

  public boolean modelChanged(int modelIndex);

  public boolean fileLoaded(String fileName, String qualifier,
                            String assignmentData, boolean isLocal);

  public boolean atomPicked(int atomIndex);

  public boolean atomsPicked(BitSet bsAtoms);
}
