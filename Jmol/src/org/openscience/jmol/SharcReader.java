
/*
 * Copyright 2002 The Jmol Development Team
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;


/**
 *  Reader of the SHARC chemical mime type for ab initio NMR. For definition of
 *  the format, see
 *  <a href="http://www.ccc.uni-erlangen.de/sharc/MIME.html">http://www.ccc.uni-erlangen.de/sharc/MIME.html</a>
 *
 */
public class SharcReader {

  BufferedReader input;
  String entryLine;
  boolean hasMore;

  public SharcReader(BufferedReader input) throws IOException {
    this.input = input;
    findNextEntry();
  }

  public boolean hasNext() {
    return hasMore;
  }

  public SharcShielding next() throws IOException {

    StringTokenizer tokenizer = new StringTokenizer(entryLine);
    tokenizer.nextToken();
    String nmrPart = tokenizer.nextToken();
    tokenizer.nextToken();
    tokenizer.nextToken();
    String optimizationPart = tokenizer.nextToken();

    SharcShielding result = new SharcShielding(nmrPart + optimizationPart);

    String line = input.readLine();
    while ((line != null) && !line.startsWith("NUCS")) {
      line = input.readLine();
    }
    int numberOfNuclei = Integer.parseInt(line.substring(4).trim());

    for (int i = 0; i < numberOfNuclei; ++i) {
      line = input.readLine();
      if (line == null) {
        throw new IOException("Unexpected end of file");
      }
      StringTokenizer tokenizer2 = new StringTokenizer(line);
      String element = tokenizer2.nextToken().substring(2);
      for (int j = 0; j < 4; ++j) {
        tokenizer2.nextToken();
      }

      double value = new Double(tokenizer2.nextToken()).doubleValue();

      result.setShielding(element, value);
    }

    findNextEntry();
    return result;
  }

  /**
   *  Finds the SHARC entry. The entryLine is set to the header of
   *  the next entry.
   */
  void findNextEntry() throws IOException {

    entryLine = input.readLine();
    while ((entryLine != null) && !entryLine.startsWith("SHARC ")) {
      entryLine = input.readLine();
    }
    if (entryLine == null) {
      hasMore = false;
    } else {
      hasMore = true;
    }
  }
}
