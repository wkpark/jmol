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

import java.io.*;
import java.util.Hashtable;
import java.util.regex.*;
import java.util.Vector;

class Token {

  int tok;
  Object value;
  int intValue = Integer.MAX_VALUE;

  static String str;
  static Matcher m;

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

  final static int nada         = 0;
  final static int identifier   = 1;
  final static int integer      = 2;
  final static int decimal      = 3;
  final static int string       = 4;
  final static int unknown      = 5;
  final static int keyword      = 6;

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

  // rasmol commands
  final static int backbone     = command |  0 | predefinedset | bool;
  final static int background   = command |  1 | colorparam | setspecial;
  final static int bond         = command |  2 | bool;
  final static int cartoon      = command |  3 | setspecial;
  final static int center       = command |  4 | showparam | expression;
  final static int clipboard    = command |  5;
  final static int color        = command |  6 | colorparam;
  final static int connect      = command |  7 | bool;
  final static int cpk          = command |  8;
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
  final static int source       = command | 34 | specialstring;
  final static int spacefill    = command | 35 | bool;
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
  final static int restore      = command | 52;
  final static int spin         = command | 53 | showparam | bool;
  final static int list         = command | 54 | showparam;
  final static int display3d    = command | 55;

  // parameters
  final static int ambient              = setparam |  0;
  final static int axes                 = setparam |  1;
  // background
  final static int backfade             = setparam |  2;
  final static int bondmode             = setparam |  3;
  final static int bonds                = setparam |  4;
  final static int boundbox             = setparam |  5;
  // cartoon
  final static int cisangle             = setparam |  6;
  final static int display              = setparam |  7;
  final static int fontsize             = setparam |  8;
  final static int fontstroke           = setparam |  9;
  // hbonds
  final static int hetero               = setparam | 10 | predefinedset;
  final static int hourglass            = setparam | 11;
  final static int hydrogen             = setparam | 12 | predefinedset;
  final static int kinemage             = setparam | 13;
  final static int menus                = setparam | 14;
  // monitor
  final static int mouse                = setparam | 15;
  final static int picking              = setparam | 16;
  final static int radius               = setparam | 17 | atomproperty;
  final static int shadow               = setparam | 18;
  final static int slabmode             = setparam | 19;
  final static int solvent              = setparam | 20 | predefinedset;
  final static int specular             = setparam | 21;
  final static int specpower            = setparam | 22;
  // ssbonds
  // stereo
  // strands
  final static int transparent          = setparam | 23;
  final static int unitcell             = setparam | 24;
  final static int vectps               = setparam | 25;
  // write

  // chime set parameters
  final static int charge               = setparam | 26;
  final static int clear                = setparam | 27;
  final static int gaussian             = setparam | 28;
  // load
  final static int mep                  = setparam | 29;
  final static int mlp                  = setparam | 30 | showparam;
  final static int molsurface           = setparam | 31;

  final static int information          = showparam |  0;
  final static int phipsi               = showparam |  1;
  // center centre
  final static int ramprint             = showparam |  2;
  final static int rotation             = showparam |  3;
  // selected
  final static int group                = showparam |  4;
  final static int chain                = showparam |  5;
  final static int atom                 = showparam |  6;
  final static int sequence             = showparam |  7;
  final static int symmetry             = showparam |  8;
  final static int translation          = showparam |  9;
  // zoom
  // chime show parameters
  final static int residue              = showparam | 10;
  final static int model                = showparam | 11;
  // mlp
  // list
  // spin
  final static int all                  = showparam | 13 | expression;

  // atom expression operators
  final static int leftparen            = expression |  0;
  final static int rightparen           = expression |  1;
  final static int hyphen               = expression |  2;
  final static int opAnd                = expression |  3;
  final static int opOr                 = expression |  4;
  final static int opNot                = expression |  5;
  final static int within               = expression | 12;
  final static int plus                 = expression | 13;
  final static int pick                 = expression | 14;

