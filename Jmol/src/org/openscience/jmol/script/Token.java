/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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

package org.openscience.jmol.script;

import java.util.Hashtable;

class Token {

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
  final static int predefinedset     = (1 << 16) | expression;
  final static int colorparam        = (1 << 17);
  final static int specialstring     = (1 << 18); // load and echo
  // generally, the minus sign is used to denote atom ranges
  // this property is used for the few commands which allow negative integers
  final static int negativeints      = (1 << 19);
  // for some commands the 'set' is optional
  // so, just delete the set command from the token list
  // but not for hbonds nor ssbonds
  final static int setspecial        = (1 << 20);
  final static int aminoacidset      = (1 << 21) | expression;

  final static int varArgCount     = (1 << 22);
  final static int onDefault1      = (1 << 23) | 1;
  final static int setDefaultOn    = (1 << 24);

  // rasmol commands
  final static int backbone     = command |  0 | predefinedset;
  final static int background   = command |  1 | colorparam | setspecial;
  final static int bond         = command |  2 | bool;
  final static int cartoon      = command |  3 | setspecial;
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
  final static int label        = command | 15 | bool;
  // FIXME -- why did I have load tagged as a setparam?
  final static int load         = command | 16 | specialstring; // setparam;
  final static int molecule     = command | 17;
  final static int monitor      = command | 18 | setspecial | bool;
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
  final static int spacefill    = command | 35 | bool | negativeints;
  final static int ssbonds      = command | 36 | setparam | bool;
  final static int star         = command | 37 | bool;
  final static int stereo       = command | 38
    | setspecial | bool | negativeints;
  final static int strands      = command | 39 | setspecial | bool;
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
  final static int within       = expression | 12;
  final static int plus         = expression | 13;
  final static int pick         = expression | 14;

  final static int atomno       = atomproperty | 0;
  final static int elemno       = atomproperty | 1;
  final static int resno        = atomproperty | 2;
  // radius;
  final static int temperature  = atomproperty | 3;
  final static int bondedcount  = atomproperty | 4;

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
  // y
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
  final static int leftsquare   = misc | 16;
  final static int rightsquare  = misc | 17;
  final static int shapely      = misc | 18;
  final static int restore      = misc | 19; // chime extended

  final static int alpha       = predefinedset |  0;
  final static int amino       = predefinedset |  1;
  final static int cystine     = predefinedset |  2;
  final static int helix       = predefinedset |  3;
  final static int hetero      = predefinedset |  4 | setparam;
  final static int hydrogen    = predefinedset |  5 | setparam;
  final static int ions        = predefinedset |  6;
  final static int ligand      = predefinedset |  7;
  final static int protein     = predefinedset |  8;
  final static int selected    = predefinedset |  9 | showparam;
  final static int sheet       = predefinedset | 10;
  final static int sidechain   = predefinedset | 11;
  final static int solvent     = predefinedset | 12 | setparam;
  final static int turn        = predefinedset | 13;
  final static int water       = predefinedset | 14;
  // amino acids
  final static int ala         = aminoacidset |  0;
  final static int arg         = aminoacidset |  1;
  final static int asn         = aminoacidset |  2;
  final static int asp         = aminoacidset |  3;
  final static int cys         = aminoacidset |  4;
  final static int glu         = aminoacidset |  5;
  final static int gln         = aminoacidset |  6;
  final static int gly         = aminoacidset |  7;
  final static int his         = aminoacidset |  8;
  final static int ile         = aminoacidset |  9;
  final static int leu         = aminoacidset | 10;
  final static int lys         = aminoacidset | 11;
  final static int met         = aminoacidset | 12;
  final static int phe         = aminoacidset | 13;
  final static int pro         = aminoacidset | 14;
  final static int ser         = aminoacidset | 15;
  final static int thr         = aminoacidset | 16;
  final static int trp         = aminoacidset | 17;
  final static int y           = aminoacidset | 18 | misc;
  final static int tyr         = y;
  final static int val         = aminoacidset | 19;

