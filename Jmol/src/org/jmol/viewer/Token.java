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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Vector;

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
  final public static int identifier =  1;
  final static int integer           =  2;
  final static int decimal           =  3;
  final public static int string     =  4;
  final static int seqcode           =  5;
  final static int list              =  6;
  final public static int point3f    =  7;
  final public static int point4f    =  8; 
  final private static int keyword   =  9;

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string",
    "seqcode", "list", "point", "plane", "keyword"
  };


  // TOKEN BIT FIELDS
  
  // first 9 bits are generally identifier bits
  // or bits specific to a type
  
  /* bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *                   x             command
   *                  xx             atomExpressionCommand
   *                 x x             implicitStringCommand
   *                x  x             mathExpressionCommand
   *               xx  x             flowCommand
   *              x                  noeval
   *             x                   noArgs
   *            x                    defaultON
   *                    xxxxxxxxxxxx uniqueID (may include math flags)
   * 
   *              
   * math bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *    FFFF    FFFF    FFFF    FFFF
   *           x                     expression
   *          xx                     atomproperty
   *         xxx                     mathproperty
   *        x  x                     mathfunc
   *                           xxxxx unique id
   *                         xx      minmaxmask (all)
   *                          x      min
   *                         x       max
   *                        x        comparefloatx100
   *                       x         settable
   *                    xxx          maximum number of parameters
   *                   
   * 3         2         1         0
   * 0987654321098765432109876543210
   *       x   x                     mathop
   *       x   x           x         comparator
   *                            xxxx unique id
   *                        xxxx     precedence
   *
   *                        
   * parameter bit flags:
   * 
   * 3         2         1         0
   * 0987654321098765432109876543210
   *      x    x                     predefined set
   *     x                           misc
   *    x                            setparam
   *     
   *
   */
   
  static int getPrecedence(int tok) {
    return ((tok >> 4) & 0xF);  
  }

  static int getMaxMathParams(int tokCommand) {
    return  ((tokCommand >> 9) & 0x7);
  }

  final public static int command            = (1 << 12);
  
  // the command assumes an atom expression as the first parameter
  // -- center, define, delete, display, hide, restrict, select, subset, zap
  final static int atomExpressionCommand  = (1 << 13) | command;
  
  // this implicitString flag indicates that then entire command is an implied quoted string  
  // -- echo, help, hover, javascript, label, message, pause
  final static int implicitStringCommand     = (1 << 14) | command;
  
  // this implicitExpression flag indicates that phrases surrounded 
  // by ( ) should be considered the same as { }. 
  // -- elseif, forcmd, ifcmd, print, returncmd, set, var, whilecmd
  final static int mathExpressionCommand = (1 << 15) | command;
  
  // program flow commands include:
  // -- breakcmd, continuecmd, elsecmd, elseif, end, endifcmd,
  //    forcmd, function, ifcmd, whilecmd
  final static int flowCommand        = (1 << 16) | mathExpressionCommand;
  
  // Command argument compile flags
  
  // the noeval keyword indicates that a command should be processed by the 
  // compiler but should not be passed on to Eval. 
  // "var" also takes this just to remind us that it will never appear in Eval
  // -- function, end, var
  final static int noeval         = (1 << 17);
  final static int noArgs         = (1 << 18);
  final static int defaultON      = (1 << 19);
  
  final static int expression        = (1 << 20);
  final static int atomproperty      = (1 << 21) | expression;
  final static int mathproperty      = (1 << 22) | atomproperty;
  final static int mathfunc          = (1 << 23) | expression;  
  final static int mathop            = (1 << 24) | expression;

  //
  // parameter bit flags
  //
  
  final static int predefinedset     = (1 << 25) | expression;
  final static int misc              = (1 << 26); // misc parameter
  final static int setparam          = (1 << 27); // parameter to set command
  

  final static int center       = 1 | atomExpressionCommand;
  final static int define       = 2 | atomExpressionCommand | expression | setparam;
  final static int delete       = 3 | atomExpressionCommand;
  final static int display      = 4 | atomExpressionCommand | setparam;
  final static int hide         = 5 | atomExpressionCommand;
  final static int restrict     = 6 | atomExpressionCommand;
