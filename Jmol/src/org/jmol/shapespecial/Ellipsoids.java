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
import java.util.Map.Entry;

import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.shape.Shape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Txt;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.P3;
import org.jmol.util.Tensor;
import javajs.util.V3;

public class Ellipsoids extends Shape {

  private static final String PROPERTY_MODES = "ax ce co de eq mo on op sc tr";
  
  public Map<String, Ellipsoid> simpleEllipsoids = new Hashtable<String, Ellipsoid>();
  public Map<Tensor, Ellipsoid> atomEllipsoids = new Hashtable<Tensor, Ellipsoid>();

  public boolean isActive() {
    return !atomEllipsoids.isEmpty() || !simpleEllipsoids.isEmpty();
  }

  private String typeSelected = "1";
  private BS selectedAtoms;
  private Lst<Ellipsoid> ellipsoidSet;

  @Override
  public int getIndexFromName(String thisID) {
    return (checkID(thisID) ? 1 : -1);
  }

  @Override
  protected void setSize(int size, BS bsSelected) {
    if (ms.at == null || size == 0 && ms.atomTensors == null)
      return;
    boolean isAll = (bsSelected == null);
    if (!isAll && selectedAtoms != null)
      bsSelected = selectedAtoms;
    Lst<Object> tensors = vwr.ms.getAllAtomTensors(typeSelected);
    if (tensors == null)
      return;
    Atom[] atoms = ms.at;
    for (int i = tensors.size(); --i >= 0;) {
      Tensor t = (Tensor) tensors.get(i);
      if (isAll || t.isSelected(bsSelected, -1)) {
        Ellipsoid e = atomEllipsoids.get(t);
        boolean isNew = (size != 0 && e == null); 
        if (isNew)
          atomEllipsoids.put(t, e = Ellipsoid.getEllipsoidForAtomTensor(t,
              atoms[t.atomIndex1]));
        if (e != null && (isNew || size != Integer.MAX_VALUE)) { // MAX_VALUE --> "create only"
          e.setScale(size, true);
        }
      }
    }
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
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "checkID") {
      return (checkID((String) data[0]));
    }
    return false;
  }



  private boolean checkID(String thisID) {
    ellipsoidSet = new Lst<Ellipsoid>();
    if (thisID == null)
      return false;
    thisID = thisID.toLowerCase();
    if (Txt.isWild(thisID)) {
      for (Entry<String, Ellipsoid> e : simpleEllipsoids.entrySet()) {
        String key = e.getKey().toLowerCase();
        if (Txt.isMatch(key, thisID, true, true))
          ellipsoidSet.addLast(e.getValue());
      }
    }
    Ellipsoid e = simpleEllipsoids.get(thisID);
    if (e != null)
      ellipsoidSet.addLast(e);
    return (ellipsoidSet.size() > 0);
  }

  private boolean initEllipsoids(Object value) {
    boolean haveID = (value != null);
    checkID((String) value);
    if (haveID)
      typeSelected = null;
    selectedAtoms = null;
    return haveID;
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    //System.out.println(propertyName + " " + value + " " + bs);
    if (propertyName == "thisID") {
      if (initEllipsoids(value) && ellipsoidSet.size() == 0) {
        String id = (String) value;
        Ellipsoid e = Ellipsoid.getEmptyEllipsoid(id,
            vwr.getCurrentModelIndex());
        ellipsoidSet.addLast(e);
        simpleEllipsoids.put(id, e);
      }
      return;
    }

    if ("atoms" == propertyName) {
      selectedAtoms = (BS) value;
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
      ellipsoidSet.clear();
      return;
    }
    int mode = PROPERTY_MODES.indexOf((propertyName+"  ").substring(0, 2));
    if (ellipsoidSet.size() > 0) {
      if ("translucentLevel" == propertyName) {
        setPropS(propertyName, value, bs);
        return;
      }
      if (mode >= 0) 
      for (int i = ellipsoidSet.size(); --i >= 0;)
        setProp(ellipsoidSet.get(i), mode / 3, value);
      return;
    }

   if ("color" == propertyName) {
      short colix = C.getColixO(value);
      byte pid = PAL.pidOf(value);
      if (selectedAtoms != null)
        bs = selectedAtoms;
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected) && e.tensor.isSelected(bs, -1)) {
          e.colix = getColixI(colix, pid, e.tensor.atomIndex1);
          e.pid = pid;
        }
      return;
    }

    if ("on" == propertyName) {
      boolean isOn = ((Boolean) value).booleanValue();
      if (selectedAtoms != null)
        bs = selectedAtoms;
      if (isOn)
        setSize(Integer.MAX_VALUE, bs);
      for (Ellipsoid e : atomEllipsoids.values()) {
        Tensor t = e.tensor;
        if ((t.type.equals(typeSelected) || typeSelected.equals(t.altType))
            && t.isSelected(bs, -1)) {
          e.isOn = isOn;
        }
      }
      return;
    }

    if ("options" == propertyName) {
      String options = ((String) value).toLowerCase().trim();
      if (options.length() == 0)
        options = null;
      if (selectedAtoms != null)
        bs = selectedAtoms;
      if (options != null)
        setSize(Integer.MAX_VALUE, bs);
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected) && e.tensor.isSelected(bs, -1))
          e.options = options;
      return;
    }

    if ("params" == propertyName) {
      Object[] data = (Object[]) value;
      data[2] = null;// Jmol does not allow setting sizes this way from PyMOL yet
      typeSelected = "0";
      setSize(50, bs);
      // onward...
    }

    if ("points" == propertyName) {
      //Object[] o = (Object[]) value;
      //setPoints((P3[]) o[1], (BS) o[2]);
      return;
    }

    if ("scale" == propertyName) {
      setSize((int) (((Float) value).floatValue() * 100), bs);
      return;
    }

    if ("select" == propertyName) {
      typeSelected = ((String) value).toLowerCase();
      return;
    }

    if ("translucency" == propertyName) {
      boolean isTranslucent = (value.equals("translucent"));
      for (Ellipsoid e : atomEllipsoids.values())
        if (e.tensor.type.equals(typeSelected) && e.tensor.isSelected(bs, -1))
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

  private void setProp(Ellipsoid e, int mode, Object value) {
    // "ax ce co de eq mo on op sc tr"
    //  0  1  2  3  4  5  6  7  8  9
    switch (mode) {
    case 0: // axes
      e.setAxes((V3[]) value);
      return;
    case 1: // center
      e.setCenter((P3) value);
      return;
    case 2: // color
      e.colix = C.getColixO(value);
      return;
    case 3: // delete
      simpleEllipsoids.remove(e.id);
      return;
    case 4: // equation
      e.setEquation((double[]) value);
      return;
    case 5: // modelindex
      e.tensor.modelIndex = ((Integer) value).intValue();
      return;
    case 6: // on
      e.isOn = ((Boolean) value).booleanValue();
      return;
    case 7: // options
      e.options = ((String) value).toLowerCase();
      return;
    case 8: // scale
      e.setScale(((Float) value).floatValue(), false);
      return;
    case 9: // translucency
      e.colix = C.getColixTranslucent3(e.colix, value.equals("translucent"),
          translucentLevel);
      return;
    }
    return;
  }

  @Override
  public String getShapeState() {
    if (!isActive())
      return "";
    SB sb = new SB();
    sb.append("\n");
    if (!simpleEllipsoids.isEmpty())
      getStateID(sb);
    if (!atomEllipsoids.isEmpty())
      getStateAtoms(sb);
    return sb.toString();
  }

  private void getStateID(SB sb) {
    V3 v1 = new V3();
    for (Ellipsoid ellipsoid:simpleEllipsoids.values()) {
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
      if (ellipsoid.options != null)
        sb.append(" options ").append(PT.esc(ellipsoid.options));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
  }

  private void getStateAtoms(SB sb) {
    BS bsDone = new BS();
    Map<String, BS> temp = new Hashtable<String, BS>();
    Map<String, BS> temp2 = new Hashtable<String, BS>();
    for (Ellipsoid e : atomEllipsoids.values()) {
      int iType = e.tensor.iType;
      if (bsDone.get(iType + 1))
        continue;
      bsDone.set(iType + 1);
      boolean isADP = (e.tensor.iType == Tensor.TYPE_ADP);
      String cmd = (isADP ? null : "Ellipsoids set " + PT.esc(e.tensor.type));
      for (Ellipsoid e2 : atomEllipsoids.values()) {
        if (e2.tensor.iType != iType || isADP && !e2.isOn)
          continue;
        int i = e2.tensor.atomIndex1;
        // 
        BSUtil.setMapBitSet(temp, i, i, (isADP ? "Ellipsoids " + e2.percent
            : cmd + " scale " + e2.scale 
                  + (e2.options == null ? "" : " options " + PT.esc(e2.options)) 
                  + (e2.isOn ? " ON" : " OFF")));
        if (e2.colix != C.INHERIT_ALL)
          BSUtil.setMapBitSet(temp2, i, i, getColorCommand(cmd, e2.pid,
              e2.colix, translucentAllowed));
      }
    }
    sb.append(vwr.getCommands(temp, temp2, "select"));
  }

  @Override
  public void setVisibilityFlags(BS bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    if (!isActive())
      return;
    Atom[] atoms = vwr.ms.at;
    setVis(simpleEllipsoids, bs, atoms);
    if (atomEllipsoids != null)
      for (int i = atoms.length; --i >= 0;)
        atoms[i].setShapeVisibility(vf, false);
    setVis(atomEllipsoids, bs, atoms);
  }

  private void setVis(Map<?, Ellipsoid> ellipsoids, BS bs, Atom[] atoms) {
    for (Ellipsoid e: ellipsoids.values()) {
      Tensor t = e.tensor; 
      boolean isOK = (t != null && e.isValid && e.isOn);
      if (isOK && t.atomIndex1 >= 0) {
        if (t.iType == Tensor.TYPE_ADP) {
          boolean isModTensor = t.isModulated;
          boolean isUnmodTensor = t.isUnmodulated;
          boolean isModAtom = ms.isModulated(t.atomIndex1);
          isOK =(!isModTensor && !isUnmodTensor || isModTensor == isModAtom);
        }
        atoms[t.atomIndex1].setShapeVisibility(vf, true);
      }
      e.visible = isOK && (e.modelIndex < 0 || bs.get(e.modelIndex));
    }
  }

  @Override
  public void setModelClickability() {
    if (atomEllipsoids.isEmpty())
      return;
    for (Ellipsoid e: atomEllipsoids.values()) {
      int i = e.tensor.atomIndex1;
      Atom atom = ms.at[i];
      if ((atom.shapeVisibilityFlags & vf) == 0
          || ms.isAtomHidden(i))
        continue;
      atom.setClickable(vf);
    }
  }

}
