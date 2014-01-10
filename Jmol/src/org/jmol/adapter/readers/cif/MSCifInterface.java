package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.MSInterface;

public interface MSCifInterface extends MSInterface {

  // methods called from org.jmol.adapters.readers.cif.CifReader
 
  int processLoopBlock() throws Exception;
  void processEntry() throws Exception;
  
}
