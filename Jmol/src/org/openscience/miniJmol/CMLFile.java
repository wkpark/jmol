/*
 * @(#)CMLFile.java    0.2 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.miniJmol;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import com.ibm.xml.parsers.*;

public class CMLFile extends ChemFile {

  private Vector cfs;
  private int retFrame;
  private AtomTypeLookup atlu;

    /**
     * CML files contain a single ChemFrame object.
     * @see ChemFrame
     * @param is input stream for the PDB file
     */
    public CMLFile(InputStream is, AtomTypeLookup at) throws Exception {        
        super();
        atlu = at;
        InputSource input = new InputSource(is);
        Parser parser = new SAXParser();
        //EntityResolver resolver = new DTDResolver();
        EntityResolver resolver = new DTDResolver();
        DocumentHandler handler = new CMLHandler(atlu);
	//        parser.setEntityResolver(resolver);
                parser.setEntityResolver(resolver);
        parser.setDocumentHandler(handler);
	parser.parse(input);
        frames = ((CMLHandler)handler).returnChemFrames ();
    }
}
