package org.jmol.viewer;

import java.io.Reader;
import java.io.StringReader;

/**
 * 
 * ArrayDataReader subclasses BufferedReader and overrides its
 * read, readLine, mark, and reset methods so that JmolAdapter 
 * works with String[] arrays without any further adaptation. 
 * 
 */
public class ArrayDataReader extends DataReader {
  public ArrayDataReader(Reader in) {
    super(in);
  }
  
  String[] data;
  int pt = 0;
  public ArrayDataReader(String[] data) {
    super(new StringReader(""));
    this.data = data;
  }

  public String readLine() {
    return (pt < data.length ? data[pt++] : null);
  }
  
  public int read(char[] buf) {
    int nRead = 0;
    String line = readLine();
    int linept = 0;
    int linelen = (line == null ? -1 : line.length());
    for (int i = 0; i < buf.length && linelen >= 0; i++) {
        if (linept >= linelen) {
          linept = 0;
          buf[i] = '\n';
          line = readLine();
          linelen = (line == null ? -1 : line.length());
        } else {
          buf[i] = line.charAt(linept++);
        }
        nRead++;
    }
    return nRead;
  }
  
  int ptMark;
  public void mark(long ptr) {
    //ignore ptr.
    ptMark = pt;
  }
  
  public void reset() {
    pt = ptMark;
  }
}
