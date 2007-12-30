/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;
import java.util.StringTokenizer;

import netscape.javascript.JSObject;

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import java.util.Hashtable;

public class Resolver {

  private final static String classBase = "org.jmol.adapter.readers.";
  private final static String[] readerSets = new String[] {
    "cifpdb.", "Cif;Pdb;",
    "molxyz.", "Mol;Xyz;",
    "xml.", "Xml;"
  };
  
  private final static String getReaderClassBase(String type) {
    String base = "more.";
    for (int i = 1; i < readerSets.length; i += 2)
      if (readerSets[i].indexOf(type + ";") >= 0) {
        base = readerSets[i - 1];
        break;
      }
    return classBase + base + type + "Reader";
  }
  
  static String getFileType(BufferedReader br) {
    try {
      return determineAtomSetCollectionReader(br, false);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * In the case of spt files, no need to load them; here we are just checking for type
   * In the case of .spardir directories, we need to provide a list of 
   * the critical files that need loading and concatenation for the SpartanSmolReader
   * 
   * we return an array for which:
   * 
   * [0] file type (class prefix) or null for SPT file
   * [1] header to add for each BEGIN/END block
   * [2...] files to load and concatenate
   * 
   * @param name
   * @param type
   * @return array detailing action for this set of files
   */
  public static String[] specialLoad(String name, String type) {
    int pt;
    if (name.indexOf(".spt") == name.length() - 4)
      return new String[] { null, null, null}; //DO NOT actually load any file here
    if ((pt = name.lastIndexOf(".spardir")) >= 0) {
      if (name.indexOf(".spardir.") >= 0)
        return null; // could easily be .spardir.zip -- these MUST be .spardir or .spardir/...
      name = name.replace('\\','/');
      name = name.substring(0, pt + (name.indexOf("/M") == pt + 8 ? 14 : 8));
      if (name.indexOf("/M") < 0)
        name += "/M0001";
      return new String[] { "SpartanSmol", "Directory Entry ",
          name + "/input",
          name + "/archive", 
          name + "/proparc"};
    }
    return null;
  }

  static Object resolve(String name, String type, BufferedReader bufferedReader) throws Exception {
    return resolve(name, type, bufferedReader, null);
  }

  static Object resolve(String fullName, String type, BufferedReader bufferedReader,
                        Hashtable htParams) throws Exception {
    AtomSetCollectionReader atomSetCollectionReader = null;
    String atomSetCollectionReaderName;
    if (type != null) {
      atomSetCollectionReaderName = getReaderFromType(type);
      if (atomSetCollectionReaderName == null)
        return "unrecognized file format type " + type;
      Logger.info("The Resolver assumes " + atomSetCollectionReaderName);
    } else {
      atomSetCollectionReaderName = determineAtomSetCollectionReader(
          bufferedReader, true);
      if (atomSetCollectionReaderName.indexOf("\n") >= 0)
        return "unrecognized file format for file " + fullName + "\n"
            + atomSetCollectionReaderName;
      Logger.info("The Resolver thinks " + atomSetCollectionReaderName);
    }
    if (htParams == null)
      htParams = new Hashtable();
    htParams.put("readerName", atomSetCollectionReaderName);
    if (atomSetCollectionReaderName.indexOf("(xml)") >= 0)
      atomSetCollectionReaderName = "Xml";
    String className = null;
    Class atomSetCollectionReaderClass;
    String err = null;
    try {
      className = getReaderClassBase(atomSetCollectionReaderName);
      atomSetCollectionReaderClass = Class.forName(className);//,true, Thread.currentThread().getContextClassLoader());
      atomSetCollectionReader = (AtomSetCollectionReader) atomSetCollectionReaderClass
          .newInstance();
    } catch (Exception e) {
      err = "File reader was not found:" + className;
      Logger.error(err);
      return err;
    }
    atomSetCollectionReader.initialize(htParams);
    AtomSetCollection atomSetCollection = atomSetCollectionReader
        .readAtomSetCollection(bufferedReader);
    bufferedReader.close();
    bufferedReader = null;
    return finalize(atomSetCollection, fullName);
  }

  static Object DOMResolve(Object DOMNode) throws Exception {
    String className = null;
    Class atomSetCollectionReaderClass;
    AtomSetCollectionReader atomSetCollectionReader; 
    Hashtable htParams = new Hashtable();
    String atomSetCollectionReaderName = getXmlType((JSObject) DOMNode);
    if (Logger.debugging) {
      Logger.debug("The Resolver thinks " + atomSetCollectionReaderName);
    }
    htParams.put("readerName", atomSetCollectionReaderName);
    try {
      className = classBase + "xml.XmlReader";
      atomSetCollectionReaderClass = Class.forName(className);
      atomSetCollectionReader = (AtomSetCollectionReader) atomSetCollectionReaderClass.newInstance();
    } catch (Exception e) {
      String err = "File reader was not found:" + className;
      Logger.error(err, e);
      return err;
    }
    atomSetCollectionReader.initialize();
    AtomSetCollection atomSetCollection =
      atomSetCollectionReader.readAtomSetCollectionFromDOM(DOMNode);
    return finalize(atomSetCollection, "DOM node");
  }

  private static final String CML_NAMESPACE_URI = "http://www.xml-cml.org/schema";

  private static String getXmlType(JSObject DOMNode) {
    String namespaceURI = (String) DOMNode.getMember("namespaceURI");
    String localName = (String) DOMNode.getMember("localName");
    if (namespaceURI.startsWith("http://www.molpro.net/"))
      return specialTags[SPECIAL_MOLPRO_DOM][0];
    if ("odyssey_simulation".equals(localName))
      return specialTags[SPECIAL_ODYSSEY_DOM][0];
    if ("arguslab".equals(localName))
      return specialTags[SPECIAL_ARGUS_DOM][0];
    if (namespaceURI.startsWith(CML_NAMESPACE_URI) || "cml".equals(localName))
      return specialTags[SPECIAL_CML_DOM][0];
    return "unidentified " + specialTags[SPECIAL_CML_DOM][0];
  }

  static Object finalize(AtomSetCollection atomSetCollection, String filename) {
    for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;) {
      atomSetCollection.setAtomSetAuxiliaryInfo("fileName", filename, i);
    }
    atomSetCollection.freeze();
    if (atomSetCollection.errorMessage != null)
      return atomSetCollection.errorMessage + "\nfor file " + filename + "\ntype "
          + atomSetCollection.fileTypeName;
    if (atomSetCollection.atomCount == 0)
      return "No atoms found\nfor file " + filename + "\ntype "
          + atomSetCollection.fileTypeName;
    return atomSetCollection;
  }

  static String determineAtomSetCollectionReader(BufferedReader bufferedReader, boolean returnLines)
      throws Exception {
    String[] lines = new String[16];
    LimitedLineReader llr = new LimitedLineReader(bufferedReader, 16384);
    int nLines = 0;
    for (int i = 0; i < lines.length; ++i) {
      lines[i] = llr.readLineWithNewline();
      if (lines[i].length() > 0)
        nLines++;
    }

    String readerName = checkSpecial(nLines, lines);
    
    if (readerName != null)
      return readerName;

    // run these loops forward ... easier for people to understand
    //file starts with added 4/26 to ensure no issue with NWChem files
    
    String leader = llr.getHeader(LEADER_CHAR_MAX);

    for (int i = 0; i < fileStartsWithRecords.length; ++i) {
      String[] recordTags = fileStartsWithRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (leader.startsWith(recordTag))
          return recordTags[0];
      }
    }
    for (int i = 0; i < lineStartsWithRecords.length; ++i) {
      String[] recordTags = lineStartsWithRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        for (int k = 0; k < lines.length; ++k) {
          if (lines[k].startsWith(recordTag))
            return recordTags[0];
        }
      }
    }

