
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

import junit.framework.*;
import java.awt.*;
import java.util.*;
import java.beans.*;

/**
 * Unit tests for the BaseAtomType class.
 */
public class TestBaseAtomType extends TestCase {

	/**
	 * Creates a Test for the given method.
	 */
	public TestBaseAtomType(String name) {
		super(name);
	}

	/**
	 * BaseAtomType fixture.
	 */
	BaseAtomType at1;

	/**
	 * BaseAtomType fixture.
	 */
	BaseAtomType at2;

	/**
	 * Set up for testing.
	 */
	public void setUp() {
		at1 = BaseAtomType.get("type1", "root1", 1, 1.1, 2.2, 3.3,
				new Color(4, 5, 6));
		at2 = BaseAtomType.get("type2");
	}

	/**
	 * Test the constructors.
	 */
	public void testConstructors() {

		assertEquals("type1", at1.getName());
		assertEquals("root1", at1.getRoot());
		assertEquals(1, at1.getAtomicNumber());
		assert(1.1 == at1.getMass());
		assert(2.2 == at1.getVdwRadius());
		assert(3.3 == at1.getCovalentRadius());
		assertEquals(new Color(4, 5, 6), at1.getColor());
		assertEquals("type2", at2.getName());
		assertEquals(null, at2.getRoot());
		assertEquals(0, at2.getAtomicNumber());
		assertEquals(null, at2.getColor());
	}

	/**
	 * Returns a Test containing all the tests.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(TestBaseAtomType.class);
		return suite;
	}
}
