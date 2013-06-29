/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-22 07:48:05 -0500 (Tue, 22 May 2007) $
 * $Revision: 7806 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.jmol.api.JmolStateCreator;
import org.jmol.constant.EnumPalette;
import org.jmol.modelset.Atom;
//import org.jmol.script.T;
import org.jmol.shape.Shape;
//import org.jmol.shapesurface.IsosurfaceMesh;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.JmolList;
import org.jmol.util.P3;
//import org.jmol.util.P4;
import org.jmol.util.Tensor;
import org.jmol.util.SB;
import org.jmol.util.V3;


public class Ellipsoids extends Shape {

  public Map<String, Ellipsoid> simpleEllipsoids = new Hashtable<String, Ellipsoid>();
  public Map<Tensor, Ellipsoid> atomEllipsoids = new Hashtable<Tensor, Ellipsoid>();

  public boolean isActive() {
    return !atomEllipsoids.isEmpty() || !simpleEllipsoids.isEmpty();
  }

  private String typeSelected = "1";
  private Ellipsoid ellipsoidSelected;

  @Override
  public int getIndexFromName(String thisID) {
    return ((ellipsoidSelected = simpleEllipsoids.get(thisID)) == null ? -1 : 1);
  }

  @Override
  protected void setSize(int size, BS bsSelected) {
    if (modelSet.atoms == null) 
      return;
    boolean isAll = (bsSelected == null);    
    JmolList<Tensor> tensors = modelSet.getAllAtomTensors(typeSelected);
    if (tensors == null)
      return;
    Atom[] atoms = modelSet.atoms;
    for (int i = tensors.size(); --i >= 0;) {
      Tensor t = tensors.get(i);
      if (isAll || bsSelected.get(t.atomIndex1)) {
        Ellipsoid e = atomEllipsoids.get(t);
        if (size != 0 && e == null)
            atomEllipsoids.put(t, e = Ellipsoid.getEllipsoidForAtomTensor(t, atoms[t.atomIndex1]));
        if (e != null) {
          e.visible = (size != 0);
          e.setScale(size, true);
        }
      }
    }
    BS bsVisible = BS.newN(atoms.length);
    for (Ellipsoid e: atomEllipsoids.values())
       if (e.visible)
         bsVisible.set(e.tensor.atomIndex1);
    for (int i = atoms.length; --i >= 0;)
      atoms[i].setShapeVisibility(myVisibilityFlag, bsVisible.get(i));    
  }

//  @SuppressWarnings("unchecked")
//  @Override
//  public boolean getPropertyData(String property, Object[] data) {
//    if (property == "quadric") {
//      Tensor t = (Tensor) data[0];
//      if (t == null)
//        return false;
//      Ellipsoid e = atomEllipsoids.get(t);
//      if (e == null) {
//        P3 center = (P3) data[1];
//        if (center == null)
//          center = new P3();
//        e = Ellipsoid.getEllipsoidForAtomTensor(t, center);
//      }
//      data[2] = e.getEquation();
//      return true;
//    }
//    return false;
//  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if (propertyName == "thisID") {
      ellipsoidSelected = (value == null ? null : (Ellipsoid) simpleEllipsoids.get(value));
      if (value == null)
        return;
      if (ellipsoidSelected == null) {
        String id = (String) value;
        ellipsoidSelected = Ellipsoid.getEmptyEllipsoid(id, viewer.getCurrentModelIndex());
        simpleEllipsoids.put(id, ellipsoidSelected);
      }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Iterator<Ellipsoid> e = simpleEllipsoids.values().iterator();
      while (e.hasNext())
        if (e.next().tensor.modelIndex == modelIndex)
          e.remove();
      e = atomEllipsoids.values().iterator();
      while (e.hasNext())
        if (e.next().modelIndex == modelIndex)
          e.remove();
      ellipsoidSelected = null;
      return;
    }
    if (ellipsoidSelected != null) {
      if ("delete" == propertyName) {
        simpleEllipsoids.remove(ellipsoidSelected.id);
        return;
      }
      if ("modelindex" == propertyName) {
        ellipsoidSelected.tensor.modelIndex = ((Integer) value).intValue();
        return;
      }
      if ("on" == propertyName) {
        ellipsoidSelected.isOn = ((Boolean) value).booleanValue();
        return;
      }
      if ("atoms" == propertyName) {
        //setPoints(viewer.modelSet.atoms, (BS) value);
        return;
      }
      if ("points" == propertyName) {
        //Object[] o = (Object[]) value;
        //setPoints((P3[]) o[1], (BS) o[2]);
        return;
      }
      if ("axes" == propertyName) {
        ellipsoidSelected.setAxes((V3[]) value);
        return;
      }
      if ("equation" == propertyName) {
        ellipsoidSelected.setEquation((double[]) value);
        return;
      }
      if ("center" == propertyName) {
        ellipsoidSelected.setCenter((P3) value);
        return;
      }
      if ("scale" == propertyName) {
        ellipsoidSelected.setScale(((Float) value).floatValue(), false);
        return;
      }
      if ("color" == propertyName) {
        ellipsoidSelected.colix = C.getColixO(value);
        return;
      }
      if ("translucentLevel" == propertyName) {
        setPropS(propertyName, value, bs);
        return;
      }
      if ("translucency" == propertyName) {
        boolean isTranslucent = (value.equals("translucent"));
        ellipsoidSelected.colix = C.getColixTranslucent3(ellipsoidSelected.colix,
            isTranslucent, translucentLevel);
        return;
      }
    }

    if ("select" == propertyName) {
      typeSelected = (String) value;
      return;
    }

    if ("params" == propertyName) {
      Object[] data = (Object[]) value;
      data[2] = null;// Jmol does not allow setting sizes this way from PyMOL yet
      typeSelected = "0";
      setSize(50, bs);
      // onward...
    }

    if ("color" == propertyName) {
      short colix = C.getColixO(value);
      byte pid = EnumPalette.pidOf(value);
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected)) {
          e.colix = getColixI(colix, pid, e.tensor.atomIndex1);
          e.pid = pid;
        }
      return;
    }

    if ("translucency" == propertyName) {
      boolean isTranslucent = (value.equals("translucent"));
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected))
          e.colix = C.getColixTranslucent3(e.colix, isTranslucent,
              translucentLevel);
      return;
    }
    setPropS(propertyName, value, bs);
  }

