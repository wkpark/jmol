/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

package org.openscience.jmol.viewer.script;

import java.util.Hashtable;

public class Token {

  int tok;
  Object value;
  int intValue = Integer.MAX_VALUE;

  Token(int tok, int intValue, Object value) {
    this.tok = tok;
    this.intValue = intValue;
    this.value = value;
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
  final static int unknown           =  5;
  final static int keyword           =  6;
  final static int whitespace        =  7;
  final static int comment           =  8;
  final static int endofline         =  9;
  final static int endofstatement    = 10;

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string", "unknown", "keyword"
  };

  final static int command           = (1 <<  8);
  final static int setparam          = (1 <<  9); // parameter to set command
  final static int showparam         = (1 << 10); // parameter to show command
  final static int bool              = (1 << 11);
  final static int misc              = (1 << 12); // misc parameter
  final static int expression        = (1 << 13);
  // every property is also valid in an expression context
  final static int atomproperty      = (1 << 14) | expression;
  // every predefined is also valid in an expression context
  final static int comparator        = (1 << 15) | expression;
  // FIXME mth 2003 07 19
  // "backbone on" is not compiling because it is also a predefinedset
  // so, I am taking a stab in the dark by not defining it as an expression
  // ... we'll see
  final static int predefinedset     = (1 << 16); // | expression;
  final static int colorparam        = (1 << 17);
  final static int specialstring     = (1 << 18); // load, echo, label
  // generally, the minus sign is used to denote atom ranges
  // this property is used for the few commands which allow negative integers
  final static int negativeints      = (1 << 19);
  // for some commands the 'set' is optional
  // so, just delete the set command from the token list
  // but not for hbonds nor ssbonds
  final static int setspecial        = (1 << 20);

  final static int varArgCount     = (1 << 22);
  final static int onDefault1      = (1 << 23) | 1;
  final static int setDefaultOn    = (1 << 24);

  // rasmol commands
  final static int backbone     = command |  0 | bool | predefinedset;
  final static int background   = command |  1 | colorparam | setspecial;
  final static int bond         = command |  2 | setparam | bool;
  final static int cartoon      = command |  3 | setparam;
  final static int center       = command |  4 | showparam | expression;
  final static int clipboard    = command |  5;
  final static int color        = command |  6 | colorparam;
  final static int connect      = command |  7 | bool;
  final static int define       = command |  9 | expression;
  final static int dots         = command | 10 | bool;
  final static int echo         = command | 11 | specialstring;
  final static int exit         = command | 12;
  final static int hbonds       = command | 13 | setparam | bool;
  final static int help         = command | 14;
  final static int label        = command | 15 | specialstring;
  // FIXME -- why did I have load tagged as a setparam?
  final static int load         = command | 16 | specialstring; // setparam;
  final static int molecule     = command | 17;
  final static int monitor      = command | 18 | setparam | bool;
  final static int pause        = command | 19;
  final static int print        = command | 20;
  final static int quit         = command | 21;
  final static int refresh      = command | 22;
  final static int renumber     = command | 23 | negativeints;
  final static int reset        = command | 24;
  final static int restrict     = command | 25 | expression;
  final static int ribbons      = command | 26 | bool;
  final static int rotate       = command | 27 | bool | negativeints;
  final static int save         = command | 28;
  final static int script       = command | 29 | specialstring;
  final static int select       = command | 30 | expression;
  final static int set          = command | 31 | bool | negativeints;
  final static int show         = command | 32;
  final static int slab         = command | 33 | bool;
  final static int spacefill    = command | 35 | setparam | bool |negativeints;
  final static int ssbonds      = command | 36 | setparam | bool;
  final static int star         = command | 37 | bool;
  final static int stereo       = command | 38
    | setspecial | bool | negativeints;
  final static int strands      = command | 39 | setparam | bool;
  final static int structure    = command | 40;
  final static int trace        = command | 41 | bool;
  final static int translate    = command | 42 | negativeints;
  final static int unbond       = command | 43;
  final static int wireframe    = command | 44 | bool;
  final static int write        = command | 45 | setparam;
  final static int zap          = command | 46;
  final static int zoom         = command | 47 | showparam | bool;
  // chime commands
  final static int delay        = command | 48;
  final static int loop         = command | 49;
  final static int move         = command | 50 | negativeints;
  final static int view         = command | 51;
  final static int spin         = command | 53 | showparam | bool;
  final static int list         = command | 54 | showparam;
  final static int display3d    = command | 55;
  // jmol commands
  final static int animate      = command | 100;

