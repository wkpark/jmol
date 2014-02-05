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

import javajs.util.List;

import org.jmol.adapter.readers.molxyz.MolReader;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.JmolJDXMOLReader;
import org.jmol.adapter.smarter.JmolJDXMOLParser;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.Interface;
import org.jmol.io.JmolBinary;
import javajs.util.BS;
import org.jmol.util.Logger;

/**
 * A preliminary reader for JCAMP-DX files having ##$MODELS= and ##$PEAKS=
 * records
 * 
 * Designed by Robert Lancashire and Bob Hanson
 * 
 * specifications (by example here):
 * 
 * ##$MODELS= <Models> <ModelData id="acetophenone" type="MOL"> acetophenone
 * DSViewer 3D 0
 * 
 * 17 17 0 0 0 0 0 0 0 0999 V2000 ... 17 14 1 0 0 0 M END </ModelData>
 * <ModelData id="irvibs" type="XYZVIB" baseModel="acetophenone"
 * vibrationScale="0.1"> 17 1 Energy: -1454.38826 Freq: 3199.35852 C -1.693100
 * 0.007800 0.000000 -0.000980 0.000120 0.000000 ... </ModelData> </Models>
 * 
 * -- All XML data should be line-oriented in the above fashion. Leading spaces
 * will be ignored. -- Any number of <ModelData> segments can be present -- The
 * first model is referred to as the "base" model -- The base model: -- will
 * generally be of type MOL, but any known type is acceptable -- will be used to
 * generate bonding for later models that have no bonding information -- will be
 * the only model for NMR -- Additional models can represent vibrations (XYZ
 * format) or MS fragmentation (MOL format, probably)
 * 
 * ##$PEAKS= <Peaks type="IR" xUnits="1/cm" yUnits="TRANSMITTANCE" > <PeakData
 * id="1" title="asymm stretch of aromatic CH group (~3100 cm-1)"
 * peakShape="broad" model="irvibs.1" xMax="3121" xMin="3081" yMax="1" yMin="0"
 * /> <PeakData id="2" title="symm stretch of aromatic CH group (~3085 cm-1)"
 * peakShape="broad" model="irvibs.2" xMax="3101" xMin="3071" yMax="1" yMin="0"
 * /> ... </Peaks>
 * 
 * -- peak record must be a single line of information because Jmol will use
 * line.trim() as a key to pass information to JSpecView.
 * 
 * 
 * <p>
 */

public class JcampdxReader extends MolReader implements JmolJDXMOLReader {

  private int selectedModel;
  private JmolJDXMOLParser mpr;

  @Override
  public void initializeReader() throws Exception {
    // trajectories would be OK for IR, but just too complicated for others.

    // tells Jmol to start talking with JSpecView

    viewer.setBooleanProperty("_JSpecView".toLowerCase(), true);
    // necessary to not use "jspecview" here, as buildtojs.xml will change that to "JSV"
    if (isTrajectory) {
      Logger.warn("TRAJECTORY keyword ignored");
      isTrajectory = false;
    }
    // forget reversing models!
    if (reverseModels) {
      Logger.warn("REVERSE keyword ignored");
      reverseModels = false;
    }
    selectedModel = desiredModelNumber;
    desiredModelNumber = Integer.MIN_VALUE;
    if (!checkFilterKey("NOSYNC"))
      addJmolScript("sync on");
  }

  @Override
  public boolean checkLine() throws Exception {
    int i = line.indexOf("=");
    if (i < 0 || !line.startsWith("##"))
      return true;
    String label = line.substring(0, i).trim();
    int pt = "##$MODELS ##$PEAKS  ##$SIGNALS".indexOf(label);
    if (pt < 0)
      return true;
    if (mpr == null)
      mpr = ((JmolJDXMOLParser) Interface
          .getOptionInterface("jsv.JDXMOLParser")).set(this, filePath,
          htParams);
    mpr.setLine(line.substring(i + 1));
    switch (pt) {
    case 0:
      mpr.readModels();
      break;
    case 10:
    case 20:
      mpr.readPeaks(pt == 20, -1);
      break;
    }
    return true;
  }

  @Override
  public void finalizeReader() throws Exception {
    if (mpr != null)
      processPeakData();
    finalizeReaderMR();
  }

