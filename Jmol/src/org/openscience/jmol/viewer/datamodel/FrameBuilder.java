/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import javax.vecmath.Point3f;

final public class FrameBuilder {

  final JmolViewer viewer;
  final JmolModelAdapter adapter;
  Object clientFile;
  final int frameNumber;

  public FrameBuilder(JmolViewer viewer,
                      JmolModelAdapter adapter,
                      Object clientFile,
                      int frameNumber) {
    this.viewer = viewer;
    this.adapter = adapter;
    this.clientFile = clientFile;
    this.frameNumber = frameNumber;
  }

  protected void finalize() {
    System.out.println("FrameBuilder.finalize() called!");
  }

  public Frame buildFrame() {
    long timeBegin = System.currentTimeMillis();
    int atomCount = adapter.getAtomCount(clientFile, frameNumber);
    int modelType = adapter.getModelType(clientFile);
    boolean hasPdbRecords = adapter.hasPdbRecords(clientFile, frameNumber);

    Frame frame = new Frame(viewer, atomCount, modelType, hasPdbRecords);

    /****************************************************************
     * crystal cell must come first, in case atom coordinates
     * need to be transformed to fit in the crystal cell
     ****************************************************************/
    frame.setNotionalUnitcell(adapter.getNotionalUnitcell(clientFile,
                                                          frameNumber));
    frame.setPdbScaleMatrix(adapter.getPdbScaleMatrix(clientFile,
                                                      frameNumber));
    frame.setPdbScaleTranslate(adapter.
                               getPdbScaleTranslate(clientFile,
                                                    frameNumber));


    for (JmolModelAdapter.AtomIterator iterAtom =
           adapter.getAtomIterator(clientFile, frameNumber);
         iterAtom.hasNext(); ) {
      byte atomicNumber = (byte)iterAtom.getAtomicNumber();
      if (atomicNumber <= 0)
        atomicNumber = JmolConstants.
          atomicNumberFromAtomicSymbol(iterAtom.getAtomicSymbol());
      frame.addAtom(iterAtom.getUniqueID(), atomicNumber,
                    iterAtom.getAtomicCharge(), iterAtom.getAtomTypeName(),
                    iterAtom.getX(), iterAtom.getY(), iterAtom.getZ(),
                    (short)iterAtom.getPdbModelNumber(),
                    iterAtom.getPdbAtomRecord());
    }

    {
      JmolModelAdapter.BondIterator iterBond =
        adapter.getBondIterator(clientFile, frameNumber);
      if (iterBond != null)
        while (iterBond.hasNext())
          frame.bondAtoms(iterBond.getAtomUid1(),
                               iterBond.getAtomUid2(),
                               iterBond.getOrder());
    }

    if (hasPdbRecords) {
      String[] structures =
        adapter.getPdbStructureRecords(clientFile, frameNumber);
      if (structures != null && structures.length > 0)
        frame.pdbFile.setStructureRecords(structures);
    }
      
    frame.freeze();
    long msToBuild = System.currentTimeMillis() - timeBegin;
    System.out.println("Build a frame:" + msToBuild + " ms");
    adapter.finish(clientFile);
    clientFile = null;
    return frame;
  }
}
