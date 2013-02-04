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
import org.jmol.modelset.ShapeSettings;
import org.jmol.util.BoxInfo;
//import org.jmol.util.Escape;
import org.jmol.util.BitSet;
import org.jmol.util.Colix;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3f;
import org.jmol.util.StringXBuilder;
import org.jmol.viewer.JmolConstants;

/**
 * experimental PyMOL PSE (binary Python serialization) file
 * 
 * bigendian file.
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
  private int[] atomMap;
  private Map<String, BitSet> ssMap = new Hashtable<String, BitSet>();
  private Map<String, BitSet> ssMapA = new Hashtable<String, BitSet>();
  private List<Integer> colixList = new ArrayList<Integer>();

  private List<ShapeSettings> shapes = new ArrayList<ShapeSettings>();
  private short[] colixes;
  private boolean isStateScript;

  @Override
  protected void initializeReader() throws Exception {
    isBinary = true;
    isStateScript = htParams.containsKey("isStateScript");
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("noAutoBond", Boolean.TRUE);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("shapes", shapes);
    super.initializeReader();
  }

  //  private Map<Object, Object> temp  = new Hashtable<Object, Object>();

  private Point3f xyzMin = Point3f.new3(1e6f, 1e6f, 1e6f);
  private Point3f xyzMax = Point3f.new3(-1e6f, -1e6f, -1e6f);
  private int nModels;

  @Override
  public void processBinaryDocument(JmolDocument doc) throws Exception {
    PickleReader reader = new PickleReader(doc);
    Map<String, Object> map = reader.getMap();
    reader = null;
    process(map);
  }

  private BitSet[] reps = new BitSet[17];
  
  private void process(Map<String, Object> map) {
    for (int i = 0; i < 17; i++)
      reps[i] = BitSet.newN(1000);
    settings = getMapList(map, "settings");
    List<Object> names = getMapList(map, "names");
    for (int i = 1; i < names.size(); i++) {
      processBranch(getList(names, i));
    }
    
    // we are done if this is a state script
    
    if (isStateScript)
      return;
    setRendering(getMapList(map, "view"));
  }

  /**
   * This is what a normal reader would not have.
   * Only executed if NOT in a state script
   * 
   * @param view 
   * 
   */
  
  private void setRendering(List<Object> view) {
    StringXBuilder sb = new StringXBuilder();
    
    setView(sb, view);
    setColixes();
    setShapes();

    //sb.append("background white;");
    sb.append("frame *;");
    sb.append("set cartoonfancy;");

    addJmolScript(sb.toString());
  }

  private static String getString(List<Object> list, int i) {
    String s = (String) list.get(i);
    return (s.length() == 0 ? " " : s);
  }

  private static int getInt(List<Object> list, int i) {
    return ((Integer) list.get(i)).intValue();
  }

  private static float getFloat(List<Object> list, int i) {
    return ((Double) list.get(i)).floatValue();
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

  private void processBranch(List<Object> branch) {
    String name = (String) branch.get(0);
    if (name.indexOf("_") == 0 || getInt(branch, 1) != 0) // otherwise, it's just a selection
      return;
    isHidden = (getInt(branch, 2) != 1);
    Logger.info("PyMOL model " + (nModels + 1) + " Branch " + name + (isHidden ? " (hidden)" : ""));
    int type = getInt(branch, 4);
    List<Object> deepBranch = getList(branch, 5);
    switch (type) {
    case BRANCH_MOLECULE:
      processBranchModels(deepBranch);
      break;
    case BRANCH_MAPSURFACE:
      break;
    case BRANCH_MAPMESH:
      break;
    case BRANCH_MEASURE:
      break;
    case BRANCH_CGO:
      break;
    case BRANCH_SURFACE:
      break;
    case BRANCH_GROUP:
      break;
    }
  }

  private void processBranchModels(List<Object> deepBranch) {
    processCryst(getList(deepBranch, 10));
    atomCount = atomCount0 = atomSetCollection.getAtomCount();
    atomMap = new int[getInt(deepBranch, 3)];
    List<Object> coordBranches = getList(deepBranch, 4);
    List<Object> bonds = getList(deepBranch, 6);
    pymolAtoms = getList(deepBranch, 7);
    int lastEnd = -1;
    for (int i = 0; i < coordBranches.size(); i++) {
      List<Object> coordBranch = getList(coordBranches, i);
      //int thisCount = getInt(coordBranch, 0);
      int thisEnd = getInt(coordBranch, 1);
      if (thisEnd != lastEnd) {
        processStructures();
        model(++nModels);
        lastEnd = thisEnd;
      }
      List<Object> coords = getList(coordBranch, 2);
      List<Object> idxToAtm = getList(coordBranch, 3);
      int n = idxToAtm.size();
      for (int idx = 0; idx < n; idx++)
        addAtom(pymolAtoms, getInt(idxToAtm, idx), idx, coords);
    }
    processStructures();
    processBonds(bonds);
  }

  //chain     => 1,     # chain ID
  //ac        => 2,     # alternate conformation indicator
  //segid     => 4,     # segment ID
  //          
  //resix     => 0,     # without insertion code, unlike "resno"
  //resno     => 3,     # using this and not the sequence number (resix) to deal with boundary case of insertion code... untested    
  //residue   => 5,     # 3-letter identifier
  //          
  //atomno    => 22,    # original PDB atom number
  //atom      => 6,     # (e.g. CB, NZ)
  //symbol    => 7,     # (e.g. C, N)
  //type      => 13,    # internal index number of "atom name"
  //mol2      => 8,     # MOL2 atom type (i.e. N.am)
  //charge    => 18,    # atom charge
  //
  //bf        => 14,    # temperature factor
  //occ       => 15,    # occupany
  //vdw       => 16,    # van der Waals radius
  //          
  //color     => 21,    # color code index
  //label     => 9,     # label text
  //          
  //reps      => 20,    # representation flags 
  //ss        => 10,    # s.s. assignment, S/H/L/""
  //
  //cartoon   => 23,    # cartoon type modifier
  //
  //### UNMAPPED: 11, 12, 17, 19

  private void addAtom(List<Object> pymolAtoms, int apt, int idx,
                       List<Object> coords) {
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
      return;
    atom.bfactor = getFloat(a, 14);
    atom.occupancy = (int) (getFloat(a, 15) * 100);
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
    bsHidden.setBitTo(atomCount, isHidden);
    atomMap[apt] = atomCount++;
    int serNo = getInt(a, 22);
    bs.set(seqNo);
    int charge = getInt(a, 18);
    int cpt = idx * 3;
    float x = getFloat(coords, cpt);
    float y = getFloat(coords, ++cpt);
    float z = getFloat(coords, ++cpt);
    BoxInfo.addPointXYZ(x, y, z, xyzMin, xyzMax, 0);
    //System.out.println(chainID +  " " + fileAtomIndex + " " + serNo + " " + x  + " " + y + " " + z);
    processAtom2(atom, serNo, x, y, z, charge);
    int color = getInt(a, 21);
    color = PyMOL.getColor(color);
    colixList.add(Integer.valueOf(Colix.getColixO(Integer.valueOf(color))));    
  }

  @Override
  protected void setAdditionalAtomParameters(Atom atom) {
  }

  private void processStructures() {
    if (atomSetCollection.bsStructuredModels == null)
      atomSetCollection.bsStructuredModels = new BitSet();
    atomSetCollection.bsStructuredModels.set(Math.max(atomSetCollection.getCurrentAtomSetIndex(), 0));

    processSS(ssMap.get("H"),ssMapA.get("H"),EnumStructure.HELIX, 0);
    processSS(ssMap.get("S"),ssMapA.get("S"),EnumStructure.SHEET, 1);
    processSS(ssMap.get("L"),ssMapA.get("L"),EnumStructure.TURN, 0);
    ssMap = new Hashtable<String, BitSet>();
  }

  private void processSS(BitSet bs, BitSet bsAtomType, EnumStructure type, int strandCount) {
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
      Structure structure = new Structure(imodel, type, type, type.toString(), ++strucNo, strandCount);
      Atom a = atoms[istart];
      Atom b = atoms[iend];
      bsAtomType.setBits(istart, iend + 1);
      structure.set(a.chainID, a.sequenceNumber, a.insertionCode,
          b.chainID, b.sequenceNumber, b.insertionCode);
      atomSetCollection.addStructure(structure);
      istart = iend = inew;
    }    
  }

  private void processBonds(List<Object> bonds) {
    boolean valence = getBooleanSetting(PyMOL.valence);
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

  private void setColixes() {
    colixes = new short[colixList.size()];
    for (int i = colixes.length; --i >= 0;)
      colixes[i] = (short) colixList.get(i).intValue();
  }

  private final static int REP_STICKS    = 0;
  private final static int REP_SPHERES   = 1;  
  private final static int REP_NBSPHERES = 4;
  private final static int REP_CARTOON   = 5;
  private final static int REP_LINES     = 7;
  private final static int REP_NONBONDED = 11;
  private final static int REP_MAX = 12;

  //TODO:
  
  private final static int REP_SURFACE   = 2;
  private final static int REP_LABELS    = 3;  
  private final static int REP_BACKBONE  = 6;
  private final static int REP_MESH      = 8;
  private final static int REP_DOTS      = 9;
  private final static int REP_DASHES    = 10;

  
  private void setShapes() {
    ShapeSettings ss;
    BitSet bs = BitSet.newN(atomCount);
    bs.setBits(0, atomCount);
    ss = new ShapeSettings(JmolConstants.SHAPE_BALLS, bs, null);
    ss.setSize(0);
    ss.setColors(colixes, 0);
    shapes.add(ss);
    ss = new ShapeSettings(JmolConstants.SHAPE_STICKS, bs, null);
    ss.setSize(0);
    shapes.add(ss);
    bs = new BitSet();
    for (int i = 0; i < REP_MAX; i++)
      setShape(i);
    if (!bsHidden.isEmpty())
      shapes.add(new ShapeSettings(0, bsHidden, "hidden"));
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
      ss = new ShapeSettings(JmolConstants.SHAPE_BALLS, bs, null);
      ss.setRadiusData(new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO));
      ss.setColors(colixes, 0);
      shapes.add(ss);
      break;
    case REP_NBSPHERES:
    case REP_SPHERES:
      f = (shapeID == REP_NBSPHERES ? 1 : getFloatSetting(PyMOL.sphere_scale));
      ss = new ShapeSettings(JmolConstants.SHAPE_BALLS, bs, null);
      ss.setRadiusData(new RadiusData(null, f, RadiusData.EnumType.FACTOR, EnumVdw.AUTO));
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
      f = getFloatSetting(PyMOL.line_width) * 8/1000;
      ss = new ShapeSettings(JmolConstants.SHAPE_STICKS, bs, null);
      ss.setSize(f);
      shapes.add(ss);
      break;
    case REP_CARTOON:
      setCartoon("H", PyMOL.cartoon_oval_length, 2);
      setCartoon("S", PyMOL.cartoon_rect_length, 2);
      setCartoon("L", PyMOL.cartoon_loop_radius, 2);
      break;
    default:
    }
  }

  private void setCartoon(String key, int sizeID, float factor) {
    BitSet bs = ssMapA.get(key);
    if (bs == null)
      return;
    bs.and(reps[REP_CARTOON]);
    if (bs.isEmpty())
      return;
    ShapeSettings ss = new ShapeSettings(JmolConstants.SHAPE_CARTOON, bs, null);
    ss.setColors(colixes, getFloatSetting(PyMOL.cartoon_transparency));
    ss.setSize(getFloatSetting(sizeID) * factor);
    shapes.add(ss);
  }
  
  private float getRotationRadius() {
    Point3f center = Point3f.new3(
        (xyzMax.x + xyzMin.x)/2,
        (xyzMax.y + xyzMin.y)/2,
        (xyzMax.z + xyzMin.z)/2);
    float d2max = 0;
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = 0; i < atomCount; i++) {
      Atom a = atoms[i];
      float dx = (a.x - center.x);
      float dy = (a.y - center.y);
      float dz = (a.z - center.z);
      float d2 = dx*dx + dy*dy + dz*dz;
      if (d2 > d2max)
        d2max = d2;
    }
    // 1 is approximate -- for atom radius
    return (float) Math.pow(d2max, 0.5f) + 1;
  }

  private void setView(StringXBuilder sb, List<Object> view) {

    sb.append("set navigationMode off; set zoomLarge false;");
    
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

    Point3f center = Point3f.new3(getFloat(view, 19), getFloat(view, 20),
        getFloat(view, 21));

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

    boolean depthCue = getBooleanSetting(PyMOL.depth_cue); // 84
    sb.append("set zShade " + depthCue + ";");
    if (depthCue) {
      float fog_start = getFloatSetting(PyMOL.fog_start); // 192
      sb.append("set zshadePower 2;set zslab " + (fog_start * 100) + "; set zdepth 0;");
    }
    
    boolean orthographic = getBooleanSetting(PyMOL.orthoscopic);
    sb.append("set perspectiveDepth " + !orthographic + ";");

  }


}
