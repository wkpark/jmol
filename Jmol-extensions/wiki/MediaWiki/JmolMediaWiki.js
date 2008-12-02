/*
 * Copyright (C) 2006 Nicolas Vervelle,  The Jmol Development Team
 *
 * Contact: nico@jmol.org, jmol-developers@lists.sf.net
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

function jmolWikiPopupWindow(extensionPath, windowTitle, windowSize, windowLeft, windowTop, windowCode) {
  var windowWidth = parseInt(windowSize) + 15;
  var windowHeight = parseInt(windowSize) + 15;
  var opt = "width=" + windowWidth + "," +
            "height=" + windowHeight + "," +
            "toolbar=0,location=0,directories=0,status=0,menubar=0,scrollbars=0,resizable=1," +
            "left=" + windowLeft + ",top=" + windowTop ;
  var s =
    "<html><head>\n" +
    "<script type='text/javascript' src='" + extensionPath + "/Jmol.js'><" + "/script>\n" +
    "<title>" + windowTitle + "</title>\n" +
    "</head><body>" +
    "<script type='text/javascript'>jmolInitialize('" + extensionPath + "', false);\n" +
    windowCode + "\n<" + "/script>\n" +
    "</body></html>";

  var w = open("", "Jmol_popup", opt);	// window name in IE cannot contain spaces or parentheses (and windowTitle may have them)
  w.document.open();
  w.document.write(s);
  w.document.close();
  w.focus();
}
