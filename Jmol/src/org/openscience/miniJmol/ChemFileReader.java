
/*
 * ChemFileReader.java
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
package org.openscience.miniJmol;

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
	ChemFile read(StatusDisplay putStatus, boolean bondsEnabled)
			throws IOException;
}
