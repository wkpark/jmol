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

package org.jmol.adapter.smarter;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.api.JmolAdapterStructureIterator;
import org.jmol.api.JmolFilesReaderInterface;

import javajs.api.GenericBinaryDocument;
import javajs.util.Lst;

import org.jmol.script.SV;
import org.jmol.util.Logger;
import javajs.util.P3;
import javajs.util.Rdr;
import javajs.util.V3;
import org.jmol.viewer.Viewer;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.Map;


public class SmarterJmolAdapter extends JmolAdapter {

  /**************************************************************
   * 
   * AtomSetCollectionReader.readData() will close any BufferedReader
   *  
   * **************************************************************/

  public final static String PATH_KEY = ".PATH";
  public final static String PATH_SEPARATOR =
    System.getProperty("path.separator", "/");

  /**
   * Just get the resolved file type; if a file, does NOT close the reader
   * 
   * @param ascOrReader 
   * @return a file type or null
   * 
   */
  @Override
  public String getFileTypeName(Object ascOrReader) {
    if (ascOrReader instanceof AtomSetCollection)
      return ((AtomSetCollection)ascOrReader).fileTypeName;
    if (ascOrReader instanceof BufferedReader)
      return Resolver.getFileType((BufferedReader)ascOrReader);
    return null;
  }

  @Override
  public Object getAtomSetCollectionReader(String name, String type,
                                           Object bufferedReader, Map<String, Object> htParams) {
    return staticGetAtomSetCollectionReader(name, type, bufferedReader, htParams);
  }

    /**
   * The primary file or string reader -- returns just the reader now
   * 
   * @param name 
   * @param type 
   * @param bufferedReader 
   * @param htParams 
   * @return        an AtomSetCollectionReader or an error string
   * 
   */
  public static Object staticGetAtomSetCollectionReader(String name, String type,
                                   Object bufferedReader, Map<String, Object> htParams) {
    try {
      Object ret = Resolver.getAtomCollectionReader(name, type,
          bufferedReader, htParams, -1);
      if (ret instanceof String) {
        try {
          close(bufferedReader);
        } catch (Exception e) {
          //
        }
      } else {
        ((AtomSetCollectionReader) ret).setup(name, htParams, bufferedReader);
      }
      return ret;        
    } catch (Throwable e) {
      try {
        close(bufferedReader);
      } catch (Exception ex) {
        //
      }
      bufferedReader = null;
      Logger.error("" + e);
      return "" + e;
    }
  }

  @Override
  public Object getAtomSetCollectionFromReader(String fname,
                                            Object reader,
                                            Map<String, Object> htParams) throws Exception {
    Object ret = Resolver.getAtomCollectionReader(fname, null, reader,
        htParams, -1);
    if (ret instanceof AtomSetCollectionReader) {
      ((AtomSetCollectionReader) ret).setup(fname, htParams, reader);
      return ((AtomSetCollectionReader) ret).readData();
    }
    return "" + ret;    
  }

  /**
   * Create the AtomSetCollection and return it
   * 
   * @param ascReader 
   * @return an AtomSetCollection or an error string
   * 
   */
  @Override
  public Object getAtomSetCollection(Object ascReader) {
    return staticGetAtomSetCollection((AtomSetCollectionReader) ascReader);
  }

  public static Object staticGetAtomSetCollection(AtomSetCollectionReader a) {
    BufferedReader br = null;
    try {
      br = a.reader;
      Object ret = a.readData();
      if (!(ret instanceof AtomSetCollection))
        return ret;
      AtomSetCollection asc = (AtomSetCollection) ret;
      if (asc.errorMessage != null)
        return asc.errorMessage;
      return asc;
    } catch (Throwable e) {
      try{ 
        Logger.info(e.toString());
      } catch (Exception ee) {
        Logger.error(e.toString());
      }
      try {
        br.close();
      } catch (Exception ex) {
        //
      }
      br = null;
      Logger.error("" + e);
      return "" + e;
    }
  }

