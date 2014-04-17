package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.MSInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
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
 * TODO: Uij, d > 1
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/7/13
 * 
 */

public class MSReader implements MSInterface {

  protected AtomSetCollectionReader cr; // Cif or Jana

  protected int modDim;
  protected String modAxes;
  protected boolean modAverage;
  protected boolean isCommensurate;
  protected int commensurateSection1; // TODO

  private boolean modPack;
  private boolean modVib;
  private String modType;
  private String modCell;
  private boolean modDebug;
  private int modSelected = -1;
  private boolean modLast;

  private Matrix sigma;

  Matrix getSigma() {
    return sigma;
  }

  private double[] q1;
  private P3 q1Norm;
  private Map<String, double[]> htModulation;
  private Map<String, Lst<Modulation>> htAtomMods;

  private int iopLast = -1;
  private M3 gammaE; // standard operator rotation matrix
  private int nOps;
  private boolean haveOccupancy;
  private Atom[] atoms;
  private int ac;
  private boolean haveAtomMods;

  private boolean modCoord;

  private boolean finalized;

  public MSReader() {
    // for reflection from Jana
  }

  @Override
  public int initialize(AtomSetCollectionReader r, int modDim)
      throws Exception {
    cr = r;
    modCoord = r.checkFilterKey("MODCOORD");
    modDebug = r.checkFilterKey("MODDEBUG");
    modPack = !r.checkFilterKey("MODNOPACK");
    modLast = r.checkFilterKey("MODLAST"); // select last symmetry, not first, for special positions  
    modAxes = r.getFilter("MODAXES="); // xyz
    modType = r.getFilter("MODTYPE="); //ODU
    modCell = r.getFilter("MODCELL="); // substystem for cell
    modSelected = r.parseIntStr("" + r.getFilter("MOD="));
    modVib = r.checkFilterKey("MODVIB"); // then use MODULATION ON  to see modulation
    modAverage = r.checkFilterKey("MODAVE");
    setModDim(modDim);
    return modDim;
  }

