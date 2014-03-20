/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-05 09:07:28 -0500 (Thu, 05 Apr 2007) $
 * $Revision: 7326 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.util.Map;

import org.jmol.api.Interface;

import javajs.api.ZInputStream;
import javajs.util.AU;
import javajs.util.Base64;
import javajs.util.Encoding;
import javajs.util.OC;
import javajs.util.SB;

public class Binary {

  static Encoding getUTFEncodingForStream(BufferedInputStream is) throws IOException {
    /**
     * @j2sNative
     * 
     *  is.resetStream();
     * 
     */
    {
    }
    byte[] abMagic = new byte[4];
    abMagic[3] = 1;
    try{
    is.mark(5);
    } catch (Exception e) {
      return Encoding.NONE;
    }
    is.read(abMagic, 0, 4);
    is.reset();
    return getUTFEncoding(abMagic);
  }

  public static String fixUTF(byte[] bytes) {
    
    Encoding encoding = getUTFEncoding(bytes);
    if (encoding != Encoding.NONE)
    try {
      String s = new String(bytes, encoding.name().replace('_', '-'));
      switch (encoding) {
      case UTF8:
      case UTF_16BE:
      case UTF_16LE:
        // extra byte at beginning removed
        s = s.substring(1);
        break;
      default:
        break;        
      }
      return s;
    } catch (UnsupportedEncodingException e) {
      System.out.println(e);
    }
    return new String(bytes);
  }

