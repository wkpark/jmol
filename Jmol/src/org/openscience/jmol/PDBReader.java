/*
 * PDBReader.java    1.0 99/10/19
 *
 * Copyright (c) 1999 The University of Notre Dame. All Rights Reserved.
 *
 * The University of Notre Dame grants you ("Licensee") a
 * non-exclusive, royalty free, license to use, modify and
 * redistribute this software in source and binary code form, provided
 * that the following conditions are met:
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
 * EXCLUDED.  THE UNIVERSITY OF NOTRE DAME AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL THE UNIVERSITY OF NOTRE DAME OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF THE
 * UNIVERSITY OF NOTRE DAME HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import java.io.*;
import java.util.Vector;
import java.util.StringTokenizer;


/**
 * A reader for PDB (Protein Data Bank) files.
 *
 * <p>PDB files are a very widely used method of communicating
 * structural information about biomolecules.  The column position of a 
 * field within a given line governs how that field is interpreted.
 *
 * <p>Only the END, ATOM and HETATM command strings are processed for
 * now, and the ATOM and HETATM entries are used only for coordinate
 * information.  We would, of course, gladly accept code donations
 * that parse more of the detailed information contained within PDB
 * files.
 *
 * <p>A full specification of the PDB format is available at:
 *    http://www.rcsb.org/pdb/docs/format/pdbguide2.2/guide2.2_frame.html
 *
 * <p>PDB files also contain only a single frame.
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Brookhaven National Labs or the Research Collaboratory
 * for Structural Bioinformatics.  If you have problems, please
 * contact the author of this code, not the operators of the Protein
 * Data Bank.
 *
 * @author J. Daniel Gezelter (gezelter.1@nd.edu)
 * @version 1.0 
 */
public class PDBReader implements ChemFileReader {
    /**
     * Create an PDB file reader.
     *
     * @param input source of PDB data
     */
    public PDBReader(Reader input) {
        this.input = new BufferedReader(input);
    }
    
    /**
     * Read the PDB output.
     *
     * @return a ChemFile with the coordinates, charges, vectors, etc.
     * @exception IOException if an I/O error occurs
     */
    public ChemFile read() throws IOException, Exception {
        ChemFile file = new ChemFile();
        ChemFrame frame = new ChemFrame();
        StringTokenizer st;
        
        String line = input.readLine();
        while (input.ready() && line != null) {
            st = new StringTokenizer(line, "\t ,;");

            String command;

            try {
                command = new String(line.substring(0, 6).trim());
            } catch (StringIndexOutOfBoundsException sioobe) { break; }
            
            if (command.equalsIgnoreCase("ATOM") || 
                command.equalsIgnoreCase("HETATM")) {
                
                String atype = new String(line.substring(13, 14).trim());
                String sx = new String(line.substring(29, 38).trim());
                String sy = new String(line.substring(38, 46).trim());
                String sz = new String(line.substring(46, 54).trim());
                
                double x = FortranFormat.atof(sx);
                double y = FortranFormat.atof(sy);
                double z = FortranFormat.atof(sz);
                frame.addVert(atype, (float) x, (float) y, (float) z);
            }

            if (command.equalsIgnoreCase("END")) {
                file.frames.addElement(frame);
                return file;
            }
            
            line = input.readLine();
        }
        // No END marker, so just wrap things up as if we had seen one:
        file.frames.addElement(frame);
        return file;
    }
    
    private BufferedReader input;
}
