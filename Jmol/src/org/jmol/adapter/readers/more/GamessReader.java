/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Vector;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

abstract public class GamessReader extends AtomSetCollectionReader {

  protected int atomCount = 0;
  //int moCount = 0;
  protected int shellCount = 0;
  protected int gaussianCount = 0;
  protected String calculationType = "?";
  protected Hashtable moData = new Hashtable();
  protected Vector orbitals = new Vector();
  protected Vector atomNames = new Vector();
  abstract public AtomSetCollection readAtomSetCollection(BufferedReader reader); 
 
  abstract protected void readAtomsInBohrCoordinates() throws Exception;  
 
  protected void readGaussianBasis(String initiator, String terminator) throws Exception {
    Vector gdata = new Vector();
    gaussianCount = 0;
    int nGaussians = 0;
    shellCount = 0;
    String thisShell = "0";
    String[] tokens;
    discardLinesUntilContains(initiator);
    readLine();
    int[] slater = null;
    Hashtable shellsByAtomType = new Hashtable();
    Vector slatersByAtomType = new Vector();
    String atomType = null;
    
    while (readLine() != null && line.indexOf(terminator) < 0) {
      //System.out.println(line);
      if (line.indexOf("(") >= 0)
        line = GamessReader.fixBasisLine(line);
      tokens = getTokens();
      switch (tokens.length) {
      case 1:
        if (atomType != null) {
          if (slater != null) {
            slater[2] = nGaussians;
            slatersByAtomType.addElement(slater);
            slater = null;
          }
          shellsByAtomType.put(atomType, slatersByAtomType);
        }
        slatersByAtomType = new Vector();
        atomType = tokens[0];
        break;
      case 0:
        break;
      default:
        if (!tokens[0].equals(thisShell)) {
          if (slater != null) {
            slater[2] = nGaussians;
            slatersByAtomType.addElement(slater);
          }
          thisShell = tokens[0];
          shellCount++;
          slater = new int[] {
              JmolAdapter.getQuantumShellTagID(fixShellTag(tokens[1])), gaussianCount,
              0 };
          nGaussians = 0;
        }
        ++nGaussians;
        ++gaussianCount;
        gdata.addElement(tokens);
      }
    }
    if (slater != null) {
      slater[2] = nGaussians;
      slatersByAtomType.addElement(slater);
    }
    if (atomType != null)
      shellsByAtomType.put(atomType, slatersByAtomType);
    Vector sdata = new Vector();
    atomCount = atomNames.size();
    for (int i = 0; i < atomCount; i++) {
      atomType = (String) atomNames.elementAt(i);
      Vector slaters = (Vector) shellsByAtomType.get(atomType);
      if (slaters == null) {
        Logger.error("slater for atom " + i + " atomType " + atomType
            + " was not found in listing. Ignoring molecular orbitals");
        return;
      }
      for (int j = 0; j < slaters.size(); j++) {
        slater = (int[]) slaters.elementAt(j);
        sdata.addElement(new int[] { i, slater[0], slater[1], slater[2] });
        //System.out.println(atomType + " " + i + " " + slater[0] + " " + slater[1] + " "+ slater[2]);
          
      }
    }
    float[][] garray = new float[gaussianCount][];
    for (int i = 0; i < gaussianCount; i++) {
      tokens = (String[]) gdata.get(i);
      garray[i] = new float[tokens.length - 3];
      for (int j = 3; j < tokens.length; j++)
        garray[i][j - 3] = parseFloat(tokens[j]);
    }
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    if (Logger.debugging) {
      Logger.debug(shellCount + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
    moData.put("calculationType", calculationType);
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
  }

  abstract protected String fixShellTag(String tag);

  /*
   ------------------
   MOLECULAR ORBITALS
   ------------------

          ------------
          EIGENVECTORS
          ------------

                      1          2          3          4          5
                  -79.9156   -20.4669   -20.4579   -20.4496   -20.4419
                     A          A          A          A          A   
    1  C  1  S   -0.000003  -0.000029  -0.000004   0.000011   0.000016
    2  C  1  S   -0.000009   0.000140   0.000001   0.000057   0.000065
    3  C  1  X    0.000007  -0.000241  -0.000022  -0.000010  -0.000061
    4  C  1  Y   -0.000008   0.000017  -0.000027  -0.000010   0.000024
    5  C  1  Z    0.000007   0.000313   0.000009  -0.000002  -0.000001
    6  C  1  S    0.000049   0.000875  -0.000164  -0.000521  -0.000440
    7  C  1  X   -0.000066   0.000161   0.000125   0.000034   0.000406
    8  C  1  Y    0.000042   0.000195  -0.000165  -0.000254  -0.000573
    9  C  1  Z    0.000003   0.000045   0.000052   0.000112  -0.000129
   10  C  1 XX   -0.000010   0.000010  -0.000040   0.000019   0.000045
   11  C  1 YY   -0.000010  -0.000031   0.000000  -0.000003   0.000019
...

                      6          7          8          9         10
                  -20.4354   -20.4324   -20.3459   -20.3360   -11.2242
                     A          A          A          A          A   
    1  C  1  S    0.000000  -0.000001   0.000001   0.000000   0.008876
    2  C  1  S   -0.000003   0.000002   0.000003   0.000002   0.000370

...
 TOTAL NUMBER OF BASIS SET SHELLS             =  101

   */

  protected void readMolecularOrbitals() throws Exception {
    Hashtable[] mos = null;
    Vector[] data = null;
    Vector coeffLabels = null;
    readLine(); // -------
    int nThisLine = 0;
    while (readLine() != null) {
      String[] tokens = getTokens();
      if (Logger.debugging) {
        Logger.debug(tokens.length + " --- " + line);
      }
      if (line.indexOf("end") >= 0)
        break;
      if (line.length() == 0 || line.indexOf("--") >= 0 || line.indexOf(".....") >=0) {
        for (int iMo = 0; iMo < nThisLine; iMo++) {
          float[] coefs = new float[data[iMo].size()];
          int iCoeff = 0;
          while (iCoeff < coefs.length) {
            // Reorder F coeffs; leave the rest untouched
            if (((String) coeffLabels.get(iCoeff)).equals("XXX")) {
              Hashtable fCoeffs = new Hashtable();
              for (int ifc = 0; ifc < 10; ifc++) {
                fCoeffs.put(coeffLabels.get(iCoeff+ifc), data[iMo].get(iCoeff+ifc));
              }
              for (int ifc = 0; ifc < 10; ifc++) {
                String orderLabel = JmolAdapter.getQuantumSubshellTag(JmolAdapter.SHELL_F_CARTESIAN, ifc);
                coefs[iCoeff++] = parseFloat((String) fCoeffs.get(orderLabel));
              }
            } else {
              coefs[iCoeff] = parseFloat((String) data[iMo].get(iCoeff));
              iCoeff++;
            }
          }
          mos[iMo].put("coefficients", coefs);
          orbitals.addElement(mos[iMo]);
        }
        nThisLine = 0;
        if (line.length() == 0)
          continue;
        break;
      }
      //read the data line:
      if (nThisLine == 0) {
        nThisLine = tokens.length;
        if (mos == null || nThisLine > mos.length) {
           mos = new Hashtable[nThisLine];
           data = new Vector[nThisLine];
        }
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable();
          data[i] = new Vector();
        }
        getMOHeader(tokens, mos, nThisLine);
        coeffLabels = new Vector();
        continue;
      }

      int nSkip = tokens.length - nThisLine;
      coeffLabels.addElement(JmolAdapter.canonicalizeQuantumSubshellTag(tokens[nSkip - 1].toUpperCase()));
      for (int i = 0; i < nThisLine; i++)
        data[i].addElement(tokens[i + nSkip]);
      line = "";
    }
    moData.put("mos", orbitals);
    setMOData(moData);
  }

  abstract protected void getMOHeader(String[] tokens, Hashtable[] mos, int nThisLine) throws Exception;
  
  protected void readFrequencies() throws Exception {
    //not for GamessUK yet
    int totalFrequencyCount = 0;
    int atomCountInFirstModel = atomSetCollection.getAtomCount();
    float[] xComponents = new float[5];
    float[] yComponents = new float[5];
    float[] zComponents = new float[5];
    float[] frequencies = new float[5];
    discardLinesUntilContains("FREQUENCY:");
    while (line != null && line.indexOf("FREQUENCY:") >= 0) {
      int lineBaseFreqCount = totalFrequencyCount;
      int lineFreqCount = 0;
      String[] tokens = getTokens();
      for (int i = 0; i < tokens.length; i++) {
        float frequency = parseFloat(tokens[i]);
        if (tokens[i].equals("I"))
          frequencies[lineFreqCount - 1] = -frequencies[lineFreqCount - 1];
        if (Float.isNaN(frequency))
          continue; // may be "I" for imaginary
        frequencies[lineFreqCount] = frequency;
        lineFreqCount++;
        if (Logger.debugging) {
          Logger.debug(totalFrequencyCount + " frequency=" + frequency);
        }
        if (lineFreqCount == 5)
          break;
      }
      String[] red_masses = null;
      String[] intensities = null;
      readLine();
      if (line.indexOf("MASS") >= 0) {
        red_masses = getTokens();
        readLine();
      }
      if (line.indexOf("INTENS") >= 0) {
        intensities = getTokens();
      }
      for (int i = 0; i < lineFreqCount; i++) {
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          atomSetCollection.cloneFirstAtomSet();
        atomSetCollection.setAtomSetName(frequencies[i] + " cm-1");
        atomSetCollection.setAtomSetProperty("Frequency", frequencies[i]
            + " cm-1");
        if (red_masses != null)
          atomSetCollection.setAtomSetProperty("Reduced Mass", red_masses[i + 2]
            + " AMU");
        if (intensities != null)
          atomSetCollection.setAtomSetProperty("IR Intensity", intensities[i + 2]
            + " D^2/AMU-Angstrom^2");

      }
      Atom[] atoms = atomSetCollection.getAtoms();
      discardLinesUntilBlank();
      for (int i = 0; i < atomCountInFirstModel; ++i) {
        readLine();
        readComponents(lineFreqCount, xComponents);
        readLine();
        readComponents(lineFreqCount, yComponents);
        readLine();
        readComponents(lineFreqCount, zComponents);
        for (int j = 0; j < lineFreqCount; ++j) {
          int atomIndex = (lineBaseFreqCount + j) * atomCountInFirstModel + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = xComponents[j];
          atom.vectorY = yComponents[j];
          atom.vectorZ = zComponents[j];
        }
      }
      discardLines(12);
      readLine();
    }
  }

  private void readComponents(int count, float[] components) {
    for (int i = 0, start = 20; i < count; ++i, start += 12)
      components[i] = parseFloat(line, start, start + 12);
  }

  protected static String fixBasisLine(String line) {
    int pt, pt1;
    line = line.replace(')', ' ');
    while ((pt = line.indexOf("(")) >= 0) {
      pt1 = pt;
      while (line.charAt(--pt1) == ' '){}
      while (line.charAt(--pt1) != ' '){}
      line = line.substring(0, ++pt1) + line.substring(pt + 1);
    }
    return line;
  }
}
