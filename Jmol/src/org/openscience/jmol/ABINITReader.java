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

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import javax.vecmath.Point3f;
import java.lang.reflect.Array;

/**
 * Abinit summary (www.abinit.org)
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
 * An abinit input file is composed of many keywords arranged in a non-spec=
ific
 * order. Each keyword is followed by one or more numbers (integers or
 * floats depending of the keyword). Characters following a '#' are ignored=
.
 * The fisrt line of the file can be considered as a title.
 * This implementaton supports only 1 dataset!!!
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Network Computing Services, Inc. (the authors of XMol).
 * If you have problems, please contact the author of this code, not
 * the developers of XMol.
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.0 */
public class ABINITReader implements ChemFileReader {

    /**
     * Create an ABINIT output reader.
     *
     * @param input source of ABINIT data
     */

    public static final double angstromPerBohr = 0.529177249;

    // This variable will be used to parse the input file
    private StringTokenizer st;
   
    public ABINITReader(Reader input) {
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
     * Read the ABINIT output.
     *
     * @return a ChemFile with the coordinates
     */
    public ChemFile read() throws IOException {

	ChemFile file = new ChemFile(bondsEnabled);

	int natom = 1;
	int ntype = 1;
	double[] acell = new double[3];
	double[][] rprim = new double[3][3];
	String info = ""; 
	String sn;

	String line;


	info = input.readLine();
	System.out.println(info);

	//Check if this is a multidataset file. Multidataset is not yet supported
	input.mark(1024*1024);
	line = input.readLine();
	st = new StringTokenizer(line, " \t");
	sn = nextAbinitToken(input);

	while (sn != null) {
	    if (sn.equals("ndtset"))
		{
		    sn = nextAbinitToken(input);
		    if (FortranFormat.atof(sn) > 1)
			{
			    System.out.println("ABINITReader: multidataset not supported");
			    fireFrameRead();
			    return file;
			}
		}
	    sn = nextAbinitToken(input);
	}
	input.reset();

	//First pass through the file (get variables of known dimension)
	input.mark(1024*1024);  
	line = input.readLine();
	st = new StringTokenizer(line, " \t");
	sn = nextAbinitToken(input);

	while (sn != null) {
	    if (sn.equals("acell"))
		{
		    for (int i=0; i<3 ; i++)
			{

			    sn = nextAbinitToken(input);
			    int index=sn.indexOf("*");
			    if (index >= 0)    //Test if number format of type i*f
				{
				    int times = Integer.parseInt(sn.substring(0, index));
				    System.out.println("times : "+times);
				    double value = FortranFormat.atof(sn.substring(index+1));
				    System.out.println("value : "+value);
				    for (int j=i; j<i+times ; j++)
					{
					    acell[j] = value;
					}
				    i=i+times-1;
				}
			    else
				{
				    acell[i] = FortranFormat.atof(sn);
				}
			}

		    System.out.println("acell: "+acell[0]+" "+acell[1]+" "+acell[2]);
			   
		}
	    else if (sn.equals("rprim"))
		{
		    for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
			    sn = nextAbinitToken(input);
			    rprim[i][j]=FortranFormat.atof(sn);
			}
		    }
		}
	    else if (sn.equals("ntype"))
		{
		    sn = nextAbinitToken(input);
		    ntype = Integer.parseInt(sn);
		}
	    else if (sn.equals("natom"))
		{
		    sn = nextAbinitToken(input);
		    natom = Integer.parseInt(sn);
		}
	    sn = nextAbinitToken(input);
	}



	//Initialize dynamic variables
	int[] zatnum=(int[]) Array.newInstance(int.class, ntype);
	int[] type=(int[]) Array.newInstance(int.class, natom);
	ChemFrame frame = new ChemFrame(natom);
	int[] dims = {natom,3};
	double[][] xangst = (double[][]) Array.newInstance(double.class, dims);
	   
	double[][] xred = (double[][]) Array.newInstance(double.class, dims);

	//Second pass through the file
	input.reset();
	sn = nextAbinitToken(input);

	while (sn != null) {
	    if (sn.equals("zatnum"))
		{
		    for (int i = 0; i < ntype; i++) {
			sn = nextAbinitToken(input);
			zatnum[i]=Integer.parseInt(sn);
		    }
		}
	    else if (sn.equals("type"))    //type MUST BE after natom in the input  file !!!BUG?!!!
		{
		    for (int i = 0; i < natom; i++) {
			sn = nextAbinitToken(input);
			int index=sn.indexOf("*");
			if (index >= 0)    //Test if number format of type i*i
			    {
				int times = Integer.parseInt(sn.substring(0, index));
				System.out.println("times : "+times);
				int value = Integer.parseInt(sn.substring(index+1));
				System.out.println("value : "+value);
				for (int j=i; j<i+times ; j++)
				    {
					type[j] = value;
				    }
				i=i+times-1;
			    }
			else
			    {
				type[i] = Integer.parseInt(sn);
			    }
		    }
		}
	    else if (sn.equals("xangst")) 
		{
		    for (int i = 0; i < natom; i++) {
			for (int j = 0; j < 3; j++) {
			    sn = nextAbinitToken(input);
			    xangst[i][j]=FortranFormat.atof(sn);
			}
		    }
		}
	    else if (sn.equals("xcart")) 
		{
		    for (int i = 0; i < natom; i++) {
			for (int j = 0; j < 3; j++) {
			    sn = nextAbinitToken(input);
			    xangst[i][j]=FortranFormat.atof(sn) * angstromPerBohr;
			}
		    }
		}
	    else if (sn.equals("xred"))
		{
		    for (int i = 0; i < natom; i++) {
			for (int j = 0; j < 3; j++) {
			    sn = nextAbinitToken(input);
			    xred[i][j]=FortranFormat.atof(sn);
			}
			xangst[i][0]=(xred[i][0]*rprim[0][0]+xred[i][1]*rprim[1][0]+xred[i][2]*rprim[2][0])*acell[0]*angstromPerBohr;
			xangst[i][1]=(xred[i][0]*rprim[0][1]+xred[i][1]*rprim[1][1]+xred[i][2]*rprim[2][1])*acell[1]*angstromPerBohr;
			xangst[i][2]=(xred[i][0]*rprim[0][2]+xred[i][1]*rprim[1][2]+xred[i][2]*rprim[2][2])*acell[2]*angstromPerBohr;
		    }
		}
	    sn = nextAbinitToken(input);
	}



	frame.setInfo(info);
	for (int i = 0; i < natom ; i++) {
	    int atomIndex = frame.addAtom(zatnum[type[i]-1] , (float) xangst[i][0], (float) xangst[i][1], (float) xangst[i][2]);
	   
	}
    
	file.addFrame(frame);
	fireFrameRead();
	return file;
    }
   
    /**
     * Find the next token of an abinit file
     */
    public String nextAbinitToken(BufferedReader input) throws IOException {
	String line;
	String sn;
	while (!st.hasMoreTokens() && input.ready())
	    {
		line = input.readLine();
		st = new StringTokenizer(line, " \t");
	    }
	if (st.hasMoreTokens())
	    {
		sn = st.nextToken();
		if (sn.startsWith("#"))
		    {
			line = input.readLine();
			if (input.ready())
			    {
				st = new StringTokenizer(line, " \t");
				sn = nextAbinitToken(input);
			    }
		    }
	    }
	else
	    sn = null;

	return sn;
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
	listenerList.removeElement(l);
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

