/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2001-2003  The Jmol Development Team
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
import org.openscience.jmol.CrystalFile;
import org.openscience.jmol.CrystalBox;
import org.openscience.jmol.UnitCellBox;
import org.openscience.jmol.FortranFormat;
import org.openscience.jmol.Energy;
import org.openscience.jmol.EnergyBand;

import org.openscience.jmol.util.*;

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
import javax.swing.JOptionPane;


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
 *<p><p> <p> Create an ABINIT output reader.
 *
 *
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.3 */
public class ABINITOutputReader extends ABINITReader {
  
  // ABINIT variables

  int ionmov= 0;
  int natom = 1;
  int ntype = 1;
  double[] acell = new double[3];
  double[][] rprim = new double[3][3];
  String info = "";
  String line;
  int count = 0;
  double energy = 1000000;
  Vector dataset = new Vector(0);  //Store the dataset numbers as a string
  int selectedDataset = 0;
  int enunit;
  int[] zatnum;
  int[] type;
  double[][] xangst;
  int nkpt=0;
  int nband=0;
  
  /**
   * Creates a new <code>ABINITOutputReader</code> instance.
   *
   * @param input a <code>Reader</code> value
   */
  public ABINITOutputReader(Reader input) {
    super(input);
  }
  
  /**
   * Read an ABINIT *output* file.
   * @return a <code>ChemFile</code> value
   * @exception IOException if an error occurs
   */
  public ChemFile read() throws IOException {



    /*
     * The reading of the output file is essentially the same as the
     * reading of an output file except that the output file can
     * contain several frames and there is no need to consider
     * to parse things such as "integer*double".
     */

    inputBuffer.mark(1024 * 1024);
    // Analyze which DATASET are available 
    // (need to analyse up to  "-outvars:")
    nextAbinitToken(true);
    while (!fieldVal.equals("-outvars:")) {
      if (fieldVal.equals("DATASET")) {
	nextAbinitToken(false);
	dataset.addElement(fieldVal);
      }
      nextAbinitToken(true);
    }

    if (dataset.size() == 0) { //There is *no* dataset actually means
      dataset.addElement("1");    // there is only *one* dataset
      selectedDataset=0;
    } else {
      selectDataset();
    }


    
        
    inputBuffer.mark(1024 * 1024);
    // Read the value of preprocessed input variables  (First pass)
    // (starting from "-outvars:")
    while (fieldVal != null) {
      if (fieldVal.equals("acell") ||
	  fieldVal.equals("acell" + dataset.elementAt(selectedDataset))) {
        acell = new double[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitToken(false);
          acell[i] = FortranFormat.atof(fieldVal)
	    * ANGSTROMPERBOHR;    //in angstrom
        }
      } else if (fieldVal.equals("enunit") ||
		 fieldVal.equals("enunit" + 
				 dataset.elementAt(selectedDataset))) {
	nextAbinitToken(false);
	enunit = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("ionmov") ||
		 fieldVal.equals("ionmov" + 
				 dataset.elementAt(selectedDataset))) {
	nextAbinitToken(false);
	ionmov = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("natom") ||
		 fieldVal.equals("natom" +
				 dataset.elementAt(selectedDataset))) {
        nextAbinitToken(false);
        natom = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("nband") ||
		 fieldVal.equals("nband" +
				 dataset.elementAt(selectedDataset))) {
        nextAbinitToken(false);
        nband = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("ntype") ||
		 fieldVal.equals("ntype" +
				 dataset.elementAt(selectedDataset))) {
        nextAbinitToken(false);
        ntype = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("nkpt") ||
		 fieldVal.equals("nkpt" +
				 dataset.elementAt(selectedDataset))) {
        nextAbinitToken(false);
        nkpt = Integer.parseInt(fieldVal);
      } else if (fieldVal.equals("rprim") ||
		 fieldVal.equals("rprim" +
				 dataset.elementAt(selectedDataset))) {
        rprim = new double[3][3];
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            rprim[i][j] = FortranFormat.atof(fieldVal);
          }
        }
	break;   //rprim is the last in the list. No need to continue.
      } //end if
      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitToken(true);
    }  //end while


    //Initialize dynamic variables
    zatnum = new int[ntype];
    type =new int[natom]; 
    xangst =new double[natom][3];

    //Second pass through the file (continue to read 
    // echo values of preprocessed input variables)
    inputBuffer.reset();
    nextAbinitToken(true);

    count = 0;
    while (fieldVal != null) {
      if (fieldVal.equals("zatnum") ||
	  fieldVal.equals("zatnum" +
			  dataset.elementAt(selectedDataset))) {
        for (int i = 0; i < ntype; i++) {
          nextAbinitToken(false);
          zatnum[i] = (int) FortranFormat.atof(fieldVal);
        }
        break; // zatnum is the last in the list
      } else if (fieldVal.equals("type") ||
		 fieldVal.equals("type" +
				 dataset.elementAt(selectedDataset))) {
        for (int i = 0; i < natom; i++) {
          nextAbinitToken(false);
          type[i] = Integer.parseInt(fieldVal);
        }
      } else if (fieldVal.equals("xangst") ||
		 fieldVal.equals("xangst" +
				 dataset.elementAt(selectedDataset))) {
	for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            xangst[i][j] = FortranFormat.atof(fieldVal);
	  }
	} 
	
      } //end if
      
      // It is unnecessary to scan the end of the line. 
      // Go directly to the next line
      nextAbinitToken(true);
    } //end while

    echoPreprocessedVariables();


    //Go to the selected DATASET
    nextAbinitToken(true);
    while (fieldVal != null) {
      if (fieldVal.equals("==") &&
      	  nextAbinitToken(false).equals("DATASET") &&
      	  nextAbinitToken(false).equals(dataset.elementAt(selectedDataset))) {
	break;
      }
      nextAbinitToken(true);
    }
    
    
    if (ionmov == 0) { //There is only one frame
      // Generate a frame with outvars parameters
      setFrame();
      readEnergyBand();
    }
    else if (ionmov == 2 || ionmov == 3) { 
      String frameSep = "BROYDEN";
      readFrames(frameSep);
    }

    return crystalFile;
  }  //end read
   

  private void echoPreprocessedVariables() {
    System.out.println("Info: " + info);
    System.out.println("Dataset Number: " + dataset.size());
    System.out.println("Selected Dataset: " 
		       + dataset.elementAt(selectedDataset));
    

    System.out.println("acell: " + " " +
		       acell[0] + " " +
		       acell[1] + " " +
		       acell[2]);    
    System.out.println("enunit: " + enunit);
    System.out.println("ionmov: " + ionmov);
    System.out.println("natom: " + natom);
    System.out.println("nband: " + nband);
    System.out.println("nkpt: " + nkpt);
    System.out.println("ntype: " + ntype);
    System.out.println("rprim: ");
    System.out.println("   "+rprim[0][0]+" "+rprim[0][1]+" "+rprim[0][2]);
    System.out.println("   "+rprim[1][0]+" "+rprim[1][1]+" "+rprim[1][2]);
    System.out.println("   "+rprim[2][0]+" "+rprim[2][1]+" "+rprim[2][2]);
    System.out.print("type: ");
    for (int i = 0; i< natom; i++) {
      System.out.print(type[i] + " ");
    }
    System.out.println("");

    System.out.println("xangst: ");
    for (int i = 0; i< natom; i++) {
      System.out.print("   "); 
      for (int j =0; j<3; j++) {
	System.out.print(xangst[i][j] + " ");
      }
      System.out.println("");
    }
    System.out.print("zatnum: ");
    for (int i = 0; i< ntype; i++) {
      System.out.print(zatnum[i] + " ");
    }
    System.out.println("");
  } //end echoPreprocessedVariables()

  
  //Create a new frame based on class fields.
  private void setFrame() {
    
    //Set the atom types
    int[] atomType = new int[natom];
    for (int i = 0; i < natom; i++) {
      atomType[i] = zatnum[type[i] - 1];
    }
    
    //Store unit cell info
    UnitCellBox unitCellBox = new UnitCellBox(rprim, acell, true,
					      atomType, xangst);
    unitCellBox.setInfo(info);
    crystalFile.setUnitCellBox(unitCellBox);
    
    //use defaults value
    crystalFile.setCrystalBox(new CrystalBox());
        
    //generate the frame
    crystalFile.generateCrystalFrame();
    System.out.println("New Frame set!");
    
    //Add the Energy Property to the frame
    if (energy != 1000000) {
      crystalFile.getFrame(crystalFile.getNumberOfFrames()-1)
	.addProperty(new Energy(energy));
    }
  } //end setFrame()
  
  private void readFrames(String frameSep) throws IOException {
    //We start reading the frames
    while (fieldVal != null) {
      if (fieldVal.equals(frameSep)) {
        info = fieldVal;
        nextAbinitToken(false);    //"STEP "
        info = info + " " + fieldVal;
        nextAbinitToken(false);    //"NUMBER"
        info = info + " " + fieldVal;
        nextAbinitToken(false);    // NUMBER
        info = info + " " + fieldVal;
        System.out.println(info);
      } else if (fieldVal.equals("acell=")) {
        acell = new double[3];
        for (int i = 0; i < 3; i++) {
          nextAbinitToken(false);
          acell[i] = FortranFormat.atof(fieldVal) 
	    *ANGSTROMPERBOHR ;    //in angstrom
        }
      } else if (fieldVal.equals("rprim=")) {
        rprim = new double[3][3];
        for (int i = 0; i < 3; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            rprim[i][j] = FortranFormat.atof(fieldVal);
          }
        }
      } else if (fieldVal.equals("Cartesian")
                 && nextAbinitToken(false).equals("coordinates")) {
        nextAbinitToken(false);    //read "(bohr)"
        for (int i = 0; i < natom; i++) {
          for (int j = 0; j < 3; j++) {
            nextAbinitToken(false);
            xangst[i][j] = FortranFormat.atof(fieldVal)
	      * ANGSTROMPERBOHR;
          }
        }

      } else if (fieldVal.equals("At") &&
                 nextAbinitToken(false).equals("the")){
        //Read: "end of Broyden step  0, total energy=" (7 tokens)
        for (int i=0; i<7; i++) {
          nextAbinitToken(false);
        }
        nextAbinitToken(false);
        energy = FortranFormat.atof(fieldVal);

        //Energy is the last thing to be read --> store data

	setFrame();
        
      } //end if "Cartesian coordinates"


      // It is unnecessary to scan the end of the line.
      // Go directly to the next line
      nextAbinitToken(true);
    }                              //end while
    
    
  } //end readFrames


  private void readEnergyBand() throws IOException {
    EnergyBand energyBand = new EnergyBand(Units.EV);
    
    Point3d a = new Point3d();
    Point3d b;
    Point3d c;
    Point3d orig = new Point3d();
    Point3d end = new Point3d();
    String origName ="";
    String endName = "";

    //Go to "Eigenvalues"
    nextAbinitToken(true);
    while(!fieldVal.equals("Eigenvalues")) {
      nextAbinitToken(true);
    }

    int nkptRead=0; //number of kpoint read (cursor position in the file)
    inputBuffer.mark(1024 * 1024);    
    int nkptReadSinceLastMark=0;
    int lkpt=0; //Number of kpoint in a line
    while (nkptRead < nkpt) {
      
      inputBuffer.reset();
      nkptRead = nkptRead - nkptReadSinceLastMark;
      nkptReadSinceLastMark=0;

      a.x = FortranFormat.atof(nextAbinitTokenFollowing(" kpt="));
      a.y = FortranFormat.atof(nextAbinitToken(false));
      a.z = FortranFormat.atof(nextAbinitToken(false));
      nkptRead++;
      nkptReadSinceLastMark++;

      b = new Point3d(a);
      c = new Point3d(a);
      lkpt =0;
      orig = new Point3d(a);
      
      while (aligned(a,b,c) || lkpt <=2) {
	if(!aligned(a,b,c) && lkpt <= 2) { //Only 2 aligned points
	  orig=b;                          //is not a line
	  lkpt--;
	}
	lkpt++;
	end = new Point3d(a);
	c= new Point3d(b);
	b= new Point3d(a);
	if (nkptRead < nkpt) {
	  a.x = FortranFormat.atof(nextAbinitTokenFollowing(" kpt="));
	  a.y = FortranFormat.atof(nextAbinitToken(false));
	  a.z = FortranFormat.atof(nextAbinitToken(false));
	  nkptRead++;
	  nkptReadSinceLastMark++;
	} else {
	  break;
	}
      } //end while aligned
      
      inputBuffer.reset();
      nkptRead = nkptRead - nkptReadSinceLastMark;
      nkptReadSinceLastMark=0;


      if (orig.x == 0.0f && orig.y == 0.0f && orig.z == 0.0f){
	origName="\\S G";
      } else {
	origName="";
      }
      if (end.x == 0.0f && end.y == 0.0f && end.z == 0.0f){
	endName="\\S G";
      } else {
	endName="";
      }
      
      energyBand.addKLine(orig,origName,end,endName,lkpt,nband); //create a new line

      System.out.println("New K Line");
      for (int ikpt=0; ikpt < lkpt; ikpt++) {
	if (ikpt ==(lkpt-1)) {
	  inputBuffer.mark(1024*1024);
	  nkptReadSinceLastMark=0;
	}
	a.x = FortranFormat.atof(nextAbinitTokenFollowing(" kpt="));
	a.y = FortranFormat.atof(nextAbinitToken(false));
	a.z = FortranFormat.atof(nextAbinitToken(false));
	nkptRead++;
	nkptReadSinceLastMark++;

	System.out.println("  kpt: " + a.x + " " +a.y +" "+a.z );
	energyBand.addKPoint(a); //add kPoint
	for (int iband=0; iband<nband; iband++) { //add energies
	  if (iband==0) { 
	    nextAbinitToken(true);
	  } else {
	    nextAbinitToken(false);
	  }
	  energyBand.addEPoint(FortranFormat.atof(fieldVal));
	}
      }//end for

    }//end while

    crystalFile.getFrame(crystalFile.getNumberOfFrames()-1)
      .addProperty(energyBand);

  } //end readEnergyBand()

  protected boolean aligned(Point3d a, Point3d b, Point3d c) {
    if (a.equals(b) || a.equals(c) || b.equals(c)) { //trivial
      return true;
    }
    
    if( (((b.x-c.x)*(a.y-b.y)) < ((b.y-c.y)*(a.x-b.x)+0.0001)) &&
	(((b.x-c.x)*(a.y-b.y)) > ((b.y-c.y)*(a.x-b.x)-0.0001)) &&
	(((b.x-c.x)*(a.z-b.z)) < ((b.z-c.z)*(a.x-b.x))+0.0001) && 
	(((b.x-c.x)*(a.z-b.z)) > ((b.z-c.z)*(a.x-b.x))-0.0001) 	) {
      return true;
    } else {
      return false;
    }
  } //end aligned

  protected void selectDataset() {

    String message;
    message = "There are " + dataset.size() + " dataset in this file. "+
      "Please select one";

    String[] possibleValues = new String[dataset.size()];
    for(int i=0; i<dataset.size();i++) {
      possibleValues[i] = (String)dataset.elementAt(i);
    }
    String returnValue
      = (String)JOptionPane.showInputDialog(null,
				    message, "Dataset selection",
				    JOptionPane.QUESTION_MESSAGE, null,
				    possibleValues, possibleValues[0]);

    for(int i=0; i<dataset.size();i++) {
      if (possibleValues[i].equals(returnValue)) {
	selectedDataset = i;
      }
    }
    

    
  }  //end selectDataset(Vector dataset)
  
} // end class ABINITOutputReader
