/*
 * Copyright 2002-2003 The Jmol Development Team
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
package org.openscience.jmol.io;

import org.xml.sax.InputSource;
import org.xml.sax.ext.EntityResolver2;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

/**
 * This class provides access to the CML DTDs by resolving the system id of a
 * DOCTYPE declaration to a local resource. Since an applet can only access the
 * its server, the DTD must be found within the applet resources.
 *
 * <p>This class implements the EntityResolver2 rather than the EntityResolver
 * because of problems in the gnujaxp XML library currently used
 * (gnujaxp-1.0beta1). If the EntityResolver interface is used, the parser will
 * try to "absolutize" the system id URI before trying to resolve it. For system
 * ids which are not full URIs, this causes an exception from which no recovery
 * is possible.
 * </p>
 */
public class CMLResolver implements EntityResolver2 {

  public CMLResolver() {
  }

  /**
   * Returns null since no external subsets are provided.
   *
   * @param name unused.
   * @param baseURI unused.
   * @return null.
   */
  public InputSource getExternalSubset(String name, String baseURI) {
    return null;
  }

  /**
   * This method delegates to the {@link #resolveEntity(String, String)
   * resolveEntity(publicId, systemId)} method.
   *
   * @param name unused.
   * @param publicId passed through.
   * @param baseURI unused.
   * @param systemId passed through.
   * @return the result from {@link #resolveEntity(String, String)
   * resolveEntity(publicId, systemId)}
   * @see #resolveEntity(String, String)
   */
  public InputSource resolveEntity(String name, String publicId,
      String baseURI, String systemId) {

    return resolveEntity(publicId, systemId);
  }

  /**
   * Resolves CML DTDs based solely on the system id. The system id must contain
   * one of the strings "cml.dtd", "cml1_0_1.dtd", "cml1_0.dtd",
   * "cml-2001-04-06.dtd", or "cml-1999-05-15.dtd".
   *
   * @param publicId unused.
   * @param systemId the system id of the DTD.
   * @return the InputSource to the DTD or null if the resolution failed.
   */
  public InputSource resolveEntity(String publicId, String systemId) {

    InputSource input = null;
    systemId = systemId.toLowerCase();
    if ((systemId.indexOf("cml.dtd") >= 0)
        || (systemId.indexOf("cml1_0.dtd") >= 0)
          || (systemId.indexOf("cml-1999-05-15.dtd") >= 0)) {
      input = getInputSource("org/openscience/jmol/Data/cml1_0.dtd.txt");
    } else if ((systemId.indexOf("cml1_0_1.dtd") >= 0)
        || (systemId.indexOf("cml-2001-04-06.dtd") >= 0)) {
      input = getInputSource("org/openscience/jmol/Data/cml1_0_1.dtd.txt");
    } else {
      System.err.println("jmol.CMLResolver: Could not resolve \"" + systemId
          + "\"");
    }
    return input;
  }

  /**
   * Creates an InputSource to the resource given.
   *
   * @param resource a resource accessible from this class.
   * @return the InputSource to the resource or null if the resource was not
   *   found.
   */
  private InputSource getInputSource(String resource) {

    InputStream input = this.getClass().getClassLoader().getResourceAsStream(resource);
    if (input != null) {
      return new InputSource(new BufferedReader(new InputStreamReader(input)));
    }
    System.err.println("jmol.CMLResolver: Unable to find resource \""
        + resource + "\"");
    return null;
  }
}

