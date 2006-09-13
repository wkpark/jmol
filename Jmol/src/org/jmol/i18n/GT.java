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
    
  private static GT getTextWrapper = new GT();
  private ResourceBundle translationResources;
  private ResourceBundle appletTranslationResources;

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
      translationResources = ResourceBundle.getBundle(
          "org.jmol.translation.Jmol.Messages");
    } catch (MissingResourceException mre) {
      Logger.warn("Translations do not seem to have been installed!");
      Logger.warn(mre.getMessage());
      translationResources = null;
    } catch (Exception exception) {
      Logger.error("Some exception occured!", exception);
      translationResources = null;
    }
    try {
     appletTranslationResources = ResourceBundle.getBundle(
         "org.jmol.translation.JmolApplet.Messages");
    } catch (MissingResourceException mre) {
      Logger.warn("Applet translations do not seem to have been installed!");
      Logger.warn(mre.getMessage());
      appletTranslationResources = null;
    } catch (Exception exception) {
      Logger.error("Some exception occured!", exception);
      appletTranslationResources = null;
    }
  }

  public static String _(String string) {
    return getTextWrapper.getString(string);
  }

  public static String _(String string, Object[] objects) {
    return getTextWrapper.getString(string, objects);
  }

  private String getString(String string) {
    if (translationResources != null) {
      try {
        String trans = translationResources.getString(string);
        //Logger.debug("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //Logger.debug("No trans, using default: " + string);
        //return string;
      }
    }
    if (appletTranslationResources != null) {
      try {
        String trans = appletTranslationResources.getString(string);
        //Logger.debug("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //Logger.debug("No trans, using default: " + string);
        //return string;
      }
    }
    if ((translationResources != null) ||
        (appletTranslationResources != null)) {
      Logger.debug("No trans, using default: " + string);
    }
    return string;
  }

  private String getString(String string, Object[] objects) {
    String trans = string;
    if (translationResources != null) {
      try {
        trans = MessageFormat.format(translationResources.getString(string), objects);
        //Logger.debug("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //trans = MessageFormat.format(string, objects);
        //Logger.debug("No trans, using default: " + trans);
      }
    }
    if (appletTranslationResources != null) {
      try {
        trans = MessageFormat.format(appletTranslationResources.getString(string), objects);
        //Logger.debug("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //trans = MessageFormat.format(string, objects);
        //Logger.debug("No trans, using default: " + trans);
      }
    }
    trans = MessageFormat.format(string, objects);
    if ((translationResources != null) ||
        (appletTranslationResources != null)) {
      Logger.debug("No trans, using default: " + trans);
    }
    return trans;
  }
}
