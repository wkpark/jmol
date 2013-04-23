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


import org.jmol.adapter.readers.cifpdb.PdbReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.Structure;
import org.jmol.api.JmolDocument;
import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.shape.Text;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BoxInfo; //import org.jmol.util.Escape;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.Dimension;
import org.jmol.util.Escape;
import org.jmol.util.JmolFont;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JC;

/**
 * experimental PyMOL PSE (binary Python session) file reader Feb 2013 Jmol
 * 13.1.13
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class PyMOLReader extends PdbReader {
  

  private final static int BRANCH_SELECTION = -1;
  private final static int BRANCH_MOLECULE = 1;
  private final static int BRANCH_MAPSURFACE = 2;
  private final static int BRANCH_MAPMESH = 3;
  private final static int BRANCH_MEASURE = 4;
  private final static int BRANCH_CALLBACK = 5;
  private final static int BRANCH_CGO = 6; // compiled graphics object
  private final static int BRANCH_SURFACE = 7;
  private final static int BRANCH_GADGET = 8;
  private final static int BRANCH_CALCULATOR = 9;
  private final static int BRANCH_SLICE = 10;
  private final static int BRANCH_ALIGNMENT = 11;
  private final static int BRANCH_GROUP = 12;
  
  private static final int MIN_RESNO = -1000; // minimum allowed residue number

  private static String nucleic = " A C G T U ADE THY CYT GUA URI DA DC DG DT DU ";


  private boolean usePymolRadii = true;
  private boolean allowSurface = true;
  private boolean doResize = false;

  private JmolList<Object> settings;
  private Map<Integer,JmolList<Object>> localSettings;
  private int atomCount0;
  private int atomCount;
  private int strucNo;
  private boolean isHidden;
  private JmolList<Object> pymolAtoms;
  private BS bsBondedPyMOL = new BS();
  private BS bsBondedJmol = new BS();
  private BS bsHidden = new BS();
  private BS bsNucleic = new BS();
  private BS bsNoSurface = new BS();
  private boolean haveTraceOrBackbone;
  private boolean haveNucleicLadder;
  
  private int[] atomMap;
  private Map<Float, BS> htSpacefill = new Hashtable<Float, BS>();
  private Map<String, int[]> htAtomMap = new Hashtable<String, int[]>();
  private Map<String, BS> ssMapSeq = new Hashtable<String, BS>();
  private Map<String, BS> ssMapAtom = new Hashtable<String, BS>();
  private JmolList<Integer> atomColorList = new  JmolList<Integer>();
  private JmolList<Text> labels = new  JmolList<Text>();

  private JmolList<ModelSettings> modelSettings = new  JmolList<ModelSettings>();
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
    atomSetCollection.setAtomSetAuxiliaryInfo("pdbNoHydrogens", Boolean.TRUE);
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("isPyMOL", Boolean.TRUE);
    super.initializeReader();
  }

  private P3 xyzMin = P3.new3(1e6f, 1e6f, 1e6f);
  private P3 xyzMax = P3.new3(-1e6f, -1e6f, -1e6f);
  private int nModels;
  
  private boolean logging;

  @Override
  public void processBinaryDocument(JmolDocument doc) throws Exception {
    doResize =   checkFilterKey("DORESIZE");
    allowSurface = !checkFilterKey("NOSURFACE");
    
    PickleReader reader = new PickleReader(doc, viewer);
    logging = false;
    Map<String, Object> map = reader.getMap(logging);
    reader = null;
    process(map);
  }

  private BS[] reps = new BS[REP_JMOL_MAX];
  private float cartoonTranslucency;
  private float sphereTranslucency;
  private float stickTranslucency;
  private boolean cartoonLadderMode;
  private boolean  cartoonRockets;
  private boolean solventAsSpheres;
  private int labelFontId;
  
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
  private int pymolVersion;
  
  @SuppressWarnings("unchecked")
  private void process(Map<String, Object> map) {
    pymolVersion = ((Integer) map.get("version")).intValue();
    appendLoadNote("PyMOL version: " + pymolVersion);
    settings = getMapList(map, "settings");
    JmolList<Object> file = getList(settings, PyMOL.session_file);
    if (file != null)
      Logger.info("PyMOL session file: " + file.get(2)); 
    setVersionSettings();
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("settings", settings);
    setUniqueSettings(getMapList(map, "unique_settings"));
    logging = (viewer.getLogFile().length() > 0);
    JmolList<Object> names = getMapList(map, "names");
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String name = e.getKey();
      Logger.info(name);
      if (name.equals("names")) {
        for (int i = 1; i < names.size(); i++)
          Logger.info("  " + getString(getList(names, i), 0));
      }
    }
    if (logging) {
      if (logging)
        viewer.log("$CLEAR$");
      //String s = map.toString();//.replace('=', '\n');
      for (Map.Entry<String, Object> e : map.entrySet()) {
        String name = e.getKey();
        if (!"names".equals(name)) {
          viewer.log("\n===" + name + "===");
          viewer.log(TextFormat.simpleReplace(e.getValue().toString(), "[", "\n["));
        }
      }
      viewer.log("\n===names===");
      for (int i = 1; i < names.size(); i++) {
        viewer.log("");
        JmolList<Object> list = (JmolList<Object>) names.get(i);
        viewer.log(" =" + list.get(0).toString() + "=");
        try {
          viewer.log(TextFormat.simpleReplace(list.toString(), "[", "\n["));
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }
    addColors(getMapList(map, "colors"));
    allStates = getBooleanSetting(PyMOL.all_states);
    pymolFrame = (int) getFloatSetting(PyMOL.frame);
    JmolList<Object> mov = getMapList(map, "movie");
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
        appendLoadNote("PyMOL movie frameCount = " + frameCount);
      }
    }
    if (!isStateScript && doResize) {
      try {
        width = getInt(getMapList(map, "main"), 0);
        height = getInt(getMapList(map, "main"), 1);
      } catch (Exception e) {
        // ignore
      }
      String note;
      if (width > 0 && height > 0) {
        note = "PyMOL dimensions width=" + width + " height=" + height;
        atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
            "perferredWidthHeight", new int[] { width, height });
        Dimension d = viewer.resizeInnerPanel(width, height);
        width = d.width;
        height = d.height;
        //viewer.setScreenDimension(width, height);
      } else {
        note = "PyMOL dimensions?";
      }
      appendLoadNote(note);
    }
    totalAtomCount = getTotalAtomCount(names);
    Logger.info("PyMOL total atom count = " + totalAtomCount);
    getPoint((JmolList<Object>) getList(settings, PyMOL.label_position).get(2), 0, labelPosition0);
    selections = new JmolList<JmolList<Object>>();    
    for (int i = 1; i < names.size(); i++)
      processBranch(getList(names, i));
    processSelections();
    proecssMeshes();
    if (isMovie) {
      appendLoadNote("PyMOL trajectories read: " + lstTrajectories.size());
      atomSetCollection.finalizeTrajectoryAs(lstTrajectories, null);
    }

    // we are done if this is a state script

    setDefinitions();
    setRendering(getMapList(map, "view"));
  }

  /**
   * add a 
   */
  @SuppressWarnings("unchecked")
  private void proecssMeshes() {
    if (meshes == null)
      return;
    for (int i = meshes.size(); --i >= 0;) {
      JmolList<Object> mesh = meshes.get(i);
      String surfaceName = getString((JmolList<Object>)getList(mesh, 2).get(0), 1);
      JmolList<Object> surface = surfaces.get(surfaceName);
      if (surface == null)
        continue;
      String meshName = mesh.get(mesh.size() - 1).toString();
      mesh.addLast(surfaceName);
      appendLoadNote("PyMOL mesh " + meshName + " references surface " + surfaceName);
      surfaces.put(meshName, mesh);
      surfaces.put("__pymolSurfaceData__", mesh);
      atomSetCollection.setAtomSetAuxiliaryInfo("jmolSurfaceInfo", surfaces);        
      ModelSettings ms = new ModelSettings(T.mesh, null, mesh);
      modelSettings.addLast(ms);
      ms.setSize(getFloatSetting(PyMOL.mesh_width));
      ms.argb = PyMOL.getRGB(getInt(getList(mesh, 0), 2));
    }
  }

  private void setVersionSettings() {
    if (pymolVersion < 100) {
      addSetting(PyMOL.movie_fps, 2, Integer.valueOf(0));
      addSetting(PyMOL.label_digits, 2, Integer.valueOf(2));
      addSetting(PyMOL.label_position, 4, new double[] { 1, 1, 0 });
      if (pymolVersion < 99) {
        addSetting(PyMOL.cartoon_ladder_mode, 2, Integer.valueOf(0));
        addSetting(PyMOL.cartoon_tube_cap, 2, Integer.valueOf(0));
        addSetting(PyMOL.cartoon_nucleic_acid_mode, 2, Integer.valueOf(1));
      }
    }
  }

  private void addSetting(int key, int type, Object val) {
    int settingCount = settings.size();
    if (settingCount <= key)
    for (int i = key + 1; --i >= settingCount;)
      settings.addLast(null);
    if (type == 4) {
      double[] d = (double[]) val;
      JmolList<Object> list;
      val = list = new JmolList<Object>();
      for (int i = 0; i < 3; i++)
        list.addLast(Double.valueOf(d[i]));
    }    
    JmolList<Object> setting = new JmolList<Object>();
    setting.addLast(Integer.valueOf(key));
    setting.addLast(Integer.valueOf(type));
    setting.addLast(val);    
    settings.set(key, setting);
  }

  private void setDefinitions() {
    modelSettings.addLast(new ModelSettings(T.define, null, htNames));
    appendLoadNote(viewer.getAtomDefs(htNames));
  }

  private int getTotalAtomCount(JmolList<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      JmolList<Object> branch = getList(names, i);
      int type = getBranchType(branch);
      if (type == BRANCH_MOLECULE && checkBranch(branch)) {//, type, true)) {
        JmolList<Object> deepBranch = getList(branch, 5);
        if (isMovie) {
          n += getBranchAtoms(deepBranch).size();
        } else {
          JmolList<Object> states = getList(deepBranch, 4);
          int ns = states.size();
          for (int j = 0; j < ns; j++) {
            JmolList<Object> state = getList(states, j);
            JmolList<Object> idxToAtm = getList(state, 3);
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

  static float getFloatAt(JmolList<Object> list, int i) {
    return (list == null ? 0 : ((Number) list.get(i)).floatValue());
  }

  static P3 getPoint(JmolList<Object> list, int i, P3 pt) {
    pt.set(getFloatAt(list, i++), getFloatAt(list, i++), getFloatAt(list, i));
    return pt;
  }

  @SuppressWarnings("unchecked")
  static JmolList<Object> getList(JmolList<Object> list, int i) {
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
    float v = 0;
    try {
      JmolList<Object> setting = null;
      if (localSettings != null)
        setting = localSettings.get(Integer.valueOf(i));
      if (setting == null)
        setting = getList(settings, i);
      if (settings == null)
        return 0;
      v = ((Number) setting.get(2)).floatValue();
      Logger.info("Pymol setting " + i + " = " + v);
    } catch (Exception e) {
      Logger.info("PyMOL " + pymolVersion
          + " does not have setting " + i);
    }
    return v;
  }

  private String branchName;
  private BS bsModelAtoms = BS.newN(1000);
  private int branchID;
  private float nonBondedSize;
  private float sphereScale;
  private JmolList<JmolList<Object>> selections;
  private Hashtable<Integer, JmolList<Object>> uniqueSettings;
  private P3 labelPosition;
  private float labelColor, labelSize;
  private P3 labelPosition0 = new P3();
  private Hashtable<String, JmolList<Object>> surfaces;
  private JmolList<JmolList<Object>> meshes;
  
  private void processBranch(JmolList<Object> branch) {
    int type = getBranchType(branch);
    if (!checkBranch(branch))//, type, false))
      return;
    Logger.info("PyMOL model " + (nModels + 1) + " Branch " + branchName
        + (isHidden ? " (hidden)" : " (visible)"));
    JmolList<Object> deepBranch = getList(branch, 5);
    branchID = 0;
    String msg = "" + type;
    switch (type) {
    case BRANCH_SELECTION:
      selections.addLast(branch);
      return;
    case BRANCH_MOLECULE:
      processBranchModels(deepBranch);
      return;
    case BRANCH_MEASURE:
      processBranchMeasure(deepBranch);
      return;
    case BRANCH_MAPMESH:
      processMap(deepBranch, true);
      return;
    case BRANCH_MAPSURFACE:
      processMap(deepBranch, false);
      return;
    case BRANCH_ALIGNMENT:
      msg = "ALIGNEMENT";
      break;
    case BRANCH_CALCULATOR:
      msg = "CALCULATOR";
      break;
    case BRANCH_CALLBACK:
      msg = "CALLBACK";
      break;
    case BRANCH_CGO:
      msg = "CGO";
      break;
    case BRANCH_GADGET:
      msg = "GADGET";
      break;
    case BRANCH_GROUP:
      msg = "GROUP";
      break;
    case BRANCH_SLICE:
      msg = "SLICE";
      break;
    case BRANCH_SURFACE:
      msg = "SURFACE";
      break;
    }
    Logger.error("Unprocessed branch type " + msg);
  }

  private void processMap(JmolList<Object> deepBranch, boolean isMesh) {
    if (isMesh) {
      if (isHidden)
        return; // for now
      if (meshes == null)
        meshes = new JmolList<JmolList<Object>>();
      meshes.addLast(deepBranch);
    } else {
      if (surfaces == null)
        surfaces = new Hashtable<String, JmolList<Object>>();
      surfaces.put(branchName, deepBranch);
    }
    deepBranch.addLast(branchName);
  }

  private void processSelections() {
    for (int i = selections.size(); --i >= 0;) {      
      JmolList<Object> branch = selections.get(i);
      checkBranch(branch);//, BRANCH_SELECTION, false);
      processBranchSelection(getList(branch, 5));
    }
  }

  //  [Ca, 1, 0, 
  //   [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], -1, 
  //   [
  //   [H115W, 
  //   [2529, 2707], 
  //   [1, 1]]], ]

  private void processBranchSelection(JmolList<Object> deepBranch) {
    BS bs = new BS();
    for (int j = deepBranch.size(); --j >= 0;) {
      JmolList<Object> data = getList(deepBranch, j);
      String parent = getString(data, 0);
      atomMap = htAtomMap.get(parent);
      if (atomMap == null)
        continue;
      JmolList<Object> atoms = getList(data, 1);
      for (int i = atoms.size(); --i >= 0;) {
        int ia = atomMap[getInt(atoms, i)];
        if (ia >= 0)
          bs.set(ia);
      }
    }
    if (!bs.isEmpty())
      addName(branchName, bs);
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
    //if (!branchName.equals("aricept_docked")) return  false;
    isHidden = (getInt(branch, 2) != 1);
    return (branchName.indexOf("_") != 0);
  }

  private void processBranchMeasure(JmolList<Object> deepBranch) {
    if (isHidden)
      return; // will have to reconsider this if there is a movie, though
    Logger.info("PyMOL measure " + branchName);
    JmolList<Object> measure = getList(getList(deepBranch, 2), 0);

    int color = getInt(getList(deepBranch, 0), 2);
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
    
    JmolList<Object> branchInfo = getList(deepBranch, 0);
    setLocalSettings(getList(branchInfo, 8));
    if (isMovie) {
    } else {
      processCryst(getList(deepBranch, 10));
    }
    atomCount = atomCount0 = atomSetCollection.getAtomCount();
    atomMap = new int[getInt(deepBranch, 3)];
    htAtomMap.put(branchName, atomMap);
    JmolList<Object> states = getList(deepBranch, 4);
    JmolList<Bond> bonds = processBonds(getList(deepBranch, 6));
    pymolAtoms = getBranchAtoms(deepBranch);
    int ns = states.size();
    if (ns > 1)
      Logger.info(ns + " PyMOL states");
    if (ns == 1)
      allStates = true;
    BS bsState = null;
    BS bsAtoms = BS.newN(atomCount0 + pymolAtoms.size());
    addName(branchName, bsAtoms);
    for (int i = 0; i < REP_JMOL_MAX; i++)
      reps[i] = BS.newN(1000);
    Logger.info("PyMOL molecule " + branchName);
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
        if (idxToAtm == null) {
          Logger.error("movie error: no idxToAtm");
          continue;
        }
        for (int j = idxToAtm.size(); --j >= 0;)
          bsState.set(getInt(idxToAtm, j));
      }
      for (int i = bsState.nextSetBit(0); i >= 0; i = bsState.nextSetBit(i + 1))
        if (!addAtom(pymolAtoms, i, -1, null, bsAtoms))
          bsState.clear(i);
      for (int i = 0; i < ns; i++) {
        JmolList<Object> state = getList(states, i);
        JmolList<Object> coords = getList(state, 2);
        JmolList<Object> idxToAtm = getList(state, 3);
        if (idxToAtm == null)
          continue;
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
          BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
        }
      }
      processStructures();
      setBranchShapes();
    } else {
      allStates |= (ns > 1); // testing
      ns = 1; // I guess I don't understand what the second set is for in each of these.
      lstStates.clear();
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
        model(++nModels);
        for (int idx = 0; idx < n; idx++)
          addAtom(pymolAtoms, getInt(idxToAtm, idx), idx, coords, bsState);
        if (bsState != null) {
          bsAtoms.or(bsState);
        }
        processStructures();
        setBranchShapes();
      }
    }
    setBonds(bonds);
    
    Logger.info("reading " + (atomCount - atomCount0) + " atoms");
    dumpBranch();
  }

  private void setAtomReps(int iAtom, int atomColor) {
    PyMOLAtom atom = (PyMOLAtom) atomSetCollection.getAtom(iAtom);

    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (atom.bsReps.get(i))
        reps[i].set(iAtom);
    if (reps[PyMOL.REP_LABELS].get(iAtom)) {
      if (atom.label.equals(" "))
        reps[PyMOL.REP_LABELS].clear(iAtom);
      else {
        int icolor = (int) getUniqueFloat(atom.uniqueID, PyMOL.label_color, labelColor);
        if (icolor < 0)
          icolor = atomColor;
        P3 offset = getUniquePoint(atom.uniqueID, PyMOL.label_position, null);
        if (offset == null)
          offset = labelPosition;
        else 
          offset.add(labelPosition);
        labels.addLast(newTextLabel(atom.label, offset, 
            getColix(icolor, 0),
            (int) getUniqueFloat(atom.uniqueID, PyMOL.label_font_id, labelFontId),
            getUniqueFloat(atom.uniqueID, PyMOL.label_size, labelSize)));
      }
    }
    boolean isSphere = reps[PyMOL.REP_SPHERES].get(iAtom);
    if (!isSphere && !solventAsSpheres && reps[PyMOL.REP_NONBONDED].get(iAtom)
        && !atom.bonded) {
      reps[PyMOL.REP_NBSPHERES].clear(iAtom);
      //reps[PyMOL.REP_SPHERES].clear(iAtom);
      reps[PyMOL.REP_NONBONDED].clear(iAtom);
      reps[REP_JMOL_STARS].set(iAtom);
    }
    float rad = 0;
    if (isSphere) {
      float mySphereSize = getUniqueFloat(atom.uniqueID, PyMOL.sphere_scale,
          sphereScale);
      // nl1_nl2 -- stumped!
      rad = atom.radius * mySphereSize;
    } else if (reps[PyMOL.REP_NONBONDED].get(iAtom)
        || reps[PyMOL.REP_NBSPHERES].get(iAtom)) {
      // Penta_vs_mutants calcium
      float myNonBondedSize = getUniqueFloat(atom.uniqueID,
          PyMOL.nonbonded_size, nonBondedSize);
      rad = -atom.radius * myNonBondedSize;
    }
    if (!usePymolRadii)
      atom.radius = Float.NaN; // sorry, can't use these for surfaces
    if (rad != 0)
      addSpacefill(iAtom, rad);
    if (reps[PyMOL.REP_CARTOON].get(iAtom)) {
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
      switch (atom.cartoonType) {
      case -1:
        reps[PyMOL.REP_CARTOON].clear(iAtom);
        break;
      case 1:
        reps[REP_JMOL_TRACE].set(iAtom);
        break;
      case 4:
        reps[REP_JMOL_TRACE].set(iAtom);
        break;
      case 7:
        reps[PyMOL.REP_CARTOON].clear(iAtom);
        reps[REP_JMOL_PUTTY].set(iAtom);
        break;
      }
    }
  }
  
  private Text newTextLabel(String label, P3 labelOffset, short colix,
                            int fontID, float fontSize) {
    // 0 GLUT 8x13 
    // 1 GLUT 9x15 
    // 2 GLUT Helvetica10 
    // 3 GLUT Helvetica12 
    // 4 GLUT Helvetica18 
    // 5 DejaVuSans
    // 6 DejaVuSans_Oblique
    // 7 DejaVuSans_Bold
    // 8 DejaVuSans_BoldOblique
    // 9 DejaVuSerif
    // 10 DejaVuSerif_Bold
    // 11 DejaVuSansMono
    // 12 DejaVuSansMono_Oblique
    // 13 DejaVuSansMono_Bold
    // 14 DejaVuSansMono_BoldOblique
    // 15 GenR102
    // 16 GenI102
    // 17 DejaVuSerif_Oblique
    // 18 DejaVuSerif_BoldOblique

    String face;
    float factor = 1f;
    switch (fontID) {
    default:
    case 11:
    case 12:
    case 13:
    case 14:
      // 11-14: Jmol doesn't support sansserif mono -- just using SansSerif here
      face = "SansSerif";
      break;
    case 0:
    case 1:
      face = "Monospaced";
      break;
    case 9:
    case 10:
    case 15:
    case 16:
    case 17:
    case 18:
      face = "Serif";
      break;
    }
    String style;
    switch (fontID) {
    default:
      style = "Plain";
      break;
    case 6:
    case 12:
    case 16:
    case 17:
      style = "Italic";
      break;
    case 7:
    case 10:
    case 13:
      style = "Bold";
      break;
    case 8:
    case 14:
    case 18:
      style = "BoldItalic";
      break;
    }
    JmolFont font = viewer.getFont3D(face, style, fontSize == 0 ? 12 : fontSize * factor);
    Text t = Text.newLabel(viewer.getGraphicsData(), font, label, colix,
        (short) 0, 0, 0, 0, 0, 0, 0, labelOffset);
    return t;
  }

  private void setBonds(JmolList<Bond> bonds) {
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      Bond bond = bonds.get(i);
      bond.atomIndex1 = atomMap[bond.atomIndex1];
      bond.atomIndex2 = atomMap[bond.atomIndex2];
      if (bond.atomIndex1 >= 0 && bond.atomIndex2 >= 0)
        atomSetCollection.addBond(bond);
    }
  }

  @SuppressWarnings("unchecked")
  private void setLocalSettings(JmolList<Object> list) {
    localSettings = new Hashtable<Integer, JmolList<Object>>();
    if (list != null && list.size() != 0) {
      Logger.info(list.toString());
      for (int i = list.size(); --i >= 0;) {
        JmolList<Object> setting = (JmolList<Object>) list.get(i);
        localSettings.put((Integer) setting.get(0), setting);
      }
    }
    nonBondedSize = getFloatSetting(PyMOL.nonbonded_size);
    sphereScale = getFloatSetting(PyMOL.sphere_scale);
    valence = getBooleanSetting(PyMOL.valence);
    cartoonTranslucency = getFloatSetting(PyMOL.cartoon_transparency);
    stickTranslucency = getFloatSetting(PyMOL.stick_transparency);
    sphereTranslucency = getFloatSetting(PyMOL.sphere_transparency);
    cartoonLadderMode = getBooleanSetting(PyMOL.cartoon_ladder_mode);
    cartoonRockets = getBooleanSetting(PyMOL.cartoon_cylindrical_helices);
    solventAsSpheres = getBooleanSetting(PyMOL.sphere_solvent);
    labelPosition = new P3();
    try {
      JmolList<Object> setting = localSettings.get(Integer.valueOf(PyMOL.label_position));      
      getPoint((JmolList) setting.get(2), 0, labelPosition);
    } catch (Exception e) {
      // no problem.
    }
    labelPosition.add(labelPosition0);
    labelColor = getFloatSetting(PyMOL.label_color);
    labelSize = getFloatSetting(PyMOL.label_size);
    labelFontId = (int) getFloatSetting(PyMOL.label_font_id);
  }

  @SuppressWarnings("unchecked")
  private void setUniqueSettings(JmolList<Object> list) {
    uniqueSettings = new Hashtable<Integer, JmolList<Object>>();
    if (list != null && list.size() != 0) {
      for (int i = list.size(); --i >= 0;) {
        JmolList<Object> atomSettings = (JmolList<Object>) list.get(i);
        int id = getInt(atomSettings, 0);
        JmolList<Object> mySettings = (JmolList<Object>) atomSettings.get(1);
        for (int j = mySettings.size(); --j >= 0;) {
          JmolList<Object> setting = (JmolList<Object>) mySettings.get(j);
          int uid = id * 1000 + getInt(setting, 0);
          uniqueSettings.put(Integer.valueOf(uid), setting);
          Logger.info("PyMOL unique setting " + id + " " + setting);
        }
      }
    }
  }

  private float getUniqueFloat(int id, int key, float defaultValue) {
    JmolList<Object> setting;
    if (id < 0
        || (setting = uniqueSettings.get(Integer.valueOf(id * 1000 + key))) == null)
      return defaultValue;
    float v = ((Number) setting.get(2)).floatValue();
    Logger.info("Pymol unique setting for " + id + ": " + key + " = " + v);
    return v;
  }
  
  @SuppressWarnings("unchecked")
  private P3 getUniquePoint(int id, int key, P3 pt) {
    JmolList<Object> setting;
    if (id < 0
        || (setting = uniqueSettings.get(Integer.valueOf(id * 1000 + key))) == null)
      return pt;
    pt = new P3();
    getPoint((JmolList) setting.get(2), 0, pt);
    Logger.info("Pymol unique setting for " + id + ": " + key + " = " + pt);
    return pt;
  }  

  private void addName(String name, BS bs) {
    htNames.put(fixName(name), bs);
  }

  private static String fixName(String name) {
    char[] chars = name.toLowerCase().toCharArray();
    for (int i = chars.length; --i >= 0;)
      if (!Character.isLetterOrDigit(chars[i]))
        chars[i] = '_';
    return "__" + String.valueOf(chars);
  }

  private static int getBranchType(JmolList<Object> branch) {
    return getInt(branch, 4);
  }

  private static JmolList<Object> getBranchAtoms(JmolList<Object> deepBranch) {
    return getList(deepBranch, 7);  
  }

  @Override
  protected void model(int modelNumber) {
    bsModelAtoms.clearAll();
    super.model(modelNumber);
  }

  // [0] Int        resv
  // [1] String     chain
  // [2] String     alt
  // [3] String     resi
  // [4] String     segi
  // [5] String     resn
  // [6] String     name
  // [7] String     elem
  // [8] String     textType
  // [9] String     label
  // [10] String    ssType
  // [11] Int       hydrogen
  // [12] Int       customType
  // [13] Int       priority
  // [14] Float     b-factor
  // [15] Float     occupancy
  // [16] Float     vdw
  // [17] Float     partialCharge
  // [18] Int       formalCharge
  // [19] Int       hetatm
  // [20] List      reps
  // [21] Int       color pointer
  // [22] Int       id
  // [23] Int       cartoon type
  // [24] Int       flags
  // [25] Int       bonded
  // [26] Int       chemFlag
  // [27] Int       geom
  // [28] Int       valence
  // [29] Int       masked
  // [30] Int       protekted
  // [31] Int       protons
  // [32] Int       unique_id
  // [33] Int       stereo
  // [34] Int       discrete_state
  // [35] Float     elec_radius
  // [36] Int       rank
  // [37] Int       hb_donor
  // [38] Int       hb_acceptor
  // [39] Int       atomic_color
  // [40] Int       has_setting
  // [41] Float     U11
  // [42] Float     U22
  // [43] Float     U33
  // [44] Float     U12
  // [45] Float     U13
  // [46] Float     U23

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
    String sym = getString(a, 7);
    if (sym.equals("A"))
      sym = "C";
    boolean isHetero = (getInt(a, 19) != 0);
    PyMOLAtom atom = (PyMOLAtom) processAtom(new PyMOLAtom(), name, altLoc
        .charAt(0), group3, chainID.charAt(0), seqNo, insCode.charAt(0),
        isHetero, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return false;
    boolean isNucleic = (nucleic.indexOf(group3) >= 0);
    if (isNucleic)
      bsNucleic.set(atomCount);
    atom.label = getString(a, 9);
    String ss = getString(a, 10);
    if (seqNo >= MIN_RESNO && (!ss.equals(" ") || name.equals("CA") || isNucleic)) {
      if (ssMapAtom.get(ss) == null)
        ssMapAtom.put(ss, new BS());
      BS bs = ssMapSeq.get(ss);
      if (bs == null)
        ssMapSeq.put(ss, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
      ss += chainID;
      bs = ssMapSeq.get(ss);
      if (bs == null)
        ssMapSeq.put(ss, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
    }
    atom.bfactor = getFloatAt(a, 14);
    atom.occupancy = (int) (getFloatAt(a, 15) * 100);
    atom.radius = getFloatAt(a, 16);
    atom.partialCharge = getFloatAt(a, 17);
    int formalCharge = getInt(a, 18);
    atom.bsReps = getBsReps(getList(a, 20));
    int serNo = getInt(a, 22);
    atom.cartoonType = getInt(a, 23);
    atom.flags = getInt(a, 24);
    atom.bonded = getInt(a, 25) != 0;
    if (a.size() > 40 && getInt(a, 40) == 1)
      atom.uniqueID = getInt(a, 32);
    if ((atom.flags & PyMOL.FLAG_NOSURFACE) != 0)
      bsNoSurface.set(atomCount);
    float translucency = getUniqueFloat(atom.uniqueID,
        PyMOL.sphere_transparency, sphereTranslucency);
    int atomColor = getInt(a, 21);
    atomColorList.addLast(Integer.valueOf(getColix(atomColor, translucency)));
    bsHidden.setBitTo(atomCount, isHidden);
    bsModelAtoms.set(atomCount);
    if (bsState != null)
      bsState.set(atomCount);
    int cpt = icoord * 3;
    float x = getFloatAt(coords, cpt);
    float y = getFloatAt(coords, ++cpt);
    float z = getFloatAt(coords, ++cpt);
    BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    processAtom2(atom, serNo, x, y, z, formalCharge);
    if (a.size() > 46) {
      float[] data = new float[7];
      for (int i = 0; i < 6; i++)
        data[i] = getFloatAt(a, i + 41);
      atomSetCollection.setAnisoBorU(atom, data, 12);
    }
    setAtomReps(atomCount, atomColor);
    atomMap[apt] = atomCount++;
    return true;
  }

  private short getColix(int colorIndex, float translucency) {
    return C.getColixTranslucent3(C.getColixO(Integer.valueOf(PyMOL
        .getRGB(colorIndex))), translucency > 0, translucency);
  }

  private BS getBsReps(JmolList<Object> list) {
    BS bsReps = new BS();
    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (getInt(list, i) == 1)
        bsReps.set(i);
    return bsReps;
  }

  private void dumpBranch() {
    Logger.info("----------");
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
  }

  private void processStructures() {
    if (atomSetCollection.bsStructuredModels == null)
      atomSetCollection.bsStructuredModels = new BS();
    atomSetCollection.bsStructuredModels.set(Math.max(atomSetCollection
        .getCurrentAtomSetIndex(), 0));

    processSS("H", ssMapAtom.get("H"), EnumStructure.HELIX, 0);
    processSS("S", ssMapAtom.get("S"), EnumStructure.SHEET, 1);
    processSS("L", ssMapAtom.get("L"), EnumStructure.TURN, 0);
    processSS(" ", ssMapAtom.get(" "), EnumStructure.NONE, 0);
    ssMapSeq = new Hashtable<String, BS>();
  }

  private void processSS(String ssType, BS bsAtom,
                         EnumStructure type, int strandCount) {
    if (ssMapSeq.get(ssType) == null)
      return;
    int istart = -1;
    int iend = -1;
    char ichain = '\0';
    Atom[] atoms = atomSetCollection.getAtoms();
    BS bsSeq = null;
    int n = atomCount + 1;
    int seqNo = -1;
    char thischain = '\0';
    int imodel = -1;
    int thismodel = -1; 
    for (int i = atomCount0; i < n; i++) {
      if (i == atomCount) {
        thischain = '\0';
      } else {
        seqNo = atoms[i].sequenceNumber;
        thischain = atoms[i].chainID;
        thismodel = atoms[i].atomSetIndex;
      }
      if (thischain != ichain || thismodel != imodel) {
        ichain = thischain;
        imodel = thismodel;
        bsSeq = ssMapSeq.get(ssType + thischain);
        --i; // replay this one
        if (istart < 0)
          continue;
      } else if (bsSeq != null && seqNo >= MIN_RESNO
          && bsSeq.get(seqNo - MIN_RESNO)) {
        iend = i;
        if (istart < 0)
          istart = i;
        continue;
      } else if (istart < 0) {
        continue;
      }
      if (type != EnumStructure.NONE) {
        Structure structure = new Structure(imodel, type, type, type
            .toString(), ++strucNo, strandCount);
        Atom a = atoms[istart];
        Atom b = atoms[iend];
        structure.set(a.chainID, a.sequenceNumber, a.insertionCode, b.chainID,
            b.sequenceNumber, b.insertionCode);
        atomSetCollection.addStructure(structure);
      }
      bsAtom.setBits(istart, iend + 1);
      istart = -1;
    }
  }

  private JmolList<Bond>  processBonds(JmolList<Object> bonds) {
    JmolList<Bond> bondList = new JmolList<Bond>();
    bsBondedPyMOL.clear(totalAtomCount); // sets length
    int color = (int) getFloatSetting(PyMOL.stick_color);
    float radius = getFloatSetting(PyMOL.stick_radius) / 2;
    float translucency = getFloatSetting(PyMOL.stick_transparency);
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      JmolList<Object> b = getList(bonds, i);
      int order = (valence ? getInt(b, 2) : 1);
      if (order < 1 || order > 3)
        order = 1;
      // TODO: hydrogen bonds?
      int ia = getInt(b, 0);
      int ib = getInt(b, 1);
      bsBondedPyMOL.set(ia);
      bsBondedPyMOL.set(ib);
      Bond bond = new Bond(ia, ib, order);
      bondList.addLast(bond);
      int c;
      float rad, t;
      boolean hasID = (b.size() > 6 && getInt(b, 6) != 0);
      if (hasID) {
        int id = getInt(b, 5);
        rad = getUniqueFloat(id, PyMOL.stick_radius, radius) / 2;
        c = (int) getUniqueFloat(id, PyMOL.stick_color, color);
        t = getUniqueFloat(id, PyMOL.stick_transparency, translucency);
      } else {
        rad = radius;
        c = color;
        t = translucency;
      }
      bond.radius = rad;      
      if (c >= 0)
        bond.colix = C.getColixTranslucent3(C.getColix(PyMOL.getRGB(c)), t > 0, t);
    }
    return bondList;
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
    setFrame();
    if (!bsHidden.isEmpty())
      modelSettings.addLast(new ModelSettings(T.hidden, bsHidden, null));
    addJmolScript(sb.toString());
  }

  private void setJmolDefaults() {
    viewer.setBooleanProperty("navigationMode", false);
    viewer.setBooleanProperty("zoomLarge", false);
    viewer.setBooleanProperty("ssBondsBackbone", false);
    viewer.setStringProperty("measurementUnits", "ANGSTROMS");
  }

  private final static int REP_JMOL_MIN = 13;
  private final static int REP_JMOL_TRACE = 13;
  private final static int REP_JMOL_PUTTY = 14;
  private final static int REP_JMOL_STARS = 15;
  private final static int REP_JMOL_MAX = 16;

  private void setBranchShapes() {
    if (isStateScript)
      return;
    BS bs = BSUtil.newBitSet2(atomCount0, atomCount);
    ModelSettings ms = new ModelSettings(JC.SHAPE_BALLS, bs, null);
    colixes = setColors(colixes, atomColorList);
    ms.setSize(0);
    ms.setColors(colixes, 0);
    modelSettings.addLast(ms);
    ms = new ModelSettings(JC.SHAPE_STICKS, bs, null);
    ms.setSize(0);
    modelSettings.addLast(ms);
    setSpacefill();
    cleanSingletonCartoons(reps[PyMOL.REP_CARTOON]);
    reps[REP_JMOL_TRACE].and(reps[PyMOL.REP_CARTOON]);
    reps[PyMOL.REP_CARTOON].andNot(reps[REP_JMOL_TRACE]);
    //reps[REP_JMOL_PUTTY].and(reps[REP_CARTOON]);
    // reps[REP_CARTOON].andNot(reps[REP_JMOL_PUTTY]);
    for (int i = 0; i < REP_JMOL_MAX; i++)
      setShape(i);
    setSurface();
    ssMapAtom = new Hashtable<String, BS>();
  }
  
  private short[] setColors(short[] colixes,
                            JmolList<Integer> colorList) {
    if (colixes == null) 
      colixes = new short[atomCount];
    else
      colixes = ArrayUtil.ensureLengthShort(colixes, atomCount);
    for (int i = atomCount; --i >= atomCount0;)
      colixes[i] = (short) colorList.get(i).intValue();
    return colixes;
  }

  private void setShape(int shapeID) {
    // add more to implement
    BS bs = reps[shapeID];
    float f;
    switch (shapeID) {
    case PyMOL.REP_NONBONDED:
    case PyMOL.REP_NBSPHERES:
      break;
    case PyMOL.REP_LINES:
      bs.andNot(reps[PyMOL.REP_STICKS]);
      break;
    }
    if (bs.isEmpty())
      return;
    ModelSettings ss = null;
    switch (shapeID) {
    case REP_JMOL_STARS:
      ss = new ModelSettings(JC.SHAPE_STARS, bs, null);
      ss.rd = new RadiusData(null, getFloatSetting(PyMOL.nonbonded_size)/2, RadiusData.EnumType.FACTOR,
          EnumVdw.AUTO);
      ss.setColors(colixes, 0);
      modelSettings.addLast(ss);
      break;
    case PyMOL.REP_NONBONDED:
      ss = new ModelSettings(JC.SHAPE_BALLS, bs, null);
      ss.setColors(colixes, 0);
      ss.translucency = sphereTranslucency;
      modelSettings.addLast(ss);
      break;
    case PyMOL.REP_NBSPHERES:
    case PyMOL.REP_SPHERES:
      ss = new ModelSettings(JC.SHAPE_BALLS, bs, null);
      ss.setColors(colixes, 0);
      ss.translucency = sphereTranslucency;
      modelSettings.addLast(ss);
      break;
    case PyMOL.REP_STICKS:
      f = getFloatSetting(PyMOL.stick_radius) * 2;
      ss = new ModelSettings(JC.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      ss.translucency = stickTranslucency;
      modelSettings.addLast(ss);
      break;
    case PyMOL.REP_DOTS: //   = 9;
      ss = new ModelSettings(JC.SHAPE_DOTS, bs, null);
      f = getFloatSetting(PyMOL.sphere_scale);
      ss.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      modelSettings.addLast(ss);
      break;
    case PyMOL.REP_LINES:
      f = getFloatSetting(PyMOL.line_width) / 15;
      ss = new ModelSettings(JC.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      modelSettings.addLast(ss);
      break;
    case PyMOL.REP_CARTOON:
      if (cartoonRockets)
        setCartoon("H", PyMOL.cartoon_helix_radius, 2);
      else
        setCartoon("H", PyMOL.cartoon_oval_length, 2);
      setCartoon("S", PyMOL.cartoon_rect_length, 2);
      setCartoon("L", PyMOL.cartoon_loop_radius, 2);
      setCartoon(" ", PyMOL.cartoon_loop_radius, 2);
      break;
    case PyMOL.REP_MESH: //   = 8;
    case PyMOL.REP_SURFACE: //   = 2;
      // must be done for each model
      break;
    case PyMOL.REP_LABELS: //   = 3;
      JmolList<Text> myLabels = new  JmolList<Text>();
      for (int i = 0; i < labels.size(); i++)
        myLabels.addLast(labels.get(i));
      labels.clear();
      ss = new ModelSettings(JC.SHAPE_LABELS, bs, myLabels);
      modelSettings.addLast(ss);
      break;
    case REP_JMOL_PUTTY:
      setPutty(bs);
      break;
    case REP_JMOL_TRACE:
      haveTraceOrBackbone = true;
      setTrace(bs);
      break;
    case PyMOL.REP_BACKBONE: //   = 6; // ribbon
      haveTraceOrBackbone = true;
      setRibbon(bs);
      break;
    case PyMOL.REP_DASHES: //   = 10;
      // backbone dashes? Maybe an old setting
      break;
    default:
      if (shapeID < REP_JMOL_MIN)
        Logger.error("Unprocessed representation type " + shapeID);
    }
  }

  private void addSpacefill(int i, float rad) {
    Float r = Float.valueOf(rad);
    BS bsr = htSpacefill.get(r);
    if (bsr == null)
      htSpacefill.put(r, bsr = new BS());
    bsr.set(i);
  }

  private void setSpacefill() {
    for (int i = bsBondedPyMOL.nextSetBit(0); i >= 0; i = bsBondedPyMOL.nextSetBit(i + 1)) {
      if (i >= atomMap.length)
       break;
      int pt = atomMap[i]; 
      if (pt >= 0)
        bsBondedJmol.set(pt);
    }
    for (Map.Entry<Float, BS> e : htSpacefill.entrySet()) {
      float r = e.getKey().floatValue();
      BS bs = e.getValue();
      if (r < 0) {
        bs.andNot(bsBondedJmol);
        r = -r;
      }
      if (bs.isEmpty())
        continue;
      ModelSettings ss = new ModelSettings(JC.SHAPE_BALLS, bs, null);
      ss.rd = new RadiusData(null, r, RadiusData.EnumType.ABSOLUTE,
          EnumVdw.AUTO);
      modelSettings.addLast(ss);
    }
    htSpacefill.clear();
  }

  private void setSurface() {
    if (!allowSurface || isStateScript || bsModelAtoms.isEmpty())
      return;
    if (isHidden)
      return; // will have to reconsider this if there is a movie, though
    BS bs = reps[PyMOL.REP_SURFACE];
    BSUtil.andNot(bs, bsNoSurface);
    if (!bs.isEmpty()) {
      ModelSettings ss = new ModelSettings(JC.SHAPE_ISOSURFACE, bs,
          new String[] {
              branchName + "_" + branchID,
              getBooleanSetting(PyMOL.two_sided_lighting) ? "FULLYLIT"
                  : "FRONTLIT" });
      ss.setSize(getFloatSetting(PyMOL.solvent_radius));
      ss.translucency = getFloatSetting(PyMOL.transparency);
      ss.setColors(colixes, 0);
      modelSettings.addLast(ss);
    }
    bs = reps[PyMOL.REP_MESH];
    BSUtil.andNot(bs, bsNoSurface);
    if (!bs.isEmpty()) {
      ModelSettings ss = new ModelSettings(JC.SHAPE_ISOSURFACE, bs,
          new String[] { branchName + "_" + branchID, null });
      ss.setSize(getFloatSetting(PyMOL.solvent_radius));
      ss.translucency = getFloatSetting(PyMOL.transparency);
      ss.setColors(colixes, 0);
      modelSettings.addLast(ss);
    }
  }

  private void setTrace(BS bs) {
    ModelSettings ss;
    BS bsNuc = BSUtil.copy(bsNucleic);
    bsNuc.and(bs);
    if (!bsNuc.isEmpty() && cartoonLadderMode) {
      haveNucleicLadder = true;
      // we will just use cartoons for ladder mode
      ss = new ModelSettings(JC.SHAPE_CARTOON, bsNuc, null);
      ss.setColors(colixes, cartoonTranslucency);
      ss.setSize(getFloatSetting(PyMOL.cartoon_tube_radius) * 2);
      modelSettings.addLast(ss);
      bs.andNot(bsNuc);
      if (bs.isEmpty())
        return;
    }
    ss = new ModelSettings(JC.SHAPE_TRACE, bs, null);
    ss.setColors(colixes, cartoonTranslucency);
    ss.setSize(getFloatSetting(PyMOL.cartoon_tube_radius) * 2);
    modelSettings.addLast(ss);
  }

  private void setPutty(BS bs) {
    ModelSettings ss;
    float[] info = new float[] {
        getFloatSetting(PyMOL.cartoon_putty_quality),
        getFloatSetting(PyMOL.cartoon_putty_radius),
        getFloatSetting(PyMOL.cartoon_putty_range),
        getFloatSetting(PyMOL.cartoon_putty_scale_min),
        getFloatSetting(PyMOL.cartoon_putty_scale_max),
        getFloatSetting(PyMOL.cartoon_putty_scale_power),        
        getFloatSetting(PyMOL.cartoon_putty_transform)        
    };
 
    ss = new ModelSettings(JC.SHAPE_MESHRIBBON, bs, info);
    ss.setColors(colixes, cartoonTranslucency);
    modelSettings.addLast(ss);
  }

  private void setRibbon(BS bs) {
    // 2ace: 0, 1 ==> backbone
    // fig8: 0, 1 ==> backbone
    // casp: 0, 1 ==> backbone
    // NLG3_AchE: 0, 1 ==> backbone
    // NLG3_HuAChE: 0, 10 ==> trace
    // tach: 0, 10 ==> trace
    // tah-lev: 0, 10 ==> trace
    // 496: -1, 1 ==> backbone
    // kinases: -1, 1 ==> backbone
    // 443_D1: -1, 1 ==> backbone
    // 476Rainbow_New: 10, 8 ==> trace
    
    //float smoothing = getFloatSetting(PyMOL.ribbon_smooth);
    float sampling = getFloatSetting(PyMOL.ribbon_sampling);
    ModelSettings ss = new ModelSettings((//smoothing > 0 || 
        sampling > 1? JC.SHAPE_TRACE
        : JC.SHAPE_BACKBONE), bs, null);
    ss.setColors(colixes, 0); // no translucency
    ss.setSize(getFloatSetting(PyMOL.ribbon_width) * 0.1f);
    modelSettings.addLast(ss);
  }

  private void setCartoon(String key, int sizeID, float factor) {
    BS bs = BSUtil.copy(ssMapAtom.get(key));
    if (bs == null)
      return;
    bs.and(reps[PyMOL.REP_CARTOON]);
    if (bs.isEmpty())
      return;
    ModelSettings ss = new ModelSettings(JC.SHAPE_CARTOON, bs, null);
    ss.setColors(colixes, cartoonTranslucency);
    ss.setSize(getFloatSetting(sizeID) * factor);
    modelSettings.addLast(ss);
  }

  private void cleanSingletonCartoons(BS bs) {
    BS bsr = new BS();
    for (int pass = 0; pass < 2; pass++) {
      int offset = 1;
      int iPrev = Integer.MIN_VALUE;
      int iSeqLast = Integer.MIN_VALUE;
      int iSeq = Integer.MIN_VALUE;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (!isSequential(i, iPrev))
          offset++;
        iSeq = atomSetCollection.getAtom(i).sequenceNumber;
        if (iSeq != iSeqLast) {
          iSeqLast = iSeq;
          offset++;
        }
        if (pass == 0)
          bsr.set(offset);
        else if (!bsr.get(offset))
          bs.clear(i);
        iPrev = i;
      }
      if (pass == 1)
        break;
      BS bsnot = new BS();
      for (int i = bsr.nextSetBit(0); i >= 0; i = bsr.nextSetBit(i + 1))
        if (!bsr.get(i - 1) && !bsr.get(i + 1))
          bsnot.set(i);
      bsr.andNot(bsnot);
    }
  }

  private boolean isSequential(int i, int iPrev) {
    if (i != iPrev + 1)
      return false;
    Atom a = atomSetCollection.getAtom(iPrev);
    Atom b = atomSetCollection.getAtom(i);
    return a.chainID == b.chainID && a.atomSetIndex == b.atomSetIndex;
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

    // calculate Jmol camera position, which is in screen widths,
    // and is from the front of the screen, not the center.
    //
    //               |-------w--------| 1 unit
    //                       o
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
    sb.append(";center ").append(Escape.eP(ptCenter));

    float fov = getFloatSetting(PyMOL.field_of_view);
    float tan = (float) Math.tan(fov / 2 * Math.PI / 180);
    float jmolCameraToCenter = 0.5f / tan; // d
    float jmolCameraDepth = (jmolCameraToCenter - 0.5f);
    String szoom = "100";//"; = 100;//jmolCameraToCenter / pymolCameraToCenter * 100;
    float pymolDistanceToCenter = -getFloatAt(view, 18);
    float w = pymolDistanceToCenter * tan * 2;
    boolean noDims = (width == 0 || height == 0);
    if (noDims) {
      width = viewer.getScreenWidth();
      height = viewer.getScreenHeight();
      //w = 2 * getRotationRadius();
      //szoom = "{visible} 0";
    } else {
    }
    float aspectRatio = (width * 1.0f / height);
    if (aspectRatio < 1)
      szoom = "" + (100 / aspectRatio);
    
    float pymolCameraToCenter = pymolDistanceToCenter / w;
    float pymolCameraToSlab = getFloatAt(view, 22) / w;
    float pymolCameraToDepth = getFloatAt(view, 23) / w;
    int slab = 50 + (int) ((pymolCameraToCenter - pymolCameraToSlab) * 100);
    int depth = 50 + (int) ((pymolCameraToCenter - pymolCameraToDepth) * 100);

    sb.append(";set perspectiveDepth " + (!getBooleanSetting(PyMOL.ortho)));
    sb.append(";set cameraDepth " + jmolCameraDepth);
    sb.append(";set rotationRadius " + (w / 2));
    sb
        .append(";zoom " + szoom + "; slab on; slab " + slab + "; depth "
            + depth);

    sb
        .append(";rotate @{quaternion({")
        // only the first two rows are needed
        .appendF(getFloatAt(view, 0)).append(" ").appendF(getFloatAt(view, 1))
        .append(" ").appendF(getFloatAt(view, 2)).append("}{").appendF(
            getFloatAt(view, 4)).append(" ").appendF(getFloatAt(view, 5))
        .append(" ").appendF(getFloatAt(view, 6)).append("})}");
    sb.append(";translate X ").appendF(getFloatAt(view, 16)).append(
        " angstroms;");
    sb.append(";translate Y ").appendF(-getFloatAt(view, 17)).append(
        " angstroms");

    // seems to be something else here -- fog is not always present
    boolean depthCue = getBooleanSetting(PyMOL.depth_cue); // 84
    boolean fog = getBooleanSetting(PyMOL.fog); // 88

    if (depthCue && fog) {
      float range = depth - slab;
      float fog_start = getFloatSetting(PyMOL.fog_start); // 192
      sb.append(";set zShade true; set zshadePower 1;set zslab "
          + Math.min(100, slab + fog_start * range) + "; set zdepth " + Math.max(0, depth));
    } else if (depthCue) {
      sb.append(";set zShade true; set zshadePower 1;set zslab "
          + ((slab + depth) / 2f) + "; set zdepth " + depth);
    } else {
      sb.append(";set zShade false");
    }

    sb.append(";set traceAlpha "
        + getBooleanSetting(PyMOL.cartoon_round_helices));
    sb.append(";set cartoonRockets " + cartoonRockets);
    if (cartoonRockets)
      sb.append(";set rocketBarrels " + cartoonRockets);
    sb.append(";set cartoonLadders " + haveNucleicLadder);
    sb.append(";set ribbonBorder "
        + getBooleanSetting(PyMOL.cartoon_fancy_helices));
    sb.append(";set cartoonFancy "
        + (!isMovie && !getBooleanSetting(PyMOL.cartoon_fancy_helices))); // for now

    //{ command => 'set hermiteLevel -4',                                                       comment => 'so that SS reps have some thickness' },
    //{ command => 'set ribbonAspectRatio 8',                                                   comment => 'degree of W/H ratio, but somehow not tied directly to actual width parameter...' },
    JmolList<Object> bg = getList(settings, PyMOL.bg_rgb);
    Object o = bg.get(2);
    if (bg.get(1).equals(Integer.valueOf(5))) {
      String s = "000000" + Integer.toHexString(((Integer) o).intValue());
      o = "[x" + s.substring(s.length() - 6) + "]";
    }
    sb.append(";background " + o);
    if (isMovie)
      sb.append(";animation mode loop");
    sb.append(";");
  }

//  private float getRotationRadius() {
//    P3 center = P3.new3((xyzMax.x + xyzMin.x) / 2, (xyzMax.y + xyzMin.y) / 2,
//        (xyzMax.z + xyzMin.z) / 2);
//    float d2max = 0;
//    Atom[] atoms = atomSetCollection.getAtoms();
//    if (isMovie)
//      for (int i = lstTrajectories.size(); --i >= 0;) {
//        P3[] pts = lstTrajectories.get(i);
//        for (int j = pts.length; --j >= 0;) {
//          P3 pt = pts[j];
//          if (pt != null)
//            d2max = maxRadius(d2max, pt.x, pt.y, pt.z, center);
//        }
//      }
//    else
//      for (int i = 0; i < atomCount; i++) {
//        Atom a = atoms[i];
//        d2max = maxRadius(d2max, a.x, a.y, a.z, center);
//      }
//    // 1 is approximate -- for atom radius
//    return (float) Math.pow(d2max, 0.5f) + 1;
//  }
//
//  private static float maxRadius(float d2max, float x, float y, float z,
//                                 P3 center) {
//    float dx = (x - center.x);
//    float dy = (y - center.y);
//    float dz = (z - center.z);
//    float d2 = dx * dx + dy * dy + dz * dz;
//    if (d2 > d2max)
//      d2max = d2;
//    return d2max;
//  }

  @Override
  public void finalizeModelSet(ModelSet modelSet, int baseModelIndex, int baseAtomIndex) {
    BS bsCarb = (haveTraceOrBackbone ? modelSet.getAtomBits(T.carbohydrate, null) : null);
    if (modelSettings != null) {
      for (int i = 0; i < modelSettings.size(); i++) {
        ModelSettings ss = modelSettings.get(i);
        ss.offset(baseModelIndex, baseAtomIndex);
        ss.createShape(modelSet, bsCarb);
      }
    }
    viewer.setTrajectoryBs(BSUtil.newBitSet2(baseModelIndex, modelSet.modelCount));
  }
}
