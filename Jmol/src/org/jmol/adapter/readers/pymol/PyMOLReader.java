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

import java.util.Collection;
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
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BoxInfo; //import org.jmol.util.Escape;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
//import org.jmol.util.Dimension;
import org.jmol.util.Escape;
import org.jmol.util.JmolFont;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JC;

/**
 * PyMOL PSE (binary Python session) file reader.
 * development started Feb 2013 Jmol 13.1.13
 * reasonably full implementation May 2013 Jmol 13.1.16
 * 
 * PyMOL state --> Jmol model 
 * PyMOL object --> Jmol named atom set, isosurface, CGO, or measurement 
 * PyMOL group --> Jmol named atom set (TODO: add isosurfaces and measures to these?) 
 * PyMOL movie: an initial view and a set of N "frames" 
 * PyMOL frame: references (a) a state, (b) a script, and (c) a view
 *
 * using set LOGFILE, we can dump this to a readable form.
 * trajectories are not supported yet
 *     
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 * 
 */

public class PyMOLReader extends PdbReader {

  // a continuation of PyMOL.REP_xxx
  private final static int REP_JMOL_TRACE = 13;
  private final static int REP_JMOL_PUTTY = 14;
  private final static int REP_JMOL_MAX = 15;

  private final static int BRANCH_SELECTION = -1;
  private final static int BRANCH_MOLECULE = 1;
  private final static int BRANCH_MAPDATA = 2;
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

  private boolean allowSurface = true;
  private boolean doResize = false;
  boolean doCache = false;

  private JmolList<Object> settings;
  private Map<Integer, JmolList<Object>> localSettings;

  private int atomCount0;
  private int atomCount;
  private int stateCount;
  private int surfaceCount;
  private int structureCount;

  private boolean isHidden;
  private JmolList<Object> pymolAtoms;

  private BS bsHidden = new BS();
  private BS bsNucleic = new BS();
  private BS bsStructureDefined = new BS();
  private BS bsExcluded;

  private boolean haveTraceOrBackbone;
  private boolean haveNucleicLadder;

  private int[] atomMap;
  private Map<String, BS> ssMapSeq, ssMapAtom;

  private Map<Float, BS> htSpacefill = new Hashtable<Float, BS>();
  //private Map<String, int[]> htAtomMap = new Hashtable<String, int[]>();
  private Map<String, Boolean> occludedBranches = new Hashtable<String, Boolean>();

  private JmolList<Integer> atomColorList = new JmolList<Integer>();
  private JmolList<Text> labels = new JmolList<Text>();
  private JmolObject frameObj;
  private JmolList<JmolObject> jmolObjects = new JmolList<JmolObject>();

  private short[] colixes;
  private boolean isStateScript;
  private boolean valence;

  private P3 xyzMin = P3.new3(1e6f, 1e6f, 1e6f);
  private P3 xyzMax = P3.new3(-1e6f, -1e6f, -1e6f);

  private int nModels;
  private boolean logging;

  private BS[] reps = new BS[REP_JMOL_MAX];
  private float cartoonTranslucency;
  private float sphereTranslucency;
  private float stickTranslucency;
  private boolean cartoonLadderMode;
  private boolean cartoonRockets;
  private int surfaceMode;
  private int surfaceColor;
  private int labelFontId;

  private boolean isMovie;

  private Map<String, Object> htNames = new Hashtable<String, Object>();
  private int pymolFrame, pymolState;
  private boolean allStates;
  private int totalAtomCount;
  private int pymolVersion;
  private P3[] trajectoryStep;
  private int trajectoryPtr;

  private String branchName;
  private String branchNameID;
  private final static P3 ptTemp = new P3();

  private BS bsModelAtoms = BS.newN(1000);
  private float nonBondedSize;
  private float sphereScale;
  private JmolList<JmolList<Object>> selections;
  private Hashtable<Integer, JmolList<Object>> uniqueSettings;
  private P3 labelPosition;
  private float labelColor, labelSize;
  private P3 labelPosition0 = new P3();
  private Map<String, JmolList<Object>> volumeData;
  private JmolList<JmolList<Object>> mapObjects;
  private Map<String, String> branchIDs = new Hashtable<String, String>();
  private Hashtable<String, PyMOLGroup> groups;
  private boolean haveMeasurements;
  private int[] frames;
  private String mepList = "";
  BS bsCarb;

  @Override
  protected void initializeReader() throws Exception {
    isBinary = true;
    isStateScript = htParams.containsKey("isStateScript");
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("noAutoBond",
        Boolean.TRUE);
    atomSetCollection.setAtomSetAuxiliaryInfo("pdbNoHydrogens", Boolean.TRUE);
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("isPyMOL", Boolean.TRUE);
    if (isTrajectory)
      trajectorySteps = new JmolList<P3[]>();
    super.initializeReader();
  }

  @Override
  public void processBinaryDocument(JmolDocument doc) throws Exception {
    doResize = checkFilterKey("DORESIZE");
    allowSurface = !checkFilterKey("NOSURFACE");
    doCache = checkFilterKey("DOCACHE");
    if (doCache)
      bsExcluded = new BS();
    PickleReader reader = new PickleReader(doc, viewer);
    logging = false;
    Map<String, Object> map = reader.getMap(logging);
    reader = null;
    process(map);
  }

  @Override
  protected void model(int modelNumber) {
    bsModelAtoms.clearAll();
    super.model(modelNumber);
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
    // override PDBReader settings
  }

  /**
   * At the end of the day, we need to finalize all the JmolObjects, 
   * set the trajectories, and, if filtered with DOCACHE, create a 
   * streamlined binary file for inclusion in the PNGJ file.
   * 
   */
  @Override
  public void finalizeModelSet(ModelSet modelSet, int baseModelIndex,
                               int baseAtomIndex) {
    bsCarb = (haveTraceOrBackbone ? modelSet.getAtomBits(T.carbohydrate, null)
        : null);
    if (jmolObjects != null) {
      for (int i = 0; i < jmolObjects.size(); i++) {
        try {
          JmolObject obj = jmolObjects.get(i);
          obj.offset(baseModelIndex, baseAtomIndex);
          obj.finalizeObject(modelSet, this);
        } catch (Exception e) {
          System.out.println(e);
        }
      }
      if (haveMeasurements) {
        appendLoadNote(viewer.getMeasurementInfoAsString());
        setLoadNote();
      }
    }
    viewer.setTrajectoryBs(BSUtil.newBitSet2(baseModelIndex,
        modelSet.modelCount));
    //if (baseModelIndex == 0)
    //viewer.setBooleanProperty("_ismovie", true);
    if (!isStateScript && frameObj != null) {
      frameObj.finalizeObject(modelSet, this);
    }
    
    // exclude unnecessary named objects
    
    if (bsExcluded != null) {
      int nExcluded = bsExcluded.cardinality();
      byte[] bytes0 = (byte[]) viewer.getFileAsBytes(filePath, null);
      byte[] bytes = new byte[bytes0.length - nExcluded];
      for (int i = bsExcluded.nextClearBit(0), n = bytes0.length, pt = 0; i < n; i = bsExcluded
          .nextClearBit(i + 1))
        bytes[pt++] = bytes0[i];
      bytes0 = null;
      String fileName = filePath;
      viewer.cacheFile(fileName, bytes);
    }
  }

