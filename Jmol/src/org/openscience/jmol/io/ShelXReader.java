/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Project
 *
 * Contact: jmol-developers@lists.sourceforge.net
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
package org.openscience.jmol.io;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.ChemFile;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.CrystalFile;
import java.io.Reader;
import java.io.IOException;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.Crystal;
import org.openscience.cdk.tools.ChemModelManipulator;
import org.openscience.jmol.Convertor;

/**
 * CML files contain a single ChemFrame object.
 * @see ChemFrame
 */
public class ShelXReader extends DefaultChemFileReader {

    private org.openscience.cdk.io.ShelXReader cdkreader;

  /**
   * Creates a CML reader.
   *
   * @param input source of CML data
   */
  public ShelXReader(DisplayControl control, Reader input) {
    super(control, input);
    cdkreader = new org.openscience.cdk.io.ShelXReader(input);
  }

  public org.openscience.jmol.ChemFile read() throws IOException {
  
    org.openscience.jmol.ChemFile file =
      new org.openscience.jmol.ChemFile(control);

    try {
      org.openscience.cdk.ChemFile cf = (org.openscience.cdk.ChemFile)cdkreader.read(
        new org.openscience.cdk.ChemFile());

      AtomContainer ac;

      for (int seq = 0; seq < cf.getChemSequenceCount(); seq++) {
        ChemSequence chemSeq = cf.getChemSequence(seq);
        for (int model = 0; model < chemSeq.getChemModelCount(); model++) {
          ChemModel chemModel = chemSeq.getChemModel(model);
          Crystal crystal = chemModel.getCrystal();
          if (crystal != null) {
            // found crystal
            ac = crystal;
            CrystalFile cfile = Convertor.convertCrystal(control, crystal);
            // dirty ! But CrystalFrame does not unit cell stuff
            return cfile;
          } else {
            // no crystal
            ac = ChemModelManipulator.getAllInOneContainer(chemModel);
          }

          // store read data
          if (ac != null) {
            ChemFrame frame = Convertor.convert(control, ac);
            file.addFrame(frame);
            fireFrameRead();
          }
        }
      }
    } catch (Exception e) {
    }

    return file;
  }
}
