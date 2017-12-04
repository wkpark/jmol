package org.jmol.adapter.writers;

import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.DF;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.io.JSONWriter;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.quantum.SlaterData;
import org.jmol.util.Vibration;
import org.jmol.viewer.Viewer;

public class QCSchemaWriter extends JSONWriter {

  private final static String version = "QCJSON 0-0-0.Jmol_"
      + Viewer.getJmolVersion().replace(' ', '_');

  private final static String knownUnits =
  /////0         1         2         3         4
  /////01234567890123456789012345678901234567890123456789
      "cm cm^-1 cm-1 angstroms au atomic units";

  private Map<String, Object> moBases = new Hashtable<String, Object>();
  
  private boolean filterMOs;

  public String getUnitsConversion(String units) {
    String convFactor = "\"?\"";
    units = units.toLowerCase();
    switch (knownUnits.indexOf(units)) {
    case 3:
    case 9:
      units = "cm-1";
      break;
    case 14:
      units = "angstroms";
      convFactor = "1.8897";
      break;
    case 24:
    case 27:
      units = "au";
      convFactor = "1";
      break;
    }
    return "[\"" + units + "\"," + convFactor + "]";
  }

  private Viewer vwr;

  public void set(Viewer viewer, OutputStream os) {
    vwr = viewer;
    setStream(os);
  }

  @Override
  public String toString() {
    return (oc == null ? "{}" : oc.toString());
  }

  public void writeJSON() {
    openSchema();
    writeMagic();
    oc.append(",\n");
    writeSchemaMetadata();
    writeJobs();
    closeSchema();
  }

  public void writeSchemaMetadata() {
    mapOpen();
    mapAddKeyValue("__jmol_created", new Date(), ",\n");
    mapAddKeyValue("__jmol_source", vwr.getP("_modelFile"),"");
    mapClose();
  }

  public void openSchema() {
    arrayOpen(false);
  }

  public void writeMagic() {
    writeString(version);
  }

  public void closeSchema() {
    oc.append("\n");
    arrayClose(false);
    closeStream();
  }

  public void writeJobs() {
    // only one job in Jmol
    writeJob(1);
  }

  public void writeJob(int iJob) {    
    append(",\n");
    mapOpen();
    {
      mapAddKeyValue("__jmol_block", "Job " + iJob, ",\n");
      writeJobMetadata();
      writeModels();
      writeMOBases();
    }
    mapClose();
  }

  public void writeJobMetadata() {
    mapAddKey("metadata");
    mapOpen();
    {
      mapAddMapAllExcept("__jmol_info", vwr.getModelSetAuxiliaryInfo(),
          ";group3Counts;properties;group3Lists;models;");
    }
    mapClose();
  }

  public void writeModels() {
    int nModels = vwr.ms.mc;
    oc.append(",\n");
    mapAddKey("steps");
    arrayOpen(true);
    {
      oc.append("\n");
      for (int i = 0; i < nModels;) {
        if (i > 0)
          append(",\n");
        i = writeModel(i);
      }
    }
    arrayClose(true);
  }

  public int writeModel(int modelIndex) {
    int nextModel = modelIndex + 1;
    append("");
    mapOpen();
    {
      mapAddKeyValue("__jmol_block", "Model " + (modelIndex + 1), ",\n");
      writeTopology(modelIndex);
      if (isVibration(modelIndex)) {
        oc.append(",\n");
        nextModel = writeVibrations(modelIndex);
      }
      if (haveMOData(modelIndex)) {
        oc.append(",\n");
        writeMOData(modelIndex);
      }
      oc.append(",\n");
      writeModelMetadata(modelIndex);
    }
    mapClose();
    oc.append("\n");
    return nextModel;
  }

  public void writeTopology(int modelIndex) {
    mapAddKey("topology");
    mapOpen();
    {
      writeAtoms(modelIndex);
      writeBonds(modelIndex);
    }
    mapClose();
  }

