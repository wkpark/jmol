/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-14 23:28:16 -0500 (Sat, 14 Apr 2007) $
 * $Revision: 7408 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

package org.jmol.shapecgo;




import java.util.ArrayList;
import java.util.List;

import org.jmol.shapespecial.DrawMesh;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Normix;
import org.jmol.util.Tuple3f;

/*
 * Compiled Graphical Object -- ala PyMOL
 * for reading PyMOL PSE files
 * 
 */

public class CGOMesh extends DrawMesh {
  
  public JmolList<Object> cmds;

  CGOMesh(String thisID, short colix, int index) {
    super(thisID, colix, index);
  }
  
  public final static int GL_POINTS = 0;
  public final static int GL_LINES = 1;
  public final static int GL_LINE_LOOP = 2;
  public final static int GL_LINE_STRIP = 3;
  public final static int GL_TRIANGLES = 4;
  public final static int GL_TRIANGLE_STRIP = 5;
  public final static int GL_TRIANGLE_FAN = 6;
  

  private final static int[] sizes = new int[] {
     0,  0,  1,  0,  3,
     3,  3,  4, 27, 13,
     1,  1,  1,  1, 13,
    15,  1, 35, 13,  3, 
     2,  3,  9,  1,  2,
     1, 14, 16,  1,  2
  };
  
  public static int getSize(int i) {
    return (i >= 0 && i < sizes.length ? sizes[i] : -1);
  }
  
  public final static int STOP                = 0;
  public final static int NULL                = 1;
  public final static int BEGIN               = 2;
  public final static int END                 = 3;
  public final static int VERTEX              = 4;
 
  public final static int NORMAL              = 5;
  public final static int COLOR               = 6;
  public final static int SPHERE              = 7;
  public final static int TRIANGLE            = 8;
  public final static int CYLINDER            = 9;
  
  public final static int LINEWIDTH           = 10;
  public final static int WIDTHSCALE          = 11;
  public final static int ENABLE              = 12;
  public final static int DISABLE             = 13;
  public final static int SAUSAGE             = 14;

  public final static int CUSTOM_CYLINDER     = 15;
  public final static int DOTWIDTH            = 16;
  public final static int ALPHA_TRIANGLE      = 17;
  public final static int ELLIPSOID           = 18;
  public final static int FONT                = 19;

  public final static int FONT_SCALE          = 20;
  public final static int FONT_VERTEX         = 21;
  public final static int FONT_AXES           = 22;
  public final static int CHAR                = 23;
  public final static int INDENT              = 24;

  public final static int ALPHA               = 25;
  public final static int QUADRIC             = 26;
  public final static int CONE                = 27;
  public final static int RESET_NORMAL        = 28;
  public final static int PICK_COLOR          = 29;

  
  public JmolList<Short> nList = new JmolList<Short>();
  public JmolList<Short> cList = new JmolList<Short>();
  
  public int getPoint(int i, Tuple3f pt) {
    pt.set(getFloat(i++), getFloat(i++), getFloat(i));
    return i;
  }

  public int getInt(int i) {
    return ((Number) cmds.get(i)).intValue();
  }

  public float getFloat(int i) {
    return ((Number) cmds.get(i)).floatValue();
  }
  

  @SuppressWarnings("unchecked")
  boolean set(JmolList<Object> list) {
    // vertices will be in list.get(0). normals?
    width = 200;
    diameter = 0;//200;
    bsTemp = new BS();
    try {
      cmds = (JmolList<Object>) list.get(1);
      if (cmds == null)
        cmds = (JmolList<Object>) list.get(0);
      cmds = (JmolList<Object>) cmds.get(1);
      int n = cmds.size();
      for (int i = 0; i < n; i++) {
        int type = ((Number) cmds.get(i)).intValue();
        switch(type) {
        case STOP:
          return true;
        case NORMAL:
          getPoint(i + 1, vTemp);
          nList.addLast(Short.valueOf(Normix.get2SidedNormix(vTemp, bsTemp)));
          break;
        case COLOR:
          getPoint(i + 1, vTemp);
          cList.addLast(Short.valueOf(C.getColix(ColorUtil.colorPtToInt(vTemp))));
          break;
        }        
        int len = getSize(type);
        if (len < 0) {
          Logger.error("CGO unknown type: " + type);
          return false;
        }
        Logger.info("CGO " + thisID + " type " + type + " len " + len);
        i += len;
      }
      return true;
    } catch (Exception e) {
      Logger.error("CGOMesh error: " + e);
      cmds = null;
      return false;
    }
  }
}
