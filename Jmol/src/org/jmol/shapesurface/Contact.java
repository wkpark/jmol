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

import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.modelset.Atom;
import org.jmol.script.Token;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;

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
    BitSet bsA = (BitSet) value[0];
    BitSet bsB = (BitSet) value[1];
    BitSet[] bsFilters = (BitSet[]) value[2];
    int type = ((Integer) value[3]).intValue();
    RadiusData rd = (RadiusData) value[4];
    float[] params = (float[]) value[5];
    Object func = value[6];
    boolean isColorDensity = ((Boolean) value[7]).booleanValue();
    String command = (String) value[8];

    BitSet bs;
    atomCount = viewer.getAtomCount();
    atoms = viewer.getModelSet().atoms;

    int intramolecularMode = (int) (params == null || params.length < 2 ? 0
        : params[1]);
    float ptSize = (isColorDensity && params != null && params[0] < 0 ? Math
        .abs(params[0]) : 0.15f);
    if (Logger.debugging) {
      Logger.info("Contact intramolecularMode " + intramolecularMode);
      Logger.info("Contacts for " + bsA.cardinality() + ": "
          + Escape.escape(bsA));
      Logger.info("Contacts to " + bsB.cardinality() + ": "
          + Escape.escape(bsB));
    }
    setProperty("newObject", null, null);

    switch (type) {
    case Token.nci:
      if (isColorDensity)
        sg.setParameter("colorDensity", null);
      bs = BitSetUtil.copy(bsA);
      bs.or(bsB); // for now -- TODO -- need to distinguish ligand
      if (params[0] < 0)
        params[0] = 0; // reset to default for density
      setProperty("select", bs, null);
      setProperty("bsSolvent", bsB, null);
      setProperty("parameters", params, null);
      setProperty("nci", Boolean.TRUE, null);
      break;
    case Token.cap:
      float resolution = sg.getParams().resolution;
      newSurface(Token.slab, bsA, bsB, rd, null, null, false);
      MeshData data1 = new MeshData();
      fillMeshData(data1, MeshData.MODE_GET_VERTICES, thisMesh);
      setProperty("init", null, null);
      if (isColorDensity)
        sg.setParameter("colorDensity", null);
      sg.getParams().resolution = resolution;
      newSurface(Token.plane, bsA, bsB, rd, params, func, isColorDensity);
      merge(data1);
      break;
    case Token.vanderwaals:
      newSurface(type, bsA, bsB, rd, null, null, false);
      break;
    default:
      combineSurfaces(type, bsA, bsB, rd, params, func, isColorDensity,
          intramolecularMode, bsFilters);
      break;
    }
    thisMesh.jvxlData.vertexDataOnly = true;
    thisMesh.reinitializeLightingAndColor();
    setProperty("finalize", command, null);
    if (isColorDensity) {
      setProperty("pointSize", Float.valueOf(ptSize), null);
    }
    if (thisMesh.slabOptions != null) {
      thisMesh.slabOptions = null;
      thisMesh.polygonCount0 = -1; // disable slabbing.
    }
  }

  private void combineSurfaces(int type, BitSet bsA, BitSet bsB, RadiusData rd,
                               float[] params, Object func,
                               boolean isColorDensity, int intramolecularMode,
                               BitSet[] bsFilters) {
    if (bsB.cardinality() == 0)
      bsB = bsA;
    List<ContactPair> pairs = getPairs(bsA, bsB, rd, intramolecularMode,
        bsFilters);
    BitSet bs1 = new BitSet();
    BitSet bs2 = new BitSet();
    int logLevel = Logger.getLogLevel();
    Logger.setLogLevel(0);
    float resolution = sg.getParams().resolution;
    for (int i = 0; i < pairs.size(); i++) {
      ContactPair cp = pairs.get(i);
      bs1.set(cp.iAtom1);
      bs2.set(cp.iAtom2);
      MeshData prev = null;
      if (i == 0) {
        newSg();
        minData = thisMesh.jvxlData.dataMin;
        maxData = thisMesh.jvxlData.dataMax;
      } else {
        prev = new MeshData();
        fillMeshData(prev, MeshData.MODE_GET_VERTICES, thisMesh);
        setProperty("clear", null, null);
        setProperty("init", null, null);
      }
      if (isColorDensity)
        sg.setParameter("colorDensity", null);
      sg.getParams().resolution = resolution;
      switch (type) {
      case Token.full:
        newSurface((type == Token.cap ? Token.plane : type), bs1, bs2, rd,
            null, null, false);
        MeshData data1 = new MeshData();
        fillMeshData(data1, MeshData.MODE_GET_VERTICES, thisMesh);
        setProperty("init", null, null);
        if (isColorDensity)
          sg.setParameter("colorDensity", null);
        sg.getParams().resolution = resolution;
        newSurface(type, bs2, bs1, rd, null, null, false);
        merge(data1);
        break;
      case Token.trim:
      case Token.plane:
      case Token.connect:
        newSurface(type, bs1, bs2, rd, params, func, isColorDensity);
        break;
      }
      if (prev != null)
        merge(prev);
      bs1.clear(cp.iAtom1);
      bs2.clear(cp.iAtom2);
    }
    Logger.setLogLevel(logLevel);
  }

  private List<ContactPair> getPairs(BitSet bsA, BitSet bsB, RadiusData rd,
                                     int intramolecularMode, BitSet[] bsFilters) {
    List<ContactPair> list = new ArrayList<ContactPair>();
    AtomData ad = new AtomData();
    ad.radiusData = rd;
    BitSet bs = BitSetUtil.copy(bsA);
    bs.or(bsB);
    ad.bsSelected = bs;
    viewer.fillAtomData(ad, AtomData.MODE_FILL_RADII
        | (intramolecularMode > 0 ? AtomData.MODE_FILL_MOLECULES : 0));
    boolean isMisc = (bsFilters != null && bsFilters[1] != null);
    for (int ia = bsA.nextSetBit(0); ia >= 0; ia = bsA.nextSetBit(ia + 1))
      for (int ib = bsB.nextSetBit(0); ib >= 0; ib = bsB.nextSetBit(ib + 1)) {
        if (ia == ib)
          continue;
        switch (intramolecularMode) {
        case 0:
          break;
        case 1:
        case 2:
          if ((ad.atomMolecule[ia] == ad.atomMolecule[ib]) != (intramolecularMode == 1))
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
        ContactPair cp = new ContactPair(ia, ib, ad.atomRadius[ia],
            ad.atomRadius[ib]);
        if (isContactClear(cp, ad, bs))
          list.add(cp);
      }
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

  private class ContactPair {
    int iAtom1, iAtom2;
    Point3f pt;

    ContactPair(int i1, int i2, float vdw1, float vdw2) {
      iAtom1 = i1;
      iAtom2 = i2;

      //     ------d------------
      //    i1--------|->vdw1
      //        vdw2<-|--------i2
      //              pt

      Vector3f v = new Vector3f(atoms[i2]);
      v.sub(atoms[i1]);
      float dAB = v.length();
      float f = (vdw1 - vdw2 + dAB) / (2 * dAB);
      
      //NOT float f = (vdw1*vdw1 - vdw2*vdw2 + dAB*dAB) / (2 * dAB*dAB);
      // as that would be for truly planar section, but it is not quite planar
      
      pt = new Point3f();
      pt.scaleAdd(f, v, atoms[i1]);
      //System.out.println("draw a"+i1 + "_" + i2 + " arrow @{point"+new Point3f(atoms[i1]) +"} @{point"+pt+"}");
      //System.out.println(atoms[i1] + "-" + atoms[i2] + "\t" + atoms[i1].distance(pt) + " " + atoms[i2].distance(pt));
    }
  }

  /*
  private static float getDistance(Point3f pt, Point3f[] points, float[] radii) {
    float value = Float.MAX_VALUE;
    for (int i = 0; i < points.length; i++) {
      float r = pt.distance(points[i]) - radii[i];
      if (r < value)
        value = r;
    }
    return (value == Float.MAX_VALUE ? Float.NaN : value);
  }
  */
  /*
  private void doInterIntra(int type, BitSet bsA, BitSet bsB, RadiusData rd,
                            float[] params, Object func,
                            boolean isColorDensity, int intramolecularMode,
                            boolean isFirst) {

    if (intramolecularMode != 0) {
      // get molecules
      // run through all molecules a and b
      AtomData ad = new AtomData();
      viewer.fillAtomData(ad, AtomData.MODE_FILL_MOLECULES);
      BitSet bsA1 = new BitSet();
      BitSet bsB1 = new BitSet();
      if (bsB.cardinality() == 0)
        bsB = bsA;
      if (isFirst) {
        minData = thisMesh.jvxlData.dataMin;
        maxData = thisMesh.jvxlData.dataMax;
      }
      for (int i = 0; i < ad.bsMolecules.length; i++) {
        bsA1.clear();
        bsA1.or(ad.bsMolecules[i]);
        bsA1.and(bsA);
        if (bsA1.nextSetBit(0) < 0)
          continue;
        if (intramolecularMode == 1) {
          doInterIntra(type, bsA1, bsA1, rd, params, func, isColorDensity, 0,
              isFirst);
          isFirst = false;
        } else {
          for (int j = 0; j < ad.bsMolecules.length; j++) {
            if (j == i)
              continue;
            bsB1.clear();
            bsB1.or(ad.bsMolecules[j]);
            bsB1.and(bsB);
            if (bsB1.nextSetBit(0) < 0)
              continue;
            doInterIntra(type, bsA1, bsB1, rd, params, func, isColorDensity, 0,
                isFirst);
            isFirst = false;
          }
        }
      }
      return;
    }

    MeshData prev = null;
    float resolution = sg.getParams().resolution;
    if (isFirst) {
      newSg();
    } else {
      prev = new MeshData();
      fillMeshData(prev, MeshData.MODE_GET_VERTICES, thisMesh);
      setProperty("clear", null, null);
      setProperty("init", null, null);
    }
    if (isColorDensity)
      sg.setParameter("colorDensity", null);
    sg.getParams().resolution = resolution;
    switch (type) {
    case Token.full:
    case Token.cap:
      newSurface((type == Token.full ? type : Token.plane), bsA, bsB, rd, null, null, false);
      MeshData data1 = new MeshData();
      fillMeshData(data1, MeshData.MODE_GET_VERTICES, thisMesh);
      resolution = sg.getParams().resolution;
      setProperty("init", null, null);
      if (isColorDensity)
        sg.setParameter("colorDensity", null);
      sg.getParams().resolution = resolution;
      if (type == Token.full)
        newSurface(type, bsB, bsA, rd, null, null, false);
      else
        newSurface(Token.slab, bsA, bsB, rd, null, null, false);     
      merge(data1);
      break;
    case Token.plane:
    case Token.connect:
      newSurface(type, bsA, bsB, rd, params, func, isColorDensity);
      break;
    }
    if (prev != null)
      merge(prev);
  }
  */
  private void merge(MeshData md) {
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

  private void newSurface(int type, BitSet bs1, BitSet bs2, RadiusData rd,
                          Object params, Object func, boolean isColorDensity) {
    if (bs1.isEmpty() || bs2.isEmpty())
      return;
    System.out.println("--------newSurface----" + Token.nameOf(type) + bs1 + " " + bs2);
    switch (type) {
    case Token.vanderwaals:
    case Token.trim:
    case Token.full:
    case Token.slab:
      setProperty("select", bs1, null);
      BitSet bs = BitSetUtil.copyInvert(bs1, atomCount);
      setProperty("ignore", bs, null);
      setProperty("radius", rd, null);
      setProperty("bsSolvent", null, null);
      setProperty("sasurface", Float.valueOf(0), null);
      setProperty("map", Boolean.TRUE, null);
      setProperty("select", bs2, null);
      bs = BitSetUtil.copyInvert(bs2, atomCount);
      setProperty("ignore", bs, null);
      setProperty("radius", rd, null);
      setProperty("sasurface", Float.valueOf(0), null);
      switch (type) {
      case Token.full:
      case Token.trim:
        thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(-100, 0), false);
        break;
      case Token.slab:
        thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(0, -100), false);
      }
      return;
    case Token.plane:
    case Token.connect:
      if (type == Token.connect)
        setProperty("parameters", params, null);
      setProperty("func", func, null);
      setProperty("intersection", new BitSet[] { bs1, bs2 }, null);
      if (isColorDensity)
        setProperty("cutoffRange", new float[] { -0.3f, 0.3f }, null);
      setProperty("radius", rd, null);
      setProperty("bsSolvent", null, null);
      setProperty("sasurface", Float.valueOf(0), null);
      // mapping
      setProperty("map", Boolean.TRUE, null);
      setProperty("select", bs1, null);
      setProperty("radius", rd, null);
      setProperty("sasurface", Float.valueOf(0), null);
      //System.out.println("Contact newSurface " + bs1 + " " + bs2);

      if (type != Token.connect)
        thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(-100, 0), false);
    }
  }

}
