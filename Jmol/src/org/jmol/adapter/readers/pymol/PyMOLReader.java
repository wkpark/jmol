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
import org.jmol.api.PymolAtomReader;
import org.jmol.constant.EnumStructure;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BoxInfo; //import org.jmol.util.Escape;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
//import org.jmol.util.Dimension;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.P3;
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
 * trajectories are not supported yet.
 * 
 * Basic idea is as follows: 
 * 
 * 1) Pickle file is read into a Hashtable.
 * 2) Atoms, bonds, and structures are created, as per other readers, from MOLECULE objects
 * 3) Rendering of atoms and bonds is interpreted as JmolObject objects 
 * 3) Other objects such as electron density maps, compiled graphical objects, and 
 *    measures are interpreted, creating more JmolObjects
 * 3) JmolObjects are finalized after file reading takes place by a call from ModelLoader
 *    back here to finalizeModelSet(), which runs JmolObject.finalizeObject.
 * 
 * 
 *     
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 * 
 */

public class PyMOLReader extends PdbReader implements PymolAtomReader {

  private final static int OBJECT_SELECTION = -1;
  private final static int OBJECT_MOLECULE = 1;
  private final static int OBJECT_MAPDATA = 2;
  private final static int OBJECT_MAPMESH = 3;
  private final static int OBJECT_MEASURE = 4;
  private final static int OBJECT_CALLBACK = 5;
  private final static int OBJECT_CGO = 6; // compiled graphics object
  private final static int OBJECT_SURFACE = 7;
  private final static int OBJECT_GADGET = 8;
  private final static int OBJECT_CALCULATOR = 9;
  private final static int OBJECT_SLICE = 10;
  private final static int OBJECT_ALIGNMENT = 11;
  private final static int OBJECT_GROUP = 12;

  private static final int MIN_RESNO = -1000; // minimum allowed residue number

  private static String nucleic = " A C G T U ADE THY CYT GUA URI DA DC DG DT DU ";

  private boolean allowSurface = true;
  private boolean doResize = false;
  boolean doCache = false;

  private JmolList<Object> settings;

  private int atomCount0;
  private int atomCount;
  private int stateCount;
  private int structureCount;

  private boolean isHidden;

  private BS bsStructureDefined = new BS();
  private BS bsExcluded;

  private int[] atomMap;
  private Map<String, BS> ssMapAtom, ssMapSeq;

  //private Map<String, int[]> htAtomMap = new Hashtable<String, int[]>();

  private JmolList<Integer> atomColorList = new JmolList<Integer>();
  private JmolObject frameObj;
  private PyMOLScene pymolScene;

  private short[] colixes;
  private boolean isStateScript;

  private P3 xyzMin = P3.new3(1e6f, 1e6f, 1e6f);
  private P3 xyzMax = P3.new3(-1e6f, -1e6f, -1e6f);

  private int nModels;
  private boolean logging;

  private BS[] reps = new BS[PyMOL.REP_JMOL_MAX];

  private boolean isMovie;

  private Map<String, Object> htNames = new Hashtable<String, Object>();
  private int pymolFrame, pymolState;
  private boolean allStates;
  private int totalAtomCount;
  private int pymolVersion;
  private P3[] trajectoryStep;
  private int trajectoryPtr;

  private String objectName;
  private String objectNameID;

  //private JmolList<JmolList<Object>> selections;
  private Map<String, JmolList<Object>> volumeData;
  private JmolList<JmolList<Object>> mapObjects;
  private Map<String, String> objectIDs = new Hashtable<String, String>();
  private boolean haveMeasurements;
  private int[] frames;
  private String mepList = "";
  private Hashtable<Integer, JmolList<Object>> uniqueSettings;
  private Atom[] atoms;

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
  protected void setAdditionalAtomParameters(Atom atom) {
    // override PDBReader settings
  }

