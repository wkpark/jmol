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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.jmol.adapter.readers.molxyz.MolReader;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.SmarterJmolAdapter;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

/**
 * A preliminary reader for JCAMP-DX files having ##$MODELS= and ##PEAK_LINKS= records
 * 
 * Designed by Robert Lancashire and Bob Hanson
 * 
 * specifications (by example here):

##$MODELS=
<Models>
 <Model id="acetophenone" type="MOL">
  <ModelData>
acetophenone
  DSViewer          3D                             0

 17 17  0  0  0  0  0  0  0  0999 V2000
...
 17 14  1  0  0  0
M  END
  </ModelData>
 </Model>
 <Model id="irvibs" type="XYZVIB">
  <ModelData>
17
1  Energy: -1454.38826  Freq: 3199.35852
C    -1.693100    0.007800    0.000000   -0.000980    0.000120    0.000000
...
  </ModelData>
  </Model>
</Models>

-- All XML data should be line-oriented in the above fashion. Leading spaces will be ignored.
-- Any number of <model> segments can be present
-- The first model is referred to as the "base" model
-- The base model:
   -- will generally be of type MOL, but any known type is acceptable
   -- will be used to generate bonding for later models that have no bonding information
   -- will be the only model for NMR
-- Additional models can represent vibrations (XYZ format) or MS fragmentation (MOL format, probably)

##$PEAK_LINKS=
<PeakList type="IR" xUnits="1/cm" yUnits="TRANSMITTANCE" >
<Peak id="1" title="asymm stretch of aromatic CH group (~3100 cm-1)" peakShape="broad" model="irvibs.1"  xMax="3121" xMin="3081"  yMax="1" yMin="0" />
<Peak id="2" title="symm stretch of aromatic CH group (~3085 cm-1)" peakShape="broad" model="irvibs.2"  xMax="3101" xMin="3071"  yMax="1" yMin="0" />
...
</PeakList>

-- peak record must be a single line of information because
   Jmol will use line.trim() as a key to pass information to JSpecView. 

 * 
 *<p>
 */

public class JcampdxReader extends MolReader {

  private String modelID;
  private AtomSetCollection models;
  private String modelIdList = "";
  private List<String> peakData = new ArrayList<String>();
  private String lastModel = "";
  
  @Override
  public void initializeReader() throws Exception {
    if (isTrajectory) {
      Logger.warn("TRAJECTORY keyword ignored");
      isTrajectory = false;
    }
    if (reverseModels) {
      Logger.warn("REVERSE keyword ignored");
      reverseModels = false;
    }
    if (desiredModelNumber != Integer.MIN_VALUE) {
      Logger.warn("desired model number ignored");
      desiredModelNumber = Integer.MIN_VALUE;
    }
    
  }

  @Override
  public boolean checkLine() throws Exception {
    if (line.startsWith("##$MODELS")) {
      readModels();
    } else if (line.startsWith("##$PEAK") && line.contains("LINKS=")) {
      readPeakLinks();
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void finalizeReader() {
    // process peak data
    if (peakData.size() > 0) {
      BitSet bsModels = new BitSet();
      int n = peakData.size();
      for (int p = 0; p < n; p++) {
        line = peakData.get(p);
        String type = getAttribute(line, "type");
        String title = type + ": " + getAttribute(line, "title");
        modelID = getAttribute(line, "model");
        String key = "jdxAtomSelect_" + getAttribute(line, "type");
        int i = findModelById(modelID);
        if (i >= 0) {
          bsModels.set(i);
          if (modelID.indexOf('.') >= 0) {
            atomSetCollection.setAtomSetAuxiliaryInfo("name", title, i);
            atomSetCollection
                .setAtomSetAuxiliaryInfo("jdxModelSelect", line, i);
          } else if (getAttribute(line, "atoms").length() != 0) {
            List<String> peaks = (List<String>) atomSetCollection
                .getAtomSetAuxiliaryInfo(i, key);
            if (peaks == null)
              atomSetCollection.setAtomSetAuxiliaryInfo(key,
                  peaks = new ArrayList<String>(), i);
            peaks.add(line);
          }
          Logger.info(line);
        }
      }
      for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;) {
        modelID = (String) atomSetCollection.getAtomSetAuxiliaryInfo(i,
            "modelID");
        if (!bsModels.get(i) && modelID.indexOf(".") >= 0)
          atomSetCollection.removeAtomSet(i);
      }
    }
  }
  
  private int findModelById(String modelID) {
    for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;)
      if (modelID.equals(atomSetCollection
          .getAtomSetAuxiliaryInfo(i, "modelID")))
        return i;
    return -1;
  }

  private void readModels() throws Exception {
    discardLinesUntilContains("<Models");
    // if load xxx.jdx n  then we must temporarily set n to 1 for the base model reading
    // load xxx.jdx 0  will mean "load only the base model(s)"
    models = null;
    line = "";
    modelID = "";
    boolean isFirst = true;
    while (true) {
      int model0 = atomSetCollection.getCurrentAtomSetIndex();
      discardLinesUntilNonBlank();
      if (line == null || !line.contains("<Model"))
        break;
      models = getModelAtomSetCollection();
      if (models != null) {
        atomSetCollection.appendAtomSetCollection(-1, models);
      }
      updateModelIDs(model0, isFirst);
      isFirst = false;
    }
  }