  @Override
  public void processModelData(String data, String id, String type,
                               String base, String last, float vibScale,
                               boolean isFirst) throws Exception {
    int model0 = atomSetCollection.currentAtomSetIndex;
    AtomSetCollection model = null;
    while (true) {
      Object ret = SmarterJmolAdapter.staticGetAtomSetCollectionReader(
          filePath, type, JmolBinary.getBR(data), htParams);
      if (ret instanceof String) {
        Logger.warn("" + ret);
        break;
      }
      ret = SmarterJmolAdapter
          .staticGetAtomSetCollection((AtomSetCollectionReader) ret);
      if (ret instanceof String) {
        Logger.warn("" + ret);
        break;
      }
      model = (AtomSetCollection) ret;
      String baseModel = base;
      if (baseModel.length() == 0)
        baseModel = last;
      if (baseModel.length() != 0) {
        int ibase = findModelById(baseModel);
        if (ibase >= 0) {
          atomSetCollection.setAtomSetAuxiliaryInfoForSet("jdxModelID",
              baseModel, ibase);
          for (int i = model.atomSetCount; --i >= 0;)
            model.setAtomSetAuxiliaryInfoForSet("jdxBaseModel", baseModel, i);
          if (model.bondCount == 0)
            setBonding(model, ibase);
        }
      }
      if (!Float.isNaN(vibScale)) {
        Logger.info("jdx applying vibrationScale of " + vibScale + " to "
            + model.atomCount + " atoms");
        Atom[] atoms = model.atoms;
        for (int i = model.atomCount; --i >= 0;)
          atoms[i].scaleVector(vibScale);
      }
      Logger.info("jdx model=" + id + " type=" + model.fileTypeName);
      atomSetCollection.appendAtomSetCollection(-1, model);
      break;
    }
    updateModelIDs(id, model0, isFirst);
  }

  /**
   * add bonding to a set of ModelData based on a MOL file only if the this set
   * has no bonding already
   * 
   * @param a
   * @param ibase
   */
  private void setBonding(AtomSetCollection a, int ibase) {
    int n0 = atomSetCollection.getAtomSetAtomCount(ibase);
    int n = a.atomCount;
    if (n % n0 != 0) {
      Logger.warn("atom count in secondary model (" + n
          + ") is not a multiple of " + n0 + " -- bonding ignored");
      return;
    }
    Bond[] bonds = atomSetCollection.bonds;
    int b0 = 0;
    for (int i = 0; i < ibase; i++)
      b0 += atomSetCollection.getAtomSetBondCount(i);
    int b1 = b0 + atomSetCollection.getAtomSetBondCount(ibase);
    int ii0 = atomSetCollection.getAtomSetAtomIndex(ibase);
    int nModels = a.atomSetCount;
    for (int j = 0; j < nModels; j++) {
      int i0 = a.getAtomSetAtomIndex(j) - ii0;
      if (a.getAtomSetAtomCount(j) != n0) {
        Logger.warn("atom set atom count in secondary model ("
            + a.getAtomSetAtomCount(j) + ") is not equal to " + n0
            + " -- bonding ignored");
        return;
      }
      for (int i = b0; i < b1; i++)
        a.addNewBondWithOrder(bonds[i].atomIndex1 + i0, bonds[i].atomIndex2
            + i0, bonds[i].order);
    }
  }

  /**
   * The first model set is allowed to be a single model and given no extension.
   * All other model sets are given .1 .2 .3 ... extensions to their IDs.
   * 
   * @param id
   * @param model0
   * @param isFirst
   */
  private void updateModelIDs(String id, int model0, boolean isFirst) {
    int n = atomSetCollection.atomSetCount;
    if (isFirst && n == model0 + 2) {
      atomSetCollection.setAtomSetAuxiliaryInfo("modelID", id);
      return;
    }
    for (int pt = 0, i = model0; ++i < n;)
      atomSetCollection.setAtomSetAuxiliaryInfoForSet("modelID", id + "."
          + (++pt), i);
  }

  private List<String> peakData = new List<String>();

  @Override
  public void addPeakData(String info) {
    peakData.addLast(info);
  }

