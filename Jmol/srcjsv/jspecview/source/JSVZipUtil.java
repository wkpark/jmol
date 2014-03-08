/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package jspecview.source;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javajs.util.AU;
import javajs.util.List;
import javajs.util.SB;
import jspecview.api.JSVZipInterface;
import jspecview.api.JSVZipReader;
import jspecview.common.JSViewer;

import org.jmol.util.Logger;

public class JSVZipUtil implements JSVZipInterface {

	public JSVZipUtil() {
		// for reflection
	}
	@Override
	public InputStream newGZIPInputStream(InputStream bis)
			throws IOException {
		return new GZIPInputStream(bis, 512);
	}

	@Override
	public BufferedReader newJSVZipFileSequentialReader(InputStream in,
			String[] subFileList, String startCode) {
		return ((JSVZipReader) JSViewer.getInterface("jspecview.source.JSVZipFileSequentialReader")).set(in, subFileList, startCode);
	}

  public static boolean isZipFile(byte[] bytes) throws Exception {
    return (bytes.length > 4 
        && bytes[0] == 0x50  //PK<03><04> 
        && bytes[1] == 0x4B
        && bytes[2] == 0x03 
        && bytes[3] == 0x04);
  }

  /**
   *  iteratively drills into zip files of zip files to extract file content
   *  or zip file directory. Also works with JAR files.
   * 
   * @param is
   * @param list
   * @param listPtr
   * @return  directory listing or subfile contents
   */
  @SuppressWarnings("resource")
	static public Object getZipFileContents(InputStream is, String[] list,
                                          int listPtr) {
    SB ret = new SB();
    if (list == null || listPtr >= list.length)
      return getZipDirectoryAsStringAndClose(is);
    String fileName = list[listPtr];
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    try {
      boolean isAll = (fileName.equals("."));
      if (isAll || fileName.lastIndexOf("/") == fileName.length() - 1) {
        while ((ze = zis.getNextEntry()) != null) {
          String name = ze.getName();
          if (isAll || name.startsWith(fileName))
            ret.append(name).appendC('\n');
        }
        return ret.toString();
      }
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = getZipEntryAsBytes(zis);
        if (isZipFile(bytes))
          return getZipFileContents(new BufferedInputStream(
              new ByteArrayInputStream(bytes)), list, ++listPtr);
        return new String(bytes);
      }
    } catch (Exception e) {
    }
    return "";
  }
  
  @SuppressWarnings("resource")
	static public byte[] getZipFileContentsAsBytes(InputStream is, String[] list,
                                          int listPtr) {
    byte[] ret = new byte[0];
    String fileName = list[listPtr];
    if (fileName.lastIndexOf("/") == fileName.length() - 1)
      return ret;
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    try {
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = getZipEntryAsBytes(zis);
        if (isZipFile(bytes) && ++listPtr < list.length)
          return getZipFileContentsAsBytes(new BufferedInputStream(
              new ByteArrayInputStream(bytes)), list, listPtr);
        return bytes;
      }
    } catch (Exception e) {
    }
    return ret;
  }
  
  static public String getZipDirectoryAsStringAndClose(InputStream is) {
    SB sb = new SB();
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(is, false);
      is.close();
    } catch (Exception e) { 
      Logger.error(e.toString());
    }
    for (int i = 0; i < s.length; i++)
      sb.append(s[i]).appendC('\n');
    return sb.toString();
  }
  
  static public String[] getZipDirectoryAndClose(InputStream is, boolean addManifest) {
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(is, addManifest);
      is.close();
    } catch (Exception e) { 
      Logger.error(e.toString());
    }
    return s;
  }
  
  private static String[] getZipDirectoryOrErrorAndClose(InputStream is, boolean addManifest) throws IOException {
    List<String> v = new List<String>();
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    String manifest = null;
    while ((ze = zis.getNextEntry()) != null) {
      String fileName = ze.getName();
      if (addManifest && fileName.equals("JmolManifest"))
        manifest = getZipEntryAsString(zis); 
      else
        v.addLast(fileName);
    }
    zis.close();
    if (addManifest)
      v.add(0, manifest == null ? "" : manifest + "\n############\n");
    int len = v.size();
    String[] dirList = new String[len];
    for (int i = 0; i < len; i++)
      dirList[i] = v.get(i);
    return dirList;
  }
  
  public static String getZipEntryAsString(ZipInputStream zis) throws IOException {
    SB sb = new SB();
    byte[] buf = new byte[1024];
    int len;
    while (zis.available() == 1 && (len = zis.read(buf, 0, 1024)) > 0)
      sb.append(new String(buf, 0, len));
    return sb.toString();
  }
  
  public static byte[] getZipEntryAsBytes(ZipInputStream zis) throws IOException {
    
    //What is the efficient way to read an input stream into a byte array?
    
    byte[] buf = new byte[1024];
    byte[] bytes = new byte[4096];
    int len = 0;
    int totalLen = 0;
    while (zis.available() == 1 && (len = zis.read(buf, 0, 1024)) > 0) {
      totalLen += len;
      if (totalLen >= bytes.length)
        bytes = AU.ensureLengthByte(bytes, totalLen * 2);
      System.arraycopy(buf, 0, bytes, totalLen - len, len);
    }
    buf = new byte[totalLen];
    System.arraycopy(bytes, 0, buf, 0, totalLen);
    return buf;
  }

  public static byte[] getStreamAsBytes(BufferedInputStream bis) throws IOException {
    byte[] buf = new byte[1024];
    byte[] bytes = new byte[4096];
    int len = 0;
    int totalLen = 0;
    while ((len = bis.read(buf, 0, 1024)) > 0) {
      totalLen += len;
      if (totalLen >= bytes.length)
        bytes = AU.ensureLengthByte(bytes, totalLen * 2);
      System.arraycopy(buf, 0, bytes, totalLen - len, len);
    }
    buf = new byte[totalLen];
    System.arraycopy(bytes, 0, buf, 0, totalLen);
    return buf;
  }
}
