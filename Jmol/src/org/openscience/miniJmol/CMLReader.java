
/*
 * @(#)CMLReader.java    0.2 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.miniJmol;

import java.io.Reader;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

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
			DocumentHandler handler = new CMLHandler(bondsEnabled);
			parser.setEntityResolver(resolver);
			parser.setDocumentHandler(handler);
			parser.parse(source);
			ChemFile file = new ChemFile(bondsEnabled);
			Enumeration framesIter =
				((CMLHandler) handler).returnChemFrames().elements();
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
