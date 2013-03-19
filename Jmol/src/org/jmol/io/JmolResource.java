package org.jmol.io;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.jmol.util.TextFormat;

import org.jmol.util.J2SIgnoreImport;
import org.jmol.util.Logger;

@J2SIgnoreImport({java.util.ResourceBundle.class,java.util.MissingResourceException.class, java.util.Locale.class})
public class JmolResource {

  private Object resource;
  
  private JmolResource(Object resource) {
    this.resource = resource;
  }

  @SuppressWarnings("null")
  public static JmolResource getResource(String className, String name) {
    Object poData = null;
    /**
     * @j2sNative
     * 
     * var base = ClazzLoader.fastGetJ2SLibBase();
     * var fname = base + "/trans/" + name + ".po";
     * poData = Jmol._doAjax(fname, null, null); 
     * 
     */
    { 
      Class<?> bundleClass = null;
      className += name + ".Messages_" + name;
      //    if (languagePath != null
      //      && !ZipUtil.isZipFile(languagePath + "_i18n_" + name + ".jar"))
        //  return;
      try {
        bundleClass = Class.forName(className);
      } catch (Throwable e) {
        Logger.error("GT could not find the class " + className);
      }
      if (bundleClass == null
          || !ResourceBundle.class.isAssignableFrom(bundleClass))
        return null;
      try {
        ResourceBundle resource = (ResourceBundle) bundleClass.newInstance();
        return new JmolResource(resource);
      } catch (IllegalAccessException e) {
        Logger.warn("Illegal Access Exception: " + e.toString());
      } catch (InstantiationException e) {
        Logger.warn("Instantiation Exception: " + e.toString());
      }
    }
    // poData will be returned as byte[] only if it is read properly
    return (poData == null || 
        !(poData instanceof byte[]) ? null : getResourceFromPO((byte[]) poData));
  }

  public String getString(String string) {
    /**
     * @j2sNative
     * 
     *            return this.resource.get(string);
     * 
     */
    {
      try {
        return ((ResourceBundle) resource).getString(string);
      } catch (MissingResourceException e) {
        return null;
      }
    }
  }

  public static String getLanguage() {
    String language = null;
    /**
     * @j2sNative
     * 
     *            language = (navigator.language || navigator.userLanguage);
     * 
     */
    {
      Locale locale = Locale.getDefault();
      if (locale != null) {
        language = locale.getLanguage();
        if (locale.getCountry() != null) {
          language += "_" + locale.getCountry();
          if (locale.getVariant() != null && locale.getVariant().length() > 0)
            language += "_" + locale.getVariant();
        }
      }
    }
    return language;
  }

  /**
   * 
   * JavaScript only
   * @param bytes 
   * @return JmolResource
   * 
   */
  static JmolResource getResourceFromPO(byte[] bytes) {
    /*
    #: org/jmol/console/GenericConsole.java:94
    msgid "press CTRL-ENTER for new line or paste model data and press Load"
    msgstr ""
    "pulsa Ctrl+Intro para una línea nueva o pega datos de un modelo y luego "
    "pulsa Cargar"
       */
    if (bytes == null || bytes.length == 0)
      return null;
    Hashtable<String, String> map = null;
       try {
      String[] lines = TextFormat.split(new String(bytes, "UTF-8"), '\n');
      map = new Hashtable<String, String>();
      int mode = 0;
      String msgstr = "";
      String msgid = "";
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (line.length() <= 2) {
          if (mode == 2 && msgstr.length() != 0 && msgid.length() != 0)
            map.put(msgid, msgstr);
        } else if (line.indexOf("msgid") == 0) {
          mode = 1;
          msgid = fix(line);
        } else if (line.indexOf("msgstr") == 0) {
          mode = 2;
          msgstr = fix(line);
        } else if (mode == 1) {
          msgid += fix(line);
        } else if (mode == 2) {
          msgstr += fix(line);
        }
      }
      
    } catch (UnsupportedEncodingException e) {
      //ignore
    }    
    return (map == null || map.size() == 0 ? null : new JmolResource(map));
  }

  private static String fix(String line) {
    return TextFormat.simpleReplace(line.substring(line.indexOf("\"") + 1, line
        .lastIndexOf("\"")), "\\n", "\n");
  }


}
