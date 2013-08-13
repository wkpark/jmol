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

import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Modulation;
import org.jmol.util.ModulationSet;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.util.Tensor;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;


/**
 * abstract modulation class for CIF and Jana
 * 
 * Current status:
 * 
 * -- d=1 only
 * -- includes Fourier, Crenel, Sawtooth; displacement, occupancy, and Uiso
 * -- reading composite subsystem files such as ms-fit-1.cif
 * 
 * TODO: Uij, d > 1
 * TODO: handle subsystems properly
 * 
 * No plan to implement rigid-body rotation
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
  private BS bsAtoms;
  
  private P3 q1;  
  private V3 q1Norm;  
  private Map<String, P3> htModulation;
  private Map<String, JmolList<Modulation>> htAtomMods;
  private int modT;
  protected Map<String, Object> htSubsystems;
  
  
  protected void initializeMod() throws Exception {
    modAxes = getFilter("MODAXES=");
    modVib = checkFilterKey("MODVIB"); // then use MODULATION ON  to see modulation
    modAverage = checkFilterKey("MODAVE");
    if (!modVib && !modAverage)
      addJmolScript("modulation on");
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
    return htModulation.get(id + "@0");
  }

  protected void addModulation(Map<String, P3> map, String id, P3 pt, int iModel) {
    if (map == null)
      map = htModulation;
    id += "@" + (iModel >= 0 ? iModel : atomSetCollection.getCurrentAtomSetIndex());
    Logger.info("Adding " + id + " " + pt);
    map.put(id, pt);
  }

  /**
   * synthesizes modulation data
   * 
   */
  protected void setModulation() {
    if (!incommensurate || htModulation == null)
      return;
    setModulationForStructure(atomSetCollection.getCurrentAtomSetIndex());
  }
  
  private String suffix;
  private P3 getMod(String key) {
    return htModulation.get(key + suffix);
  }
  
  private void setModulationForStructure(int iModel) {
    suffix = "@" + iModel;
    String key;
    if (htModulation.containsKey("X_" + suffix))
      return;
    htModulation.put("X_" +suffix, new P3());
    q1 = getMod("W_1");
    if (q1 == null)
      return;
    q1Norm = V3.new3(q1.x == 0 ? 0 : 1, q1.y == 0 ? 0 : 1, q1.z == 0 ? 0 : 1);
    P3 pt;    
    int n = atomSetCollection.getAtomCount();
    Map<String, P3> map = new Hashtable<String, P3>();
    for (Entry<String, P3> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey())) == null)
        continue;
      pt = e.getValue();
      switch (key.charAt(0)) {
      case 'O':
          if (!modVib && bsAtoms == null)
            bsAtoms = atomSetCollection.bsAtoms = BSUtil.newBitSet2(0, n);
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
        if (key.indexOf("_q_") >= 0) {
          // this will fail until we set up fn for multiple q.
          P3 pf = new P3();
          if (pt.x != 0)
            pf.scaleAdd2(pt.x, getMod("W_1"), pf);
          if (pt.y != 0)
            pf.scaleAdd2(pt.y, getMod("W_2"), pf);
          if (pt.z != 0)
            pf.scaleAdd2(pt.z, getMod("W_3"), pf);
          key = TextFormat.simpleReplace(key, "_q_", "");
          addModulation(map, key, pf, iModel);
          appendLoadNote("Wave vector " + key + "(n=" + (modDim == 1 ? Integer.valueOf((int)pt.x) : pt)+") = " + pf);
        } else {
          int fn = (int) (pt.dot(q1) / q1.dot(q1) * 1.01f);
          String k2 = key  + "_q_";
          if (!htModulation.containsKey(k2 + suffix)) {
            addModulation(map, k2, P3.new3(fn, 0, 0), iModel);
            appendLoadNote("Wave vector " + key + " = " + pt + " n = " + fn);
          }
        }
        break;
      }
    }
    if (!map.isEmpty())
      htModulation.putAll(map);
    boolean haveAtomMods = false;
    for (Entry<String, P3> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey())) == null)
        continue;
      P3 params = e.getValue();
      String atomName = key.substring(key.indexOf(";") + 1);
      int pt_ = atomName.indexOf("#=");
      if (pt_ >= 0) {
        params = getMod(atomName.substring(pt_ + 2));
        atomName = atomName.substring(0, pt_);
      }
      if (Logger.debuggingHigh)
        Logger.debug("SetModulation: " + key + " " + params);
      int type = key.charAt(0);
      pt_ = key.indexOf("#") + 1;
      String utens = null;
      switch (type) {
      case 'U':
        utens = key.substring(4, key.indexOf(";"));
        //$FALL-THROUGH$
      case 'O':
      case 'D':
        char id = key.charAt(2);
        char axis = key.charAt(pt_);
        type = (id == 'S' ? Modulation.TYPE_DISP_SAWTOOTH 
            : id == '0' ? Modulation.TYPE_OCC_CRENEL 
            : type == 'O' ? Modulation.TYPE_OCC_FOURIER
            : type == 'U' ? Modulation.TYPE_U_FOURIER
            : Modulation.TYPE_DISP_FOURIER);
        if (htAtomMods == null)
          htAtomMods = new Hashtable<String, JmolList<Modulation>>();
        int fn = (id == 'S' ? 0 : parseIntStr(key.substring(2)));
        key = (fn == 0 ? "W_1" : "F_" + fn);
        //TODO -- THIS IS WRONG. n is just a label. It will break in 2D
        //        but I don't know to determine "n" any other way in a CIF file. 
        P3 nq = getMod(key);
        fn = (fn == 0 ? 1 : (int) getMod(key + "_q_").x);
        JmolList<Modulation> list = htAtomMods.get(atomName);
        if (list == null)
          htAtomMods.put(atomName, list = new JmolList<Modulation>());
        list.addLast(new Modulation(nq, axis, type, fn, params, utens));
        haveAtomMods = true;
        break;
      }
    }
    if (!haveAtomMods)
      return;
    atoms = atomSetCollection.getAtoms();
    symmetry = atomSetCollection.getSymmetry();
    iopLast = -1;
    f4 = new float[4];
    SB sb = new SB();
    for (int i = atomSetCollection.getLastAtomSetAtomIndex(); i < n; i++)
      modulateAtom(atoms[i], sb);
    atomSetCollection.setAtomSetAtomProperty("modt", sb.toString(), -1);
    htAtomMods = null;
  }

  private String checkKey(String key) {
    int pt_ = key.indexOf(suffix);
    return (pt_ < 0 ? null : key.substring(0, pt_));
  }

  private int iopLast = -1;
  private Matrix3f rot;
  private float[] f4;
  private float epsilon, delta;
  
  /**
   * The displacement will be set as the atom vibration vector; the string
   * buffer will be appended with the t value for a given unit cell.
   * 
   * Modulation generally involves x4 = q.r + t. Here we arbitrarily set t =
   * modT = 0, but modT could be a FILTER option MODT=n. There would need to be
   * one modT per dimension.
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
    // now set to 
    int mdim = 0;    // just the first row -- assuming d=1 here
    
    //if (a.index == 40)
      //Logger.debuggingHigh = Logger.debugging = true;

    if (Logger.debuggingHigh)
      Logger.debug("setModulation: i=" + a.index + " " + a.atomName + " xyz=" + a + " occ=" + a.occupancy);
    if (iop != iopLast) {
      symmetry.getMod456Row(iop, mdim, f4);
      //System.out.println("mdim=" + mdim + " op=" + (iop + 1) + " " + symmetry.getSpaceGroupOperation(iop) + " " + symmetry.getSpaceGroupXyz(iop, false));
      iopLast = iop;
      rot = new Matrix3f();
      epsilon = f4[0];  //symmetry.getModParam(iop, 0);
      delta = f4[3] - modT;    //symmetry.getModParam(iop, 1);
    }
    symmetry.getSpaceGroupOperation(iop).getRotationScale(rot);
    if (Logger.debugging) {
      Logger.debug("setModulation iop = " + iop + " "  + symmetry.getSpaceGroupXyz(iop, false) + " " + a.bsSymmetry);
    }
    ModulationSet ms = new ModulationSet(a.index + " " + a.atomName, list);
    a.vib = ms;
    ms.epsilon = epsilon;
    ms.delta = delta;
    ms.r = P3.newP(a);
    ms.rot = rot;
    ms.calculate();
    if (ms.v < 0.5f) {
      a.occupancy = 0;
      if (bsAtoms != null)
        bsAtoms.clear(a.index);
    }
    if (ms.htValues != null) {
      // Uiso or Uij. We add the displacements, create the tensor, then rotate it, 
      // replacing the tensor already present for that atom.
      if (Logger.debuggingHigh) {
        Logger.debug("setModulation Uij(initial)=" + Escape.eAF(a.anisoBorU));
        Logger.debug("setModulation tensor=" + Escape.e(a.tensors.get(0).getInfo("all")));
      }
      for (Entry<String, Float>e : ms.htValues.entrySet())
        addUStr(a, e.getKey(), e.getValue().floatValue());
      
      if (a.tensors != null)
        a.tensors.get(0).isUnmodulated = true;
      Tensor t = atomSetCollection.addRotatedTensor(a, symmetry.getTensor(a.anisoBorU), iop, false);
      t.isModulated = true;
      if (Logger.debuggingHigh) {
        Logger.debug("setModulation Uij(final)=" + Escape.eAF(a.anisoBorU) + "\n");
        Logger.debug("setModulation tensor=" + a.tensors.get(0).getInfo("all"));
      }
    }

    // set property_modT to be Math.floor (q.r/|q|) -- really only for d=1

    if (modVib || a.occupancy != 0) {
      float t = q1Norm.dot(a);
      if (Math.abs(t - (int) t) > 0.001f)
        t = (int) Math.floor(t);
      sb.append(((int) t) + "\n");
    }
    // displace the atom and reverse the vector only if not filter "MODVIB"
//    if (!modVib) {
  //    a.add(ms);
    //  ms.setModT(true, Integer.MAX_VALUE);
   // }
    symmetry.toCartesian(ms, true);
    //System.out.println("a.vib(xyz)=" + a.vib);
  }
  
  protected void addSubsystem(String code, Matrix4f m4, String atomName) {
    if (htSubsystems == null)
      htSubsystems = new Hashtable<String, Object>();
    if (m4 == null)
      htSubsystems.put(";" + atomName, code);
    else
      htSubsystems.put(code, m4);
  }

  protected final static String U_LIST = "U11U22U33U12U13U23OTPUISO";
  
  private void addUStr(Atom atom, String id, float val) {
    int i = U_LIST.indexOf(id) / 3;
    if (Logger.debuggingHigh)
      Logger.debug("MOD RDR adding " + id + " " + i + " " + val + " to " + atom.anisoBorU[i]);
    setU(atom, i, val + atom.anisoBorU[i]);
  }
  
  protected void setU(Atom atom, int i, float val) {
    // Ortep Type 8: D = 2pi^2, C = 2, a*b*
    float[] data = atomSetCollection.getAnisoBorU(atom);
    if (data == null)
      atomSetCollection.setAnisoBorU(atom, data = new float[8], 8);
    data[i] = val;
  }


}