  final static int atomno               = atomproperty | 0;
  final static int elemno               = atomproperty | 1;
  final static int resno                = atomproperty | 2;
  // radius;
  final static int temperature          = atomproperty | 3;

  final static int opGT                 = comparator |  0;
  final static int opGE                 = comparator |  1;
  final static int opLE                 = comparator |  2;
  final static int opLT                 = comparator |  3;
  final static int opEQ                 = comparator |  4;
  final static int opNE                 = comparator |  5;

  final static int off                  = bool |  0;
  final static int on                   = bool |  1;

  final static int dash                 = misc |  0; //backbone
  final static int user                 = misc |  1; //spacefill & star
  final static int x                    = misc |  2;
  final static int y                    = misc |  3;
  final static int z                    = misc |  4;
  final static int none                 = misc |  5 | expression;
  final static int normal               = misc |  7;
  final static int rasmol               = misc |  8;
  final static int insight              = misc |  9;
  final static int quanta               = misc | 10;
  final static int ident                = misc | 11;
  final static int distance             = misc | 12;
  final static int angle                = misc | 13;
  final static int torsion              = misc | 14;
  final static int coord                = misc | 15;
  
  final static int at          = predefinedset |  0;
  final static int acidic      = predefinedset |  1;
  final static int acyclic     = predefinedset |  2;
  final static int aliphatic   = predefinedset |  3;
  final static int alpha       = predefinedset |  4;
  final static int amino       = predefinedset |  5;
  final static int aromatic    = predefinedset |  6;
  // backbone
  final static int basic       = predefinedset |  7;
  final static int bonded      = predefinedset |  8;
  final static int buried      = predefinedset |  9;
  final static int cg          = predefinedset | 10;
  final static int charged     = predefinedset | 11;
  final static int cyclic      = predefinedset | 12;
  final static int cystine     = predefinedset | 13;
  final static int helix       = predefinedset | 14;
  // hetero
  // hydrogen
  final static int hydrophobic = predefinedset | 15;
  final static int ions        = predefinedset | 16;
  final static int large       = predefinedset | 17;
  final static int ligand      = predefinedset | 18;
  final static int medium      = predefinedset | 19;
  final static int neutral     = predefinedset | 20;
  final static int nucleic     = predefinedset | 21;
  final static int polar       = predefinedset | 22;
  final static int protein     = predefinedset | 23;
  final static int purine      = predefinedset | 24;
  final static int pyrimidine  = predefinedset | 25;
  final static int selected    = predefinedset | 26 | showparam;
  final static int sheet       = predefinedset | 27;
  final static int sidechain   = predefinedset | 28;
  final static int small       = predefinedset | 29;
  // solvent
  final static int surface     = predefinedset | 30;
  final static int turn        = predefinedset | 31;
  final static int water       = predefinedset | 32;
  // amino acids
  final static int ala         = predefinedset | 33;
  final static int arg         = predefinedset | 34;
  final static int asn         = predefinedset | 35;
  final static int asp         = predefinedset | 36;
  final static int cys         = predefinedset | 37;
  final static int glu         = predefinedset | 38;
  final static int gln         = predefinedset | 39;
  final static int gly         = predefinedset | 40;
  final static int his         = predefinedset | 41;
  final static int ile         = predefinedset | 42;
  final static int leu         = predefinedset | 43;
  final static int lys         = predefinedset | 44;
  final static int met         = predefinedset | 45;
  final static int phe         = predefinedset | 46;
  final static int pro         = predefinedset | 47;
  final static int ser         = predefinedset | 48;
  final static int thr         = predefinedset | 49;
  final static int trp         = predefinedset | 50;
  final static int tyr         = predefinedset | 51;
  final static int val         = predefinedset | 52;

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

