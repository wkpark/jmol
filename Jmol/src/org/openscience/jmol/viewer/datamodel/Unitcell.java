/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.viewer.datamodel;

import org.openscience.cdk.geometry.CrystalGeometryTools;
import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import java.util.BitSet;

public class Unitcell extends Graphic {

  boolean show;
  boolean hasUnitcell;
  float a, b, c;
  float alpha, beta, gamma;
  Point3f[] vertices;

  public void initGraphic() {
    float[] notionalUnitcell = frame.notionalUnitcell;
    hasUnitcell = notionalUnitcell != null;
    if (hasUnitcell) {
      /****************************************************************
       * someone needs to fix this code
       * all you have to do is calculate the correct
       * points for the vertices
       ****************************************************************/
      a = notionalUnitcell[0];
      b = notionalUnitcell[1];
      c = notionalUnitcell[2];
      alpha = notionalUnitcell[3];
      beta  = notionalUnitcell[4];
      gamma = notionalUnitcell[5];
      // these vertices are wrong, but it is the best that mth can do
      double[][] cart = CrystalGeometryTools.notionalToCartesian(a,b,c,alpha,beta,gamma);
      vertices = new Point3f[] {
        new Point3f(0, 0, 0),
        new Point3f((float)cart[0][0], (float)cart[0][1], (float)cart[0][2]), // a
        new Point3f((float)cart[1][0], (float)cart[1][1], (float)cart[1][2]), // b
        new Point3f((float)cart[0][0] + (float)cart[1][0], 
                    (float)cart[0][1] + (float)cart[1][1], 
                    (float)cart[0][2] + (float)cart[1][2]), // a+b
        new Point3f((float)cart[2][0], (float)cart[2][1], (float)cart[2][2]), // c
        new Point3f((float)cart[0][0] + (float)cart[2][0], 
                    (float)cart[0][1] + (float)cart[2][1], 
                    (float)cart[0][2] + (float)cart[2][2]), // a+c
        new Point3f((float)cart[1][0] + (float)cart[2][0], 
                    (float)cart[1][1] + (float)cart[2][1], 
                    (float)cart[1][2] + (float)cart[2][2]), // b+c
        new Point3f((float)cart[0][0] + (float)cart[1][0] + (float)cart[2][0], 
                    (float)cart[0][1] + (float)cart[1][1] + (float)cart[2][1], 
                    (float)cart[0][2] + (float)cart[1][2] + (float)cart[2][2]), // a+b+c
      };
      /****************************************************************
       * all your changes should be above this line
       ****************************************************************/
    }
  }

  public void setShow(boolean show) {
    this.show = show;
  }
  
  public void setMad(short mad, BitSet bsSelected) {
  }
  
  public void setColix(byte palette, short colix, BitSet bsSelected) {
  }
}
