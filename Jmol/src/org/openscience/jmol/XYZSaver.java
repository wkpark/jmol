
/*
 * Copyright 2001 The Jmol Development Team
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

import javax.vecmath.Point3f;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class XYZSaver extends FileSaver {

  /**
   * Constructor.
   * @param cf the ChemFile to dump.
   * @param out the stream to write the XYZ file to.
   */
  public XYZSaver(ChemFile cf, OutputStream out) throws IOException {
    super(cf, out);
  }

  public void writeFileStart(ChemFile cf, BufferedWriter w)
      throws IOException {

    // No preamble for XYZ Files
  }

  public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException {

    // No postamble for XYZ Files
  }

  /**
   * writes a single frame in XYZ format to the Writer.
   * @param cf the ChemFrame to write
   * @param w the Writer to write it to
   */
  public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {

    int na = 0;
    String info = "";
    String st = "";
    boolean writecharge = cf.hasAtomProperty(Charge.DESCRIPTION);

    try {

      String s1 = new Integer(cf.getNumberOfAtoms()).toString();
      w.write(s1, 0, s1.length());
      w.newLine();

      String s2 = cf.getInfo();
      if (s2 != null) {
        w.write(s2, 0, s2.length());
      }
      w.newLine();

      // Loop through the atoms and write them out:

      for (int i = 0; i < cf.getNumberOfAtoms(); i++) {

        Atom a = cf.getAtomAt(i);
        st = a.getType().getName();

        double[] pos = cf.getAtomCoords(i);
        st = st + "\t" + new Double(pos[0]).toString() + "\t"
            + new Double(pos[1]).toString() + "\t"
              + new Double(pos[2]).toString();

        if (writecharge) {
          Charge ct = (Charge) a.getProperty(Charge.DESCRIPTION);
          st = st + "\t" + ct.stringValue();
        }

        if (a.getVector() != null) {
          Point3f vector = a.getVector();
          st = st + "\t" + vector.x + "\t" + vector.y + "\t" + vector.z;
        }
        st = st;
        w.write(st, 0, st.length());
        w.newLine();

      }

    } catch (IOException e) {
      throw e;
    }

  }
}