  /**
   * integrate the <PeakAssignment> records into the associated models, and
   * delete unreferenced n.m models
   */
  private void processPeakData() {
    int n = peakData.size();
    if (n == 0)
      return;
    BS bsModels = new BS();
    boolean havePeaks = (n > 0);
    for (int p = 0; p < n; p++) {
      line = peakData.get(p);
      String type = mpr.getAttribute(line, "type");
      String id = mpr.getAttribute(line, "model");
      int i = findModelById(id);
      if (i < 0) {
        Logger.warn("cannot find model " + id + " required for " + line);
        continue;
      }
      addType(i, type);
      String title = type + ": " + mpr.getAttribute(line, "title");
      String key = "jdxAtomSelect_" + mpr.getAttribute(line, "type");
      bsModels.set(i);
      String s;
      if (mpr.getAttribute(line, "atoms").length() != 0) {
        processPeakSelectAtom(i, key, line);
        s = type + ": ";
      } else if (processPeakSelectModel(i, title)) {
        s = "model: ";
      } else {
        s = "ignored: ";
      }
      Logger.info(s + line);
    }
    n = atomSetCollection.atomSetCount;
    for (int i = n; --i >= 0;) {
      String id = (String) atomSetCollection.getAtomSetAuxiliaryInfoValue(i,
          "modelID");
      if (havePeaks && !bsModels.get(i) && id.indexOf(".") >= 0) {
        atomSetCollection.removeAtomSet(i);
        n--;
      }
    }
    if (selectedModel == Integer.MIN_VALUE) {
      if (allTypes != null)
        appendLoadNote(allTypes);
    } else {
      if (selectedModel == 0)
        selectedModel = n - 1;
      for (int i = atomSetCollection.atomSetCount; --i >= 0;)
        if (i + 1 != selectedModel)
          atomSetCollection.removeAtomSet(i);
      if (n > 0)
        appendLoadNote((String) atomSetCollection.getAtomSetAuxiliaryInfoValue(
            0, "name"));
    }
    for (int i = atomSetCollection.atomSetCount; --i >= 0;)
      atomSetCollection.setAtomSetNumber(i, i + 1);
    atomSetCollection.centralize();
  }

  private int findModelById(String modelID) {
    for (int i = atomSetCollection.atomSetCount; --i >= 0;) {
      String id = (String) atomSetCollection.getAtomSetAuxiliaryInfoValue(i,
          "modelID");
      if (modelID.equals(id))
        return i;
    }
    return -1;
  }

  private String allTypes;

  /**
   * sets an auxiliaryInfo string to "HNMR 13CNMR" or "IR" or "MS"
   * 
   * @param imodel
   * @param type
   */
  private void addType(int imodel, String type) {
    String types = addType(
        (String) atomSetCollection.getAtomSetAuxiliaryInfoValue(imodel,
            "spectrumTypes"), type);
    if (types == null)
      return;
    atomSetCollection.setAtomSetAuxiliaryInfoForSet("spectrumTypes", types,
        imodel);
    String s = addType(allTypes, type);
    if (s != null)
      allTypes = s;
  }

  private String addType(String types, String type) {
    if (types != null && types.contains(type))
      return null;
    if (types == null)
      types = "";
    else
      types += ",";
    return types + type;
  }

  @SuppressWarnings("unchecked")
  private void processPeakSelectAtom(int i, String key, String data) {
    List<String> peaks = (List<String>) atomSetCollection
        .getAtomSetAuxiliaryInfoValue(i, key);
    if (peaks == null)
      atomSetCollection.setAtomSetAuxiliaryInfoForSet(key,
          peaks = new List<String>(), i);
    peaks.addLast(data);
  }

  private boolean processPeakSelectModel(int i, String title) {
    if (atomSetCollection.getAtomSetAuxiliaryInfoValue(i, "jdxModelSelect") != null)
      return false;
    // assign name and jdxModelSelect ONLY if first found.
    atomSetCollection.setAtomSetAuxiliaryInfoForSet("name", title, i);
    atomSetCollection.setAtomSetAuxiliaryInfoForSet("jdxModelSelect", line, i);
    return true;
  }

  @Override
  public void setSpectrumPeaks(int nH, String piUnitsX, String piUnitsY) {
    // JSpecView only    
  }

}
