/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Piero Canepa, University of Kent, UK
 *
 * Contact: pc229@kent.ac.uk or pieremanuele.canepa@gmail.com
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
 *
 */

package org.jmol.adapter.readers.xtal;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.vecmath.Vector3f;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * http://cms.mpi.univie.ac.at/vasp/
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */


public class VaspReader extends AtomSetCollectionReader {

  private String[] atomMapped;
  private String[] elementName; //this array is to store the name of the element 
  private int atomCount = 0;

  @Override
  protected boolean checkLine() throws Exception {

    //reads the kind of atoms namely H, Ca etc
    if (line.toUpperCase().contains("INCAR")) {
      readIonsname();
      return true;
    }

    //this read  how many atoms per species 
    if (line.contains("LEXCH")) {
      discardLinesUntilContains("support grid");
      readNumberofatom();
      return true;
    }

    //This look for lattice vector in the cell
    if (line.contains("direct lattice vectors")) {
      readcellVector();
      return true;
    }

    //read the intial ionic position as in input
    if (line.contains("position of ions in fractional coordinates")) {
      readAtomicposfirst();
      return true;
    }

    //This Reads the ionic positions
    if (line.contains("POSITION")) {
      readAtomicpos();
      return true;
    }

    //This Reads the energy at the end of each step
    if (line.startsWith("  FREE ENERGIE")) {
      readEnergy();
      return true;
    }

    //This Reads how many normal modes  in case of frequency calculations
    if (line.startsWith("  Degree of freedom:")) {
      readnoModes();
      return true;
    }

    //This Reads normal modes
    if (line.startsWith(" Eigenvectors after division by SQRT(mass)")) {
      readFrequency();
      return true;
    }

    return true;
  }
  
  

  /*  
    POTCAR:    PAW H                                 
    POTCAR:    PAW O                                 
    POTCAR:    PAW Pd                                
    POTCAR:    PAW H                                 
      VRHFIN =H: ultrasoft test 
   */
  private void readIonsname() throws Exception {
    int counter = 0;
    boolean flagUnderscore = false;
    // Here I set the number of species to 100.
    // We might need a larger one. Because we are mapping the atom coordinate on it
    // See mapAtom().
    String[] tmpName = new String[100]; 

    while (readLine() != null && line.indexOf("VRHFIN") < 0) {
      if (line.contains("_")) {
        flagUnderscore = true;
        line = line.replace("_", " ");
      }

      String[] tokens = getTokens(line.substring(line.indexOf(":") + 1));

      if (!flagUnderscore) {
        tmpName[counter] = tokens[1];
      } else {
        tmpName[counter] = tokens[2];
      }
      counter++;
      flagUnderscore = false;
    }

    elementName = removeDuplicates(tmpName);

  }
  
  
  //This removes duplicates
  private String[] removeDuplicates(String[] array) {
    List<String> list = Arrays.asList(array);
    Set<String> set = new HashSet<String>(list);
    set.remove(null);
    String[] sortedArray = new String[set.size()];
    set.toArray(sortedArray);
    return sortedArray;
  }
  
/*  
  Dimension of arrays:
    k-Points           NKPTS =     10   number of bands    NBANDS=     16
    number of dos      NEDOS =    301   number of ions     NIONS =      8
    non local maximal  LDIM  =      4   non local SUM 2l+1 LMDIM =      8
    total plane-waves  NPLWV =  74088
    max r-space proj   IRMAX =   5763   max aug-charges    IRDMAX=  42942
    dimension x,y,z NGX =    42 NGY =   42 NGZ =   42
    dimension x,y,z NGXF=    84 NGYF=   84 NGZF=   84
    support grid    NGXF=    84 NGYF=   84 NGZF=   84
    ions per type =               6   2*/
  
  
  private void readNumberofatom() throws Exception {
    int[] numofElement = new int[100];
    readLine();
    String[] tokens = getTokens(line.substring(line.indexOf("=") + 1));
    atomCount = 0;
    for (int i = 0; i < tokens.length; i++)
      atomCount += (numofElement[i] = parseInt(tokens[i].trim()));
  //this is to reconstruct the atomMappedarray containing the atom
    atomMapped = new String[atomCount];
    for (int pt = 0, i = 0; i < numofElement.length; i++)
      for (int j = 0; j < numofElement[i]; j++)
        atomMapped[pt++] = elementName[i];
  }
      /*direct lattice vectors                 reciprocal lattice vectors
    1.850000000  1.850000000  0.000000000     0.270270270  0.270270270 -0.270270270
    0.000000000  1.850000000  1.850000000    -0.270270270  0.270270270  0.270270270
    1.850000000  0.000000000  1.850000000     0.270270270 -0.270270270  0.270270270*/
  
