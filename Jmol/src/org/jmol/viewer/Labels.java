/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.viewer;

import org.jmol.g3d.*;

import java.util.BitSet;

class Labels extends Shape {

  String[] strings;
  short[] colixes;
  short[] bgcolixes;
  byte[] fids;
  short[] offsets;

  Font3D defaultFont3D;

  void initShape() {
    defaultFont3D = g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
                                  JmolConstants.DEFAULT_FONTSTYLE,
                                  JmolConstants.LABEL_DEFAULT_FONTSIZE);
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      String palette = null;
      short colix = Graphics3D.getColix(value);
      if (colix == Graphics3D.UNRECOGNIZED)
        palette = (String)value;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          if (colixes == null || i >= colixes.length) {
            if (colix == 0)
              continue;
            colixes = Util.ensureLength(colixes, i + 1);
          }
          colixes[i] =
            ((colix != Graphics3D.UNRECOGNIZED) ? colix :
             viewer.getColixAtomPalette(frame.getAtomAt(i), palette));
        }
    }
    // no translucency
    if ("bgcolor" == propertyName) {
      short bgcolix = Graphics3D.getColix(value);
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          //Atom atom = atoms[i];
          if (bgcolixes == null || i >= bgcolixes.length) {
            if (bgcolix == 0)
              continue;
            bgcolixes = Util.ensureLength(bgcolixes, i + 1);
          }
          bgcolixes[i] = bgcolix;
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
        fids = null;
        return;
      }
      byte fid = g3d.getFontFid(fontsize);
      fids = Util.ensureLength(fids, frame.atomCount);
      for (int i = frame.atomCount; --i >= 0; )
        fids[i] = fid;
      return;
    }
    
    if ("font" == propertyName) {
      byte fid = ((Font3D)value).fid;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          if (fids == null || i >= fids.length) {
            if (fid == defaultFont3D.fid)
              continue;
            fids = Util.ensureLength(fids, i + 1);
          }
          fids[i] = fid;
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

    if ("pickingLabel" == propertyName) {
      // toggle
      int atomIndex = ((Integer)value).intValue();
      if (strings != null &&
          strings.length > atomIndex &&
          strings[atomIndex] != null) {
        strings[atomIndex] = null;
      } else {
        String strLabel;
        if (viewer.getModelCount() > 1)
          strLabel = "[%n]%r:%c.%a/%M";
        else if (viewer.getChainCount() > 1)
          strLabel = "[%n]%r:%c.%a";
        else if (viewer.getGroupCount() <= 1)
          strLabel = "%e%i";
        else
          strLabel = "[%n]%r.%a";
        Atom atom = atoms[atomIndex];
        strings = Util.ensureLength(strings, atomIndex + 1);
        strings[atomIndex] = atom.formatLabel(strLabel);
      }
      return;
    }
  }
}
