/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-16 14:04:41 -0600 (Thu, 16 Nov 2006) $
 * $Revision: 6233 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import java.util.BitSet;

import org.jmol.util.ArrayUtil;

abstract class IsosurfaceMeshCollection extends MeshFileCollection {

  // Draw, Isosurface(LcaoCartoon MolecularOrbital), Pmesh
  
  IsosurfaceMesh[] isomeshes = new IsosurfaceMesh[4];
  IsosurfaceMesh thisMesh;
  
  void allocMesh(String thisID) {
    meshes = isomeshes = (IsosurfaceMesh[])ArrayUtil.ensureLength(isomeshes, meshCount + 1);
    currentMesh = thisMesh = isomeshes[meshCount++] = new IsosurfaceMesh(thisID, g3d, colix);
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    currentMesh = thisMesh;
    super.setProperty(propertyName, value,bs);
    thisMesh = (IsosurfaceMesh)currentMesh;
  }

}