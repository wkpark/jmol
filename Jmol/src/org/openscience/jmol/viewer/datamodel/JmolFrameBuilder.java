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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;
import javax.vecmath.Point3d;

public class JmolFrameBuilder {

  JmolViewer viewer;
  Object clientFile;
  int frameNumber;

  public JmolFrameBuilder(JmolViewer viewer,
                          Object clientFile, int frameNumber) {
    this.viewer = viewer;
    this.clientFile = clientFile;
    this.frameNumber = frameNumber;
  }

  public JmolFrame buildJmolFrame() {
    JmolModelAdapter adapter = viewer.getJmolModelAdapter();
    System.out.println("JmolFrameBuilder.buildJmolFrame()");
    int atomCount = adapter.getAtomCount(clientFile, frameNumber);
    boolean hasPdbRecords = adapter.hasPdbRecords(clientFile, frameNumber);

    JmolFrame frame = new JmolFrame(viewer, atomCount, hasPdbRecords);
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
          frame.bondAtomShapes(iterCovalent.getAtom1(),
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
          frame.bondAtomShapes(iterAssoc.getAtom1(),
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
          frame.addLineShape(new ArrowLineShape(iterVector.getPoint1(),
                                                iterVector.getPoint2()));
        }
    }

    {
      JmolModelAdapter.LineIterator iterCell =
        adapter.getCrystalCellIterator(clientFile, frameNumber);
      if (iterCell != null)
        for (int i = 0; iterCell.hasNext(); ++i) {
          iterCell.moveNext();
          LineShape line;
          if (i < 3)
            line = new ArrowLineShape(iterCell.getPoint1(),
                                      iterCell.getPoint2());
          else
            line = new LineShape(iterCell.getPoint1(), iterCell.getPoint2());
          frame.addCrystalCellLine(line);
        }
    }

    frame.finalize();
    return frame;
  }
}
