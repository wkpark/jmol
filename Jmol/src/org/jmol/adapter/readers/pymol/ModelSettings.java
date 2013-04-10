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

package org.jmol.adapter.readers.pymol;


import java.util.Map;

import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.Atom;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.JmolList;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;

/**
 * a class to store rendering information prior to finishing file loading,
 * specifically designed for reading PyMOL PSE files.
 * 
 * More direct than a script.
 * 
 */
public class ModelSettings {

  static final int cPuttyTransformNormalizedNonlinear = 0;
  static final int cPuttyTransformRelativeNonlinear   = 1;
  static final int cPuttyTransformScaledNonlinear     = 2;
  static final int cPuttyTransformAbsoluteNonlinear   = 3;
  static final int cPuttyTransformNormalizedLinear    = 4;
  static final int cPuttyTransformRelativeLinear      = 5;
  static final int cPuttyTransformScaledLinear        = 6;
  static final int cPuttyTransformAbsoluteLinear      = 7;
  static final int cPuttyTransformImpliedRMS          = 8;

  
  private int id;
  private BS bsAtoms;
  private Object info;
  
  private int size = -1;
  
  private short[] colixes;
  private Object[] colors;

  public int argb;
  public float translucency = 0;
  public RadiusData rd;

  /**
   * 
   * @param id   A Token or JmolConstants.SHAPE_XXXX
   * @param bsAtoms
   * @param info     optional additional information for the shape
   */
  public ModelSettings(int id, BS bsAtoms, Object info) {
    this.id = id;
    this.bsAtoms = bsAtoms;
    this.info = info;
  }
  
  /**
   * offset is carried out in ModelLoader when the "script" is processed to move
   * the bits to skip the base atom index.
   * 
   * @param modelOffset
   * @param atomOffset 
   */
  @SuppressWarnings("unchecked")
  public void offset(int modelOffset, int atomOffset) {
    if (atomOffset <= 0)
      return;
    if (id == T.movie) {
      Map<String, Object> movie = (Map<String, Object>) info;
      movie.put("baseModel", Integer.valueOf(modelOffset));
      JmolList<BS> aStates = (JmolList<BS>)movie.get("states");
      for (int i = aStates.size(); --i >= 0;)
        BSUtil.offset(aStates.get(i), 0, atomOffset);
      return;
    }
    if (id == T.define) {
      JmolList<BS> defs = (JmolList<BS>)info;
      for (int i = defs.size(); --i >= 0;)
        BSUtil.offset(defs.get(i), 0, atomOffset);
      return;
    }
    if (bsAtoms != null)
      BSUtil.offset(bsAtoms, 0, atomOffset);
    if (colixes != null) {
      short[] c = new short[colixes.length + atomOffset];
      System.arraycopy(colixes, 0, c, atomOffset, colixes.length);
      colixes = c;
    }
  }

