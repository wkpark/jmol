/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:23:28 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5305 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

package org.jmol.shapesurface;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.AtomIndexIterator;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.modelset.Atom;
import org.jmol.script.Token;
import org.jmol.util.BitSetUtil;
import org.jmol.util.ContactPair;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.MeshSurface;
import org.jmol.viewer.JmolConstants;

public class Contact extends Isosurface {

  @Override
  public void initShape() {
    super.initShape();
    myType = "contact";
  }

  @Override
  public void setProperty(String propertyName, Object value, BitSet bs) {

    if ("set" == propertyName) {
      setContacts((Object[]) value);
      return;
    }

    super.setProperty(propertyName, value, bs);
  }

  protected Atom[] atoms;
  private int atomCount;
  private float minData, maxData;

  private void setContacts(Object[] value) {
    Logger.startTimer();
    BitSet bsA = (BitSet) value[0];
    BitSet bsB = (BitSet) value[1];
    BitSet[] bsFilters = (BitSet[]) value[2];
    int type = ((Integer) value[3]).intValue();
    RadiusData rd = (RadiusData) value[4];
    float[] parameters = (float[]) value[5];
    Object func = value[6];
    boolean isColorDensity = ((Boolean) value[7]).booleanValue();
    if (isColorDensity) {
      switch (type) {
      case Token.full:
      case Token.trim:
      case Token.plane:
        type = Token.trim;
        break;
      case Token.connect:
      case Token.nci:
      case Token.vanderwaals:
        // ok as is
        break;
      case Token.cap:
        isColorDensity = false;
        break;
      }
    }
    String command = (String) value[8];

    BitSet bs;
    atomCount = viewer.getAtomCount();
    atoms = viewer.getModelSet().atoms;

    int intramolecularMode = (int) (parameters == null || parameters.length < 2 ? 0
        : parameters[1]);
    float ptSize = (isColorDensity && parameters != null && parameters[0] < 0 ? Math
        .abs(parameters[0]) : 0.15f);
    if (Logger.debugging) {
      Logger.info("Contact intramolecularMode " + intramolecularMode);
      Logger.info("Contacts for " + bsA.cardinality() + ": "
          + Escape.escape(bsA));
      Logger.info("Contacts to " + bsB.cardinality() + ": "
          + Escape.escape(bsB));
    }
    super.setProperty("newObject", null, null);
    thisMesh.setMerged(true);
    Parameters params = sg.getParams();
    switch (type) {
    case Token.nci:
      bs = BitSetUtil.copy(bsA);
      bs.or(bsB); // for now -- TODO -- need to distinguish ligand
      if (parameters[0] < 0)
        parameters[0] = 0; // reset to default for density
      params.colorDensity = isColorDensity;
      params.bsSelected = bs;
      params.bsSolvent = bsB;
      sg.setParameter("parameters", parameters);
      super.setProperty("nci", Boolean.TRUE, null);
      break;
    case Token.cap:
      newSurface(Token.slab, null, bsA, bsB, rd, null, null, false, null);
      VolumeData volumeData = sg.getVolumeData();
      sg.initState();
      newSurface(Token.plane, null, bsA, bsB, rd, parameters, func, isColorDensity, volumeData);
      mergeMesh(null);
      break;
    case Token.vanderwaals:
      newSurface(type, null, bsA, bsB, rd, null, null, isColorDensity, null);
      break;
    case Token.full:
    case Token.trim:
    case Token.plane:
    case Token.connect:
      combineSurfaces(type, bsA, bsB, rd, parameters, func, isColorDensity,
          intramolecularMode, bsFilters);
      mergeMesh(null);
      break;
    }
    thisMesh.setMerged(false);
    thisMesh.jvxlData.vertexDataOnly = true;
    thisMesh.reinitializeLightingAndColor();
    super.setProperty("finalize", command, null);
    if (isColorDensity) {
      super.setProperty("pointSize", Float.valueOf(ptSize), null);
    } else {
      super.setProperty("token", Integer.valueOf(JmolConstants.FULLYLIT), null);
    }
    if (thisMesh.slabOptions != null) {
      thisMesh.slabOptions = null;
      thisMesh.polygonCount0 = -1; // disable slabbing.
    }
    Logger.checkTimer("contact");
  }

