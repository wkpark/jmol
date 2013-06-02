package org.jmol.adapter.readers.pymol;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.JmolSceneGenerator;
import org.jmol.api.PymolAtomReader;
import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.Escape;
import org.jmol.util.JmolFont;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.SB;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * A class to allow manipulation of scenes dissociated from file loading. A
 * "scene" in this context is a distillation of PyMOL information into a
 * Hashtable for easier retrieval. This organization is:
 * 
 */
class PyMOLScene implements JmolSceneGenerator {

  private Viewer viewer;
  private int pymolVersion;

  // filled by PyMOLReader; used to generate the scene
  BS bsHidden = new BS();
  BS bsNucleic = new BS();
  BS bsNonbonded = new BS();
  BS bsLabeled = new BS();
  BS bsHydrogen = new BS();
  BS bsNoSurface = new BS();

  // private -- only needed for file reading

  private Map<Float, BS> htSpacefill = new Hashtable<Float, BS>();
  private Map<String, BS> ssMapAtom = new Hashtable<String, BS>();
  private JmolList<Integer> atomColorList = new JmolList<Integer>();
  private Map<String, Boolean> occludedObjects = new Hashtable<String, Boolean>();
  private Map<Integer, Text> labels = new Hashtable<Integer, Text>();
  private short[] colixes;
  private JmolObject frameObj;
  private Map<String, PyMOLGroup> groups;
  private Map<Integer, JmolList<Object>> objectSettings;

  private void clearReaderData(boolean isAll) {
    reader = null;
    colixes = null;
    atomColorList = null;
    objectSettings = null;
    if (!isAll)
      return;
    groups = null;
    labels = null;
    htSpacefill = null;
    ssMapAtom = null;
    htAtomMap = null;
    htMeasures = null;
    htObjectGroups = null;
    htObjectAtoms = null;
    htObjectSettings = null;
    objectInfo = null;
    settings = null;
    uniqueSettings = null;
    occludedObjects = null;
    htHiddenObjects = null;
    bsHidden = bsNucleic = bsNonbonded = bsLabeled = bsHydrogen = bsNoSurface = bsCartoon = null;
  }

  // private -- needed for processing Scenes

  private BS bsCartoon = new BS();
  private Map<String, BS> htCarveSets = new Hashtable<String, BS>();
  private Map<String, BS> htDefinedAtoms = new Hashtable<String, BS>();
  private Map<String, Boolean> htHiddenObjects = new Hashtable<String, Boolean>();
  private JmolList<String> moleculeNames = new JmolList<String>();
  private JmolList<JmolObject> jmolObjects = new JmolList<JmolObject>();
  private Map<String, int[]> htAtomMap = new Hashtable<String, int[]>();
  private Map<String, BS> htObjectAtoms = new Hashtable<String, BS>();
  private Map<String, String> htObjectGroups = new Hashtable<String, String>();
  private Map<String, MeasurementData[]> htMeasures = new Hashtable<String, MeasurementData[]>();
  private Map<String, Map<Integer, JmolList<Object>>> htObjectSettings = new Hashtable<String, Map<Integer, JmolList<Object>>>();
  private Map<String, Object[]> objectInfo = new Hashtable<String, Object[]>();
  private JmolList<Object> settings;
  private Map<Integer, JmolList<Object>> uniqueSettings;

  private int bgRgb;
  private int surfaceMode;
  private int surfaceColor;
  private int labelFontId;
  private int labelColor;
  private float labelSize;
  private float sphereTranslucency;
  private float cartoonTranslucency;
  private float stickTranslucency;
  private float nonBondedSize;
  private float sphereScale;
  private boolean cartoonLadderMode;
  private boolean cartoonRockets;
  private boolean haveNucleicLadder;
  private P3 labelPosition;
  private P3 labelPosition0 = new P3();

  private String objectName;
  private String objectNameID;
  private String objectSelectionName;
  private int objectType;
  private BS objectAtoms;
  private boolean objectHidden;

  // during file loading we have a reader, but after that we must rely on data saved by the server

  private PymolAtomReader reader;
  private int[] uniqueIDs;
  private int[] cartoonTypes;
  private int[] sequenceNumbers;
  private boolean[] newChain;
  private float[] radii;

  private int baseModelIndex;
  private int baseAtomIndex;
  private int stateCount;

  private String mepList;

  private boolean doCache;
  private boolean haveScenes;
  private BS bsCarve;
  private boolean solventAccessible;

  void setStateCount(int stateCount) {
    this.stateCount = stateCount;
  }

  @SuppressWarnings("unchecked")
  PyMOLScene(PymolAtomReader reader, Viewer viewer, JmolList<Object> settings,
      Map<Integer, JmolList<Object>> uniqueSettings, int pymolVersion) {
    this.reader = reader;
    this.viewer = viewer;
    this.settings = settings;
    this.uniqueSettings = uniqueSettings;
    this.pymolVersion = pymolVersion;
    setVersionSettings();
    bgRgb = colorSetting(listAt(settings, PyMOL.bg_rgb));
    pointAt((JmolList<Object>) listAt(settings, PyMOL.label_position).get(2),
        0, labelPosition0);
  }

  @SuppressWarnings("unchecked")
  void setObjectInfo(String name, int type, String groupName, boolean isHidden,
                     JmolList<Object> list, String ext) {
    objectName = name;
    objectHidden = isHidden;
    objectNameID = (objectName == null ? null : PyMOLScene.fixName(objectName
        + ext));
    objectSettings = new Hashtable<Integer, JmolList<Object>>();
    if (objectName != null) {
      objectSelectionName = getSelectionName(name);
      if (groupName != null) {
        htObjectGroups.put(objectName, groupName);
        htObjectGroups.put(objectNameID, groupName);
      }
      objectInfo.put(objectName, new Object[] { objectNameID,
          Integer.valueOf(type) });
      htObjectSettings.put(objectName, objectSettings);
      if (list != null && list.size() != 0) {
        if (Logger.debugging)
          Logger.info(objectName + " local settings: " + list.toString());
        for (int i = list.size(); --i >= 0;) {
          JmolList<Object> setting = (JmolList<Object>) list.get(i);
          objectSettings.put((Integer) setting.get(0), setting);
        }
      }
    }
    getObjectSettings();
  }

