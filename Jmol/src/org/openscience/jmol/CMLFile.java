/*
 * @(#)CMLFile.java    0.2 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.jmol;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.openscience.cdopi.*;
import org.openscience.cml.*;

public class CMLFile extends ChemFile {

  private static final String pClass = "com.microstar.xml.SAXDriver";

  private Vector cfs;
  private int retFrame;

    /**
     * CML files contain a single ChemFrame object.
     * @see ChemFrame
     * @param is input stream for the PDB file
     */
    public CMLFile(InputStream is) throws Exception {        

        super();

        InputSource input = new InputSource(is);
        Parser parser = ParserFactory.makeParser(pClass);
        EntityResolver resolver = new DTDResolver();
        DocumentHandler handler = new CMLHandler((CDOInterface)new JMolCDO());
	handler.setDebug();
        parser.setEntityResolver(resolver);
        parser.setDocumentHandler(handler);
        parser.parse(input);
        JMolCDO cdo = (JMolCDO)((CMLHandler)handler).returnCDO();
        frames = cdo.returnChemFrames();
	System.out.println("Back in CMLFile...");
    }
}
