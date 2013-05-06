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


import java.util.Collection;
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
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.SB;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;

/**
 * a class to store rendering information prior to finishing file loading,
 * specifically designed for reading PyMOL PSE files.
 * 
 * More direct than a script
 * 
 */
class JmolObject {

  private static final int cPuttyTransformNormalizedNonlinear = 0;
  private static final int cPuttyTransformRelativeNonlinear   = 1;
  private static final int cPuttyTransformScaledNonlinear     = 2;
  private static final int cPuttyTransformAbsoluteNonlinear   = 3;
  private static final int cPuttyTransformNormalizedLinear    = 4;
  private static final int cPuttyTransformRelativeLinear      = 5;
  private static final int cPuttyTransformScaledLinear        = 6;
  private static final int cPuttyTransformAbsoluteLinear      = 7;
  private static final int cPuttyTransformImpliedRMS          = 8;

  
  int id;
  private BS bsAtoms;
  private Object info;
  private int size = -1;  
  private short[] colixes;
  private Object[] colors;

  String branchNameID;
  int argb;
  float translucency = 0;
  boolean visible = true;
  RadiusData rd;

  /**
   * 
   * @param id   A Token or JmolConstants.SHAPE_XXXX
   * @param branchNameID
   * @param bsAtoms
   * @param info     optional additional information for the shape
   */
  JmolObject(int id, String branchNameID, BS bsAtoms, Object info) {
    this.id = id;
    this.bsAtoms = bsAtoms;
    this.info = info;
    this.branchNameID = branchNameID;
  }
  
