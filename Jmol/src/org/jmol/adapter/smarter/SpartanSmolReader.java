/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;
import java.util.Vector;
import java.util.Hashtable;

class SpartanSmolReader extends AtomSetCollectionReader {

  final boolean logging = false;

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {

    atomSetCollection = new AtomSetCollection("spartan .smol");

    try {

      String line = reader.readLine();
      while (line != null) {
        if (line.startsWith("BEGININPUT"))
          readAtoms(reader);
        if (line.equals("GEOMETRY"))
          readMoreAtoms(reader);
        if (line.startsWith("BEGINPROPARC"))
          readProperties(reader);
        line = reader.readLine();
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      atomSetCollection.errorMessage = "Could not read Spartan SMOL file:" + ex;
      return atomSetCollection;
    }

    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

  void readAtoms(BufferedReader reader) throws Exception {
    atomSetCollection.newAtomSet();
    String line;
    int atomNum;
    logger.log("Reading BEGININPUT atom records...");
    line = reader.readLine();
    line = reader.readLine();
    logger.log(line);
    atomSetCollection.setAtomSetName(line + " Input");
    line = reader.readLine();
    while ((line = reader.readLine()) != null
        && (atomNum = parseInt(line, 0, 2)) > 0) {
      float x = parseFloat(line, 2, 16);
      float y = parseFloat(line, 16, 30);
      float z = parseFloat(line, 30, 44);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = getElementSymbol(atomNum);
      atom.x = x;
      atom.y = y;
      atom.z = z;
    }
  }

  void readMoreAtoms(BufferedReader reader) throws Exception {
    atomSetCollection.discardPreviousAtoms();
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetName("Geometry"); // start with an empty name
    String line;
    int atomNum;
    logger.log("Reading BEGINARCHIVE GEOMETERY atom records...");
    while ((line = reader.readLine()) != null
        && (atomNum = parseInt(line, 0, 3)) > 0) {
      //logger.log("atom: " + line);
      /*
       was for OUTPUT section  
       String elementSymbol = parseToken(line, 10, 12);
       float x = parseFloat(line, 17, 30);
       float y = parseFloat(line, 31, 43);
       float z = parseFloat(line, 44, 58);
       */
      float x = parseFloat(line, 4, 17);
      float y = parseFloat(line, 18, 31);
      float z = parseFloat(line, 32, 44);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = getElementSymbol(atomNum);
      atom.x = x * ANGSTROMS_PER_BOHR;
      atom.y = y * ANGSTROMS_PER_BOHR;
      atom.z = z * ANGSTROMS_PER_BOHR;
    }
  }

  void readProperties(BufferedReader reader) throws Exception {
    String line;
    logger.log("Reading PROPARC properties records...");
    while ((line = reader.readLine()) != null
        && (line.length() < 10 || !line.substring(0, 10).equals("ENDPROPARC"))) {
      if (line.length() >= 4 && line.substring(0, 4).equals("PROP"))
        readProperty(reader, line);
      if (line.length() >= 7 && line.substring(0, 7).equals("VIBFREQ"))
        readVibFreqs(reader);
    }
  }

  void readProperty(BufferedReader reader, String line) throws Exception {
    String tokens[] = getTokens(line);
    if (tokens.length == 0)
      return;
    //logger.log("reading property line:" + line);
    boolean isString = (tokens[1].equals("STRING"));
    String keyName = tokens[2];
    Object value = new Object();
    Vector vector = new Vector();
    if (tokens[3].equals("=")) {
      if (isString) {
        value = getString(line, tokens[4].substring(0, 1));
      } else {
        value = new Float(parseFloat(tokens[4]));
      }
    } else if (tokens[tokens.length - 1].equals("BEGIN")) {
      int nValues = parseInt(tokens[tokens.length - 2]);
      if (nValues == 0)
        nValues = 1;
      boolean isArray = (tokens.length == 6);
      Vector atomInfo = new Vector();
      while ((line = reader.readLine()) != null
          && !line.substring(0, 3).equals("END")) {
        if (isString) {
          value = getString(line, "\"");
          vector.add(value);
        } else {
          String tokens2[] = getTokens(line);
          for (int i = 0; i < tokens2.length; i++) {
            if (isArray) {
              atomInfo.add(new Float(parseFloat(tokens2[i])));
              if ((i + 1) % nValues == 0) {
                vector.add(atomInfo);
                atomInfo = new Vector();
              }
            } else {
              value = new Float(parseFloat(tokens2[i]));
              vector.add(value);
            }
          }
        }
      }
      value = null;
    } else {
      logger.log(" Skipping property line " + line);
    }
    //logger.log(keyName + " = " + value + " ; " + vector);
    if (value != null)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(keyName, value);
    if (vector.size() != 0)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(keyName, vector);
  }

  //logger.log("reading property line:" + line);

  void readVibFreqs(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    String label = "";
    int frequencyCount = parseInt(line);
    Vector vibrations = new Vector();
    Vector freqs = new Vector();
    logger.log("reading VIBFREQ vibration records: frequencyCount = "
        + frequencyCount);
    for (int i = 0; i < frequencyCount; ++i) {
      atomSetCollection.cloneLastAtomSet();
      line = reader.readLine();
      Hashtable info = new Hashtable();
      float freq = parseFloat(line);
      info.put("freq", new Float(freq));
      if (line.length() > 15
          && !(label = line.substring(15, line.length())).equals("???"))
        info.put("label", label);
      freqs.add(info);
      atomSetCollection.setAtomSetName(label + " " + freq+" cm^-1");
      atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,"Frequencies");
    }
    // System.out.print(freqs);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("VibFreqs", freqs);
    int atomCount = atomSetCollection.getFirstAtomSetAtomCount();
    Atom[] atoms = atomSetCollection.atoms;
    Vector vib = new Vector();
    Vector vibatom = new Vector();
    int ifreq = 0;
    int iatom = atomCount; // add vibrations starting at second atomset
    int nValues = 3;
    float[] atomInfo = new float[3];
    while ((line = reader.readLine()) != null) {
      String tokens2[] = getTokens(line);
      for (int i = 0; i < tokens2.length; i++) {
        float f = parseFloat(tokens2[i]);
        atomInfo[i % nValues] = f;
        vibatom.add(new Float(f));
        if ((i + 1) % nValues == 0) {
          if (logging) 
            logger.log(ifreq + " atom "+iatom +"/"+atomCount+" vectors: " + atomInfo[0] + " "+atomInfo[1] + " " + atomInfo[2]);
          atoms[iatom].addVibrationVector(atomInfo[0],atomInfo[1],atomInfo[2]);
          vib.add(vibatom);
          vibatom = new Vector();
          ++iatom;
        }
      }
      if (iatom % atomCount == 0) {
        vibrations.add(vib);
        vib = new Vector();
        if (++ifreq == frequencyCount)
          break; ///loop exit
      }
    }
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("vibration", vibrations);
  }

}
