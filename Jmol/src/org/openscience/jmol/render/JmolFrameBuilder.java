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

  private JmolFrame buildJmolFrame() {
    int atomCount = control.getAtomCount(clientFile, frameNumber);
    boolean hasPdbRecords = control.hasPdbRecords(clientFile, frameNumber);

    JmolFrame frame = new JmolFrame(control, atomCount, hasPdbRecords);
    for (Iterator iterAtom = control.getAtomIterator(clientFile, frameNumber);
         iterAtom.hasNext(); ) {
      frame.addAtom(iterAtom.next());
    }

    for (Iterator iterBond = control.getCovalentBondIterator(clientFile,
                                                             frameNumber);
         iterBond.hasNext(); ) {
      Object clientBond = iterBond.next();
      //      frame.bondAtomShapes(control.getCovalentBondAtom1(clientBond),
      //                   control.getCovalentBondAtom2(clientBond),
      //                   control.getCovalentBondOrder(clientBond));
    }

    for (Iterator iterAssoc = control.getAssociationIterator(clientFile,
                                                             frameNumber);
         iterAssoc.hasNext(); ) {
      Object clientAssoc = iterAssoc.next();
      //      frame.bondAtomShapes(control.getAssociationAtom1(clientAssoc),
      //                   control.getAssociationAtom2(clientAssoc),
      //                   control.getAssociationType(clientAssoc));
    }
      

    for (Iterator iterVector = control.getVectorIterator(clientFile,
                                                         frameNumber);
         iterVector.hasNext(); ) {
      Object clientVector = iterVector.next();
      // Point3d pointOrigin = control.getVectorOrigin(clientVector);
      // Point3d pointVector = control.getVectorVector(clientVector);
      // frame.addLineShape(new ArrowLineShape(pointOrigin, pointVector));
    }
      

    for (Iterator iterCell = control.getCrystalCellIterator(clientFile,
                                                            frameNumber);
         iterCell.hasNext(); ) {
      Object clientCell = iterCell.next();
      // Point3d point1 = control.getCrystalCellPoint1(clientCell);
      // Point3d point2 = control.getCrystalCellPoint2(clientCell);
      // LineShape ls = new LineShape(point1, point2);
      // frame.addCrystalCellLine(ls);
    }

    /*
      if (this instanceof CrystalFrame) {
      CrystalFrame crystalFrame = (CrystalFrame) this;
      double[][] rprimd = crystalFrame.getRprimd();
      
      // The three primitives vectors with arrows
      for (int i = 0; i < 3; i++)
        jmframe.addLineShape(new ArrowLineShape(zeroPoint,
                                                new Point3d(rprimd[i])));
    */
    frame.finalize();
    return frame;
  }
}