  // parameters
  final static int ambient      = setparam |  0;
  final static int axes         = setparam |  1;
  // background
  final static int backfade     = setparam |  2;
  final static int bondmode     = setparam |  3;
  final static int bonds        = setparam |  4;
  final static int boundbox     = setparam |  5;
  // cartoon
  final static int cisangle     = setparam |  6;
  final static int display      = setparam |  7;
  final static int fontsize     = setparam |  8;
  final static int fontstroke   = setparam |  9;
  // hbonds
  // hetero
  final static int hourglass    = setparam | 10;
  // hydrogen
  final static int kinemage     = setparam | 11;
  final static int menus        = setparam | 12;
  // monitor
  final static int mouse        = setparam | 13;
  final static int picking      = setparam | 14;
  final static int radius       = setparam | 15 | atomproperty;
  final static int shadow       = setparam | 16;
  final static int slabmode     = setparam | 17;
  // solvent
  final static int specular     = setparam | 18;
  final static int specpower    = setparam | 19;
  // ssbonds
  // stereo
  // strands
  final static int transparent  = setparam | 20;
  final static int unitcell     = setparam | 21;
  final static int vectps       = setparam | 22;
  // write

  // chime set parameters
  final static int charge       = setparam | 23;
  final static int clear        = setparam | 24;
  final static int gaussian     = setparam | 25;
  // load
  final static int mep          = setparam | 26;
  final static int mlp          = setparam | 27 | showparam;
  final static int molsurface   = setparam | 28;
  //
  // jmol extensions
  final static int property     = setparam | 29;

  final static int information  = showparam |  0;
  final static int phipsi       = showparam |  1;
  // center centre
  final static int ramprint     = showparam |  2;
  final static int rotation     = showparam |  3;
  // selected
  final static int group        = showparam |  4;
  final static int chain        = showparam |  5;
  final static int atom         = showparam |  6;
  final static int sequence     = showparam |  7;
  final static int symmetry     = showparam |  8;
  final static int translation  = showparam |  9;
  // zoom
  // chime show parameters
  final static int residue      = showparam | 10;
  final static int model        = showparam | 11;
  // mlp
  // list
  // spin
  final static int all                  = showparam | 13 | expression;

  // atom expression operators
  final static int leftparen    = expression |  0;
  final static int rightparen   = expression |  1;
  final static int hyphen       = expression |  2;
  final static int opAnd        = expression |  3;
  final static int opOr         = expression |  4;
  final static int opNot        = expression |  5;
  final static int within       = expression |  6;
  final static int plus         = expression |  7;
  final static int pick         = expression |  8;
  final static int asterisk     = expression |  9;
  final static int dot          = expression | 11;
  final static int leftsquare   = expression | 12;
  final static int rightsquare  = expression | 13;
  final static int colon        = expression | 14;

  final static int atomno       = atomproperty | 0;
  final static int elemno       = atomproperty | 1;
  final static int resno        = atomproperty | 2;
  // radius;
  final static int temperature  = atomproperty | 3;
  final static int _bondedcount = atomproperty | 4;
  final static int _resid       = atomproperty | 5;
  final static int _atomid      = atomproperty | 6;

  final static int opGT         = comparator |  0;
  final static int opGE         = comparator |  1;
  final static int opLE         = comparator |  2;
  final static int opLT         = comparator |  3;
  final static int opEQ         = comparator |  4;
  final static int opNE         = comparator |  5;

  // misc
  final static int off          = bool |  0;
  final static int on           = bool |  1;

  final static int dash         = misc |  0; //backbone
  final static int user         = misc |  1; //spacefill & star
  final static int x            = misc |  2;
  final static int y            = misc | 3 | predefinedset;
  final static int z            = misc |  4;
  final static int none         = misc |  5 | expression;
  final static int normal       = misc |  7;
  final static int rasmol       = misc |  8;
  final static int insight      = misc |  9;
  final static int quanta       = misc | 10;
  final static int ident        = misc | 11;
  final static int distance     = misc | 12;
  final static int angle        = misc | 13;
  final static int torsion      = misc | 14;
  final static int coord        = misc | 15;
  final static int shapely      = misc | 18;
  final static int restore      = misc | 19; // chime extended
  final static int colorRGB     = misc | 20 | colorparam;
  final static int spec_name            = misc | 21;
  final static int spec_number          = misc | 22;
  final static int spec_number_range    = misc | 23;
  final static int spec_chain           = misc | 24;
  final static int spec_model           = misc | 25;
  final static int spec_atom            = misc | 26;

