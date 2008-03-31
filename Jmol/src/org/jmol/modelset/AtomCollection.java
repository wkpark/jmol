/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import java.awt.Rectangle;
import java.util.BitSet;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.atomdata.AtomData;
import org.jmol.bspt.Bspf;
import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;

abstract public class AtomCollection {

  protected void releaseModelSet() {
    atoms = null;
    viewer = null;
    g3d = null;
    bspf = null;
    surfaceDistance100s = null;
    bsSurface = null;
    tainted = null;

    atomNames = null;
    atomSerials = null;
    clientAtomReferences = null;
    vibrationVectors = null;
    occupancies = null;
    bfactor100s = null;
    partialCharges = null;
    specialAtomIDs = null;

  }

  protected void merge(AtomCollection mergeModelSet) {
    tainted = mergeModelSet.tainted;
    atomNames = mergeModelSet.atomNames;
    atomSerials = mergeModelSet.atomSerials;
    clientAtomReferences = mergeModelSet.clientAtomReferences;
    vibrationVectors = mergeModelSet.vibrationVectors;
    occupancies = mergeModelSet.occupancies;
    bfactor100s = mergeModelSet.bfactor100s;
    partialCharges = mergeModelSet.partialCharges;
    specialAtomIDs = mergeModelSet.specialAtomIDs;
  }
  
  public Viewer viewer; //TESTING ONLY
  Graphics3D g3d;

  final protected static boolean showRebondTimes = true;

  public Atom[] atoms;
  int atomCount;
  String[] atomNames;


  public Atom[] getAtoms() {
    return atoms;
  }

  public Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  public int getAtomCount() {
    return atomCount;
  }
  
  public String[] getAtomNames() {
    return atomNames;
  }

  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  //
  // used by Atom:
  //
  int[] atomSerials;
  byte[] specialAtomIDs;
  Object[] clientAtomReferences;
  Vector3f[] vibrationVectors;

  public boolean modelSetHasVibrationVectors(){
    return (vibrationVectors != null);
  }
  
