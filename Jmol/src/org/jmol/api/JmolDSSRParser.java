package org.jmol.api;

import java.util.Map;

import org.jmol.java.BS;
import org.jmol.modelset.Bond;
import org.jmol.viewer.Viewer;

import javajs.api.GenericLineReader;
import javajs.util.Lst;

public interface JmolDSSRParser {

  String process(Map<String, Object> info, GenericLineReader reader, String line0)
      throws Exception;

  BS getAtomBits(Viewer vwr, String key, Object dssr, Map<String, BS> dssrCache);

  void setAllDSSRParametersForModel(Viewer vwr, int modelIndex);

  String getHBonds(Viewer vwr, int modelIndex, Lst<Bond> vHBonds, boolean doReport);

  String calculateStructure(Viewer vwr, BS bsAtoms);

}
