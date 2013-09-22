package org.jmol.awt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jmol.api.JmolOutputChannel;

/**
 * 
 * A wrapper for FileOutputStream, allowing debugging.
 * 
 *  
 */

public class LocalOutputChannel extends FileOutputStream implements JmolOutputChannel {
 
  private String fileName;
  
  public String getFileName() {
    return fileName;
  }
  
  public LocalOutputChannel(String fileName) throws FileNotFoundException {
    super(fileName);    
    this.fileName = fileName;
  }

  public void cancel() {
    close();
  }
  
  @Override
  public void close() {
    try {
      super.close();
    } catch (IOException e) {
      // TODO
    }
  }
}
