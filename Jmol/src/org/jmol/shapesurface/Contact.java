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
    Logger.info("Contacts for " + Escape.escape(bsA));
    Logger.info("Contacts to " + Escape.escape(bsB));
    //BitSet bsIgnore = (BitSet) value[2];
    BitSet bs;
    int type = ((Integer) value[3]).intValue();
    RadiusData rd = (RadiusData) value[4];
    float[] params = (float[]) value[5];
    Object func = value[6];
    Object slabObject = value[7];
    boolean isColorDensity = ((Boolean) value[8]).booleanValue();
    String command = (String) value[9];
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
      newSurface(Token.vanderwaals, bsA, bsB, rd, null, null, false);
      break;
    default:
      doInterIntra(type, bsA, bsB, rd, params, func, isColorDensity,
          intramolecularMode, true);
      if (type == Token.full) {
        thisMesh.jvxlData.vertexDataOnly = true;
        thisMesh.reinitializeLightingAndColor();
      } else if (type == Token.plane) {
        setProperty("clear", null, null);
        setProperty("init", null, null);
        setProperty("slab", slabObject, null);
      }
      break;
    }
    setProperty("finalize", command, null);
    if (isColorDensity) {
      setProperty("pointSize", Float.valueOf(ptSize), null);
    }
    if (thisMesh != null && thisMesh.slabOptions != null) {
      thisMesh.slabOptions = null;
      thisMesh.polygonCount0 = -1; // disable slabbing.
    }
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
            //System.out.println("contact " + bsA1 + " " + bsB1);
            doInterIntra(type, bsA1, bsB1, rd, params, func, isColorDensity, 0,
                isFirst);
            isFirst = false;
          }
        }
      }
      return;
    }

    MeshData prev = null;
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
    switch (type) {
    case Token.full:
      newSurface(Token.full, bsA, bsB, rd, null, null, false);
      MeshData data1 = new MeshData();
      fillMeshData(data1, MeshData.MODE_GET_VERTICES, thisMesh);
      setProperty("init", null, null);
      if (isColorDensity)
        sg.setParameter("colorDensity", null);
      newSurface(Token.full, bsB, bsA, rd, null, null, false);
      // not ready for this yet: thisMesh.slabPolygons(MeshSurface.getSlabObject(Token.mesh, 
      //    new Object[] { Float.valueOf(100), data1}, false));
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
    jvxlData.mappedDataMin = Math.min(minData, jvxlData.mappedDataMin);
    jvxlData.mappedDataMax = Math.max(maxData, jvxlData.mappedDataMax);
  }

  private void newSurface(int type, BitSet bsA, BitSet bsB, RadiusData rd,
                          Object params, Object func, boolean isColorDensity) {
    switch (type) {
    case Token.vanderwaals:
    case Token.full:
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
      if (type == Token.full)
        thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(-100, 0));
      return;
    case Token.plane:
    case Token.connect:
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
    }
  }

}
