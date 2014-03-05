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

/**
 * Parses JDX-MOL records ##$MODELS and ##$PEAKS/##$SIGNALS. Used in both Jmol
 * and JSpecView.
 * 
 * Also gets info from ACD Labs files JCAMP-DX=5.00 $$ ACD/SpecManager v 12.01
 * 
 */
public class JDXMOLParser implements JmolJDXMOLParser {

  private String line;
  private String lastModel = "";
  private String thisModelID;
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

  /**
   * MOL file embedded in JDX file
   * 
   */
  @Override
  public String readACDMolFile() throws Exception {
    //##$MOLFILE=  $$ Empty String
    //  ACD/Labs03231213092D
    //  $$ Empty String
    // 11 10  0  0  0  0  0  0  0  0 12 V2000
    //...
    //$$$$
    SB sb = new SB();
    sb.append(line.substring(line.indexOf("=") + 1)).appendC('\n');
    while (readLine() != null && !line.contains("$$$$")) {
      line = PT.rep(line, "  $$ Empty String", "");
      sb.append(line).appendC('\n');
    }
    return sb.toString();
  }

  @Override
  public List<float[]> readACDAssignments(int nPoints) throws Exception {
    // ##PEAK ASSIGNMENTS=(XYMA)
    // (25.13376,1.00, ,<1>)
    // (25.13376,1.00, ,<3>)
    // (63.97395,0.35, ,<2>)
    List<float[]> list = new List<float[]>();
    readLine(); // flushes "XYMA"
    for (int i = 0; i < nPoints; i++) {
      line = PT.replaceAllCharacters(readLine(), "()<>", " ");
      Logger.info("Peak Assignment: " + line);
      String[] tokens = PT.split(line, ",");
      if (tokens.length == 4)
        list.addLast(new float[] { PT.parseFloat(tokens[0]),
            PT.parseFloat(tokens[1]), PT.parseInt(tokens[tokens.length - 1]) });
    }
    return list;
  }

  @Override
  public int setACDAssignments(String model, String mytype, int peakCount,
                               List<float[]> acdlist) throws Exception {
    try {
      if (peakCount >= 0)
        peakIndex = new int[] { peakCount };
      String file = " file=" + PT.esc(peakFilePath.replace('\\', '/'));
      model = " model=" + PT.esc(model + " (assigned)");
      piUnitsX = "";
      piUnitsY = "";
      float peakWidth = (mytype.indexOf("CNMR") >= 0 ? 2f : 0.05f);
      Map<String, Object[]> htSets = new Hashtable<String, Object[]>();
      List<Object[]> list = new List<Object[]>();
      int nPeaks = acdlist.size();
      for (int i = 0; i < nPeaks; i++) {
        float[] data = acdlist.get(i);
        float x = data[0];
        int iatom = (int) data[2];
        float xMin = x - peakWidth;
        float xMax = x + peakWidth;
        line = " atoms=\"%ATOMS%\" xMin=\"" + xMin + "\" xMax=\"" + xMax + ">";
        getStringInfo(file, null, mytype, model, "" + iatom, iatom, htSets, "" + x, list);
      }
      return setPeakData(list, 0);
    } catch (Exception e) {
      return 0;
    }
  }

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
			String mytype = PT.getQuotedAttribute(line, "type");
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
					if (mytype == null)
						mytype = PT.getQuotedAttribute(line, "type");
					String atoms = PT.getQuotedAttribute(line, "atoms");
					String key = ((int) (PT.parseFloat(PT
							.getQuotedAttribute(line, "xMin")) * 100))
							+ "_"
							+ ((int) (PT.parseFloat(PT.getQuotedAttribute(line, "xMax")) * 100));
					line = line.substring(tag2.length()).trim();
					getStringInfo(file, title, mytype,
							(PT.getQuotedAttribute(line, "model") == null ? model : ""),
							atoms, -1, htSets, key, list);
				}
			}
			return setPeakData(list, offset);
		} catch (Exception e) {
			return 0;
		}
	}

	private int setPeakData(List<Object[]> list, int offset) {
		int nH = 0;
		int n = list.size();
		for (int i = 0; i < n; i++) {
			Object[] o = list.get(i);
			String info = PT.rep((String) o[0], "%INDEX%", "" + (++peakIndex[0]));
			BS bs = (BS) o[1];
			if (bs != null) {
				String s = "";
				for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
					s += "," + (j + offset);
				int na = bs.cardinality();
				nH += na;
				info = PT.rep(info, "%ATOMS%", s.substring(1));
				info = PT.rep(info, "%S%", (na == 1 ? "" : "s"));
				info = PT.rep(info, "%NATOMS%", "" + na);
			}
			Logger.info("adding PeakData " + info);
			loader.addPeakData(info);
		}
		loader.setSpectrumPeaks(nH, piUnitsX, piUnitsY);
		return n;
}

	private void getStringInfo(String file, String title, String mytype,
			String model, String atoms, int iatom, Map<String, Object[]> htSets,
			String key, List<Object[]> list) {
		if ("HNMR".equals(mytype))
			mytype = "1HNMR";
		else if ("CNMR".equals(mytype))
			mytype = "13CNMR";
		String type = (mytype == null ? "" : " type=" + PT.esc(mytype));
		if (title == null) {
			title = ("1HNMR".equals(mytype) ? "atom%S%: %ATOMS%; integration: %NATOMS%"
					: "");
			title = " title=" + PT.esc(title);
		} else {
			title = "";
		}
		String stringInfo = "<PeakData " + file + " index=\"%INDEX%\"" + title
				+ type + model + " " + line;
		if (atoms != null)
			stringInfo = PT.rep(stringInfo, "atoms=\"" + atoms + "\"",
					"atoms=\"%ATOMS%\"");
		Object[] o = htSets.get(key);
		if (o == null) {
			o = new Object[] { stringInfo, (atoms == null ? null : new BS()) };
			htSets.put(key, o);
			list.addLast(o);
		}
		if (atoms != null) {
			BS bs = (BS) o[1];
			atoms = atoms.replace(',', ' ');
			if (iatom < 0)
				bs.or(BS.unescape("({" + atoms + "})"));
			else
				bs.set(iatom);
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
    String modelType = getAttribute(line, "type").toLowerCase();
    vibScale = PT.parseFloat(getAttribute(line, "vibrationScale"));
    if (modelType.equals("xyzvib"))
      modelType = "xyz";
    else if (modelType.length() == 0)
      modelType = null; // let Jmol set the type
    SB sb = new SB();
    while (readLine() != null && !line.contains("</ModelData>"))
      sb.append(line).appendC('\n');
    loader.processModelData(sb.toString(), thisModelID, modelType, baseModel,
        lastModel, Float.NaN, vibScale, isFirst);
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

  private String readLine() throws Exception {
    return line = loader.readLine();
  }

  @Override
  public void setLine(String s) {
    line = s;
  }

}
