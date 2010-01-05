/* $RCSfile: ADFReader.java,v $
 * $Author: egonw $
 * $Date: 2004/02/23 08:52:55 $
 * $Revision: 1.3.2.4 $
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;
//import org.jmol.util.Escape;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A reader for ADF output.
 * Amsterdam Density Functional (ADF) is a quantum chemistry program
 * by Scientific Computing & Modelling NV (SCM)
 * (http://www.scm.com/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all ADF output. If you have problems,
 * please contact the author of this code, not the developers
 * of ADF.
 *
 *<p> Added note (Bob Hanson) -- 1/1/2010 -- 
 *    Trying to implement reading of orbitals; ran into the problem
 *    that the atomic Slater description uses Cartesian orbitals,
 *    but the MO refers to spherical orbitals. 
 *
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class AdfReader extends MopacDataReader {

  

  private Hashtable htSymmetries;
  private SlaterData[] slaters;
  private Vector vSymmetries;
  private String energy = null;
  private int nXX = 0;

  /**
   * Read the ADF output.
   *
   * @param reader  input stream
   */
  public void readAtomSetCollection(BufferedReader reader) {
    atomSetCollection = new AtomSetCollection("adf", this);
    this.reader = reader;
    boolean iHaveAtoms = false;
    modelNumber = 0;
    String symLine = null;
    try {
      while (readLine() != null) {
        if (line.indexOf("Irreducible Representations, including subspecies") >= 0) {
          readSymmetries();
          continue;
        }
        if (line.indexOf("S F O s  ***  (Symmetrized Fragment Orbitals)  ***") >= 0) {
          readSlaterBasis(); // Cartesians
          continue;
        }
        if (line.indexOf(" Coordinates (Cartesian, in Input Orientation)") >= 0
            || line.indexOf("G E O M E T R Y  ***  3D  Molecule  ***") >= 0) {
          if (!doGetModel(++modelNumber)) {
            if (isLastModel(modelNumber) && iHaveAtoms)
              break;
            iHaveAtoms = false;
            continue;
          }
          iHaveAtoms = true;
          readCoordinates();
          continue;
        }
        if (!iHaveAtoms)
          continue;
        if (line.indexOf("Energy:") >= 0) {
          String[] tokens = getTokens(line.substring(line.indexOf("Energy:")));
          energy = tokens[1];
          continue;
        }
        if (line.indexOf("Vibrations") >= 0) {
          readFrequencies();
          continue;
        }
        if (line.indexOf(" === ") >= 0) {
          symLine = line;
          continue;
        }
        if (line.indexOf(" ======  Eigenvectors (rows) in BAS representation") >= 0) {
          readMolecularOrbitals(getTokens(symLine)[1]);
          continue;
        }              
      }
    } catch (Exception e) {
      setError(e);
    }

  }

  /**
   * Reads a set of coordinates
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readCoordinates() throws Exception {

    /*
     * 
 Coordinates (Cartesian)
 =======================

   Atom                      bohr                                 angstrom                 Geometric Variables
                   X           Y           Z              X           Y           Z       (0:frozen, *:LT par.)
 --------------------------------------------------------------------------------------------------------------
   1 XX         .000000     .000000     .000000        .000000     .000000     .000000      0       0       0


OR


 ATOMS
 =====                            X Y Z                    CHARGE
                                (Angstrom)             Nucl     +Core       At.Mass
                       --------------------------    ----------------       -------
    1  Ni              0.0000    0.0000    0.0000     28.00     28.00       57.9353

     * 
     */
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetName("" + energy); // start with an empty name
    discardLinesUntilContains("----");
    nXX = 0;
    while (readLine() != null && !line.startsWith(" -----")) {
      String[] tokens = getTokens();
      if (tokens.length < 5)
        break;
      String symbol = tokens[1];
      if (JmolAdapter.getElementNumber(symbol) < 1) {
        nXX++;
        continue;
      }
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = symbol;
      atom.set(parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]));
      if (tokens.length > 8)
        atom.scale(ANGSTROMS_PER_BOHR);
    }
  }

  /*
   Vibrations and Normal Modes  ***  (cartesian coordinates, NOT mass-weighted)  ***
   ===========================
   
   The headers on the normal mode eigenvectors below give the Frequency in cm-1
   (a negative value means an imaginary frequency, no output for (almost-)zero frequencies)


   940.906                      1571.351                      1571.351
   ------------------------      ------------------------      ------------------------
   1.XX          .000    .000    .000          .000    .000    .000          .000    .000    .000
   2.N           .000    .000    .115          .008    .067    .000         -.067    .008    .000
   3.H           .104    .180   -.534          .323   -.037   -.231          .580   -.398    .098
   4.H          -.208    .000   -.534          .017   -.757    .030         -.140   -.092   -.249
   5.H           .104   -.180   -.534         -.453   -.131    .201          .485    .378    .151


   ====================================
   */
  /**
   * Reads a set of vibrations.
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readFrequencies() throws Exception {
    readLine();
    while (readLine() != null) {
      while (readLine() != null && line.indexOf(".") < 0
          && line.indexOf("====") < 0) {
      }
      if (line == null || line.indexOf(".") < 0)
        return;
      String[] frequencies = getTokens();
      readLine(); // -------- -------- --------
      int iAtom0 = atomSetCollection.getAtomCount();
      int atomCount = atomSetCollection.getLastAtomSetAtomCount();
      int frequencyCount = frequencies.length;
      boolean[] ignore = new boolean[frequencyCount];
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        if (ignore[i])
          continue;
        atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetName(frequencies[i] + " cm^-1");
        atomSetCollection.setAtomSetProperty("Frequency", frequencies[i]
            + " cm^-1");
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
            "Frequencies");
      }
      discardLines(nXX);
      fillFrequencyData(iAtom0, atomCount, ignore, true, 0, 0);
    }
  }
  
  private void readSymmetries() throws Exception {
    /*
 Irreducible Representations, including subspecies
 -------------------------------------------------
 A1
 A2
 B1
 B2
     */
    vSymmetries = new Vector();
    htSymmetries = new Hashtable();
    readLine();
    int index = 0;
    while (readLine() != null && line.length() > 1) {
      String sym = line.trim();
      SymmetryData sd = new SymmetryData(index++, sym);
      htSymmetries.put(sym, sd);
      vSymmetries.add(sd);
    }
  }

  class SymmetryData {
    int index;
    String sym;
    int nSFO;
    int nBF;
    float[][] coefs;
    Hashtable[] mos;
    int[] basisFunctions;
    public SymmetryData(int index, String sym) {
      this.index = index;
      this.sym = sym;
    }
    
  }
  
  private void readSlaterBasis() throws Exception {
    if (vSymmetries == null)
      return;
    int nBF = 0;
    for (int i = 0; i < vSymmetries.size(); i++) {
      SymmetryData sd = (SymmetryData) vSymmetries.get(i);
      discardLinesUntilContains("=== " + sd.sym + " ===");
      if (line == null)
        return;
    /*
                                      === A1 ===
 Nr. of SFOs :   20
 Cartesian basis functions that participate in this irrep (total number =    32) :
      1     2     3     4     5     8    11    14    20    15
     18    30    23    28    31    43    32    44    33    45
     36    48    34    46    42    54    39    51    37    40
     49    52
     */
      sd.nSFO = parseInt(readLine().substring(15)); 
      sd.nBF = parseInt(readLine().substring(75));
      String funcList = "";
      while (readLine() != null && line.length() > 1)
        funcList += line;
      String[] tokens = getTokens(funcList);
      if (tokens.length != sd.nBF)
        return;
      sd.basisFunctions = new int[tokens.length];
      for (int j = tokens.length; --j >= 0; ) {
        int n = parseInt(tokens[j]);
        if (n > nBF)
          nBF = n;
        sd.basisFunctions[j] = n - 1;
      }
    }
    slaters = new SlaterData[nBF];
        /*
     (power of) X  Y  Z  R     Alpha  on Atom
                ==========     =====     ==========

 N                                    1
                                  ---------------------------------------------------------------------------
    Core    0  0  0  0     6.380      1
            0  0  0  1     1.500      2
            0  0  0  1     2.500      3
            0  0  0  1     5.150      4
            1  0  0  0     1.000      5

 H                                    2    3
                                  ---------------------------------------------------------------------------
            0  0  0  0     0.690     31   43
            0  0  0  0     0.920     32   44
            0  0  0  0     1.580     33   45
            1  0  0  0     1.250     34   46

       */
    discardLinesUntilContains("(power of)");
    discardLines(2);
    while (readLine() != null && line.indexOf("Total") < 0) {
      String[] tokens = getTokens();
      int nAtoms = tokens.length - 1;
      int[] atomList = new int[nAtoms];
      for (int i = 1; i <= nAtoms; i++)
        atomList[i - 1] = parseInt(tokens[i]) - 1;
      readLine();
      while (readLine() != null && line.length() >= 10) {
        tokens = getTokens();
        boolean isCore = tokens[0].equals("Core");
        int pt = (isCore ? 1 : 0);
        int x = parseInt(tokens[pt++]);
        int y = parseInt(tokens[pt++]);
        int z = parseInt(tokens[pt++]);
        int r = parseInt(tokens[pt++]);
        float zeta = parseFloat(tokens[pt++]);
        for (int i = 0; i < nAtoms; i++) {
          int ptBF = parseInt(tokens[pt++]) - 1;
          slaters[ptBF] = new SlaterData(atomList[i], x, y, z, r, zeta, isCore);
        }
      }
    }
  }

  private class SlaterData {
    boolean isCore;
    int iAtom;
    int x;
    int y;
    int z;
    int r;
    float alpha;
    public int pt;
    public SlaterData(int iAtom, int x, int y, int z, int r, float alpha, boolean isCore) {
      this.iAtom = iAtom;
      this.x = x;
      this.y = y;
      this.z = z;
      this.r = r;
      this.alpha = alpha;
      this.isCore = isCore;      
    }
  }
  
  private void readMolecularOrbitals(String sym) throws Exception {
    /*
 ======  Eigenvectors (rows) in BAS representation

  column           1                   2                   3                   4
  row   
    1    2.97448635016195E-01  7.07156589388012E-01  6.86546190383583E-03 -1.61065890134540E-03
    2   -1.38294969376236E-01 -1.62913073678337E-02 -1.31464541737858E-01  5.35848303329039E-01
    3    3.86427624200707E-02  2.84046375688973E-02  3.66872765902448E-02 -2.21326610798233E-01
     */
    SymmetryData sd = (SymmetryData) htSymmetries.get(sym);
    if (sd == null)
      return;
    int ptSym = sd.index;
    boolean isLast = (ptSym == vSymmetries.size() - 1);
    int n = 0;
    int nBF = slaters.length;
    sd.coefs = new float[sd.nSFO][nBF];
    while (n < sd.nBF) {
      readLine();
      int nLine = getTokens(readLine()).length;
      readLine();
      sd.mos = new Hashtable[sd.nSFO];
      String[][] data = new String[sd.nSFO][];
      fillDataBlock(data);
      for (int j = 1; j < nLine; j++) {
        int pt = sd.basisFunctions[n++];
        for (int i = 0; i < sd.nSFO; i++)
          sd.coefs[i][pt] = parseFloat(data[i][j]);
      }
    }
    for (int i = 0; i < sd.nSFO; i++) {
      Hashtable mo = new Hashtable();
      mo.put("coefficients", sd.coefs[i]);
      //System.out.println(i + " " + Escape.escapeArray(sd.coefs[i]));
      mo.put("id", sym + " " + (i + 1));
      sd.mos[i] = mo;
    }
    if (!isLast)
      return;
   /*
 Orbital Energies, all Irreps
 ========================================

 Irrep        no.  (spin)   Occup              E (au)                E (eV)
 ---------------------------------------------------------------------------
 A1            1             2.00       -0.18782651837132E+02      -511.1020
 A1            2             2.00       -0.93500325051330E+00       -25.4427
    */
    discardLinesUntilContains("Orbital Energies, all Irreps");
    discardLines(4);
    while (readLine() != null && line.length() > 10) {
      String[] tokens = getTokens();
      sd = (SymmetryData) htSymmetries.get(tokens[0]);
      int moPt = parseInt(tokens[1]) - 1;
      Hashtable mo = sd.mos[moPt];
      mo.put("occupancy", new Float(parseFloat(tokens[2])));
      mo.put("energy", new Float(parseFloat(tokens[4]))); //eV
      mo.put("symmetry", sd.sym + "_" + (sd.index + 1));
      orbitals.add(mo);
    }
    int[] pointers = new int[nBF];
    for (int i = 0; i < nBF; i++)
      slaters[i].pt = i;
    Arrays.sort(slaters, new SlaterSorter());
    int iAtom0 = atomSetCollection.getLastAtomSetAtomIndex();
    for (int i = 0; i < nBF; i++) {
      SlaterData sld = slaters[i];
      addSlater(iAtom0 + sld.iAtom, sld.x, sld.y, sld.z, sld.r, sld.alpha, 1);
      pointers[i] = slaters[i].pt;
    }
    setSlaters();
    sortOrbitalCoefficients(pointers);
    sortOrbitals();
    setMOs("eV");
  }
  
  class SlaterSorter implements Comparator {

    public int compare(Object arg0, Object arg1) {
      SlaterData s0 = (SlaterData) arg0;
      SlaterData s1 = (SlaterData) arg1;
      return (s0.iAtom < s1.iAtom ? -1 : s0.iAtom > s1.iAtom ? 1 : 0);
    }
    
  }
}
