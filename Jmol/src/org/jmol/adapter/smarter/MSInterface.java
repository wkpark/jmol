package org.jmol.adapter.smarter;

import java.util.Map;

import javajs.util.M4;
import javajs.util.P3;


/**
 * Modulated Structure Reader Interface
 * 
 */
public interface MSInterface {

  // methods called from org.jmol.adapters.readers.xtal.JanaReader
  
  void addModulation(Map<String, P3> map, String id, P3 pt, int iModel);

  void addSubsystem(String code, M4 m4, String atomName);

  void finalizeModulation();

  P3 getMod(String key);

  int initialize(AtomSetCollectionReader r, String data) throws Exception;

  void setModulation();

}