  private void getObjectSettings() {
    nonBondedSize = floatSetting(PyMOL.nonbonded_size);
    sphereScale = floatSetting(PyMOL.sphere_scale);
    cartoonTranslucency = floatSetting(PyMOL.cartoon_transparency);
    stickTranslucency = floatSetting(PyMOL.stick_transparency);
    sphereTranslucency = floatSetting(PyMOL.sphere_transparency);
    cartoonLadderMode = booleanSetting(PyMOL.cartoon_ladder_mode);
    cartoonRockets = booleanSetting(PyMOL.cartoon_cylindrical_helices);
    surfaceMode = (int) floatSetting(PyMOL.surface_mode);
    surfaceColor = (int) floatSetting(PyMOL.surface_color);
    solventAccessible = booleanSetting(PyMOL.surface_solvent);
    String carveSet = stringSetting(PyMOL.surface_carve_selection).trim();
    if (carveSet.length() == 0) {
      bsCarve = null;
    } else {
      bsCarve = htCarveSets.get(carveSet);
      if (bsCarve == null)
        htCarveSets.put(carveSet, bsCarve = new BS());      
    }

    //solventAsSpheres = getBooleanSetting(PyMOL.sphere_solvent); - this is for SA-Surfaces
    labelPosition = new P3();
    try {
      JmolList<Object> setting = getObjectSetting(PyMOL.label_position);
      pointAt(listAt(setting, 2), 0, labelPosition);
    } catch (Exception e) {
      // no problem.
    }
    labelPosition.add(labelPosition0);
    labelColor = (int) floatSetting(PyMOL.label_color);
    labelSize = floatSetting(PyMOL.label_size);
    labelFontId = (int) floatSetting(PyMOL.label_font_id);
  }

  void setAtomInfo(int[] uniqueIDs, int[] cartoonTypes, int[] sequenceNumbers,
                   boolean newChain[], float radii[]) {
    this.uniqueIDs = uniqueIDs;
    this.cartoonTypes = cartoonTypes;
    this.sequenceNumbers = sequenceNumbers;
    this.newChain = newChain;
    this.radii = radii;
  }

  /**
   * 
   * @param name
   * @param thisScene
   * @param htObjNames
   * @param htSecrets
   */
  @SuppressWarnings("unchecked")
  void buildScene(String name, JmolList<Object> thisScene,
                  Map<String, JmolList<Object>> htObjNames,
                  Map<String, JmolList<Object>> htSecrets) {

    // generator : this
    // name : scene name
    // pymolFrame : specified frame
    // pymolView : specified view [ 18-member array ]
    // visibilities: { name1: [visFlag, repOn, repVis, colorIndex],...}
    // moleculeReps: [ representation-based list of object lists]
    // colors: [colorIndex, object list]
    // 
    Map<String, Object> smap = new Hashtable<String, Object>();
    smap.put("generator", this);
    smap.put("name", name);
    JmolList<Object> view = listAt(thisScene, 0);
    Object frame = thisScene.get(2);
    if (frame != null)
      smap.put("pymolFrame", frame);
    if (view != null)
      smap.put("pymolView", getPymolView(view, false));

    // get the overall object visibilities:
    //   {name : [ visFlag, repOn, objVis, color ], ...}
    // As far as I can tell, repOn is not useful, and objVis
    // is only used for measurements.

    Map<String, Object> visibilities = (Map<String, Object>) thisScene.get(1);
    smap.put("visibilities", visibilities);

    // get all subtypes from names (_lines, _sticks, etc.)
    String sname = "_scene_" + name + "_";
    Object[] reps = new Object[PyMOL.REP_LIST.length];
    for (int j = PyMOL.REP_LIST.length; --j >= 0;) {
      JmolList<Object> list = htObjNames.get(sname + PyMOL.REP_LIST[j]);
      JmolList<Object> data = listAt(list, 5);
      if (data != null && data.size() > 0)
        reps[j] = listToMap(data);
    }
    smap.put("moleculeReps", reps);

    // there's no real point in getting 
    // get all colors from selector_secrets
    sname = "_!c_" + name + "_";
    JmolList<Object> colorection = listAt(thisScene, 3);
    int n = colorection.size();
    // I don't know what the idea of the pyList is that it is bivalued:
    // [3, 262, 0, 263, 4, 264, 26, 265, 27, 266, 28, 267]
    Object[] colors = new Object[n / 2];
    for (int j = 0, i = 0; j < n; j += 2) {
      int color = intAt(colorection, j);
      JmolList<Object> c = htSecrets.get(sname + color);
      if (c != null && c.size() > 1)
        colors[i++] = new Object[] { Integer.valueOf(color), c.get(1) };
    }
    smap.put("colors", colors);
    addJmolObject(T.scene, null, smap).objectNameID = name;
  }

