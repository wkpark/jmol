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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jmol.adapter.readers.cifpdb.PdbReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolDocument;
import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ShapeSettings;
import org.jmol.script.Token;
import org.jmol.util.BoxInfo; //import org.jmol.util.Escape;
import org.jmol.util.BitSet;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Colix;
import org.jmol.util.ColorUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3f;
import org.jmol.util.Point3fi;
import org.jmol.util.StringXBuilder;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

/**
 * experimental PyMOL PSE (binary Python session) file reader Feb 2013 Jmol
 * 13.1.13
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class PyMOLReader extends PdbReader {
  private List<Object> settings;
  private int atomCount0;
  private int atomCount;
  private int strucNo;
  private boolean isHidden;
  private List<Object> pymolAtoms;
  private BitSet bsBonded = new BitSet();
  private BitSet bsHidden = new BitSet();
  private BitSet bsWater = new BitSet();
  private int[] atomMap;
  private Map<String, BitSet> ssMap = new Hashtable<String, BitSet>();
  private Map<String, BitSet> ssMapA = new Hashtable<String, BitSet>();
  private List<Integer> colixList = new ArrayList<Integer>();
  private List<String> labels = new ArrayList<String>();

  private List<ShapeSettings> shapes = new ArrayList<ShapeSettings>();
  private short[] colixes;
  private boolean isStateScript;
  private int width;
  private int height;

  private boolean valence;

  @Override
  protected void initializeReader() throws Exception {
    isBinary = true;
    isStateScript = htParams.containsKey("isStateScript");
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("noAutoBond",
        Boolean.TRUE);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("shapes", shapes);
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("isPyMOL", Boolean.TRUE);
    super.initializeReader();
  }

  //  private Map<Object, Object> temp  = new Hashtable<Object, Object>();

  private Point3f xyzMin = Point3f.new3(1e6f, 1e6f, 1e6f);
  private Point3f xyzMax = Point3f.new3(-1e6f, -1e6f, -1e6f);
  private int nModels;

  @Override
  public void processBinaryDocument(JmolDocument doc) throws Exception {
    PickleReader reader = new PickleReader(doc, (Viewer) viewer);
    Map<String, Object> map = reader.getMap();
    reader = null;
    process(map);
  }

  private BitSet[] reps = new BitSet[17];
  private float cartoonTranslucency;
  private Map<String, Object> movie;
  private boolean isMovie;

  private Map<String, Object> pymol = new Hashtable<String, Object>();
  private List<BitSet> lstStates = new ArrayList<BitSet>();
  private List<Point3f[]> lstTrajectories = new ArrayList<Point3f[]>();
  private int currentFrame = -1;
  private boolean allStates;
  private int totalAtomCount;

  private void process(Map<String, Object> map) {

    addColors(getMapList(map, "colors"));
    for (int i = 0; i < 17; i++)
      reps[i] = BitSet.newN(1000);
    settings = getMapList(map, "settings");
    allStates = getBooleanSetting(PyMOL.all_states);
    List<Object> mov = getMapList(map, "movie");
    if (mov != null && !allStates) {
      int frameCount = getInt(mov, 0);
      if (frameCount > 0) {
        currentFrame = (int) getFloatSetting(PyMOL.frame);
        isMovie = true;
        movie = new Hashtable<String, Object>();
        movie.put("states", lstStates);
        movie.put("trajectories", lstTrajectories);
        movie.put("frameCount", Integer.valueOf(frameCount));
        movie.put("frames", getList(mov, 4));
        movie.put("frameStrings", getList(mov, 5));
        movie.put("currentFrame", Integer.valueOf(currentFrame));
        pymol.put("movie", movie);
      }
    }
    if (!isStateScript && filter != null && filter.indexOf("DORESIZE") >= 0)
      try {
        width = getInt(getMapList(map, "main"), 0);
        height = getInt(getMapList(map, "main"), 1);
        if (width > 0 && height > 0) {
          atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
              "perferredWidthHeight", new int[] { width, height });
          viewer.resizeInnerPanel(width, height);
        }
      } catch (Exception e) {
        // ignore
      }
    valence = getBooleanSetting(PyMOL.valence);
    cartoonTranslucency = getFloatSetting(PyMOL.cartoon_transparency);
    List<Object> names = getMapList(map, "names");
    totalAtomCount = getTotalAtomCount(names);
    Logger.info("PyMOL total atom count = " + totalAtomCount);
    for (int i = 1; i < names.size(); i++)
      processBranch(getList(names, i));

    if (isMovie)
      atomSetCollection.finalizeTrajectoryAs(lstTrajectories, null);

    // we are done if this is a state script

    setRendering(getMapList(map, "view"));
  }

  private int getTotalAtomCount(List<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      List<Object> branch = getList(names, i);
      if (checkBranch(branch) && getBranchType(branch) == BRANCH_MOLECULE)
        n += getBranchAoms(getList(branch, 5)).size();
    }
    return n;
  }

  private void addColors(List<Object> colors) {
    if (colors == null || colors.size() == 0)
      return;
    Point3f pt = new Point3f();
    for (int i = colors.size(); --i >= 0;) {
      List<Object> c = getList(colors, i);
      PyMOL.addColor((Integer) c.get(1), ColorUtil.colorPtToInt(getPoint(
          getList(c, 2), 0, pt)));
    }
  }

  private static String getString(List<Object> list, int i) {
    String s = (String) list.get(i);
    return (s.length() == 0 ? " " : s);
  }

  private static int getInt(List<Object> list, int i) {
    return ((Integer) list.get(i)).intValue();
  }

  private static float getFloat(List<Object> list, int i) {
    return (list == null ? 0 : ((Double) list.get(i)).floatValue());
  }

  private Point3f getPoint(List<Object> list, int i, Point3f pt) {
    pt.set(getFloat(list, i++), getFloat(list, i++), getFloat(list, i));
    return pt;
  }

  @SuppressWarnings("unchecked")
  private static List<Object> getList(List<Object> list, int i) {
    if (list == null || list.size() <= i)
      return null;
    Object o = list.get(i);
    return (o instanceof List<?> ? (List<Object>) o : null);
  }

  @SuppressWarnings("unchecked")
  private static List<Object> getMapList(Map<String, Object> map, String key) {
    return (List<Object>) map.get(key);
  }

  private boolean getBooleanSetting(int i) {
    return (getFloatSetting(i) != 0);
  }

  private float getFloatSetting(int i) {
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

  //  #cf. \pymol\layer1\PyMOLObject.h
  //  #define cObjectCallback     5
  //  #define cObjectGadget       8
  //  #define cObjectCalculator   9
  //  #define cObjectSlice        10
  //  #define cObjectAlignment    11

  private String branchName;
  private BitSet bsModelAtoms = BitSet.newN(1000);
  private int branchID;

  private void processBranch(List<Object> branch) {
    if (!checkBranch(branch))
      return;
    Logger.info("PyMOL model " + (nModels + 1) + " Branch " + branchName
        + (isHidden ? " (hidden)" : ""));
    int type = getBranchType(branch);
    List<Object> deepBranch = getList(branch, 5);
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

  private boolean checkBranch(List<Object> branch) {
    branchName = getString(branch, 0);
    if (branchName.indexOf("_") == 0 || getInt(branch, 1) != 0) // otherwise, it's just a selection
      return false;
    isHidden = (getInt(branch, 2) != 1);
    return !isHidden;
  }

  private void processBranchMeasure(List<Object> deepBranch) {
    if (isHidden || branchName.indexOf("measure") < 0)
      return;
    List<Object> measure = getList(getList(deepBranch, 2), 0);

    int color = getInt(getList(deepBranch, 0), 2);
    //List<Object> reps = getList(getList(deepBranch, 0), 3);
    int pt;
    int nCoord = (measure.get(pt = 1) instanceof List<?> ? 2 : measure
        .get(pt = 4) instanceof List<?> ? 3
        : measure.get(pt = 6) instanceof List<?> ? 4 : 0);
    if (nCoord == 0)
      return;
    List<Object> list = getList(measure, pt);
    List<Object> points = new ArrayList<Object>();
    for (int i = 0, p = 0; i < nCoord; i++, p += 3)
      points.add(getPoint(list, p, new Point3fi()));
    BitSet bs = BitSetUtil.newAndSetBit(0);
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
    if (nDigits >= 0)
      strFormat = nCoord + ":%0." + nDigits + "VALUE %UNITS";
    md.strFormat = strFormat;
    md.colix = Colix.getColix(PyMOL.getRGB(color));
    ShapeSettings ss = new ShapeSettings(JmolConstants.SHAPE_MEASURES, bs, md);
    //int n = -(int) (getFloatSetting(PyMOL.dash_width) + 0.5);
    //ss.setSize(0.2f); probably good, but this will set it to be not dashed. Should implement that in Jmol
    shapes.add(ss);
  }

  private void processBranchModels(List<Object> deepBranch) {
    if (!isMovie)
      processCryst(getList(deepBranch, 10));
    atomCount = atomCount0 = atomSetCollection.getAtomCount();
    atomMap = new int[getInt(deepBranch, 3)];
    List<Object> states = getList(deepBranch, 4);
    List<Object> bonds = getList(deepBranch, 6);
    pymolAtoms = getBranchAoms(deepBranch);
    int ns = states.size();
    BitSet bsState = null;
    if (isMovie) {
      // we create only one model and put all atoms into it.
      if (nModels == 0)
        model(++nModels);
      int n = pymolAtoms.size();
      // only pull in referenced atoms 
      // (could revise this if necessary and pull in all atoms)
      bsState = BitSet.newN(n);
      if (lstTrajectories.size() == 0) {
        for (int i = ns; --i >= 0;) {
          lstTrajectories.add(new Point3f[totalAtomCount]);
          lstStates.add(new BitSet());
        }
      }
      for (int i = ns; --i >= 0;) {
        List<Object> state = getList(states, i);
        List<Object> idxToAtm = getList(state, 3);
        for (int j = idxToAtm.size(); --j >= 0;)
          bsState.set(j);
      }
      for (int i = bsState.nextSetBit(0); i >= 0; i = bsState.nextSetBit(i + 1))
        if (!addAtom(pymolAtoms, i, -1, null, null))
          bsState.clear(i);
      for (int i = 0; i < ns; i++) {
        List<Object> state = getList(states, i);
        List<Object> coords = getList(state, 2);
        List<Object> idxToAtm = getList(state, 3);
        Point3f[] trajectory = lstTrajectories.get(i);
        BitSet bs = lstStates.get(i);
        for (int j = idxToAtm.size(); --j >= 0;) {
          int apt = getInt(idxToAtm, j);
          if (!bsState.get(apt))
            continue;
          int ia = atomMap[apt];
          bs.set(ia);
          int cpt = j * 3;
          float x = getFloat(coords, cpt);
          float y = getFloat(coords, ++cpt);
          float z = getFloat(coords, ++cpt);
          trajectory[ia] = Point3f.new3(x, y, z);
          BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
        }
      }
    } else {
      for (int i = 0; i < ns; i++) {
        List<Object> state = getList(states, i);
        List<Object> coords = getList(state, 2);
        List<Object> idxToAtm = getList(state, 3);
        int n = idxToAtm.size();
        String name = getString(state, 5);
        System.out.println("i=" + i + " nAtoms=" + n + " name=" + name);
        if (n == 0)
          continue;
        branchID++;
        if (name.trim().length() == 0) {
          currentFrame = (int) getFloatSetting(PyMOL.frame);
          if (lstStates.size() < ns)
            for (int j = lstStates.size(); j < ns; j++)
              lstStates.add(new BitSet());
          bsState = lstStates.get(i);
        }
        processStructures();
        setSurface();
        model(++nModels);
        for (int idx = 0; idx < n; idx++)
          addAtom(pymolAtoms, getInt(idxToAtm, idx), idx, coords, bsState);
      }
    }
    Logger.info("read " + (atomCount - atomCount0) + " atoms");
    processStructures();
    setSurface();
    processBonds(bonds);
  }

  private static int getBranchType(List<Object> branch) {
    return getInt(branch, 4);
  }

  private static List<Object> getBranchAoms(List<Object> deepBranch) {
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
  private boolean addAtom(List<Object> pymolAtoms, int apt, int icoord,
                          List<Object> coords, BitSet bsState) {
    atomMap[apt] = -1;
    List<Object> a = getList(pymolAtoms, apt);
    int seqNo = getInt(a, 0);
    String chainID = getString(a, 1);
    String altLoc = getString(a, 2);
    String insCode = " "; //?    
    String group3 = getString(a, 5);
    if (group3.length() > 3)
      group3 = group3.substring(0, 3);
    if (group3.equals(" "))
      group3 = "UNK";
    String name = getString(a, 6);
    String sym = getString(a, 7);
    if (sym.equals(" "))
      sym = getString(a, 7);
    Atom atom = processAtom(name, altLoc.charAt(0), group3, chainID.charAt(0),
        seqNo, insCode.charAt(0), false, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return false;
    if (group3.equals("HOH"))
      bsWater.set(atomCount);
    atom.bfactor = getFloat(a, 14);
    atom.occupancy = (int) (getFloat(a, 15) * 100);
    if (bsState != null)
      bsState.set(atomCount);
    String ss = getString(a, 10);
    BitSet bs = ssMap.get(ss);
    if (bs == null)
      ssMap.put(ss, bs = new BitSet());
    if (ssMapA.get(ss) == null)
      ssMapA.put(ss, new BitSet());
    List<Object> list2 = getList(a, 20);
    for (int i = 0; i < REP_MAX; i++)
      if (getInt(list2, i) == 1)
        reps[i].set(atomCount);
    if (reps[REP_LABELS].get(atomCount)) {
      String label = getString(a, 9);
      if (label.equals(" "))
        reps[REP_LABELS].clear(atomCount);
      else
        labels.add(label);
    }
    bsHidden.setBitTo(atomCount, isHidden);
    bsModelAtoms.set(atomCount);
    atomMap[apt] = atomCount++;
    int serNo = getInt(a, 22);
    bs.set(seqNo);
    int charge = getInt(a, 18);
    int cpt = icoord * 3;
    float x = getFloat(coords, cpt);
    float y = getFloat(coords, ++cpt);
    float z = getFloat(coords, ++cpt);
    if (coords != null)
      BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    //System.out.println(chainID +  " " + fileAtomIndex + " " + serNo + " " + x  + " " + y + " " + z);
    processAtom2(atom, serNo, x, y, z, charge);
    int color = PyMOL.getRGB(getInt(a, 21));
    colixList.add(Integer.valueOf(Colix.getColixO(Integer.valueOf(color))));
    return true;
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
  }

  private void processStructures() {
    if (atomSetCollection.bsStructuredModels == null)
      atomSetCollection.bsStructuredModels = new BitSet();
    atomSetCollection.bsStructuredModels.set(Math.max(atomSetCollection
        .getCurrentAtomSetIndex(), 0));

    processSS(ssMap.get("H"), ssMapA.get("H"), EnumStructure.HELIX, 0);
    processSS(ssMap.get("S"), ssMapA.get("S"), EnumStructure.SHEET, 1);
    processSS(ssMap.get("L"), ssMapA.get("L"), EnumStructure.TURN, 0);
    ssMap = new Hashtable<String, BitSet>();
  }

  private void processSS(BitSet bs, BitSet bsAtomType, EnumStructure type,
                         int strandCount) {
    if (bs == null)
      return;
    int istart = -1;
    int iend = -1;
    int inew = -1;
    int imodel = -1;
    int thismodel = -1;
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = atomCount0; i < atomCount; i++) {
      int seqNo = atoms[i].sequenceNumber;
      thismodel = atoms[i].atomSetIndex;
      if (seqNo >= 0 && bs.get(seqNo)) {
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
      Structure structure = new Structure(imodel, type, type, type.toString(),
          ++strucNo, strandCount);
      Atom a = atoms[istart];
      Atom b = atoms[iend];
      bsAtomType.setBits(istart, iend + 1);
      structure.set(a.chainID, a.sequenceNumber, a.insertionCode, b.chainID,
          b.sequenceNumber, b.insertionCode);
      atomSetCollection.addStructure(structure);
      istart = iend = inew;
    }
  }

  private void processBonds(List<Object> bonds) {
    bsBonded.clear(atomCount); // sets length
    for (int i = 0; i < bonds.size(); i++) {
      List<Object> b = getList(bonds, i);
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

  private void processCryst(List<Object> cryst) {
    if (cryst == null || cryst.size() == 0)
      return;
    List<Object> l = getList(getList(cryst, 0), 0);
    List<Object> a = getList(getList(cryst, 0), 1);
    setUnitCell(getFloat(l, 0), getFloat(l, 1), getFloat(l, 2), getFloat(a, 0),
        getFloat(a, 1), getFloat(a, 2));
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

  private void setRendering(List<Object> view) {

    if (isStateScript)
      return;

    setJmolDefaults();
    StringXBuilder sb = new StringXBuilder();
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
    viewer.setBooleanProperty("cartoonFancy", true); // for now
    viewer.setStringProperty("measurementUnits", "ANGSTROMS");
  }

  private void setColixes() {
    colixes = new short[colixList.size()];
    for (int i = colixes.length; --i >= 0;)
      colixes[i] = (short) colixList.get(i).intValue();
  }

  private final static int REP_STICKS = 0;
  private final static int REP_SPHERES = 1;
  private final static int REP_NBSPHERES = 4;
  private final static int REP_CARTOON = 5;
  private final static int REP_LINES = 7;
  private final static int REP_NONBONDED = 11;
  private final static int REP_MAX = 12;

  //TODO:

  private final static int REP_SURFACE = 2;
  private final static int REP_LABELS = 3;
  private final static int REP_BACKBONE = 6;
  private final static int REP_MESH = 8;
  private final static int REP_DOTS = 9;
  private final static int REP_DASHES = 10;

  private void setShapes() {
    ShapeSettings ss;
    BitSet bs = BitSetUtil.newBitSet2(0, atomCount);
    ss = new ShapeSettings(JmolConstants.SHAPE_BALLS, bs, null);
    ss.setSize(0);
    ss.setColors(colixes, 0);
    shapes.add(ss);
    ss = new ShapeSettings(JmolConstants.SHAPE_STICKS, bs, null);
    ss.setSize(0);
    shapes.add(ss);
    for (int i = 0; i < REP_MAX; i++)
      setShape(i);
    if (!bsHidden.isEmpty())
      shapes.add(new ShapeSettings(Token.hidden, bsHidden, null));
  }

  private void setShape(int shapeID) {
    // add more to implement
    BitSet bs = reps[shapeID];
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
    ShapeSettings ss = null;
    switch (shapeID) {
    case REP_NONBONDED:
      f = getFloatSetting(PyMOL.nonbonded_size);
      BitSet bs1 = new BitSet();
      bs1.or(bs);
      bs1.andNot(bsWater);
      if (!bs1.isEmpty()) {
        ss = new ShapeSettings(JmolConstants.SHAPE_BALLS, bs, null);
        ss.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR,
            EnumVdw.AUTO);
        ss.setColors(colixes, 0);
        shapes.add(ss);
      }
      bs1.clearAll();
      bs1.or(bs);
      bs1.and(bsWater);
      if (!bs1.isEmpty()) {
        ss = new ShapeSettings(JmolConstants.SHAPE_STARS, bs, null);
        ss.rd = new RadiusData(null, 0.25f, RadiusData.EnumType.ABSOLUTE,
            EnumVdw.AUTO);
        ss.setColors(colixes, 0);
        shapes.add(ss);
      }
      break;
    case REP_NBSPHERES:
    case REP_SPHERES:
      f = (shapeID == REP_NBSPHERES ? 1 : getFloatSetting(PyMOL.sphere_scale));
      ss = new ShapeSettings(JmolConstants.SHAPE_BALLS, bs, null);
      ss.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      ss.setColors(colixes, 0);
      shapes.add(ss);
      break;
    case REP_STICKS:
      f = getFloatSetting(PyMOL.stick_radius) * 2;
      ss = new ShapeSettings(JmolConstants.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      shapes.add(ss);
      break;
    case REP_LINES:
      f = getFloatSetting(PyMOL.line_width) * 8 / 1000;
      ss = new ShapeSettings(JmolConstants.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      shapes.add(ss);
      break;
    case REP_CARTOON:
      setCartoon("H", PyMOL.cartoon_oval_length, 2);
      setCartoon("S", PyMOL.cartoon_rect_length, 2);
      setCartoon("L", PyMOL.cartoon_loop_radius, 2);
      break;
    case REP_SURFACE: //   = 2;
      // must be done for each model
      break;
    case REP_LABELS: //   = 3;
      ss = new ShapeSettings(JmolConstants.SHAPE_LABELS, bs, labels);
      shapes.add(ss);
      break;
    case REP_BACKBONE: //   = 6;
    case REP_MESH: //   = 8;
    case REP_DOTS: //   = 9;
    case REP_DASHES: //   = 10;
    default:
      System.out.println("Unprocessed representation type " + shapeID);
    }
  }

  private void setSurface() {
    BitSet bs = reps[REP_SURFACE];
    if (isStateScript || bsModelAtoms.isEmpty() || bs.isEmpty())
      return;
    ShapeSettings ss = new ShapeSettings(JmolConstants.SHAPE_ISOSURFACE, bs,
        branchName + "_" + branchID);
    ss.setSize(getFloatSetting(PyMOL.solvent_radius));
    ss.translucency = getFloatSetting(PyMOL.transparency);
    setColixes();
    ss.setColors(colixes, 0);
    shapes.add(ss);
  }

  private void setCartoon(String key, int sizeID, float factor) {
    BitSet bs = ssMapA.get(key);
    if (bs == null)
      return;
    bs.and(reps[REP_CARTOON]);
    if (bs.isEmpty())
      return;
    ShapeSettings ss = new ShapeSettings(JmolConstants.SHAPE_CARTOON, bs, null);
    ss.setColors(colixes, cartoonTranslucency);
    ss.setSize(getFloatSetting(sizeID) * factor);
    shapes.add(ss);
  }

  private void setFrame() {
    BitSet bs = BitSetUtil.newAndSetBit(0);
    if (!allStates && pymol.containsKey("movie")) {
      shapes.add(new ShapeSettings(Token.movie, bs, pymol.get("movie")));
    } else {
      shapes.add(new ShapeSettings(Token.frame, bs, Integer
          .valueOf(currentFrame)));
    }
  }

  private void setView(StringXBuilder sb, List<Object> view) {

    float modelWidth = 2 * getRotationRadius();

    // calculate Jmol camera position, which is in screen widths,
    // and is from the front of the screen, not the center.

    float fov = getFloatSetting(PyMOL.field_of_view);
    float tan = (float) Math.tan(fov / 2 * Math.PI / 180);
    float jmolCameraDepth = (0.5f / tan - 0.5f);
    float pymolCameraToCenter = -getFloat(view, 18) / modelWidth;
    float zoom = (jmolCameraDepth + 0.5f) / pymolCameraToCenter * 100;

    sb.append("set cameraDepth " + jmolCameraDepth + ";");
    sb.append("zoom " + zoom + ";");

    Logger.info("set cameraDepth " + jmolCameraDepth);
    Logger.info("zoom " + zoom);

    //float aspectRatio = viewer.getScreenWidth() * 1.0f
    //  / viewer.getScreenHeight();
    //if (aspectRatio < 1)
    //fov *= aspectRatio;

    Point3f center = getPoint(view, 19, new Point3f());

    sb.append("center ").append(Escape.escapePt(center)).append(";");
    sb.append("rotate @{quaternion({")
        // only the first two rows are needed
        .appendF(getFloat(view, 0)).append(" ").appendF(getFloat(view, 1))
        .append(" ").appendF(getFloat(view, 2)).append("}{").appendF(
            getFloat(view, 4)).append(" ").appendF(getFloat(view, 5)).append(
            " ").appendF(getFloat(view, 6)).append("})};");
    sb.append("translate X ").appendF(getFloat(view, 16)).append(" angstroms;");
    sb.append("translate Y ").appendF(-getFloat(view, 17))
        .append(" angstroms;");

    // seems to be something else here -- fog is not always present
    boolean depthCue = getBooleanSetting(PyMOL.depth_cue); // 84
    boolean fog = getBooleanSetting(PyMOL.fog); // 88
    sb.append("set zShade " + (depthCue && fog) + ";");
    if (depthCue && fog) {
      float fog_start = getFloatSetting(PyMOL.fog_start); // 192
      sb.append("set zshadePower 2;set zslab " + (fog_start * 100)
          + "; set zdepth 0;");
    }

    sb
        .append("set perspectiveDepth " + (!getBooleanSetting(PyMOL.ortho))
            + ";");

    sb.append("set traceAlpha "
        + getBooleanSetting(PyMOL.cartoon_round_helices) + ";");
    sb.append("set cartoonRockets "
        + getBooleanSetting(PyMOL.cartoon_cylindrical_helices) + ";");
    sb.append("set ribbonBorder "
        + getBooleanSetting(PyMOL.cartoon_fancy_helices) + ";");
    //{ command => 'set hermiteLevel -4',                                                       comment => 'so that SS reps have some thickness' },
    //{ command => 'set ribbonAspectRatio 8',                                                   comment => 'degree of W/H ratio, but somehow not tied directly to actual width parameter...' },
    sb.append("background " + getList(settings, PyMOL.bg_rgb).get(2) + ";");
    if (isMovie)
      sb.append("animation mode loop;");
  }

  private float getRotationRadius() {
    Point3f center = Point3f.new3((xyzMax.x + xyzMin.x) / 2,
        (xyzMax.y + xyzMin.y) / 2, (xyzMax.z + xyzMin.z) / 2);
    float d2max = 0;
    Atom[] atoms = atomSetCollection.getAtoms();
    if (isMovie)
      for (int i = lstTrajectories.size(); --i >= 0;) {
        Point3f[] pts = lstTrajectories.get(i);
        for (int j = pts.length; --j >= 0;) {
          Point3f pt = pts[j];
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

  private static float maxRadius(float d2max, float x, float y, float z, Point3f center) {
    float dx = (x - center.x);
    float dy = (y - center.y);
    float dz = (z - center.z);
    float d2 = dx * dx + dy * dy + dz * dz;
    if (d2 > d2max)
      d2max = d2;
    return d2max;
  }

}
