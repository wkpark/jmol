
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
package org.openscience.miniJmol;

import java.io.Reader;
import java.io.IOException;
import java.util.Enumeration;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.EntityResolver;
import org.xml.sax.DocumentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserFactory;
import org.openscience.cml.CMLHandler;

/**
 * CML files contain a single ChemFrame object.
 * @see ChemFrame
 */
public class CMLReader implements ChemFileReader {

	/**
	 * Creates a CML reader.
	 *
	 * @param input source of CML data
	 */
	public CMLReader(Reader input) {
		this.input = input;
	}

	/**
	 * Read the CML data.
	 */
	public ChemFile read(StatusDisplay putStatus, boolean bondsEnabled)
			throws IOException {

		try {
			InputSource source = new InputSource(input);
			Parser parser =
				ParserFactory.makeParser("com.microstar.xml.SAXDriver");
			EntityResolver resolver = new DTDResolver();
			JMolCDO cdo = new JMolCDO();
			CMLHandler handler = new CMLHandler(cdo);
			parser.setEntityResolver(resolver);
			parser.setDocumentHandler(handler);
			parser.parse(source);
			ChemFile file = new ChemFile(bondsEnabled);
			Enumeration framesIter =
				((JMolCDO) handler.returnCDO()).returnChemFrames().elements();
			while (framesIter.hasMoreElements()) {
				file.addFrame((ChemFrame) framesIter.nextElement());
			}
			return file;
		} catch (ClassNotFoundException ex) {
			throw new IOException("CMLReader exception: " + ex);
		} catch (InstantiationException ex) {
			throw new IOException("CMLReader exception: " + ex);
		} catch (SAXException ex) {
			throw new IOException("CMLReader exception: " + ex);
		} catch (IllegalAccessException ex) {
			throw new IOException("CMLReader exception: " + ex);
		}
	}

	/**
	 * The source for CML data.
	 */
	private Reader input;
}
