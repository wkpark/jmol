/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.io2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;

import javajs.api.GenericZipTools;
import javajs.api.GenericBinaryDocument;
import javajs.util.Binary;
import javajs.util.List;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.ZipTools;

import java.util.Hashtable;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.io.JmolBinary;
import org.jmol.io.JmolZipUtilities;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

public class JmolUtil implements JmolZipUtilities {

  public JmolUtil() {
    // for reflection
  }

  @Override
  public Object getAtomSetCollectionOrBufferedReaderFromZip(GenericZipTools zpt, JmolAdapter adapter,
                                                            InputStream is,
                                                            String fileName,
                                                            String[] zipDirectory,
                                                            Map<String, Object> htParams,
                                                            int subFilePtr,
                                                            boolean asBufferedReader) {

    // we're here because user is using | in a load file name
    // or we are opening a zip file.

    boolean doCombine = (subFilePtr == 1);
    htParams.put("zipSet", fileName);
    String[] subFileList = (String[]) htParams.get("subFileList");
    if (subFileList == null)
      subFileList = checkSpecialInZip(zipDirectory);
    String subFileName = (subFileList == null
        || subFilePtr >= subFileList.length ? null : subFileList[subFilePtr]);
    if (subFileName != null
        && (subFileName.startsWith("/") || subFileName.startsWith("\\")))
      subFileName = subFileName.substring(1);
    int selectedFile = 0;
    if (subFileName == null && htParams.containsKey("modelNumber")) {
      selectedFile = ((Integer) htParams.get("modelNumber")).intValue();
      if (selectedFile > 0 && doCombine)
        htParams.remove("modelNumber");
    }

    // zipDirectory[0] is the manifest if present
    String manifest = (String) htParams.get("manifest");
    boolean useFileManifest = (manifest == null);
    if (useFileManifest)
      manifest = (zipDirectory.length > 0 ? zipDirectory[0] : "");
    boolean haveManifest = (manifest.length() > 0);
    if (haveManifest) {
      if (Logger.debugging)
        Logger.debug("manifest for  " + fileName + ":\n" + manifest);
    }
    boolean ignoreErrors = (manifest.indexOf("IGNORE_ERRORS") >= 0);
    boolean selectAll = (manifest.indexOf("IGNORE_MANIFEST") >= 0);
    boolean exceptFiles = (manifest.indexOf("EXCEPT_FILES") >= 0);
    if (selectAll || subFileName != null)
      haveManifest = false;
    if (useFileManifest && haveManifest) {
      String path = JmolBinary.getManifestScriptPath(manifest);
      if (path != null)
        return JmolAdapter.NOTE_SCRIPT_FILE + fileName + path + "\n";
    }
    List<Object> vCollections = new List<Object>();
    Map<String, Object> htCollections = (haveManifest ? new Hashtable<String, Object>()
        : null);
    int nFiles = 0;
    // 0 entry is manifest

    // check for a Spartan directory. This is not entirely satisfying,
    // because we aren't reading the file in the proper sequence.
    // this code is a hack that should be replaced with the sort of code
    // running in FileManager now.

    Object ret = checkSpecialData(zpt, is, zipDirectory);
    if (ret instanceof String)
      return ret;
    SB data = (SB) ret;
    try {
      if (data != null) {
        BufferedReader reader = Binary.getBR(data.toString());
        if (asBufferedReader)
          return reader;
        ret = adapter
            .getAtomSetCollectionFromReader(fileName, reader, htParams);
        if (ret instanceof String)
          return ret;
        if (ret instanceof AtomSetCollection) {
          AtomSetCollection atomSetCollection = (AtomSetCollection) ret;
          if (atomSetCollection.errorMessage != null) {
            if (ignoreErrors)
              return null;
            return atomSetCollection.errorMessage;
          }
          return atomSetCollection;
        }
        if (ignoreErrors)
          return null;
        return "unknown reader error";
      }
      if (is instanceof BufferedInputStream)
        is = Binary.getPngZipStream((BufferedInputStream) is);
      ZipInputStream zis = (ZipInputStream) Binary.newZipInputStream(is);
      ZipEntry ze;
      if (haveManifest)
        manifest = '|' + manifest.replace('\r', '|').replace('\n', '|') + '|';
      while ((ze = zis.getNextEntry()) != null
          && (selectedFile <= 0 || vCollections.size() < selectedFile)) {
        if (ze.isDirectory())
          continue;
        String thisEntry = ze.getName();
        if (subFileName != null && !thisEntry.equals(subFileName))
          continue;
        if (subFileName != null)
          htParams.put("subFileName", subFileName);
        if (thisEntry.startsWith("JmolManifest") || haveManifest
            && exceptFiles == manifest.indexOf("|" + thisEntry + "|") >= 0)
          continue;
        byte[] bytes = Binary.getStreamBytes(zis, ze.getSize());
        //        String s = new String(bytes);
        //        System.out.println("ziputil " + s.substring(0, 100));
        if (Binary.isGzipB(bytes))
          bytes = Binary.getStreamBytes(ZipTools.getUnGzippedInputStream(bytes), -1);
        if (Binary.isZipB(bytes) || Binary.isPngZipB(bytes)) {
          BufferedInputStream bis = Binary.getBIS(bytes);
          String[] zipDir2 = Binary.getZipDirectoryAndClose(bis, "JmolManifest");
          bis = Binary.getBIS(bytes);
          Object atomSetCollections = getAtomSetCollectionOrBufferedReaderFromZip(
              zpt, adapter, bis, fileName + "|" + thisEntry, zipDir2, htParams,
              ++subFilePtr, asBufferedReader);
          if (atomSetCollections instanceof String) {
            if (ignoreErrors)
              continue;
            return atomSetCollections;
          } else if (atomSetCollections instanceof AtomSetCollection
              || atomSetCollections instanceof List<?>) {
            if (haveManifest && !exceptFiles)
              htCollections.put(thisEntry, atomSetCollections);
            else
              vCollections.addLast(atomSetCollections);
          } else if (atomSetCollections instanceof BufferedReader) {
            if (doCombine)
              zis.close();
            return atomSetCollections; // FileReader has requested a zip file
            // BufferedReader
          } else {
            if (ignoreErrors)
              continue;
            zis.close();
            return "unknown zip reader error";
          }
        } else if (JmolBinary.isPickleB(bytes)) {
          BufferedInputStream bis = Binary.getBIS(bytes);
          if (doCombine)
            zis.close();
          return bis;
        } else {
          String sData;
          if (Binary.isCompoundDocumentB(bytes)) {
            GenericBinaryDocument jd = (GenericBinaryDocument) Interface
                .getUtil("CompoundDocument");
            jd.setStream(Binary.getBIS(bytes), true);
            sData = jd.getAllDataFiles("Molecule", "Input").toString();
          } else {
            // could be a PNGJ file with an internal pdb.gz entry, for instance
            sData = Binary.fixUTF(bytes);
          }
          BufferedReader reader = Binary.getBR(sData);
          if (asBufferedReader) {
            if (doCombine)
              zis.close();
            return reader;
          }
          String fname = fileName + "|" + ze.getName();

          ret = adapter.getAtomSetCollectionFromReader(fname, reader, htParams);

          if (!(ret instanceof AtomSetCollection)) {
            if (ignoreErrors)
              continue;
            zis.close();
            return "" + ret;
          }
          if (haveManifest && !exceptFiles)
            htCollections.put(thisEntry, ret);
          else
            vCollections.addLast(ret);
          AtomSetCollection a = (AtomSetCollection) ret;
          if (a.errorMessage != null) {
            if (ignoreErrors)
              continue;
            zis.close();
            return a.errorMessage;
          }
        }
      }

      if (doCombine)
        zis.close();

      // if a manifest exists, it sets the files and file order

      if (haveManifest && !exceptFiles) {
        String[] list = PT.split(manifest, "|");
        for (int i = 0; i < list.length; i++) {
          String file = list[i];
          if (file.length() == 0 || file.indexOf("#") == 0)
            continue;
          if (htCollections.containsKey(file))
            vCollections.addLast(htCollections.get(file));
          else if (Logger.debugging)
            Logger.debug("manifested file " + file + " was not found in "
                + fileName);
        }
      }
      if (!doCombine)
        return vCollections;
      AtomSetCollection result = new AtomSetCollection("Array", null, null,
          vCollections);
      if (result.errorMessage != null) {
        if (ignoreErrors)
          return null;
        return result.errorMessage;
      }
      if (nFiles == 1)
        selectedFile = 1;
      if (selectedFile > 0 && selectedFile <= vCollections.size())
        return vCollections.get(selectedFile - 1);
      return result;

    } catch (Exception e) {
      if (ignoreErrors)
        return null;
      Logger.error("" + e);
      return "" + e;
    } catch (Error er) {
      Logger.errorEx(null, er);
      return "" + er;
    }
  }