    String header = llr.getHeader(0);
    String type = null;
    for (int i = 0; i < containsRecords.length; ++i) {
      String[] recordTags = containsRecords[i];
      for (int j = 1; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (header.indexOf(recordTag) != -1) {
          type = recordTags[0];
          if (type.equals("Xml"))
            if (header.indexOf("XHTML") >= 0 || header.indexOf("xhtml") >= 0)
              break; //probably an error message from a server -- certainly not XML
            type = getXmlType(header);
          return type;
        }
      }
    }

    return (returnLines ? "\n" + lines[0] + "\n" + lines[1] + "\n" + lines[2] + "\n" : null);
  }

  private static String getXmlType(String header) throws Exception  {
    if (header.indexOf("http://www.molpro.net/") >= 0) {
      return specialTags[SPECIAL_MOLPRO_XML][0];
    }
    if (header.indexOf("odyssey") >= 0) {
      return specialTags[SPECIAL_ODYSSEY_XML][0];
    }
    if (header.indexOf("C3XML") >= 0) {
      return specialTags[SPECIAL_CHEM3D_XML][0];
    }
    if (header.indexOf("arguslab") >= 0) {
      return specialTags[SPECIAL_ARGUS_XML][0];
    }
    if (header.indexOf(CML_NAMESPACE_URI) >= 0
        || header.indexOf("cml:") >= 0) {
      return specialTags[SPECIAL_CML_XML][0];
    }
    return "unidentified " + specialTags[SPECIAL_CML_XML][0];
  }

  final static int SPECIAL_JME                = 0;
  final static int SPECIAL_MOPACGRAPHF        = 1;
  final static int SPECIAL_V3000              = 2;
  final static int SPECIAL_ODYSSEY            = 3;
  final static int SPECIAL_MOL                = 4;
  final static int SPECIAL_XYZ                = 5;
  final static int SPECIAL_FOLDINGXYZ         = 6;
  final static int SPECIAL_CUBE               = 7;
  
  final public static int SPECIAL_ARGUS_XML   = 8;
  final public static int SPECIAL_CML_XML     = 9;
  final public static int SPECIAL_CHEM3D_XML  = 10;
  final public static int SPECIAL_MOLPRO_XML  = 11;
  final public static int SPECIAL_ODYSSEY_XML = 12;
  
  final public static int SPECIAL_ARGUS_DOM   = 13;
  final public static int SPECIAL_CML_DOM     = 14;
  final public static int SPECIAL_CHEM3D_DOM  = 15;
  final public static int SPECIAL_MOLPRO_DOM  = 16;
  final public static int SPECIAL_ODYSSEY_DOM = 17;
  
  final public static String[][] specialTags = {
    { "Jme" },
    { "MopacGraphf" },
    { "V3000" },
    { "Odyssey" },    
    { "Mol" },
    { "Xyz" },
    { "FoldingXyz" },
    { "Cube" },
    
    { "argus(xml)" }, 
    { "cml(xml)" },
    { "chem3d(xml)" },
    { "molpro(xml)" },
    { "odyssey(xml)" },

    { "argus(DOM)" }, 
    { "cml(DOM)" },
    { "chem3d(DOM)" },
    { "molpro(DOM)" },
    { "odyssey(DOM)" }

  };

  final static String checkSpecial(int nLines, String[] lines) {
    // the order here is CRITICAL
    if (nLines == 1 && lines[0].length() > 0
        && Character.isDigit(lines[0].charAt(0)))
      return specialTags[SPECIAL_JME][0]; //only one line, and that line starts with a number 
    if (checkMopacGraphf(lines))
      return specialTags[SPECIAL_MOPACGRAPHF][0]; //must be prior to checkFoldingXyz and checkMol
    if (checkV3000(lines))
      return specialTags[SPECIAL_V3000][0];
    if (checkOdyssey(lines))
      return specialTags[SPECIAL_ODYSSEY][0];
    if (checkMol(lines))
      return specialTags[SPECIAL_MOL][0];
    if (checkXyz(lines))
      return specialTags[SPECIAL_XYZ][0];
    if (checkFoldingXyz(lines))
      return specialTags[SPECIAL_FOLDINGXYZ][0];
    if (checkCube(lines))
      return specialTags[SPECIAL_CUBE][0];
    return null;
  }
  
  final public static String getReaderFromType(String type) {
    type = type.toLowerCase();
    String base = null;
    if ((base = checkType(specialTags, type)) != null)
      return base;
    if ((base = checkType(fileStartsWithRecords, type)) != null)
      return base;
    if ((base = checkType(lineStartsWithRecords, type)) != null)
      return base;
    return checkType(containsRecords, type);
  }
  
  final private static String checkType(String[][] typeTags, String type) {
    for (int i = 0; i < typeTags.length; ++i)
      if (typeTags[i][0].toLowerCase().equals(type))
        return typeTags[i][0];
    return null;
  }
  
  ////////////////////////////////////////////////////////////////
  // file types that need special treatment
  ////////////////////////////////////////////////////////////////

  private static boolean checkOdyssey(String[] lines) {
    int i;
    for (i = 0; i < lines.length; i++)
      if (!lines[i].startsWith("C ") && lines[i].length() != 0)
        break;
    if (i >= lines.length 
        || lines[i].charAt(0) != ' ' 
        || (i = i + 2) >= lines.length
        || !TextFormat.replaceAllCharacters(lines[i], "\r\n", "").equals("0 1"))
        return false;
    return true;
  }
  
  private static boolean checkV3000(String[] lines) {
    if (lines[3].length() >= 6) {
      String line4trimmed = lines[3].trim();
      if (line4trimmed.endsWith("V3000"))
        return true;
    }
    return false;
  }

  private static boolean checkMol(String[] lines) {
    if (lines[3].length() >= 6) {
      String line4trimmed = lines[3].trim();
      if (line4trimmed.endsWith("V2000") ||
          line4trimmed.endsWith("v2000"))
        return true;
      try {
        Integer.parseInt(lines[3].substring(0, 3).trim());
        Integer.parseInt(lines[3].substring(3, 6).trim());
        return (lines[0].indexOf("@<TRIPOS>") != 0 
            && lines[1].indexOf("@<TRIPOS>") != 0
            && lines[2].indexOf("@<TRIPOS>") != 0
            );
      } catch (NumberFormatException nfe) {
      }
    }
    return false;
  }

  private static boolean checkXyz(String[] lines) {
    try {
      Integer.parseInt(lines[0].trim());
      return true;
    } catch (NumberFormatException nfe) {
    }
    return false;
  }

  /**
   * @param lines First lines of the files.
   * @return Indicates if the file may be a Folding@Home file.
   */
  private static boolean checkFoldingXyz(String[] lines) {
    // Checking first line: <number of atoms> <protein name>
    StringTokenizer tokens = new StringTokenizer(lines[0].trim(), " \t");
    if (tokens.countTokens() < 2)
      return false;
    try {
      Integer.parseInt(tokens.nextToken().trim());
    } catch (NumberFormatException nfe) {
      return false;
    }
    
    // Checking second line: <atom number> ...
    String secondLine = lines[1].trim();
    if (secondLine.length() == 0)
        secondLine = lines[2].trim();
    tokens = new StringTokenizer(secondLine, " \t");
    if (tokens.countTokens() == 0)
      return false;
    try {
      Integer.parseInt(tokens.nextToken().trim());
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }
  
  /**
   * @param lines First lines of the files.
   * @return Indicates if the file is a Mopac GRAPHF output file.
   */
  
  private static boolean checkMopacGraphf(String[] lines) {
    return (lines[0].indexOf("MOPAC-Graphical data") == 6);
  }

  private static boolean checkCube(String[] lines) {
    try {
      StringTokenizer tokens2 = new StringTokenizer(lines[2]);
      if (tokens2 == null || tokens2.countTokens() != 4)
        return false;
      Integer.parseInt(tokens2.nextToken());
      for (int i = 3; --i >= 0; )
        new Float(tokens2.nextToken());
      StringTokenizer tokens3 = new StringTokenizer(lines[3]);
      if (tokens3 == null || tokens3.countTokens() != 4)
        return false;
      Integer.parseInt(tokens3.nextToken());
      for (int i = 3; --i >= 0; )
        if ((new Float(tokens3.nextToken())).floatValue() < 0)
          return false;
      return true;
    } catch (NumberFormatException nfe) {
    }
    return false;
  }
/*
  private void dumpLines(String[] lines) {
      for (int i = 0; i < lines.length; i++) {
        Logger.info("\nLine "+i + " len " + lines[i].length());
        for (int j = 0; j < lines[i].length(); j++)
          Logger.info("\t"+(int)lines[i].charAt(j));
      }
      Logger.info("");
  }

*/
  
  ////////////////////////////////////////////////////////////////
  // these test files that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  final static int LEADER_CHAR_MAX = 20;
  
  final static String[] cubeRecords =
  {"Cube", "JVXL", "#JVXL"};

  final static String[] mol2Records =
  {"Mol2", "mol2", "@<TRIPOS>"};

  final static String[] webmoRecords =
  {"WebMO", "[HEADER]"};
  
  final static String[] moldenRecords =
  {"Molden", "[Molden"};

  final static String[][] fileStartsWithRecords =
  { cubeRecords, mol2Records, webmoRecords, moldenRecords};

  ////////////////////////////////////////////////////////////////
  // these test lines that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  final static String[] pqrRecords = 
  { "Pqr", "REMARK   1 PQR" };

  final static String[] pdbRecords = {
    "Pdb", "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK",
    "DBREF ", "SEQADV", "SEQRES", "MODRES", 
    "HELIX ", "SHEET ", "TURN  ",
    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",
    "ATOM  ", "HETATM", "MODEL ",
  };

  final static String[] shelxRecords =
  { "Shelx", "TITL ", "ZERR ", "LATT ", "SYMM ", "CELL " };

  final static String[] cifRecords =
  { "Cif", "data_", "_publ" };

  final static String[] ghemicalMMRecords =
  { "GhemicalMM", "!Header mm1gp", "!Header gpr" };

  final static String[] jaguarRecords =
  { "Jaguar", "  |  Jaguar version", };

  final static String[] hinRecords = 
  { "Hin", "mol "};

  final static String[] mdlRecords = 
  { "Mol", "$MDL "};

  final static String[] spartanSmolRecords =
  { "SpartanSmol", "INPUT="};

  final static String[] csfRecords =
  { "Csf", "local_transform"};
  
  final static String[][] lineStartsWithRecords =
  { pqrRecords, pdbRecords, shelxRecords, cifRecords, 
    ghemicalMMRecords, jaguarRecords, hinRecords, 
    mdlRecords, spartanSmolRecords, csfRecords, mol2Records};

  ////////////////////////////////////////////////////////////////
  // contains formats
  ////////////////////////////////////////////////////////////////

  final static String[] xmlRecords = 
  { "Xml", "<?xml", "<atom", "<molecule", "<reaction", "<cml", "<bond", ".dtd\"",
    "<list>", "<entry", "<identifier", "http://www.xml-cml.org/schema/cml2/core" };

  final static String[] gaussianRecords =
  { "Gaussian", "Entering Gaussian System", "Entering Link 1", "1998 Gaussian, Inc." };

  final static String[] mopacRecords =
  { "Mopac", "MOPAC 93 (c) Fujitsu", "MOPAC2002 (c) Fujitsu",
    "MOPAC FOR LINUX (PUBLIC DOMAIN VERSION)"};

  final static String[] qchemRecords = 
  { "Qchem", "Welcome to Q-Chem", "A Quantum Leap Into The Future Of Chemistry" };

  final static String[] gamessRecords =
  { "Gamess", "GAMESS" };

  final static String[] spartanBinaryRecords =
  { "SpartanSmol" , "|PropertyArchive", "_spartan", "spardir" };

  final static String[] spartanRecords =
  { "Spartan", "Spartan" };

  final static String[] adfRecords =
  { "Adf", "Amsterdam Density Functional" };
  
  final static String[] psiRecords =
  { "Psi", "    PSI  3"};
 
  final static String[] nwchemRecords =
  { "NWChem", " argument  1 = "};

  final static String[][] containsRecords =
  { xmlRecords, gaussianRecords, mopacRecords, qchemRecords, gamessRecords,
    spartanBinaryRecords, spartanRecords, mol2Records, adfRecords, psiRecords,
    nwchemRecords, 
  };

}