  private void combineSurfaces(int type, BitSet bsA, BitSet bsB, RadiusData rd,
                               float[] parameters, Object func,
                               boolean isColorDensity, int intramolecularMode,
                               BitSet[] bsFilters) {
    if (!bsA.equals(bsB))
      bsB.andNot(bsA);
    List<ContactPair> pairs = getPairs(bsA, bsB, rd, intramolecularMode,
        bsFilters);
    VolumeData volumeData = new VolumeData();
    int logLevel = Logger.getLogLevel();
    Logger.setLogLevel(0);
    float resolution = sg.getParams().resolution;
    int n = pairs.size();
    if (type == Token.full && resolution == Float.MAX_VALUE)
      resolution = (n > 1000 ? 3 : 10);
    for (int i = n; --i >= 0;) {
      ContactPair cp = pairs.get(i);
      setVolumeData(type, volumeData, cp, resolution, n);
      switch (type) {
      case Token.full:
        newSurface(type, cp, null, null, rd, null, func, isColorDensity, volumeData);
        cp.switchAtoms();
        newSurface(type, cp, null, null, rd, null, null, isColorDensity, volumeData);
        break;
      case Token.trim:
      case Token.plane:
      case Token.connect:
        newSurface(type, cp, null, null, rd, parameters, func, isColorDensity, volumeData);
        break;
      }
      if (i > 0 && (i % 100) == 0 && logLevel == 4) {
        Logger.setLogLevel(4);
        Logger.info("contact..." + i);
        Logger.setLogLevel(0);
      }
    }
    Logger.setLogLevel(logLevel);
  }