  /**
   * called by SmarterJmolAdapter to see if we have a Spartan directory and, if
   * so, open it and get all the data into the correct order.
   * @param zpt 
   * 
   * @param is
   * @param zipDirectory
   * @return String data for processing
   */
  private static SB checkSpecialData(GenericZipTools zpt, InputStream is, String[] zipDirectory) {
    boolean isSpartan = false;
    // 0 entry is not used here
    for (int i = 1; i < zipDirectory.length; i++) {
      if (zipDirectory[i].endsWith(".spardir/")
          || zipDirectory[i].indexOf("_spartandir") >= 0) {
        isSpartan = true;
        break;
      }
    }
    if (!isSpartan)
      return null;
    SB data = new SB();
    data.append("Zip File Directory: ").append("\n")
        .append(Escape.eAS(zipDirectory, true)).append("\n");
    Map<String, String> fileData = new Hashtable<String, String>();
    zpt.getAllZipData(is, new String[] {}, "", "Molecule", fileData);
    String prefix = "|";
    String outputData = fileData.get(prefix + "output");
    if (outputData == null)
      outputData = fileData.get((prefix = "|" + zipDirectory[1]) + "output");
    data.append(outputData);
    String[] files = getSpartanFileList(prefix, getSpartanDirs(outputData));
    for (int i = 2; i < files.length; i++) {
      String name = files[i];
      if (fileData.containsKey(name))
        data.append(fileData.get(name));
      else
        data.append(name + "\n");
    }
    return data;
  }

