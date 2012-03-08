package org.jmol.api;

import java.util.Properties;

public interface JSVInterface {
  
  public void setProperties(Properties properties);  
  public void saveProperties(Properties properties);
  public void exitJSpecView(boolean withDialog, Object frame);

}