  final static int amino       = predefinedset | 0;
  final static int hetero      = predefinedset | 1 | setparam;
  final static int hydrogen    = predefinedset | 2 | setparam;
  final static int selected    = predefinedset | 3 | showparam;
  final static int solvent     = predefinedset | 4 | setparam;

  final static Token tokenOn  = new Token(on, 1, "on");
  final static Token tokenAll = new Token(all, "all");
  final static Token tokenAnd = new Token(opAnd, "and");

  final static Object[] arrayPairs  = {
    // commands
    "backbone",          new Token(backbone,  onDefault1, "backbone"),
    "background",        new Token(background,         1, "background"),
    "bond",              new Token(bond,     varArgCount, "bond"),
    "cartoon",           new Token(cartoon,  varArgCount, "cartoon"),
    "center",            new Token(center,   varArgCount,  "center"),
    "centre",            null,
    "clipboard",         new Token(clipboard,          0, "clipboard"),
    "color",             new Token(color,    varArgCount, "color"),
    "colour",            null,
    "connect",           new Token(connect,  varArgCount, "connect"),
    "define",            new Token(define,   varArgCount, "define"),
    "@",                 null,
    "dots",              new Token(dots,      onDefault1, "dots"),
    "echo",              new Token(echo,     varArgCount, "echo"),
    "exit",              new Token(exit,               0, "exit"),
    "hbonds",            new Token(hbonds,    onDefault1, "hbonds"),
    "help",              new Token(help,     varArgCount, "help"),
    "label",             new Token(label,              1, "label"),
    "labels",            null,
    "load",              new Token(load,     varArgCount, "load"),
    "molecule",          new Token(molecule,           1, "molecule"),
    "monitor",           new Token(monitor,  varArgCount, "monitor"),
    "monitors",          null,
    "pause",             new Token(pause,              0, "pause"),
    "wait",              null,
    "print",             new Token(print,              0, "print"),
    "quit",              new Token(quit,               0, "quit"),
    "refresh",           new Token(refresh,            0, "refresh"),
    "renumber",          new Token(renumber,  onDefault1, "renumber"),
    "reset",             new Token(reset,              0, "reset"),
    "restrict",          new Token(restrict, varArgCount, "restrict"),
    "ribbons",           new Token(ribbons,   onDefault1, "ribbons"),
    "rotate",            new Token(rotate,   varArgCount, "rotate"),
    "save",              new Token(save,     varArgCount, "save"),
    "script",            new Token(script,             1, "script"),
    "source",            null,
    "select",            new Token(select,   varArgCount, "select"),
    "set",               new Token(set,      varArgCount, "set"),
    "show",              new Token(show,     varArgCount, "show"),
    "slab",              new Token(slab,      onDefault1, "slab"),
    "spacefill",         new Token(spacefill, onDefault1, "spacefill"),
    "cpk",               null,
    "ssbonds",           new Token(ssbonds,   onDefault1, "ssbonds"),
    "star",              new Token(star,      onDefault1, "star"),
    "stereo",            new Token(stereo,             1, "stereo"),
    "strands",           new Token(strands,   onDefault1, "strands"),
    "structure",         new Token(structure,          0, "structure"),
    "trace",             new Token(trace,     onDefault1, "trace"),
    "translate",         new Token(translate,varArgCount, "translate"),
    "unbond",            new Token(unbond,   varArgCount, "unbond"),
    "wireframe",         new Token(wireframe, onDefault1, "wireframe"),
    "write",             new Token(write,    varArgCount, "write"),
    "zap",               new Token(zap,                0, "zap"),
    "zoom",              new Token(zoom,      onDefault1, "zoom"),
  // chime commands
    "delay",             new Token(delay,     onDefault1, "delay"),
    "loop",              new Token(loop,      onDefault1, "loop"),
    "move",              new Token(move,     varArgCount, "move"),
    "view",              new Token(view,     varArgCount, "view"),
    "spin",              new Token(spin,      onDefault1, "spin"),
    "list",              new Token(list,     varArgCount, "list"),
    "display3d",         new Token(display3d,  "display3d"),
  // jmol extended commands
    "animate",           new Token(animate, "animate"),

    // setparams
    "ambient",      new Token(ambient,         "ambient"),
    "axes",         new Token(axes,            "axes"),
    "backfade",     new Token(backfade,        "backfade"),
    "bondmode",     new Token(bondmode,        "bondmode"),
    "bonds",        new Token(bonds,           "bonds"),
    "boundbox",     new Token(boundbox,        "boundbox"),
    "cisangle",     new Token(cisangle,        "cisangle"),
    "display",      new Token(display,         "display"),
    "fontsize",     new Token(fontsize,        "fontsize"),
    "fontstroke",   new Token(fontstroke,      "fontstroke"),
    // hetero
    "hourglass",    new Token(hourglass,       "hourglass"),
    // hydrogen
    "kinemage",     new Token(kinemage,        "kinemage"),
    "menus",        new Token(menus,           "menus"),
    "mouse",        new Token(mouse,           "mouse"),
    "picking",      new Token(picking,         "picking"),
    "radius",       new Token(radius,          "radius"),
    "shadow",       new Token(shadow,          "shadow"),
    "slabmode",     new Token(slabmode,        "slabmode"),
    // solvent
    "specular",     new Token(specular,        "specular"),
    "specpower",    new Token(specpower,       "specpower"),
    "transparent",  new Token(transparent,     "transparent"),
    "unitcell",     new Token(unitcell,        "unitcell"),
    "vectps",       new Token(vectps,          "vectps"),
    // chime setparams
    "charge",       new Token(charge,          "charge"),
    "clear",        new Token(clear,           "clear"),
    "gaussian",     new Token(gaussian,        "gaussian"),
    "mep",          new Token(mep,             "mep"),
    "mlp",          new Token(mlp,             "mlp"),
    "molsurface",   new Token(molsurface,      "molsurface"),

    // jmol extensions
    "property",     new Token(property,        "property"),
  
    // show parameters
    "information",  new Token(information,     "information"),
    "phipsi",       new Token(phipsi,          "phipsi"),
    "ramprint",     new Token(ramprint,        "ramprint"),
    "rotation",     new Token(rotation,        "rotation"),
    "group",        new Token(group,           "group"),
    "chain",        new Token(chain,           "chain"),
    "atom",         new Token(atom,            "atom"),
    "atoms",        null,
    "sequence",     new Token(sequence,        "sequence"),
    "symmetry",     new Token(symmetry,        "symmetry"),
    "translation",  new Token(translation,     "translation"),
    // chime show parameters
    "residue",      new Token(residue,         "residue"),
    "model",        new Token(model,           "model"),

    // atom expressions
    "(",            new Token(leftparen, "("),
    ")",            new Token(rightparen, ")"),
    "-",            new Token(hyphen, "-"),
    "and",          tokenAnd,
    "&",            null,
    "or",           new Token(opOr, "or"),
    ",",            null,
    "|",            null,
    "not",          new Token(opNot, "not"),
    "!",            null,
    "<",            new Token(opLT, "<"),
    "<=",           new Token(opLE, "<="),
    ">=",           new Token(opGE, ">="),
    ">",            new Token(opGT, ">="),
    "==",           new Token(opEQ, "=="),
    "=",            null,
    "!=",           new Token(opNE, "!="),
    "<>",           null,
    "/=",           null,
    "within",       new Token(within, "within"),
    "+",            new Token(plus, "+"),
    "pick",         new Token(pick, "pick"),
    ".",            new Token(dot, "."),
    "[",            new Token(leftsquare,  "["),
    "]",            new Token(rightsquare, "]"),
    ":",            new Token(colon, ":"),
    "atomno",       new Token(atomno, "atomno"),
    "elemno",       new Token(elemno, "elemno"),
    "resno",        new Token(resno, "resno"),
    "temperature",  new Token(temperature, "temperature"),
    "_bondedcount", new Token(_bondedcount, "_bondedcount"),
    "_resid",       new Token(_resid, "_resid"),
    "_atomid",      new Token(_atomid, "_atomid"),

    "off",          new Token(off, 0, "off"),
    "false",        null,
    "no",           null,
    "on",           tokenOn,
    "true",         null,
    "yes",          null,

    "dash",         new Token(dash, "dash"),
    "user",         new Token(user, "user"),
    "x",            new Token(x, "x"),
    "y",            new Token(y, "y"),
    "z",            new Token(z, "z"),
    "*",            new Token(asterisk, "*"),
    "all",          tokenAll,
    "none",         new Token(none, "none"),
    "normal",       new Token(normal, "normal"),
    "rasmol",       new Token(rasmol, "rasmol"),
    "insight",      new Token(insight, "insight"),
    "quanta",       new Token(quanta, "quanta"),
    "ident",        new Token(ident, "ident"),
    "distance",     new Token(distance, "distance"),
    "angle",        new Token(angle, "angle"),
    "torsion",      new Token(torsion, "torsion"),
    "coord",        new Token(coord, "coord"),
    "shapely",      new Token(shapely,         "shapely"),

    "restore",           new Token(restore,    "restore"),
  
    "amino",        new Token(amino,           "amino"),
    "hetero",       new Token(hetero,          "hetero"),
    "hydrogen",     new Token(hydrogen,        "hydrogen"),
    "hydrogens",    null,
    "selected",     new Token(selected,        "selected"),
    "solvent",      new Token(solvent,         "solvent"),

  };

