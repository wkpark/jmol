/*
 * Copyright 2002 The Jmol Development Team
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

import java.io.Reader;
import java.io.IOException;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.Crystal;
import org.openscience.cdk.libio.jmol.Convertor;

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
  public ShelXReader(Reader input) {
    super(input);
    cdkreader = new org.openscience.cdk.io.ShelXReader(input);
  }

  public org.openscience.jmol.ChemFile read() throws IOException {
  
    org.openscience.jmol.ChemFile file = new org.openscience.jmol.ChemFile();

    try {
      org.openscience.cdk.ChemFile cf = (ChemFile)cdkreader.read(
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
            CrystalFile cfile = Convertor.convertCrystal(crystal);
            // dirty ! But CrystalFrame does not unit cell stuff
            return cfile;
          } else {
            // no crystal
            ac = chemModel.getAllInOneContainer();
          }

          // store read data
          if (ac != null) {
            ChemFrame frame = Convertor.convert(ac);
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
