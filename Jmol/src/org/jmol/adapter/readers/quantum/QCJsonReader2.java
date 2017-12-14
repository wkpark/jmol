package org.jmol.adapter.readers.quantum;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.writers.QCSchema;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

/**
 * A molecular structure and orbital reader for MolDen files.
 * See http://www.cmbi.ru.nl/molden/molden_format.html
 * 
 * updated by Bob Hanson <hansonr@stolaf.edu> for Jmol 12.0/12.1
 * 
 * adding [spacegroup] [operators] [cell] [cellaxes] for Jmol 14.3.7 
 * 
 * @author Matthew Zwier <mczwier@gmail.com>
 */

public class QCJsonReader2 extends MoldenReader {
  
  private Map<String, Object> job;

  private int jobCount;

  private int modelCount;
  
  @SuppressWarnings("unchecked")
  @Override
  protected void initializeReader() {
    super.initializeReader();
    SB sb  = new SB();
    try {
      while (rd() != null)
        sb.append(line);
      Lst<Object> json = vwr.parseJSONArray(sb.toString());
      // first record is version tag
      Logger.info(json.get(0).toString());
      // second record is Jmol info; not used here
      jobCount = json.size() - 2;
      for (int i = 0; i < jobCount; i++)
        processJob((Map<String, Object>)json.get(i + 2));
    } catch (Exception e) {
      e.printStackTrace();
    }
    continuing = false;
  }

  /**
   * @param job 
   * @throws Exception 
   */
  private void processJob(Map<String, Object> job) throws Exception {
    this.job = job;
    readSteps();
    /*
    if (loadVibrations)
      readFreqsAndModes();
    if (loadGeometries)
      readGeometryOptimization();
    checkSymmetry();
    if (asc.atomSetCount == 1 && moData != null)
      finalizeMOData(moData);
      */
 }