  static String[] predefinitions = {
    "@at a,t",
    "@acidic d,e",
    "@acyclic a,r,n,d,c,e,q,g,i,l,k,m,s,t,v",
    "@aliphatic a,g,i,l,v",
    "@alpha _atomid=1", // rasmol doc says "approximately *.CA" - whatever?
    "@amino _resid<=23",
    "@aromatic h,f,w,y",
    "@backbone amino & _atomid<=3,nucleic & _atomid>=7 & _atomid<=18",
    "@mainchain backbone",
    "@basic r,h,k",
    "@bonded _bondedcount>0",
    "@buried a,c,i,l,m,f,w,v",
    "@cg c,g",
    "@charged acidic,basic",
    "@cyclic h,f,p,w,y",
    //    "@cystine",
    //    "@helix",
    //    "@hetero", handled specially
    // doc on hydrophobic is inconsistent
    // text description of hydrophobic says this
    //    "@hydrophobic ala,leu,val,ile,pro,phe,met,trp",
    // table says this
    "@hydrophobic a,g,i,l,m,f,p,w,y,v",
    "@ions _resid=48,_resid=49",
    "@large r,e,q,h,i,l,k,m,f,w,y",
    "@ligand hetero & !solvent",
    "@medium n,d,c,p,t,v",
    // doc is inconsistent
    // is h basic or neutral
    "@negative acidic",
    "@neutral a,n,c,q,g,h,i,l,m,f,p,s,t,w,y,v",
    "@nucleic a,c,g,t",
    "@polar !hydrophobic",
    "@positive basic",
    "@protein amino", // + common post-translational modifications ??
    "@purine a,g",
    "@pyrimidine c,t",
    // selected - special and is handled at runtime
    //    "@sheet"
    "@sidechain (protein or nucleic) and !backbone", // doc & code inconsistent
    "@small a,g,s",
    "@solvent _resid>=46 & _resid<=49", // water or ions
    "@surface !buried",
    //    "@turn",
    "@water _resid=46,_resid=47", "@hoh water",

    "@ala _resid=0", "@a ala",
    "@gly _resid=1", "@g gly",
    "@leu _resid=2", "@l leu",
    "@ser _resid=3", "@s ser",
    "@val _resid=4", "@v val",
    "@thr _resid=5", "@t thr",
    "@lys _resid=6", "@k lys",
    "@asp _resid=7", "@d asp",
    "@ile _resid=8", "@i ile",
    "@asn _resid=9", "@n asn",
    "@glu _resid=10", "@e glu",
    "@pro _resid=11", "@p pro",
    "@arg _resid=12", "@r arg",
    "@phe _resid=13", "@f phe",
    "@gln _resid=14", "@q gln",
    "@tyr _resid=15", "@y tyr",
    "@his _resid=16", "@h his",
    "@cys _resid=17", "@c cys",
    "@met _resid=18", "@m met",
    "@trp _resid=19", "@w trp",

    // "@hydrogen elemno=1", handled specially
    "@helium elemno=2",
    "@lithium elemno=3",
    "@beryllium elemno=4",
    "@barium elemno=5",
    "@carbon elemno=6",
    "@nitrogen elemno=7",
    "@oxygen elemno=8",
    "@fluorine elemno=9",
    "@neon elemno=10",
    "@sodium elemno=11",
    "@magnesium elemno=12",
    "@aluminum elemno=13",  //US
    "@aluminium elemno=13", //UK
    "@silicon elemno=14",
    "@phosphorus elemno=15",
    "@sulfur elemno=16",   //US
    "@sulphur elemno=16",   //UK
    "@chlorine elemno=17",
    "@argon elemno=18",
    "@potassium elemno=19",
    "@calcium elemno=20",
    "@scandium elemno=21",
    "@titanium elemno=22",
    "@vanadium elemno=23",
    "@chromium elemno=24",
    "@manganese elemno=25",
    "@iron elemno=26",
    "@cobolt elemno=27",
    "@nickel elemno=28",
    "@copper elemno=29",
    "@zinc elemno=30",
    "@gallium elemno=31",
    "@germanium elemno=32",
    "@arsenic elemno=33",
    "@selenium elemno=34",
    "@bromine elemno=35",
    "@krypton elemno=36",
    // that is enough for now
  };

