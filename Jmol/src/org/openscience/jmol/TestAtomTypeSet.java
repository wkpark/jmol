/*
 * TestAtomTypeSet.java
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
import java.awt.Color;

/**
 * Unit tests for the AtomTypeSet class.
 */
public class TestAtomTypeSet extends TestCase {

	/**
	 * Creates a Test for the given method.
	 */
	public TestAtomTypeSet(String name) {
		super(name);
	}

	/**
	 * Fixture for testing empty AtomTypeSet objects.
	 */
	AtomTypeSet ats1;

	/**
	 * Fixture for testing AtomTypeSet objects loaded with AtomTypes.
	 */
	AtomTypeSet ats2;

	/**
	 * BaseAtomType fixture for loading into AtomTypeSets.
	 */
	BaseAtomType at1;

	/**
	 * Set up for testing.
	 */
	public void setUp() {
		ats1 = new AtomTypeSet();
		at1 = BaseAtomType.get("type1", "root1", 0, 0.0, 0.0, 0.0, new Color(0, 0, 0));
		ats2 = new AtomTypeSet();
		assert("setUp failed", ats2.add(at1));
	} 

	/**
	 * Test the isEmpty method.
	 * Expect true for ats1 and false for ats2.
	 */
	public void testIsEmpty() {
		assert(ats1.isEmpty());
		assert(!ats2.isEmpty());
	} 

	/**
	 * Test the add method.
	 */
	public void testAdd() {
		assertEquals(0, ats1.size());
		assert(ats1.add(at1));
		assertEquals(1, ats1.size());
		assert(!ats1.add(at1));
		assertEquals(1, ats1.size());
	}

	/**
	 * Test the load method.
	 */
	public void testLoad() {
		try {
			ats1.load(getClass().getResourceAsStream("Data/AtomTypes"));
			assert(ats1.size() > 100);
			assert(ats1.contains(BaseAtomType.get("CAR")));
		} catch (IOException ex) {
			fail("load failed: " + ex.toString());
		}
	}

	/**
	 * Returns a Test containing all the tests.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(TestAtomTypeSet.class);
		return suite;
	} 
}
