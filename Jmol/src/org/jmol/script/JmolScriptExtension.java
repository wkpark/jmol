package org.jmol.script;

import org.jmol.atomdata.RadiusData;
import org.jmol.util.BS;
import org.jmol.viewer.ShapeManager;


public interface JmolScriptExtension {
  
  public JmolScriptExtension init(Object eval);
  
  public boolean dispatch(int iShape, boolean b, T[] st) throws ScriptException;

  public String plot(T[] args) throws ScriptException;

  public Object getBitsetIdent(BS bs, String label, Object tokenValue,
                               boolean useAtomMap, int index, boolean isExplicitlyAll);

  public boolean evaluateParallel(ScriptContext context,
                                  ShapeManager shapeManager);

  public String write(T[] args) throws ScriptException;

  public BS setContactBitSets(BS bsA, BS bsB, boolean localOnly,
                              float distance, RadiusData rd,
                              boolean warnMultiModel);
  
}
