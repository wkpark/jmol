/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.script.T;
import org.jmol.util.Elements;
import org.jmol.util.Logger;

import javajs.J2SIgnoreImport;
import javajs.J2SRequireImport;

import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Properties;


@J2SIgnoreImport({java.util.Properties.class,java.io.BufferedInputStream.class})
@J2SRequireImport({javajs.util.SB.class})
public final class JC {

  // axes mode constants --> org.jmol.constant.EnumAxesMode
  // callback constants --> org.jmol.constant.EnumCallback
  // draw constants --> org.jmol.shapespecial.draw.EnumCallback
  
  public static final String PDB_ANNOTATIONS = ";dssr;rna3d;dom;val;";

  public static final String CACTUS_FILE_TYPES = ";alc;cdxml;cerius;charmm;cif;cml;ctx;gjf;gromacs;hyperchem;jme;maestro;mol;mol2;sybyl2;mrv;pdb;sdf;sdf3000;sln;smiles;xyz;";

  // note list of RCSB access points: http://www.rcsb.org/pdb/static.do?p=download/http/index.html
  
  public static String[] databases = { 
    "aflowbin", "http://aflowlib.mems.duke.edu/users/jmolers/binary_new/%FILE.aflow_binary",
    "aflow", "http://aflowlib.mems.duke.edu/users/jmolers/binary_new/%FILE.aflow_binary",
    // _#DOCACHE_ flag indicates that the loaded file should be saved in any state in full
    // ' at start indicates a Jmol script evaluation
    "ams", "'http://rruff.geo.arizona.edu/AMS/viewJmol.php?'+(0+'%file'==0? 'mineral':('%file'.length==7? 'amcsd':'id'))+'=%file&action=showcif#_DOCACHE_'",    
    "dssr", "http://dssr-jmol.x3dna.org/report.php?id=%FILE&opts=--json=ebi",
    "dssrModel", "http://dssr-jmol.x3dna.org/report.php?POST?opts=--json=ebi&model=", // called in DSSR1.java
    "iucr", "http://scripts.iucr.org/cgi-bin/sendcif_yard?%FILE", // e.g. wf5113sup1
    "cod", "http://www.crystallography.net/cod/cif/%c1/%c2%c3/%c4%c5/%FILE.cif",
    "nmr", "http://www.nmrdb.org/new_predictor?POST?molfile=",
    "nmrdb", "http://www.nmrdb.org/service/predictor?POST?molfile=",
    "nmrdb13", "http://www.nmrdb.org/service/jsmol13c?POST?molfile=",
    //"pdb", "http://ftp.wwpdb.org/pub/pdb/data/structures/divided/pdb/%c2%c3/pdb%file.ent.gz", // new Jmol 14.5.0 10/28/2015
    "magndata", "http://webbdcrista1.ehu.es/magndata/mcif/%FILE.mcif",
    "mmtf", "https://mmtf.rcsb.org/full/%FILE", // new Jmol 14.5.4 4/2016
    "rna3d", "http://rna.bgsu.edu/rna3dhub/%TYPE/download/%FILE",
    // now https:
    "ligand", "https://files.rcsb.org/ligands/download/%FILE.cif",
    "mp", "https://www.materialsproject.org/materials/mp-%FILE/cif#_DOCACHE_", // e.g. https://materialsproject.org/rest/v1/materials/mp-24972/cif 
    "nci", "https://cactus.nci.nih.gov/chemical/structure/%FILE",
    "pdb", "https://files.rcsb.org/download/%FILE.pdb", // new Jmol 14.4.4 3/2016
    "pdb0", "https://files.rcsb.org/download/%FILE.pdb", // used in JSmol
    "pdbe", "https://www.ebi.ac.uk/pdbe/entry-files/download/%FILE.cif",
    "pdbe2", "https://www.ebi.ac.uk/pdbe/static/entry/%FILE_updated.cif",
    "pubchem", "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/%FILE/SDF?record_type=3d",
    "map", "https://www.ebi.ac.uk/pdbe/api/%TYPE/%FILE?pretty=false&metadata=true", 
    "pdbemap", "https://www.ebi.ac.uk/pdbe/coordinates/files/%file.ccp4",
    "pdbemapdiff", "https://www.ebi.ac.uk/pdbe/coordinates/files/%file_diff.ccp4"
  };

  /**
   * Check for databases that have changed from http:// to https:// over time.
   * We substitute https here in case this is from an old reference.
   * 
   * @param name
   * @return https protocol if necessary
   */
  public static String fixProtocol(String name) {
    if (name == null)
      return name;
    return (name.indexOf("http://www.rcsb.org/pdb/files/") == 0 
        && name.indexOf("/ligand/") < 0 ? 
        "http://files.rcsb.org/view/" + name.substring(30)
        : (name.indexOf("http://pubchem") == 0 
        || name.indexOf("http://cactus") == 0
        || name.indexOf("http://www.materialsproject") == 0)
        ? "https://" + name.substring(7) : name);
  }

  public static String[] macros = {
    "aflow", "http://aflowlib.mems.duke.edu/users/jmolers/jmol/spt/AFLOW.spt"
  };

  public static String getMacroList() {
    SB s = new SB();
    for (int i = 0; i < macros.length; i += 2)
      s.append(macros[i]).append("\t").append(macros[i + 1]).append("\n");
    return s.toString();
  }


  public static String getMacro(String key) {
    for (int i = 0; i < macros.length; i += 2)
      if (macros[i].equals(key))
        return macros[i + 1];
    return null;
  }
  
  public final static String copyright = "(C) 2015 Jmol Development";
  
  public final static String version;
  public final static String date;
  public final static int versionInt;

