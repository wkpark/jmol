package org.jmol.adapter.readers.cif;

import javajs.util.CifDataParser;

/**
 * see http://journals.iucr.org/j/issues/2016/01/00/aj5269/index.html
 * 
 */
public class Cif2DataParser extends CifDataParser {

  
  /**
   * 
   * @return the next token of any kind, or null
   * @throws Exception
   */
  @Override
  public String getNextToken() throws Exception {
    while (!strHasMoreTokens())
      if (setStringNextLine() == null)
        return null;
    return nextStrToken();
  }


}
