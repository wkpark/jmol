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

package org.jmol.export.image;

import java.util.Hashtable;
import java.util.Map;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.IOException;

import org.jmol.api.Interface;
import org.jmol.io.JmolOutputChannel;
import org.jmol.util.J2SIgnoreImport;

/** Abstract class for writing out an image.
 *  <P>
 *  A framework for classes that encode and write out an image in
 *  a particular file format.
 *  <P>
 *  This provides a simplified rendition of the ImageConsumer interface.
 *  It always delivers the pixels as ints in the RGBdefault color model.
 *  It always provides them in top-down left-right order.
 *  If you want more flexibility you can always implement ImageConsumer 
 *  directly.
 *  <P>
 *  <A HREF="/resources/classes/Acme/JPM/Encoders/ImageEncoder.java">Fetch the software.</A><BR>
 *  <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
 * <P>
 * @see GifEncoder
 * @see PpmEncoder
 * 
 * See http://www.acme.com/java/
 * 
 * extensively modified for Jmol
 * 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

@J2SIgnoreImport({java.awt.Image.class, java.awt.image.ColorModel.class, 
  java.awt.image.ImageConsumer.class, java.awt.image.ImageProducer.class})
public abstract class ImageEncoder implements ImageConsumer {

  protected JmolOutputChannel out;

  protected int width = -1;
  protected int height = -1;

  private ImageProducer producer;
  private int hintflags = 0;
  private boolean started = false;
  private boolean encoding;
  private IOException iox;  
  
  public static boolean write(String type, Object image, JmolOutputChannel out,
                             Map<String, Object> params) throws IOException {
    ImageEncoder ie = (ImageEncoder) Interface
        .getInterface("org.jmol.export.image." + type + "Encoder");
    if (ie == null)
      return false;
    ie.setParams(params);
    /**
     * @j2sNative
     * 
     *  ie.producer = image; 
     */
    {
      ie.producer = (image == null ? null : ((Image) image).getSource());
    }
    ie.out = out;    
    if (ie.producer != null)
      ie.encode();
    ie.close();
    return true;
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

  // Methods that subclasses implement.

  abstract protected void setParams(Map<String, Object> params);

  /// Subclasses implement this to initialize an encoding.
  abstract protected void encodeStart() throws IOException;

  /// Subclasses implement this to actually write out some bits.  They
  // are guaranteed to be delivered in top-down-left-right order.
  // One int per pixel, index is row * scansize + off + col,
  // RGBdefault (AARRGGBB) color model.
  abstract protected void encodePixels(int x, int y, int w, int h, int[] rgbPixels,
                             int off, int scansize) throws IOException;

  /// Subclasses implement this to finish an encoding.
  abstract protected void encodeDone() throws IOException;

  // Our own methods.

  /// Call this after initialization to get things going.
  protected synchronized void encode() throws IOException {
    iox = null;
    /**
     * @j2sNative
     *
     * this.width = this.producer.width;
     * this.height= this.producer.height;
     * this.accumulator = this.producer.buf32;
     * this.encodeStart();
     * this.encodeFinish();
     * this.encodeDone();
     * 
     */
    {
    encoding = true;
    producer.startProduction(this);
    while (encoding)
      try {
        wait();
      } catch (InterruptedException e) {
      }
    if (iox != null)
      throw iox;
    }
  }

  private int[] accumulator;

  private void encodePixelsWrapper(int x, int y, int w, int h, int[] rgbPixels,
                                   int off, int scansize) throws IOException {
    if (!started) {
      started = true;
      encodeStart();
      if ((hintflags & TOPDOWNLEFTRIGHT) == 0)
        accumulator = new int[width * height];
    }
    if (accumulator != null)
      for (int row = 0; row < h; ++row)
        System.arraycopy(rgbPixels, row * scansize + off, accumulator,
            (y + row) * width + x, w);
    else
      encodePixels(x, y, w, h, rgbPixels, off, scansize);
  }

  private void encodeFinish() throws IOException {
    if (accumulator != null) {
      encodePixels(0, 0, width, height, accumulator, 0, width);
      accumulator = null;
    }
  }

  private synchronized void stop() {
    encoding = false;
    notifyAll();
  }

  // Methods from ImageConsumer.

  public void setDimensions(int width, int height) { // from image dimensions
    this.width = width; 
    this.height = height;
  }

  public void setProperties(Hashtable<?, ?> props) {
    // Ignore.
  }

  public void setColorModel(ColorModel model) {
    // Ignore.
  }

  public void setHints(int hintflags) {
    this.hintflags = hintflags;
  }

  public void setPixels(int x, int y, int w, int h, ColorModel model,
                        byte[] pixels, int off, int scansize) {
    int[] rgbPixels = new int[w];
    for (int row = 0; row < h; ++row) {
      int rowOff = off + row * scansize;
      for (int col = 0; col < w; ++col)
        rgbPixels[col] = model.getRGB(pixels[rowOff + col] & 0xff);
      try {
        encodePixelsWrapper(x, y + row, w, 1, rgbPixels, 0, w);
      } catch (IOException e) {
        iox = e;
        stop();
        return;
      }
    }
  }

  public void setPixels(int x, int y, int w, int h, ColorModel model,
                        int[] pixels, int off, int scansize) {
    if (model == ColorModel.getRGBdefault()) {
      try {
        encodePixelsWrapper(x, y, w, h, pixels, off, scansize);
      } catch (IOException e) {
        iox = e;
        stop();
        return;
      }
    } else {
      int[] rgbPixels = new int[w];
      for (int row = 0; row < h; ++row) {
        int rowOff = off + row * scansize;
        for (int col = 0; col < w; ++col)
          rgbPixels[col] = model.getRGB(pixels[rowOff + col]);
        try {
          encodePixelsWrapper(x, y + row, w, 1, rgbPixels, 0, w);
        } catch (IOException e) {
          iox = e;
          stop();
          return;
        }
      }
    }
  }

  public void imageComplete(int status) {
    producer.removeConsumer(this);
    if (status == ImageConsumer.IMAGEABORTED)
      iox = new IOException("image aborted");
    else {
      try {
        encodeFinish();
        encodeDone();
      } catch (IOException e) {
        iox = e;
      }
    }
    stop();
  }

}
