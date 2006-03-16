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
      discardLinesUntilStartsWith(reader, "BEGINARCHIVE");
      if (discardLinesUntilContains(reader, "GEOMETRY") != null)
        readAtoms(reader);
      if (discardLinesUntilContains(reader, "BEGINPROPARC") != null)
        readProperties(reader);
      //      if (discardLinesUntilContains(reader, "VIBRATIONAL FREQUENCIES") != null)
      //        readFrequencies(reader);
    } catch (Exception ex) {
      ex.printStackTrace();
      atomSetCollection.errorMessage = "Could not read file:" + ex;
      return atomSetCollection;
    }
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

  void readAtoms(BufferedReader reader) throws Exception {
    //no need to discard after GEOMETRY
    //discardLines(reader, 2);
    String line;
    int atomNum;
    System.out.println("Reading BEGINARCHIVE GEOMETERY atom records...");
    while ((line = reader.readLine()) != null
        && (atomNum = parseInt(line, 0, 5)) > 0) {
      //System.out.println("atom: " + line);
      /*
       was for OUTPUT section  
       String elementSymbol = parseToken(line, 10, 12);
       float x = parseFloat(line, 17, 30);
       float y = parseFloat(line, 31, 43);
       float z = parseFloat(line, 44, 58);
       */
      float x = parseFloat(line, 6, 19);
      float y = parseFloat(line, 20, 33);
      float z = parseFloat(line, 34, 47);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = getElementSymbol(atomNum);
      atom.x = x * ANGSTROMS_PER_BOHR;
      atom.y = y * ANGSTROMS_PER_BOHR;
      atom.z = z * ANGSTROMS_PER_BOHR;
    }
  }

  void readProperties(BufferedReader reader) throws Exception {
    String line;
    System.out.println("Reading PROPARC properties records...");
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
    //System.out.println("reading property line:" + line);
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
      System.out.println(" Skipping property line " + line);
    }
    //System.out.println(keyName + " = " + value + " ; " + vector);
    if (value != null)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(keyName, value);
    if (vector.size() != 0)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(keyName, vector);
  }

  //System.out.println("reading property line:" + line);

  void readVibFreqs(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    String label = "";
    int freqCount = parseInt(line);
    Vector vibrations = new Vector();
    Vector freqs = new Vector();
    System.out.println("reading VIBFREQ vibration records: freqCount = "
        + freqCount);
    for (int i = 1; i < freqCount; i++)
      atomSetCollection.cloneFirstAtomSet();
    for (int i = 0; i < freqCount; i++) {
      line = reader.readLine();
      Hashtable info = new Hashtable();
      info.put("freq", new Float(parseFloat(line)));
      if (line.length() > 15
          && !(label = line.substring(15, line.length())).equals("???"))
        info.put("label", label);
      freqs.add(info);
    }
    // System.out.print(freqs);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("VibFreqs", freqs);
    int atomCount = atomSetCollection.getFirstAtomSetAtomCount();
    Atom[] atoms = atomSetCollection.atoms;
    Vector vib = new Vector();
    Vector vibatom = new Vector();
    int ifreq = 0;
    int iatom = 0;
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
            System.out.println(ifreq + " atom "+iatom +"/"+atomCount+" vectors: " + atomInfo[0] + " "+atomInfo[1] + " " + atomInfo[2]);
          atoms[iatom].addVibrationVector(atomInfo[0],atomInfo[1],atomInfo[2]);
          vib.add(vibatom);
          vibatom = new Vector();
          ++iatom;
        }
      }
      if (iatom % atomCount == 0) {
        vibrations.add(vib);
        vib = new Vector();
        if (++ifreq == freqCount)
          break; ///loop exit
      }
    }
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("vibration", vibrations);
  }

  void readFrequencies(BufferedReader reader) throws Exception {

    // deprecated 3/15/06 RMH in favor of the ARCHIVE reader.

    discardLinesUntilBlank(reader);

    int totalFrequencyCount = 0;
    while (true) {
      String line = discardLinesUntilNonBlank(reader);
      int lineBaseFreqCount = totalFrequencyCount;
      //      System.out.println("lineBaseFreqCount=" + lineBaseFreqCount);
      ichNextParse = 16;
      int lineFreqCount;
      line = line.substring(13); // skip the " Frequency:"
      for (lineFreqCount = 0; lineFreqCount < 3; ++lineFreqCount) {
        float frequency = parseFloat(line, ichNextParse);
        //        System.out.println("frequency=" + frequency);
        if (Float.isNaN(frequency))
          break; //////////////// loop exit is here
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          atomSetCollection.cloneFirstAtomSet();
      }
      if (lineFreqCount == 0)
        return;
      Atom[] atoms = atomSetCollection.atoms;
      discardLines(reader, 2);
      int firstAtomSetAtomCount = atomSetCollection.getFirstAtomSetAtomCount();
      for (int i = 0; i < firstAtomSetAtomCount; ++i) {
        line = reader.readLine();
        for (int j = 0; j < lineFreqCount; ++j) {
          int ichCoords = j * 23 + 10;
          float x = parseFloat(line, ichCoords, ichCoords + 7);
          float y = parseFloat(line, ichCoords + 7, ichCoords + 14);
          float z = parseFloat(line, ichCoords + 14, ichCoords + 21);
          int atomIndex = (lineBaseFreqCount + j) * firstAtomSetAtomCount + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = x;
          atom.vectorY = y;
          atom.vectorZ = z;
          //          System.out.println("x=" + x + " y=" + y + " z=" + z);
        }
      }
    }
  }
}