  byte[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  
  public float[] getPartialCharges() {
    return partialCharges;
  }

  public short[] getBFactors() {
    return bfactor100s;
  }

  protected int[] surfaceDistance100s;

  private BitSet bsHidden = new BitSet();

  public void setBsHidden(BitSet bs) { //from selection manager
    bsHidden = bs;
  }

  public boolean isAtomHidden(int iAtom) {
    return bsHidden.get(iAtom);
  }
  
  //////////// atoms //////////////
  
  public String getAtomInfo(int i) {
    return atoms[i].getInfo();
  }

  public String getAtomInfoXYZ(int i, boolean withScreens) {
    return atoms[i].getInfoXYZ(withScreens);
  }

  public String getElementSymbol(int i) {
    return atoms[i].getElementSymbol();
  }

  public int getElementNumber(int i) {
    return atoms[i].getElementNumber();
  }

  String getElementName(int i) {
      return JmolConstants.elementNameFromNumber(atoms[i]
          .getAtomicAndIsotopeNumber());
  }

  public String getAtomName(int i) {
    return atoms[i].getAtomName();
  }

  public int getAtomNumber(int i) {
    return atoms[i].getAtomNumber();
  }

  public float getAtomX(int i) {
    return atoms[i].x;
  }

  public float getAtomY(int i) {
    return atoms[i].y;
  }

  public float getAtomZ(int i) {
    return atoms[i].z;
  }

  public Point3f getAtomPoint3f(int i) {
    return atoms[i];
  }

  public float getAtomRadius(int i) {
    return atoms[i].getRadius();
  }

  public float getAtomVdwRadius(int i) {
    return atoms[i].getVanderwaalsRadiusFloat();
  }

  public short getAtomColix(int i) {
    return atoms[i].getColix();
  }

  public String getAtomChain(int i) {
    return "" + atoms[i].getChainID();
  }

  public String getAtomSequenceCode(int i) {
    return atoms[i].getSeqcodeString();
  }

  public int getAtomModelIndex(int i) {
    return atoms[i].getModelIndex();
  }
  
  protected int getAtomCountInModel(int modelIndex) {
    int n = 0;
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].modelIndex == modelIndex)
        n++;
    return n;
  }
  
  public int getAtomIndexFromAtomNumber(int atomNumber) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < atomCount; i++) {
      if (atoms[i].getAtomNumber() == atomNumber)
        return i;
    }
    return -1;
  }

  public void setFormalCharges(BitSet bs, int formalCharge) {
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        atoms[i].setFormalCharge(formalCharge);
        taint(i, TAINT_FORMALCHARGE);
      }
  }
  
  public void setProteinType(BitSet bs, byte iType) {
    int monomerIndexCurrent = -1;
    int iLast = -1;
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        if (iLast != i - 1)
          monomerIndexCurrent = -1;
        iLast = i;
        monomerIndexCurrent = atoms[i].setProteinStructureType(iType,
            monomerIndexCurrent);
      }
  }
  
  public float calcRotationRadius(Point3f center) {
    float maxRadius = 0;
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  protected float getRadiusVdwJmol(Atom atom) {
    return JmolConstants.getVanderwaalsMar(atom.getElementNumber(),
        JmolConstants.VDW_JMOL) / 1000f;
  }
  
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  protected float maxBondingRadius = Float.MIN_VALUE;
  private float maxVanderwaalsRadius = Float.MIN_VALUE;

  public float getMaxVanderwaalsRadius() {
    //Dots
    if (maxVanderwaalsRadius == Float.MIN_VALUE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  protected void findMaxRadii() {
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float bondingRadius = atom.getBondingRadiusFloat();
      if (bondingRadius > maxBondingRadius)
        maxBondingRadius = bondingRadius;
      float vdwRadius = atom.getVanderwaalsRadiusFloat();
      if (vdwRadius > maxVanderwaalsRadius)
        maxVanderwaalsRadius = vdwRadius;
    }
  }

  private boolean hasBfactorRange;
  private int bfactor100Lo;
  private int bfactor100Hi;

  public void clearBfactorRange() {
    hasBfactorRange = false;
  }

  private void calcBfactorRange(BitSet bs) {
    if (!hasBfactorRange) {
      bfactor100Lo = Integer.MAX_VALUE;
      bfactor100Hi = Integer.MIN_VALUE;
      for (int i = atomCount; --i > 0;)
        if (bs == null || bs.get(i)) {
          int bf = atoms[i].getBfactor100();
          if (bf < bfactor100Lo)
            bfactor100Lo = bf;
          else if (bf > bfactor100Hi)
            bfactor100Hi = bf;
        }
      hasBfactorRange = true;
    }
  }

  public int getBfactor100Lo() {
    //ColorManager
    if (!hasBfactorRange) {
      if (viewer.isRangeSelected()) {
        calcBfactorRange(viewer.getSelectionSet());
      } else {
        calcBfactorRange(null);
      }
    }
    return bfactor100Lo;
  }

  public int getBfactor100Hi() {
    //ColorManager
    getBfactor100Lo();
    return bfactor100Hi;
  }

  private int surfaceDistanceMax;

  public int getSurfaceDistanceMax() {
    //ColorManager, Eval
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistanceMax;
  }

  private BitSet bsSurface;
  private int nSurfaceAtoms;

  int getSurfaceDistance100(int atomIndex) {
    //atom
    if (nSurfaceAtoms == 0)
      return -1;
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistance100s[atomIndex];
  }

  private void calcSurfaceDistances() {
    calculateSurface(null, -1);
  }
  
  public Point3f[] calculateSurface(BitSet bsSelected, float envelopeRadius) {
    if (envelopeRadius < 0)
      envelopeRadius = EnvelopeCalculation.SURFACE_DISTANCE_FOR_CALCULATION;
    EnvelopeCalculation ec = new EnvelopeCalculation(viewer, atomCount, null);
    ec.calculate(Float.MAX_VALUE, envelopeRadius, 1, Float.MAX_VALUE, 
        bsSelected, BitSetUtil.copyInvert(bsSelected, atomCount), 
        true, false, false, false, true);
    Point3f[] points = ec.getPoints();
    surfaceDistanceMax = 0;
    bsSurface = ec.getBsSurfaceClone();
    surfaceDistance100s = new int[atomCount];
    nSurfaceAtoms = BitSetUtil.cardinalityOf(bsSurface);
    if (nSurfaceAtoms == 0 || points == null || points.length == 0)
      return points;
    //for (int i = 0; i < points.length; i++) {
    //  System.out.println("draw pt"+i+" " + Escape.escape(points[i]));
    //}
    float radiusAdjust = (envelopeRadius == Float.MAX_VALUE ? 0 : envelopeRadius);
    for (int i = 0; i < atomCount; i++) {
      //surfaceDistance100s[i] = Integer.MIN_VALUE;
      if (bsSurface.get(i)) {
        surfaceDistance100s[i] = 0;
      } else {
        float dMin = Float.MAX_VALUE;
        Atom atom = atoms[i];
        for (int j = points.length; --j >= 0;) {
          float d = Math.abs(points[j].distance(atom) - radiusAdjust);
          if (d < 0 && Logger.debugging)
            Logger.debug("draw d" + j + " " + Escape.escape(points[j])
                + " \"" + d + " ? " + atom.getInfo() + "\"");
          dMin = Math.min(d, dMin);
        }
        int d = surfaceDistance100s[i] = (int) (dMin * 100);
        surfaceDistanceMax = Math.max(surfaceDistanceMax, d);
      }
    }
    return points;
  }

  public float getMeasurement(int[] countPlusIndices) {
    float value = Float.NaN;
    if (countPlusIndices == null)
      return value;
    int count = countPlusIndices[0];
    if (count < 2)
      return value;
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] < 0) {
        return value;
      }
    switch (count) {
    case 2:
      value = getDistance(countPlusIndices[1], countPlusIndices[2]);
      break;
    case 3:
      value = getAngle(countPlusIndices[1], countPlusIndices[2],
          countPlusIndices[3]);
      break;
    case 4:
      value = getTorsion(countPlusIndices[1], countPlusIndices[2],
          countPlusIndices[3], countPlusIndices[4]);
      break;
    default:
      Logger.error("Invalid count in measurement calculation:" + count);
      throw new IndexOutOfBoundsException();
    }

    return value;
  }

  public float getDistance(int atomIndexA, int atomIndexB) {
    return atoms[atomIndexA].distance(atoms[atomIndexB]);
  }

  public float getAngle(int atomIndexA, int atomIndexB, int atomIndexC) {
    Point3f pointA = atoms[atomIndexA];
    Point3f pointB = atoms[atomIndexB];
    Point3f pointC = atoms[atomIndexC];
    return Measure.computeAngle(pointA, pointB, pointC, true);
  }

  public float getTorsion(int atomIndexA, int atomIndexB, int atomIndexC,
                   int atomIndexD) {
    return Measure.computeTorsion(atoms[atomIndexA], atoms[atomIndexB],
        atoms[atomIndexC], atoms[atomIndexD], true);
  }

  public void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    Point3f xyz = null;
    Point3f[] values = null;
    if (xyzValues instanceof Point3f)
      xyz = (Point3f) xyzValues;
    else
      values = (Point3f[]) xyzValues;
    if (xyz == null && (values == null || values.length == 0))
      return;
    int n = 0;
    for (int i = 0; i < atomCount; i++) {
      if (!bs.get(i))
        continue;
      if (values != null) { 
        if (n >= values.length)
          return;
        xyz = values[n++];
      }
      switch (tokType) {
      case Token.xyz:
        setAtomCoord(i, xyz.x, xyz.y, xyz.z);
        break;
      case Token.fracXyz:
        atoms[i].setFractionalCoord(xyz);
        taint(i, TAINT_COORD);
        break;
      case Token.vibXyz:
        setAtomVibrationVector(i, xyz.x, xyz.y, xyz.z);
        break;
      }
    }
  }

  private void setAtomVibrationVector(int atomIndex, float x, float y, float z) {
    setVibrationVector(atomIndex, x, y, z);  
    taint(atomIndex, TAINT_VIBRATION);
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x = x;
    atoms[atomIndex].y = y;
    atoms[atomIndex].z = z;
    taint(atomIndex, TAINT_COORD);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x += x;
    atoms[atomIndex].y += y;
    atoms[atomIndex].z += z;
    taint(atomIndex, TAINT_COORD);
  }

  protected void setAtomCoordRelative(BitSet atomSet, float x, float y, float z) {
    bspf = null;
    for (int i = atomCount; --i >= 0;)
      if (atomSet.get(i))
        setAtomCoordRelative(i, x, y, z);
  }

  public void setAtomProperty(BitSet bs, int tok, int iValue, float fValue, float[] values) {
    int n = 0;
    if (values != null && values.length == 0)
      return;
    for (int i = 0; i < atomCount; i++) {
      if (!bs.get(i))
        continue;
      if (values != null) {
        if (n >= values.length)
          return;
        fValue = values[n++];
        iValue = (int) fValue;
      }
      Atom atom = atoms[i];
      switch (tok) {
      case Token.atomX:
        setAtomCoord(i, fValue, atom.y, atom.z);
        break;
      case Token.atomY:
        setAtomCoord(i, atom.x, fValue, atom.z);
        break;
      case Token.atomZ:
        setAtomCoord(i, atom.x, atom.y, fValue);
        break;
      case Token.vibX:
      case Token.vibY:
      case Token.vibZ:
        setVibrationVector(i, tok, fValue);
        break;
      case Token.fracX:
      case Token.fracY:
      case Token.fracZ:
        atom.setFractionalCoord(tok, fValue);
        taint(i, TAINT_COORD);
        break;
      case Token.formalCharge:
        atom.setFormalCharge(iValue);
        taint(i, TAINT_FORMALCHARGE);
        break;
      case Token.occupancy:
        setOccupancy(i, iValue);
        taint(i, TAINT_OCCUPANCY);
        break;
      case Token.partialCharge:
        setPartialCharge(i, fValue);
        taint(i, TAINT_PARTIALCHARGE);
        break;
      case Token.temperature:
        setBFactor(i, fValue);
        taint(i, TAINT_TEMPERATURE);
        break;
      case Token.valence:
        atom.setValency(iValue);
        taint(i, TAINT_VALENCE);
        break;
      case Token.vanderwaals:
        if (atom.setRadius(fValue))        
            taint(i, TAINT_VANDERWAALS);
        else
            untaint(i, TAINT_VANDERWAALS);
        break;
      default:
        Logger.error("unsettable atom property: " + Token.nameOf(tok));
        break;        
      }
    }
  }

  public float getVibrationCoord(int atomIndex, char c) {
    if (vibrationVectors == null || vibrationVectors[atomIndex] == null)
      return 0;
    switch (c) {
    case 'x':
      return vibrationVectors[atomIndex].x;
    case 'y':
      return vibrationVectors[atomIndex].y;
    default:
      return vibrationVectors[atomIndex].z;
    }
  }

  public Vector3f getVibrationVector(int atomIndex) {
    return vibrationVectors == null ? null : vibrationVectors[atomIndex];
  }

  protected void setVibrationVector(int atomIndex, float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return;
    if (vibrationVectors == null)
      vibrationVectors = new Vector3f[atoms.length];
    vibrationVectors[atomIndex] = new Vector3f(x, y, z);
    atoms[atomIndex].setVibrationVector();
  }

  private void setVibrationVector(int atomIndex, int tok, float fValue) {
    Vector3f v = getVibrationVector(atomIndex);
    if (v == null)
      v = new Vector3f();
    switch(tok) {
    case Token.vibX:
      v.x = fValue;
      break;
    case Token.vibY:
      v.y = fValue;
      break;
    case Token.vibZ:
      v.z = fValue;
      break;
    }
    setAtomVibrationVector(atomIndex, v.x, v.y, v.z);
  }

  protected void setOccupancy(int atomIndex, int occupancy) {
    if (occupancy < 0)
      occupancy = 0;
    else if (occupancy > 100)
      occupancy = 100;
    if (occupancy != 100) {
      if (occupancies == null)
        occupancies = new byte[atoms.length];
      occupancies[atomIndex] = (byte)occupancy;
    }
  }
  
  protected void setPartialCharge(int atomIndex, float partialCharge) {
    if (Float.isNaN(partialCharge))
      return;
    if (partialCharges == null)
      partialCharges = new float[atoms.length];
    partialCharges[atomIndex] = partialCharge;
  }

  protected void setBFactor(int atomIndex, float bfactor) {
  if (Float.isNaN(bfactor) || bfactor == 0)
    return;
    if (bfactor100s == null)
      bfactor100s = new short[atoms.length];
    bfactor100s[atomIndex] = (short)(bfactor * 100);
  }

  // loading data
  
  public void loadData(int type, String name, String dataString) {
    float[] fData = null;
    BitSet bs = null;
    switch (type) {
    case TAINT_COORD:
      loadCoordinates(dataString, false);
      return;
    case TAINT_VIBRATION:
      loadCoordinates(dataString, true);
      return;
    case TAINT_MAX:
      fData = new float[atomCount];
      bs = new BitSet(atomCount);break;
    }
    int[] lines = Parser.markLines(dataString, ';');
    int n = 0;
    try {
      int nData = Parser.parseInt(dataString.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(dataString.substring(
            lines[i], lines[i + 1] - 1)));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        if (atomIndex < 0 || atomIndex >= atomCount)
          continue;
        n++;
        float x = Parser.parseFloat(tokens[tokens.length - 1]);
        switch (type) {
        case TAINT_MAX:
          fData[atomIndex] = x;
          bs.set(atomIndex);
          continue;
        case TAINT_FORMALCHARGE:
          atoms[atomIndex].setFormalCharge((int)x);          
          break;
        case TAINT_PARTIALCHARGE:
          setPartialCharge(atomIndex, x);          
          break;
        case TAINT_TEMPERATURE:
          setBFactor(atomIndex, x);
          break;
        case TAINT_VALENCE:
          atoms[atomIndex].setValency((int)x);          
          break;
        case TAINT_VANDERWAALS:
          atoms[atomIndex].setRadius(x);          
          break;
        }
        taint(atomIndex, (byte) type);
      }
      if (type == TAINT_MAX && n > 0)
        viewer.setData(name, new Object[] {name, fData, bs}, 0, 0, 0);
        
    } catch (Exception e) {
      Logger.error("AtomCollection.loadData error: " + e);
    }    
  }
  
  private void loadCoordinates(String data, boolean isVibrationVectors) {
    if (!isVibrationVectors)
      bspf = null;
    int[] lines = Parser.markLines(data, ';');
    try {
      int nData = Parser.parseInt(data.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(data.substring(
            lines[i], lines[i + 1])));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        float x = Parser.parseFloat(tokens[3]);
        float y = Parser.parseFloat(tokens[4]);
        float z = Parser.parseFloat(tokens[5]);
        if (isVibrationVectors) {
          setAtomVibrationVector(atomIndex, x, y, z);
        } else {
          setAtomCoord(atomIndex, x, y, z);
        }
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }


  // Binary Space Partitioning Forest
  
  protected Bspf bspf;

  // state tainting
  
  ////  atom coordinate and property changing  //////////
  
  final public static byte TAINT_COORD = 0;
  final private static byte TAINT_FORMALCHARGE = 1;
  final private static byte TAINT_OCCUPANCY = 2;
  final private static byte TAINT_PARTIALCHARGE = 3;
  final private static byte TAINT_TEMPERATURE = 4;
  final private static byte TAINT_VALENCE = 5;
  final private static byte TAINT_VANDERWAALS = 6;
  final private static byte TAINT_VIBRATION = 7;
  final public static byte TAINT_MAX = 8;

  final private static String[] userSettableValues = {
    "coord",
    "formalCharge",
    "occupany",
    "partialCharge",
    "temperature",
    "valency",
    "vanderWaals",
    "vibrationVector"
  };
  
  protected BitSet[] tainted;  // not final -- can be set to null

  public static int getUserSettableType(String dataType) {
    for (int i = 0; i < userSettableValues.length; i++)
      if (userSettableValues[i].equalsIgnoreCase(dataType))
        return i;
    return dataType.toLowerCase().indexOf("property_") == 0 ? TAINT_MAX : -1;
  }

  public BitSet getTaintedAtoms(byte type) {
    return tainted == null ? null : tainted[type];
  }
  
  protected void taint(int atomIndex, byte type) {
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    tainted[type].set(atomIndex);
  }

  private void untaint(int i, byte type) {
    if (tainted == null || tainted[type] == null)
      return;
    tainted[type].clear(i);
  }

  public void setTaintedAtoms(BitSet bs, byte type) {
    if (bs == null) {
      if (tainted == null)
        return;
      tainted[type] = null;
      return;
    }
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    BitSetUtil.copy(bs, tainted[type]);
  }

  public String getAtomicPropertyState(int taintWhat, BitSet bsSelected) {
    BitSet bs;
    StringBuffer commands = new StringBuffer();
    for (byte i = 0; i < TAINT_MAX; i++)
      if (taintWhat < 0 || i == taintWhat)
      if((bs = (bsSelected != null ? bsSelected : getTaintedAtoms(i))) != null)
        getAtomicPropertyState(commands, atoms, atomCount, i, bs, null, null);
    return commands.toString();
  }
  
  public static void getAtomicPropertyState(StringBuffer commands,
                                            Atom[] atoms, int atomCount,
                                            byte type, BitSet bs, String label,
                                            float[] fData) {
    StringBuffer s = new StringBuffer();
    int n = 0;
    String dataLabel = (label == null ? userSettableValues[type] : label)
        + " set";
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        s.append(i + 1).append(" ").append(atoms[i].getElementSymbol()).append(
            " ").append(atoms[i].getInfo().replace(' ', '_')).append(" ");
        switch (type) {
        case TAINT_MAX:
          s.append(" ").append(fData[i]);
          break;
        case TAINT_COORD:
          s.append(" ").append(atoms[i].x).append(" ").append(atoms[i].y)
              .append(" ").append(atoms[i].z);
          break;
        case TAINT_FORMALCHARGE:
          s.append(atoms[i].getFormalCharge());
          break;
        case TAINT_OCCUPANCY:
          s.append(atoms[i].getOccupancy());
          break;
        case TAINT_PARTIALCHARGE:
          s.append(atoms[i].getPartialCharge());
          break;
        case TAINT_TEMPERATURE:
          s.append(atoms[i].getBfactor100() / 100f);
          break;
        case TAINT_VALENCE:
          s.append(atoms[i].getValence());
          break;
        case TAINT_VANDERWAALS:
          s.append(atoms[i].getVanderwaalsRadiusFloat());
          break;
        case TAINT_VIBRATION:
          Vector3f v = atoms[i].getVibrationVector();
          if (v == null)
            v = new Vector3f();
          s.append(" ").append(v.x).append(" ").append(v.y).append(" ").append(
              v.z);
        }
        s.append(" ;\n");
        ++n;
      }
    if (n == 0)
      return;
    commands.append("\n  DATA \"" + dataLabel + "\"\n").append(n).append(
        " ;\nJmol Property Data Format 1 -- Jmol ").append(
        Viewer.getJmolVersion()).append(";\n");
    commands.append(s);
    commands.append("  end \"" + dataLabel + "\";\n");
  }

