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

import org.openscience.jmol.viewer.JmolConstants;
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
  final static int unknown           =  6;
  final static int keyword           =  7;
  final static int whitespace        =  8;
  final static int comment           =  9;
  final static int endofline         = 10;
  final static int endofstatement    = 11;

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string",
    "seqcode",  "unknown", "keyword"
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
  final static int cpk          = command | 35 | setparam | bool |negativeints;
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
  final static int spin         = command | 53 | setparam | showparam | bool;
  final static int list         = command | 54 | showparam;
  final static int display3d    = command | 55;
  final static int animation    = command | 56;

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
  //  final static int radius       = setparam | 15 | atomproperty;
  final static int shadow       = setparam | 15;
  final static int slabmode     = setparam | 16;
  // solvent
  final static int specular     = setparam | 17;
  final static int specpower    = setparam | 18;
  // ssbonds
  // stereo
  // strands
  final static int transparent  = setparam | 19;
  final static int unitcell     = setparam | 20;
  final static int vectps       = setparam | 21;
  // write

  // chime set parameters
  final static int charge       = setparam | 22;
  final static int clear        = setparam | 23;
  final static int gaussian     = setparam | 24;
  // load
  final static int mep          = setparam | 25;
  final static int mlp          = setparam | 26 | showparam;
  final static int molsurface   = setparam | 27;
  final static int debugscript  = setparam | 28;
  final static int scale3d      = setparam | 29;
  // jmol extensions
  final static int property     = setparam | 30;
  final static int diffuse      = setparam | 31;
  final static int labeloffset  = setparam | 32;
  final static int frank        = setparam | 33;

  final static int information  = showparam |  0;
  final static int phipsi       = showparam |  1;
  // center centre
  final static int ramprint     = showparam |  2;
  final static int rotation     = showparam |  3;
  // selected
  final static int group        = showparam |  4 | expression;
  final static int chain        = showparam |  5 | expression;
  final static int atom         = showparam |  6;
  final static int sequence     = showparam |  7;
  final static int symmetry     = showparam |  8;
  final static int translation  = showparam |  9;
  // zoom
  // chime show parameters
  final static int residue      = showparam | 10;
  // model
  // mlp
  // list
  // spin
  final static int all          = showparam | 11 | expression;
  final static int pdbheader    = showparam | 12 | expression;

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
  final static int slash        = expression | 15;

  final static int atomno       = atomproperty | 0;
  final static int elemno       = atomproperty | 1;
  final static int resno        = atomproperty | 2;
  final static int radius       = atomproperty | 3 | setparam;
  final static int temperature  = atomproperty | 4;
  final static int model        = atomproperty | 5 | showparam | expression;
  final static int _bondedcount = atomproperty | 6;
  final static int _groupID     = atomproperty | 7;
  final static int _atomID      = atomproperty | 8;
  final static int _structure   = atomproperty | 9;

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
  final static int user         = misc |  1; //cpk & star
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
  final static int spec_resid           = misc | 21;
  final static int spec_name_pattern    = misc | 22;
  final static int spec_number          = misc | 23;
  final static int spec_number_range    = misc | 24;
  final static int spec_chain           = misc | 25;
  final static int spec_model           = misc | 26;
  final static int spec_atom            = misc | 27;
  final static int percent      = misc | 28;
  final static int dotted       = misc | 29;
  final static int mode         = misc | 30;
  final static int direction    = misc | 31;
  final static int fps          = misc | 32;
  final static int frame        = misc | 33;

  final static int amino       = predefinedset | 0;
  final static int hetero      = predefinedset | 1 | setparam;
  final static int hydrogen    = predefinedset | 2 | setparam;
  final static int selected    = predefinedset | 3 | showparam;
  final static int solvent     = predefinedset | 4 | setparam;
  final static int sidechain   = predefinedset | 5;

  final static Token tokenOn  = new Token(on, 1, "on");
  final static Token tokenAll = new Token(all, "all");
  final static Token tokenAnd = new Token(opAnd, "and");
  final static Token tokenElemno = new Token(elemno, "elemno");

  final static String[] comparatorNames = {">", ">=", "<=", "<", "=", "!="};
  final static String[] atomPropertyNames = {
    "atomno", "elemno", "resno", "radius", "temperature", "model",
    "_bondedcount", "_groupID", "_atomID", "_structure"};

  final static Object[] arrayPairs  = {
    // commands
    "backbone",          new Token(backbone,  onDefault1, "backbone"),
    "background",        new Token(background,         1, "background"),
    "bond",              new Token(bond,     varArgCount, "bond"),
    "cartoon",           new Token(cartoon,   onDefault1, "cartoon"),
    "cartoons",          null,
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
    "cpk",               new Token(cpk,      varArgCount, "cpk"),
    "spacefill",         null,
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
    "animation",         new Token(animation,  "animation"),
    "anim",              null,

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
    "debugscript",  new Token(debugscript,     "debugscript"),
    "fps",          new Token(fps,             "fps"),
    "scale3d",      new Token(scale3d,         "scale3d"),

    // jmol extensions
    "property",     new Token(property,        "property"),
    "diffuse",      new Token(diffuse,         "diffuse"),
    "labeloffset",  new Token(labeloffset,     "labeloffset"),
    "frank",        new Token(frank,           "frank"),
  
    // show parameters
    "information",  new Token(information,     "information"),
    "info",         null,
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
    "pdbheader",    new Token(pdbheader,       "pdbheader"),

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
    "/",            new Token(slash, "/"),

    "atomno",       new Token(atomno, "atomno"),
    "elemno",       tokenElemno,
    "_e",           tokenElemno,
    "resno",        new Token(resno, "resno"),
    "temperature",  new Token(temperature, "temperature"),
    "_bondedcount", new Token(_bondedcount, "_bondedcount"),
    "_groupID",     new Token(_groupID, "_groupID"),
    "_g",           null,
    "_atomID",      new Token(_atomID, "_atomID"),
    "_a",           null,
    "_structure",   new Token(_structure, "_structure"),

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
    "%",            new Token(percent,         "%"),
    "dotted",       new Token(dotted,          "dotted"),
    "sidechain",    new Token(sidechain,       "sidechain"),
    "mode",         new Token(mode,            "mode"),

  };

  static String[] predefinitions = {
    "@at a,t",
    "@acidic asp,glu",
    "@acyclic amino&!cyclic",
    "@aliphatic ala,gly,ile,leu,val",
    "@alpha _a=1", // rasmol doc says "approximately *.CA" - whatever?
    "@amino _g<=22",
    "@aromatic his,phe,trp,tyr",
    //    "@backbone amino & _a<=3,nucleic & _a>=4 & _a<=15",
    "@backbone amino & _a<=3,nucleic & (_a>=8 & _a<=29)",
    "@mainchain backbone",
    "@basic arg,his,lys",
    "@bonded _bondedcount>0",
    "@buried ala,cys,ile,leu,met,phe,trp,val",
    "@cg c,g",
    "@charged acidic,basic",
    "@cyclic his,phe,pro,trp,tyr",
    //    "@cystine",
    "@helix _structure=3",
    //    "@hetero", handled specially
    // doc on hydrophobic is inconsistent
    // text description of hydrophobic says this
    //    "@hydrophobic ala,leu,val,ile,pro,phe,met,trp",
    // table says this
    "@hydrophobic ala,gly,ile,leu,met,phe,pro,trp,tyr,val",
    "@ions _g=69,_g=70",
    "@large arg,glu,gln,his,ile,leu,lys,met,phe,trp,tyr",
    "@ligand hetero & !solvent",
    "@medium asn,asp,cys,pro,thr,val",
    // doc is inconsistent
    // is h basic or neutral
    "@negative acidic",
    "@neutral amino&!(acidic,basic)",
    "@polar amino&!hydrophobic",
    "@positive basic",
    "@protein amino", // + common post-translational modifications ??
    // selected - special and is handled at runtime
    "@sheet _structure=2",
    "@sidechain protein and !backbone", // doc & code inconsistent
    "@base nucleic and !backbone",
    "@small ala,gly,ser",
    "@solvent _g>=69 & _g<=72", // water or ions
    "@surface !buried",
    "@turn _structure=1",
    "@water _g=69,_g=70",
    "@hoh water",

    "@nucleic _g>=23 & _g<=68",
    "@purine _g>=23 & _g<=28",
    "@pyrimidine _g>=29 & _g<=34",
    "@a _g=23,_g=24,_g>=35 & _g<=36,_g>=51 & _g<=53",
    "@c _g=29,_g=30,_g>=37 & _g<=38,_g>=60 & _g<=62",
    "@g _g=25,_g=26,_g>=39 & _g<=45,_g>=54 & _g<=56",
    "@t _g=31,_g=32,_g>=63 & _g<=65",
    "@u _g=33,_g=34,_g>=47 & _g<=50,_g>=66 & _g<=68",
    "@i _g=27,_g=28,_g>=57 & _g<=59",

    // "@hydrogen _e=1", handled specially
    "@helium _e=2",
    "@lithium _e=3",
    "@beryllium _e=4",
    "@barium _e=5",
    "@carbon _e=6",
    "@nitrogen _e=7",
    "@oxygen _e=8",
    "@fluorine _e=9",
    "@neon _e=10",
    "@sodium _e=11",
    "@magnesium _e=12",
    "@aluminum _e=13",  //US
    "@aluminium _e=13", //UK
    "@silicon _e=14",
    "@phosphorus _e=15",
    "@sulfur _e=16",   //US
    "@sulphur _e=16",   //UK
    "@chlorine _e=17",
    "@argon _e=18",
    "@potassium _e=19",
    "@calcium _e=20",
    "@scandium _e=21",
    "@titanium _e=22",
    "@vanadium _e=23",
    "@chromium _e=24",
    "@manganese _e=25",
    "@iron _e=26",
    "@cobolt _e=27",
    "@nickel _e=28",
    "@copper _e=29",
    "@zinc _e=30",
    "@gallium _e=31",
    "@germanium _e=32",
    "@arsenic _e=33",
    "@selenium _e=34",
    "@bromine _e=35",
    "@krypton _e=36",
    "@rubidium _e=37",
    "@strontium _e=38",
    "@yttrium _e=39",
    "@zirconium _e=40",
    "@niobioum _e=41",
    "@molybdenum _e=42",
    "@technetium _e=43",
    "@ruthenium _e=44",
    "@rhodium _e=45",
    "@palladium _e=46",
    "@silver _e=47",
    "@cadmium _e=48",
    "@indium _e=49",
    "@tin _e=50",
    "@antimony _e=51",
    "@tellurium _e=52",
    "@iodine _e=53",
    "@xenon _e=54",
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
}
