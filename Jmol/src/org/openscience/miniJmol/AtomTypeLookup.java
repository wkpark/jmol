/*
 * @(#)AtomTypeLookup.java    1.0 98/11/04
 * Heavily stripped down version of AtomTypeTable.java 
 *
 * Copyright (c) 1998 T.Grey All Rights Reserved.
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
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

import java.io.*;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.util.*;

public class AtomTypeLookup{
    private boolean DEBUG = false;

    /* 
       OK, this is going to be a bit confusing:
       There is a TableModel that is used to display the spreadsheet-like
       list of atom types.   However, we use a Hashtable to do (relatively)
       fast lookups when we need to know what atom Type is what.  So, the
       data is stored twice.  For now, I am treating the TableModel as
       the primary source that we use to load up the Hashtable.
    */

    AtomTypesModel atModel;
    static AtomType defaultAtomType;
    
    public AtomTypeLookup(String propertiesURL, boolean URLnotFile) {
        // Create a model of the data.
        atModel = new AtomTypesModel();
        if (URLnotFile){
            java.net.URL myURL;
            try{
              myURL = new java.net.URL(propertiesURL);
            }catch(java.net.MalformedURLException e){
              throw new RuntimeException("Properties URL is malformed: "+e);
            }
            try{
               ReadAtypes(myURL.openStream());
            }catch(Exception e){
              throw new RuntimeException("Got Exception trying to read properties: "+e);
            }
        }else{
            try{
               ReadAtypes(new java.io.FileInputStream(propertiesURL));
            }catch(Exception e){
              throw new RuntimeException("Got Exception trying to read properties: "+e);
            }
        }
    }

    public AtomTypeLookup(java.net.URL propertiesURL) {
        // Create a model of the data.
        atModel = new AtomTypesModel();
        try{
           ReadAtypes(propertiesURL.openStream());
        }catch(Exception e){
           throw new RuntimeException("Got Exception trying to read properties: "+e);
        }
    }
    
    void ReadAtypes(InputStream is) throws Exception {        
        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1024);
        StringTokenizer st; 

        String s;  
        
        atModel.clear();

        try {
            while (true) {
                s = r.readLine();
                if (s == null) break;
                if (!s.startsWith("#")) {          
                    String name = "";
                    String rootType = "";
                    int an = 0, rl = 0, gl = 0, bl = 0;
                    double mass = 0.0, vdw = 0.0, covalent = 0.0;
                    st = new StringTokenizer(s, "\t ,;");
                    int nt = st.countTokens();
                    
                    if (nt == 9) {
                        name = st.nextToken();                    
                        rootType = st.nextToken();
                        String san = st.nextToken();
                        String sam = st.nextToken();
                        String svdw = st.nextToken();
                        String scov = st.nextToken();
                        String sr = st.nextToken();
                        String sg = st.nextToken();
                        String sb = st.nextToken();
                        
                        try {
                            mass = new Double(sam).doubleValue();
                            vdw = new Double(svdw).doubleValue();
                            covalent = new Double(scov).doubleValue();
                            an = new Integer(san).intValue();
                            rl = new Integer(sr).intValue(); 
                            gl = new Integer(sg).intValue(); 
                            bl = new Integer(sb).intValue(); 
                        } catch (NumberFormatException nfe) {
                            throw new JmolException("AtomTypeTable.ReadAtypes",
                                                    "Malformed Number");
                        }
                        
                        AtomType at = new AtomType(name, rootType, an, mass, 
                                                   vdw, covalent, rl, gl, bl);

                        atModel.updateAtomType(at);
                        
                    } else {
                        throw new JmolException("AtomTypeTable.ReadAtypes", 
                                                "Wrong Number of fields");
                    }
                }
            }  // end while
            
            is.close();
            
        }  // end Try
        catch( IOException e) {}
        
    }
        
    public AtomType get(String name) {
        return atModel.get(name);
    }
    public AtomType get(int atomicNumber) {
        return atModel.get(atomicNumber);
    }
    
    public synchronized Enumeration elements() {
        return atModel.elements();
    }
}