  static {
    String tmpVersion = null;
    String tmpDate = null;

    /**
     * definitions are incorporated into j2s/java/core.z.js by buildtojs.xml
     * 
     * @j2sNative
     * 
     *            tmpVersion = Jmol.___JmolVersion; tmpDate = Jmol.___JmolDate;
     */
    {
      BufferedInputStream bis = null;
      InputStream is = null;
      try {
        // Reading version from resource   inside jar
        is = JC.class.getClassLoader().getResourceAsStream(
            "org/jmol/viewer/Jmol.properties");
        bis = new BufferedInputStream(is);
        Properties props = new Properties();
        props.load(bis);
        tmpVersion = PT.trimQuotes(props.getProperty("Jmol.___JmolVersion",
            tmpVersion));
        tmpDate = PT.trimQuotes(props.getProperty("Jmol.___JmolDate", tmpDate));
      } catch (Exception e) {
        // Nothing to do
      } finally {
        if (bis != null) {
          try {
            bis.close();
          } catch (Exception e) {
            // Nothing to do
          }
        }
        if (is != null) {
          try {
            is.close();
          } catch (Exception e) {
            // Nothing to do
          }
        }
      }
    }
    if (tmpDate != null) {
      tmpDate = tmpDate.substring(7, 23);
      // NOTE : date is updated in the properties by SVN, and is in the format
      // "$Date$"
      //  0         1         2
      //  012345678901234567890123456789
    }
    version = (tmpVersion != null ? tmpVersion : "(Unknown version)");
    date = (tmpDate != null ? tmpDate : "(Unknown date)");
    // 11.9.999 --> 1109999
    int v = -1;
    try {
      String s = version;
      // Major number
      int i = s.indexOf(".");
      if (i < 0) {
        v = 100000 * Integer.parseInt(s);
        s = null;
      }
      if (s != null) {
        v = 100000 * Integer.parseInt(s.substring(0, i));

        // Minor number
        s = s.substring(i + 1);
        i = s.indexOf(".");
        if (i < 0) {
          v += 1000 * Integer.parseInt(s);
          s = null;
        }
        if (s != null) {
          v += 1000 * Integer.parseInt(s.substring(0, i));

          // Revision number
          s = s.substring(i + 1);
          i = s.indexOf("_");
          if (i >= 0)
            s = s.substring(0, i);
          i = s.indexOf(" ");
          if (i >= 0)
            s = s.substring(0, i);
          v += Integer.parseInt(s);
        }
      }
    } catch (NumberFormatException e) {
      // We simply keep the version currently found
    }
    versionInt = v;
  }

  public final static boolean officialRelease = false;

  public final static String DEFAULT_HELP_PATH = "http://chemapps.stolaf.edu/jmol/docs/index.htm";

  public final static String STATE_VERSION_STAMP = "# Jmol state version ";

  public final static String EMBEDDED_SCRIPT_TAG = "**** Jmol Embedded Script ****";

  public static String embedScript(String s) {
    return "\n/**" + EMBEDDED_SCRIPT_TAG + " \n" + s + "\n**/";
  }

  public final static String NOTE_SCRIPT_FILE = "NOTE: file recognized as a script file: ";
 
  public final static String SCRIPT_EDITOR_IGNORE = "\1## EDITOR_IGNORE ##";
  public final static String REPAINT_IGNORE = "\1## REPAINT_IGNORE ##";

  public final static String LOAD_ATOM_DATA_TYPES = ";xyz;vxyz;vibration;temperature;occupancy;partialcharge;";
      
  public final static float radiansPerDegree = (float) (Math.PI / 180);

  public final static String allowedQuaternionFrames = "RC;RP;a;b;c;n;p;q;x;";

  //note: Eval.write() processing requires drivers to be first-letter-capitalized.
  //do not capitalize any other letter in the word. Separate by semicolon.
  public final static String EXPORT_DRIVER_LIST = "Idtf;Maya;Povray;Vrml;X3d;Stl;Tachyon;Obj"; 

  public final static V3 center = V3.new3(0, 0, 0);
  public final static V3 axisX = V3.new3(1, 0, 0);
  public final static V3 axisY = V3.new3(0, 1, 0);
  public final static V3 axisZ = V3.new3(0, 0, 1);
  public final static V3 axisNX = V3.new3(-1, 0, 0);
  public final static V3 axisNY = V3.new3(0, -1, 0);
  public final static V3 axisNZ = V3.new3(0, 0, -1);
  public final static V3[] unitAxisVectors = {
    axisX, axisY, axisZ, axisNX, axisNY, axisNZ };

  public final static int XY_ZTOP = 100; // Z value for [x y] positioned echos and axis origin
  public final static int DEFAULT_PERCENT_VDW_ATOM = 23; // matches C sizes of AUTO with 20 for Jmol set
  public final static float DEFAULT_BOND_RADIUS = 0.15f;
  public final static short DEFAULT_BOND_MILLIANGSTROM_RADIUS = (short) (DEFAULT_BOND_RADIUS * 1000);
  public final static float DEFAULT_STRUT_RADIUS = 0.3f;
  //angstroms of slop ... from OpenBabel ... mth 2003 05 26
  public final static float DEFAULT_BOND_TOLERANCE = 0.45f;
  //minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  public final static float DEFAULT_MIN_BOND_DISTANCE = 0.4f;
  public final static float DEFAULT_MAX_CONNECT_DISTANCE = 100000000f;
  public final static float DEFAULT_MIN_CONNECT_DISTANCE = 0.1f;
  public final static int MINIMIZATION_ATOM_MAX = 200;
  public final static float MINIMIZE_FIXED_RANGE = 5.0f;

  public final static float ENC_CALC_MAX_DIST = 3f;
  public final static int ENV_CALC_MAX_LEVEL = 3;//Geodesic.standardLevel;


  public final static int MOUSE_NONE = -1;

  public final static byte MULTIBOND_NEVER =     0;
  public final static byte MULTIBOND_WIREFRAME = 1;
  public final static byte MULTIBOND_NOTSMALL =  2;
  public final static byte MULTIBOND_ALWAYS =    3;

  // maximum number of bonds that an atom can have when
  // autoBonding
  // All bonding is done by distances
  // this is only here for truly pathological cases
  public final static int MAXIMUM_AUTO_BOND_COUNT = 20;
  
  public final static short madMultipleBondSmallMaximum = 500;

  /* .cube files need this */
  public final static float ANGSTROMS_PER_BOHR = 0.5291772f;

  public final static int[] altArgbsCpk = {
    0xFFFF1493, // Xx 0
    0xFFBFA6A6, // Al 13
    0xFFFFFF30, // S  16
    0xFF57178F, // Cs 55
    0xFFFFFFC0, // D 2H
    0xFFFFFFA0, // T 3H
    0xFFD8D8D8, // 11C  6 - lighter
    0xFF505050, // 13C  6 - darker
    0xFF404040, // 14C  6 - darker still
    0xFF105050, // 15N  7 - darker
  };
  
  // hmmm ... what is shapely backbone? seems interesting
  //public final static int argbShapelyBackbone = 0xFFB8B8B8;
  //public final static int argbShapelySpecial =  0xFF5E005E;
  //public final static int argbShapelyDefault =  0xFFFF00FF;


  public final static int[] argbsFormalCharge = {
    0xFFFF0000, // -4
    0xFFFF4040, // -3
    0xFFFF8080, // -2
    0xFFFFC0C0, // -1
    0xFFFFFFFF, // 0
    0xFFD8D8FF, // 1
    0xFFB4B4FF, // 2
    0xFF9090FF, // 3
    0xFF6C6CFF, // 4
    0xFF4848FF, // 5
    0xFF2424FF, // 6
    0xFF0000FF, // 7
  };

