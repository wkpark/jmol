/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

import org.openscience.jmol.AtomicSymbol;
import org.openscience.jmol.AtomTypeList;
import org.openscience.jmol.viewer.*;
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
import org.openscience.jmol.FortranFormat;

import org.openscience.jmol.CrystalFile;
import org.openscience.jmol.CrystalBox;
import org.openscience.jmol.UnitCellBox;

/* 
 *
 * Read output files generated with the VASP software 
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.3 */
public class VASPReader extends DefaultChemFileReader {

    // Factor conversion bohr to angstrom
    protected static final double ANGSTROMPERBOHR = 0.529177249f;

    // This variable is used to parse the input file
    protected StringTokenizer st =  new StringTokenizer("", "");;
    protected String fieldVal;
    protected int repVal = 0;

    protected BufferedReader inputBuffer;

    // The resulting CrystalFile
    CrystalFile crystalFile;


    // VASP VARIABLES
    int natom = 1;
    int ntype = 1;
    double acell[] = new double[3];
    double[][] rprim = new double[3][3];
    String info = "";
    String line;
    String[] anames; //size is ntype. Contains the names of the atoms
    int natom_type[]; //size is natom. Contain the atomic number
    String representation; // "Direct" only so far
    
    /**
     * Creates a new <code>VASPReader</code> instance.
     *
     * @param input a <code>Reader</code> value
     */
    public VASPReader(JmolViewer viewer, Reader input) {
	super(viewer, input);
	this.inputBuffer = (BufferedReader) input;
	crystalFile = new CrystalFile(viewer);
    }


    /**
     * Read an ABINIT file. Automagically determines if it is
     * an abinit input or output file
     *
     * @return a ChemFile with the coordinates
     * @exception IOException if an error occurs
     */
    public ChemFile read() throws IOException {

	// Get the info line (first token of the first line)
	inputBuffer.mark(255);
	info = nextVASPToken(false);
	System.out.println(info);
	inputBuffer.reset(); 
    
	// Get the number of different atom "NCLASS=X"
	inputBuffer.mark(255);
	nextVASPTokenFollowing("NCLASS");
	ntype = Integer.parseInt(fieldVal);
	System.out.println("NCLASS= " + ntype);
	inputBuffer.reset(); 

	// Get the different atom names
	anames = new String[ntype];
	
	nextVASPTokenFollowing("ATOM");
	for(int i = 0; i < ntype; i++) {
	    anames[i] = fieldVal;
	    nextVASPToken(false);
	}
	
	// Get the number of atom of each type
	int[] natom_type = new int[ntype];     
	natom = 0;
	for(int i = 0; i < ntype; i++) {
	    natom_type[i] = Integer.parseInt(fieldVal);
	    nextVASPToken(false);
	    natom = natom + natom_type[i];
	}
	
	// Get the representation type of the primitive vectors
	// only "Direct" is recognize now.
	representation = fieldVal;
	if(representation.equals("Direct")) {
	    System.out.println("Direct representation");
	    // DO NOTHING
	} else {
	    System.out.println("This VASP file is not supported. Please contact the Jmol developpers");
	    // Return an empty crystal
	    return crystalFile; 
	}
	


	while(nextVASPToken(false) != null) {

	    // Get acell
	    for(int i=0; i<3; i++) {
		acell[i] = FortranFormat.atof(fieldVal); // all the same FIX?
	    }
	    
	    // Get primitive vectors
	    for(int i = 0; i < 3; i++) {
		for(int j = 0; j < 3; j++) {
		    nextVASPToken(false);
		    rprim[i][j] = FortranFormat.atof(fieldVal);
		}
	    }
	    
	    // Get atomic position
	    int[] atomType = new int[natom];
	    double[][] xred = new double[natom][3];
	    int atomIndex=0;

	    for(int i = 0; i < ntype; i++) {
		for(int j = 0; j < natom_type[i] ; j++) {

		    atomType[atomIndex] = AtomicSymbol.elementToAtomicNumber(anames[i]);
		    System.out.println("aname: " + anames[i]);
		    System.out.println("atomType: "+atomType[atomIndex]);
		    
		    nextVASPToken(false);
		    xred[atomIndex][0] = FortranFormat.atof(fieldVal);
		    nextVASPToken(false);
		    xred[atomIndex][1] = FortranFormat.atof(fieldVal);
		    nextVASPToken(false);
		    xred[atomIndex][2] = FortranFormat.atof(fieldVal);
		    
		    atomIndex = atomIndex+1;
		    
		}
	    }
	    
	    //Store unit cell info
	    UnitCellBox unitCellBox = new UnitCellBox(rprim, acell, false,
						      atomType, xred);
	    unitCellBox.setInfo(info);
	    crystalFile.setUnitCellBox(unitCellBox);
	    
	    //use defaults value
	    crystalFile.setCrystalBox(new CrystalBox());
	    
	    //generate the frame
	    crystalFile.generateCrystalFrame();
	    System.out.println("New Frame set!");

	} //end while
	
	return crystalFile;
    }
	   
    
    
    /**
     * Find the next token of an VASP file.
     * ABINIT tokens are words separated by space(s). Characters
     * following a "#" are ignored till the end of the line.
     * @param input a <code>BufferedReader</code> value
     * @return a <code>String</code> value
     * @exception IOException if an error occurs
     */
    public String nextVASPToken(boolean newLine) throws IOException {
	
	String line;
	
	if (newLine) {    //We ignore the end of the line and go to the following line
	    if (inputBuffer.ready()) {
		line = inputBuffer.readLine();
		st = new StringTokenizer(line, " =\t");
	    }
	}
	
	while (!st.hasMoreTokens() && inputBuffer.ready()) {
	    line = inputBuffer.readLine();
	    st = new StringTokenizer(line, " =\t");
	}
	if (st.hasMoreTokens()) {
	    fieldVal = st.nextToken();
	    if (fieldVal.startsWith("#")) {
		nextVASPToken(true);
	    }
	} else {
	    fieldVal = null;
	}
	return this.fieldVal;
    } //end nextVASPToken(boolean newLine)


    /* Find the next token of a VASP file begining
     * with the *next* line.
     */
    public String nextVASPTokenFollowing(String string)  throws IOException{
	int index;
	String line;
	while (inputBuffer.ready()) {
	    line = inputBuffer.readLine();
	    index = line.indexOf(string);
	    if (index > 0) {
		index = index + string.length();
		line = line.substring(index);
		st = new StringTokenizer(line, " =\t");
		while(!st.hasMoreTokens() && inputBuffer.ready()) {
		    line = inputBuffer.readLine();
		    st = new StringTokenizer(line, " =\t");
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
    } //end nextVASPTokenFollowing(String string) 

} //end class VASPReader

    