  static Hashtable map = new Hashtable();
  static {
    Token tokenLast = null;
    String stringThis;
    Token tokenThis;
    for (int i = 0; i + 1 < arrayPairs.length; i += 2) {
      stringThis = (String) arrayPairs[i];
      tokenThis = (Token) arrayPairs[i + 1];
      if (tokenThis == null)
        tokenThis = tokenLast;
      if (map.get(stringThis) != null)
        System.out.println("duplicate token definition:" + stringThis);
      map.put(stringThis, tokenThis);
      tokenLast = tokenThis;
    }
  }

  public String toString() {
    return "Token[" + astrType[tok<=keyword ? tok : keyword] +
      "-" + tok +
      ((intValue == Integer.MAX_VALUE) ? "" : ":" + intValue) +
      ((value == null) ? "" : ":" + value) + "]";
  }

  // amino acids
  public final static byte RESID_ALA =  0;
  public final static byte RESID_GLY =  1;
  public final static byte RESID_LEU =  2;
  public final static byte RESID_SER =  3;
  public final static byte RESID_VAL =  4;
  public final static byte RESID_THR =  5;
  public final static byte RESID_LYS =  6;
  public final static byte RESID_ASP =  7;
  public final static byte RESID_ILE =  8;
  public final static byte RESID_ASN =  9;
  public final static byte RESID_GLU = 10;
  public final static byte RESID_PRO = 11;
  public final static byte RESID_ARG = 12;
  public final static byte RESID_PHE = 13;
  public final static byte RESID_GLN = 14;
  public final static byte RESID_TYR = 15;
  public final static byte RESID_HIS = 16;
  public final static byte RESID_CYS = 17;
  public final static byte RESID_MET = 18;
  public final static byte RESID_TRP = 19;

