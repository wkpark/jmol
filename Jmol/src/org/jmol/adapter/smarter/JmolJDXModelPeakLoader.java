package org.jmol.adapter.smarter;

public interface JmolJDXModelPeakLoader {

  String readLine() throws Exception;

  String discardLinesUntilContains2(String tag1, String tag2) throws Exception;

  String discardLinesUntilContains(String string) throws Exception;

  String discardLinesUntilNonBlank() throws Exception;

  void processModelData(String id, String data, String type, String base,
                        String last, float vibScale, boolean isFirst)
      throws Exception;

  void setSpectrumPeaks(int nH, String piUnitsX, String piUnitsY);

  void addPeakData(String info);


}
