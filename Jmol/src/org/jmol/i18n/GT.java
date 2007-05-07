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
  private boolean doTranslate = true;
  private String language;

  public GT(String la) {
    getTranslation(la);
  }
  
  private GT() {
    getTranslation(null);
  }

  private static String[][] languageList;
  
  public static String[][] getLanguageList() {
    return (languageList != null ? languageList : getTextWrapper().createLanguageList());
  }

  /**
   * This is the place to put the list of supported languages. It is accessed
   * by JmolPopup to create the menu list. Note that the names are in GT._
   * even though we set doTranslate false. That ensures that the language name
   * IN THIS LIST is untranslated, but it provides the code xgettext needs in
   * order to provide the list of names that will need translation by translators
   * (the .po files). Later, in JmolPopup.updateLanguageMenu(), GT._() is used
   * again to create the actual, localized menu item name. 
   * Introduced in Jmol 11.1.34 
   * @author Bob Hanson May 7, 2007
   * @return  list of codes and untranslated names
   */
  synchronized private String[][] createLanguageList() {
    boolean wasTranslating = doTranslate;
    doTranslate = false;
    languageList = new String[][] {
    {"ca", GT._("Catalan")},
    {"cs", GT._("Czech")},
    {"nl", GT._("Dutch")},
    {"en_US", GT._("English (US)")},
    {"et", GT._("Estonian")},
    {"fr", GT._("French")},
    {"de", GT._("German")},
    {"pt", GT._("Portugese")},
    {"es", GT._("Spanish")},
    {"tr", GT._("Turkish")},};
    doTranslate = wasTranslating;
    return languageList;
  }

  private String getSupported(String languageCode) {
    if (languageCode == null)
      return null;
    if (languageList == null)
      createLanguageList();
    for (int i = 0; i < languageList.length; i++) {
      if (languageList[i][0].equalsIgnoreCase(languageCode))
        return languageList[i][0];
    }
    return findClosest(languageCode);
  }
 
  /**
   * 
   * @param la
   * @return   a localization of the desired language, but not it exactly 
   */
  private String findClosest(String la) {
    for (int i = 0; i < languageList.length; i++) {
      if (languageList[i][0].startsWith(la))
        return languageList[i][0];
    }
    return null;    
  }
  
  public static String getLanguage() {
    return getTextWrapper().language;
  }
  
  synchronized private void getTranslation(String langCode) {
    Locale locale;
    translationResources = null;
    translationResourcesCount = 0;
    getTextWrapper = this;
    if (langCode != null)
      language = langCode;
    if ("none".equals(language))
      language = null;
    if (language == null && (locale = Locale.getDefault()) != null) {
      language = locale.getLanguage();
      if (locale.getCountry() != null) {
        language += "_" + locale.getCountry();
        if (locale.getVariant() != null && locale.getVariant().length() > 0)
          language += "_" + locale.getVariant();
      }
    }
    if (language == null)
      language = "en_US";

    int i;
    String la = language;
    String la_co = language;
    String la_co_va = language;
    if ((i = language.indexOf("_")) >= 0) {
      la = la.substring(0, i);
      if ((i = language.indexOf("_", ++i)) >= 0) {
        la_co = la.substring(0, i);
      } else {
        la_co_va = null;
      }
    } else {
      la_co = null;
      la_co_va = null;
    }

    if ((language = getSupported(la_co_va)) != null) {
    } else if ((language = getSupported(la_co)) != null) {
      la_co_va = null;
    } else if ((language = getSupported(la)) != null) {
      la_co_va = null;
      la_co = null;
    } else {
      la = language = "en";
      la_co = null;
      la_co_va = null;
      Logger.debug("English: no need for gettext wrapper");
      return;
    }
    switch (language.length()) {
    case 2:
      la = language;
      break;
    case 5:
      la_co = language;
      la = language.substring(0, 2);
      break;
    default:
      la_co_va = language;
      la_co = language.substring(0, 5);
      la = language.substring(0, 2);
    }

    if (la.equals("en")) //no variants on Engish for now
      return;

    Logger.debug("Instantiating gettext wrapper for " + language + "...");
    String className;
    try {
      if (!ignoreApplicationBundle) {
        className = "org.jmol.translation.Jmol.Messages_";
        if (la_co_va != null)
          addBundle(className + la_co_va);
        if (la_co != null)
          addBundle(className + la_co);
        if (la != null)
          addBundle(className + la);
      }
    } catch (Exception exception) {
      Logger.error("Some exception occurred!", exception);
      translationResources = null;
    }
    try {
      className = "org.jmol.translation.JmolApplet.Messages_";
      if (la_co_va != null)
        addBundle(className + la_co_va);
      if (la_co != null)
        addBundle(className + la_co);
      if (la != null)
        addBundle(className + la);
    } catch (Exception exception) {
      Logger.error("Some exception occurred!", exception);
    }
  }
  
  private void addBundle(String name_lang) {
    Class bundleClass = null;
    try {
      bundleClass = Class.forName(name_lang);
    } catch (ClassNotFoundException e) {
      Logger.error("GT could not find the class " + name_lang);
    }
    if ((bundleClass != null)
        && ResourceBundle.class.isAssignableFrom(bundleClass)) {
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
    return (getTextWrapper == null ? getTextWrapper = new GT() : getTextWrapper);
  }

  public static void ignoreApplicationBundle() {
    //Logger.warn("Ignore");
    ignoreApplicationBundle = true;
  }

  public static void setDoTranslate(boolean TF) {
    getTextWrapper().doTranslate = TF;
//    System.out.println("setDoTranslate " + doTranslate.booleanValue());
  }

  public static boolean getDoTranslate() {
    return getTextWrapper().doTranslate;
  }

  public static String _(String string) {
    return getTextWrapper().getString(string);
  }

  public static String _(String string, String item) {
    return getTextWrapper().getString(string, new Object[] { item });
  }

  public static String _(String string, int item) {
    return getTextWrapper().getString(string,
        new Object[] { new Integer(item) });
  }

  public static String _(String string, Object[] objects) {
    return getTextWrapper().getString(string, objects);
  }

  //forced translations
  
  public static String _(String string, boolean t) {
    return _(string, (Object[])null, t);
  }

  public static String _(String string, String item, boolean t) {
    return _(string, new Object[] { item });
  }

  public static String _(String string, int item, boolean t) {
    return _(string, new Object[] { new Integer(item) });
  }

  public static synchronized String _(String string, Object[] objects, boolean t) {
    boolean wasTranslating;
    if (!(wasTranslating = getTextWrapper().doTranslate))
      setDoTranslate(true);
    String str = (objects == null ? _(string) : _(string, objects));
    if (!wasTranslating)
      setDoTranslate(false);
    return str;
  }

  private String getString(String string) {
    if (!doTranslate)
      return string;
    for (int bundle = 0; bundle < translationResourcesCount; bundle++) {
      try {
        String trans = translationResources[bundle].getString(string);
        return trans;
      } catch (MissingResourceException e) {
        // Normal
      }
    }
    if (translationResourcesCount > 0) {
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("No trans, using default: " + string);
      }
    }
    return string;
  }

  private String getString(String string, Object[] objects) {
    String trans = null;
    if (!doTranslate)
      return MessageFormat.format(string, objects);
    for (int bundle = 0; bundle < translationResourcesCount; bundle++) {
      try {
        trans = MessageFormat.format(translationResources[bundle]
            .getString(string), objects);
        return trans;
      } catch (MissingResourceException e) {
        // Normal
      }
    }
    trans = MessageFormat.format(string, objects);
    if (translationResourcesCount > 0) {
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("No trans, using default: " + trans);
      }
    }
    return trans;
  }

}