  public final static byte RESID_ASX = 20;
  public final static byte RESID_GLX = 21;
  public final static byte RESID_PCA = 22;
  public final static byte RESID_HYP = 23;

  // FIXME mth -- in the rasmol source they have are using PCA as the
  // last amino acid. What is the scoop on HYP?
  public final static byte RESID_AMINO_MAX = 24;

  // DNA Nucleotides
  public final static byte RESID_A   = 24;
  public final static byte RESID_C   = 25;
  public final static byte RESID_G   = 26;
  public final static byte RESID_T   = 27;

  public final static byte RESID_DNA_MAX = 28;

  // RNA Nucleotides
  public final static byte RESID_U   = 28;
  public final static byte RESID_PLUSU  = 29; // plus U
  public final static byte RESID_I   = 30;
  public final static byte RESID_1MA = 31;
  public final static byte RESID_5MC = 32;
  public final static byte RESID_OMC = 33; // Letter O not zero
  public final static byte RESID_1MG = 34;
  public final static byte RESID_2MG = 35;
  public final static byte RESID_M2G = 36;
  public final static byte RESID_7MG = 37;
  public final static byte RESID_OMG = 38; // letter
  public final static byte RESID_YG  = 39;
  public final static byte RESID_H2U = 40;
  public final static byte RESID_5MU = 42;
  public final static byte RESID_PSU = 43;

