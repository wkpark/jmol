package org.jmol.api;

public interface JmolSyncInterface {

  public abstract void syncScript(String script);

  public abstract void registerApplet(String appletID, JmolSyncInterface jsi);

}