  @Override
  public void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
  }
  
  private void readSteps() throws Exception {
    Lst<Object> steps = getList(job, "steps");
    int nSteps = steps.size();
    for (int iStep = 0; iStep < nSteps; iStep++) {
      if (!doGetModel(++modelCount, null)) {
        if (!checkLastModel())
          return;
        continue;
      }
      asc.newAtomSet();
      @SuppressWarnings("unchecked")
      Map<String, Object> step = (Map<String, Object>) steps.get(iStep);
      Map<String, Object> topology = getMap(step, "topology");
      Map<String, Object> atoms = getMap(topology, "atoms");

      // one or the other of these is required:
      String[] symbols = getStringArray(atoms, "symbol");
      int[] atomNumbers = getIntArray(atoms, "atom_number");
      String[] atom_names = getStringArray(atoms, "atom_names");

      float[] coords = getFloatArray(atoms, "coords");
      modelAtomCount = coords.length / 3;
      float f = getConversionFactor(atoms, "coords", QCSchema.UNITS_ANGSTROMS);
      boolean isFractional = (f == 0);
      setFractionalCoordinates(isFractional);
      if (isFractional) {
        f = getConversionFactor(atoms, "unit_cell", QCSchema.UNITS_ANGSTROMS);
        float[] cell = getFloatArray(atoms, "unit_cell");
        // a b c alpha beta gamma 
        // m.m00, m.m10, m.m20, // Va
        // m.m01, m.m11, m.m21, // Vb
        // m.m02, m.m12, m.m22, // Vc
        // dimension, (float) volume,
        if (cell == null) {
          Logger.error("topology.unit_cell is missing even though atoms are listed as fractional");
        } else {
          for (int i = 0; i < 6; i++) {
            switch (i) {
            case 3:
              f = 1;
              //$FALL-THROUGH$
            default:
              setUnitCellItem(i, cell[i] * f);
              break;
            }
          }
        }
      }
      for (int i = 0, pt = 0; i < modelAtomCount; i++) {
        Atom atom = asc.addNewAtom();
        setAtomCoordXYZ(atom, coords[pt++] * f, coords[pt++] * f, coords[pt++]
            * f);
        String sym = (symbols == null ? JmolAdapter
            .getElementSymbol(atomNumbers[i]) : symbols[i]);
        atom.atomName = (atom_names == null ? sym : atom_names[i]);
        atom.elementNumber = (short) (atomNumbers == null ? JmolAdapter
            .getElementNumber(sym) : atomNumbers[i]);
      }
      if (doReadMolecularOrbitals) {
        readMolecularOrbitals(getMap(step, "molecular_orbitals"));
        clearOrbitals();
      }
      applySymmetryAndSetTrajectory();
      if (loadVibrations) {
        readFreqsAndModes(getList(step, "vibrations"));
      }
      
    }
  }
  
  private boolean readFreqsAndModes(Lst<Object> vibrations) throws Exception {
    //  "frequency":{"value":-0.00,"units":["cm^-1","?"]},
    //      "ir_intensity":{"value":0.000005,"units":["au",1]},
    //    "vectors":[

    if (vibrations != null) {
      int n = vibrations.size();
      for (int i = 0; i < n; i++) {
        @SuppressWarnings("unchecked")
        Map<String, Object> vib = (Map<String, Object>) vibrations.get(i);
        float freq = getFloat(vib, "frequency", QCSchema.UNITS_CM_1);
        float[] vectors = getFloatArray(vib, "vectors");
        if (i > 0)
          asc.cloneLastAtomSet();
        asc.setAtomSetFrequency(null, null, "" + freq, QCSchema.UNITS_CM_1);
        int i0 = asc.getLastAtomSetAtomIndex();
        for (int j = 0, pt = 0; j < modelAtomCount; j++) {
          asc.addVibrationVector(j + i0, vectors[pt++] * ANGSTROMS_PER_BOHR,
              vectors[pt++] * ANGSTROMS_PER_BOHR, vectors[pt++]
                  * ANGSTROMS_PER_BOHR);
        }
      }
    }
    return true;
  }
  
  @SuppressWarnings("unchecked")
  private float getFloat(Map<String, Object> mo, String key, String toUnits) {
    Object o = mo.get(key);
    float conv = 1;
    if (toUnits != null)
      if (o instanceof Map<?, ?>) {
        //  "frequency":{"value":-0.00,"units":["cm^-1",4.5563359e-6]},
        return convertTo((Map<String, Object>) o, toUnits);
      } else if (mo.containsKey(key + "_units")) {
        //  "frequency_units":["cm^-1",4.5563359e-6],
        //  "frequency":-0.00,
        conv = getConversionFactor(mo, key, toUnits);
      }
    Float f = (Float) o;
    return (f == null ? Float.NaN : f.floatValue() * conv);
  }

  private float convertTo(Map<String, Object> map, String toUnits) {
      return getFloat(map, "value", null) * getConversionFactor(map, "units", toUnits);
  }

  private float getConversionFactor(Map<String, Object> map, String key, String toUnits) {
    Lst<Object> list = getList(map, key + "_units");
    String units = (list == null ? null : list.get(0).toString());
    float f = QCSchema.getConversionFactorTo(list, toUnits);
    if (Float.isNaN(f)) {
      System.out.println("units for " + units + "? " + units);
      f = 1;
    }
    return f;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getMap(Object obj, String key) {
    return (obj == null ? null : (Map<String, Object>) ((Map<String, Object>) obj).get(key));
  }

  /**
   * Retrieve an array of any sort as a list of objects, possibly unpacking it if it is run-length encoded. 
   * 
   * @param mapOrList
   * @param key
   * @return unpacked array
   */
  private Lst<Object> getList(Object mapOrList, String key) {
    @SuppressWarnings("unchecked")
    Lst<Object> list = (Lst<Object>) (key == null ? mapOrList : ((Map<String, Object>)mapOrList).get(key));
    if (list == null)
      return null;
    int n = list.size();
    if (n == 0 || !"_RLE_".equals(list.get(0)))
      return list;
    Lst<Object> list1 = new Lst<Object>();
    for (int i = 1; i < n; i++) {
      int count = ((Number) list.get(i)).intValue();
      Object value = list.get(++i);
      for (int j = 0; j < count; j++)
        list1.addLast(value);
    }
    return list1;
  }

  /**
   * Retrieve a float array, possibly unpacking it if it is run-length encoded.
   * Read any error as Float.NaN. 
   * 
   * @param mapOrList
   * @param key  into mapOrList, or null if mapOrList is a list
   * @return unpacked float[]
   */
  private float[] getFloatArray(Object mapOrList, String key) {
    Lst<Object> list = getList(mapOrList, key);
    if (list == null)
      return null;
    float[] a = new float[list.size()];
    for (int i = a.length; --i >= 0;) {
      try {
        a[i] = ((Number) list.get(i)).floatValue();
      } catch (Exception e) {
        a[i] = Float.NaN;
      }
    }
    return a;
  }

  /**
   * Retrieve an int array, possibly unpacking it if it is run-length encoded.
   * Errors are not trapped. 
   * 
   * @param mapOrList
   * @param key null if mapOrList is a list
   * @return unpacked int[]
   */
  private int[] getIntArray(Object mapOrList, String key) {
    Lst<Object> list = getList(mapOrList, key);
    if (list == null)
      return null;
    int[] a = new int[list.size()];
    for (int i = a.length; --i >= 0;)
      a[i] = ((Number) list.get(i)).intValue();
    return a;
  }

  /**
   * Retrieve a String array, possibly unpacking it if it is run-length encoded.
   * Any "null" string is read as null. 
   * 
   * @param mapOrList
   * @param key into mapOrlist if it is a map, or null if mapOrList is a list
   * @return unpacked String[]
   */
  private String[] getStringArray(Object mapOrList, String key) {
    Lst<Object> list = getList(mapOrList, key);
    if (list == null)
      return null;
    String[] a = new String[list.size()];
    for (int i = a.length; --i >= 0;) {
      a[i] = list.get(i).toString();
      if (a[i].equals("null"))
        a[i] = null;
    }
    return a;
  }

  
 private boolean haveEnergy = true;
  
  /**
   * Read basis and orbital information.
   * 
   * @param moInfo
   * @return true if successful
   * 
   * @throws Exception
   */
  private boolean readMolecularOrbitals(Map<String, Object> moInfo) throws Exception {
    if (moInfo == null)
      return false;
    String moBasisID = moInfo.get("basis_id").toString();//:"MOBASIS_1"
    if (!readBasis(moBasisID))
      return false;

    Lst<Object> mos = getList(moInfo, "orbitals");
    int n = mos.size();
    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      Map<String, Object> thisMO = (Map<String, Object>) mos.get(i); 
      float energy = getFloat(thisMO, "energy", "ev");
      float occupancy = getFloat(thisMO, "occupancy", null);
      String symmetry = (String) thisMO.get("symmetry");
      String spin = (String) thisMO.get("type");
      if (spin != null) {
        if (spin.indexOf("beta") >= 0)
          alphaBeta = "beta";
        else if (spin.indexOf("alpha") >= 0)
          alphaBeta = "alpha";
      }
      calculationType = (String) thisMO.get("jmol_calculation_type");
      if (calculationType == null)
        calculationType = "?";
      float[] coefs = getFloatArray(thisMO, "coefficients");
      line = "" + symmetry;
      if (filterMO()) {
        Map<String, Object> mo = new Hashtable<String, Object>();
        mo.put("coefficients", coefs);
        if (Float.isNaN(energy)) {
          haveEnergy = false;
        } else {
          mo.put("energy", Float.valueOf(energy));
        }
        if (!Float.isNaN(occupancy))
          mo.put("occupancy", Float.valueOf(occupancy));
        if (symmetry != null)
          mo.put("symmetry", symmetry);
        if (alphaBeta.length() > 0)
          mo.put("type", alphaBeta);
        setMO(mo);
        if (debugging) {
          Logger.debug(coefs.length + " coefficients in MO " + orbitals.size());
        }
      }
    }
    if (debugging)
      Logger.debug("read " + orbitals.size() + " MOs");
    Lst<Object> units = getList(moInfo, "orbitals_energy_units");
    String sunits = (units == null ? null : units.get(0).toString());
    setMOs(sunits == null || sunits.equals("?") ? "?" : sunits);
    if (haveEnergy && doSort)
      sortMOs();
    return false;
  }
  
  String lastBasisID = null;
  private boolean readBasis(String moBasisID) throws Exception {
    Map<String, Object> moBasisData = getMap(job, "mo_bases");
    Map<String, Object> moBasis = getMap(moBasisData, moBasisID);
    if (moBasis == null) {
      Logger.error("No job.mo_bases entry for " + moBasisID);
      return false;
    }
    if (moBasisID == lastBasisID)
      return true;
    lastBasisID = moBasisID;
    Lst<Object> listG = getList(moBasis, "gaussians");
    Lst<Object> listS = getList(moBasis, "shells");
    if (listG == null && listS == null) {
      listG = listS = getList(moBasis, "slaters");
    }
    if ((listG == null) != (listS == null)) {
      Logger.error("gaussians/shells or slaters missing");
      return false;
    }
    if (listG == listS) {
      readSlaterBasis(listS);
    } else {
      readGaussianBasis(listG, listS);
    }
    return true;
  }

  boolean readSlaterBasis(Lst<Object> listS) throws Exception {
    /*
    1    0    0    0    1             1.5521451600        0.9776767193          
    1    1    0    0    0             1.5521451600        1.6933857512          
    1    0    1    0    0             1.5521451600        1.6933857512          
    1    0    0    1    0             1.5521451600        1.6933857512          
    2    0    0    0    0             1.4738648100        1.0095121222          
    3    0    0    0    0             1.4738648100        1.0095121222          
     */
    
    nCoef = listS.size();
    for (int i = 0; i < nCoef; i++) {
      float[] a = getFloatArray(listS.get(i), null);
      addSlater((int) a[0], (int) a[1], (int) a[2], (int) a[3], (int) a[4], a[5], a[6]);
    }
    setSlaters(false, false);
    return true;
  }

  private boolean readGaussianBasis(Lst<Object> listG, Lst<Object> listS) throws Exception {
    shells = new Lst<int[]>();
    for (int i = 0; i < listS.size(); i++)
      shells.addLast(getIntArray(listS.get(i), null));
    int gaussianPtr = listG.size();
    float[][] garray = AU.newFloat2(gaussianPtr);
    for (int i = 0; i < gaussianPtr; i++)
      garray[i] = getFloatArray(listG.get(i), null); // [exp, coef], [exp, coef]...
    moData.put("shells", shells);
    moData.put("gaussians", garray);
    Logger.info(shells.size() + " slater shells read");
    Logger.info(garray.length + " gaussian primitives read");
    //Logger.info(nCoef + " MO coefficients expected for orbital type " + orbitalType);
    asc.setCurrentModelInfo("moData", moData);
    return false;
  }
 
  @SuppressWarnings("unchecked")
  private void sortMOs() {
    Object[] list = orbitals.toArray(new Object[orbitals.size()]);
    Arrays.sort(list, new MOEnergySorter());
    orbitals.clear();
    for (int i = 0; i < list.length; i++)
      orbitals.addLast((Map<String, Object>)list[i]);
  }

  /////////////////// from Molden reader -- TODO /////////////////
  
