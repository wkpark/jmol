
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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import java.net.URL;
import java.io.*;

public class DTDResolver implements EntityResolver {

        public InputSource resolveEntity (String publicId, String systemId) {
        if (systemId.equalsIgnoreCase("cml.dtd") || 
            systemId.equalsIgnoreCase("CML-1999-05-15.dtd")) {
            try
                {
                    String fname = "org/openscience/jmol/Data/cml.dtd";
                    URL url = ClassLoader.getSystemResource(fname);
                    InputStream is = url.openStream();
                    BufferedReader r = new BufferedReader(new InputStreamReader(is));
                    return new InputSource(r);
                }
            catch(Exception exc) 
                {
                    System.out.println("Error while trying to read CML DTD: " + exc.toString());
                    return null;
                }

        } else {
            // use the default behaviour
            return null;
        }
    }    
}