  /**
   * At the end of the day, we need to finalize all the JmolObjects, set the
   * trajectories, and, if filtered with DOCACHE, create a streamlined binary
   * file for inclusion in the PNGJ file.
   * 
   */
  @Override
  public void finalizeModelSet(ModelSet modelSet, int baseModelIndex,
                               int baseAtomIndex) {

    pymolScene.setObjects(modelSet, mepList, doCache, baseModelIndex, baseAtomIndex);
    if (haveMeasurements) {
      appendLoadNote(viewer.getMeasurementInfoAsString());
      setLoadNote();
    }
    viewer.setTrajectoryBs(BSUtil.newBitSet2(baseModelIndex,
        modelSet.modelCount));
    //if (baseModelIndex == 0)
    //viewer.setBooleanProperty("_ismovie", true);
    if (!isStateScript && frameObj != null) {
      frameObj.finalizeObject(modelSet, null, false);
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
    pymolScene = new PyMOLScene(this, viewer, settings, uniqueSettings, pymolVersion);

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
    addColors(getMapList(map, "colors"), pymolScene.globalSetting(PyMOL.clamp_colors) != 0);
    
    // set a few global flags
    allStates = (pymolScene.globalSetting(PyMOL.all_states) != 0);
    pymolFrame = (int) pymolScene.globalSetting(PyMOL.frame);
    pymolState = (int) pymolScene.globalSetting(PyMOL.state);
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
    //selections = new JmolList<JmolList<Object>>();

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
        processObject(listAt(names, i), true, j);
    }
    for (int i = 1; i < names.size(); i++)
      processObject(listAt(names, i), false, 0);
    pymolScene.setObjectInfo(null, null, false, null);
    objectNameID = null;
    
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
   * This will be used later in processing molecule objects.
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
          int uid = (id << 10) + intAt(setting, 0);
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
      PyMOL.addColor((Integer) c.get(1), isClamped ? PyMOLScene.colorSettingClamped(c)
          : PyMOLScene.colorSetting(c));
    }
  }

  /**
   * Look through all named objects for molecules, counting
   * atoms and also states; see if trajectories are compatible (experimental).
   *  
   * @param names
   */
  private void getAtomAndStateCount(JmolList<Object> names) {
    int n = 0;
    for (int i = 1; i < names.size(); i++) {
      JmolList<Object> execObject = listAt(names, i);
      int type = intAt(execObject, 4);
      if (!checkObject(execObject))
        continue;
      if (type == OBJECT_MOLECULE) {
        JmolList<Object> pymolObject = listAt(execObject, 5);
        JmolList<Object> states = listAt(pymolObject, 4);
        int ns = states.size();
        if (ns > stateCount)
          stateCount = ns;
        int nAtoms = listAt(pymolObject, 7).size();
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

  private boolean checkObject(JmolList<Object> execObject) {
    objectName = stringAt(execObject, 0);
    isHidden = (intAt(execObject, 2) != 1);
    return (objectName.indexOf("_") != 0);
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
      pymolScene.setObjectInfo(null, null, false, null);
      frameObj = pymolScene.getJmolObject(T.movie, null, movie);
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
   * The main object processor. 
   * Not implemented: ALIGNMENT, CALLBACK, SLICE, SURFACE 
   * 
   * @param execObject
   * @param moleculeOnly
   * @param iState
   */
  @SuppressWarnings("unchecked")
  private void processObject(JmolList<Object> execObject, boolean moleculeOnly,
                             int iState) {
    int type = intAt(execObject, 4);
    JmolList<Object> startLen = (JmolList<Object>) execObject
        .get(execObject.size() - 1);
    if ((type == OBJECT_MOLECULE) != moleculeOnly || !checkObject(execObject))
      return;
    Logger.info("PyMOL model " + (nModels) + " Object " + objectName
        + (isHidden ? " (hidden)" : " (visible)"));
    JmolList<Object> pymolObject = listAt(execObject, 5);
    objectNameID = fixName(objectName
        + (moleculeOnly ? "_" + (iState + 1) : ""));
    objectIDs.put(objectName, objectNameID);
    System.out.println("processObj " + objectNameID);
    String msg = null;
    JmolList<Object> objectHeader = listAt(pymolObject, 0);
    pymolScene.setObjectInfo(objectName, objectNameID, isHidden, listAt(objectHeader, 8));
    boolean doGroups = !isStateScript;
    String parentGroupName = (!doGroups || execObject.size() < 8 ? null : stringAt(
        execObject, 6));
    BS bsAtoms = null;
    boolean doExclude = (bsExcluded != null);

    switch (type) {
    default:
      msg = "" + type;
      break;
    case OBJECT_SELECTION:
      // not treating these properly yet
      //selections.addLast(execObject);
      break;
    case OBJECT_MOLECULE:
      doExclude = false;
      bsAtoms = processMolecule(pymolObject, iState);
      break;
    case OBJECT_MEASURE:
      doExclude = false;
      processMeasure(pymolObject);
      break;
    case OBJECT_MAPMESH:
    case OBJECT_MAPDATA:
      processMap(pymolObject, type == OBJECT_MAPMESH);
      break;
    case OBJECT_GADGET:
      processGadget(pymolObject);
      break;
    case OBJECT_GROUP:
      if (objectName.startsWith("AllLig"))
      System.out.println(objectName);
      if (doGroups)
        pymolScene.addGroup(execObject, null, type);
      break;
    case OBJECT_CGO:
      msg = "CGO";
      processCGO(pymolObject);
      break;

      // unimplemented:
      
    case OBJECT_ALIGNMENT:
      msg = "ALIGNEMENT";
      break;
    case OBJECT_CALCULATOR:
      msg = "CALCULATOR";
      break;
    case OBJECT_CALLBACK:
      msg = "CALLBACK";
      break;
    case OBJECT_SLICE:
      msg = "SLICE";
      break;
    case OBJECT_SURFACE:
      msg = "SURFACE";
      break;
    }
    if (parentGroupName != null) {
      PyMOLGroup group = pymolScene.addGroup(execObject, parentGroupName, type);
      if (bsAtoms != null)
        pymolScene.addGroupAtoms(group, bsAtoms);
    }
    if (doExclude) {
      int i0 = intAt(startLen, 0);
      int len = intAt(startLen, 1);
      bsExcluded.setBits(i0, i0 + len);
      Logger.info("cached PSE file excludes PyMOL object type " + type
          + " name=" + objectName + " len=" + len);
    }
    if (msg != null)
      Logger.error("Unprocessed object type " + msg + " " + objectName);
  }

  /**
   * Create a CGO JmolObject, just passing on key information. 
   * 
   * @param pymolObject
   */
  private void processCGO(JmolList<Object> pymolObject) {
    if (isStateScript)
      return;
    if (isHidden)
      return;
    int color = intAt(listAt(pymolObject, 0), 2);
    JmolList<Object> data = listAt(listAt(pymolObject, 2), 0);
    data.addLast(objectName);
    JmolObject jo = addJmolObject(JC.SHAPE_CGO, null, data);
    jo.argb = PyMOL.getRGB(color);
    jo.translucency = pymolScene.floatSetting(PyMOL.cgo_transparency);
    appendLoadNote("CGO " + fixName(objectName));
  }

  /**
   * Only process _e_pot objects -- which we need for color settings 
   * @param pymolObject
   */
  private void processGadget(JmolList<Object> pymolObject) {
    if (objectName.endsWith("_e_pot"))
      processMap(pymolObject, true);
  }

  /**
   * Create mapObjects and volumeData; create an ISOSURFACE JmolObject.
   * 
   * @param pymolObject
   * @param isObject
   */
  private void processMap(JmolList<Object> pymolObject, boolean isObject) {
    if (isObject) {
      if (isStateScript)
        return;
      if (isHidden)
        return; // for now
      if (mapObjects == null)
        mapObjects = new JmolList<JmolList<Object>>();
      mapObjects.addLast(pymolObject);
    } else {
      if (volumeData == null)
        volumeData = new Hashtable<String, JmolList<Object>>();
      volumeData.put(objectName, pymolObject);
      if (!isHidden && !isStateScript)
        addJmolObject(T.isosurface, null, objectName);
    }
    pymolObject.addLast(objectName);
  }

//  private void processSelections() {
//    for (int i = selections.size(); --i >= 0;) {
//      JmolList<Object> execObject = selections.get(i);
//      checkObject(execObject);
//      processObjectSelection(listAt(execObject, 5));
//    }
//  }
////  [Ca, 1, 0, 
////   [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], -1, 
////   [
////   [H115W, 
////   [2529, 2707], 
////   [1, 1]]], ]
//  private void processObjectSelection(JmolList<Object> pymolObject) {
//    BS bs = new BS();
//    for (int j = pymolObject.size(); --j >= 0;) {
//      JmolList<Object> data = listAt(pymolObject, j);
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
//      htNames.put(objectName, bs);
//  }


  /**
   * Create a MEASURE JmolObject.
   * 
   * @param pymolObject
   */
  private void processMeasure(JmolList<Object> pymolObject) {
    if (isStateScript)
      return;
    if (isHidden)
      return; // will have to reconsider this if there is a movie, though
    Logger.info("PyMOL measure " + objectName);
    JmolList<Object> measure = listAt(listAt(pymolObject, 2), 0);
    int pt;
    int nCoord = (measure.get(pt = 1) instanceof JmolList<?> ? 2 : measure
        .get(pt = 4) instanceof JmolList<?> ? 3
        : measure.get(pt = 6) instanceof JmolList<?> ? 4 : 0);
    if (nCoord == 0)
      return;
    JmolList<Object> setting = listAt(pymolObject, 0);
    BS bsReps = getBsReps(listAt(setting, 3));
    JmolList<Object> list = listAt(measure, pt);
    JmolList<Object> offsets = listAt(measure, 8);
    boolean haveLabels = (measure.size() > 8);
    int color = intAt(setting, 2);
    if (pymolScene.addMeasurements(nCoord, list, bsReps, color, offsets, haveLabels))
      haveMeasurements = true;    
  }

  /**
   * Create everything necessary to generate a molecule in Jmol. 
   * 
   * @param pymolObject
   * @param iState
   * @return atom set only if this is a trajectory.
   */
  private BS processMolecule(JmolList<Object> pymolObject, int iState) {
    atomCount = atomCount0 = atomSetCollection.getAtomCount();
    int nAtoms = intAt(pymolObject, 3);
    if (nAtoms == 0)
      return null;
    ssMapSeq = new Hashtable<String, BS>();
    ssMapAtom = new Hashtable<String, BS>();
    atomMap = new int[nAtoms];
    //htAtomMap.put(objectName, atomMap); -- ah, but what about different states?
    if (iState == 0)
      processMolCryst(listAt(pymolObject, 10));
    JmolList<Bond> bonds = processMolBonds(listAt(pymolObject, 6));
    JmolList<Object> states = listAt(pymolObject, 4);
    JmolList<Object> pymolAtoms = listAt(pymolObject, 7);
    String fname = "__" + fixName(objectName);
    BS bsAtoms = (BS) htNames.get(fname);
    if (bsAtoms == null) {
      bsAtoms = BS.newN(atomCount0 + nAtoms);
      Logger.info("PyMOL molecule " + objectName);
      htNames.put(fname, bsAtoms);
    }
    for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
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
    int color = (int) pymolScene.floatSetting(PyMOL.stick_color);
    float radius = pymolScene.floatSetting(PyMOL.stick_radius);
    float translucency = pymolScene.floatSetting(PyMOL.stick_transparency);
    boolean valence = pymolScene.booleanSetting(PyMOL.valence);
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
        rad = pymolScene.getUniqueFloatDef(id, PyMOL.stick_radius, radius);
        c = (int) pymolScene.getUniqueFloatDef(id, PyMOL.stick_color, color);
        t = pymolScene.getUniqueFloatDef(id, PyMOL.stick_transparency, translucency);
      } else {
        rad = radius;
        c = color;
        t = translucency;
      }
      // I think rad is being ignored, because later we set it with a script
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
                     JmolList<Object> coords,
                     JmolList<Object> labelPositions, BS bsState, int iState) {
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
    Atom atom = processAtom(new Atom(), name, altLoc
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
    boolean isNucleic = (nucleic.indexOf(group3) >= 0);
    if (bsState != null)
      bsState.set(atomCount);

    String label = stringAt(a, 9);      
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
    
    BS bsReps = getBsReps(listAt(a, 20));
    int atomColor = intAt(a, 21);
    int serNo = intAt(a, 22);
    int cartoonType = intAt(a, 23);
    int flags = intAt(a, 24);
    boolean bonded = (intAt(a, 25) != 0);
    int uniqueID = (a.size() > 40 && intAt(a, 40) == 1 ? intAt(a, 32) : -1);
    atom.vectorX = uniqueID;
    atom.vectorY = cartoonType;
    if (a.size() > 46) {
      float[] data = PyMOLScene.floatsAt(a, 41, new float[7], 6);
      atomSetCollection.setAnisoBorU(atom, data, 12);
    }
    float translucency = pymolScene.getUniqueFloatDef(uniqueID,
        PyMOL.sphere_transparency, pymolScene.sphereTranslucency);
    atomColorList.addLast(Integer.valueOf(pymolScene.getColix(atomColor, translucency)));
    processAtom2(atom, serNo, x, y, z, formalCharge);
    
    // set pymolScene bit sets and create labels
    
    if (!bonded)
      pymolScene.bsNonbonded.set(atomCount);
    if (!label.equals(" ")) {
      pymolScene.bsLabeled.set(atomCount);
      JmolList<Object> labelOffset = listAt(labelPositions, apt);
      pymolScene.addLabel(atomCount, uniqueID, atomColor, labelOffset, label);
    }
    if (isHidden)
    pymolScene.bsHidden.set(atomCount);
    if (isNucleic)
      pymolScene.bsNucleic.set(atomCount);
    for (int i = 0; i < PyMOL.REP_MAX; i++)
      if (bsReps.get(i))
        reps[i].set(atomCount);
    if (atom.elementSymbol.equals("H"))
      pymolScene.bsHydrogen.set(atomCount);
    if ((flags & PyMOL.FLAG_NOSURFACE) != 0)
      pymolScene.bsNoSurface.set(atomCount);
    atomMap[apt] = atomCount++;
    return null;
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
    atoms = atomSetCollection.getAtoms();
    pymolScene.createShapeObjects(reps, ssMapAtom, allowSurface && !isHidden, atomCount0, atomCount);
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
        String isosurfaceName = objectIDs.get(root + "chg");
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
          jo.setSize(pymolScene.globalSetting(PyMOL.mesh_width));
          jo.argb = PyMOL.getRGB(intAt(listAt(obj, 0), 2));
        }
        jo.translucency = pymolScene.globalSetting(PyMOL.transparency);
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
   * A PyMOL scene consists of one or more of:
   *   view
   *   frame
   *   visibilities, by object
   *   colors, by color
   *   reps, by type
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
    //finalizeSceneData();
    //JmolList<Object> names = getMapList(map, "names");
    for (int i = 0; i < order.size(); i++) {
      String name = stringAt(order, i);
      Map<String, Object> smap = new Hashtable<String, Object>();
      JmolList<Object> thisScene = getMapList(scenes, name);
      if (thisScene == null)
        continue;
      JmolList<Object> view = listAt(thisScene, 0);
      Object frame = thisScene.get(2);
      if (frame != null)
        smap.put("pymolFrame", frame);
      if (view != null)
        smap.put("pymolView", pymolScene.getPymolView(view, false));
      if (view == null)
        continue; // just views for now
/*
      smap.put("settings", settings);
      smap.put("uniqueSettings", uniqueSettings);
      // will need htAtomMap for this as well
      smap.put("scene", pymolScene);
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
      JmolList<Object> secrets = getMapList(map, "selector_secrets");
      sname = "_!c_scene_" + name + "_";
      for (int j = secrets.size(); --j >= 0;) {
        JmolList<Object> c = listAt(secrets, j);
        String cname = stringAt(c, 0);
        if (cname.startsWith(sname)) {
          int color = PyMOL
              .getRGB(parseIntStr(cname.substring(sname.length())));
          colors.addLast(new Object[] { Integer.valueOf(color), c.get(1) });
        }
      }
*/      
      addJmolObject(T.scene, null, smap).objectNameID = name;
      appendLoadNote("scene: " + name);
    }
  }

  ////////////////// set the rendering ////////////////

//  /**
//   * Make sure atom uniqueID (vectorX) and cartoonType (vectorY) are made
//   * permanent
//   */
//  private void finalizeSceneData() {
//    int[] cartoonTypes = new int[atomCount];
//    int[] uniqueIDs = new int[atomCount];
//    int[] sequenceNumbers = new int[atomCount];
//    boolean[] newChain = new boolean[atomCount];
//    float[] radii = new float[atomCount];
//    int lastAtomChain = Integer.MIN_VALUE;
//    int lastAtomSet = Integer.MIN_VALUE;
//    for (int i = 0; i < atomCount; i++) {
//      cartoonTypes[i] = getCartoonType(i);
//      uniqueIDs[i] = getUniqueID(i);
//      sequenceNumbers[i] = getSequenceNumber(i);
//      radii[i] = getVDW(i);
//      if (lastAtomChain != atoms[i].chainID || lastAtomSet != atoms[i].atomSetIndex) {
//        newChain[i] = true;
//        lastAtomChain = atoms[i].chainID;
//        lastAtomSet = atoms[i].atomSetIndex;
//      }
//    }
//    pymolScene.setAtomInfo(uniqueIDs, cartoonTypes, sequenceNumbers, newChain, radii);    
//  }

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
    pymolScene.setGroupVisibilities();
    addJmolScript(getViewScript(view).toString());
  }

  private SB getViewScript(JmolList<Object> view) {
    SB sb = new SB();
    float[] pymolView = pymolScene.getPymolView(view, true);
    sb.append(";set zshadePower 1;set traceAlpha "
        + (pymolScene.globalSetting(PyMOL.cartoon_round_helices) != 0));
    boolean rockets = pymolScene.cartoonRockets;
    sb.append(";set cartoonRockets " + rockets);
    if (rockets)
      sb.append(";set rocketBarrels " + rockets);
    sb.append(";set cartoonLadders " + pymolScene.haveNucleicLadder);
    sb.append(";set ribbonBorder "
        + (pymolScene.globalSetting(PyMOL.cartoon_fancy_helices) != 0));
    sb.append(";set cartoonFancy "
        + (pymolScene.globalSetting(PyMOL.cartoon_fancy_helices) == 0));
    String s = "000000" + Integer.toHexString(pymolScene.bgRgb);
    s = "[x" + s.substring(s.length() - 6) + "]";
    sb.append(";background " + s);
    sb.append(";moveto 0 PyMOL " + Escape.eAF(pymolView));
    sb.append(";");
    return sb;
  }

  private JmolObject addJmolObject(int id, BS bsAtoms, Object info) {
    return pymolScene.addJmolObject(id, bsAtoms, info);
  }

  // generally useful static methods

  private static int intAt(JmolList<Object> list, int i) {
    return ((Number) list.get(i)).intValue();
  }

  private static String stringAt(JmolList<Object> list, int i) {
    String s = list.get(i).toString();
    return (s.length() == 0 ? " " : s);
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

  private float floatAt(JmolList<Object> a, int i) {
    return PyMOLScene.floatAt(a, i);
  }

  private JmolList<Object> listAt(JmolList<Object> list, int i) {
    return PyMOLScene.listAt(list, i);
  }

  
  /// PymolAtomReader interface
  
  public int getUniqueID(int iAtom) {
    return (int) atoms[iAtom].vectorX;
  }

  public int getCartoonType(int iAtom) {
    return (int) atoms[iAtom].vectorY;
  }

  public float getVDW(int iAtom) {
    return atoms[iAtom].radius;
  }

  public int getSequenceNumber(int iAtom) {
    return atoms[iAtom].sequenceNumber;
  }

  public boolean compareAtoms(int iPrev, int i) {
    return atoms[iPrev].chainID != atoms[i].chainID;
  }



}
