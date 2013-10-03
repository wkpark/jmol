package org.jmol.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jmol.util.Txt;

import org.jmol.util.J2SIgnoreImport;
import org.jmol.util.Logger;

@J2SIgnoreImport({java.util.ResourceBundle.class,java.util.Locale.class})
class Resource {

  private Object resource;
  String className;
  
  private Resource(Object resource, String className) {
    this.resource = resource;
    this.className = className;
  }

  @SuppressWarnings("null")
  static Resource getResource(String className, String name) {
    String poData = null;
    /**
     * 
     * poData will be returned as byte[] only if it is read properly
     * 
     * @j2sNative
     * 
     *            var base = ClazzLoader.fastGetJ2SLibBase(); var fname = base +
     *            "/trans/" + name + ".po"; poData = Jmol._doAjax(fname, null,
     *            null); if (!poData) return null;
     *            poData = poData.toString();
     * 
     */
    {
      Class<?> bundleClass = null;
      className += name + ".Messages_" + name;
      try {
        bundleClass = Class.forName(className);
      } catch (Throwable e) {
        Logger.error("GT could not find the class " + className);
      }
      try {
        return (bundleClass == null
            || !ResourceBundle.class.isAssignableFrom(bundleClass) ? null
            : new Resource(bundleClass.newInstance(), className));
      } catch (IllegalAccessException e) {
        Logger.warn("Illegal Access Exception: " + e.toString());
      } catch (InstantiationException e) {
        Logger.warn("Instantiation Exception: " + e.toString());
      }
    }
    try {
      return (poData == null ? null : getResourceFromPO(poData));
    } catch (Exception e) {
      return null;
    }
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
     *            language = Jmol.featureDetection.getDefaultLanguage().replace(/-/g,'_');
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
   * @param data 
   * @return JmolResource
   * 
   */
  static Resource getResourceFromPO(String data) {
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
     *            if (data == null || data.length == 0) return null; 
     *            var map = null; try { 
     *              var lines = org.jmol.util.TextFormat.split(data, '\n'); 
     *              map = new java.util.Hashtable(); 
     *              var mode = 0; var msgstr = ""; var msgid = ""; 
     *              for (var i = 0; i < lines.length; i++) { 
     *                var line = lines[i]; 
     *                if (line.length <= 2) { 
     *                  if (mode == 2 && msgstr.length != 0 && msgid.length != 0) map.put(msgid, msgstr); 
     *                } else if (line.indexOf("msgid") == 0) { 
     *                  mode = 1; msgid = org.jmol.i18n.Resource.fix(line); 
     *                } else if (line.indexOf("msgstr") == 0) { 
     *                  mode = 2; msgstr = org.jmol.i18n.Resource.fix(line); 
     *                } else if (mode == 1) { 
     *                  msgid += org.jmol.i18n.Resource.fix(line); 
     *                } else if (mode == 2) { 
     *                  msgstr += org.jmol.i18n.Resource.fix(line); 
     *                } 
     *              }
     *            } catch (e) { } 
     *            return (map == null || map.size() == 0 ? null : new org.jmol.i18n.Resource(map));
     */
    {
      return null;
    }
  }

  static String fix(String line) {
    return Txt.simpleReplace(line.substring(line.indexOf("\"") + 1, line
        .lastIndexOf("\"")), "\\n", "\n");
  }


}
