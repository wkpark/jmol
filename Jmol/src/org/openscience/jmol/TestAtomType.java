/*
 * TestAtomType.java
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
import java.awt.*;
import java.util.*;

/**
 * Unit tests for the AtomType class.
 */
public class TestAtomType extends TestCase {

	/**
	 * Creates a Test for the given method.
	 */
	public TestAtomType(String name) {
		super(name);
	}

	/**
	 * AtomType fixture.
	 */
	AtomType at1;
	/**
	 * AtomType fixture.
	 */
	AtomType at2;

	/**
	 * Set up for testing.
	 */
	public void setUp() {
		at1 = new AtomType("type1", "root1", 1, 1.1, 2.2, 3.3, 4, 5, 6);
		at2 = new AtomType("type2", "root2", 0, 0.0, 0.0, 0.0, 0, 0, 0);
	} 

	/**
	 * Test the constructors.
	 */
	public void testConstructors() {
		assertEquals(BaseAtomType.get("type1"), at1.getBaseAtomType());
		assertEquals(BaseAtomType.get("type2"), at2.getBaseAtomType());
	}

	/**
	 * Returns a Test containing all the tests.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(TestAtomType.class);
		return suite;
	} 
}
