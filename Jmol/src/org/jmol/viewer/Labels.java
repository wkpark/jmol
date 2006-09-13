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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;
import org.jmol.util.ArrayUtil;

import java.util.Hashtable;
import java.util.BitSet;

class Labels extends Shape {

  String[] strings;
  short[] colixes;
  short[] bgcolixes;
  byte[] fids;
  int[] offsets;
  Hashtable atomLabels = new Hashtable();
  Text text;
  Font3D defaultFont3D;

  void initShape() {
    defaultFont3D = g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
                                  JmolConstants.DEFAULT_FONTSTYLE,
                                  JmolConstants.LABEL_DEFAULT_FONTSIZE);
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      String palette = null;
      short colix = Graphics3D.getColix(value);
      if (colix == Graphics3D.UNRECOGNIZED)
        palette = (String) value;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          if (colixes == null || i >= colixes.length) {
            if (colix == 0)
              continue;
            colixes = ArrayUtil.ensureLength(colixes, i + 1);
          }
          colixes[i] = ((colix != Graphics3D.UNRECOGNIZED) ? colix : viewer
              .getColixAtomPalette(frame.getAtomAt(i), palette));
          text = (Text) atomLabels.get(atoms[i]);
          if (text != null)
            text.setColix(colixes[i]);
        }
    }
    // no translucency
    if ("bgcolor" == propertyName) {
      short bgcolix = Graphics3D.getColix(value);
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          //Atom atom = atoms[i];
          if (bgcolixes == null || i >= bgcolixes.length) {
            if (bgcolix == 0)
              continue;
            bgcolixes = ArrayUtil.ensureLength(bgcolixes, i + 1);
          }
          bgcolixes[i] = bgcolix;
          text = (Text) atomLabels.get(atoms[i]);
          if (text != null)
            text.setBgColix(bgcolix);
        }
    }

    if ("label" == propertyName) {
      String strLabel = (String) value;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          Atom atom = atoms[i];
          String label = atom.formatLabel(strLabel);
          atom.setShapeVisibility(myVisibilityFlag, label != null);
          if (strings == null || i >= strings.length)
            strings = ArrayUtil.ensureLength(strings, i + 1);
          strings[i] = label;
          text = (Text) atomLabels.get(atoms[i]);
          if (text != null)
            text.setText(label);
        }
      return;
    }

    if ("fontsize" == propertyName) {
      int fontsize = ((Integer) value).intValue();
      if (fontsize == JmolConstants.LABEL_DEFAULT_FONTSIZE) {
        fids = null;
        return;
      }
      byte fid = g3d.getFontFid(fontsize);
      fids = ArrayUtil.ensureLength(fids, frame.atomCount);
      for (int i = frame.atomCount; --i >= 0;) {
        fids[i] = fid;
        text = (Text) atomLabels.get(atoms[i]);
        if (text != null)
          text.setFid(fid);
      }
      return;
    }

    if ("font" == propertyName) {
      byte fid = ((Font3D) value).fid;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          if (fids == null || i >= fids.length) {
            if (fid == defaultFont3D.fid)
              continue;
            fids = ArrayUtil.ensureLength(fids, i + 1);
          }
          fids[i] = fid;
          text = (Text) atomLabels.get(atoms[i]);
          if (text != null)
            text.setFid(fid);
        }
      return;
    }

    if ("offset" == propertyName) {
      int offset = ((Integer) value).intValue();
      // 0 must be the default, because we initialize the array
      // in segments
      if (offset == 0)
        offset = Short.MAX_VALUE;
      else if (offset == ((JmolConstants.LABEL_DEFAULT_X_OFFSET << 8) | JmolConstants.LABEL_DEFAULT_Y_OFFSET))
        offset = 0;
      for (int i = frame.atomCount; --i >= 0;) {
        if (bsSelected.get(i)) {
          if (offsets == null || i >= offsets.length) {
            if (offset == 0)
              continue;
            offsets = ArrayUtil.ensureLength(offsets, i + 1);
          }
          offsets[i] = (offsets[i] & 3) + (offset << 2);
        }
        text = (Text) atomLabels.get(atoms[i]);
        if (text != null)
          text.setOffset(offset);
      }
      return;
    }

    if ("align" == propertyName) {
      String type = (String) value;
      int offset = Text.LEFT;
      if (type.equalsIgnoreCase("right"))
        offset = Text.RIGHT;
      else if (type.equalsIgnoreCase("center"))
        offset = Text.CENTER;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          if (offsets == null || i >= offsets.length) {
            if (offset == Text.LEFT)
              continue;
            offsets = ArrayUtil.ensureLength(offsets, i + 1);
          }
          offsets[i] = (offsets[i] & ~3) + offset;
          text = (Text) atomLabels.get(atoms[i]);
          if (text != null)
            text.setAlignment(offset);
        }
      return;
    }

    if ("pickingLabel" == propertyName) {
      // toggle
      int atomIndex = ((Integer) value).intValue();
      if (strings != null && strings.length > atomIndex
          && strings[atomIndex] != null) {
        strings[atomIndex] = null;
      } else {
        String strLabel = viewer.getStandardLabelFormat();
        Atom atom = atoms[atomIndex];
        strings = ArrayUtil.ensureLength(strings, atomIndex + 1);
        strings[atomIndex] = atom.formatLabel(strLabel);
      }
      return;
    }
  }

  void setModelClickability() {
    if (strings == null)
      return;
    for (int i = strings.length; --i >= 0; ) {
      String label = strings[i];
      if (label != null && frame.atoms.length > i)
        frame.atoms[i].clickabilityFlags |= myVisibilityFlag;
    }
  }
}
