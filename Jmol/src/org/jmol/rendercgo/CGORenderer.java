/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 11:44:18 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4528 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sourceforge.net
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
package org.jmol.rendercgo;


import org.jmol.renderspecial.DrawRenderer;
import org.jmol.shape.Mesh;
import org.jmol.shapecgo.CGO;
import org.jmol.shapecgo.CGOMesh;
import org.jmol.util.ColorUtil;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;

public class CGORenderer extends DrawRenderer {

  private CGOMesh cgoMesh;
  private JmolList<Object> cmds;
  private V3 vTemp1 = new V3();

  @Override
  protected boolean render() {
    needTranslucent = false;
    imageFontScaling = viewer.getImageFontScaling();
    CGO cgo = (CGO) shape;
    for (int i = cgo.meshCount; --i >= 0;)
      renderMesh(cgoMesh = (CGOMesh) cgo.meshes[i]);
    return needTranslucent;
  }

  @Override
  public boolean renderMesh(Mesh mesh) {
    this.mesh = mesh;
    cmds = cgoMesh.cmds;
    int n = cmds.size();
    int mode = 0;
    int nPts = 0;
    if (!g3d.setColix(cgoMesh.colix)) {
      needTranslucent = true;
      return true;
    }
    for (int i = 0; i < n; i++) {
      int type = getInt(i);
      if (type == 0)
        break;
      int len = CGOMesh.getSize(type);
      if (len < 0) {
        Logger.error("CGO unknown type: " + type);
        return false;
      }
      switch(type) {
      default:
        System.out.println("CGO ? " + type);
        //$FALL-THROUGH$
      case CGOMesh.RESET_NORMAL:
        i += len;
        break;
      case CGOMesh.NULL:
        continue;
      case CGOMesh.BEGIN:
        mode = getInt(++i);
        //$FALL-THROUGH$
      case CGOMesh.END:
        nPts = 0;
        break;
      case CGOMesh.VERTEX:
        if (nPts++ == 0) {
          i = getPoint(++i, vpt0);
          viewer.transformPtScr(vpt0, pt0i);
          continue;
        }
        i = getPoint(++i, vpt1);
        viewer.transformPtScr(vpt1, pt1i);
        P3 pt = vpt0;
        vpt0 = vpt1;
        vpt1 = pt;
        P3i spt = pt0i;
        pt0i = pt1i;
        pt1i = spt;
        drawLine(1, 2, false, vpt0, vpt1, pt0i, pt1i); 
        break;
      case CGOMesh.LINEWIDTH:
        diameter = getInt(++i);
        break;
      case CGOMesh.SAUSAGE:
        i = getPoint(++i, vpt0);
        viewer.transformPtScr(vpt0, pt0i);
        i = getPoint(++i, vpt1);
        viewer.transformPtScr(vpt1, pt1i);
        i = getPoint(++i, vTemp); // color1
        i = getPoint(++i, vTemp2); // color2
        g3d.setColor(ColorUtil.colorPtToInt(vTemp));
        drawLine(1, 2, false, vpt0, vpt1, pt0i, pt1i);
        break;
      case CGOMesh.TRIANGLE:
        i = getPoint(++i, vpt0);
        viewer.transformPtScr(vpt0, pt0i);
        i = getPoint(++i, vpt1);
        viewer.transformPtScr(vpt1, pt1i);
        i = getPoint(++i, vpt2);
        viewer.transformPtScr(vpt2, pt2i);
        i = getPoint(++i, vTemp);  // normal1
        i = getPoint(++i, vTemp);  // normal2
        i = getPoint(++i, vTemp);  // normal3
        i = getPoint(++i, vTemp);  // color1
        i = getPoint(++i, vTemp1); // color2
        i = getPoint(++i, vTemp2); // color3
        g3d.setColor(ColorUtil.colorPtToInt(vTemp));
        drawLine(1, 2, false, vpt0, vpt1, pt0i, pt1i);
        g3d.setColor(ColorUtil.colorPtToInt(vTemp1));
        drawLine(1, 2, false, vpt1, vpt2, pt1i, pt2i);
        g3d.setColor(ColorUtil.colorPtToInt(vTemp2));
        drawLine(1, 2, false, vpt2, vpt0, pt2i, pt0i);
        //drawTriangleRGB(pt0i, pt1i, pt2i, 
          //  ColorUtil.colorPtToInt(vTemp),
            //ColorUtil.colorPtToInt(vTemp),
            //ColorUtil.colorPtToInt(vTemp));        
        break;
      }      
    }
    return true;
  }

//  private void drawTriangleRGB(P3i s1, P3i s2, P3i s3, int c1,
//                               int c2, int c3) {
//
//    //g3d.fillCylinderRGB(s1, s2, c1, c2, GData.ENDCAPS_OPEN, width);
//    //g3d.fillCylinderRGB(s2, s3, c2, c3, GData.ENDCAPS_OPEN, width);
//    //g3d.fillCylinderRGB(s3, s1, c3, c1, GData.ENDCAPS_OPEN, width);    
//  }

  private int getPoint(int i, Tuple3f pt) {
    pt.set(getFloat(i++), getFloat(i++), getFloat(i));
    return i;
  }

  private int getInt(int i) {
    return ((Number) cmds.get(i)).intValue();
  }

  private float getFloat(int i) {
    return ((Number) cmds.get(i)).floatValue();
  }
}
