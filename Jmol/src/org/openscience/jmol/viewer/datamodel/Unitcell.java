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
      a = notionalUnitcell[0];
      b = notionalUnitcell[1];
      c = notionalUnitcell[2];
      alpha = notionalUnitcell[3];
      beta  = notionalUnitcell[4];
      gamma = notionalUnitcell[5];
      vertices = new Point3f[] {
        new Point3f(  0,  0,  0),
        new Point3f(  0,  0, 10),
        new Point3f(  0, 10,  0),
        new Point3f(  0, 10, 10),
        new Point3f( 10,  0,  0),
        new Point3f( 10,  0, 10),
        new Point3f( 10, 10,  0),
        new Point3f( 10, 10, 10),
      };
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
