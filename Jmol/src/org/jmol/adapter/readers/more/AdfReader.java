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
import org.jmol.util.Logger;

import java.io.BufferedReader;
import java.util.BitSet;
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

  

  String energy = null;
  int nXX = 0;

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
    try {
      while (readLine() != null) {
        if (line.indexOf(" (power of)") >= 0) {
          readSlaterBasis(); // Cartesians
          continue;
        }
        if (line.indexOf("Coordinates (Cartesian)") >= 0
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
          String[] tokens = getTokens();
          energy = tokens[1];
          continue;
        }
        if (line.indexOf("Vibrations") >= 0) {
          readFrequencies();
          continue;
        }
        if (line.indexOf(" Populations of individual BAS functions") >= 0) {
          readSlaterCoefficients();
        }
        if (line.indexOf("Scaled ZORA Orbital Energies") >= 0) {
          getOrbitalEnergies();
          continue;
        }
        if (line.indexOf("S F O   P O P U L A T I O N S ,   M O   A N A L Y S I S") >= 0) {
          if (false)
            readMolecularOrbitals(); // spherical!
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
  
  private Hashtable htSlatersByType;
  private void readSlaterBasis() throws Exception {
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
       */
    discardLines(3);
    if (htSlatersByType == null)
      htSlatersByType = new Hashtable();
    String atomType = line.substring(1, 3).trim();
    if (htSlatersByType.get(atomType) != null)
      return;
    readLine();
    Vector v = new Vector();
    while (readLine() != null && line.length() >= 10) {
      String[] tokens = getTokens();
      boolean isCore = tokens[0].equals("Core");
      int pt = (isCore ? 1 : 0);
      v.add(new SlaterData(isCore, parseInt(tokens[pt++]),
          parseInt(tokens[pt++]),parseInt(tokens[pt++]),parseInt(tokens[pt++]),
          parseFloat(tokens[pt])));
    }
    htSlatersByType.put(atomType, v);
  }

  private class SlaterData {
    boolean isCore;
    int x;
    int y;
    int z;
    int r;
    float alpha;
    String code;
    public SlaterData(boolean isCore, int x, int y, int z, int r, float alpha) {
      this.isCore = isCore;
      this.x = x;
      this.y = y;
      this.z = z;
      this.r = r;
      this.alpha = alpha;
      
    }
      
    
  }
  
  private void readSlaterCoefficients() throws Exception {
    /*
 Populations of individual BAS functions
 ----------------------------------------
 1 N            0.0005  0.3151  0.9502 -0.0507  0.1598  0.1571 -0.1218  0.8890  0.8947  0.8388
                0.1667  0.1686  0.1840 -0.0217  0.0000  0.0640 -0.0219  0.0612  0.0646 -0.0084
               -0.0027 -0.0104 -0.0028  0.0000  0.0147 -0.0087 -0.0103  0.0150  0.0245
 2 O            0.0005  0.9353  0.9852 -0.0173  0.4464  0.4351  0.1963  0.9416  0.9297  0.7311
                0.2980  0.2931  0.2053 -0.0068  0.0000  0.0208 -0.0065  0.0214  0.0433 -0.0091
               -0.0031 -0.0101 -0.0030  0.0000  0.0149 -0.0088 -0.0100  0.0149  0.0234

     */
    if (htSlatersByType == null)
      return;
    discardLines(2);
    while (line != null && line.length() >= 10) {
      String[] tokens = getTokens(line.substring(0, 10));
      int iAtom = parseInt(tokens[0]) - 1;
      Vector v = (Vector) htSlatersByType.get(tokens[1]);
      if (v == null) {
        Logger.error("ADF reader: no slaters of type " + tokens[1]);
        return;
      }
      StringBuffer data = new StringBuffer();
      data.append(line.substring(10));
      while (readLine() != null && line.length() > 0 && line.charAt(1) == ' ')
        data.append(line);
      tokens = getTokens(data.toString());
      if (tokens.length == v.size()) {
        for (int i = 0; i < tokens.length; i++) {
          SlaterData sd = (SlaterData) v.get(i);
          if (!sd.isCore)
            addSlater(iAtom, sd.x, sd.y, sd.z, sd.r, sd.alpha, parseFloat(tokens[i]));
        }
      } else {
        Logger.error("ADF reader: slaters wrong length for type " + tokens[1]);
        return;
      }
    }
  }


  private int nCore = 0;
  private Vector energies;
  
  private void getOrbitalEnergies() throws Exception {
    // ignoring symmetry here
    boolean isCore = (line.indexOf("Core") >= 0);
    discardLinesUntilContains("---------------");
    if (isCore) {
      while (readLine() != null && line.length() >= 10)
        nCore++;
      return;
    }
   /*
 Scaled ZORA Orbital Energies, per Irrep and Spin:
 =================================================
                        Occup              E (au)              E (eV)       Diff (eV) with prev. cycle
                        -----      --------------------        ------       --------------------------
 A
              19        2.000     -0.36836056274196E+00       -10.024               6.74E-07

    */
    readLine();
    energies = new Vector();
    while (readLine() != null && line.length() >= 10)
      energies.add(new Float(parseFloat(getTokens(line)[3])));
  }

  private void readMolecularOrbitals() throws Exception {
    /*
 Orb.:       19     20     21     22     23     24     25     26     27     28     29     30     31     32
 occup:     2.00   2.00   2.00   2.00   2.00   2.00   2.00   2.00   2.00   2.00   0.00   0.00   0.00   0.00
 CF+SFO     ----   ----   ----   ----   ----   ----   ----   ----   ----   ----   ----   ----   ----   ----
 ------
     13:    0.00  -0.01   0.00   0.00  -0.04   0.00   0.00   0.00   0.00   0.00   0.00   0.00  -1.07   0.00
     15:    0.10   0.00  -0.01   0.00   0.00   0.19   0.00   0.00   6.43   0.00   0.00   0.00   0.00  32.38
     */
    bsBases = new BitSet();    
    discardLinesUntilContains(" === ");
    String symmetry = getTokens()[1];
    
    discardLinesUntilContains(" Orb.:");
    String[] ids = getTokens(line.substring(10));
    String[] occupancies = getTokens(readLine().substring(10));
    int nOrbitals = occupancies.length;    
    discardLines(2);
    
    float[][] list = new float[nOrbitals][];
    Vector data = new Vector();
    nBases = 0;
    while (readLine() != null && line.length() >= 10) {
      data.add(getTokens(line.substring(10)));
      int i = parseInt(line.substring(0, 7)) - nCore - 1;
      bsBases.set(i);
      //System.out.println("nBases " + nBases + " basis " + i + " required " + line);
      nBases++;
    }
    int nFragments = data.size();
    //System.out.println("nFragments: " + nFragments);
    Hashtable mo;
    for (int i = 0; i < nOrbitals; i++) {
      orbitals.add(mo = new Hashtable());
      list[i] = new float[nFragments];
      for (int j = 0; j < nFragments; j++) {
        float val = parseFloat(((String[])data.get(j))[i]);
        //if (val < 10)
          //val = 0;
        //else
          //System.out.println("orb " + i + "  fragment " + j + ": " + val);
        list[i][j] = val;
      }
      mo.put("energy", energies.get(i));
      mo.put("occupancy", new Float(parseFloat(occupancies[i])));
      mo.put("coefficients", list[i]);
      mo.put("id", symmetry + ids[i]);
      //System.out.println("mo " + symmetry + ids[i] + " occup " + occupancies[i] + " energy " + energies.get(i));
    }
    //System.out.println(Escape.escape(list, false));
    setSlaters();
    setMOs("eV");
  }
}