  public Object getProperty(int modelIndex, String key) {
    @SuppressWarnings("unchecked")
    Map<String, Object> props = (Map<String, Object>) (modelIndex >= vwr.ms.am.length ? null :  vwr.ms.am[modelIndex].auxiliaryInfo.get("modelProperties"));
    return (props == null ? null : props.get(key));
  }

  private boolean isVibration(int modelIndex) {
    return (getProperty(modelIndex, "Frequency") != null);
  }

  public void writeModelMetadata(int modelIndex) {
    mapAddKey("metadata");
    mapOpen();
    {
      mapAddMapAllExcept("__jmol_info", vwr.ms.am[modelIndex].auxiliaryInfo,
          ";.PATH;PATH;fileName;moData;");
    }
    mapClose();
  }

  public void writeAtoms(int modelIndex) {
    SparseArray symbols = new SparseArray("_RLE_");
    SparseArray numbers = new SparseArray("_RLE_");
    SparseArray charges = new SparseArray("_RLE_");
    SparseArray names = new SparseArray("_RLE_");
    SparseArray types = new SparseArray("_RLE_");
    mapAddKey("atoms");
    mapOpen();
    {
      writePrefix_Units("coords_", "Angstroms");
      mapAddKey("coords");
      arrayOpen(true);
      {
        oc.append("\n");
        BS bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
        int last = bs.length() - 1;
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
          Atom a = vwr.ms.at[i];
          append("");
          oc.append(formatNumber(a.x)).append(",\t")
            .append(formatNumber(a.y)).append(",\t")
            .append(formatNumber(a.z)).append(i < last ? ",\n" : "\n");
          symbols.add(PT.esc(a.getElementSymbol()));
          numbers.add("" + a.getElementNumber());
          charges.add("" + a.getPartialCharge());
          String name = a.getAtomName();
          names.add(name);
          String type = a.getAtomType();
          types.add(type.equals(name) ? null : type);
        }
      }
      arrayClose(true);
      oc.append(",\n");
      if (charges.isNumericAndNonZero()) {
        mapAddKeyValueRaw("charge", charges, ",\n");
      }
      if (types.hasValues()) {
        mapAddKeyValueRaw("types", types, ",\n");
      }
      mapAddKeyValueRaw("symbol", symbols, ",\n");
      mapAddKeyValueRaw("atom_number", numbers, "\n");
    }
    mapClose();
  }

  private String formatNumber(float x) {
    return (x < 0 ? "" : " ") + DF.formatDecimal(x, -6);
  }

  private void writePrefix_Units(String prefix, String units) {
    mapAddKeyValueRaw(prefix + "units", getUnitsConversion(units), ",\n");
  }

  public void writeBonds(int modelIndex) {
    // TODO
  }

  public int writeVibrations(int modelIndex) {
    mapAddKey("vibrations");
    arrayOpen(true);
    {
      oc.append("\n");
      String sep = null;
      int ivib = 0;
      while (isVibration(++modelIndex)) {
        if (sep != null)
          oc.append(sep);
        sep = ",\n";
        append("");
        mapOpen();
        {
          mapAddKeyValue("__jmol_block", "Vibration " + (++ivib), ",\n");
          Object value = getProperty(modelIndex, "FreqValue");
          String freq = (String) getProperty(modelIndex, "Frequency");
          String intensity = (String) getProperty(modelIndex, "IRIntensity");
          if (value == null) {
            System.out.println("model " + modelIndex + " has no _M.properties.FreqValue");
            continue;
          }
          if (freq == null) {
            System.out.println("model " + modelIndex + " has no _M.properties.Frequency");
            continue;
          }
          String[] tokens = PT.split(freq, " ");
          if (tokens.length == 1) {
            System.out.println("model " + modelIndex
                + " has no frequency units");
            continue;
          }
          writeMapKeyValueUnits("frequency", value, tokens[1]);
          if (intensity != null) {
            tokens = PT.split(intensity, " ");
            writeMapKeyValueUnits("ir_intensity", tokens[0], tokens[1]);

          }
            
          String label = (String) getProperty(modelIndex, "FrequencyLabel");
          if (label != null)
            mapAddKeyValue("label", label, ",\n");
          mapAddKey("vectors");
          arrayOpen(true);
          {
            oc.append("\n");
            BS bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
            int last = bs.length() - 1;
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
              Atom a = vwr.ms.at[i];
              Vibration v = a.getVibrationVector();
              append("");
              oc.append(formatNumber(v.x)).append(",\t")
                .append(formatNumber(v.y)).append(",\t")
                .append(formatNumber(v.z)).append(i < last ? ",\n" : "\n");
            }
          }
          arrayClose(true);
        }
        append("");
        mapClose();
      }
    }
    oc.append("\n");
    arrayClose(true);
    return modelIndex;
  }

  private void writeMapKeyValueUnits(String key, Object value,
                                      String units) {
    mapAddKeyValueRaw(key, "{\"value\":" + value + ",\"units\":"
        + getUnitsConversion(units) + "}", ",\n");    
  }

  private boolean haveMOData(int modelIndex) {
    return (getAuxiliaryData(modelIndex, "moData") != null);
  }

  private Object getAuxiliaryData(int modelIndex, String key) {
    return vwr.ms.am[modelIndex].auxiliaryInfo.get(key);
  }

  private int basisID = 0;
  private Lst<int[]> shells;

  private int[][] dfCoefMaps;

  private void writeMOData(int modelIndex) {
    @SuppressWarnings("unchecked")
    Map<String, Object> moData = (Map<String, Object>) getAuxiliaryData(modelIndex, "moData");
    Map<String, Object> moDataJSON = new Hashtable<String, Object>();
    moDataJSON.put("orbitals", moData.get("mos"));
    // units
    String units = (String) moData.get("EnergyUnits");
    if (units == null)
      units = "?";
    moDataJSON.put("orbitals_energy_units",getUnitsConversion(units));
    // normalization is critical for Molden, NWChem, and many other readers.
    // not needed for Gaussian, Jaguar, WebMO, Spartan, or GenNBO
    moDataJSON.put("normalized", Boolean.valueOf(moData.get("isNormalized") == Boolean.TRUE));
    String type = (String) moData.get("calculationType");
    moDataJSON.put("calculation_type", type == null ? "?" : type);
    moDataJSON.put("basis_id", getBasisID(moData));
    filterMOs = true;
    mapAddKeyValue("molecular_orbitals", moDataJSON, "\n");
    filterMOs = false;
    append("");
  }

  @Override
  protected Object getAndCheckValue(Map<String, Object> map, String key) {
    if (filterMOs) {
      if (key.equals("dfCoefMaps"))
        return null;
      if (key.equals("symmetry"))
        return ((String) map.get(key)).replace('_', ' ').trim();
      if (key.equals("coefficients") && dfCoefMaps != null) {
        return fixCoefficients((double[]) map.get(key));
      }
      
    }
    return map.get(key);
  }

  private Object fixCoefficients(double[] coeffs) {
    double[] c = new double[coeffs.length];
    for (int i = 0, n = shells.size(); i < n; i++) {
      int[] shell = shells.get(i);
      int type = shell[1];
      int[] map = dfCoefMaps[type];
      for (int j = 0, coefPtr = 0; j < map.length; j++, coefPtr++)
        c[coefPtr + j] = coeffs[coefPtr + map[j]];
    }
    return c;
  }

  @SuppressWarnings("unchecked")
  private String getBasisID(Map<String, Object> moData) {
    String hash = "!";
    dfCoefMaps = (int[][]) moData.get("dfCoefMaps");
    if (dfCoefMaps != null) {
      // just looking for a non-zero map
      boolean haveMap = false;
      for (int i = 0; !haveMap && i < dfCoefMaps.length; i++) {
        int[] m = dfCoefMaps[i];
        for (int j = 0; j < m.length; j++)
          if (m[j] != 0) {
            haveMap = true;
            break;
          }
      }
      if (!haveMap)
        dfCoefMaps = null;
    }
    Object gaussians = moData.get("gaussians");
    if (gaussians != null) {
      hash += gaussians.hashCode();
    }
    shells = (Lst<int[]>) moData.get("shells");
    if (shells != null) {
      hash += shells.hashCode();
    }
    Object slaters = moData.get("slaters");
    if (slaters != null) {
      hash += slaters.hashCode();
    }
    String key = (String) moBases.get(hash);
    if (key == null) {
      moBases.put(hash, key = "MOBASIS_" + ++basisID);
      Map<String, Object> map = new Hashtable<String, Object>();
      if (gaussians != null) {
        map.put("gaussians", gaussians);
      }
      if (shells != null) {

        // shells array: [iAtom, type, gaussianPtr, gaussianCount]
        //
        // where type is one of:
        //
        //        final public static int S = 0;
        //        final public static int P = 1;
        //        final public static int SP = 2;
        //        final public static int DS = 3;
        //        final public static int DC = 4;
        //        final public static int FS = 5;
        //        final public static int FC = 6;
        //        final public static int GS = 7;
        //        final public static int GC = 8;
        //        final public static int HS = 9;
        //        final public static int HC = 10;
        //        final public static int IS = 11;
        //        final public static int IC = 12;

        // Note that this is currently implemented in Jmol with reference to a 
        // coefficient map that allows us to maintain the file-based MO ordering
        // and only map the actual coefficient to the function at MO creation time.

        map.put("shells", shells);
      }
      if (slaters != null) {
        map.put("slaters", slaters);
      }
      moBases.put(key, map);
    }
    return key;
  }

  public void writeMOBases() {
    if (moBases.isEmpty())
      return;
    oc.append(",\n");
    mapAddKey("mo_bases");
    mapOpen();
    {
      String sep = "";
      for (String key : moBases.keySet()) {
        if (key.startsWith("!"))
          continue;
        append(sep);
        mapAddKeyValue(key, moBases.get(key), "\n");
        sep = ",";
      }
    }
    mapClose();
    moBases.clear();
  }

  @Override
  public void writeObject(Object o) {
    if (o instanceof SlaterData) {
      oc.append(o.toString());
    } else {
      super.writeObject(o);
    }
  }

  //// sparse array handling ////
  public class SparseArray extends SB {
    private int repeatCount = 0;
    private int elementCount = 0;
    private String lastElement = null;
    private String sep = "";
    private String type; // _RLE_
    private boolean isRLE;

    public SparseArray(String type) {
      this.type = type;
      isRLE = (type.equals("_RLE_"));
    }

    protected void add(String element) {
      if (element == null)
        element = "null";
      if (!isRLE) {
        append(sep);
        append(element);
        sep = ",";
        return;
      }
      if (repeatCount > 0 && !element.equals(lastElement)) {
        append(sep);
        appendI(repeatCount);
        sep = ",";
        append(sep);
        append(lastElement);
        repeatCount = 0;
      }
      lastElement = element;
      repeatCount++;
      elementCount++;
    }

    public String lastElement() {
      return lastElement;
    }

    public boolean isEmpty() {
      return (elementCount == 0);
    }

    public boolean allNaN() {
      return (allSame() && PT.parseFloat(lastElement) == Float.NaN);
    }

    public boolean allNull() {
      return (allSame() && lastElement.equals("null"));
    }

    public boolean allEmptyString() {
      return (allSame() && lastElement.equals(""));
    }

    public boolean allSame() {
      return (!isEmpty() && elementCount == repeatCount);
    }

    public boolean allZero() {
      return (allSame() && PT.parseFloat(lastElement) != Float.NaN);
    }

    public boolean hasValues() {
      return (!allSame() || !allNull() && !allEmptyString());
    }

    public boolean isNumericAndNonZero() {
      return (allSame() && !allNaN() && !allZero());
    }

    @Override
    public String toString() {
      String s = super.toString();
      return (s.length() == 0 ? "[]" : "[\""+type+"\"," + s
          + (repeatCount > 0 ? sep + repeatCount + "," + lastElement : "")
          + "]");
    }
  }

}
