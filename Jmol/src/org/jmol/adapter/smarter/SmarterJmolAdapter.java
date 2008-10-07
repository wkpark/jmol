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

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolFileReaderInterface;
import org.jmol.util.CompoundDocument;
import org.jmol.util.Escape;
import org.jmol.util.TextFormat;
import org.jmol.util.ZipUtil;
import org.jmol.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SmarterJmolAdapter extends JmolAdapter {

  public SmarterJmolAdapter() {
    super("SmarterJmolAdapter");
  }

  /* **************************************************************
   * the file related methods
   * **************************************************************/

  public final static String PATH_KEY = ".PATH";
  public final static String PATH_SEPARATOR =
    System.getProperty("path.separator");
  
  public void finish(Object clientFile) {
    ((AtomSetCollection)clientFile).finish();
  }

  public String[] specialLoad(String name, String type) {
    return Resolver.specialLoad(name, type);  
  }
  

  public Object openBufferedReader(String name, String type,
                                   BufferedReader bufferedReader, Hashtable htParams) {
    return staticOpenBufferedReader(name, type, bufferedReader, htParams);
  }

  private static Object staticOpenBufferedReader(String name, String type, BufferedReader bufferedReader, Hashtable htParams) {
    //FileOpenThread, TesetSmarterJmolAdapter
    try {
      Object atomSetCollectionOrErrorMessage =
        Resolver.resolve(name, type, bufferedReader, htParams, -1);
      if (atomSetCollectionOrErrorMessage instanceof String)
        return atomSetCollectionOrErrorMessage;
      if (atomSetCollectionOrErrorMessage instanceof AtomSetCollection) {
        AtomSetCollection atomSetCollection =
          (AtomSetCollection)atomSetCollectionOrErrorMessage;
        if (atomSetCollection.errorMessage != null)
          return atomSetCollection.errorMessage;
        return atomSetCollection;
      }
      return "unknown reader error";
    } catch (Exception e) {
      Logger.error(null, e);
      bufferedReader = null;
      return "" + e;
    }
  }

  public Object openBufferedReaders(JmolFileReaderInterface fileReader, String[] names, String[] types,
                                    Hashtable[] htParams) {
    return staticOpenBufferedReaders(fileReader, names, types, htParams);
  }

  private static Object staticOpenBufferedReaders(
                                                  JmolFileReaderInterface fileReader,
                                                  String[] names,
                                                  String[] types,
                                                  Hashtable[] htParams) {
    //FilesOpenThread
    int size = names.length;
    AtomSetCollection[] atomSetCollections = new AtomSetCollection[size];
    for (int i = 0; i < size; i++) {
      try {
        BufferedReader reader = fileReader.getBufferedReader(i);
        if (reader == null)
          return null;
        Object atomSetCollectionOrErrorMessage = Resolver.resolve(names[i],
            (types == null ? null : types[i]), reader, (htParams == null ? null
                : htParams[i]), i);
        if (atomSetCollectionOrErrorMessage instanceof String)
          return atomSetCollectionOrErrorMessage;
        if (atomSetCollectionOrErrorMessage instanceof AtomSetCollection) {
          atomSetCollections[i] = (AtomSetCollection) atomSetCollectionOrErrorMessage;
          if (atomSetCollections[i].errorMessage != null)
            return atomSetCollections[i].errorMessage;
        } else {
          return "unknown reader error";
        }
      } catch (Exception e) {
        Logger.error(null, e);
        return "" + e;
      }
    }
    if (htParams != null && htParams[0].containsKey("trajectorySteps")) {
      // this is one model with a set of coordinates from a 
      // molecular dynamics calculation
      // all the htParams[] entries point to the same Hashtable
      atomSetCollections[0].finalizeTrajectory((Vector) htParams[0]
          .get("trajectorySteps"));
      return atomSetCollections[0];
    }
    AtomSetCollection result = new AtomSetCollection(atomSetCollections);
    if (result.errorMessage != null)
      return result.errorMessage;
    return result;
  }

  public Object openZipFiles(InputStream is, String fileName, String[] zipDirectory,
                             Hashtable htParams, boolean asBufferedReader) {
    return staticOpenZipFiles(is, fileName, zipDirectory, htParams, 1, asBufferedReader);
  }

  private static Object staticOpenZipFiles(InputStream is, String fileName, String[] zipDirectory,
                             Hashtable htParams, int subFilePtr, boolean asBufferedReader) {

    boolean doCombine = (subFilePtr == 1);
    int[] params = (htParams == null ? null : (int[]) htParams
        .get("params"));
    String[] subFileList = (htParams == null ? null : (String[]) htParams
        .get("subFileList"));
    String subFileName = (subFileList == null
        || subFilePtr >= subFileList.length ? null : subFileList[subFilePtr]);
    if (subFileName != null 
        && (subFileName.startsWith("/") || subFileName.startsWith("\\")))
      subFileName = subFileName.substring(1);
    int selectedFile = (params == null ? 1 : params[0]);
    if (selectedFile > 0 && doCombine && params != null)
      params[0] = 0;
    // zipDirectory[0] is the manifest if present
    String manifest = (htParams == null ? null : (String) htParams
        .get("manifest"));
    if (manifest == null)
      manifest = (zipDirectory.length > 0 ? zipDirectory[0] : "");
    boolean haveManifest = (manifest.length() > 0);
    if (haveManifest) {
      if (Logger.debugging)
        Logger.info("manifest for  " + fileName + ":\n" + manifest);
      manifest = '|' + manifest.replace('\r', '|').replace('\n', '|') + '|';
    }
    boolean ignoreErrors = (manifest.indexOf("IGNORE_ERRORS") >= 0);
    boolean selectAll = (manifest.indexOf("IGNORE_MANIFEST") >= 0);
    boolean exceptFiles = (manifest.indexOf("EXCEPT_FILES") >= 0);
    if (selectAll || subFileName != null)
      haveManifest = false;
    Vector vCollections = new Vector();
    Hashtable htCollections = (haveManifest ? new Hashtable() : null);
    boolean isSpartan = false;
    //0 entry is manifest
    for (int i = 1; i < zipDirectory.length; i++) {
      if (zipDirectory[i].indexOf("_spartandir") >= 0) {
        isSpartan = true;
        break;
      }
    }
    int nFiles = 0;
    StringBuffer data = new StringBuffer();
    if (isSpartan) {
      data = new StringBuffer();
      data.append("Zip File Directory: ").append("\n").append(
          Escape.escape(zipDirectory)).append("\n");
    }
    ZipInputStream zis = (is instanceof ZipInputStream ? (ZipInputStream) is 
        : new ZipInputStream(new BufferedInputStream(is)));
    ZipEntry ze;
    try {
      while ((ze = zis.getNextEntry()) != null
          && (selectedFile <= 0 || vCollections.size() < selectedFile)) {
        if (ze.isDirectory())
          continue;
        byte[] bytes = ZipUtil.getZipEntryAsBytes(zis);
        String thisEntry = ze.getName();
        if (subFileName != null && !thisEntry.equals(subFileName))
          continue;
        if (thisEntry.equals("JmolManifest") || haveManifest
            && exceptFiles == manifest.indexOf("|" + thisEntry + "|") >= 0)
          continue;
        if (isSpartan) {
          data.append("\nBEGIN Zip File Entry: ").append(thisEntry)
              .append("\n");
          data.append(new String(bytes));
          data.append("\nEND Zip File Entry: ").append(thisEntry).append("\n");
          data.append(ze.getName()).append("/n");
        } else if (ZipUtil.isZipFile(bytes)) {
          BufferedInputStream bis = new BufferedInputStream(
              new ByteArrayInputStream(bytes));
          String[] zipDir2 = ZipUtil.getZipDirectoryAndClose(bis, true);
          bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
          Object clientFiles = staticOpenZipFiles(bis, fileName
              + "|" + thisEntry, zipDir2, htParams, ++subFilePtr, asBufferedReader);
          if (clientFiles instanceof String) {
            if (ignoreErrors)
              continue;
            return clientFiles;
          } else if (clientFiles instanceof AtomSetCollection
              || clientFiles instanceof Vector) {
            if (haveManifest && !exceptFiles)
              htCollections.put(thisEntry, clientFiles);
            else
              vCollections.addElement(clientFiles);
          } else if (clientFiles instanceof BufferedReader) {
            if (doCombine)
              zis.close();
            return clientFiles; // FileReader has requested a zip file BufferedReader
          } else {
            if (ignoreErrors)
              continue;
            zis.close();
            return "unknown zip reader error";
          }
        } else {
          String sData = (CompoundDocument.isCompoundDocument(bytes) ? (new CompoundDocument(
              new BufferedInputStream(new ByteArrayInputStream(bytes))))
              .getAllData().toString()
              : new String(bytes));
          BufferedReader reader = new BufferedReader(new StringReader(sData)); 
          if (asBufferedReader) {
            if (doCombine)
              zis.close();
            return reader;
          }
          Object clientFile = Resolver.resolve(fileName + "|" + ze.getName(),
              null, reader, htParams, -1);
          if (clientFile instanceof AtomSetCollection) {
            if (haveManifest && !exceptFiles)
              htCollections.put(thisEntry, clientFile);
            else
              vCollections.addElement(clientFile);
            AtomSetCollection a = (AtomSetCollection) clientFile;
            if (a.errorMessage != null) {
              if (ignoreErrors)
                continue;
              zis.close();
              return a.errorMessage;
            }
          } else {
            if (ignoreErrors)
              continue;
            zis.close();
            return "unknown reader error";
          }
        }
      }
      if (doCombine)
        zis.close();
      if (isSpartan) {
        BufferedReader reader = new BufferedReader(new StringReader(data.toString()));
        if (asBufferedReader) {
          return reader;
        }
        Object clientFile = Resolver.resolve(fileName, null, reader);
        if (clientFile instanceof String)
          return clientFile;
        if (clientFile instanceof AtomSetCollection) {
          AtomSetCollection atomSetCollection = (AtomSetCollection) clientFile;
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

      // if a manifest exists, it sets the files and file order

      if (haveManifest && !exceptFiles) {
        String[] list = TextFormat.split(manifest, '|');
        for (int i = 0; i < list.length; i++) {
          String file = list[i];
          if (file.length() == 0 || file.indexOf("#") == 0)
            continue;
          if (htCollections.containsKey(file))
            vCollections.add(htCollections.get(file));
          else if (Logger.debugging)
            Logger.info("manifested file " + file + " was not found in "
                + fileName);
        }
      }

      if (!doCombine)
        return vCollections;
      AtomSetCollection result = new AtomSetCollection(vCollections);
      if (result.errorMessage != null) {
        if (ignoreErrors)
          return null;
        return result.errorMessage;
      }
      if (nFiles == 1)
        selectedFile = 1;
      if (selectedFile > 0 && selectedFile <= vCollections.size())
        return vCollections.elementAt(selectedFile - 1);
      return result;

    } catch (Exception e) {
      if (ignoreErrors)
        return null;
      Logger.error(null, e);
      return "" + e;
    }
  }

  public Object openDOMReader(Object DOMNode) {
    return staticOpenDOMReader(DOMNode);
  }

  private static Object staticOpenDOMReader(Object DOMNode) {
    try {
      Object atomSetCollectionOrErrorMessage = 
        Resolver.DOMResolve(DOMNode);
      if (atomSetCollectionOrErrorMessage instanceof String)
        return atomSetCollectionOrErrorMessage;
      if (atomSetCollectionOrErrorMessage instanceof AtomSetCollection) {
        AtomSetCollection atomSetCollection =
          (AtomSetCollection)atomSetCollectionOrErrorMessage;
        if (atomSetCollection.errorMessage != null)
          return atomSetCollection.errorMessage;
        return atomSetCollection;
      }
      return "unknown DOM reader error";
    } catch (Exception e) {
      Logger.error(null, e);
      return "" + e;
    }
  }

  public String getFileTypeName(Object clientFile) {
    return staticGetFileTypeName(clientFile);
  }

  private static String staticGetFileTypeName(Object clientFile) {
    if (clientFile == null)
      return null;
    if (clientFile instanceof BufferedReader)
      return Resolver.getFileType((BufferedReader)clientFile);
    if (clientFile instanceof AtomSetCollection)
      return ((AtomSetCollection)clientFile).fileTypeName;
    return null;
  }

  public String getAtomSetCollectionName(Object clientFile) {
    return ((AtomSetCollection)clientFile).collectionName;
  }
  
  public Properties getAtomSetCollectionProperties(Object clientFile) {
    return ((AtomSetCollection)clientFile).atomSetCollectionProperties;
  }

  public Hashtable getAtomSetCollectionAuxiliaryInfo(Object clientFile) {
    return ((AtomSetCollection)clientFile).atomSetCollectionAuxiliaryInfo;
  }

  public int getAtomSetCount(Object clientFile) {
    return ((AtomSetCollection)clientFile).atomSetCount;
  }

  public int getAtomSetNumber(Object clientFile, int atomSetIndex) {
    return ((AtomSetCollection)clientFile).getAtomSetNumber(atomSetIndex);
  }

  public String getAtomSetName(Object clientFile, int atomSetIndex) {
    return ((AtomSetCollection)clientFile).getAtomSetName(atomSetIndex);
  }
  
  public Properties getAtomSetProperties(Object clientFile, int atomSetIndex) {
    return ((AtomSetCollection)clientFile).getAtomSetProperties(atomSetIndex);
  }
  
  public Hashtable getAtomSetAuxiliaryInfo(Object clientFile, int atomSetIndex) {
    return ((AtomSetCollection) clientFile)
        .getAtomSetAuxiliaryInfo(atomSetIndex);
  }

  /* **************************************************************
   * The frame related methods
   * **************************************************************/

  public int getEstimatedAtomCount(Object clientFile) {
    return ((AtomSetCollection)clientFile).getAtomCount();
  }

  public boolean coordinatesAreFractional(Object clientFile) {
    return ((AtomSetCollection)clientFile).coordinatesAreFractional;
  }

  public float[] getNotionalUnitcell(Object clientFile) {
    return ((AtomSetCollection)clientFile).notionalUnitCell;
  }

  public float[] getPdbScaleMatrix(Object clientFile) {
    float[] a = ((AtomSetCollection)clientFile).notionalUnitCell;
    if (a.length < 22)
      return null;
    float[] b = new float[16];
    for (int i = 0; i < 16; i++)
      b[i] = a[6 + i];
    return b;
  }

  public float[] getPdbScaleTranslate(Object clientFile) {
    float[] a = ((AtomSetCollection)clientFile).notionalUnitCell;
    if (a.length < 22)
      return null;
    float[] b = new float[3];
    b[0] = a[6 + 4*0 + 3];
    b[1] = a[6 + 4*1 + 3];
    b[2] = a[6 + 4*2 + 3];
    return b;
  }
  
  ////////////////////////////////////////////////////////////////

  public JmolAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator((AtomSetCollection)clientFile);
  }

  public JmolAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator((AtomSetCollection)clientFile);
  }

  public JmolAdapter.StructureIterator
    getStructureIterator(Object clientFile) {
    AtomSetCollection atomSetCollection = (AtomSetCollection)clientFile;
    return atomSetCollection.structureCount == 0 ? null : new StructureIterator(atomSetCollection);
  }

  /* **************************************************************
   * the frame iterators
   * **************************************************************/
  class AtomIterator extends JmolAdapter.AtomIterator {
    AtomSetCollection atomSetCollection;
    int iatom;
    Atom atom;
    int atomCount;
    Atom[] atoms;

    AtomIterator(AtomSetCollection atomSetCollection) {
      this.atomSetCollection = atomSetCollection;
      this.atomCount = atomSetCollection.atomCount;
      this.atoms = atomSetCollection.atoms;
      iatom = 0;
    }
    public boolean hasNext() {
      if (iatom == atomCount)
        return false;
      atom = atoms[iatom++];
      return true;
    }
    public int getAtomSetIndex() { return atom.atomSetIndex; }
    public BitSet getAtomSymmetry() { return atom.bsSymmetry; }
    public int getAtomSite() { return atom.atomSite + 1; }
    public Object getUniqueID() { return new Integer(atom.atomIndex); }
    public String getElementSymbol() {
      if (atom.elementSymbol != null)
        return atom.elementSymbol;
      return atom.getElementSymbol();
    }
    public int getElementNumber() { return atom.elementNumber; }
    public String getAtomName() { return atom.atomName; }
    public int getFormalCharge() { return atom.formalCharge; }
    public float getPartialCharge() { return atom.partialCharge; }
    public Object[] getEllipsoid() { return atom.ellipsoid; }
    public float getRadius() { return atom.radius; }
    public float getX() { return atom.x; }
    public float getY() { return atom.y; }
    public float getZ() { return atom.z; }
    public float getVectorX() { return atom.vectorX; }
    public float getVectorY() { return atom.vectorY; }
    public float getVectorZ() { return atom.vectorZ; }
    public float getBfactor() { return Float.isNaN(atom.bfactor) && atom.anisoBorU != null ?
        atom.anisoBorU[7] * 100f : atom.bfactor; }
    public int getOccupancy() { return atom.occupancy; }
    public boolean getIsHetero() { return atom.isHetero; }
    public int getAtomSerial() { return atom.atomSerial; }
    public char getChainID() { return canonizeChainID(atom.chainID); }
    public char getAlternateLocationID()
    { return canonizeAlternateLocationID(atom.alternateLocationID); }
    public String getGroup3() { return atom.group3; }
    public int getSequenceNumber() { return atom.sequenceNumber; }
    public char getInsertionCode()
    { return canonizeInsertionCode(atom.insertionCode); }
    
  }

  class BondIterator extends JmolAdapter.BondIterator {
    AtomSetCollection atomSetCollection;
    //Atom[] atoms;
    Bond[] bonds;
    int ibond;
    Bond bond;
    int bondCount;
    
    BondIterator(AtomSetCollection atomSetCollection) {
      this.atomSetCollection = atomSetCollection;
      //atoms = atomSetCollection.atoms;
      bonds = atomSetCollection.bonds;
      bondCount = atomSetCollection.bondCount;
      
      ibond = 0;
    }
    public boolean hasNext() {
      if (ibond == bondCount)
        return false;
      bond = bonds[ibond++];
      return true;
    }
    public Object getAtomUniqueID1() {
      return new Integer(bond.atomIndex1);
    }
    public Object getAtomUniqueID2() {
      return new Integer(bond.atomIndex2);
    }
    public int getEncodedOrder() {
      return bond.order;
    }
  }

  public class StructureIterator extends JmolAdapter.StructureIterator {
    int structureCount;
    Structure[] structures;
    Structure structure;
    int istructure;
    
    StructureIterator(AtomSetCollection atomSetCollection) {
      structureCount = atomSetCollection.structureCount;
      structures = atomSetCollection.structures;
      istructure = 0;
    }

    public boolean hasNext() {
      if (istructure == structureCount)
        return false;
      structure = structures[istructure++];
      return true;
    }

    public int getModelIndex() {
      return structure.modelIndex;
    }
    public String getStructureType() {
      return structure.structureType;
    }

    public char getStartChainID() {
      return canonizeChainID(structure.startChainID);
    }
    
    public int getStartSequenceNumber() {
      return structure.startSequenceNumber;
    }
    
    public char getStartInsertionCode() {
      return canonizeInsertionCode(structure.startInsertionCode);
    }
    
    public char getEndChainID() {
      return canonizeChainID(structure.endChainID);
    }
    
    public int getEndSequenceNumber() {
      return structure.endSequenceNumber;
    }
      
    public char getEndInsertionCode() {
      return structure.endInsertionCode;
    }
  }

  
  
}
