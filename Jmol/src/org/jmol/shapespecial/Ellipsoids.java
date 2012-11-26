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


import org.jmol.shape.AtomShape;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSet;
import org.jmol.util.Colix;
import org.jmol.util.Escape;
import org.jmol.util.Matrix3f;
import org.jmol.util.Point3f;
import org.jmol.util.Quadric;
import org.jmol.util.StringXBuilder;
import org.jmol.util.Vector3f;
import org.jmol.viewer.StateManager;


public class Ellipsoids extends AtomShape {

  public Map<String, Ellipsoid> htEllipsoids = new Hashtable<String, Ellipsoid>();
  public boolean haveEllipsoids;
  public short[][] colixset;
  byte[][] paletteIDset;
  public short[][] madset;

  public static class Ellipsoid {

    String id;
    public Vector3f[] axes;
    public float[] lengths;
    public Point3f center = Point3f.new3(0, 0, 0);
    double[] coef;
    public short colix = Colix.GOLD;
    int modelIndex;
    float scale = 1;
    public boolean visible;
    public boolean isValid;
    boolean isOn = true;

    Ellipsoid(String id, int modelIndex) {
      this.id = id;
      this.modelIndex = modelIndex;
    }

  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    return super.getPropertyData(property, data);
  }

  @Override
  public int getIndexFromName(String thisID) {
    return ((ellipsoid = htEllipsoids.get(thisID)) == null ? -1 : 1);
  }

  Ellipsoid ellipsoid;
  private int iSelect;

