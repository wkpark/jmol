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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.shape.AtomShape;
import org.jmol.util.Escape;
import org.jmol.util.Quadric;
import org.jmol.viewer.JmolConstants;

public class Ellipsoids extends AtomShape {
  // most differences are in renderer
  
  Hashtable htEllipsoids = new Hashtable();
  boolean haveEllipsoids;
  
  class Ellipsoid {
    
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
    
  public int getIndexFromName(String thisID) {
    if (htEllipsoids.containsKey(thisID))
      return 1; 
    return -1; 
  }

  public void setProperty(String propertyName, Object value, BitSet bs) {
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[])((Object[])value)[2])[0];
      Enumeration e = htEllipsoids.keys();
      while (e.hasMoreElements()) {
        String id = (String) e.nextElement();
        Ellipsoid ellipsoid = (Ellipsoid) htEllipsoids.get(id);
        if (ellipsoid.modelIndex > modelIndex)
          ellipsoid.modelIndex--;
        else if (ellipsoid.modelIndex == modelIndex)
          htEllipsoids.remove(id);
      }
    } else if (value instanceof Object[]) { // not atom-based
      Ellipsoid ellipsoid;
      String id;
      
      Object[] info = (Object[]) value;
      id = (String) info[0];
      ellipsoid = (Ellipsoid) htEllipsoids.get(id);
      if (ellipsoid == null) {
        ellipsoid = new Ellipsoid(id, viewer.getCurrentModelIndex());
        htEllipsoids.put(id, ellipsoid);
      }
      haveEllipsoids = true;
      value = info[1];
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
          if (ellipsoid.axes[i].length() > ellipsoid.axes[i+1].length()) {
            Vector3f v = ellipsoid.axes[i];
            ellipsoid.axes[i] = ellipsoid.axes[i + 1];
            ellipsoid.axes[i + 1] = v;
            if (i == 1)
              i = -1;
          }
        }
        for (int i = 0; i < 3; i++) {
          ellipsoid.lengths[i] = ellipsoid.axes[i].length();
          if (ellipsoid.lengths[i] == 0) {
            return;
          }
          ellipsoid.axes[i].normalize();
        }
        if (Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            || Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            || Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            )
            return;
        updateEquation(ellipsoid);
        return;
      }
      if ("equation" == propertyName) {
        ellipsoid.coef = (double[]) value;
        ellipsoid.axes = new Vector3f[3];
        ellipsoid.lengths = new float[3];
        Quadric.getAxesForEllipsoid(ellipsoid.coef, ellipsoid.axes, ellipsoid.lengths);
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
          return;
        }
        for (int i = 0; i < 3; i++)
          ellipsoid.lengths[i] *= scale / ellipsoid.scale;
        ellipsoid.scale = scale;
        updateEquation(ellipsoid);
        return;
      }
      if ("color" == propertyName) {
        ellipsoid.colix = Graphics3D.getColix(value);
        return;
      }
      if ("translucency" == propertyName) {
        boolean isTranslucent = (value.equals("translucent"));
        ellipsoid.colix = Graphics3D.getColixTranslucent(ellipsoid.colix, isTranslucent, translucentLevel);
        return;
      }
      return; 
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
  
  public String getShapeState() {
    Enumeration e = htEllipsoids.elements();
    StringBuffer sb = new StringBuffer();
    Vector3f v1 = new Vector3f();
    while (e.hasMoreElements()) {
      Ellipsoid ellipsoid = (Ellipsoid) e.nextElement();
      if (ellipsoid.axes == null || ellipsoid.lengths == null)
        continue;
      sb.append("  Ellipsoid ").append(ellipsoid.id).append(" modelIndex ")
          .append(ellipsoid.modelIndex).append(" center ").append(
              Escape.escape(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.set(ellipsoid.axes[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.escape(v1));
      }
      sb.append(" color ").append(
          Escape.escapeColor(g3d.getColixArgb(ellipsoid.colix)));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
    if (isActive) {
      Hashtable temp = new Hashtable();
      Hashtable temp2 = new Hashtable();
      for (int i = atomCount; --i >= 0;) {
        if (bsSizeSet != null && bsSizeSet.get(i))
          setStateInfo(temp, i, "Ellipsoids " + mads[i]);
        if (bsColixSet != null && bsColixSet.get(i))
          setStateInfo(temp2, i, getColorCommand("Ellipsoids", paletteIDs[i],
              colixes[i]));
      }
      sb.append(getShapeCommands(temp, temp2, atomCount));
    }
    return sb.toString();
  }
  
  public void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * 
     */
    Enumeration e = htEllipsoids.elements();
    while (e.hasMoreElements()) {
      Ellipsoid ellipsoid = (Ellipsoid) e.nextElement();
      ellipsoid.visible = ellipsoid.isOn && (ellipsoid.modelIndex < 0 || bs.get(ellipsoid.modelIndex)); 
    }
  }
}

