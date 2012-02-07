package org.jmol.api;


/*
 * This interface can be used by any applet to register itself
 * with Jmol and thus allow direct applet-applet scripting and 
 * syncing operations.
 *  
 */
public interface JmolScriptInterface extends JmolSyncInterface {

  public abstract Object setStereoGraphics(boolean isStereo);

  public abstract String scriptWait(String script, String statusParams);

  public abstract String scriptNoWait(String script);

}