///////////////////////////////////////////
  
  private final static int minimumPixelSelectionRadius = 6;

  /*
   * generalized; not just balls
   * 
   * This algorithm assumes that atoms are circles at the z-depth
   * of their center point. Therefore, it probably has some flaws
   * around the edges when dealing with intersecting spheres that
   * are at approximately the same z-depth.
   * But it is much easier to deal with than trying to actually
   * calculate which atom was clicked
   *
   * A more general algorithm of recording which object drew
   * which pixel would be very expensive and not worth the trouble
   */
  protected void findNearestAtomIndex(int x, int y, Atom[] closest) {
    Atom champion = null;
    //int championIndex = -1;
    for (int i = atomCount; --i >= 0;) {
      Atom contender = atoms[i];
      if (contender.isClickable()
          && isCursorOnTopOf(contender, x, y, minimumPixelSelectionRadius,
              champion))
        champion = contender;
    }
    closest[0] = champion;
  }

  /**
   * used by Frame and AminoMonomer and NucleicMonomer -- does NOT check for clickability
   * @param contender
   * @param x
   * @param y
   * @param radius
   * @param champion
   * @return true if user is pointing to this atom
   */
  boolean isCursorOnTopOf(Atom contender, int x, int y, int radius,
                          Atom champion) {
    return contender.screenZ > 1 && !g3d.isClippedZ(contender.screenZ)
        && g3d.isInDisplayRange(contender.screenX, contender.screenY)
        && contender.isCursorOnTopOf(x, y, radius, champion);
  }

  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  final BitSet bsEmpty = new BitSet();
  final BitSet bsFoundRectangle = new BitSet();

  public BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (rect.contains(atom.screenX, atom.screenY))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  protected void fillAtomData(AtomData atomData, int mode) {
    atomData.atomXyz = atoms;
    atomData.atomCount = atomCount;
    atomData.atomicNumber = new int[atomCount];
    boolean includeRadii = (mode == AtomData.MODE_FILL_COORDS_AND_RADII);
    if (includeRadii)
      atomData.atomRadius = new float[atomCount];
    for (int i = 0; i < atomCount; i++) {
      if (atomData.modelIndex >= 0 && atoms[i].modelIndex != atomData.firstModelIndex) {
        if (atomData.bsIgnored == null)
          atomData.bsIgnored = new BitSet();
        atomData.bsIgnored.set(i);
        continue;
      }
      atomData.atomicNumber[i] = atoms[i].getElementNumber();
      atomData.lastModelIndex = atoms[i].modelIndex;
      if (includeRadii)
        atomData.atomRadius[i] = (atomData.useIonic ? atoms[i]
            .getBondingRadiusFloat() : atoms[i].getVanderwaalsRadiusFloat());
    }
  }
  
  protected Point3f[][] getAdditionalHydrogens(BitSet atomSet, int[] nTotal) {
    Vector3f z = new Vector3f();
    Vector3f x = new Vector3f();
    Point3f[][] hAtoms = new Point3f[atomCount][];
    Point3f pt;
    int nH = 0;
    // just not doing aldehydes here -- all A-X-B bent == sp3 for now
    for (int i = 0; i < atomCount; i++) {
      if (atomSet.get(i) && atoms[i].getElementNumber() == 6) {

        int n = 0;
        Atom atom = atoms[i];
        int nBonds = (atom.getCovalentHydrogenCount() > 0 ? 0 : atom
            .getCovalentBondCount());
        if (nBonds == 3 || nBonds == 2) { //could be XA3 sp2 or XA2 sp
          String hybridization = getHybridizationAndAxes(i, z, x, "sp3", true);
          if (hybridization == null || hybridization.equals("sp"))
            nBonds = 0;
        }
        if (nBonds > 0 && nBonds <= 4)
          n += 4 - nBonds;
        hAtoms[i] = new Point3f[n];
        nH += n;
        n = 0;
        switch (nBonds) {
        case 1:
          getHybridizationAndAxes(i, z, x, "sp3a", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          getHybridizationAndAxes(i, z, x, "sp3b", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          getHybridizationAndAxes(i, z, x, "sp3c", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          break;
        case 2:
          String hybridization = getHybridizationAndAxes(i, z, x, "sp3", true);
          if (hybridization != null && !hybridization.equals("sp")) {
            getHybridizationAndAxes(i, z, x, "lpa", false);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
            getHybridizationAndAxes(i, z, x, "lpb", false);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
          }
          break;
        case 3:
          if (getHybridizationAndAxes(i, z, x, "sp3", true) != null) {
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
          }
        default:
        }

      }
    }
    nTotal[0] = nH;
    return hAtoms;
  }

  ////// special method for lcaoCartoons
  
  public String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
                           String lcaoTypeRaw, boolean hybridizationCompatible) {
    String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw
        .substring(1)
        : lcaoTypeRaw);
    Atom atom = atoms[atomIndex];
    String hybridization = "";
    z.set(0, 0, 0);
    x.set(0, 0, 0);
    Atom atom1 = atom;
    Atom atom2 = atom;
    int nBonds = 0;
    float _180 = (float) Math.PI * 0.95f;
    Vector3f n = new Vector3f();
    Vector3f x2 = new Vector3f();
    Vector3f x3 = new Vector3f(3.14159f, 2.71828f, 1.41421f);
    Vector3f x4 = new Vector3f();
    Vector3f y1 = new Vector3f();
    Vector3f y2 = new Vector3f();
    if (atom.bonds != null)
      for (int i = atom.bonds.length; --i >= 0;)
        if (atom.bonds[i].isCovalent()) {
          ++nBonds;
          atom1 = atom.bonds[i].getOtherAtom(atom);
          n.sub(atom, atom1);
          n.normalize();
          z.add(n);
          switch (nBonds) {
          case 1:
            x.set(n);
            atom2 = atom1;
            break;
          case 2:
            x2.set(n);
            break;
          case 3:
            x3.set(n);
            x4.set(-z.x, -z.y, -z.z);
            break;
          case 4:
            x4.set(n);
            break;
          default:
            i = -1;
          }
        }
    switch (nBonds) {
    case 0:
      z.set(0, 0, 1);
      x.set(1, 0, 0);
      break;
    case 1:
      if (lcaoType.indexOf("sp3") == 0) { // align z as sp3 orbital
        hybridization = "sp3";
        x.cross(x3, z);
        y1.cross(z, x);
        x.normalize();
        y1.normalize();
        y2.set(x);
        z.normalize();
        x.scaleAdd(2.828f, x, z); // 2*sqrt(2)
        if (!lcaoType.equals("sp3a") && !lcaoType.equals("sp3")) {
          x.normalize();
          AxisAngle4f a = new AxisAngle4f(z.x, z.y, z.z, (lcaoType
              .equals("sp3b") ? 1 : -1) * 2.09439507f); // PI*2/3
          Matrix3f m = new Matrix3f();
          m.setIdentity();
          m.set(a);
          m.transform(x);
        }
        z.set(x);
        x.cross(y1, z);
        break;
      }
      hybridization = "sp";
      if (atom1.getCovalentBondCount() == 3) {
        //special case, for example R2C=O oxygen
        getHybridizationAndAxes(atom1.atomIndex, z, x3, lcaoType, false);
        x3.set(x);
        if (lcaoType.indexOf("sp2") == 0) { // align z as sp2 orbital
          hybridization = "sp2";
          z.scale(-1);
        }
      }
      x.cross(x3, z);
      break;
    case 2:
      if (z.length() < 0.1) {
        // linear A--X--B
        hybridization = "sp";
        if (!lcaoType.equals("pz")) {
          if (atom1.getCovalentBondCount() != 3)
            atom1 = atom2;
          if (atom1.getCovalentBondCount() == 3) {
            //special case, for example R2C=C=CR2 central carbon
            getHybridizationAndAxes(atom1.atomIndex, x, z, "pz", false);
            if (lcaoType.equals("px"))
              x.scale(-1);
            z.set(x2);
            break;
          }
        }
        z.set(x);
        x.cross(x3, z);
        break;
      }
      // bent A--X--B
      hybridization = (lcaoType.indexOf("sp3") == 0 ? "sp3" : "sp2");
      x3.cross(z, x);
      if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
        if (lcaoType.equals("sp2a") || lcaoType.equals("sp2b")) {
          z.set(lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
        }
        x.cross(z, x3);
        break;
      }
      if (lcaoType.indexOf("lp") == 0) { // align z as lone pair
        hybridization = "lp"; //any is OK
        x3.normalize();
        z.normalize();
        y1.scaleAdd(1.2f, x3, z);
        y2.scaleAdd(-1.2f, x3, z);
        if (!lcaoType.equals("lp"))
          z.set(lcaoType.indexOf("b") >= 0 ? y2 : y1);
        x.cross(z, x3);
        break;
      }
      hybridization = lcaoType;
      // align z as p orbital
      x.cross(z, x3);
      z.set(x3);
      if (z.z < 0) {
        z.set(-z.x, -z.y, -z.z);
        x.set(-x.x, -x.y, -x.z);
      }
      break;
    default:
      //3 or 4 bonds
      if (x.angle(x2) < _180)
        y1.cross(x, x2);
      else
        y1.cross(x, x3);
      y1.normalize();
      if (x2.angle(x3) < _180)
        y2.cross(x2, x3);
      else
        y2.cross(x, x3);
      y2.normalize();
      if (Math.abs(y2.dot(y1)) < 0.95f) {
        hybridization = "sp3";
        if (lcaoType.indexOf("sp") == 0) { // align z as sp3 orbital
          z.set(lcaoType.equalsIgnoreCase("sp3")
                  || lcaoType.indexOf("d") >= 0 ? x4
                  : lcaoType.indexOf("c") >= 0 ? x3
                      : lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
          x.set(y1);
        } else { //needs testing here
          if (lcaoType.indexOf("lp") == 0 && nBonds == 3) { // align z as lone pair            
            hybridization = "lp"; //any is OK
          }
          x.cross(z, x);
        }
        break;
      }
      hybridization = "sp2";
      if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
        z.set(lcaoType.equalsIgnoreCase("sp3") || lcaoType.indexOf("d") >= 0 ? x4
                : lcaoType.indexOf("c") >= 0 ? x3
                    : lcaoType.indexOf("b") >= 0 ? x2 : x);
        z.scale(-1);
        x.set(y1);
        break;
      }
      // align z as p orbital
      z.set(y1);
      if (z.z < 0) {
        z.set(-z.x, -z.y, -z.z);
        x.set(-x.x, -x.y, -x.z);
      }
    }

    x.normalize();
    z.normalize();

    if (Logger.debugging) {
      Logger.debug(atom.getInfo() + " nBonds=" + nBonds + " " + hybridization);
    }
    if (hybridizationCompatible) {
      if (hybridization == "")
        return null;
      if (lcaoType.indexOf("p") == 0) {
        if (hybridization == "sp3")
          return null;
      } else {
        if (lcaoType.indexOf(hybridization) < 0)
          return null;
      }
    }
    return hybridization;
  }
  
  /* ******************************************************
   * 
   * These next methods are used by Eval to select for 
   * specific atom sets. They all return a BitSet
   * 
   ********************************************************/
 
  /**
   * general unqualified lookup of atom set type
   * @param tokType
   * @return BitSet; or null if we mess up the type
   */
  protected BitSet getAtomBits(int tokType) {
    switch (tokType) {
    case Token.hetero:
      return getHeteroSet();
    case Token.hydrogen:
      return getHydrogenSet();
    case Token.protein:
      return getProteinSet();
    case Token.carbohydrate:
      return getCarbohydrateSet();
    case Token.nucleic:
      return getNucleicSet();
    case Token.dna:
      return getDnaSet();
    case Token.rna:
      return getRnaSet();
    case Token.purine:
      return getPurineSet();
    case Token.pyrimidine:
      return getPyrimidineSet();
    }
    return null;
  }

  public BitSet getModelBitSet(BitSet atomList) {
    BitSet bs = new BitSet();
    for (int i = 0; i < atomCount; i++)
      if (atomList == null || atomList.get(i))
        bs.set(atoms[i].modelIndex);
    return bs;
  }

  private BitSet getHeteroSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isHetero())
        bs.set(i);
    return bs;
  }

  private BitSet getHydrogenSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getElementNumber() == 1)
        bs.set(i);
    }
    return bs;
  }

  private BitSet getProteinSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isProtein())
        bs.set(i);
    return bs;
  }

  private BitSet getCarbohydrateSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isCarbohydrate())
        bs.set(i);
    return bs;
  }

  private BitSet getNucleicSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isNucleic())
        bs.set(i);
    return bs;
  }

  private BitSet getDnaSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isDna())
        bs.set(i);
    return bs;
  }

  private BitSet getRnaSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isRna())
        bs.set(i);
    return bs;
  }

  private BitSet getPurineSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isPurine())
        bs.set(i);
    return bs;
  }

  private BitSet getPyrimidineSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isPyrimidine())
        bs.set(i);
    return bs;
  }

  /**
   * general lookup for String type
   * @param tokType
   * @param specInfo
   * @return BitSet or null in certain cases
   */
  public BitSet getAtomBits(int tokType, String specInfo) {
    switch (tokType) {
    case Token.identifier:
      return getIdentifierOrNull(specInfo);
    case Token.spec_atom:
      return getSpecAtom(specInfo);
    case Token.spec_name_pattern:
      return getSpecName(specInfo);
    case Token.spec_alternate:
      return getSpecAlternate(specInfo);
    }
    return null;
  }

  protected BitSet getAtomBits(int tokType, int specInfo) {
    switch (tokType) {
    case Token.atomno:
      return getSpecAtomNumber(specInfo);
    case Token.spec_resid:
      return getSpecResid(specInfo);
    case Token.spec_chain:
      return getSpecChain((char) specInfo);
    case Token.spec_seqcode:
      return getSpecSeqcode(specInfo, true);
    }
    return null;
  }

  /**
   * overhauled by RMH Nov 1, 2006.
   * 
   * @param identifier
   * @return null or bs
   */
  private BitSet getIdentifierOrNull(String identifier) {
    //a primitive lookup scheme when [ ] are not used
    //nam
    //na?
    //nam45
    //nam45C
    //nam45^
    //nam45^A
    //nam45^AC -- note, no colon here -- if present, handled separately
    //nam4? does NOT match anything for PDB files, but might for others
    //atom specifiers:
    //H?
    //H32
    //H3?

    //in the case of a ?, we take the whole thing

    BitSet bs = getSpecNameOrNull(identifier);
    if (bs != null || identifier.indexOf("?") > 0)
      return bs;

    int pt = identifier.indexOf("*");
    if (pt > 0)
      return getSpecNameOrNull(identifier.substring(0, pt) + "??????????"
          + identifier.substring(pt + 1));
    int len = identifier.length();
    pt = 0;
    while (pt < len && Character.isLetter(identifier.charAt(pt)))
      ++pt;
    bs = getSpecNameOrNull(identifier.substring(0, pt));
    if (pt == len)
      return bs;
    if (bs == null)
      bs = new BitSet();
    //
    // look for a sequence number or sequence number ^ insertion code
    //
    int pt0 = pt;
    while (pt < len && Character.isDigit(identifier.charAt(pt)))
      ++pt;
    int seqNumber = 0;
    try {
      seqNumber = Integer.parseInt(identifier.substring(pt0, pt));
    } catch (NumberFormatException nfe) {
      return null;
    }
    char insertionCode = ' ';
    if (pt < len && identifier.charAt(pt) == '^')
      if (++pt < len)
        insertionCode = identifier.charAt(pt);
    int seqcode = Group.getSeqcode(seqNumber, insertionCode);
    BitSet bsInsert = getSpecSeqcode(seqcode, false);
    if (bsInsert == null) {
      if (insertionCode != ' ')
        bsInsert = getSpecSeqcode(Character.toUpperCase(identifier.charAt(pt)),
            false);
      if (bsInsert == null)
        return null;
      pt++;
    }
    bs.and(bsInsert);
    if (pt >= len)
      return bs;
    //
    // look for a chain spec -- no colon
    //
    char chainID = identifier.charAt(pt++);
    bs.and(getSpecChain(chainID));
    if (pt == len)
      return bs;
    //
    // not applicable
    //
    return null;
  }

  private BitSet getSpecAtom(String atomSpec) {
    BitSet bs = new BitSet();
    atomSpec = atomSpec.toUpperCase();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].isAtomNameMatch(atomSpec)) {
        bs.set(i);
      }
    }
    return bs;
  }

  private BitSet getSpecName(String name) {
    BitSet bs = getSpecNameOrNull(name);
    if (bs != null)
      return bs;
    int pt = name.indexOf("*");
    if (pt > 0) {
      bs = getSpecNameOrNull(name.substring(0, pt) + "??????????"
          + name.substring(pt + 1));
    }
    return (bs == null ? new BitSet() : bs);
  }

  private BitSet getSpecNameOrNull(String name) {
    BitSet bs = null;
    name = name.toUpperCase();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isGroup3OrNameMatch(name)) {
        if (bs == null)
          bs = new BitSet(i + 1);
        bs.set(i);
      }
    return bs;
  }

  protected BitSet getSpecAlternate(String alternateSpec) {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].isAlternateLocationMatch(alternateSpec))
        bs.set(i);
    }
    return bs;
  }

  protected BitSet getSpecResid(int resid) {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getGroupID() == resid)
        bs.set(i);
    }
    return bs;
  }

  protected BitSet getSpecSeqcode(int seqcode, boolean returnEmpty) {
    BitSet bs = new BitSet();
    int seqNum = Group.getSequenceNumber(seqcode);
    boolean haveSeqNumber = (seqNum != Integer.MAX_VALUE);
    boolean isEmpty = true;
    char insCode = Group.getInsertionCode(seqcode);
    switch (insCode) {
    case '?':
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (!haveSeqNumber 
            || seqNum == Group.getSequenceNumber(atomSeqcode)
            && Group.getInsertionCodeValue(atomSeqcode) != 0) {
          bs.set(i);
          isEmpty = false;
        }
      }
      break;
    default:
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (seqcode == atomSeqcode || 
            !haveSeqNumber && seqcode == Group.getInsertionCodeValue(atomSeqcode) 
            || insCode == '*' && seqNum == Group.getSequenceNumber(atomSeqcode)) {
          bs.set(i);
          isEmpty = false;
        }
      }
    }
    return (!isEmpty || returnEmpty ? bs : null);
  }

  protected BitSet getSpecChain(char chain) {
    boolean caseSensitive = viewer.getChainCaseSensitive();
    if (!caseSensitive)
      chain = Character.toUpperCase(chain);
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      char ch = atoms[i].getChainID();
      if (!caseSensitive)
        ch = Character.toUpperCase(ch);
      if (chain == ch)
        bs.set(i);
    }
    return bs;
  }

  protected BitSet getSpecAtomNumber(int atomno) {
    //for Measures
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getAtomNumber() == atomno)
        bs.set(i);
    }
    return bs;
  }

  protected BitSet getCellSet(int ix, int jy, int kz) {
    BitSet bs = new BitSet();
    Point3f cell = new Point3f(ix / 1000f, jy / 1000f, kz / 1000f);
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isInLatticeCell(cell))
        bs.set(i);
    return bs;
  }

  public int[] getAtomIndices(BitSet bs) {
    int len = bs.size();
    int n = 0;
    int[] indices = new int[atomCount];
    for (int j = 0; j < len; j++)
      if (bs.get(j))
        indices[j] = ++n;
    return indices;
  }

  public BitSet getAtomsWithin(float distance, Point4f plane) {
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float d = Graphics3D.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.atomIndex);
    }
    return bsResult;
  }
  
  public BitSet getVisibleSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isVisible())
        bs.set(i);
    return bs;
  }

  public BitSet getClickableSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isClickable())
        bs.set(i);
    return bs;
  }

  public BitSet getAtomsWithin(int tokType, BitSet bs) {
    switch (tokType) {
    case Token.group:
      return withinGroup(bs);
    case Token.chain:
      return withinChain(bs);
    case Token.structure:
      return withinStructure(bs);
    case Token.model:
      return withinModel(bs);
    case Token.element:
      return withinElement(bs);
    case Token.site:
      return withinSite(bs);
    }
    return null;
  }

  private BitSet withinStructure(BitSet bs) {
    Object structureLast = null;
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (!bs.get(i))
        continue;
      Object structure = atoms[i].getGroup().getStructure();
      if (structure != null && structure != structureLast) {
        for (int j = atomCount; --j >= 0;)
          if (atoms[j].getGroup().getStructure() == structure)
            bsResult.set(j);
        structureLast = structure;
      }
    }
    return bsResult;
  }

  private BitSet withinGroup(BitSet bs) {
    //Logger.debug("withinGroup");
    Group groupLast = null;
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (!bs.get(i))
        continue;
      Atom atom = atoms[i];
      Group group = atom.getGroup();
      if (group != groupLast) {
        group.selectAtoms(bsResult);
        groupLast = group;
      }
    }
    return bsResult;
  }

  private BitSet withinChain(BitSet bs) {
    Chain chainLast = null;
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (!bs.get(i))
        continue;
      Chain chain = atoms[i].getChain();
      if (chain != chainLast) {
        for (int j = atomCount; --j >= 0;)
          if (atoms[j].getChain() == chain)
            bsResult.set(j);
        chainLast = chain;
      }
    }
    return bsResult;
  }

  private BitSet withinModel(BitSet bs) {
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i))
        bsThis.set(atoms[i].modelIndex);
    for (int i = atomCount; --i >= 0;)
      if (bsThis.get(atoms[i].modelIndex))
        bsResult.set(i);
    return bsResult;
  }

  private BitSet withinSite(BitSet bs) {
    //Logger.debug("withinGroup");
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i))
        bsThis.set(atoms[i].atomSite);
    for (int i = atomCount; --i >= 0;)
      if (bsThis.get(atoms[i].atomSite))
        bsResult.set(i);
    return bsResult;
  }

  private BitSet withinElement(BitSet bs) {
    //Logger.debug("withinGroup");
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i))
        bsThis.set(getElementNumber(i));
    for (int i = atomCount; --i >= 0;)
      if (bsThis.get(getElementNumber(i)))
        bsResult.set(i);
    return bsResult;
  }

  public void deleteAtoms(int firstAtomIndex, int nAtoms, BitSet bs) {
    BitSetUtil.deleteBits(bsHidden, bs);
    BitSetUtil.deleteBits(viewer.getSelectionSet(), bs);
    BitSetUtil.deleteBits(viewer.getSelectionSubset(), bs);
    atoms = (Atom[]) ArrayUtil.deleteElements(atoms, firstAtomIndex, nAtoms);
    atomCount = atoms.length;
    for (int j = firstAtomIndex; j < atomCount; j++) {
      atoms[j].atomIndex = j;
      atoms[j].modelIndex--;
    }
    //System.out.println("atomcollection deleteAtoms atomslen=" + atoms.length);

    atomNames = (String[]) ArrayUtil.deleteElements(atomNames, firstAtomIndex,
        nAtoms);
    atomSerials = (int[]) ArrayUtil.deleteElements(atomSerials, firstAtomIndex,
        nAtoms);
    bfactor100s = (short[]) ArrayUtil.deleteElements(bfactor100s,
        firstAtomIndex, nAtoms);
    hasBfactorRange = false;
    occupancies = (byte[]) ArrayUtil.deleteElements(occupancies,
        firstAtomIndex, nAtoms);
    partialCharges = (float[]) ArrayUtil.deleteElements(partialCharges,
        firstAtomIndex, nAtoms);
    specialAtomIDs = (byte[]) ArrayUtil.deleteElements(specialAtomIDs,
        firstAtomIndex, nAtoms);
    vibrationVectors = (Vector3f[]) ArrayUtil.deleteElements(vibrationVectors,
        firstAtomIndex, nAtoms);
    clientAtomReferences = (Object[]) ArrayUtil.deleteElements((Object) clientAtomReferences,
        firstAtomIndex, nAtoms);
    nSurfaceAtoms = 0;
    bsSurface = null;
    surfaceDistance100s = null;
    if (tainted != null)
      for (int i = 0; i < TAINT_MAX; i++)
        BitSetUtil.deleteBits(tainted[i], bs);
  }

}