  private void readcellVector() throws Exception {
    
    float aCell, bCell, cCell, alphaCell, betaCell, gammaCell;
    Vector3f[] abc = new Vector3f[3];
 
    newAtomSet();
    readLine();
    for (int i = 0; i < 3; i++) {
      float x, y, z;
      String[] tokens = getTokens();
      x = parseFloat(tokens[0]);
      y = parseFloat(tokens[1]);
      z = parseFloat(tokens[2]);
      abc[i] = new Vector3f(x, y, z);
      readLine();
    }
    aCell = abc[0].length();
    bCell = abc[1].length();
    cCell = abc[2].length();
    alphaCell = (float) Math.toDegrees(abc[1].angle(abc[2]));
    betaCell = (float) Math.toDegrees(abc[2].angle(abc[0]));
    gammaCell = (float) Math.toDegrees(abc[0].angle(abc[1]));

    setUnitCell(aCell, bCell, cCell, alphaCell, betaCell, gammaCell);
  }
  
  //taken from CrystaReader.java
  //this add a model every time a new cell parameter is read 
  private void newAtomSet() throws Exception {
    if (atomSetCollection.getAtomCount() == 0)
      return;
    applySymmetryAndSetTrajectory();
    atomSetCollection.newAtomSet();
  }

/*
  position of ions in fractional coordinates (direct lattice) 
  0.87800000  0.62200000  0.25000000
  0.25000000  0.87800000  0.62200000
  0.62200000  0.25000000  0.87800000
  0.12200000  0.37800000  0.75000000
  0.75000000  0.12200000  0.37800000
  0.37800000  0.75000000  0.12200000
  0.00000000  0.00000000  0.00000000
  0.50000000  0.50000000  0.50000000
 
  position of ions in cartesian coordinates
  */
  ///This is the initial geometry not the geometry during the geometry dump
  private void readAtomicposfirst() throws Exception{
    int counter = 0;
      while (readLine() != null && line.length() > 10){
        Atom atom = atomSetCollection.addNewAtom();
        String[] tokens = getTokens();
        atom.atomName = atomMapped[counter++];
        float x = parseFloat(tokens[0]);
        float y = parseFloat(tokens[1]);
        float z = parseFloat(tokens[2]);
        setAtomCoord(atom, x, y, z);
      }
  }
  
  
/*  
  POSITION                                       TOTAL-FORCE (eV/Angst)
  -----------------------------------------------------------------------------------
       1.14298      0.83102      6.90311        -0.003060      0.001766      0.000000
      -1.29117      0.57434      6.90311         0.000000     -0.003533      0.000000
       0.14819     -1.40536      6.90311         0.003060      0.001766      0.000000
      -1.14298     -0.83102      4.93079         0.003060     -0.001766      0.000000
       1.29117     -0.57434      4.93079         0.000000      0.003533      0.000000
      -0.14819      1.40536      4.93079        -0.003060     -0.001766      0.000000
       0.00000      0.00000      0.00000         0.000000      0.000000      0.000000
       0.00000      0.00000      5.91695         0.000000      0.000000      0.000000
  -----------------------------------------------------------------------------------
  */
  private void readAtomicpos() throws Exception {
    int counter = 0;
    discardLines(1);
    while (readLine() != null && line.indexOf("----------") < 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      atom.atomName = atomMapped[counter];
      float x = parseFloat(tokens[0]);
      float y = parseFloat(tokens[1]);
      float z = parseFloat(tokens[2]);
      setAtomCoord(atom, x, y, z);
      counter++;
    }
  }
  
  
/*  FREE ENERGIE OF THE ION-ELECTRON SYSTEM (eV)
  ---------------------------------------------------
  free  energy   TOTEN  =       -20.028155 eV

  energy  without entropy=      -20.028155  energy(sigma->0) =      -20.028155
  */
  
  private Double energy, entropy;