  @Override
  protected void setSize(int size, BitSet bsSelected) {
    super.setSize(size, bsSelected);
    checkSets();
    madset[iSelect] = mads;
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1)) {
      if (size != 0)
        atoms[i].scaleEllipsoid(size, iSelect);
      boolean isVisible = (madset[0] != null && madset[0].length > i && madset[0][i] > 0
          || madset[1] != null && madset[1].length > i && madset[1][i] > 0
          || madset[2] != null && madset[2].length > i && madset[2][i] > 0);
      bsSizeSet.setBitTo(i, isVisible);
      atoms[i].setShapeVisibility(myVisibilityFlag, isVisible);
    }
  }

  @Override
  public void setProperty(String propertyName, Object value, BitSet bs) {
    if (propertyName == "thisID") {
      ellipsoid = (value == null ? null : (Ellipsoid) htEllipsoids.get(value));
      if (value == null)
        return;
      if (ellipsoid == null) {
        String id = (String) value;
        ellipsoid = new Ellipsoid(id, viewer.getCurrentModelIndex());
        htEllipsoids.put(id, ellipsoid);
        haveEllipsoids = true;
      }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
      while (e.hasNext()) {
        Ellipsoid ellipsoid = e.next();
        if (ellipsoid.modelIndex > modelIndex)
          ellipsoid.modelIndex--;
        else if (ellipsoid.modelIndex == modelIndex)
          e.remove();
      }
      haveEllipsoids = !htEllipsoids.isEmpty();
      ellipsoid = null;
      return;
    }
    if (ellipsoid != null) {
      if ("delete" == propertyName) {
        htEllipsoids.remove(ellipsoid.id);
        haveEllipsoids = !htEllipsoids.isEmpty();
        return;
      }
      if ("modelindex" == propertyName) {
        ellipsoid.modelIndex = ((Integer) value).intValue();
        return;
      }
      if ("on" == propertyName) {
        ellipsoid.isOn = ((Boolean) value).booleanValue();
        return;
      }
      if ("axes" == propertyName) {
        ellipsoid.isValid = false;
        ellipsoid.axes = (Vector3f[]) value;
        ellipsoid.lengths = new float[3];
        ellipsoid.scale = 1;
        for (int i = 0; i < 2; i++) {
          if (ellipsoid.axes[i].length() > ellipsoid.axes[i + 1].length()) {
            Vector3f v = ellipsoid.axes[i];
            ellipsoid.axes[i] = ellipsoid.axes[i + 1];
            ellipsoid.axes[i + 1] = v;
            if (i == 1)
              i = -1;
          }
        }
        for (int i = 0; i < 3; i++) {
          ellipsoid.lengths[i] = ellipsoid.axes[i].length();
          if (ellipsoid.lengths[i] == 0)
            return;
          ellipsoid.axes[i].normalize();
        }
        if (Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            || Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            || Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f)
          return;
        updateEquation(ellipsoid);
        return;
      }
      if ("equation" == propertyName) {
        ellipsoid.coef = (double[]) value;
        ellipsoid.axes = new Vector3f[3];
        ellipsoid.lengths = new float[3];
        Quadric.getAxesForEllipsoid(ellipsoid.coef, ellipsoid.axes,
            ellipsoid.lengths);
        return;
      }
      if ("center" == propertyName) {
        ellipsoid.center = (Point3f) value;
        updateEquation(ellipsoid);
        return;
      }
      if ("scale" == propertyName) {
        float scale = ((Float) value).floatValue();
        if (scale <= 0 || ellipsoid.lengths == null) {
          ellipsoid.isValid = false;
        } else {
          for (int i = 0; i < 3; i++)
            ellipsoid.lengths[i] *= scale / ellipsoid.scale;
          ellipsoid.scale = scale;
          updateEquation(ellipsoid);
        }
        return;
      }
      if ("color" == propertyName) {
        ellipsoid.colix = Colix.getColixO(value);
        return;
      }
      if ("translucentLevel" == propertyName) {
        super.setProperty(propertyName, value, bs);
        return;
      }
      if ("translucency" == propertyName) {
        boolean isTranslucent = (value.equals("translucent"));
        ellipsoid.colix = Colix.getColixTranslucent3(ellipsoid.colix,
            isTranslucent, translucentLevel);
        return;
      }
    }

    if ("select" == propertyName) {
      iSelect = ((Integer) value).intValue() - 1;
      checkSets();
      colixes = colixset[iSelect];
      paletteIDs = paletteIDset[iSelect];
      mads = madset[iSelect];
      return;
    }

    super.setProperty(propertyName, value, bs);

    if (colixset != null) {
      if ("color" == propertyName || "translucency" == propertyName
          || "deleteModelAtoms" == propertyName) {
        colixset[iSelect] = colixes;
        paletteIDset[iSelect] = paletteIDs;
        madset[iSelect] = mads;
      }
    }
  }

  private void checkSets() {
    if (colixset == null) {
      colixset = ArrayUtil.newShort2(3);
      paletteIDset = ArrayUtil.newByte2(3);
      madset = ArrayUtil.newShort2(3);
    }
  }

  private void updateEquation(Ellipsoid ellipsoid) {
    if (ellipsoid.axes == null || ellipsoid.lengths == null)
      return;
    Matrix3f mat = new Matrix3f();
    Matrix3f mTemp = new Matrix3f();
    Vector3f v1 = new Vector3f();
    ellipsoid.coef = new double[10];
    Quadric.getEquationForQuadricWithCenter(ellipsoid.center.x,
        ellipsoid.center.y, ellipsoid.center.z, mat, v1, mTemp, ellipsoid.coef,
        null);
    ellipsoid.isValid = true;
  }

  @Override
  public String getShapeState() {
    StringXBuilder sb = new StringXBuilder();
    getStateID(sb);
    getStateAtoms(sb);
    return sb.toString();
  }

  private void getStateID(StringXBuilder sb) {
    if (!haveEllipsoids)
      return;
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    Vector3f v1 = new Vector3f();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      if (ellipsoid.axes == null || ellipsoid.lengths == null)
        continue;
      sb.append("  Ellipsoid ID ").append(ellipsoid.id).append(" modelIndex ")
          .appendI(ellipsoid.modelIndex).append(" center ").append(
              Escape.escapePt(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.setT(ellipsoid.axes[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.escapePt(v1));
      }
      sb.append(" " + getColorCommandUnk("", ellipsoid.colix));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
  }

  private void getStateAtoms(StringXBuilder sb) {
    if (madset == null)
      return;
    for (int ii = 0; ii < 3; ii++) {
      if (madset[ii] == null)
        continue;
      StateManager.appendCmd(sb, "Ellipsoids set " + (ii + 1) + "\n");
      Map<String, BitSet> temp = new Hashtable<String, BitSet>();
      Map<String, BitSet> temp2 = new Hashtable<String, BitSet>();
      if (bsSizeSet != null)
        for (int i = bsSizeSet.nextSetBit(0); i >= 0; i = bsSizeSet
            .nextSetBit(i + 1))
          setStateInfo(temp, i, "Ellipsoids " + madset[ii][i]);
      if (bsColixSet != null && colixset[ii] != null)
        for (int i = bsColixSet.nextSetBit(0); i >= 0; i = bsColixSet
            .nextSetBit(i + 1))
          setStateInfo(temp2, i, getColorCommand("Ellipsoids",
              paletteIDset[ii][i], colixset[ii][i]));
      sb.append(getShapeCommands(temp, temp2));
    }
  }

  @Override
  public void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    if (!haveEllipsoids)
      return;
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      ellipsoid.visible = ellipsoid.isOn
          && (ellipsoid.modelIndex < 0 || bs.get(ellipsoid.modelIndex));
    }
  }

}