//  private void setPoints(P3[] points, BS bs) {
//    return;
    // doesn't really work. Just something I was playing with.
//    if (points == null)
//      return;
//    int n = bs.cardinality();
//    if (n < 3)
//      return;
//    P3 ptCenter = new P3();
//    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
//      ptCenter.add(points[i]);
//    ptCenter.scale(1.0f/n);
//    double Sxx = 0, Syy = 0, Szz = 0, Sxy = 0, Sxz = 0, Syz = 0;
//    P3 pt = new P3();
//    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
//      pt.setT(points[i]);
//      pt.sub(ptCenter);
//      Sxx += (double) pt.x * (double) pt.x;
//      Sxy += (double) pt.x * (double) pt.y;
//      Sxz += (double) pt.x * (double) pt.z;
//      Syy += (double) pt.y * (double) pt.y;
//      Szz += (double) pt.z * (double) pt.z;
//      Syz += (double) pt.y * (double) pt.z;      
//    }
//    double[][] N = new double[3][3];
//    N[0][0] = Syy + Szz;
//    N[1][1] = Sxx + Szz;
//    N[2][2] = Sxx + Syy;
//    Eigen eigen = Eigen.newM(N);
//    ellipsoid.setEigen(ptCenter, eigen, 1f / n / 3);
//  }


  @Override
  public String getShapeState() {
    SB sb = new SB();
    getStateID(sb);
    getStateAtoms(sb);
    return sb.toString();
  }

  private void getStateID(SB sb) {
    if (!isActive())
      return;
    Iterator<Ellipsoid> e = simpleEllipsoids.values().iterator();
    V3 v1 = new V3();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      Tensor t = ellipsoid.tensor;
      if (!ellipsoid.isValid || t == null)
        continue;
      sb.append("  Ellipsoid ID ").append(ellipsoid.id).append(" modelIndex ")
          .appendI(t.modelIndex).append(" center ").append(
              Escape.eP(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.setT(t.eigenVectors[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.eP(v1));
      }
      sb.append(" " + getColorCommandUnk("", ellipsoid.colix, translucentAllowed));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
  }

  private void getStateAtoms(SB sb) {
    JmolStateCreator sc = viewer.getStateCreator();
    if (sc == null)
      return;
    String keyDone = "";
    for (Ellipsoid e: atomEllipsoids.values()) {
      String type = e.tensor.type;
      String key = ";" + type + ";";
      if (keyDone.indexOf(key) >= 0)
        continue;
      keyDone += key;
      appendCmd(sb, "Ellipsoids set " + Escape.eS(type));
      Map<String, BS> temp = new Hashtable<String, BS>();
      Map<String, BS> temp2 = new Hashtable<String, BS>();
      for (Ellipsoid e2: atomEllipsoids.values()) {
        if (e2.tensor.type.equals(type)) {
          int i = e2.tensor.atomIndex1;
          BSUtil.setMapBitSet(temp, i, i, "Ellipsoids " + e.percent);
          if (e.colix != C.INHERIT_ALL) {
              BSUtil.setMapBitSet(temp2, i, i, getColorCommand("Ellipsoids",
                  e.pid, e.colix, translucentAllowed));
          }
        }
      }
      sb.append(sc.getCommands(temp, temp2, "select"));
    }
  }

  @Override
  public void setVisibilityFlags(BS bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    if (!isActive())
      return;
    setVis(simpleEllipsoids, bs);
    setVis(atomEllipsoids, bs);
  }

  private void setVis(Map<?, Ellipsoid> ellipsoids, BS bs) {
    Iterator<Ellipsoid> e = ellipsoids.values().iterator();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      ellipsoid.visible = ellipsoid.isValid && ellipsoid.isOn
          && (ellipsoid.modelIndex < 0 || bs.get(ellipsoid.modelIndex));
    }
  }

  @Override
  public void setModelClickability() {
    if (atomEllipsoids.isEmpty())
      return;
    Iterator<Ellipsoid> e = atomEllipsoids.values().iterator();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      int i = ellipsoid.tensor.atomIndex1;
      Atom atom = modelSet.atoms[i];
      if ((atom.getShapeVisibilityFlags() & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(i))
        continue;
      atom.setClickable(myVisibilityFlag);
    }
  }

}
