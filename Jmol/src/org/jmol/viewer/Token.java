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

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Bond.BondSet;


public class Token {

  public int tok;
  public Object value;
  public int intValue = Integer.MAX_VALUE;

  public Token(int tok, int intValue, Object value) {
    this.tok = tok;
    this.intValue = intValue;
    this.value = value;
  }
 
  // a wrapper class that allows a second int value
  // implemented for bitsets that mascarade for single
  // atom values -- that index stored in intValue2
  
  public static class Token2 extends Token {
    int intValue2;
    Token2(int tok, int intValue2, Object value) {
      super(tok, value);
      this.intValue2 = intValue2;
    }
    public static int bsItem2(Object x1) {
      return (x1 instanceof Token2 ? ((Token2)x1).intValue2 : -1);
    }
  }

  final public static Token intToken(int intValue) {
    return new Token(integer, intValue);
  }

  //next two are private so that ALL tokens are either
  //integer tokens or have a value that is (more likely to be) non-null
  //null token values can cause problems in Eval.statementAsString()
  public Token(int tok) {
    this.tok = tok;
  }

  public Token(int tok, int intValue) {
    this.tok = tok;
    this.intValue = intValue;
  }

  public Token(int tok, Object value) {
    this.tok = tok;
    this.value = value;
  }

  final static int nada              =  0;
  final public static int identifier        =  1;
  final static int integer           =  2;
  final static int decimal           =  3;
  final public static int string            =  4;
  final static int seqcode           =  5;
  final static int list              =  6;
  final public static int point3f           =  7;
  final public static int point4f           =  8; 
  final static int keyword           =  9;
  final static int truefalse         = 10;

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string",
    "seqcode", "list", "point", "plane", "keyword"
  };

  // command types
  
  final static int command           = (1 <<  8);
  final static int expressionCommand = (3 <<  8); // expression command
  final static int embeddedExpression= (1 << 10); // embedded expression
  final static int specialstring     = (1 << 11); // echo, label, javascript
  // generally, the minus sign is used to denote atom ranges
  // this property is used for the commands which allow negative integers
  final static int negnums           = (1 << 12);
  // the noeval keyword indicates that a command should be processed by the 
  // compiler but should not be passed on to Eval. Use: function, end
  // "var" also takes this just to remind us that it will never appear in Eval
  final static int noeval            = (1 << 13);
  final static int flowCommand       = (1 << 14);

  // this next flag indicates that the parameters may be negative numbers
  // and that expressions such as {atomno=3} may be included. It is only used privately
  private final static int numberOrExpression = negnums | embeddedExpression; 

  // this next flag indicates that phrases surrounded by ( ) should be
  // considered the same as { }. It is for set, print, return, if, elseif, for, while
  final static int implicitExpression= (1 << 15) | numberOrExpression;

  
  
  // parameter types
  
  final static int setparam          = (1 << 16); // parameter to set command
  final static int colorparam        = (1 << 17);
  final static int misc              = (1 << 18); // misc parameter
  
  // expression terms
  
  final static int expression        = (1 << 19);
  final static int predefinedset     = (1 << 20) | expression;
  final static int atomproperty      = (1 << 21) | expression;
  final static int mathproperty      = (1 << 22) | atomproperty;
  final static int mathop            = (1 << 23) | expression;
  final static int mathfunc          = (1 << 24) | expression;  
  final public static int comparator        = (1 << 25) | expression;
  final static int settable          = (1 << 26);

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
  final public static int connect      = command |  7 | numberOrExpression | colorparam;
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
  final static int rotate       = command | 27 | numberOrExpression;
  final static int save         = command | 28;
