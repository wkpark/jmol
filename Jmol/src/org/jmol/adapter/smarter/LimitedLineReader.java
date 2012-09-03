package org.jmol.adapter.smarter;

import java.io.BufferedReader;

class LimitedLineReader {
  private char[] buf;
  private int cchBuf;
  private int ichCurrent;

  LimitedLineReader(BufferedReader bufferedReader, int readLimit)
    throws Exception {  
    // It appears that some web servers cannot handle this. 
    // All I know is that the Indiana University smi23d server
    // returns only one char the SECOND time this is run. 
    // for some reason the URL connection is not closing?
    // but the problem only occurs when this limited buffer is used;
    // when you use MOL:: in front of the filename, then it is fine.
    // So we do that...
    bufferedReader.mark(readLimit);
    buf = new char[readLimit];
    cchBuf = Math.max(bufferedReader.read(buf), 0);
    ichCurrent = 0;
    bufferedReader.reset();
  }

  String getHeader(int n) {
    return (n == 0 ? new String(buf) : new String(buf, 0, Math.min(cchBuf, n)));
  }
  
  String readLineWithNewline() {
    // mth 2004 10 17
    // for now, I am going to put in a hack here
    // we have some CIF files with many lines of '#' comments
    // I believe that for all formats we can flush if the first
    // char of the line is a #
    // if this becomes a problem then we will need to adjust
    while (ichCurrent < cchBuf) {
      int ichBeginningOfLine = ichCurrent;
      char ch = 0;
      while (ichCurrent < cchBuf &&
             (ch = buf[ichCurrent++]) != '\r' && ch != '\n') {
      }
      if (ch == '\r' && ichCurrent < cchBuf && buf[ichCurrent] == '\n')
        ++ichCurrent;
      int cchLine = ichCurrent - ichBeginningOfLine;
      if (buf[ichBeginningOfLine] == '#')
        continue; // flush comment lines;
      StringBuffer sb = new StringBuffer(cchLine);
      sb.append(buf, ichBeginningOfLine, cchLine);
      return sb.toString();
    }
    //Logger.debug("org.jmol.adapter.smarter.AtomSetCollectionReader;
    // miguel 2005 01 26
    // for now, just return the empty string.
    // it will only affect the Resolver code
    // it will be easier to handle because then everyone does not
    // need to check for the null pointer
    //
    // If it becomes a problem, then change this to null and modify
    // all the code above to make sure that it tests for null before
    // attempting to invoke methods on the strings. 
    return "";
  }
}
