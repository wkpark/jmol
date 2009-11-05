package org.jmol.viewer;

import java.io.BufferedReader;
import java.io.Reader;

/**
 * Just a simple abstract class to join a String reader and a String[]
 * reader under the same BufferedReader umbrella.
 * 
 * Subclassed as StringDataReader and ArrayDataReader
 * 
 */

abstract public class DataReader extends BufferedReader {

  public DataReader(Reader in) {
    super(in);
  }

  public BufferedReader getBufferedReader() {
    return this;
  }  

}
