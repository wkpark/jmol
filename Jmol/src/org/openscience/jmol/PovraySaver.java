
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

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.io.*;

/**
 * Generates files for viewing in the freeware povray reaytracer
 * (http://www.povray.org)<p> The types of atoms and bonds is
 * controlled by PovrayStyleWriter
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 *  @author Thomas James Grey
 */
public class PovraySaver extends FileSaver {

  private DisplaySettings settings;
  private String viewMat = null;
  private String background = null;
  private PovrayStyleWriter myStyle;
  private int framenumber = 0;
  private Matrix4d amat, tmat, zmat, ttmat;
  private float xfac, edge;
  private float dx, dy, dz, scale;
  private int screenWidth, screenHeight;
  protected ChemFile cf;

  /**
   * Constructor.
   *
   * @param cf The <code>ChemFile</code> object that is being written.
   * @param out The <code>OutputStream</code> which will be written to.
   */
  public PovraySaver(ChemFile cf, OutputStream out) throws IOException {

    super(cf, out);

    //Hack hack- keep a pointer of our own as FileSaver's is private
    this.cf = cf;
    myStyle = new PovrayStyleWriter();
  }

  /**
   * Sets the number of the actual frame that is written out, by
   * default this is frame 1.
   *
   * @param framenumber THe frame number to write out.
   */
  public void setFramenumber(int framenumber) {
    this.framenumber = framenumber;
  }

  /**
   * Sets the background colour of the renderered scene to the
   * colour given.
   *
   * @param bgColor The background color.
   */
  public void setBackgroundColor(java.awt.Color bgColor) {
    background = povrayColor(bgColor);
  }

  /**
   * Set the <code>DisplaySettings</code> object for the saver.
   *
   * @param settings The <code>DisplaySettings</code> object to be set.
   */
  public void setSettings(DisplaySettings settings) {
    this.settings = settings;
  }

  /**
   * Sets the rotation matrix
   *
   * @param amat The rotation matrix to be set.
   */
  public void setAmat(Matrix4d amat) {
    this.amat = amat;

  }

  /**
   * Sets the translation matrix.
   *
   * @param tmat The translation matrix to be set.
   */
  public void setTmat(Matrix4d tmat) {
    this.tmat = tmat;
    ttmat = new Matrix4d(tmat);

  }

  /**
   * Sets the Zoom Matrix.
   *
   * @param zmat The Zoom matrix to be set.
   */
  public void setZmat(Matrix4d zmat) {
    this.zmat = zmat;

  }

  /**
   * Sets the xfac to scale the translation.
   *
   * @param xfac The translation scaling factor.
   */
  public void setXfac(float xfac) {
    this.xfac = xfac;
  }

  /**
   * Sets the output size of the picture.
   *
   * @param width The width of the picture in pixels.
   * @param height The Height of the picture in pixels.
   */
  public void setSize(int width, int height) {
    this.screenWidth = width;
    this.screenHeight = height;
  }


