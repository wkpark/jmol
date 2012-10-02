package org.jmol.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.util.StringXBuilder;

public class OutputStringBuilder {

  public String type;
  StringXBuilder sb;
  BufferedWriter bw;
  long nBytes;
  
  public OutputStringBuilder(BufferedOutputStream os) {
    if (os == null) {
      sb = new StringXBuilder();
    } else {     
      OutputStreamWriter osw = new OutputStreamWriter(os);
      bw = new BufferedWriter(osw, 8192);
    }
  }
  
  public OutputStringBuilder append(String s) {
    if (bw == null) {
      sb.append(s);
    } else {
      nBytes += s.length();
      try {
        bw.write(s);
      } catch (IOException e) {
        // TODO
      }      
    }
    return this;
  }

  public long length() {
    return (bw == null ? sb.length() : nBytes);
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