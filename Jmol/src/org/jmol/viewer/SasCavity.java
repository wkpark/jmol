/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.viewer;

import org.jmol.g3d.Graphics3D;

import javax.vecmath.*;

class SasCavity {
  
  final Point3f probeCenter;
  final Point3f pointBottom = new Point3f();
  final short normixBottom;
  
  // probeCenter is the center of the probe
  // probeBase is the midpoint between this cavity
  // and its mirror image on the other side
  SasCavity(Point3f centerI, Point3f centerJ, Point3f centerK,
            Point3f probeCenter, float radiusP, Point3f probeBase,
            // pass these in as temps so that we do not have to
            // reallocate them every time
            Vector3f vectorPI, Vector3f vectorPJ, Vector3f vectorPK,
            Vector3f vectorT, Graphics3D g3d) {
    this.probeCenter = new Point3f(probeCenter);
    
    vectorPI.sub(centerI, probeCenter);
    vectorPI.normalize();
    vectorPI.scale(radiusP);
    
    vectorPJ.sub(centerJ, probeCenter);
    vectorPJ.normalize();
    vectorPJ.scale(radiusP);
    
    vectorPK.sub(centerK, probeCenter);
    vectorPK.normalize();
    vectorPK.scale(radiusP);
    
    // the bottomPoint;
    
    vectorT.add(vectorPI, vectorPJ);
    vectorT.add(vectorPK);
    vectorT.normalize();
    pointBottom.scaleAdd(radiusP, vectorT, probeCenter);
    
    normixBottom = g3d.getInverseNormix(vectorT);
  }
}