  private void updateModelIDs(int model0, boolean isFirst) {
    int n = atomSetCollection.getAtomSetCount();
    if (isFirst && n == model0 + 2) {
      atomSetCollection.setAtomSetAuxiliaryInfo("modelID", modelID);
      return;
    }
    for (int pt = 0, i = model0; ++i < n;) {
      atomSetCollection.setAtomSetAuxiliaryInfo("modelID", modelID + "."
          + (++pt), i);
    }
  }

  private static String getAttribute(String line, String tag) {
    String attr = Parser.getQuotedAttribute(line, tag);
    return (attr == null ? "" : attr);
  }

  private AtomSetCollection getModelAtomSetCollection() throws Exception {
    if (line.indexOf("<Model") < 0)
      discardLinesUntilContains("<Model");
    lastModel = modelID;
    modelID = getAttribute(line, "id");
    // read model only once for a given ID
    String key = ";" + modelID + ";";
    if (modelIdList.indexOf(key) >= 0)
      return null;
    modelIdList += key;
    String baseModel = getAttribute(line, "baseModel");
    String modelType = getAttribute(line, "type").toLowerCase();
    if (modelType.equals("xyzvib"))
      modelType = "xyz";
    else if (modelType.length() == 0)
      modelType = null; // let Jmol set the type
    String data = getModelData();
    discardLinesUntilContains("</Model>");
    
    /*
    int imodel = desiredModelNumber;
    if (imodel != Integer.MIN_VALUE)
      htParams.put("modelNumber", Integer.valueOf(1));
    baseModel = getModelAtomSetCollection();
    if (imodel != Integer.MIN_VALUE)
      htParams.put("modelNumber", Integer.valueOf(imodel));
    if (baseModel == null)
      return;

    */
    
    Object ret = SmarterJmolAdapter.staticGetAtomSetCollectionReader(filePath, modelType, new BufferedReader(new StringReader(data)), htParams);
    if (ret instanceof String) {
      Logger.warn("" + ret);
      return null;
    }
    ret = SmarterJmolAdapter.staticGetAtomSetCollection((AtomSetCollectionReader) ret);
    if (ret instanceof String) {
      Logger.warn("" + ret);
      return null;
    }
    AtomSetCollection a = (AtomSetCollection) ret;
    if (a.getBondCount() == 0) {
      if (baseModel.length() == 0)
        baseModel = lastModel ;
    if (baseModel.length() != 0)
      setBonding(a, baseModel); 
    }
    Logger.info("jdx model=" + modelID + " type=" + a.getFileTypeName());
    return a;
  }

  private void setBonding(AtomSetCollection a, String baseModel) {
    int ibase = findModelById(baseModel);
    if (ibase < 0)
      return;
    int n0 = atomSetCollection.getAtomSetAtomCount(ibase);
    int n = a.getAtomCount();
    if (n % n0 != 0) {
      Logger.warn("atom count in secondary model (" + n + ") is not a multiple of " + n0 + " -- bonding ignored");
      return;
    }
    Bond[] bonds = atomSetCollection.getBonds();
    int b0 = 0;
    for (int i = 0; i < ibase; i++)
      b0 += atomSetCollection.getAtomSetBondCount(i);
    int b1 = b0 + atomSetCollection.getAtomSetBondCount(ibase);
    int ii0 = atomSetCollection.getAtomSetAtomIndex(ibase);
    int nModels = a.getAtomSetCount();
    for (int j = 0; j < nModels; j++) {
      int i0 = a.getAtomSetAtomIndex(j) - ii0;
      if (a.getAtomSetAtomCount(j) != n0) {
        Logger.warn("atom set atom count in secondary model (" + a.getAtomSetAtomCount(j) + ") is not equal to " + n0 + " -- bonding ignored");
        return;
      }      
      for (int i = b0; i < b1; i++) 
        a.addNewBond(bonds[i].atomIndex1 + i0, bonds[i].atomIndex2 + i0, bonds[i].order);      
    }
  }

  private String getModelData() throws Exception {
    discardLinesUntilContains("<ModelData");
    StringBuffer sb = new StringBuffer();
    while (readLine() != null && !line.contains("</ModelData>"))
      sb.append(line).append('\n');
    return sb.toString();
  }
  
  private void readPeakLinks() throws Exception {
    discardLinesUntilContains("<PeakList");
    String type = getAttribute(line, "type");
    while (readLine() != null && !(line = line.trim()).startsWith("</PeakList"))
      if (line.startsWith("<Peak"))
        peakData.add("<Peak file=" + Escape.escape(filePath) + " type=\"" + type + "\" " + line.trim().substring(5));      
  }
  
}
