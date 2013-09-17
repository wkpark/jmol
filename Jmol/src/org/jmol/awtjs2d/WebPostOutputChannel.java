package org.jmol.awtjs2d;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.jmol.io.JmolBinary;
import org.jmol.viewer.FileManager;

public class WebPostOutputChannel extends ByteArrayOutputStream {
 
  private String fileName;
  private FileManager fm;
  private BufferedWriter bw;
  
  public WebPostOutputChannel(FileManager fm, String fileName, boolean asWriter) {
    this.fm = fm;
    this.fileName = fileName;
    if (asWriter)
      bw = new BufferedWriter(new OutputStreamWriter(this));
  }

  // methods of BufferedWriter. These will be accessed by JavaScript, so no type checking
  
  public void write(String data) throws IOException {
    bw.write(data);
  }

  @Override
  public void flush() {
    if (bw != null)
      try {
        bw.flush();
      } catch (IOException e) {
      }
    else
      flush();
  }
  
  @Override
  public void close() {
    JmolBinary.postByteArray(fm, fileName, toByteArray()); 
  }
}
