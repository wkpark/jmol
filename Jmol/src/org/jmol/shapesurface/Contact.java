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
import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.modelset.Atom;
import org.jmol.script.Token;
import org.jmol.util.BitSetUtil;
import org.jmol.util.ColorEncoder;
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
  //private final static String hbondH = "_H & connected(_O|_N and his and not *.N |_S)";
  //private final static float HBOND_CUTOFF = -0.8f;
  private final static RadiusData rdVDW =  new RadiusData(1, RadiusData.TYPE_FACTOR, JmolConstants.VDW_AUTO);
  
  private void setContacts(Object[] value) {
    Logger.startTimer();
    int contactType = ((Integer) value[0]).intValue();
    int displayType = ((Integer) value[1]).intValue();
    boolean colorDensity = ((Boolean) value[2]).booleanValue();
    boolean colorByType = ((Boolean) value[3]).booleanValue();
    BitSet bsA = (BitSet) value[4];
    BitSet bsB = (BitSet) value[5];
    RadiusData rd = (RadiusData) value[6];
    float[] parameters = (float[]) value[7];
    String command = (String) value[8];

    if (colorDensity) {
      switch (displayType) {
      case Token.full:
      case Token.trim:
      case Token.plane:
        displayType = Token.trim;
        break;
      case Token.connect:
      case Token.nci:
      case Token.surface:
        // ok as is
        break;
      case Token.cap:
        colorDensity = false;
        break;
      }
    }

    BitSet bs;
    atomCount = viewer.getAtomCount();
    atoms = viewer.getModelSet().atoms;

    int intramolecularMode = (int) (parameters == null || parameters.length < 2 ? 0
        : parameters[1]);
    float ptSize = (colorDensity && parameters != null && parameters[0] < 0 ? Math
        .abs(parameters[0])
        : 0.15f);
    if (Logger.debugging) {
      Logger.info("Contact intramolecularMode " + intramolecularMode);
      Logger.info("Contacts for " + bsA.cardinality() + ": "
          + Escape.escape(bsA));
      Logger.info("Contacts to " + bsB.cardinality() + ": "
          + Escape.escape(bsB));
    }
    super.setProperty("newObject", null, null);
    thisMesh.setMerged(true);
    thisMesh.nSets = 0;
    Parameters params = sg.getParams();

    String func = null;
    switch (displayType) {
    case Token.full:
      func = "(a>b?a:b)";
      break;
    case Token.plane:
    case Token.cap:
      func = "a-b";
      break;
    case Token.connect:
      func = "a+b";
      break;
    }

    switch (displayType) {
    case Token.nci:
      colorByType = false;
      bs = BitSetUtil.copy(bsA);
      bs.or(bsB); // for now -- TODO -- need to distinguish ligand
      if (parameters[0] < 0)
        parameters[0] = 0; // reset to default for density
      params.colorDensity = colorDensity;
      params.bsSelected = bs;
      params.bsSolvent = bsB;
      sg.setParameter("parameters", parameters);
      super.setProperty("nci", Boolean.TRUE, null);
      break;
    case Token.surface:
      colorByType = false;
      if (rd == null)
        rd = rdVDW;
      thisMesh.nSets = 1;
      newSurface(Token.surface, null, bsA, bsB, rd, null, null, colorDensity,
          null);
      break;
    case Token.cap:
      colorByType = false;
      if (rd == null)
        rd = rdVDW;
      thisMesh.nSets = 1;
      newSurface(Token.slab, null, bsA, bsB, rd, null, null, false, null);
      VolumeData volumeData = sg.getVolumeData();
      sg.initState();
      newSurface(Token.plane, null, bsA, bsB, rd, parameters, func,
          colorDensity, volumeData);
      mergeMesh(null);
      break;
    case Token.connect:
    case Token.full:
    case Token.plane:
    case Token.trim:
      if (rd == null)
        rd = new RadiusData(0.25f, RadiusData.TYPE_OFFSET,
            JmolConstants.VDW_AUTO);
      float volume = 0;
      List<ContactPair> pairs = getPairs(bsA, bsB, rd, intramolecularMode);
      volume += combineSurfaces(pairs, contactType, displayType, parameters,
          func, colorDensity, colorByType);
      thisMesh.calculatedVolume = Float.valueOf(volume);
      mergeMesh(null);
      break;
    }
    thisMesh.setMerged(false);
    thisMesh.jvxlData.vertexDataOnly = true;
    thisMesh.reinitializeLightingAndColor();
    super.setProperty("finalize", command, null);
    if (colorDensity) {
      super.setProperty("pointSize", Float.valueOf(ptSize), null);
    } else {
      super.setProperty("token", Integer.valueOf(JmolConstants.FULLYLIT), null);
    }
    if (thisMesh.slabOptions != null) {
      thisMesh.slabOptions = null;
      thisMesh.polygonCount0 = -1; // disable slabbing.
    }
    discardTempData(true);
    String defaultColor = null;
    switch (contactType) {
    case Token.hbond:
      defaultColor = "lightgreen";
      break;
    case Token.clash:
      defaultColor = "yellow";
      break;
    case Token.surface:
      defaultColor = "skyblue";
      break;
    }
    ColorEncoder ce = null;
    if (colorByType) {
      ce = viewer.getColorEncoder("rwb");
      ce.setRange(-0.5f, 0.5f, false);
    } else if (defaultColor != null) {
      super.setProperty("color", Integer.valueOf(Graphics3D
          .getArgbFromString(defaultColor)), null);
    } else if (displayType == Token.nci) {
      ce = viewer.getColorEncoder("bgr");
      ce.setRange(-0.03f, 0.03f, false);
    } else {
      ce = viewer.getColorEncoder("rgb");
      if (colorDensity)
        ce.setRange(-0.3f, 0.3f, false);
      else
        ce.setRange(-0.5f, 1f, false);
    }
    if (ce != null)
      thisMesh.remapColors(ce, translucentLevel);
    Logger.checkTimer("contact");
  }

  /**
   * @param pairs 
   * @param contactType 
   * @param displayType 
   * @param parameters 
   * @param func 
   * @param isColorDensity 
   * @param colorByType  
   * @return               volume
   */
  private float combineSurfaces(List<ContactPair> pairs, int contactType,
                                int displayType, float[] parameters,
                                Object func, boolean isColorDensity,
                                boolean colorByType) {
    VolumeData volumeData = new VolumeData();
    int logLevel = Logger.getLogLevel();
    Logger.setLogLevel(0);
    float resolution = sg.getParams().resolution;
    int nContacts = pairs.size();
    double volume = 0;
    if (displayType == Token.full && resolution == Float.MAX_VALUE)
      resolution = (nContacts > 1000 ? 3 : 10);

    for (int i = nContacts; --i >= 0;) {
      ContactPair cp = pairs.get(i);
      if (contactType != Token.nada && cp.contactType != contactType)
        continue;
      int nV = thisMesh.vertexCount;
      thisMesh.nSets++;
      volume += cp.volume;
      setVolumeData(displayType, volumeData, cp, resolution, nContacts);
      switch (displayType) {
      case Token.full:
        newSurface(displayType, cp, null, null, null, null, func,
            isColorDensity, volumeData);
        cp.switchAtoms();
        newSurface(displayType, cp, null, null, null, null, null,
            isColorDensity, volumeData);
        break;
      case Token.trim:
      case Token.plane:
      case Token.connect:
        newSurface(displayType, cp, null, null, null, parameters, func,
            isColorDensity, volumeData);
        break;
      }
      if (i > 0 && (i % 1000) == 0 && logLevel == 4) {
        Logger.setLogLevel(4);
        Logger.info("contact..." + i);
        Logger.setLogLevel(0);
      }
      if (colorByType)
        for (int iv = thisMesh.vertexCount; --iv >= nV;)
          thisMesh.vertexValues[iv] = getVertexValueForType(cp.contactType, cp.score);
    }
    Logger.setLogLevel(logLevel);
    return (float) volume;
  }
  
  private static float getVertexValueForType(int contactType, float score) {
    return (contactType == Token.hbond ? 4 : score);
  }

  /**
   * 
   * @param contactType
   *        Token.vanderwaals, Token.clash, Token.hbond
   * @param displayType
   * @param bsA
   * @param bsB
   * @param rd
   * @param intramolecularMode
   * @return a list of pairs of atoms to process
   */
  private List<ContactPair> getPairs(BitSet bsA, BitSet bsB,
                                     RadiusData rd, int intramolecularMode) {
    List<ContactPair> list = new ArrayList<ContactPair>();
    AtomData ad = new AtomData();
    ad.radiusData = rd;
    BitSet bs = BitSetUtil.copy(bsA);
    bs.or(bsB);
    if (bs.isEmpty())
      return list;
    ad.bsSelected = bs;
    boolean isMultiModel = (atoms[bs.nextSetBit(0)].modelIndex != atoms[bs
        .length() - 1].modelIndex);
    boolean isSelf = bsA.equals(bsB);
    viewer.fillAtomData(ad, AtomData.MODE_FILL_RADII
        | (isMultiModel ? AtomData.MODE_FILL_MULTIMODEL : 0)
        | AtomData.MODE_FILL_MOLECULES);
    float maxRadius = 0;
    float hbondCutoff = -1.0f;//HBOND_CUTOFF;
    for (int ib = bsB.nextSetBit(0); ib >= 0; ib = bsB.nextSetBit(ib + 1))
      if (ad.atomRadius[ib] > maxRadius)
        maxRadius = ad.atomRadius[ib];
    AtomIndexIterator iter = viewer.getSelectedAtomIterator(bsB, isSelf, false,
        isMultiModel);
    for (int ia = bsA.nextSetBit(0); ia >= 0; ia = bsA.nextSetBit(ia + 1)) {
      Atom atomA = atoms[ia];
      float vdwA = atomA.getVanderwaalsRadiusFloat(viewer, JmolConstants.VDW_AUTO);
      if (isMultiModel)
        viewer.setIteratorForPoint(iter, -1, ad.atomXyz[ia], ad.atomRadius[ia]
            + maxRadius);
      else
        viewer.setIteratorForAtom(iter, ia, ad.atomRadius[ia] + maxRadius);
      while (iter.hasNext()) {
        int ib = iter.next();
        Atom atomB = atoms[ib];
        boolean isSameMolecule = (ad.atomMolecule[ia] == ad.atomMolecule[ib]);
        if (ia == ib || isSameMolecule
            && atomA.isWithinFourBonds(atomB))
          continue;
        switch (intramolecularMode) {
        case 0:
          break;
        case 1:
        case 2:
          if (isSameMolecule != (intramolecularMode == 1))
            continue;
        }
        float vdwB = atomB.getVanderwaalsRadiusFloat(viewer, JmolConstants.VDW_AUTO);
        float ra = ad.atomRadius[ia];
        float rb = ad.atomRadius[ib];
        float d = atomA.distance(atomB);
        if (d > ra + rb)
          continue;
        ContactPair cp = new ContactPair(atoms, ia, ib, ra, rb, vdwA, vdwB);
                
        // check for O--H...N or O...H--N and not considering
        // hydrogens and still have a filter

        // return is 1 (donor), -1 (acceptor), -2 (not), or 0 (unknown)
        int typeA = atomA.getHbondDonorAcceptorType();
        int typeB = (typeA == -2 ? -2 : atomB
            .getHbondDonorAcceptorType());
        boolean isHbond = (typeA != -2 && typeB != -2 && typeA * typeB <= 0 
            && cp.score > hbondCutoff);
        if (isHbond && cp.score < 0)
          cp.contactType = Token.hbond;
        
        list.add(cp);
      }
    }
    iter.release();
    iter = null;
    int n = list.size() - 1;
    for (int i = 0; i < n; i++) {
      ContactPair cp1 = list.get(i);
      for (int j = i + 1; j <= n; j++) {
        ContactPair cp2 = list.get(j);
        for (int m = 0; m < 2; m++) {
          for (int p = 0; p < 2; p++) {
            switch (checkCp(cp1, cp2, m, p)) {
            case 1:
              list.remove(i);
              j = n--;
              i--;
              break;
            case 2:
              list.remove(j);
              j--;
              n--;
              break;
            default:
            }
          }
        }
      }
    }

    if (Logger.debugging)
      for (int i = 0; i < list.size(); i++)
        Logger.info(list.get(i).toString());
    Logger.info("Contact pairs: " + list.size());
    return list;
  }

  /**
   * 
   * @param cp1
   * @param cp2
   * @param i1
   * @param i2
   * @return    0 (no clash); 1 (remove #1); 2 (remove #2)
   */
  private static int checkCp(ContactPair cp1, ContactPair cp2, int i1, int i2) {
    if (cp1.myAtoms[i1] != cp2.myAtoms[i2])
      return 0;
    boolean clash1 = (cp1.pt.distance(cp2.myAtoms[1 - i2]) < cp2.radii[1 - i2]);
    boolean clash2 = (cp2.pt.distance(cp1.myAtoms[1 - i1]) < cp1.radii[1 - i1]);
    // remove higher score (less overlap)
    return (!clash1 && !clash2 ? 0 : cp1.score > cp2.score ? 1 : 2);
  }

  private void newSurface(int displayType, ContactPair cp, BitSet bs1, BitSet bs2,
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
    switch (displayType) {
    case Token.surface:
    case Token.slab:
    case Token.trim:
    case Token.full:
      RadiusData rdA, rdB;
      if (displayType == Token.surface) {
        rdA = new RadiusData(1, RadiusData.TYPE_FACTOR, JmolConstants.VDW_AUTO);
        rdB = new RadiusData((rd.factorType == RadiusData.TYPE_OFFSET ? rd.value * 2 : (rd.value - 1) * 2 + 1), 
            rd.factorType, rd.vdwType);
      } else {
        rdA = rdB = rd;
      }
      params.colorDensity = isColorDensity;
      if (isColorDensity) {
        super.setProperty("cutoffRange", new float[] { -100f, 0f }, null);
      }
      if (cp == null) {
        params.atomRadiusData = rdA;
        params.bsIgnore = BitSetUtil.copyInvert(bs1, atomCount);
        params.bsSelected = bs1;
        params.bsSolvent = null;
      }
      params.volumeData = volumeData;
      super.setProperty("sasurface", Float.valueOf(0), null);
      super.setProperty("map", Boolean.TRUE, null);
      if (cp == null) {
        params.atomRadiusData = rdB;
        params.bsIgnore = BitSetUtil.copyInvert(bs2, atomCount);
        params.bsSelected = bs2;
      }
      params.volumeData = volumeData;
      super.setProperty("sasurface", Float.valueOf(0), null);
      switch (displayType) {
      case Token.full:
      case Token.trim:
        iSlab0 = -100;
        break;
      case Token.surface:
        if (isColorDensity)
          iSlab0 = -100;
        break;
      case Token.slab:
        iSlab1 = -100;
      }
      break;
    case Token.plane:
    case Token.connect:
      if (displayType == Token.connect)
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
      if (displayType != Token.connect)
        iSlab0 = -100;
    }
    if (iSlab0 != iSlab1)
      thisMesh.slabPolygons(MeshSurface.getSlabWithinRange(iSlab0, iSlab1),
          false);
    if (displayType != Token.surface)
      thisMesh.setMerged(true);
  }

  private Vector3f vZ = new Vector3f();
  private Vector3f vY = new Vector3f();
  private Vector3f vX = new Vector3f();
  private Point3f pt1 = new Point3f();
  private Point3f pt2 = new Point3f();
  
  private void setVolumeData(int type, VolumeData volumeData, ContactPair cp,
                             float resolution, int nPairs) {
    pt1.set(cp.myAtoms[0]);
    pt2.set(cp.myAtoms[1]);
    vX.sub(pt2, pt1);
    float dAB = vX.length();
    float dYZ = (cp.radii[0] * cp.radii[0] + dAB * dAB - cp.radii[1] * cp.radii[1])/(2 * dAB * cp.radii[0]);
    dYZ = 2.1f * (float) (cp.radii[0] * Math.sin(Math.acos(dYZ)));
    Measure.getNormalToLine(pt1, pt2, vZ);
    vZ.scale(dYZ);
    vY.cross(vZ, vX);
    vY.normalize();
    vY.scale(dYZ);
    if (type != Token.connect) {
      vX.normalize();
      pt1.scaleAdd((dAB - cp.radii[1]) * 0.95f, vX, pt1);
      pt2.scaleAdd((cp.radii[0] - dAB) * 0.95f, vX, pt2);
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
