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

package org.jmol.export.image;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.jmol.api.JmolImageCreatorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.util.Base64;
import org.jmol.util.JpegEncoder;
import org.jmol.util.Logger;

public class ImageCreator implements JmolImageCreatorInterface {
  
  JmolViewer viewer;
  
  
  public ImageCreator() {
    // can set viewer later
  }
  
  public ImageCreator(JmolViewer viewer){
    this.viewer = viewer;
  }
 
  public void setViewer(JmolViewer viewer) {
    this.viewer = viewer;
  }
  
  public void clipImage(String text) {
    if (text == null) {
      ImageSelection.setClipboard(viewer.getScreenImage());
      viewer.releaseScreenImage();
      return;
    }
    ImageSelection.setClipboard(text);
  }

  public String getClipboardText() {
    return ImageSelection.getClipboardText();
  }
  
  public static String getClipboardTextStatic() {
    return ImageSelection.getClipboardText();
  }

  public String createImage(String fileName, String type, Object text_bytes,
                            int quality) {
    boolean isBytes = (text_bytes instanceof byte[]);
    String text = (isBytes ? null : (String) text_bytes);
    boolean isText = (quality == Integer.MIN_VALUE);
    if (fileName == null) {
      clipImage(text);
      return "OK " + text.length();
    }
    if ((isText || isBytes) && text_bytes == null)
      return "NO DATA";
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(fileName);
      if (isBytes) {
        os.write((byte[]) text_bytes);
        os.flush();
        os.close();
      } else if (isText) {
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw, 8192);
        bw.write(text);
        bw.close();
        os = null;
      } else {
        Image eImage = viewer.getScreenImage();
        if (type.equalsIgnoreCase("JPEG") || type.equalsIgnoreCase("JPG")) {
          if (quality <= 0)
            quality = 75;
          (new JpegEncoder(eImage, quality, os)).Compress();
        } else if (type.equalsIgnoreCase("JPG64")) {
          ByteArrayOutputStream osb = new ByteArrayOutputStream();
          (new JpegEncoder(eImage, quality, osb)).Compress();
          osb.flush();
          osb.close();
          StringBuffer jpg = Base64.getBase64(osb.toByteArray());
          os.write(Base64.toBytes(jpg));
        } else if (type.equalsIgnoreCase("PNG")) {
            if (quality < 0)
              quality = 2;
            else if (quality > 9)
              quality = 9;
          byte[] pngbytes = (new PngEncoder(eImage, false,
              PngEncoder.FILTER_NONE, quality)).pngEncode();
          os.write(pngbytes);
        } else if (type.equalsIgnoreCase("PPM")) {
          (new PpmEncoder(eImage, os)).encode();
        } else if (type.equalsIgnoreCase("GIF")) {
          (new GifEncoder(eImage, os)).encode();
        }
        os.flush();
        os.close();
        viewer.releaseScreenImage();
      }
    } catch (IOException exc) {
      viewer.releaseScreenImage();
      if (exc != null) {
        Logger.error("IO Exception", exc);
        return exc.toString();
      }
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          Logger.error("IO Exception", e);
        }
      }
    }
    return "OK " + type + " " + (new File(fileName)).length() + " " + fileName
        + (quality == Integer.MIN_VALUE ? "" : "; quality=" + quality);
  }
}
