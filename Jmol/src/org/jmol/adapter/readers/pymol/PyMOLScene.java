package org.jmol.adapter.readers.pymol;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.PymolAtomReader;
import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.JmolFont;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * A class to allow manipulation of scenes dissociated from file loading.
 * 
 */
class PyMOLScene {

  // used by PyMOLReader
  float sphereTranslucency;
  boolean cartoonRockets;
  boolean haveNucleicLadder;
  int bgRgb;

  // filled by PyMOLReader
  BS bsHidden = new BS();
  BS bsNucleic = new BS();
  BS bsNonbonded = new BS();
  BS bsLabeled = new BS();
  BS bsHydrogen = new BS();
  BS bsNoSurface = new BS();

  private Map<Float, BS> htSpacefill = new Hashtable<Float, BS>();

  // private

  private final static int OBJECT_MOLECULE = 1;

  private Viewer viewer;

  /*
    rep_list = [ "lines","sticks","spheres",
                     "dots","surface","mesh",
                     "nonbonded", "nb_spheres",
                     "cartoon","ribbon","labels","slice"]
   */

  private JmolList<JmolObject> jmolObjects = new JmolList<JmolObject>();

  //private Map<String, int[]> htAtomMap = new Hashtable<String, int[]>();
  private Map<String, Boolean> occludedObjects = new Hashtable<String, Boolean>();

  private Map<String, PyMOLGroup> groups;
  private int surfaceCount;
  private int pymolVersion;
  private float cartoonTranslucency;
  private float stickTranslucency;
  private boolean cartoonLadderMode;
  private int surfaceMode;
  private int surfaceColor;
  private float nonBondedSize;
  private float sphereScale;
  private int labelFontId;
  private P3 labelPosition;
  private int labelColor;
  private float labelSize;
  private P3 labelPosition0 = new P3();

  private Map<Integer, JmolList<Object>> objectSettings;
  private JmolList<Object> settings;
  private Map<Integer, JmolList<Object>> uniqueSettings;
  private Map<String, Map<Integer, JmolList<Object>>> htObjectSettings = new Hashtable<String, Map<Integer, JmolList<Object>>>();
  private String objectName;
  private Map<Integer, Text> labels;
  private String objectNameID;
  private boolean isHidden;
  private Map<String, BS> ssMapAtom;
  
  // during file loading we have a reader, but after that we must rely on data saved by the server
  
  private PymolAtomReader reader;
  private int[] uniqueIDs;
  private int[] cartoonTypes;
  private int[] sequenceNumbers;
  private boolean[] newChain;
  private float[] radii;

  @SuppressWarnings("unchecked")
  public PyMOLScene(PymolAtomReader reader, Viewer viewer,
      JmolList<Object> settings, Map<Integer, JmolList<Object>> uniqueSettings,
      int pymolVersion) {
    this.reader = reader;
    this.viewer = viewer;
    this.settings = settings;
    this.uniqueSettings = uniqueSettings;
    this.pymolVersion = pymolVersion;
    bgRgb = colorSetting(listAt(settings, PyMOL.bg_rgb));
    pointAt((JmolList<Object>) listAt(settings, PyMOL.label_position).get(2),
        0, labelPosition0);
    labels = new Hashtable<Integer, Text>();
  }

