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

package org.openscience.jmol;
import javax.vecmath.Point3d;
import org.openscience.jmol.ProteinProp;
import org.openscience.jmol.render.JmolFrame;
import java.awt.Color;
import org.openscience.cdk.renderer.color.AtomColorer;
import org.openscience.cdk.renderer.color.PartialAtomicChargeColors;
import java.io.Reader;
import java.io.IOException;
import org.openscience.jmol.io.ReaderFactory;
import org.openscience.jmol.io.ChemFileReader;
import java.util.Iterator;

public class DeprecatedAdapter implements JmolClientAdapter {
  AtomColorer[] colorSchemes;

  public DeprecatedAdapter() {
    colorSchemes = new AtomColorer[JmolClientAdapter.COLORSCHEME_MAX];
    colorSchemes[JmolClientAdapter.COLORSCHEME_CPK] =
      new DefaultCdkAtomColors();
    colorSchemes[JmolClientAdapter.COLORSCHEME_CHARGE] =
      new PartialAtomicChargeColors();
  }

  /****************************************************************
   * the file related methods
   ****************************************************************/

  public Object openReader(DisplayControl control,
                           String name, Reader reader) {
    ChemFile chemFile = null;
    try {
      ChemFileReader chemFileReader = null;
      try {
        chemFileReader = ReaderFactory.createReader(control, reader);
        /*
          FIXME -- need to notify the awt component of file change
          firePropertyChange(openFileProperty, oldFile, currentFile);
        */
      } catch (IOException ex) {
        return "Error determining input format: " + ex;
      }
      if (chemFileReader == null) {
        return "unrecognized input format";
      }
      chemFile = chemFileReader.read();
    } catch (IOException ex) {
      return "Error reading input:" + ex;
    }
    if (chemFile == null)
      return "unknown error reading file";
    return chemFile;
  }

  public String getModelName(Object clientFile) {
    return null;
  }

  public int getFrameCount(Object clientFile) {
    return ((ChemFile)clientFile).getNumberOfFrames();
  }


  public JmolFrame getJmolFrame(Object clientFile, int frameNumber) {
    return ((ChemFile)clientFile).getFrame(frameNumber).getJmolFrame();
  }

  /****************************************************************
   * The frame related methods
   ****************************************************************/

  public int getAtomCount(Object clientFile, int frameNumber) {
    return ((ChemFile)clientFile).getFrame(frameNumber).getAtomCount();
  }

  public boolean hasPdbRecords(Object clientFile, int frameNumber) {
    return false;
  }

  public Iterator getAtomIterator(Object clientFile, int frameNumber) {
    return new AtomIterator();
  }

  public Iterator getCovalentBondIterator(Object clientFile, int frameNumber) {
    return new CovalentBondIterator();
  }

  public Iterator getAssociationIterator(Object clientFile, int frameNumber) {
    return new AssociationIterator();
  }

  public Iterator getVectorIterator(Object clientFile, int frameNumber) {
    return new VectorIterator();
  }

  public Iterator getCrystalCellIterator(Object clientFile, int frameNumber) {
    return new CrystalCellIterator();
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator implements Iterator {
    public boolean hasNext() {
      return false;
    }
    public Object next() {
      return null;
    }
    public void remove() {
      throw new NullPointerException();
    }
  }

  class CovalentBondIterator implements Iterator {
    public boolean hasNext() {
      return false;
    }
    public Object next() {
      return null;
    }
    public void remove() {
      throw new NullPointerException();
    }
  }

  class AssociationIterator implements Iterator {
    public boolean hasNext() {
      return false;
    }
    public Object next() {
      return null;
    }
    public void remove() {
      throw new NullPointerException();
    }
  }

  class VectorIterator implements Iterator {
    public boolean hasNext() {
      return false;
    }
    public Object next() {
      return null;
    }
    public void remove() {
      throw new NullPointerException();
    }
  }

  class CrystalCellIterator implements Iterator {
    public boolean hasNext() {
      return false;
    }
    public Object next() {
      return null;
    }
    public void remove() {
      throw new NullPointerException();
    }
  }

  /****************************************************************
   * The atom related methods
   ****************************************************************/

  public int getAtomicNumber(Object clientAtom) {
    return ((Atom)clientAtom).getAtomicNumber();
  }
  
  public String getAtomicSymbol(int atomicNumber, Object clientAtom) {
    return ((Atom)clientAtom).getSymbol();
  }

  public String getAtomTypeName(int atomicNumber, Object clientAtom) {
    return ((Atom)clientAtom).getAtomTypeName();
  }

  public double getVanderwaalsRadius(int atomicNumber, Object clientAtom) {
    return ((Atom)clientAtom).getVanderwaalsRadius();
  }

  public double getCovalentRadius(int atomicNumber, Object clientAtom) {
    return ((Atom)clientAtom).getCovalentRadius();
  }

  public Point3d getPoint3d(Object clientAtom) {
    return ((Atom)clientAtom).getPoint3D();
  }

  public String getPdbAtomRecord(Object clientAtom){
    return ((Atom)clientAtom).getPdbRecord();
  }

  public Color getColor(int atomicNumber, Object clientAtom, int colorScheme) {
    if (colorScheme >= colorSchemes.length ||
        colorSchemes[colorScheme] == null)
      colorScheme = 0;
    return colorSchemes[colorScheme].getAtomColor((Atom)clientAtom);
  }

  class DefaultCdkAtomColors implements AtomColorer {

    /**
     * Returns the color for a certain atom type
     */
    public Color getAtomColor(org.openscience.cdk.Atom a) {
        Object o = a.getProperty("org.openscience.jmol.color");
        if (o instanceof Color) {
            return (Color)o;
        } else {
          // no color set. return pink - easy to see
          return Color.pink;
        }
    }
  }
}
