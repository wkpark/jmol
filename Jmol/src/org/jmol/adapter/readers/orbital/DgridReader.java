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
package org.jmol.adapter.readers.orbital;

import org.jmol.adapter.smarter.*;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A reader for Dgrid BASISFILE data.
 *
 *
 */
public class DgridReader extends MopacDataReader {

  private String title;

  /**
   * Read the ADF output.
   *
   * @param reader  input stream
   */
  public void readAtomSetCollection(BufferedReader reader) {
    atomSetCollection = new AtomSetCollection("dgrid", this);
    this.reader = reader;
    modelNumber = 0;
    try {
      while (readLine() != null) {
        if (line.indexOf(":title") == 0) {
          readTitle();
          continue;
        }
        if (line.indexOf("basis:  CARTESIAN  STO") >= 0) {        
          readSlaterBasis(); // Cartesians
          continue;
        }
        if (line.indexOf(":atom") == 0) {
          readCoordinates();
          continue;
        }
        if (line.indexOf(" MO  DATA ") >= 0) {
          readMolecularOrbitals();
          continue;
        }
      }
    } catch (Exception e) {
      setError(e);
    }

  }

  private void readTitle() throws Exception {
    title = readLine().substring(2);
  }

  /**
   * Reads a set of coordinates
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readCoordinates() throws Exception {

    /*
     * 
:atom  No.         x          y          z          charge
:---------------------------------------------------------
  N     1:       0.0000     0.0000     4.8054         5.00
  O     2:       0.0000     0.0000     7.0933         6.00
  C     3:       0.0000     0.0000    -0.0761         4.00
  C     4:       0.0000     0.0000     2.6002         4.00
  C     5:       2.3400     0.0000    -1.3762         4.00
     * 
     */
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetName(title);
    discardLinesUntilContains("----");
    while (readLine() != null && !line.startsWith(":-----")) {
      String[] tokens = getTokens();
      if (tokens.length < 5)
        break;
      String symbol = tokens[0];
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = symbol;
      atom.set(parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]));
      atom.scale(ANGSTROMS_PER_BOHR);
    }
  }

  Hashtable htExponents = new Hashtable();
  private void readSlaterBasis() throws Exception {
     /*
:                           +--------------------------+
                            :  basis:  CARTESIAN  STO  :
:                           +--------------------------+


: atom  No.       type            exponents and coefficients
:-----------------------------------------------------------------------------------
   N     1         1s    exp:     6.38000000e+00
   N     1         2s    exp:     1.50000000e+00
   N     1         2s    exp:     2.50000000e+00
   N     1         2s    exp:     5.15000000e+00
   N     1         2p    exp:     1.00000000e+00
   N     1         2p    exp:     1.88000000e+00
       */
    discardLinesUntilContains(":-");
    char ch = 'a';    
    while (readLine() != null && line.indexOf(":-") < 0) {
      String atomSymbol = line.substring(3,6).trim();
      String xyz = line.substring(19, 21);
      String code = atomSymbol + xyz;
      if (htExponents.get(code) == null) {
        ch = 'a';
      } else {
        code += "_" + ch++;
      }
      String exp = line.substring(34);
      htExponents.put(code, new Float(parseFloat(exp)));
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
    String code;
        
    SlaterData(String atomSymbol, String xyz) {
      setCode(atomSymbol, xyz);
    }

    void setCode(String atomSymbol, String xyz) {
      char ch;
      char abc = ' ';
      char type = ' ';
      int exp = 1;
      int el = 0;
      for (int i = xyz.length(); --i >= 0;) {
        switch (ch = xyz.charAt(i)) {
        case '_':
          type = abc;
          break;
        case '1':
        case '2':
        case '3':
        case '4':
          exp = ch - '0';
          break;
        case 'x':
          x = exp;
          el += exp;
          exp = 1;
          break;
        case 'y':
          y = exp;
          el += exp;
          exp = 1;
          break;
        case 'z':
          z = exp;
          el += exp;
          exp = 1;
          break;
        case 's':
        case 'p':
        case 'd':
        case 'f':
        default:
          abc = ch;
        }
      }
      r = (exp - el - 1);
      code = atomSymbol + xyz.substring(0, 2);
      if (type != ' ')
        code += "_" + type;
      Float f = (Float) htExponents.get(code);
      if (f == null)
        Logger.error("Exponent for " + code + " not found");
      else
        alpha = f.floatValue();
      //System.out.println("DgridReader [" + iAtom + " " 
          //+ x + " " + y + " " + z + " " + r + "]" + " " + alpha);
    }
  }
 
  private Hashtable htFuncMap;
  private void readMolecularOrbitals() throws Exception {
    /*
sym: A1                 1 1s            2 1s            3 1s            4 1s            5 1s         
                        9 1s            6 1s            8 1s            7 1s           10 1s         
                       11 1s            1 2s            1 2s_a          1 2s_b          1 2pz        
                        1 2pz_a         1 2pz_b         1 3dz2          1 3dx2          1 3dy2       
     */
    htFuncMap = new Hashtable();
    discardLines(3);
    boolean sorting = true;
    while (line != null && line.indexOf(":") != 0) {
      discardLinesUntilContains("sym: ");
      String symmetry = line.substring(4, 10).trim();
      if (symmetry.indexOf("_FC") >= 0)
        break;
      StringBuffer data = new StringBuffer();
      data.append(line.substring(10));
      while (readLine() != null && line.length() >= 10)
        data.append(line);
      String[] tokens = getTokens(data.toString());
      int nFuncs = tokens.length / 2;
      int[] ptSlater = new int[nFuncs];
      Atom[] atoms = atomSetCollection.getAtoms();
      for (int i = 0, pt = 0; i < tokens.length;) {
        int iAtom = parseInt(tokens[i++]) - 1;
        String code = tokens[i++];
        String key = iAtom + "_" + code;
        if (htFuncMap.containsKey(key)) {
          ptSlater[pt++] = ((Integer) htFuncMap.get(key)).intValue();
        } else {
          int n = intinfo.size();
          ptSlater[pt++] = n;
          htFuncMap.put(key, new Integer(n));
          //System.out.println(code + " " + key);
          SlaterData sd = new SlaterData(atoms[iAtom].elementSymbol, code);
          // temporarily we put the slater pointer into coef; 
          // we will change this to 1.0 later
          addSlater(iAtom, sd.x, sd.y, sd.z, sd.r, sd.alpha, (sorting ? n : 1));
        }
      }
      discardLinesUntilContains(":-");
      readLine();
      while (line != null && line.length() >= 20) {
        int iOrb = parseInt(line.substring(0, 10));
        float energy = parseFloat(line.substring(10, 20));
        StringBuffer cData = new StringBuffer();
        cData.append(line.substring(20));
        while (readLine() != null && line.length() >= 10) {
          if (line.charAt(3) != ' ')
            break;
          cData.append(line);
        }
        float[] list = new float[intinfo.size()];
        tokens = getTokens(cData.toString());
        if (tokens.length != nFuncs)
          Logger
              .error("DgridReader: number of coefficients does not equal number of functions");
        for (int i = 0; i < tokens.length; i++) {
          int pt = ptSlater[i];
          list[pt] = parseFloat(tokens[i]);
          if (symmetry.equals("B2") && iOrb == 6)
            System.out.println(pt + ": coef=" + list[pt] + " for slater " + Escape.escape((int[])intinfo.get(pt)).replace('\n',' '));
        }
        Hashtable mo = new Hashtable();
        orbitals.add(mo);
        mo.put("energy", new Float(energy));
        mo.put("coefficients", list);
        mo.put("symmetry", symmetry + "_" + iOrb);
        //System.out.println(orbitals.size() + " " + symmetry + "_" + iOrb);
      }
    }

    if (sorting)
      sortSlaters();

    /*
:                        +------------+
                         | OCCUPATION |
:                        +------------+



:------------------------------------------------------------
:  #  symmetry         orb          ALPHA            BETA
:------------------------------------------------------------
   1  A1                1      1.00000000000    1.00000000000
   2  A1                2      1.00000000000    1.00000000000
   3  A1                3      1.00000000000    1.00000000000
     */
    discardLinesUntilContains(":  #  symmetry");
    readLine();
    for (int i = 0; i < orbitals.size(); i++) {
      readLine();
      float occupancy = parseFloat(line.substring(31, 45)) + parseFloat(line.substring(47, 61));
      ((Hashtable) orbitals.get(i)).put("occupancy", new Float(occupancy));
    }
    sortOrbitals();
    // System.out.println(Escape.escape(list, false));
    setSlaters(true, false);
    setMOs("eV");
  }

  private void sortSlaters() {
    int atomCount = atomSetCollection.getAtomCount();
    int nSlaters = intinfo.size();
    int[] intdata;
    float[] floatdata;
    Vector[] slaters = new Vector[atomCount];
    for (int i = atomCount; --i >= 0;)
      slaters[i] = new Vector();
    for (int i = nSlaters; --i >= 0;) {
      intdata = (int[]) intinfo.get(i);
      floatdata = (float[]) floatinfo.get(i);
      slaters[intdata[0]].add(floatdata);
    }
    Vector oldinfo = (Vector) intinfo.clone();
    intinfo.clear();
    floatinfo.clear();
    // sort the slaters in order by atom, because it
    // takes considerable time to switch between atoms
    // and we want all the references to a given atom in the same block
    int[] pointers = new int[nSlaters];
    for (int i = 0, pt = 0; i < atomCount; i++) {
      Vector v = slaters[i];
      for (int j = v.size(); --j >= 0;) {
        floatdata = (float[]) v.get(j);
        int k = pointers[pt++] = (int) floatdata[1];
        floatdata[1] = 1;
        intinfo.add(oldinfo.get(k));
        floatinfo.add(floatdata);
      }
    }
    sortOrbitalCoefficients(pointers);
  }

}