  private void readEnergy() throws Exception {

    readLine();
    String[] tokens = getTokens(readLine());
    energy = Double.valueOf(Double.parseDouble(tokens[4]));
    readLine();
    tokens = getTokens(readLine());
    entropy = Double.valueOf(Double.parseDouble(tokens[3]));
    setenergyValue();
  }
  
  private void setenergyValue() {
    atomSetCollection.setAtomSetEnergy("" + energy, energy.floatValue());
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetAuxiliaryInfo("Entropy", entropy);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Entropy", entropy);
    atomSetCollection.setAtomSetName("Energy = " + energy + " eV, Entropy = "
        + entropy + " eV");
  }
  
  
  
  
  int noModes = 0;

  private void readnoModes() {
    String newLine = line.replace("/ ", " / ");
    noModes = parseInt(newLine.substring(newLine.indexOf(":") + 1, newLine
        .indexOf("/") - 1));
  }

/*  
  Eigenvectors after division by SQRT(mass)
  
  Eigenvectors and eigenvalues of the dynamical matrix
  ----------------------------------------------------
  
  
    1 f  =   61.880092 THz   388.804082 2PiTHz 2064.097613 cm-1   255.915580 meV
              X         Y         Z           dx          dy          dz
       1.154810  0.811470  6.887910     0.142328    0.284744    0.218505  
      -1.280150  0.594360  6.887910    -0.325017   -0.005914    0.256184  
       0.125350 -1.405820  6.887910     0.147813   -0.308571    0.250139  
      -1.154810 -0.811470  4.919940    -0.142322   -0.284738   -0.218502  
       1.280150 -0.594360  4.919940     0.325007    0.005917   -0.256181  
      -0.125350  1.405820  4.919940    -0.147808    0.308554   -0.250128  
       0.000000  0.000000  0.000000     0.000000    0.000000    0.000000  
       0.000000  0.000000  5.903930     0.000000    0.000000    0.000000  
  
    2 f  =   56.226671 THz   353.282596 2PiTHz 1875.519821 cm-1   232.534905 meV
              X         Y         Z           dx          dy          dz
       1.154810  0.811470  6.887910     0.005057   -0.193034   -0.074407  
      -1.280150  0.594360  6.887910    -0.215700   -0.046660    0.255430  
       0.125350 -1.405820  6.887910    -0.155522    0.437794   -0.342377  
      -1.154810 -0.811470  4.919940     0.005053   -0.193045   -0.074415  
       1.280150 -0.594360  4.919940    -0.215675   -0.046662    0.255412  
      -0.125350  1.405820  4.919940    -0.155521    0.437777   -0.342368  
       0.000000  0.000000  0.000000     0.015694    0.002977    0.005811  
       0.000000  0.000000  5.903930     0.011571   -0.016988    0.006533 
  
  
  
  */
  private void readFrequency() throws Exception {
    BitSet bs = new BitSet(noModes);
    int pt = atomSetCollection.getCurrentAtomSetIndex();
    int iAtom0 = atomSetCollection.getAtomCount();
    for (int i = 0; i < noModes; ++i) {
      if (!doGetVibration(++vibrationNumber))
        continue;
      bs.set(i);
      atomSetCollection.cloneLastAtomSet();
    }
    discardLines(5);
    boolean[] ignore = new boolean[1];
    for (int i = 0; i < noModes; i++) {
      readLine();
      atomSetCollection.setCurrentAtomSetIndex(++pt);
      atomSetCollection.setAtomSetFrequency(null, null, 
          line.substring(line.indexOf("PiTHz") + 1, line.indexOf("c") - 1).trim(), null);
      ignore[0] = !bs.get(i);
      readLine();
      fillFrequencyData(iAtom0, atomCount,atomCount, ignore, true, 35, 12, null);
      iAtom0 += atomCount;
      readLine();
    }
  }
  
  
  //this is spot on what we did for the Crystal Reader 
/*  private void setFreqValue(int i) {
    String activity = "Freq: " + data[2];
    atomSetCollection.setAtomSetFrequency(null, activity, "" + frequencies[i], null);
    atomSetCollection.setAtomSetName(data[0] + " "
        + TextFormat.formatDecimal(frequencies[i], 2) + " cm-1 ("
        + TextFormat.formatDecimal(Float.parseFloat(data[1]), 0) + " km/Mole), "
        + activity);
  }*/
  
}