  public final static int[] argbsRwbScale = {
    0xFFFF0000, // red
    0xFFFF1010, //
    0xFFFF2020, //
    0xFFFF3030, //
    0xFFFF4040, //
    0xFFFF5050, //
    0xFFFF6060, //
    0xFFFF7070, //
    0xFFFF8080, //
    0xFFFF9090, //
    0xFFFFA0A0, //
    0xFFFFB0B0, //
    0xFFFFC0C0, //
    0xFFFFD0D0, //
    0xFFFFE0E0, //
    0xFFFFFFFF, // white
    0xFFE0E0FF, //
    0xFFD0D0FF, //
    0xFFC0C0FF, //
    0xFFB0B0FF, //
    0xFFA0A0FF, //
    0xFF9090FF, //
    0xFF8080FF, //
    0xFF7070FF, //
    0xFF6060FF, //
    0xFF5050FF, //
    0xFF4040FF, //
    0xFF3030FF, //
    0xFF2020FF, //
    0xFF1010FF, //
    0xFF0000FF, // blue
  };

  public final static int FORMAL_CHARGE_COLIX_RED = Elements.elementSymbols.length + altArgbsCpk.length;
  public final static int PARTIAL_CHARGE_COLIX_RED = FORMAL_CHARGE_COLIX_RED + argbsFormalCharge.length;  
  public final static int PARTIAL_CHARGE_RANGE_SIZE = argbsRwbScale.length;

  
//  $ print  color("red","blue", 33,true)
//  [xff0000][xff2000][xff4000]
  //[xff6000][xff8000][xff9f00]
  
  //[xffbf00][xffdf00] -------  
  //[xffff00] ------- [xdfff00]
  
  //[xbfff00][x9fff00][x7fff00]
  //[x60ff00][x40ff00][x20ff00]
  //[x00ff00][x00ff20][x00ff40]
  //[x00ff60][x00ff7f][x00ff9f]
  //[x00ffbf][x00ffdf][x00ffff]
  //[x00dfff][x00bfff][x009fff]
  //[x0080ff][x0060ff][x0040ff]
  //[x0020ff][x0000ff]

  public final static int[] argbsRoygbScale = {
    // 35 in all //why this comment?: must be multiple of THREE for high/low
    0xFFFF0000,    0xFFFF2000,    0xFFFF4000,
    0xFFFF6000,    0xFFFF8000,    0xFFFFA000,
    
    // yellow gets compressed, so give it an extra boost

    0xFFFFC000,    0xFFFFE000,    0xFFFFF000,
    0xFFFFFF00,    0xFFF0F000,    0xFFE0FF00,
    
    0xFFC0FF00,    0xFFA0FF00,    0xFF80FF00,
    0xFF60FF00,    0xFF40FF00,    0xFF20FF00,
    0xFF00FF00,    0xFF00FF20,    0xFF00FF40,
    0xFF00FF60,    0xFF00FF80,    0xFF00FFA0,
    0xFF00FFC0,    0xFF00FFE0,    0xFF00FFFF,
    0xFF00E0FF,    0xFF00C0FF,    0xFF00A0FF,    
    0xFF0080FF,    0xFF0060FF,    0xFF0040FF,
    0xFF0020FF,    0xFF0000FF,
  };

  // positive and negative default colors used for
  // isosurface rendering of .cube files
  // multiple colors removed -- RMH 3/2008 11.1.28
  
  public final static int argbsIsosurfacePositive = 0xFF5020A0;
  public final static int argbsIsosurfaceNegative = 0xFFA02050;

  
  
  ////////////////////////////////////////////////////////////////
  // currently, ATOMIDs must be >= 0 && <= 127
  // if we need more then we can go to 255 by:
  //  1. applying 0xFF mask ... as in atom.specialAtomID & 0xFF;
  //  2. change the interesting atoms table to be shorts
  //     so that we can store negative numbers
  ////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////
  // keep this table in order to make it easier to maintain
  ////////////////////////////////////////////////////////////////

  // the following refer to jmol.biomodelset.Resolver.specialAtomNames

  // atomID 0 => nothing special, just an ordinary atom
  public final static byte ATOMID_AMINO_NITROGEN  = 1;
  public final static byte ATOMID_ALPHA_CARBON    = 2;
  public final static byte ATOMID_CARBONYL_CARBON = 3;
  public final static byte ATOMID_CARBONYL_OXYGEN = 4;
  public final static byte ATOMID_O1              = 5;
  
  // this is for groups that only contain an alpha carbon
  public final static int ATOMID_ALPHA_ONLY_MASK = 1 << ATOMID_ALPHA_CARBON;

  //this is entries 1 through 3 ... 3 bits ... N, CA, C
  public final static int ATOMID_PROTEIN_MASK =  0x7 << ATOMID_AMINO_NITROGEN;

  public final static byte ATOMID_O5_PRIME        = 6;
  public final static byte ATOMID_C5_PRIME        = 7;
  public final static byte ATOMID_C4_PRIME        = 8;
  public final static byte ATOMID_C3_PRIME        = 9;
  public final static byte ATOMID_O3_PRIME        = 10;
  public final static byte ATOMID_C2_PRIME        = 11;
  public final static byte ATOMID_C1_PRIME        = 12;
  public final static byte ATOMID_O4_PRIME        = 78;

  // this is entries 6 through through 12 ... 7 bits
  public final static int ATOMID_NUCLEIC_MASK = 0x7F << ATOMID_O5_PRIME;

  public final static byte ATOMID_NUCLEIC_PHOSPHORUS = 13;
  
  // this is for nucleic groups that only contain a phosphorus
  public final static int ATOMID_PHOSPHORUS_ONLY_MASK =
    1 << ATOMID_NUCLEIC_PHOSPHORUS;

  // this can be increased as far as 32, but not higher.
  public final static int ATOMID_DISTINGUISHING_ATOM_MAX = 14;
  
  public final static byte ATOMID_CARBONYL_OD1 = 14;
  public final static byte ATOMID_CARBONYL_OD2 = 15;
  public final static byte ATOMID_CARBONYL_OE1 = 16;
  public final static byte ATOMID_CARBONYL_OE2 = 17;
  //public final static byte ATOMID_SG = 18;
  
