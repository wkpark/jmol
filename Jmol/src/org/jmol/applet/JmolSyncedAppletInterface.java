package org.jmol.applet;


/*
 * This interface can be used by any applet to register itself
 * with Jmol and thus allow direct applet-applet scripting and 
 * syncing operations.
 *  
 */
public interface JmolSyncedAppletInterface {

  public abstract Object setStereoGraphics(boolean isStereo);

  public abstract String scriptWait(String script, String statusParams);

  public abstract String scriptNoWait(String script);

  public abstract void syncScript(String script);

  public abstract void registerApplet(String appletID, JmolSyncedAppletInterface applet);

}
