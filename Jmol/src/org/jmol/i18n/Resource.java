package org.jmol.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jmol.util.TextFormat;

import org.jmol.util.J2SIgnoreImport;
import org.jmol.util.Logger;

@J2SIgnoreImport({java.util.ResourceBundle.class,java.util.Locale.class})
class Resource {

  private Object resource;
  
  private Resource(Object resource) {
    this.resource = resource;
  }

  static Resource getResource(String className, String name) {
    /**
     * 
     * poData will be returned as byte[] only if it is read properly
     * 
     * @j2sNative
     * 
     *            var base = ClazzLoader.fastGetJ2SLibBase(); var fname = base +
     *            "/trans/" + name + ".po"; var poData = Jmol._doAjax(fname,
     *            null, null); return (poData == null || !(Clazz.instanceOf
     *            (poData, Array)) ? null :
     *            org.jmol.i18n.Resource.getResourceFromPO (poData));
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
        return new Resource(resource);
      } catch (IllegalAccessException e) {
        Logger.warn("Illegal Access Exception: " + e.toString());
      } catch (InstantiationException e) {
        Logger.warn("Instantiation Exception: " + e.toString());
      }
    }
    return null;
  }

  String getString(String string) {
    /**
     * @j2sNative
     * 
     *            return this.resource.get(string);
     * 
     */
    {
      try {
        return ((ResourceBundle) resource).getString(string);
      } catch (Exception e) {
        return null;
      }
    }
  }

  static String getLanguage() {
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
  static Resource getResourceFromPO(byte[] bytes) {
    /*
    #: org/jmol/console/GenericConsole.java:94
    msgid "press CTRL-ENTER for new line or paste model data and press Load"
    msgstr ""
    "pulsa Ctrl+Intro para una línea nueva o pega datos de un modelo y luego "
    "pulsa Cargar"
       */
    /**
     * @j2sNative
     * 
     *            if (bytes == null || bytes.length == 0) return null; var map =
     *            null; try { var lines =
     *            org.jmol.util.TextFormat.split(String.instantialize(bytes,
     *            "UTF-8"), '\n'); map = new org.jmol.util.Hashtable(); var mode
     *            = 0; var msgstr = ""; var msgid = ""; for (var i = 0; i <
     *            lines.length; i++) { var line = lines[i]; if (line.length <=
     *            2) { if (mode == 2 && msgstr.length != 0 && msgid.length != 0)
     *            map.put(msgid, msgstr); } else if (line.indexOf("msgid") == 0)
     *            { mode = 1; msgid = org.jmol.i18n.fix(line); } else if
     *            (line.indexOf("msgstr") == 0) { mode = 2; msgstr =
     *            org.jmol.i18n.fix(line); } else if (mode == 1) { msgid +=
     *            org.jmol.i18n.fix(line); } else if (mode == 2) { msgstr +=
     *            org.jmol.i18n.fix(line); } }
     *            } catch (e) { //ignore } return (map == null || map.size() ==
     *            0 ? null : new org.jmol.i18n.Resource(map));
     */
    {
      return null;
    }
//    if (bytes == null || bytes.length == 0)
//      return null;
//    Hashtable<String, String> map = null;
//       try {
//      String[] lines = TextFormat.split(new String(bytes, "UTF-8"), '\n');
//      map = new Hashtable<String, String>();
//      int mode = 0;
//      String msgstr = "";
//      String msgid = "";
//      for (int i = 0; i < lines.length; i++) {
//        String line = lines[i];
//        if (line.length() <= 2) {
//          if (mode == 2 && msgstr.length() != 0 && msgid.length() != 0)
//            map.put(msgid, msgstr);
//        } else if (line.indexOf("msgid") == 0) {
//          mode = 1;
//          msgid = fix(line);
//        } else if (line.indexOf("msgstr") == 0) {
//          mode = 2;
//          msgstr = fix(line);
//        } else if (mode == 1) {
//          msgid += fix(line);
//        } else if (mode == 2) {
//          msgstr += fix(line);
//        }
//      }
//      
//    } catch (UnsupportedEncodingException e) {
//      //ignore
//    }    
//    return (map == null || map.size() == 0 ? null : new Resource(map));
  }

  static String fix(String line) {
    return TextFormat.simpleReplace(line.substring(line.indexOf("\"") + 1, line
        .lastIndexOf("\"")), "\\n", "\n");
  }


}
