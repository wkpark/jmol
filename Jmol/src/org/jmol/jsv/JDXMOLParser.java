package org.jmol.jsv;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.JmolJDXMOLReader;
import org.jmol.adapter.smarter.JmolJDXMOLParser;
import org.jmol.java.BS;
import org.jmol.util.Logger;

import javajs.util.List;
import javajs.util.PT;
import javajs.util.SB;

public class JDXMOLParser implements JmolJDXMOLParser {

  private String line;
  private String lastModel = "";
  private String thisModelID;
  private String modelType;
  private String baseModel;

  private float vibScale;
  private String piUnitsX, piUnitsY;

  private JmolJDXMOLReader loader;

  private String modelIdList = "";
  private int[] peakIndex;
  private String peakFilePath;

  public JDXMOLParser() {
    // for reflection
  }

  @Override
  public JmolJDXMOLParser set(JmolJDXMOLReader loader, String filePath,
                                    Map<String, Object> htParams) {
    this.loader = loader;
    peakFilePath = filePath;
    peakIndex = new int[1];
    if (htParams != null) {
      htParams.remove("modelNumber");
      // peakIndex will be passed on to additional files in a ZIP file load
      // the peak file path is stripped of the "|xxxx.jdx" part 
      if (htParams.containsKey("zipSet")) {
        peakIndex = (int[]) htParams.get("peakIndex");
        if (peakIndex == null) {
          peakIndex = new int[1];
          htParams.put("peakIndex", peakIndex);
        }
        if (!htParams.containsKey("subFileName"))
          peakFilePath = PT.split(filePath, "|")[0];
      }
    }
    return this;
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#getAttribute(java.lang.String, java.lang.String)
   */
  @Override
  public String getAttribute(String line, String tag) {
    String attr = PT.getQuotedAttribute(line, tag);
    return (attr == null ? "" : attr);
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#getRecord(java.lang.String)
   */
  @Override
  public String getRecord(String key) throws Exception {
    if (line == null || line.indexOf(key) < 0)
      return null;
    String s = line;
    while (s.indexOf(">") < 0)
      s += " " + readLine();
    return line = s;
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#readModels()
   */
  @Override
  public boolean readModels() throws Exception {
    if (!findRecord("Models"))
      return false;
    // if load xxx.jdx n  then we must temporarily set n to 1 for the base model reading
    // load xxx.jdx 0  will mean "load only the base model(s)"
    line = "";
    thisModelID = "";
    boolean isFirst = true;
    while (true) {
      line = loader.discardLinesUntilNonBlank();
      if (getRecord("<ModelData") == null)
        break;
      getModelData(isFirst);
      // updateModel here regardless???
      isFirst = false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#readPeaks(boolean, int)
   */
  @Override
  public int readPeaks(boolean isSignals, int peakCount) throws Exception {
    try {
      if (peakCount >= 0)
        peakIndex = new int[] { peakCount };
      int offset = (isSignals ? 1 : 0);
      String tag1 = (isSignals ? "Signals" : "Peaks");
      String tag2 = (isSignals ? "<Signal" : "<PeakData");
      if (!findRecord(tag1))
        return 0;
      String file = " file=" + PT.esc(peakFilePath.replace('\\', '/'));
      String model = PT.getQuotedAttribute(line, "model");
      model = " model=" + PT.esc(model == null ? thisModelID : model);
      String type = PT.getQuotedAttribute(line, "type");
      if ("HNMR".equals(type))
        type = "1HNMR";
      else if ("CNMR".equals(type))
        type = "13CNMR";
      type = (type == null ? "" : " type=" + PT.esc(type));
      piUnitsX = PT.getQuotedAttribute(line, "xLabel");
      piUnitsY = PT.getQuotedAttribute(line, "yLabel");
      Map<String, Object[]> htSets = new Hashtable<String, Object[]>();
      List<Object[]> list = new List<Object[]>();
      while (readLine() != null
          && !(line = line.trim()).startsWith("</" + tag1)) {
        if (line.startsWith(tag2)) {
          getRecord(tag2);
          Logger.info(line);
          String title = PT.getQuotedAttribute(line, "title");
          if (title == null) {
            title = (type == "1HNMR" ? "atom%S%: %ATOMS%; integration: %NATOMS%"
                : "");
            title = " title=" + PT.esc(title);
          } else {
            title = "";
          }
          String stringInfo = "<PeakData " + file + " index=\"%INDEX%\""
              + title + type
              + (PT.getQuotedAttribute(line, "model") == null ? model : "")
              + " " + line.substring(tag2.length()).trim();
          String atoms = PT.getQuotedAttribute(stringInfo, "atoms");
          if (atoms != null)
            stringInfo = PT.rep(stringInfo, "atoms=\"" + atoms + "\"",
                "atoms=\"%ATOMS%\"");
          String key = ((int) (PT.parseFloat(PT
              .getQuotedAttribute(line, "xMin")) * 100))
              + "_"
              + ((int) (PT.parseFloat(PT.getQuotedAttribute(line, "xMax")) * 100));
          Object[] o = htSets.get(key);
          if (o == null) {
            o = new Object[] { stringInfo, (atoms == null ? null : new BS()) };
            htSets.put(key, o);
            list.addLast(o);
          }
          BS bs = (BS) o[1];
          if (bs != null) {
            atoms = atoms.replace(',', ' ');
            bs.or(BS.unescape("({" + atoms + "})"));
          }
        }
      }
      int nH = 0;
      int n = list.size();
      for (int i = 0; i < n; i++) {
        Object[] o = list.get(i);
        String stringInfo = (String) o[0];
        stringInfo = PT.rep(stringInfo, "%INDEX%", "" + (++peakIndex[0]));
        BS bs = (BS) o[1];
        if (bs != null) {
          String s = "";
          for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
            s += "," + (j + offset);
          int na = bs.cardinality();
          nH += na;
          stringInfo = PT.rep(stringInfo, "%ATOMS%", s.substring(1));
          stringInfo = PT.rep(stringInfo, "%S%", (na == 1 ? "" : "s"));
          stringInfo = PT.rep(stringInfo, "%NATOMS%", "" + na);
        }
        Logger.info("adding PeakData " + stringInfo);
        loader.addPeakData(stringInfo);
      }
      loader.setSpectrumPeaks(nH, piUnitsX, piUnitsY);
      return n;
    } catch (Exception e) {
      return 0;
    }
  }
  
  private void getModelData(boolean isFirst) throws Exception {
    lastModel = thisModelID;
    thisModelID = getAttribute(line, "id");
    // read model only once for a given ID
    String key = ";" + thisModelID + ";";
    if (modelIdList.indexOf(key) >= 0) {
      line = loader.discardLinesUntilContains("</ModelData>");
      return;
    }
    modelIdList += key;
    baseModel = getAttribute(line, "baseModel");
    while (line.indexOf(">") < 0 && line.indexOf("type") < 0)
      readLine();
    modelType = getAttribute(line, "type").toLowerCase();
    vibScale = PT.parseFloat(getAttribute(line, "vibrationScale"));
    if (modelType.equals("xyzvib"))
      modelType = "xyz";
    else if (modelType.length() == 0)
      modelType = null; // let Jmol set the type
    SB sb = new SB();
    while (readLine() != null && !line.contains("</ModelData>"))
      sb.append(line).appendC('\n');
    loader.processModelData(sb.toString(), thisModelID, modelType, baseModel, lastModel, vibScale, isFirst);
  }

  /**
   * @param tag
   * @return line
   * @throws Exception
   */
  private boolean findRecord(String tag) throws Exception {
    if (line == null)
      readLine();
    if (line.indexOf("<" + tag) < 0)
      line = loader.discardLinesUntilContains2("<" + tag, "##");
    return (line.indexOf("<" + tag) >= 0);
  }

  private Object readLine() throws Exception {
    return line = loader.readLine();
  }

  @Override
  public void setLine(String s) {
    line = s;
  }

}
