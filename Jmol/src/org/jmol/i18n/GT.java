/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.i18n;

import java.text.MessageFormat;
import java.util.*;
import org.jmol.util.Logger;

public class GT {

  private static boolean ignoreApplicationBundle = false;
  private static GT getTextWrapper;
  private ResourceBundle[] translationResources = null;
  private int translationResourcesCount = 0;

  private GT() {
    Locale locale = Locale.getDefault();
    if ((locale != null) &&
        (locale.getLanguage() != null)) {
      String language = locale.getLanguage();
      if ((language.equals("")) ||
          (language.equals(Locale.ENGLISH.getLanguage()))) {
        Logger.debug("English: no need for gettext wrapper");
        return;
      }
    }
    Logger.debug("Instantiating gettext wrapper...");
    try {
      if (!ignoreApplicationBundle) {
        addBundles("org.jmol.translation.Jmol.Messages");
      }
    } catch (Exception exception) {
      Logger.error("Some exception occured!", exception);
      translationResources = null;
    }
    try {
      addBundles("org.jmol.translation.JmolApplet.Messages");
    } catch (Exception exception) {
      Logger.error("Some exception occured!", exception);
    }
  }

  private void addBundles(String name) {
    Locale locale = Locale.getDefault();
    if ((locale != null) && (locale.getLanguage() != null)) {
      if (locale.getCountry() != null) {
        if (locale.getVariant() != null) {
          // NOTE: Currently, there's no need for variants
          /*addBundle(
              name + "_" + locale.getLanguage() +
              "_" + locale.getCountry() + "_" + locale.getVariant());*/
        }
        // NOTE: Currently, there's no need for countries
        //addBundle(name + "_" + locale.getLanguage() + "_" + locale.getCountry());
      }
      addBundle(name + "_" + locale.getLanguage());
    }
    // NOTE: Currently, there's no base class
    //addBundle(name);
  }

  private void addBundle(String name) {
    ClassLoader loader = getClass().getClassLoader();
    Class bundleClass = null;
    try {
      if (loader != null) {
        bundleClass = loader.loadClass(name);
      } else {
        bundleClass = Class.forName(name);
      }
    } catch (ClassNotFoundException e) {
      // Class not found: can be normal
    }
    if ((bundleClass != null) && ResourceBundle.class.isAssignableFrom(bundleClass)) {
      try {
        ResourceBundle myBundle = (ResourceBundle) bundleClass.newInstance();
        if (myBundle != null) {
          if (translationResources == null) {
            translationResources = new ResourceBundle[8];
            translationResourcesCount = 0;
          }
          translationResources[translationResourcesCount] = myBundle;
          translationResourcesCount++;
        }
      } catch (IllegalAccessException e) {
        Logger.warn("Illegal Access Exception: " + e.getMessage());
      } catch (InstantiationException e) {
        Logger.warn("Instantiation Excaption: " + e.getMessage());
      }
    }
  }

  private static GT getTextWrapper() {
    if (getTextWrapper == null) {
      getTextWrapper = new GT();
    }
    return getTextWrapper;
  }

  public static void ignoreApplicationBundle() {
    Logger.warn("Ignore");
    ignoreApplicationBundle = true;
  }

  public static String _(String string) {
    return getTextWrapper().getString(string);
  }

  public static String _(String string, Object[] objects) {
    return getTextWrapper().getString(string, objects);
  }

  private String getString(String string) {
    for (int bundle = 0; bundle < translationResourcesCount; bundle++) {
      try {
        String trans = translationResources[bundle].getString(string);
        return trans;
      } catch (MissingResourceException e) {
        // Normal
      }
    }
    if (translationResourcesCount > 0) {
      Logger.debug("No trans, using default: " + string);
    }
    return string;
  }

  private String getString(String string, Object[] objects) {
    String trans = null;
    for (int bundle = 0; bundle < translationResourcesCount; bundle++) {
      try {
        trans = MessageFormat.format(
            translationResources[bundle].getString(string), objects);
        return trans;
      } catch (MissingResourceException e) {
        // Normal
      }
    }
    trans = MessageFormat.format(string, objects);
    if (translationResourcesCount > 0) {
      Logger.debug("No trans, using default: " + trans);
    }
    return trans;
  }
}
