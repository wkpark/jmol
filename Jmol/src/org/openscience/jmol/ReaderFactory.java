/*
 * ReaderFactory.java
 *
 * Copyright (C) 1999  Bradley A. Smith
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

/**
 * A factory for creating ChemFileReaders.
 * The type of reader created is determined from the input.
 *
 * @author  Bradley A. Smith (yeldar@home.com);
 * @version 1.0
 */
public abstract class ReaderFactory  {
    /**
     * Creates a ChemFileReader of the type determined by
     * reading the input. The input is read line-by-line
     * until a line containing an identifying string is
     * found.
     *
     * @return  If the input type is determined, a 
     *   ChemFileReader subclass is returned; otherwise,
     *   null is returned.
     * @exception IOException  if an I/O error occurs
     */
    public static ChemFileReader createReader(Reader input) throws IOException {
        BufferedReader buffer = new BufferedReader(input);
        String line;
        String line2;

		if (buffer.markSupported()) {
			/* The mark and reset on the buffer, is so that we can read
			 * the first line line to test for XYZ files without screwing
			 * up the other tests below 
			 */
			buffer.mark(255);
			line = buffer.readLine();
			line2 = buffer.readLine();
			buffer.reset();
			
			/* an integer-valued first line is a special test for XYZ files */
			try {
				Integer i = new Integer(line.trim());
				return new XYZReader(buffer); 
			} catch(NumberFormatException nfe) {
			}
			
			/* This line wasn't an integer, so move on to the rest of
			   our filters */
			
			/* Test for CML */
			if (line.startsWith("<?xml") && line2.indexOf("cml.dtd") >=0) {
				return new CMLReader(buffer); 
			}
		} else {
			line = buffer.readLine();
		}

		/* Search file for a line containing an identifying keyword */
		while (buffer.ready() && line != null) {
			if (line.indexOf("Gaussian 98:") >= 0) {
				return new Gaussian98Reader(buffer);
			} else if (line.indexOf("Gaussian 94:") >= 0) {
				return new Gaussian94Reader(buffer);
			} else if (line.indexOf("Gaussian 92:") >= 0) {
				return new Gaussian92Reader(buffer);
			} else if (line.indexOf("GAMESS") >= 0) {
				return new GamessReader(buffer);
			} else if (line.indexOf("ACES2") >= 0) {
				return new Aces2Reader(buffer);
			} else if (line.indexOf("Amsterdam Density Functional") >= 0) {
				return new ADFReader(buffer);                
			} else if (line.startsWith("HEADER")) {
				return new PDBReader(buffer);
			}
			line = buffer.readLine();
		}
        return null;        
    }
}
