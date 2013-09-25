/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-06-02 12:14:13 -0500 (Sat, 02 Jun 2007) $
 * $Revision: 7831 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// ImageEncoder - abstract class for writing out an image
//
// Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package org.jmol.image;

import java.io.IOException;
import java.util.Map;

import org.jmol.api.ApiPlatform;
import org.jmol.api.Interface;
import org.jmol.io.JmolOutputChannel;


/** 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public abstract class ImageEncoder {

  protected JmolOutputChannel out;

  protected int width = -1;
  protected int height = -1;
  //protected int[][] rgbPixels;
  
  /**
   * @param apiPlatform 
   * @param type 
   * @param objImage 
   * @param out 
   * @param params 
   * @param errRet  
   * @return  true if successful
   */
  public static boolean write(ApiPlatform apiPlatform, String type,
                              Object objImage, JmolOutputChannel out,
                              Map<String, Object> params, String[] errRet) {
    ImageEncoder ie = (ImageEncoder) Interface
        .getInterface("org.jmol.image." + type + "Encoder");
    if (ie == null) {
      errRet[0] = "Image encoder type " + type + " not available";
      return false;
    }
    ie.encode(apiPlatform, objImage, out, params);
    try {
      ie.createImage();
    } catch (IOException e) {
      errRet[0] = e.toString();
      out.cancel();
      return false;
    } finally {
      ie.close();
    }
    return true;
  }

  abstract protected void setParams(Map<String, Object> params);
  abstract protected void createImage() throws IOException;

  protected int[] pixels;

  private void encode(ApiPlatform apiPlatform, Object objImage,
                      JmolOutputChannel out, Map<String, Object> params) {
    this.out = out;
    setParams(params);
    width = apiPlatform.getImageWidth(objImage);
    height = apiPlatform.getImageHeight(objImage);
    /**
     * @j2sNative
     *
     * pixels = null;
     * 
     */
    {
      pixels = new int[width * height];
    }
    //rgbPixels = new int[height][width];
    pixels = apiPlatform.grabPixels(objImage, width, height, pixels, 0, height);
    //for (int row = 0; row < height; ++row)
      //System.arraycopy(pixels, row * width,
        //  rgbPixels[row], 0, width);
  }

  protected void close() {
    out.closeChannel();
  }

  protected void putString(String str) {
    out.append(str);
  }

  protected void putByte(int b) {
    out.writeByteAsInt(b);
  }
  
}
