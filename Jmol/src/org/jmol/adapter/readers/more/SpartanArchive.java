/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-05 00:45:22 -0500 (Wed, 05 Jul 2006) $
 * $Revision: 5271 $
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
import org.jmol.api.JmolAdapter;
import org.jmol.util.Parser;

import java.io.BufferedReader;
import java.util.Vector;
import java.util.Hashtable;
import javax.vecmath.Vector3f;

import org.jmol.util.Logger;

public class SpartanArchive {

  int atomCount = 0;
  String bondData; // not in archive; may or may not have
  int bondCount = 0;
  int moCount = 0;
  int coefCount = 0;
  int shellCount = 0;
  int gaussianCount = 0;
  String endCheck;
  String calculationType = "";
  BufferedReader reader;
  String line;
  
  String[] getTokens() {
    return Parser.getTokens(line);
  }

  AtomSetCollection atomSetCollection;
  AtomSetCollectionReader r;
  Hashtable moData;
  Vector orbitals = new Vector();

  SpartanArchive(AtomSetCollectionReader r,
      AtomSetCollection atomSetCollection, Hashtable moData) {
    initialize(r, atomSetCollection, moData, "");
  }

  SpartanArchive(AtomSetCollectionReader r,
      AtomSetCollection atomSetCollection, Hashtable moData, String bondData, String endCheck) {
    initialize(r, atomSetCollection, moData, bondData);
    this.endCheck = endCheck;
  }

  private void initialize(AtomSetCollectionReader r,
                          AtomSetCollection atomSetCollection,
                          Hashtable moData, String bondData) {
    this.r = r;
    this.reader = r.reader;
    this.atomSetCollection = atomSetCollection;
    this.moData = moData;
    moData.put("energyUnits","");
    this.bondData = bondData;
  }

  void setEndCheck(String endChec){
    
  }
  int readArchive(String infoLine, boolean haveGeometryLine) throws Exception {
    atomCount = setInfo(infoLine);
    line = (haveGeometryLine ? "GEOMETRY" : "");
    boolean haveMOData = false;
    while (line != null) {
      if (line.equals("GEOMETRY")) {
        readAtoms();
        if (bondData.length() > 0)
          addBonds(bondData);
      } else if (line.indexOf("BASIS") == 0) {
        readBasis();
      } else if (line.indexOf("WAVEFUNC") == 0 || line.indexOf("BETA") == 0) {
        readMolecularOrbital();
        haveMOData = true;
      } else if (line.equals("ENDARCHIVE")
          || endCheck != null && line.indexOf(endCheck) == 0) {
        break;
      }
      readLine();
    }
    if (haveMOData)
      r.setMOData(moData);
    return atomCount;
  }

  static String[] getTokens(String info) {
    return Parser.getTokens(info);
  }

  int parseInt(String info) {
    return r.parseInt(info);
  }

  float parseFloat(String info) {
    return r.parseFloat(info);
  }

  int setInfo(String info) throws Exception {
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    //    0   1  2   3    4   5   6   7  8        9

    String[] tokens = getTokens(info);
    if (Logger.debugging) {
      Logger.debug("reading Spartan archive info :" + info);
    }
    atomCount = parseInt(tokens[0]);
    coefCount = parseInt(tokens[1]);
    shellCount = parseInt(tokens[2]);
    gaussianCount = parseInt(tokens[3]);
    //overallCharge = parseInt(tokens[4]);
    moCount = parseInt(tokens[6]);
    calculationType = tokens[9];
    String s = (String) moData.get("calculationType");
    if (s != null)
      calculationType += s;
    moData.put("calculationType", calculationType);
    return atomCount;
  }

  void readAtoms() throws Exception {
    for (int i = 0; i < atomCount; i++) {
      readLine();
      String tokens[] = getTokens();
      float x = parseFloat(tokens[1]);
      float y = parseFloat(tokens[2]);
      float z = parseFloat(tokens[3]);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = AtomSetCollectionReader
          .getElementSymbol(parseInt(tokens[0]));
      atom.set(x, y, z);
      atom.scale(AtomSetCollectionReader.ANGSTROMS_PER_BOHR);
    }
    if (Logger.debugging) {
      Logger.debug(atomCount + " atoms read");
    }
  }

  void addBonds(String data) {
    addBonds(data, 0);
  }

