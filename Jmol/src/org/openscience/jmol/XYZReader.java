/*
 * XYZReader.java    1.0 99/10/06
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
 * A reader for XYZ Cartesian molecular model (XMol) files.
 * XMol is a closed source program similar in scope to Jmol.  
 * Details on XMol are available at http://www.msc.edu/docs/xmol/
 *
 * <p> XYZ files reference molecular geometris using a simple
 * cartesian coordinate system. Each XYZ file can contain multiple
 * frames for the purposes of animation.  Each frame in the animation
 * is represented by a two line header, followed by one line for each atom.
 *
 * <p> The first line of a frame's header is the number of atoms in
 * that frame.  Only the integer is read, it may be preceded by white
 * space, and anything on the line after the integer is ignored.
 * 
 * <p> The second line of the header is the "info" string for the
 * frame.  The info line may be blank, or it may contain information
 * pertinent to that step, but it must exist, and it may only be one
 * line long.
 * 
 * <p> Each line describing a single atom contains 4, 5, 7, 8, or
 * possibly more fields separated by white space.  The first 4 fields
 * are always the same: the atom's type (a short string of
 * alphanumeric characters), and its x-, y-, and z-positions.
 * Optionally, extra fields may be used to specify a charge for the
 * atom, and/or a vector associated with the atoms.  If an input line
 * contains five or eight fields, the fifth field is interpreted as
 * the atom's charge; otherwise, a charge of zero is assumed.  If an
 * input line contains seven or eight fields, the last three fields
 * are interpreted as the components of a vector.  These components
 * should be specified in angstroms.  If there are more than eight
 * fields, only the first 4 are parsed by the reader, and all
 * additional fields are ignored.
 * 
 * <p>The XYZ format contains no connectivity information.  Jmol
 * attempts to generate connectivity information using the covalent
 * radii of the specified atomic types.  If the distance between two
 * atoms is less than the sum of their covalent radii (times a fudge
 * factor), they are considered bonded.
 *
 * <p> This reader was developed without the assistance or approval of
 * anyone from Network Computing Services, Inc. (the authors of XMol).
 * If you have problems, please contact the author of this code, not
 * the developers of XMol.
 *
 * @author J. Daniel Gezelter (gezelter.1@nd.edu)
 * @version 1.0 */
public class XYZReader implements ChemFileReader {
    /**
     * Create an XYZ output reader.
     *
     * @param input source of XYZ data
     */
    public XYZReader(Reader input) {
        this.input = new BufferedReader(input);
    }
    
    /**
     * Read the XYZ output.
     *
     * @return a ChemFile with the coordinates, charges, vectors, etc.
     * @exception IOException if an I/O error occurs
     */
    public ChemFile read() throws IOException, Exception {
        ChemFile file = new ChemFile();

        int na = 0;
        String info = "";
        StringTokenizer st;
        
        String line = input.readLine();
        while (input.ready() && line != null) {
            st = new StringTokenizer(line, "\t ,;");
            
            String sn = st.nextToken();
            na = Integer.parseInt(sn);
            info = input.readLine();
            System.out.println(info);
            
            ChemFrame frame = new ChemFrame(na);
            frame.setInfo(info);
            
            String s; // temporary variable used to store data as we read it
            
            for (int i = 0; i < na; i++) {
                s = input.readLine();
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
                        throw new JmolException("XYZReader.read", 
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
                        Charge cp = new Charge(c);
                        props.addElement(cp);
                    }
                    
                    if (readvect) {
                        String svx = st.nextToken();
                        String svy = st.nextToken();
                        String svz = st.nextToken();
                        vect[0] = FortranFormat.atof(svx);
                        vect[1] = FortranFormat.atof(svy);
                        vect[2] = FortranFormat.atof(svz);
                        VProperty vp = new VProperty(vect);
                        props.addElement(vp);
                    }
                    
                    if (readcharge || readvect) {
                        frame.addPropertiedVert(aname, 
                                                (float) x,
                                                (float) y, 
                                                (float) z, 
                                                props);
                    } else 
                        frame.addVert(aname, 
                                      (float) x, 
                                      (float) y, 
                                      (float) z);
                }
            }
            file.frames.addElement(frame);
            Vector fp = frame.getAtomProps();
            for (int i = 0; i < fp.size(); i++) {
                if (file.AtomPropertyList.indexOf(fp.elementAt(i)) < 0) {
                    file.AtomPropertyList.addElement(fp.elementAt(i));
                }
            }
            line = input.readLine();
        }
        return file;
    }
    
    private BufferedReader input;
}
