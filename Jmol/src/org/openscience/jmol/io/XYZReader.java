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
package org.openscience.jmol.io;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.ChemFile;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.FortranFormat;
import org.openscience.cdk.Atom;
import java.util.StringTokenizer;
import javax.vecmath.Point3d;
import java.io.Reader;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.IOException;


/**
 * A reader for XYZ Cartesian molecular model (XMol) files.
 * XMol is a closed source program similar in scope to Jmol.
 * Details on XMol were once available at http://www.msc.edu/docs/xmol/.
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
 * <p> An extension has been made to the format to allow specifying coordinates
 * in Bohr. If the first line contains the word "Bohr", the coorinates are
 * converted from Bohr to Angstroms.
 *
 * @author J. Daniel Gezelter (gezelter.1@nd.edu)
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class XYZReader extends DefaultChemFileReader {

  /**
   * Scaling factor for converting atomic coordinates from
   * units of Bohr to Angstroms.
   */
  public static final double angstromPerBohr = 0.529177249;

  /**
   * Create an XYZ output reader.
   *
   * @param input source of XYZ data
   */
  public XYZReader(DisplayControl control, Reader input) {
    super(control, input);
  }

  /**
   * Read the XYZ output.
   *
   * @return a ChemFile with the coordinates, charges, vectors, etc.
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(control, bondsEnabled);

    int na = 0;
    String info = "";
    StringTokenizer st;

    String line = input.readLine();
    while (input.ready() && (line != null)) {
      st = new StringTokenizer(line, "\t ,;");

      String sn = st.nextToken();
      na = Integer.parseInt(sn);
      boolean readingBohr = false;
      if (st.hasMoreTokens()) {
        if ("Bohr".equalsIgnoreCase(st.nextToken())) {
          readingBohr = true;
        }
      }
      info = input.readLine();
      System.out.println(info);

      ChemFrame frame = new ChemFrame(control, na);
      frame.setInfo(info);

      for (int i = 0; i < na; i++) {
        String s = input.readLine();
        if (s == null) {
          break;
        }
        if (!s.startsWith("#")) {
            // System.out.println(s);
          double x = 0.0;
          double y = 0.0;
          double z = 0.0;
          st = new StringTokenizer(s, "\t ,;");
          int numberTokens = st.countTokens();

          String aname = st.nextToken();
          String sx = st.nextToken();
          String sy = st.nextToken();
          String sz = st.nextToken();

          x = convertToDouble(sx, readingBohr);
          y = convertToDouble(sy, readingBohr);
          z = convertToDouble(sz, readingBohr);

          org.openscience.cdk.Atom atom = new org.openscience.cdk.Atom(aname);
          atom.setX3D(x);
          atom.setY3D(y);
          atom.setZ3D(z);
          // System.out.println("Storing atom " + frame.getAtomCount() + ": " + atom.toString());
          frame.addAtom(atom);
          int atomIndex = frame.getAtomCount();

          if ((numberTokens == 5) || (numberTokens > 7)) {
            double c = FortranFormat.atof(st.nextToken());
            frame.getAtomAt(atomIndex-1).setCharge(c);
          }

          if (numberTokens >= 7) {
            double vect[] = new double[3];
            vect[0] = FortranFormat.atof(st.nextToken());
            vect[1] = FortranFormat.atof(st.nextToken());
            vect[2] =  FortranFormat.atof(st.nextToken());
            ((org.openscience.jmol.Atom)frame.getAtomAt(atomIndex-1)).
                setVector(new Point3d(vect));
          }

        }
      }
      frame.rebond();
      file.addFrame(frame);
      fireFrameRead();
      line = input.readLine();
    }
    return file;
  }
  
  private double convertToDouble(String numberString, boolean readingBohr) {
    double result = FortranFormat.atof(numberString);
    if (readingBohr) {
      result *= angstromPerBohr;
    }
    return result;
  }
  
}
