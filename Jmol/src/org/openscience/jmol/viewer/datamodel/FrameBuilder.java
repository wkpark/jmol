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

import org.jmol.api.ModelAdapter;
import org.openscience.jmol.viewer.*;
import javax.vecmath.Point3f;

final public class FrameBuilder {

  final JmolViewer viewer;
  final ModelAdapter adapter;

  public FrameBuilder(JmolViewer viewer, ModelAdapter adapter) {
    this.viewer = viewer;
    this.adapter = adapter;
  }

  protected void finalize() {
    System.out.println("FrameBuilder.finalize() called!");
  }

  public Frame buildFrame(Object clientFile) {
    long timeBegin = System.currentTimeMillis();
    int atomCount = adapter.getAtomCount(clientFile);
    int modelType = adapter.getModelType(clientFile);
    boolean hasPdbRecords = adapter.hasPdbRecords(clientFile);

    Frame frame = new Frame(viewer, atomCount, modelType, hasPdbRecords);

    /****************************************************************
     * crystal cell must come first, in case atom coordinates
     * need to be transformed to fit in the crystal cell
     ****************************************************************/
    frame.fileCoordinatesAreFractional =
      adapter.coordinatesAreFractional(clientFile);
    frame.setNotionalUnitcell(adapter.getNotionalUnitcell(clientFile));
    frame.setPdbScaleMatrix(adapter.getPdbScaleMatrix(clientFile));
    frame.setPdbScaleTranslate(adapter.getPdbScaleTranslate(clientFile));

    for (ModelAdapter.AtomIterator iterAtom =
           adapter.getAtomIterator(clientFile);
         iterAtom.hasNext(); ) {
      byte elementNumber = (byte)iterAtom.getElementNumber();
      if (elementNumber <= 0)
        elementNumber = JmolConstants.
          elementNumberFromSymbol(iterAtom.getElementSymbol());
      frame.addAtom(iterAtom.getModelNumber(), iterAtom.getUniqueID(),
                    elementNumber,
                    iterAtom.getAtomName(),
                    iterAtom.getAtomicCharge(),
                    iterAtom.getOccupancy(),
                    iterAtom.getBfactor(),
                    iterAtom.getX(), iterAtom.getY(), iterAtom.getZ(),
                    iterAtom.getIsHetero(), iterAtom.getAtomSerial(),
                    iterAtom.getPdbAtomRecord(),
                    iterAtom.getClientAtomReference());
    }

    {
      ModelAdapter.BondIterator iterBond =
        adapter.getBondIterator(clientFile);
      if (iterBond != null)
        while (iterBond.hasNext())
          frame.bondAtoms(iterBond.getAtomUid1(),
                          iterBond.getAtomUid2(),
                          iterBond.getOrder());
    }

    if (hasPdbRecords) {
      String[] structures =
        adapter.getPdbStructureRecords(clientFile);
      if (structures != null && structures.length > 0)
        frame.pdbFile.setStructureRecords(structures);
    }
      
    frame.freeze();
    long msToBuild = System.currentTimeMillis() - timeBegin;
    System.out.println("Build a frame:" + msToBuild + " ms");
    adapter.finish(clientFile);
    return frame;
  }
}
