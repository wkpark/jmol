/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.util.JmolList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Modulation;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;


/**
 * abstract modulation class for CIF and Jana
 *  
 *  @author Bob Hanson hansonr@stolaf.edu 8/7/13
 *  
 */
abstract public class ModulationReader extends AtomSetCollectionReader {

  protected boolean allowRotations = true;
  protected boolean modVib;
  protected String modAxes;
  protected boolean modAverage;
  protected boolean checkSpecial = true;
  protected int modDim;
  //protected boolean modCentered;
  //protected boolean modOffset;
  protected boolean incommensurate;
  private Map<String, P3> htModulation;
  protected Atom[] atoms;
  
  protected void initializeMod() throws Exception {
    modAxes = getFilter("MODAXES=");
    modVib = checkFilterKey("MODVIB");
    modAverage = checkFilterKey("MODAVE");
    checkSpecial = !checkFilterKey("NOSPECIAL");
    atomSetCollection.setCheckSpecial(checkSpecial);
    allowRotations = !checkFilterKey("NOSYM");
    //modOffset = !checkFilterKey("NOMODOFFSET");
    //modCentered = !checkFilterKey("NOMODCENT");
    //if (!modCentered) {
    //  if (doCentralize && filter.indexOf("CENTER") == filter.lastIndexOf("CENTER"))
    //    doCentralize = false;
    //  appendLoadNote("CIF reader not using delta to recenter modulation.");
    // }
  }

  private JmolList<float[]> lattvecs;

  protected void addLatticeVector(String data) {
    if (lattvecs == null)
      lattvecs = new JmolList<float[]>();
    float[] a = getTokensFloat(data, null, modDim + 3);
    boolean isOK = false;
    for (int i = a.length; --i >= 0 && !isOK;) {
      if (Float.isNaN(a[i]))
        return;
      isOK |= (a[i] != 0);
    }
    if (isOK)
      lattvecs.addLast(a);
  }

  protected void finalizeIncommensurate() {
    if (incommensurate)
      atomSetCollection.setBaseSymmetryAtomCount(atomSetCollection.getAtomCount());
    if (lattvecs != null)
      atomSetCollection.getSymmetry().addLatticeVectors(lattvecs);
  }

  protected void setModDim(String token) {
    modDim = parseIntStr(token);
    if (modAverage)
      return;
    if (modDim > 1) {
      // not ready for dim=2
      appendLoadNote("Too high modulation dimension (" + modDim + ") -- reading average structure");
      modDim = 0;
      modAverage = true;
    } else {
      appendLoadNote("Modulation dimension = " + modDim);   
      htModulation = new Hashtable<String, P3>();
    }
    incommensurate = (modDim > 0);
  }
  
  protected P3 getModulationVector(String id) {
    return htModulation.get(id);
  }
  
  protected void addModulation(Map<String, P3> map, String id, P3 pt) {
    if (map == null)
      map = htModulation;
    map.put(id, pt);
    Logger.info("Adding " + id + " " + pt);
    if (id.charAt(0) == 'W' || id.charAt(0) == 'F')
      appendLoadNote("Wave vector " + id +" = " + pt);   
  }

