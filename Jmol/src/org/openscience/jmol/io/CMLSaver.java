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
import java.util.Vector;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  Egon Willighagen
 */
public class CMLSaver extends FileSaver {

  private final String CHARGEDESCRIPTION = "Atomic Charge";

  private static int MODEL = 1;
  private static int ANIMATION = 2;

  private int mode;
  private int frame_count;
  private int frames;

  /**
   * Constructor.
   * @param cf the ChemFile to dump.
   * @param out the stream to write the XYZ file to.
   */
  public CMLSaver(ChemFile cf, OutputStream out) throws IOException {
    super(cf, out);
    mode = MODEL;
    frame_count = 0;
    frames = cf.getNumberOfFrames();
  }

  public synchronized void writeFile() throws IOException {

    if (frames > 1) {
      System.out.println("Mode: ANIMATION");
      mode = ANIMATION;
    }
    super.writeFile();
  }

  public void writeFileStart(ChemFile cf, BufferedWriter w)
      throws IOException {

    // Preamble for CML Files
    w.write("<?xml version=\"1.0\" ?>");
    w.newLine();

    if (mode == ANIMATION) {
      w.write("<list convention=\"");
      if (mode == ANIMATION) {
        w.write("JMOL-ANIMATION");
      } else {
        w.write("JMOL-MODEL");
      }
      w.write("\">");
      w.newLine();
    }
  }

  public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException {
    if (mode == ANIMATION) {
      w.write("</list>");
    }
  }

  /**
   * writes a single frame in CML format to the Writer.
   * @param cf the ChemFrame to write
   * @param w the Writer to write it to
   */
  public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {

    boolean writecharge = cf.hasAtomProperty(CHARGEDESCRIPTION);

    frame_count++;

    w.write("<molecule id=\"FRAME" + frame_count + "\">");
    w.newLine();

    String name = cf.getInfo();
    if (name == null) {
      name = "unknown";
    }
    w.write("  <string title=\"COMMENT\">" + name + "</string>");
    w.newLine();

    StringBuffer ids = new StringBuffer();
    StringBuffer elementTypes = new StringBuffer();
    StringBuffer x3s = new StringBuffer();
    StringBuffer y3s = new StringBuffer();
    StringBuffer z3s = new StringBuffer();
    StringBuffer formalCharges = new StringBuffer();
    String lineSeparator = System.getProperty("line.separator");

    int count = 0;

    for (int i = 0; i < cf.getAtomCount(); i++) {

      if (ids.length() > 0) {
        ids.append(" ");
      }
      ids.append("a");
      ids.append(Integer.toString(i));

      Atom a = (org.openscience.jmol.Atom)cf.getAtomAt(i);
      if (elementTypes.length() > 0) {
        elementTypes.append(" ");
      }
      elementTypes.append(a.getSymbol());

      double[] pos = cf.getAtomCoords(i);
      if (x3s.length() > 0) {
        x3s.append(" ");
      }
      if (y3s.length() > 0) {
        y3s.append(" ");
      }
      if (z3s.length() > 0) {
        z3s.append(" ");
      }
      x3s.append(new Double(pos[0]).toString());
      y3s.append(new Double(pos[1]).toString());
      z3s.append(new Double(pos[2]).toString());

      ++count;
      if ((count == 5) && (i + 1 < cf.getAtomCount())) {
        count = 0;
        x3s.append(lineSeparator);
        x3s.append("     ");
        y3s.append(lineSeparator);
        y3s.append("     ");
        z3s.append(lineSeparator);
        z3s.append("     ");
      }

      if (writecharge) {
        if (formalCharges.length() > 0) {
          formalCharges.append(" ");
        }
        double ct = cf.getAtomAt(i).getCharge();
        formalCharges.append(ct);
      }
    }
    w.write("  <atomArray>");
    w.newLine();
    w.write("    <stringArray builtin=\"id\">");
    w.newLine();
    w.write("      " + ids + "");
    w.newLine();
    w.write("    </stringArray>");
    w.newLine();
    w.write("    <stringArray builtin=\"elementType\">");
    w.newLine();
    w.write("      " + elementTypes + "");
    w.newLine();
    w.write("    </stringArray>");
    w.newLine();
    w.write("    <floatArray builtin=\"x3\">");
    w.newLine();
    w.write("      " + x3s + "");
    w.newLine();
    w.write("    </floatArray>");
    w.newLine();
    w.write("    <floatArray builtin=\"y3\">");
    w.newLine();
    w.write("      " + y3s + "");
    w.newLine();
    w.write("    </floatArray>");
    w.newLine();
    w.write("    <floatArray builtin=\"z3\">");
    w.newLine();
    w.write("      " + z3s + "");
    w.newLine();
    w.write("    </floatArray>");
    w.newLine();
    if (writecharge) {
      w.write("    <floatArray builtin=\"formalCharge\">");
      w.newLine();
      w.write("      " + formalCharges + "");
      w.newLine();
      w.write("    </floatArray>");
      w.newLine();
    }
    w.write("  </atomArray>");
    w.newLine();
    w.write("</molecule>");
    w.newLine();
  }
}