  static Encoding getUTFEncoding(byte[] bytes) {
    if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF)
      return Encoding.UTF8;
    if (bytes.length >= 4 && bytes[0] == (byte) 0 && bytes[1] == (byte) 0 
        && bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF)
      return Encoding.UTF_32BE;
    if (bytes.length >= 4 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE 
        && bytes[2] == (byte) 0 && bytes[3] == (byte) 0)
      return Encoding.UTF_32LE;
    if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
      return Encoding.UTF_16LE;
    if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)
      return Encoding.UTF_16BE;
    return Encoding.NONE;
  
  }

  public static boolean isCompoundDocumentS(InputStream is) {
    return isCompoundDocumentB(getMagic(is, 8));
  }

  public static boolean isCompoundDocumentB(byte[] bytes) {
    return (bytes.length >= 8 && bytes[0] == (byte) 0xD0
        && bytes[1] == (byte) 0xCF && bytes[2] == (byte) 0x11
        && bytes[3] == (byte) 0xE0 && bytes[4] == (byte) 0xA1
        && bytes[5] == (byte) 0xB1 && bytes[6] == (byte) 0x1A 
        && bytes[7] == (byte) 0xE1);
  }

  public static boolean isGzipS(InputStream is) {
    return isGzipB(getMagic(is, 2));
  }

  public static boolean isGzipB(byte[] bytes) {    
      return (bytes != null && bytes.length >= 2 
          && bytes[0] == (byte) 0x1F && bytes[1] == (byte) 0x8B);
  }

  public static boolean isZipS(InputStream is) {
    return isZipB(getMagic(is, 4));
  }

  public static boolean isZipB(byte[] bytes) {
    return (bytes.length >= 4 
        && bytes[0] == 0x50  //PK<03><04> 
        && bytes[1] == 0x4B
        && bytes[2] == 0x03 
        && bytes[3] == 0x04);
  }

  public static String getZipRoot(String fileName) {
    int pt = fileName.indexOf("|");
    return (pt < 0 ? fileName : fileName.substring(0, pt));
  }

  static JmolZipTools jzt;

  static JmolZipTools getJzt() {
    return (jzt == null ? jzt = (JmolZipTools) Interface.getOption("io2.ZipTools") : jzt);
  }

  public static void readFileAsMap(BufferedInputStream is,
                                   Map<String, Object> bdata) {
    getJzt().readFileAsMap(is, bdata);
  }

  public static String getZipDirectoryAsStringAndClose(BufferedInputStream t) {
    return getJzt().getZipDirectoryAsStringAndClose(t);
  }

  public static InputStream newGZIPInputStream(BufferedInputStream bis) throws IOException {
    return getJzt().newGZIPInputStream(bis);
  }

  public static ZInputStream newZipInputStream(InputStream in) {
    return getJzt().newZipInputStream(in);
  }

  public static Object getZipFileDirectory(BufferedInputStream bis,
                                          String[] subFileList, int listPtr, boolean asBufferedInputStream) {
    return getJzt().getZipFileDirectory(bis, subFileList, listPtr, asBufferedInputStream);
  }

  public static String[] getZipDirectoryAndClose(BufferedInputStream t,
                                                 boolean addManifest) {
    return getJzt().getZipDirectoryAndClose(t, addManifest);
  }

  public static void getAllZipData(BufferedInputStream bis, String[] subFileList,
                                String replace, String string,
                                Map<String, String> fileData) {
    getJzt().getAllZipData(bis, subFileList, replace, string, fileData);
  }

  public static Object getZipFileContentsAsBytes(BufferedInputStream bis,
                                                 String[] subFileList, int i) {
    return getJzt().getZipFileContentsAsBytes(bis, subFileList, i);
  }

  public static void addZipEntry(Object zos, String fileName) throws IOException {
    getJzt().addZipEntry(zos, fileName);    
  }

  public static void closeZipEntry(Object zos) throws IOException {
    getJzt().closeZipEntry(zos);
  }

  public static Object getZipOutputStream(Object bos) {
    return getJzt().getZipOutputStream(bos);
  }

  public static int getCrcValue(byte[] bytes) {
    return getJzt().getCrcValue(bytes);
  }

  public static Object getStreamAsBytes(BufferedInputStream bis,
                                         OC out) throws IOException {
    byte[] buf = new byte[1024];
    byte[] bytes = (out == null ? new byte[4096] : null);
    int len = 0;
    int totalLen = 0;
    while ((len = bis.read(buf, 0, 1024)) > 0) {
      totalLen += len;
      if (out == null) {
        if (totalLen >= bytes.length)
          bytes = AU.ensureLengthByte(bytes, totalLen * 2);
        System.arraycopy(buf, 0, bytes, totalLen - len, len);
      } else {
        out.write(buf, 0, len);
      }
    }
    bis.close();
    if (out == null) {
      return AU.arrayCopyByte(bytes, totalLen);
    }
    return totalLen + " bytes";
  }

  public static boolean isBase64(SB sb) {
    return (sb.indexOf(";base64,") == 0);
  }

  public static byte[] getBytesFromSB(SB sb) {
    return (isBase64(sb) ? Base64.decodeBase64(sb.substring(8)) : sb.toBytes(0, -1));    
  }

  public static BufferedInputStream getBIS(byte[] bytes) {
    return new BufferedInputStream(new ByteArrayInputStream(bytes));
  }

  public static BufferedReader getBR(String string) {
    return new BufferedReader(new StringReader(string));
  }

  /**
   * @param bis
   * @param charSet
   * @return Reader
   * @throws IOException
   */
  public static BufferedReader getBufferedReader(BufferedInputStream bis, String charSet)
      throws IOException {
    // could also just make sure we have a buffered input stream here.
    if (getUTFEncodingForStream(bis) == Encoding.NONE)
      return new BufferedReader(new InputStreamReader(bis, (charSet == null ? "UTF-8" : charSet)));
    byte[] bytes = getStreamBytes(bis, -1);
    bis.close();
    return getBR(charSet == null ? fixUTF(bytes) : new String(bytes, charSet));
  }

  public static BufferedInputStream getUnzippedInputStream(BufferedInputStream bis) throws IOException {
    while (isGzipS(bis))
      bis = new BufferedInputStream(newGZIPInputStream(bis));
    return bis;
  }

  public static String StreamToString(BufferedInputStream bis) {
    String[] data = new String[1];
    try {
      readAllAsString(getBufferedReader(bis, "UTF-8"), -1, true, data, 0);
    } catch (IOException e) {
    }
    return data[0];
  }

  public static boolean readAllAsString(BufferedReader br, int nBytesMax, boolean allowBinary, String[] data, int i) {
    try {
      SB sb = SB.newN(8192);
      String line;
      if (nBytesMax < 0) {
        line = br.readLine();
        if (allowBinary || line != null && line.indexOf('\0') < 0
            && (line.length() != 4 || line.charAt(0) != 65533
            || line.indexOf("PNG") != 1)) {
          sb.append(line).appendC('\n');
          while ((line = br.readLine()) != null)
            sb.append(line).appendC('\n');
        }
      } else {
        int n = 0;
        int len;
        while (n < nBytesMax && (line = br.readLine()) != null) {
          if (nBytesMax - n < (len = line.length()) + 1)
            line = line.substring(0, nBytesMax - n - 1);
          sb.append(line).appendC('\n');
          n += len + 1;
        }
      }
      br.close();
      data[i] = sb.toString();
      return true;
    } catch (Exception ioe) {
      data[i] = ioe.toString();
      return false;
    }
  }

  public static byte[] getStreamBytes(InputStream is, long n)
      throws IOException {

    //Note: You cannot use InputStream.available() to reliably read
    //      zip data from the web. 

    int buflen = (n > 0 && n < 1024 ? (int) n : 1024);
    byte[] buf = new byte[buflen];
    byte[] bytes = new byte[n < 0 ? 4096 : (int) n];
    int len = 0;
    int totalLen = 0;
    if (n < 0)
      n = Integer.MAX_VALUE;
    while (totalLen < n && (len = is.read(buf, 0, buflen)) > 0) {
      totalLen += len;
      if (totalLen > bytes.length)
        bytes = AU.ensureLengthByte(bytes, totalLen * 2);
      System.arraycopy(buf, 0, bytes, totalLen - len, len);
      if (n != Integer.MAX_VALUE && totalLen + buflen > bytes.length)
        buflen = bytes.length - totalLen;

    }
    if (totalLen == bytes.length)
      return bytes;
    buf = new byte[totalLen];
    System.arraycopy(bytes, 0, buf, 0, totalLen);
    return buf;
  }

  public static byte[] getMagic(InputStream is, int n) {
    byte[] abMagic = new byte[n];
    /**
     * @j2sNative
     * 
     * is.resetStream();
     * 
     */
    {
    }
    try {
      is.mark(n + 1);
      is.read(abMagic, 0, n);
    } catch (IOException e) {
    }
    try {
      is.reset();
    } catch (IOException e) {
    }
    return abMagic;
  }

  public static boolean isPngZipB(byte[] bytes) {
    // \0PNGJ starting at byte 50
    return (bytes[50] == 0 && bytes[51] == 0x50 && bytes[52] == 0x4E && bytes[53] == 0x47 && bytes[54] == 0x4A);
  }

  public static byte[] deActivatePngZipB(byte[] bytes) {
    // \0PNGJ starting at byte 50 changed to \0 NGJ
    if (isPngZipB(bytes))
      bytes[51] = 32;
    return bytes;
  }

  public static boolean isPngZipStream(InputStream is) {
    return isPngZipB(getMagic(is, 55));
  }

  public static boolean isJmolManifest(String thisEntry) {
    return thisEntry.startsWith("JmolManifest");
  }

  public static void getPngZipPointAndCount(BufferedInputStream bis, int[] pt_count) {
    bis.mark(75);
    try {
      byte[] data = getStreamBytes(bis, 74);
      bis.reset();
      int pt = 0;
      for (int i = 64, f = 1; --i > 54; f *= 10)
        pt += (data[i] - '0') * f;
      int n = 0;
      for (int i = 74, f = 1; --i > 64; f *= 10)
        n += (data[i] - '0') * f;
      pt_count[0] = pt;
      pt_count[1] = n;
    } catch (Throwable e) {
      pt_count[1] = 0;
    }
  }

  public static BufferedInputStream getPngImageStream(BufferedInputStream bis) {
    if (isPngZipStream(bis))
      try {
        int pt_count[] = new int[2];
        getPngZipPointAndCount(bis, pt_count);
        if (pt_count[1] != 0) {
          byte[] data = getStreamBytes(bis, pt_count[0]);
          bis.close();
          return getBIS(data);
        }
      } catch (IOException e) {
      }
    return bis;
  }

  /**
   * Look at byte 50 for "\0PNGJxxxxxxxxx+yyyyyyyyy" where xxxxxxxxx is a byte
   * offset to the JMOL data and yyyyyyyyy is the length of the data.
   * 
   * @param bis
   * @return same stream or byte stream
   */
  public static BufferedInputStream getPngZipStream(BufferedInputStream bis) {
    if (!isPngZipStream(bis))
      return bis;
    byte[] data = new byte[0];
    bis.mark(75);
    try {
      int pt_count[] = new int[2];
      getPngZipPointAndCount(bis, pt_count);
      if (pt_count[1] != 0) {
        int pt = pt_count[0];
        while (pt > 0)
          pt -= bis.skip(pt);
        data = getStreamBytes(bis, pt_count[1]);
      }
    } catch (Throwable e) {
    } finally {
      try {
        bis.close();
      } catch (Exception e) {
        // ignore
      }
    }
    return getBIS(data);
  }

}

