/*
 * @(#)FileSaver.java    1.0 99/06/09
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

import java.io.*;

public abstract class FileSaver {

    protected OutputStream out;
    
    private ChemFile cf;
   
    public FileSaver(ChemFile cf, OutputStream out) throws IOException {
        this.cf = cf;
        this.out = out;
    }

    public synchronized void writeFile() throws IOException {

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out), 
                                              1024);                         
        writeFileStart(cf, w);
        int nframes = cf.nFrames();
        try {
            for (int i = 0; i < nframes; i++) {
                ChemFrame cfr = cf.getFrame(i);
                writeFrame(cfr, w);
            }
        } catch (IOException e) {
            throw e;
        }
        writeFileEnd(cf, w);
        w.flush();
        w.close();
        out.flush();
        out.close();
    }
    
    // Methods that subclasses implement.
    // All of the work is done in writeFrame.  
    abstract void writeFrame(ChemFrame cfr, BufferedWriter w) throws IOException;

    // Here in case we need to write preamble material before all frames.
    abstract void writeFileStart(ChemFile cf, BufferedWriter w) throws IOException;

    // Here in case we need to write postamble material after all frames.
    abstract void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException;
}
