package org.jmol.adapter.readers.molxyz;

import org.jmol.adapter.smarter.AtomSetCollectionReader;

public interface IntV3000 {

  void readAtomsAndBonds(String[] tokens) throws Exception;

  IntV3000 set(AtomSetCollectionReader molReader);

}
