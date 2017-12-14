package org.jmol.adapter.writers;

import javajs.util.Lst;

import org.jmol.viewer.Viewer;

public class QCSchema {

  public final static String version = "QCJSON 0-0-0.Jmol_"
      + Viewer.getJmolVersion().replace(' ', '_');

  // 
  //  http://cccbdb.nist.gov/hartree.asp
  //  A hartree is equal to 2625.5 kJ/mol, 627.5 kcal/mol, 27.211 eV, and 219474.6 cm-1.
  //  One bohr = 0.529 177 210 67 x 10-10 m 
  
  public final static String UNITS_FRACTIONAL = "fractional";

  public final static String UNITS_AU = "au",               TOAU_AU = "1";
  // distance
  public final static String UNITS_ANGSTROMS = "angstroms", TOAU_ANGSTROMS = "1.88972613";
  public final static String UNITS_BOHR = "bohr",           TOAU_BOHR = "1";
  // eneergy
  public final static String UNITS_HARTREE = "hartree",     TOAU_HARTREE = "1";
  public final static String UNITS_EV = "ev",               TOAU_EV = "0.03688675765";
  public final static String UNITS_CM_1 = "cm-1",           TOAU_CM_1 = "4.5563359e-6";
  public final static String UNITS_KJ_MOL = "kj/mol",       TOAU_KJ_MOL = "0.00038087983";
  public final static String UNITS_KCAL_MOL = "kcal/mol",   TOAU_KCAL_MOL = "0.00159362549";
  
  // TODO: should be an Enum
  public final static String knownUnits =
  /////0         1         2         3         4         5         6         7         8
  /////012345678901234567890123456789012345678901234567890123456789012345678901234567890123
      "cm cm^-1 cm-1 angstroms au atomic units fractional bohr hartree ev kj_mol kcal_mol";


  public static float getUnitConversion(String units, String toUnits) {
    if (units.equalsIgnoreCase(toUnits))
      return 1;
    try {
      float toAUDesired = Float.parseFloat(getFactorToAU(toUnits));
      float toAUActual = Float.parseFloat(getFactorToAU(units));
      if (Float.isNaN(toAUActual))
        return 1;
      return toAUActual / toAUDesired;
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   * Get the nominal conversion factor to atomic units for this unit as a string.
   * The nominal conversion factor for "fractional" is 0; for unknown units, "?"
   * 
   * @param units
   * @return the nominal conversion factor or 0 (fractional) or "?" (unknown; with quotes)
   */
  public static String getFactorToAU(String units) {
    String convFactor = "\"?\"";
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
      convFactor = "1";
      break;
    case 40:
      units = "UNITS_FRACTIONAL";
      convFactor = "0";
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
   * @return the conversion factor or Float.NaN if not uncodable
   */
  public static float getConversionFactorTo(Lst<Object> unitsFactor, String unitsDesired) {
    try {
    float toAUDesired = Float.parseFloat(getFactorToAU(unitsDesired));
    float toAUActual = Float.parseFloat(getFactorToAU(unitsFactor == null ? UNITS_AU : unitsFactor.get(0).toString()));
    if (Float.isNaN(toAUActual))
      toAUActual = Float.parseFloat(unitsFactor.get(1).toString());
    return toAUActual / toAUDesired;
    } catch (Exception e) {
      return Float.NaN;
    }
  }


}
