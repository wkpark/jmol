package org.jmol.api;


import java.util.Map;

import org.jmol.java.BS;
import org.jmol.script.SV;

import javajs.util.Lst;
import javajs.util.P3;
import org.jmol.viewer.Viewer;

public interface JmolPropertyManager {

  void setViewer(Viewer vwr);

  Object getProperty(String returnType, String infoType, Object paramInfo);

  String getDefaultPropertyParam(int propertyID);

  int getPropertyNumber(String name);

  boolean checkPropertyParameter(String name);

  Object extractProperty(Object property, SV[] args, int pt, Lst<Object> v2);

  Map<String, Object> getModelInfo(Object atomExpression);

  Map<String, Object> getLigandInfo(Object atomExpression);

  Object getSymmetryInfo(BS bsAtoms, String xyz, int op, P3 pt,
                         P3 pt2, String id, int type);

  String getModelFileInfo(BS visibleFramesBitSet);

  String getChimeInfo(int tok, BS selectionSet);

  String getModelExtract(BS atomBitSet, boolean doTransform, boolean isModelKit,
                         String type);


}
