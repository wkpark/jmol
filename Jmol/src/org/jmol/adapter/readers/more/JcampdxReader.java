/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 07:52:29 -0600 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
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

package org.jmol.adapter.readers.more;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.readers.molxyz.MolReader;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.SmarterJmolAdapter;

import org.jmol.util.Logger;
import org.jmol.util.Parser;

/**
 * A reader for Jcamdx files having <model> and <peaklist> records.
 * Designed by Robert Lancashire and Bob Hanson
 * 
 * 
 *<p>
 */

public class JcampdxReader extends MolReader {

  private String dataType;
  
  @Override
  public void initializeReader() throws Exception {
    //
  }

  @Override
  public boolean checkLine() throws Exception {
    if (line.startsWith("##DATA TYPE")) {
      setDataType();
      return true;
    }
    if (line.startsWith("##$ASSIGNMENT TYPE")) {
      if (!line.contains("JMOL")) {
        Logger.warn("assignment type not recognized: " + line);
        dataType = null;
        return true;
      }
      readModels();
    }

    return true;
  }

  private void setDataType() {
    line = line.toUpperCase();
    dataType = (line.contains("INFRARED") ? "IR" : line.contains("NMR") ? "NMR" : line.contains("MASS") ? "MS" : null);
    if (dataType == null)
      Logger.warn("data type not recognized: " + line);
  }

  AtomSetCollection baseModel;
  AtomSetCollection additionalModels;
  private void readModels() throws Exception {
    discardLinesUntilContains("<models");
    readLine();
    baseModel = null;
    baseModel = getModelAtomSetCollection();
    if (baseModel == null)
      return;
    if (desiredModelNumber == 1) {
      atomSetCollection.appendAtomSetCollection(-1, baseModel);
      return;
    }
    additionalModels = null;
    if (desiredModelNumber == 0) {
      baseModel = null;
    }
    if (readLine().contains("<model")) {
      additionalModels = getModelAtomSetCollection();
      atomSetCollection.appendAtomSetCollection(-1, additionalModels);
    }
  }

  private Map<String, AtomSetCollection> htModels = new Hashtable<String, AtomSetCollection>();
  
  private AtomSetCollection getModelAtomSetCollection() throws Exception {
    if (line.indexOf("<model") < 0)
      discardLinesUntilContains("<model");
    String modelId = getAttribute(line, "id");
    String modelType = getAttribute(line, "type").toLowerCase();
    if (modelType.equals("xyzvib"))
      modelType = "xyz";
    String data = getModelData();
    discardLinesUntilContains("</model>");
    Object ret = SmarterJmolAdapter.staticGetAtomSetCollectionReader(filePath, modelType, new BufferedReader(new StringReader(data)), htParams); 
    if (ret instanceof String) {
      Logger.warn("" + ret);
      dataType = null;
      return null;
    }
    ret = SmarterJmolAdapter.staticGetAtomSetCollection((AtomSetCollectionReader) ret);
    if (ret instanceof String) {
      Logger.warn("" + ret);
      dataType = null;
      return null;
    }
    AtomSetCollection a = (AtomSetCollection) ret;
    htModels.put(modelId, a);
    if (modelType.equals("xyz") && baseModel != null)
      setBonding(a);
    return a;
  }

  private void setBonding(AtomSetCollection a) {
    if (a.getBondCount() != 0)
      return;
    int n0 = baseModel.getAtomCount();
    int n = a.getAtomCount();
    if (n % n0 != 0) {
      Logger.warn("atom count in secondary model (" + n + ") is not a multiple of " + n0 + " -- bonding ignored");
      return;
    }
    int nModels = a.getAtomSetCount();
    Bond[] bonds = baseModel.getBonds();
    int nBonds = baseModel.getBondCount();
    for (int j = 0; j < nModels; j++) {
      int i0 = a.getAtomSetAtomIndex(j);
      if (a.getAtomSetAtomCount(j) != n0) {
        Logger.warn("atom set atom count in secondary model (" + a.getAtomSetAtomCount(j) + ") is not equal to " + n0 + " -- bonding ignored");
        return;
      }
      
      for (int i = 0; i < nBonds; i++) 
        a.addNewBond(bonds[i].atomIndex1 + i0, bonds[i].atomIndex2 + i0, bonds[i].order);      
    }
  }

  private String getAttribute(String line, String tag) {
    int i = line.indexOf(tag);
    if (i < 0) return "";
    return Parser.getNextQuotedString(line, i);
  }

  private String getModelData() throws Exception {
    discardLinesUntilContains("<modelData");
    StringBuffer sb = new StringBuffer();
    while (readLine() != null && !line.contains("</modelData>"))
      sb.append(line).append('\n');
    return sb.toString();
  }
}
