
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

import java.io.*;

public abstract class FileSaver {

  protected OutputStream out;

  private ChemFile cf;

  public FileSaver(ChemFile cf, OutputStream out) throws IOException {
    this.cf = cf;
    this.out = out;
  }

  public synchronized void writeFile() throws IOException {

    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out), 1024);
    writeFileStart(cf, w);
    int nframes = cf.getNumberFrames();
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
  abstract void writeFrame(ChemFrame cfr, BufferedWriter w)
          throws IOException;

  // Here in case we need to write preamble material before all frames.
  abstract void writeFileStart(ChemFile cf, BufferedWriter w)
          throws IOException;

  // Here in case we need to write postamble material after all frames.
  abstract void writeFileEnd(ChemFile cf, BufferedWriter w)
          throws IOException;
}