  public final static byte ATOMID_N1 = 32;
  public final static byte ATOMID_C2 = 33;
  public final static byte ATOMID_N3 = 34;
  public final static byte ATOMID_C4 = 35;
  public final static byte ATOMID_C5 = 36;
  public final static byte ATOMID_C6 = 37; // wing
  public final static byte ATOMID_O2 = 38;
  public final static byte ATOMID_N7 = 39;
  public final static byte ATOMID_C8 = 40;
  public final static byte ATOMID_N9 = 41;
  public final static byte ATOMID_N4 = 42;
  public final static byte ATOMID_N2 = 43;
  public final static byte ATOMID_N6 = 44;
  public final static byte ATOMID_C5M= 45;
  public final static byte ATOMID_O6 = 46;
  public final static byte ATOMID_O4 = 47;
  public final static byte ATOMID_S4 = 48;
  public final static byte ATOMID_C7 = 49;
  
  public final static byte ATOMID_TERMINATING_OXT = 64;
  
  public final static byte ATOMID_H5T_TERMINUS    = 72;
  public final static byte ATOMID_O5T_TERMINUS    = 73;
  public final static byte ATOMID_O1P             = 74;
  public final static byte ATOMID_OP1             = 75;
  public final static byte ATOMID_O2P             = 76;
  public final static byte ATOMID_OP2             = 77;
  public final static byte ATOMID_O2_PRIME        = 79;
  public final static byte ATOMID_H3T_TERMINUS    = 88;
  public final static byte ATOMID_HO3_PRIME       = 89;
  public final static byte ATOMID_HO5_PRIME       = 90;

  // These masks are only used for P-only and N-only polymers
  // or cases where there are so few atoms that a monomer's type
  // cannot be determined by checking actual atoms and connections.
  // They are not used for NucleicMonomer or AminoMonomer classes.
  //
  //             I  A G        
  //   purine:   100101 = 0x25
  //
  //              UT C
  // pyrimidine: 011010 = 0x1A
  //
  //            +IUTACGDIUTACG IUTACG
  //        dna: 001111 111111 001000 = 0x0FFC8
  //  
  //            +IUTACGDIUTACG IUTACG
  //        rna: 110??? 000000 110111 = 0x30037
  
  public static final int PURINE_MASK = 0x25 | (0x25 << 6) | (0x25 << 12);
  public static final int PYRIMIDINE_MASK = 0x1A | (0x1A << 6) | (0x1A << 12);
  public static final int DNA_MASK = 0x0FFC8;
  public static final int RNA_MASK = 0x30037;
  

  

  ////////////////////////////////////////////////////////////////
  // GROUP_ID related stuff for special groupIDs
  ////////////////////////////////////////////////////////////////
  
  public final static int GROUPID_ARGININE          = 2;
  public final static int GROUPID_ASPARAGINE        = 3;
  public final static int GROUPID_ASPARTATE         = 4;
  public final static int GROUPID_CYSTEINE          = 5;
  public final static int GROUPID_GLUTAMINE        =  6;
  public final static int GROUPID_GLUTAMATE        =  7;
  public final static int GROUPID_HISTIDINE        =  9;
  public final static int GROUPID_LYSINE           = 12;
  public final static int GROUPID_PROLINE          = 15;
  public final static int GROUPID_TRYPTOPHAN       = 19;
  public final static int GROUPID_AMINO_MAX        = 24;
  public final static int GROUPID_NUCLEIC_MAX      = 42;  
  public final static int GROUPID_WATER           = 42;
  public final static int GROUPID_SOLVENT_MIN     = 45; // urea only
  private final static int GROUPID_ION_MIN         = 46;
  private final static int GROUPID_ION_MAX         = 48;
  

  ////////////////////////////////////////////////////////////////
  // predefined sets
  ////////////////////////////////////////////////////////////////

  // these must be removed after various script commands so that they stay current
  
  public static String[] predefinedVariable = {
    //  
    // main isotope (variable because we can do {xxx}.element = n;
    //
    "@_1H _H & !(_2H,_3H)",
    "@_12C _C & !(_13C,_14C)",
    "@_14N _N & !(_15N)",

    //
    // solvent
    //
    // @water is specially defined, avoiding the CONNECTED() function
    //"@water _g>=" + GROUPID_WATER + " & _g<" + GROUPID_SOLVENT_MIN
    //+ ", oxygen & connected(2) & connected(2, hydrogen), (hydrogen) & connected(oxygen & connected(2) & connected(2, hydrogen))",

    "@solvent water, (_g>=" + GROUPID_SOLVENT_MIN + " & _g<" + GROUPID_ION_MAX + ")", // water, other solvent or ions
    "@ligand _g=0|!(_g<"+ GROUPID_ION_MIN + ",protein,nucleic,water)", // includes UNL

    // protein structure
    "@turn structure=1",
    "@sheet structure=2",
    "@helix structure=3",
    "@helix310 substructure=7",
    "@helixalpha substructure=8",
    "@helixpi substructure=9",

    // nucleic acid structures
    "@bulges within(dssr,'bulges')",
    "@coaxStacks within(dssr,'coaxStacks')",
    "@hairpins within(dssr,'hairpins')",
    "@hbonds within(dssr,'hbonds')",
    "@helices within(dssr,'helices')",
    "@iloops within(dssr,'iloops')",
    "@isoCanonPairs within(dssr,'isoCanonPairs')",
    "@junctions within(dssr,'junctions')",
    "@kissingLoops within(dssr,'kissingLoops')",
    "@multiplets within(dssr,'multiplets')",
    "@nonStack within(dssr,'nonStack')",
    "@nts within(dssr,'nts')",
    "@pairs within(dssr,'pairs')",
    "@ssSegments within(dssr,'ssSegments')",
    "@stacks within(dssr,'stacks')",
    "@stems within(dssr,'stems')",
    
  };
  
  // these are only updated once per file load or file append
  
