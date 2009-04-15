/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;

import java.util.Hashtable;
import java.util.Vector;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

abstract public class GamessReader extends MOReader {

  protected Vector atomNames = new Vector();

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
    int atomCount = atomNames.size();
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

  protected void readFrequencies() throws Exception {
    //not for GamessUK yet
    int totalFrequencyCount = 0;
    int atomCount = atomSetCollection.getLastAtomSetAtomCount();
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
      for (int i = 0; i < atomCount; ++i) {
        readLine();
        readComponents(lineFreqCount, xComponents);
        readLine();
        readComponents(lineFreqCount, yComponents);
        readLine();
        readComponents(lineFreqCount, zComponents);
        for (int j = 0; j < lineFreqCount; ++j) {
          int atomIndex = (lineBaseFreqCount + j) * atomCount + i;
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
