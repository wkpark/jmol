/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.viewer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;

import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;
import org.jmol.g3d.Graphics3D;


public class Token {

  int tok;
  Object value;
  int intValue = Integer.MAX_VALUE;

  Token(int tok, int intValue, Object value) {
    this.tok = tok;
    this.intValue = intValue;
    this.value = value;
  }

  Token(int tok, int intValue) {
    this.tok = tok;
    this.intValue = intValue;
  }

  Token(int tok) {
    this.tok = tok;
  }

  Token(int tok, Object value) {
    this.tok = tok;
    this.value = value;
  }

  final static int nada              =  0;
  final static int identifier        =  1;
  final static int integer           =  2;
  final static int decimal           =  3;
  final static int string            =  4;
  final static int seqcode           =  5;
  final static int list              =  6;
  final static int point3f           =  7;
  final static int point4f           =  8; 
  final static int keyword           =  9;

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string",
    "seqcode", "list", "point", "plane", "keyword"
  };

  // command types
  
  final static int command           = (1 <<  8);
  final static int expressionCommand = (1 <<  9); // expression command
  final static int embeddedExpression= (1 << 10); // embedded expression
  final static int specialstring     = (1 << 11); // echo, label, javascript
//generally, the minus sign is used to denote atom ranges
  // this property is used for the commands which allow negative integers
  final static int negnums           = (1 << 12);
  
  // parameter types
  
  final static int setparam          = (1 << 13); // parameter to set command
  final static int colorparam        = (1 << 14);
  final static int misc              = (1 << 15); // misc parameter
  
  // expression terms
  
  final static int expression        = (1 << 16);
  final static int predefinedset     = (1 << 17) | expression;
  final static int atomproperty      = (1 << 18) | expression;
  final static int mathproperty      = (1 << 19) | atomproperty;
  final static int mathop            = (1 << 20) | expression;
  final static int mathfunc          = (1 << 21) | expression;  
  final static int comparator        = (1 << 22) | expression;

  final static int coordOrSet = negnums | embeddedExpression; 

  // These are unrelated
  final static int varArgCount       = (1 << 4);
  final static int maxArg1           = (1 << 4) | 1;
  final static int maxArg2           = (1 << 4) | 2;
  final static int maxArg3           = (1 << 4) | 3;
  final static int maxArg4           = (1 << 4) | 4;
  final static int onDefault1        = (1 << 5) | 1;
  
  // rasmol commands
  final static int backbone     = command |  0 | predefinedset;
  final static int background   = command |  1 | colorparam | setparam;
  final static int cartoon      = command |  3;
  final static int center       = command |  4 | expressionCommand;
  //final static int color        = command |  6 | colorparam | setparam | embeddedExpression; with mathfunc
  final static int connect      = command |  7 | embeddedExpression | colorparam;
  //final static int data         = command |  8; with mathfunc
  final static int define       = command |  9 | expressionCommand | expression | setparam;
  final static int dots         = command | 10 | embeddedExpression;
  final static int echo         = command | 11 | setparam | specialstring;
  final static int exit         = command | 12;
  final static int hbond        = command | 13 | setparam | expression;
  final static int help         = command | 14 | specialstring;
  //final static int label        = command | 15 | specialstring; with mathfunc
  //final static int load         = command | 16 | negnums; with mathfunc
  final static int monitor      = command | 18 | setparam | embeddedExpression | expression;
  final static int pause        = command | 19 | misc | specialstring;
  final static int quit         = command | 21;
  final static int refresh      = command | 22;
  final static int reset        = command | 24;
  final static int restrict     = command | 25 | expressionCommand;
  final static int ribbon       = command | 26;
  final static int rotate       = command | 27 | coordOrSet;
  final static int save         = command | 28;
