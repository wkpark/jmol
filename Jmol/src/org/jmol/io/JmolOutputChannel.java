package org.jmol.io;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jmol.util.SB;
import org.jmol.viewer.FileManager;

/**
 * 
 * A generic output method. JmolOutputChannel can be used to:
 * 
 * add characters to a StringBuffer 
 *   using append() and toString()
 *   
 * add bytes utilizing ByteArrayOutputStream 
 *   using writeBytes(), writeByteAsInt(), append()*, and bytesAsArray()
 *       *append() can be used as long as os==ByteArrayOutputStream
 *        or it is not used before one of the writeByte methods. 
 * 
 * output characters to a FileOutputStream 
 *  using os==FileOutputStream, asWriter==true, append(), and closeChannel()
 *  
 * output bytes to a FileOutputStream 
 *  using os==FileOutputStream, writeBytes(), writeByteAsInt(), append(), and closeChannel()
 * 
 * post characters or bytes to a remote server
 *  using filename=="http://..." or "https://...",
 *    writeBytes(), writeByteAsInt(), append(), and closeChannel()
 *  
 *  
 */

public class JmolOutputChannel extends OutputStream {
 
  private FileManager fm; // only necessary for writing to http:// or https://
  private String fileName;
  private BufferedWriter bw;
  private boolean isLocalFile;
  private int byteCount;
  private boolean isCanceled;
  private OutputStream os;
  private SB sb;
  private String type;
  
  public JmolOutputChannel setParams(FileManager fm, String fileName, boolean asWriter, OutputStream os) {
    this.fm = fm;
    this.fileName = fileName;
    this.os = os;
    isLocalFile = (fileName != null && !(fileName.startsWith("http://") || fileName.startsWith("https://")));
    if (asWriter)
      bw = new BufferedWriter(new OutputStreamWriter(os));
    return this;
  }

  public String getFileName() {
    return fileName;
  }
  
  public int getByteCount() {
    return byteCount;
  }

  /**
   * 
   * @param type  user-identified type (PNG, JPG, etc)
   */
  public void setType(String type) {
    this.type = type;
  }
  
  public String getType() {
    return type;
  }

  /**
   * will go to string buffer if bw == null and os == null
   * 
   * @param s
   * @return this, for chaining like a standard StringBuffer
   * 
   */
  public JmolOutputChannel append(String s) {
    try {
      if (bw != null) {
        bw.write(s);
      } else if (os == null) {
        if (sb == null)
          sb = new SB();
        sb.append(s);
      } else {
        byte[] b = s.getBytes();
        os.write(b, 0, b.length);
        byteCount += b.length;
        return this;
      }
    } catch (IOException e) {
      // ignore
    }
    byteCount += s.length(); // not necessarily exactly correct if unicode
    return this;
  }

  public void writeBytes(byte[] buf, int i, int len) {
    if (os == null)
      os = new ByteArrayOutputStream();
    /**
     * @j2sNative
     * 
     *            this.os.writeBytes(buf, i, len);
     * 
     */
    {
      try {
        os.write(buf, i, len);
      } catch (IOException e) {
      }
    }
    byteCount += len;
  }

  /**
   * @param b  
   */
  public void writeByteAsInt(int b) {
    if (os == null)
      os = new ByteArrayOutputStream();
    /**
     * @j2sNative
     * 
     *  this.os.writeByteAsInt(b);
     * 
     */
    {
      try {
        os.write(b);
      } catch (IOException e) {
      }
    }
    byteCount++;
  }
  
  /**
   * @j2sIgnore
   * 
   * @param b
   */
  @Override
  @Deprecated
  public void write(int b) {
    // required by ZipOutputStream -- do not use, as it will break JavaScript methods
    if (os == null)
      os = new ByteArrayOutputStream();
    try {
      os.write(b);
    } catch (IOException e) {
    }
    byteCount++;
  }

  public void cancel() {
    isCanceled = true;
    closeChannel();
  }

  public String closeChannel() {
    // can't cancel file writers
    try {
      if (bw != null) {
        bw.flush();
        bw.close();
      } else if (os != null) {
        os.flush();
        os.close();
      }
    } catch (Exception e) {
      // ignore closing issues
    }
    if (isCanceled)
      return null;
    if (fileName == null)
      return (sb == null ? null : sb.toString());

    /**
     * @j2sNative
     * 
     *            Jmol._doAjax(this.fileName, null, (this.sb == null ?
     *            this.toByteArray() : this.sb.toString()));
     * 
     */
    {
      if (!isLocalFile) // unsigned applet could do this
        return JmolBinary.postByteArray(fm, fileName, 
            (sb == null ? toByteArray() : sb.toString().getBytes()));
    }
    return null;
  }

  public byte[] toByteArray() {
    return (os instanceof ByteArrayOutputStream ? ((ByteArrayOutputStream)os).toByteArray() : null);
  }


  @Override
  public String toString() {
    if (bw != null)
      try {
        bw.flush();
      } catch (IOException e) {
        // TODO
      }
    if (sb != null)
      return closeChannel();
    return byteCount + " bytes";
  }


}
