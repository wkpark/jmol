/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.util.Hashtable;
import java.util.StringTokenizer;

public class SharcShielding {

  /**
   *  The NMR method used for calculating the shieldings.
   */
  String nmrMethod = "";

  /**
   *  The ab initio method used for the NMR calculation.
   */
  String abInitioMethod = "";

  /**
   *  The basis set used for the NMR calculation.
   */
  String basisSet = "";

  /**
   *  The ab initio method used for optimizing the molecule.
   */
  String optimizationMethod = "";

  /**
   *  The basis set used for optimizing the molecule.
   */
  String optimizationBasisSet = "";

  /**
   *  Map of element to magnetic shielding.
   */
  Hashtable shieldings = new Hashtable();

  /**
   *  String separator used in method notation.
   */
  static final String separator = "/";

  /**
   *  Creates a SHARC NMR shielding.
   */
  public SharcShielding() {
  }

  /**
   *  Creates a SHARC NMR shielding with the method set by parsing a
   *  string representation. The expected format for the string is
   *  nmrMethod/abInitioMethod/basisSet//optimizationMethod/optimizationBasisSet.
   */
  public SharcShielding(String method) {

    int index = method.indexOf(separator + separator);
    String nmrPart;
    String optimizationPart = null;
    if (index >= 0) {
      nmrPart = method.substring(0, index);
      optimizationPart = method.substring(index + 2);
    } else {
      nmrPart = method;
    }

    StringTokenizer tokenizer = new StringTokenizer(nmrPart, separator);
    if (tokenizer.hasMoreTokens()) {
      nmrMethod = tokenizer.nextToken();
    }
    if (tokenizer.hasMoreTokens()) {
      abInitioMethod = tokenizer.nextToken();
    }
    if (tokenizer.hasMoreTokens()) {
      basisSet = tokenizer.nextToken();
    }

    if (optimizationPart != null) {
      tokenizer = new StringTokenizer(optimizationPart, separator);
      if (tokenizer.hasMoreTokens()) {
        optimizationMethod = tokenizer.nextToken();
      }
      if (tokenizer.hasMoreTokens()) {
        optimizationBasisSet = tokenizer.nextToken();
      }
    }
  }

  /**
   *  Returns the NMR method.
   */
  public String getNMRMethod() {
    return nmrMethod;
  }

  /**
   *  Sets the NMR method.
   */
  public void setNMRMethod(String method) {
    nmrMethod = method;
  }

  /**
   *  Returns the ab initio method used for NMR calculation.
   */
  public String getAbInitioMethod() {
    return abInitioMethod;
  }

  /**
   *  Sets the ab initio method used for NMR calculation.
   */
  public void setAbInitioMethod(String method) {
    abInitioMethod = method;
  }

  /**
   *  Returns the basis set used for NMR calculation.
   */
  public String getBasisSet() {
    return basisSet;
  }

  /**
   *  Sets the basis set used for NMR calculation.
   */
  public void setBasisSet(String basisSet) {
    this.basisSet = basisSet;
  }

  /**
   *  Returns the optimization method.
   */
  public String getOptimizationMethod() {
    return optimizationMethod;
  }

  /**
   *  Sets the optimization method.
   */
  public void setOptimizationMethod(String method) {
    optimizationMethod = method;
  }

  /**
   *  Returns the optimization basis set.
   */
  public String getOptimizationBasisSet() {
    return optimizationBasisSet;
  }

  /**
   *  Sets the optimization basis set.
   */
  public void setOptimizationBasisSet(String basisSet) {
    optimizationBasisSet = basisSet;
  }

  /**
   *  Sets the isotropic shielding for an element. If the element
   *  was not previously defined, the element is added.
   */
  public void setShielding(String element, double isotropicShielding) {
    shieldings.put(element, new Double(isotropicShielding));
  }

  /**
   *  Gets the isotropic shielding for an element.
   *
   *  @param element  the element to lookup
   *  @return the istropic shielding value, or 0.0 if the element is not found.
   */
  public double getShielding(String element) {

    double result = 0.0;
    if (shieldings.containsKey(element)) {
      result = ((Double)(shieldings.get(element))).doubleValue();
    }
    return result;
  }

  /**
   *  Whether the isotropic shielding for an element is defined.
   *
   *  @param element  the element to lookup
   *  @return true if the shielding value has been set.
   */
  public boolean containsElement(String element) {
    return shieldings.containsKey(element);
  }

  /**
   *  Returns the method used for calculating this shielding. The format is
   *  nmrMethod/abInitioMethod/basisSet//optimizationMethod/optimizationBasisSet.
   */
  public String getMethod() {

    StringBuffer result = new StringBuffer();
    result.append(nmrMethod);
    result.append(separator);
    result.append(abInitioMethod);
    result.append(separator);
    result.append(basisSet);
    result.append(separator);
    result.append(separator);
    result.append(optimizationMethod);
    result.append(separator);
    result.append(optimizationBasisSet);

    return result.toString();
  }
}