  @SuppressWarnings("unchecked")
  public void createShape(ModelSet m) {
    ShapeManager sm = m.shapeManager;
    int modelIndex = getModelIndex(m);
    switch (id) {
    case T.movie:
      sm.viewer.setMovie((Map<String, Object>) info);
      return;
    case T.frame:
      int frame = ((Integer) info).intValue();
      if (frame > 0)
        sm.viewer.setCurrentModelIndex(frame + modelIndex - 1);
      else {
        sm.viewer.setAnimationRange(-1, -1);
        sm.viewer.setCurrentModelIndex(-1);
      }
      return;
    case T.hidden:
      sm.viewer.displayAtoms(bsAtoms, false, false, Boolean.TRUE, true);
      return;
    case T.define:
      sm.viewer.defineAtomSets((Map<String, Object>) info);
      return;
    case T.bonds:
    case JC.SHAPE_BALLS:
      break;
    case JC.SHAPE_ISOSURFACE:
      if (modelIndex < 0)
        return;
      sm.setShapePropertyBs(JC.SHAPE_BALLS, "colors", colors, bsAtoms);
      String s = ((String[]) info)[0].toString().replace('\'', '_').replace(
          '"', '_');
      String lighting = ((String[]) info)[1];
      s = "script('isosurface ID \"" + s + "\"  model "
          + m.models[modelIndex].getModelNumberDotted() + " select ("
          + Escape.eBS(bsAtoms) + " and not solvent) only solvent "
          + (size / 1000f) + " map property color";
      if (translucency > 0)
        s += " translucent " + translucency;
      s += " " + lighting;
      s += "')";
      System.out.println("shapeSettings: " + s);
      sm.viewer.evaluateExpression(s);
      return;
    case JC.SHAPE_LABELS:
      sm.loadShape(id);
      sm.setShapePropertyBs(id, "labels", info, bsAtoms);
      return;
    case JC.SHAPE_MEASURES:
      if (modelIndex < 0)
        return;
      sm.loadShape(id);
      MeasurementData md = (MeasurementData) info;
      md.setModelSet(m);
      JmolList<Object> points = md.points;
      for (int i = points.size(); --i >= 0;)
        ((Point3fi) points.get(i)).modelIndex = (short) modelIndex;
      sm.setShapePropertyBs(id, "measure", md, bsAtoms);
      if (size != -1)
        sm.setShapeSizeBs(id, size, null, null);
      return;
    case JC.SHAPE_DOTS:
      sm.loadShape(id);
      sm.setShapePropertyBs(id, "ignore", BSUtil.copyInvert(bsAtoms, sm.viewer
          .getAtomCount()), null);
      break;
    case JC.SHAPE_MESHRIBBON: // PyMOL putty
      id = JC.SHAPE_TRACE;
      float[] data = new float[bsAtoms.length()];
      rd = new RadiusData(data, 0, RadiusData.EnumType.ABSOLUTE, EnumVdw.AUTO);
      Atom[] atoms = sm.viewer.modelSet.atoms;

      double sum = 0.0,
      sumsq = 0.0;
      float min = Float.MAX_VALUE;
      float max = 0;
      int n = bsAtoms.cardinality();
      for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
        float value = Atom.atomPropertyFloat(null, atoms[i], T.temperature);
        sum += value;
        sumsq += (value * value);
        if (value < min)
          min = value;
        if (value > max)
          max = value;
      }
      float mean = (float) (sum / n);
      float stdev = (float) Math.sqrt((sumsq - (sum * sum / n)) / n);

      /*
      getFloatSetting(PyMOL.cartoon_putty_quality),
      getFloatSetting(PyMOL.cartoon_putty_radius),
      getFloatSetting(PyMOL.cartoon_putty_range),
      getFloatSetting(PyMOL.cartoon_putty_scale_min),
      getFloatSetting(PyMOL.cartoon_putty_scale_max),
      getFloatSetting(PyMOL.cartoon_putty_scale_power),        
      getFloatSetting(PyMOL.cartoon_putty_transform),
      */
      float rad = ((float[]) info)[1];
      float range = ((float[]) info)[2];
      float scale_min = ((float[]) info)[3];
      float scale_max = ((float[]) info)[4];
      float power = ((float[]) info)[5];
      
      int transform = (int) ((float[]) info)[6];
      float data_range = max - min;
      boolean nonlinear = false;
      switch (transform) {
      case cPuttyTransformNormalizedNonlinear:
      case cPuttyTransformRelativeNonlinear:
      case cPuttyTransformScaledNonlinear:
      case cPuttyTransformAbsoluteNonlinear:
        nonlinear = true;
        break;
      }
      for (int i = bsAtoms.nextSetBit(0), pt = 0; i >= 0; i = bsAtoms
          .nextSetBit(i + 1), pt++) {
        float scale = Atom.atomPropertyFloat(null, atoms[i], T.temperature);
        switch (transform) {
        case cPuttyTransformAbsoluteNonlinear:
        case cPuttyTransformAbsoluteLinear:
        default:
          break;
        case cPuttyTransformNormalizedNonlinear:
        case cPuttyTransformNormalizedLinear:
          /* normalized by Z-score, with the range affecting the distribution width */
          scale = 1 + (scale - mean) / range / stdev;
          break;
        case cPuttyTransformRelativeNonlinear:
        case cPuttyTransformRelativeLinear:
          scale = (scale - min) / data_range / range;
          break;
        case cPuttyTransformScaledNonlinear:
        case cPuttyTransformScaledLinear:
          scale /= range;
          break;
        case cPuttyTransformImpliedRMS:
          if (scale < 0.0F)
            scale = 0.0F;
          scale = (float) (Math.sqrt(scale / 8.0) / Math.PI);
          break;
        }
        if (scale < 0.0F)
          scale = 0.0F;
        if (nonlinear)
          scale = (float) Math.pow(scale, power);
        if ((scale < scale_min) && (scale_min >= 0.0))
          scale = scale_min;
        if ((scale > scale_max) && (scale_max >= 0.0))
          scale = scale_max;
        data[i] = scale * rad;
      }
      break;
    }
    // cartoon, trace, etc.
    if (size != -1 || rd != null)
      sm.setShapeSizeBs(id, size, rd, bsAtoms);
    if (translucency > 0) {
      sm.setShapePropertyBs(id, "translucentLevel",
          Float.valueOf(translucency), bsAtoms);
      sm.setShapePropertyBs(id, "translucency", "translucent", bsAtoms);
    }
    if (argb != 0)
      sm.setShapePropertyBs(id, "color", Integer.valueOf(argb), bsAtoms);
    else if (colors != null)
      sm.setShapePropertyBs(id, "colors", colors, bsAtoms);
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