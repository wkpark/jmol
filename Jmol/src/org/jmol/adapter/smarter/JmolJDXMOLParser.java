package org.jmol.adapter.smarter;

import java.util.Map;

import javajs.util.List;


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

  public abstract String readACDMolFile() throws Exception;

  List<float[]> readACDAssignments(int nPoints) throws Exception;

  int setACDAssignments(String model, String type, int peakCount,
                        List<float[]> acdlist) throws Exception;

}
