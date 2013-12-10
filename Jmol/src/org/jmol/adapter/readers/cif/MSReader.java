package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.List;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.MSInterface;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Modulation;
import org.jmol.util.ModulationSet;
import org.jmol.util.Tensor;

/**
 * generalized modulated structure reader class for CIF and Jana
 * 
 * Current status:
 * 
 * -- includes Fourier, Crenel, Sawtooth; displacement, occupancy, and Uiso --
 * reading composite subsystem files such as ms-fit-1.cif but not handling
 * matrix yet
 * 
 * TODO: Uij, d > 1 TODO: handle subsystems properly
 * 
 * No plan to implement rigid-body rotation
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/7/13
 * 
 */

public class MSReader implements MSInterface {

  protected AtomSetCollectionReader cr;

  protected boolean modVib;
  protected String modAxes;
  protected boolean modAverage;
  protected String modType;
  protected boolean modDebug;
  protected int modSelected = -1;
  protected boolean modLast;

  protected int modDim;

  private P3 q1;
  private V3 q1Norm;
  private Map<String, P3> htModulation;
  private Map<String, List<Modulation>> htAtomMods;
  protected Map<String, Object> htSubsystems;

  public MSReader() {
    // for reflection from Jana
  }

  @Override
  public int initialize(AtomSetCollectionReader r, String data)
      throws Exception {
    cr = r;
    modDebug = r.checkFilterKey("MODDEBUG");
    modLast = r.checkFilterKey("MODLAST"); // select last symmetry, not first, for special positions  
    modAxes = r.getFilter("MODAXES="); // xyz
    modType = r.getFilter("MODTYPE="); //ODU
    modSelected = r.parseIntStr("" + r.getFilter("MOD="));
    modVib = r.checkFilterKey("MODVIB"); // then use MODULATION ON  to see modulation
    modAverage = r.checkFilterKey("MODAVE");
    setModDim(r.parseIntStr(data));
    return modDim;
  }

  protected void setModDim(int ndim) {
    if (modAverage)
      return;
    modDim = ndim;
    if (modDim > 3) {
      // not ready for dim=2
      cr.appendLoadNote("Too high modulation dimension (" + modDim
          + ") -- reading average structure");
      modDim = 0;
      modAverage = true;
    } else {
      cr.appendLoadNote("Modulation dimension = " + modDim);
      htModulation = new Hashtable<String, P3>();
    }
  }

  /**
   * Types include O (occupation) D (displacement) U (anisotropy) _q_ indicates
   * this is a wave description
   * 
   * 
   * @param map
   * @param id
   * @param pt
   * @param iModel
   */
  @Override
  public void addModulation(Map<String, P3> map, String id, P3 pt, int iModel) {
    char ch = id.charAt(0);
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
        + (iModel >= 0 ? iModel : cr.atomSetCollection.getCurrentAtomSetIndex());
    Logger.info("Adding " + id + " " + pt);
    map.put(id, pt);
  }

  /**
   * Both the Jana reader and the CIF reader will call this to set the
   * modulation for a given model.
   * 
   */
  @Override
  public void setModulation() {
    if (modDim == 0 || htModulation == null)
      return;
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = true;
    setModulationForStructure(cr.atomSetCollection.getCurrentAtomSetIndex());
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = false;
  }

  /**
   * Create a script that will run to turn modulation on and to display only
   * atoms with modulated occupancy > 0.5.
   * 
   */
  @Override
  public void finalizeModulation() {
    if (modDim > 0 && !modVib)
      cr.addJmolScript("modulation on"
          + (haveOccupancy ? ";display occupancy > 0.5" : ""));
  }

  private String atModel = "@0";

  /**
   * Filter keys only for this model.
   * 
   * @param key
   * @param checkQ
   * @return trimmed key without model part or null
   * 
   */
  private String checkKey(String key, boolean checkQ) {
    int pt = key.indexOf(atModel);
    return (pt < 0 || key.indexOf("*;*") >= 0 || checkQ
        && key.indexOf("?") >= 0 ? null : key.substring(0, pt));
  }

  /**
   * Modulation data keys are keyed by model number as well as type using [at]n,
   * where n is the model number, starting with 0.
   * 
   * @param key
   * @return modulation data
   */
  @Override
  public P3 getMod(String key) {
    return htModulation.get(key + atModel);
  }

  private M3 q123;
  private double[] qlen;
  private boolean haveOccupancy;
  private Atom[] atoms;

  private int atomCount;

