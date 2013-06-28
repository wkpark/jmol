/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.shapespecial;

import org.jmol.util.C;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.Tensor;
import org.jmol.util.V3;

public class Ellipsoid extends Tensor {
  
    public short colix = C.GOLD;
    public boolean visible;
    public boolean isValid;
    boolean isOn = true;

    protected Ellipsoid setID(String id, int modelIndex, String type) {
      this.id = id;
      this.modelIndex = modelIndex;
      this.type = type;
      return this;
    }

    public void setCenter(P3 center) {
      this.center = center;
      updateEquation();
    }

    @Override
    public void setScale(float scale) {
      if (scale <= 0 || lengths == null) {
        isValid = false;
      } else {
        for (int i = 0; i < 3; i++)
          lengths[i] *= scale / scale;
        this.scale = scale;
        updateEquation();
      }
    }

//    public void setEigen(P3 ptCenter, Eigen eigen, float factor) {
//      center = ptCenter;
//      axes = eigen.getEigenVectors3();
//      double[] v = eigen.getEigenvalues();
//      lengths = new float[3];
//      for (int i = 0; i < 3; i++)
//        lengths[i] = (float) (v[i] * factor);
//      scale = 1;
//      updateEquation();
//    }

    protected void setEquation(double[] coef) {
      Tensor t = new Tensor().setThermal(this.coef = coef);
      eigenVectors = t.eigenVectors;
      lengths = new float[] { t.getLength(0), t.getLength(1), t.getLength(2) };
    }

    protected void setAxes(V3[] axes) {
      isValid = false;
      eigenVectors = axes;
      lengths = new float[3];
      scale = 1;
      for (int i = 0; i < 2; i++) {
        if (axes[i].length() > axes[i + 1].length()) {
          V3 v = axes[i];
          axes[i] = axes[i + 1];
          axes[i + 1] = v;
          if (i == 1)
            i = -1;
        }
      }
      for (int i = 0; i < 3; i++) {
        lengths[i] = axes[i].length();
        if (lengths[i] == 0)
          return;
        axes[i].normalize();
      }
      if (Math.abs(axes[0].dot(axes[1])) > 0.0001f
          || Math.abs(axes[0].dot(axes[1])) > 0.0001f
          || Math.abs(axes[0].dot(axes[1])) > 0.0001f)
        return;
      updateEquation();
    }

    private void updateEquation() {
      if (eigenVectors == null || lengths == null)
        return;
      Matrix3f mat = new Matrix3f();
      Matrix3f mTemp = new Matrix3f();
      V3 v1 = new V3();
      coef = new double[10];
      Ellipsoids.getEquationForQuadricWithCenter(center.x, center.y, center.z, mat, v1,
          mTemp, coef, null);
      isValid = true;
    }

  }