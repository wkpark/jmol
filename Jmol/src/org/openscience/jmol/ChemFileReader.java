
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

import java.io.IOException;

/**
 * An interface for reading output from chemistry programs.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public interface ChemFileReader {

	/**
	 * Read the data.
	 *
	 * @return a ChemFile with the data.
	 * @exception IOException if an I/O error occurs
	 */
	public ChemFile read() throws IOException, Exception;
}
