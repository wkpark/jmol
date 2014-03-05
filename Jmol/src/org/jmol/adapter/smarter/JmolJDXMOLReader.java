package org.jmol.adapter.smarter;

public interface JmolJDXMOLReader {

  void addPeakData(String info);

  String discardLinesUntilContains2(String tag1, String tag2) throws Exception;

  String discardLinesUntilContains(String string) throws Exception;

  String discardLinesUntilNonBlank() throws Exception;

  void processModelData(String data, String id, String type, String base,
                        String last, float modelScale, float vibScale, boolean isFirst)
      throws Exception;

  String readLine() throws Exception;

  void setSpectrumPeaks(int nH, String piUnitsX, String piUnitsY);


}
