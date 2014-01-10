package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.Atom;

public interface MMCifInterface {

  boolean checkAtom(Atom atom, String assemblyID, int index);

  void finalizeReader(int nAtoms) throws Exception;

  boolean initialize(CifReader cifReader);

  void processEntry() throws Exception;
  
  boolean processLoopBlock() throws Exception;

}
