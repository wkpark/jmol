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

public class FrameBuilder {

  JmolViewer viewer;
  Object clientFile;
  int frameNumber;

  public FrameBuilder(JmolViewer viewer,
                          Object clientFile, int frameNumber) {
    this.viewer = viewer;
    this.clientFile = clientFile;
    this.frameNumber = frameNumber;
  }

  public Frame buildFrame() {
    long timeBegin = System.currentTimeMillis();
    JmolModelAdapter adapter = viewer.getJmolModelAdapter();
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
    frame.setCrystalScaleMatrix(adapter.getCrystalScaleMatrix(clientFile,
                                                              frameNumber));
    frame.setCrystalScaleTranslate(adapter.
                                   getCrystalScaleTranslate(clientFile,
                                                            frameNumber));


    for (JmolModelAdapter.AtomIterator iterAtom =
           adapter.getAtomIterator(clientFile, frameNumber);
         iterAtom.hasNext(); ) {
      frame.addAtom(iterAtom.next());
    }

    {
      JmolModelAdapter.BondIterator iterCovalent =
        adapter.getCovalentBondIterator(clientFile, frameNumber);
      if (iterCovalent != null)
        while (iterCovalent.hasNext()) {
          iterCovalent.moveNext();
          frame.bondAtoms(iterCovalent.getAtom1(),
                               iterCovalent.getAtom2(),
                               iterCovalent.getOrder());
        }
    }

    {
      JmolModelAdapter.BondIterator iterAssoc =
        adapter.getAssociationBondIterator(clientFile, frameNumber);
      if (iterAssoc != null)
        while (iterAssoc.hasNext()) {
          iterAssoc.moveNext();
          frame.bondAtoms(iterAssoc.getAtom1(),
                          iterAssoc.getAtom2(),
                          iterAssoc.getOrder());
        }
    }

    {
      JmolModelAdapter.LineIterator iterVector =
        adapter.getVectorIterator(clientFile, frameNumber);
      if (iterVector != null)
        while (iterVector.hasNext()) {
          iterVector.moveNext();
	  Point3f point1 = new Point3f(iterVector.getPoint1X(),
				       iterVector.getPoint1Y(),
				       iterVector.getPoint1Z());
	  Point3f point2 = new Point3f(iterVector.getPoint2X(),
				       iterVector.getPoint2Y(),
				       iterVector.getPoint2Z());
          frame.addLineShape(new Line(point1, point2, true));
        }
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
    return frame;
  }
}
