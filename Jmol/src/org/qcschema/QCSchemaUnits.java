package org.qcschema;

import java.util.Hashtable;
import java.util.Map;

import java.util.ArrayList;

import org.jmol.viewer.Viewer;

/**
 * A general Java class for working with QCShema units and array types.
 * 
 * j2sNative blocks can be ignored -- they just increase efficiency in the JavaScript rendition.
 *  
 */
public class QCSchemaUnits {

  public final static String version = "QCJSON 0-0-0.Jmol_"
      + Viewer.getJmolVersion().replace(' ', '_');

  // 
  //  http://cccbdb.nist.gov/hartree.asp
  //  A hartree is equal to 2625.5 kJ/mol, 627.5 kcal/mol, 27.211 eV, and 219474.6 cm-1.
  //  One bohr = 0.529 177 210 67 x 10-10 m 
  
  public final static String UNITS_FRACTIONAL = "fractional";

  public final static String UNITS_AU = "au";
  public final static double TOAU_AU = 1;
  // distance
  public final static String UNITS_ANGSTROMS = "angstroms";
  public final static double TOAU_ANGSTROMS = 1.88972613;
  public final static String UNITS_BOHR = "bohr";
  public final static double TOAU_BOHR = 1;
  // energy
  public final static String UNITS_HARTREE = "hartree";
  public final static double TOAU_HARTREE = 1;
  public final static String UNITS_EV = "ev";
  public final static double TOAU_EV = 0.03688675765;
  public final static String UNITS_CM_1 = "cm-1";
  public final static double TOAU_CM_1 = 4.5563359e-6;
  public final static String UNITS_KJ_MOL = "kj/mol";
  public final static double TOAU_KJ_MOL = 0.00038087983;
  public final static String UNITS_KCAL_MOL = "kcal/mol";
  public final static double TOAU_KCAL_MOL = 0.00159362549;
  
  // TODO: should be an Enum
  public final static String knownUnits =
  /////0         1         2         3         4         5         6         7         8
  /////012345678901234567890123456789012345678901234567890123456789012345678901234567890123
      "cm cm^-1 cm-1 angstroms au atomic units fractional bohr hartree ev kj_mol kcal_mol";


  private static Hashtable<String, Double> htConvert = new Hashtable<String, Double>();
  
  /**
   * Calculate the unit conversion between two units, using a static 
   * unit-to-unit cache for efficiency.
   * 
   * @param fromUnits
   * @param toUnits
   * @return conversion factor
   */
  public static double getUnitConversion(String fromUnits, String toUnits) {
    if (fromUnits.equalsIgnoreCase(toUnits))
      return 1;
    String key = "" + fromUnits + toUnits;
    Double d = htConvert.get(key);
    if (d != null)
      return d.doubleValue();
    double val = 1;
    try {
      double toAUDesired = getFactorToAU(toUnits);
      double toAUActual = getFactorToAU(fromUnits);
      if (!Double.isNaN(toAUActual))
        val = toAUActual / toAUDesired;
    } catch (Exception e) {
      // just leave it as 1
    }
    htConvert.put(key,  Double.valueOf(val));
    return val;
  }

  /**
   * Get the ["units", toAU] JSON code or just a new String[] {units, toAU}
   * @param units
   * @param asArray
   * @return String or String[]
   */
  public static Object getUnitsJSON(String units, boolean asArray) {
    double d = getFactorToAU(units);
    String toAU = (!Double.isNaN(d) ? "" + d : asArray ? "?" :  "\"?\"");
    return (asArray ? new String[] { units, toAU } : "[\"" + units + "\","
        + toAU + "]");
  }


  /**
   * Get the nominal conversion factor to atomic units for this unit as a string.
   * The nominal conversion factor for "fractional" is 0; for unknown units, "?"
   * 
   * @param units
   * @return the nominal conversion factor or 0 (fractional) or "?" (unknown; with quotes)
   */
  public static double getFactorToAU(String units) {
    double convFactor = Double.NaN;
    units = units.toLowerCase();
    switch (knownUnits.indexOf(units)) {
    case 3:
    case 9:
      units = UNITS_CM_1;
      convFactor = TOAU_CM_1;
      break;
    case 14:
      units = UNITS_ANGSTROMS;
      convFactor = TOAU_ANGSTROMS;
      break;
    case 24:
    case 27:
      units = UNITS_AU;
      convFactor = 1;
      break;
    case 40:
      units = "UNITS_FRACTIONAL";
      convFactor = 0;
      break;
    case 51:
      units = UNITS_BOHR;
      convFactor = TOAU_BOHR;
      break;
    case 56:
      units = UNITS_HARTREE;
      convFactor = TOAU_HARTREE;
      break;
    case 64:
      units = UNITS_EV;
      convFactor = TOAU_EV;
      break;
    case 67:
      units = UNITS_KCAL_MOL;
      convFactor = TOAU_KCAL_MOL;
      break;
    case 74:
      units = UNITS_KJ_MOL;
      convFactor = TOAU_KJ_MOL;
      break;

    }
    return convFactor;
  }
  