//  final static int script       = command | 29; with mathfunc
  final static int select       = command | 30 | expressionCommand;
  final static int set          = command | 31 | negnums | embeddedExpression | colorparam;
  final static int show         = command | 32;
  final static int slab         = command | 33 | negnums | embeddedExpression;
  final static int spacefill    = command | 35 | negnums;
  final static int ssbond       = command | 36 | setparam;
  final static int stereo       = command | 38 | colorparam | negnums;
  final static int strands      = command | 39 | setparam;
  final static int trace        = command | 41;
  final static int translate    = command | 42 | negnums;
  final static int wireframe    = command | 44;
  final static int write        = command | 45;
  final static int zap          = command | 46;
  final static int zoom         = command | 47 | negnums | embeddedExpression;
  final static int zoomTo       = command | 48 | negnums | embeddedExpression;
  final static int initialize   = command | 49;
  // openrasmol commands
  final static int depth        = command | 50 | negnums | embeddedExpression;
  final static int star         = command | 51;
  // chime commands
  final static int delay        = command | 60;
  final static int loop         = command | 61;
  final static int move         = command | 62 | negnums;
  final static int spin         = command | 64 | setparam | coordOrSet;
  final static int animation    = command | 67;
  final static int frame        = command | 68;
  // jmol commands
  final static int trajectory   = command | 78;
  final static int hide         = command | 79 | expressionCommand;
  final static int font         = command | 80;
  final static int hover        = command | 81 | specialstring;
  final static int vibration    = command | 82 | negnums;
  final static int vector       = command | 83 | negnums;
  final static int meshRibbon   = command | 84;
  final static int halo         = command | 85;
  final static int rocket       = command | 86;
  final static int geosurface   = command | 87 | embeddedExpression;
  final static int moveto       = command | 88 | negnums | embeddedExpression;
  final static int bondorder    = command | 89;
  final static int console      = command | 90;
  final static int pmesh        = command | 91;
  final static int polyhedra    = command | 92 | embeddedExpression | colorparam;
  final static int centerAt     = command | 93;
  final static int isosurface   = command | 94 | colorparam | coordOrSet;
  final static int draw         = command | 95 | coordOrSet | colorparam;
  final static int getproperty  = command | 96 | embeddedExpression;
  final static int dipole       = command | 97 | coordOrSet;
  final static int configuration = command | 98;
  final static int mo           = command | 99 | colorparam | negnums;
  final static int lcaocartoon  = command | 100| colorparam | embeddedExpression;
  final static int message      = command | 101 | specialstring;
  final static int translateSelected = command | 102 | negnums;
  final static int calculate    = command | 103;
  final static int restore      = command | 104;
  final static int selectionHalo = command | 105 | setparam;
  final static int history       = command | 106 | setparam;
  final static int display       = command | 107 | setparam | expressionCommand;
  final static int ifcmd         = command | 108 |  negnums | embeddedExpression;
  final static int elsecmd       = command | 109;
  final static int endifcmd      = command | 110;
  final static int subset        = command | 111 | expressionCommand | predefinedset;
  final static int axes          = command | 112 | setparam;
  final static int boundbox      = command | 113 | setparam;
  final static int unitcell      = command | 114 | setparam | expression | predefinedset;
  final static int frank         = command | 115 | setparam;
  final static int navigate      = command | 116 | negnums | embeddedExpression;
  final static int gotocmd       = command | 117;
  final static int invertSelected = command | 118 | negnums | embeddedExpression;
  final static int rotateSelected = command | 119 | negnums | embeddedExpression;

  //the following are listed with atomproperty because they must be registered as atom property names
  //final static int model           = atomproperty | 5 | command;
  //final static int file            = atomproperty | 26 | command;

  final static int expressionBegin = expression | 100;
  final static int expressionEnd   = expression | 101;

  // atom expression operators
  final static int dot          = expression | 1;
  final static int colon        = expression | 2;
  final static int leftbrace    = expression | 3;
  final static int rightbrace   = expression | 4;
  final static int dollarsign   = expression | 5;
  final static int altloc       = expression | 6;
  final static int insertion    = expression | 7;
  final static int group        = expression | 8;
  final static int chain        = expression | 9;
  final static int sequence     = expression | 10;
  final static int comma        = expression | 11;
  final static int coord        = expression | 12;
  final static int none         = expression | 13;
  final static int all          = expression | 15;

  // generated by compiler:
  
  final static int spec_resid           = expression | 24;
  final static int spec_name_pattern    = expression | 25;
  final static int spec_seqcode         = expression | 26;
  final static int spec_seqcode_range   = expression | 27;
  final static int spec_chain           = expression | 28;
  final static int spec_alternate       = expression | 29;
  final static int spec_model           = expression | 30;  // /3, /4
  final static int spec_model2          = expression | 31;  // 1.2, 1.3
  final static int spec_atom            = expression | 32;

  final static int amino         = predefinedset |  0;
  final static int hetero        = predefinedset |  1 | setparam;
  final static int hydrogen      = predefinedset |  2 | setparam;
  final static int selected      = predefinedset |  3;
  final static int solvent       = predefinedset |  4 | setparam;
  final static int sidechain     = predefinedset |  5;
  final static int protein       = predefinedset |  6;
  final static int nucleic       = predefinedset |  7;
  final static int dna           = predefinedset |  8;
  final static int rna           = predefinedset |  9;
  final static int purine        = predefinedset | 10;
  final static int pyrimidine    = predefinedset | 11;
  final static int surface       = predefinedset | 12;

  // these next are predefined in the sense that they are known quantities
  final static int visible       = predefinedset | 17;
  final static int clickable     = predefinedset | 18;
  final static int carbohydrate  = predefinedset | 19;
  final static int hidden        = predefinedset | 20;
  final static int displayed     = predefinedset | 21;
  final static int symmetry      = predefinedset | 22;
  final static int specialposition = predefinedset | 23;

  final static int prec(Token op) {
    return ((op.tok >> 3) & 0xF);  
  }

  // precedence is set by the << 3 shift
  
  final static int leftparen    = 0  | mathop | 0 << 3;
  final static int rightparen   = 1  | mathop | 0 << 3;

  final static int leftsquare   = 0  | mathop | 1 << 3;
  final static int rightsquare  = 1  | mathop | 1 << 3;

  final static int opOr         = 0  | mathop | 2 << 3;
  final static int opXor        = 1  | mathop | 2 << 3;
  final static int opToggle     = 2  | mathop | 2 << 3;

  final static int opAnd        = 0  | mathop | 3 << 3;
  
  final static int opNot        = 0  | mathop | 4 << 3;
 
  final static int opGT         = 0  | mathop | 5 << 3 | comparator;
  final static int opGE         = 1  | mathop | 5 << 3 | comparator;
  final static int opLE         = 2  | mathop | 5 << 3 | comparator;
  final static int opLT         = 3  | mathop | 5 << 3 | comparator;
  final static int opEQ         = 4  | mathop | 5 << 3 | comparator;
  final static int opNE         = 5  | mathop | 5 << 3 | comparator;
   
  final static int hyphen       = 0  | mathop | 6 << 3;
  final static int plus         = 1  | mathop | 6 << 3;
  
  final static int slash        = 0  | mathop | 7 << 3;
  final static int asterisk     = 1  | mathop | 7 << 3;
  final static int percent      = 2  | mathop | 7 << 3;
  
  final static int unaryMinus   = 0  | mathop | 8 << 3;
  final static int propselector = 1  | mathop | 9 << 3;

  // for non-.-operators only, the maximum number of parameters is << 3
  // for .-operators, the maximum is 9, set byt propselector, above
  // no more than 7 per group due to << 3.
  
  // .distance (Point3f)
  // .label (String format)
  // within (float min|int tokType|String planeType|String sequence, BitSet atomset|Point3f pt, Point4f plane)
  // connected(float min, float max, String type, BitSet atomset1, BitSet atomset2)
  // substructure("smiles")

  final static int split        = 1  | mathfunc | mathproperty | 0 << 3;
  final static int join         = 2  | mathfunc | mathproperty | 0 << 3;
  final static int trim         = 3  | mathfunc | mathproperty | 0 << 3;
  
  // for lists: 
  
  final static int add          = 4  | mathfunc | mathproperty | 0 << 3;
  final static int sub          = 5  | mathfunc | mathproperty | 0 << 3;
  final static int mul          = 6  | mathfunc | mathproperty | 0 << 3;
  final static int div          = 7  | mathfunc | mathproperty | 0 << 3;

  final static int label        = 1  | mathfunc | mathproperty | 1 << 3 | command | specialstring | setparam;
  final static int find         = 2  | mathfunc | mathproperty | 1 << 3;
  final static int load         = 3  | mathfunc | 1 << 3 | command | negnums;
  final static int substructure = 4  | mathfunc | 1 << 3;
  final static int script       = 5  | mathfunc | 1 << 3 | command;
  final static int javascript   = 6  | mathfunc | 1 << 3 | command | specialstring;

  final static int distance     = 1  | mathfunc | mathproperty | 2 << 3;
  final static int data         = 2  | mathfunc | 2 << 3 | command;
  final static int replace      = 3  | mathfunc | mathproperty | 2 << 3;

  final static int angle        = 1  | mathfunc | 4 << 3;
  final static int within       = 1  | mathfunc | 5 << 3;
  final static int connected    = 2  | mathfunc | 5 << 3;


  final static int minmaxmask = 3 << 6;
  final static int min        = 1 << 6;
  final static int max        = 2 << 6;
  
  final static int atom      = 1 | mathproperty;
  final static int bonds     = 2 | mathproperty | setparam;
  final static int color     = 3 | mathproperty | command | colorparam | setparam | embeddedExpression | negnums;
  final static int ident     = 4 | mathproperty;
  final static int length    = 5 | mathproperty;
  final static int lines     = 6 | mathproperty;
  final static int size      = 7 | mathproperty;
  final static int xyz       = 8 | mathproperty;
  final static int property  = 9 | mathproperty | setparam;

  final static int atompropertyfloat = atomproperty | 1 << 5;
  
  final static int atomno        = atomproperty | 1;
  final static int atomID        = atomproperty | 2;
  final static int bondcount     = atomproperty | 3;
  final static int atomIndex     = atomproperty | 4;
  final static int cell          = atomproperty | 5;
  final static int element       = atomproperty | 6;
  final static int elemno        = atomproperty | 7;
  final static int file          = atomproperty | 8 | command;
  final static int formalCharge  = atomproperty | 9 | setparam;
  final static int groupID       = atomproperty | 10;
  final static int model         = atomproperty | 13 | command;
  final static int molecule      = atomproperty | 14;
  final static int occupancy     = atomproperty | 15;
  final static int polymerLength = atomproperty | 16;
  final static int radius        = atomproperty | 17 | setparam;
  final static int resno         = atomproperty | 18;
  final static int site          = atomproperty | 19;
  final static int structure     = atomproperty | 20;
  final static int symop         = atomproperty | 21;

  final static int atomX           = atompropertyfloat | 0;
  final static int atomY           = atompropertyfloat | 1;
  final static int atomZ           = atompropertyfloat | 2;
  final static int partialCharge   = atompropertyfloat | 3;
  final static int phi             = atompropertyfloat | 4;
  final static int psi             = atompropertyfloat | 5;
  final static int surfacedistance = atompropertyfloat | 6;
  final static int temperature     = atompropertyfloat | 7;
  
 // math-related Token static methods
  
  final static Point3f pt0 = new Point3f();

  static boolean bValue(Token x) {
    switch (x.tok) {
    case Token.on:
      return true;
    case Token.off:
      return false;
    case Token.integer:
      return x.intValue != 0;
    case Token.decimal:
    case Token.string:
    case Token.list:
      return fValue(x) != 0;
    case Token.bitset:
      return iValue(x) != 0;
    case Token.point3f:
    case Token.point4f:
      return Math.abs(fValue(x)) > 0.0001f;
    default:
      return false;
    }
  }

  static int iValue(Token x) {
    switch (x.tok) {
    case Token.on:
      return 1;
    case Token.off:
      return 0;
    case Token.integer:
      return x.intValue;
    case Token.decimal:
    case Token.list:
    case Token.string:
    case Token.point3f:
    case Token.point4f:
      return (int)fValue(x);
    case Token.bitset:
      return Viewer.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static float fValue(Token x) {
    switch (x.tok) {
    case Token.on:
      return 1;
    case Token.off:
      return 0;
    case Token.integer:
      return x.intValue;
    case Token.decimal:
      return ((Float) x.value).floatValue();
    case Token.list:
      int i = x.intValue;
      String[] list = (String[]) x.value;
      if (i == Integer.MAX_VALUE)
        return list.length;
    case Token.string: 
      String s = sValue(x);
      if (s.equalsIgnoreCase("true"))
        return 1;
      if (s.equalsIgnoreCase("false") || s.length() == 0)
        return 0;
      return Parser.parseFloat(s);
    case Token.bitset:
      return iValue(x);
    case Token.point3f:
      return ((Point3f) x.value).distance(pt0);
    case Token.point4f:
      return Graphics3D.distanceToPlane((Point4f) x.value, pt0);
    default:
      return 0;
    }
  }  
  
  static String sValue(Token x) {
    int i;
    switch (x.tok) {
    case Token.on:
      return "true";
    case Token.off:
      return "false";
    case Token.integer:
      return "" + x.intValue;
    case Token.point3f:
      return StateManager.escape((Point3f) x.value);
    case Token.point4f:
      return StateManager.escape((Point4f) x.value);
    case Token.bitset:
      return StateManager.escape((BitSet) x.value, !(x.value instanceof BondSet));
    case Token.list:
      String[] list = (String[]) x.value;
      i = x.intValue;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > list.length ? "" : list[i - 1]);
      StringBuffer sb = new StringBuffer();
      for (i = 0; i < list.length; i++)
        sb.append(list[i]).append("\n");
      return sb.toString();
    case Token.string:
      String s = (String) x.value;
      i = x.intValue;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i-1);
    case Token.decimal:
    default:
      return "" + x.value;
    }
  }

  static int sizeOf(Token x) {
    switch (x.tok) {
    case Token.on:
    case Token.off:
      return -1;
    case Token.integer:
      return -2;
    case Token.decimal:
      return -4;
    case Token.point3f:
      return -8;
    case Token.point4f:
      return -16;
    case Token.string:
      return ((String)x.value).length();
    case Token.list:
      return ((String[]) x.value).length;
    case Token.bitset:
      return Viewer.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static String[] concatList(Token x1, Token x2, boolean x1IsList,
                             boolean x2IsList) {
    String[] list1 = (x1IsList ? (String[]) x1.value : TextFormat.split(
        (String) x1.value, "\n"));
    String[] list2 = (x2IsList ? (String[]) x2.value : TextFormat.split(
        (String) x2.value, "\n"));
    String[] list = new String[list1.length + list2.length];
    int pt = 0;
    for (int i = 0; i < list1.length; i++)
      list[pt++] = list1[i];
    for (int i = 0; i < list2.length; i++)
      list[pt++] = list2[i];
    return list;
  }

  static BitSet bsSelect(Token token) {
    selectItem(token, Integer.MIN_VALUE);
    return (BitSet)token.value;
  }

  static BitSet bsSelect(Token token, int n) {
    selectItem(token, Integer.MIN_VALUE);
    selectItem(token, 1);
    selectItem(token, n);
    return (BitSet)token.value;
  }

  static Token selectItem(Token token, int i2) {
    
    // negative number is a count from the end
    
    BitSet bs = null;
    String[] st = null;
    String s =null;
    
    int i1 = token.intValue;
    if (i1 == Integer.MAX_VALUE) {
      if (i2 != Integer.MIN_VALUE)
      token.intValue = i2;
      return token;
    }
    int len = 0;
    int n = 0;
    switch (token.tok) {
    case Token.bitset:
      bs = (BitSet) token.value;
      len = bs.size();
      break;
    case Token.list:
      st = (String[]) token.value;
      len = st.length;
      break;
    case Token.string:
      s = (String) token.value;
      len = s.length();
    }

    token.intValue = Integer.MAX_VALUE;
    if (i1 < 0)
      i1 = len + i1 + 1;
    if (i1 < 1)
      i1 = 1;
    if (i2 == 0)
      i2 = len;
    else if (i2 < 0)
      i2 = len + i2;
    
    if (i2 > len)
      i2 = len;
    else if (i2 < i1)
      i2 = i1;

    switch (token.tok) {
    case Token.bitset:
      for (int j = 0; j < len; j++)
        if (bs.get(j) && (++n < i1 || n > i2))
          bs.clear(j);
      return token;
    case Token.string:
      if (i1 < 1 || i1 > len)
        token.value = "";
      else
        token.value = s.substring(i1 - 1, i2);
      return token;
    case Token.list:
      if (i1 < 1 || i1 > len || i2 > len)
        return new Token(Token.string, "");     
      if (i2 == i1)
        return new Token(Token.string, st[i1 - 1]);
      String[]list = new String[i2 - i1 + 1];
      for (int i = 0; i < list.length; i++)
        list[i] = st[i + i1 - 1];
      token.value = list;
      return token;
    }
    return token;
  }
  
  // parameters
  final static int ambient       = setparam |  0;
  final static int bondmode      = setparam |  1;
  final static int fontsize      = setparam |  2;
  final static int picking       = setparam |  4;
  final static int specular      = setparam |  7;
  final static int specpercent   = setparam |  8;  
  final static int specpower     = setparam |  9;
  final static int specexponent  = setparam | 10;
  final static int transparent   = setparam | 11;
  final static int defaultColors = setparam | 12;
  // load
  final static int scale3d      = setparam | 29;
  // jmol extensions
  final static int diffuse      = setparam | 31;
  final static int pickingStyle = setparam | 33;

  // misc
  final static int off          = misc |  0;
  final static int on           = misc |  1;
  final static int clipboard    = misc |  2;
  final static int spacegroup   = misc |  3;
  final static int normal       = misc |  4;
  final static int rasmol       = misc |  5;
  final static int axisangle    = misc |  6;
  final static int clear        = misc |  8;
  final static int mep          = misc |  9;
  final static int torsion      = misc | 10;
  final static int shapely      = misc | 14;
  final static int colorRGB     = misc | 20 | colorparam;
  final static int user         = misc | 21; //color option
  final static int fixedtemp    = misc | 22; // color option

  final static int dotted       = misc | 30;
  final static int mode         = misc | 31;
  final static int direction    = misc | 32;
  final static int displacement = misc | 34;
  final static int type         = misc | 35;
    
  final static int rubberband   = misc | 37;
  final static int monomer      = misc | 38;
  final static int opaque       = misc | 40;
  final static int translucent  = misc | 41;
  final static int delete       = misc | 42;
  final static int solid        = misc | 45;
  final static int jmol         = misc | 46;
  final static int absolute     = misc | 47;
  final static int average      = misc | 48;
  final static int nodots       = misc | 49;
  final static int mesh         = misc | 50;
  final static int nomesh       = misc | 51;
  final static int fill         = misc | 52;
  final static int nofill       = misc | 53;
  final static int vanderwaals  = misc | 54;
  final static int ionic        = misc | 55;
  final static int resume       = misc | 56;
  final static int play         = misc | 57;
  final static int next         = misc | 58;
  final static int prev         = misc | 59;
  final static int rewind       = misc | 60;
  final static int playrev      = misc | 61;
  final static int range        = misc | 62;
  final static int sasurface    = misc | 64;
  final static int left         = misc | 65;
  final static int right        = misc | 66;
  final static int front        = misc | 67;
  final static int back         = misc | 68;
  final static int top          = misc | 69;
  final static int bottom       = misc | 70;
  final static int bitset       = misc | 71;
  final static int bondset      = misc | 72;
  final static int last         = misc | 73;
  final static int rotation     = misc | 74;
  final static int translation  = misc | 75;
  final static int residue      = misc | 76;
  final static int url          = misc | 77;  
  final static int transform    = misc | 78;
  final static int orientation  = misc | 79;
  final static int state        = misc | 80;
  final static int pdbheader    = misc | 81;
  final static int triangles    = misc | 82;
  final static int notriangles  = misc | 83;
  final static int frontonly    = misc | 84;
  final static int notfrontonly = misc | 85;

  // predefined Tokens: 
  
  final static Token tokenOn  = new Token(on, 1, "on");
  final static Token tokenOff = new Token(off, 0, "off");
  final static Token tokenAll = new Token(all, "all");
  final static Token tokenAnd = new Token(opAnd, "and");
  final static Token tokenOr  = new Token(opOr, "or");
  final static Token tokenComma = new Token(comma, ",");
  final static Token tokenMinus = new Token(hyphen, "-");
 
  final static Token tokenExpressionBegin =
    new Token(expressionBegin, "expressionBegin");
  final static Token tokenExpressionEnd =
    new Token(expressionEnd, "expressionEnd");
  final static Token tokenCoordinateBegin =
    new Token(leftbrace, "{");
  final static Token tokenCoordinateEnd =
    new Token(rightbrace, "}");
  
  // user names
  
  final static Object[] arrayPairs  = {
    // commands
    "backbone",          new Token(backbone,  onDefault1),
    "background",        new Token(background, varArgCount),
    "cartoon",           new Token(cartoon,   onDefault1),
    "cartoons",          null,
    "center",            new Token(center,   varArgCount),
    "centre",            null,
    "color",             new Token(color,    varArgCount),
    "colour",            null,
    "connect",           new Token(connect,  varArgCount),
    "data",              new Token(data,         maxArg4),
    "define",            new Token(define,   varArgCount),
    "@",                 null,
    "dots",              new Token(dots,     varArgCount),
    "echo",              new Token(echo,         maxArg1),
    "exit",              new Token(exit,               0),
    "hbond",             new Token(hbond,     onDefault1),
    "hbonds",            null,
    "help",              new Token(help,         maxArg1),
    "label",             new Token(label,     onDefault1),
    "labels",            null,
    "load",              new Token(load,     varArgCount),
    "measure",           new Token(monitor,  varArgCount),
    "measures",          null,
    "measurement",       null,
    "measurements",      null,
    "monitor",           null,
    "monitors",          null,
    
    "pause",             new Token(pause,               maxArg1),
    "wait",              null, 
    "quit",              new Token(quit,                      0),
    "refresh",           new Token(refresh,                   0),
    "reset",             new Token(reset,               maxArg1),
    "restore",           new Token(restore,             maxArg3),
    "restrict",          new Token(restrict,        varArgCount),
    "hide",              new Token(hide,            varArgCount),
    "ribbon",            new Token(ribbon,           onDefault1),
    "ribbons",           null, 
    "rotate",            new Token(rotate,          varArgCount),
    "save",              new Token(save,                maxArg3),
    "script",            new Token(script,          varArgCount),
    "source",            null, 
    "javascript",        new Token(javascript,                1),
    "select",            new Token(select,          varArgCount),
    "set",               new Token(set,             varArgCount),
    "show",              new Token(show,                maxArg2),
    "slab",              new Token(slab,            varArgCount),
    "spacefill",         new Token(spacefill,           maxArg2),
    "cpk",               null, 
    "ssbond",            new Token(ssbond,           onDefault1),
    "ssbonds",           null, 
    "stereo",            new Token(stereo,          varArgCount),
    "strand",            new Token(strands,          onDefault1),
    "strands",           null, 
    "trace",             new Token(trace,            onDefault1),
    "translate",         new Token(translate,                 2),
    "wireframe",         new Token(wireframe,        onDefault1),
    "write",             new Token(write,           varArgCount),
    "zap",               new Token(zap,                       0),
    "zoom",              new Token(zoom,            varArgCount),
    "zoomTo",            new Token(zoomTo,          varArgCount),
    "initialize",        new Token(initialize,                0),
    //                   openrasmol commands
    "depth",             new Token(depth,           varArgCount),
    "star",              new Token(star,                maxArg2),
    "stars",             null, 
                          
    //                   chime commands
                          
    "delay",             new Token(delay,            onDefault1),
    "loop",              new Token(loop,             onDefault1),
    "move",              new Token(move,            varArgCount),
    "spin",              new Token(spin,            varArgCount),
    "frame",             new Token(frame,           varArgCount),
    "frames",            null, 
    "trajectory",        new Token(trajectory,      varArgCount),
    "trajectories",      null, 
                
    "animation",         new Token(animation),
    "anim",              null, 
                          
    //                   Jmol commands
    "centerat",          new Token(centerAt,        varArgCount),
    "font",              new Token(font,            varArgCount),
    "hover",             new Token(hover,            onDefault1),
    "vibration",         new Token(vibration,           maxArg2),
    "vector",            new Token(vector,              maxArg2),
    "vectors",           null, 
    "meshribbon",        new Token(meshRibbon, onDefault1),
    "meshribbons",       null, 
    "halo",              new Token(halo,                maxArg2),
    "halos",             null, 
    "rocket",            new Token(rocket,           onDefault1),
    "rockets",           null, 
    "moveto",            new Token(moveto,          varArgCount),
    "navigate",          new Token(navigate,        varArgCount),
    "navigation",        null, 
    "bondorder",         new Token(bondorder,                 1),
    "console",           new Token(console,          onDefault1),
    "pmesh",             new Token(pmesh,           varArgCount),
    "draw",              new Token(draw,            varArgCount),
    "dipole",            new Token(dipole,          varArgCount),
    "dipoles",           null, 
    "polyhedra",         new Token(polyhedra,       varArgCount),
    "mo",                new Token(mo,              varArgCount),
    "isosurface",        new Token(isosurface,      varArgCount),
    "geosurface",        new Token(geosurface,      varArgCount),
    "getproperty",       new Token(getproperty,     varArgCount),
    "lcaocartoon",       new Token(lcaocartoon,     varArgCount),
    "lcaocartoons",      null, 
    "message",           new Token(message,                   1),
    "if",                new Token(ifcmd,           varArgCount),
    "else",              new Token(elsecmd,                   0),
    "endif",             new Token(endifcmd,                  0),
    "goto",              new Token(gotocmd,                   1),
    "calculate",         new Token(calculate,       varArgCount),
    "history",           new Token(history,             maxArg2),
    "subset",            new Token(subset,          varArgCount),
    "boundbox",          new Token(boundbox,         onDefault1),
    "frank",             new Token(frank,            onDefault1),
    "unitcell",          new Token(unitcell,        varArgCount),
    "selectionHalos",    new Token(selectionHalo,    onDefault1),
    "selectionhalo",     null, 
    "translateSelected", new Token(translateSelected, varArgCount),
    "configuration",     new Token(configuration,   varArgCount),
    "config",            null, 
    "conformation",      null, 
    "invertSelected",    new Token(invertSelected,  varArgCount),
    "rotateSelected",    new Token(rotateSelected,  varArgCount),
    "model",             new Token(model,           varArgCount),
    "models",            null, 
    "file",              new Token(file,                      1),
                          
    //                   setparams 
    "axes",              new Token(axes,            varArgCount),
    "bondmode",          new Token(bondmode),
    "bonds",             new Token(bonds),
    "bond",              null, 
    "display",           new Token(display,         varArgCount),
    "fontsize",          new Token(fontsize),
    "picking",           new Token(picking),
    "pickingStyle",      new Token(pickingStyle),
    "radius",            new Token(radius),
    //                   solvent 
    "transparent",       new Token(transparent),
    "cell",              new Token(cell),
    //                   chime setparams
    "clear",             new Token(clear),
    "mep",               new Token(mep),
    "scale3D",           new Token(scale3d),
                          
    //                   jmol extensions
    "property",          new Token(property),
                          
    "formalCharge",      new Token(formalCharge),
    "charge",            null, 
    "partialCharge",     new Token(partialCharge),
    "phi",               new Token(phi),
    "psi",               new Token(psi),
                          
    //                   lighting 
                          
    "ambientPercent",    new Token(ambient),
    "ambient",           null, 
    "diffusePercent",    new Token(diffuse),
    "diffuse",           null, 
    "specular",          new Token(specular),
    "specularPercent",   new Token(specpercent),
    "specularPower",     new Token(specpower),
    "specpower",         null, 
    "specularExponent",  new Token(specexponent),
                          
                          
    //                   show parameters
    "rotation",          new Token(rotation),
    "group",             new Token(group),
    "chain",             new Token(chain),
    "atom",              new Token(atom),
    "atoms",             null, 
    "sequence",          new Token(sequence),
    "specialPosition",   new Token(specialposition),
    "symmetry",          new Token(symmetry),
    "spaceGroup",        new Token(spacegroup),
    "translation",       new Token(translation),
    // chime show parameters
    "residue",           new Token(residue),
    "pdbheader",         new Token(pdbheader),
                          
    "axisangle",         new Token(axisangle),
    "transform",         new Token(transform),
    "orientation",       new Token(orientation),
    "state",             new Token(state),
    "url",               new Token(url),

    // atom expressions
    "(",            new Token(leftparen),
    ")",            new Token(rightparen),
    "-",            tokenMinus,
    "and",          tokenAnd,
    "&",            null,
    "&&",           null,
    "or",           tokenOr,
    ",",            tokenComma,
    "|",            null,
    "||",           null,
    "not",          new Token(opNot),
    "!",            null,
    "xor",          new Token(opXor),
//no-- don't do this; it interferes with define
//  "~",            null,
    "tog",          new Token(opToggle),
    ",|",           null,
    "<",            new Token(opLT),
    "<=",           new Token(opLE),
    ">=",           new Token(opGE),
    ">",            new Token(opGT),
    "=",            new Token(opEQ),
    "==",           null,
    "!=",           new Token(opNE),
    "<>",           null,
    "/=",           null,
    "within",       new Token(within),
    "+",            new Token(plus),
    ".",            new Token(dot),
    "[",            new Token(leftsquare),
    "]",            new Token(rightsquare),
    "{",            new Token(leftbrace),
    "}",            new Token(rightbrace),
    "$",            new Token(dollarsign),
    "%",            new Token(percent),
    "*",            new Token(asterisk),
    ":",            new Token(colon),
    "/",            new Token(slash),
    
    "molecule",          new Token(molecule),
    "molecules",         null, 
    "altloc",            new Token(altloc),
    "altlocs",           null,
    "insertion",         new Token(insertion),
    "insertions",        null, 
    "substructure",      new Token(substructure),
    "connected",         new Token(connected),
    "atomIndex",         new Token(atomIndex),
    "atomno",            new Token(atomno),
    "elemno",            new Token(elemno),
    "_e",                null,
    "element",           new Token(element),
    "resno",             new Token(resno),
    "temperature",       new Token(temperature),
    "relativetemperature", null,
    "bondCount",         new Token(bondcount),
    "groupID",           new Token(groupID),
    "_groupID",          null, 
    "_g",                null, 
    "atomID",            new Token(atomID),
    "_atomID",           null,
    "_a",                null, 
    "structure",         new Token(structure),
    "_structure",        null,
    "occupancy",         new Token(occupancy),
    "polymerLength",     new Token(polymerLength),
    "site",              new Token(site),
    "symop",             new Token(symop),
    "off",               tokenOff, 
    "false",             null, 
    "on",                tokenOn, 
    "true",              null,                           
    "user",              new Token(user),
    "clipboard",         new Token(clipboard),
    "atomx",             new Token(atomX),
    "atomy",             new Token(atomY),
    "atomz",             new Token(atomZ),
    "all",          tokenAll,
    "none",         new Token(none),
    "null",         null,
    "inherit",      null,
    "normal",       new Token(normal),
    "rasmol",       new Token(rasmol),
    "torsion",      new Token(torsion),
    "coord",        new Token(coord),
    "shapely",      new Token(shapely),

    "amino",        new Token(amino),
    "hetero",       new Token(hetero),
    "hydrogen",     new Token(hydrogen),
    "hydrogens",    null,
    "selected",     new Token(selected),
    "hidden",       new Token(hidden),
    "displayed",    new Token(displayed),
    "solvent",      new Token(solvent),
    "dotted",       new Token(dotted),
    "sidechain",    new Token(sidechain),
    "protein",      new Token(protein),
    "carbohydrate", new Token(carbohydrate),
    "nucleic",      new Token(nucleic),
    "DNA",          new Token(dna),
    "RNA",          new Token(rna),
    "purine",       new Token(purine),
    "pyrimidine",   new Token(pyrimidine),
    "surface",      new Token(surface),
    "surfaceDistance", new Token(surfacedistance),
    "visible",      new Token(visible),
    "clickable",    new Token(clickable),
    "mode",         new Token(mode),
    "direction",    new Token(direction),
    "Jmol",         new Token(jmol),
    "displacement", new Token(displacement),
    "type",         new Token(type),
    "fixedTemperature", new Token(fixedtemp),
    "rubberband",   new Token(rubberband),
    "monomer",      new Token(monomer),
    "defaultColors",new Token(defaultColors),
    "opaque",       new Token(opaque),
    "translucent",  new Token(translucent),
    "delete",       new Token(delete),
    "solid",        new Token(solid),
    "absolute",     new Token(absolute),
    "average",      new Token(average),
    "noDots",       new Token(nodots),
    "mesh",         new Token(mesh),
    "noMesh",       new Token(nomesh),
    "fill",         new Token(fill),
    "noFill",       new Token(nofill),
    "triangles",    new Token(triangles),
    "noTriangles",  new Token(notriangles),
    "frontOnly",    new Token(frontonly),
    "notFrontOnly", new Token(notfrontonly),
    "vanderWaals",  new Token(vanderwaals),
    "vdw",          null,
    "ionic",        new Token(ionic),
    "resume",       new Token(resume),
    "next",         new Token(next),
    "previous",      new Token(prev),
    "prev",         null,
    "rewind",       new Token(rewind),
    "last",         new Token(last),
    "playRev",      new Token(playrev),
    "play",         new Token(play),
    "range",        new Token(range),
    "saSurface",    new Token(sasurface),
    "top",          new Token(top),    
    "bottom",       new Token(bottom),    
    "left",         new Token(left),    
    "right",        new Token(right),    
    "front",        new Token(front),    
    "back",         new Token(back),    
    
    "list",         new Token(list),

    "ident",        new Token(ident),
    "xyz",          new Token(xyz),
    "min",          new Token(min),
    "max",          new Token(max),
    "distance",     new Token(distance),
    "length",       new Token(length),
    "lines",        new Token(lines),
    "angle",        new Token(angle),
    "find",         new Token(find),
    "size",         new Token(size),
    "split",        new Token(split),
    "join",         new Token(join),
    "trim",         new Token(trim),
    "replace",      new Token(replace),
    "add",          new Token(add),
    "sub",          new Token(sub),
    "mul",          new Token(mul),
    "div",          new Token(div),
  };

  static Hashtable map = new Hashtable();
  static {
    Token tokenLast = null;
    String stringThis;
    Token tokenThis;
    String lcase;
    for (int i = 0; i + 1 < arrayPairs.length; i += 2) {
      stringThis = (String) arrayPairs[i];
      lcase = stringThis.toLowerCase();
      tokenThis = (Token) arrayPairs[i + 1];
      if (tokenThis == null)
        tokenThis = tokenLast;
      if (tokenThis.value == null)
        tokenThis.value = stringThis;
      if (map.get(lcase) != null)
        Logger.error("duplicate token definition:" + lcase);
      map.put(lcase, tokenThis);
      tokenLast = tokenThis;
    }
  }

  public static Token getTokenFromName(String name) {
    return (Token) map.get(name);  
  }
  
  public static String nameOf(int tok) {
    Enumeration e = map.elements();
    while (e.hasMoreElements()) {
      Token token = (Token)e.nextElement();
      if (token.tok == tok)
        return "" + token.value;
    }
    return "0x"+Integer.toHexString(tok);
   }
   
  public String toString() {
    return "Token[" + astrType[tok<=keyword ? tok : keyword] +
      "-" + tok +
      ((intValue == Integer.MAX_VALUE) ? "" : ":" + intValue + " = 0x" + Integer.toHexString(intValue)) +
      ((value == null) ? "" : ":" + value) + "]";
  }
  
  ////////command sets ///////

  /**
   * retrieves an unsorted list of viable commands that could be
   * completed by this initial set of characters. If fewer than
   * two characters are given, then only the "preferred" command
   * is given (measure, not monitor, for example), and in all cases
   * if both a singular and a plural might be returned, only the
   * singular is returned.
   * 
   * @param strBegin initial characters of the command, or null
   * @return UNSORTED semicolon-separated string of viable commands
   */
  public static String getCommandSet(String strBegin) {
    String cmds = "";
    Hashtable htSet = new Hashtable();
    int nCmds = 0;
    String s = (strBegin == null || strBegin.length() == 0 ? null : strBegin
        .toLowerCase());
    boolean isMultiCharacter = (s != null && s.length() > 1);
    Enumeration e = map.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Token token = (Token) map.get(name);
      if ((token.tok & Token.command) != 0
//          && (token.tok & Token.unimplemented) == 0
          && (s == null || name.indexOf(s) == 0)
          && (isMultiCharacter || ((String) token.value).equals(name)))
        htSet.put(name, Boolean.TRUE);
    }
    e = htSet.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (name.charAt(name.length() - 1) != 's'
          || !htSet.containsKey(name.substring(0, name.length() - 1)))
        cmds += (nCmds++ == 0 ? "" : ";") + name;
    }
    return cmds;
  }
/*
  public static String getSetParameters() {
    String cmds = "";
    Enumeration e = map.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Token token = (Token) map.get(name);
      if ((token.tok & Token.setparam) != 0)
        cmds +=name + "\n";
    }
    return cmds;
  }
  */
}