class LimitedLineReader {
  char[] buf;
  int cchBuf;
  int ichCurrent;

  LimitedLineReader(BufferedReader bufferedReader, int readLimit)
    throws Exception {
    bufferedReader.mark(readLimit);
    buf = new char[readLimit];
    cchBuf = bufferedReader.read(buf);
    ichCurrent = 0;
    bufferedReader.reset();
  }

  String getHeader(int n) {
    return (n == 0 ? new String(buf) : new String(buf, 0, Math.min(cchBuf, n)));
  }
  
  String readLineWithNewline() {
    // mth 2004 10 17
    // for now, I am going to put in a hack here
    // we have some CIF files with many lines of '#' comments
    // I believe that for all formats we can flush if the first
    // char of the line is a #
    // if this becomes a problem then we will need to adjust
    while (ichCurrent < cchBuf) {
      int ichBeginningOfLine = ichCurrent;
      char ch = 0;
      while (ichCurrent < cchBuf &&
             (ch = buf[ichCurrent++]) != '\r' && ch != '\n') {
      }
      if (ch == '\r' && ichCurrent < cchBuf && buf[ichCurrent] == '\n')
        ++ichCurrent;
      int cchLine = ichCurrent - ichBeginningOfLine;
      if (buf[ichBeginningOfLine] == '#') // flush comment lines;
        continue;
      StringBuffer sb = new StringBuffer(cchLine);
      sb.append(buf, ichBeginningOfLine, cchLine);
      return sb.toString();
    }
    //Logger.debug("org.jmol.adapter.smarter.Resolver short input buffer");
    // miguel 2005 01 26
    // for now, just return the empty string.
    // it will only affect the Resolver code
    // it will be easier to handle because then everyone does not
    // need to check for the null pointer
    //
    // If it becomes a problem, then change this to null and modify
    // all the code above to make sure that it tests for null before
    // attempting to invoke methods on the strings. 
    return "";
  }
}