  final static Object[] arrayPairs  = {
    // commands
    "backbone",          new Token(backbone,   "backbone"),
    "background",        new Token(background, "background"),
    "bond",              new Token(bond,       "bond"),
    "cartoon",           new Token(cartoon,    "cartoon"),
    "center",            new Token(center,     "center"),
    "centre",            null,
    "clipboard",         new Token(clipboard,  "clipboard"),
    "color",             new Token(color,      "color"),
    "colour",            null,
    "connect",           new Token(connect,    "connect"),
    "cpk",               new Token(cpk,        "cpk"),
    "define",            new Token(define,     "define"),
    "dots",              new Token(dots,       "dots"),
    "echo",              new Token(echo,       "echo"),
    "exit",              new Token(exit,       "exit"),
    "hbonds",            new Token(hbonds,     "hbonds"),
    "help",              new Token(help,       "help"),
    "label",             new Token(label,      "label"),
    "load",              new Token(load,       "load"),
    "molecule",          new Token(molecule,   "molecule"),
    "monitor",           new Token(monitor,    "monitor"),
    "monitors",          null,
    "pause",             new Token(pause,      "pause"),
    "print",             new Token(print,      "print"),
    "quit",              new Token(quit,       "quit"),
    "refresh",           new Token(refresh,    "refresh"),
    "renumber",          new Token(renumber,   "renumber"),
    "reset",             new Token(reset,      "reset"),
    "restrict",          new Token(restrict,   "restrict"),
    "ribbons",           new Token(ribbons,    "ribbons"),
    "rotate",            new Token(rotate,     "rotate"),
    "save",              new Token(save,       "save"),
    "script",            new Token(script,     "script"),
    "select",            new Token(select,     "select"),
    "set",               new Token(set,        "set"),
    "show",              new Token(show,       "show"),
    "slab",              new Token(slab,       "slab"),
    "source",            new Token(source,     "source"),
    "spacefill",         new Token(spacefill,  "spacefill"),
    "ssbonds",           new Token(ssbonds,    "ssbonds"),
    "star",              new Token(star,       "star"),
    "stereo",            new Token(stereo,     "stereo"),
    "strands",           new Token(strands,    "strands"),
    "structure",         new Token(structure,  "structure"),
    "trace",             new Token(trace,      "trace"),
    "translate",         new Token(translate,  "translate"),
    "unbond",            new Token(unbond,     "unbond"),
    "wireframe",         new Token(wireframe,  "wireframe"),
    "write",             new Token(write,      "write"),
    "zap",               new Token(zap,        "zap"),
    "zoom",              new Token(zoom,       "zoom"),
  // chime commands
    "delay",             new Token(delay,      "delay"),
    "loop",              new Token(loop,       "loop"),
    "move",              new Token(move,       "move"),
    "view",              new Token(view,       "view"),
    "restore",           new Token(restore,    "restore"),
    "spin",              new Token(spin,       "spin"),
    "list",              new Token(list,       "list"),
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
    "hetero",       new Token(hetero,          "hetero"),
    "hourglass",    new Token(hourglass,       "hourglass"),
    "hydrogen",     new Token(hydrogen,        "hydrogen"),
    "kinemage",     new Token(kinemage,        "kinemage"),
    "menus",        new Token(menus,           "menus"),
    "mouse",        new Token(mouse,           "mouse"),
    "picking",      new Token(picking,         "picking"),
    "radius",       new Token(radius,          "radius"),
    "shadow",       new Token(shadow,          "shadow"),
    "slabmode",     new Token(slabmode,        "slabmode"),
    "solvent",      new Token(solvent,         "solvent"),
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

    "false",        new Token(off, "false"),
    "off",          null,
    "no",           null,
    "true",         new Token(on, "true"),
    "on",           null,
    "yes",          null,

    "dash",         new Token(dash, "dash"),
    "user",         new Token(user, "user"),
    "x",            new Token(x, "x"),
    "y",            new Token(y, "y"),
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
  
    "at",           new Token(at, "at"),
    "acidic",       new Token(acidic, "acidic"),
    "acyclic",      new Token(acyclic, "acyclic"),
    "aliphatic",    new Token(aliphatic, "aliphatic"),
    "alpha",        new Token(alpha, "alpha"),
    "amino",        new Token(amino, "amino"),
    "aromatic",     new Token(aromatic, "aromatic"),
    "basic",        new Token(basic, "basic"),
    "bonded",       new Token(bonded, "bonded"),
    "buried",       new Token(buried, "buried"),
    "cg",           new Token(cg, "cg"),
    "charged",      new Token(charged, "charged"),
    "cyclic",       new Token(cyclic, "cyclic"),
    "cystine",      new Token(cystine, "cystine"),
    "helix",        new Token(helix, "helix"),
    "hydrophobic",  new Token(hydrophobic, "hydrophobic"),
    "ions",         new Token(ions, "ions"),
    "large",        new Token(large, "large"),
    "ligand",       new Token(ligand, "ligand"),
    "medium",       new Token(medium, "medium"),
    "neutral",      new Token(neutral, "neutral"),
    "nucleic",      new Token(nucleic, "nucleic"),
    "polar",        new Token(polar, "polar"),
    "protein",      new Token(protein, "protein"),
    "purine",       new Token(purine, "purine"),
    "pyrimidine",   new Token(pyrimidine, "pyrimidine"),
    "selected",     new Token(selected, "selected"),
    "sheet",        new Token(sheet, "sheet"),
    "sidechain",    new Token(sidechain, "sidechain"),
    "small",        new Token(small, "small"),
    "surface",      new Token(surface, "surface"),
    "turn",         new Token(turn, "turn"),
    "water",        new Token(water, "water"),

    "ala",       new Token(ala, "ala"),
    "a",         null,
    "arg",       new Token(arg, "arg"),
    "r",         null,
    "asn",       new Token(asn, "asn"),
    "n",         null,
    "asp",       new Token(asp, "asp"),
    "d",         null,
    "cys",       new Token(cys, "cys"),
    "c",         null,
    "glu",       new Token(glu, "glu"),
    "e",         null,
    "gln",       new Token(gln, "gln"),
    "q",         null,
    "gly",       new Token(gly, "gly"),
    "g",         null,
    "his",       new Token(his, "his"),
    "h",         null,
    "ile",       new Token(ile, "ile"),
    "i",         null,
    "leu",       new Token(leu, "leu"),
    "l",         null,
    "lys",       new Token(lys, "lys"),
    "k",         null,
    "met",       new Token(met, "met"),
    "m",         null,
    "phe",       new Token(phe, "phe"),
    "f",         null,
    "pro",       new Token(pro, "pro"),
    "p",         null,
    "ser",       new Token(ser, "ser"),
    "s",         null,
    "thr",       new Token(thr, "thr"),
    "t",         null,
    "trp",       new Token(trp, "trp"),
    "w",         null,
    "tyr",       new Token(tyr, "tyr"),
    //    "y",       new Token(tyr, "y"), // what to do about this?
    "val",       new Token(val, "val"),
    "v",         null,


    "black",      new Token(black,     black     &(colorparam-1), "black"),
    "blue",       new Token(blue,      blue      &(colorparam-1), "blue"),
    "bluetint",   new Token(bluetint,  bluetint  &(colorparam-1), "bluetint"),
    "brown",      new Token(brown,     brown     &(colorparam-1), "brown"),
    "cyan",       new Token(cyan,      cyan      &(colorparam-1), "cyan"),
    "gold",       new Token(gold,      gold      &(colorparam-1), "gold"),
    "grey",       new Token(grey,      grey      &(colorparam-1), "grey"),
    "green",      new Token(green,     green     &(colorparam-1), "green"),
    "greenblue",  new Token(greenblue, greenblue &(colorparam-1), "greenblue"),
    "greentint",  new Token(greentint, greentint &(colorparam-1), "greentint"),
    "hotpink",    new Token(hotpink,   hotpink   &(colorparam-1), "hotpink"),
    "magenta",    new Token(magenta,   magenta   &(colorparam-1), "magenta"),
    "orange",     new Token(orange,    orange    &(colorparam-1), "orange"),
    "pink",       new Token(pink,      pink      &(colorparam-1), "pink"),
    "pinktint",   new Token(pinktint,  pinktint  &(colorparam-1), "pinktint"),
    "purple",     new Token(purple,    purple    &(colorparam-1), "purple"),
    "red",        new Token(red,       red       &(colorparam-1), "red"),
    "redorange",  new Token(redorange, redorange &(colorparam-1), "redorange"),
    "seagreen",   new Token(seagreen,  seagreen  &(colorparam-1), "seagreen"),
    "skyblue",    new Token(skyblue,   skyblue   &(colorparam-1), "skyblue"),
    "violet",     new Token(violet,    violet    &(colorparam-1), "violet"),
    "white",      new Token(white,     white     &(colorparam-1), "white"),
    "yellow",     new Token(yellow,    yellow    &(colorparam-1), "yellow"),
    "yellowtint", new Token(yellowtint,yellowtint&(colorparam-1), "yellowtint")
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

  final static Pattern patternLeadingWhiteSpace =
    Pattern.compile("[\\s&&[^\\r\\n]]+");
  final static Pattern patternComment =
    Pattern.compile("#[^;\\r\\n]*");
  final static Pattern patternEndOfStatement =
    Pattern.compile(";|\\r?\\n|\\r|$", Pattern.MULTILINE);
  final static Pattern patternDecimal =
    Pattern.compile("-?\\d+\\.(\\d*)?|-?\\.\\d+");
  final static Pattern patternPositiveInteger =
    Pattern.compile("\\d+");
  final static Pattern patternPossibleNegativeInteger =
    Pattern.compile("-?\\d+");
  final static Pattern patternString =
    Pattern.compile("([\"'`])(.*?)\\1");
  final static Pattern patternSpecialString =
    Pattern.compile("[^;\\r\\n]+");
  final static Pattern patternLookup =
    Pattern.compile("\\(|\\)|," +
                    "|<=|<|>=|>|==|=|!=|<>|/=" +
                    "|&|\\||!" +
                    "|\\*" +                      // select *
                    "|-" +                        // range
                    "|\\+" +                      // bond
                    "|\\?" +                      // help command
                    "|[a-zA-Z_][a-zA-Z_0-9]*"
                    );

  static boolean lookingAt(Pattern pattern, String description) {
    m = pattern.matcher(str);
    boolean looking = m.lookingAt();
    if (looking) {
      System.out.println("lookingAt:" + description + ":" + m.group() + ":");
    }
    return looking;
  }
  public static Token[][] tokenize(String strScript) throws ScriptException {
    Vector lltoken = new Vector();
    Vector ltoken = new Vector();
    Token tokenCommand = null;
    int tokCommand = nada;
    str = strScript;

    for ( ; true; str = str.substring(m.end())) {
      if (lookingAt(patternLeadingWhiteSpace, "leading whitespace"))
        continue;
      if (lookingAt(patternComment, "comment"))
        continue;
      if (lookingAt(patternEndOfStatement, "end of statement")) {
        if (tokCommand != nada) {
          System.out.println("tokCommand != nada");
          Token[] atoken = new Token[ltoken.size()];
          ltoken.copyInto(atoken);
          if ((atoken[0].tok & expression) != 0)
            atoken = compileExpression(atoken);
          lltoken.add(atoken);
          ltoken.setSize(0);
          tokCommand = nada;
        }
        if (str.length() > 0)
          continue;
        break;
      }
      if (tokCommand != nada) {
        if (lookingAt(patternString, "string")) {
          System.out.println("lookingAt end of Statement");
          ltoken.add(new Token(Token.string, m.group(2)));
          continue;
        }
        if ((tokCommand & specialstring) != 0 &&
            lookingAt(patternSpecialString, "special string")) {
          ltoken.add(new Token(Token.string, m.group()));
          continue;
        }
        if (lookingAt(patternDecimal, "decimal")) {
          double value = Float.parseFloat(m.group());
          ltoken.add(new Token(Token.decimal, new Double(value)));
          continue;
        }
        if (lookingAt(patternPositiveInteger, "positive integer") || 
            ((tokCommand & negativeints) != 0 &&
             lookingAt(patternPossibleNegativeInteger, "negative integer"))) {
          int val = Integer.parseInt(m.group());
          ltoken.add(new Token(integer, val, null));
          continue;
        }
      }
      if (lookingAt(patternLookup, "lookup")) {
        String ident = m.group().toLowerCase();
        Token token = (Token) map.get(ident);
        if (token == null)
          token = new Token(identifier, ident);
        switch (tokCommand) {
        case nada:
          tokenCommand = token;
          tokCommand = token.tok;
          if ((tokCommand & command) == 0)
            throw new ScriptException("Command expected - found:" + ident);
          break;
        case set:
          if (ltoken.size() == 1) {
            if ((token.tok & setspecial) != 0) {
              tokenCommand = token;
              tokCommand = token.tok;
              ltoken.clear();
              break;
            }
            if ((token.tok & setparam) == 0)
              throw new ScriptException("Cannot set:" + ident);
          }
          break;
        case show:
          if ((token.tok & showparam) == 0)
            throw new ScriptException("Cannot show:" + ident);
          break;
        case define:
          if ((ltoken.size() >= 2) && ((token.tok & expression) == 0))
            throw new ScriptException("Invalid expression token:" + ident);
          break;
        case center:
        case restrict:
        case select:
          if (token.tok != identifier && (token.tok & expression) == 0)
            throw new ScriptException("Invalid expression token:" + ident);
          break;
        }
        ltoken.add(token);
        continue;
      }
      throw new ScriptException(((ltoken.size() == 0)
                                   ? "Command Expected:"
                                   : "Unrecognized token:") +
                                  str.substring(0,Math.min(str.length(),32)));
    }
    Token[][] aatoken = new Token[lltoken.size()][];
    lltoken.copyInto(aatoken);
    return aatoken;
  }

  public String toString() {
    return "Token[" + astrType[tok<=keyword ? tok : keyword] +
      "-" + tok +
      ((intValue == Integer.MAX_VALUE) ? "" : ":" + intValue) +
      ((value == null) ? "" : ":" + value) + "]";
  }

  Token lookAhead(Token[] atoken, int i) {
    if (atoken.length <= i)
      return null;
    return atoken[i];
  }

  /*
    expression       :: = clauseOr

    clauseOr         ::= clauseAnd {OR clauseAnd}*

    clauseAnd        ::= clauseNot {AND clauseNot}*

    clauseNot        ::= {NOT}?  | clausePrimitive

    clausePrimitive  ::= clauseInteger |
                         clauseComparator | 
                         all | none |
                         identifier
                         ( clauseOr )

    clauseInteger    ::= integer | integer - integer

    clauseComparator ::= atomproperty comparatorop integer
  */

  public static Token[] compileExpression(Token[] atoken)
    throws ScriptException {
    int i = 1;
    if (atoken[0].tok == define)
      i = 2;
    return compileExpression(atoken, i);
  }

  static Vector ltokenPostfix = null;
  static Token[] atokenInfix;
  static int itokenInfix;
                  
  public static Token[] compileExpression(Token[] atoken, int itoken)
    throws ScriptException {
    ltokenPostfix = new Vector();
    for (int i = 0; i < itoken; ++i)
      ltokenPostfix.add(atoken[i]);
    atokenInfix = atoken;
    itokenInfix = itoken;
    clauseOr();
    if (itokenInfix != atokenInfix.length)
      throw new ScriptException("end of expression expected");
    Token[] atokenPostfix = new Token[ltokenPostfix.size()];
    ltokenPostfix.copyInto(atokenPostfix);
    System.out.println("compiled expression:");
    for (int i = 0; i < atokenPostfix.length; ++i) {
      System.out.print(" " + atokenPostfix[i]);
    }
    System.out.println("");
    return atokenPostfix;
  }

  static Token tokenNext() {
    if (itokenInfix == atokenInfix.length)
      return null;
    return atokenInfix[itokenInfix++];
  }

  static int tokPeek() {
    if (itokenInfix == atokenInfix.length)
      return 0;
    return atokenInfix[itokenInfix].tok;
  }

  static void clauseOr() throws ScriptException {
    clauseAnd();
    while (tokPeek() == opOr) {
      Token tokenOr = tokenNext();
      clauseAnd();
      ltokenPostfix.add(tokenOr);
    }
  }

  static void clauseAnd() throws ScriptException {
    clauseNot();
    while (tokPeek() == opAnd) {
      Token tokenAnd = tokenNext();
      clauseNot();
      ltokenPostfix.add(tokenAnd);
    }
  }

  static void clauseNot() throws ScriptException {
    if (tokPeek() == opNot) {
      Token tokenNot = tokenNext();
      clauseNot();
      ltokenPostfix.add(tokenNot);
    } else {
      clausePrimitive();
    }
  }

  static void clausePrimitive() throws ScriptException {
    switch (tokPeek()) {
    case integer:
      clauseInteger();
      break;
    case atomno:
    case elemno:
    case resno:
    case radius:
    case temperature:
      clauseComparator();
      break;
    case all:
    case none:
    case identifier:
      ltokenPostfix.add(tokenNext());
      break;
    case leftparen:
      tokenNext();
      clauseOr();
      if (tokPeek() != rightparen)
        throw new ScriptException("right parenthesis expected");
      tokenNext();
      break;
    default:
      throw new ScriptException("unrecognized expression token");
    }
  }

  static void clauseInteger() throws ScriptException {
    Token tokenInt1 = tokenNext();
    if (tokPeek() != hyphen) {
      ltokenPostfix.add(tokenInt1);
      return;
    }
    tokenNext();
    if (tokPeek() != integer)
      throw new ScriptException("integer expected after hyphen");
    Token tokenInt2 = tokenNext();
    int min = tokenInt1.intValue;
    int max = tokenInt2.intValue;
    if (max < min) {
      int intT = max; max = min; min = intT;
    }
    ltokenPostfix.add(new Token(hyphen, min, new Integer(max)));
  }

  static void clauseComparator() throws ScriptException {
    Token tokenAtomProperty = tokenNext();
    if ((tokPeek() & comparator) == 0)
      throw new ScriptException("comparison operator expected");
    Token tokenComparator = tokenNext();
    if (tokPeek() != integer)
      throw new ScriptException("integer expected after comparison operator");
    Token tokenValue = tokenNext();
    int val = tokenValue.intValue;
    System.out.println("atomProperty=" + tokenAtomProperty +
                       " comparator=" + tokenComparator);
    ltokenPostfix.add(new Token(tokenComparator.tok,
                                tokenAtomProperty.tok,
                                new Integer(val)));
  }

  public static void main (String arg[] ) {
    System.out.println("hola " + y);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String lines = "";
    String line;
    try {
      while ((line = in.readLine()) != null)
        lines += line + "\n";
      System.out.println("----");
      System.out.println(lines);
      System.out.println("----");
      Token[][] aatoken = Token.tokenize(lines);
      System.out.println("" + aatoken.length + " statements");
      for (int i = 0; i < aatoken.length; ++i) {
        Token[] atoken = aatoken[i];
        if (atoken.length == 0) {
          System.out.println("null");
          continue;
        }
        System.out.println("" + atoken.length + " tokens in the line");
        for (int j = 0; j < atoken.length; ++j) {
            System.out.print(atoken[j] + " ");
        }
        System.out.println();
      }
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("que?");
    }
    System.out.println("adios");
  }
}