  final static int black                = colorparam |  0;
  final static int blue                 = colorparam |  1;
  final static int bluetint             = colorparam |  2;
  final static int brown                = colorparam |  3;
  final static int cyan                 = colorparam |  4;
  final static int gold                 = colorparam |  5;
  final static int grey                 = colorparam |  6;
  final static int green                = colorparam |  7;
  final static int greenblue            = colorparam |  8;
  final static int greentint            = colorparam |  9;
  final static int hotpink              = colorparam | 10;
  final static int magenta              = colorparam | 11;
  final static int orange               = colorparam | 12;
  final static int pink                 = colorparam | 13;
  final static int pinktint             = colorparam | 14;
  final static int purple               = colorparam | 15;
  final static int red                  = colorparam | 16;
  final static int redorange            = colorparam | 17;
  final static int seagreen             = colorparam | 18;
  final static int skyblue              = colorparam | 19;
  final static int violet               = colorparam | 20;
  final static int white                = colorparam | 21;
  final static int yellow               = colorparam | 22;
  final static int yellowtint           = colorparam | 23;

  final static Token tokenOn = new Token(on, 1, "on");

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
    "label",             new Token(label,    varArgCount, "label"),
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
  
    // show parameters
    "information",  new Token(information,     "information"),
    "phipsi",       new Token(phipsi,          "phipsi"),
    "ramprint",     new Token(ramprint,        "ramprint"),
    "rotation",     new Token(rotation,        "rotation"),
    "group",        new Token(group,           "group"),
    "chain",        new Token(chain,           "chain"),
    "atom",         new Token(atom,            "atom"),
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
    "and",          new Token(opAnd, "and"),
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
    "atomno",       new Token(atomno, "atomno"),
    "elemno",       new Token(elemno, "elemno"),
    "resno",        new Token(resno, "resno"),
    "temperature",  new Token(temperature, "temperature"),
    "_bondedcount", new Token(bondedcount, "_bondedcount"),

    "off",          new Token(off, 0, "off"),
    "false",        null,
    "no",           null,
    "on",           tokenOn,
    "true",         null,
    "yes",          null,

    "dash",         new Token(dash, "dash"),
    "user",         new Token(user, "user"),
    "x",            new Token(x, "x"),
    // y
    "z",            new Token(z, "z"),
    "all",          new Token(all, "all"),
    "*",            null,
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
  
    "alpha",        new Token(alpha,           "alpha"),
    "amino",        new Token(amino,           "amino"),
    "cystine",      new Token(cystine,         "cystine"),
    "helix",        new Token(helix,           "helix"),
    "hetero",       new Token(hetero,          "hetero"),
    "hydrogen",     new Token(hydrogen,        "hydrogen"),
    "ions",         new Token(ions,            "ions"),
    "ligand",       new Token(ligand,          "ligand"),
    "protein",      new Token(protein,         "protein"),
    "selected",     new Token(selected,        "selected"),
    "sheet",        new Token(sheet,           "sheet"),
    "sidechain",    new Token(sidechain,       "sidechain"),
    "solvent",      new Token(solvent,         "solvent"),
    "turn",         new Token(turn,            "turn"),
    "water",        new Token(water,           "water"),

    "ala",       new Token(ala,                "ala"),
    "a",         null,
    "arg",       new Token(arg,                "arg"),
    "r",         null,
    "asn",       new Token(asn,                "asn"),
    "n",         null,
    "asp",       new Token(asp,                "asp"),
    "d",         null,
    "cys",       new Token(cys,                "cys"),
    "c",         null,
    "glu",       new Token(glu,                "glu"),
    "e",         null,
    "gln",       new Token(gln,                "gln"),
    "q",         null,
    "gly",       new Token(gly,                "gly"),
    "g",         null,
    "his",       new Token(his,                "his"),
    "h",         null,
    "ile",       new Token(ile,                "ile"),
    "i",         null,
    "leu",       new Token(leu,                "leu"),
    "l",         null,
    "lys",       new Token(lys,                "lys"),
    "k",         null,
    "met",       new Token(met,                "met"),
    "m",         null,
    "phe",       new Token(phe,                "phe"),
    "f",         null,
    "pro",       new Token(pro,                "pro"),
    "p",         null,
    "ser",       new Token(ser,                "ser"),
    "s",         null,
    "thr",       new Token(thr,                "thr"),
    "t",         null,
    "trp",       new Token(trp,                "trp"),
    "w",         null,
    "tyr",       new Token(tyr,                "tyr"),
    "y",         new Token(y,                  "y"),
    "val",       new Token(val,                "val"),
    "v",         null,