  /**
   * offset is carried out in ModelLoader when the "script" is processed to move
   * the bits to skip the base atom index.
   * 
   * @param modelOffset
   * @param atomOffset 
   */
  @SuppressWarnings("unchecked")
  void offset(int modelOffset, int atomOffset) {
    if (id == T.frame) {
      int i = ((Integer) info).intValue();
      if (i > 0)
        info = Integer.valueOf(modelOffset + i - 1);
      return;
    }
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
      Collection<Object> map = ((Map<String, Object>) info).values();
      for (Object o: map)
        BSUtil.offset((BS) o, 0, atomOffset);
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
  void finalizeObject(ModelSet m, BS bsCarb) {
    ShapeManager sm = m.shapeManager;
    int modelIndex = getModelIndex(m);
    String sID;
    SB sb = null;
    switch (id) {
    case T.define:
      // executed even for states
      sm.viewer.defineAtomSets((Map<String, Object>) info);
      return;
    case T.movie:
      sm.viewer.setMovie((Map<String, Object>) info);
      return;
    case T.frame:
      int frame = ((Integer) info).intValue();
      if (frame >= 0) {
        sm.viewer.setCurrentModelIndex(frame);
      } else {
        sm.viewer.setAnimationRange(-1, -1);
        sm.viewer.setCurrentModelIndex(-1);
      }
      return;
    case T.group:
      // if we implement GROUP in Jmol, we need to do something here.
      return;
    case T.hidden:
      sm.viewer.displayAtoms(bsAtoms, false, false, Boolean.TRUE, true);
      return;
    case JC.SHAPE_LABELS:
      sm.loadShape(id);
      sm.setShapePropertyBs(id, "textLabels", info, bsAtoms);
      return;
    case JC.SHAPE_STICKS:
    case JC.SHAPE_BALLS:
      break;
    case JC.SHAPE_TRACE:
    case JC.SHAPE_BACKBONE:
      BSUtil.andNot(bsAtoms, bsCarb);
      break;
    case JC.SHAPE_DOTS:
      sm.loadShape(id);
      sm.setShapePropertyBs(id, "ignore", BSUtil.copyInvert(bsAtoms, sm.viewer
          .getAtomCount()), null);
      break;
    case JC.SHAPE_MESHRIBBON: // PyMOL putty
      setPutty(sm);
      break;
    case JC.SHAPE_ISOSURFACE:
      if (bsAtoms == null? !visible : modelIndex < 0)
        return;
      break;
    default:
      if (!visible)
        return; // for now -- could be occluded by a nonvisible group
      break;
    }

    switch (id) {
    case JC.SHAPE_MEASURES:
      if (modelIndex < 0)
        return;
      sm.loadShape(id);
      MeasurementData md = (MeasurementData) info;
      md.setModelSet(m);
      JmolList<Object> points = md.points;
      for (int i = points.size(); --i >= 0;)
        ((Point3fi) points.get(i)).modelIndex = (short) modelIndex;
      System.out.println("PyMOL measurement " + md.thisID);
      sm.setShapePropertyBs(id, "measure", md, bsAtoms);
      return;
    case JC.SHAPE_CGO:
      JmolList<Object> cgo = (JmolList<Object>) info;
      sID = (String) cgo.get(cgo.size() - 1);
      sm.viewer.setCGO(cgo);
      break;
    case T.mep:
      JmolList<Object> mep = (JmolList<Object>) info;
      sID = mep.get(mep.size() - 2).toString();
      String mapID = mep.get(mep.size() - 1).toString();
      float min = PyMOLReader.floatAt(PyMOLReader.listAt(mep, 3), 0);
      float max = PyMOLReader.floatAt(PyMOLReader.listAt(mep, 3), 2);
      sb = new SB();
      sb.append("set isosurfacekey true;isosurface ID ").append(Escape.eS(sID))
          .append(" map \"\" ").append(Escape.eS(mapID)).append(
              ";color isosurface range " + min + " " + max
                  + ";isosurface colorscheme rwb");
      break;
    case T.mesh:
      modelIndex = sm.viewer.getCurrentModelIndex();
      JmolList<Object> mesh = (JmolList<Object>) info;
      sID = mesh.get(mesh.size() - 2).toString();
      sb = new SB();
      sb.append("isosurface ID ").append(Escape.eS(sID)).append(" model ")
          .append(m.models[modelIndex].getModelNumberDotted())
          .append(" color ").append(Escape.escapeColor(argb)).append(" \"\" ")
          .append(Escape.eS(sID)).append(" mesh nofill frontonly");
      float within = PyMOLReader.floatAt(PyMOLReader.listAt(PyMOLReader.listAt(
          mesh, 2), 0), 11);
      JmolList<Object> list = PyMOLReader.listAt(PyMOLReader.listAt(PyMOLReader
          .listAt(mesh, 2), 0), 12);
      if (within > 0) {
        P3 pt = new P3();
        sb.append(";isosurface slab within ").appendF(within).append(" [ ");
        for (int j = list.size() - 3; j >= 0; j -= 3) {
          PyMOLReader.pointAt(list, j, pt);
          sb.append(Escape.eP(pt));
        }
        sb.append(" ]");
      }
      sb.append(";set meshScale ").appendI(size / 500);
      break;
    case JC.SHAPE_ISOSURFACE:
      sID = (bsAtoms == null ? (String) info : branchNameID);
      sb = new SB();
      sb.append("isosurface ID ").append(Escape.eS(sID));
      if (bsAtoms == null) {
        // point display of map 
        modelIndex = sm.viewer.getCurrentModelIndex();
        sb.append(" model ")
            .append(m.models[modelIndex].getModelNumberDotted()).append(
                " color density sigma 1.0").append(" \"\" ").append(
                Escape.eS(sID));
        break;
      }
      if (argb == 0)
        sm.setShapePropertyBs(JC.SHAPE_BALLS, "colors", colors, bsAtoms);
      String lighting = (String) info;
      String resolution = "";
      if (lighting == null) {
        lighting = "mesh nofill";
        resolution = " resolution 1.5";
      }
      sb.append(" model ").append(m.models[modelIndex].getModelNumberDotted())
          .append(resolution).append(" select ").append(Escape.eBS(bsAtoms))
          .append(" only").append(" solvent ").appendF(size / 1000f);
      if (argb == 0)
        sb.append(" map property color");
      else
        sb.append(" color ").append(Escape.escapeColor(argb));
      sb.append(" frontOnly ").append(lighting);
      if (translucency > 0)
        sb.append(" translucent ").appendF(translucency);
      break;
    }
    if (sb != null) {
      sm.viewer.runScript(sb.toString());
      return;
    }
    // cartoon, trace, etc.
    if (size != -1 || rd != null)
      sm.setShapeSizeBs(id, size, rd, bsAtoms);
    if (argb != 0)
      sm.setShapePropertyBs(id, "color", Integer.valueOf(argb), bsAtoms);
    if (translucency > 0) {
      sm.setShapePropertyBs(id, "translucentLevel",
          Float.valueOf(translucency), bsAtoms);
      sm.setShapePropertyBs(id, "translucency", "translucent", bsAtoms);
    } else if (colors != null)
      sm.setShapePropertyBs(id, "colors", colors, bsAtoms);
  }

  private void setPutty(ShapeManager sm) {
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
  }

  private int getModelIndex(ModelSet m) {
    if (bsAtoms == null)
      return -1;
    int iAtom = bsAtoms.nextSetBit(0);
    return (iAtom < 0 ? -1 : m.atoms[iAtom].modelIndex);
  }

  void setColors(short[] colixes, float translucency) {
    this.colixes = colixes;
    colors = new Object[] {colixes, Float.valueOf(translucency) };
  }
  
  void setSize(float size) {
    this.size = (int) (size * 1000);
  }

}