  public static String[] predefinedStatic = {
    //
    // protein related
    //
    // protein is hardwired
    "@amino _g>0 & _g<=23",
    "@acidic asp,glu",
    "@basic arg,his,lys",
    "@charged acidic,basic",
    "@negative acidic",
    "@positive basic",
    "@neutral amino&!(acidic,basic)",
    "@polar amino&!hydrophobic",

    "@cyclic his,phe,pro,trp,tyr",
    "@acyclic amino&!cyclic",
    "@aliphatic ala,gly,ile,leu,val",
    "@aromatic his,phe,trp,tyr",
    "@cystine within(group, (cys.sg or cyx.sg) and connected(cys.sg or cyx.sg))",

    "@buried ala,cys,ile,leu,met,phe,trp,val",
    "@surface amino&!buried",

    // doc on hydrophobic is inconsistent
    // text description of hydrophobic says this
    //    "@hydrophobic ala,leu,val,ile,pro,phe,met,trp",
    // table says this
    "@hydrophobic ala,gly,ile,leu,met,phe,pro,trp,tyr,val",
    "@mainchain backbone",
    "@small ala,gly,ser",
    "@medium asn,asp,cys,pro,thr,val",
    "@large arg,glu,gln,his,ile,leu,lys,met,phe,trp,tyr",

    //
    // nucleic acid related

    // nucleic, dna, rna, purine, pyrimidine are hard-wired
    //
    "@c nucleic & ([C] or [DC] or within(group,_a="+ATOMID_N4+"))",
    "@g nucleic & ([G] or [DG] or within(group,_a="+ATOMID_N2+"))",
    "@cg c,g",
    "@a nucleic & ([A] or [DA] or within(group,_a="+ATOMID_N6+"))",
    "@t nucleic & ([T] or [DT] or within(group,_a="+ATOMID_C5M+" | _a="+ATOMID_C7+"))",
    "@at a,t",
    "@i nucleic & ([I] or [DI] or within(group,_a="+ATOMID_O6+") & !g)",
    "@u nucleic & ([U] or [DU] or within(group,_a="+ATOMID_O4+") & !t)",
    "@tu nucleic & within(group,_a="+ATOMID_S4+")",

    //
    // ions
    //
    "@ions _g>="+GROUPID_ION_MIN+"&_g<"+GROUPID_ION_MAX,

    //
    // structure related
    //
    "@alpha _a=2", // rasmol doc says "approximately *.CA" - whatever?
    "@_bb protein&(_a>=1&_a<6|_a=64) | nucleic&(_a>=6&_a<14|_a>=73&&_a<=79||_a==99||_a=100)", // no H atoms    
    "@backbone _bb | _H && connected(single, _bb)",    
    "@spine protein&_a>=1&_a<4|nucleic&(_a>=6&_a<11|_a=13)",
    "@sidechain (protein,nucleic) & !backbone",
    "@base nucleic & !backbone",
    "@dynamic_flatring search('[a]')",

    //periodic table
    "@nonmetal _H,_He,_B,_C,_N,_O,_F,_Ne,_Si,_P,_S,_Cl,_Ar,_As,_Se,_Br,_Kr,_Te,_I,_Xe,_At,_Rn",
    "@metal !nonmetal",
    "@alkaliMetal _Li,_Na,_K,_Rb,_Cs,_Fr",
    "@alkalineEarth _Be,_Mg,_Ca,_Sr,_Ba,_Ra",
    "@nobleGas _He,_Ne,_Ar,_Kr,_Xe,_Rn",
    "@metalloid _B,_Si,_Ge,_As,_Sb,_Te",
    "@transitionMetal elemno>=21&elemno<=30|elemno>=39&elemno<=48|elemno>=72&elemno<=80|elemno>=104&elemno<=112",
    "@lanthanide elemno>=57&elemno<=71",
    "@actinide elemno>=89&elemno<=103",

    //    "@hetero", handled specially

  };

  public final static String MODELKIT_ZAP_STRING = "5\n\nC 0 0 0\nH .63 .63 .63\nH -.63 -.63 .63\nH -.63 .63 -.63\nH .63 -.63 -.63";
  public final static String MODELKIT_ZAP_TITLE = "Jmol Model Kit";//do not ever change this -- it is in the state
  public final static String ZAP_TITLE = "zapped";//do not ever change this -- it is in the state
  public final static String ADD_HYDROGEN_TITLE = "Viewer.AddHydrogens"; //do not ever change this -- it is in the state

  ////////////////////////////////////////////////////////////////
  // font-related
  ////////////////////////////////////////////////////////////////

  public final static String DEFAULT_FONTFACE = "SansSerif";
  public final static String DEFAULT_FONTSTYLE = "Plain";

  public final static int MEASURE_DEFAULT_FONTSIZE = 15;
  public final static int AXES_DEFAULT_FONTSIZE = 14;

  ////////////////////////////////////////////////////////////////
  // do not rearrange/modify these shapes without
  // updating the String[] shapeBaseClasses below &&
  // also creating a token for this shape in Token.java &&
  // also updating shapeToks to confirm consistent
  // conversion from tokens to shapes
  ////////////////////////////////////////////////////////////////

  public final static int SHAPE_BALLS      = 0;
  public final static int SHAPE_STICKS     = 1;
  public final static int SHAPE_HSTICKS    = 2;  //placeholder only; handled by SHAPE_STICKS
  public final static int SHAPE_SSSTICKS   = 3;  //placeholder only; handled by SHAPE_STICKS
  public final static int SHAPE_STRUTS     = 4;  //placeholder only; handled by SHAPE_STICKS
  public final static int SHAPE_LABELS     = 5;
  public final static int SHAPE_MEASURES   = 6;
  public final static int SHAPE_STARS      = 7;

  public final static int SHAPE_MIN_HAS_SETVIS = 8;
  
  public final static int SHAPE_HALOS      = 8;

  public final static int SHAPE_MIN_SECONDARY = 9; //////////
  
    public final static int SHAPE_BACKBONE   = 9;
    public final static int SHAPE_TRACE      = 10;
    public final static int SHAPE_CARTOON    = 11;
    public final static int SHAPE_STRANDS    = 12;
    public final static int SHAPE_MESHRIBBON = 13;
    public final static int SHAPE_RIBBONS    = 14;
    public final static int SHAPE_ROCKETS    = 15;
  
  public final static int SHAPE_MAX_SECONDARY = 16; //////////
  public final static int SHAPE_MIN_SPECIAL    = 16; //////////

    public final static int SHAPE_DOTS       = 16;
    public final static int SHAPE_DIPOLES    = 17;
    public final static int SHAPE_VECTORS    = 18;
    public final static int SHAPE_GEOSURFACE = 19;
    public final static int SHAPE_ELLIPSOIDS = 20;

  public final static int SHAPE_MAX_SIZE_ZERO_ON_RESTRICT = 21; //////////
  
  public final static int SHAPE_MIN_HAS_ID  = 21; //////////

  public final static int SHAPE_POLYHEDRA   = 21;  // for restrict, uses setProperty(), not setSize()

  public final static int SHAPE_DRAW        = 22;
  
  public final static int SHAPE_MAX_SPECIAL = 23; //////////

  public final static int SHAPE_CGO         = 23;

  public final static int SHAPE_MIN_SURFACE = 24; //////////

  public final static int SHAPE_ISOSURFACE  = 24;
  public final static int SHAPE_CONTACT     = 25;

  public final static int SHAPE_LCAOCARTOON = 26;

  private final static int SHAPE_LAST_ATOM_VIS_FLAG = 26; // LCAO 
  // no setting of atom.shapeVisibilityFlags after this point

  public final static int SHAPE_MO          = 27;  //but no ID for MO
  public final static int SHAPE_NBO         = 28;  //but no ID for MO

