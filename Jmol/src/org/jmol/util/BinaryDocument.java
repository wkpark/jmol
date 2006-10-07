/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: hansonr@stolaf.edu
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
package org.jmol.util;


import java.io.DataInputStream;
//import java.io.RandomAccessFile;

/* a basic binary file reader (extended by CompountDocument). 
 * 
 * random access file info: 
 * http://java.sun.com/docs/books/tutorial/essential/io/rafs.html
 * 
 * SHOOT! random access is only for applications, not applets!
 * 
 * Note that YOU are responsible for determining whether a file
 * is bigEndian or littleEndian; the default is bigEndian.
 * 
 */

class BinaryDocument {

  BinaryDocument() {  
  }
  
//  RandomAccessFile file;
  
  DataInputStream stream;
  boolean isRandom = false;
  boolean isBigEndian = true;

  byte readByte() throws Exception {
    return stream.readByte();
  }

  void readByteArray(byte[] b) throws Exception {
    stream.read(b);
  }

  void readByteArray(byte[] b, int off, int len) throws Exception {
    stream.read(b, off, len);
  }

  short readShort() throws Exception {
    if (isBigEndian)
      return stream.readShort();
    return (short) ((((int) stream.readByte()) & 0xff) | (((int) stream
        .readByte()) & 0xff) << 8);
  }

  int readInt() throws Exception {
    if (isBigEndian)
      return stream.readInt();
    return ((((int) stream.readByte()) & 0xff)
        | (((int) stream.readByte()) & 0xff) << 8
        | (((int) stream.readByte()) & 0xff) << 16 | (((int) stream.readByte()) & 0xff) << 24);
  }

  long readLong() throws Exception {
    if (isBigEndian)
      return stream.readLong();
    return ((((long) stream.readByte()) & 0xff)
        | (((long) stream.readByte()) & 0xff) << 8
        | (((long) stream.readByte()) & 0xff) << 16
        | (((long) stream.readByte()) & 0xff) << 24
        | (((long) stream.readByte()) & 0xff) << 32
        | (((long) stream.readByte()) & 0xff) << 40
        | (((long) stream.readByte()) & 0xff) << 48 | (((long) stream
        .readByte()) & 0xff) << 54);
  }

  void seek(long offset) {
    // slower, but all that is available using the applet
    try {
      stream.reset();
      stream.skipBytes((int)offset);
    } catch (Exception e) {
      Logger.error(null, e);
    }
  }

/*  random access -- application only:
 * 
    void seekFile(long offset) {
    try {
      file.seek(offset);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
*/
}