  /**
   * synthesizes modulation data
   * 
   */
  protected void setModulation() {
    if (!incommensurate || htModulation == null)
      return;
    Map<String, P3> map = new Hashtable<String, P3>();
    for (Entry<String, P3> e : htModulation.entrySet()) {
      String key = e.getKey();
      P3 pt = e.getValue();
      switch (key.charAt(0)) {
      case 'D':
      case 'O':
        // fix modulus/phase option only for non-special modulations;
        if (pt.z == 1 && key.charAt(2) != 'S') {
          // modulus/phase M cos(2pi(q.r) + 2pi(p))
          //  --> A cos(2pi(p)) cos(2pi(q.r)) + A sin(-2pi(p)) sin(2pi(q.r))
          double a = pt.x;
          double d = 2 * Math.PI * pt.y;
          pt.x = (float) (a * Math.cos(d));
          pt.y = (float) (a * Math.sin(-d));
          pt.z = 0;
          Logger.info("msCIF setting " + key + " " + pt);
        }
        break;
      case 'F':
        // convert JAVA Fourier descriptions to standard descriptions
        // this will fail now.
        if (key.indexOf("_q_") >= 0) {
          P3 pf = new P3();
          if (pt.x != 0)
            pf.scaleAdd2(pt.x, htModulation.get("W_1"), pf);
          if (pt.y != 0)
            pf.scaleAdd2(pt.y, htModulation.get("W_2"), pf);
          if (pt.z != 0)
            pf.scaleAdd2(pt.z, htModulation.get("W_3"), pf);
          key = TextFormat.simpleReplace(key, "_q_", "");
          addModulation(map, key, pf);
        }
        break;
      }
    }
    if (!map.isEmpty())
      htModulation.putAll(map);
    boolean haveAtomMods = false;
    for (Entry<String, P3> e : htModulation.entrySet()) {
      String key = e.getKey();
      switch (key.charAt(0)) {
      case 'O':
        // TODO
        break;
      case 'D':
        char axis = key.charAt(3);
        if (key.charAt(2) == 'S') {
          // TODO -- sawtooth, so now what? 
        } else {
          P3 coefs = e.getValue();
          String label = key.substring(key.indexOf(";") + 1);
          int n = key.charAt(2) - '0';
          key = "F_" + n;
          P3 q = htModulation.get(key);
          addAtomModulation(label, q, axis, coefs, n);
          haveAtomMods = true;
        }
        break;
      }
    }
    if (!haveAtomMods)
      return;
    atoms = atomSetCollection.getAtoms();
    symmetry = atomSetCollection.getSymmetry();
    mtemp3 = new Matrix3f();
    //mtemp3i = new Matrix3f();
    ptemp = new P3();
    offset = new V3();
    //opTrans = new V3();
    // step one: All base atoms.
    int n = atomSetCollection.baseSymmetryAtomCount;
    P3[] pts = new P3[n];
    SB sb = new SB();
    for (int i = 0; i < n; i++) {
      pts[i] = P3.newP(atoms[i]);
      modulateAtom(i, modVib, null, sb);
    }
    // step two: All other atoms.
    int n1 = atomSetCollection.getAtomCount();
    for (int i = n1; --i >= n;)
      modulateAtom(i, modVib, pts[atoms[i].atomSite], sb);
    atomSetCollection.setAtomSetAtomProperty("modt", sb.toString(), -1);
  }
  
  private Matrix3f mtemp3;
  private P3 ptemp;
  private V3 offset;
  private Map<String, JmolList<Modulation>> htAtomMods;
  
  public void addAtomModulation(String label, P3 q, char axis, P3 coefs, int n) {
    if (htAtomMods == null)
      htAtomMods = new Hashtable<String, JmolList<Modulation>>();
    JmolList<Modulation> list = htAtomMods.get(label);
    if (list == null)
      htAtomMods.put(label, list = new JmolList<Modulation>());
    list.addLast(new Modulation(q, n, axis, coefs));
  }
  
  private V3 qNorm;

  /**
   * Modulation generally involves u(x4) = u(q.r + t). Here we arbitrarily set t
   * = 0, making this u(x4) = u(q.r). For symmetry- related atoms, including
   * lattice shifts, we need to apply this as:
   * 
   * u'(x4') = R'u(q.r + q.s')
   * 
   * where s' is the sum of all shifts, and R' is the product of all rotations.
   * We already track symmetry, so we should be able to figure this out.
   * 
   * @param i
   * @param modvib
   * @param pt0
   * @param sb
   */
  public void modulateAtom(int i, boolean modvib, P3 pt0, SB sb) {
    Atom a = atoms[i];
    a.vib = new V3();
    JmolList<Modulation> list = htAtomMods.get(a.atomName);
    if (list == null || symmetry == null)
      return;
    //    if (pt0 != null) {
    int iop = a.bsSymmetry.nextSetBit(0);
    if (iop < 0)
      iop = 0;
    Matrix4f m = symmetry.getSpaceGroupOperation(iop);
    m.getRotationScale(mtemp3);
    //mtemp3i.invertM(mtemp3);
    //opTrans = symmetry.getOriginalTranslation(iop);
    //m.get(opTrans);
    float epsilon = symmetry.getModParam(iop, 0);
    float delta = symmetry.getModParam(iop, 1);
    ptemp.setT(a);
    symmetry.unitize(ptemp);
    offset.sub2(a, ptemp);
    //System.out.println("=========CIF i=" + i + " " + a.atomName + " " + a);
    //System.out.println("op=" + (iop + 1) + " "
      //  + symmetry.getSpaceGroupXyz(iop, false) + " ep=" + epsilon + " de="
        //+ delta + " a=" + a);
    qNorm = V3.newV(list.get(0).getWaveVector());
    qNorm.normalize();
    Modulation.modulateAtom(ptemp, offset, list, epsilon, delta, a.vib);
    System.out.println("a.vib(abc)=" + a.vib);
    mtemp3.transform(a.vib);
    sb.append((int) (qNorm.dot(offset) * 1.01f) + "\n");
    if (!modvib) {
      a.add(a.vib);
      a.vib.scale(-1);
    }
    symmetry.toCartesian(a.vib, true);
    //System.out.println("a.vib(xyz)=" + a.vib);
    //if (i == 98 || i == 99)
      //System.out.println("CIFTEST");
  }
}