//final static int select       see mathfunc
  final static int subset       = 7 | atomExpressionCommand | predefinedset;
  final static int zap          = 8 | atomExpressionCommand;

  final static int print        = 1 | mathExpressionCommand;
  final static int returncmd    = 2 | mathExpressionCommand;
  final static int set          = 3 | mathExpressionCommand;
  final static int var          = 4 | mathExpressionCommand | noeval | setparam;

  final static int echo         = 1 | implicitStringCommand | setparam;
  final static int help         = 2 | implicitStringCommand;
  final static int hover        = 3 | implicitStringCommand | defaultON;
//final static int javascript   see mathfunc
//final static int label        see mathfunc
  final static int message      = 4 | implicitStringCommand;
  final static int pause        = 5 | implicitStringCommand;

  //these commands control flow
  //sorry about GOTO!
//final static int function     see mathfunc
  final static int ifcmd        = 1 | flowCommand;
  final static int elseif       = 2 | flowCommand;
  final static int elsecmd      = 3 | flowCommand | noArgs;
  final static int endifcmd     = 4 | flowCommand | noArgs;
  final static int forcmd       = 5 | flowCommand;
  final static int whilecmd     = 6 | flowCommand;
  final static int breakcmd     = 7 | flowCommand | noArgs;
  final static int continuecmd  = 8 | flowCommand | noArgs;
  final static int end          = 9 | flowCommand | noeval;
  
  final static int animation    = command | 1;
  final static int axes         = command | 2 | setparam | defaultON;
  final static int backbone     = command | 3 | predefinedset | defaultON;
  final static int background   = command | 4 | setparam;
  final static int bondorder    = command | 5;
  final static int calculate    = command | 6;
  final static int cartoon      = command | 7 | defaultON;
  final static int cd           = command | 7 | implicitStringCommand;
  final static int centerAt     = command | 8;
//final static int color        see mathfunc
  final static int configuration = command | 9;
  final public static int connect = command | 10;
  final static int console      = command | 11 | defaultON;
//final static int data         see mathfunc
  final static int delay        = command | 12 | defaultON;
  final static int depth        = command | 13 | defaultON;
  final static int dipole       = command | 14;
  final public static int dots         = command | 15 | defaultON;
  final public static int draw         = command | 16;
  final static int ellipsoid    = command | 17 | defaultON;
  final static int exit         = command | 18 | noArgs;
//final static int file         see mathfunc
  final static int font         = command | 19;
  final static int frame        = command | 20;
  final static int frank        = command | 21 | setparam | defaultON;
  final static int geosurface   = command | 22 | defaultON;
  final static int gotocmd      = command | 23;
  final static int halo         = command | 24 | defaultON;
  final static int hbond        = command | 25 | setparam | expression | defaultON;
  final static int history      = command | 26 | setparam;
  final static int initialize   = command | 27 | noArgs;
  final static int invertSelected = command | 28;
  final static int isosurface   = command | 29;
  final static int lcaocartoon  = command | 30;
//final static int load         see mathfunc
  final static int loop         = command | 31 | defaultON;
  final static int meshRibbon   = command | 32 | defaultON;
  final static int minimize     = command | 33;
  final static int mo           = command | 34;
//final static int model        see mathfunc
  final static int monitor      = command | 35 | setparam | expression | defaultON;
  final static int move         = command | 36;
  final static int moveto       = command | 37;
  final static int navigate     = command | 38;
  final static int pmesh        = command | 39;
  final static int polyhedra    = command | 40;
//final static int quaternion   see mathfunc
  final static int quit         = command | 41 | noArgs;
  final static int ramachandran = command | 42 | expression;
  final static int refresh      = command | 43 | noArgs;
  final static int reset        = command | 44;
  final static int restore      = command | 45;
  final static int ribbon       = command | 46 | defaultON;
  final static int rocket       = command | 47 | defaultON;
  final static int rotate       = command | 48 | defaultON;
  final static int rotateSelected = command | 49;
  final static int save         = command | 50;