  /**
   * 
   * Special loading for file directories. This method is called from the
   * FileManager via SmarterJmolAdapter. It's here because Resolver is the place
   * where all distinctions are made.
   * 
   * In the case of spt files, no need to load them; here we are just checking
   * for type.
   * 
   * In the case of .spardir directories, we need to provide a list of the
   * critical files that need loading and concatenation for the
   * SpartanSmolReader.
   * 
   * we return an array for which:
   * 
   * [0] file type (class prefix) or null for SPT file [1] header to add for
   * each BEGIN/END block (ignored) [2...] files to load and concatenate
   * 
   * @param name
   * @param type
   * @return array detailing action for this set of files
   */
  @Override
  public String[] spartanFileList(GenericZipTools zpt, String name, String type) {
    // make list of required files
    String[] dirNums = getSpartanDirs(type);
    if (dirNums.length == 0 && name.endsWith(".spardir.zip")
        && type.indexOf(".zip|output") >= 0) {
      // try again, with the idea that 
      String sname = name.replace('\\', '/');
      int pt = name.lastIndexOf(".spardir");
      pt = sname.lastIndexOf("/");
      // mac directory zipped up?
      sname = name + "|" + name.substring(pt + 1, name.length() - 4);
      return new String[] { "SpartanSmol", sname, sname + "/output" };
    }
    return getSpartanFileList(name, dirNums);
  }

  /**
   * read the output file from the Spartan directory and decide from that what
   * files need to be read and in what order - usually M0001 or a set of
   * Profiles. But Spartan saves the Profiles in alphabetical order, not
   * numerical. So we fix that here.
   * 
   * @param outputFileData
   * @return String[] list of files to read
   */
  private static String[] getSpartanDirs(String outputFileData) {
    if (outputFileData == null)
      return new String[] {};
    if (outputFileData.startsWith("java.io.FileNotFoundException")
        || outputFileData.startsWith("FILE NOT FOUND")
        || outputFileData.indexOf("<html") >= 0)
      return new String[] { "M0001" };
    List<String> v = new List<String>();
    String token;
    String lasttoken = "";
    try {
      StringTokenizer tokens = new StringTokenizer(outputFileData, " \t\r\n");
      while (tokens.hasMoreTokens()) {
        // profile file name is just before each right-paren:
        /*
         * MacSPARTAN '08 ENERGY PROFILE: x86/Darwin 130
         * 
         * Dihedral Move : C3 - C2 - C1 - O1 [ 4] -180.000000 .. 180.000000
         * Dihedral Move : C2 - C1 - O1 - H3 [ 4] -180.000000 .. 180.000000
         * 
         * 1 ) -180.00 -180.00 -504208.11982719 2 ) -90.00 -180.00
         * -504200.18593376
         * 
         * ...
         * 
         * 24 ) 90.00 180.00 -504200.18564495 25 ) 180.00 180.00
         * -504208.12129747
         * 
         * Found a local maxima E = -504178.25455465 [ 3 3 ]
         * 
         * 
         * Reason for exit: Successful completion Mechanics CPU Time : 1:51.42
         * Mechanics Wall Time: 12:31.54
         */
        if ((token = tokens.nextToken()).equals(")"))
          v.addLast(lasttoken);
        else if (token.equals("Start-")
            && tokens.nextToken().equals("Molecule"))
          v.addLast(PT.split(tokens.nextToken(), "\"")[1]);
        lasttoken = token;
      }
    } catch (Exception e) {
      //
    }
    return v.toArray(new String[v.size()]);
  }

