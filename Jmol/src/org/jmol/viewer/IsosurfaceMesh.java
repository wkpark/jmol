/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-03 14:51:57 -0600 (Sun, 03 Dec 2006) $
 * $Revision: 6372 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: miguel@jmol.org, jmol-developers@lists.sf.net
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

import javax.vecmath.Point4f;

import org.jmol.g3d.Graphics3D;

class IsosurfaceMesh extends Mesh {
  String jvxlFileHeader;
  String jvxlExtraLine;
  int jvxlCompressionRatio;
  String jvxlSurfaceData;
  String jvxlEdgeData;
  String jvxlColorData;
  boolean isJvxlPrecisionColor;
  Point4f jvxlPlane;
  String jvxlDefinitionLine;
  String jvxlInfoLine;
  boolean isContoured;
  boolean isBicolorMap;
  float mappedDataMin;
  float mappedDataMax;
  float valueMappedToRed;
  float valueMappedToBlue;
  float cutoff;
  int nBytes;
  int nContours;  
  
  IsosurfaceMesh(Viewer viewer, String thisID, Graphics3D g3d, short colix) {
    super(viewer, thisID, g3d, colix);
  }

}
