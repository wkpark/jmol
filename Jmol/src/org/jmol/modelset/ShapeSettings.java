/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

package org.jmol.modelset;

import java.util.List;

import org.jmol.atomdata.RadiusData;
import org.jmol.util.BitSet;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.ShapeManager;

/**
 * a class to store rendering information prior to finishing file loading,
 * specifically designed for reading PyMOL PSE files.
 * 
 * More direct than a script.
 * 
 */
public class ShapeSettings {

  private int shapeID;
  private BitSet bsAtoms;
  private Object info;
  
  private int size = -1;
  
  private short[] colixes;
  private Object[] colors;

  public int argb;
  public float translucency;
  public RadiusData rd;

  /**
   * 
   * @param shapeID
   * @param bsAtoms
   * @param info     optional additional information for the shape
   */
  public ShapeSettings(int shapeID, BitSet bsAtoms, Object info) {
    this.shapeID = shapeID;
    this.bsAtoms = bsAtoms;
    this.info = info;
  }
  
  /**
   * offset is carried out in ModelLoader when the "script" is processed to move
   * the bits to skip the base atom index.
   * 
   * @param offset
   */
  public void offset(int offset) {
    if (offset <= 0)
      return;
    if (bsAtoms != null)
      bsAtoms = BitSetUtil.offset(bsAtoms, 0, offset);
    if (colixes != null) {
      short[] c = new short[colixes.length + offset];
      System.arraycopy(colixes, 0, c, offset, colixes.length);
      colixes = c;
    }
  }

  public void createShape(ModelSet m) {
    ShapeManager sm = m.shapeManager;
    int modelIndex = getModelIndex(m);
    sm.loadShape(shapeID);
    switch (shapeID) {
    case -1:
      sm.viewer.displayAtoms(bsAtoms, false, false, Boolean.TRUE, true);
      return;
    case JmolConstants.SHAPE_ISOSURFACE:
      if (modelIndex < 0)
        return;
      sm.setShapePropertyBs(JmolConstants.SHAPE_BALLS, "colors", colors, bsAtoms);      
      String s = info.toString().replace('\'', '_').replace('"', '_');
      s = "script('isosurface ID \"" + s + "\"  model " + m.models[modelIndex].getModelNumberDotted() + " select " + Escape.escape(bsAtoms) 
          + " solvent " + (size/1000f) + " map property color')";
      if (translucency > 0)
        s += " translucent " + translucency;
      System.out.println("shapeSettings: " + s);
      sm.viewer.evaluateExpression(s);
      return;
    case JmolConstants.SHAPE_LABELS:
      sm.setShapePropertyBs(JmolConstants.SHAPE_LABELS, "labels", info, bsAtoms);
      return;
    case JmolConstants.SHAPE_MEASURES:
      if (modelIndex < 0)
        return;
      MeasurementData md = (MeasurementData) info;
      md.setModelSet(m);
      List<Object> points = md.points;
      for (int i = points.size(); --i >= 0;)
        ((Point3fi) points.get(i)).modelIndex = (short) modelIndex;
      sm.setShapePropertyBs(JmolConstants.SHAPE_MEASURES, "measure", md, bsAtoms);
      if (size != -1)
        sm.setShapeSizeBs(shapeID, size, null, null);
      return;
    default:
      if (size != -1 || rd != null)
        sm.setShapeSizeBs(shapeID, size, rd, bsAtoms);
      if (argb != 0)
        sm.setShapePropertyBs(shapeID, "color", Integer.valueOf(argb), bsAtoms);
      else if (colors != null)
        sm.setShapePropertyBs(shapeID, "colors", colors, bsAtoms);
    }
  }

  private int getModelIndex(ModelSet m) {
    if (bsAtoms == null)
      return -1;
    int iAtom = bsAtoms.nextSetBit(0);
    return (iAtom < 0 ? -1 : m.atoms[iAtom].modelIndex);
  }

  public void setColors(short[] colixes, float translucency) {
    this.colixes = colixes;
    this.colors = new Object[] {colixes, Float.valueOf(translucency) };
  }
  
  public void setSize(float size) {
    this.size = (int) (size * 1000);
  }
    
}
