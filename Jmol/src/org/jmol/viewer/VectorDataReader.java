package org.jmol.viewer;

import java.io.StringReader;
import java.util.Vector;

/**
 * 
 * VectorDataReader subclasses BufferedReader and overrides its
 * read, readLine, mark, and reset methods so that JmolAdapter 
 * works with Vector<String> arrays without any further adaptation. 
 * 
 */
public class VectorDataReader extends DataReader {

  private Vector data;
  private int pt;
  private int len;
  
  public VectorDataReader(Vector data) {
    super(new StringReader(""));
    this.data = data;
    len = data.size();
  }

  public String readLine() {
    return (pt < len ? (String) data.get(pt++) : null);
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