  /**
   * Generate the saved scene.
   * 
   * @param scene
   * 
   */
  @SuppressWarnings("unchecked")
  public void generateScene(Map<String, Object> scene) {
    Logger.info("PyMOLScene - generateScene " + scene.get("name"));
    // generateVisibities();
    jmolObjects.clear();
    bsHidden.clearAll();
    occludedObjects.clear();
    htHiddenObjects.clear();
    Integer frame = (Integer) scene.get("pymolFrame");
    if (frame != null)
      addJmolObject(T.frame, null, Integer.valueOf(frame.intValue() - 1));
    try {
      int istate = (frame == null ? 0 : frame.intValue());
      generateVisibilities((Map<String, Object>) scene.get("visibilities"),
          istate);
      generateColors((Object[]) scene.get("colors"), istate);
      generateShapes((Object[]) scene.get("moleculeReps"), istate);
      finalizeEverything();
      finalizeObjects();
    } catch (Exception e) {
      System.out.println("PyMOLScene exception " + e);
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  private void generateColors(Object[] colors, int istate) {
    if (colors == null)
      return;
    // note that colors are for ALL STATES
    for (int i = colors.length; --i >= 0;) {
      Object[] item = (Object[]) colors[i];
      int color = ((Integer) item[0]).intValue();
      int icolor = PyMOL.getRGB(color);
      JmolList<Object> molecules = (JmolList<Object>) item[1];
      BS bs = getSelectionAtoms(molecules, istate, new BS());
      addJmolObject(JC.SHAPE_BALLS, bs, null).argb = icolor;
    }
  }

  private BS getSelectionAtoms(JmolList<Object> molecules, int istate, BS bs) {
    if (molecules != null)
      for (int j = molecules.size(); --j >= 0;)
        selectAllAtoms(listAt(molecules, j), istate, bs);
    return bs;
  }

  private void selectAllAtoms(JmolList<Object> obj, int istate, BS bs) {
    String name = (String) obj.get(0);
    setObject(name, istate);
    JmolList<Object> atomList = listAt(obj, 1);
    int k0 = (istate == 0 ? 1 : istate);
    int k1 = (istate == 0 ? stateCount : istate + 1);
    for (int k = k0; k <= k1; k++) {
      int[] atomMap = htAtomMap.get(fixName(name + "_" + k));
      if (atomMap == null)
        continue;
      getBsAtoms(atomList, atomMap, bs);
    }
  }

  @SuppressWarnings("unchecked")
  private void generateVisibilities(Map<String, Object> vis, int istate) {
    if (vis == null)
      return;
    BS bs = new BS();
    addJmolObject(T.hide, null, null);
    for (Entry<String, PyMOLGroup> e : groups.entrySet())
      e.getValue().visible = true;
    for (Entry<String, Object> e : vis.entrySet()) {
      String name = e.getKey();
      if (name.equals("all"))
        continue;
      JmolList<Object> list = (JmolList<Object>) e.getValue();
      int tok = (intAt(list, 0) == 1 ? T.display : T.hide);
      if (tok == T.hide)
        htHiddenObjects.put(name, Boolean.TRUE);
      switch (getObjectType(name)) {
      case PyMOL.OBJECT_GROUP:
        PyMOLGroup g = groups.get(name);
        if (g != null)
          g.visible = (tok == T.display);
        break;
      }
    }
    setGroupVisibilities();
    for (Entry<String, Object> e : vis.entrySet()) {
      String name = e.getKey();
      if (name.equals("all"))
        continue;
      setObject(name, istate);
      if (objectHidden)
        continue;
      JmolList<Object> list = (JmolList<Object>) e.getValue();
      int tok = (objectHidden ? T.hide : T.display);
      bs = null;
      String info = objectSelectionName;
      switch (objectType) {
      case PyMOL.OBJECT_MOLECULE:
        bs = viewer.getDefinedAtomSet(info);
        if (bs.nextSetBit(0) < 0)
          continue;
        break;
      case PyMOL.OBJECT_GROUP:
        continue;
      case PyMOL.OBJECT_MEASURE:
        if (tok == T.display) {
          MeasurementData[] mdList = htMeasures.get(name);
          if (mdList != null)
            addMeasurements(mdList, mdList[0].points.size(), null,
                getBS(listAt(list, 2)), intAt(list, 3), null, true);
        }
        info += "_*";
        break;
      case PyMOL.OBJECT_CGO:
      case PyMOL.OBJECT_MAPMESH:
      case PyMOL.OBJECT_MAPDATA:
        // might need to set color here for these?
        break;
      }
      //addJmolObject(tok, null, objectNameID);
      addJmolObject(tok, bs, info);
    }
  }

  private void setObject(String name, int istate) {
    objectName = name;
    objectType = getObjectType(name);
    objectSelectionName = getSelectionName(name);
    objectNameID = (istate == 0 ? getObjectID(name) : objectSelectionName + "_"
        + istate);
    objectAtoms = htObjectAtoms.get(name);
    objectSettings = htObjectSettings.get(name);
    String groupName = htObjectGroups.get(objectName);
    objectHidden = (htHiddenObjects.containsKey(name) || groupName != null
        && !groups.get(groupName).visible);
    getObjectSettings();
  }

  @SuppressWarnings("unchecked")
  private void generateShapes(Object[] reps, int istate) {
    if (reps == null)
      return;
    addJmolObject(T.restrict, null, null);
    // through all molecules...
    //    for (int m = moleculeNames.size(); --m >= 0;) {
    for (int m = 0; m < moleculeNames.size(); m++) {
      setObject(moleculeNames.get(m), istate);
      if (objectHidden)
        continue;
      BS[] molReps = new BS[PyMOL.REP_JMOL_MAX];
      for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
        molReps[i] = new BS();
      // through all representations...
      for (int i = reps.length; --i >= 0;) {
        Map<String, JmolList<Object>> repMap = (Map<String, JmolList<Object>>) reps[i];
        JmolList<Object> list = (repMap == null ? null : repMap.get(objectName));
        if (list != null)
          selectAllAtoms(list, istate, molReps[i]);
      }
      createShapeObjects(molReps, true, -1, -1);
    }
  }

  private BS getBS(JmolList<Object> list) {
    BS bs = new BS();
    for (int i = list.size(); --i >= 0;)
      bs.set(intAt(list, i));
    return bs;
  }

  private void getBsAtoms(JmolList<Object> list, int[] atomMap, BS bs) {
    for (int i = list.size(); --i >= 0;)
      bs.set(atomMap[intAt(list, i)]);
  }

  private final static P3 ptTemp = new P3();

  @SuppressWarnings("unchecked")
  static int getColorPt(Object o) {
    return (o instanceof Integer ? ((Integer) o).intValue() : ColorUtil
        .colorPtToInt(pointAt((JmolList<Object>) o, 0, ptTemp)));
  }

  static int intAt(JmolList<Object> list, int i) {
    return ((Number) list.get(i)).intValue();
  }

  static int colorSetting(JmolList<Object> c) {
    return getColorPt(c.get(2));
  }

  void setObjects(String mepList, boolean doCache, int baseModelIndex,
                  int baseAtomIndex, boolean haveScenes) {
    this.baseModelIndex = baseModelIndex;
    this.baseAtomIndex = baseAtomIndex;
    this.mepList = mepList;
    this.doCache = doCache;
    this.haveScenes = haveScenes;
    clearReaderData(!haveScenes);
    finalizeObjects();
  }

  private void finalizeObjects() {
    for (int i = 0; i < jmolObjects.size(); i++) {
      try {
        JmolObject obj = jmolObjects.get(i);
        obj.offset(baseModelIndex, baseAtomIndex);
        obj.finalizeObject(viewer.modelSet, mepList, doCache);
      } catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
      }
    }
    jmolObjects.clear();
  }

  JmolObject getJmolObject(int id, BS bsAtoms, Object info) {
    return new JmolObject(id, objectNameID, bsAtoms, info);
  }

  JmolObject addJmolObject(int id, BS bsAtoms, Object info) {
    return addObject(getJmolObject(id, bsAtoms, info));
  }

  /**
   * adds depth_cue, fog, and fog_start
   * 
   * @param view
   * @param isViewObj
   * @return 22-element array
   */
  float[] getPymolView(JmolList<Object> view, boolean isViewObj) {
    float[] pymolView = new float[21];
    boolean depthCue = booleanSetting(PyMOL.depth_cue); // 84
    boolean fog = booleanSetting(PyMOL.fog); // 88
    float fog_start = floatSetting(PyMOL.fog_start); // 192

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

    boolean isOrtho = booleanSetting(PyMOL.ortho); // 23
    float fov = floatSetting(PyMOL.field_of_view); // 152

    pymolView[pt++] = (isOrtho ? fov : -fov);
    pymolView[pt++] = (depthCue ? 1 : 0);
    pymolView[pt++] = (fog ? 1 : 0);
    pymolView[pt++] = fog_start;
    return pymolView;
  }

  @SuppressWarnings("unchecked")
  float globalSetting(int i) {
    try {
      JmolList<Object> setting = (JmolList<Object>) settings.get(i);
      return ((Number) setting.get(2)).floatValue();
    } catch (Exception e) {
      return PyMOL.getDefaultSetting(i, pymolVersion);
    }
  }

  /**
   * Create a heirarchical list of named groups as generally seen on the PyMOL
   * app's right-hand object menu.
   * 
   * @param object
   * @param parent
   * @param type
   * @return group
   */

  PyMOLGroup addGroup(JmolList<Object> object, String parent, int type) {
    if (groups == null)
      groups = new Hashtable<String, PyMOLGroup>();
    PyMOLGroup myGroup = getGroup(objectName);
    myGroup.object = object;
    myGroup.objectNameID = objectNameID;
    myGroup.visible = !objectHidden;
    myGroup.type = type;
    if (!myGroup.visible) {
      occludedObjects.put(objectNameID, Boolean.TRUE);
      htHiddenObjects.put(objectName, Boolean.TRUE);
    }
    if (parent != null && parent.length() != 0)
      getGroup(parent).addList(myGroup);
    return myGroup;
  }

  private PyMOLGroup getGroup(String name) {
    PyMOLGroup g = groups.get(name);
    if (g == null) {
      groups.put(name, (g = new PyMOLGroup(name)));
      defineAtoms(name, g.bsAtoms);
    }
    return g;
  }

  /**
   * Create group JmolObjects, and set hierarchical visibilities
   */
  void finalizeEverything() {
    viewer.setStringProperty("measurementUnits", "ANGSTROMS");
    viewer.setBooleanProperty("zoomHeight", true);
    setGroupVisibilities();
    if (groups != null) {
      for (int i = jmolObjects.size(); --i >= 0;) {
        JmolObject obj = jmolObjects.get(i);
        if (obj.objectNameID != null
            && occludedObjects.containsKey(obj.objectNameID))
          obj.visible = false;
      }
      addJmolObject(T.group, null, groups);
    }
    if (!bsHidden.isEmpty())
      addJmolObject(T.hidden, bsHidden, null);
  }

  void setCarveSets(Map<String, JmolList<Object>> htObjNames) {
    if (htCarveSets.isEmpty())
      return;
    for (Entry<String, BS> e: htCarveSets.entrySet())
      getSelectionAtoms(listAt(htObjNames.get(e.getKey()), 5), 0, e.getValue());
  }

  private void setGroupVisibilities() {
    if (groups != null) {
      Collection<PyMOLGroup> list = groups.values();
      BS bsAll = new BS();
      for (PyMOLGroup g : list) {
        bsAll.or(g.bsAtoms);
        if (g.parent == null) // top
          setGroupVisible(g, true);
        else if (g.list.isEmpty()) // bottom
          g.addGroupAtoms(new BS());
      }
      defineAtoms("all", bsAll);
    }
  }

  private void defineAtoms(String name, BS bs) {
    htDefinedAtoms.put(getSelectionName(name), bs);
  }

  private static String getSelectionName(String name) {
    return "__" + fixName(name);
  }

  void createShapeObjects(BS[] reps, boolean allowSurface, int atomCount0,
                          int atomCount) {
    if (atomCount >= 0) {
      // initial creation, not just going to this scene
      objectAtoms = BSUtil.newBitSet2(atomCount0, atomCount);
      JmolObject jo;
      // from reader
      jo = addJmolObject(JC.SHAPE_BALLS, objectAtoms, null);
      colixes = ArrayUtil.ensureLengthShort(colixes, atomCount);
      for (int i = atomCount; --i >= atomCount0;)
        colixes[i] = (short) atomColorList.get(i).intValue();
      jo.setColors(colixes, 0);
      jo.setSize(0);
      jo = addJmolObject(JC.SHAPE_STICKS, objectAtoms, null);
      jo.setSize(0);
    }
    createShapeObject(PyMOL.REP_LINES, reps[PyMOL.REP_LINES]);
    createShapeObject(PyMOL.REP_STICKS, reps[PyMOL.REP_STICKS]);
    fixReps(reps);
    createSpacefillObjects();
    for (int i = 0; i < PyMOL.REP_JMOL_MAX; i++)
      switch (i) {
      case PyMOL.REP_LINES:
      case PyMOL.REP_STICKS:
        continue;
      case PyMOL.REP_MESH:
      case PyMOL.REP_SURFACE:
        // surfaces depend upon global flags
        if (!allowSurface)
          continue;

        //    #define cRepSurface_by_flags       0
        //    #define cRepSurface_all            1
        //    #define cRepSurface_heavy_atoms    2
        //    #define cRepSurface_vis_only       3
        //    #define cRepSurface_vis_heavy_only 4

        switch (surfaceMode) {
        case 0:
          reps[i].andNot(bsNoSurface);
          break;
        case 1:
        case 3:
          break;
        case 2:
        case 4:
          reps[i].andNot(bsHydrogen);
          break;
        }
        //$FALL-THROUGH$
      default:
        createShapeObject(i, reps[i]);
        continue;
      }
    objectAtoms = null;
  }

  void addLabel(int atomIndex, int uniqueID, int atomColor,
                JmolList<Object> labelOffset, String label) {
    int icolor = (int) getUniqueFloatDef(uniqueID, PyMOL.label_color,
        labelColor);
    if (icolor == PyMOL.COLOR_BACK || icolor == PyMOL.COLOR_FRONT) {
      // deal with this later
    } else if (icolor < 0) {
      icolor = atomColor;
    }
    float[] labelPos = new float[7];
    if (labelOffset == null) {
      P3 offset = getUniquePoint(uniqueID, PyMOL.label_position, null);
      if (offset == null)
        offset = labelPosition;
      else
        offset.add(labelPosition);
      setLabelPosition(offset, labelPos);
    } else {
      for (int i = 0; i < 7; i++)
        labelPos[i] = floatAt(labelOffset, i);
    }
    labels.put(Integer.valueOf(atomIndex), newTextLabel(label, labelPos,
        icolor, (int) getUniqueFloatDef(uniqueID, PyMOL.label_font_id,
            labelFontId), getUniqueFloatDef(uniqueID, PyMOL.label_size,
            labelSize)));
  }

  float getUniqueFloatDef(int id, int key, float defaultValue) {
    JmolList<Object> setting;
    if (id < 0
        || (setting = uniqueSettings.get(Integer.valueOf((id << 10) + key))) == null)
      return defaultValue;
    float v = ((Number) setting.get(2)).floatValue();
    if (Logger.debugging)
      Logger.info("Pymol unique setting for " + id + ": [" + key + "] = " + v);
    return v;
  }

  @SuppressWarnings("unchecked")
  P3 getUniquePoint(int id, int key, P3 pt) {
    JmolList<Object> setting;
    if (id < 0
        || (setting = uniqueSettings.get(Integer.valueOf((id << 10) + key))) == null)
      return pt;
    pt = new P3();
    pointAt((JmolList) setting.get(2), 0, pt);
    Logger.info("Pymol unique setting for " + id + ": " + key + " = " + pt);
    return pt;
  }

  JmolList<Object> getObjectSetting(int i) {
    return objectSettings.get(Integer.valueOf(i));
  }

  boolean booleanSetting(int i) {
    return (floatSetting(i) != 0);
  }

  @SuppressWarnings("unchecked")
  float floatSetting(int i) {
    try {
      JmolList<Object> setting = null;
      if (objectSettings != null)
        setting = objectSettings.get(Integer.valueOf(i));
      if (setting == null)
        setting = (JmolList<Object>) settings.get(i);
      return ((Number) setting.get(2)).floatValue();
    } catch (Exception e) {
      return PyMOL.getDefaultSetting(i, pymolVersion);
    }
  }

  @SuppressWarnings("unchecked")
  String stringSetting(int i) {
    try {
      JmolList<Object> setting = null;
      if (objectSettings != null)
        setting = objectSettings.get(Integer.valueOf(i));
      if (setting == null)
        setting = (JmolList<Object>) settings.get(i);
      return setting.get(2).toString();
    } catch (Exception e) {
      return null;
    }
  }

  static P3 pointAt(JmolList<Object> list, int i, P3 pt) {
    pt.set(floatAt(list, i++), floatAt(list, i++), floatAt(list, i));
    return pt;
  }

  static float floatAt(JmolList<Object> list, int i) {
    return (list == null ? 0 : ((Number) list.get(i)).floatValue());
  }

  static float[] floatsAt(JmolList<Object> a, int pt, float[] data, int len) {
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

  static float[] setLabelPosition(P3 offset, float[] labelPos) {
    labelPos[0] = 1;
    labelPos[1] = offset.x;
    labelPos[2] = offset.y;
    labelPos[3] = offset.z;
    return labelPos;
  }

  boolean addMeasurements(MeasurementData[] mdList, int nCoord,
                          JmolList<Object> list, BS bsReps, int color,
                          JmolList<Object> offsets, boolean haveLabels) {
    boolean isNew = (mdList == null);
    int n = (isNew ? list.size() / 3 / nCoord : mdList.length);
    if (n == 0)
      return false;
    boolean drawLabel = haveLabels && bsReps.get(PyMOL.REP_LABELS);
    boolean drawDashes = bsReps.get(PyMOL.REP_DASHES);
    float rad = floatSetting(PyMOL.dash_width) / 20;
    if (rad == 0)
      rad = 0.05f;
    if (!drawDashes)
      rad = -0.0005f;
    if (color < 0)
      color = (int) floatSetting(PyMOL.dash_color);
    int c = PyMOL.getRGB(color);
    short colix = C.getColix(c);
    int clabel = (int) floatSetting(PyMOL.label_color);
    if (clabel < 0)
      clabel = color;
    if (isNew) {
      mdList = new MeasurementData[n];
      htMeasures.put(objectName, mdList);
    }
    BS bs = BSUtil.newAndSetBit(0);
    for (int index = 0, p = 0; index < n; index++) {
      MeasurementData md;
      float[] offset;
      if (isNew) {
        JmolList<Object> points = new JmolList<Object>();
        for (int i = 0; i < nCoord; i++, p += 3)
          points.addLast(pointAt(list, p, new Point3fi()));
        offset = floatsAt(listAt(offsets, index), 0, new float[7], 7);
        if (offset == null)
          offset = setLabelPosition(labelPosition, new float[7]);
        md = mdList[index] = new MeasurementData(objectNameID + "_"
            + (index + 1), viewer, points);
        md.note = objectName;
      } else {
        md = mdList[index];
        offset = md.text.pymolOffset;
      }
      int nDigits = (int) floatSetting(MEAS_DIGITS[nCoord - 2]);
      String strFormat = nCoord + ": "
          + (drawLabel ? "%0." + (nDigits < 0 ? 1 : nDigits) + "VALUE" : "");
      //strFormat += " -- " + objectNameID + " " + floatSetting(PyMOL.surface_color) + " " + Integer.toHexString(c);
      Text text = newTextLabel(strFormat, offset, clabel,
          (int) floatSetting(PyMOL.label_font_id),
          floatSetting(PyMOL.label_size));
      md.set(T.define, null, strFormat, "angstroms", null, false, false, null,
          false, (int) (rad * 2000), colix, text);
      addJmolObject(JC.SHAPE_MEASURES, bs, md);
    }
    return true;
  }

  SB getViewScript(JmolList<Object> view) {
    SB sb = new SB();
    float[] pymolView = getPymolView(view, true);
    sb.append(";set zshadePower 1;set traceAlpha "
        + (globalSetting(PyMOL.cartoon_round_helices) != 0));
    boolean rockets = cartoonRockets;
    sb.append(";set cartoonRockets " + rockets);
    if (rockets)
      sb.append(";set rocketBarrels " + rockets);
    sb.append(";set cartoonLadders " + haveNucleicLadder);
    sb.append(";set ribbonBorder "
        + (globalSetting(PyMOL.cartoon_fancy_helices) != 0));
    sb.append(";set cartoonFancy "
        + (globalSetting(PyMOL.cartoon_fancy_helices) == 0));
    String s = "000000" + Integer.toHexString(bgRgb);
    s = "[x" + s.substring(s.length() - 6) + "]";
    sb.append(";background " + s);
    sb.append(";moveto 0 PyMOL " + Escape.eAF(pymolView));
    sb.append(";save orientation 'default';");
    return sb;
  }

  short getColix(int colorIndex, float translucency) {
    short colix = (colorIndex == PyMOL.COLOR_BACK ? (ColorUtil
        .getBgContrast(bgRgb) == C.WHITE ? C.BLACK : C.WHITE)
        : colorIndex == PyMOL.COLOR_FRONT ? ColorUtil.getBgContrast(bgRgb) : C
            .getColixO(Integer.valueOf(PyMOL.getRGB(colorIndex))));

    return C.getColixTranslucent3(colix, translucency > 0, translucency);
  }

  static int colorSettingClamped(JmolList<Object> c) {
    return (c.size() < 6 || intAt(c, 4) == 0 ? colorSetting(c) : getColorPt(c
        .get(5)));
  }

  void setAtomColor(int uniqueID, int atomColor) {
    float translucency = getUniqueFloatDef(uniqueID, PyMOL.sphere_transparency,
        sphereTranslucency);
    atomColorList.addLast(Integer.valueOf(getColix(atomColor, translucency)));
  }

  void setFrameObject(int type, Object info) {
    if (info != null) {
      frameObj = getJmolObject(type, null, info);
      return;
    }
    if (frameObj == null)
      return;
    frameObj.finalizeObject(viewer.getModelSet(), null, false);
    frameObj = null;
  }

  static String fixName(String name) {
    char[] chars = name.toLowerCase().toCharArray();
    for (int i = chars.length; --i >= 0;)
      if (!Character.isLetterOrDigit(chars[i]))
        chars[i] = '_';
    return String.valueOf(chars); 
  }

  String getObjectID(String name) {
    return (String) objectInfo.get(name)[0];
  }

  int getObjectType(String name) {
    return ((Integer) objectInfo.get(name)[1]).intValue();
  }

  BS setAtomMap(int[] atomMap, int atomCount0) {
    htAtomMap.put(objectNameID, atomMap);
    BS bsAtoms = htDefinedAtoms.get(objectSelectionName);
    if (bsAtoms == null) {
      bsAtoms = BS.newN(atomCount0 + atomMap.length);
      Logger.info("PyMOL molecule " + objectName);
      htDefinedAtoms.put(objectSelectionName, bsAtoms);
      htObjectAtoms.put(objectName, bsAtoms);
      moleculeNames.addLast(objectName);
    }
    return bsAtoms;
  }

  private final static int[] MEAS_DIGITS = { PyMOL.label_distance_digits,
      PyMOL.label_angle_digits, PyMOL.label_dihedral_digits };

  private Text newTextLabel(String label, float[] labelOffset, int colorIndex,
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
    Text t = Text.newLabel(viewer.getGraphicsData(), font, label, getColix(
        colorIndex, 0), (short) 0, 0, 0, labelOffset);
    return t;
  }

  /**
   * Attempt to adjust for PyMOL versions. See PyMOL layer3.Executive.c
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

  private void fixReps(BS[] reps) {
    htSpacefill.clear();
    bsCartoon.clearAll();
    for (int iAtom = objectAtoms.nextSetBit(0); iAtom >= 0; iAtom = objectAtoms
        .nextSetBit(iAtom + 1)) {
      float rad = 0;
      int uniqueID = (reader == null ? uniqueIDs[iAtom] : reader
          .getUniqueID(iAtom));
      if (reps[PyMOL.REP_SPHERES].get(iAtom)) {
        rad = (reader == null ? radii[iAtom] : reader.getVDW(iAtom))
            * getUniqueFloat(uniqueID, PyMOL.sphere_scale);
      } else if (reps[PyMOL.REP_NBSPHERES].get(iAtom)) {
        // Penta_vs_mutants calcium
        rad = getUniqueFloat(uniqueID, PyMOL.nonbonded_size);
      }
      if (rad != 0) {
        Float r = Float.valueOf(rad);
        BS bsr = htSpacefill.get(r);
        if (bsr == null)
          htSpacefill.put(r, bsr = new BS());
        bsr.set(iAtom);
      }
      int cartoonType = (reader == null ? cartoonTypes[iAtom] : reader
          .getCartoonType(iAtom));
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

        switch (cartoonType) {
        case 1:
        case 4:
          reps[PyMOL.REP_JMOL_TRACE].set(iAtom);
          //$FALL-THROUGH$
        case -1:
          reps[PyMOL.REP_CARTOON].clear(iAtom);
          bsCartoon.clear(iAtom);
          break;
        case 7:
          reps[PyMOL.REP_JMOL_PUTTY].set(iAtom);
          reps[PyMOL.REP_CARTOON].clear(iAtom);
          bsCartoon.clear(iAtom);
          break;
        default:
          bsCartoon.set(iAtom);
        }
      }
    }

    reps[PyMOL.REP_CARTOON].and(bsCartoon);
    cleanSingletons(reps[PyMOL.REP_CARTOON]);
    cleanSingletons(reps[PyMOL.REP_RIBBON]);
    cleanSingletons(reps[PyMOL.REP_JMOL_TRACE]);
    cleanSingletons(reps[PyMOL.REP_JMOL_PUTTY]);
    bsCartoon.and(reps[PyMOL.REP_CARTOON]);
  }

  /**
   * PyMOL does not display cartoons or traces for single-residue runs. This
   * two-pass routine first sets bits in a residue bitset, then it clears out
   * all singletons, and in a second pass all atom bits for not-represented
   * residues are cleared.
   * 
   * @param bs
   */
  private void cleanSingletons(BS bs) {
    if (bs.isEmpty())
      return;
    bs.and(objectAtoms);
    BS bsr = new BS();
    int n = bs.length();
    int pass = 0;
    while (true) {
      for (int i = 0, offset = 0, iPrev = Integer.MIN_VALUE, iSeqLast = Integer.MIN_VALUE, iSeq = Integer.MIN_VALUE; i < n; i++) {
        if (iPrev < 0
            || (reader == null ? newChain[i] : reader.compareAtoms(iPrev, i)))
          offset++;
        iSeq = (reader == null ? sequenceNumbers[i] : reader
            .getSequenceNumber(i));
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

  /**
   * Create JmolObjects for each shape.
   * 
   * Note that LINES and STICKS are done initially, then all the others are
   * processed.
   * 
   * @param shapeID
   * @param bs
   */
  private void createShapeObject(int shapeID, BS bs) {
    // add more to implement
    float f;
    if (bs.isEmpty())
      return;
    JmolObject jo = null;
    switch (shapeID) {
    case PyMOL.REP_NONBONDED: // stars
      bs.and(bsNonbonded);
      if (bs.isEmpty())
        return;
      jo = addJmolObject(JC.SHAPE_STARS, bs, null);
      jo.rd = new RadiusData(null, floatSetting(PyMOL.nonbonded_size) / 2,
          RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      break;
    case PyMOL.REP_NBSPHERES:
    case PyMOL.REP_SPHERES:
      jo = addJmolObject(JC.SHAPE_BALLS, bs, null);
      jo.translucency = sphereTranslucency;
      break;
    case PyMOL.REP_DOTS:
      jo = addJmolObject(JC.SHAPE_DOTS, bs, null);
      f = floatSetting(PyMOL.sphere_scale);
      jo.rd = new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO);
      break;
    case PyMOL.REP_CARTOON:
      createCartoonObject("H", (cartoonRockets ? PyMOL.cartoon_helix_radius
          : PyMOL.cartoon_oval_length));
      createCartoonObject("S", PyMOL.cartoon_rect_length);
      createCartoonObject("L", PyMOL.cartoon_loop_radius);
      createCartoonObject(" ", PyMOL.cartoon_loop_radius);
      break;
    case PyMOL.REP_MESH: //   = 8;
      jo = addJmolObject(T.isosurface, bs, null);
      jo.setSize(floatSetting(PyMOL.solvent_radius));
      jo.translucency = floatSetting(PyMOL.transparency);
      break;
    case PyMOL.REP_SURFACE: //   = 2;
      float withinDistance = floatSetting(PyMOL.surface_carve_cutoff);
      jo = addJmolObject(T.isosurface, bs, new Object[] {
          booleanSetting(PyMOL.two_sided_lighting) ? "FULLYLIT" : "FRONTLIT",
          (surfaceMode == 3 || surfaceMode == 4) ? " only" : "", 
              bsCarve, Float.valueOf(withinDistance)});
      jo.setSize(floatSetting(PyMOL.solvent_radius) * (solventAccessible ? -1 : 1));
      jo.translucency = floatSetting(PyMOL.transparency);
      if (surfaceColor >= 0)
        jo.argb = PyMOL.getRGB(surfaceColor);
      break;
    case PyMOL.REP_LABELS: //   = 3;
      bs.and(bsLabeled);
      if (bs.isEmpty())
        return;
      jo = addJmolObject(JC.SHAPE_LABELS, bs, labels);
      break;
    case PyMOL.REP_JMOL_PUTTY:
      createPuttyObject(bs);
      break;
    case PyMOL.REP_JMOL_TRACE:
      createTraceObject(bs);
      break;
    case PyMOL.REP_RIBBON: // backbone or trace, depending
      createRibbonObject(bs);
      break;
    case PyMOL.REP_LINES:
      jo = addJmolObject(JC.SHAPE_STICKS, bs, null);
      jo.setSize(floatSetting(PyMOL.line_width) / 15);
      break;
    case PyMOL.REP_STICKS:
      jo = addJmolObject(JC.SHAPE_STICKS, bs, null);
      jo.setSize(floatSetting(PyMOL.stick_radius) * 2);
      jo.translucency = stickTranslucency;
      break;
    default:
      Logger.error("Unprocessed representation type " + shapeID);
    }
  }

  /**
   * Create a BALLS JmolObject for each radius.
   * 
   */
  private void createSpacefillObjects() {
    for (Map.Entry<Float, BS> e : htSpacefill.entrySet()) {
      float r = e.getKey().floatValue();
      BS bs = e.getValue();
      addJmolObject(JC.SHAPE_BALLS, bs, null).rd = new RadiusData(null, r,
          RadiusData.EnumType.ABSOLUTE, EnumVdw.AUTO);
    }
    htSpacefill.clear();
  }

  /**
   * trace, or cartoon in the case of cartoon ladders.
   * 
   * @param bs
   */
  private void createTraceObject(BS bs) {
    JmolObject jo;
    checkNucleicObject(bs, true);
    if (bs.isEmpty())
      return;
    jo = addJmolObject(JC.SHAPE_TRACE, bs, null);
    jo.translucency = cartoonTranslucency;
    jo.setSize(floatSetting(PyMOL.cartoon_tube_radius) * 2);
  }

  private void checkNucleicObject(BS bs, boolean isTrace) {
    JmolObject jo;
    BS bsNuc = BSUtil.copy(bsNucleic);
    bsNuc.and(bs);
    if (!bsNuc.isEmpty()) {
      if (isTrace && cartoonLadderMode)
        haveNucleicLadder = true;
      // we will just use cartoons for ladder mode
      jo = addJmolObject(JC.SHAPE_CARTOON, bsNuc, null);
      jo.translucency = cartoonTranslucency;
      jo.setSize(floatSetting(PyMOL.cartoon_tube_radius) * 2);
      bs.andNot(bsNuc);
    }
  }

  /**
   * "Putty" shapes scaled in a variety of ways.
   * 
   * @param bs
   */
  private void createPuttyObject(BS bs) {
    float[] info = new float[] { floatSetting(PyMOL.cartoon_putty_quality),
        floatSetting(PyMOL.cartoon_putty_radius),
        floatSetting(PyMOL.cartoon_putty_range),
        floatSetting(PyMOL.cartoon_putty_scale_min),
        floatSetting(PyMOL.cartoon_putty_scale_max),
        floatSetting(PyMOL.cartoon_putty_scale_power),
        floatSetting(PyMOL.cartoon_putty_transform) };
    addJmolObject(JC.SHAPE_TRACE, bs, info).translucency = cartoonTranslucency;
  }

  /**
   * PyMOL "ribbons" could be Jmol backbone or trace, depending upon the value
   * of PyMOL.ribbon_sampling.
   * 
   * @param bs
   */
  private void createRibbonObject(BS bs) {
    // 2ace: 0, 1 ==> backbone  // r rpc w 0.0 1.3 3.0 too small
    // fig8: 0, 1 ==> backbone // r rpc w 0.0 1.0 3.0  OK
    // casp: 0, 1 ==> backbone // r rpc w 0.0 1.3 3.0  too small
    // NLG3_AchE: 0, 1 ==> backbone  //r rpc w 0.0 1.3 4.0 too small 
    // NLG3_HuAChE: 0, 10 ==> trace
    // tach: 0, 10 ==> trace
    // tah-lev: 0, 10 ==> trace
    // 496: -1, 1 ==> backbone  // r rpc 0.0 1.3 3.0 too small
    // kinases: -1, 1 ==> backbone
    // 443_D1: -1, 1 ==> backbone
    // 476Rainbow_New: 10, 8 ==> trace

    //float smoothing = getFloatSetting(PyMOL.ribbon_smooth);
    boolean isTrace = (floatSetting(PyMOL.ribbon_sampling) > 1);
    float r = floatSetting(PyMOL.ribbon_radius) * 2;
    float rayScale = floatSetting(PyMOL.ray_pixel_scale);
    if (r == 0)
      r = floatSetting(PyMOL.ribbon_width)
          * (isTrace ? 1 : (rayScale <= 1 ? 0.5f : rayScale)) * 0.1f;
    addJmolObject((isTrace ? JC.SHAPE_TRACE : JC.SHAPE_BACKBONE), bs, null)
        .setSize(r);
  }

  private void createCartoonObject(String key, int sizeID) {
    BS bs = BSUtil.copy(ssMapAtom.get(key));
    if (bs == null)
      return;
    bs.and(bsCartoon);
    if (bs.isEmpty())
      return;
    if (key.equals(" ")) {
      checkNucleicObject(bs, false);
      if (bs.isEmpty())
        return;
    }
    JmolObject jo = addJmolObject(JC.SHAPE_CARTOON, bs, null);
    jo.translucency = cartoonTranslucency;
    jo.setSize(floatSetting(sizeID) * 2);
  }

  private JmolObject addObject(JmolObject obj) {
    jmolObjects.addLast(obj);
    return obj;
  }

  /**
   * Iterate through groups, setting visibility flags.
   * 
   * @param g
   * @param parentVis
   */
  private void setGroupVisible(PyMOLGroup g, boolean parentVis) {
    boolean vis = parentVis && g.visible;
    if (vis)
      return;
    g.visible = false;
    occludedObjects.put(g.objectNameID, Boolean.TRUE);
    htHiddenObjects.put(g.name, Boolean.TRUE);
    switch (g.type) {
    case PyMOL.OBJECT_MOLECULE:
      bsHidden.or(g.bsAtoms);
      break;
    default:
      // a group?
      g.occluded = true;
      break;
    }
    for (PyMOLGroup gg : g.list.values()) {
      setGroupVisible(gg, vis);
    }
  }

  private float getUniqueFloat(int uniqueID, int i) {
    float f;
    switch (i) {
    case PyMOL.sphere_transparency:
      f = sphereTranslucency;
      break;
    case PyMOL.sphere_scale:
      f = sphereScale;
      break;
    case PyMOL.nonbonded_size:
      f = nonBondedSize;
      break;
    default:
      return 0;
    }
    return getUniqueFloatDef(uniqueID, i, f);
  }

  BS getSSMapAtom(String ssType) {
    BS bs = ssMapAtom.get(ssType);
    if (bs == null)
      ssMapAtom.put(ssType, bs = new BS());
    return bs;
  }

  /**
   * return a map of lists of the type: [ [name1,...], [name2,...], ...]
   * 
   * @param list
   * @return Hashtable
   */
  static Map<String, JmolList<Object>> listToMap(JmolList<Object> list) {
    Hashtable<String, JmolList<Object>> map = new Hashtable<String, JmolList<Object>>();
    for (int i = list.size(); --i >= 0;) {
      JmolList<Object> item = PyMOLScene.listAt(list, i);
      if (item != null && item.size() > 0)
        map.put((String) item.get(0), item);
    }
    return map;
  }
  
  Map<String, Object> setAtomDefs() {
    setGroupVisibilities();
    Map<String, Object> defs = new Hashtable<String, Object>();
    for (Entry<String, BS> e: htDefinedAtoms.entrySet()) {
      BS bs = e.getValue();
      if (!bs.isEmpty())
        defs.put(e.getKey(), bs);
    }
    addJmolObject(T.define, null, defs);
    return defs;
  }

  public boolean needSelections() {
    return haveScenes || !htCarveSets.isEmpty();
  }

}
