// Version 1.0a
// Copyright (C) 1998, James R. Weeks and BioElectroMech.
// Visit BioElectroMech at www.obrador.com.  Email James@obrador.com.

// See license.txt for details about the allowed used of this software.
// This software is based in part on the work of the Independent JPEG Group.
// See IJGreadme.txt for details about the Independent JPEG Group's license.

// This encoder is inspired by the Java Jpeg encoder by Florian Raemy,
// studwww.eurecom.fr/~raemy.
// It borrows a great deal of code and structure from the Independent
// Jpeg Group's Jpeg 6a library, Copyright Thomas G. Lane.
// See license.txt for details 


/*
 * JpegEncoder and its associated classes are Copyright (c) 1998, James R. Weeks and BioElectroMech
 * see(Jmol/src/com/obrador/license.txt)
 * 
 * Jmol.src.org.jmol.util.JpegEncoder.java was adapted by Bob Hanson
 * for Jmol in the following ways:
 * 
 * 1) minor coding efficiencies were made in some for() loops.
 * 2) methods not used by Jmol were commented out
 * 3) method and variable signatures were modified to provide 
 *    more appropriate method privacy.
 * 4) additions for Java2Script compatibility 
 * 
 * Original files are maintained in the Jmol.src.com.obrador package, but
 * these original files are not distributed with Jmol.
 *   
*/

package org.jmol.export.image;

import java.io.IOException;
import java.util.Map;

import org.jmol.io.Base64;
import org.jmol.io.JmolOutputChannel;

public class Jpg64Encoder extends JpgEncoder {

  
  private JmolOutputChannel outTemp;

  @Override
  protected void setParams(Map<String, Object> params) {
    defaultQuality = 75;
    outTemp = (JmolOutputChannel) params.remove("outputChannelTemp");
    super.setParams(params);
  }

  @Override
  protected void createImage() throws IOException {
    JmolOutputChannel out0 = out;
    out = outTemp;
    super.createImage();
    byte[] bytes = Base64.getBytes64(out.toByteArray());
    outTemp = null;
    out = out0;
    out.writeBytes(bytes, 0, bytes.length);
  }

}