  private void newSurface(int type, ContactPair cp, BitSet bs1, BitSet bs2,
                          RadiusData rd, float[] parameters, Object func,
                          boolean isColorDensity, VolumeData volumeData) {
    Parameters params = sg.getParams();
    if (cp == null) {
      bs2.andNot(bs1);
      if (bs1.isEmpty() || bs2.isEmpty())
        return;
    } else {
      params.contactPair = cp;
    }
    int iSlab0 = 0, iSlab1 = 0;
    sg.initState();
    switch (type) {
    case Token.vanderwaals:
    case Token.slab:
    case Token.trim:
    case Token.full:
      params.colorDensity = isColorDensity;
      if (isColorDensity) {
        super.setProperty("cutoffRange", new float[] { -100f, 0f }, null);
      }
      if (cp == null) {
        params.atomRadiusData = rd;
        params.bsIgnore = BitSetUtil.copyInvert(bs1, atomCount);
        params.bsSelected = bs1;
        params.bsSolvent = null;
      }
      params.volumeData = volumeData;
      super.setProperty("sasurface", Float.valueOf(0), null);
      super.setProperty("map", Boolean.TRUE, null);
      if (cp == null) {
        params.atomRadiusData = rd;
        params.bsIgnore = BitSetUtil.copyInvert(bs2, atomCount);
        params.bsSelected = bs2;
      }
      params.volumeData = volumeData;
      super.setProperty("sasurface", Float.valueOf(0), null);
      switch (type) {
      case Token.full:
      case Token.trim:
        iSlab0 = -100;
        break;
      case Token.vanderwaals:
        if (isColorDensity)
          iSlab0 = -100;
        break;
      case Token.slab:
        iSlab1 = -100;
      }
      break;
    case Token.plane:
    case Token.connect:
      if (type == Token.connect)
        sg.setParameter("parameters", parameters);
      if (cp == null) {
        params.atomRadiusData = rd;
        params.bsIgnore = BitSetUtil.copyInvert(bs2, atomCount);
        params.bsIgnore.andNot(bs1);
      }
      params.func = func;
      params.intersection = new BitSet[] { bs1, bs2 };
      params.volumeData = volumeData;
      params.colorDensity = isColorDensity;
      if (isColorDensity)
        super.setProperty("cutoffRange", new float[] { -5f, 0f }, null);
      super.setProperty("sasurface", Float.valueOf(0), null);
      // mapping
      super.setProperty("map", Boolean.TRUE, null);
      params.volumeData = volumeData;
      super.setProperty("sasurface", Float.valueOf(0), null);
      if (type != Token.connect)
        iSlab0 = -100;
    }
    if (iSlab0 != iSlab1)
      thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(iSlab0, iSlab1),
          false);
    if (type != Token.vanderwaals)
      thisMesh.setMerged(true);
  }

  private Vector3f vZ = new Vector3f();
  private Vector3f vY = new Vector3f();
  private Vector3f vX = new Vector3f();
  private Point3f pt1 = new Point3f();
  private Point3f pt2 = new Point3f();
  
  private void setVolumeData(int type, VolumeData volumeData, ContactPair cp,
                             float resolution, int nPairs) {
    pt1.set(atoms[cp.iAtom1]);
    pt2.set(atoms[cp.iAtom2]);
    vX.sub(pt2, pt1);
    float dAB = vX.length();
    float dYZ = (cp.radius1 * cp.radius1 + dAB * dAB - cp.radius2 * cp.radius2)/(2 * dAB * cp.radius1);
    dYZ = 2.1f * (float) (cp.radius1 * Math.sin(Math.acos(dYZ)));
    Measure.getNormalToLine(pt1, pt2, vZ);
    vZ.scale(dYZ);
    vY.cross(vZ, vX);
    vY.normalize();
    vY.scale(dYZ);
    if (type != Token.connect) {
      vX.normalize();
      pt1.scaleAdd((dAB - cp.radius2) * 0.95f, vX, pt1);
      pt2.scaleAdd((cp.radius1 - dAB) * 0.95f, vX, pt2);
      vX.sub(pt2, pt1);
    }
    if (resolution == Float.MAX_VALUE)
      resolution = (nPairs > 100 ? 3 : 10);
    
    // now set voxel counts and vectors, and grid origin

    int nX = Math.max(5, (int) (pt1.distance(pt2) * resolution + 1));
    if ((nX % 2) == 0)
      nX++;
    int nYZ = Math.max(7, (int) (dYZ * resolution + 1));
    if ((nYZ % 2) == 0)
      nYZ++;
    volumeData.setVoxelCounts(nX, nYZ, nYZ);
    pt1.scaleAdd(-0.5f, vY, pt1);
    pt1.scaleAdd(-0.5f, vZ, pt1);
    volumeData.setVolumetricOrigin(pt1.x, pt1.y, pt1.z);
    /*
    System.out.println("draw pt1 @{point"+pt1+"} color red");
    System.out.println("draw vx vector @{point"+pt1+"} @{point"+vX+"} color red");
    System.out.println("draw vy vector @{point"+pt1+"} @{point"+vY+"} color green");
    System.out.println("draw vz vector @{point"+pt1+"} @{point"+vZ+"} color blue");
    */

    vX.scale(1f/(nX-1));
    vY.scale(1f/(nYZ-1));
    vZ.scale(1f/(nYZ-1));
    volumeData.setVolumetricVector(0, vX.x, vX.y, vX.z);
    volumeData.setVolumetricVector(1, vY.x, vY.y, vY.z);
    volumeData.setVolumetricVector(2, vZ.x, vZ.y, vZ.z);

  }

  private List<ContactPair> getPairs(BitSet bsA, BitSet bsB, RadiusData rd,
                                     int intramolecularMode, BitSet[] bsFilters) {
    List<ContactPair> list = new ArrayList<ContactPair>();
    AtomData ad = new AtomData();
    ad.radiusData = rd;
    BitSet bs = BitSetUtil.copy(bsA);
    bs.or(bsB);
    ad.bsSelected = bs;
    boolean isSelf = bsA.equals(bsB);
    viewer.fillAtomData(ad, AtomData.MODE_FILL_RADII 
        | AtomData.MODE_FILL_MULTIMODEL
        | AtomData.MODE_FILL_MOLECULES);
    boolean isMisc = (bsFilters != null && bsFilters[1] != null);
    boolean isMultiModel = (ad.firstModelIndex != ad.lastModelIndex || ad.firstAtomIndex != atoms[bsA.nextSetBit(0)].modelIndex);
    
    float maxRadius = 0;
    for (int ib = bsB.nextSetBit(0); ib >= 0; ib = bsB.nextSetBit(ib + 1))
      if (ad.atomRadius[ib] > maxRadius)
        maxRadius = ad.atomRadius[ib];
    AtomIndexIterator iter = viewer.getSelectedAtomIterator(bsB, isSelf, false, isMultiModel);
    for (int ia = bsA.nextSetBit(0); ia >= 0; ia = bsA.nextSetBit(ia + 1)) {
      if (isMultiModel)
        viewer.setIteratorForPoint(iter, -1, ad.atomXyz[ia], ad.atomRadius[ia] + maxRadius);
      else
        viewer.setIteratorForAtom(iter, ia, ad.atomRadius[ia] + maxRadius);
      while (iter.hasNext()) {
        int ib = iter.next();
        boolean isSameMolecule = (ad.atomMolecule[ia] == ad.atomMolecule[ib]);
        if (ia == ib || isSameMolecule && atoms[ia].isWithinFourBonds(atoms[ib]))
          continue;
        switch (intramolecularMode) {
        case 0:
          break;
        case 1:
        case 2:
          if (isSameMolecule != (intramolecularMode == 1))
            continue;
        }
        if (isMisc) {
          if (bsFilters[0].get(ia) && bsFilters[1].get(ib)
              || bsFilters[0].get(ib) && bsFilters[1].get(ia)) {
            // good -- crossed groups 
          } else {
            continue;
          }
        } else if (bsFilters != null
            && (!bsFilters[0].get(ia) || !bsFilters[0].get(ib))) {
          continue;
        }
        if (atoms[ia].distance(atoms[ib]) > ad.atomRadius[ia]
            + ad.atomRadius[ib])
          continue;
        ContactPair cp = new ContactPair(atoms, ia, ib, ad.atomRadius[ia],
            ad.atomRadius[ib]);
        if (isContactClear(cp, ad, bs))
          list.add(cp);
      }
    }
    iter.release();
    iter = null;
    Logger.info("Contact pairs: " + list.size());
    return list;
  }

  private boolean isContactClear(ContactPair cp, AtomData ad, BitSet bs) {
    float d;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (ad.bsIgnored != null && ad.bsIgnored.get(i) || i == cp.iAtom1
          || i == cp.iAtom2 || (d = ad.atomRadius[i]) == 0)
        continue;
      if (atoms[i].distance(cp.pt) < d)
        return false;
    }
    return true;
  }

  private void mergeMesh(MeshData md) {
    thisMesh.merge(md);
    if (minData == Float.MAX_VALUE) {
      // just assign it
    } else if (jvxlData.mappedDataMin == Float.MAX_VALUE) {
      jvxlData.mappedDataMin = minData;
      jvxlData.mappedDataMax = maxData;
    } else {
      jvxlData.mappedDataMin = Math.min(minData, jvxlData.mappedDataMin);
      jvxlData.mappedDataMax = Math.max(maxData, jvxlData.mappedDataMax);
    }
    minData = jvxlData.mappedDataMin;
    maxData = jvxlData.mappedDataMax;
    jvxlData.valueMappedToBlue = minData;
    jvxlData.valueMappedToRed = maxData;

  }

}
