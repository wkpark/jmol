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

import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Modulation;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;


/**
 * abstract modulation class for CIF and Jana
 * 
 * Current status:
 * 
 * -- d=1 only
 * -- only simple atom displacement, no occupation crenel, no sawtooth
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
  protected boolean incommensurate;
  protected Atom[] atoms;
  
  private V3 q1Norm;  
  private Matrix3f rot;
  private Map<String, P3> htModulation;
  private Map<String, JmolList<Modulation>> htAtomMods;
  private int modT;
  
  protected void initializeMod() throws Exception {
    modAxes = getFilter("MODAXES=");
    modVib = checkFilterKey("MODVIB");
    modAverage = checkFilterKey("MODAVE");
    checkSpecial = !checkFilterKey("NOSPECIAL");
    atomSetCollection.setCheckSpecial(checkSpecial);
    allowRotations = !checkFilterKey("NOSYM");
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
  }

  /**
   * synthesizes modulation data
   * 
   */
  protected void setModulation() {
    if (!incommensurate || htModulation == null)
      return;
    int n = atomSetCollection.getAtomCount();
    Map<String, P3> map = new Hashtable<String, P3>();
    for (Entry<String, P3> e : htModulation.entrySet()) {
      String key = e.getKey();
      P3 pt = e.getValue();
      switch (key.charAt(0)) {
      case 'W':
        appendLoadNote("Wave vector " + key + " = " + pt);
        
        if (key.equals("W_1")) {
          q1Norm = V3.newV(pt);
          q1Norm.normalize();
        }
        break;
      case 'O':
          if (!modVib && atomSetCollection.bsAtoms == null)
            atomSetCollection.bsAtoms = BSUtil.newBitSet2(0, n);
        //$FALL-THROUGH$
      case 'D':
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
        } else {
          appendLoadNote("Wave vector " + key + " = " + pt);
        }
        break;
      }
    }
    if (!map.isEmpty())
      htModulation.putAll(map);
    boolean haveAtomMods = false;
    for (Entry<String, P3> e : htModulation.entrySet()) {
      String key = e.getKey();
      P3 params = e.getValue();
      String label = key.substring(key.indexOf(";") + 1);
      System.out.println(key);
      int type = key.charAt(0);
      switch (type) {
      case 'O':
      case 'D':
        char id = key.charAt(2);
        char axis = key.charAt(3);
        type = (id == 'S' ? Modulation.TYPE_DISP_SAWTOOTH 
            : id == '0' ? Modulation.TYPE_OCC_CRENEL 
            : type == 'O' ? Modulation.TYPE_OCC_FOURIER
            : Modulation.TYPE_DISP_FOURIER);
        if (htAtomMods == null)
          htAtomMods = new Hashtable<String, JmolList<Modulation>>();
        int fn = (id == 'S' ? -1 : id - '0');
        key = (fn <= 0 ? "W_1" : "F_" + fn);
        //TODO -- THIS IS WRONG. n is just a label. It will break in 2D
        //        but I don't know to determine "n" any other way in a CIF file. 
        P3 nq = htModulation.get(key);
        JmolList<Modulation> list = htAtomMods.get(label);
        if (list == null)
          htAtomMods.put(label, list = new JmolList<Modulation>());
        list.addLast(new Modulation(nq, axis, type, fn, params));
        haveAtomMods = true;
        break;
      }
    }
    if (!haveAtomMods)
      return;
    atoms = atomSetCollection.getAtoms();
    symmetry = atomSetCollection.getSymmetry();
    rot = new Matrix3f();
    SB sb = new SB();
    for (int i = 0; i < n; i++)
      modulateAtom(atoms[i], sb);
    atomSetCollection.setAtomSetAtomProperty("modt", sb.toString(), -1);
  }
  
  /**
   * The displacement will be set as the atom vibration vector; the string buffer
   * will be appended with the t value for a given unit cell. 
   * 
   * Modulation generally involves x4 = q.r + t. Here we arbitrarily set t = modT = 0, 
   * but modT could be a FILTER option MODT=n. There would need to be one modT per dimension.
   *  
   * @param a
   * @param sb
   */
  public void modulateAtom(Atom a, SB sb) {
    JmolList<Modulation> list = htAtomMods.get(a.atomName);
    if (list == null || symmetry == null || a.bsSymmetry == null)
      return;
    int iop = a.bsSymmetry.nextSetBit(0);
    if (iop < 0)
      iop = 0;
    float epsilon = symmetry.getModParam(iop, 0);
    float delta = symmetry.getModParam(iop, 1);
    delta -= modT;
    symmetry.getSpaceGroupOperation(iop).getRotationScale(rot);
    System.out.println("=========MR i=" + a.index + " " + a.atomName + " " + a + " " + a.occupancy);
    System.out.println("op=" + (iop + 1) + " " + symmetry.getSpaceGroupXyz(iop, false) + " ep=" + epsilon + " de=" + delta);
    a.vib = new V3();
    float val = Modulation.modulateAtom(list, a, epsilon, delta, rot, a.vib);
    if (val < 0.5f) {
      a.occupancy = 0;
      System.out.println("occup 0 for " + a.index + " a=" + a + " " + a.atomName);
      if (!modVib)
        atomSetCollection.bsAtoms.clear(a.index);
    }
    
    System.out.println("a.vib(abc)=" + a.vib);
    
    
    // set property_modT to be Math.floor (q.r/|q|) -- really only for d=1

    float t = q1Norm.dot(a);
    if (Math.abs(t - (int) t) > 0.001f)
      t = (int) Math.floor(t);
    sb.append(((int) t) + "\n");

    // displace the atom and reverse the vector only if not filter "MODVIB"
    if (!modVib) {
      a.add(a.vib);
      a.vib.scale(-1);
    }
    symmetry.toCartesian(a.vib, true);
    //System.out.println("a.vib(xyz)=" + a.vib);
    
  }
  
}
