
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
package org.openscience.jmol.io;

import org.openscience.jmol.ChemFile;
import org.openscience.jmol.CrystalFile;
import java.util.Vector;
import java.util.StringTokenizer;
import java.lang.reflect.Array;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;

/**
 * Abinit summary (www.abinit.org):
 *   ABINIT is a package whose main program allows one to find the total
 *   energy, charge density and electronic structure of systems made of
 *   electrons and nuclei (molecules and periodic solids) within
 *   Density Functional Theory (DFT), using pseudopotentials and a
 *   planewave basis. ABINIT also includes options to optimize the
 *   geometry according to the DFT forces and stresses, or to perform
 *   molecular dynamics simulation using these forces, or to generate
 *   dynamical matrices, Born effective charges, and dielectric tensors.
 *
 *
 * <p> An abinit input file is composed of many keywords arranged
 * in a non-specific
 * order. Each keyword is followed by one or more numbers (integers or
 * doubles depending of the keyword).
 * Characters following a '#' are ignored.
 * The fisrt line of the file can be considered as a title.
 * This implementaton supports only 1 dataset!!!
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Network Computing Services, Inc. (the authors of XMol).
 * If you have problems, please contact the author of this code, not
 * the developers of XMol.
 *
 *<p><p> <p> Create an ABINIT input and output reader.
 *
 *
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.3 */
public class ABINITReader extends DefaultChemFileReader {

  // Factor conversion bohr to angstrom
  protected static final double ANGSTROMPERBOHR = 0.529177249f;

  // This variable is used to parse the input file
  protected StringTokenizer st;
  protected String fieldVal;
  protected int repVal = 0;

  protected BufferedReader inputBuffer;

  // The resulting CrystalFile
  CrystalFile crystalFile;
  

  /**
   * Creates a new <code>ABINITReader</code> instance.
   *
   * @param input a <code>Reader</code> value
   */
  public ABINITReader(Reader input) {
    super(input);
    this.inputBuffer = (BufferedReader) input;
    crystalFile = new CrystalFile();
  }


  /**
   * Read an ABINIT file. Automagically determines if it is
   * an abinit input or output file
   *
   * @return a ChemFile with the coordinates
   * @exception IOException if an error occurs
   */
  public ChemFile read() throws IOException {

    String line;

    /**
     * Check if it is an ABINIT *input* file or an abinit *output* file.
     * Actually we check if it is an abinit output file. If not, this is
     * an abinit input file
     */

    /**
     * The words "Version" and "ABINIT" must be found on the
     * same line and within the first three lines.
     */

    inputBuffer.mark(1024);
    for (int i = 0; i < 3; i++) {
      if (inputBuffer.ready()) {
        line = inputBuffer.readLine();
        if ((line.indexOf("ABINIT") >= 0) && (line.indexOf("Version") >= 0)) {

          //This is an output file
          inputBuffer.reset();
          System.out.println("We have an abinit *output* file");
          return (new ABINITOutputReader(input)).read();
        }
      }
    }

    //We don't have an output file so we have an input file
    inputBuffer.reset();
    System.out.println("We have an abinit *input* file");
    return (new ABINITInputReader(input)).read();
  } //end read()
  
  
  /**
   * Find the next token of an abinit file.
   * ABINIT tokens are words separated by space(s). Characters
   * following a "#" are ignored till the end of the line.
   * @param input a <code>BufferedReader</code> value
   * @return a <code>String</code> value
   * @exception IOException if an error occurs
   */
  public String nextAbinitToken(boolean newLine) throws IOException {

    String line;

    if (newLine) {    //We ignore the end of the line and go to the following line
      if (inputBuffer.ready()) {
        line = inputBuffer.readLine();
        st = new StringTokenizer(line, " \t");
      }
    }

    while (!st.hasMoreTokens() && inputBuffer.ready()) {
      line = inputBuffer.readLine();
      st = new StringTokenizer(line, " \t");
    }
    if (st.hasMoreTokens()) {
      fieldVal = st.nextToken();
      if (fieldVal.startsWith("#")) {
        nextAbinitToken(true);
      }
    } else {
      fieldVal = null;
    }
    return this.fieldVal;
  } //end nextAbinitToken(boolean newLine)

  
  public String nextAbinitTokenFollowing(String string)  throws IOException{
    int index;
    String line;
    while (inputBuffer.ready()) {
      line = inputBuffer.readLine();
      index = line.indexOf(string);
      if (index > 0) {
	index = index + string.length();
	line = line.substring(index);
	st = new StringTokenizer(line, " \t");
	while(!st.hasMoreTokens() && inputBuffer.ready()) {
	  line = inputBuffer.readLine();
	  st = new StringTokenizer(line, " \t");
	} 
	if (st.hasMoreTokens()) {
	  fieldVal = st.nextToken();
	} else {
	  fieldVal = null;
	}
	break;
      }
    }
    return fieldVal;
  } //end nextAbinitTokenFollowing(String string) 

} //end class ABINITReader

