/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.openscience.jmol.adapters;

// these are standard and should be needed by all adapters
import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;

import java.awt.Color;
import javax.vecmath.Point3d;
import java.io.BufferedReader;
import java.util.StringTokenizer;

// client-specific imports

public class XyzJmolModelAdapter implements JmolModelAdapter {

  /****************************************************************
   * the capabilities
   ****************************************************************/
  public boolean suppliesAtomicNumber() { return false; }
  public boolean suppliesAtomicSymbol() { return true; }
  public boolean suppliesAtomTypeName() { return false; }
  public boolean suppliesVanderwaalsRadius() { return false; }
  public boolean suppliesCovalentRadius() { return false; }
  public boolean suppliesAtomColor() { return false; }
  
  /****************************************************************
   * the file related methods
   ****************************************************************/

  public Object openBufferedReader(JmolViewer viewer,
                                   String name, BufferedReader bufferedReader) {
    System.out.println("Xyz reading:" + name);
    Xyz xyz = new Xyz(bufferedReader);
    if (xyz.errorMessage != null)
      return xyz.errorMessage;
    return xyz;
  }

  public String getModelName(Object clientFile) {
    return ((Xyz)clientFile).modelName;
  }

  public int getFrameCount(Object clientFile) {
    return 1;
  }


  /****************************************************************
   * The frame related methods
   ****************************************************************/

  public int getAtomCount(Object clientFile, int frameNumber) {
    return ((Xyz)clientFile).atomCount;
  }

  public boolean hasPdbRecords(Object clientFile, int frameNumber) {
    return false;
  }

  public JmolModelAdapter.AtomIterator
    getAtomIterator(Object clientFile, int frameNumber) {
    return new AtomIterator((Xyz)clientFile);
  }

  public JmolModelAdapter.BondIterator
    getCovalentBondIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.BondIterator
    getAssociationBondIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getVectorIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getCrystalCellIterator(Object clientFile, int frameNumber) {
    return null;
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolModelAdapter.AtomIterator {
    Xyz xyz;
    int iatom;

    AtomIterator(Xyz xyz) {
      this.xyz = xyz;
      iatom = 0;
    }
    public boolean hasNext() {
      return iatom < xyz.atomCount;
    }
    public Object next() {
      return xyz.atoms[iatom++];
    }
  }

  /****************************************************************
   * The atom related methods
   ****************************************************************/

  public int getAtomicNumber(Object clientAtom) {
    return -1;
  }
  
  public String getAtomicSymbol(Object clientAtom) {
    return ((XyzAtom)clientAtom).atomicSymbol;
  }

  public String getAtomTypeName(Object clientAtom) {
    return null;
  }

  public double getVanderwaalsRadius(Object clientAtom) {
    return 0;
  }

  public double getCovalentRadius(Object clientAtom) {
    return 0;
  }

  public Point3d getPoint3d(Object clientAtom) {
    return ((XyzAtom)clientAtom).point3d;
  }

  public String getPdbAtomRecord(Object clientAtom){
    return null;
  }

  public Color getAtomColor(Object clientAtom, int colorScheme) {
    return null;
  }

  class XyzAtom {
    String atomicSymbol;
    Point3d point3d;
    XyzAtom(String atomicSymbol, double x, double y, double z) {
      this.atomicSymbol = atomicSymbol;
      point3d = new Point3d(x, y, z);
    }
  }

  class Xyz {
    int atomCount;
    String modelName;
    XyzAtom atoms[];
    String errorMessage;

    Xyz(BufferedReader reader) {
      try {
        readAtomCount(reader);
        readModelName(reader);
        readAtoms(reader);
      } catch (Exception ex) {
        errorMessage = "Could not read file:" + ex;
        System.out.println(errorMessage);
      }
    }

    void readAtomCount(BufferedReader reader) throws Exception {
      String line = reader.readLine();
      StringTokenizer tokenizer = new StringTokenizer(line, "\t ");
      atomCount = Integer.parseInt(tokenizer.nextToken());
      System.out.println("atomCount=" + atomCount);
    }

    void readModelName(BufferedReader reader) throws Exception {
      String line = reader.readLine();
      if (!line.equals("")) {
        modelName = line;
        System.out.println("modelName=" + modelName);
      }
    }

    void readAtoms(BufferedReader reader) throws Exception {
      atoms = new XyzAtom[atomCount];
      for (int i = 0; i < atomCount; ++i) {
        StringTokenizer tokenizer = new StringTokenizer(reader.readLine(), "\t ");
        String atomicSymbol = tokenizer.nextToken();
        double x = Double.valueOf(tokenizer.nextToken()).doubleValue();
        double y = Double.valueOf(tokenizer.nextToken()).doubleValue();
        double z = Double.valueOf(tokenizer.nextToken()).doubleValue();
        atoms[i] = new XyzAtom(atomicSymbol, x, y, z);
      }
    }
  }
}
