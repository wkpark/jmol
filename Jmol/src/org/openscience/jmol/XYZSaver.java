/*
 * @(#)XYZSaver.java    1.0 99/06/09
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

package org.openscience.jmol;

import java.util.Vector;
import java.io.*;

public class XYZSaver extends FileSaver {

    /**
     * Constructor.
     * @param cf the ChemFile to dump.
     * @param out the stream to write the XYZ file to.
     */
    public XYZSaver( ChemFile cf, OutputStream out ) throws IOException {
        super( cf, out );
    }

    public void writeFileStart(ChemFile cf, BufferedWriter w) throws IOException{
        // No preamble for XYZ Files
    }
    public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException{
        // No postamble for XYZ Files
    }

    /**
     * writes a single frame in XYZ format to the Writer.
     * @param cf the ChemFrame to write
     * @param w the Writer to write it to
     */
    public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {

        int na = 0;
        String info = "";
        String st = "";
        String tab = "\t";
        boolean writecharge = false;
        boolean writevect = false;

        try {

            String s1 = new Integer(cf.getNvert()).toString() + "\n";
            w.write(s1, 0, s1.length());
            
            String s2 = cf.getInfo();
            if (s2 == null) {
                w.newLine();
            } else {
                w.write(s2 + "\n", 0, s2.length()+1);
            }

            Vector fp = cf.getAtomProps();

            // Create some dummy properties:
            Charge c = new Charge(0.0);
            double[] vect = new double[3];
            vect[0] = 0.0;
            vect[1] = 0.0;
            vect[2] = 0.0;
            VProperty vp = new VProperty(vect);

            // test if we have charges or vectors in this frame:
            for (int i = 0; i < fp.size(); i++) {
                String prop = (String) fp.elementAt(i);
                if (prop.equals(c.getDescriptor())) writecharge = true;
                if (prop.equals(vp.getDescriptor())) writevect = true;
            }

            // Loop through the atoms and write them out:

            for (int i = 0; i < cf.getNvert(); i++) {
                
                AtomType a = cf.getAtomType(i);
                st = a.getBaseAtomType().getName();
                
                double[] pos = cf.getVertCoords(i);
                st = st + tab + 
                    new Double(pos[0]).toString() + tab + 
                    new Double(pos[1]).toString() + tab + 
                    new Double(pos[2]).toString();

                Vector props = cf.getVertProps(i);

                if (writecharge) {
                    for (int j = 0; j < props.size(); j++) {
                        PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
                        String desc = p.getDescriptor();
                        if (desc.equals(c.getDescriptor())) {
                            Charge ct = (Charge) p;
                            st = st + tab + ct.stringValue();
                        }
                    }
                }
                
                if (writevect) {
                    for (int j = 0; j < props.size(); j++) {
                        PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
                        String desc = p.getDescriptor();
                        if (desc.equals(vp.getDescriptor())) {
                            VProperty vt = (VProperty) p;
                            double[] vtmp;
                            vtmp = vt.getVector();
                            st = st + tab + 
                                new Double(vtmp[0]).toString() + tab +
                                new Double(vtmp[1]).toString() + tab + 
                                new Double(vtmp[2]).toString();
                        }
                    }
                }
                st = st + "\n";
                w.write(st, 0, st.length());

            }
                
        } catch (IOException e) {
            throw e;
        }
        
    }
}
