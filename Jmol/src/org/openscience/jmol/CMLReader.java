/*
 * @(#)CMLReader.java    0.2 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.jmol;

import java.io.Reader;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.openscience.cdopi.*;
import org.openscience.cml.*;

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
    public ChemFile read() throws IOException {
        try {
            InputSource source = new InputSource(input);
            Parser parser = ParserFactory.makeParser("com.microstar.xml.SAXDriver");
            EntityResolver resolver = new DTDResolver();
            JMolCDO cdo = new JMolCDO();
            CMLHandler handler = new CMLHandler(cdo);
            ((CMLHandler)handler).registerConvention("JMOL-ANIMATION", new JMOLANIMATIONConvention(cdo));
            parser.setEntityResolver(resolver);
            parser.setDocumentHandler(handler);
            parser.parse(source);
            ChemFile file = new ChemFile();
            Enumeration framesIter = ((JMolCDO)handler.returnCDO()).returnChemFrames().elements();
            while (framesIter.hasMoreElements()) {
                System.err.println("New Frame!!!!!!!");
                file.frames.addElement((ChemFrame)framesIter.nextElement());
            }
            return file;
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex.toString());
        } catch (InstantiationException ex) {
            throw new IOException(ex.toString());
        } catch (SAXException ex) {
            throw new IOException(ex.toString());
        } catch (IllegalAccessException ex) {
            throw new IOException(ex.toString());
        }
    }

    /**
     * The source for CML data.
     */
    private Reader input;
}