  public final static byte RESID_NUCLEOTIDES_LAST = 43;

  //Miscellaneous
  public final static byte RESID_UNK = 44;
  public final static byte RESID_ACE = 45;
  public final static byte RESID_FOR = 46;
  public final static byte RESID_HOH = 47;
  public final static byte RESID_DOD = 48;
  public final static byte RESID_SO4 = 49;
  public final static byte RESID_PO4 = 50;
  public final static byte RESID_NAD = 51;
  public final static byte RESID_COA = 52;
  public final static byte RESID_NAP = 53;
  public final static byte RESID_NDP = 54;

  public final static byte RESID_MAX = 55;

  private static String[] residues3 = {
    // this table taken directly from RasMol source molecule.h
    
    /*===============*/
    /*  Amino Acids  */
    /*===============*/

/* Ordered by Cumulative Frequency in Brookhaven *
 * Protein Databank, December 1991               */

          "ALA", /* 8.4% */     "GLY", /* 8.3% */
          "LEU", /* 8.0% */     "SER", /* 7.5% */
          "VAL", /* 7.1% */     "THR", /* 6.4% */
          "LYS", /* 5.8% */     "ASP", /* 5.5% */
          "ILE", /* 5.2% */     "ASN", /* 4.9% */
          "GLU", /* 4.9% */     "PRO", /* 4.4% */
          "ARG", /* 3.8% */     "PHE", /* 3.7% */
          "GLN", /* 3.5% */     "TYR", /* 3.5% */
          "HIS", /* 2.3% */     "CYS", /* 2.0% */
          "MET", /* 1.8% */     "TRP", /* 1.4% */

          "ASX", "GLX", "PCA", "HYP",

    /*===================*/
    /*  DNA Nucleotides  */
    /*===================*/
          "  A", "  C", "  G", "  T",

    /*===================*/
    /*  RNA Nucleotides  */
    /*===================*/
          "  U", " +U", "  I", "1MA", 
          "5MC", "OMC", "1MG", "2MG", 
          "M2G", "7MG", "OMG", " YG", 
          "H2U", "5MU", "PSU",

    /*=================*/
    /*  Miscellaneous  */ 
    /*=================*/
          "UNK", "ACE", "FOR", "HOH",
          "DOD", "SO4", "PO4", "NAD",
          "COA", "NAP", "NDP"  };

  private static final Hashtable htResidue = new Hashtable();
  static {
    for (int i = 0; i < residues3.length; ++i) {
      htResidue.put(residues3[i], new Integer(i));
    }
  }

  public static String getResidue3(byte resid) {
    return (resid < 0 || resid > RESID_MAX) ? "???" : residues3[resid];
  }

  public static byte getResid(String residue3) {
    Integer res = (Integer)htResidue.get(residue3);
    return (res == null) ? -1 : (byte)res.intValue();
  }

  public final static int ATOM_BACKBONE_MIN =  0;
  public final static int ATOM_BACKBONE_MAX =  3;
  public final static int ATOM_SHAPELY_MAX  =  7;
  public final static int ATOM_NUCLEIC_BACKBONE_MIN =  7;
  public final static int ATOM_NICLEIC_BACKBONE_MAX = 18;

  final static String[] atomNames = {
    "N",   // 0
    "CA",
    "C",
    "O",   // 3
    "C'",  // 4
    "OT",
    "S",
    "P",   // 7
    "O1P",
    "O2P",
    "O5*",
    "C5*",
    "C4*",
    "O4*",
    "C3*",
    "O3*",
    "C2*",
    "O2*",
    "C1*",
    "CA2", 
   "SG",
    "N1",
    "N2",
    "N3",
    "N4",
    "N6",
    "O2",
    "O4",
    "O6"
  };
  
  private static Hashtable htAtom = new Hashtable();
  static {
    for (int i = 0; i < atomNames.length; ++i) {
      htAtom.put(atomNames[i], new Integer(i));
    }
  }

  public static String getAtomName(byte atomid) {
    return (atomid < 0 || atomid >= atomNames.length)
      ? "??" : atomNames[atomid];
  }

  public static byte getAtomid(String strAtom) {
    Integer iatom = (Integer)htAtom.get(strAtom);
    return iatom == null ? -1 : (byte)iatom.intValue();
  }
}
