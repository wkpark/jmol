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
package org.openscience.jmol.render;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.JmolClientAdapter;
import java.util.Iterator;
import javax.vecmath.Point3d;

public class JmolFrameBuilder {

  DisplayControl control;
  Object clientFile;
  int frameNumber;

  public JmolFrameBuilder(DisplayControl control,
                          Object clientFile, int frameNumber) {
    this.control = control;
    this.clientFile = clientFile;
    this.frameNumber = frameNumber;
  }

  public JmolFrame buildJmolFrame() {
    JmolClientAdapter adapter = control.getClientAdapter();
    System.out.println("JmolFrameBuilder.buildJmolFrame()");
    int atomCount = adapter.getAtomCount(clientFile, frameNumber);
    boolean hasPdbRecords = adapter.hasPdbRecords(clientFile, frameNumber);

    JmolFrame frame = new JmolFrame(control, atomCount, hasPdbRecords);
    for (Iterator iterAtom = adapter.getAtomIterator(clientFile, frameNumber);
         iterAtom.hasNext(); ) {
      frame.addAtom(iterAtom.next());
    }

    {
      JmolClientAdapter.BondIterator iterCovalent =
        adapter.getCovalentBondIterator(clientFile, frameNumber);
      if (iterCovalent != null)
        while (iterCovalent.hasNext()) {
          iterCovalent.next();
          frame.bondAtomShapes(iterCovalent.getAtom1(),
                               iterCovalent.getAtom2(),
                               iterCovalent.getOrder());
        }
    }

    {
      JmolClientAdapter.BondIterator iterAssoc =
        adapter.getAssociationBondIterator(clientFile, frameNumber);
      if (iterAssoc != null)
        while (iterAssoc.hasNext()) {
          iterAssoc.next();
          frame.bondAtomShapes(iterAssoc.getAtom1(),
                               iterAssoc.getAtom2(),
                               iterAssoc.getOrder());
        }
    }
      
    {
      JmolClientAdapter.LineIterator iterVector =
        adapter.getVectorIterator(clientFile, frameNumber);
      if (iterVector != null)
        while (iterVector.hasNext()) {
          iterVector.next();
          frame.addLineShape(new ArrowLineShape(iterVector.getPoint1(),
                                                iterVector.getPoint2()));
        }
    }

    {
      JmolClientAdapter.LineIterator iterCell =
        adapter.getCrystalCellIterator(clientFile, frameNumber);
      if (iterCell != null)
        for (int i = 0; iterCell.hasNext(); ++i) {
          iterCell.next();
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
