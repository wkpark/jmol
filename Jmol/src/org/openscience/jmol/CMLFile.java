/*
 * @(#)CMLFile.java    0.2 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
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
        parser.setEntityResolver(resolver);
        parser.setDocumentHandler(handler);
        parser.parse(input);
        JMolCDO cdo = (JMolCDO)((CMLHandler)handler).returnCDO();
        frames = cdo.returnChemFrames();
	System.out.println("Back in CMLFile...");
    }
}
