/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Chemistry Development Kit (CDK) project
 *
 * Contact: steinbeck@ice.mpg.de, gezelter@maul.chem.nd.edu, egonw@sci.kun.nl
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.openscience.jmol;

import org.openscience.cdk.*;

/**
 * Class that provides convertor procedures to
 * convert CDK classes to Jmol (v5) classes and visa versa.
 *
 * Jmol is a Java 3D viewer specialized for viewing
 * animations and vibrational modes. It
 * can be found at: http://jmol.sourceforge.net/
 *
 * @author     egonw
 *
 * @keyword    Jmol
 * @keyword    class convertor
 */
public class Convertor {

    /**
     * Converts an org.openscience.cdk.Atom class into a
     * org.openscience.jmol.Atom class.
     *
     * Conversion includes:
     *   - atomic number
     *   - coordinates
     *
     * @param   atom    class to be converted
     * @return          converted class in Jmol
     **/
    public static org.openscience.jmol.Atom convert(org.openscience.cdk.Atom atom) {
        return new Atom(atom);
    }

    /**
     * Converts an org.openscience.jmol.Atom class into a
     * org.openscience.cdk.Atom class.
     *
     * Conversion includes:
     *   - atomic number
     *   - coordinates
     *
     * @param   atom    class to be converted
     * @return          converted class in CDK
     **/
    public static org.openscience.cdk.Atom convert(org.openscience.jmol.Atom atom) {
        return atom;
    }

    /**
     * Converts an org.openscience.cdk.AtomContainer class into a
     * org.openscience.cdk.ChemFrame class.
     *
     * Conversion includes:
     *   - atoms
     *
     * @param   ac      class to be converted
     * @return          converted class in Jmol
     **/
    public static ChemFrame convert(AtomContainer ac) {
        if (ac != null) {
            int NOatoms = ac.getAtomCount();
            ChemFrame converted = new ChemFrame(NOatoms);
            for (int i=0; i<NOatoms; i++) {
                org.openscience.cdk.Atom atom = ac.getAtomAt(i);
                javax.vecmath.Point3d xyz = atom.getPoint3D();
                if (atom.getPoint3D() != null) {
                } else if (atom.getPoint2D() != null) {
                    xyz =
                        new javax.vecmath.Point3d(
                            atom.getX2D(),
                            atom.getY2D(),
                            0.0
                        );
                } else {
                    xyz = new javax.vecmath.Point3d(0.0, 0.0, 0.0);
                }
                converted.addAtom(
                    atom.getSymbol(),
                    (float)xyz.x,
                    (float)xyz.y,
                    (float)xyz.z
                );
            }
            return converted;
        } else {
            return null;
        }
    }

    /**
     * Converts an org.openscience.jmol.ChemFrame class into a
     * org.openscience.cdk.AtomContainer class.
     *
     * Conversion includes:
     *   - atoms
     *
     * @param   atom    class to be converted
     * @return          converted class in CDK
     **/
    public static AtomContainer convert(org.openscience.jmol.ChemFrame mol) {
        if (mol != null) {
            AtomContainer converted = new AtomContainer();
            int NOatoms = mol.getNumberOfAtoms();
            for (int i=0; i<NOatoms; i++) {
                converted.addAtom(convert(mol.getAtomAt(i)));
            }
            return converted;
        } else {
            return null;
        }
    }

    /**
     * Converts an org.openscience.cdk.Crystal class into a
     * org.openscience.cdk.ChemFile class.
     *
     * Conversion includes:
     *   - atoms
     *   - unit cell axes
     *
     * @param   crystal class to be converted
     * @return          converted class in Jmol
     **/
    public static org.openscience.jmol.CrystalFile convertCrystal(Crystal crystal) {
        if (crystal != null) {
            // first convert content
            org.openscience.jmol.ChemFile file = new org.openscience.jmol.ChemFile();
            file.addFrame(convert((AtomContainer)crystal));
            // now add unit cell info
            double[][] rprim = new double[3][3];
            double[] acell = new double[3];
            double[] a = crystal.getA();
            for (int i=0; i<3; i++) {
                rprim[0][i] = a[i];
            }
            double[] b = crystal.getB();
            for (int i=0; i<3; i++) {
                rprim[1][i] = b[i];
            }
            double[] c = crystal.getC();
            for (int i=0; i<3; i++) {
                rprim[2][i] = c[i];
            }
            for (int i=0; i<3; i++) {
                acell[i] = 1.0;
            }
            CrystalFile converted = new CrystalFile(file, rprim, acell);
            return converted;
        } else {
            return null;
        }
    }

}