  @SuppressWarnings("unchecked")
  void setObjectInfo(String objectName, String objectNameID, boolean isHidden,
           JmolList<Object> list) {
    this.objectName = objectName;
    this.objectNameID = objectNameID;
    this.isHidden = isHidden;
    objectSettings = new Hashtable<Integer, JmolList<Object>>();
    if (objectName == null)
      return;
    htObjectSettings.put(objectName, objectSettings);
    if (list != null && list.size() != 0) {
      System.out.println("local settings: " + list.toString());
      for (int i = list.size(); --i >= 0;) {
        JmolList<Object> setting = (JmolList<Object>) list.get(i);
        objectSettings.put((Integer) setting.get(0), setting);
      }
    }
    nonBondedSize = floatSetting(PyMOL.nonbonded_size);
    sphereScale = floatSetting(PyMOL.sphere_scale);
    cartoonTranslucency = floatSetting(PyMOL.cartoon_transparency);
    stickTranslucency = floatSetting(PyMOL.stick_transparency);
    sphereTranslucency = floatSetting(PyMOL.sphere_transparency);
    cartoonLadderMode = booleanSetting(PyMOL.cartoon_ladder_mode);
    cartoonRockets = booleanSetting(PyMOL.cartoon_cylindrical_helices);
    surfaceMode = (int) floatSetting(PyMOL.surface_mode);
    surfaceColor = (int) floatSetting(PyMOL.surface_color);
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

  public void setAtomInfo(int[] uniqueIDs, int[] cartoonTypes, int[] sequenceNumbers, boolean newChain[], float radii[]) {
    this.uniqueIDs = uniqueIDs;
    this.cartoonTypes = cartoonTypes;
    this.sequenceNumbers = sequenceNumbers;
    this.newChain = newChain;
    this.radii = radii;
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

  void setObjects(ModelSet modelSet, String mepList, boolean doCache,
                  int baseModelIndex, int baseAtomIndex) {
    for (int i = 0; i < jmolObjects.size(); i++) {
      JmolObject obj = jmolObjects.get(i);
      obj.offset(baseModelIndex, baseAtomIndex);
    }
    for (int i = 0; i < jmolObjects.size(); i++) {
      try {
        JmolObject obj = jmolObjects.get(i);
        obj.finalizeObject(modelSet, mepList, doCache);
      } catch (Exception e) {
        System.out.println(e);
      }
    }
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
    myGroup.visible = !isHidden;
    myGroup.type = type;
    if (parent != null)
      getGroup(parent).addList(myGroup);
    return myGroup;
  }

  void addGroupAtoms(PyMOLGroup group, BS bsAtoms) {
    group.bsAtoms = bsAtoms;
  }

  /**
   * Create group JmolObjects, and set hierarchical visibilities
   */
  void setGroupVisibilities() {
    if (groups != null) {
      Collection<PyMOLGroup> list = groups.values();
      for (PyMOLGroup g : list) {
        if (g.parent != null)
          continue;
        setGroupVisible(g, true);
      }
      addJmolObject(T.group, null, groups);
      for (int i = jmolObjects.size(); --i >= 0;) {
        JmolObject obj = jmolObjects.get(i);
        if (obj.objectNameID != null
            && occludedObjects.containsKey(obj.objectNameID))
          obj.visible = false;
      }
    }
    if (!bsHidden.isEmpty())
      addJmolObject(T.hidden, bsHidden, null);
  }

  void createShapeObjects(BS[] reps, Map<String, BS> ssMapAtom,
                          boolean allowSurface, int i0, int i1) {
    this.ssMapAtom = ssMapAtom;
    createShapeObject(PyMOL.REP_LINES, reps);
    createShapeObject(PyMOL.REP_STICKS, reps);
    fixReps(reps, i0, i1);
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
        createShapeObject(i, reps);
        continue;
      }
  }

  private void fixReps(BS[] reps, int i0, int i1) {
    
    for (int iAtom = i0; iAtom < i1; iAtom++) {
      float rad = 0;
      int uniqueID = (reader == null ? uniqueIDs[iAtom] : reader.getUniqueID(iAtom));
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
      int cartoonType = (reader == null ? cartoonTypes[iAtom] : reader.getCartoonType(iAtom));
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
          break;
        case 7:
          reps[PyMOL.REP_CARTOON].clear(iAtom);
          reps[PyMOL.REP_JMOL_PUTTY].set(iAtom);
          break;
        }
      }
    }
    cleanSingletons(reps[PyMOL.REP_CARTOON]);
    cleanSingletons(reps[PyMOL.REP_RIBBON]);
    cleanSingletons(reps[PyMOL.REP_JMOL_TRACE]);
    cleanSingletons(reps[PyMOL.REP_JMOL_PUTTY]);
  }

  /**
   * PyMOL does not display cartoons or traces for single-residue runs.
   * This two-pass routine first sets bits in a residue bitset, 
   * then it clears out all singletons, and in a second pass
   * all atom bits for not-represented residues are cleared.
   * 
   * @param bs
   */
  private void cleanSingletons(BS bs) {
    BS bsr = new BS();
    int n = bs.length();
    int pass = 0;
    while (true) {
      for (int i = 0, offset = 0, 
          iPrev = Integer.MIN_VALUE, 
          iSeqLast = Integer.MIN_VALUE, 
          iSeq = Integer.MIN_VALUE; i < n; i++) {
        if (iPrev < 0 || (reader == null ? newChain[i] : reader.compareAtoms(iPrev, i)))
          offset++;
        iSeq = (reader == null ? sequenceNumbers[i] : reader.getSequenceNumber(i));
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
   * REP_DASHES not implemented yet.
   * 
   * @param shapeID
   * @param reps
   */
  private void createShapeObject(int shapeID, BS[] reps) {
    // add more to implement
    BS bs = reps[shapeID];
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
      createCartoonObject(reps, "H",
          (cartoonRockets ? PyMOL.cartoon_helix_radius
              : PyMOL.cartoon_oval_length));
      createCartoonObject(reps, "S", PyMOL.cartoon_rect_length);
      createCartoonObject(reps, "L", PyMOL.cartoon_loop_radius);
      createCartoonObject(reps, " ", PyMOL.cartoon_loop_radius);
      break;
    case PyMOL.REP_MESH: //   = 8;
      jo = addJmolObject(T.isosurface, bs, null);
      jo.setSize(floatSetting(PyMOL.solvent_radius));
      jo.translucency = floatSetting(PyMOL.transparency);
      break;
    case PyMOL.REP_SURFACE: //   = 2;
      surfaceCount++;
      jo = addJmolObject(T.isosurface, bs,
          new String[] {booleanSetting(PyMOL.two_sided_lighting) ? "FULLYLIT" : "FRONTLIT", (surfaceMode == 3 || surfaceMode == 4) ? " only" : ""});
      jo.setSize(floatSetting(PyMOL.solvent_radius));
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

  private void createCartoonObject(BS[] reps, String key, int sizeID) {
    BS bs = BSUtil.copy(ssMapAtom.get(key));
    if (bs == null)
      return;
    bs.and(reps[PyMOL.REP_CARTOON]);
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
    if (!vis)
      switch (g.type) {
      case OBJECT_MOLECULE:
        if (g.bsAtoms != null) {
          bsHidden.or(g.bsAtoms);
        }
        break;
      default:
        // a group?
        g.occluded = true;
        occludedObjects.put(g.objectNameID, Boolean.TRUE);
        break;
      }
    for (PyMOLGroup gg : g.list.values()) {
      setGroupVisible(gg, vis);
    }
  }

  private PyMOLGroup getGroup(String name) {
    PyMOLGroup g = groups.get(name);
    if (g == null)
      groups.put(name, (g = new PyMOLGroup(name)));
    return g;
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

  float getUniqueFloatDef(int id, int key, float defaultValue) {
    JmolList<Object> setting;
    if (id < 0
        || (setting = uniqueSettings.get(Integer.valueOf((id << 10) + key))) == null)
      return defaultValue;
    float v = ((Number) setting.get(2)).floatValue();
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

  public void addLabel(int atomIndex, int uniqueID, int atomColor,
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

  static float[] setLabelPosition(P3 offset, float[] labelPos) {
    labelPos[0] = 1;
    labelPos[1] = offset.x;
    labelPos[2] = offset.y;
    labelPos[3] = offset.z;
    return labelPos;
  }

  final static int[] MEAS_DIGITS = { PyMOL.label_distance_digits,
      PyMOL.label_angle_digits, PyMOL.label_dihedral_digits };

  public boolean addMeasurements(int nCoord, JmolList<Object> list, BS bsReps,
                                 int color, JmolList<Object> offsets,
                                 boolean haveLabels) {
    int len = list.size();
    if (len == 0)
      return false;
    boolean drawLabel = haveLabels && bsReps.get(PyMOL.REP_LABELS);
    boolean drawDashes = bsReps.get(PyMOL.REP_DASHES);
    float rad = floatSetting(PyMOL.dash_width) / 20;
    if (rad == 0)
      rad = 0.05f;
    if (!drawDashes)
      rad = -0.0005f;
    int index = 0;
    if (color < 0)
      color = (int) floatSetting(PyMOL.dash_color);
    int c = PyMOL.getRGB(color);
    short colix = C.getColix(c);
    int clabel = (int) floatSetting(PyMOL.label_color);
    if (clabel < 0)
      clabel = color;
    for (int p = 0; p < len;) {
      JmolList<Object> points = new JmolList<Object>();
      for (int i = 0; i < nCoord; i++, p += 3)
        points.addLast(pointAt(list, p, new Point3fi()));
      BS bs = BSUtil.newAndSetBit(0);
      float[] offset = floatsAt(listAt(offsets, index++), 0, new float[7], 7);
      if (offset == null)
        offset = setLabelPosition(labelPosition, new float[7]);
      MeasurementData md = new MeasurementData(objectNameID + "_" + index,
          viewer, points);
      md.note = objectName;
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

  Text newTextLabel(String label, float[] labelOffset, int colorIndex,
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

}
