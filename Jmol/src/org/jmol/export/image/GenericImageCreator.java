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

import java.io.IOException;
import java.util.Map;

import org.jmol.api.JmolImageCreatorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.io.Base64;
import org.jmol.io.JmolOutputChannel;
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
    JmolOutputChannel out = (JmolOutputChannel) params.get("outputChannel");

    boolean closeStream = (out == null);
    long len = -1;
    try {
      if (!viewer.checkPrivateKey(privateKey))
        return "NO SECURITY";
      // returns message starting with OK or an error message
      if ("OutputChannel".equals(type))
        return viewer.openOutputChannel(privateKey, fileName, false);
      if (bytes != null) {
        len = bytes.length;
        if (out == null)
          out = viewer.openOutputChannel(privateKey, fileName, false);
        out.writeBytes(bytes, 0, bytes.length);
      } else if (objImage != null) {
        getImageBytes(params);
        return fileName;
      } else if (text != null) {
        if (out == null)
          out = viewer.openOutputChannel(privateKey, fileName, true);
        len = text.length();
        out.append(text);
      } else {
        len = 1;
        Object bytesOrError = getImageBytes(params);
        if (bytesOrError instanceof String)
          return bytesOrError;
        bytes = (byte[]) bytesOrError;
        if (bytes != null)
          return (fileName == null ? bytes : new String(bytes));
        len = ((Integer) params.get("byteCount")).intValue();
      }
    } catch (IOException exc) {
      Logger.errorEx("IO Exception", exc);
      return exc.toString();
    } finally {
      if (out != null && closeStream)
        out.closeChannel();
    }
    return (len < 0 ? "Creation of " + fileName + " failed: "
        + viewer.getErrorMessageUn() : "OK " + type + " "
        + (len > 0 ? len + " " : "") + fileName
        + (quality == Integer.MIN_VALUE ? "" : "; quality=" + quality));
  }

  /**
   * 
   * this method needs to set "byteCount" to be the length of bytes if not "asBytes"
   * @param params 
   * @return        bytes[] or (String) error or null
   * @throws IOException 
   * 
   */
  public Object getImageBytes(Map<String, Object> params) throws IOException {
    byte[] bytes = null;
    String errMsg = null;
    String type = ((String) params.get("type")).toUpperCase();
    String fileName = (String) params.get("fileName");
    //String text = (String) params.get("text");
    String[] scripts = (String[]) params.get("scripts");
    Object objImage = params.get("image");
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    JmolOutputChannel channel = (JmolOutputChannel) params.get("outputChannel");
    int len = -1;

    boolean closeChannel = (channel == null && fileName != null);
    boolean asBytes = (channel == null && fileName == null);
    boolean isImage = (objImage != null);
    Object image = (isImage ? objImage : viewer
        .getScreenImageBuffer(null, true));
    try {
      if (image == null) {
        errMsg = viewer.getErrorMessage();
      } else {
        Object ret = null;
        boolean includeState = (!asBytes || type.equals("PNGJ"));
        if (channel == null)
          channel = viewer.openOutputChannel(privateKey, fileName, false);
        if (type.equals("JPEG") || type.equals("JPG")) {
          if (quality <= 0)
            quality = 100; // high quality
          String caption = (asBytes ? Viewer.getJmolVersion()
              : includeState ? (String) viewer.getWrappedState(null, null,
                  true, false, viewer.apiPlatform.getImageWidth(image),
                  viewer.apiPlatform.getImageHeight(image)) : Viewer
                  .getJmolVersion());
          JpegEncoder.write(viewer.apiPlatform, image, quality, channel,
              caption);
          len = channel.getByteCount();
        } else if (type.equals("JPG64") || type.equals("JPEG64")) {
          if (quality <= 0)
            quality = 75;
          bytes = JpegEncoder.getBytes(viewer.apiPlatform, image, quality,
              Viewer.getJmolVersion());
          if (asBytes) {
            bytes = Base64.getBytes64(bytes);
            len = bytes.length;
          } else {
            Base64.write(bytes, channel);
            len = bytes.length;
            bytes = null;
          }
        } else if (type.startsWith("PNG")) {
          if (type.equals("PNGJ")) // get zip file data
            ret = viewer.getWrappedState(fileName, scripts, true, true,
                viewer.apiPlatform.getImageWidth(image), viewer.apiPlatform
                    .getImageHeight(image));
          if (quality < 0)
            quality = 2;
          else if (quality > 9)
            quality = 9;
          int bgcolor = (type.equals("PNGT") ? viewer.getBackgroundArgb() : 0);
          int[] ptJmol = new int[1];
          bytes = GenericPngEncoder.getBytesType(viewer.apiPlatform, image,
              quality, bgcolor, type, ptJmol);
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
          len = (b == null ? 0 : b.length) + bytes.length;
          if (!asBytes) {
            if (b != null)
              channel.writeBytes(b, 0, b.length);
            channel.writeBytes(bytes, 0, bytes.length);
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
          boolean isOK = getOtherBytes(image, type, channel, params, errRet);
          if (isOK && asBytes)
            bytes = channel.toByteArray();
          len = channel.getByteCount();
          errMsg = errRet[0];
          if (isOK && params.containsKey("captureByteCount"))
            errMsg = "OK: " + params.get("captureByteCount").toString()
                + " bytes";
        }
        if (closeChannel)
          channel.closeChannel();
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
    params.put("byteCount", Integer.valueOf(errMsg == null ? len : -1));
    if (errMsg != null)
      return errMsg;
    return bytes;
  }

  /**
   * 
   * GIF PPM PDF -- not available in JavaScript
   * 
   * @param objImage 
   * @param type 
   * @param out 
   * @param params TODO
   * @param errRet 
   * @return byte array if needed
   * @throws IOException 
   */
  protected boolean getOtherBytes(Object objImage, String type, JmolOutputChannel out, Map<String, Object> params, String[] errRet) throws IOException {
    errRet[0] = "file type " + type + " not available on this platform";
    return false;
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