  /**
   * primary for String[] or File[] reading -- two options are implemented ---
   * return a set of simultaneously open readers, or return one single
   * collection using a single reader
   * 
   * @param filesReader
   * @param names
   * @param types
   * @param htParams
   * @param getReadersOnly
   *        TRUE for a set of readers; FALSE for one asc
   * 
   * @return a set of AtomSetCollectionReaders, a single AtomSetCollection, or
   *         an error string
   * 
   */
  @Override
  public Object getAtomSetCollectionReaders(JmolFilesReaderInterface filesReader,
                                            String[] names, String[] types,
                                            Map<String, Object> htParams,
                                            boolean getReadersOnly) {
    //FilesOpenThread
    Viewer vwr = (Viewer) htParams.get("vwr"); // don't pass this on to user
    int size = names.length;
    AtomSetCollectionReader[] readers = (getReadersOnly ? new AtomSetCollectionReader[size]
        : null);
    Object reader = null;
    if (htParams.containsKey("concatenate")) {
      String s = "";
      for (int i = 0; i < size; i++) {
        String f = vwr.getFileAsString(names[i], false);
        if (i > 0 && size <= 3 && f.startsWith("{")) {
          // JSON annotations and validations; could have both
          // hack to determine type:
          String type = (f.contains("\"value\":") ? "validations" : "annotations");
          SV x = vwr.evaluateExpressionAsVariable(f);
          if (x != null && x.getMap() != null)
            htParams.put(type, x);
          continue;
        }
        s += f;
        if (!s.endsWith("\n"))
          s += "\n";
      }
      size = 1;
      reader = Rdr.getBR(s);
    }
    AtomSetCollection[] atomsets = (getReadersOnly ? null
        : new AtomSetCollection[size]);
    AtomSetCollectionReader r = null;

    for (int i = 0; i < size; i++) {
      try {
        if (r != null)
          htParams.put("vwr", vwr);
        if (reader == null)
          reader = filesReader.getBufferedReaderOrBinaryDocument(i, false);
        if (!(reader instanceof BufferedReader || reader instanceof GenericBinaryDocument))
          return reader;
        Object ret = Resolver.getAtomCollectionReader(names[i],
            (types == null ? null : types[i]), reader, htParams, i);
        if (!(ret instanceof AtomSetCollectionReader))
          return ret;
        r = (AtomSetCollectionReader) ret;
        r.setup(null, null, null);
        if (r.isBinary) {
          r.setup(names[i], htParams,
              filesReader.getBufferedReaderOrBinaryDocument(i, true));
        } else {
          r.setup(names[i], htParams, reader);
        }
        reader = null;
        if (getReadersOnly) {
          readers[i] = r;
        } else {
          ret = r.readData();
          if (!(ret instanceof AtomSetCollection))
            return ret;
          atomsets[i] = (AtomSetCollection) ret;
          if (atomsets[i].errorMessage != null)
            return atomsets[i].errorMessage;
        }
      } catch (Throwable e) {
        Logger.error("" + e);
        if (!vwr.isJS)
          e.printStackTrace();
        return "" + e;
      }
    }
    if (getReadersOnly)
      return readers;
    return getAtomSetCollectionFromSet(readers, atomsets, htParams);
  }
   