  /**
   * For a reader, use the [units, factor] along with a desired unit to get the conversion
   * factor from file values to desired units.
   * 
   * @param unitsFactor
   * @param unitsDesired
   * @return the conversion factor or Double.NaN if not uncodable
   */
  public static double getConversionFactorTo(ArrayList<Object> unitsFactor, String unitsDesired) {
    try {
    double toAUDesired = getFactorToAU(unitsDesired);
    double toAUActual = getFactorToAU(unitsFactor == null ? UNITS_AU : unitsFactor.get(0).toString());
    if (Double.isNaN(toAUActual))
      toAUActual = Double.parseDouble(unitsFactor.get(1).toString());
    return toAUActual / toAUDesired;
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  /**
   * Read a {value:xxxx, units:["name",toUnits]} map, converting it to the desired units.
   * 
   * @param valueUnits
   * @param toUnits
   * @return converted value
   */
  public static double convertValue(Map<String, Object> valueUnits, String toUnits) {
      return getDouble(valueUnits, "value", null) * getConversionFactor(valueUnits, "units", toUnits);
  }

  /**
   * Get the necessary conversion factor to the desired units from a key_units or atomic units 
   * @param map
   * @param key map key that has associated key_units element or null for "from atomic units"
   * @param toUnits
   * @return conversion factor
   */
  public static double getConversionFactor(Map<String, Object> map, String key, String toUnits) {
    ArrayList<Object> list = getList(map, key + "_units");
    String units = (list == null ? null : list.get(0).toString());
    double f = getConversionFactorTo(list, toUnits);
    if (Double.isNaN(f)) {
      System.out.println("units for " + units + "? " + units);
      f = 1;
    }
    return f;
  }

  /**
   * Reads a value from an associative array, converting it to the desired units.
   * 
   * @param map
   * @param key
   * @param toUnits
   * @return value
   */
  @SuppressWarnings("unchecked")
  public static double getDouble(Map<String, Object> map, String key, String toUnits) {
    Object o = map.get(key);
    double conv = 1;
    if (toUnits != null)
      if (o instanceof Map<?, ?>) {
        //  "frequency":{"value":-0.00,"units":["cm^-1",4.5563359e-6]},
        return convertValue((Map<String, Object>) o, toUnits);
      } else if (map.containsKey(key + "_units")) {
        //  "frequency_units":["cm^-1",4.5563359e-6],
        //  "frequency":-0.00,
        conv = getConversionFactor(map, key, toUnits);
      }
    return (o == null ? Double.NaN : ((Number) o).doubleValue() * conv);
  }

  /**
   * Retrieve an array of any sort as a list of objects, possibly unpacking it
   * if it is run-length encoded.
   * 
   * @param mapOrList
   * @param key
   * @return unpacked array
   */
  public static ArrayList<Object> getList(Object mapOrList, String key) {
    @SuppressWarnings("unchecked")
    ArrayList<Object> list = (ArrayList<Object>) (key == null ? mapOrList
        : ((Map<String, Object>) mapOrList).get(key));
    if (list == null)
      return null;
    int n = list.size();
    if (n == 0 || !"_RLE_".equals(list.get(0)))
      return list;
    ArrayList<Object> list1 = newList();
    for (int i = 1; i < n; i++) {
      int count = ((Number) list.get(i)).intValue();
      Object value = list.get(++i);
      for (int j = 0; j < count; j++)
      /**
       * j2s avoids overloaded add() for speed
       * 
       * @j2sNative list1.addLast(value);
       */
      {
        list1.add(value);
      }
    }
    return list1;
  }

  /**
   * @return ArrayList, or in JavaScript javajs.util.Lst
   *  
   * @j2sNative
   * 
   *  return new JU.Lst();
   */
  protected static ArrayList<Object> newList() {
      return new ArrayList<Object>();
  }

  /**
   * Retrieve a double array, possibly unpacking it if it is run-length encoded.
   * Read any error as Double.NaN. 
   * 
   * @param mapOrList
   * @param key  into mapOrList, or null if mapOrList is a list
   * @return unpacked double[]
   */
  public static double[] getDoubleArray(Object mapOrList, String key) {
    ArrayList<Object> list = getList(mapOrList, key);
    if (list == null)
      return null;
    double[] a = new double[list.size()];
    for (int i = a.length; --i >= 0;) {
      try {
        a[i] = ((Number) list.get(i)).doubleValue();
      } catch (Exception e) {
        a[i] = Double.NaN;
      }
    }
    return a;
  }

  /**
   * Retrieve an int array, possibly unpacking it if it is run-length encoded.
   * Errors are not trapped. 
   * 
   * @param mapOrList
   * @param key null if mapOrList is a list
   * @return unpacked int[]
   */
  public static int[] getIntArray(Object mapOrList, String key) {
    ArrayList<Object> list = getList(mapOrList, key);
    if (list == null)
      return null;
    int[] a = new int[list.size()];
    for (int i = a.length; --i >= 0;)
      a[i] = ((Number) list.get(i)).intValue();
    return a;
  }

  /**
   * Retrieve a String array, possibly unpacking it if it is run-length encoded.
   * Any "null" string is read as null. 
   * 
   * @param mapOrList
   * @param key into mapOrlist if it is a map, or null if mapOrList is a list
   * @return unpacked String[]
   */
  public static String[] getStringArray(Object mapOrList, String key) {
    ArrayList<Object> list = getList(mapOrList, key);
    if (list == null)
      return null;
    String[] a = new String[list.size()];
    for (int i = a.length; --i >= 0;) {
      a[i] = list.get(i).toString();
      if (a[i].equals("null"))
        a[i] = null;
    }
    return a;
  }

}
