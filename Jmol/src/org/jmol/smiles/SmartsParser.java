/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-04 07:53:26 -0500 (Tue, 04 May 2010) $
 * $Revision: 13011 $
 *
 * Copyright (C) 2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.smiles;

/**
 * Parses a SMARTS String to create a <code>SmilesMolecule</code>.
 * The SMILES specification has been found at the
 * <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>.
 * An other explanation can be found in the
 * <a href="http://www.daylight.com/dayhtml/doc/theory/theory.smarts.html">Daylight Smarts Theory Manual</a>. <br>
 * 
 * Currently this parser supports only parts of the SMARTS specification. <br>
 * 
 * An example on how to use it:
 * <pre><code>
 * try {
 *   SmartsParser sp = new SmartsParser();
 *   SmilesMolecule sm = sp.parseSmarts("CC(C)C(=O)O");
 *   // Use the resulting molecule 
 * } catch (InvalidSmilesException e) {
 *   // Exception management
 * }
 * </code></pre>
 * 
 * @see <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>
 */
public class SmartsParser extends SmilesParser {

  public static SmilesSearch getMolecule(String smarts) throws InvalidSmilesException {
    return (new SmartsParser()).parse(smarts);
  }

  /**
   * Parses a SMARTS String
   * 
   * @param smarts SMILES String
   * @return Molecule corresponding to <code>smiles</code>
   * @throws InvalidSmilesException
   */
  SmilesSearch parse(String smarts) throws InvalidSmilesException {
    isSmarts = true;
    return super.parse(smarts);
  }
}
