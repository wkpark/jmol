/*
 * TestGaussian98Reader.java
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

import junit.framework.*;
import java.io.*;
import java.util.*;

public class TestGaussian98Reader extends TestCase {

	public TestGaussian98Reader(String name) {
		super(name);
	}

	Gaussian98Reader reader1;

	public void setUp() {
		try {
			AtomTypeSet ats1 = new AtomTypeSet();
			ats1.load(getClass().getResourceAsStream("Data/AtomTypes"));
			String sampleFileName = "samples/g98.out";
			if (System.getProperty("jmol.home") != null) {
				sampleFileName = System.getProperty("jmol.home") + "/" + sampleFileName;
			}
			reader1 = new Gaussian98Reader(new FileReader(sampleFileName));
		} catch (IOException ex) {
			fail("unable to open Gaussian98 test file: " + ex.toString());
		} 
	} 

	public void testRead() {
		try {
			ChemFile cf1 = reader1.read();
			assertEquals(8, cf1.nFrames());
			assertEquals("SCF Done:  E(UHF) =  -304.909591195     A.U. after   38 cycles", cf1.getFrame(0).getInfo());
			assertEquals("SCF Done:  E(UHF) =  -304.912139784     A.U. after    1 cycles", cf1.getFrame(7).getInfo());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.toString());
		}
	} 


	public static Test suite() {
		TestSuite suite = new TestSuite(TestGaussian98Reader.class);
		return suite;
	} 
}
