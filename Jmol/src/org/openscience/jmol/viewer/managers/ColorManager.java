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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;
import org.openscience.jmol.viewer.datamodel.AtomShape;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.script.Token;

import java.awt.Color;
import java.util.Hashtable;

public class ColorManager {

  JmolViewer viewer;
  JmolModelAdapter modelAdapter;
  boolean suppliesAtomArgb;
  public byte schemeDefault = JmolViewer.ATOMTYPE;

  public ColorManager(JmolViewer viewer, JmolModelAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
    suppliesAtomArgb = modelAdapter.suppliesAtomArgb();
    if (JmolModelAdapter.argbsCpk.length != JmolModelAdapter.atomicNumberMax)
      System.out.println("WARNING! argbsCpk.length not consistent");
    if (JmolModelAdapter.argbsPdbAmino.length != Token.RESID_AMINO_MAX)
      System.out.println("WARNING! argbsPdbAmino.length not consistent");
    if (JmolModelAdapter.argbsPdbShapely.length != Token.RESID_DNA_MAX)
      System.out.println("WARNING! argbsPdbShapely.length not consistent");
  }

  public void setSchemeDefault(byte scheme) {
    schemeDefault = scheme;
  }

  public byte getSchemeDefault() {
    return schemeDefault;
  }

  public Color colorSelection = Color.orange;
  public short colixSelection = Colix.ORANGE;

  public void setColorSelection(Color c) {
    colorSelection = c;
    colixSelection = Colix.getColix(c);
  }

  public Color getColorSelection() {
    return colorSelection;
  }

  public short getColixSelection() {
    return colixSelection;
  }

  public Color colorRubberband = Color.pink;
  public short colixRubberband = Colix.PINK;
  public Color getColorRubberband() {
    return colorRubberband;
  }

  public short getColixRubberband() {
    return colixRubberband;
  }

  public boolean isBondAtomColor = true;
  public void setIsBondAtomColor(boolean isBondAtomColor) {
    this.isBondAtomColor = isBondAtomColor;
  }

  public Color colorBond = null;
  public short colixBond = 0;
  public void setColorBond(Color c) {
    colorBond = c;
    colixBond = Colix.getColix(c);
  }

  public Color colorLabel = Color.black;
  public short colixLabel = Colix.BLACK;
  public void setColorLabel(Color color) {
    colorLabel = color;
    colixLabel = Colix.getColix(color);
  }

    public short colixDotsConvex = 0;
    public short colixDotsConcave = Colix.GREEN;
    public short colixDotsSaddle = Colix.RED;

    public void setColorDotsConvex(Color color) {
	colixDotsConvex = Colix.getColix(color);
    }
    public void setColorDotsConcave(Color color) {
	colixDotsConcave = Colix.getColix(color);
	if (colixDotsConcave == 0)
	    colixDotsConcave = Colix.GREEN;
    }
    public void setColorDotsSaddle(Color color) {
	colixDotsSaddle = Colix.getColix(color);
	if (colixDotsSaddle == 0)
	    colixDotsSaddle = Colix.RED;
    }

  public Color colorDistance = Color.black;
  public short colixDistance = Colix.BLACK;
  public void setColorDistance(Color c) {
    colorDistance = c;
    colixDistance = Colix.getColix(c);
  }

  public Color colorAngle = Color.black;
  public short colixAngle = Colix.BLACK;
  public void setColorAngle(Color c) {
    colorAngle = c;
    colixAngle = Colix.getColix(c);
  }

  public Color colorDihedral = Color.black;
  public short colixDihedral = Colix.BLACK;
  public void setColorDihedral(Color c) {
    colorDihedral = c;
    colixDihedral = Colix.getColix(c);
  }

