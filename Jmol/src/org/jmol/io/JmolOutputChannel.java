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
 * A generic output method. JmolOutputChannel can be used to output to a StringBuffer,
 * a ByteArrayOutputStream (both just using new JmolOutputChannel()), to a file
 * (os = FileOutputStream()) 
 * a Extension of OutputStream is for the sole purpose
 * of allowing the zip utility to use new ZipOutputStream(jmolOutputChannel);
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
    isLocalFile = (fileName != null && (fileName.startsWith("http://") || fileName.startsWith("https://")));
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

  public void setType(String type) {
    this.type = type;
  }
  
  public String getType() {
    return type;
  }
  
  public JmolOutputChannel append(String s) {
    try {
      if (bw != null) {
        bw.write(s);
      } else if (os != null) {
        byte[] b = s.getBytes();
        os.write(b, 0, b.length);
      } else {
        if (sb == null)
          sb = new SB();
        sb.append(s);
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
    try {
      if (bw != null) {
        bw.flush();
        bw.close();
      } else if (os != null) {
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
     *            os.toByteArray() : this.sb.toString()));
     * 
     */
    {
      if (!isLocalFile)
        return JmolBinary.postByteArray(fm, fileName, toByteArray());
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
