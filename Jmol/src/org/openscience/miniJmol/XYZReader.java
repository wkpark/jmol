
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
package org.openscience.miniJmol;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import org.openscience.jmol.FortranFormat;

/**
 * XYZ files may contain multiple ChemFrame objects, and may have charges
 * and vector information contained along with atom types and coordinates.
 * XYZ files <em>must</em> have a number of atoms at the beginning of each
 * line then another line (which may be blank) to identify each frame.
 */
public class XYZReader implements ChemFileReader {

  /**
   * Creates an XYZ file reader.
   *
   * @param input source of XYZ data
   */
  public XYZReader(Reader input) {
    this.input = new BufferedReader(input, 1024);
  }

  /**
   * Read the XYZ file.
   */
  public ChemFile read(StatusDisplay putStatus, boolean bondsEnabled)
          throws IOException {

    int fr = 0;
    ChemFile file = new ChemFile(bondsEnabled);

    while (true) {
      fr++;
      ChemFrame cf = readFrame(putStatus, fr, bondsEnabled);
      if (cf == null) {
        break;
      }
      file.addFrame(cf);
      Enumeration propIter = cf.getFrameProps().elements();
      while (propIter.hasMoreElements()) {
        file.addProperty((String) propIter.nextElement());
      }
    }
    return file;
  }

  /**
   * Parses the next section of the XYZ file into a ChemFrame.
   */
  public ChemFrame readFrame(
          StatusDisplay putStatus, int frameNum, boolean bondsEnabled)
            throws IOException {

    int na = 0;
    String info = "";
    StringBuffer stat = new StringBuffer();
    String statBase = null;

    String line = input.readLine();
    if (line == null) {
      return null;
    }
    StringTokenizer st = new StringTokenizer(line, "\t ,;");
    String sn = st.nextToken();
    na = Integer.parseInt(sn);
    info = input.readLine();
    if (putStatus != null) {
      stat.append("Reading Frame ");
      stat.append(frameNum);
      stat.append(": ");
      statBase = stat.toString();
      stat.append("0 %");
      putStatus.setStatusMessage(stat.toString());
    }

    // OK, we got enough to start building a ChemFrame:
    ChemFrame cf = new ChemFrame(na, bondsEnabled);
    cf.setInfo(info);

    for (int i = 0; i < na; i++) {
      line = input.readLine();
      if (line == null) {
        break;
      }
      if (!line.startsWith("#")) {
        double x = 0.0f;
        double y = 0.0f;
        double z = 0.0f;
        double c = 0.0f;
        double vect[] = new double[3];
        st = new StringTokenizer(line, "\t ,;");
        boolean readcharge = false;
        boolean readvect = false;
        int nt = st.countTokens();
        switch (nt) {
        case 1 :
        case 2 :
        case 3 :
          throw new IOException(
                  "XYZFile.readFrame(): Not enough fields on line.");

        case 5 :     // atype, x, y, z, charge                    
          readcharge = true;
          break;

        case 7 :     // atype, x, y, z, vx, vy, vz
          readvect = true;
          break;

        case 8 :     // atype, x, y, z, charge, vx, vy, vz
          readcharge = true;
          readvect = true;
          break;

        default :    // 4, 6, or > 8  fields, just read atype, x, y, z
          break;
        }

        String aname = st.nextToken();
        String sx = st.nextToken();
        String sy = st.nextToken();
        String sz = st.nextToken();
        x = FortranFormat.atof(sx);
        y = FortranFormat.atof(sy);
        z = FortranFormat.atof(sz);

        Vector props = new Vector();
        if (readcharge) {
          String sc = st.nextToken();
          c = FortranFormat.atof(sc);
        }

        if (readvect) {
          String svx = st.nextToken();
          String svy = st.nextToken();
          String svz = st.nextToken();
          vect[0] = FortranFormat.atof(svx);
          vect[1] = FortranFormat.atof(svy);
          vect[2] = FortranFormat.atof(svz);
        }

        if (readcharge || readvect) {
          cf.addAtom(aname, (float) x, (float) y, (float) z, props);
        } else {
          cf.addAtom(aname, (float) x, (float) y, (float) z);
        }
      }
      if (putStatus != null) {
        stat.setLength(0);
        stat.append(statBase);
        if (na > 1) {
          stat.append((int) (100 * i / (na - 1)));
        } else {
          stat.append(100);
        }
        stat.append(" %");
        putStatus.setStatusMessage(stat.toString());
      }
    }
    return cf;
  }

  private BufferedReader input;
}