  public final static int SHAPE_PMESH       = 29;
  public final static int SHAPE_PLOT3D      = 30;

  public final static int SHAPE_MAX_SURFACE         = 30; //////////
  public final static int SHAPE_MAX_MESH_COLLECTION = 30; //////////
  
  public final static int SHAPE_ECHO       = 31;
  
  public final static int SHAPE_MAX_HAS_ID = 32;
  
  public final static int SHAPE_BBCAGE     = 32;

  public final static int SHAPE_MAX_HAS_SETVIS = 33;

  public final static int SHAPE_UCCAGE     = 33;
  public final static int SHAPE_AXES       = 34;
  public final static int SHAPE_HOVER      = 35;
  public final static int SHAPE_FRANK      = 36;
  public final static int SHAPE_MAX        = SHAPE_FRANK + 1;
    
  public final static int getShapeVisibilityFlag(int shapeID) {
    return 16 << Math.min(shapeID, SHAPE_LAST_ATOM_VIS_FLAG);
  }

  public static final int VIS_BOND_FLAG = 16 << SHAPE_STICKS;
  public static final int VIS_BALLS_FLAG = 16 << SHAPE_BALLS;
  public static final int VIS_LABEL_FLAG = 16 << SHAPE_LABELS;
  public static final int VIS_BACKBONE_FLAG = 16 << SHAPE_BACKBONE;
  public final static int VIS_CARTOON_FLAG = 16 << SHAPE_CARTOON;  

  public final static int ALPHA_CARBON_VISIBILITY_FLAG = 
      (16 << SHAPE_ROCKETS) | (16 << SHAPE_TRACE) | (16 << SHAPE_STRANDS) 
      | (16 << SHAPE_MESHRIBBON) | (16 << SHAPE_RIBBONS)
      | VIS_CARTOON_FLAG | VIS_BACKBONE_FLAG;
  

  // note that these next two arrays *MUST* be in the same sequence 
  // given in SHAPE_* and they must be capitalized exactly as in their class name 

  public final static String[] shapeClassBases = {
    "Balls", "Sticks", "Hsticks", "Sssticks", "Struts",
      //Hsticks, Sssticks, and Struts classes do not exist, but this returns Token for them
    "Labels", "Measures", "Stars", "Halos",
    "Backbone", "Trace", "Cartoon", "Strands", "MeshRibbon", "Ribbons", "Rockets", 
    "Dots", "Dipoles", "Vectors", "GeoSurface", "Ellipsoids", "Polyhedra", 
    "Draw", "CGO", "Isosurface", "Contact", "LcaoCartoon", "MolecularOrbital", "NBO", "Pmesh", "Plot3D", 
    "Echo", "Bbcage", "Uccage", "Axes", "Hover", 
    "Frank"
     };
  // .hbond and .ssbonds will return a class,
  // but the class is never loaded, so it is skipped in each case.
  // coloring and sizing of hydrogen bonds and S-S bonds is now
  // done by Sticks.

  public final static int shapeTokenIndex(int tok) {
    switch (tok) {
    case T.atoms:
    case T.balls:
      return SHAPE_BALLS;
    case T.bonds:
    case T.wireframe:
      return SHAPE_STICKS;
    case T.hbond:
      return SHAPE_HSTICKS;
    case T.ssbond:
      return SHAPE_SSSTICKS;
    case T.struts:
      return SHAPE_STRUTS;
    case T.label:
      return SHAPE_LABELS;
    case T.measure:
    case T.measurements:
      return SHAPE_MEASURES;
    case T.star:
      return SHAPE_STARS;
    case T.halo:
      return SHAPE_HALOS;
    case T.backbone:
      return SHAPE_BACKBONE;
    case T.trace:
      return SHAPE_TRACE;
    case T.cartoon:
      return SHAPE_CARTOON;
    case T.strands:
      return SHAPE_STRANDS;
    case T.meshRibbon:
      return SHAPE_MESHRIBBON;
    case T.ribbon:
      return SHAPE_RIBBONS;
    case T.rocket:
      return SHAPE_ROCKETS;
    case T.dots:
      return SHAPE_DOTS;
    case T.dipole:
      return SHAPE_DIPOLES;
    case T.vector:
      return SHAPE_VECTORS;
    case T.geosurface:
      return SHAPE_GEOSURFACE;
    case T.ellipsoid:
      return SHAPE_ELLIPSOIDS;
    case T.polyhedra:
      return SHAPE_POLYHEDRA;
    case T.cgo:
      return SHAPE_CGO;
    case T.draw:
      return SHAPE_DRAW;
    case T.isosurface:
      return SHAPE_ISOSURFACE;
    case T.contact:
      return SHAPE_CONTACT;
    case T.lcaocartoon:
      return SHAPE_LCAOCARTOON;
    case T.mo:
      return SHAPE_MO;
    case T.nbo:
      return SHAPE_NBO;
    case T.pmesh:
      return SHAPE_PMESH;
    case T.plot3d:
      return SHAPE_PLOT3D;
    case T.echo:
      return SHAPE_ECHO;
    case T.axes:
      return SHAPE_AXES;
    case T.boundbox:
      return SHAPE_BBCAGE;
    case T.unitcell:
      return SHAPE_UCCAGE;
    case T.hover:
      return SHAPE_HOVER;
    case T.frank:
      return SHAPE_FRANK;
    }
    return -1;
  }
  
  public final static String getShapeClassName(int shapeID, boolean isRenderer) {
    if (shapeID < 0)
      return shapeClassBases[~shapeID];
    return "org.jmol." + (isRenderer ? "render" : "shape") 
        + (shapeID >= SHAPE_MIN_SECONDARY && shapeID < SHAPE_MAX_SECONDARY 
            ? "bio."
        : shapeID >= SHAPE_MIN_SPECIAL && shapeID < SHAPE_MAX_SPECIAL 
            ? "special."        
        : shapeID >= SHAPE_MIN_SURFACE && shapeID < SHAPE_MAX_SURFACE 
            ? "surface." 
        : shapeID == SHAPE_CGO 
            ? "cgo." 
        : ".") + shapeClassBases[shapeID];
  }

//  public final static String binaryExtensions = ";pse=PyMOL;";// PyMOL

  public static final String SCRIPT_COMPLETED = "Script completed";
  public static final String JPEG_EXTENSIONS = ";jpg;jpeg;jpg64;jpeg64;";
  public final static String IMAGE_TYPES = JPEG_EXTENSIONS + "gif;gift;pdf;ppm;png;pngj;pngt;";
  public static final String IMAGE_OR_SCENE = IMAGE_TYPES + "scene;";

