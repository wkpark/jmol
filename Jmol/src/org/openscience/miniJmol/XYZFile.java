/*
 * @(#)XYZFile.java    1.0 98/08/27
 *
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
import java.util.Vector;
import java.util.StringTokenizer;

public class XYZFile extends ChemFile {

    /**
     * XYZ files may contain multiple ChemFrame objects, and may have charges
     * and vector information contained along with atom types and coordinates.
     * XYZ files <em>must</em> have a number of atoms at the beginning of each
     * line then another line (which may be blank) to identify each frame.
     * @see ChemFrame
     * @param is input stream for the XYZ file
     */
    public XYZFile(InputStream is) throws Exception {
        super();

        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1024);                 
        while (true) {
            try {
                ChemFrame cf = readFrame(r);                
                frames.addElement(cf);
                Vector fp = cf.getFrameProps();
                for (int i = 0; i < fp.size(); i++) {
                    if (PropertyList.indexOf(fp.elementAt(i)) < 0) {
                        PropertyList.addElement(fp.elementAt(i));
                    }
                }
                nframes++;
            } catch (JmolException e) {   
                break;
            }          
        }
    }

    /**
     * parses the next section of the XYZ file into a ChemFrame
     * @param r the reader that will supply the next part of the XYZ file
     */
    public ChemFrame readFrame(BufferedReader r) throws Exception {

        int na = 0;
        String info = "";
        StringTokenizer st;

        try {
            String l = r.readLine();
            if (l == null) throw new JmolException("XYZFile.readFrame", "no more frames!");
            st = new StringTokenizer(l, "\t ,;");
            String sn = st.nextToken();
            na = Integer.parseInt(sn);
            info = r.readLine();
            System.out.println(info);
            
            // OK, we got enough to start building a ChemFrame:
            

            ChemFrame cf = new ChemFrame(na);
            cf.setInfo(info);
            
            String s; // temporary variable used to store data as we read it
            
            for (int i = 0; i < na; i++) {
                s = r.readLine();
                if (s == null) break;
                if (!s.startsWith("#")) {          
                    double x = 0.0f, y = 0.0f, z = 0.0f, c = 0.0f;
                    double vect[] = new double[3];
                    st = new StringTokenizer(s, "\t ,;");
                    boolean readcharge = false;
                    boolean readvect = false;
                    int nt = st.countTokens();
                    switch (nt) {
                    case 1:
                    case 2:
                    case 3:
                        throw new JmolException("XYZFile.readFrame", 
                                                "Not enough fields on line.");
                        
                    case 5: // atype, x, y, z, charge                    
                        readcharge = true;
                        break;
                    case 7: // atype, x, y, z, vx, vy, vz
                        readvect = true;
                        break;
                    case 8: // atype, x, y, z, charge, vx, vy, vz
                        readcharge = true;
                        readvect = true;
                        break;
                    default: // 4, 6, or > 8  fields, just read atype, x, y, z
                        break;
                    }

                    String aname = st.nextToken();                    
                    String sx = st.nextToken();
                    String sy = st.nextToken();
                    String sz = st.nextToken();
                    x = FortranFormat.atof(sx);
                    y = FortranFormat.atof(sy);
                    z = FortranFormat.atof(sz);

                    Vector props = new Vector();
                    if (readcharge) {
                        String sc = st.nextToken();
                        c = FortranFormat.atof(sc);
                    }
                    
                    if (readvect) {
                        String svx = st.nextToken();
                        String svy = st.nextToken();
                        String svz = st.nextToken();
                        vect[0] = FortranFormat.atof(svx);
                        vect[1] = FortranFormat.atof(svy);
                        vect[2] = FortranFormat.atof(svz);
                    }
                    
                    if (readcharge || readvect) {
                        cf.addPropertiedVert(aname, 
                                             (float) x,
                                             (float) y, 
                                             (float) z, 
                                             props);
                    } else 
                        cf.addVert(aname, 
                                   (float) x, 
                                   (float) y, 
                                   (float) z);
                }
            }
            return cf;
        } catch (Exception e) {
            throw e;
        }
        
    }
    
}
