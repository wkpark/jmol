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
import java.io.Reader;
import java.awt.Color;
import java.util.Iterator;

public interface JmolClientAdapter {

  public Object openReader(DisplayControl control, String name, Reader reader);
  public int getFrameCount(Object clientFile);
  public String getModelName(Object clientFile);

  public int getAtomCount(Object clientFile, int frameNumber);
  public boolean hasPdbRecords(Object clientFile, int frameNumber);
  public AtomIterator getAtomIterator(Object clientFile, int frameNumber);
  public BondIterator getCovalentBondIterator(Object clientFile,
                                              int frameNumber);
  public BondIterator getAssociationBondIterator(Object clientFile,
                                                 int frameNumber);
  public LineIterator getVectorIterator(Object clientFile, int frameNumber);
  public LineIterator getCrystalCellIterator(Object clientFile,
                                             int frameNumber);

  public final static int COLORSCHEME_CPK = 0;
  public final static int COLORSCHEME_CHARGE = 1;
  public final static int COLORSCHEME_MAX = 2;

  public int getAtomicNumber(Object clientAtom);
  public String getAtomicSymbol(int atomicNumber, Object clientAtom);
  public String getAtomTypeName(int atomicNumber, Object clientAtom);
  public double getVanderwaalsRadius(int atomicNumber, Object clientAtom);
  public double getCovalentRadius(int atomicNumber, Object clientAtom);
  public Point3d getPoint3d(Object clientAtom);
  public String getPdbAtomRecord(Object clientAtom);
  public Color getColor(int atomicNumber, Object clientAtom, int colorScheme);

  // Java 1.1 does not have java.util.Iterator
  // and I don't want to use Enumerator
  // so we will define our own AtomIterator
  public abstract class AtomIterator {
    public abstract boolean hasNext();
    public abstract Object next();
  }

  public abstract class BondIterator {
    public abstract boolean hasNext();
    public abstract Object next();
    public abstract Object getAtom1();
    public abstract Object getAtom2();
    public abstract int getOrder();
  }

  public abstract class LineIterator {
    public abstract boolean hasNext();
    public abstract Object next();
    public abstract Point3d getPoint1();
    public abstract Point3d getPoint2();
  }
}