  static {
    /**
     * @j2sNative
     */
    {
      if (argbsFormalCharge.length != Elements.FORMAL_CHARGE_MAX
          - Elements.FORMAL_CHARGE_MIN + 1) {
        Logger.error("formal charge color table length");
        throw new NullPointerException();
      }
      if (shapeClassBases.length != SHAPE_MAX) {
        Logger.error("shapeClassBases wrong length");
        throw new NullPointerException();
      }
      if (shapeClassBases.length != SHAPE_MAX) {
        Logger.error("the shapeClassBases array has the wrong length");
        throw new NullPointerException();
      }
    }
  }

  ///////////////// LABEL and ECHO ///////////////////////
  

  // note that the y offset is positive upward
  
  //  3         2         1        
  // 10987654321098765432109876543210
  //  -x-offset--y-offset-___cafgaabp
  //                      |||||||| ||_pointer on
  //                      |||||||| |_background pointer color
  //                      ||||||||_text alignment 0xC 
  //                      |||||||_labels group 0x10
  //                      ||||||_labels front  0x20
  //                      |||||_absolute
  //                      ||||_centered
  //                      |||_reserved
  //                      ||_reserved
  //                      |_reserved

  public final static int LABEL_MINIMUM_FONTSIZE = 6;
  public final static int LABEL_MAXIMUM_FONTSIZE = 63;
  public final static int LABEL_DEFAULT_FONTSIZE = 13;
  public final static int LABEL_DEFAULT_X_OFFSET = 4;
  public final static int LABEL_DEFAULT_Y_OFFSET = 4;
  public final static int LABEL_OFFSET_MAX       = 500; // 0x1F4; 

  private final static int LABEL_OFFSET_MASK          = 0x3FF; // 10 bits for each offset (-500 to 500)
  private final static int LABEL_FLAGY_OFFSET_SHIFT   = 11;    // 11-20 is Y offset
  private final static int LABEL_FLAGX_OFFSET_SHIFT   = 21;    // 21-30 is X offset
  
  public final static int LABEL_FLAGS                 = 0x03F; // does not include absolute or centered
  private final static int LABEL_POINTER_FLAGS        = 0x003;
  public final static int LABEL_POINTER_NONE          = 0x000;
  public final static int LABEL_POINTER_ON            = 0x001;  // add label pointer
  public final static int LABEL_POINTER_BACKGROUND    = 0x002;  // add label pointer to background

  private final static int TEXT_ALIGN_SHIFT           = 0x002;
  private final static int TEXT_ALIGN_FLAGS           = 0x00C;
  public final static int TEXT_ALIGN_NONE             = 0x000;
  public final static int TEXT_ALIGN_LEFT             = 0x004;
  public final static int TEXT_ALIGN_CENTER           = 0x008;
  public final static int TEXT_ALIGN_RIGHT            = 0x00C;
  
  private final static int LABEL_ZPOS_FLAGS           = 0x030;
  public final static int LABEL_ZPOS_GROUP            = 0x010;
  public final static int LABEL_ZPOS_FRONT            = 0x020;
  
  private final static int LABEL_EXPLICIT             = 0x040;
  
  private final static int LABEL_CENTERED             = 0x100;

  public static int LABEL_DEFAULT_OFFSET = 
      (LABEL_DEFAULT_X_OFFSET << LABEL_FLAGX_OFFSET_SHIFT)
    | (LABEL_DEFAULT_Y_OFFSET << LABEL_FLAGY_OFFSET_SHIFT);

  public final static int ECHO_TOP      = 0;
  public final static int ECHO_BOTTOM   = 1;
  public final static int ECHO_MIDDLE   = 2;
  public final static int ECHO_XY       = 3;
  public final static int ECHO_XYZ      = 4;

  private final static String[] echoNames = { "top", "bottom", "middle", "xy", "xyz" };

  public static String getEchoName(int type) {
    return echoNames[type];
  }

  public static int setZPosition(int offset, int pos) {
    return (offset & ~LABEL_ZPOS_FLAGS) | pos;
  }

  public static int setPointer(int offset, int pointer) {
    return (offset & ~LABEL_POINTER_FLAGS) | pointer;  
  }

  public static int getPointer(int offset) {
    return offset & LABEL_POINTER_FLAGS;
  }

  public static String getPointerName(int pointer) {
    return ((pointer & LABEL_POINTER_ON) == 0 ? ""
        : (pointer & LABEL_POINTER_BACKGROUND) > 0 ? "background" : "on");
  }   

  public static boolean isOffsetAbsolute(int offset) {
    return ((offset & LABEL_EXPLICIT) != 0);
  }

  /**
   * Construct an 32-bit integer packed with 10-byte x and y offsets (-500 to 500)
   * along with flags to indicate if exact and, if not, a flag to indicate that
   * the 0 in x or y indicates "centered". The non-exact default offset of [4,4] is 
   * represented as 0 so that new array elements do not have to be initialized. 
   * 
   * @param xOffset
   * @param yOffset
   * @param isAbsolute
   * @return packed offset x and y with positioning flags
   */
  public static int getOffset(int xOffset, int yOffset, boolean isAbsolute) {
    xOffset = Math.min(Math.max(xOffset, -LABEL_OFFSET_MAX), LABEL_OFFSET_MAX);
    yOffset = (Math.min(Math.max(yOffset, -LABEL_OFFSET_MAX), LABEL_OFFSET_MAX));
    int offset = ((xOffset & LABEL_OFFSET_MASK) << LABEL_FLAGX_OFFSET_SHIFT)
         | ((yOffset & LABEL_OFFSET_MASK) << LABEL_FLAGY_OFFSET_SHIFT)
         | (isAbsolute ? LABEL_EXPLICIT : 0);
    if (offset == LABEL_DEFAULT_OFFSET)
      offset = 0;
    else if (!isAbsolute && (xOffset == 0 || yOffset == 0))
      offset |= LABEL_CENTERED;
    return offset;
  }

  /**
   * X offset in pixels. 
   * 
   * negative of this is the actual screen offset
   * 
   * @param offset  0 for an offset indicates "not set" and delivers the default offset
   * @return screen offset from left
   */
  public static int getXOffset(int offset) {
    if (offset == 0)
      return LABEL_DEFAULT_X_OFFSET;
    int x = (offset >> LABEL_FLAGX_OFFSET_SHIFT) & LABEL_OFFSET_MASK;
    x = (x > LABEL_OFFSET_MAX ? x - LABEL_OFFSET_MASK - 1 : x);
    return x;
  }

