package org.jmol.api;


/*
 * Used for application - application interface (Jmol-JSpecView)
 *  
 */
public interface JmolSyncInterface {

  public abstract void syncScript(String script);
  public abstract void register(String id, JmolSyncInterface jsi);
  public Object getProperty(String key);

}
