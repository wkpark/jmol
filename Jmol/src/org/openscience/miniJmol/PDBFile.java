/*
 * @(#)PDBFile.java    1.0 98/08/27
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

public class PDBFile extends ChemFile {

    private AtomTypeLookup atlu;

    /**
     * PDB files contain a single ChemFrame object.  Only the END, ATOM and 
     * HETATM command strings are processed for now, and the ATOM and HETATM
     * entries are only used for coordinate information.
     * @see ChemFrame
     * @param is input stream for the PDB file
     */
    public PDBFile(InputStream is, AtomTypeLookup at) throws Exception {        

        super();
        atlu = at;

        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1024);         
        try {
            ChemFrame cf = readFrame(r);                
            frames.addElement(cf);
            nframes++;
        } catch (Exception e) { System.out.println(e.toString());}          
    }

    /**
     * parses the PDB file into a ChemFrame
     * @param r the reader that will supply the PDB file
     */
    public ChemFrame readFrame(BufferedReader r) throws Exception {

        StringTokenizer st;
        
        ChemFrame cf = new ChemFrame();
        cf.setAtomTypeLookup(atlu);
            
        String s; // temporary variable used to store data as we read it

        while (true) {
            try {
                s = r.readLine();
            } catch (IOException ioe) { 
                break;
            }
            if (s == null) break;

            String command;
            
            try {
                command = new String(s.substring(0, 6).trim());
            } catch (StringIndexOutOfBoundsException sioobe) { break; }
            
            if (command.equalsIgnoreCase("ATOM") || 
                command.equalsIgnoreCase("HETATM")) {
                
                String atype = new String(s.substring(13, 14).trim());
                String sx = new String(s.substring(29, 38).trim());
                String sy = new String(s.substring(38, 46).trim());
                String sz = new String(s.substring(46, 54).trim());
                
                double x = FortranFormat.atof(sx);
                double y = FortranFormat.atof(sy);
                double z = FortranFormat.atof(sz);
                cf.addVert(atype, (float) x, (float) y, (float) z);
            }
            
            if (command.equalsIgnoreCase("END")) {
                return cf;
            }
        }
        
        return cf;        
    }       
}
