
/*
 * Copyright 2002 The Jmol Development Team
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

import com.baysmith.io.FileUtilities;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for the Gaussian98Reader class.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class Gaussian98ReaderTest extends TestCase {

  /**
   * Create a test case with given name.
   *
   * @param name this test case's name.
   */
  public Gaussian98ReaderTest(String name) {
    super(name);
  }

  /**
   * Returns a suite of tests contained in this test case.
   *
   * @return a suite of tests contained in this test case.
   */
  public static Test suite() {
    TestSuite suite = new TestSuite(Gaussian98ReaderTest.class);
    return suite;
  }

  /**
   * A reader with which to test.
   */
  Gaussian98Reader reader1;

  /**
   *  Test directory for isolating testing operations.
   */
  File testDirectory;

  /**
   * Setup fixtures.
   */
  public void setUp() {

    testDirectory = new File(getClass().getName());
    FileUtilities.deleteAll(testDirectory);
    assertTrue("Unable to create test directory \"" + testDirectory.getName()
        + "\"", testDirectory.mkdir());

    try {
      AtomTypeSet ats1 = new AtomTypeSet();
      ats1.load(getClass().getResourceAsStream("Data/AtomTypes"));

      File g98File = new File(testDirectory, "g98.out");
      FileUtilities.copyStreamToFile(getClass().getResourceAsStream("Test-"
              + g98File.getName()), g98File);
      reader1 = new Gaussian98Reader(new FileReader(g98File));
    } catch (IOException ex) {
      fail("unable to open Gaussian98 test file: " + ex.toString());
    }
  }

  /**
   * Destroy fixtures.
   */
  public void tearDown() {
    testDirectory = null;
  }

  /**
   * Test reading a file.
   */
  public void testRead() {

    try {
      ChemFile cf1 = reader1.read();
      assertEquals(8, cf1.getNumberFrames());
      assertEquals(
          "SCF Done:  E(UHF) =  -304.909591195     A.U. after   38 cycles",
            cf1.getFrame(0).getInfo());
      assertEquals(
          "SCF Done:  E(UHF) =  -304.912139784     A.U. after    1 cycles",
            cf1.getFrame(7).getInfo());
    } catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.toString());
    }
  }

}
