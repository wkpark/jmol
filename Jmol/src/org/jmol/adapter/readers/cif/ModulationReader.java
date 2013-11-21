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
import javajs.util.List;
import javajs.util.SB;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

//import org.jmol.java.BS;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Modulation;
import org.jmol.util.ModulationSet;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import org.jmol.util.Tensor;
import javajs.util.V3;


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
  protected String modType;
  protected boolean checkSpecial = true;
  protected boolean modDebug;
  protected int modSelected = -1; 
  
  protected int modDim;
  protected boolean incommensurate;
  protected Atom[] atoms;
  //private BS bsAtoms;
  
  private P3 q1;  
  private V3 q1Norm;  
  private Map<String, P3> htModulation;
  private Map<String, List<Modulation>> htAtomMods;
  protected Map<String, Object> htSubsystems;
  
  
  protected void initializeModulation() throws Exception {
    modDebug = checkFilterKey("MODDEBUG");
    modAxes = getFilter("MODAXES=");
    modType = getFilter("MODTYPE="); //ODU
    modSelected = parseIntStr("" + getFilter("MOD="));
    modVib = checkFilterKey("MODVIB"); // then use MODULATION ON  to see modulation
    modAverage = checkFilterKey("MODAVE");
    checkSpecial = !checkFilterKey("NOSPECIAL");
    atomSetCollection.setCheckSpecial(checkSpecial);
    allowRotations = !checkFilterKey("NOSYM");
  }

  protected void setModDim(int ndim) {
    if (modAverage)
      return;
    modDim = ndim;
    if (modDim > 3) {
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
    char ch  = id.charAt(0);
      switch (ch) {
      case 'O':
      case 'D':
      case 'U':
        if (modType != null && modType.indexOf(ch) < 0 || modSelected > 0
            && modSelected != 1)
          return;
        break;
      }
    if (modSelected > 0 && id.contains("_q_"))
      switch (modSelected) {
      case 1:
        pt.y = pt.z = 0;
        break;
      case 2:
        pt.x = pt.z = 0;
        break;
      case 3:
        pt.x = pt.y = 0;
        break;
      }
    if (pt.x == 0 && pt.y == 0 && pt.z == 0)
      return;
    if (map == null)
      map = htModulation;
    id += "@"
        + (iModel >= 0 ? iModel : atomSetCollection.getCurrentAtomSetIndex());
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
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = true;
    setModulationForStructure(atomSetCollection.getCurrentAtomSetIndex());
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = false;
  }
  
  protected void finalizeModulation() {
    if (!incommensurate)
      return;
    if (!modVib)
      addJmolScript("modulation on" + (haveOccupancy  ? ";display occupancy > 0.5" : ""));
  }

  private String suffix;
  private P3 getMod(String key) {
    return htModulation.get(key + suffix);
  }
  
  private M3 q123;
  private double[] qlen;
  private boolean haveOccupancy;
  
  private void setModulationForStructure(int iModel) {
    suffix = "@" + iModel;
    String key;
    if (htModulation.containsKey("X_" + suffix))
      return;
    htModulation.put("X_" + suffix, new P3());
    q123 = new M3();
    qlen = new double[modDim];
    for (int i = 0; i < modDim; i++) {
      P3 pt = getMod("W_" + (i + 1));
      if (pt == null) {
        Logger.info("Not enough cell wave vectors for d=" + modDim);
        return;
      }
      appendLoadNote("W_" + (i + 1) + " = " + pt);
      if (i == 0)
        q1 = P3.newP(pt);
      q123.setRowV(i, pt);
      qlen[i] = pt.length();
    }
    q1Norm = V3.new3(q1.x == 0 ? 0 : 1, q1.y == 0 ? 0 : 1, q1.z == 0 ? 0 : 1);
    P3 qlist100 = P3.new3(1, 0, 0);
    P3 pt;
    int n = atomSetCollection.getAtomCount();
    Map<String, P3> map = new Hashtable<String, P3>();
    for (Entry<String, P3> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey())) == null)
        continue;
      pt = e.getValue();
      switch (key.charAt(0)) {
      case 'O':
        haveOccupancy = true;
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
      case 'W':
        if (modDim > 1) {
          continue;
        }
        //$FALL-THROUGH$
      case 'F':
        // convert JANA Fourier descriptions to standard descriptions
        if (key.indexOf("_q_") >= 0) {
          // d > 1 -- already set from coefficients
          appendLoadNote("Wave vector " + key + "=" + pt);
        } else {
          P3 ptHarmonic = getQCoefs(pt); 
          if (ptHarmonic == null) {
            appendLoadNote("Cannot match atom wave vector " + key + " " + pt
                + " to a cell wave vector or its harmonic");
          } else {
            String k2 = key + "_q_";
            if (!htModulation.containsKey(k2 + suffix)) {
              addModulation(map, k2, ptHarmonic, iModel);
              if (key.startsWith("F_"))
                appendLoadNote("atom wave vector " + key + " = " + pt + " fn = " + ptHarmonic);
            }
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
          htAtomMods = new Hashtable<String, List<Modulation>>();
        int fn = (id == 'S' ? 0 : parseIntStr(key.substring(2)));
        if (fn == 0) {
          addAtomModulation(atomName, axis, type, params, utens, qlist100);
        } else {
          P3 qlist = getMod("F_" + fn + "_q_");
          if (qlist == null) {
            Logger.error("Missing qlist for F_" + fn);
            appendLoadNote("Missing cell wave vector for atom wave vector "
                + fn + " for " + key + " " + params);
          } else {
            addAtomModulation(atomName, axis, type, params, utens, qlist);
          }
        }
        haveAtomMods = true;
        break;
      }
    }
    if (!haveAtomMods)
      return;
    atoms = atomSetCollection.getAtoms();
    symmetry = atomSetCollection.getSymmetry();
    iopLast = -1;
    SB sb = new SB();
    for (int i = atomSetCollection.getLastAtomSetAtomIndex(); i < n; i++)
      modulateAtom(atoms[i], sb);
    atomSetCollection.setAtomSetAtomProperty("modt", sb.toString(), -1);
    htAtomMods = null;
  }

  private P3[] qs;

  /**
   * determine simple linear combination assuming simple -3 to 3 no more than
   * two dimensions.
   * 
   * @param p
   * @return {i j k}
   */
  private P3 getQCoefs(P3 p) {
    if (qs == null) {
      qs = new P3[3];
      for (int i = 0; i < 3; i++)
        qs[i] = getMod("W_" + (i + 1));
    }
    P3 pt = new P3();
    // test n * q
    for (int i = 0; i < 3; i++)
      if (qs[i] != null) {
        float fn = p.dot(qs[i]) / qs[i].dot(qs[i]);
        int ifn = Math.round(fn);
        if (Math.abs(fn - ifn) < 0.001f) {
          switch(i) {
          case 0:
            pt.x = ifn;
            break;
          case 1:
            pt.y = ifn;
            break;
          case 2:
            pt.z = ifn;
            break;
          }
          return pt; 
        }
      }
    // test linear combination -3 to +3:
    int jmin = (modDim < 2 ? 0 : -3);
    int jmax = (modDim < 2 ? 0 : 3);
    int kmin = (modDim < 3 ? 0 : -3);
    int kmax = (modDim < 3 ? 0 : 3);
    for (int i = -3; i <= 3; i++)
      for (int j = jmin; j <= jmax; j++)
        for (int k = kmin; k <= kmax; k++) {
          pt.setT(qs[0]);
          pt.scale(i);
          if (qs[1] != null)
            pt.scaleAdd2(j, qs[1], pt);
          if (qs[2] != null)
            pt.scaleAdd2(k, qs[2], pt);
          if (pt.distanceSquared(p) < 0.0001f) {
            pt.set(i, j, 0);
            return pt;
          }
        }
    return null;
  }

  private void addAtomModulation(String atomName, char axis, int type,
                                 P3 params, String utens, P3 qcoefs) {
    List<Modulation> list = htAtomMods.get(atomName);
    if (list == null)
      htAtomMods.put(atomName, list = new List<Modulation>());
    list.addLast(new Modulation(axis, type, params, utens, qcoefs));
  }

  private String checkKey(String key) {
    int pt_ = key.indexOf(suffix);
    return (pt_ < 0 ? null : key.substring(0, pt_));
  }

  private int iopLast = -1;
  private M3 gammaE;
  private M4 gammaIS;
  private int nOps;
  
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
      
    List<Modulation> list = htAtomMods.get(a.atomName);
    if (list == null || symmetry == null || a.bsSymmetry == null)
      return;
    int iop = a.bsSymmetry.nextSetBit(0);
    if (iop < 0)
      iop = 0;
    //Logger.debugging = Logger.debuggingHigh = (iop == 2);
    //if (a.index == 5 || a.index == 7 || Logger.debugging)
    // Logger.debugging = Logger.debuggingHigh = true;

    if (Logger.debuggingHigh)
      Logger.debug("\nsetModulation: i=" + a.index + " " + a.atomName + " xyz="
          + a + " occ=" + a.foccupancy);
    if (iop != iopLast) {
      //System.out.println("mdim=" + mdim + " op=" + (iop + 1) + " " + symmetry.getSpaceGroupOperation(iop) + " " + symmetry.getSpaceGroupXyz(iop, false));
      iopLast = iop;
      gammaE = new M3();
      symmetry.getSpaceGroupOperation(iop).getRotationScale(gammaE);
      gammaIS = symmetry.getOperationGammaIS(iop);
      nOps = symmetry.getSpaceGroupOperationCount();
    }
    if (Logger.debugging) {
      Logger.debug("setModulation iop = " + iop + " "
          + symmetry.getSpaceGroupXyz(iop, false) + " " + a.bsSymmetry);
    }
    M4 q123w = M4.newMV(q123, new V3());
    setSubsystemMatrix(a.atomName, q123w);
    ModulationSet ms = new ModulationSet(a.index + " " + a.atomName, 
        P3.newP(a), modDim, list, gammaE, gammaIS, q123w, qlen);
    ms.calculate();
    if (!Float.isNaN(ms.vOcc)) {
      P3 pt = getMod("J_O#0;" + a.atomName);
      float occ0 = ms.vOcc0;
      float occ;
      if (Float.isNaN(occ0)) {
        // Crenel
        occ = ms.vOcc; 
      } else if (pt == null) {
        // cif Fourier
        // _atom_site_occupancy + SUM
        occ = a.foccupancy + ms.vOcc; 
      } else if (a.vib != null) {
        // cif with m40 Fourier
        // occ_site * (occ_0 + SUM)
        float site_mult = a.vib.x;
        float o_site = a.foccupancy * site_mult / nOps / pt.y;
        occ = o_site * (pt.y + ms.vOcc);  
      } else {
        // m40 Fourier
        // occ_site * (occ_0 + SUM)
        occ = pt.x * (pt.y + ms.vOcc);  
      }
      a.foccupancy = Math.min(1, Math.max(0, occ));
      a.vib = ms;
//      if (!modVib) {
//        if (occ < 0.5f) {
//          a.foccupancy = 0;
//          if (bsAtoms != null)
//            bsAtoms.clear(a.index);
//        } else if (occ >= 0.5f) {
//          a.foccupancy = 1;
//        }
//      } 
    } else if (ms.htUij != null) {
      // Uiso or Uij. We add the displacements, create the tensor, then rotate it, 
      // replacing the tensor already present for that atom.
      if (Logger.debuggingHigh) {
        Logger.debug("setModulation Uij(initial)=" + Escape.eAF(a.anisoBorU));
        Logger.debug("setModulation tensor="
            + Escape.e(((Tensor) a.tensors.get(0)).getInfo("all")));
      }
      for (Entry<String, Float> e : ms.htUij.entrySet())
        addUStr(a, e.getKey(), e.getValue().floatValue());

      if (a.tensors != null)
        ((Tensor)a.tensors.get(0)).isUnmodulated = true;
      Tensor t = atomSetCollection.addRotatedTensor(a, symmetry
          .getTensor(a.anisoBorU), iop, false);
      t.isModulated = true;
      if (Logger.debuggingHigh) {
        Logger.debug("setModulation Uij(final)=" + Escape.eAF(a.anisoBorU)
            + "\n");
        Logger.debug("setModulation tensor=" + ((Tensor) a.tensors.get(0)).getInfo("all"));
      }
    } else {
      a.vib = ms;
    }

    // set property_modT to be Math.floor (q.r/|q|) -- really only for d=1

    if (modVib || a.foccupancy != 0) {
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
  
  private void setSubsystemMatrix(String atomName, M4 q123w) {
    Object o;
    if (true || htSubsystems == null || (o = htSubsystems.get(";" + atomName)) == null)
      return;
// not sure what to do yet.
    String subcode = (String) o;
    M4 wmatrix = (M4) htSubsystems.get(subcode);
    q123w.mulM4(wmatrix);
  }

  protected void addSubsystem(String code, M4 m4, String atomName) {
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
