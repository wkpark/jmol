package org.jmol.io;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.jmol.util.ArrayUtil;
import org.jmol.util.SB;


public class OutputStringBuilder {

  public String type;
  SB sb;
  BufferedWriter bw;
  long nBytes;
  private BufferedOutputStream bos;
  private byte[] buf;
  
  public OutputStringBuilder(BufferedOutputStream bos, boolean asBytes){
    if (bos != null) {
      if (asBytes) {
        this.bos = bos;
      } else {
        OutputStreamWriter osw = new OutputStreamWriter(bos);
        bw = new BufferedWriter(osw, 8192);
      }
    } else if (asBytes) {
      buf = new byte[8092];
    } else {
      sb = new SB();
    }
  }
  
  public OutputStringBuilder append(String s) {
    try {
      if (bw != null) {
        bw.write(s);
      } else if (bos != null) {
        byte[] buf = s.getBytes();
        bos.write(buf, 0, buf.length);
        return this;
      } else {
        sb.append(s);
      }
      nBytes += s.length();
    } catch (IOException e) {
      // TODO
    }
    return this;
  }
  
  public void write(byte[] buf, int offset, int len) throws IOException {
    if (bos == null) {
      if (this.buf.length < nBytes + len)
        this.buf = ArrayUtil.ensureLengthByte(this.buf, (int)nBytes * 2 + len);
      System.arraycopy(buf, offset, this.buf, (int) nBytes, len);
    } else {
      bos.write(buf, offset, len);
    }
    nBytes += buf.length;
  }

  public long length() {
    return nBytes;
  }

  public byte[] getBytes() {
    return (buf != null ? buf : sb != null ? sb.toBytes(0, -1) : null);
  }
  
  @Override
  public String toString() {
    if (bw != null)
      try {
        bw.flush();
      } catch (IOException e) {
        // TODO
      }
    return (bw == null ? sb.toString() : nBytes + " bytes");
  }

}
