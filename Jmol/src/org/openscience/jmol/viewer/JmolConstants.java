/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.viewer;

final public class JmolConstants {

  public final static byte LABEL_NONE     = 0;
  public final static byte LABEL_SYMBOL   = 1;
  public final static byte LABEL_TYPENAME = 2;
  public final static byte LABEL_ATOMNO   = 3;

  public final static byte STYLE_DELETED   =-1;
  public final static byte STYLE_NONE      = 0;
  public final static byte STYLE_WIREFRAME = 1;
  public final static byte STYLE_SHADED    = 2;
    
  public final static byte PALETTE_COLOR      =-1;
  public final static byte PALETTE_CPK        = 0;
  public final static byte PALETTE_CHARGE     = 1;
  public final static byte PALETTE_STRUCTURE  = 2;
  public final static byte PALETTE_AMINO      = 3;
  public final static byte PALETTE_SHAPELY    = 4;
  public final static byte PALETTE_CHAIN      = 5;

  public final static byte AXES_NONE = 0;
  public final static byte AXES_UNIT = 1;
  public final static byte AXES_BBOX = 2;

  public static final int MOUSE_ROTATE = 0;
  public static final int MOUSE_ZOOM = 1;
  public static final int MOUSE_XLATE = 2;
  public static final int MOUSE_PICK = 3;
  public static final int MOUSE_DELETE = 4;
  public static final int MOUSE_MEASURE = 5;
  public static final int MOUSE_DEFORM = 6; // mth -- what is this?
  public static final int MOUSE_ROTATE_Z = 7;
  public static final int MOUSE_SLAB_PLANE = 8;
  public static final int MOUSE_POPUP_MENU = 9;

  public final static byte MULTIBOND_NEVER =     0;
  public final static byte MULTIBOND_WIREFRAME = 1;
  public final static byte MULTIBOND_SMALL =     2;
  public final static byte MULTIBOND_ALWAYS =    3;

  public final static short marMultipleBondSmallMaximum = 128;

}