  private void setSubsystemOptions() {
    cr.doPackUnitCell = modPack;
    if (!cr.doApplySymmetry) {
      cr.doApplySymmetry = true;
      cr.latticeCells[0] = 1;
      cr.latticeCells[1] = 1;
      cr.latticeCells[2] = 1;
    }
    if (modCell != null)
      cr.addJmolScript("unitcell {%" + modCell + "}");
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
      htModulation = new Hashtable<String, double[]>();
    }
  }

  /**
   * Types include O (occupation) D (displacement) U (anisotropy) _coefs_
   * indicates this is a wave description
   * 
   * 
   * @param map
   * @param id
   * @param pt
   * @param iModel
   */
  @Override
  public void addModulation(Map<String, double[]> map, String id, double[] pt,
                            int iModel) {
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
    boolean isOK = false;
    for (int i = pt.length; --i >= 0;) {
      if (modSelected > 0 && i + 1 != modSelected && id.contains("_coefs_")) {
        pt[i] = 0;
      } else if (pt[i] != 0) {
        isOK = true;
        break;
      }
    }
    if (!isOK)
      return;
    if (map == null)
      map = htModulation;
    if (id.indexOf("@") < 0)
      id += "@" + (iModel >= 0 ? iModel : cr.asc.iSet);
    Logger.info("Adding " + id + " " + Escape.e(pt));
    map.put(id, pt);
  }

  /**
   * Both the Jana reader and the CIF reader will call this to set the
   * modulation for a given model.
   * @throws Exception 
   * 
   */
  @Override
  public void setModulation(boolean isPost) throws Exception {
    if (modDim == 0 || htModulation == null)
      return;
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = true;
    cr.asc.setInfo("someModelsAreModulated", Boolean.TRUE);
    setModulationForStructure(cr.asc.iSet, isPost);
    if (modDebug)
      Logger.debugging = Logger.debuggingHigh = false;
  }

  /**
   * Create a script that will run to turn modulation on and to display only
   * atoms with modulated occupancy >= 0.5.
   * 
   */
  @Override
  public void finalizeModulation() {
    if (!finalized && modDim > 0 && !modVib) {
      cr.asc.setInfo("modulationOn", Boolean.TRUE);
      cr.addJmolScript((haveOccupancy && !isCommensurate ? ";display occupancy >= 0.5"
          : ""));
    }
    finalized = true;
  }

  private String atModel = "@0";

  private Matrix[] modMatrices;

  private double[] qlist100;

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
    return (pt < 0 || key.indexOf("_pos#") >= 0 || key.indexOf("*;*") >= 0 || checkQ
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
  public double[] getMod(String key) {
    return htModulation.get(key + atModel);
  }

  @Override
  public Map<String, double[]> getModulationMap() {
    return htModulation;
  }

  /**
   * Called when structure creation is complete and all modulation data has been
   * collected.
   * 
   * @param iModel
   * @param isPost
   * @throws Exception 
   */
  private void setModulationForStructure(int iModel, boolean isPost) throws Exception {
    atModel = "@" + iModel;

    if (htModulation.containsKey("X_" + atModel))
      return;

    if (!isPost) {
      initModForStructure(iModel);
      return;
    }

    // check to see we have not already done this.

    htModulation.put("X_" + atModel, new double[0]);

    if (!haveAtomMods)
      return;

    // here we go -- apply all atom modulations. 

    int n = cr.asc.ac;
    atoms = cr.asc.atoms;
    cr.symmetry = cr.asc.getSymmetry();
    if (cr.symmetry != null)
      nOps = cr.symmetry.getSpaceGroupOperationCount();
    iopLast = -1;
    SB sb = new SB();
    for (int i = cr.asc.getLastAtomSetAtomIndex(); i < n; i++)
      modulateAtom(atoms[i], sb);
    cr.asc.setAtomSetAtomProperty("modt", sb.toString(), -1);
    cr.appendLoadNote(modCount + " modulations for " + ac + " atoms");
    htAtomMods = null;
    if (minXYZ0 != null)
      trimAtomSet();
    htSubsystems = null;
  }

  private void initModForStructure(int iModel) throws Exception {
    String key;

    // we allow for up to three wave vectors in the form of a matrix
    // along with their lengths as an array.

    sigma = new Matrix(null, modDim, 3);
    qs = null;

    modMatrices = new Matrix[] { sigma, null };

    // we should have W_1, W_2, W_3 up to the modulation dimension

    for (int i = 0; i < modDim; i++) {
      double[] pt = getMod("W_" + (i + 1));
      if (pt == null) {
        Logger.info("Not enough cell wave vectors for d=" + modDim);
        return;
      }
      cr.appendLoadNote("W_" + (i + 1) + " = " + Escape.e(pt));

      sigma.getArray()[i] = new double[] { pt[0], pt[1], pt[2] };
    }
    q1 = sigma.getArray()[0];

    // q1Norm is used specifically for occupancy modulation, where dim = 1 only

    q1Norm = P3
        .new3(q1[0] == 0 ? 0 : 1, q1[1] == 0 ? 0 : 1, q1[2] == 0 ? 0 : 1);
    double[] pt;

    // Take care of loose ends.
    // O: occupation   (set haveOccupancy; set a cos(theta) + b sin(theta) format)
    // D: displacement (set a cos(theta) + b sin(theta) format)
    // U: anisotropy   (no issues)
    // W: primary wave vector (see F if dim > 1)
    // F: Jana-type wave vector, referencing W vectors (set pt to coefficients, including harmonics)

    Map<String, double[]> map = new Hashtable<String, double[]>();
    for (Entry<String, double[]> e : htModulation.entrySet()) {
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
        if (pt[2] == 1 && key.charAt(2) != 'S') {
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
            double a = pt[0];
            double d = 2 * Math.PI * pt[1];
            pt[0] = (float) (a * Math.cos(d));
            pt[1] = (float) (a * Math.sin(-d));
            pt[2] = 0;
            Logger.info("msCIF setting " + key + " " + Escape.e(pt));
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
        if (key.indexOf("_coefs_") >= 0) {
          // d > 1 -- already set from coefficients
          cr.appendLoadNote("Wave vector " + key + "=" + Escape.eAD(pt));
        } else {
          double[] ptHarmonic = calculateQCoefs(pt);
          if (ptHarmonic == null) {
            cr.appendLoadNote("Cannot match atom wave vector " + key + " "
                + Escape.eAD(pt) + " to a cell wave vector or its harmonic");
          } else {
            String k2 = key + "_coefs_";
            if (!htModulation.containsKey(k2 + atModel)) {
              addModulation(map, k2, ptHarmonic, iModel);
              if (key.startsWith("F_"))
                cr.appendLoadNote("atom wave vector " + key + " = "
                    + Escape.e(pt) + " fn = " + Escape.e(ptHarmonic));
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

    if (htSubsystems == null) {
      haveAtomMods = false;
    } else {
      haveAtomMods = true;
      htAtomMods = new Hashtable<String, Lst<Modulation>>();
    }
    for (Entry<String, double[]> e : htModulation.entrySet()) {
      if ((key = checkKey(e.getKey(), true)) == null)
        continue;
      double[] params = e.getValue();
      String atomName = key.substring(key.indexOf(";") + 1);
      int pt_ = atomName.indexOf("#=");
      if (pt_ >= 0) {
        params = getMod(atomName.substring(pt_ + 2));
        atomName = atomName.substring(0, pt_);
      }
      if (Logger.debuggingHigh)
        Logger.debug("SetModulation: " + key + " " + Escape.e(params));
      char type = key.charAt(0);
      pt_ = key.indexOf("#") + 1;
      String utens = null;
      switch (type) {
      case 'U':
        utens = key.substring(4, key.indexOf(";"));
        //$FALL-THROUGH$
      case 'O':
      case 'D':
        char axis = key.charAt(pt_);
        type = getModType(key);
        if (htAtomMods == null)
          htAtomMods = new Hashtable<String, Lst<Modulation>>();
        double[] p = new double[params.length];
        for (int i = p.length; --i >= 0;)
          p[i] = params[i];
        double[] qcoefs = getQCoefs(key);
        if (qcoefs == null)
            throw new Exception("Missing cell wave vector for atom wave vector for " + key + " " + Escape.e(params));
        addAtomModulation(atomName, axis, type, p, utens, qcoefs);
        haveAtomMods = true;
        break;
      }
    }
  }

  @Override
  public double[] getQCoefs(String key) {
    int fn = Math.max(0, cr.parseIntStr(key.substring(2)));        
    if (fn == 0) {
      if (qlist100 == null) {
        qlist100 = new double[modDim];
        qlist100[0] = 1;
      }
      return qlist100;
    }     
    return getMod("F_" + fn + "_coefs_");
  }

  @Override
  public char getModType(String key) {
    char type = key.charAt(0);
    char id = key.charAt(2);
    return  (id == 'S' ? Modulation.TYPE_DISP_SAWTOOTH
        : id == '0' ? Modulation.TYPE_OCC_CRENEL
            : type == 'O' ? Modulation.TYPE_OCC_FOURIER
                : type == 'U' ? Modulation.TYPE_U_FOURIER
                    : Modulation.TYPE_DISP_FOURIER);
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
  private double[] calculateQCoefs(double[] p) {
    if (qs == null) {
      qs = new P3[modDim];
      for (int i = 0; i < modDim; i++) {
        qs[i] = toP3(getMod("W_" + (i + 1)));
      }
    }
    P3 pt = toP3(p);
    // test n * q
    for (int i = 0; i < modDim; i++)
      if (qs[i] != null) {
        int ifn = approxInt(pt.dot(qs[i]) / qs[i].dot(qs[i]));
        if (ifn != 0) {
          p = new double[modDim];
          p[i] = ifn;
          return p;
        }
      }
    P3 p3 = toP3(p);
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
          if (modDim > 1 && qs[1] != null)
            pt.scaleAdd2(j, qs[1], pt);
          if (modDim > 2 && qs[2] != null)
            pt.scaleAdd2(k, qs[2], pt);
          if (pt.distanceSquared(p3) < 0.0001f) {
            p = new double[modDim];
            switch (modDim) {
            default:
              p[2] = k;
              //$FALL-THROUGH$
            case 2:
              p[1] = j;
              //$FALL-THROUGH$
            case 1:
              p[0] = i;
              break;
            }
            return p;
          }
        }

    // test dropped rational component
    // eg: 
    //     q = 0 0.33333 0.166666 
    // and f = 0 0.33333 0.666666 

    pt = toP3(p);
    for (int i = 0; i < modDim; i++)
      if (qs[i] != null) {
        p3 = qs[i];
        int ifn = 0;
        if (pt.x != 0)
          ifn = approxInt(pt.x / p3.x);
        if (pt.y != 0)
          ifn = Math.max(approxInt(pt.y / p3.y), ifn);
        if (ifn == 0 && pt.z != 0)
          ifn = Math.max(approxInt(pt.z / p3.z), ifn);
        if (ifn == 0)
          continue;
        if (p3.x != 0 && approxInt(10 + p3.x * ifn - pt.x) == 0 || p3.y != 0
            && approxInt(10 + p3.y * ifn - pt.y) == 0 || p3.z != 0
            && approxInt(10 + p3.z * ifn - pt.z) == 0)
          continue;

        p = new double[modDim];
        p[i] = ifn;
        return p;
      }
    return null;
  }

  private int approxInt(float fn) {
    int ifn = Math.round(fn);
    return (Math.abs(fn - ifn) < 0.001f ? ifn : 0);
  }

  private P3 toP3(double[] x) {
    return P3.new3((float) x[0], (float) x[1], (float) x[2]);
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
  private void addAtomModulation(String atomName, char axis, char type,
                                 double[] params, String utens, double[] qcoefs) {
    Lst<Modulation> list = htAtomMods.get(atomName);
    if (list == null) {
      ac++;
      htAtomMods.put(atomName, list = new Lst<Modulation>());
    }
    list.addLast(new Modulation(axis, type, params, utens, qcoefs));
    modCount++;
  }

  @Override
  public void addSubsystem(String code, Matrix w) {
    if (code == null)
      return;
    Subsystem ss = new Subsystem(this, code, w);
    cr.appendLoadNote("subsystem " + code + "\n" + w);
    setSubsystem(code, ss);
  }

  //  private void setSubsystems() {
  //    atoms = cr.asc.atoms;
  //    int n = cr.asc.ac;
  //    for (int i = cr.asc.getLastAtomSetAtomIndex(); i < n; i++) 
  //      getUnitCell(atoms[i]);
  //  }

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
  private void modulateAtom(Atom a, SB sb) {

    // Modulation is based on an atom's first symmetry operation.
    // (Special positions should generate the same atom regardless of which operation is employed.)

    if (modCoord && htSubsystems != null) {
      // I think this does nothing.....
      P3 ptc = P3.newP(a);
      SymmetryInterface spt = getSymmetry(a);
      spt.toCartesian(ptc, true);
    }
    
    Lst<Modulation> list = htAtomMods.get(a.atomName);
    if (list == null && a.altLoc != '\0' && htSubsystems != null) {
      // force treatment if a subsystem
      list = new Lst<Modulation>();
    }
    if (list == null || cr.symmetry == null || a.bsSymmetry == null)
      return;

    int iop = Math.max(a.bsSymmetry.nextSetBit(0), 0);
    if (modLast)
      iop = Math.max((a.bsSymmetry.length() - 1) % nOps, iop);
    if (Logger.debuggingHigh)
      Logger.debug("\nsetModulation: i=" + a.index + " " + a.atomName + " xyz="
          + a + " occ=" + a.foccupancy);
    if (iop != iopLast) {
      // for each new operator, we need to generate new matrices.
      // gammaE is the pure rotation part of the operation;
      // gammaIS is a full rotation/translation matrix in fractional coordinates.
      // nOps is used as a factor in occupation modulation only.
      iopLast = iop;
      gammaE = new M3();
      getSymmetry(a).getSpaceGroupOperation(iop).getRotationScale(gammaE);
    }
    if (Logger.debugging) {
      Logger.debug("setModulation iop = " + iop + " "
          + cr.symmetry.getSpaceGroupXyz(iop, false) + " " + a.bsSymmetry);
    }

    // The magic happens here.

    ModulationSet ms = new ModulationSet().set(a.index + " " + a.atomName,
        a, modDim, list, gammaE, getMatrices(a), iop, getSymmetry(a));
    ms.calculate(null, false);

    // ms parameter values are used to set occupancies, 
    // vibrations, and anisotropy tensors.

    if (!Float.isNaN(ms.vOcc)) {
      double[] pt = getMod("J_O#0;" + a.atomName);
      float occ0 = ms.vOcc0;
      double occ;
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
        double site_mult = a.vib.x;
        double o_site = a.foccupancy * site_mult / nOps / pt[1];
        occ = o_site * (pt[1] + ms.vOcc);
      } else {
        // m40 Fourier
        // occ_site * (occ_0 + SUM)
        occ = pt[0] * (pt[1] + ms.vOcc);
      }
      // 49/50 is an important range for cutoffs -- we let this range be void
      a.foccupancy = (occ > 0.49 && occ < 0.50 ? 0.489f : (float) Math.min(1, Math.max(0, occ)));
      Logger.info("atom " + a.atomName + " occupancy = " + a.foccupancy);
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
      SymmetryInterface symmetry = getAtomSymmetry(a, cr.symmetry);
      Tensor t = cr.asc.getXSymmetry().addRotatedTensor(a,
          symmetry.getTensor(a.anisoBorU), iop, false, symmetry);
      t.isModulated = true;
      if (Logger.debuggingHigh) {
        Logger.debug("setModulation Uij(final)=" + Escape.eAF(a.anisoBorU)
            + "\n");
        Logger.debug("setModulation tensor="
            + ((Tensor) a.tensors.get(0)).getInfo("all"));
      }
    }
    if (Float.isNaN(ms.x))
      ms.set(0, 0, 0);
    a.vib = ms;
    // set property_modT to be Math.floor (q.r/|q|) -- really only for d=1

    if (modVib || a.foccupancy != 0) {
      float t = q1Norm.dot(a);
      if (Math.abs(t - (int) t) > 0.001f)
        t = (int) Math.floor(t);
      sb.append(((int) t) + "\n");
    }
  }

  /**
   * When applying symmetry, this method allows us to use a set of symmetry
   * operators unique to this particular atom -- or in this case, to its
   * subsystem.
   * 
   */
  @Override
  public SymmetryInterface getAtomSymmetry(Atom a,
                                           SymmetryInterface defaultSymmetry) {
    Subsystem ss;
    return (htSubsystems == null || (ss = getSubsystem(a)) == null ? defaultSymmetry
        : ss.getSymmetry());
  }

  Map<String, Subsystem> htSubsystems;

  private void setSubsystem(String code, Subsystem system) {
    if (htSubsystems == null)
      htSubsystems = new Hashtable<String, Subsystem>();
    htSubsystems.put(code, system);
    setSubsystemOptions();
  }

  private Matrix[] getMatrices(Atom a) {
    Subsystem ss = getSubsystem(a);
    return (ss == null ? modMatrices : ss.getModMatrices());
  }

  private SymmetryInterface getSymmetry(Atom a) {
    Subsystem ss = getSubsystem(a);
    return (ss == null ? cr.symmetry : ss.getSymmetry());
  }

  private Subsystem getSubsystem(Atom a) {
    return (htSubsystems == null ? null : htSubsystems.get("" + a.altLoc));
  }

  private P3 minXYZ0, maxXYZ0;

  @Override
  public void setMinMax0(P3 minXYZ, P3 maxXYZ) {
    if (htSubsystems == null)
      return;
    SymmetryInterface symmetry = getDefaultUnitCell();
    minXYZ0 = P3.newP(minXYZ);
    maxXYZ0 = P3.newP(maxXYZ);
    P3 pt0 = P3.newP(minXYZ);
    P3 pt1 = P3.newP(maxXYZ);
    P3 pt = new P3();
    symmetry.toCartesian(pt0, true);
    symmetry.toCartesian(pt1, true);
    P3[] pts = BoxInfo.unitCubePoints;
    for (Entry<String, Subsystem> e : htSubsystems.entrySet()) {
      SymmetryInterface sym = e.getValue().getSymmetry();
      for (int i = 8; --i >= 0;) {
        pt.x = (pts[i].x == 0 ? pt0.x : pt1.x);
        pt.y = (pts[i].y == 0 ? pt0.y : pt1.y);
        pt.z = (pts[i].z == 0 ? pt0.z : pt1.z);
        expandMinMax(pt, sym, minXYZ, maxXYZ);
      }
    }
    //System.out.println("msreader min max " + minXYZ + " " + maxXYZ);
  }

  private void expandMinMax(P3 pt, SymmetryInterface sym, P3 minXYZ, P3 maxXYZ) {
    P3 pt2 = P3.newP(pt);
    float slop = 0.0001f;
    sym.toFractional(pt2, false);
    if (minXYZ.x > pt2.x + slop)
      minXYZ.x = (int) Math.floor(pt2.x) - 1;
    if (minXYZ.y > pt2.y + slop)
      minXYZ.y = (int) Math.floor(pt2.y) - 1;
    if (minXYZ.z > pt2.z + slop)
      minXYZ.z = (int) Math.floor(pt2.z) - 1;
    if (maxXYZ.x < pt2.x - slop)
      maxXYZ.x = (int) Math.ceil(pt2.x) + 1;
    if (maxXYZ.y < pt2.y - slop)
      maxXYZ.y = (int) Math.ceil(pt2.y) + 1;
    if (maxXYZ.z < pt2.z - slop)
      maxXYZ.z = (int) Math.ceil(pt2.z) + 1;
  }

  private void trimAtomSet() {
    if (!cr.doApplySymmetry)
      return;
    AtomSetCollection asc = cr.asc;
    BS bs = asc.bsAtoms;
    SymmetryInterface sym = getDefaultUnitCell();
    Atom[] atoms = asc.atoms;
    P3 pt = new P3();
    if (bs == null)
      bs = asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      pt.setT(a);
      pt.add(a.vib);
      getSymmetry(a).toCartesian(pt, false);
      sym.toFractional(pt, false);
      if (!asc.xtalSymmetry.isWithinCell(3, pt, minXYZ0.x, maxXYZ0.x,
          minXYZ0.y, maxXYZ0.y, minXYZ0.z, maxXYZ0.z, 0.001f)
          || isCommensurate
          && a.foccupancy < 0.5f)
        bs.clear(i);
    }
  }

  private SymmetryInterface getDefaultUnitCell() {
    return (modCell != null && htSubsystems.containsKey(modCell) ? htSubsystems
        .get(modCell).getSymmetry() : cr.asc.getSymmetry());
  }

  @Override
  public SymmetryInterface getSymmetryFromCode(String code) {
    return htSubsystems.get(code).getSymmetry();
  }

  @Override
  public boolean addLatticeVector(Lst<float[]> lattvecs, String data)
      throws Exception {
    float[] a = null;
    char c = data.charAt(0);
    switch (c) {
    case 'P':
    case 'X':
      break;
    case 'A':
    case 'B':
    case 'C':
    case 'I':
      a = new float[] { 0.5f, 0.5f, 0.5f };
      if (c != 'I')
        a[c - 'A'] = 0;
      break;
    case 'F':
      addLatticeVector(lattvecs, "A");
      addLatticeVector(lattvecs, "B");
      addLatticeVector(lattvecs, "C");
      break;
    case '0': // X explicit
      if (data.indexOf(".") >= 0)
        a = AtomSetCollectionReader.getTokensFloat(data, null, modDim + 3);
      break;
    default:
      return false;
    }
    if (a != null)
      lattvecs.addLast(a);
    return true;
  }

}
