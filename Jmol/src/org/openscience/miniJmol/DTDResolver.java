/*
 * @(#)DTDResolver.java    1.0 99/06/09
 *
 * Copyright (c) 1999 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.miniJmol;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import java.net.URL;
import java.io.*;

public class DTDResolver implements EntityResolver {

    public InputSource resolveEntity (String publicId, String systemId) {
        String s = new String("");      
        BufferedReader r = new BufferedReader(new StringReader(s));
        return new InputSource(r);
    }    
}
/*
public class DTDResolver implements EntityResolver {

        public InputSource resolveEntity (String publicId, String systemId) {
        if (systemId.equalsIgnoreCase("cml.dtd") || 
            systemId.equalsIgnoreCase("CML-1999-05-15.dtd")) {
            try
                {
                    String fname = "org/openscience/miniJmol/Data/cml.dtd";
//                    String fname = "cml.dtd";
                    URL url = getClass().getClassLoader().getResource(fname);
//                    URL url = new URL(fname);
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
*/