//final static int script   see mathfunc
  final static int selectionHalo = command | 51 | setparam | defaultON;
  final static int show         = command | 52;
  final static int slab         = command | 53 | defaultON;
  final static int spacefill    = command | 54 | defaultON;
  final static int spin         = command | 55 | setparam | defaultON;
  final static int ssbond       = command | 56 | setparam | defaultON;
  final static int star         = command | 57 | defaultON;
  final static int stereo       = command | 58 | defaultON;
  final static int strands      = command | 59 | setparam | defaultON;
  final static int sync         = command | 60;
  final static int trace        = command | 61 | defaultON;
  final static int translate    = command | 62;
  final static int translateSelected = command | 63;
  final public static int unitcell = command | 64 | setparam | expression | predefinedset | defaultON;
  final static int vector       = command | 65;
  final static int vibration    = command | 66;
  final static int wireframe    = command | 67 | defaultON;
  //final static int write   see mathfunc
  final static int zoom         = command | 68;
  final static int zoomTo       = command | 69;

  //
  // atom expression terms
  //
  
  final static int expressionBegin     = expression | 1;
  final static int expressionEnd       = expression | 2;

  final static int all                 = expression | 3;
  final static int altloc              = expression | 4;
  final public static int branch       = expression | 5;
  final public static int chain        = expression | 6;
  final static int colon               = expression | 7;
  final static int coord               = expression | 8;
  final static int dollarsign          = expression | 9;
  final static int dot                 = expression | 10;
  final public static int group        = expression | 11;
  final static int insertion           = expression | 12;
  final public static int isaromatic   = expression | 13;
  final static int leftbrace           = expression | 14;
  final static int none                = expression | 15;
  final public static int off                 = expression | 16; //for within(dist,false,...)
  final public static int on                  = expression | 17; //for within(dist,true,...)
  final static int rightbrace          = expression | 18;
  final static int semicolon           = expression | 19;
  final public static int sequence     = expression | 20;

  // generated by compiler:
  
  final public static int spec_alternate       = expression | 31;
  final public static int spec_atom            = expression | 32;
  final public static int spec_chain           = expression | 33;
  final public static int spec_model           = expression | 34;  // /3, /4
  final static int spec_model2                 = expression | 35;  // 1.2, 1.3
  final public static int spec_name_pattern    = expression | 36;
  final public static int spec_resid           = expression | 37;
  final public static int spec_seqcode         = expression | 38;
  final public static int spec_seqcode_range   = expression | 39;

  final static int amino                = predefinedset | 1;
  final public static int dna           = predefinedset | 2;
  final public static int hetero        = predefinedset | 3 | setparam;
  final public static int hydrogen      = predefinedset | 4 | setparam;
  final public static int nucleic       = predefinedset | 5;
  final public static int protein       = predefinedset | 6;
  final public static int purine        = predefinedset | 7;
  final public static int pyrimidine    = predefinedset | 8;
  final public static int rna           = predefinedset | 9;
  final public static int selected      = predefinedset | 10;
  final static int solvent              = predefinedset | 11 | setparam;
  final static int sidechain            = predefinedset | 12;
  final static int surface              = predefinedset | 13;
  final static int thismodel            = predefinedset | 14;

  // these next are predefined in the sense that they are known quantities
  final public static int carbohydrate    = predefinedset | 21;
  final static int clickable              = predefinedset | 22;
  final static int displayed              = predefinedset | 23;
  final static int hidden                 = predefinedset | 24;
  final public static int specialposition = predefinedset | 25;
  final public static int symmetry        = predefinedset | 26;
  final static int visible                = predefinedset | 27;

  final static int comparator       = mathop | 1 << 8;
  
  final static int leftparen    = 0 | mathop | 0 << 4;
  final static int rightparen   = 1 | mathop | 0 << 4;

  final static int comma        = 0 | mathop | 1 << 4;

  final static int leftsquare   = 0 | mathop | 2 << 4;
  final static int rightsquare  = 1 | mathop | 2 << 4;

  final static int opOr         = 0 | mathop | 3 << 4;
  final static int opXor        = 1 | mathop | 3 << 4;
  final static int opToggle     = 2 | mathop | 3 << 4;

  final static int opAnd        = 0 | mathop | 4 << 4;
  
  final static int opNot        = 0 | mathop | 5 << 4;
 
  final static int opGT         = 0 | comparator | 6 << 4;
  final static int opGE         = 1 | comparator | 6 << 4;
  final static int opLE         = 2 | comparator | 6 << 4;
  final static int opLT         = 3 | comparator | 6 << 4;
  final public static int opEQ  = 4 | comparator | 6 << 4;
  final static int opNE         = 5 | comparator | 6 << 4;
   
  final static int minus        = 0 | mathop | 7 << 4;
  final static int plus         = 1 | mathop | 7 << 4;
 
  final static int divide       = 0 | mathop | 8 << 4;
  final static int times        = 1 | mathop | 8 << 4;
  final static int percent      = 2 | mathop | 8 << 4;
  final static int leftdivide   = 3 | mathop | 8 << 4;  //   quaternion1 \ quaternion2
  
  final static int unaryMinus   = 0 | mathop | 9 << 4;
  
  final static int propselector = 1 | mathop | 10 << 4;
  
  // these atom and math properties are invoked after a ".":
  // x.atoms
  // myset.bonds
  
  // .min and .max, and .all are bitfields added to a preceding property selector
  // for example, x.atoms.max, x.atoms.all
  // .all gets incorporated as minmaxmask

  final static int minmaxmask       = 3 << 5;
  final static int min              = 1 << 5;
  final static int max              = 2 << 5;
  final static int comparefloatx100 = 1 << 7;
  final static int settable         = 1 << 8;
  
  // ___.xxx math properties and all atom properties 
  
  final public static int atoms     = 1 | mathproperty;
  final public static int bonds     = 2 | mathproperty | setparam;
  final public static int color     = 3 | mathproperty | command | setparam | settable;
  final static int identify         = 4 | mathproperty;
  final static int length           = 5 | mathproperty;
  final static int lines            = 6 | mathproperty;
  final static int size             = 7 | mathproperty;
  final public static int type      = 8 | mathproperty;
  final public static int xyz       = 9 | mathproperty | settable;
  final public static int fracXyz   =10 | mathproperty | settable;
  final public static int vibXyz    =11 | mathproperty | settable;
  final static int property         =12 | mathproperty | setparam | settable;
  final public static int boundbox  =13 | mathproperty | setparam | command | defaultON;
  final public static int adpmax    =14 | mathproperty;
  final public static int adpmin    =15 | mathproperty;
  
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
  final public static int structure     = atomproperty | 21 | command;
  final static int symop                = atomproperty | 22;
  final public static int vanderwaals   = atomproperty | 23 | settable;
  final public static int valence       = atomproperty | 24 | settable;

  final public static int atomX           = atomproperty | comparefloatx100 | 0 | settable;
  final public static int atomY           = atomproperty | comparefloatx100 | 1 | settable;
  final public static int atomZ           = atomproperty | comparefloatx100 | 2 | settable;
  final public static int fracX           = atomproperty | comparefloatx100 | 3 | settable;
  final public static int fracY           = atomproperty | comparefloatx100 | 4 | settable;
  final public static int fracZ           = atomproperty | comparefloatx100 | 5 | settable;
  final public static int partialCharge   = atomproperty | comparefloatx100 | 6 | settable;
  final static int phi                    = atomproperty | comparefloatx100 | 7;
  final static int psi                    = atomproperty | comparefloatx100 | 8;
  final public static int straightness    = atomproperty | comparefloatx100 | 9;
  final static int surfacedistance        = atomproperty | comparefloatx100 |10;
  final public static int temperature     = atomproperty | comparefloatx100 |11 | settable;
  final public static int vibX            = atomproperty | comparefloatx100 |12 | settable;
  final public static int vibY            = atomproperty | comparefloatx100 |13 | settable;
  final public static int vibZ            = atomproperty | comparefloatx100 |14 | settable;


  // mathfunc               means x = somefunc(a,b,c)
  // mathfunc|mathproperty  means x = y.somefunc(a,b,c)
  // 
  // maximum number of parameters is set by the << 9 shift
  // the min/max mask requires that the first number here must not exceed 63
  // the only other requirement is that these numbers be unique

  // xxx(a)
 
  final static int function     = 1 | 0 << 9 | mathfunc | flowCommand | noeval;

  final static int array        = 1 | 0 << 9 | mathfunc;
  final static int getproperty  = 2 | 0 << 9 | mathfunc | command;
  final static int write        = 3 | 0 << 9 | mathfunc | command;

  final static int load         = 1 | 1 << 9 | mathfunc | command;
  final static int substructure = 2 | 1 << 9 | mathfunc;
  final static int javascript   = 3 | 1 << 9 | mathfunc | implicitStringCommand;
  final static int sin          = 4 | 1 << 9 | mathfunc;
  final static int cos          = 5 | 1 << 9 | mathfunc;
  final static int sqrt         = 6 | 1 << 9 | mathfunc;

  // ___.xxx(a)
  
  // a.distance(b) is in a different set -- distance(b,c) -- because it CAN take
  // two parameters and it CAN be a dot-function (but not both together)
  
  final static int split        = 0 | 1 << 9 | mathfunc | mathproperty;
  final static int join         = 1 | 1 << 9 | mathfunc | mathproperty;
  final static int trim         = 2 | 1 << 9 | mathfunc | mathproperty;  
  final static int find         = 3 | 1 << 9 | mathfunc | mathproperty;
  final static int add          = 4 | 1 << 9 | mathfunc | mathproperty;
  final static int sub          = 5 | 1 << 9 | mathfunc | mathproperty;
  final static int mul          = 6 | 1 << 9 | mathfunc | mathproperty;
  final static int div          = 7 | 1 << 9 | mathfunc | mathproperty;
  final static int label        = 8 | 1 << 9 | mathfunc | mathproperty | implicitStringCommand | defaultON | setparam;
  
  // xxx(a,b)

  final static int random       = 1 | 2 << 9 | mathfunc;
  final static int cross        = 2 | 2 << 9 | mathfunc;
  final static int script       = 3 | 2 << 9 | mathfunc | command;

  // ___.xxx(a,b)

  // note that distance is here because it can take two forms:
  //     a.distance(b)
  // and
  //     distance(a,b)
  //so it can be a math property and it can have up to two parameters
  
  final static int distance     = 1 | 2 << 9 | mathfunc | mathproperty;
  final static int replace      = 3 | 2 << 9 | mathfunc | mathproperty;

  // xxx(a,b,c)
  
  final static int select       = 1 | 3 << 9 | mathfunc | atomExpressionCommand;
  
  // xxx(a,b,c,d)
  
  final static int angle        = 1 | 4 << 9 | mathfunc;
  final static int data         = 2 | 4 << 9 | mathfunc | command;
  final static int plane        = 3 | 4 << 9 | mathfunc;
  final static int point        = 4 | 4 << 9 | mathfunc;
  final static int quaternion   = 5 | 4 << 9 | mathfunc | command;
  final static int axisangle    = 6 | 4 << 9 | mathfunc;

  // xxx(a,b,c,d,e)
  
  final static int within           = 1 | 5 << 9 | mathfunc;
  final public static int connected = 2 | 5 << 9 | mathfunc;
  
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
  
  final public static int ambient       = setparam |  1;
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
  final static int axis         = misc |  3;
  final static int babel        = misc |  4;
  final static int back         = misc |  5;
  final public static int backlit      = misc |  6;
  final public static int bitset= misc |  7;
  final static int bondset      = misc |  8;
  final static int bottom       = misc |  9;
  final static int clear        = misc | 10;
  final static int clipboard    = misc | 11;
  final static int constraint   = misc | 12;
  final static int direction    = misc | 13;
  final static int displacement = misc | 14;
  final static int dotted       = misc | 15;
  final public static int fill         = misc | 16;
  final static int fixedtemp    = misc | 17; // color option
  final public static int front        = misc | 18;
  final public static int frontlit     = misc | 19;
  final public static int frontonly    = misc | 20;
  final public static int fullylit     = misc | 21;
  final static int image               = misc | 121;  //11.5.53
  final public static int info  = misc | 122;
  final static int ionic        = misc | 22;
  final static int jmol         = misc | 23;
  final static int last         = misc | 24;
  final static int left         = misc | 25;
  final static int mep          = misc | 26;
  final public static int mesh         = misc | 27;
  final static int mode         = misc | 28;
  final static int monomer      = misc | 29;
  final static int next         = misc | 30;
  final public static int nodots       = misc | 31;
  final public static int nofill       = misc | 32;
  final public static int nomesh       = misc | 33;
  final static int normal       = misc | 34;
  final public static int notfrontonly = misc | 35;
  final public static int notriangles  = misc | 36;
  final static int only         = misc | 136;
  final static int opaque       = misc | 37;
  final static int orientation  = misc | 38;
  final static int pdbheader    = misc | 39;
  final static int play         = misc | 40;
  final static int playrev      = misc | 41;
  final static int pointgroup   = misc | 411;
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
  final static int scale        = misc | 53;
  final static int shape        = misc | 54;
  final static int shapely      = misc | 55;
  final static int solid        = misc | 56;
  final static int spacegroup   = misc | 57;
  final static int state        = misc | 58;
  final static int top          = misc | 59;
  final static int torsion      = misc | 60;
  final static int transform    = misc | 61;
  final static int translation  = misc | 62;
  final public static int translucent  = misc | 63;
  final public static int triangles    = misc | 64;
  final static int url          = misc | 65; 
  final static int user         = misc | 66; //color option
  final static int qw           = misc | 67;


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
    
  

  final private static Object[] arrayPairs  = {
    // commands
    "animation",         new Token(animation),
    "anim",              null,
    "axes",              new Token(axes),
    "backbone",          new Token(backbone),
    "background",        new Token(background),
    "bondorder",         new Token(bondorder),
    "boundbox",          new Token(boundbox),
    "break",             new Token(breakcmd),
    "calculate",         new Token(calculate),
    "cartoon",           new Token(cartoon),
    "cartoons",          null,
    "center",            new Token(center),
    "centre",            null,
    "centerat",          new Token(centerAt),
    "color",             new Token(color),
    "colour",            null,
    "configuration",     new Token(configuration),
    "conformation",      null,
    "config",            null,
    "connect",           new Token(connect),
    "console",           new Token(console),
    "continue",          new Token(continuecmd),
    "data",              new Token(data),
    "define",            new Token(define),
    "@",                 null,
    "delay",             new Token(delay),
    "delete",            new Token(delete),
    "depth",             new Token(depth),
    "dipole",            new Token(dipole),
    "dipoles",           null,
    "cd",                new Token(cd),
    "display",           new Token(display),
    "dots",              new Token(dots),
    "draw",              new Token(draw),
    "echo",              new Token(echo),
    "ellipsoid",         new Token(ellipsoid),
    "ellipsoids",        null,
    "else",              new Token(elsecmd),
    "elseif",            new Token(elseif),
    "end",               new Token(end),
    "endif",             new Token(endifcmd),
    "exit",              new Token(exit),
    "file",              new Token(file),
    "font",              new Token(font),
    "for",               new Token(forcmd),
    "frame",             new Token(frame),
    "frames",            null,
    "frank",             new Token(frank),
    "function",          new Token(function),
    "functions",         null,
    "geosurface",        new Token(geosurface),
    "getproperty",       new Token(getproperty),
    "goto",              new Token(gotocmd),
    "halo",              new Token(halo),
    "halos",             null,
    "hbond",             new Token(hbond),
    "hbonds",            null,
    "help",              new Token(help),
    "hide",              new Token(hide),
    "history",           new Token(history),
    "hover",             new Token(hover),
    "if",                new Token(ifcmd),
    "image",             new Token(image),
    "initialize",        new Token(initialize),
    "invertSelected",    new Token(invertSelected),
    "isosurface",        new Token(isosurface),
    "javascript",        new Token(javascript),
    "label",             new Token(label),
    "labels",            null,
    "lcaocartoon",       new Token(lcaocartoon),
    "lcaocartoons",      null,
    "load",              new Token(load),
    "loop",              new Token(loop),
    "measure",           new Token(monitor),
    "measurement",       null,
    "measurements",      null,
    "measures",          null,
    "monitor",           null,
    "monitors",          null,
    "meshribbon",        new Token(meshRibbon),
    "meshribbons",       null,
    "message",           new Token(message),
    "minimize",          new Token(minimize),
    "minimization",      null,
    "mo",                new Token(mo),
    "model",             new Token(model),
    "models",            null,
    "move",              new Token(move),
    "moveto",            new Token(moveto),
    "navigate",          new Token(navigate),
    "navigation",        null,
    "pause",             new Token(pause),
    "wait",              null,
    "pmesh",             new Token(pmesh),
    "polyhedra",         new Token(polyhedra),
    "print",             new Token(print),
    "quaternion",        new Token(quaternion),
    "quaternions",       null,
    "quit",              new Token(quit),
    "ramachandran",      new Token(ramachandran),
    "rama",              null,
    "refresh",           new Token(refresh),
    "reset",             new Token(reset),
    "restore",           new Token(restore),
    "restrict",          new Token(restrict),
    "return",            new Token(returncmd),
    "ribbon",            new Token(ribbon),
    "ribbons",           null,
    "rocket",            new Token(rocket),
    "rockets",           null,
    "rotate",            new Token(rotate),
    "rotateSelected",    new Token(rotateSelected),
    "save",              new Token(save),
    "script",            new Token(script),
    "source",            null,
    "select",            new Token(select),
    "selectionHalos",    new Token(selectionHalo),
    "selectionHalo",     null,
    "set",               new Token(set),
    "show",              new Token(show),
    "slab",              new Token(slab),
    "spacefill",         new Token(spacefill),
    "cpk",               null,
    "spin",              new Token(spin),
    "ssbond",            new Token(ssbond),
    "ssbonds",           null,
    "star",              new Token(star),
    "stars",             null,
    "stereo",            new Token(stereo),
    "strand",            new Token(strands),
    "strands",           null,
    "structure",         new Token(structure),
    "_structure",        null,
    "subset",            new Token(subset),
    "synchronize",       new Token(sync),
    "sync",              null,
    "trace",             new Token(trace),
    "translate",         new Token(translate),
    "translateSelected", new Token(translateSelected),
    "unitcell",          new Token(unitcell),
    "var",               new Token(var),
    "vector",            new Token(vector),
    "vectors",           null,
    "vibration",         new Token(vibration),
    "while",             new Token(whilecmd),
    "wireframe",         new Token(wireframe),
    "write",             new Token(write),
    "zap",               new Token(zap),
    "zoom",              new Token(zoom),
    "zoomTo",            new Token(zoomTo),
                          
    //                   setparams 
    
    "bondmode",          new Token(bondmode),
    "bonds",             new Token(bonds),
    "bond",              null, 
    "fontsize",          new Token(fontsize),
    "picking",           new Token(picking),
    "pickingStyle",      new Token(pickingStyle),
    "radius",            new Token(radius),
    "scale",             new Token(scale),
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
    "axis",              new Token(axis),
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
    "only",             new Token(only),
    "opaque",           new Token(opaque),
    "partialCharge",    new Token(partialCharge),
    "phi",              new Token(phi),
    "plane",            new Token(plane),
    "play",             new Token(play),
    "playRev",          new Token(playrev),
    "point",            new Token(point),
    "pointGroup",       new Token(pointgroup),
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
    return "Token["
        + astrType[tok <= keyword ? tok : keyword]
        + "("+(tok%(1<<9))+"/0x" + Integer.toHexString(tok) + ")"
        + ((intValue == Integer.MAX_VALUE) ? "" : " intValue=" + intValue
            + "(0x" + Integer.toHexString(intValue) + ")")
        + ((value == null) ? "" : value instanceof String ? " value=\"" + value
            + "\"" : " value=" + value) + "]";
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
  
  static String[] getTokensLike(String type) {
    int attr = (type.equals("setparam") ? setparam 
        : type.equals("misc") ? misc 
        : type.equals("mathfunc") ? mathfunc : command);
    Vector v = new Vector();
    Enumeration e = map.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Token token = (Token) map.get(name);
      if (Compiler.tokAttr(token.tok, attr))
        v.add(name);
    }
    String[] a = new String[v.size()];
    for (int i = 0; i < a.length; i++)
      a[i] = (String) v.get(i);
    Arrays.sort(a);
    return a;
  }

  public static int getSettableTokFromString(String s) {
    Token token = getTokenFromName(s);
    int tok;
    if (token != null)
      return (Compiler.tokAttr((tok = token.tok), settable) 
          && !Compiler.tokAttr(tok, mathproperty) ? token.tok : nada);
    if (s.equals("x"))
      return atomX;
    else if (s.equals("y"))
      return atomY;
    else if (s.equals("z"))
      return atomZ;
    else if (s.equals("w"))
      return qw;
    return nada;
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