  /**
   * Sets the style controller for this file to that given. Style
   * controllers must subclass PovrayStyleWriter and control the
   * appearence of the atoms and bonds.
   *
   * @param style The <code>PovrayStyleWriter</code> that will
   *              control how the atoms and bonds appear.
   */
  public void setStyleController(PovrayStyleWriter style) {
    myStyle = style;
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

    cf.findBB();
    dx = (cf.getXMax() - cf.getXMin());
    dy = (cf.getXMax() - cf.getYMin());
    dz = (cf.getZMax() - cf.getZMin());

    Vector3d tvect = new Vector3d();
    ttmat.get(tvect);
    tvect.x /= xfac;
    tvect.y /= -xfac;
    tvect.z /= xfac;
    ttmat.set(tvect);

    edge = dx;
    if (edge < dy) {
      edge = dy;
    }
    if (edge < dz) {
      edge = dz;
    }

    edge /= Math.pow(zmat.getScale(), 2);
    edge /= 0.7;

    myStyle.setAmat(amat);
    myStyle.setTmat(ttmat);

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
    w.write("#declare Width = " + screenWidth + ";\n");
    w.write("#declare Height = " + screenHeight + ";\n");
    w.write("#declare Ratio = Width / Height\n" + "\n");
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

    if (background != null) {
      w.write("background { color " + background + " }\n");
      w.write("\n");
    }


    w.write("light_source { < 0, 0, zoom> " + " rgb <1.0,1.0,1.0> }\n");
    w.write("light_source { < -zoom, zoom, zoom> " + " rgb <1.0,1.0,1.0> }\n");
    w.write("\n");
    w.write("\n");

    myStyle.writeAtomsAndBondsMacros(w, cf, settings.getAtomSphereFactor(),
            settings.getBondWidth());

    String st = "";
    boolean writevect = false;

    boolean drawHydrogen = settings.getShowHydrogens();


    try {

      if (settings.getShowAtoms()) {

        w.write("//***********************************************\n");
        w.write("// List of all of the atoms\n");
        w.write("//***********************************************\n");
        w.write("\n");


        Vector fp = cf.getFrameProps();

        // Create some dummy properties:
        double[] vect = new double[3];
        vect[0] = 0.0;
        vect[1] = 0.0;
        vect[2] = 0.0;
        VProperty vp = new VProperty(vect);

        // test if we have vectors in this frame:
        for (int i = 0; i < fp.size(); i++) {
          String prop = (String) fp.elementAt(i);
          if (prop.equals(vp.getDescriptor())) {
            writevect = true;
          }
        }

        // Loop through the atoms and write them out:

        boolean write_out = true;


        for (int i = 0; i < cf.getNvert(); i++) {

          // don't write out if atom is a hydrogen and !showhydrogens

          if (!drawHydrogen
                  && (cf.getAtomAt(i).getBaseAtomType().getAtomicNumber()
                    == 1)) {

            // atom is a hydrogen and should not be written

            write_out = false;
          }

          if (write_out) {
            myStyle.writeAtom(w, i, cf);
          }

          write_out = true;


          /*
            Vector props = cf.getVertProps(i);

            if (writevect) {
            for (int j = 0; j < props.size(); j++) {
            PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
            String desc = p.getDescriptor();
            if (desc.equals(vp.getDescriptor())) {
            VProperty vt = (VProperty) p;
            double[] vtmp;
            vtmp = vt.getVector();
            st = st + tab +
            new Double(vtmp[0]).toString() + tab +
            new Double(vtmp[1]).toString() + tab +
            new Double(vtmp[2]).toString();
            }
            }
            }
            st = st + "\n";
            w.write(st, 0, st.length());
          */
        }
      }

      /* write the bonds */

      if (settings.getShowBonds()) {

        boolean[] bond_drawn = new boolean[cf.getBondCount()];

        for (int i = 0; i < cf.getBondCount(); i++) {
          bond_drawn[i] = false;
        }


        w.write("\n");
        w.write("\n");
        w.write("//***********************************************\n");
        w.write("// The list of bonds\n");
        w.write("//***********************************************\n");
        w.write("\n");

        for (int i = 0; i < cf.getNvert(); i++) {

          int na = cf.getNumberOfBondsForAtom(i);
          for (int k = 0; k < na; k++) {
            int which = cf.getOtherBondedAtom(i, k);

            if (!drawHydrogen && cf.getBondAt(which).bindsHydrogen()) {

              /*bond contains hydrogen, and shouldn't be
                written, therefore we'll say its already been
                written */
              bond_drawn[which] = true;
            }
            if (!bond_drawn[which]) {
              myStyle.writeBond(w, which, cf);
              bond_drawn[which] = true;
            }
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
  protected String povrayColor(java.awt.Color col) {
    float tff = (float) 255.0;
    return "rgb<" + ((float) col.getRed() / tff) + ","
            + ((float) col.getGreen() / tff) + ","
              + ((float) col.getBlue() / tff) + ">";
  }
}