  public Color colorBackground = Color.white;
  public short colixBackground = Colix.WHITE;
  public void setColorBackground(Color bg) {
    if (bg == null)
      colorBackground = Color.getColor("colorBackground");
    else
      colorBackground = bg;
    colixBackground = Colix.getColix(colorBackground);
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  public Color colorVector = Color.black;
  public short colixVector = Colix.BLACK;
  public void setColorVector(Color c) {
    colorVector = c;
    colixVector = Colix.getColix(c);
  }
  public Color getColorVector() {
    return colorVector;
  }

  public void setColorBackground(String colorName) {
    if (colorName != null && colorName.length() > 0)
      setColorBackground(getColorFromString(colorName));
  }

  // 140 JavaScript color names
  // includes 16 official HTML 4.0 color names & values

  public static String[] colorNames = {
    "aliceblue",            // F0F8FF
    "antiquewhite",         // FAEBD7
    "aqua",                 // 00FFFF
    "aquamarine",           // 7FFFD4
    "azure",                // F0FFFF
    "beige",                // F5F5DC
    "bisque",               // FFE4C4
    "black",                // 000000
    "blanchedalmond",       // FFEBCD
    "blue",                 // 0000FF
    "blueviolet",           // 8A2BE2
    "brown",                // A52A2A
    "burlywood",            // DEB887
    "cadetblue",            // 5F9EA0
    "chartreuse",           // 7FFF00
    "chocolate",            // D2691E
    "coral",                // FF7F50
    "cornflowerblue",       // 6495ED
    "cornsilk",             // FFF8DC
    "crimson",              // DC143C
    "cyan",                 // 00FFFF
    "darkblue",             // 00008B
    "darkcyan",             // 008B8B
    "darkgoldenrod",        // B8860B
    "darkgray",             // A9A9A9
    "darkgreen",            // 006400
    "darkkhaki",            // BDB76B
    "darkmagenta",          // 8B008B
    "darkolivegreen",       // 556B2F
    "darkorange",           // FF8C00
    "darkorchid",           // 9932CC
    "darkred",              // 8B0000
    "darksalmon",           // E9967A
    "darkseagreen",         // 8FBC8F
    "darkslateblue",        // 483D8B
    "darkslategray",        // 2F4F4F
    "darkturquoise",        // 00CED1
    "darkviolet",           // 9400D3
    "deeppink",             // FF1493
    "deepskyblue",          // 00BFFF
    "dimgray",              // 696969
    "dodgerblue",           // 1E90FF
    "firebrick",            // B22222
    "floralwhite",          // FFFAF0
    "forestgreen",          // 228B22
    "fuchsia",              // FF00FF
    "gainsboro",            // DCDCDC
    "ghostwhite",           // F8F8FF
    "gold",                 // FFD700
    "goldenrod",            // DAA520
    "gray",                 // 808080
    "green",                // 008000
    "greenyellow",          // ADFF2F
    "honeydew",             // F0FFF0
    "hotpink",              // FF69B4
    "indianred",            // CD5C5C
    "indigo",               // 4B0082
    "ivory",                // FFFFF0
    "khaki",                // F0E68C
    "lavender",             // E6E6FA
    "lavenderblush",        // FFF0F5
    "lawngreen",            // 7CFC00
    "lemonchiffon",         // FFFACD
    "lightblue",            // ADD8E6
    "lightcoral",           // F08080
    "lightcyan",            // E0FFFF
    "lightgoldenrodyellow", // FAFAD2
    "lightgreen",           // 90EE90
    "lightgrey",            // D3D3D3
    "lightpink",            // FFB6C1
    "lightsalmon",          // FFA07A
    "lightseagreen",        // 20B2AA
    "lightskyblue",         // 87CEFA
    "lightslategray",       // 778899
    "lightsteelblue",       // B0C4DE
    "lightyellow",          // FFFFE0
    "lime",                 // 00FF00
    "limegreen",            // 32CD32
    "linen",                // FAF0E6
    "magenta",              // FF00FF
    "maroon",               // 800000
    "mediumaquamarine",     // 66CDAA
    "mediumblue",           // 0000CD
    "mediumorchid",         // BA55D3
    "mediumpurple",         // 9370DB
    "mediumseagreen",       // 3CB371
    "mediumslateblue",      // 7B68EE
    "mediumspringgreen",    // 00FA9A
    "mediumturquoise",      // 48D1CC
    "mediumvioletred",      // C71585
    "midnightblue",         // 191970
    "mintcream",            // F5FFFA
    "mistyrose",            // FFE4E1
    "moccasin",             // FFE4B5
    "navajowhite",          // FFDEAD
    "navy",                 // 000080
    "oldlace",              // FDF5E6
    "olive",                // 808000
    "olivedrab",            // 6B8E23
    "orange",               // FFA500
    "orangered",            // FF4500
    "orchid",               // DA70D6
    "palegoldenrod",        // EEE8AA
    "palegreen",            // 98FB98
    "paleturquoise",        // AFEEEE
    "palevioletred",        // DB7093
    "papayawhip",           // FFEFD5
    "peachpuff",            // FFDAB9
    "peru",                 // CD853F
    "pink",                 // FFC0CB
    "plum",                 // DDA0DD
    "powderblue",           // B0E0E6
    "purple",               // 800080
    "red",                  // FF0000
    "rosybrown",            // BC8F8F
    "royalblue",            // 4169E1
    "saddlebrown",          // 8B4513
    "salmon",               // FA8072
    "sandybrown",           // F4A460
    "seagreen",             // 2E8B57
    "seashell",             // FFF5EE
    "sienna",               // A0522D
    "silver",               // C0C0C0
    "skyblue",              // 87CEEB
    "slateblue",            // 6A5ACD
    "slategray",            // 708090
    "snow",                 // FFFAFA
    "springgreen",          // 00FF7F
    "steelblue",            // 4682B4
    "tan",                  // D2B48C
    "teal",                 // 008080
    "thistle",              // D8BFD8
    "tomato",               // FF6347
    "turquoise",            // 40E0D0
    "violet",               // EE82EE
    "wheat",                // F5DEB3
    "white",                // FFFFFF
    "whitesmoke",           // F5F5F5
    "yellow",               // FFFF00
    "yellowgreen",          // 9ACD32
    // plus a few rasmol names/values
    "bluetint",             // AFD7FF
    "greenblue",            // 2E8B57
    "greentint",            // 98FFB3
    "grey",                 // 808080
    "pinktint",             // FFABBB
    "redorange",            // FF4500
    "yellowtint",           // F6F675
  };

  public static int[] colorArgbs = {
    0xFFF0F8FF,
    0xFFFAEBD7,
    0xFF00FFFF,
    0xFF7FFFD4,
    0xFFF0FFFF,
    0xFFF5F5DC,
    0xFFFFE4C4,
    0xFF000000,
    0xFFFFEBCD,
    0xFF0000FF,
    0xFF8A2BE2,
    0xFFA52A2A,
    0xFFDEB887,
    0xFF5F9EA0,
    0xFF7FFF00,
    0xFFD2691E,
    0xFFFF7F50,
    0xFF6495ED,
    0xFFFFF8DC,
    0xFFDC143C,
    0xFF00FFFF,
    0xFF00008B,
    0xFF008B8B,
    0xFFB8860B,
    0xFFA9A9A9,
    0xFF006400,
    0xFFBDB76B,
    0xFF8B008B,
    0xFF556B2F,
    0xFFFF8C00,
    0xFF9932CC,
    0xFF8B0000,
    0xFFE9967A,
    0xFF8FBC8F,
    0xFF483D8B,
    0xFF2F4F4F,
    0xFF00CED1,
    0xFF9400D3,
    0xFFFF1493,
    0xFF00BFFF,
    0xFF696969,
    0xFF1E90FF,
    0xFFB22222,
    0xFFFFFAF0,
    0xFF228B22,
    0xFFFF00FF,
    0xFFDCDCDC,
    0xFFF8F8FF,
    0xFFFFD700,
    0xFFDAA520,
    0xFF808080,
    0xFF008000,
    0xFFADFF2F,
    0xFFF0FFF0,
    0xFFFF69B4,
    0xFFCD5C5C,
    0xFF4B0082,
    0xFFFFFFF0,
    0xFFF0E68C,
    0xFFE6E6FA,
    0xFFFFF0F5,
    0xFF7CFC00,
    0xFFFFFACD,
    0xFFADD8E6,
    0xFFF08080,
    0xFFE0FFFF,
    0xFFFAFAD2,
    0xFF90EE90,
    0xFFD3D3D3,
    0xFFFFB6C1,
    0xFFFFA07A,
    0xFF20B2AA,
    0xFF87CEFA,
    0xFF778899,
    0xFFB0C4DE,
    0xFFFFFFE0,
    0xFF00FF00,
    0xFF32CD32,
    0xFFFAF0E6,
    0xFFFF00FF,
    0xFF800000,
    0xFF66CDAA,
    0xFF0000CD,
    0xFFBA55D3,
    0xFF9370DB,
    0xFF3CB371,
    0xFF7B68EE,
    0xFF00FA9A,
    0xFF48D1CC,
    0xFFC71585,
    0xFF191970,
    0xFFF5FFFA,
    0xFFFFE4E1,
    0xFFFFE4B5,
    0xFFFFDEAD,
    0xFF000080,
    0xFFFDF5E6,
    0xFF808000,
    0xFF6B8E23,
    0xFFFFA500,
    0xFFFF4500,
    0xFFDA70D6,
    0xFFEEE8AA,
    0xFF98FB98,
    0xFFAFEEEE,
    0xFFDB7093,
    0xFFFFEFD5,
    0xFFFFDAB9,
    0xFFCD853F,
    0xFFFFC0CB,
    0xFFDDA0DD,
    0xFFB0E0E6,
    0xFF800080,
    0xFFFF0000,
    0xFFBC8F8F,
    0xFF4169E1,
    0xFF8B4513,
    0xFFFA8072,
    0xFFF4A460,
    0xFF2E8B57,
    0xFFFFF5EE,
    0xFFA0522D,
    0xFFC0C0C0,
    0xFF87CEEB,
    0xFF6A5ACD,
    0xFF708090,
    0xFFFFFAFA,
    0xFF00FF7F,
    0xFF4682B4,
    0xFFD2B48C,
    0xFF008080,
    0xFFD8BFD8,
    0xFFFF6347,
    0xFF40E0D0,
    0xFFEE82EE,
    0xFFF5DEB3,
    0xFFFFFFFF,
    0xFFF5F5F5,
    0xFFFFFF00,
    0xFF9ACD32,
    // RasMol values
    0xFFAFD7FF,
    0xFF2E8B57,
    0xFF98FFB3,
    0xFF808080,
    0xFFFFABBB,
    0xFFFF4500,
    0xFFF6F675,
  };

  private static final Hashtable mapJavaScriptColors = new Hashtable();
  static {
    for (int i = colorNames.length; --i >= 0; )
      mapJavaScriptColors.put(colorNames[i], new Color(colorArgbs[i]));
  }

  public static int getArgbFromString(String strColor) {
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return (0xFF000000 |
                  (red & 0xFF) << 16 |
                  (grn & 0xFF) << 8  |
                  (blu & 0xFF));
        } catch (NumberFormatException e) {
        }
      } else {
        Color color = (Color)mapJavaScriptColors.get(strColor.toLowerCase());
        if (color != null)
          return color.getRGB();
      }
    }
    return 0;
  }

  public static Color getColorFromString(String strColor) {
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return new Color(red, grn, blu);
        } catch (NumberFormatException e) {
        }
      } else {
        Color color = (Color)mapJavaScriptColors.get(strColor.toLowerCase());
        if (color != null)
          return color;
      }
    }
    System.out.println("error converting string to color:" + strColor);
    return Color.pink;
  }

  public short getColixAtom(AtomShape atom) {
    return getColixAtomScheme(atom, schemeDefault);
  }

  public short getColixAtomScheme(AtomShape atom, byte scheme) {
    int argb = 0;
    if (suppliesAtomArgb) {
      argb = modelAdapter.getAtomArgb(atom.clientAtom, scheme);
      if (argb != 0)
        return Colix.getColix(argb);
      System.out.println("JmolModelAdapter.getColorAtom returned null");
    }
    switch (scheme) {
    case JmolModelAdapter.COLORSCHEME_CPK:
      argb = JmolModelAdapter.argbsCpk[atom.atomicNumber];
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_STRUCTURE:
      if (atom.pdbatom != null)
        argb = JmolModelAdapter.argbsPdbStructure[atom.pdbatom.structureType];
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_AMINO:
      if (atom.pdbatom != null) {
        byte resid = atom.pdbatom.getResID();
        if (resid >= 0 && resid < Token.RESID_AMINO_MAX)
          argb = JmolModelAdapter.argbsPdbAmino[resid];
      }
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_SHAPELY:
      if (atom.pdbatom != null) {
        byte resid = atom.pdbatom.getResID();
        if (resid >= 0 && resid < Token.RESID_DNA_MAX)
          argb = JmolModelAdapter.argbsPdbShapely[resid];
        else
          argb = JmolModelAdapter.argbPdbShapelyDefault;
      }
      break;
    case JmolModelAdapter.COLORSCHEME_PDB_CHAIN:
      if (atom.pdbatom != null) {
        int chain = atom.pdbatom.getChain() & 0x0F;
        argb = JmolModelAdapter.argbsPdbChain[chain];
      }
      break;
    case JmolModelAdapter.COLORSCHEME_CHARGE:
      break;
    }
    if (argb == 0)
      return Colix.PINK;
    return Colix.getColix(argb);
  }

  public void flushCachedColors() {
  }
}