  /**
   * The main processor.
   * 
   * @param map
   */
  @SuppressWarnings("unchecked")
  private void process(Map<String, Object> map) {
    pymolVersion = ((Integer) map.get("version")).intValue();
    appendLoadNote("PyMOL version: " + pymolVersion);
    
    // create settings and uniqueSettings lists
    settings = getMapList(map, "settings");
    JmolList<Object> file = listAt(settings, PyMOL.session_file);
    if (file != null)
      Logger.info("PyMOL session file: " + file.get(2));
    setVersionSettings();
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("settings", settings);
    setUniqueSettings(getMapList(map, "unique_settings"));

    // just log and display some information here
    
    logging = (viewer.getLogFile().length() > 0);
    JmolList<Object> names = getMapList(map, "names");
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String name = e.getKey();
      Logger.info(name);
      if (name.equals("names")) {
        for (int i = 1; i < names.size(); i++)
          Logger.info("  " + stringAt(listAt(names, i), 0));
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
          viewer.log(TextFormat.simpleReplace(e.getValue().toString(), "[",
              "\n["));
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
    
    // set up additional colors
    // not 100% sure what color clamping is, but this seems to work.
    addColors(getMapList(map, "colors"), getBooleanSetting(PyMOL.clamp_colors));
    
    // set a few global flags
    allStates = getBooleanSetting(PyMOL.all_states);
    pymolFrame = (int) getFloatSetting(PyMOL.frame);
    pymolState = (int) getFloatSetting(PyMOL.state);
    frameObj = addJmolObject(T.frame, null, (allStates ? Integer.valueOf(-1)
        : Integer.valueOf(pymolState - 1)));
    appendLoadNote("frame=" + pymolFrame + " state=" + pymolState
        + " all_states=" + allStates);
    
    // discover totalAtomCount and stateCount:
    getAtomAndStateCount(names);

    // resize frame
    if (!isStateScript && doResize) {
      int width = 0, height = 0;
      try {
        width = intAt(getMapList(map, "main"), 0);
        height = intAt(getMapList(map, "main"), 1);
      } catch (Exception e) {
        // ignore
      }
      String note;
      if (width > 0 && height > 0) {
        note = "PyMOL dimensions width=" + width + " height=" + height;
        atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
            "perferredWidthHeight", new int[] { width, height });
        //Dimension d = 
        viewer.resizeInnerPanel(width, height);
      } else {
        note = "PyMOL dimensions?";
      }
      appendLoadNote(note);
    }
    // PyMOL setting all_states disables movies
    JmolList<Object> mov;
    if (!isStateScript && !allStates && (mov = getMapList(map, "movie")) != null) {
      int frameCount = intAt(mov, 0);
      if (frameCount > 0)
        processMovie(mov, frameCount);
    }
    if (totalAtomCount == 0)
      atomSetCollection.newAtomSet();
    pointAt((JmolList<Object>) listAt(settings, PyMOL.label_position).get(2),
        0, labelPosition0);
    selections = new JmolList<JmolList<Object>>();

    if (allStates && desiredModelNumber == Integer.MIN_VALUE) {
      // if all_states and no model number indicated, display all states
    } else if (isMovie) {
      // otherwise, if a movie, load all states
      switch (desiredModelNumber) {
      case Integer.MIN_VALUE:
        break;
      default:
        desiredModelNumber = frames[(desiredModelNumber > 0
            && desiredModelNumber <= frames.length ? desiredModelNumber
            : pymolFrame) - 1];
        frameObj = addJmolObject(T.frame, null, Integer
            .valueOf(desiredModelNumber - 1));
        break;
      }
    } else if (desiredModelNumber == 0) {
      // otherwise if you specify model "0", only load the current PyMOL state
      desiredModelNumber = pymolState;
    } else {
      // load only the state you request, or all states, if you don't specify
    }
    for (int j = 0; j < stateCount; j++) {
      //if (desiredModelNumber == Integer.MIN_VALUE && j + 1 != pymolState)continue;
      if (!doGetModel(++nModels, null))
        continue;
      model(nModels);
      if (isTrajectory) {
        trajectoryStep = new P3[totalAtomCount];
        trajectorySteps.addLast(trajectoryStep);
        trajectoryPtr = 0;
      }
      for (int i = 1; i < names.size(); i++)
        processBranch(listAt(names, i), true, j);
    }
    for (int i = 1; i < names.size(); i++)
      processBranch(listAt(names, i), false, 0);
    branchNameID = null;
    
    // not currently generating selections
    //processSelections();
    
    // meshes are special objects that depend upon grid map data
    if (mapObjects != null && allowSurface)
      processMeshes();
    
    // trajectories are not supported yet
    if (isTrajectory) {
      appendLoadNote("PyMOL trajectories read: " + trajectorySteps.size());
      atomSetCollection.finalizeTrajectoryAs(trajectorySteps, null);
    }
    
    processDefinitions();
    processScenes(map);
    // no need to render if this is a state script
    if (!isStateScript)
      setRendering(getMapList(map, "view"));
    if (atomCount == 0)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("dataOnly",
          Boolean.TRUE);
  }

  /**
   * Attempt to adjust for PyMOL versions. 
   * See PyMOL layer3.Executive.c
   *  
   */
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

