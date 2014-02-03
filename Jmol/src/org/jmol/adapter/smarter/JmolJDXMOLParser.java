package org.jmol.adapter.smarter;

import java.util.Map;


public interface JmolJDXMOLParser {

  public abstract JmolJDXMOLParser set(JmolJDXMOLReader loader,
                                             String filePath,
                                             Map<String, Object> htParams);

  public abstract String getRecord(String key) throws Exception;

  public abstract String getAttribute(String line, String tag);
  
  public abstract boolean readModels() throws Exception;

  public abstract int readPeaks(boolean isSignals, int peakCount)
      throws Exception;

  public abstract void setLine(String s);

}