//  private boolean checkSymmetry() throws Exception {
//    // extension for symmetry
//    if (line.startsWith("[SPACEGROUP]")) {
//      setSpaceGroupName(rd());
//      rd();
//      return true;
//    }
//    if (line.startsWith("[OPERATORS]")) {
//      while (rd() != null && line.indexOf("[") < 0)
//        if (line.length() > 0) {
//          Logger.info("adding operator " + line);
//          setSymmetryOperator(line);
//        }
//      return true;
//    }
//    if (line.startsWith("[CELL]")) {
//      rd();
//      Logger.info("setting cell dimensions " + line);
//      // ANGS assumed here
//      next[0] = 0;
//      for (int i = 0; i < 6; i++)
//        setUnitCellItem(i, parseFloat());
//      rd();
//      return true;
//    }
//    if (line.startsWith("[CELLAXES]")) {
//      float[] f = new float[9];
//      fillFloatArray(null, 0, f);
//      addExplicitLatticeVector(0, f, 0);
//      addExplicitLatticeVector(1, f, 3);
//      addExplicitLatticeVector(2, f, 6);
//      return true;
//    }
//    return false;
//  }
//
//  /*
//[GEOCONV]
//energy
//-.75960756002000E+02
//-.75961091052100E+02
//-.75961320555300E+02
//-.75961337317300E+02
//-.75961338487700E+02
//-.75961338493500E+02
//max-force
//0.15499000000000E-01
//0.11197000000000E-01
//0.50420000000000E-02
//0.15350000000000E-02
//0.42000000000000E-04
//0.60000000000000E-05
//[GEOMETRIES] XYZ
//     3
//
// o  0.00000000000000E+00 0.00000000000000E+00 -.36565628831562E+00
// h  -.77567072215814E+00 0.00000000000000E+00 0.18282805096053E+00
// h  0.77567072215814E+00 0.00000000000000E+00 0.18282805096053E+00
//
//  */
//  private boolean readGeometryOptimization() throws Exception {
//    Lst<String> energies = new  Lst<String>();
//    rd(); // energy
//    while (rd() != null 
//        && line.indexOf("force") < 0)
//      energies.addLast("" + PT.dVal(line.trim()));
//    skipTo("[GEOMETRIES] XYZ");
//    int nGeom = energies.size();
//    int firstModel = (optOnly || desiredModelNumber >= 0 ? 0 : 1);
//    modelNumber = firstModel; // input model counts as model 1; vibrations do not count
//    boolean haveModel = false;
//    if (desiredModelNumber == 0 || desiredModelNumber == nGeom)
//      desiredModelNumber = nGeom; 
//    else if (asc.atomSetCount > 0)
//      finalizeMOData(moData);
//    for (int i = 0; i < nGeom; i++) {
//      readLines(2);
//      if (doGetModel(++modelNumber, null)) {
//        readAtomSet("Step " + (modelNumber - firstModel) + "/" + nGeom + ": " + energies.get(i), false, 
//            !optOnly || haveModel);
//        haveModel = true;
//      } else {
//        readLines(modelAtomCount);
//      }
//    }
//    return true;
//  }
//
//  private void skipTo(String key) throws Exception {
//    key = key.toUpperCase();
//    if (line == null || !line.toUpperCase().contains(key))
////      discardLinesUntilContains(key);
//      while (rd() != null && line.toUpperCase().indexOf(key) < 0) {
//      }
//    
//  }
//
//  private void readAtomSet(String atomSetName, boolean isBohr, boolean asClone) throws Exception {
//    if (asClone && desiredModelNumber < 0)
//      asc.cloneFirstAtomSet(0);
//    float f = (isBohr ? ANGSTROMS_PER_BOHR : 1);
//    asc.setAtomSetName(atomSetName);
//    if (asc.ac == 0) {
//      while (rd() != null && line.indexOf('[') < 0) {    
//        String [] tokens = getTokens();
//        if (tokens.length == 4)
//          setAtomCoordScaled(null, tokens, 1, f).atomName = tokens[0];
//      }    
//      modelAtomCount = asc.getLastAtomSetAtomCount();
//      return;
//    }
//    Atom[] atoms = asc.atoms;
//    int i0 = asc.getLastAtomSetAtomIndex();
//    for (int i = 0; i < modelAtomCount; i++)
//      setAtomCoordScaled(atoms[i + i0], PT.getTokens(rd()), 1, f);
//  }
}