  void addBonds(String data, int atomCount0) {
    /* from cached data:
     
     <one number per atom>
     1    2    1
     1    3    1
     1    4    1
     1    5    1
     1    6    1
     1    7    1

     */

    String tokens[] = getTokens(data);
    bondCount = 0;
    for (int i = atomCount; i < tokens.length;) {
      int sourceIndex = parseInt(tokens[i++]) - 1 + atomCount0;
      int targetIndex = parseInt(tokens[i++]) - 1 + atomCount0;
      int bondOrder = parseInt(tokens[i++]);
      if (bondOrder > 0) {
        atomSetCollection.addBond(new Bond(sourceIndex, targetIndex,
            bondOrder < 4 ? bondOrder : bondOrder == 5 ? JmolAdapter.ORDER_AROMATIC : 1));
        bondCount++;
      }
    }
    if (Logger.debugging) {
      Logger.debug(bondCount + " bonds read");
    }
  }

  void readBasis() throws Exception {
    /*
     * standard Gaussian format:
     
     BASIS
     0   2   1   1   0
     0   1   3   1   0
     0   3   4   2   0
     1   2   7   2   0
     1   1   9   2   0
     0   3  10   3   0
     ...
     5.4471780000D+00
     3.9715132057D-01   0.0000000000D+00   0.0000000000D+00   0.0000000000D+00
     8.2454700000D-01
     5.5791992333D-01   0.0000000000D+00   0.0000000000D+00   0.0000000000D+00

     */

    Vector sdata = new Vector();
    float[][] garray = new float[gaussianCount][];
    int[] typeArray = new int[gaussianCount];
    for (int i = 0; i < shellCount; i++) {
      readLine();
      String[] tokens = getTokens();
      int[] slater = new int[4];
      slater[0]= parseInt(tokens[3]) - 1; //atom pointer; 1-based
      int iBasis = parseInt(tokens[0]); //0 = S, 1 = SP, 2 = D, 3 = F
      slater[1] = (iBasis == 0 ? 0 : iBasis + 1);//0 = S, 1 = P, 2 = SP, 3 = D, 4 = F
      int gaussianPtr = slater[2] = parseInt(tokens[2]) - 1;
      int nGaussians = slater[3] = parseInt(tokens[1]);
      for (int j = 0; j < nGaussians; j++)
        typeArray[gaussianPtr + j] = iBasis;
      sdata.addElement(slater);
    }
    for (int i = 0; i < gaussianCount; i++) {
      float alpha = parseFloat(readLine());
      String[] tokens = getTokens(readLine());
      int nData = tokens.length;
      float[] data = new float[nData + 1];
      data[0] = alpha;
      int iBasis = typeArray[i];
      //we put D and F into coef 1. This may change if I find that Gaussian output
      //lists D and F in columns 3 and 4 as well.
      switch(iBasis) {
      case 1: //SP
        data[2] = parseFloat(tokens[1]);
        //fall through
      case 0: //S
        data[1] = parseFloat(tokens[0]);
        break;
      case 2: //D
        data[1] = parseFloat(tokens[2]);
        break;
      case 3: //F
        data[1] = parseFloat(tokens[3]);
      }
      garray[i] = data;
    }
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    if (Logger.debugging) {
      Logger.debug(sdata.size() + " slater shells read");
      Logger.debug(garray.length + " gaussian primitives read");
    }
  }

  void readMolecularOrbital() throws Exception {
    int tokenPt = 0;
    String[] tokens = getTokens("");
    float[] energies = new float[moCount];
    float[][] coefficients = new float[moCount][coefCount];
    for (int i = 0; i < moCount; i++) {
      if (tokenPt == tokens.length) {
        tokens = getTokens(readLine());
        tokenPt = 0;
      }
      energies[i] = parseFloat(tokens[tokenPt++]);
    }
    for (int i = 0; i < moCount; i++) {
      for (int j = 0; j < coefCount; j++) {
        if (tokenPt == tokens.length) {
          tokens = getTokens(readLine());
          tokenPt = 0;
        }
        coefficients[i][j] = parseFloat(tokens[tokenPt++]);
      }
    }
    for (int i = 0; i < moCount; i++) {
      Hashtable mo = new Hashtable();
      mo.put("energy", new Float(energies[i]));
      //mo.put("occupancy", new Float(-1));
      mo.put("coefficients", coefficients[i]);
      orbitals.addElement(mo);
    }
    if (Logger.debugging) {
      Logger.debug(orbitals.size() + " molecular orbitals read");
    }
    moData.put("mos", orbitals);
  }

  void readProperties() throws Exception {
    Logger.debug("Reading PROPARC properties records...");
    while (readLine() != null
        && (line.length() < 10 || !line.substring(0, 10).equals("ENDPROPARC"))) {
      if (line.length() >= 4 && line.substring(0, 4).equals("PROP"))
        readProperty();
      if (line.length() >= 6 && line.substring(0, 6).equals("DIPOLE"))
        readDipole();
      if (line.length() >= 7 && line.substring(0, 7).equals("VIBFREQ"))
        readVibFreqs();
    }
  }

