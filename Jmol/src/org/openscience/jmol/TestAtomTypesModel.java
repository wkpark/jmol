
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
import java.io.*;
import java.util.*;
import java.awt.Color;

/**
 * Unit tests for the AtomTypesModel class.
 */
public class TestAtomTypesModel extends TestCase {

	/**
	 * Creates a Test for the given method.
	 */
	public TestAtomTypesModel(String name) {
		super(name);
	}

	/**
	 * Fixture for testing empty AtomTypesModel objects.
	 */
	AtomTypesModel atm1;

	/**
	 * Fixture for testing AtomTypesModel objects loaded with AtomTypes.
	 */
	AtomTypesModel atm2;

	/**
	 * AtomType fixture for loading into AtomTypesModels.
	 */
	BaseAtomType at1;

	/**
	 * Set up for testing.
	 */
	public void setUp() {

		atm1 = new AtomTypesModel();
		at1 = BaseAtomType.get("type1", "root1", 0, 0.0, 0.0, 0.0,
				new Color(0, 0, 0));
		atm2 = new AtomTypesModel();
		atm2.updateAtomType(at1);
	}

	/**
	 * Test the getColumnName method.
	 * Expect appropriate strings for valid indexes and null for all else.
	 */
	public void testGetColumnName() {

		assertEquals("Atom Type", atm1.getColumnName(0));
		assertEquals("Atomic\nNumber", atm1.getColumnName(2));
		assertEquals("Color", atm1.getColumnName(6));
		assertEquals(null, atm1.getColumnName(-1));
		assertEquals(null, atm1.getColumnName(20));
	}

	/**
	 * Test the get method.
	 * Expect appropriate strings for valid indexes and null for all else.
	 */
	public void testGet() {

		assertEquals(null, atm1.get("test"));
		assertEquals(null, atm1.get(5));
		assertEquals(null, atm1.get(-1));
		assertEquals(null, atm1.get(null));
		assertEquals(null, atm2.get(null));
		assertEquals("failed get(0)", at1, atm2.get(0));
		assertEquals("failed get(type1)", at1, atm2.get("type1"));
	}

	/**
	 * Test the elements method.
	 * Expect list of atom types.
	 */
	public void testElements() {

		Enumeration types = atm1.elements();
		assert("hasMoreElements() failed", false == types.hasMoreElements());

		Enumeration types2 = atm2.elements();
		assert("hasMoreElements() failed", true == types2.hasMoreElements());
		assertEquals("first nextElement() failed", at1, types2.nextElement());
		assert("hasMoreElements() failed", false == types2.hasMoreElements());
		try {
			types2.nextElement();
			fail("no exception thrown at end of elements");
		} catch (NoSuchElementException ex) {
		}
	}

	/**
	 * Test updateAtomType method.
	 * Expect addition of AtomType if not already in model (based upon name), and
	 * replacement of AtomType if name already in model.
	 */
	public void testUpdateAtomType() {

		atm1.updateAtomType(at1);
		assertEquals(at1, atm1.get(0));
		assertEquals(null, atm1.get("root1"));
		assertEquals(at1, atm1.get("type1"));

		BaseAtomType tmpAt1 = BaseAtomType.get("temp type", "type1", 0, 0.0,
								  0.0, 0.0, new Color(0, 0, 0));
		atm1.updateAtomType(tmpAt1);
		assertEquals(at1, atm1.get(0));
		assertEquals(tmpAt1, atm1.get("temp type"));
		assertEquals(at1, atm1.get("type1"));

		BaseAtomType at1Replace = BaseAtomType.get("type1", "root2", 1, 0.0,
									  0.0, 0.0, new Color(0, 0, 0));
		atm2.updateAtomType(at1Replace);
		assertEquals(null, atm2.get(0));
		assertEquals(at1Replace, atm2.get(1));
		assertEquals(at1Replace, atm2.get("type1"));
		assertEquals("root2", atm2.get(1).getRoot());
	}

	/**
	 * Test setValueAt method.
	 * Expect exceptions if range or object class incorrect.
	 * Expect set of BaseAtomType values if correct parameters, except for name.
	 */
	public void testSetValueAt() {

		try {
			atm1.setValueAt("test", 0, 0);
			fail("exception not thrown on out of range index");
		} catch (ArrayIndexOutOfBoundsException ex) {
		}
		atm2.setValueAt("test", 0, 1);
		assertEquals("test", atm2.get(0).getRoot());
		try {
			atm2.setValueAt(new Integer(0), 0, 1);
			fail("exception not thrown on invalid class");
		} catch (ClassCastException ex) {
		}
	}

	/**
	 * Test changing BaseAtomType name with setValueAt method.
	 * Expect name column to not be editable, and setValueAt on name
	 * column will not change value.
	 */
	public void testNameSetValueAt() {
		assertEquals("type1", atm2.get(0).getName());
		assert(false == atm2.isCellEditable(0, 0));
		atm2.setValueAt("test", 0, 0);
		assertEquals("type1", atm2.get(0).getName());
	}

	/**
	 * Test isCellEditable method.
	 * Expect false if invalid row or column 0. Otherwise expect true.
	 */
	public void testIsCellEditable() {

		assert(false == atm2.isCellEditable(0, 0));
		assert(false == atm2.isCellEditable(0, -1));
		assert(false == atm2.isCellEditable(0, 23));
		assert(false == atm2.isCellEditable(0, 0));
		assert(true == atm2.isCellEditable(0, 1));
		assert(true == atm2.isCellEditable(0, 2));
		assert(true == atm2.isCellEditable(0, 3));
		assert(true == atm2.isCellEditable(0, 5));
	}

	/**
	 * Test the getValueAt method.
	 * Expect appropriate object returned for values of BaseAtomType if
	 * correct parameters given. Otherwise an empty String is returned.
	 */
	public void testGetValueAt() {

		try {
			assert(atm1.getValueAt(0, 0) instanceof String);
			assertEquals(0, ((String) atm1.getValueAt(0, 0)).length());
			assert(atm2.getValueAt(0, 0) instanceof String);
			assertEquals(at1.getName(), (String) atm2.getValueAt(0, 0));
			assert(atm2.getValueAt(-1, -1) instanceof String);
			assertEquals(0, ((String) atm2.getValueAt(-1, -1)).length());
		} catch (Exception ex) {
			fail(ex.toString());
		}
	}

	/**
	 * Test the clear method.
	 * Expect the getRowCount to always be 5 after clearing, and
	 * that old atom types are not longer returned by get.
	 */
	public void testClear() {

		assertEquals(5, atm2.getRowCount());
		assertEquals(at1, atm2.get("type1"));
		atm2.clear();
		assertEquals(5, atm2.getRowCount());
		assertEquals(null, atm2.get("type1"));
	}

	/**
	 * Test the getColumnClass method.
	 * Expected results are Integer for 2; Double for 3, 4, and 5; Color for 6;
	 * and String for everything else.
	 */
	public void testGetColumnClass() {
		assertEquals(Integer.class, atm1.getColumnClass(2));
		assertEquals(Double.class, atm1.getColumnClass(3));
		assertEquals(String.class, atm1.getColumnClass(1));
		assertEquals(String.class, atm1.getColumnClass(-1));
	}

	/**
	 * Returns a Test containing all the tests.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(TestAtomTypesModel.class);
		return suite;
	}
}
