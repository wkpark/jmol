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

import org.openscience.jmol.ChemFile;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.Atom;
import org.openscience.jmol.viewer.JmolViewer;
import java.util.Date;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Generates files for viewing in the freeware povray raytracer
 * (http://www.povray.org).
 *
 * <p> The types of atoms and bonds is
 * controlled by PovrayStyleWriter
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 *  @author Thomas James Grey
 */
public class PovraySaver extends FileSaver {

  private JmolViewer viewer;
  private PovrayStyleWriter style;
  private int framenumber = 0;
  
  private double edge;
  protected ChemFile cf;

  /**
   * Constructor.
   *
   * @param cf The <code>ChemFile</code> object that is being written.
   * @param out The <code>OutputStream</code> which will be written to.
   */
  public PovraySaver(ChemFile cf, OutputStream out, PovrayStyleWriter style,
                     JmolViewer viewer) throws IOException {

    super(cf, out);

    //Hack hack- keep a pointer of our own as FileSaver's is private
    this.cf = cf;

    this.style = style;
    this.viewer = viewer;
  }

  public void writeFileStart(ChemFile cf, BufferedWriter w)
      throws IOException {

    // POvray files don't work like this! Each frame is a separate file so this method is redundant.
  }

  public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException {

    //Again this is meaningless as each frame is a file.
  }

  /**
   * Writes a single frame in povray format to the Writer.
   *
   * @param cf the ChemFrame to write
   * @param w the Writer to write it to
   */
  public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {
    edge = viewer.getJmolFrame().getRotationRadius() * 2;
    edge *= 1.1; // for some reason I need a little more margin
    edge /= viewer.getZoomPercent() / 100.0;

    style.setRotate(viewer.getPovRotateMatrix());
    style.setTranslate(viewer.getPovTranslateMatrix());

    Date now = new Date();
    SimpleDateFormat sdf =
      new SimpleDateFormat("EEE, MMMM dd, yyyy 'at' h:mm aaa");

    String now_st = sdf.format(now);

    w.write("//******************************************************\n");
    w.write("// Jmol generated povray script.\n");
    w.write("//\n");
    w.write("// This script was generated on :\n");
    w.write("// " + now_st + "\n");
    String s2 = cf.getInfo();
    if (s2 != null) {
      w.write("//\n");
      w.write("// Frame comment:" + s2 + "\n");
    }
    w.write("//******************************************************\n");
    w.write("\n");
    w.write("\n");
    w.write("//******************************************************\n");
    w.write("// Declare the resolution, camera, and light sources.\n");
    w.write("//******************************************************\n");
    w.write("\n");
    w.write("// NOTE: if you plan to render at a different resoltion,\n");
    w.write("// be sure to update the following two lines to maintain\n");
    w.write("// the correct aspect ratio.\n" + "\n");
    w.write("#declare Width = "+ viewer.getScreenDimension().width + ";\n");
    w.write("#declare Height = "+ viewer.getScreenDimension().height + ";\n");
    w.write("#declare Ratio = Width / Height;\n");
    w.write("#declare zoom = " + edge + ";\n\n");
    w.write("camera{\n");
    w.write("  location < 0, 0, zoom>\n" + "\n");
    w.write("  // Ratio is negative to switch povray to\n");
    w.write("  // a right hand coordinate system.\n");
    w.write("\n");
    w.write("  right < -Ratio , 0, 0>\n");
    w.write("  look_at < 0, 0, 0 >\n");
    w.write("}\n");
    w.write("\n");

    w.write("background { color " +
            povrayColor(viewer.getColorBackground()) + " }\n");
    w.write("\n");

    w.write("light_source { < 0, 0, zoom> " + " rgb <1.0,1.0,1.0> }\n");
    w.write("light_source { < -zoom, zoom, zoom> "
        + " rgb <1.0,1.0,1.0> }\n");
    w.write("\n");
    w.write("\n");

    style.writeAtomsAndBondsMacros(w, cf, viewer.getPercentVdwAtom() / 100.0,
                                   viewer.getMarBond() / 1000.0);

    boolean drawHydrogen = viewer.getShowHydrogens();


    try {

      w.write("//***********************************************\n");
      w.write("// List of all of the atoms\n");
      w.write("//***********************************************\n");
      w.write("\n");
      
      // Loop through the atoms and write them out:
      
      for (int i = 0; i < cf.getAtomCount(); i++) {
        // don't write out if atom is a hydrogen and !showhydrogens
        if (drawHydrogen
            || (cf.getJmolAtomAt(i).getAtomicNumber() != 1)) {
          style.writeAtom(w, i, cf);
        }
      }

      /* write the bonds */

      
      Hashtable bondsDrawn = new Hashtable();
      
      w.write("\n");
      w.write("\n");
      w.write("//***********************************************\n");
      w.write("// The list of bonds\n");
      w.write("//***********************************************\n");
      w.write("\n");
      
      for (int i = 0; i < cf.getAtomCount(); ++i) {
        Atom atom1 = (org.openscience.jmol.Atom)cf.getAtomAt(i);
        Atom[] bondedAtoms = atom1.getBondedAtoms();
        if (bondedAtoms == null)
          continue;
        for (int j = 0; j < bondedAtoms.length; ++j) {
          Atom atom2 = (Atom) bondedAtoms[j];
          if ((bondsDrawn.get(atom2) == null)
              || !bondsDrawn.get(atom2).equals(atom1)) {
            style.writeBond(w, atom1, atom2, cf);
            bondsDrawn.put(atom1, atom2);
          }
        }
      }

    } catch (IOException e) {
      throw e;
    }

  }

  /**
   * The default implemention of this method writes all frames to
   * one file- we override to open a new file for each frame.
   *
   * @throws IOException
   */
  public synchronized void writeFile() throws IOException {

    try {
      BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out),
                           1024);

      //System.out.println("MARK "+framenumber);
      ChemFrame cfr = cf.getFrame(framenumber);
      writeFrame(cfr, w);
      w.flush();
      w.close();
      out.flush();
      out.close();
    } catch (IOException e) {
      System.out.println("Got IOException " + e + " trying to write frame.");
    }
  }

  /**
   * Takes a java colour and returns a String representing the
   * colour in povray eg 'rgb<1.0,0.0,0.0>'
   *
   * @param col The color to convert
   *
   * @return A string representaion of the color in povray rgb format.
   */
  protected String povrayColor(Color color) {
    return "rgb<" +
      color.getRed() / 255f + "," +
      color.getGreen() / 255f + "," +
      color.getBlue() / 255f + ">";
  }
}
