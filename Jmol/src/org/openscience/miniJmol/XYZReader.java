
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
import javax.swing.event.EventListenerList;

/**
 * A reader for XYZ Cartesian molecular model (XMol) files.
 * XMol is a closed source program similar in scope to Jmol.
 * Details on XMol are available at http://www.msc.edu/docs/xmol/
 *
 * <p> XYZ files reference molecular geometris using a simple
 * cartesian coordinate system. Each XYZ file can contain multiple
 * frames for the purposes of animation.  Each frame in the animation
 * is represented by a two line header, followed by one line for each atom.
 *
 * <p> The first line of a frame's header is the number of atoms in
 * that frame.  Only the integer is read, it may be preceded by white
 * space, and anything on the line after the integer is ignored.
 *
 * <p> The second line of the header is the "info" string for the
 * frame.  The info line may be blank, or it may contain information
 * pertinent to that step, but it must exist, and it may only be one
 * line long.
 *
 * <p> Each line describing a single atom contains 4, 5, 7, 8, or
 * possibly more fields separated by white space.  The first 4 fields
 * are always the same: the atom's type (a short string of
 * alphanumeric characters), and its x-, y-, and z-positions.
 * Optionally, extra fields may be used to specify a charge for the
 * atom, and/or a vector associated with the atoms.  If an input line
 * contains five or eight fields, the fifth field is interpreted as
 * the atom's charge; otherwise, a charge of zero is assumed.  If an
 * input line contains seven or eight fields, the last three fields
 * are interpreted as the components of a vector.  These components
 * should be specified in angstroms.  If there are more than eight
 * fields, only the first 4 are parsed by the reader, and all
 * additional fields are ignored.
 *
 * <p>The XYZ format contains no connectivity information.  Jmol
 * attempts to generate connectivity information using the covalent
 * radii of the specified atomic types.  If the distance between two
 * atoms is less than the sum of their covalent radii (times a fudge
 * factor), they are considered bonded.
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Network Computing Services, Inc. (the authors of XMol).
 * If you have problems, please contact the author of this code, not
 * the developers of XMol.
 *
 * @author J. Daniel Gezelter (gezelter.1@nd.edu)
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class XYZReader implements ChemFileReader {

  /**
   * Create an XYZ output reader.
   *
   * @param input source of XYZ data
   */
  public XYZReader(Reader input) {
    this.input = new BufferedReader(input);
  }

  /**
   * Whether bonds are enabled in the files and frames read.
   */
  private boolean bondsEnabled = true;
  
  /**
   * Sets whether bonds are enabled in the files and frames which are read.
   *
   * @param bondsEnabled if true, enables bonds.
   */
  public void setBondsEnabled(boolean bondsEnabled) {
    this.bondsEnabled = bondsEnabled;
  }
  
  /**
   * Read the XYZ output.
   *
   * @return a ChemFile with the coordinates, charges, vectors, etc.
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);

    while (true) {
      ChemFrame cf = readFrame();
      if (cf == null) {
        break;
      }
      file.addFrame(cf);
    }
    return file;
  }

  /**
   * Parses the next section of the XYZ file into a ChemFrame.
   */
  public ChemFrame readFrame() throws IOException {

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
 
    }
    fireFrameRead();
    return cf;
  }

  /**
   * Holder of reader event listeners.
   */
  private Vector listenerList = new Vector();
  
  /**
   * An event to be sent to listeners. Lazily initialized.
   */
  private ReaderEvent readerEvent = null;
  
  /**
   * Adds a reader listener.
   *
   * @param l the reader listener to add.
   */
  public void addReaderListener(ReaderListener l) {
    listenerList.addElement(l);
  }
  
  /**
   * Removes a reader listener.
   *
   * @param l the reader listener to remove.
   */
  public void removeReaderListener(ReaderListener l) {
    listenerList.remove(l);
  }
  
  /**
   * Sends a frame read event to the reader listeners.
   */
  private void fireFrameRead() {
    for (int i = 0; i < listenerList.size(); ++i) {
      ReaderListener listener = (ReaderListener) listenerList.elementAt(i);
      // Lazily create the event:
      if (readerEvent == null) {
        readerEvent = new ReaderEvent(this);
      }
      listener.frameRead(readerEvent);
    }
  }
 
  private BufferedReader input;
}