  /**
   * 
   * needed to consolidate a set of models into one model; could start
   * with AtomSetCollectionReader[] or with AtomSetCollection[]
   * 
   * @param readerSet 
   * @param atomsets 
   * @param htParams 
   * @return a single AtomSetCollection or an error string
   * 
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object getAtomSetCollectionFromSet(Object readerSet, Object atomsets,
                                            Map<String, Object> htParams) {
    AtomSetCollectionReader[] readers = (AtomSetCollectionReader[]) readerSet;
    AtomSetCollection[] asc = (atomsets == null ? new AtomSetCollection[readers.length]
        : (AtomSetCollection[]) atomsets);
    if (atomsets == null) {
      for (int i = 0; i < readers.length; i++) {
        try {
          Object ret = readers[i].readData();
          if (!(ret instanceof AtomSetCollection))
            return ret;
          asc[i] = (AtomSetCollection) ret;
          if (asc[i].errorMessage != null)
            return asc[i].errorMessage;
        } catch (Throwable e) {
          Logger.error("" + e);
          return "" + e;
        }
      }
    }
    AtomSetCollection result;
    if (htParams.containsKey("trajectorySteps")) {
      // this is one model with a set of coordinates from a 
      // molecular dynamics calculation
      // all the htParams[] entries point to the same Hashtable
      result = asc[0];
      try {
        result.finalizeTrajectoryAs(
            (Lst<P3[]>) htParams.get("trajectorySteps"),
            (Lst<V3[]>) htParams.get("vibrationSteps"));
      } catch (Exception e) {
        if (result.errorMessage == null)
          result.errorMessage = "" + e;
      }
    } else if (asc[0].isTrajectory){ 
      result = asc[0];
      for (int i = 1; i < asc.length; i++)
        asc[0].mergeTrajectories(asc[i]);
    } else {
      result = (asc.length == 1 ? asc[0]: new AtomSetCollection("Array", null, asc, null));
    }
    return (result.errorMessage == null ? result : result.errorMessage);
  }

  /**
   * Direct DOM HTML4 page reading; Egon was interested in this at one point.
   * 
   * @param DOMNode 
   * @param htParams 
   * @return a single AtomSetCollection or an error string
   * 
   */
  @Override
  public Object getAtomSetCollectionFromDOM(Object DOMNode, Map<String, Object> htParams) {
    try {
      Object ret = Resolver.DOMResolve(DOMNode, htParams);
      if (!(ret instanceof AtomSetCollectionReader))
        return ret;
      AtomSetCollectionReader a = (AtomSetCollectionReader) ret;
      a.setup("DOM node", htParams, null);
      ret = a.readDataObject(DOMNode);
      if (!(ret instanceof AtomSetCollection))
        return ret;
      AtomSetCollection asc = (AtomSetCollection) ret;
      if (asc.errorMessage != null)
        return asc.errorMessage;
      return asc;
    } catch (Throwable e) {
      Logger.error("" + e);
      return "" + e;
    }
  }
  
  @Override
  public void finish(Object asc) {
    ((AtomSetCollection)asc).finish();
  }

  ////////////////////////// post processing ////////////////////////////
  
  @Override
  public String getAtomSetCollectionName(Object asc) {
    return ((AtomSetCollection)asc).collectionName;
  }
  
  @Override
  public Map<String, Object> getAtomSetCollectionAuxiliaryInfo(Object asc) {
    return ((AtomSetCollection)asc).ascAuxiliaryInfo;
  }

  @Override
  public int getAtomSetCount(Object asc) {
    return ((AtomSetCollection)asc).atomSetCount;
  }

  @Override
  public int getAtomSetNumber(Object asc, int atomSetIndex) {
    return ((AtomSetCollection)asc).getAtomSetNumber(atomSetIndex);
  }

  @Override
  public String getAtomSetName(Object asc, int atomSetIndex) {
    return ((AtomSetCollection)asc).getAtomSetName(atomSetIndex);
  }
  
  @Override
  public Map<String, Object> getAtomSetAuxiliaryInfo(Object asc, int atomSetIndex) {
    return ((AtomSetCollection) asc)
        .getAtomSetAuxiliaryInfo(atomSetIndex);
  }

  /* **************************************************************
   * The frame related methods
   * **************************************************************/

  @Override
  public int getHydrogenAtomCount(Object asc) {
    return ((AtomSetCollection)asc).getHydrogenAtomCount();
  }
  
  @Override
  public String[][] getBondList(Object asc) {
    return ((AtomSetCollection)asc).getBondList();
  }


  @Override
  public int getAtomCount(Object asc) {
    AtomSetCollection a = (AtomSetCollection)asc; 
    return (a.bsAtoms == null ? a.ac : a.bsAtoms.cardinality());
  }

  @Override
  public boolean coordinatesAreFractional(Object asc) {
    return ((AtomSetCollection)asc).coordinatesAreFractional;
  }

  ////////////////////////////////////////////////////////////////

  @Override
  public JmolAdapterAtomIterator getAtomIterator(Object asc) {
    return new AtomIterator((AtomSetCollection)asc);
  }

  @Override
  public JmolAdapterBondIterator getBondIterator(Object asc) {
    return new BondIterator((AtomSetCollection)asc);
  }

  @Override
  public JmolAdapterStructureIterator getStructureIterator(Object asc) {
    return ((AtomSetCollection)asc).structureCount == 0 ? 
        null : new StructureIterator((AtomSetCollection)asc);
  }

  public static void close(Object bufferedReader) throws IOException {
    if (bufferedReader instanceof BufferedReader)
      ((BufferedReader) bufferedReader).close();
      else
        ((GenericBinaryDocument) bufferedReader).close();
  }

}
