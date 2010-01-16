/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

public class XmlUtil {

  // / simple Xml parser/generator ///

  public static void openDocument(StringBuffer data) {
    data.append("<?xml version=\"1.0\"?>\n");
  }

  public static void openTag(StringBuffer sb, String name) {
    sb.append("<").append(name).append(">\n");
  }

  public static void openTag(StringBuffer sb, String name, Object[] attributes) {
    appendTag(sb, name, attributes, null, false, false);
    sb.append("\n");
  }

  public static void closeTag(StringBuffer sb, String name) {
    sb.append("</").append(name).append(">\n");
  }

  public static void appendTag(StringBuffer sb, String name,
                               Object[] attributes, String data,
                               boolean isCdata, boolean doClose) {
    sb.append("<").append(name);
    if (attributes != null)
      for (int i = 0; i < attributes.length; i++) {
        Object o = attributes[i];
        if (o instanceof Object[])
          appendAttrib(sb, ((Object[]) o)[0], ((Object[]) o)[1]);
        else
          appendAttrib(sb, o, attributes[++i]);
      }
    sb.append(">");
    if (data != null) {
      if (isCdata)
        sb.append("<![CDATA[");
      sb.append(data);
      if (isCdata)
        sb.append("]]>");
    }
    if (doClose)
      closeTag(sb, name);
  }

  /**
   * standard <name attr="..." attr="...">data</name>"
   * 
   * @param sb
   * @param name
   * @param sep
   * @param attributes
   * @param data
   */
  public static void appendTag(StringBuffer sb, String name,
                               String[] attributes, String data) {
    appendTag(sb, name, attributes, data, false, true);
  }

  /**
   * standard <name>data</name>"
   * 
   * @param sb
   * @param name
   * @param data
   */

  public static void appendTag(StringBuffer sb, String name, String data) {
    appendTag(sb, name, null, data, false, true);
  }

  /**
   * <name><![CDATA[data]]></name>"
   * 
   * will convert ]]> to ]]_>
   * 
   * @param sb
   * @param name
   * @param data
   */

  public static void appendCdata(StringBuffer sb, String name, String data) {
    if (data.indexOf("]]>") >= 0)
      data = TextFormat.simpleReplace(data, "]]>", "]]_>");
    appendTag(sb, name, null, data, true, true);
  }

  /**
   * 
   * @param sb
   * @param name
   * @param value
   * @param sep
   */
  public static void appendAttrib(StringBuffer sb, Object name, Object value) {
    if (value == null)
      return;
    sb.append(" ").append(name).append("=\"").append(value).append("\"");
  }

}
