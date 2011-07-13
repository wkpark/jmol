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

import java.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.jvxl.data.MeshData;
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

  private int atomCount;
  private float minData, maxData;
  
  private void setContacts(Object[] value) {
    BitSet bsA = (BitSet) value[0];
    BitSet bsB = (BitSet) value[1];
    if (true || Logger.debugging) {
      Logger.info("Contacts for " + bsA.cardinality() + ": " + Escape.escape(bsA));
      Logger.info("Contacts to " + bsB.cardinality() + ": " + Escape.escape(bsB));
    }
    BitSet[] bsFilters = (BitSet[]) value[2];
    boolean doSlabByType = (bsFilters != null);
    boolean isMisc = (doSlabByType && bsFilters[1] != null);
    BitSet bs;
    int type = ((Integer) value[3]).intValue();
    RadiusData rd = (RadiusData) value[4];
    float[] params = (float[]) value[5];
    Object func = value[6];
    boolean isColorDensity = ((Boolean) value[7]).booleanValue();
    String command = (String) value[8];
    float ptSize = (isColorDensity && params != null && params[0] < 0 ? Math
        .abs(params[0]) : 0.15f);
    atomCount = viewer.getAtomCount();
    int intramolecularMode = (int) (params == null || params.length < 2 ? 0
        : params[1]);
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
    case Token.vanderwaals:
    case Token.trim:
      newSurface(type, bsA, bsB, rd, null, null, false);
      break;
    default:
      doInterIntra(type, bsA, bsB, rd, params, func, isColorDensity,
          intramolecularMode, true);
      break;
    }    
    if (doSlabByType) {
      if (isMisc) {
        BitSet bsHbond = slabMisc(bsFilters[0], bsA, bsB, rd);
        BitSet bsHydro = slabMisc(bsFilters[1], bsA, bsB, rd);
        // from "not hbond and not hydro" to "anything"
        bs = BitSetUtil.copy(bsA);
        bs.andNot(bsFilters[0]);
        bs.andNot(bsFilters[1]);
        slabByType(bs, bsA, rd);
        thisMesh.bsSlabDisplay.or(bsHbond);
        thisMesh.bsSlabDisplay.or(bsHydro);
      } else {
        bs = BitSetUtil.copy(bsA);
        bs.and(bsFilters[0]);
        slabByType(bs, bsA, rd);
        bs = BitSetUtil.copy(bsB);
        bs.and(bsFilters[0]);
        slabByType(bs, bsB, rd);
      }
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

  private BitSet slabMisc(BitSet bs, BitSet bsA, BitSet bsB, RadiusData rd) {
    BitSet bsTemp = BitSetUtil.copy(thisMesh.bsSlabDisplay);
    BitSet bs1 = BitSetUtil.copy(bsA);
    bs1.and(bs);
    slabByType(bs1, bsA, rd);
    bs1 = BitSetUtil.copy(bsB);
    bs1.andNot(bs);
    slabByType(bs1, bsB, rd);
    bs1 = BitSetUtil.copy(thisMesh.bsSlabDisplay);
    if (bsTemp == null)
      thisMesh.resetSlab();
    else
      BitSetUtil.copy(bsTemp, thisMesh.bsSlabDisplay);
    return bs1;
  }

  private void slabByType(BitSet bs1, BitSet bsAll, RadiusData rd) {
    float[] fData = new float[thisMesh.vertexCount];
    BitSet bs2 = BitSetUtil.copy(bsAll);
    bs2.andNot(bs1);
    setSlabData(fData, bs1, bs2, rd);
    thisMesh.slabPolygons(new Object[] { Integer.valueOf(Token.data), 
        fData, Boolean.FALSE });
  }

  private void setSlabData(float[] fData, BitSet bs1, BitSet bs2,
                           RadiusData radiusData) {
    AtomData ad1 = getAtomData(radiusData, bs1);
    AtomData ad2 = getAtomData(radiusData, bs2);
    for (int i = 0; i < thisMesh.vertexCount; i++) {
      if (thisMesh.vertices[i] == null)
        continue;
      float d1 = getDistance(thisMesh.vertices[i], ad1.atomXyz, ad1.atomRadius);
      float d2 = getDistance(thisMesh.vertices[i], ad2.atomXyz, ad2.atomRadius);
      fData[i] = d2 - d1;
    }
  }

  private AtomData getAtomData(RadiusData radiusData, BitSet bs) {
    AtomData ad = new AtomData();
    ad.radiusData = radiusData;
    ad.bsSelected = bs;
    viewer.fillAtomData(ad, AtomData.MODE_FILL_CONTACT);
    return ad;
  }

  private static float getDistance(Point3f pt, Point3f[] points, float[] radii) {
    float value = Float.MAX_VALUE;
    for (int i = 0; i < points.length; i++) {
      float r = pt.distance(points[i]) - radii[i];
      if (r < value)
        value = r;
    }
    return (value == Float.MAX_VALUE ? Float.NaN : value);
  }

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

  private void newSurface(int type, BitSet bsA, BitSet bsB, RadiusData rd,
                          Object params, Object func, boolean isColorDensity) {
    if (bsA.nextSetBit(0) < 0 || bsB.nextSetBit(0) < 0)
      return;
    System.out.println("--------newSurface----" + Token.nameOf(type));
    switch (type) {
    case Token.vanderwaals:
    case Token.trim:
    case Token.full:
    case Token.slab:
      setProperty("select", bsA, null);
      BitSet bs = BitSetUtil.copyInvert(bsA, atomCount);
      setProperty("ignore", bs, null);
      setProperty("radius", rd, null);
      setProperty("bsSolvent", null, null);
      setProperty("sasurface", Float.valueOf(0), null);
      setProperty("map", Boolean.TRUE, null);
      setProperty("select", bsB, null);
      bs = BitSetUtil.copyInvert(bsB, atomCount);
      setProperty("ignore", bs, null);
      setProperty("radius", rd, null);
      setProperty("sasurface", Float.valueOf(0), null);
      switch (type) {
      case Token.full:
      case Token.trim:
        thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(-100, 0));
        break;
      case Token.slab:
        thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(0, -100));
      }
      return;
    case Token.plane:
    case Token.connect:
    case Token.cap:
      if (type == Token.connect)
        setProperty("parameters", params, null);
      setProperty("func", func, null);
      setProperty("intersection", new BitSet[] { bsA, bsB }, null);
      if (isColorDensity)
        setProperty("cutoffRange", new float[] { -0.3f, 0.3f }, null);
      setProperty("radius", rd, null);
      setProperty("bsSolvent", null, null);
      setProperty("sasurface", Float.valueOf(0), null);
      // mapping
      setProperty("map", Boolean.TRUE, null);
      setProperty("select", bsA, null);
      setProperty("radius", rd, null);
      setProperty("sasurface", Float.valueOf(0), null);
      thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(-100, 0));
    }
  }

}
