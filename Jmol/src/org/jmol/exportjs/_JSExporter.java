/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.exportjs;


import java.util.Hashtable;
import java.util.Map;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

import org.jmol.util.ColorUtil;
import org.jmol.util.GData;
import org.jmol.util.J2SRequireImport;

@J2SRequireImport({org.jmol.exportjs.___Exporter.class, org.jmol.exportjs.__CartesianExporter.class, org.jmol.exportjs.Export3D.class})
public class _JSExporter extends __CartesianExporter {

  private UseTable useTable;

  public _JSExporter() {
  useTable = new UseTable();
  }

	@Override
	protected void outputFace(int[] is, int[] coordMap, int faceVertexMax) {
		// TODO Auto-generated method stub
		
	}

  private Map<String, Boolean> htSpheresRendered = new Hashtable<String, Boolean>();

  private Map<String, Object> htObjects = new Hashtable<String, Object>();
  
	@Override
	protected void outputSphere(Point3f ptCenter, float radius, short colix,
			boolean checkRadius) {
    int iRad = (int) (radius * 100);
    String check = round(ptCenter) + (checkRadius ? " " + iRad : "");
    if (htSpheresRendered.get(check) != null)
      return;
    htSpheresRendered.put(check, Boolean.TRUE);
    String id = useTable.getDef("S" + colix + "_" + iRad);
    if (id.charAt(0) != '_')
      htObjects.put(id, new Object[] {getColor(colix), Float.valueOf(radius)});
    useSphereAt(id, ptCenter);
	}

  private void useSphereAt(String id, Point3f pt) {
    // implemented in JavaScript only
    System.out.println(id + " " + pt);
  }
  
  @Override
  protected boolean outputCylinder(Point3f ptCenter, Point3f pt1, Point3f pt2,
      short colix, byte endcaps, float radius, Point3f ptX, Point3f ptY,
      boolean checkRadius) {
    // ptX and ptY are ellipse major and minor axes
    // not implemented yet
    if (ptX != null)
      return false;
    float length = pt1.distance(pt2);
    String id = useTable.getDef("C" + colix + "_" + (int) (length * 100) + "_" + radius
        + "_" + endcaps);
    if (id.charAt(0) != '_')
      htObjects.put(id, new Object[] { getColor(colix), new Float(length), new Float(radius) });
    useCylinder(id, ptCenter, pt1, pt2);
    return true;
  }

  private void useCylinder(String id, Point3f ptCenter, Point3f pt1, Point3f pt2) {
    // implemented in JavaScript only
    System.out.println(id + " " + ptCenter + " " + pt1 + " " + pt2);
  }

  @Override
  protected void outputCircle(Point3f pt1, Point3f pt2, float radius,
      short colix, boolean doFill) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void outputCone(Point3f ptBase, Point3f ptTip, float radius,
      short colix) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void outputEllipsoid(Point3f center, Point3f[] points, short colix) {
    // TODO Auto-generated method stub
    
  }

  private Point3f getColor(short colix) {
    return ColorUtil.colorPointFromInt2(g3d.getColorArgbOrGray(colix));
  }

  void addObject(String id, Object o) {
    // id will be "_S1" or "_C1"
    // implemented in JavaScript only
     /**
      * @j2sNative
      * 
      */
     {
       System.out.println(id + " " + o);
     }
  }

  private void useObject(String id, Matrix4f transform) {
    // implemented in JavaScript only
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(id + " " + transform);
    }
  }

	@Override
	protected void outputTextPixel(Point3f pt, int argb) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3,
			short colix) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void output(Tuple3f pt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void outputHeader() {
		// TODO Auto-generated method stub
		
	}
  
}


