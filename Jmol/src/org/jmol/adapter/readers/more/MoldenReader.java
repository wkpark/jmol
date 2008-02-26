package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;


import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Vector;
import java.util.NoSuchElementException;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

/**
 * A molecular structure and orbital reader for MolDen files.
 * See http://www.cmbi.ru.nl/molden/molden_format.html
 * 
 * @author Matthew Zwier <mczwier@gmail.com>
 */

public class MoldenReader extends MopacDataReader {
  protected float[] frequencies = null;
  protected AtomSetCollection freqAtomSet = null;
  
	public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
		this.reader = reader;
		atomSetCollection = new AtomSetCollection("molden");
		modelNumber = 0;
		try {
			readLine();
			while (line != null) {
        if (line.startsWith("[Atoms]") ) {
          readAtoms();
          continue;
        } else if (line.equals("[GTO]")) {
          readGaussianBasis();
          continue;
        } else if (line.equals("[MO]")) {
          readMolecularOrbitals();
          continue;
        } else if (line.indexOf("[FREQ]") >= 0) {
          readFreqsAndModes();
          continue;
        }
        readLine();
			}			
		} catch (Exception e) {
		 	return setError(e);
		}
		return atomSetCollection;
	}
  
  void readAtoms() throws Exception {
    /* 
     [Atoms] {Angs|AU}
     C     1    6         0.0076928100       -0.0109376700        0.0000000000
     H     2    1         0.0779745600        1.0936027600        0.0000000000
     H     3    1         0.9365572000       -0.7393011000        0.0000000000
     H     4    1         1.1699572800        0.2075167300        0.0000000000
     H     5    1        -0.4338802400       -0.3282176500       -0.9384614500
     H     6    1        -0.4338802400       -0.3282176500        0.9384614500
     */
    
    String coordUnit = line.substring(7).trim();
    
    int nPrevAtom = 0, nCurAtom = 0;
   
    if ( ! (coordUnit.equals("Angs") || coordUnit.equals("AU"))) {
      throw new Exception("invalid coordinate unit " + coordUnit + " in [Atoms]"); 
    }
    
    readLine();
    while (line != null && line.charAt(0) != '[') {    
      Atom atom = atomSetCollection.addNewAtom();
      String [] tokens = getTokens();
      atom.atomName = tokens[0];
      // tokens[1] is the atom number.  Since sane programs shouldn't list
      // these out of order, just throw an exception if one is encountered
      // out of order (for now)
      nCurAtom = parseInt(tokens[1]);
      if (nPrevAtom > 0 && nCurAtom != nPrevAtom + 1 ) { 
        throw new Exception("out of order atom in [Atoms]");
      } 
      nPrevAtom = nCurAtom;
      atom.set(parseFloat(tokens[3]), parseFloat(tokens[4]), parseFloat(tokens[5]));
      readLine();
    }
    
    if (coordUnit.equals("AU"))
      for (int i = atomSetCollection.getAtomCount(); --i >= 0;)
        atomSetCollection.getAtom(i).scale(ANGSTROMS_PER_BOHR);
  }
  
  void readGaussianBasis() throws Exception {
    /* 
     [GTO]
       1 0
      s   10 1.00
       0.8236000000D+04  0.5309998617D-03
       0.1235000000D+04  0.4107998930D-02
       0.2808000000D+03  0.2108699451D-01
       0.7927000000D+02  0.8185297868D-01
       0.2559000000D+02  0.2348169388D+00
       0.8997000000D+01  0.4344008869D+00
       0.3319000000D+01  0.3461289099D+00
       0.9059000000D+00  0.3937798974D-01
       0.3643000000D+00 -0.8982997660D-02
       0.1285000000D+00  0.2384999379D-02
      s   10 1.00
     */
    Vector sdata = new Vector();
    Vector gdata = new Vector();
    int atomIndex = 0;
    int gaussianPtr = 0;
    
    while (readLine() != null 
        && ! ((line = line.trim()).length() == 0 || line.charAt(0) == '[') ) {
      // First, expect the number of the atomic center
      // The 0 following the atom index is now optional
      String[] tokens = getTokens();
      
      atomIndex = parseInt(tokens[0]) - 1;
      
      // Next is a sequence of shells and their primitives
      while (readLine() != null && line.trim().length() > 0) {
        // Next line has the shell label and a count of the number of primitives
        tokens = getTokens();
        String shellLabel = tokens[0].toUpperCase();
        int nPrimitives = parseInt(tokens[1]);
        int[] slater = new int[4];
        
        slater[0] = atomIndex;
        slater[1] = JmolAdapter.getQuantumShellTagID(shellLabel);
        slater[2] = gaussianPtr;
        slater[3] = nPrimitives;
        
        for (int ip = nPrimitives; --ip >= 0;) {
          // Read ip primitives, each containing an exponent and one (s,p,d,f)
          // or two (sp) contraction coefficient(s)
          String [] primTokens = getTokens(readLine());
          int nTokens = primTokens.length;
          float orbData[] = new float[nTokens];
          
          for (int d = 0; d < nTokens; d++)
            orbData[d] = parseFloat(primTokens[d]);
          gdata.addElement(orbData);
          gaussianPtr++;
        }
        sdata.addElement(slater);
      }      
      // Next atom
    }

    float [][] garray = new float[gaussianPtr][];
    for (int i = 0; i < gaussianPtr; i++)
      garray[i] = (float[]) gdata.get(i);
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    if (Logger.debugging) {
      Logger.debug(sdata.size() + " slater shells read");
      Logger.debug(garray.length + " gaussian primitives read");
    }
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
  }
  
  void readMolecularOrbitals() throws Exception {
    /*
      [MO]
       Ene=     -11.5358
       Spin= Alpha
       Occup=   2.000000
         1   0.99925949663
         2  -0.00126378192
         3   0.00234724545
     [and so on]
       110   0.00011350764
       Ene=      -1.3067
       Spin= Alpha
       Occup=   1.984643
         1  -0.00865451496
         2   0.79774685891
         3  -0.01553604903
     */
    
    readLine();
    if (line.equals("[5D]")) {
      //TODO May be a bug here if there is a mixture of 6D and 7F
      // We don't know while parsing the [GTO] section if we'll be using 
      // spherical or Cartesian harmonics, so walk the list of shell information
      // and reset as appropriate.
      Vector sdata = (Vector) moData.get("shells");
      for (int i = sdata.size(); --i >=0 ;) {
        int[] slater = (int[]) sdata.get(i);
        switch (slater[1]) {
        case JmolAdapter.SHELL_D_CARTESIAN:
          slater[1] = JmolAdapter.SHELL_D_SPHERICAL;
          break;
        case JmolAdapter.SHELL_F_CARTESIAN:
          slater[1] = JmolAdapter.SHELL_F_SPHERICAL;
          break;
        default:
          // Nothing needs to happen
          break;
        }
      }
      
      readLine();
    }
    
    String[] tokens = getTokens();
    while (tokens != null &&  line.indexOf('[') < 0) {
      Hashtable mo = new Hashtable();
      Vector data = new Vector();
      float energy = Float.NaN;
      float occupancy = Float.NaN;
      
      while (tokens != null && parseInt(tokens[0]) == Integer.MIN_VALUE) {
        String[] kvPair;
        if (tokens[0].startsWith("Ene")) {
          kvPair = splitKeyValue();
          energy = parseFloat(kvPair[1]);          
        } else if (tokens[0].startsWith("Occup")) {
          kvPair = splitKeyValue();
          occupancy = parseFloat(kvPair[1]);
        }  // TODO: Symmetry and spin
        tokens = getTokens(readLine());
      }
      
      if (tokens == null)
        throw new Exception("error reading MOs: unexpected EOF reading coeffs");
      
      while (tokens != null && parseInt(tokens[0]) != Integer.MIN_VALUE) {
        if (tokens.length != 2)
          throw new Exception("invalid MO coefficient specification");
        // tokens[0] is the function number, and tokens[1] is the coefficient
        data.addElement(tokens[1]);
        tokens = getTokens(readLine());
      }
      
      float[] coefs = new float[data.size()];
      for (int i = data.size(); --i >= 0;) {
        coefs[i] = parseFloat((String) data.get(i));
      }
      mo.put("energy", new Float(energy));
      mo.put("occupancy", new Float(occupancy));
      mo.put("coefficients", coefs);
      orbitals.addElement(mo);
      if (Logger.debugging) {
        Logger.debug(coefs.length + " coefficients in MO " + orbitals.size() );
      }
    }
    Logger.debug("read " + orbitals.size() + " MOs");
    setMOs("eV");
  }
  
  void readFreqsAndModes() throws Exception {
    String[] tokens;
    Vector frequencies = new Vector();
    while (readLine() != null && line.indexOf('[') < 0) {
      frequencies.add(getTokens()[0]);
    }
    if (line.indexOf("[FR-COORD]") < 0)
      throw new Exception("error reading normal modes: [FREQ] must be followed by [FR-COORD]");
    
    final int nFreqs = frequencies.size();
    final int nAtoms = atomSetCollection.getFirstAtomSetAtomCount();
    
    atomSetCollection.cloneLastAtomSet();
    atomSetCollection.setAtomSetName("frequency base geometry");
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int nAtom = 0; nAtom < nAtoms; nAtom++) {
      tokens = getTokens(readLine());
      Atom atom = atoms[nAtom + atomSetCollection.getLastAtomSetAtomIndex()];
      atom.atomName = tokens[0];
      atom.set(parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
      atom.scale(ANGSTROMS_PER_BOHR);      
    }
      
    readLine();
    if (line.indexOf("[FR-NORM-COORD]") < 0) 
      throw new Exception("error reading normal modes: [FR-COORD] must be followed by [FR-NORM-COORD]");
    
    for (int nFreq = 0; nFreq < nFreqs; nFreq++) {
      if (readLine().indexOf("Vibration") < 0)
        throw new Exception("error reading normal modes: expected vibration data");
      atomSetCollection.cloneLastAtomSet();
      atomSetCollection.setAtomSetName(frequencies.get(nFreq) + " cm-1");
      atoms = atomSetCollection.getAtoms();
      for (int nAtom = 0; nAtom < nAtoms; nAtom++) {
        Atom atom = atoms[nAtom + atomSetCollection.getLastAtomSetAtomIndex()];
        tokens = getTokens(readLine());
        atom.vectorX = parseFloat(tokens[0]) * ANGSTROMS_PER_BOHR;
        atom.vectorY = parseFloat(tokens[1]) * ANGSTROMS_PER_BOHR;
        atom.vectorZ = parseFloat(tokens[2]) * ANGSTROMS_PER_BOHR;
      }      
    }
    readLine();
  }

  String[] splitKeyValue() {
    return splitKeyValue("=", line);
  }
  
  String[] splitKeyValue(String sep) {
    return splitKeyValue(sep, line);
  }
  
  String[] splitKeyValue(String sep, String text) throws NoSuchElementException {
    String[] kvPair = new String[2];
    int posSep = text.indexOf(sep);
    if (posSep < 0)
      throw new NoSuchElementException("separator not found");
    kvPair[0] = text.substring(0, posSep);
    kvPair[1] = text.substring(posSep + sep.length());
    return kvPair;    
  }
}
