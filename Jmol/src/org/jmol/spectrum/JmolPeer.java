/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-10-02 18:29:23 -0500 (Sun, 02 Oct 2011) $
 * $Revision: 16204 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.spectrum;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.jmol.api.JmolPeerInterface;
import org.jmol.viewer.Viewer;

/**
 * Abstract class that is implemented in both Jmol and JSpecView
 * These two packages should extend this abstract class to process signals in their own way.
 * 
 **/

public class JmolPeer extends JmolSpectralPeer implements JmolPeerInterface {

  private Viewer viewer;

  /* (non-Javadoc)
   * @see org.jmol.spectrum.JmolPeerInterface#setViewer(org.jmol.viewer.Viewer)
   */
  public void setViewer(Viewer viewer) {
    this.viewer = viewer;
  }

  // JmolSpectralPeer class extensions

  @Override
  public void highlight(List<String> list) {
    // from peer. list is list of assignment strings to highlight
    BitSet bs = new BitSet();
    if (list != null) 
    for (int j = list.size(); --j >= 0;) {
      String key = list.get(j);
      for (int i = specData.size(); --i >= 0;) {
        Map<String, BitSet> map = specData.get(i);
        if (map != null && map.containsKey(key))
          bs.or(map.get(key));
      }
    }
    viewer.select(bs, false, null, false);
    int iatom = bs.nextSetBit(0);
    if (iatom >= 0)
      viewer.setCurrentModelIndex(viewer.getAtomModelIndex(iatom));
  }

  /* (non-Javadoc)
   * @see org.jmol.spectrum.JmolPeerInterface#fileLoaded(java.lang.String, java.lang.String, boolean)
   */
  @Override
  public boolean fileLoaded(String fileName, String qualifier,
                            String assignmentData, boolean isLocal) {
    if (isLocal) {
      assignSpectrum(fileName, assignmentData);
      if (peer != null)
        peer.fileLoaded(fileName, qualifier, assignmentData, false);
    } else {
      // from peer
      if (fileName == null) {
        viewer.script("zap");
      } else {
        viewer.script("load \"" + fileName + "\" filter \"" + qualifier + "\"");
      }
    }
    return true;
  }

  // Jmol-specific methods
  
  public boolean modelChanged(int modelIndex) {
    // TODO
    return false;
  }

  public boolean atomPicked(int atomIndex) {
    BitSet bs = new BitSet();
    bs.set(atomIndex);
    return atomsPicked(bs);
  }

  public boolean atomsPicked(BitSet bsAtoms) {
    BitSet bs = getBitSet(bsAtoms);
    if (bs == null)
      return false;
    if (!bs.equals(bsAtoms) || !bs.equals(viewer.getSelectionSet(false)))
      viewer.select(bs, false, null, false);
    if (peer != null)
      sendPeer(bs);
    return true;
  }

  private void sendPeer(BitSet bsAtoms) {
    List<String> list = new ArrayList<String>();
    for (int i = specData.size(); --i >= 0;) {
      Map<String, BitSet> map = specData.get(i);
      if (map == null)
        continue;
      for (Map.Entry<String, BitSet> entry : map.entrySet())
        if (entry.getValue().intersects(bsAtoms))
          list.add(entry.getKey());
    }
    peer.highlight(list);
  }

  private List<Map<String, BitSet>> specData = new ArrayList<Map<String, BitSet>>();
  
  private BitSet getBitSet(BitSet bsAtoms) {
    BitSet bsAll = new BitSet();
    for (int i = specData.size(); --i >= 0;) {
      Map<String, BitSet> map = specData.get(i);
      if (map == null)
        continue;
      for (Map.Entry<String, BitSet> entry : map.entrySet()) {
        BitSet bs = entry.getValue();
        if (bs.intersects(bsAtoms))
          bsAll.or(bs);
      }
    }
    BitSet bs = new BitSet();
    bs.or(bsAll);
    bs.or(bsAtoms);
    return (bs.equals(bsAll) ? bsAll : null);
  }

  /**
   * parse data from JCAMP file or CML with structure/assignment blocks
   * 
   * @param fileName 
   * @param data  to be defined...   
   * @return true if successful
   */
  private boolean assignSpectrum(String fileName, String data) {
    // TODO
    // create a data set encompassing structure/spectra correlations. 
    // this data should only be the assignment section from the JCAMP file.
    
    return true;   
  }

  
}
