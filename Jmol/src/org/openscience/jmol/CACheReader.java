/*
 * CACheReader.java
 *
 * Copyright (C) 2000 Charles R. Fulton
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol;

import java.io.*;
import java.util.StringTokenizer;

/**
 * A reader for CAChe molstruct files
 *
 * <p> Molecular coordinates are the only thing read in right now, 
 * I plan to add energies, and other property data next. 
 *
 * <p> Warning. This reader will only work on files generated from the Windows version of
 * CAChe, not babel or MAC versions. This will be fixed shortly with a "smarter" version. 
 * See source code comments below. 
 * 
 * @author Charles R. Fulton (fultoncr@ucarb.com)
 * @version 1.0
 *
 */

public class CACheReader implements ChemFileReader 
{
	/**
	 * The source for CAChe molstruct data
	 */
	private BufferedReader input;

	/**
	 * Create an CAChe output reader.
	 *
	 * @param input source of CAChe data
	 */
	public CACheReader(Reader input) 
	{
		this.input = new BufferedReader(input);
		System.out.println("Parsing CAChe Molstruct File");
	}

	/**
	 * Read in CAChe molstruct data
	 * 
	 * @return a ChemFile with the coordinates
	 * @exception IOException if an I/O error occurs
	 */
	public ChemFile read() throws IOException, Exception 
	{
		ChemFile file = new ChemFile();
		ChemFrame frame = null;
		String line = input.readLine();
		System.out.println(line);

		// -- Get all of the stuff before object_classes start -- //

		//String writtenby = null;  // mopac,dgauss,etc
		//String datadictver = null; // version of Data Dictionary used
			// really need to find out exactly how many diff. data dictionaries
			// there are


		// ################################################################################## //
		// -- initial brute force reader
		// -- just trying to get xyz coordinates for now
		// -- later will implement object_class & property methods
		// -- look for "object_class" (e.g. object_class atom)
		// -- now look for "property X" (e.g. Property anum)
		// -- now parse lines until seeing the next "object_class" line (might want to use property_flags later)
		// ################################################################################## //
		
		while (input.ready() && line != null)
		{
			if (line.indexOf("ID dflag sym anum chrg xyz_coordinates") >=0)
			{
				// frame is where we store property data
				frame = new ChemFrame();
				try 
				{
					readCoordinates(frame);
				} catch (Exception e) {
					System.out.println("Problem getting coordinates: " + e + "\n");
					e.printStackTrace();
				}
			} 
			line = input.readLine(); // get next line
		}

		// Add current frame to file
		try
		{
			file.frames.addElement(frame);
		} catch (Exception e) {
			System.out.println("Error adding frame to ChemFile: " + e);
			e.printStackTrace();
		}
		return file;
	}

	/** 
	 * Reads a set of coordinates into ChemFrame.
	 *
	 * @param frame the destination ChemFrame
	 * @exception IOException if an I/O error occurs.
	 *
	 */
	private void readCoordinates(ChemFrame frame) throws IOException, Exception
	{
		String line;
		while (input.ready())
		{
			line = input.readLine();
			System.out.println(line); // should only print out from object_class - property_flags

			if (line == null || line.indexOf("property_flags:") >= 0)
			{								// will probably need to change this, because some object_classes 
				break;						// use the property_flags block. (e.g. basis sets)
			}
			
			int anum;
			StringReader sr = new StringReader(line);
			StreamTokenizer token = new StreamTokenizer(sr);

			// ignore first 4 tokens
			token.nextToken(); // ID
			token.nextToken(); // dflag1 
			token.nextToken(); // dflag2
			token.nextToken(); // sym
			
			if (token.nextToken() == StreamTokenizer.TT_NUMBER)
			{
				anum = (int)token.nval;
			}  else throw new IOException("Error reading anum_coord");

			token.nextToken(); // chrg
			System.out.println(token);

			double x;
			double y;
			double z;
			if (token.nextToken() == StreamTokenizer.TT_NUMBER)
			{
				x = token.nval;
			} else throw new IOException("Error reading x_coord");
			if (token.nextToken() == StreamTokenizer.TT_NUMBER)
			{
				y = token.nval;
			} else throw new IOException("Error reading y_coord");
			if (token.nextToken() == StreamTokenizer.TT_NUMBER)
			{
				z = token.nval;
			} else throw new IOException("Error reading z_coord");

			// have to add atomic number because ChemFrame does the atomtyping 
			// maybe that is where to check on fixing Zr atoms...
			frame.addVert(anum, (float)x, (float)y, (float)z);
		}
	}
}
		

