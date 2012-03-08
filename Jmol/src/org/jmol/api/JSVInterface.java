package org.jmol.api;

import java.util.Properties;

/**
 * and interface for JSpecView for the Jmol application
 * 
 */
public interface JSVInterface {
  
  public void setProperties(Properties properties);  
  public void saveProperties(Properties properties);
  public void exitJSpecView(boolean withDialog, Object frame);

}
