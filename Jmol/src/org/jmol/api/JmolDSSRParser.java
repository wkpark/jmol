package org.jmol.api;

import java.util.Map;

import javajs.api.GenericLineReader;

public interface JmolDSSRParser {

  String process(Map<String, Object> info, GenericLineReader reader)
      throws Exception;

}