  /**
   * Y offset in pixels; negative of this is the actual screen offset
   * 
   * @param offset  0 for an offset indicates "not set" and delivers the default offset
   * @return screen offset from bottom
   */
  public static int getYOffset(int offset) {
    if (offset == 0)
      return LABEL_DEFAULT_Y_OFFSET;
    int y = (offset >> LABEL_FLAGY_OFFSET_SHIFT) & LABEL_OFFSET_MASK;
    return (y > LABEL_OFFSET_MAX ? y - LABEL_OFFSET_MASK - 1 : y);
  }
  
  public static int getAlignment(int offset) {
    return (offset & TEXT_ALIGN_FLAGS);
  }

  public static int setHorizAlignment(int offset, int hAlign) {
    return (offset & ~TEXT_ALIGN_FLAGS) | hAlign;
  }

  private final static String[] hAlignNames = { "", "left", "center", "right" };

  public static String getHorizAlignmentName(int align) {
    return hAlignNames[(align >> TEXT_ALIGN_SHIFT) & 3];
  }
  
  public static boolean isSmilesCanonical(String options) {
    return (options != null && PT.isOneOf(options.toLowerCase(), ";/cactvs///;/cactus///;/nci///;/canonical///;"));
  }

  public static final int SMILES_TYPE_SMILES         = 0x1; // placeholder -- DO NOT TEST FOR THIS as it is also in openSMARTS
  public static final int SMILES_TYPE_SMARTS         = 0x2; // CmdExt -> matcher
  public static final int SMILES_TYPE_OPENSMILES     = 0x5; // includes aromatic normalization of pattern; tests true when openSMARTS as well
  public static final int SMILES_TYPE_OPENSMARTS     = 0x7; // 

  public static final int SMILES_FIRST_MATCH_ONLY           = 0x8; // 0xFF0 reserved for SmilesMatcher mflag
  
  public final static int SMILES_NO_AROMATIC                = 0x010; //SmilesParser -> SmilesSearch
 
  public final static int SMILES_IGNORE_STEREOCHEMISTRY     = 0x020; //SmilesParser -> SmilesSearch

  public final static int SMILES_INVERT_STEREOCHEMISTRY     = 0x040; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_DEFINED draws all aromatic bonds from connection definitions
   * It is deprecated, because a=a will set it by itself. 
   */
  public final static int SMILES_AROMATIC_DEFINED           = 0x080; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_STRICT enforces Hueckel 4+2 rule, not allowing acyclic double bonds
   * 
   */
  public final static int SMILES_AROMATIC_STRICT            = 0x100; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_DOUBLE allows a distinction between single and double, as for
   * example is necessary to distinguish between n=cNH2 and ncNH2 (necessary for
   * MMFF94 atom typing)
   */
  public final static int SMILES_AROMATIC_DOUBLE            = 0x200; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_MMFF94 also raises the strictness level to force all 6- and
   * 7-membered rings to have exactly three double bonds.
   */
  public static final int SMILES_AROMATIC_MMFF94            = 0x300; // includes AROMATIC_STRICT and AROMATIC_DOUBLE;

  //  /**
  //   * AROMATIC_JSME_NONCANONICAL matches the JSME noncanonical option.
  //  * 
  //   */
  //  final static int AROMATIC_JSME_NONCANONICAL = 0x800; //SmilesParser -> SmilesSearch

  /**
   * AROMATIC_PLANAR only invokes planarity (Jmol default through 14.5)
   * 
   */
  public final static int SMILES_AROMATIC_PLANAR            = 0x400; //SmilesParser -> SmilesSearch

  public static final int SMILES_IGNORE_ATOM_CLASS          = 0x800;

  public static final int SMILES_GEN_EXPLICIT_H                = 0x00001000; // SmilesExt -> generator
  public static final int SMILES_GEN_TOPOLOGY                  = 0x00002000; // SmilesExt -> generator
  public static final int SMILES_GEN_POLYHEDRAL                = 0x00010000; // polyhedron -> generator
  public static final int SMILES_GEN_ATOM_COMMENT              = 0x00020000; // polyhedron,Viewer -> generator
  
  public static final int SMILES_GEN_BIO                       = 0x00100000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS = 0x00300000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_COV_CROSSLINK         = 0x00500000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_HH_CROSSLINK          = 0x00900000; // MathExt -> generator
  public static final int SMILES_GEN_BIO_COMMENT               = 0x01100000; // MathExt -> Viewer
  public static final int SMILES_GEN_BIO_NOCOMMENTS            = 0x02100000; // MathExt -> Generator

  public static final int SMILES_GROUP_BY_MODEL                = 0x04000000; // MathExt -> search

  


  
  public static final int JSV_NOT = -1;
  public static final int JSV_SEND_JDXMOL = 0;
  public static final int JSV_SETPEAKS = 7;
  public static final int JSV_SELECT = 14;
  public static final int JSV_STRUCTURE = 21;
  public static final int JSV_SEND_H1SIMULATE = 28;
  public static final int JSV_SEND_C13SIMULATE = 35;
  public static final int NBO_MODEL = 42;
  public static final int NBO_RUN = 49;
  public static final int NBO_VIEW = 56;
  public static final int NBO_SEARCH = 63;
  public static final int NBO_CONFIG = 70;
  public static final int JSV_CLOSE = 77;

 
  public static int getServiceCommand(String script) {
    return (script.length() < 7 ? -1 : ("" +
        "JSPECVI" +
        "PEAKS: " +
        "SELECT:" +
        "JSVSTR:" +
        "H1SIMUL" +
        "C13SIMU" +
        "NBO:MOD" +
        "NBO:RUN" +
        "NBO:VIE" +
        "NBO:SEA" +
        "NBO:CON" +
        "NONESIM"
        )
        .indexOf(script.substring(0, 7).toUpperCase()));
  }

  public static String READER_NOT_FOUND = "File reader was not found:";

  public final static int UNITID_MODEL = 1;
  public final static int UNITID_RESIDUE = 2;
  public final static int UNITID_ATOM = 4;
  public final static int UNITID_INSCODE = 8;
  public final static int UNITID_TRIM = 16;
  /**
   * Get a unitID type
   * 
   * @param type -mra (model name, residue, atom, and ins code), 
   *             -mr (model and residue; no atom)
   *             -ra default
   *             - or -r  just residue 
   *             -t right-trim
   *             
   * @return coded type
   */
  public static int getUnitIDFlags(String type) {
    int i = UNITID_RESIDUE | UNITID_ATOM | UNITID_INSCODE;
    if (type.indexOf("-") == 0) {
      if (type.indexOf("m") > 0)
        i |= UNITID_MODEL;
      if (type.indexOf("a") < 0)
        i ^= UNITID_ATOM;
      if (type.indexOf("t") > 0)
        i |= UNITID_TRIM;
    }
    return i;
  }


}
