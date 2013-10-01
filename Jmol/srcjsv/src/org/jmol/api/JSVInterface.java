package org.jmol.api;

import java.util.Properties;

public interface JSVInterface {
  
  public void exitJSpecView(boolean withDialog, Object frame);
  public void runScript(String script);
  public void saveProperties(Properties properties);
  public void setProperties(Properties properties);  
  public void syncToJmol(String msg);
  
}
