package org.jmol.api;

import java.util.Map;

import org.jmol.java.BS;
import org.jmol.viewer.Viewer;

import javajs.api.GenericLineReader;

public interface JmolDSSRParser {

  String process(Map<String, Object> info, GenericLineReader reader)
      throws Exception;

  BS getAtomBits(String key, Object dssr, Map<String, BS> dssrCache, Viewer vwr);

}
