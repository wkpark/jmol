/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.pymol;

import org.jmol.util.JmolList;
import java.util.Hashtable;

import java.util.Map;

import jspecview.util.TextFormat;

import org.jmol.adapter.readers.cifpdb.PdbReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolDocument;
import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelSettings;
import org.jmol.script.T;
import org.jmol.util.BoxInfo; //import org.jmol.util.Escape;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.SB;
import org.jmol.viewer.JC;

/**
 * experimental PyMOL PSE (binary Python session) file reader Feb 2013 Jmol
 * 13.1.13
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class PyMOLReader extends PdbReader {
  private JmolList<Object> settings;
  private int settingCount;
  private int atomCount0;
  private int atomCount;
  private int strucNo;
  private boolean isHidden;
  private JmolList<Object> pymolAtoms;
  private BS bsBonded = new BS();
  private BS bsHidden = new BS();
  private BS bsWater = new BS();
  private int[] atomMap;
  private Map<String, BS> ssMapSeq = new Hashtable<String, BS>();
  private Map<String, BS> ssMapAtom = new Hashtable<String, BS>();
  private JmolList<Integer> colixList = new  JmolList<Integer>();
  private JmolList<String> labels = new  JmolList<String>();

  private JmolList<ModelSettings> modelSettings = new  JmolList<ModelSettings>();
  private short[] colixes;
  private boolean isStateScript;
  private int width;
  private int height;

  private boolean valence;
  
  private static String nucleic = " A C G T U ADE THY CYT GUA URI DA DC DG DT DU ";

  @Override
  protected void initializeReader() throws Exception {
    isBinary = true;
    isStateScript = htParams.containsKey("isStateScript");
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("noAutoBond",
        Boolean.TRUE);
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("shapes", modelSettings);
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("isPyMOL", Boolean.TRUE);
    super.initializeReader();
  }

  //  private Map<Object, Object> temp  = new Hashtable<Object, Object>();

  private P3 xyzMin = P3.new3(1e6f, 1e6f, 1e6f);
  private P3 xyzMax = P3.new3(-1e6f, -1e6f, -1e6f);
  private int nModels;
  
  private boolean logging;

  @Override
  public void processBinaryDocument(JmolDocument doc) throws Exception {
    PickleReader reader = new PickleReader(doc, viewer);
    logging = false;
    Map<String, Object> map = reader.getMap(logging);
    reader = null;
    process(map);
  }

  private BS[] reps = new BS[REP_JMOL_MAX];
  private float cartoonTranslucency;
  private Map<String, Object> movie;
  private boolean isMovie;

  private Map<String, Object> pymol = new Hashtable<String, Object>();
  private JmolList<BS> lstStates = new  JmolList<BS>();
  private Map<String, Object> htNames = new Hashtable<String, Object>();
  private JmolList<P3[]> lstTrajectories = new  JmolList<P3[]>();
  private int currentFrame = -1;
  private int pymolFrame;
  private boolean allStates;
  private int totalAtomCount;

  private void process(Map<String, Object> map) {

    logging = (viewer.getLogFile().length() > 0);
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String name = e.getKey();
      System.out.println(name);
      if (name.equals("names")) {
        JmolList<Object> names = getMapList(map, "names");
        for (int i = 1; i < names.size(); i++)
          System.out.println("  " + getString(getList(names, i), 0));
      } else if (name.equals("version")) {
        appendLoadNote("PyMOL version: " + map.get("version").toString());
      }
    }
    if (logging) {
      String s = map.toString().replace('=', '\n');
      for (Map.Entry<String, Object> e : map.entrySet()) {
        String name = e.getKey();
        s = TextFormat.simpleReplace(s, name, "\n\n" + name + "\n");
      }
      s = TextFormat.simpleReplace(s, "[", "\n[");
      viewer.log("\n");
      viewer.log(s);
    }
    addColors(getMapList(map, "colors"));
    for (int i = 0; i < REP_JMOL_MAX; i++)
      reps[i] = BS.newN(1000);
    settings = getMapList(map, "settings");
    settingCount = settings.size();
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("settings", settings);
    allStates = getBooleanSetting(PyMOL.all_states);
    pymolFrame = (int) getFloatSetting(PyMOL.frame);
    JmolList<Object> mov = getMapList(map, "movie");
    ssMapAtom.put("nucleic", new BS());
    if (mov != null && !allStates) {
      int frameCount = getInt(mov, 0);
      if (frameCount > 0) {
        currentFrame = (int) getFloatSetting(PyMOL.frame);
        isMovie = true;
        movie = new Hashtable<String, Object>();
        movie.put("states", lstStates);
        //movie.put("trajectories", lstTrajectories);
        movie.put("frameCount", Integer.valueOf(frameCount));
        movie.put("frames", getList(mov, 4));
        //movie.put("frameStrings", getList(mov, 5));
        movie.put("currentFrame", Integer.valueOf(currentFrame));
        pymol.put("movie", movie);
      }
    }
    if (!isStateScript && checkFilterKey("DORESIZE")) {
      try {
        width = getInt(getMapList(map, "main"), 0);
        height = getInt(getMapList(map, "main"), 1);
      } catch (Exception e) {
        // ignore
      }
      if (width > 0 && height > 0) {
        appendLoadNote("PyMOL dimensions width=" + width + " height=" + height);
        atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
            "perferredWidthHeight", new int[] { width, height });
        viewer.resizeInnerPanel(width, height);
        Logger.info("Jmol dimensions width=" + viewer.getScreenWidth()
            + " height=" + viewer.getScreenHeight());
      } else {
        appendLoadNote("PyMOL dimensions unknown");
      }
    }
    valence = getBooleanSetting(PyMOL.valence);
    cartoonTranslucency = getFloatSetting(PyMOL.cartoon_transparency);
    JmolList<Object> names = getMapList(map, "names");
    totalAtomCount = getTotalAtomCount(names);
    Logger.info("PyMOL total atom count = " + totalAtomCount);
    for (int i = 1; i < names.size(); i++)
      processBranch(getList(names, i));

    if (isMovie)
      atomSetCollection.finalizeTrajectoryAs(lstTrajectories, null);

    // we are done if this is a state script

    setDefinitions();
    setRendering(getMapList(map, "view"));
  }

  private void setDefinitions() {
    modelSettings.addLast(new ModelSettings(T.define, null, htNames));
    appendLoadNote(viewer.getAtomDefs(htNames));
  }

  private int getTotalAtomCount(JmolList<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      JmolList<Object> branch = getList(names, i);
      if (checkBranch(branch) && getBranchType(branch) == BRANCH_MOLECULE) {
        JmolList<Object> deepBranch = getList(branch, 5);
        if (isMovie) {
          n += getBranchAoms(deepBranch).size();
        } else {
          JmolList<Object> states = getList(deepBranch, 4);
          int ns = states.size();
          for (int j = 0; j < ns; j++) {
            JmolList<Object> state = getList(states, j);
            JmolList<Object> idxToAtm = getList(state, 3);
            System.out.println(idxToAtm.size());
            n += idxToAtm.size();
          }
        }
      }
    }
    return n;
  }

  private void addColors(JmolList<Object> colors) {
    if (colors == null || colors.size() == 0)
      return;
    P3 pt = new P3();
    for (int i = colors.size(); --i >= 0;) {
      JmolList<Object> c = getList(colors, i);
      PyMOL.addColor((Integer) c.get(1), ColorUtil.colorPtToInt(getPoint(
          getList(c, 2), 0, pt)));
    }
  }

  private static String getString(JmolList<Object> list, int i) {
    String s = (String) list.get(i);
    return (s.length() == 0 ? " " : s);
  }

  private static int getInt(JmolList<Object> list, int i) {
    return ((Integer) list.get(i)).intValue();
  }

  private static float getFloatAt(JmolList<Object> list, int i) {
    return (list == null ? 0 : ((Double) list.get(i)).floatValue());
  }

  private P3 getPoint(JmolList<Object> list, int i, P3 pt) {
    pt.set(getFloatAt(list, i++), getFloatAt(list, i++), getFloatAt(list, i));
    return pt;
  }

  @SuppressWarnings("unchecked")
  private static JmolList<Object> getList(JmolList<Object> list, int i) {
    if (list == null || list.size() <= i)
      return null;
    Object o = list.get(i);
    return (o instanceof JmolList<?> ? (JmolList<Object>) o : null);
  }

  @SuppressWarnings("unchecked")
  private static JmolList<Object> getMapList(Map<String, Object> map, String key) {
    return (JmolList<Object>) map.get(key);
  }

  private boolean getBooleanSetting(int i) {
    return (getFloatSetting(i) != 0);
  }

  private float getFloatSetting(int i) {
    if (i >= settingCount)
      return 0;
    float v = ((Number) getList(settings, i).get(2)).floatValue();
    Logger.info("Pymol setting " + i + " = " + v);
    return v;
  }

  private final static int BRANCH_MOLECULE = 1;
  private final static int BRANCH_MAPSURFACE = 2;
  private final static int BRANCH_MAPMESH = 3;
  private final static int BRANCH_MEASURE = 4;
  private final static int BRANCH_CGO = 6; // compiled graphics object
  private final static int BRANCH_SURFACE = 7;
  private final static int BRANCH_GROUP = 12;
  
  private static final int MIN_RESNO = -1000; // minimum allowed residue number

  //  #cf. \pymol\layer1\PyMOLObject.h
  //  #define cObjectCallback     5
  //  #define cObjectGadget       8
  //  #define cObjectCalculator   9
  //  #define cObjectSlice        10
  //  #define cObjectAlignment    11

  private String branchName;
  private BS bsModelAtoms = BS.newN(1000);
  private int branchID;

  private void processBranch(JmolList<Object> branch) {
    if (!checkBranch(branch))
      return;
    Logger.info("PyMOL model " + (nModels + 1) + " Branch " + branchName
        + (isHidden ? " (hidden)" : " (visible)"));
    int type = getBranchType(branch);
    JmolList<Object> deepBranch = getList(branch, 5);
    branchID = 0;
    switch (type) {
    case BRANCH_MOLECULE:
      processBranchModels(deepBranch);
      break;
    case BRANCH_MEASURE:
      processBranchMeasure(deepBranch);
      break;
    case BRANCH_MAPSURFACE:
    case BRANCH_MAPMESH:
    case BRANCH_CGO:
    case BRANCH_SURFACE:
    case BRANCH_GROUP:
      System.out.println("Unprocessed branch type " + type);
      break;
    }
  }

  //  ['measure01', 0, 1, [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], 4, 
  //db:[
  //    [4, 'measure01', 1, rep:[1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1], 
  //      [-30.535999298095703, 19.607000350952148, -5.2620000839233398], 
  //      [-29.770000457763672, 20.642000198364258, -3.375], 1, 0, None, 1, 0, 
  //      [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], 0, None
  //    ], 
  //    1, 
  //    [
  // mb: [2, 
  //       [-29.770000457763672, 19.607000350952148, -3.375, -30.535999298095703, 20.642000198364258, -5.2620000839233398], 
  //       [-30.152999877929688], 
  //       0, None, 0, None, None, None
  //     ]
  //    ], 
  //    0
  //   ], ''
  //  ],

  //  our $hMeasurementTypes = { # to match pymol settings (could easily also add %UNITS)- can't find global parameter that controls this; "extra" refers to additional array elements that aren't for coordinates (as yet undetermined)
  //      1 => { type => 'distance',    atoms => 2,    specifier=>'%0.%digits%VALUE',  digits=>'label_distance_digits', extra=>0 },
  //      4 => { type => 'angle',       atoms => 3,    specifier=>'%0.%digits%VALUE',  digits=>'label_angle_digits',    extra=>6 },
  //      6 => { type => 'torsion',     atoms => 4,    specifier=>'%0.%digits%VALUE',  digits=>'label_dihedral_digits', extra=>6 },
  // };

  private boolean checkBranch(JmolList<Object> branch) {
    branchName = getString(branch, 0);
    if (branchName.indexOf("_") == 0 || getInt(branch, 1) != 0) // otherwise, it's just a selection
      return false;
    isHidden = (getInt(branch, 2) != 1);
    return !isHidden;
  }

  private void processBranchMeasure(JmolList<Object> deepBranch) {
    Logger.info("PyMOL measure " + branchName);
    if (isHidden)
    //if (isHidden || branchName.indexOf("measure") < 0
      //  && !branchName.startsWith("d"))
      return;
    JmolList<Object> measure = getList(getList(deepBranch, 2), 0);

    int color = getInt(getList(deepBranch, 0), 2);
    //List<Object> reps = getList(getList(deepBranch, 0), 3);
    int pt;
    int nCoord = (measure.get(pt = 1) instanceof JmolList<?> ? 2 : measure
        .get(pt = 4) instanceof JmolList<?> ? 3
        : measure.get(pt = 6) instanceof JmolList<?> ? 4 : 0);
    if (nCoord == 0)
      return;
    JmolList<Object> list = getList(measure, pt);
    int len = list.size();
    int p = 0;
    float rad = getFloatSetting(PyMOL.dash_width)/1000;
    if (rad == 0)
      rad = 0.002f;
    while (p < len) {
      JmolList<Object> points = new JmolList<Object>();
      for (int i = 0; i < nCoord; i++, p += 3)
        points.addLast(getPoint(list, p, new Point3fi()));
      BS bs = BSUtil.newAndSetBit(0);
      MeasurementData md = new MeasurementData(viewer, points);
      md.note = branchName;
      String strFormat = "";
      int nDigits = -1;
      switch (nCoord) {
      case 2:
        nDigits = (int) getFloatSetting(PyMOL.label_distance_digits);
        break;
      case 3:
        nDigits = (int) getFloatSetting(PyMOL.label_angle_digits);
        break;
      case 4:
        nDigits = (int) getFloatSetting(PyMOL.label_dihedral_digits);
        break;
      }
      if (nDigits > 0)
        strFormat = nCoord + ":%0." + nDigits + "VALUE %UNITS";
      else
        strFormat = "";
      md.strFormat = strFormat;
      md.colix = C.getColix(PyMOL.getRGB(color));
      ModelSettings ms = new ModelSettings(JC.SHAPE_MEASURES, bs, md);
      ms.setSize(rad);
      //int n = -(int) (getFloatSetting(PyMOL.dash_width) + 0.5);
      //ss.setSize(0.2f); probably good, but this will set it to be not dashed. Should implement that in Jmol
      modelSettings.addLast(ms);

    }

  }

  private void processBranchModels(JmolList<Object> deepBranch) {
    if (!isMovie)
      processCryst(getList(deepBranch, 10));
    atomCount = atomCount0 = atomSetCollection.getAtomCount();
    atomMap = new int[getInt(deepBranch, 3)];
    JmolList<Object> states = getList(deepBranch, 4);
    JmolList<Object> bonds = getList(deepBranch, 6);
    pymolAtoms = getBranchAoms(deepBranch);
    int ns = states.size();
    System.out.println(ns + " PyMOL states");
    if (ns == 1)
      allStates = true;
    BS bsState = null;
    BS bsAtoms = BS.newN(atomCount0 + pymolAtoms.size());
    addName(branchName, bsAtoms);
    if (isMovie) {
      // we create only one model and put all atoms into it.
      if (nModels == 0)
        model(++nModels);
      int n = pymolAtoms.size();
      // only pull in referenced atoms 
      // (could revise this if necessary and pull in all atoms)
      bsState = BS.newN(n);
      if (lstTrajectories.size() == 0) {
        for (int i = ns; --i >= 0;) {
          lstTrajectories.addLast(new P3[totalAtomCount]);
          lstStates.addLast(new BS());
        }
      }
      for (int i = ns; --i >= 0;) {
        JmolList<Object> state = getList(states, i);
        JmolList<Object> idxToAtm = getList(state, 3);
        for (int j = idxToAtm.size(); --j >= 0;)
          bsState.set(j);
      }
      for (int i = bsState.nextSetBit(0); i >= 0; i = bsState.nextSetBit(i + 1))
        if (!addAtom(pymolAtoms, i, -1, null, bsAtoms))
          bsState.clear(i);
      for (int i = 0; i < ns; i++) {
        JmolList<Object> state = getList(states, i);
        JmolList<Object> coords = getList(state, 2);
        JmolList<Object> idxToAtm = getList(state, 3);
        P3[] trajectory = lstTrajectories.get(i);
        BS bs = lstStates.get(i);
        for (int j = idxToAtm.size(); --j >= 0;) {
          int apt = getInt(idxToAtm, j);
          if (!bsState.get(apt))
            continue;
          int ia = atomMap[apt];
          bs.set(ia);
          int cpt = j * 3;
          float x = getFloatAt(coords, cpt);
          float y = getFloatAt(coords, ++cpt);
          float z = getFloatAt(coords, ++cpt);
          trajectory[ia] = P3.new3(x, y, z);
          if (branchName.startsWith("E20"))
            System.out.println(branchName + " " + ia + "  " + trajectory[ia]);
          BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
        }
      }
    } else {
      for (int i = 0; i < ns; i++) {
        JmolList<Object> state = getList(states, i);
        JmolList<Object> coords = getList(state, 2);
        JmolList<Object> idxToAtm = getList(state, 3);
        int n = idxToAtm.size();
        String name = getString(state, 5).trim();
        if (n == 0)
          continue;
        branchID++;
        if (name.length() == 0) {
          currentFrame = pymolFrame;
          if (lstStates.size() < ns)
            for (int j = lstStates.size(); j < ns; j++)
              lstStates.addLast(new BS());
          bsState = lstStates.get(i);
        } else {
          bsAtoms = BS.newN(atomCount0 + pymolAtoms.size());
          addName(name, bsAtoms);
        }
        processStructures();
        setSurface();
        model(++nModels);
        for (int idx = 0; idx < n; idx++)
          addAtom(pymolAtoms, getInt(idxToAtm, idx), idx, coords, bsState);
        if (bsState != null)
          bsAtoms.or(bsState);
      }
    }
    Logger.info("reading " + (atomCount - atomCount0) + " atoms");
    processStructures();
    setSurface();
    processBonds(bonds);
    dumpBranch();
  }

  private void addName(String name, BS bs) {
    char[] chars = name.toLowerCase().toCharArray();
    for (int i = chars.length; --i >= 0;)
      if (!Character.isLetterOrDigit(chars[i]))
        chars[i] = '_';
    
    htNames.put("__" + String.valueOf(chars), bs);
  }

  private static int getBranchType(JmolList<Object> branch) {
    return getInt(branch, 4);
  }

  private static JmolList<Object> getBranchAoms(JmolList<Object> deepBranch) {
    return getList(deepBranch, 7);
  }

  @Override
  protected void model(int modelNumber) {
    bsModelAtoms.clearAll();
    super.model(modelNumber);
  }

  //resix     => 0,     # without insertion code, unlike "resno"
  //chain     => 1,     # chain ID
  //ac        => 2,     # alternate conformation indicator
  //resno     => 3,     # using this and not the sequence number (resix) to deal with boundary case of insertion code... untested    
  //segid     => 4,     # segment ID
  //residue   => 5,     # 3-letter identifier
  //atom      => 6,     # (e.g. CB, NZ)
  //symbol    => 7,     # (e.g. C, N)
  //mol2      => 8,     # MOL2 atom type (i.e. N.am)
  //label     => 9,     # label text
  //ss        => 10,    # s.s. assignment, S/H/L/""
  //??        => 11,
  //??        => 12,
  //type      => 13,    # internal index number of "atom name"
  //bf        => 14,    # temperature factor
  //occ       => 15,    # occupany
  //vdw       => 16,    # van der Waals radius
  //??        => 17,
  //charge    => 18,    # atom charge
  //??        => 19,
  //reps      => 20,    # representation flags 
  //color     => 21,    # color code index
  //atomno    => 22,    # original PDB atom number
  //cartoon   => 23,    # cartoon type modifier

  //### UNMAPPED: 11, 12, 17, 19

  /**
   * @param pymolAtoms
   *        list of atom details
   * @param apt
   *        array pointer into pymolAtoms
   * @param icoord
   *        array pointer into coords (/3)
   * @param coords
   *        coordinates array
   * @param bsState
   *        this state -- Jmol atomIndex
   * @return true if successful
   * 
   */
  private boolean addAtom(JmolList<Object> pymolAtoms, int apt, int icoord,
                          JmolList<Object> coords, BS bsState) {
    atomMap[apt] = -1;
    JmolList<Object> a = getList(pymolAtoms, apt);
    int seqNo = getInt(a, 0); // may be negative
    String chainID = getString(a, 1);
    String altLoc = getString(a, 2);
    String insCode = " "; //?    
    String name = getString(a, 6);
    String group3 = getString(a, 5);
    if (group3.length() > 3)
      group3 = group3.substring(0, 3);
    if (group3.equals(" "))
      group3 = "UNK";
    if (nucleic.indexOf(group3) >= 0) {
      ssMapAtom.get("nucleic").set(atomCount);
    }
    String sym = getString(a, 7);
    if (sym.equals("A"))
      sym = "C";
    Atom atom = processAtom(name, altLoc.charAt(0), group3, chainID.charAt(0),
        seqNo, insCode.charAt(0), false, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return false;
    if (group3.equals("HOH"))
      bsWater.set(atomCount);
    atom.bfactor = getFloatAt(a, 14);
    atom.occupancy = (int) (getFloatAt(a, 15) * 100);
    if (bsState != null)
      bsState.set(atomCount);
    String ss = getString(a, 10);
    BS bs = ssMapSeq.get(ss);
    if (bs == null)
      ssMapSeq.put(ss, bs = new BS());
    if (seqNo >= MIN_RESNO 
        && (!ss.equals(" ") || name.equals("CA")))
      bs.set(seqNo - MIN_RESNO);
    if (ssMapAtom.get(ss) == null)
      ssMapAtom.put(ss, new BS());

    JmolList<Object> list2 = getList(a, 20);
    for (int i = 0; i < REP_MAX; i++)
      if (getInt(list2, i) == 1)
        reps[i].set(atomCount);
    if (reps[REP_LABELS].get(atomCount)) {
      String label = getString(a, 9);
      if (label.equals(" "))
        reps[REP_LABELS].clear(atomCount);
      else
        labels.addLast(label);
    }
    boolean isPutty = false;
    if (!reps[REP_CARTOON].get(atomCount) && reps[REP_LINES].get(atomCount) && reps[REP_NONBONDED].get(atomCount)) {
      reps[REP_LINES].clear(atomCount);
      reps[REP_CARTOON].set(atomCount);
      isPutty = true;
      // what the heck??!!
    }
    if (reps[REP_CARTOON].get(atomCount)) {
      int cartoonType= (isPutty ? 7 : getInt(a, 23));
      /*
            -1 => { type=>'skip',       converted=>undef },
             0 => { type=>'automatic',  converted=>1 },
             1 => { type=>'loop',       converted=>1 },
             2 => { type=>'rectangle',  converted=>undef },
             3 => { type=>'oval',       converted=>undef },
             4 => { type=>'tube',       converted=>1 },
             5 => { type=>'arrow',      converted=>undef },
             6 => { type=>'dumbbell',   converted=>undef },
             7 => { type=>'putty',      converted=>1 },

       */
      switch (cartoonType) {
      case -1:
        reps[REP_CARTOON].clear(atomCount);
        break;
      case 1:
      case 4:
      case 7:
        reps[REP_JMOL_TRACE].set(atomCount);
        reps[REP_CARTOON].clear(atomCount);
        break;
      }      
    }
    bsHidden.setBitTo(atomCount, isHidden);
    bsModelAtoms.set(atomCount);
    atomMap[apt] = atomCount++;
    int serNo = getInt(a, 22);
    int charge = getInt(a, 18);
    int cpt = icoord * 3;
    float x = getFloatAt(coords, cpt);
    float y = getFloatAt(coords, ++cpt);
    float z = getFloatAt(coords, ++cpt);
    if (coords != null)
      BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    //System.out.println(chainID +  " " + fileAtomIndex + " " + serNo + " " + x  + " " + y + " " + z);
    processAtom2(atom, serNo, x, y, z, charge);
    int color = PyMOL.getRGB(getInt(a, 21));
    colixList.addLast(Integer.valueOf(C.getColixO(Integer.valueOf(color))));
    return true;
  }

  private void dumpBranch() {
   // for (int i = 0; i < reps.length; i++)
   //   if (reps[i].cardinality() > 0)
   //     System.out.println("reps[" + i + "]= " + reps[i].cardinality() + " " + reps[i]);
    System.out.println("----------");
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
  }

  private void processStructures() {
    if (atomSetCollection.bsStructuredModels == null)
      atomSetCollection.bsStructuredModels = new BS();
    atomSetCollection.bsStructuredModels.set(Math.max(atomSetCollection
        .getCurrentAtomSetIndex(), 0));

    processSS(ssMapSeq.get("H"), ssMapAtom.get("H"), EnumStructure.HELIX, 0);
    processSS(ssMapSeq.get("S"), ssMapAtom.get("S"), EnumStructure.SHEET, 1);
    processSS(ssMapSeq.get("L"), ssMapAtom.get("L"), EnumStructure.TURN, 0);
    processSS(ssMapSeq.get(" "), ssMapAtom.get(" "), EnumStructure.NONE, 0);
    ssMapSeq = new Hashtable<String, BS>();
  }

  private void processSS(BS bsSeq, BS bsAtom, EnumStructure type,
                         int strandCount) {
    if (bsSeq == null)
      return;
    int istart = -1;
    int iend = -1;
    int inew = -1;
    int imodel = -1;
    int thismodel = -1;
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = atomCount0; i < atomCount; i++) {
      thismodel = atoms[i].atomSetIndex;
      int seqNo = atoms[i].sequenceNumber;
      if (seqNo >= MIN_RESNO && bsSeq.get(seqNo - MIN_RESNO)) {
        if (istart >= 0) {
          if (imodel == thismodel) {
            iend = i;
            continue;
          }
          inew = i;
        } else {
          istart = iend = i;
          imodel = thismodel;
          continue;
        }
      } else if (istart < 0) {
        continue;
      } else {
        inew = -1;
      }
      if (type != EnumStructure.NONE) {
        Structure structure = new Structure(imodel, type, type,
            type.toString(), ++strucNo, strandCount);
        Atom a = atoms[istart];
        Atom b = atoms[iend];
        structure.set(a.chainID, a.sequenceNumber, a.insertionCode, b.chainID,
            b.sequenceNumber, b.insertionCode);
        atomSetCollection.addStructure(structure);
      }
      bsAtom.setBits(istart, iend + 1);
      istart = iend = inew;
    }
  }

  private void processBonds(JmolList<Object> bonds) {
    bsBonded.clear(atomCount); // sets length
    for (int i = 0; i < bonds.size(); i++) {
      JmolList<Object> b = getList(bonds, i);
      int order = (valence ? getInt(b, 2) : 1);
      if (order < 1 || order > 3)
        order = 1;
      // TODO: hydrogen bonds?
      int ia = atomMap[getInt(b, 0)];
      int ib = atomMap[getInt(b, 1)];
      bsBonded.set(ia);
      bsBonded.set(ib);
      atomSetCollection.addBond(new Bond(ia, ib, order));
    }
  }

  private void processCryst(JmolList<Object> cryst) {
    if (cryst == null || cryst.size() == 0)
      return;
    JmolList<Object> l = getList(getList(cryst, 0), 0);
    JmolList<Object> a = getList(getList(cryst, 0), 1);
    setUnitCell(getFloatAt(l, 0), getFloatAt(l, 1), getFloatAt(l, 2), getFloatAt(a, 0),
        getFloatAt(a, 1), getFloatAt(a, 2));
    setSpaceGroupName(getString(cryst, 1));
  }

  ////////////////// set the rendering ////////////////

  /**
   * This is what a normal reader would not have. Only executed if NOT in a
   * state script
   * 
   * @param view
   * 
   */

  private void setRendering(JmolList<Object> view) {

    if (isStateScript)
      return;

    setJmolDefaults();
    SB sb = new SB();
    setView(sb, view);
    setColixes();
    setShapes();
    setFrame();

    addJmolScript(sb.toString());
  }

  private void setJmolDefaults() {
    viewer.setBooleanProperty("navigationMode", false);
    viewer.setBooleanProperty("zoomLarge", false);
    viewer.setBooleanProperty("ssBondsBackbone", false);
    viewer.setStringProperty("measurementUnits", "ANGSTROMS");
  }

  private void setColixes() {
    colixes = new short[colixList.size()];
    for (int i = colixes.length; --i >= 0;)
      colixes[i] = (short) colixList.get(i).intValue();
  }

  private final static int REP_STICKS = 0;
  private final static int REP_SPHERES = 1;
  private final static int REP_LABELS = 3;
  private final static int REP_NBSPHERES = 4;
  private final static int REP_CARTOON = 5;
  private final static int REP_BACKBONE = 6;
  private final static int REP_LINES = 7;
  private final static int REP_DOTS = 9;
  private final static int REP_NONBONDED = 11;
  private final static int REP_MAX = 12;
  
  private final static int REP_JMOL_MIN = 13;
  private final static int REP_JMOL_TRACE = 13;
  private final static int REP_JMOL_MAX = 14;

  //TODO:

  private final static int REP_SURFACE = 2;
  private final static int REP_MESH = 8;
  private final static int REP_DASHES = 10;

  private void setShapes() {
    ModelSettings ms;
    BS bs = BSUtil.newBitSet2(0, atomCount);
    ms = new ModelSettings(JC.SHAPE_BALLS, bs, null);
    ms.setSize(0);
    ms.setColors(colixes, 0);
    modelSettings.addLast(ms);
    ms = new ModelSettings(JC.SHAPE_STICKS, bs, null);
    ms.setSize(0);
    modelSettings.addLast(ms);
    for (int i = 0; i < REP_JMOL_MAX; i++)
      setShape(i);
    if (!bsHidden.isEmpty())
      modelSettings.addLast(new ModelSettings(T.hidden, bsHidden, null));
  }

  private void setShape(int shapeID) {
    // add more to implement
    BS bs = reps[shapeID];
    float f;
    switch (shapeID) {
    case REP_NONBONDED:
    case REP_NBSPHERES:
      bs.andNot(bsBonded);
      break;
    case REP_LINES:
      bs.andNot(reps[REP_STICKS]);
      break;
    }
    if (bs.isEmpty())
      return;
    ModelSettings ss = null;
    switch (shapeID) {
    case REP_NONBONDED:
      f = getFloatSetting(PyMOL.nonbonded_size);
      BS bs1 = BSUtil.copy(bs);
      bs1.andNot(bsWater);
      if (!bs1.isEmpty()) {
        ss = new ModelSettings(JC.SHAPE_BALLS, bs1, null);
        ss.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR,
            EnumVdw.AUTO);
        ss.setColors(colixes, 0);
        modelSettings.addLast(ss);
      }
      bs1 = BSUtil.copy(bs);
      bs1.and(bsWater);
      if (!bs1.isEmpty()) {
        ss = new ModelSettings(JC.SHAPE_STARS, bs, null);
        ss.rd = new RadiusData(null, 0.25f, RadiusData.EnumType.ABSOLUTE,
            EnumVdw.AUTO);
        ss.setColors(colixes, 0);
        modelSettings.addLast(ss);
      }
      break;
    case REP_NBSPHERES:
    case REP_SPHERES:
      ss = new ModelSettings(JC.SHAPE_BALLS, bs, null);
      f = (shapeID == REP_NBSPHERES ? 1 : getFloatSetting(PyMOL.sphere_scale));
      ss.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      ss.setColors(colixes, 0);
      modelSettings.addLast(ss);
      break;
    case REP_STICKS:
      f = getFloatSetting(PyMOL.stick_radius) * 2;
      ss = new ModelSettings(JC.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      modelSettings.addLast(ss);
      break;
    case REP_DOTS: //   = 9;
      ss = new ModelSettings(JC.SHAPE_DOTS, bs, null);
      f = getFloatSetting(PyMOL.sphere_scale);
      ss.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      modelSettings.addLast(ss);
      break;
    case REP_LINES:
      f = getFloatSetting(PyMOL.line_width) / 15;
      ss = new ModelSettings(JC.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      modelSettings.addLast(ss);
      break;
    case REP_CARTOON:
      setCartoon("H", PyMOL.cartoon_oval_length, 2);
      setCartoon("S", PyMOL.cartoon_rect_length, 2);
      setCartoon("L", PyMOL.cartoon_loop_radius, 2);
      setCartoon(" ", PyMOL.cartoon_loop_radius, 2);
      break;
    case REP_SURFACE: //   = 2;
      // must be done for each model
      break;
    case REP_LABELS: //   = 3;
      ss = new ModelSettings(JC.SHAPE_LABELS, bs, labels);
      modelSettings.addLast(ss);
      break;
    case REP_JMOL_TRACE:
      setTrace(bs, false);
      break;
    case REP_BACKBONE: //   = 6;
      setTrace(bs, true);
      break;
    case REP_MESH: //   = 8;
    case REP_DASHES: //   = 10;
    default:
      if (shapeID < REP_JMOL_MIN)
        System.out.println("Unprocessed representation type " + shapeID);
    }
  }

  private void setSurface() {
    BS bs = reps[REP_SURFACE];
    if (isStateScript || bsModelAtoms.isEmpty() || bs.isEmpty())
      return;
    ModelSettings ss = new ModelSettings(JC.SHAPE_ISOSURFACE, bs, branchName
        + "_" + branchID);
    ss.setSize(getFloatSetting(PyMOL.solvent_radius));
    ss.translucency = getFloatSetting(PyMOL.transparency);
    setColixes();
    ss.setColors(colixes, 0);
    modelSettings.addLast(ss);
  }

  private void setTrace(BS bs, boolean isBackbone) {
    ModelSettings ss;
    if (!isBackbone) {
      BS bsNuc = ssMapAtom.get("nucleic");
      bsNuc.and(bs);
      if (!bsNuc.isEmpty() && getBooleanSetting(PyMOL.cartoon_ladder_mode)) {
        // we will just use cartoons for ladder mode
        ss = new ModelSettings(JC.SHAPE_CARTOON, bsNuc, null);
        ss.setColors(colixes, cartoonTranslucency);
        ss.setSize(getFloatSetting(PyMOL.cartoon_tube_radius) * 2);
        modelSettings.addLast(ss);
        bs.andNot(bsNuc);
        if (bs.isEmpty())
          return;
      }
    }
    ss = new ModelSettings(JC.SHAPE_TRACE, bs, null);
    ss.setColors(colixes, cartoonTranslucency);
    ss.setSize(isBackbone ? 0.3f : getFloatSetting(PyMOL.cartoon_tube_radius) * 2);
    modelSettings.addLast(ss);
  }

  private void setCartoon(String key, int sizeID, float factor) {
    BS bs = ssMapAtom.get(key);
    if (bs == null)
      return;
    bs.and(reps[REP_CARTOON]);
    if (bs.isEmpty())
      return;
    ModelSettings ss = new ModelSettings(JC.SHAPE_CARTOON, bs, null);
    ss.setColors(colixes, cartoonTranslucency);
    ss.setSize(getFloatSetting(sizeID) * factor);
    modelSettings.addLast(ss);
  }

  private void setFrame() {
    BS bs = BSUtil.newAndSetBit(0);
    if (!allStates && isMovie) {
      modelSettings.addLast(new ModelSettings(T.movie, bs, pymol.get("movie")));
    } else if (!allStates || isMovie) {
      modelSettings.addLast(new ModelSettings(T.frame, bs, Integer
          .valueOf(currentFrame)));
    } else {
      modelSettings.addLast(new ModelSettings(T.frame, bs, Integer
          .valueOf(-1)));
    }
    
  }

  private void setView(SB sb, JmolList<Object> view) {

    float w = 2 * getRotationRadius();

    // calculate Jmol camera position, which is in screen widths,
    // and is from the front of the screen, not the center.
    //
    //     |---------model width------------------|
    //               |-------o--------| 1 unit
    //                       |       /
    //                       |      /
    //                       |     /
    //                     d |    /
    //                       |   /
    //                       |  /
    //                       | / angle = fov/2
    //                       |/
    //

    P3 ptCenter = getPoint(view, 19, new P3()); // o
    sb.append("center ").append(Escape.eP(ptCenter)).append(";");

    float fov = getFloatSetting(PyMOL.field_of_view);

    float jmolCameraToCenter = (float) (0.5 / Math.tan(fov / 2 * Math.PI / 180)); // d
    float pymolCameraToCenter = -getFloatAt(view, 18) / w;
    float jmolCameraDepth = (jmolCameraToCenter - 0.5f);
    float zoom = jmolCameraToCenter / pymolCameraToCenter * 100;
    float aspectRatio = viewer.getScreenWidth() * 1.0f
        / viewer.getScreenHeight();
    if (aspectRatio < 1)
      zoom /= aspectRatio;

    float pymolCameraToSlab = getFloatAt(view, 22) / w;
    float pymolCameratToDepth = getFloatAt(view, 23) / w;
    int slab = 50 + (int) ((pymolCameraToCenter - pymolCameraToSlab) * 100);
    int depth = 50 + (int) ((pymolCameraToCenter - pymolCameratToDepth) * 100);

    sb
        .append("set perspectiveDepth " + (!getBooleanSetting(PyMOL.ortho))
            + ";");

    sb.append("set cameraDepth " + jmolCameraDepth + ";");
    sb.append("zoom " + zoom + "; slab on; slab " + slab + "; depth " + depth
        + ";");

    sb
        .append("rotate @{quaternion({")
        // only the first two rows are needed
        .appendF(getFloatAt(view, 0)).append(" ").appendF(getFloatAt(view, 1))
        .append(" ").appendF(getFloatAt(view, 2)).append("}{").appendF(
            getFloatAt(view, 4)).append(" ").appendF(getFloatAt(view, 5))
        .append(" ").appendF(getFloatAt(view, 6)).append("})};");
    sb.append("translate X ").appendF(getFloatAt(view, 16)).append(
        " angstroms;");
    sb.append("translate Y ").appendF(-getFloatAt(view, 17)).append(
        " angstroms;");

    // seems to be something else here -- fog is not always present
    boolean depthCue = getBooleanSetting(PyMOL.depth_cue); // 84
    boolean fog = getBooleanSetting(PyMOL.fog); // 88

 
      if (depthCue && fog) {
        float range = depth - slab;
        float fog_start = getFloatSetting(PyMOL.fog_start); // 192
        sb.append("set zShade true; set zshadePower 1;set zslab "
            + (slab + fog_start * range) + "; set zdepth " + depth + ";");
      } else if (depthCue) {
        sb.append("set zShade true; set zshadePower 1;set zslab "
            + ((slab + depth) / 2f) + "; set zdepth " + depth + ";");
      } else {
        sb.append("set zShade false;");
      }

    sb.append("set traceAlpha "
        + getBooleanSetting(PyMOL.cartoon_round_helices) + ";");
    sb.append("set cartoonRockets "
        + getBooleanSetting(PyMOL.cartoon_cylindrical_helices) + ";");
    sb.append("set ribbonBorder "
        + getBooleanSetting(PyMOL.cartoon_fancy_helices) + ";");
    sb.append("set cartoonFancy "
        + !getBooleanSetting(PyMOL.cartoon_fancy_helices) + ";"); // for now

    //{ command => 'set hermiteLevel -4',                                                       comment => 'so that SS reps have some thickness' },
    //{ command => 'set ribbonAspectRatio 8',                                                   comment => 'degree of W/H ratio, but somehow not tied directly to actual width parameter...' },
    sb.append("background " + getList(settings, PyMOL.bg_rgb).get(2) + ";");
    if (isMovie)
      sb.append("animation mode loop;");
  }

  private float getRotationRadius() {
    P3 center = P3.new3((xyzMax.x + xyzMin.x) / 2, (xyzMax.y + xyzMin.y) / 2,
        (xyzMax.z + xyzMin.z) / 2);
    float d2max = 0;
    Atom[] atoms = atomSetCollection.getAtoms();
    if (isMovie)
      for (int i = lstTrajectories.size(); --i >= 0;) {
        P3[] pts = lstTrajectories.get(i);
        for (int j = pts.length; --j >= 0;) {
          P3 pt = pts[j];
          if (pt != null)
            d2max = maxRadius(d2max, pt.x, pt.y, pt.z, center);
        }
      }
    else
      for (int i = 0; i < atomCount; i++) {
        Atom a = atoms[i];
        d2max = maxRadius(d2max, a.x, a.y, a.z, center);
      }
    // 1 is approximate -- for atom radius
    return (float) Math.pow(d2max, 0.5f) + 1;
  }

  private static float maxRadius(float d2max, float x, float y, float z,
                                 P3 center) {
    float dx = (x - center.x);
    float dy = (y - center.y);
    float dz = (z - center.z);
    float d2 = dx * dx + dy * dy + dz * dz;
    if (d2 > d2max)
      d2max = d2;
    return d2max;
  }

}
