/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-05 09:07:28 -0500 (Thu, 05 Apr 2007) $
 * $Revision: 7326 $
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
package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;

import javajs.util.Binary;
import javajs.util.List;
import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public class JmolBinary {

  public FileManager fm;

  public Map<String, Object> pngjCache;
  public Map<String, byte[]> spardirCache;

  public JmolBinary(FileManager fm) {
    this.fm = fm;
  }
 
  public static final String JPEG_CONTINUE_STRING = " #Jmol...\0";
  
  public final static String PMESH_BINARY_MAGIC_NUMBER = "PM\1\0";

  private final static String DELPHI_BINARY_MAGIC_NUMBER = "\24\0\0\0";

  public static String determineSurfaceTypeIs(InputStream is) {
    BufferedReader br;
    try {
      br = Binary.getBufferedReader(new BufferedInputStream(is), "ISO-8859-1");
    } catch (IOException e) {
      return null;
    }
    return determineSurfaceFileType(br);
  }
  
  public static String determineSurfaceFileType(BufferedReader bufferedReader) {
    // JVXL should be on the FIRST line of the file, but it may be 
    // after comments or missing.

    // Apbs, Jvxl, or Cube, also efvet and DHBD

    String line = null;
    LimitedLineReader br = null;
    
    try {
      br = new LimitedLineReader(bufferedReader, 16000);
      line = br.getHeader(0);
    } catch (Exception e) {
      //
    }
    if (br == null || line == null || line.length() == 0)
      return null;

    //for (int i = 0; i < 220; i++)
    //  System.out.print(" " + i + ":" + (0 + line.charAt(i)));
    //System.out.println("");
    
    switch (line.charAt(0)) {
    case '@':
      if (line.indexOf("@text") == 0)
        return "Kinemage";
      break;
    case '#':
      if (line.indexOf(".obj") >= 0)
        return "Obj"; // #file: pymol.obj
      if (line.indexOf("MSMS") >= 0)
        return "Msms";
      break;
    case '&':
      if (line.indexOf("&plot") == 0)
        return "Jaguar";
      break;
    case '\r':
    case '\n':
      if (line.indexOf("ZYX") >= 0)
        return "Xplor";
      break;
    }
    if (line.indexOf("Here is your gzipped map") >= 0)
      return "UPPSALA" + line;
    if (line.indexOf("! nspins") >= 0)
      return "CastepDensity";
    if (line.indexOf("<jvxl") >= 0 && line.indexOf("<?xml") >= 0)
      return "JvxlXml";
    if (line.indexOf("#JVXL+") >= 0)
      return "Jvxl+";
    if (line.indexOf("#JVXL") >= 0)
      return "Jvxl";
    if (line.indexOf("<efvet ") >= 0)
      return "Efvet";
    if (line.indexOf("usemtl") >= 0)
      return "Obj";
    if (line.indexOf("# object with") == 0)
      return "Nff";
    if (line.indexOf("BEGIN_DATAGRID_3D") >= 0 || line.indexOf("BEGIN_BANDGRID_3D") >= 0)
      return "Xsf";
    // binary formats: problem here is that the buffered reader
    // may be translating byte sequences into unicode
    // and thus shifting the offset
    int pt0 = line.indexOf('\0');
    if (pt0 >= 0) {
      if (line.indexOf(PMESH_BINARY_MAGIC_NUMBER) == 0)
        return "Pmesh";
      if (line.indexOf(DELPHI_BINARY_MAGIC_NUMBER) == 0)
        return "DelPhi";
      if (line.indexOf("MAP ") == 208)
        return "Mrc";
      if (line.length() > 37 && (line.charAt(36) == 0 && line.charAt(37) == 100 
          || line.charAt(36) == 0 && line.charAt(37) == 100)) { 
           // header19 (short)100
          return "Dsn6";
      }
    }
    
    if (line.indexOf(" 0.00000e+00 0.00000e+00      0      0\n") >= 0)
      return "Uhbd"; // older APBS http://sourceforge.net/p/apbs/code/ci/9527462a39126fb6cd880924b3cc4880ec4b78a9/tree/src/mg/vgrid.c
    
    // Apbs, Jvxl, Obj, or Cube, maybe formatted Plt

    line = br.readLineWithNewline();
    if (line.indexOf("object 1 class gridpositions counts") == 0)
      return "Apbs";

    String[] tokens = PT.getTokens(line);
    String line2 = br.readLineWithNewline();// second line
    if (tokens.length == 2 && PT.parseInt(tokens[0]) == 3
        && PT.parseInt(tokens[1]) != Integer.MIN_VALUE) {
      tokens = PT.getTokens(line2);
      if (tokens.length == 3 && PT.parseInt(tokens[0]) != Integer.MIN_VALUE
          && PT.parseInt(tokens[1]) != Integer.MIN_VALUE
          && PT.parseInt(tokens[2]) != Integer.MIN_VALUE)
        return "PltFormatted";
    }
    String line3 = br.readLineWithNewline(); // third line
    if (line.startsWith("v ") && line2.startsWith("v ") && line3.startsWith("v "))
        return "Obj";
    //next line should be the atom line
    int nAtoms = PT.parseInt(line3);
    if (nAtoms == Integer.MIN_VALUE)
      return (line3.indexOf("+") == 0 ? "Jvxl+" : null);
    if (nAtoms >= 0)
      return "Cube"; //Can't be a Jvxl file
    nAtoms = -nAtoms;
    for (int i = 4 + nAtoms; --i >= 0;)
      if ((line = br.readLineWithNewline()) == null)
        return null;
    int nSurfaces = PT.parseInt(line);
    if (nSurfaces == Integer.MIN_VALUE)
      return null;
    return (nSurfaces < 0 ? "Jvxl" : "Cube"); //Final test looks at surface definition line
  }

  public static boolean isPickleS(InputStream is) {
    return isPickleB(Binary.getMagic(is, 2));
  }

  public static boolean isPickleB(byte[] bytes) {    
    return (bytes != null && bytes.length >= 2 
        && bytes[0] == (byte) 0x7D && bytes[1] == (byte) 0x71);
}

  public static String getEmbeddedScript(String script) {
    if (script == null)
      return script;
    int pt = script.indexOf(JC.EMBEDDED_SCRIPT_TAG);
    if (pt < 0)
      return script;
    int pt1 = script.lastIndexOf("/*", pt);
    int pt2 = script.indexOf((script.charAt(pt1 + 2) == '*' ? "*" : "") + "*/",
        pt);
    if (pt1 >= 0 && pt2 >= pt)
      script = script.substring(
          pt + JC.EMBEDDED_SCRIPT_TAG.length(), pt2)
          + "\n";
    while ((pt1 = script.indexOf(JPEG_CONTINUE_STRING)) >= 0)
      script = script.substring(0, pt1)
          + script.substring(pt1 + JPEG_CONTINUE_STRING.length() + 4);
    if (Logger.debugging)
      Logger.debug(script);
    return script;
  }

  static JmolZipUtilities jzu;
  
  static JmolZipUtilities getJzu() {
    return (jzu == null ? jzu = (JmolZipUtilities) Interface.getOption("io2.JmolUtil") : jzu);
  }

  public byte[] getCachedPngjBytes(String pathName) {
    return (pathName.indexOf(".png") < 0 ? null : getJzu().getCachedPngjBytes(this, pathName));
  }

  public boolean clearAndCachePngjFile(String[] data) {
      pngjCache = new Hashtable<String, Object>();
      if (data == null)
        return false;
      data[1] = null;
      if (data[0] == null)
        return false;
    return getJzu().cachePngjFile(this, data);
  }
  
  public void spardirPut(String name, byte[] bytes) {
    if (spardirCache == null)
      spardirCache = new Hashtable<String, byte[]>();
    spardirCache.put(name, bytes);
  }
  
  public void clearPngjCache(String fileName) {
    if (fileName != null && (pngjCache == null || !pngjCache.containsKey(fileName)))
        return;
      pngjCache = null;
      Logger.info("PNGJ cache cleared");
  }

  /**
   * A rather complicated means of reading a ZIP file, which could be a 
   * single file, or it could be a manifest-organized file, or it could be
   * a Spartan directory.
   * 
   * @param adapter 
   * 
   * @param is 
   * @param fileName 
   * @param zipDirectory 
   * @param htParams 
   * @param asBufferedReader 
   * @return a single atomSetCollection
   * 
   */
  public static Object getAtomSetCollectionOrBufferedReaderFromZip(JmolAdapter adapter, InputStream is, String fileName, String[] zipDirectory,
                             Map<String, Object> htParams, boolean asBufferedReader) {
    return getJzu().getAtomSetCollectionOrBufferedReaderFromZip(Binary.getJzt(), adapter, is, fileName, zipDirectory, htParams, 1, asBufferedReader);
  }

  public static String[] spartanFileList(String name, String zipDirectory) {
    return getJzu().spartanFileList(Binary.getJzt(), name, zipDirectory);
  }
  
  public static void getFileReferences(String script, List<String> fileList) {
    for (int ipt = 0; ipt < FileManager.scriptFilePrefixes.length; ipt++) {
      String tag = FileManager.scriptFilePrefixes[ipt];
      int i = -1;
      while ((i = script.indexOf(tag, i + 1)) >= 0) {
        String s = PT.getQuotedStringAt(script, i);
        if (s.indexOf("::") >= 0)
          s = PT.split(s, "::")[1];
        fileList.addLast(s);
      }
    }
  }

  /**
   * check a JmolManifest for a reference to a script file (.spt)
   * 
   * @param manifest
   * @return null, "", or a directory entry in the ZIP file
   */

  public static String getManifestScriptPath(String manifest) {
    if (manifest.indexOf("$SCRIPT_PATH$") >= 0)
      return "";
    String ch = (manifest.indexOf('\n') >= 0 ? "\n" : "\r");
    if (manifest.indexOf(".spt") >= 0) {
      String[] s = PT.split(manifest, ch);
      for (int i = s.length; --i >= 0;)
        if (s[i].indexOf(".spt") >= 0)
          return "|" + PT.trim(s[i], "\r\n \t");
    }
    return null;
  }

  public static BufferedReader getBufferedReaderForResource(Viewer vwr, 
                                                            Object resourceClass,
                                              String classPath,
                                              String resourceName)
       throws IOException {

     /**
      * @j2sNative
      * 
      *            resourceName = vwr.vwrOptions.get("codePath") +
      *            classPath + resourceName;
      * 
      */
     {
       URL url = resourceClass.getClass().getResource(resourceName);
       boolean ret = true;
       if (url == null) {
         System.err.println("Couldn't find file: " + classPath + resourceName);
         throw new IOException();
       }
       if (ret) // avoids dead code message
         return Binary.getBufferedReader(new BufferedInputStream(
             (InputStream) url.getContent()), null);
     }
     // JavaScript only; here and not in JavaDoc to preserve Eclipse search reference
     return (BufferedReader) vwr.getBufferedReaderOrErrorMessageFromName(
         resourceName, new String[] { null, null }, false);
   }

}

