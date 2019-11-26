/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * distanceJMolecule.java
 *
 * Created on 03 April 2006, 17:05
 *
 */

package org.openscience.jmol.app.janocchio;

import javajs.util.BS;

import org.jmol.modelset.Atom;
import org.jmol.quantum.NMRCalculation;
import org.jmol.quantum.NoeMatrix;

/**
 * Connection to NMRCalculation allowing for a CDK version as well (which is no
 * longer supported because of the 25 MB hit from the CDK library and because
 * Jmol already provides smart ways to group the hydrogen atoms).
 * 
 * @author Bob Hanson
 */
public class DistanceJMoleculeNoCDK extends DistanceJMolecule {

}