//  final static int script       = command | 29; with mathfunc 
  final static int set          = command | 31 | implicitExpression | colorparam;
  final static int show         = command | 32;
  final static int slab         = command | 33 | numberOrExpression;
  final static int spacefill    = command | 35 | negnums;
  final static int ssbond       = command | 36 | setparam;
  final static int stereo       = command | 38 | colorparam | negnums;
  final static int strands      = command | 39 | setparam;
  final static int trace        = command | 41;
  final static int translate    = command | 42 | negnums;
  final static int wireframe    = command | 44;
  //final static int write        = command | 45; with mathfunc
  final static int zap          = command | 46 | expressionCommand;
  final static int zoom         = command | 47 | numberOrExpression;
  final static int zoomTo       = command | 48 | numberOrExpression;
  final static int initialize   = command | 49;
  // openrasmol commands
  final static int depth        = command | 50 | numberOrExpression;
  final static int star         = command | 51 | negnums;
  final static int ellipsoid    = command | 52 | negnums;
  // chime commands
  final static int delay        = command | 60;
  final static int loop         = command | 61;
  final static int move         = command | 62 | negnums;
  final static int spin         = command | 64 | setparam | numberOrExpression;
  final static int animation    = command | 67;
  final static int frame        = command | 68;
  // jmol commands
  final static int hide         = command | 79 | expressionCommand;
  final static int font         = command | 80;
  final static int hover        = command | 81 | specialstring;
  final static int vibration    = command | 82 | negnums;
  final static int vector       = command | 83 | negnums;
  final static int meshRibbon   = command | 84;
  final static int halo         = command | 85 | negnums;
  final static int rocket       = command | 86;
  final static int geosurface   = command | 87 | embeddedExpression;
  final static int moveto       = command | 88 | numberOrExpression;
  final static int bondorder    = command | 89 | negnums;
  final static int console      = command | 90;
  final static int pmesh        = command | 91;
  final static int polyhedra    = command | 92 | embeddedExpression | colorparam;
  final static int centerAt     = command | 93;
  final static int isosurface   = command | 94 | colorparam | numberOrExpression;
  final static int draw         = command | 95 | numberOrExpression | colorparam;
  final static int dipole       = command | 97 | numberOrExpression;
  final static int configuration = command | 98;
  final static int mo           = command | 99 | colorparam | negnums;
  final static int lcaocartoon  = command | 100| colorparam | numberOrExpression;
  final static int message      = command | 101 | specialstring;
  final static int translateSelected = command | 102 | negnums;
  final static int calculate    = command | 103 | embeddedExpression;
  final static int restore      = command | 104;
  final static int selectionHalo = command | 105 | setparam;
  final static int history       = command | 106 | setparam;
  final static int display       = command | 107 | setparam | expressionCommand;
  final static int subset        = command | 108 | expressionCommand | predefinedset;
  final static int axes          = command | 109 | setparam;
  final public static int unitcell      = command | 111 | setparam | expression | predefinedset;
  final static int frank         = command | 112 | setparam;
  final static int navigate      = command | 113 | numberOrExpression;
  final static int gotocmd       = command | 114;
  final static int invertSelected = command | 115 | numberOrExpression;
  final static int rotateSelected = command | 116 | numberOrExpression;
  //final static int quaternion     = command | 117 | expression; mathfunc
  final static int ramachandran   = command | 118 | expression;
  final static int sync           = command | 119;
  final static int print          = command | 120 | implicitExpression;
  final static int returncmd      = command | 121 | implicitExpression;  
  final static int var            = command | 122 | implicitExpression | noeval | setparam;
  final static int delete         = command | 123 | expressionCommand;
  final static int minimize       = command | 124 | embeddedExpression | negnums;
  
  //these commands control flow and may not be nested
  //sorry about GOTO!
  final static int function       = command | 1 | flowCommand | noeval | mathfunc;
  final static int ifcmd          = command | 2 | flowCommand | implicitExpression;
  final static int elseif         = command | 3 | flowCommand | implicitExpression;
  final static int elsecmd        = command | 4 | flowCommand;
  final static int endifcmd       = command | 5 | flowCommand;
  final static int forcmd         = command | 6 | flowCommand | implicitExpression;
  final static int whilecmd       = command | 7 | flowCommand | implicitExpression;
  final static int breakcmd       = command | 8 | flowCommand;
  final static int continuecmd    = command | 9 | flowCommand;
  final static int end            = command | 10 | flowCommand | noeval;
  
  //the following are listed with atomproperty because they must be registered as atom property names
  //final static int model           = atomproperty | 5 | command;
  //final static int file            = atomproperty | 26 | command;

  final static int expressionBegin = expression | 100;
  final static int expressionEnd   = expression | 101;

  // atom expression operators
  final static int dot                 = expression | 1;
  final static int colon               = expression | 2;
  final static int leftbrace           = expression | 3;
  final static int rightbrace          = expression | 4;
  final static int dollarsign          = expression | 5;
  final static int altloc              = expression | 6;
  final static int insertion           = expression | 7;
  final public static int group        = expression | 8;
  final public static int chain        = expression | 9;
  final public static int sequence     = expression | 10;
  final public static int branch       = expression | 11;
  final static int coord               = expression | 12;
  final static int none                = expression | 13;
  final static int semicolon           = expression | 14;
  final static int all                 = expression | 15;
  final static int off                 = expression | 16; //for within(dist,false,...)
  final static int on                  = expression | 17; //for within(dist,true,...)
  final public static int isaromatic   = expression | 18;

  // generated by compiler:
  
  final public static int spec_resid           = expression | 24;
  final public static int spec_name_pattern    = expression | 25;
  final public static int spec_seqcode         = expression | 26;
  final public static int spec_seqcode_range   = expression | 27;
  final public static int spec_chain           = expression | 28;
  final public static int spec_alternate       = expression | 29;
  final public static int spec_model           = expression | 30;  // /3, /4
  final static int spec_model2                 = expression | 31;  // 1.2, 1.3
  final public static int spec_atom            = expression | 32;

  final static int amino                = predefinedset |  0;
  final public static int hetero        = predefinedset |  1 | setparam;
  final public static int hydrogen      = predefinedset |  2 | setparam;
  final public static int selected             = predefinedset |  3;
  final static int solvent              = predefinedset |  4 | setparam;
  final static int sidechain            = predefinedset |  5;
  final public static int protein       = predefinedset |  6;
  final public static int nucleic       = predefinedset |  7;
  final public static int dna           = predefinedset |  8;
  final public static int rna           = predefinedset |  9;
  final public static int purine        = predefinedset | 10;
  final public static int pyrimidine    = predefinedset | 11;
  final static int surface              = predefinedset | 12;
  final static int thismodel            = predefinedset | 13;

  // these next are predefined in the sense that they are known quantities
  final static int visible                = predefinedset | 17;
  final static int clickable              = predefinedset | 18;
  final public static int carbohydrate    = predefinedset | 19;
  final static int hidden                 = predefinedset | 20;
  final static int displayed              = predefinedset | 21;
  final public static int symmetry        = predefinedset | 22;
  final public static int specialposition = predefinedset | 23;

  final static int prec(int tok) {
    return ((tok >> 3) & 0xF);  
  }

  // precedence is set by the << 3 shift
  
  final static int leftparen    = 0  | mathop | 0 << 3;
  final static int rightparen   = 1  | mathop | 0 << 3;

  final static int comma        = 0  | mathop | 1 << 3;

  final static int leftsquare   = 0  | mathop | 2 << 3;
  final static int rightsquare  = 1  | mathop | 2 << 3;

  final static int opOr         = 0  | mathop | 3 << 3;
  final static int opXor        = 1  | mathop | 3 << 3;
  final static int opToggle     = 2  | mathop | 3 << 3;

  final static int opAnd        = 0  | mathop | 4 << 3;
  
  final static int opNot        = 0  | mathop | 5 << 3;
 
  final static int opGT         = 0  | mathop | 6 << 3 | comparator;
  final static int opGE         = 1  | mathop | 6 << 3 | comparator;
  final static int opLE         = 2  | mathop | 6 << 3 | comparator;
  final static int opLT         = 3  | mathop | 6 << 3 | comparator;
  final public static int opEQ  = 4  | mathop | 6 << 3 | comparator;
  final static int opNE         = 5  | mathop | 6 << 3 | comparator;
   
  final static int minus        = 0  | mathop | 7 << 3;
  final static int plus         = 1  | mathop | 7 << 3;
 
  final static int divide       = 0  | mathop | 8 << 3;
  final static int times        = 1  | mathop | 8 << 3;
  final static int percent      = 2  | mathop | 8 << 3;
  final static int leftdivide   = 3  | mathop | 8 << 3;  //   quaternion1 \ quaternion2
  
  final static int unaryMinus   = 0  | mathop | 9 << 3;
  
  final static int propselector = 1  | mathop | 10 << 3;


  // these atom and math properties are invoked after a ".":
  // x.atoms
  // myset.bonds
  
  // ___.min and ___.max are bitfields added to a preceding property selector

  final static int minmaxmask = 3 << 6;
  final static int min        = 1 << 6;
  final static int max        = 2 << 6;
  
  // ___.xxx math properties and all atom properties 
  
  final public static int atoms            = 1 | mathproperty;
  final public static int bonds     = 2 | mathproperty | setparam;
  final static int color            = 3 | mathproperty | command | colorparam | setparam | numberOrExpression | settable;
  final static int identify         = 4 | mathproperty;
  final static int length           = 5 | mathproperty;
  final static int lines            = 6 | mathproperty;
  final static int size             = 7 | mathproperty;
  final static int type             = 8 | mathproperty;
  final public static int xyz       = 9 | mathproperty | settable;
  final public static int fracXyz   =10 | mathproperty | settable;
  final public static int vibXyz    =11 | mathproperty | settable;
  final static int property         =12 | mathproperty | setparam | settable;
  final public static int boundbox  =13 | mathproperty | setparam | command | numberOrExpression;
  final public static int adpmax    =14 | mathproperty;
  final public static int adpmin    =15 | mathproperty;

  final static int atompropertyfloat = atomproperty | 1 << 5;
  
  final public static int atomno        = atomproperty | 1;
  final static int atomID               = atomproperty | 2;
  final static int bondcount            = atomproperty | 3;
  final static int atomIndex            = atomproperty | 4;
  final public static int cell          = atomproperty | 5;
  final public static int element       = atomproperty | 6;
  final public static int elemno        = atomproperty | 7;
  final static int file                 = atomproperty | 8 | command;
  final public static int formalCharge  = atomproperty | 9 | setparam | settable;
  final static int groupID              = atomproperty | 10;
  final public static int model         = atomproperty | 13 | command;
  final public static int molecule      = atomproperty | 14;
  final public static int occupancy     = atomproperty | 15 | settable;
  final static int polymerLength        = atomproperty | 16;
  final static int radius               = atomproperty | 17 | setparam;
  final static int resno                = atomproperty | 18;
  final public static int site          = atomproperty | 19;
  final public static int structure     = atomproperty | 21 | command | embeddedExpression;
  final static int symop                = atomproperty | 22;
  final public static int vanderwaals   = atomproperty | 23 | settable;
  final public static int valence       = atomproperty | 24 | settable;

  final public static int atomX           = atompropertyfloat | 0 | settable;
  final public static int atomY           = atompropertyfloat | 1 | settable;
  final public static int atomZ           = atompropertyfloat | 2 | settable;
  final public static int fracX           = atompropertyfloat | 3 | settable;
  final public static int fracY           = atompropertyfloat | 4 | settable;
  final public static int fracZ           = atompropertyfloat | 5 | settable;
  final public static int partialCharge   = atompropertyfloat | 6 | settable;
  final static int phi                    = atompropertyfloat | 7;
  final static int psi                    = atompropertyfloat | 8;
  final public static int straightness    = atompropertyfloat | 9;
  final static int surfacedistance        = atompropertyfloat |10;
  final public static int temperature     = atompropertyfloat |11 | settable;
  final public static int vibX            = atompropertyfloat |12 | settable;
  final public static int vibY            = atompropertyfloat |13 | settable;
  final public static int vibZ            = atompropertyfloat |14 | settable;


  // mathfunc               means x = somefunc(a,b,c)
  // mathfunc|mathproperty  means x = y.somefunc(a,b,c)
  // 
  // maximum number of parameters is set by the << 3 shift
  // that << 3 shift means the first number here must not exceed 7
  // the only requirement is that these numbers be unique

  // xxx(a)
 
  final static int array        = 1  | 0 << 3 | mathfunc;
  final static int getproperty  = 2  | 0 << 3 | mathfunc | command | embeddedExpression;
  final static int write        = 3  | 0 << 3 | mathfunc | command;

  final static int load         = 1  | 1 << 3 | mathfunc | command | negnums;
  final static int substructure = 2  | 1 << 3 | mathfunc;
  final static int script       = 3  | 1 << 3 | mathfunc | command;
  final static int javascript   = 4  | 1 << 3 | mathfunc | command | specialstring;
  final static int sin          = 5  | 1 << 3 | mathfunc;
  final static int cos          = 6  | 1 << 3 | mathfunc;
  final static int sqrt         = 7  | 1 << 3 | mathfunc;

  // ___.xxx(a)
  
  // a.distance(b) is in a different set -- distance(b,c) -- because it CAN take
  // two parameters and it CAN be a dot-function (but not both together)
  
  final static int split        = 0  | 1 << 3 | mathfunc | mathproperty;
  final static int join         = 1  | 1 << 3 | mathfunc | mathproperty;
  final static int trim         = 2  | 1 << 3 | mathfunc | mathproperty;  
  final static int find         = 3  | 1 << 3 | mathfunc | mathproperty;
  final static int add          = 4  | 1 << 3 | mathfunc | mathproperty;
  final static int sub          = 5  | 1 << 3 | mathfunc | mathproperty;
  final static int mul          = 6  | 1 << 3 | mathfunc | mathproperty;
  final static int div          = 7  | 1 << 3 | mathfunc | mathproperty;
  final static int label        = 7  | 1 << 3 | mathfunc | mathproperty | command | specialstring | setparam;
  // two ids with 7 are ok here because the additional command bits of label distinguish it  
  // we really could put any of these in the 2 << 3 set, because this only specifies
  // a maximum number of parameters, and the actual number is check in the evaluateXXX call.
  

  // xxx(a,b)

  final static int random       = 1  | 2 << 3 | mathfunc;
  final static int data         = 2  | 2 << 3 | mathfunc | command;
  final static int cross        = 3  | 2 << 3 | mathfunc;

  // ___.xxx(a,b)

  // note that distance is here because it can take two forms:
  //     a.distance(b)
  // and
  //     distance(a,b)
  //so it can be a math property and it can have up to two parameters
  
  final static int distance     = 1  | 2 << 3 | mathfunc | mathproperty;
  final static int replace      = 3  | 2 << 3 | mathfunc | mathproperty;

  // xxx(a,b,c)
  
  final static int select       = 1  | 3 << 3 | mathfunc | expressionCommand;
  
  // xxx(a,b,c,d)
  
  final static int angle        = 1  | 4 << 3 | mathfunc;
  final static int plane        = 2  | 4 << 3 | mathfunc | specialstring; //may appear as string alone
  final static int point        = 3  | 4 << 3 | mathfunc;
  final static int quaternion   = 4  | 4 << 3 | mathfunc | command;
  final static int axisangle    = 5  | 4 << 3 | mathfunc;

  // xxx(a,b,c,d,e)
  
  final static int within           = 1  | 5 << 3 | mathfunc;
  final public static int connected = 2  | 5 << 3 | mathfunc;
  
 // math-related Token static methods
  
  final static Point3f pt0 = new Point3f();

  static Object oValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return Boolean.TRUE;
    case nada:
    case off:
      return Boolean.FALSE;
    case integer:
      return new Integer(x.intValue);
    default:
      return x.value;
    }        
  }
  
  static Object nValue(Token x) {
    int iValue = 0;
    switch (x == null ? nada : x.tok) {
      case integer:
        iValue = x.intValue;
        break;
      case decimal:
        return x.value;
      case string:
        if (((String) x.value).indexOf(".") >= 0)
          return new Float(fValue(x));
        iValue = iValue(x);
      }
    return new Integer(iValue);
  }
  
  static boolean bValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return true;
    case off:
      return false;
    case integer:
      return x.intValue != 0;
    case decimal:
    case string:
    case list:
      return fValue(x) != 0;
    case bitset:
      return iValue(x) != 0;
    case point3f:
    case point4f:
      return Math.abs(fValue(x)) > 0.0001f;
    default:
      return false;
    }
  }

  static int iValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
    case list:
    case string:
    case point3f:
    case point4f:
      return (int)fValue(x);
    case bitset:
      return BitSetUtil.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static float fValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
      return ((Float) x.value).floatValue();
    case list:
      int i = x.intValue;
      String[] list = (String[]) x.value;
      if (i == Integer.MAX_VALUE)
        return list.length;
    case string: 
      String s = sValue(x);
      if (s.equalsIgnoreCase("true"))
        return 1;
      if (s.equalsIgnoreCase("false") || s.length() == 0)
        return 0;
      return Parser.parseFloatStrict(s);
    case bitset:
      return iValue(x);
    case point3f:
      return ((Point3f) x.value).distance(pt0);
    case point4f:
      return Graphics3D.distanceToPlane((Point4f) x.value, pt0);
    default:
      return 0;
    }
  }  
  
  static String sValue(Token x) {
    if (x == null)
        return "";
    int i;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case point3f:
      return Escape.escape((Point3f) x.value);
    case point4f:
      return Escape.escape((Point4f) x.value);
    case bitset:
      return Escape.escape(bsSelect(x), !(x.value instanceof BondSet));
    case list:
      String[] list = (String[]) x.value;
      i = x.intValue;
      if (i <= 0)
        i = list.length - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > list.length ? "" : list[i - 1]);
      StringBuffer sb = new StringBuffer();
      for (i = 0; i < list.length; i++)
        sb.append(list[i]).append("\n");
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i-1);
    case decimal:
    default:
      return "" + x.value;
    }
  }

  static int sizeOf(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case off:
      return -1;
    case integer:
      return -2;
    case decimal:
      return -4;
    case point3f:
      return -8;
    case point4f:
      return -16;
    case string:
      return ((String)x.value).length();
    case list:
      return x.intValue == Integer.MAX_VALUE ? ((String[])x.value).length : sizeOf(selectItem(x));
    case bitset:
      return BitSetUtil.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static String typeOf(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case off:
      return "boolean";
    case integer:
      return "integer";
    case decimal:
      return "decimal";
    case point3f:
      return "point";
    case point4f:
      return "plane";
    case string:
      return "string";
    case list:
      return "array";
    case bitset:
      return "bitset";
    default:
      return "?";
    }
  }

  static String[] concatList(Token x1, Token x2) {
    String[] list1 = (x1.tok == list ? (String[]) x1.value : TextFormat.split(
        sValue(x1), "\n"));
    String[] list2 = (x2.tok == list ? (String[]) x2.value : TextFormat.split(
        sValue(x2), "\n"));
    String[] list = new String[list1.length + list2.length];
    int pt = 0;
    for (int i = 0; i < list1.length; i++)
      list[pt++] = list1[i];
    for (int i = 0; i < list2.length; i++)
      list[pt++] = list2[i];
    return list;
  }

  static BitSet bsSelect(Token token) {
    token = selectItem(token, Integer.MIN_VALUE);
    return (BitSet)token.value;
  }

  static BitSet bsSelect(Token token, int n) {
    token = selectItem(token);
    token = selectItem(token, 1);
    token = selectItem(token, n);
    return (BitSet)token.value;
  }

  static Token selectItem(Token tokenIn) {
    return selectItem(tokenIn, Integer.MIN_VALUE); 
  }

  static Token selectItem(Token tokenIn, int i2) {
    if (tokenIn.tok != bitset 
        && tokenIn.tok != list
        && tokenIn.tok != string)
      return tokenIn;

    // negative number is a count from the end
    
    BitSet bs = null;
    String[] st = null;
    String s =null;
    
    int i1 = tokenIn.intValue;
    if (i1 == Integer.MAX_VALUE) {
      // no selections have been made yet --
      // we just create a new token with the 
      // same bitset and now indicate either
      // the selected value or "ALL" (max_value)
      if (i2 == Integer.MIN_VALUE)
        i2 = i1;
      return new Token(tokenIn.tok, i2, tokenIn.value);
    }
    int len = 0;
    Token tokenOut = new Token(tokenIn.tok, Integer.MAX_VALUE);
    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn.value instanceof BondSet) {
        tokenOut.value = new BondSet((BitSet) tokenIn.value, ((BondSet)tokenIn.value).getAssociatedAtoms());
        bs = (BitSet) tokenOut.value;
        len = BitSetUtil.cardinalityOf(bs);
        break;
      }
      bs = BitSetUtil.copy((BitSet) tokenIn.value);
      len = (tokenIn instanceof Token2 ? 1 : BitSetUtil.cardinalityOf(bs));
      tokenOut.value = bs;
      break;
    case list:
      st = (String[]) tokenIn.value;
      len = st.length;
      break;
    case string:
      s = (String) tokenIn.value;
      len = s.length();
    }

    // "testing"[0] gives "g"
    // "testing"[-1] gives "n"
    // "testing"[3][0] gives "sting"
    // "testing"[-1][0] gives "ng"
    // "testing"[0][-2] gives just "g" as well
    if (i1 <= 0)
      i1 = len + i1;
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

    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn instanceof Token2) {
        if (i1 > 1)
          bs.clear();
        break;
      }
      len = BitSetUtil.length(bs);
      int n = 0;
        for (int j = 0; j < len; j++)
          if (bs.get(j) && (++n < i1 || n > i2))
            bs.clear(j);
      break;
    case string:
      if (i1 < 1 || i1 > len)
        tokenOut.value = "";
      else
        tokenOut.value = s.substring(i1 - 1, i2);
      break;
    case list:
      if (i1 < 1 || i1 > len || i2 > len)
        return new Token(string, "");     
      if (i2 == i1)
        return tValue(st[i1 - 1]);
      String[]list = new String[i2 - i1 + 1];
      for (int i = 0; i < list.length; i++)
        list[i] = st[i + i1 - 1];
      tokenOut.value = list;
      break;
    }
    return tokenOut;
  }

  static Token tValue(String str) {
    Object v = unescapePointOrBitsetAsToken(str);
    if (!(v instanceof String))
      return (Token) v;
    String s = (String) v;
    if (s.toLowerCase() == "true")
      return tokenOn;
    if (s.toLowerCase() == "false")
      return tokenOff;
    float f;
    if (!Float.isNaN(f = Parser.parseFloatStrict(s)))
      return (f == (int) f && s.indexOf(".") < 0 ? intToken((int)f) 
          : new Token(decimal, new Float(f)));
    return new Token(string, v);  
  }
  
  public static Object unescapePointOrBitsetAsToken(String s) {
    if (s == null || s.length() == 0)
      return s;
    Object v = s;
    if (s.charAt(0) == '{')
      v = Escape.unescapePoint(s);
    else if (s.indexOf("({") == 0 && s.indexOf("({") == s.lastIndexOf("({"))
      v = Escape.unescapeBitset(s);
    else if (s.indexOf("[{") == 0)
      v = new BondSet(Escape.unescapeBitset(s));
    if (v instanceof Point3f)
      return new Token(point3f, v);
    if (v instanceof Point4f)
      return new Token(point4f, v);
    if (v instanceof BitSet)
      return new Token(bitset, v);
    return s;
  }


  // more SET parameters
  
  final static int ambient       = setparam |  1;
  final static int bondmode      = setparam |  2;
  final static int fontsize      = setparam |  3;
  final static int picking       = setparam |  4;
  final static int specular      = setparam |  5;
  final static int specpercent   = setparam |  6;  
  final static int specpower     = setparam |  7;
  final static int specexponent  = setparam |  8;
  final static int transparent   = setparam |  9;
  final static int defaultColors = setparam | 10;
  final static int scale3d       = setparam | 11;
  final static int diffuse       = setparam | 12;
  final static int pickingStyle  = setparam | 13;

  // misc

  final static int absolute     = misc |  1;
  final static int average      = misc |  2;
  final static int babel        = misc |  4;
  final static int back         = misc |  5;
  final static int backlit      = misc |  6;
  final public static int bitset= misc |  7;
  final static int bondset      = misc |  8;
  final static int bottom       = misc |  9;
  final static int clear        = misc | 10;
  final static int clipboard    = misc | 11;
  final static int colorRGB     = misc | 12 | colorparam;
  final static int constraint   = misc | 112;
  final static int direction    = misc | 13;
  final static int displacement = misc | 14;
  final static int dotted       = misc | 15;
  final static int fill         = misc | 16;
  final static int fixedtemp    = misc | 17; // color option
  final static int front        = misc | 18;
  final static int frontlit     = misc | 19;
  final static int frontonly    = misc | 20;
  final static int fullylit     = misc | 21;
  final public static int info  = misc | 121;
  final static int ionic        = misc | 22;
  final static int jmol         = misc | 23;
  final static int last         = misc | 24;
  final static int left         = misc | 25;
  final static int mep          = misc | 26;
  final static int mesh         = misc | 27;
  final static int mode         = misc | 28;
  final static int monomer      = misc | 29;
  final static int next         = misc | 30;
  final static int nodots       = misc | 31;
  final static int nofill       = misc | 32;
  final static int nomesh       = misc | 33;
  final static int normal       = misc | 34;
  final static int notfrontonly = misc | 35;
  final static int notriangles  = misc | 36;
  final static int opaque       = misc | 37;
  final static int orientation  = misc | 38;
  final static int pdbheader    = misc | 39;
  final static int play         = misc | 40;
  final static int playrev      = misc | 41;
  final static int polymer      = misc | 42;
  final static int prev         = misc | 43;
  final static int range        = misc | 44;
  final static int rasmol       = misc | 45;
  final public static int residue= misc | 46;
  final static int resume       = misc | 47;
  final static int rewind       = misc | 48;
  final static int right        = misc | 49;
  final static int rotation     = misc | 50;
  final static int rubberband   = misc | 51;
  final static int sasurface    = misc | 52;
  final static int shape        = misc | 53;
  final static int shapely      = misc | 54;
  final static int solid        = misc | 55;
  final static int spacegroup   = misc | 56;
  final static int state        = misc | 57;
  final static int top          = misc | 58;
  final static int torsion      = misc | 59;
  final static int transform    = misc | 60;
  final static int translation  = misc | 61;
  final static int translucent  = misc | 62;
  final static int triangles    = misc | 63;
  final static int url          = misc | 64; 
  final static int user         = misc | 65; //color option
  final static int qw           = misc | 66;


  // predefined Tokens: 
  
  final static Token tokenOn  = new Token(on, 1, "on");
  final static Token tokenOff = new Token(off, 0, "off");
  final static Token tokenAll = new Token(all, "all");
  final public static Token tokenAnd = new Token(opAnd, "and");
  final public static Token tokenOr  = new Token(opOr, "or");
  final public static Token tokenComma = new Token(comma, ",");
  final static Token tokenPlus = new Token(plus, "+");
  final static Token tokenMinus = new Token(minus, "-");
  final static Token tokenTimes = new Token(times, "*");
  final static Token tokenDivide = new Token(divide, "/");

  final public static Token tokenLeftParen = new Token(leftparen, "(");
  final public static Token tokenRightParen = new Token(rightparen, ")");
  final static Token tokenArraySelector = new Token(leftsquare, "[");
 
  final public static Token tokenExpressionBegin = new Token(expressionBegin, "expressionBegin");
  final public static Token tokenExpressionEnd   = new Token(expressionEnd, "expressionEnd");
  final static Token tokenCoordinateBegin = new Token(leftbrace, "{");
  final static Token tokenCoordinateEnd   = new Token(rightbrace, "}");
  final static Token tokenSet             = new Token(set, '=', "");
  final static Token tokenSetArray        = new Token(set, '[', "");
  final static Token tokenSetProperty     = new Token(set, '.', "");
  final static Token tokenSetVar          = new Token(set, '=', "var");
    
  final static Object[] arrayPairs  = {
    // commands
    
    "animation",         new Token(animation),
    "anim",              null, 
    "axes",              new Token(axes,            varArgCount),
    "backbone",          new Token(backbone,         onDefault1),
    "background",        new Token(background,      varArgCount),
    "bondorder",         new Token(bondorder,           maxArg2),
    "boundbox",          new Token(boundbox,        varArgCount),
    "break",             new Token(breakcmd,                  0),
    "calculate",         new Token(calculate,       varArgCount),
    "cartoon",           new Token(cartoon,          onDefault1),
    "cartoons",          null,
    "center",            new Token(center,          varArgCount),
    "centre",            null,
    "centerat",          new Token(centerAt,        varArgCount),
    "color",             new Token(color,           varArgCount),
    "colour",            null,
    "configuration",     new Token(configuration,   varArgCount),
    "conformation",      null, 
    "config",            null, 
    "connect",           new Token(connect,         varArgCount),
    "console",           new Token(console,          onDefault1),
    "continue",          new Token(continuecmd,               0),
    "data",              new Token(data,                maxArg4),
    "define",            new Token(define,          varArgCount),
    "@",                 null,
    "delay",             new Token(delay,            onDefault1),
    "depth",             new Token(depth,           varArgCount),
    "dipole",            new Token(dipole,          varArgCount),
    "dipoles",           null, 
    "display",           new Token(display,         varArgCount),
    "dots",              new Token(dots,            varArgCount),
    "draw",              new Token(draw,            varArgCount),
    "echo",              new Token(echo,                maxArg1),
    "ellipsoid",         new Token(ellipsoid,       varArgCount),
    "ellipsoids",        null, 
    "else",              new Token(elsecmd,                   0),
    "elseif",            new Token(elseif,          varArgCount),
    "end",               new Token(end,             varArgCount), //varArgCount required
    "endif",             new Token(endifcmd,                  0),
    "exit",              new Token(exit,                      0),
    "file",              new Token(file,                      1),
    "font",              new Token(font,            varArgCount),
    "for",               new Token(forcmd,          varArgCount),
    "frame",             new Token(frame,           varArgCount),
    "frames",            null, 
    "frank",             new Token(frank,            onDefault1),
    "function",          new Token(function,        varArgCount), //varArgCount required
    "functions",         null,
    "geosurface",        new Token(geosurface,      varArgCount),
    "getproperty",       new Token(getproperty,     varArgCount),
    "goto",              new Token(gotocmd,                   1),
    "halo",              new Token(halo,            varArgCount),
    "halos",             null, 
    "hbond",             new Token(hbond,            onDefault1),
    "hbonds",            null,
    "help",              new Token(help,                maxArg1),
    "hide",              new Token(hide,            varArgCount),
    "history",           new Token(history,             maxArg2),
    "hover",             new Token(hover,            onDefault1),
    "if",                new Token(ifcmd,           varArgCount),
    "initialize",        new Token(initialize,                0),
    "invertSelected",    new Token(invertSelected,  varArgCount),
    "isosurface",        new Token(isosurface,      varArgCount),
    "javascript",        new Token(javascript,                1),
    "label",             new Token(label,            onDefault1),
    "labels",            null,
    "lcaocartoon",       new Token(lcaocartoon,     varArgCount),
    "lcaocartoons",      null, 
    "load",              new Token(load,            varArgCount),
    "loop",              new Token(loop,             onDefault1),
    "measure",           new Token(monitor,         varArgCount),
    "measurement",       null,
    "measurements",      null,
    "measures",          null,
    "monitor",           null,
    "monitors",          null,
    "meshribbon",        new Token(meshRibbon,       onDefault1),
    "meshribbons",       null, 
    "message",           new Token(message,                   1),
    "minimize",          new Token(minimize,        varArgCount),
    "minimization",      null,
    "mo",                new Token(mo,              varArgCount),
    "model",             new Token(model,           varArgCount),
    "models",            null, 
    "move",              new Token(move,            varArgCount),
    "moveto",            new Token(moveto,          varArgCount),
    "navigate",          new Token(navigate,        varArgCount),
    "navigation",        null, 
    "pause",             new Token(pause,               maxArg1),
    "wait",              null, 
    "pmesh",             new Token(pmesh,           varArgCount),
    "polyhedra",         new Token(polyhedra,       varArgCount),
    "print",             new Token(print,           varArgCount),
    "quaternion",        new Token(quaternion,      varArgCount),
    "quaternions",       null,
    "quit",              new Token(quit,                      0),
    "ramachandran",      new Token(ramachandran,    varArgCount),
    "rama",              null,
    "refresh",           new Token(refresh,                   0),
    "reset",             new Token(reset,               maxArg1),
    "restore",           new Token(restore,             maxArg3),
    "restrict",          new Token(restrict,        varArgCount),
    "return",            new Token(returncmd,       varArgCount),
    "ribbon",            new Token(ribbon,           onDefault1),
    "ribbons",           null, 
    "rocket",            new Token(rocket,           onDefault1),
    "rockets",           null, 
    "rotate",            new Token(rotate,          varArgCount),
    "rotateSelected",    new Token(rotateSelected,  varArgCount),
    "save",              new Token(save,                maxArg3),
    "script",            new Token(script,          varArgCount),
    "source",            null, 
    "select",            new Token(select,          varArgCount),
    "selectionHalos",    new Token(selectionHalo,    onDefault1),
    "selectionHalo",     null, 
    "set",               new Token(set,             varArgCount),
    "show",              new Token(show,                maxArg2),
    "slab",              new Token(slab,            varArgCount),
    "spacefill",         new Token(spacefill,       varArgCount),
    "cpk",               null, 
    "spin",              new Token(spin,            varArgCount),
    "ssbond",            new Token(ssbond,           onDefault1),
    "ssbonds",           null, 
    "star",              new Token(star,            varArgCount),
    "stars",             null, 
    "stereo",            new Token(stereo,          varArgCount),
    "strand",            new Token(strands,          onDefault1),
    "strands",           null, 
    "structure",         new Token(structure,       varArgCount),
    "_structure",        null,
    "subset",            new Token(subset,          varArgCount),
    "synchronize",       new Token(sync,                maxArg2),
    "sync",              null,
    "trace",             new Token(trace,            onDefault1),
    "translate",         new Token(translate,                 2),
    "translateSelected", new Token(translateSelected, varArgCount),
    "unitcell",          new Token(unitcell,        varArgCount),
    "var",               new Token(var,             varArgCount),
    "vector",            new Token(vector,              maxArg2),
    "vectors",           null, 
    "vibration",         new Token(vibration,           maxArg2),
    "while",             new Token(whilecmd,        varArgCount),
    "wireframe",         new Token(wireframe,        onDefault1),
    "write",             new Token(write,           varArgCount),
    "zap",               new Token(zap,             varArgCount),
    "zoom",              new Token(zoom,            varArgCount),
    "zoomTo",            new Token(zoomTo,          varArgCount),
                          
    //                   setparams 
    
    "bondmode",          new Token(bondmode),
    "bonds",             new Token(bonds),
    "bond",              null, 
    "fontsize",          new Token(fontsize),
    "picking",           new Token(picking),
    "pickingStyle",      new Token(pickingStyle),
    "radius",            new Token(radius),    
    "scale3D",           new Token(scale3d),
                          
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

    "atom",              new Token(atoms),
    "atoms",             null, 
    "axisangle",         new Token(axisangle),
    "orientation",       new Token(orientation),
    "pdbheader",         new Token(pdbheader),                          
    "polymer",           new Token(polymer),
    "polymers",          null,
    "residue",           new Token(residue),
    "residues",          null,
    "rotation",          new Token(rotation),
    "sequence",          new Token(sequence),
    "shape",             new Token(shape),
    "state",             new Token(state),
    "symmetry",          new Token(symmetry),
    "spaceGroup",        new Token(spacegroup),
    "transform",         new Token(transform),
    "translation",       new Token(translation),
    "url",               new Token(url),

    // atom expressions
    "(",            tokenLeftParen,
    ")",            tokenRightParen,
    "and",          tokenAnd,
    "&",            null,
    "&&",           null,
    "or",           tokenOr,
    "|",            null,
    "||",           null,
    ",",            tokenComma,
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
    ".",            new Token(dot),
    "[",            new Token(leftsquare),
    "]",            new Token(rightsquare),
    "{",            new Token(leftbrace),
    "}",            new Token(rightbrace),
    "$",            new Token(dollarsign),
    "%",            new Token(percent),
    ":",            new Token(colon),
    ";",            new Token(semicolon),
    "+",            tokenPlus,
    "-",            tokenMinus,
    "*",            tokenTimes,
    "/",            tokenDivide,
    "\\",           new Token(leftdivide),

    // misc
        
    "absolute",         new Token(absolute),
    "add",              new Token(add),
    "adpmax",           new Token(adpmax),
    "adpmin",           new Token(adpmin),
    "all",              tokenAll,
    "altloc",           new Token(altloc),
    "altlocs",          null,
    "amino",            new Token(amino),
    "angle",            new Token(angle),
    "array",            new Token(array),
    "atomID",           new Token(atomID),
    "_atomID",          null,
    "_a",               null, 
    "atomIndex",        new Token(atomIndex),
    "atomno",           new Token(atomno),
    "atomx",            new Token(atomX),
    "atomy",            new Token(atomY),
    "atomz",            new Token(atomZ),
    "average",          new Token(average),
    "babel",            new Token(babel),
    "back",             new Token(back),    
    "backlit",          new Token(backlit),
    "bondCount",        new Token(bondcount),
    "bottom",           new Token(bottom),
    "branch",           new Token(branch),
    "carbohydrate",     new Token(carbohydrate),
    "cell",             new Token(cell),
    "chain",            new Token(chain),
    "chains",           null,
    "clear",            new Token(clear),
    "clickable",        new Token(clickable),
    "clipboard",        new Token(clipboard),
    "connected",        new Token(connected),
    "constraint",       new Token(constraint),
    "coord",            new Token(coord),
    "coordinates",      null,
    "coords",           null,
    "cos",              new Token(cos),
    "cross",            new Token(cross),
    "defaultColors",    new Token(defaultColors),
    "delete",           new Token(delete),
    "direction",        new Token(direction),
    "displacement",     new Token(displacement),
    "displayed",        new Token(displayed),
    "distance",         new Token(distance),
    "div",              new Token(div),
    "DNA",              new Token(dna),
    "dotted",           new Token(dotted),
    "element",          new Token(element),
    "elemno",           new Token(elemno),
    "_e",               null,
    "fill",             new Token(fill),
    "find",             new Token(find),
    "fixedTemperature", new Token(fixedtemp),
    "formalCharge",     new Token(formalCharge),
    "charge",           null, 
    "front",            new Token(front),    
    "frontlit",         new Token(frontlit),
    "frontOnly",        new Token(frontonly),
    "fullylit",         new Token(fullylit),
    "fx",               new Token(fracX),
    "fy",               new Token(fracY),
    "fz",               new Token(fracZ),
    "fxyz",             new Token(fracXyz),
    "group",            new Token(group),
    "groups",           null,
    "groupID",          new Token(groupID),
    "_groupID",         null, 
    "_g",               null, 
    "hetero",           new Token(hetero),
    "hidden",           new Token(hidden),
    "hydrogen",         new Token(hydrogen),
    "hydrogens",        null,
    "identify",         new Token(identify),
    "ident",            null,
    "info",             new Token(info),
    "insertion",        new Token(insertion),
    "insertions",       null, 
    "ionic",            new Token(ionic),
    "isaromatic",       new Token(isaromatic),
    "Jmol",             new Token(jmol),
    "join",             new Token(join),
    "last",             new Token(last),
    "left",             new Token(left),    
    "length",           new Token(length),
    "lines",            new Token(lines),
    "list",             new Token(list),
    "max",              new Token(max),
    "mep",              new Token(mep),
    "mesh",             new Token(mesh),
    "min",              new Token(min),
    "mode",             new Token(mode),
    "molecule",         new Token(molecule),
    "molecules",        null, 
    "monomer",          new Token(monomer),
    "mul",              new Token(mul),
    "next",             new Token(next),
    "noDots",           new Token(nodots),
    "noFill",           new Token(nofill),
    "noMesh",           new Token(nomesh),
    "none",             new Token(none),
    "null",             null,
    "inherit",          null,
    "normal",           new Token(normal),
    "notFrontOnly",     new Token(notfrontonly),
    "noTriangles",      new Token(notriangles),
    "nucleic",          new Token(nucleic),
    "occupancy",        new Token(occupancy),
    "off",              tokenOff, 
    "false",            null, 
    "on",               tokenOn, 
    "true",             null,                           
    "opaque",           new Token(opaque),
    "partialCharge",    new Token(partialCharge),
    "phi",              new Token(phi),
    "plane",            new Token(plane),
    "play",             new Token(play),
    "playRev",          new Token(playrev),
    "point",            new Token(point),
    "polymerLength",    new Token(polymerLength),
    "previous",         new Token(prev),
    "prev",             null,
    "property",         new Token(property),
    "protein",          new Token(protein),
    "psi",              new Token(psi),
    "purine",           new Token(purine),
    "pyrimidine",       new Token(pyrimidine),
    "random",           new Token(random),
    "range",            new Token(range),
    "rasmol",           new Token(rasmol),
    "replace",          new Token(replace),
    "resno",            new Token(resno),
    "resume",           new Token(resume),
    "rewind",           new Token(rewind),
    "right",            new Token(right),    
    "RNA",              new Token(rna),
    "rubberband",       new Token(rubberband),
    "saSurface",        new Token(sasurface),
    "selected",         new Token(selected),
    "shapely",          new Token(shapely),
    "sidechain",        new Token(sidechain),
    "sin",              new Token(sin),
    "site",             new Token(site),
    "size",             new Token(size),
    "solid",            new Token(solid),
    "solvent",          new Token(solvent),
    "specialPosition",  new Token(specialposition),
    "sqrt",             new Token(sqrt),
    "split",            new Token(split),
    "straightness",     new Token(straightness),
    "sub",              new Token(sub),
    "substructure",     new Token(substructure),
    "surface",          new Token(surface),
    "surfaceDistance",  new Token(surfacedistance),
    "symop",            new Token(symop),
    "temperature",      new Token(temperature),
    "relativetemperature", null,
    "thisModel",        new Token(thismodel),
    "top",              new Token(top),    
    "torsion",          new Token(torsion),
    "translucent",      new Token(translucent),
    "triangles",        new Token(triangles),
    "trim",             new Token(trim),
    "type",             new Token(type),
    "user",             new Token(user),
    "valence",          new Token(valence),
    "vanderWaals",      new Token(vanderwaals),
    "vdw",              null,
    "visible",          new Token(visible),
    "vx",               new Token(vibX),
    "vy",               new Token(vibY),
    "vz",               new Token(vibZ),
    "vxyz",             new Token(vibXyz),
    "xyz",              new Token(xyz),
  };

  private static Hashtable map = new Hashtable();
  
  public static void addToken(String ident, Token token) {
    map.put(ident, token);
  }
  
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
      "(0x" + Integer.toHexString(tok)+")" +
      ((intValue == Integer.MAX_VALUE) ? "" : " intValue=" + intValue + "(0x" + Integer.toHexString(intValue) + ")") +
      ((value == null) ? "" : value instanceof String ? " value=\"" + value + "\"" : " value=" + value) + "]";
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
      if ((token.tok & command) != 0
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
      if ((token.tok & setparam) != 0)
        cmds +=name + "\n";
    }
    return cmds;
  }
  */
}
