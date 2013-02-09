package org.jmol.api;

import java.util.List;
import java.util.Map;

import org.jmol.modelset.ModelSet;
import org.jmol.script.ScriptVariable;
import org.jmol.util.BitSet;
import org.jmol.util.Point3f;
import org.jmol.viewer.Viewer;

public interface JmolPropertyManager {

  void setViewer(Viewer viewer);

  Object getProperty(String returnType, String infoType, Object paramInfo);

  String getDefaultPropertyParam(int propertyID);

  int getPropertyNumber(String name);

  boolean checkPropertyParameter(String name);

  Object extractProperty(Object property, ScriptVariable[] args, int pt);

  List<Map<String, Object>> getMoleculeInfo(ModelSet modelSet,
                                            Object atomExpression);

  Map<String, Object> getModelInfo(Object atomExpression);

  Map<String, Object> getLigandInfo(Object atomExpression);

  Object getSymmetryInfo(BitSet bsAtoms, String xyz, int op, Point3f pt,
                         Point3f pt2, String id, int type);

  String getModelFileInfo(BitSet visibleFramesBitSet);

  String getChimeInfo(int tok, BitSet selectionSet);

  Map<String, List<Map<String, Object>>> getAllChainInfo(BitSet atomBitSet);

  List<Map<String, Object>> getAllAtomInfo(BitSet atomBitSet);

  List<Map<String, Object>> getAllBondInfo(BitSet atomBitSet);

  void getAtomIdentityInfo(int atomIndex, Map<String, Object> info);

  String getModelExtract(BitSet atomBitSet, boolean doTransform, boolean isModelKit,
                         String type);

}
