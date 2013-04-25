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




import org.jmol.shapespecial.DrawMesh;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;

/*
 * Compiled Graphical Object -- ala PyMOL
 * for reading PyMOL PSE files
 * 
 */

public class CGOMesh extends DrawMesh {
  
  CGOMesh(String thisID, short colix, int index) {
    super(thisID, colix, index);
  }
  

  private static int[] sizes = new int[] {
     0,  0,  1,  0,  3,
     3,  3,  4, 27, 13,
     1,  1,  1,  1, 13,
    15,  1, 35, 13,  3, 
     2,  3,  9,  1,  2,
     1, 14, 16,  1,  2
  };
  
  private static int getSize(int i) {
    return (i >= 0 && i < sizes.length ? sizes[i] : -1);
  }
  
  public static int STOP                = 0;
  public static int NULL                = 1;
  public static int BEGIN               = 2;
  public static int END                 = 3;
  public static int VERTEX              = 4;
 
  public static int NORMAL              = 5;
  public static int COLOR               = 6;
  public static int SPHERE              = 7;
  public static int TRIANGLE            = 8;
  public static int CYLINDER            = 9;
  
  public static int LINEWIDTH           = 10;
  public static int WIDTHSCALE          = 11;
  public static int ENABLE              = 12;
  public static int DISABLE             = 13;
  public static int SAUSAGE             = 14;

  public static int CUSTOM_CYLINDER     = 15;
  public static int DOTWIDTH            = 16;
  public static int ALPHA_TRIANGLE      = 17;
  public static int ELLIPSOID           = 18;
  public static int FONT                = 19;

  public static int FONT_SCALE          = 20;
  public static int FONT_VERTEX         = 21;
  public static int FONT_AXES           = 22;
  public static int CHAR                = 23;
  public static int INDENT              = 24;

  public static int ALPHA               = 25;
  public static int QUADRIC             = 26;
  public static int CONE                = 27;
  public static int RESET_NORMAL        = 28;
  public static int PICK_COLOR          = 29;

  @SuppressWarnings("unchecked")
  boolean set(JmolList<Object> list) {
    // vertices will be in list.get(0). normals?
    JmolList<Object> cmds = (JmolList<Object>) list.get(list.size() - 1);
    cmds = (JmolList<Object>) cmds.get(1);
    int n = cmds.size();
    for (int i = 0; i < n; i++) {
      int type = ((Number)cmds.get(i)).intValue();
      if (type == 0)
        break;
      int len = getSize(type);
      if (len < 0) {
        Logger.error("CGO unknown type: " + type);
        return false; 
      }
      Logger.info("CGO " + thisID + " type " + type);
      i += len;
    }
    return true;   
  }

}
