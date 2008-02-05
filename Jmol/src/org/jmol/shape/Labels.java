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

package org.jmol.shape;

import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Escape;
import org.jmol.viewer.JmolConstants;

import java.util.Hashtable;
import java.util.BitSet;

public class Labels extends AtomShape {

  String[] strings;
  String[] formats;
  short[] bgcolixes;
  byte[] fids;
  int[] offsets;

  Hashtable atomLabels = new Hashtable();
  Text text;

  BitSet bsFontSet, bsBgColixSet;

  int defaultOffset;
  int defaultAlignment;
  int defaultZPos;
  byte defaultFontId;
  short defaultColix;
  short defaultBgcolix;
  byte defaultPaletteID;
  int defaultPointer;
  byte zeroFontId;
  int zeroOffset;

  boolean defaultsOnlyForNone = true;

  //labels

  public void initShape() {
    super.initShape();
    defaultFontId = zeroFontId = g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
        JmolConstants.DEFAULT_FONTSTYLE, JmolConstants.LABEL_DEFAULT_FONTSIZE).fid;
    defaultColix = 0; //"none" -- inherit from atom
    defaultBgcolix = 0; //"none" -- off
    defaultOffset = zeroOffset = (JmolConstants.LABEL_DEFAULT_X_OFFSET << 8)
        | JmolConstants.LABEL_DEFAULT_Y_OFFSET;
    defaultZPos = 0;
    translucentAllowed = false;
  }

  public void setProperty(String propertyName, Object value, BitSet bsSelected) {
    isActive = true;
    if ("color" == propertyName) {
      isActive = true;
      int n = 0;
      byte pid = JmolConstants.pidOf(value);
      short colix = Graphics3D.getColix(value);
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setColix(i, colix, pid, n++);
      if (n == 0 || !defaultsOnlyForNone) {
        defaultColix = colix;
        defaultPaletteID = pid;
      }
      return;
    }

    if ("scalereference" == propertyName) {
      if (strings == null)
        return;
      float val = ((Float) value).floatValue();
      float scalePixelsPerMicron = (val == 0 ? 0 : 10000f / val);
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          if (strings.length <= i)
            continue;
          text = getLabel(i);
          if (text == null) {
            text = new Text(g3d, null, strings[i], 
                (short) 0, (short)0, 0, 0, 0, 0, 0, scalePixelsPerMicron);
            putLabel(i, text);
          }
          text.scalePixelsPerMicron = scalePixelsPerMicron;
        }
      return;
    }
    
    if ("label" == propertyName) {
      isActive = true;
      if (bsSizeSet == null)
        bsSizeSet = new BitSet();
      String strLabel = (String) value;
      boolean isScaled = viewer.getFontScaling();
      float scalePixelsPerMicron = (isScaled ? viewer.getScalePixelsPerAngstrom() * 10000 : 0);
      for (int i = atomCount; --i >= 0;)
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
          text = getLabel(i);
          if (text == null && isScaled) {
            text = new Text(g3d, null, label, 
                (short) 0, (short)0, 0, 0, 0, 0, 0, scalePixelsPerMicron);
            putLabel(i, text);
          }
          else if (text != null)
            text.setText(label);
          if (defaultOffset != zeroOffset)
            setOffsets(i, defaultOffset);
          if (defaultAlignment != Text.ALIGN_LEFT)
            setAlignment(i, defaultAlignment, -1);
          if ((defaultZPos & FRONT_FLAG) != 0)
            setFront(i, true, -1);
          else if ((defaultZPos & GROUP_FLAG) != 0)
            setGroup(i, true, -1);
          if (defaultPointer != Text.POINTER_NONE)
            setPointer(i, defaultPointer, -1);
          if (defaultColix != 0 || defaultPaletteID != 0)
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
      isActive = true;
      if (bsBgColixSet == null)
        bsBgColixSet = new BitSet();
      short bgcolix = Graphics3D.getColix(value);
      int n = 0;
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setBgcolix(i, bgcolix, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultBgcolix = bgcolix;
      return;
    }

    // the rest require bsFontSet setting

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
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setFont(i, fid, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("font" == propertyName) {
      byte fid = ((Font3D) value).fid;
      int n = 0;
      for (int i = atomCount; --i >= 0;)
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
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          setOffsets(i, offset);
          n++;
        }
      if (n == 0 || !defaultsOnlyForNone)
        defaultOffset = offset;
      return;
    }

    if ("align" == propertyName) {
      String type = (String) value;
      int alignment = Text.ALIGN_LEFT;
      if (type.equalsIgnoreCase("right"))
        alignment = Text.ALIGN_RIGHT;
      else if (type.equalsIgnoreCase("center"))
        alignment = Text.ALIGN_CENTER;
      int n = 0;
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setAlignment(i, alignment, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultAlignment = alignment;
      return;
    }

    if ("pointer" == propertyName) {
      int pointer = ((Integer) value).intValue();
      int n = 0;
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setPointer(i, pointer, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultPointer = pointer;
      return;
    }

    if ("front" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      int n = 0;
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setFront(i, TF, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultZPos = (TF ? FRONT_FLAG : 0);
      return;
    }

    if ("group" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      int n = 0;
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setGroup(i, TF, n++);
      if (n == 0 || !defaultsOnlyForNone)
        defaultZPos = (TF ? GROUP_FLAG : 0);
      return;
    }

    if ("toggleLabel" == propertyName) {
      // toggle
      for (int atomIndex = atomCount; --atomIndex >= 0;) {
        Atom atom = atoms[atomIndex];
        if (bsSelected.get(atomIndex)) {
          if (formats == null || atomIndex >= formats.length)
            formats = ArrayUtil.ensureLength(formats, atomIndex + 1);
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
        } else if (strings != null && atomIndex < strings.length) {
          strings[atomIndex] = null;          
        }
        atom.setShapeVisibility(myVisibilityFlag, strings != null && atomIndex < strings.length && strings[atomIndex] != null);
      }
      return;
    }
  }

  public Object getProperty(String property, int index) {
    if (property.equals("defaultState"))
      return getDefaultState();
    return null;
  }

  void putLabel(int i, Text text) {
    if (text == null)
      atomLabels.remove(atoms[i]);
    else
      atomLabels.put(atoms[i], text);
  }

  Text getLabel(int i) {
    return (Text) atomLabels.get(atoms[i]);
  }

  void setColix(int i, short colix, byte pid, int n) {
    setColixAndPalette(colix, pid, i);
    text = getLabel(i);
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
    text = getLabel(i);
    if (text != null)
      text.setBgColix(bgcolix);
  }

  final static int POINTER_FLAGS = 0x03;
  final static int ALIGN_FLAGS   = 0x0C;
  final static int ZPOS_FLAGS    = 0x30;
  final static int GROUP_FLAG    = 0x10;
  final static int FRONT_FLAG    = 0x20;
  final static int SCALE_FLAG    = 0x40;
  //final static int TEXT_FLAG     = 0x80;
  final static int FLAGS         = 0xFF;

  private void setOffsets(int i, int offset) {
    //entry is just xxxxxxxxyyyyyyyy
    //  3         2         1        
    // 10987654321098765432109876543210
    //         xxxxxxxxyyyyyyyytsfgaabp
    //          x-align y-align||||| ||_pointer on
    //                         ||||| |_background pointer color
    //                         |||||_text alignment 0xC 
    //                         ||||_labels group 0x10
    //                         |||_labels front  0x20
    //                         ||_scaled
    //                         |_text (not simple)
    if (offsets == null || i >= offsets.length) {
      if (offset == 0)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & FLAGS) + (offset << 8);
    text = getLabel(i);
    if (text != null)
      text.setOffset(offset);
  }

  void setAlignment(int i, int alignment, int n) {
    if (offsets == null || i >= offsets.length) {
      if (alignment == Text.ALIGN_LEFT)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & ~ALIGN_FLAGS) | (alignment << 2);
    text = getLabel(i);
    if (text != null)
      text.setAlignment(alignment);
  }

  static int getAlignment(int offsetFull) {
    return (offsetFull & ALIGN_FLAGS) >> 2;
  }
  
  void setPointer(int i, int pointer, int n) {
    if (offsets == null || i >= offsets.length) {
      if (pointer == Text.POINTER_NONE)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & ~POINTER_FLAGS) + pointer;
    text = getLabel(i);
    if (text != null)
      text.setPointer(pointer);
  }

  void setFront(int i, boolean TF, int n) {
    if (offsets == null || i >= offsets.length) {
      if (!TF)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & ~ZPOS_FLAGS) + (TF ? FRONT_FLAG : 0);
  }

  void setGroup(int i, boolean TF, int n) {
    if (offsets == null || i >= offsets.length) {
      if (!TF)
        return;
      offsets = ArrayUtil.ensureLength(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & ~ZPOS_FLAGS) + (TF ? GROUP_FLAG : 0);
  }

  void setFont(int i, byte fid, int n) {
    if (fids == null || i >= fids.length) {
      if (fid == zeroFontId)
        return;
      fids = ArrayUtil.ensureLength(fids, i + 1);
    }
    fids[i] = fid;
    bsFontSet.set(i);
    text = getLabel(i);
    if (text != null)
      text.setFid(fid);
  }

  public void setModelClickability() {
    if (strings == null)
      return;
    for (int i = strings.length; --i >= 0;) {
      String label = strings[i];
      if (label != null && modelSet.atoms.length > i
          && !modelSet.isAtomHidden(i))
        modelSet.atoms[i].setClickable(myVisibilityFlag);
    }
  }

  private String getDefaultState() {
    StringBuffer s = new StringBuffer("\n# label defaults;\n");
    appendCmd(s, "select none");
    appendCmd(s, getColorCommand("label", defaultPaletteID, defaultColix));
    appendCmd(s, "background label " + encodeColor(defaultBgcolix));
    appendCmd(s, "set labelOffset " + Text.getXOffset(defaultOffset) + " "
        + (-Text.getYOffset(defaultOffset)));
    String align = Text.getAlignment(defaultAlignment);
    appendCmd(s, "set labelAlignment " + (align.length() < 5 ? "left" : align));
    String pointer = Text.getPointer(defaultPointer);
    appendCmd(s, "set labelPointer " + (pointer.length() == 0 ? "off" : pointer));
    if ((defaultZPos & FRONT_FLAG) != 0)
      appendCmd(s, "set labelFront");
    else if ((defaultZPos & GROUP_FLAG) != 0)
      appendCmd(s, "set labelGroup");
    appendCmd(s, getFontCommand("label", Font3D.getFont3D(defaultFontId)));
    return s.toString();
  }

  public String getShapeState() {
    if (!isActive)
      return "";
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    Hashtable temp3 = new Hashtable();
    for (int i = atomCount; --i >= 0;) {
      if (bsSizeSet == null || !bsSizeSet.get(i))
        continue;
      setStateInfo(temp, i, "label " + Escape.escape(formats[i]));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, getColorCommand("label", paletteIDs[i],
            colixes[i]));
      if (bsBgColixSet != null && bsBgColixSet.get(i))
        setStateInfo(temp2, i, "background label " + encodeColor(bgcolixes[i]));
      Text text = getLabel(i);
      float sppm = (text != null ? text.getScalePixelsPerMicron() : 0);
      if (sppm > 0)
        setStateInfo(temp2, i, "set labelScaleReference " + (10000f / sppm));
      if (offsets != null && offsets.length > i) {
        int offsetFull = offsets[i];
        setStateInfo(temp2, i, "set labelOffset " + Text.getXOffset(offsetFull >> 8)
              + " " + (-Text.getYOffset(offsetFull >> 8)));
        String align = Text.getAlignment(offsetFull >> 2);
        String pointer = Text.getPointer(offsetFull);
        if (pointer.length() > 0)
          setStateInfo(temp2, i, "set labelPointer " + pointer);
        if ((offsetFull & FRONT_FLAG) != 0)
          setStateInfo(temp2, i, "set labelFront");
        else if ((offsetFull & GROUP_FLAG) != 0)
          setStateInfo(temp2, i, "set labelGroup");
        //labelAlignment must come last, so we put it in a separate hash table
        if (align.length() > 0)
          setStateInfo(temp3, i, "set labelAlignment " + align);
      }
      if (bsFontSet != null && bsFontSet.get(i))
        setStateInfo(temp2, i, getFontCommand("label", Font3D.getFont3D(fids[i])));
    }
    return getShapeCommands(temp, temp2, atomCount)
        + getShapeCommands(null, temp3, atomCount);
  }

}
