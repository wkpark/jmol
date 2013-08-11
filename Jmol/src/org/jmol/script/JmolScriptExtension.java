package org.jmol.script;


public interface JmolScriptExtension {
  
  public JmolScriptExtension init(Object eval);
  
  public boolean dispatch(int iShape, boolean b, T[] st) throws ScriptException;

  public String plot(T[] args) throws ScriptException;

}
