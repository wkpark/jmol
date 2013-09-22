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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.jmol.api.JmolImageCreatorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.io.Base64;
import org.jmol.io2.JpegEncoder;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class GenericImageCreator implements JmolImageCreatorInterface {
  
  protected Viewer viewer;
  private double privateKey;
  
  public GenericImageCreator() {
    // by reflection
  }
  
  public JmolImageCreatorInterface setViewer(JmolViewer viewer, double privateKey) {
    this.viewer = (Viewer) viewer;
    this.privateKey = privateKey;
    return this;
  }
  
  private static int getInt(Map<String, Object> params, String key, int def) {
    Integer p = (Integer) params.get(key);
    return (p == null ? def : p.intValue());
  }

  /**
   * 
   * @param params
   * @return null (canceled) or a message starting with OK or an error message
   */
  public Object createImage(Map<String, Object> params) {    
    // this method may not be accessed, though public, unless 
    // accessed via viewer, which provides its private key.
    String type = (String) params.get("type");
    String fileName = (String) params.get("fileName");
    String text = (String) params.get("text");
    byte[] bytes = (byte[]) params.get("bytes");
    Object objImage = params.get("image");
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    OutputStream os = (OutputStream) params.get("outputStream");

    boolean closeStream = (os == null);
    long len = -1;
    try {
      if (!viewer.checkPrivateKey(privateKey))
        return "NO SECURITY";
      // returns message starting with OK or an error message
      if ("OutputStream".equals(type))
        return viewer.openOutputChannel(privateKey, fileName, false);
      if (bytes != null) {
        len = bytes.length;
        os = (OutputStream) viewer.openOutputChannel(privateKey, fileName, false);
        os.write(bytes, 0, bytes.length);
        os.flush();
        os.close();
        os = null;
      } else if (objImage != null) {
        getImageBytes(params);
        return fileName;
      } else if (text != null) {
        BufferedWriter bw = (BufferedWriter) viewer.openOutputChannel(
            privateKey, fileName, true);
        len = text.length();
        bw.write(text);
        bw.close();
      } else {
        len = 1;
        Object bytesOrError = getImageBytes(params);
        if (bytesOrError instanceof String)
          return bytesOrError;
        bytes = (byte[]) bytesOrError;
        if (bytes != null)
          return (fileName == null ? bytes : new String(bytes));
        len = viewer.getFileLength(privateKey, fileName);
      }
    } catch (IOException exc) {
      Logger.errorEx("IO Exception", exc);
      return exc.toString();
    } finally {
      if (os != null && closeStream) {
        try {
          os.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return (len < 0 ? "Creation of " + fileName + " failed: "
        + viewer.getErrorMessageUn() : "OK " + type + " "
        + (len > 0 ? len + " " : "") + fileName
        + (quality == Integer.MIN_VALUE ? "" : "; quality=" + quality));
  }

  public Object getImageBytes(Map<String, Object> params)
      throws IOException {
    byte[] bytes = null;
    String errMsg = null;
    String type = ((String) params.get("type")).toUpperCase();
    String fileName = (String) params.get("fileName");
    //String text = (String) params.get("text");
    String[] scripts = (String[]) params.get("scripts");
    Object objImage = params.get("image");
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    OutputStream os = (OutputStream) params.get("outputStream");

    boolean isPDF = type.equals("PDF");
    boolean isOsTemp = (os == null && fileName != null && !isPDF);
    boolean asBytes = (os == null && fileName == null && !isPDF);
    boolean isImage = (objImage != null);
    Object image = (isImage ? objImage : viewer.getScreenImageBuffer(null, true));
    try {
      if (image == null) {
        errMsg = viewer.getErrorMessage();
      } else {
        Object ret = null;
        boolean includeState = (!asBytes || type.equals("PNGJ"));
        if (type.equals("PNGJ"))
          ret = viewer.getWrappedState(fileName, scripts, true, true,
              viewer.apiPlatform.getImageWidth(image), viewer.apiPlatform
                  .getImageHeight(image));
        if (isOsTemp)
          os = (OutputStream) viewer.openOutputChannel(privateKey, fileName, false);
        if (type.equals("JPEG") || type.equals("JPG")) {
          if (quality <= 0)
            quality = 100; // high quality
          if (asBytes) {
            bytes = JpegEncoder.getBytes(viewer.apiPlatform, image, quality,
                Viewer.getJmolVersion());
          } else {
            String caption = (includeState ? (String) viewer.getWrappedState(
                null, null, true, false, viewer.apiPlatform
                    .getImageWidth(image), viewer.apiPlatform
                    .getImageHeight(image)) : Viewer.getJmolVersion());
            JpegEncoder.write(viewer.apiPlatform, image, quality, os, caption);
          }
        } else if (type.equals("JPG64") || type.equals("JPEG64")) {
          if (quality <= 0)
            quality = 75;
          bytes = JpegEncoder.getBytes(viewer.apiPlatform, image, quality,
              Viewer.getJmolVersion());
          if (asBytes) {
            bytes = Base64.getBytes64(bytes);
          } else {
            Base64.write(bytes, os);
            bytes = null;
          }
        } else if (type.startsWith("PNG")) {
          if (quality < 0)
            quality = 2;
          else if (quality > 9)
            quality = 9;
          int bgcolor = (type.equals("PNGT") ? viewer.getBackgroundArgb() : 0);
          int[] ptJmol = new int[1];
          bytes = GenericPngEncoder.getBytesType(viewer.apiPlatform, image, quality,
              bgcolor, type, ptJmol);
          byte[] b = null;
          if (includeState) {
            int nPNG = bytes.length;
            b = bytes;
            if (ret == null)
              ret = viewer.getWrappedState(null, scripts, true, false,
                  viewer.apiPlatform.getImageWidth(image), viewer.apiPlatform
                      .getImageHeight(image));
            bytes = (Escape.isAB(ret) ? (byte[]) ret : ((String) ret)
                .getBytes());
            int nState = bytes.length;
            GenericPngEncoder.setJmolTypeText(ptJmol[0], b, nPNG, nState, type);
          }
          if (!asBytes) {
            if (b != null)
              os.write(b, 0, b.length);
            os.write(bytes, 0, bytes.length);
            b = bytes = null;
          } else if (b != null) {
            byte[] bt = new byte[b.length + bytes.length];
            System.arraycopy(b, 0, bt, 0, b.length);
            System.arraycopy(bytes, 0, bt, b.length, bytes.length);
            bytes = bt;
            b = bt = null;
          }
        } else {
          String[] errRet = new String[1];
          bytes = getOtherBytes(fileName, image, type, asBytes, os, params, errRet);
          errMsg = errRet[0];
          if (bytes == null && errMsg == null && params.containsKey("captureByteCount"))
            errMsg = "OK: " + params.get("captureByteCount").toString() + " bytes";
        }
        if (os != null)
          os.flush();
        if (isOsTemp)
          os.close();
      }
    } catch (IOException e) {
      if (!isImage)
        viewer.releaseScreenImage();
      throw new IOException("" + e);
    } catch (Error er) {
      if (!isImage)
        viewer.releaseScreenImage();
      throw new Error(er);
    }
    if (!isImage)
      viewer.releaseScreenImage();
    if (errMsg != null)
      return errMsg;
    return bytes;
  }

  /**
   * 
   * GIF PPM PDF -- not available in JavaScript
   * 
   * @param fileName  
   * @param objImage 
   * @param type 
   * @param asBytes 
   * @param os 
   * @param params TODO
   * @param errRet 
   * @return byte array if needed
   * @throws IOException 
   */
  byte[] getOtherBytes(String fileName, Object objImage, String type,
                       boolean asBytes, OutputStream os, Map<String, Object> params, String[] errRet) throws IOException {
    errRet[0] = "file type " + type + " not available on this platform";
    return null;
  }
  
  public String clipImage(JmolViewer viewer, String text) {
    // Java only
    return null;
  }

  public String getClipboardText() {
    // Java only
    return null;
  }
  
}
