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

// several fixes by AH, Dec. 08

function jmolWikiPopupWindow(extensionPath, windowTitle, windowSize, windowLeft, windowTop, windowCode) {
  var windowWidth = parseInt(windowSize) + 15;
  var windowHeight = parseInt(windowSize) + 15;
  var opt = "width=" + windowWidth + "," +
            "height=" + windowHeight + "," +
            "resizable," +
            "left=" + windowLeft + ",top=" + windowTop ;
  var s =
    "<html><head>\n" +
    "<title>" + windowTitle + "</title>\n" +
    "</head><body>\n" +
	
  var t1,t2,j1,j2
  var j1 = windowCode.indexOf("jmolApplet");
  eval(windowCode.substring(0,j1));	// execute the windowCode before "jmolApplet" (jmolInitialize, jmolSetAppletColor etc.)
  jmolSetDocument(false);			// execute this
  t1 = windowCode.substring(j1);	// part of windowCode including "jmolApplet" and whatever follows
  j1 = t1.indexOf("\(");
  var ap1 = t1.substring(0,j1);		// "jmolApplet"  or  "jmolAppletInline"
  t2 = t1.substring(j1+1);
  j1 = t2.indexOf(",");
  var ap2 = t2.substring(0,j1);		// applet size
  j2 = t2.indexOf("\);");			// end of jmolApplet part
  var ap3 = t2.substring(j1+2,j2);	// script
  ap3 = ap3.replace(/\n/g, "|");	// protect newlines in inline data
  var ap4 = t2.substring(j2+2);		// whatever is after jmolApplet (only jmolBr ? )

  // make the popup applet resizable:
  ap2 = '"100%"';	// overwrites the former size
  ap3 = ap3.charAt(0) + "set zoomLarge off; " + ap3.substring(1);	// skips the quote and inserts command
  
  s += eval( ap1 + "(" + ap2 + ", " + ap3 + ")" );	// put into page the code resulting from jmolApplet
  s += eval(ap4);	// put into page the code resulting from whatever is after jmolApplet (jmolBr)
  
    "</body></html>";
  
	// window name in IE cannot contain spaces or parentheses (and windowTitle may have anything)
	// Therefore, avoid "non-word" characters (i.e. other than A-Z, numbers and underscore)
  var purgedTitle = windowTitle.replace(/\W/g, "_");
  var w = open("", purgedTitle, opt);
  w.document.open();
  w.document.write(s);
  w.document.close();
  w.focus();
}


function setupCheckboxShiftClick() {
	return;
	/* prevent the function in wikibits.js from acting	
		so that checkboxes work
	*/
}