  /**
   * Create uniqueSettings from the "unique_settings" map item.
   * This will be used later in processing molecule branches.
   * 
   * @param list
   */
  @SuppressWarnings("unchecked")
  private void setUniqueSettings(JmolList<Object> list) {
    uniqueSettings = new Hashtable<Integer, JmolList<Object>>();
    if (list != null && list.size() != 0) {
      for (int i = list.size(); --i >= 0;) {
        JmolList<Object> atomSettings = (JmolList<Object>) list.get(i);
        int id = intAt(atomSettings, 0);
        JmolList<Object> mySettings = (JmolList<Object>) atomSettings.get(1);
        for (int j = mySettings.size(); --j >= 0;) {
          JmolList<Object> setting = (JmolList<Object>) mySettings.get(j);
          int uid = id * 1000 + intAt(setting, 0);
          uniqueSettings.put(Integer.valueOf(uid), setting);
          Logger.info("PyMOL unique setting " + id + " " + setting);
        }
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

  /**
   * Add new colors from the main "colors" map object.
   * Not 100% clear how color clamping works.
   * 
   * @param colors
   * @param isClamped
   */
  private void addColors(JmolList<Object> colors, boolean isClamped) {
    if (colors == null || colors.size() == 0)
      return;
    // note, we are ignoring lookup-table colors
    for (int i = colors.size(); --i >= 0;) {
      JmolList<Object> c = listAt(colors, i);
      PyMOL.addColor((Integer) c.get(1), isClamped ? colorSettingClamped(c)
          : colorSetting(c));
    }
  }

  /**
   * Look through all named branches for molecules, counting
   * atoms and also states; see if trajectories are compatible (experimental).
   *  
   * @param names
   */
  private void getAtomAndStateCount(JmolList<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      JmolList<Object> branch = listAt(names, i);
      int type = getBranchType(branch);
      if (!checkBranch(branch))
        continue;
      if (type == BRANCH_MOLECULE) {
        JmolList<Object> deepBranch = listAt(branch, 5);
        JmolList<Object> states = listAt(deepBranch, 4);
        int ns = states.size();
        if (ns > stateCount)
          stateCount = ns;
        int nAtoms = getBranchAtoms(deepBranch).size();
        for (int j = 0; j < ns; j++) {
          JmolList<Object> state = listAt(states, j);
          JmolList<Object> idxToAtm = listAt(state, 3);
          if (idxToAtm == null) {
            isTrajectory = false;
          } else {
            int m = idxToAtm.size();
            n += m;
            if (isTrajectory && m != nAtoms)
              isTrajectory = false;
          }
        }
      }
    }
    totalAtomCount = n;
    Logger.info("PyMOL total atom count = " + totalAtomCount);
    Logger.info("PyMOL state count = " + stateCount);
  }

  private boolean checkBranch(JmolList<Object> branch) {
    branchName = stringAt(branch, 0);
    isHidden = (intAt(branch, 2) != 1);
    return (branchName.indexOf("_") != 0);
  }

  /**
   * Create a JmolObject that will represent the movie.
   * For now, only process unscripted movies without views.
   * 
   * @param mov
   * @param frameCount
   */
  private void processMovie(JmolList<Object> mov, int frameCount) {
    Map<String, Object> movie = new Hashtable<String, Object>();
    movie.put("frameCount", Integer.valueOf(frameCount));
    movie.put("currentFrame", Integer.valueOf(pymolFrame - 1));
    boolean haveCommands = false, haveViews = false, haveFrames = false;
    JmolList<Object> list = listAt(mov, 4);
    for (int i = list.size(); --i >= 0;)
      if (intAt(list, i) != 0) {
        frames = new int[list.size()];
        for (int j = frames.length; --j >= 0;)
          frames[j] = intAt(list, j) + 1;
        movie.put("frames", frames);
        haveFrames = true;
        break;
      }
    JmolList<Object> cmds = listAt(mov, 5);
    String cmd;
    for (int i = cmds.size(); --i >= 0;)
      if ((cmd = stringAt(cmds, i)) != null && cmd.length() > 1) {
        cmds = fixMovieCommands(cmds);
        if (cmds != null) {
          movie.put("commands", cmds);
          haveCommands = true;
          break;
        }
      }
    JmolList<Object> views = listAt(mov, 6);
    JmolList<Object> view;
    for (int i = views.size(); --i >= 0;)
      if ((view = listAt(views, i)) != null && view.size() >= 12
          && view.get(1) != null) {
        haveViews = true;
        views = fixMovieViews(views);
        if (views != null) {
          movie.put("views", views);
          break;
        }
      }
    appendLoadNote("PyMOL movie frameCount = " + frameCount);
    if (haveFrames && !haveCommands && !haveViews) {
      // simple animation
      isMovie = true;
      branchNameID = null;
      frameObj = getJmolObject(T.movie, null, movie);
    } else {
      //isMovie = true;  for now, no scripted movies
      //pymol.put("movie", movie);
    }
  }

  /**
   * Could implement something here that creates a Jmol view.
   * 
   * @param views
   * @return new views
   */
  private static JmolList<Object> fixMovieViews(JmolList<Object> views) {
    // TODO -- PyMOL to Jmol views
    return views;
  }

  /**
   * Could possibly implement something here that interprets PyMOL script commands.
   * 
   * @param cmds
   * @return new cmds
   */
  private static JmolList<Object> fixMovieCommands(JmolList<Object> cmds) {
    // TODO -- PyMOL to Jmol commands
    return cmds;
  }
  
  /**
   * The main branch processor. 
   * Not implemented: ALIGNMENT, CALLBACK, SLICE, SURFACE 
   * 
   * @param branch
   * @param moleculeOnly
   * @param iState
   */
  @SuppressWarnings("unchecked")
  private void processBranch(JmolList<Object> branch, boolean moleculeOnly,
                             int iState) {
    int type = getBranchType(branch);
    JmolList<Object> startLen = (JmolList<Object>) branch
        .get(branch.size() - 1);
    if ((type == BRANCH_MOLECULE) != moleculeOnly || !checkBranch(branch))
      return;
    Logger.info("PyMOL model " + (nModels) + " Branch " + branchName
        + (isHidden ? " (hidden)" : " (visible)"));
    JmolList<Object> deepBranch = listAt(branch, 5);
    branchNameID = fixName(branchName
        + (moleculeOnly ? "_" + (iState + 1) : ""));
    branchIDs.put(branchName, branchNameID);
    String msg = null;
    JmolList<Object> branchInfo = listAt(deepBranch, 0);
    setLocalSettings(listAt(branchInfo, 8));
    boolean doGroups = !isStateScript;
    String parentGroupName = (!doGroups || branch.size() < 8 ? null : stringAt(
        branch, 6));
    BS bsAtoms = null;
    boolean doExclude = (bsExcluded != null);

    switch (type) {
    default:
      msg = "" + type;
      break;
    case BRANCH_SELECTION:
      // not treating these properly yet
      selections.addLast(branch);
      break;
    case BRANCH_MOLECULE:
      doExclude = false;
      bsAtoms = processMolecule(deepBranch, iState);
      break;
    case BRANCH_MEASURE:
      doExclude = false;
      processMeasure(deepBranch);
      break;
    case BRANCH_MAPMESH:
    case BRANCH_MAPDATA:
      processMap(deepBranch, type == BRANCH_MAPMESH);
      break;
    case BRANCH_GADGET:
      processGadget(deepBranch);
      break;
    case BRANCH_GROUP:
      if (doGroups)
        addGroup(branch, null, type);
      break;
    case BRANCH_CGO:
      msg = "CGO";
      processCGO(deepBranch);
      break;

      // unimplemented:
      
    case BRANCH_ALIGNMENT:
      msg = "ALIGNEMENT";
      break;
    case BRANCH_CALCULATOR:
      msg = "CALCULATOR";
      break;
    case BRANCH_CALLBACK:
      msg = "CALLBACK";
      break;
    case BRANCH_SLICE:
      msg = "SLICE";
      break;
    case BRANCH_SURFACE:
      msg = "SURFACE";
      break;
    }
    if (parentGroupName != null) {
      PyMOLGroup group = addGroup(branch, parentGroupName, type);
      if (bsAtoms != null)
        addGroupAtoms(group, bsAtoms);
    }
    if (doExclude) {
      int i0 = intAt(startLen, 0);
      int len = intAt(startLen, 1);
      bsExcluded.setBits(i0, i0 + len);
      Logger.info("cached PSE file excludes PyMOL object type " + type
          + " name=" + branchName + " len=" + len);
    }
    if (msg != null)
      Logger.error("Unprocessed branch type " + msg + " " + branchName);
  }

  @SuppressWarnings("unchecked")
  private void setLocalSettings(JmolList<Object> list) {
    localSettings = new Hashtable<Integer, JmolList<Object>>();
    if (list != null && list.size() != 0) {
      System.out.println("local settings: " + list.toString());
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
    //solventAsSpheres = getBooleanSetting(PyMOL.sphere_solvent); - this is for SA-Surfaces
    surfaceMode = (int) getFloatSetting(PyMOL.surface_mode);
    surfaceColor = (int) getFloatSetting(PyMOL.surface_color);
    labelPosition = new P3();
    try {
      JmolList<Object> setting = localSettings.get(Integer
          .valueOf(PyMOL.label_position));
      pointAt((JmolList<Object>) setting.get(2), 0, labelPosition);
    } catch (Exception e) {
      // no problem.
    }
    labelPosition.add(labelPosition0);
    labelColor = getFloatSetting(PyMOL.label_color);
    labelSize = getFloatSetting(PyMOL.label_size);
    labelFontId = (int) getFloatSetting(PyMOL.label_font_id);
  }

  /**
   * Create a heirarchical list of named groups
   * as generally seen on the PyMOL app's right-hand object menu.
   * 
   * @param branch
   * @param parent
   * @param type
   * @return group
   */
  private PyMOLGroup addGroup(JmolList<Object> branch, String parent, int type) {
    if (groups == null)
      groups = new Hashtable<String, PyMOLGroup>();
    PyMOLGroup myGroup = getGroup(fixName(branchName));
    myGroup.branch = branch;
    myGroup.branchNameID = branchNameID;
    myGroup.visible = !isHidden;
    myGroup.type = type;
    if (parent != null)
      getGroup(fixName(parent)).addList(myGroup);
    return myGroup;
  }

  private PyMOLGroup getGroup(String name) {
    PyMOLGroup g = groups.get(name);
    if (g == null)
      groups.put(name, (g = new PyMOLGroup(name)));
    return g;
  }

  private void addGroupAtoms(PyMOLGroup group, BS bsAtoms) {
    group.bsAtoms = bsAtoms;
  }

  /**
   * Create a CGO JmolObject, just passing on key information. 
   * 
   * @param deepBranch
   */
  private void processCGO(JmolList<Object> deepBranch) {
    if (isStateScript)
      return;
    if (isHidden)
      return;
    int color = intAt(listAt(deepBranch, 0), 2);
    JmolList<Object> data = listAt(listAt(deepBranch, 2), 0);
    data.addLast(branchName);
    JmolObject jo = addJmolObject(JC.SHAPE_CGO, null, data);
    jo.argb = PyMOL.getRGB(color);
    jo.translucency = getFloatSetting(PyMOL.cgo_transparency);
    appendLoadNote("CGO " + fixName(branchName));
  }

  /**
   * Only process _e_pot objects -- which we need for color settings 
   * @param deepBranch
   */
  private void processGadget(JmolList<Object> deepBranch) {
    if (branchName.endsWith("_e_pot"))
      processMap(deepBranch, true);
  }

  /**
   * Create mapObjects and volumeData; create an ISOSURFACE JmolObject.
   * 
   * @param deepBranch
   * @param isObject
   */
  private void processMap(JmolList<Object> deepBranch, boolean isObject) {
    if (isObject) {
      if (isStateScript)
        return;
      if (isHidden)
        return; // for now
      if (mapObjects == null)
        mapObjects = new JmolList<JmolList<Object>>();
      mapObjects.addLast(deepBranch);
    } else {
      if (volumeData == null)
        volumeData = new Hashtable<String, JmolList<Object>>();
      volumeData.put(branchName, deepBranch);
      if (!isHidden && !isStateScript)
        addJmolObject(T.isosurface, null, branchName);
    }
    deepBranch.addLast(branchName);
  }

  //  private void processSelections() {
  //    for (int i = selections.size(); --i >= 0;) {
  //      JmolList<Object> branch = selections.get(i);
  //      checkBranch(branch);
  //      processBranchSelection(listAt(branch, 5));
  //    }
  //  }

  //  [Ca, 1, 0, 
  //   [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], -1, 
  //   [
  //   [H115W, 
  //   [2529, 2707], 
  //   [1, 1]]], ]

  //  private void processBranchSelection(JmolList<Object> deepBranch) {
  //    BS bs = new BS();
  //    for (int j = deepBranch.size(); --j >= 0;) {
  //      JmolList<Object> data = listAt(deepBranch, j);
  //      String parent = stringAt(data, 0);
  //      atomMap = htAtomMap.get(parent);
  //      if (atomMap == null)
  //        continue;
  //      JmolList<Object> atoms = listAt(data, 1);
  //      for (int i = atoms.size(); --i >= 0;) {
  //        int ia = atomMap[intAt(atoms, i)];
  //        if (ia >= 0)
  //          bs.set(ia);
  //      }
  //    }
  //    if (!bs.isEmpty())
  //      addName(branchName, bs);
  //  }

  /**
   * Create a MEASURE JmolObject.
   * 
   * @param deepBranch
   */
  private void processMeasure(JmolList<Object> deepBranch) {
    if (isStateScript)
      return;
    if (isHidden)
      return; // will have to reconsider this if there is a movie, though
    Logger.info("PyMOL measure " + branchName);
    JmolList<Object> measure = listAt(listAt(deepBranch, 2), 0);
    int color = intAt(listAt(deepBranch, 0), 2);
    int pt;
    int nCoord = (measure.get(pt = 1) instanceof JmolList<?> ? 2 : measure
        .get(pt = 4) instanceof JmolList<?> ? 3
        : measure.get(pt = 6) instanceof JmolList<?> ? 4 : 0);
    if (nCoord == 0)
      return;
    JmolList<Object> list = listAt(measure, pt);
    JmolList<Object> offsets = listAt(measure, 8);
    int len = list.size();
    float rad = getFloatSetting(PyMOL.dash_width) / 20;
    if (rad == 0)
      rad = 0.05f;
    int index = 0;
    int c = PyMOL.getRGB(color);
    short colix = C.getColix(c);
    for (int p = 0; p < len;) {
      JmolList<Object> points = new JmolList<Object>();
      for (int i = 0; i < nCoord; i++, p += 3)
        points.addLast(pointAt(list, p, new Point3fi()));
      BS bs = BSUtil.newAndSetBit(0);
      float[] offset = floatsAt(listAt(offsets, index++), 0, new float[7], 7);
      MeasurementData md = new MeasurementData(fixName(branchNameID + "_"
          + index), viewer, points);
      md.note = branchName;
      String strFormat = "";
      int nDigits = 1;
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
      if (offsets != null)
        strFormat = nCoord + ":%0." + (nDigits < 0 ? 1 : nDigits) + "VALUE";
      else
        strFormat = nCoord + ": ";
      Text text = newTextLabel(strFormat, offset, colix,
          (int) getFloatSetting(PyMOL.label_font_id),
          getFloatSetting(PyMOL.label_size));
      md.set(T.define, null, strFormat, "angstroms", null, false, false, null,
          false, (int) (rad * 2000), colix, text);
      addJmolObject(JC.SHAPE_MEASURES, bs, md);
      haveMeasurements = true;
    }

  }

  /**
   * Create everything necessary to generate a molecule in Jmol. 
   * 
   * @param deepBranch
   * @param iState
   * @return atom set only if this is a trajectory.
   */
  private BS processMolecule(JmolList<Object> deepBranch, int iState) {
    atomCount = atomCount0 = atomSetCollection.getAtomCount();
    int nAtoms = intAt(deepBranch, 3);
    if (nAtoms == 0)
      return null;
    ssMapSeq = new Hashtable<String, BS>();
    ssMapAtom = new Hashtable<String, BS>();
    atomMap = new int[nAtoms];
    //htAtomMap.put(branchName, atomMap);
    if (iState == 0)
      processMolCryst(listAt(deepBranch, 10));
    JmolList<Bond> bonds = processMolBonds(listAt(deepBranch, 6));
    JmolList<Object> states = listAt(deepBranch, 4);
    pymolAtoms = getBranchAtoms(deepBranch);
    String fname = "__" + fixName(branchName);
    BS bsAtoms = (BS) htNames.get(fname);
    if (bsAtoms == null) {
      bsAtoms = BS.newN(atomCount0 + nAtoms);
      Logger.info("PyMOL molecule " + branchName);
      htNames.put(fname, bsAtoms);
    }
    for (int i = 0; i < REP_JMOL_MAX; i++)
      reps[i] = BS.newN(1000);

    // TODO: Implement trajectory business here.

    JmolList<Object> state = listAt(states, iState);
    JmolList<Object> idxToAtm = listAt(state, 3);
    int n = (idxToAtm == null ? 0 : idxToAtm.size());
    if (n == 0)
      return null;

    JmolList<Object> coords = listAt(state, 2);
    JmolList<Object> labelPositions = listAt(state, 8);
    if (iState == 0 || !isTrajectory)
      for (int idx = 0; idx < n; idx++) {
        P3 a = addAtom(pymolAtoms, intAt(idxToAtm, idx), idx, coords,
            labelPositions, bsAtoms, iState);
        if (a != null)
          trajectoryStep[trajectoryPtr++] = a;
      }
    addBonds(bonds);
    addMolStructures();
    if (!isStateScript)
      createShapeObjects();
    ssMapSeq = ssMapAtom = null;

    Logger.info("reading " + (atomCount - atomCount0) + " atoms");
    Logger.info("----------");
    return bsAtoms;
  }

  /**
   * Pick up the crystal data.
   * 
   * @param cryst
   */
  private void processMolCryst(JmolList<Object> cryst) {
    if (cryst == null || cryst.size() == 0)
      return;
    JmolList<Object> l = listAt(listAt(cryst, 0), 0);
    JmolList<Object> a = listAt(listAt(cryst, 0), 1);
    setUnitCell(floatAt(l, 0), floatAt(l, 1), floatAt(l, 2), floatAt(a, 0),
        floatAt(a, 1), floatAt(a, 2));
    setSpaceGroupName(stringAt(cryst, 1));
  }

  /**
   * Create the bond set.
   * 
   * @param bonds
   * @return list of bonds
   */
  private JmolList<Bond> processMolBonds(JmolList<Object> bonds) {
    JmolList<Bond> bondList = new JmolList<Bond>();
    int color = (int) getFloatSetting(PyMOL.stick_color);
    float radius = getFloatSetting(PyMOL.stick_radius) / 2;
    float translucency = getFloatSetting(PyMOL.stick_transparency);
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      JmolList<Object> b = listAt(bonds, i);
      int order = (valence ? intAt(b, 2) : 1);
      if (order < 1 || order > 3)
        order = 1;
      // TODO: hydrogen bonds?
      int ia = intAt(b, 0);
      int ib = intAt(b, 1);
      Bond bond = new Bond(ia, ib, order);
      bondList.addLast(bond);
      int c;
      float rad, t;
      boolean hasID = (b.size() > 6 && intAt(b, 6) != 0);
      if (hasID) {
        int id = intAt(b, 5);
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
        bond.colix = C.getColixTranslucent3(C.getColix(PyMOL.getRGB(c)), t > 0,
            t);
    }
    return bondList;
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
   * @param labelPositions
   * @param bsState
   *        this state -- Jmol atomIndex
   * @param iState
   * @return true if successful
   * 
   */
  private P3 addAtom(JmolList<Object> pymolAtoms, int apt, int icoord,
                     JmolList<Object> coords, JmolList<Object> labelPositions,
                     BS bsState, int iState) {
    atomMap[apt] = -1;
    JmolList<Object> a = listAt(pymolAtoms, apt);
    int seqNo = intAt(a, 0); // may be negative
    String chainID = stringAt(a, 1);
    String altLoc = stringAt(a, 2);
    String insCode = " "; //?    
    String name = stringAt(a, 6);
    String group3 = stringAt(a, 5);
    if (group3.length() > 3)
      group3 = group3.substring(0, 3);
    if (group3.equals(" "))
      group3 = "UNK";
    String sym = stringAt(a, 7);
    if (sym.equals("A"))
      sym = "C";
    boolean isHetero = (intAt(a, 19) != 0);
    PyMOLAtom atom = (PyMOLAtom) processAtom(new PyMOLAtom(), name, altLoc
        .charAt(0), group3, chainID.charAt(0), seqNo, insCode.charAt(0),
        isHetero, sym);
    if (!filterPDBAtom(atom, fileAtomIndex++))
      return null;
    icoord *= 3;
    float x = floatAt(coords, icoord);
    float y = floatAt(coords, ++icoord);
    float z = floatAt(coords, ++icoord);
    BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    if (isTrajectory && iState > 0)
      return null;

    bsHidden.setBitTo(atomCount, isHidden);
    bsModelAtoms.set(atomCount);
    if (bsState != null)
      bsState.set(atomCount);

    boolean isNucleic = (nucleic.indexOf(group3) >= 0);
    if (isNucleic)
      bsNucleic.set(atomCount);
    atom.label = stringAt(a, 9);
    String ssType = stringAt(a, 10);
    if (seqNo >= MIN_RESNO
        && (!ssType.equals(" ") || name.equals("CA") || isNucleic)) {
      if (ssMapAtom.get(ssType) == null)
        ssMapAtom.put(ssType, new BS());
      BS bs = ssMapSeq.get(ssType);
      if (bs == null)
        ssMapSeq.put(ssType, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
      ssType += chainID;
      bs = ssMapSeq.get(ssType);
      if (bs == null)
        ssMapSeq.put(ssType, bs = new BS());
      bs.set(seqNo - MIN_RESNO);
    }
    atom.bfactor = floatAt(a, 14);
    atom.occupancy = (int) (floatAt(a, 15) * 100);
    atom.radius = floatAt(a, 16);
    if (atom.radius == 0)
      atom.radius = 1;
    atom.partialCharge = floatAt(a, 17);
    int formalCharge = intAt(a, 18);
    
    atom.bsReps = getBsReps(listAt(a, 20));
    float translucency = getUniqueFloat(atom.uniqueID,
        PyMOL.sphere_transparency, sphereTranslucency);
    int atomColor = intAt(a, 21);
    atomColorList.addLast(Integer.valueOf(getColix(atomColor, translucency)));
    int serNo = intAt(a, 22);
    atom.cartoonType = intAt(a, 23);
    atom.flags = intAt(a, 24);
    atom.bonded = intAt(a, 25) != 0;
    if (a.size() > 40 && intAt(a, 40) == 1)
      atom.uniqueID = intAt(a, 32);
    if (a.size() > 46) {
      float[] data = floatsAt(a, 41, new float[7], 6);
      atomSetCollection.setAnisoBorU(atom, data, 12);
    }
    processAtom2(atom, serNo, x, y, z, formalCharge);    
    setAtomReps(atom, apt, atomColor, labelPositions);
    atomMap[apt] = atomCount++;
    return null;
  }

  private void setAtomReps(PyMOLAtom atom, int apt, int atomColor,
                           JmolList<Object> labelPositions) {
    int iAtom = atom.atomIndex;

    if (atom.bsReps.get(PyMOL.REP_NONBONDED) && atom.bonded)
      atom.bsReps.clear(PyMOL.REP_NONBONDED);
    if (atom.bsReps.get(PyMOL.REP_LABELS) && atom.label.equals(" "))
      atom.bsReps.clear(PyMOL.REP_LABELS);

    // surfaces depend upon global flags
    boolean isVisible = !atom.bsReps.isEmpty();
    boolean isH = atom.elementSymbol.equals("H");    
    boolean surfaceAtom = true;
    //    #define cRepSurface_by_flags       0
    //    #define cRepSurface_all            1
    //    #define cRepSurface_heavy_atoms    2
    //    #define cRepSurface_vis_only       3
    //    #define cRepSurface_vis_heavy_only 4
    switch (surfaceMode) {
    case 0:
      surfaceAtom = ((atom.flags & PyMOL.FLAG_NOSURFACE) == 0);
      break;
    case 1:
      break;
    case 2:
      surfaceAtom = !isH;
      break;
    case 3:
      surfaceAtom = isVisible;
      break;
    case 4:
      surfaceAtom = isVisible && !isH;
      break;
    }
    if (!surfaceAtom) {
      atom.bsReps.clear(PyMOL.REP_MESH);
      atom.bsReps.clear(PyMOL.REP_SURFACE);
    }

    // create atom bit sets for each representation type
    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (atom.bsReps.get(i))
        reps[i].set(iAtom);
    
    if (reps[PyMOL.REP_LABELS].get(iAtom)) {
        int icolor = (int) getUniqueFloat(atom.uniqueID, PyMOL.label_color,
            labelColor);
        if (icolor == -6)
          icolor = 0;// FRONT??
        else if (icolor < 0)
          icolor = atomColor;
        float[] labelPos = new float[7];
        JmolList<Object> labelOffset = listAt(labelPositions, apt);
        if (labelOffset == null) {
          P3 offset = getUniquePoint(atom.uniqueID, PyMOL.label_position, null);
          if (offset == null)
            offset = labelPosition;
          else
            offset.add(labelPosition);
          labelPos[0] = 1;
          labelPos[1] = offset.x;
          labelPos[2] = offset.y;
          labelPos[3] = offset.z;
        } else {
          for (int i = 0; i < 7; i++)
            labelPos[i] = floatAt(labelOffset, i);
        }
        labels.addLast(newTextLabel(atom.label, labelPos, getColix(icolor, 0),
            (int) getUniqueFloat(atom.uniqueID, PyMOL.label_font_id,
                labelFontId), getUniqueFloat(atom.uniqueID, PyMOL.label_size,
                labelSize)));
    }
    float rad = 0;
    if (reps[PyMOL.REP_SPHERES].get(iAtom)) {
      float mySphereSize = getUniqueFloat(atom.uniqueID, PyMOL.sphere_scale,
          sphereScale);
      // nl1_nl2 -- stumped!
      rad = atom.radius * mySphereSize;
    } else if (reps[PyMOL.REP_NBSPHERES].get(iAtom)) {
      // Penta_vs_mutants calcium
      float myNonBondedSize = getUniqueFloat(atom.uniqueID,
          PyMOL.nonbonded_size, nonBondedSize);
      rad = atom.radius * myNonBondedSize;
    }
    if (rad != 0) {
      Float r = Float.valueOf(rad);
      BS bsr = htSpacefill.get(r);
      if (bsr == null)
        htSpacefill.put(r, bsr = new BS());
      bsr.set(iAtom);
    }
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
      
      // 0, 2, 3, 5, 6 are not treated in any special way
      
      switch (atom.cartoonType) {
      case 1:
      case 4:
        reps[REP_JMOL_TRACE].set(iAtom);
        //$FALL-THROUGH$
      case -1:
        reps[PyMOL.REP_CARTOON].clear(iAtom);
        break;
      case 7:
        reps[PyMOL.REP_CARTOON].clear(iAtom);
        reps[REP_JMOL_PUTTY].set(iAtom);
        break;
      }
    }
  }

  private Text newTextLabel(String label, float[] labelOffset, short colix,
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
    JmolFont font = viewer.getFont3D(face, style, fontSize == 0 ? 12 : fontSize
        * factor);
    Text t = Text.newLabel(viewer.getGraphicsData(), font, label, colix,
        (short) 0, 0, 0, labelOffset);
    return t;
  }

  private void addBonds(JmolList<Bond> bonds) {
    int n = bonds.size();
    for (int i = 0; i < n; i++) {
      Bond bond = bonds.get(i);
      bond.atomIndex1 = atomMap[bond.atomIndex1];
      bond.atomIndex2 = atomMap[bond.atomIndex2];
      if (bond.atomIndex1 >= 0 && bond.atomIndex2 >= 0)
        atomSetCollection.addBond(bond);
    }
  }

  private void addMolStructures() {
    if (atomSetCollection.bsStructuredModels == null)
      atomSetCollection.bsStructuredModels = new BS();
    atomSetCollection.bsStructuredModels.set(Math.max(atomSetCollection
        .getCurrentAtomSetIndex(), 0));

    addMolSS("H", ssMapAtom.get("H"), EnumStructure.HELIX);
    addMolSS("S", ssMapAtom.get("S"), EnumStructure.SHEET);
    addMolSS("L", ssMapAtom.get("L"), EnumStructure.TURN);
    addMolSS(" ", ssMapAtom.get(" "), EnumStructure.NONE);
  }

  /**
   * Secondary structure definition. 
   * 
   * @param ssType
   * @param bsAtom  to be set by this method
   * @param type
   */
  private void addMolSS(String ssType, BS bsAtom, EnumStructure type) {
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
        int pt = bsStructureDefined.nextSetBit(istart);
        if (pt >= 0 && pt <= iend)
          continue;
        bsStructureDefined.setBits(istart, iend + 1);
        Structure structure = new Structure(imodel, type, type,
            type.toString(), ++structureCount, type == EnumStructure.SHEET ? 1 : 0);
        Atom a = atoms[istart];
        Atom b = atoms[iend];
        structure.set(a.chainID, a.sequenceNumber, a.insertionCode, b.chainID,
            b.sequenceNumber, b.insertionCode, istart, iend);
        atomSetCollection.addStructure(structure);
      }
      bsAtom.setBits(istart, iend + 1);
      istart = -1;
    }
  }

  /**
   * Create JmolObjects for all the molecular shapes; not executed for a state
   * script.
   * 
   */
  private void createShapeObjects() {
    BS bs = BSUtil.newBitSet2(atomCount0, atomCount);
    JmolObject jo = addJmolObject(JC.SHAPE_BALLS, bs, null);
    colixes = ArrayUtil.ensureLengthShort(colixes, atomCount);
    for (int i = atomCount; --i >= atomCount0;)
      colixes[i] = (short) atomColorList.get(i).intValue();
    jo.setColors(colixes, 0);
    jo.setSize(0);
    jo = addJmolObject(JC.SHAPE_STICKS, bs, null);
    jo.setSize(0);
    createSpacefillObject();
    Atom[] atoms = atomSetCollection.getAtoms();
    cleanSingletons(atoms, reps[PyMOL.REP_CARTOON]);
    cleanSingletons(atoms, reps[REP_JMOL_TRACE]);
    cleanSingletons(atoms, reps[REP_JMOL_PUTTY]);
    createShapeObject(PyMOL.REP_LINES);
    createShapeObject(PyMOL.REP_STICKS);
    for (int i = 0; i < REP_JMOL_MAX; i++)
      switch (i) {
      case PyMOL.REP_LINES:
      case PyMOL.REP_STICKS:
        continue;
      default:
        createShapeObject(i);
        continue;
      }
  }

  /**
   * Create a BALLS JmolObject for each radius.
   * 
   */
  private void createSpacefillObject() {
    for (Map.Entry<Float, BS> e : htSpacefill.entrySet()) {
      float r = e.getKey().floatValue();
      BS bs = e.getValue();
      addJmolObject(JC.SHAPE_BALLS, bs, null).rd = new RadiusData(null, r,
          RadiusData.EnumType.ABSOLUTE, EnumVdw.AUTO);
    }
    htSpacefill.clear();
  }

  /**
   * PyMOL does not display cartoons or traces for single-residue runs.
   * 
   * @param atoms
   * @param bs
   */
  private static void cleanSingletons(Atom[] atoms, BS bs) {
    BS bsr = new BS();
    int n = bs.length();
    int pass = 0;
    while (true) {
      int offset = 1;
      int iPrev = Integer.MIN_VALUE;
      int iSeqLast = Integer.MIN_VALUE;
      int iSeq = Integer.MIN_VALUE;
      for (int i = 0; i < n; i++) {
        if (nextChain(atoms, i, iPrev))
          offset++;
        iSeq = atoms[i].sequenceNumber;
        if (iSeq != iSeqLast) {
          iSeqLast = iSeq;
          offset++;
        }
        if (pass == 0) {
          if (bs.get(i))
            bsr.set(offset);
        } else if (!bsr.get(offset))
          bs.clear(i);
        iPrev = i;
      }
      if (++pass == 2)
        break;
      BS bsnot = new BS();
      for (int i = bsr.nextSetBit(0); i >= 0; i = bsr.nextSetBit(i + 1))
        if (!bsr.get(i - 1) && !bsr.get(i + 1))
          bsnot.set(i);
      bsr.andNot(bsnot);
    }
  }

  private static boolean nextChain(Atom[] atoms, int i, int iPrev) {
    if (i == 0 || iPrev < 0)
      return true;
    Atom a = atoms[iPrev];
    Atom b = atoms[i];
    return a.chainID != b.chainID;
  }

  /**
   * Create JmolObjects for each shape.
   * 
   * Note that LINES and STICKS are done initially, 
   * then all the others are processed. 
   * 
   * REP_DASHES not implemented yet.
   * 
   * @param shapeID
   */
  private void createShapeObject(int shapeID) {
    // add more to implement
    BS bs = reps[shapeID];
    float f;
    if (bs.isEmpty())
      return;
    JmolObject jo = null;
    switch (shapeID) {
    case PyMOL.REP_NONBONDED:
      jo = addJmolObject(JC.SHAPE_STARS, bs, null);
      jo.rd = new RadiusData(null, getFloatSetting(PyMOL.nonbonded_size) / 2,
          RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      break;
    case PyMOL.REP_NBSPHERES:
      jo = addJmolObject(JC.SHAPE_BALLS, bs, null);
      jo.translucency = sphereTranslucency;
      jo.setSize(getFloatSetting(PyMOL.nonbonded_size) * 2);
      break;
    case PyMOL.REP_SPHERES:
      jo = addJmolObject(JC.SHAPE_BALLS, bs, null);
      jo.translucency = sphereTranslucency;
      break;
    case PyMOL.REP_DOTS:
      jo = addJmolObject(JC.SHAPE_DOTS, bs, null);
      f = getFloatSetting(PyMOL.sphere_scale);
      jo.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      break;
    case PyMOL.REP_CARTOON:
      if (cartoonRockets)
        createCartoonObject("H", PyMOL.cartoon_helix_radius, 2);
      else
        createCartoonObject("H", PyMOL.cartoon_oval_length, 2);
      createCartoonObject("S", PyMOL.cartoon_rect_length, 2);
      createCartoonObject("L", PyMOL.cartoon_loop_radius, 2);
      createCartoonObject(" ", PyMOL.cartoon_loop_radius, 2);
      break;
    case PyMOL.REP_MESH: //   = 8;
      if (!allowSurface)
        break;
      if (isHidden)
        break; // will have to reconsider this if there is a movie, though
      jo = addJmolObject(T.isosurface, bs, null);
      jo.setSize(getFloatSetting(PyMOL.solvent_radius));
      jo.translucency = getFloatSetting(PyMOL.transparency);
      break;
    case PyMOL.REP_SURFACE: //   = 2;
      if (!allowSurface)
        break;
      if (isHidden)
        break; // will have to reconsider this if there is a movie, though
      surfaceCount++;
      jo = addJmolObject(T.isosurface, bs,
          getBooleanSetting(PyMOL.two_sided_lighting) ? "FULLYLIT" : "FRONTLIT");
      jo.setSize(getFloatSetting(PyMOL.solvent_radius));
      jo.translucency = getFloatSetting(PyMOL.transparency);
      if (surfaceColor >= 0)
        jo.argb = PyMOL.getRGB(surfaceColor);
      break;
    case PyMOL.REP_LABELS: //   = 3;
      JmolList<Text> myLabels = new JmolList<Text>();
      for (int i = 0; i < labels.size(); i++)
        myLabels.addLast(labels.get(i));
      labels.clear();
      jo = addJmolObject(JC.SHAPE_LABELS, bs, myLabels);
      break;
    case REP_JMOL_PUTTY:
      createPuttyObject(bs);
      break;
    case REP_JMOL_TRACE:
      haveTraceOrBackbone = true;
      createTraceObject(bs);
      break;
    case PyMOL.REP_BACKBONE: // ribbon
      haveTraceOrBackbone = true;
      createRibbonObject(bs);
      break;
    case PyMOL.REP_LINES:
      f = getFloatSetting(PyMOL.line_width) / 15;
      jo = addJmolObject(JC.SHAPE_STICKS, bs, null);
      jo.setSize(f);
      break;
    case PyMOL.REP_STICKS:
      f = getFloatSetting(PyMOL.stick_radius) * 2;
      jo = addJmolObject(JC.SHAPE_STICKS, bs, null);
      jo.setSize(f);
      jo.translucency = stickTranslucency;
      break;
    case PyMOL.REP_DASHES:
      // backbone dashes? Maybe an old setting
    default:
      Logger.error("Unprocessed representation type " + shapeID);
    }
  }

  /**
   * trace, or cartoon in the case of cartoon ladders.
   * 
   * @param bs
   */
  private void createTraceObject(BS bs) {
    JmolObject jo;
    BS bsNuc = BSUtil.copy(bsNucleic);
    bsNuc.and(bs);
    if (cartoonLadderMode && !bsNuc.isEmpty()) {
      haveNucleicLadder = true;
      // we will just use cartoons for ladder mode
      jo = addJmolObject(JC.SHAPE_CARTOON, bsNuc, null);
      jo.translucency = cartoonTranslucency;
      jo.setSize(getFloatSetting(PyMOL.cartoon_tube_radius) * 2);
      bs.andNot(bsNuc);
      if (bs.isEmpty())
        return;
    }
    jo = addJmolObject(JC.SHAPE_TRACE, bs, null);
    jo.translucency = cartoonTranslucency;
    jo.setSize(getFloatSetting(PyMOL.cartoon_tube_radius) * 2);
  }

  /**
   * "Putty" shapes scaled in a variety of ways.
   * 
   * @param bs
   */
  private void createPuttyObject(BS bs) {
    float[] info = new float[] { getFloatSetting(PyMOL.cartoon_putty_quality),
        getFloatSetting(PyMOL.cartoon_putty_radius),
        getFloatSetting(PyMOL.cartoon_putty_range),
        getFloatSetting(PyMOL.cartoon_putty_scale_min),
        getFloatSetting(PyMOL.cartoon_putty_scale_max),
        getFloatSetting(PyMOL.cartoon_putty_scale_power),
        getFloatSetting(PyMOL.cartoon_putty_transform) };
    addJmolObject(JC.SHAPE_TRACE, bs, info).translucency = cartoonTranslucency;
  }

  /**
   * PyMOL "ribbons" could be Jmol backbone or trace, depending upon the value
   * of PyMOL.ribbon_sampling.
   * 
   * @param bs
   */
  private void createRibbonObject(BS bs) {
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
    boolean isTrace = (sampling > 1);
    float r = getFloatSetting(PyMOL.ribbon_radius) * 2;
    float rpc = getFloatSetting(PyMOL.ray_pixel_scale);
    if (r == 0)
      r = getFloatSetting(PyMOL.ribbon_width)
          * (isTrace ? 0.1f : (rpc < 1 ? 1 : rpc) * 0.05f);
    addJmolObject((isTrace ? JC.SHAPE_TRACE : JC.SHAPE_BACKBONE), bs, null)
        .setSize(r);
  }

  private void createCartoonObject(String key, int sizeID, float factor) {
    BS bs = BSUtil.copy(ssMapAtom.get(key));
    if (bs == null)
      return;
    bs.and(reps[PyMOL.REP_CARTOON]);
    if (bs.isEmpty())
      return;
    JmolObject jo = addJmolObject(JC.SHAPE_CARTOON, bs, null);
    jo.translucency = cartoonTranslucency;
    jo.setSize(getFloatSetting(sizeID) * factor);
  }

  ////// end of molecule-specific JmolObjects //////
  
  ///// final processing /////
  
  /**
   * Create mesh or mep JmolObjects. 
   * Caching the volumeData, because it will be needed 
   * by org.jmol.jvxl.readers.PyMOLMeshReader
   * 
   */
  private void processMeshes() {
    viewer.cachePut(filePath + "#jmolSurfaceInfo", volumeData);
    for (int i = mapObjects.size(); --i >= 0;) {
      JmolList<Object> obj = mapObjects.get(i);
      String objName = obj.get(obj.size() - 1).toString();
      boolean isMep = objName.endsWith("_e_pot");
      String mapName;
      int tok;
      if (isMep) {
        // a hack? for electrostatics2.pse
        // _e_chg (surface), _e_map (volume data), _e_pot (gadget)
        tok = T.mep;
        String root = objName.substring(0, objName.length() - 3);
        mapName = root + "map";
        String isosurfaceName = branchIDs.get(root + "chg");
        if (isosurfaceName == null)
          continue;
        obj.addLast(isosurfaceName);
        mepList += ";" + isosurfaceName + ";";
      } else {
        tok = T.mesh;
        mapName = stringAt(listAt(listAt(obj, 2), 0), 1);
      }
      JmolList<Object> surface = volumeData.get(mapName);
      if (surface == null)
        continue;
      obj.addLast(mapName);
      appendLoadNote("PyMOL object " + objName + " references map " + mapName);
      volumeData.put(objName, obj);
      volumeData.put("__pymolSurfaceData__", obj);
      if (!isStateScript) {
        JmolObject jo = addJmolObject(tok, null, obj);
        if (isMep) {
        } else {
          jo.setSize(getFloatSetting(PyMOL.mesh_width));
          jo.argb = PyMOL.getRGB(intAt(listAt(obj, 0), 2));
        }
        jo.translucency = getFloatSetting(PyMOL.transparency);
      }
    }
  }


  /**
   * Create a JmolObject that will define atom sets based on PyMOL objects
   * 
   */
  private void processDefinitions() {
    addJmolObject(T.define, null, htNames);
    String s = viewer.getAtomDefs(htNames);
    if (s.length() > 2)
      s = s.substring(0, s.length() - 2);
    appendLoadNote(s);
  }

  /**
   * currently just extracts viewpoint
   * 
   * @param map
   */
  @SuppressWarnings("unchecked")
  private void processScenes(Map<String, Object> map) {
    JmolList<Object> order = getMapList(map, "scene_order");
    if (order == null || order.size() == 0)
      return;
    Map<String, Object> scenes = (Map<String, Object>) map.get("scene_dict");
    //JmolList<Object> names = getMapList(map, "names");
    for (int i = 0; i < order.size(); i++) {
      String name = stringAt(order, i);
      Map<String, Object> smap = new Hashtable<String, Object>();
      JmolList<Object> scene = getMapList(scenes, name);
      if (scene == null)
        continue;
      JmolList<Object> view = listAt(scene, 0);
      if (view == null)
        continue; // just views for now
      smap.put("pymolView", getPymolView(view, false));
/*
      smap.put("scene", scene);
      String sname = "_scene_" + name + "_";
      // get all subtypes from names (_lines, _sticks, etc.)
      for (int j = names.size(); --j >= 1;) {
        JmolList<Object> obj = listAt(names, j);
        String rname = stringAt(obj, 0);
        if (rname.charAt(0) != '_')
          continue;
        if (rname.startsWith(sname))
          smap.put(rname.substring(sname.length()), obj);
      }
      // get all colors from selector_secrets
      JmolList<Object> colors = new JmolList<Object>();
      smap.put("colors", colors);
      scene = getMapList(map, "selector_secrets");
      sname = "_!c_scene_" + name + "_";
      for (int j = scene.size(); --j >= 0;) {
        JmolList<Object> c = listAt(scene, j);
        String cname = stringAt(c, 0);
        if (cname.startsWith(sname)) {
          int color = PyMOL
              .getRGB(parseIntStr(cname.substring(sname.length())));
          colors.addLast(new Object[] { Integer.valueOf(color), c.get(1) });
        }
      }
*/      
      addJmolObject(T.scene, null, smap).branchNameID = name;
      appendLoadNote("scene: " + name);
    }
  }

  ////////////////// set the rendering ////////////////

  /**
   * Some rendering is accomplished through the jmolScript mechanism, as in
   * other readers; only executed if NOT in a state script.
   * 
   * @param view
   * 
   */

  private void setRendering(JmolList<Object> view) {
    // same idea as for a Jmol state -- session reinitializes
    viewer.initialize(true);
    viewer.setStringProperty("measurementUnits", "ANGSTROMS");
    viewer.setBooleanProperty("zoomHeight", true);
    setView(view);
    if (groups != null)
      setGroupVisibilities();
    if (!bsHidden.isEmpty())
      addJmolObject(T.hidden, bsHidden, null);
  }

  /**
   * Create group JmolObjects, and set heirarchical visibilities 
   */
  private void setGroupVisibilities() {
    Collection<PyMOLGroup> list = groups.values();
    for (PyMOLGroup g : list) {
      if (g.parent != null)
        continue;
      setGroupVisible(g, true);
    }
    addJmolObject(T.group, null, groups);
    for (int i = jmolObjects.size(); --i >= 0;) {
      JmolObject obj = jmolObjects.get(i);
      if (obj.branchNameID != null
          && occludedBranches.containsKey(obj.branchNameID))
        obj.visible = false;
    }
  }

  /**
   * Iterate through groups, setting visibility flags.
   * 
   * @param g
   * @param parentVis
   */
  private void setGroupVisible(PyMOLGroup g, boolean parentVis) {
    boolean vis = parentVis && g.visible;
    if (!vis)
      switch (g.type) {
      case BRANCH_MOLECULE:
        if (g.bsAtoms != null)
          bsHidden.or(g.bsAtoms);
        break;
      default:
        g.occluded = true;
        occludedBranches.put(g.branchNameID, Boolean.TRUE);
        break;
      }
    for (int i = g.list.size(); --i >= 0;) {
      PyMOLGroup gg = g.list.get(i);
      setGroupVisible(gg, vis);
    }
  }

  private void setView(JmolList<Object> view) {
    SB sb = new SB();
    float[] pymolView = getPymolView(view, true);
    sb.append(";set slabEnabled true;set zshadePower 1;set traceAlpha "
        + getBooleanSetting(PyMOL.cartoon_round_helices));
    sb.append(";set cartoonRockets " + cartoonRockets);
    if (cartoonRockets)
      sb.append(";set rocketBarrels " + cartoonRockets);
    sb.append(";set cartoonLadders " + haveNucleicLadder);
    sb.append(";set ribbonBorder "
        + getBooleanSetting(PyMOL.cartoon_fancy_helices));
    sb.append(";set cartoonFancy "
        + !getBooleanSetting(PyMOL.cartoon_fancy_helices));
    JmolList<Object> bg = listAt(settings, PyMOL.bg_rgb);
    String s = "000000" + Integer.toHexString(colorSetting(bg));
    s = "[x" + s.substring(s.length() - 6) + "]";
    sb.append(";background " + s);
    sb.append(";moveto 0 PyMOL " + Escape.eAF(pymolView));
    sb.append(";");
    addJmolScript(sb.toString());
  }

  /**
   * adds depth_cue, fog, and fog_start
   * 
   * @param view
   * @param isViewObj
   * @return 22-element array
   */
  private float[] getPymolView(JmolList<Object> view, boolean isViewObj) {
    float[] pymolView = new float[21];
    boolean depthCue = getBooleanSetting(PyMOL.depth_cue); // 84
    boolean fog = getBooleanSetting(PyMOL.fog); // 88
    float fog_start = getFloatSetting(PyMOL.fog_start); // 192

    int pt = 0;
    int i = 0;
    // x-axis
    for (int j = 0; j < 3; j++)
      pymolView[pt++] = floatAt(view, i++);
    if (isViewObj)
      i++;
    // y-axis
    for (int j = 0; j < 3; j++)
      pymolView[pt++] = floatAt(view, i++);
    if (isViewObj)
      i++;
    // z-axis (not used)
    for (int j = 0; j < 3; j++)
      pymolView[pt++] = floatAt(view, i++);
    if (isViewObj)
      i += 5;
    // xTrans, yTrans, -distanceToCenter, center(x,y,z), distanceToSlab, distanceToDepth
    for (int j = 0; j < 8; j++)
      pymolView[pt++] = floatAt(view, i++);

    boolean isOrtho = getBooleanSetting(PyMOL.ortho); // 23
    float fov = getFloatSetting(PyMOL.field_of_view); // 152

    pymolView[pt++] = (isOrtho ? fov : -fov);
    pymolView[pt++] = (depthCue ? 1 : 0);
    pymolView[pt++] = (fog ? 1 : 0);
    pymolView[pt++] = fog_start;
    return pymolView;
  }

  private JmolObject addJmolObject(int id, BS bsAtoms, Object info) {
    JmolObject obj = getJmolObject(id, bsAtoms, info);
    jmolObjects.addLast(obj);
    return obj;
  }

  private JmolObject getJmolObject(int id, BS bsAtoms, Object info) {
    return new JmolObject(id, branchNameID, bsAtoms, info);
  }

  public boolean haveMep(String sID) {
    return Parser.isOneOf(sID, mepList);
  }

  // local and global settings retrieval
  
  private boolean getBooleanSetting(int i) {
    return (getFloatSetting(i) != 0);
  }

  private float getFloatSetting(int i) {
    try {
      JmolList<Object> setting = null;
      if (localSettings != null)
        setting = localSettings.get(Integer.valueOf(i));
      if (setting == null)
        setting = listAt(settings, i);
      return ((Number) setting.get(2)).floatValue();
    } catch (Exception e) {
      return PyMOL.getDefaultSetting(i, pymolVersion);
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
    pointAt((JmolList) setting.get(2), 0, pt);
    Logger.info("Pymol unique setting for " + id + ": " + key + " = " + pt);
    return pt;
  }
 
  // generally useful static methods
  

  private static short getColix(int colorIndex, float translucency) {
    return C.getColixTranslucent3(C.getColixO(Integer.valueOf(PyMOL
        .getRGB(colorIndex))), translucency > 0, translucency);
  }

  private static int colorSettingClamped(JmolList<Object> c) {
    return (c.size() < 6 || intAt(c, 4) == 0 ? colorSetting(c) : getColorPt(c
        .get(5)));
  }

  @SuppressWarnings("unchecked")
  private static int getColorPt(Object o) {
    return (o instanceof Integer ? ((Integer) o).intValue() : ColorUtil
        .colorPtToInt(pointAt((JmolList<Object>) o, 0, ptTemp)));
  }
  
  private static int colorSetting(JmolList<Object> c) {
    return getColorPt(c.get(2));
  }

  private static int intAt(JmolList<Object> list, int i) {
    return ((Number) list.get(i)).intValue();
  }

  static float floatAt(JmolList<Object> list, int i) {
    return (list == null ? 0 : ((Number) list.get(i)).floatValue());
  }

  static P3 pointAt(JmolList<Object> list, int i, P3 pt) {
    pt.set(floatAt(list, i++), floatAt(list, i++), floatAt(list, i));
    return pt;
  }

  private static String stringAt(JmolList<Object> list, int i) {
    String s = list.get(i).toString();
    return (s.length() == 0 ? " " : s);
  }

  private static float[] floatsAt(JmolList<Object> a, int pt, float[] data, int len) {
    if (a == null)
      return null;
    for (int i = 0; i < len; i++)
      data[i] = floatAt(a, pt++);
    return data;
  }

  @SuppressWarnings("unchecked")
  static JmolList<Object> listAt(JmolList<Object> list, int i) {
    if (list == null || i >= list.size())
      return null;
    Object o = list.get(i);
    return (o instanceof JmolList<?> ? (JmolList<Object>) o : null);
  }

  @SuppressWarnings("unchecked")
  private static JmolList<Object> getMapList(Map<String, Object> map, String key) {
    return (JmolList<Object>) map.get(key);
  }

  private static String fixName(String name) {
    char[] chars = name.toLowerCase().toCharArray();
    for (int i = chars.length; --i >= 0;)
      if (!Character.isLetterOrDigit(chars[i]))
        chars[i] = '_';
    return String.valueOf(chars);
  }

  private static BS getBsReps(JmolList<Object> list) {
    BS bsReps = new BS();
    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (intAt(list, i) == 1)
        bsReps.set(i);
    return bsReps;
  }

  private static int getBranchType(JmolList<Object> branch) {
    return intAt(branch, 4);
  }

  private static JmolList<Object> getBranchAtoms(JmolList<Object> deepBranch) {
    return listAt(deepBranch, 7);
  }


}
