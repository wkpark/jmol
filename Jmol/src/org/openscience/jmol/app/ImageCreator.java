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

package org.openscience.jmol.app;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.util.Base64;
import org.jmol.util.JpegEncoder;
import org.jmol.util.Logger;

import Acme.JPM.Encoders.PpmEncoder;

public class ImageCreator {
  
  JmolViewer viewer;
  StatusBar status;
  
  public ImageCreator(JmolViewer viewer, StatusBar status) {
    this.viewer = viewer;
    this.status = status;
  }
 
  void clipImage(String text) {
    if (text == null) {
      Image eImage = viewer.getScreenImage();
      ImageSelection.setClipboard(eImage);
      viewer.releaseScreenImage();
      return;
    }
    ImageSelection.setClipboard(text);
  }
  
  public void createImage(String fileName, String type_or_text, int quality) {
    boolean isText = (quality == Integer.MIN_VALUE);
    if (fileName == null) {
      clipImage(type_or_text);
      return;
    }
    try {
      FileOutputStream os = new FileOutputStream(fileName);
      if (isText) {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os), 8192);
        bw.write(type_or_text);
        bw.close();
        os = null;
      } else {
        Image eImage = viewer.getScreenImage();
        if (type_or_text.equalsIgnoreCase("JPEG") || type_or_text.equalsIgnoreCase("JPG")) {
          JpegEncoder jc = new JpegEncoder(eImage, quality, os);
          jc.Compress();
        } else if (type_or_text.equalsIgnoreCase("PPM")) {
          PpmEncoder pc = new PpmEncoder(eImage, os);
          pc.encode();
        } else if (type_or_text.equalsIgnoreCase("PNG")) {
          PngEncoder png = new PngEncoder(eImage);
          png.setCompressionLevel(quality == 0 ? 2 : quality); //reasonable? 500x500 is 38K
          byte[] pngbytes = png.pngEncode();
          os.write(pngbytes);
        } else if (type_or_text.equalsIgnoreCase("JPG64")) {
          ByteArrayOutputStream osb = new ByteArrayOutputStream();
          JpegEncoder jc = new JpegEncoder(eImage, quality, osb);
          jc.Compress();
          osb.flush();
          osb.close();
          StringBuffer jpg = Base64.getBase64(osb.toByteArray());
          os.write(Base64.toBytes(jpg));
        }
        os.flush();
        os.close();
        viewer.releaseScreenImage();
      }
    } catch (IOException exc) {
      viewer.releaseScreenImage();
      if (exc != null) {
        if (status != null) {
          status.setStatus(1, GT._("IO Exception:"));
          status.setStatus(2, exc.toString());
        }
        Logger.error("IO Exception", exc);
      }
    }
  }
}
