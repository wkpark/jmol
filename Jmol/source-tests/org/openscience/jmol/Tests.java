
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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite of all tests in this package.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class Tests {

  /**
   * Returns a suite containing all the tests in this package.
   *
   * @return a suite containing all the tests in this package.
   */
  public static Test suite() {

    TestSuite suite = new TestSuite();
    suite.addTest(BaseAtomTypeTest.suite());
    suite.addTest(AtomTypeTest.suite());
    suite.addTest(AtomTypeSetTest.suite());
    suite.addTest(AtomTypesModelTest.suite());
    suite.addTest(Gaussian98ReaderTest.suite());
    return suite;
  }

}

