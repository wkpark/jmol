
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
            ((CMLHandler)handler).registerConvention("JMOL-ANIMATION", 
                                                     new JMOLANIMATIONConvention(cdo));
            parser.setEntityResolver(resolver);
            parser.setDocumentHandler(handler);
            parser.parse(source);
            ChemFile file = new ChemFile();
            Enumeration framesIter = ((JMolCDO)handler.returnCDO()).returnChemFrames().elements();
            while (framesIter.hasMoreElements()) {
                System.err.println("New Frame!!!!!!!");
                
                ChemFrame frame = (ChemFrame) framesIter.nextElement();
                
                file.frames.addElement(frame);
                
                Vector ap = frame.getAtomProps(); 
                for (int i = 0; i < ap.size(); i++) {
                    if (file.AtomPropertyList.indexOf(ap.elementAt(i)) < 0) {
                        file.AtomPropertyList.addElement(ap.elementAt(i));
                    }
                }
                
                Vector fp = frame.getFrameProps();

                for (int j = 0; j < fp.size(); j++) {
                    PhysicalProperty p = (PhysicalProperty) fp.elementAt(j);
                    String desc = p.getDescriptor();

                    // Update the frameProps if we found a new property
                    if (file.FramePropertyList.indexOf(desc) < 0) {
                        file.FramePropertyList.addElement(desc);
                    }
                }

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
