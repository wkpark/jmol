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
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.bspt.Bspf;
import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import org.jmol.util.Measure;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.script.Token;
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
    atomTypes = null;
    atomSerials = null;
    vibrationVectors = null;
    occupancies = null;
    bfactor100s = null;
    partialCharges = null;
    ionicRadii = null;
    ellipsoids = null;

  }

  protected void mergeAtomArrays(AtomCollection mergeModelSet) {
    tainted = mergeModelSet.tainted;
    atomNames = mergeModelSet.atomNames;
    atomTypes = mergeModelSet.atomTypes;
    atomSerials = mergeModelSet.atomSerials;
    vibrationVectors = mergeModelSet.vibrationVectors;
    occupancies = mergeModelSet.occupancies;
    bfactor100s = mergeModelSet.bfactor100s;
    ionicRadii = mergeModelSet.ionicRadii;
    partialCharges = mergeModelSet.partialCharges;
    ellipsoids = mergeModelSet.ellipsoids;
    setHaveStraightness(false);
    surfaceDistance100s = null;
  }
  
  public void setHaveStraightness(boolean TF) {
    haveStraightness = TF;
  }
  
  protected boolean getHaveStraightness() {
    return haveStraightness;
  }
  
  public Viewer viewer;
  protected Graphics3D g3d;

  public Atom[] atoms;
  int atomCount;

  public Vector getAtomPointVector(BitSet bs) {
    Vector v = new Vector();
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        v.add(atoms[i]);
    return v;
  }

  public int getAtomCount() {
    // not established until AFTER model loading
    return atomCount;
  }
  
  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  //
  // used by Atom:
  //
  String[] atomNames;
  String[] atomTypes;
  int[] atomSerials;
  public Vector3f[] vibrationVectors;
  byte[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  float[] ionicRadii;
  protected Object[][] ellipsoids;
  protected int[] surfaceDistance100s;

  protected boolean haveStraightness;

  public boolean modelSetHasVibrationVectors(){
    return (vibrationVectors != null);
  }
  
  public String[] getAtomNames() {
    return atomNames;
  }

  public String[] getAtomTypes() {
    return atomTypes;
  }

  
  public float[] getPartialCharges() {
    return partialCharges;
  }

  public float[] getIonicRadii() {
    return ionicRadii;
  }
  
  public short[] getBFactors() {
    return bfactor100s;
  }

  private BitSet bsHidden = new BitSet();

  public void setBsHidden(BitSet bs) { //from selection manager
    bsHidden = bs;
  }

  public boolean isAtomHidden(int iAtom) {
    return bsHidden.get(iAtom);
  }
  
  //////////// atoms //////////////
  
  public String getAtomInfo(int i, String format) {
    return (format == null ? atoms[i].getInfo() : LabelToken.formatLabel(viewer, atoms[i],format));
  }

  public String getAtomInfoXYZ(int i, boolean useChimeFormat) {
    return atoms[i].getInfoXYZ(useChimeFormat);
  }

  public String getElementSymbol(int i) {
    return atoms[i].getElementSymbol();
  }

  public int getElementNumber(int i) {
    return atoms[i].getElementNumber();
  }

  String getElementName(int i) {
      return Elements.elementNameFromNumber(atoms[i]
          .getAtomicAndIsotopeNumber());
  }

  public String getAtomName(int i) {
    return atoms[i].getAtomName();
  }

  public int getAtomNumber(int i) {
    return atoms[i].getAtomNumber();
  }

  public Point3f getAtomPoint3f(int i) {
    return atoms[i];
  }

  public float getAtomRadius(int i) {
    return atoms[i].getRadius();
  }

  public float getAtomVdwRadius(int i, int iType) {
    return atoms[i].getVanderwaalsRadiusFloat(viewer, iType);
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
  
  public Object[] getEllipsoid(int i) {
    return (i < 0 || ellipsoids == null || i >= ellipsoids.length ? null
        : ellipsoids[i]);
  }

  public Quaternion getQuaternion(int i, char qtype) {
    return (i < 0 ? null : atoms[i].group.getQuaternion(qtype));
  } 

  public Object getHelixData(BitSet bs, int tokType) {
    int iAtom = bs.nextSetBit(0);
    return (iAtom < 0 ? "null"
        : atoms[iAtom].group.getHelixData(tokType, 
        viewer.getQuaternionFrame(), viewer.getHelixStep()));
  }
  
  public int getAtomIndexFromAtomNumber(int atomNumber, BitSet bsVisibleFrames) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < atomCount; i++) {
      Atom atom = atoms[i];
      if (atom.getAtomNumber() == atomNumber && bsVisibleFrames.get(atom.modelIndex))
        return i;
    }
    return -1;
  }

  public void setFormalCharges(BitSet bs, int formalCharge) {
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        atoms[i].setFormalCharge(formalCharge);
        taint(i, TAINT_FORMALCHARGE);
      }
  }
  
  public float[] getAtomicCharges() {
    float[] charges = new float[atomCount];
    for (int i = atomCount; --i >= 0; )
      charges[i] = atoms[i].getElementNumber();
    return charges;
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
      float vdwRadius = atom.getVanderwaalsRadiusFloat(viewer, JmolConstants.VDW_AUTO);
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
    if (hasBfactorRange)
      return;
    bfactor100Lo = Integer.MAX_VALUE;
    bfactor100Hi = Integer.MIN_VALUE;
    if (bs == null) {
      for (int i = 0; i < atomCount; i++)
        setBf(i);
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        setBf(i);
    }
    hasBfactorRange = true;
  }

  private void setBf(int i) {
    int bf = atoms[i].getBfactor100();
    if (bf < bfactor100Lo)
      bfactor100Lo = bf;
    else if (bf > bfactor100Hi)
      bfactor100Hi = bf;    
  }
  
  public int getBfactor100Lo() {
    //ColorManager
    if (!hasBfactorRange) {
      if (viewer.isRangeSelected()) {
        calcBfactorRange(viewer.getSelectionSet(false));
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

  public float calculateVolume(BitSet bs, int iType) {
    // Eval
    float volume = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        volume += atoms[i].getVolume(viewer, iType);
    return volume;
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
    EnvelopeCalculation ec = new EnvelopeCalculation(viewer, atomCount, null, viewer.getTestFlag2());
    ec.calculate(new RadiusData(envelopeRadius, RadiusData.TYPE_ABSOLUTE, 0), 
        Float.MAX_VALUE, 
        bsSelected, BitSetUtil.copyInvert(bsSelected, atomCount), 
        false, false, false, true);
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

  public void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    Point3f xyz = null;
    Point3f[] values = null;
    Vector v = null;
    int type = 0;
    int nValues = 1;
    if (xyzValues instanceof Point3f) {
      xyz = (Point3f) xyzValues;
      if (xyz == null)
        return;
    } else if (xyzValues instanceof Vector) {
      v = (Vector) xyzValues;
      if (v == null || (nValues = v.size()) == 0)
        return;
      type = 1;
    } else if (xyzValues instanceof Point3f[]){
      values = (Point3f[]) xyzValues;
      if (values == null || (nValues = values.length) == 0)
        return;
      type = 2;
    } else {
      return;
    }
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) { 
        switch (type) {
        case 1:
          if (n >= nValues)
            return;
          xyz = (Point3f) v.get(n++);
          break;
        case 2:
          if (n >= nValues)
            return;
          xyz = values[n++];
          break;
        }
        switch (tokType) {
        case Token.xyz:
          setAtomCoord(i, xyz.x, xyz.y, xyz.z);
          break;
        case Token.fracxyz:
          atoms[i].setFractionalCoord(xyz, true);
          taint(i, TAINT_COORD);
          break;
        case Token.fuxyz:
          atoms[i].setFractionalCoord(xyz, false);
          taint(i, TAINT_COORD);
          break;
        case Token.vibxyz:
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

  protected void setAtomCoordRelative(BitSet bs, float x, float y,
                                      float z) {
    bspf = null;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        setAtomCoordRelative(i, x, y, z);
  }

  public void setAtomProperty(BitSet bs, int tok, int iValue,
                              float fValue, String sValue, float[] values,
                              String[] list) {
    int n = 0;
    
    if (values != null && values.length == 0 || bs == null)
      return;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (values != null) {
          if (n >= values.length)
            return;
          fValue = values[n++];
          iValue = (int) fValue;
        } else if (list != null) {
          if (n >= list.length)
            return;
          sValue = list[n++];
        }
        Atom atom = atoms[i];
        switch (tok) {
        case Token.atomname:
          taint(i, TAINT_ATOMNAME);
          setAtomName(i, sValue);
          break;
        case Token.atomno:
          taint(i, TAINT_ATOMNO);
          setAtomNumber(i, iValue);
          break;
        case Token.atomtype:
          taint(i, TAINT_ATOMTYPE);
          setAtomType(i, sValue);
          break;
        case Token.atomx:
        case Token.x:
          setAtomCoord(i, fValue, atom.y, atom.z);
          break;
        case Token.atomy:
        case Token.y:
          setAtomCoord(i, atom.x, fValue, atom.z);
          break;
        case Token.atomz:
        case Token.z:
          setAtomCoord(i, atom.x, atom.y, fValue);
          break;
        case Token.vibx:
        case Token.viby:
        case Token.vibz:
          setVibrationVector(i, tok, fValue);
          break;
        case Token.fracx:
        case Token.fracy:
        case Token.fracz:
          atom.setFractionalCoord(tok, fValue, true);
          taint(i, TAINT_COORD);
          break;
        case Token.fux:
        case Token.fuy:
        case Token.fuz:
          atom.setFractionalCoord(tok, fValue, false);
          taint(i, TAINT_COORD);
          break;
        case Token.elemno:
        case Token.element:
          setElement(atom, iValue);
          break;
        case Token.formalcharge:
          atom.setFormalCharge(iValue);
          taint(i, TAINT_FORMALCHARGE);
          break;
        case Token.label:
        case Token.format:
          viewer.setAtomLabel(sValue, i);
          break;
        case Token.occupancy:
          if (iValue < 2)
            iValue = (int) (100 * fValue);
          if (setOccupancy(i, iValue))
            taint(i, TAINT_OCCUPANCY);
          break;
        case Token.partialcharge:
          if (setPartialCharge(i, fValue))
            taint(i, TAINT_PARTIALCHARGE);
          break;
        case Token.ionic:
          if (setIonicRadius(i, fValue))
            taint(i, TAINT_IONICRADIUS);
          break;
        case Token.radius:
        case Token.spacefill:
          if (fValue < 0)
            fValue = 0;
          else if (fValue > Atom.RADIUS_MAX)
            fValue = Atom.RADIUS_MAX;
          atom.madAtom = ((short) (fValue * 2000));
          break;
        case Token.selected:
          viewer.setSelectedAtom(atom.index, (fValue != 0));
          break;
        case Token.temperature:
          if (setBFactor(i, fValue))
            taint(i, TAINT_TEMPERATURE);
          break;
        case Token.valence:
          atom.setValence(iValue);
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
      if (tok == Token.selected)
        viewer.setSelectedAtom(-1, false);
  }

  protected void setElement(Atom atom, int atomicNumber) {
    taint(atom.index, TAINT_ELEMENT);
    atom.setAtomicAndIsotopeNumber(atomicNumber);
    atom.setPaletteID(JmolConstants.PALETTE_CPK);
    atom.setColixAtom(viewer.getColixAtomPalette(atom,
        JmolConstants.PALETTE_CPK));
  }

  public float getVibrationCoord(int atomIndex, char c) {
    if (vibrationVectors == null || vibrationVectors[atomIndex] == null)
      return 0;
    switch (c) {
    case 'X':
      return vibrationVectors[atomIndex].x;
    case 'Y':
      return vibrationVectors[atomIndex].y;
    default:
      return vibrationVectors[atomIndex].z;
    }
  }

  public Vector3f getVibrationVector(int atomIndex, boolean forceNew) {
    Vector3f v = (vibrationVectors == null ? null : vibrationVectors[atomIndex]);
    return (v == null && forceNew ? new Vector3f() : v);
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
    Vector3f v = getVibrationVector(atomIndex, true);
    if (v == null)
      v = new Vector3f();
    switch(tok) {
    case Token.vibx:
      v.x = fValue;
      break;
    case Token.viby:
      v.y = fValue;
      break;
    case Token.vibz:
      v.z = fValue;
      break;
    }
    setAtomVibrationVector(atomIndex, v.x, v.y, v.z);
  }

  protected void setAtomName(int atomIndex, String name) {
    byte id = JmolConstants.lookupSpecialAtomID(name);
    atoms[atomIndex].atomID = id;
    if (id > 0 && ((ModelCollection)this).models[atoms[atomIndex].modelIndex].isPDB)
      return;
    if (atomNames == null)
      atomNames = new String[atoms.length];
    atomNames[atomIndex] = name;
  }

  protected void setAtomType(int atomIndex, String type) {
      if (atomTypes == null)
        atomTypes = new String[atoms.length];
      atomTypes[atomIndex] = type;
  }
  
  protected boolean setAtomNumber(int atomIndex, int atomno) {
    if (atomSerials == null) {
      atomSerials = new int[atoms.length];
    }
    atomSerials[atomIndex] = atomno;
    return true;
  }
  
  protected boolean setOccupancy(int atomIndex, int occupancy) {
    if (occupancies == null) {
      if (occupancy == 100)
        return false; // 100 is the default;
      occupancies = new byte[atoms.length];
    }
    occupancies[atomIndex] = (byte) (occupancy > 255 ? 255 : occupancy < 0 ? 0 : occupancy);
    return true;
  }
  
  protected boolean setPartialCharge(int atomIndex, float partialCharge) {
    if (Float.isNaN(partialCharge))
      return false;
    if (partialCharges == null) {
      if (partialCharge == 0)
        return false; // no need to store a 0.
      partialCharges = new float[atoms.length];
    }
    partialCharges[atomIndex] = partialCharge;
    return true;
  }

  protected boolean setIonicRadius(int atomIndex, float radius) {
    if (Float.isNaN(radius))
      return false;
    if (ionicRadii == null) {
      ionicRadii = new float[atoms.length];
    }
    ionicRadii[atomIndex] = radius;
    return true;
  }

  protected boolean setBFactor(int atomIndex, float bfactor) {
    if (Float.isNaN(bfactor))
      return false;
    if (bfactor100s == null) {
      if (bfactor == 0 && bfactor100s == null) // there's no need to store a 0.
        return false;
      bfactor100s = new short[atoms.length];
    }
    bfactor100s[atomIndex] = (short) ((bfactor < -327.68f ? -327.68f
        : bfactor > 327.67 ? 327.67 : bfactor) * 100);
    return true;
  }

  protected void setEllipsoid(int atomIndex, Object[] ellipsoid) {
    if (ellipsoid == null)
      return;
    if (ellipsoids == null)
      ellipsoids = new Object[atoms.length][];
    ellipsoids[atomIndex] = ellipsoid;
  }

  // loading data
  
  public void setAtomData(int type, String name, String dataString, boolean isDefault) {
    float[] fData = null;
    BitSet bs = null;
    switch (type) {
    case TAINT_COORD:
      loadCoordinates(dataString, false, !isDefault);
      return;
    case TAINT_VIBRATION:
      loadCoordinates(dataString, true, true);
      return;
    case TAINT_MAX:
      fData = new float[atomCount];
      bs = new BitSet(atomCount);
      break;
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
        Atom atom = atoms[atomIndex];
        n++;
        int pt = tokens.length - 1;
        float x = Parser.parseFloat(tokens[pt]);
        switch (type) {
        case TAINT_MAX:
          fData[atomIndex] = x;
          bs.set(atomIndex);
          continue;
        case TAINT_ATOMNO:
          setAtomNumber(atomIndex, (int) x);
          break;
        case TAINT_ATOMNAME:
          setAtomName(atomIndex, tokens[pt]);
          break;
        case TAINT_ATOMTYPE:
          setAtomType(atomIndex, tokens[pt]);
          break;
        case TAINT_ELEMENT:
          atom.setAtomicAndIsotopeNumber((int)x);
          atom.setPaletteID(JmolConstants.PALETTE_CPK);
          atom.setColixAtom(viewer.getColixAtomPalette(atom, JmolConstants.PALETTE_CPK));
          break;
        case TAINT_FORMALCHARGE:
          atom.setFormalCharge((int)x);          
          break;
        case TAINT_PARTIALCHARGE:
          setPartialCharge(atomIndex, x);          
          break;
        case TAINT_IONICRADIUS:
          setIonicRadius(atomIndex, x);          
          break;
        case TAINT_TEMPERATURE:
          setBFactor(atomIndex, x);
          break;
        case TAINT_VALENCE:
          atom.setValence((int)x);          
          break;
        case TAINT_VANDERWAALS:
          atom.setRadius(x);          
          break;
        }
        taint(atomIndex, (byte) type);
      }
      if (type == TAINT_MAX && n > 0)
        viewer.setData(name, new Object[] {name, fData, bs}, 0, 0, 0, 0, 0);
        
    } catch (Exception e) {
      Logger.error("AtomCollection.loadData error: " + e);
    }    
  }
  
  private void loadCoordinates(String data, boolean isVibrationVectors, boolean doTaint) {
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
          if (!doTaint)
            untaint(atomIndex, TAINT_COORD);
        }
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }


  // Binary Space Partitioning Forest
  
  protected Bspf bspf;

  // state tainting
  
  protected boolean preserveState = true;
  
  public void setPreserveState(boolean TF) {
    preserveState = TF;
  }
  ////  atom coordinate and property changing  //////////
  
  // be sure to add the name to the list below as well!
  final public static byte TAINT_ATOMNAME = 0;
  final public static byte TAINT_ATOMTYPE = 1;
  final public static byte TAINT_COORD = 2;
  final public static byte TAINT_ELEMENT = 3;
  public final static byte TAINT_FORMALCHARGE = 4;
  final private static byte TAINT_IONICRADIUS = 5;
  final private static byte TAINT_OCCUPANCY = 6;
  final private static byte TAINT_PARTIALCHARGE = 7;
  final private static byte TAINT_TEMPERATURE = 8;
  final private static byte TAINT_VALENCE = 9;
  final private static byte TAINT_VANDERWAALS = 10;
  final private static byte TAINT_VIBRATION = 11;
  final public static byte TAINT_ATOMNO = 12;
  final public static byte TAINT_MAX = 13; // 1 more than last number, above
  
  final private static String[] userSettableValues = {
    "atomName",
    "atomType",
    "coord",
    "element",
    "formalCharge",
    "ionic",
    "occupany",
    "partialCharge",
    "temperature",
    "valence",
    "vanderWaals",
    "vibrationVector",
    "atomNo"
  };
  
  static {
   if (userSettableValues.length != TAINT_MAX)
     Logger.error("AtomCollection.java userSettableValues is not length TAINT_MAX!");
  }
  
  protected BitSet[] tainted;  // not final -- can be set to null

  public static int getUserSettableType(String dataType) {
    boolean isExplicit = (dataType.indexOf("property_") == 0);
    String check = (isExplicit ? dataType.substring(9) : dataType);
    for (int i = 0; i < TAINT_MAX; i++)
      if (userSettableValues[i].equalsIgnoreCase(check))
        return i;
    return (isExplicit ? TAINT_MAX : -1);
  }

  private boolean isTainted(int atomIndex, byte type) {
    return (tainted != null && tainted[type] != null 
        && tainted[type].get(atomIndex));
  }

  public BitSet getTaintedAtoms(byte type) {
    return tainted == null ? null : tainted[type];
  }
  
  public void taint(BitSet bsAtoms, byte type) {
    if (!preserveState)
      return;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      taint(i, type);
  }

  protected void taint(int atomIndex, byte type) {
    if (!preserveState)
      return;
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    tainted[type].set(atomIndex);
  }

  private void untaint(int atomIndex, byte type) {
    if (!preserveState)
      return;
    if (tainted == null || tainted[type] == null)
      return;
    tainted[type].clear(atomIndex);
  }

  public void setTaintedAtoms(BitSet bs, byte type) {
    if (!preserveState)
      return;
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

  public void unTaintAtoms(BitSet bs, byte type) {
    if (tainted == null || tainted[type] == null)
      return;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
      tainted[type].clear(i);
    if (tainted[type].nextSetBit(0) < 0)
      tainted[type] = null;
  }

  public String getAtomicPropertyState(int taintWhat, BitSet bsSelected) {
    if (!preserveState)
      return "";
    BitSet bs;
    StringBuffer commands = new StringBuffer();
    for (byte i = 0; i < TAINT_MAX; i++)
      if (taintWhat < 0 || i == taintWhat)
      if((bs = (bsSelected != null ? bsSelected : getTaintedAtoms(i))) != null)
        getAtomicPropertyState(commands, i, bs, null, null);
    return commands.toString();
  }
  
  public void getAtomicPropertyState(StringBuffer commands, 
                                     byte type, BitSet bs,
                                     String label, float[] fData) {
    if (!viewer.getPreserveState())
      return;
    // see setAtomData()
    StringBuffer s = new StringBuffer();
    String dataLabel = (label == null ? userSettableValues[type] : label)
        + " set";
    int n = 0;
    boolean isDefault = (type == TAINT_COORD);
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        s.append(i + 1).append(" ").append(atoms[i].getElementSymbol()).append(
            " ").append(atoms[i].getInfo().replace(' ', '_')).append(" ");
        switch (type) {
        case TAINT_MAX:
          if (i < fData.length) // when data are appended, the array may not
            // extend that far
            s.append(fData[i]);
          break;
        case TAINT_ATOMNO:
          s.append(atoms[i].getAtomNumber());
          break;
        case TAINT_ATOMNAME:
          s.append(atoms[i].getAtomName());
          break;
        case TAINT_ATOMTYPE:
          s.append(atoms[i].getAtomType());
          break;
        case TAINT_COORD:
          if (isTainted(i, TAINT_COORD))
            isDefault = false;
          s.append(atoms[i].x).append(" ").append(atoms[i].y).append(" ")
              .append(atoms[i].z);
          break;
        case TAINT_VIBRATION:
          Vector3f v = atoms[i].getVibrationVector();
          if (v == null)
            v = new Vector3f();
          s.append(v.x).append(" ").append(v.y).append(" ").append(v.z);
        case TAINT_ELEMENT:
          s.append(atoms[i].getAtomicAndIsotopeNumber());
          break;
        case TAINT_FORMALCHARGE:
          s.append(atoms[i].getFormalCharge());
          break;
        case TAINT_IONICRADIUS:
          s.append(atoms[i].getBondingRadiusFloat());
          break;
        case TAINT_OCCUPANCY:
          s.append(atoms[i].getOccupancy100());
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
          s.append(atoms[i].getVanderwaalsRadiusFloat(viewer,
              JmolConstants.VDW_AUTO));
          break;
        }
        s.append(" ;\n");
        ++n;
      }
    if (n == 0)
      return;
    if (isDefault)
      dataLabel += "(default)";
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
  protected void findNearestAtomIndex(int x, int y, Atom[] closest, BitSet bsNot) {
    Atom champion = null;
    //int championIndex = -1;
    for (int i = atomCount; --i >= 0;) {
      if (bsNot != null && bsNot.get(i))
        continue;
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
  private final BitSet bsEmpty = new BitSet();
  private final BitSet bsFoundRectangle = new BitSet();

  public BitSet findAtomsInRectangle(Rectangle rect, BitSet bsModels) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (bsModels.get(atom.modelIndex) && atom.isVisible(0) 
          && rect.contains(atom.screenX, atom.screenY))
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
      Atom atom = atoms[i];
      if (atom.isDeleted() || atomData.modelIndex >= 0
          && atom.modelIndex != atomData.firstModelIndex) {
        if (atomData.bsIgnored == null)
          atomData.bsIgnored = new BitSet();
        atomData.bsIgnored.set(i);
        continue;
      }
      atomData.atomicNumber[i] = atom.getElementNumber();
      atomData.lastModelIndex = atom.modelIndex;
      if (includeRadii) {
        float r = 0;
        RadiusData rd = atomData.radiusData;
        switch (rd.type) {
        case RadiusData.TYPE_ABSOLUTE:
          r = rd.value;
          break;
        case RadiusData.TYPE_FACTOR:
        case RadiusData.TYPE_OFFSET:
          switch (rd.vdwType) {
          case Token.ionic:
            r = atom.getBondingRadiusFloat();
            break;
          case Token.adpmax:
            r = atom.getADPMinMax(true);
            break;
          case Token.adpmin:
            r = atom.getADPMinMax(false);
            break;
          default:
            r = atom.getVanderwaalsRadiusFloat(viewer,
                atomData.radiusData.vdwType);
          }
          if (rd.type == RadiusData.TYPE_FACTOR)
            r *= rd.value;
          else
            r += rd.value;
        }
        atomData.atomRadius[i] = r + rd.valueExtended;
      }
    }
  }
  
  /**
   * get a list of potential H atom positions based on 
   * elemental valence and formal charge
   * 
   * @param bs
   * @param nTotal
   * @param doAll       -- whether we add to C that already have H or not.
   * @param justCarbon
   * @param vConnect 
   * @return     array of arrays of points added to specific atoms
   */
  public Point3f[][] getAdditionalHydrogens(BitSet bs, int[] nTotal,
                                            boolean doAll, boolean justCarbon,
                                            Vector vConnect) {
    Vector3f z = new Vector3f();
    Vector3f x = new Vector3f();
    Point3f[][] hAtoms = new Point3f[atomCount][];
    BitSet bsDeleted = viewer.getDeletedAtoms();
    Point3f pt;
    int nH = 0;
    // just not doing aldehydes here -- all A-X-B bent == sp3 for now
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (bsDeleted != null && bsDeleted.get(i))
          continue;
        Atom atom = atoms[i];
        if (justCarbon && atom.getElementNumber() != 6)
          continue;
        if (doAll && atom.getCovalentHydrogenCount() > 0)
          continue;
        int n = atom.getImplicitHydrogenCount();
        if (n == 0)
          continue;
        int nBonds = atom.getCovalentBondCount();
        int targetValence = atom.getTargetValence();
        int atomicNumber = atom.getElementNumber();

        hAtoms[i] = new Point3f[n];
        //System.out.println(atom.getInfo() + " targetValence=" + targetValence + " nB="
        //+ nBonds + " nVal=" + nVal + " n=" + n);
        int hPt = 0;
        if (nBonds == 0) {
          switch (n) {
          case 4:
            z.set(0.635f, 0.635f, 0.635f);
            pt = new Point3f(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
          // fall through
          case 3:
            z.set(-0.635f, -0.635f, 0.635f);
            pt = new Point3f(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
          // fall through
          case 2:
            z.set(-0.635f, 0.635f, -0.635f);
            pt = new Point3f(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
          // fall through
          case 1:
            z.set(0.635f, -0.635f, -0.635f);
            pt = new Point3f(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
          }
        } else {
          switch (n) {
          default:
            break;
          case 3: // three bonds needed RC
            getHybridizationAndAxes(i, z, x, "sp3a", false, true, -1);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
            getHybridizationAndAxes(i, z, x, "sp3b", false, true, -1);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
            getHybridizationAndAxes(i, z, x, "sp3c", false, true, -1);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
            break;
          case 2:
            // 2 bonds needed R2C or R-N or R2C=C or O
            //                    or RC=C or C=C
            boolean isEne = (atomicNumber == 5 || nBonds == 1
                && targetValence == 4);
            getHybridizationAndAxes(i, z, x, (isEne ? "sp2b"
                : targetValence == 3 ? "sp3b" : "lpa"), false, true, -1);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
            getHybridizationAndAxes(i, z, x, (isEne ? "sp2c"
                : targetValence == 3 ? "sp3c" : "lpb"), false, true, -1);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.add(atom);
            break;
          case 1:
            // one bond needed R2B, R3C, R-N-R, R-O R=C-R R=N R-3-C
            // nbonds ......... 2 .. 3 .. 2 ... 1 ... 2 .. 1 .. 1
            // nval ........... 2 .. 3 .. 2 ... 1 ... 3 .. 2 .. 3
            // targetValence .. 3 .. 4 .. 3 ... 2 ... 4 .. 3 .. 4
            // ................sp3 . sp3 . sp3 . sp2 sp2 . sp
            switch (targetValence - nBonds) {
            case 1:
              // sp3 or Boron sp2
              getHybridizationAndAxes(i, z, x, (atomicNumber == 5 ? "sp2c"
                  : targetValence == 2 ? "sp3b" : "lpa"), false, false, -1);
              pt = new Point3f(z);
              pt.scaleAdd(1.1f, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.add(atom);
              break;
            case 2:
              // sp2
              getHybridizationAndAxes(i, z, x, (targetValence == 4 ? "sp2c"
                  : "sp2b"), false, false, -1);
              pt = new Point3f(z);
              pt.scaleAdd(1.1f, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.add(atom);
              break;
            case 3:
              // sp
              getHybridizationAndAxes(i, z, x, "sp", false, true, -1);
              pt = new Point3f(z);
              pt.scaleAdd(1.1f, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.add(atom);
              break;
            }
          }
        }
        nH += hPt;
      }
    nTotal[0] = nH;
    return hAtoms;
  }

  ////// special method for lcaoCartoons

  private static float sqrt3_2 = (float) (Math.sqrt(3) / 2);

  public String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
                                        String lcaoTypeRaw,
                                        boolean hybridizationCompatible, boolean doAlignZ, int atomIndexNot) {
    String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw
        .substring(1)
        : lcaoTypeRaw);
    Atom atom = atoms[atomIndex];
    String hybridization = "";
    z.set(0, 0, 0);
    x.set(0, 0, 0);
    Atom atom1 = atom;
    Atom atom2 = atom;
    Atom atom3;
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
            if (atom1.index == atomIndexNot) {
              x2.set(x);
              atom2 = atom1;
              x.set(n);
            } else {
              x2.set(n);
            }
            break;
          case 3:
            if (atom1.index == atomIndexNot) {
              x3.set(x);
              atom2 = atom1;
              x.set(n);
            } else {
              x3.set(n);
            }
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
      if (lcaoType.equals("sp3b") || lcaoType.equals("sp2a") || lcaoType.equals("lpa")) {
        z.set(-0.5f, -0.7f, 1);
        x.set(1, 0, 0);
      } else if (lcaoType.equals("sp3c") || lcaoType.equals("lpb")) {
        z.set(0.5f, -0.7f, -1f);
        x.set(1, 0, 0);
      } else if (lcaoType.equals("sp3d")) {
        z.set(0, 1, 0);
        x.set(1, 0, 0);
      } else {
        z.set(0, 0, 1);
        x.set(1, 0, 0);
      }
      break;
    case 1:
      if (lcaoType.indexOf("sp3") == 0) {
        // align z as sp3 orbital
        // with reference to atoms connected to connecting atom.
        // x3 is a pseudo-random vector
        // z is along the bond
        hybridization = "sp3";
        x.cross(x3, z);
        for (int i = 0; i < atom1.bonds.length; i++) {
          if (atom1.bonds[i].isCovalent() 
              && atom1.getBondedAtomIndex(i) != atom.index) {
            x.set(atom1);
            x.sub(atom1.bonds[i].getOtherAtom(atom1));
            x.cross(z, x);
            x.cross(x, z);
            break;
          }
        }
        x.normalize();
        // x is perp to bond
        y1.cross(z, x);
        y1.normalize();
        // y1 is perp to bond and x
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
      switch (atom1.getCovalentBondCount()) {
      case 1:
        if (atom1.getValence() == 3) // C-t-C
          break;
        // C=C, no other atoms
        // fall through
      case 2:
        hybridization = "sp2";
        if (lcaoType.indexOf("a") == 0)
          break; // just directing back to other atom
        // R-C=C or C=C=C
        // get third atom
        boolean isCumulated = false;
        while (true) {
          Bond[] bonds = atom1.bonds;
          atom3 = null;
          for (int i = 0; i < bonds.length; i++) {
            atom3 = bonds[i].getOtherAtom(atom1);
            if (atom3 != atom) {
              x3.set(atom3);
              x3.sub(atom1);
              if (!isCumulated)
                x3.cross(x3, x);
              break;
            }
          }
          if (atom3 == atom) // allene, no hydrogens
            x3.set(3.14159f, 2.71828f, 1.41421f);
          if (atom1.getValence() != 4 || atom1.getCovalentBondCount() != 2)
            break;
          isCumulated = !isCumulated;
          atom = atom1;
          atom1 = atom3;
        }
        // C=C or RC=C
        z.cross(x3, x); // perp
        z.normalize();
        if (lcaoType.indexOf("b") >= 0)
          z.scale(-1);
        z.set(z.x * sqrt3_2 + x.x / 2, z.y * sqrt3_2 + x.y / 2, z.z * sqrt3_2
            + x.z / 2);
        break;
      case 3:
        // special case, for example R2C=O oxygen
        getHybridizationAndAxes(atom1.index, z, x3, lcaoType, false, doAlignZ, atomIndex);
        x3.set(x);
        if (lcaoType.indexOf("sp2") == 0) { // align z as sp2 orbital
          hybridization = "sp2";
          z.scale(-1);
        }
        break;
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
            // special case, for example R2C=C=CR2 central carbon
            getHybridizationAndAxes(atom1.index, x, z, "pz", false, doAlignZ, atomIndex);
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
        hybridization = "lp"; // any is OK
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
      // 3 or 4 bonds
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
          z
              .set(lcaoType.equalsIgnoreCase("sp3")
                  || lcaoType.indexOf("d") >= 0 ? x4
                  : lcaoType.indexOf("c") >= 0 ? x3
                      : lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
          x.set(y1);
        } else { // needs testing here
          if (lcaoType.indexOf("lp") == 0 && nBonds == 3) { // align z as lone
            // pair
            hybridization = "lp"; // any is OK
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
        //System.out.println("draw x" + lcaoType + " vector {C13} " + z);
        x.set(y1);
        break;
      }
      // align z as p orbital
      z.set(y1);
      if (z.z < 0 && doAlignZ) {
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
  
  protected String getChimeInfo(int tok, BitSet bs) {
    StringBuffer info = new StringBuffer("\n");
    char id;
    String s = "";
    Chain clast = null;
    Group glast = null;
    int modelLast = -1;
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        id = atoms[i].getChainID();
        s = (id == '\0' ? " " : "" + id);
        switch (tok) {
        case Token.chain:
          break;
        case Token.selected:
          s = atoms[i].getInfo();
          break;
        case Token.atoms:
          s = "" + atoms[i].getAtomNumber();
          break;
        case Token.group:
          s = atoms[i].getGroup3(false);
          break;
        case Token.residue:
          s = "[" + atoms[i].getGroup3(false) + "]"
              + atoms[i].getSeqcodeString() + ":" + s;
          break;
        case Token.sequence:
          if (atoms[i].getModelIndex() != modelLast) {
            info.append('\n');
            n = 0;
            modelLast = atoms[i].getModelIndex();
            info.append("Model " + atoms[i].getModelNumber());
            glast = null;
            clast = null;
          }
          if (atoms[i].getChain() != clast) {
            info.append('\n');
            n = 0;
            clast = atoms[i].getChain();
            info.append("Chain " + s + ":\n");
            glast = null;
          }
          Group g = atoms[i].getGroup();
          if (g != glast) {
            if ((n++) % 5 == 0 && n > 1)
              info.append('\n');
            TextFormat.lFill(info, "          ", "["
                + atoms[i].getGroup3(false) + "]" + atoms[i].getResno() + " ");
            glast = g;
          }
          continue;
        default:
          return "";
        }
        if (info.indexOf("\n" + s + "\n") < 0)
          info.append(s).append('\n');
      }
    if (tok == Token.sequence)
      info.append('\n');
    return info.toString().substring(1);
  }

  /*
   * ******************************************************
   * 
   * These next methods are used by Eval to select for specific atom sets. They
   * all return a BitSet
   * 
   * ******************************************************
   */

  /**
   * general unqualified lookup of atom set type
   * 
   * @param tokType
   * @param specInfo
   * @return BitSet; or null if we mess up the type
   */
  protected BitSet getAtomBits(int tokType, Object specInfo) {
    BitSet bs = new BitSet();
    BitSet bsInfo;
    BitSet bsTemp;
    int iSpec;
    
    // this first set does not assume sequential order in the file

    int i = 0;
    switch (tokType) {
    case Token.atomno:
      iSpec = ((Integer) specInfo).intValue();
      for (i = atomCount; --i >= 0;)
        if (atoms[i].getAtomNumber() == iSpec)
          bs.set(i);
      break;
    case Token.atomname:
      String names = "," + specInfo + ",";
      for (i = atomCount; --i >= 0;) {
        String name = atoms[i].getAtomName();
        if (names.indexOf(name) >= 0)
          if (names.indexOf("," + name + ",") >= 0)
            bs.set(i);
      }
      break;
    case Token.atomtype:
      String types = "," + specInfo + ",";
      for (i = atomCount; --i >= 0;) {
        String type = atoms[i].getAtomType();
        if (types.indexOf(type) >= 0)
          if (types.indexOf("," + type + ",") >= 0)
            bs.set(i);
      }
      break;
    case Token.spec_resid:
      iSpec = ((Integer) specInfo).intValue();
      for (i = atomCount; --i >= 0;)
        if (atoms[i].getGroupID() == iSpec)
          bs.set(i);
      break;
    case Token.spec_chain:
      return BitSetUtil.copy(getChainBits((char) ((Integer) specInfo).intValue()));
    case Token.spec_seqcode:
      return BitSetUtil.copy(getSeqcodeBits(((Integer) specInfo).intValue(), true));
    case Token.hetero:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isHetero())
          bs.set(i);
      break;
    case Token.hydrogen:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].getElementNumber() == 1)
          bs.set(i);
      break;
    case Token.protein:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isProtein())
          bs.set(i);
      break;
    case Token.carbohydrate:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isCarbohydrate())
          bs.set(i);
      break;
    case Token.helix: // WITHIN -- not ends
    case Token.sheet: // WITHIN -- not ends
      byte type = (tokType == Token.helix ? JmolConstants.PROTEIN_STRUCTURE_HELIX
          : JmolConstants.PROTEIN_STRUCTURE_SHEET);
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isWithinStructure(type))
          bs.set(i);
      break;
    case Token.nucleic:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isNucleic())
          bs.set(i);
      break;
    case Token.dna:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isDna())
          bs.set(i);
      break;
    case Token.rna:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isRna())
          bs.set(i);
      break;
    case Token.purine:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isPurine())
          bs.set(i);
      break;
    case Token.pyrimidine:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isPyrimidine())
          bs.set(i);
      break;
    case Token.element:
      bsInfo = (BitSet) specInfo;
      bsTemp = new BitSet();
      for (i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1))
        bsTemp.set(getElementNumber(i));
      for (i = atomCount; --i >= 0;)
        if (bsTemp.get(getElementNumber(i)))
          bs.set(i);
      break;
    case Token.site:
      bsInfo = (BitSet) specInfo;
      bsTemp = new BitSet();
      for (i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1))
        bsTemp.set(atoms[i].atomSite);
      for (i = atomCount; --i >= 0;)
        if (bsTemp.get(atoms[i].atomSite))
          bs.set(i);
      break;
    case Token.identifier:
      return getIdentifierOrNull((String) specInfo);
    case Token.spec_atom:
      String atomSpec = ((String) specInfo).toUpperCase();
      if (atomSpec.indexOf("\\?") >= 0)
        atomSpec = TextFormat.simpleReplace(atomSpec, "\\?", "\1");
      // / here xx*yy is NOT changed to "xx??????????yy"
      for (i = atomCount; --i >= 0;)
        if (isAtomNameMatch(atoms[i], atomSpec, false))
          bs.set(i);
      break;
    case Token.spec_alternate:
      String spec = (String) specInfo;
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isAlternateLocationMatch(spec))
          bs.set(i);
      break;
    case Token.spec_name_pattern:
      return getSpecName((String) specInfo);
    }
    if (i < 0)
      return bs;

    // these next assume sequential position in the file
    // speeding delivery -- Jmol 11.9.24

    bsInfo = (BitSet) specInfo;
    int iModel, iPolymer;
    int i0 = bsInfo.nextSetBit(0);
    if (i0 < 0)
      return bs;
    i = 0;
    switch (tokType) {
    case Token.group:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1))
         i = atoms[i].getGroup().selectAtoms(bs);
      break;
    case Token.model:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        iModel = atoms[i].modelIndex;
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].modelIndex == iModel)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].modelIndex == iModel)
            bs.set(i);
          else
            break;
      }
      break;
    case Token.chain:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        Chain chain = atoms[i].getChain();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].getChain() == chain)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].getChain() == chain)
            bs.set(i);
          else
            break;
      }
      break;
    case Token.polymer:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        iPolymer = atoms[i].getPolymerIndexInModel();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].getPolymerIndexInModel() == iPolymer)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].getPolymerIndexInModel() == iPolymer)
            bs.set(i);
          else
            break;
      }
      break;
    case Token.structure:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        Object structure = atoms[i].getGroup().getStructure();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].getGroup().getStructure() == structure)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].getGroup().getStructure() == structure)
            bs.set(i);
          else
            break;
      }
      break;
    }
    if (i == 0)
      Logger.error("MISSING getAtomBits entry for " + Token.nameOf(tokType));
    return bs;
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
    // * can be used here, but not with ?
    //first check with * option OFF
    BitSet bs = getSpecNameOrNull(identifier, false);
    
    if (identifier.indexOf("\\?") >= 0)
      identifier = TextFormat.simpleReplace(identifier, "\\?","\1");
    if (bs != null || identifier.indexOf("?") > 0)
      return bs;
    // now check with * option ON
    if (identifier.indexOf("*") > 0) 
      return getSpecNameOrNull(identifier, true);
    
    int len = identifier.length();
    int pt = 0;
    while (pt < len && Character.isLetter(identifier.charAt(pt)))
      ++pt;
    bs = getSpecNameOrNull(identifier.substring(0, pt), false);
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
    BitSet bsInsert = getSeqcodeBits(seqcode, false);
    if (bsInsert == null) {
      if (insertionCode != ' ')
        bsInsert = getSeqcodeBits(Character.toUpperCase(identifier.charAt(pt)),
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
    bs.and(getChainBits(chainID));
    if (pt == len)
      return bs;
    //
    // not applicable
    //
    return null;
  }

  private BitSet getSpecName(String name) {
    // * can be used here with ?
    BitSet bs = getSpecNameOrNull(name, false);
    if (bs != null)
      return bs;
    if (name.indexOf("*") > 0)     
      bs = getSpecNameOrNull(name, true);
    return (bs == null ? new BitSet() : bs);
  }

  private BitSet getSpecNameOrNull(String name, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    BitSet bs = null;
    name = name.toUpperCase();
    if (name.indexOf("\\?") >= 0)
      name = TextFormat.simpleReplace(name, "\\?","\1");
    for (int i = atomCount; --i >= 0;) {
      String g3 = atoms[i].getGroup3(true);
      if (g3 != null && g3.length() > 0) {
        if (TextFormat.isMatch(g3, name, checkStar, true)) {
          if (bs == null)
            bs = new BitSet(i + 1);
          bs.set(i);
          while (--i >= 0 && atoms[i].getGroup3(true).equals(g3))
            bs.set(i);
          i++;
        }
      } else if (isAtomNameMatch(atoms[i], name, checkStar)) {
        if (bs == null)
          bs = new BitSet(i + 1);
        bs.set(i);
      }
    }
    return bs;
  }

  private boolean isAtomNameMatch(Atom atom, String strPattern, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    /// and NOT when coming from getAtomBits with Token.spec_atom
    /// because it is presumed that some names can include "*"
    return TextFormat.isMatch(atom.getAtomName().toUpperCase(), strPattern,
        checkStar, false);
  }
  
  protected BitSet getSeqcodeBits(int seqcode, boolean returnEmpty) {
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

  protected BitSet getChainBits(char chain) {
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

  public int[] getAtomIndices(BitSet bs) {
    int n = 0;
    int[] indices = new int[atomCount];
    for (int j = bs.nextSetBit(0); j >= 0 && j < atomCount; j = bs.nextSetBit(j + 1))
      indices[j] = ++n;
    return indices;
  }

  public BitSet getAtomsWithin(float distance, Point4f plane) {
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float d = Measure.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.index);
    }
    return bsResult;
  }
  
  public BitSet getVisibleSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isVisible(0))
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

  protected void deleteModelAtoms(int firstAtomIndex, int nAtoms, BitSet bs) {
    // all atoms in the model are being deleted here
    atoms = (Atom[]) ArrayUtil.deleteElements(atoms, firstAtomIndex, nAtoms);
    atomCount = atoms.length;
    for (int j = firstAtomIndex; j < atomCount; j++) {
      atoms[j].index = j;
      atoms[j].modelIndex--;
    }
    atomNames = (String[]) ArrayUtil.deleteElements(atomNames, firstAtomIndex,
        nAtoms);
    atomTypes = (String[]) ArrayUtil.deleteElements(atomTypes, firstAtomIndex,
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
    ellipsoids = (Object[][]) ArrayUtil.deleteElements(ellipsoids,
        firstAtomIndex, nAtoms);
    vibrationVectors = (Vector3f[]) ArrayUtil.deleteElements(vibrationVectors,
        firstAtomIndex, nAtoms);
    nSurfaceAtoms = 0;
    bsSurface = null;
    surfaceDistance100s = null;
    if (tainted != null)
      for (int i = 0; i < TAINT_MAX; i++)
        BitSetUtil.deleteBits(tainted[i], bs);
    // what about data?
  }
  
}

