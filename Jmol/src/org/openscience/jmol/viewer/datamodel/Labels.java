/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.jmol.g3d.*;

import java.awt.Color;
import java.util.BitSet;

public class Labels extends Shape {

  String[] strings;
  short[] colixes;
  byte[] fontBids;
  short[] offsets;

  Font3D defaultFont3D;

  void initShape() {
    defaultFont3D = g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
                                  JmolConstants.DEFAULT_FONTSTYLE,
                                  JmolConstants.LABEL_DEFAULT_FONTSIZE);
  }

  public void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      short colix = g3d.getColix(value);
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          Atom atom = atoms[i];
          if (colixes == null || i >= colixes.length) {
            if (colix == 0)
              continue;
            colixes = Util.ensureLength(colixes, i + 1);
          }
          colixes[i] = colix;
        }
    }
    
    if ("label" == propertyName) {
      String strLabel = (String)value;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          Atom atom = atoms[i];
          String label = atom.formatLabel(strLabel);
          if (strings == null || i >= strings.length) {
            if (label == null)
              continue;
            strings = Util.ensureLength(strings, i + 1);
          }
          strings[i] = label;
        }
      return;
    }
    
    if ("fontsize" == propertyName) {
      int fontsize = ((Integer)value).intValue();
      if (fontsize == JmolConstants.LABEL_DEFAULT_FONTSIZE) {
        fontBids = null;
        return;
      }
      byte bid = g3d.getFont3D(fontsize).bid;
      fontBids = Util.ensureLength(fontBids, frame.atomCount);
      for (int i = frame.atomCount; --i >= 0; )
        fontBids[i] = bid;
      return;
    }
    
    if ("font" == propertyName) {
      byte bid = ((Font3D)value).bid;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          if (fontBids == null || i >= fontBids.length) {
            if (bid == defaultFont3D.bid)
              continue;
            fontBids = Util.ensureLength(fontBids, i + 1);
          }
          fontBids[i] = bid;
        }
      return;
    }

    if ("offset" == propertyName) {
      int offset = ((Integer)value).intValue();
      if (offset == 0)
        offset = Short.MIN_VALUE;
      else if (offset == ((JmolConstants.LABEL_DEFAULT_X_OFFSET << 8) |
                          JmolConstants.LABEL_DEFAULT_Y_OFFSET))
        offset = 0;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          if (offsets == null || i >= offsets.length) {
            if (offset == 0)
              continue;
            offsets = Util.ensureLength(offsets, i + 1);
          }
          offsets[i] = (short)offset;
        }
      return;
    }
  }
}