  /**
   * returns the list of files to read for every Spartan spardir. Simple numbers
   * are assumed to be Profiles; others are models.
   * 
   * @param name
   * @param dirNums
   * @return String[] list of files to read given a list of directory names
   * 
   */
  private static String[] getSpartanFileList(String name, String[] dirNums) {
    String[] files = new String[2 + dirNums.length * 5];
    files[0] = "SpartanSmol";
    files[1] = "Directory Entry ";
    int pt = 2;
    name = name.replace('\\', '/');
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1);
    for (int i = 0; i < dirNums.length; i++) {
      String path = name
          + (Character.isDigit(dirNums[i].charAt(0)) ? "/Profile." + dirNums[i]
              : "/" + dirNums[i]);
      files[pt++] = path + "/#JMOL_MODEL " + dirNums[i];
      files[pt++] = path + "/input";
      files[pt++] = path + "/archive";
      files[pt++] = path + "/Molecule:asBinaryString";
      files[pt++] = path + "/proparc";
    }
    return files;
  }

  /**
   * called by SmarterJmolAdapter to see if we can automatically assign a file
   * from the zip file. If so, return a subfile list for this file. The first
   * element of the list is left empty -- it would be the zipfile name.
   * 
   * Assignment can be made if (1) there is only one file in the collection or
   * (2) if the first file is xxxx.spardir/
   * 
   * Note that __MACOS? files are ignored by the ZIP file reader.
   * 
   * @param zipDirectory
   * @return subFileList
   */
  static String[] checkSpecialInZip(String[] zipDirectory) {
    String name;
    return (zipDirectory.length < 2 ? null : (name = zipDirectory[1])
        .endsWith(".spardir/") || zipDirectory.length == 2 ? new String[] { "",
        (name.endsWith("/") ? name.substring(0, name.length() - 1) : name) }
        : null);
  }

  @Override
  public byte[] getCachedPngjBytes(JmolBinary jmb, String pathName) {
    Logger.info("JmolUtil checking PNGJ cache for " + pathName);
    String shortName = shortSceneFilename(pathName);
    if (jmb.pngjCache == null
        && !jmb.clearAndCachePngjFile(new String[] { pathName, null }))
      return null;
    boolean isMin = (pathName.indexOf(".min.") >= 0);
    if (!isMin) {
      String cName = jmb.fm.getCanonicalName(Binary.getZipRoot(pathName));
      if (!jmb.pngjCache.containsKey(cName)
          && !jmb.clearAndCachePngjFile(new String[] { pathName, null }))
        return null;
      if (pathName.indexOf("|") < 0)
        shortName = cName;
    }
    if (jmb.pngjCache.containsKey(shortName)) {
      Logger.info("FileManager using memory cache " + shortName);
      return (byte[]) jmb.pngjCache.get(shortName);
    }
    //    for (String key : pngjCache.keySet())
    //    System.out.println(" key=" + key);
    //System.out.println("FileManager memory cache size=" + pngjCache.size()
    //  + " did not find " + pathName + " as " + shortName);
    if (!isMin || !jmb.clearAndCachePngjFile(new String[] { pathName, null }))
      return null;
    Logger.info("FileManager using memory cache " + shortName);
    return (byte[]) jmb.pngjCache.get(shortName);
  }

  @Override
  public boolean cachePngjFile(JmolBinary jmb, String[] data) {
    data[0] = Binary.getZipRoot(data[0]);
    String shortName = shortSceneFilename(data[0]);
    try {
      data[1] = ZipTools.cacheZipContents( 
          Binary.getPngZipStream((BufferedInputStream) jmb.fm
              .getBufferedInputStreamOrErrorMessageFromName(data[0], null,
                  false, false, null, false, true)), shortName, jmb.pngjCache, false);
    } catch (Exception e) {
      return false;
    }
    if (data[1] == null)
      return false;
    byte[] bytes = data[1].getBytes();
    jmb.pngjCache.put(jmb.fm.getCanonicalName(data[0]), bytes); // marker in case the .all. file is changed
    if (shortName.indexOf("_scene_") >= 0) {
      jmb.pngjCache.put(shortSceneFilename(data[0]), bytes); // good for all .min. files of this scene set
      bytes = (byte[]) jmb.pngjCache.remove(shortName + "|state.spt");
      if (bytes != null)
        jmb.pngjCache.put(shortSceneFilename(data[0] + "|state.spt"), bytes);
    }
    for (String key : jmb.pngjCache.keySet())
      System.out.println(key);
    return true;
  }

  private static String shortSceneFilename(String pathName) {
    int pt = pathName.indexOf("_scene_") + 7;
    if (pt < 7)
      return pathName;
    String s = "";
    if (pathName.endsWith("|state.spt")) {
      int pt1 = pathName.indexOf('.', pt);
      if (pt1 < 0)
        return pathName;
      s = pathName.substring(pt, pt1);
    }
    int pt2 = pathName.lastIndexOf("|");
    return pathName.substring(0, pt) + s
        + (pt2 > 0 ? pathName.substring(pt2) : "");
  }

}