  void readDipole() throws Exception {
    //fall-back if no other dipole record
    readLine();
    String tokens[] = getTokens();
    if (tokens.length != 3)
      return;
    Vector3f dipole = new Vector3f(parseFloat(tokens[0]),
        parseFloat(tokens[1]), parseFloat(tokens[2]));
    atomSetCollection.setAtomSetAuxiliaryInfo("dipole", dipole);
  }

  void readProperty() throws Exception {
    String tokens[] = getTokens();
    if (tokens.length == 0)
      return;
    //Logger.debug("reading property line:" + line);
    boolean isString = (tokens[1].equals("STRING"));
    String keyName = tokens[2];
    Object value = new Object();
    Vector vector = new Vector();
    if (tokens[3].equals("=")) {
      if (isString) {
        value = getQuotedString(tokens[4].substring(0, 1));
      } else {
        value = new Float(parseFloat(tokens[4]));
      }
    } else if (tokens[tokens.length - 1].equals("BEGIN")) {
      int nValues = parseInt(tokens[tokens.length - 2]);
      if (nValues == 0)
        nValues = 1;
      boolean isArray = (tokens.length == 6);
      Vector atomInfo = new Vector();
      while (readLine() != null
          && !line.substring(0, 3).equals("END")) {
        if (isString) {
          value = getQuotedString("\"");
          vector.addElement(value);
        } else {
          String tokens2[] = getTokens();
          for (int i = 0; i < tokens2.length; i++) {
            if (isArray) {
              atomInfo.addElement(new Float(parseFloat(tokens2[i])));
              if ((i + 1) % nValues == 0) {
                vector.addElement(atomInfo);
                atomInfo = new Vector();
              }
            } else {
              value = new Float(parseFloat(tokens2[i]));
              vector.addElement(value);
            }
          }
        }
      }
      value = null;
    } else {
      if (Logger.debugging) {
        Logger.debug(" Skipping property line " + line);
      }
    }
    //Logger.debug(keyName + " = " + value + " ; " + vector);
    if (value != null)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(keyName, value);
    if (vector.size() != 0)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(keyName, vector);
  }

  //Logger.debug("reading property line:" + line);

  void readVibFreqs() throws Exception {
    readLine();
    String label = "";
    int frequencyCount = parseInt(line);
    Vector vibrations = new Vector();
    Vector freqs = new Vector();
    if (Logger.debugging) {
      Logger.debug(
          "reading VIBFREQ vibration records: frequencyCount = " + frequencyCount);
    }
    for (int i = 0; i < frequencyCount; ++i) {
      int atomCount0 = atomSetCollection.getAtomCount();
      atomSetCollection.cloneLastAtomSet();
      addBonds(bondData, atomCount0);
      readLine();
      Hashtable info = new Hashtable();
      float freq = parseFloat(line);
      info.put("freq", new Float(freq));
      if (line.length() > 15
          && !(label = line.substring(15, line.length())).equals("???"))
        info.put("label", label);
      freqs.addElement(info);
      atomSetCollection.setAtomSetName(label + " " + freq + " cm^-1");
      atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
          "Frequencies");
    }
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("VibFreqs", freqs);
    int atomCount = atomSetCollection.getFirstAtomSetAtomCount();
    Atom[] atoms = atomSetCollection.getAtoms();
    Vector vib = new Vector();
    Vector vibatom = new Vector();
    int ifreq = 0;
    int iatom = atomCount; // add vibrations starting at second atomset
    int nValues = 3;
    float[] atomInfo = new float[3];
    while (readLine() != null) {
      String tokens2[] = getTokens();
      for (int i = 0; i < tokens2.length; i++) {
        float f = parseFloat(tokens2[i]);
        atomInfo[i % nValues] = f;
        vibatom.addElement(new Float(f));
        if ((i + 1) % nValues == 0) {
          //Logger.debug(ifreq + " atom " + iatom + "/" + atomCount
          //      + " vectors: " + atomInfo[0] + " " + atomInfo[1] + " "
          //      + atomInfo[2]);
          atoms[iatom]
              .addVibrationVector(atomInfo[0], atomInfo[1], atomInfo[2]);
          vib.addElement(vibatom);
          vibatom = new Vector();
          ++iatom;
        }
      }
      if (iatom % atomCount == 0) {
        vibrations.addElement(vib);
        vib = new Vector();
        if (++ifreq == frequencyCount)
          break; ///loop exit
      }
    }
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("vibration", vibrations);
  }

  String getQuotedString(String strQuote) {
    int i = line.indexOf(strQuote);
    int j = line.lastIndexOf(strQuote);
    return (j == i ? "" : line.substring(i + 1, j));
  }
  
  //because this is NOT an extension of AtomSetCollectionReader
  String readLine() throws Exception {
    line = reader.readLine();
    //System.out.println("spartanarchive " + line);
    return line;
  }
}