    "black",      new Token(black,      0x000000, "black"),
    "blue",       new Token(blue,       0x0000FF, "blue"),
    "bluetint",   new Token(bluetint,   0xAFD7FF, "bluetint"),
    "brown",      new Token(brown,      0xAF7559, "brown"),
    "cyan",       new Token(cyan,       0x00FFFF, "cyan"),
    "gold",       new Token(gold,       0xFC9C00, "gold"),
    "grey",       new Token(grey,       0x7D7D7D, "grey"),
    "green",      new Token(green,      0x00FF00, "green"),
    "greenblue",  new Token(greenblue,  0x2E8B57, "greenblue"),
    "greentint",  new Token(greentint,  0x98FFB3, "greentint"),
    "hotpink",    new Token(hotpink,    0xFF0065, "hotpink"),
    "magenta",    new Token(magenta,    0xFF00FF, "magenta"),
    "orange",     new Token(orange,     0xFFA500, "orange"),
    "pink",       new Token(pink,       0xFF6575, "pink"),
    "pinktint",   new Token(pinktint,   0xFFABBB, "pinktint"),
    "purple",     new Token(purple,     0xA020F0, "purple"),
    "red",        new Token(red,        0xFF0000, "red"),
    "redorange",  new Token(redorange,  0xFF4500, "redorange"),
    "seagreen",   new Token(seagreen,   0x00FA6D, "seagreen"),
    "skyblue",    new Token(skyblue,    0x3A90FF, "skyblue"),
    "violet",     new Token(violet,     0xEE82EE, "violet"),
    "white",      new Token(white,      0xFFFFFF, "white"),
    "yellow",     new Token(yellow,     0xFFFF00, "yellow"),
    "yellowtint", new Token(yellowtint, 0xF6F675, "yellowtint"),
    "[",          new Token(leftsquare,  "["),
    "]",          new Token(rightsquare, "]")
  };

  static String[] predefinitions = {
    "@at a,t",
    "@acidic d,e",
    "@acyclic a,r,n,d,c,e,q,g,i,l,k,m,s,t,v",
    "@aliphatic a,g,i,l,v",
    //    "@alpha approximatly *.CA", // whatever that means
    //    "@amino",
    "@aromatic h,f,w,y",
    "@backbone (protein or nucleic) & !sidechain",
    "@basic r,h,k",
    "@bonded _bondedcount=0",
    "@buried a,c,i,l,m,f,w,v", // doesn't seem right to me
    "@cg c,g",
    "@charged acidic,basic",
    "@cyclic h,f,p,w,y",
    //    "@cystine",
    //    "@helix",
    //    "@hetero",
    // doc on hydrophobic is inconsistent
    // text description of hydrophobic says this
    //    "@hydrophobic ala,leu,val,ile,pro,phe,met,trp",
    // table says this
    "@hydrophobic a,g,i,l,m,f,p,w,y,v",
    //    "@ions",
    "@large r,e,q,h,i,l,k,m,f,w,y",
    //    "@ligand",
    "@medium n,d,c,p,t,v",
    // doc is inconsistent
    // is h basic or neutral
    "@negative acidic",
    "@neutral a,n,c,q,g,h,i,l,m,f,p,s,t,w,y,v",
    "@nucleic a,c,g,t",
    "@polar !hydrophobic",
    "@positive basic",
    //    "@protein amino + common post-translational modifications",
    "@purine a,g",
    "@pyrimidine c,t",
    // selected - special and is handled at runtime
    //    "@sheet"
    //    "@sidechain (protein or nucleic) and !backbone",
    "@small a,g,s",
    "@solvent water,ions",
    "@surface !buried",
    //    "@turn",
    //    "@water"

    "@hydrogen elemno=1",
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
}
