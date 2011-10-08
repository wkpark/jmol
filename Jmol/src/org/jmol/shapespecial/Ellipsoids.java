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

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
//import java.util.Map.Entry;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
//import org.jmol.script.Token;
import org.jmol.shape.AtomShape;
import org.jmol.util.Escape;
import org.jmol.util.Quadric;

public class Ellipsoids extends AtomShape {
  // most differences are in renderer
  
  Map<String, Ellipsoid> htEllipsoids = new Hashtable<String, Ellipsoid>();
  boolean haveEllipsoids;
  
  static class Ellipsoid {
    
    String id;
    Vector3f[] axes;
    float[] lengths;
    Point3f center = new Point3f(0,0,0);
    double[] coef;
    short colix = Graphics3D.GOLD;
    int modelIndex;
    float scale = 1;
    boolean visible;
    boolean isValid;
    boolean isOn = true;
    
    Ellipsoid(String id, int modelIndex) {
      this.id = id;
      this.modelIndex = modelIndex;
    }

  }
    
//  @SuppressWarnings("unchecked")
  @Override
  public boolean getProperty(String property, Object[] data) {
/*  just implemented for MeshCollection
    if (property == "getNames") {
      Map<String, Token> map = (Map<String, Token>) data[0];
      boolean withDollar = ((Boolean) data[1]).booleanValue();
      for (Entry<String, Ellipsoid>entry : htEllipsoids.entrySet())
        map.put((withDollar ? "$" : "") + entry.getKey(), Token.tokenExpressionBegin); // just a placeholder
      return true;
    }
*/
    return super.getProperty(property, data);
  }
    
  @Override
  public int getIndexFromName(String thisID) {
    return ((ellipsoid = htEllipsoids.get(thisID))
        == null ? -1 : 1);
  }

  Ellipsoid ellipsoid;
  
  @Override
  protected void setSize(int size, BitSet bsSelected) {
    super.setSize(size, bsSelected);
    if (size == 0)
      return;
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected.nextSetBit(i + 1))
      atoms[i].scaleEllipsoid(size);
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
      ellipsoid = null;
      return;
    }
    if (ellipsoid != null) {
      haveEllipsoids = true;
      if ("delete" == propertyName) {
        htEllipsoids.remove(ellipsoid.id);
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
        ellipsoid.colix = Graphics3D.getColix(value);
        return;
      }
      if ("translucentLevel" == propertyName) {
        super.setProperty(propertyName, value, bs);
        return;
      }
      if ("translucency" == propertyName) {
        boolean isTranslucent = (value.equals("translucent"));
        ellipsoid.colix = Graphics3D.getColixTranslucent(ellipsoid.colix,
            isTranslucent, translucentLevel);
        return;
      }
    }
    super.setProperty(propertyName, value, bs);
  }

  private void updateEquation(Ellipsoid ellipsoid) {
    if (ellipsoid.axes == null || ellipsoid.lengths == null)
      return;
    Matrix3f mat = new Matrix3f();
    Matrix3f mTemp = new Matrix3f();
    Vector3f v1 = new Vector3f();
    ellipsoid.coef = new double[10];
    Quadric.getEquationForQuadricWithCenter(ellipsoid.center.x, 
        ellipsoid.center.y, ellipsoid.center.z, mat, v1, mTemp,
        ellipsoid.coef, null);
    ellipsoid.isValid = true;
  }
  
  @Override
  public String getShapeState() {
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    StringBuffer sb = new StringBuffer();
    Vector3f v1 = new Vector3f();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      if (ellipsoid.axes == null || ellipsoid.lengths == null)
        continue;
      sb.append("  Ellipsoid ID ").append(ellipsoid.id).append(" modelIndex ")
          .append(ellipsoid.modelIndex).append(" center ").append(
              Escape.escape(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.set(ellipsoid.axes[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.escape(v1));
      }
      sb.append(" " + getColorCommand("", ellipsoid.colix));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
    if (isActive) {
      Map<String, BitSet> temp = new Hashtable<String, BitSet>();
      Map<String, BitSet> temp2 = new Hashtable<String, BitSet>();
      if (bsSizeSet != null)
        for (int i = bsSizeSet.nextSetBit(0); i >= 0; i = bsSizeSet
            .nextSetBit(i + 1))
          setStateInfo(temp, i, "Ellipsoids " + mads[i]);
      if (bsColixSet != null)
        for (int i = bsColixSet.nextSetBit(0); i >= 0; i = bsColixSet
            .nextSetBit(i + 1))
          setStateInfo(temp2, i, getColorCommand("Ellipsoids", paletteIDs[i],
              colixes[i]));
      sb.append(getShapeCommands(temp, temp2));
    }
    return sb.toString();
  }
  
  @Override
  public void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      ellipsoid.visible = ellipsoid.isOn && (ellipsoid.modelIndex < 0 || bs.get(ellipsoid.modelIndex)); 
    }
  }
 
}

