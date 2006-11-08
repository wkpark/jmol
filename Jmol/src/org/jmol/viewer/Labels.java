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

  Atom[] atoms;
  String[] strings;
  String[] formats;
  short[] colixes;
  short[] bgcolixes;
  byte[] fids;
  int[] offsets;
  Hashtable atomLabels = new Hashtable();
  Text text;
  
  BitSet bsFontSet, bsBgColixSet;

  int defaultOffset;
  byte defaultFontId;
  short defaultColix;
  short defaultBgcolix;
  int defaultAlignment;
  int defaultPaletteID;

  byte zeroFontId;
  int zeroOffset;

  boolean defaultsOnlyForNone = true;
  

  void initShape() {
    defaultFontId = zeroFontId = g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
                                  JmolConstants.DEFAULT_FONTSTYLE,
                                  JmolConstants.LABEL_DEFAULT_FONTSIZE).fid;
    defaultColix = 0; //"none" -- inherit from atom
    defaultBgcolix = 0; //"none" -- off
    defaultOffset = zeroOffset = (JmolConstants.LABEL_DEFAULT_X_OFFSET << 8)
         | JmolConstants.LABEL_DEFAULT_Y_OFFSET;
    atoms = frame.atoms;
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {
    if ("color" == propertyName) {
      int n = 0;
      int pid = (value instanceof Byte ? ((Byte) value).intValue() : -1);
      short colix = Graphics3D.getColix(value);
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setColix(i, colix, pid, n++);
      if (n == 0 || !defaultsOnlyForNone) {
        defaultColix = colix;
        defaultPaletteID = pid;
      }
      return;
    }
    
    if ("label" == propertyName) {
      if (bsSizeSet == null)
        bsSizeSet = new BitSet();
      String strLabel = (String) value;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          Atom atom = atoms[i];
          String label = atom.formatLabel(strLabel);
          atom.setShapeVisibility(myVisibilityFlag, label != null);
          if (strings == null || i >= strings.length)
            strings = ArrayUtil.ensureLength(strings, i + 1);
          if (formats == null || i >= formats.length)
            formats = ArrayUtil.ensureLength(formats, i + 1);
          strings[i] = label;
          formats[i] = strLabel;
          bsSizeSet.set(i, (strLabel != null));
          text = (Text) atomLabels.get(atoms[i]);
          if (text != null)
            text.setText(label);
          if (defaultOffset != zeroOffset)
            setOffsets(i, defaultOffset, -1);
          if (defaultColix != 0)
            setColix(i, defaultColix, defaultPaletteID, -1);
          if (defaultBgcolix != 0)
            setBgcolix(i, defaultBgcolix, -1);
          if (defaultFontId != zeroFontId)
            setFont(i, defaultFontId, -1);
        }
      return;
    }

    // no translucency
    if ("bgcolor" == propertyName) {
      if (bsBgColixSet == null)
        bsBgColixSet = new BitSet();

      short bgcolix = Graphics3D.getColix(value);
      int n = 0;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setBgcolix(i, bgcolix, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultBgcolix = bgcolix;
      return;
    }

    if (bsFontSet == null)
      bsFontSet = new BitSet();

    if ("fontsize" == propertyName) {
      int fontsize = ((Integer) value).intValue();
      if (fontsize < 0) {
        fids = null;
        return;
      }
      byte fid = g3d.getFontFid(fontsize);
      int n = 0;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setFont(i, fid, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("font" == propertyName) {
      byte fid = ((Font3D) value).fid;
      int n = 0;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setFont(i, fid, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("offset" == propertyName) {
      int offset = ((Integer) value).intValue();
      // 0 must be the default, because we initialize the array
      // in segments and so there will be extra 0s.
      // but this "0" only means that "zero" offset; you 
      // can change the default to anything you want.
      if (offset == 0)
        offset = Short.MAX_VALUE;
      else if (offset == zeroOffset)
        offset = 0;
      int n = 0;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setOffsets(i, offset, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultOffset = offset;
      return;
    }

    if ("align" == propertyName) {
      String type = (String) value;
      int alignment = Text.LEFT;
      if (type.equalsIgnoreCase("right"))
        alignment = Text.RIGHT;
      else if (type.equalsIgnoreCase("center"))
        alignment = Text.CENTER;
      int n = 0;
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setAlignment(i, alignment, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultAlignment = alignment;
      return;
    }

    if ("toggleLabel" == propertyName) {
      // toggle
      for (int atomIndex = frame.atomCount; --atomIndex >= 0;)
        if (bsSelected.get(atomIndex)) {
          Atom atom = atoms[atomIndex];
          if (strings != null && strings.length > atomIndex
              && strings[atomIndex] != null) {
            strings[atomIndex] = null;
            formats[atomIndex] = null;
            bsSizeSet.clear(atomIndex);
          } else {
            String strLabel = viewer.getStandardLabelFormat();
            strings = ArrayUtil.ensureLength(strings, atomIndex + 1);
            strings[atomIndex] = atom.formatLabel(strLabel);
            formats[atomIndex] = strLabel;
            bsSizeSet.set(atomIndex);
          }
          atom.setShapeVisibility(myVisibilityFlag, strings[atomIndex] != null);
          return;
        }
      return;
    }
  }

  void setColix(int i, short colix, int pid, int n) {
    if (colixes == null || i >= colixes.length) {
      if (colix == 0)
        return;
      colixes = ArrayUtil.ensureLength(colixes, i + 1);
    }
    if (bsColixSet == null)
      bsColixSet = new BitSet();
    colixes[i] = ((colix != Graphics3D.UNRECOGNIZED) ? colix : viewer
        .getColixAtomPalette(frame.getAtomAt(i), pid));
    bsColixSet.set(i, colixes[i] != 0);
    text = (Text) atomLabels.get(atoms[i]);
    if (text != null)
      text.setColix(colixes[i]);
  }
  
  void setBgcolix(int i, short bgcolix, int n) {
    if (bgcolixes == null || i >= bgcolixes.length) {
      if (bgcolix == 0)
        return;
      bgcolixes = ArrayUtil.ensureLength(bgcolixes, i + 1);
    }
    bgcolixes[i] = bgcolix;
    bsBgColixSet.set(i, bgcolix != 0);
    text = (Text) atomLabels.get(atoms[i]);
    if (text != null)
      text.setBgColix(bgcolix);
  }
  
  void setOffsets(int i, int offset, int n) {
    if (offsets == null || i >= offsets.length) {
      if (offset == 0)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & 3) + (offset << 2);
    text = (Text) atomLabels.get(atoms[i]);
    if (text != null)
      text.setOffset(offset);
  }

  void setAlignment(int i, int alignment, int n) {
    if (offsets == null || i >= offsets.length) {
      if (alignment == Text.LEFT)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & ~3) + alignment;
    text = (Text) atomLabels.get(atoms[i]);
    if (text != null)
      text.setAlignment(alignment);
  }
  
  void setFont(int i, byte fid, int n) {
    if (fids == null || i >= fids.length) {
      if (fid == defaultFontId)
        return;
      fids = ArrayUtil.ensureLength(fids, i + 1);
    }
    fids[i] = fid;
    bsFontSet.set(i);
    text = (Text) atomLabels.get(atoms[i]);
    if (text != null)
      text.setFid(fid);  
  }
  
  void setModelClickability() {
    if (strings == null)
      return;
    for (int i = strings.length; --i >= 0;) {
      String label = strings[i];
      if (label != null && frame.atoms.length > i && !frame.bsHidden.get(i))
        frame.atoms[i].clickabilityFlags |= myVisibilityFlag;
    }
  }
  
  String getShapeState() {
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    for (int i = frame.atomCount; --i >= 0;) {
      if (bsSizeSet == null || !bsSizeSet.get(i))
        continue;
      setStateInfo(temp, i, "label " + formats[i]);
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, "color label [x"
            + viewer.getHexColorFromIndex(colixes[i]) + "]");
      if (bsBgColixSet != null && bsBgColixSet.get(i))
        setStateInfo(temp2, i, "background label [x"
            + viewer.getHexColorFromIndex(bgcolixes[i]) + "]");
      if (offsets != null && offsets.length > i) {
        setStateInfo(temp2, i, "set labelOffset "
            + Text.getXOffset(offsets[i] >> 2) + " "
            + (-Text.getYOffset(offsets[i] >> 2)));
        setStateInfo(temp2, i, "set labelAlignment "
            + Text.getAlignment(offsets[i]));
      }
      if (bsFontSet != null && bsFontSet.get(i)) {
        Font3D font = Font3D.getFont3D(fids[i]);
        setStateInfo(temp2, i, "font label " + font.fontSize + " "
            + font.fontFace + " " + font.fontStyle);
      }
    }
    return getShapeCommands(temp, temp2);
  }  
}
