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
package org.jmol.util;

import java.text.MessageFormat;
import java.util.*;

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
        System.out.println("English: no need for gettext wrapper");
        return;
      }
    }
    System.out.println("Instantiating gettext wrapper...");
    try {
      translationResources = ResourceBundle.getBundle(
          "org.jmol.translation.Jmol.Messages");
    } catch (MissingResourceException mre) {
      System.out.println("Translations do not seem to have been installed!");
      System.out.println(mre.getMessage());
      translationResources = null;
    } catch (Exception exception) {
      System.out.println("Some exception occured!");
      System.out.println(exception.getMessage());
      exception.printStackTrace();
      translationResources = null;
    }
    try {
     appletTranslationResources = ResourceBundle.getBundle(
         "org.jmol.translation.JmolApplet.Messages");
    } catch (MissingResourceException mre) {
      System.out.println("Applet translations do not seem to have been installed!");
      System.out.println(mre.getMessage());
      appletTranslationResources = null;
    } catch (Exception exception) {
      System.out.println("Some exception occured!");
      System.out.println(exception.getMessage());
      exception.printStackTrace();
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
        //System.out.println("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //System.out.println("No trans, using default: " + string);
        //return string;
      }
    }
    if (appletTranslationResources != null) {
      try {
        String trans = appletTranslationResources.getString(string);
        //System.out.println("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //System.out.println("No trans, using default: " + string);
        //return string;
      }
    }
    if ((translationResources != null) ||
        (appletTranslationResources != null)) {
      System.out.println("No trans, using default: " + string);
    }
    return string;
  }

  private String getString(String string, Object[] objects) {
    String trans = string;
    if (translationResources != null) {
      try {
        trans = MessageFormat.format(translationResources.getString(string), objects);
        //System.out.println("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //trans = MessageFormat.format(string, objects);
        //System.out.println("No trans, using default: " + trans);
      }
    }
    if (appletTranslationResources != null) {
      try {
        trans = MessageFormat.format(appletTranslationResources.getString(string), objects);
        //System.out.println("trans: " + string  + " ->" + trans);
        return trans;
      } catch (MissingResourceException mre) {
        //trans = MessageFormat.format(string, objects);
        //System.out.println("No trans, using default: " + trans);
      }
    }
    trans = MessageFormat.format(string, objects);
    if ((translationResources != null) ||
        (appletTranslationResources != null)) {
      System.out.println("No trans, using default: " + trans);
    }
    return trans;
  }
}