  /**
   * Called when structure creation is complete and all modulation data has been
   * collected.
   * 
   * @param iModel
   */
  private void setModulationForStructure(int iModel) {
    atModel = "@" + iModel;
    String key;

    // check to see we have not already done this.

    if (htModulation.containsKey("X_" + atModel))
      return;
    htModulation.put("X_" + atModel, new P3());
    // we allow for up to three wave vectors in the form of a matrix
    // along with their lengths as an array.

    q123 = new M3();
    qlen = new double[modDim];
    qs = null;

    // we should have W_1, W_2, W_3 up to the modulation dimension

    for (int i = 0; i < modDim; i++) {
      P3 pt = getMod("W_" + (i + 1));
      if (pt == null) {
        Logger.info("Not enough cell wave vectors for d=" + modDim);
        return;
      }
      cr.appendLoadNote("W_" + (i + 1) + " = " + pt);
      if (i == 0)
        q1 = P3.newP(pt);
      q123.setRowV(i, pt);
      qlen[i] = pt.length();
    }

    // q1Norm is used specifically for occupancy modulation, where dim = 1 only

    q1Norm = V3.new3(q1.x == 0 ? 0 : 1, q1.y == 0 ? 0 : 1, q1.z == 0 ? 0 : 1);
    P3 qlist100 = P3.new3(1, 0, 0);
    P3 pt;
    int n = cr.atomSetCollection.getAtomCount();

    // Take care of loose ends.
    // O: occupation   (set haveOccupancy; set a cos(theta) + b sin(theta) format)
    // D: displacement (set a cos(theta) + b sin(theta) format)
    // U: anisotropy   (no issues)
    // W: primary wave vector (see F if dim > 1)
    // F: Jana-type wave vector, referencing W vectors (set pt to coefficients, including harmonics)

    Map<String, P3> map = new Hashtable<String, P3>();
    for (Entry<String, P3> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey(), false)) == null)
        continue;
      pt = e.getValue();
      switch (key.charAt(0)) {
      case 'O':
        haveOccupancy = true;
        //$FALL-THROUGH$
      case 'U':
      case 'D':
        // fix modulus/phase option only for non-special modulations;
        if (pt.z == 1 && key.charAt(2) != 'S') {
          int ipt = key.indexOf("?");
          if (ipt >= 0) {
            String s = key.substring(ipt + 1);
            pt = getMod(key.substring(0, 2) + s + "#*;*");
            // may have       Vy1    0.0          0.0   , resulting in null pt here
            if (pt != null)
              addModulation(map, key = key.substring(0, ipt), pt, iModel);
          } else {
            // modulus/phase M cos(2pi(q.r) + 2pi(p))
            //  --> A cos(2pi(p)) cos(2pi(q.r)) + A sin(-2pi(p)) sin(2pi(q.r))
            double a = pt.x;
            double d = 2 * Math.PI * pt.y;
            pt.x = (float) (a * Math.cos(d));
            pt.y = (float) (a * Math.sin(-d));
            pt.z = 0;
            Logger.info("msCIF setting " + key + " " + pt);
          }
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
          cr.appendLoadNote("Wave vector " + key + "=" + pt);
        } else {
          P3 ptHarmonic = getQCoefs(pt);
          if (ptHarmonic == null) {
            cr.appendLoadNote("Cannot match atom wave vector " + key + " " + pt
                + " to a cell wave vector or its harmonic");
          } else {
            String k2 = key + "_q_";
            if (!htModulation.containsKey(k2 + atModel)) {
              addModulation(map, k2, ptHarmonic, iModel);
              if (key.startsWith("F_"))
                cr.appendLoadNote("atom wave vector " + key + " = " + pt
                    + " fn = " + ptHarmonic);
            }
          }
        }
        break;
      }
    }

    if (!map.isEmpty())
      htModulation.putAll(map);

    // Collect atom modulations as lists keyed on atom names in htAtomMods.
    // Loop through all modulations, selecting only those for the current model.
    // Process O, D, and U modulations via method addAtomModulation

    boolean haveAtomMods = false;
    for (Entry<String, P3> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey(), true)) == null)
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
        int fn = (id == 'S' ? 0 : cr.parseIntStr(key.substring(2)));
        if (fn == 0) {
          addAtomModulation(atomName, axis, type, params, utens, qlist100);
        } else {
          P3 qlist = getMod("F_" + fn + "_q_");
          if (qlist == null) {
            Logger.error("Missing qlist for F_" + fn);
            cr.appendLoadNote("Missing cell wave vector for atom wave vector "
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

    // here we go -- apply all atom modulations. 

    atoms = cr.atomSetCollection.getAtoms();
    cr.symmetry = cr.atomSetCollection.getSymmetry();
    if (cr.symmetry != null)
      nOps = cr.symmetry.getSpaceGroupOperationCount();
    iopLast = -1;
    SB sb = new SB();
    for (int i = cr.atomSetCollection.getLastAtomSetAtomIndex(); i < n; i++)
      modulateAtom(atoms[i], sb);
    cr.atomSetCollection.setAtomSetAtomProperty("modt", sb.toString(), -1);
    cr.appendLoadNote(modCount + " modulations for " + atomCount + " atoms");
    htAtomMods = null;
  }

  private P3[] qs;

  private int modCount;

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
          switch (i) {
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

  /**
   * Create a list of modulations for each atom type (atom name).
   * 
   * @param atomName
   * @param axis
   * @param type
   * @param params
   * @param utens
   * @param qcoefs
   */
  private void addAtomModulation(String atomName, char axis, int type,
                                 P3 params, String utens, P3 qcoefs) {
    List<Modulation> list = htAtomMods.get(atomName);
    if (list == null) {
      atomCount++;
      htAtomMods.put(atomName, list = new List<Modulation>());
    }
    list.addLast(new Modulation(axis, type, params, utens, qcoefs));
    modCount++;
  }

  private void setSubsystemMatrix(String atomName, M4 q123w) {
    Object o;
    if (true || htSubsystems == null
        || (o = htSubsystems.get(";" + atomName)) == null)
      return;
    // not sure what to do yet.
    String subcode = (String) o;
    M4 wmatrix = (M4) htSubsystems.get(subcode);
    q123w.mulM4(wmatrix);
  }

  @Override
  public void addSubsystem(String code, M4 m4, String atomName) {
    if (htSubsystems == null)
      htSubsystems = new Hashtable<String, Object>();
    if (m4 == null)
      htSubsystems.put(";" + atomName, code);
    else
      htSubsystems.put(code, m4);
  }

  private final static String U_LIST = "U11U22U33U12U13U23UISO";

  private void addUStr(Atom atom, String id, float val) {
    int i = U_LIST.indexOf(id) / 3;
    if (Logger.debuggingHigh)
      Logger.debug("MOD RDR adding " + id + " " + i + " " + val + " to "
          + atom.anisoBorU[i]);
    if (atom.anisoBorU == null)
      Logger
          .error("MOD RDR cannot modulate nonexistent atom anisoBorU for atom "
              + atom.atomName);
    else
      cr.setU(atom, i, val + atom.anisoBorU[i]);
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

    // Modulation is based on an atom's first symmetry operation.
    // (Special positions should generate the same atom regardless of which operation is employed.)

    List<Modulation> list = htAtomMods.get(a.atomName);
    if (list == null || cr.symmetry == null || a.bsSymmetry == null)
      return;
    int iop = Math.max(a.bsSymmetry.nextSetBit(0), 0);
    if (modLast)
      iop = Math.max((a.bsSymmetry.length() - 1)% nOps, iop);
    System.out.println(a.index + " " + a.atomName + " " + iop + " " + a.bsSymmetry);
    if (Logger.debuggingHigh)
      Logger.debug("\nsetModulation: i=" + a.index + " " + a.atomName + " xyz="
          + a + " occ=" + a.foccupancy);
    if (iop != iopLast) {
      // for each new operator, we need to generate new matrices.
      // gammaE is the pure rotation part of the operation;
      // gammaIS is a full rotation/translation matrix in fractional coordinates.
      // nOps is used as a factor in occupation modulation only.

      //System.out.println("mdim=" + mdim + " op=" + (iop + 1) + " " + symmetry.getSpaceGroupOperation(iop) + " " + symmetry.getSpaceGroupXyz(iop, false));
      iopLast = iop;
      gammaE = new M3();
      cr.symmetry.getSpaceGroupOperation(iop).getRotationScale(gammaE);
      gammaIS = cr.symmetry.getOperationGammaIS(iop);
    }
    if (Logger.debugging) {
      Logger.debug("setModulation iop = " + iop + " "
          + cr.symmetry.getSpaceGroupXyz(iop, false) + " " + a.bsSymmetry);
    }

    // TODO: subsystem matrices are not implemeneted yet.
    M4 q123w = M4.newMV(q123, new V3());
    setSubsystemMatrix(a.atomName, q123w);

    // The magic happens here.

    ModulationSet ms = new ModulationSet(a.index + " " + a.atomName,
        P3.newP(a), modDim, list, gammaE, gammaIS, q123w, qlen);
    ms.calculate(0);

    // ms parameter values are used to set occupancies, 
    // vibrations, and anisotropy tensors.

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
    }
    if (ms.htUij != null) {
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
        ((Tensor) a.tensors.get(0)).isUnmodulated = true;
      Tensor t = cr.atomSetCollection.addRotatedTensor(a,
          cr.symmetry.getTensor(a.anisoBorU), iop, false);
      t.isModulated = true;
      if (Logger.debuggingHigh) {
        Logger.debug("setModulation Uij(final)=" + Escape.eAF(a.anisoBorU)
            + "\n");
        Logger.debug("setModulation tensor="
            + ((Tensor) a.tensors.get(0)).getInfo("all"));
      }
    }
    a.vib = ms;
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
    cr.symmetry.toCartesian(ms, true);
    //System.out.println("a.vib(xyz)=" + a.vib);
  }

}
