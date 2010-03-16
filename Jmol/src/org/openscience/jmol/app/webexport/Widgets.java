package org.openscience.jmol.app.webexport;

import javax.vecmath.Point3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.i18n.GT;

class Widgets { 
  
  // group of javascript widgets to allow user input to Jmol

  Widget[] widgetList = new Widget[3];

  Widgets() {
    // this should just be a list of available widgets
    widgetList[0] = new SpinOnWidget();
    widgetList[1] = new BackgroundColorWidget();
    widgetList[2] = new StereoViewWidget();
    // widgetList[3] = new DownLoadWidget();
  }

  abstract class Widget {
    String name;

    /**
     * 
     * Each Widget must implement this function and make sure to use
     * the appletID number to specify the target applet i.e. "JmolApplet<appletID>"
     * @param appletID
     * @param instance
     * @return  the JavaScript and html to implement the widget
     */
    abstract String getJavaScript(int appletID, JmolInstance instance);

    /**
     *  
     *  A COPY OF THIS .JS FILE MUST BE STORED IN THE html PART OF WEBEXPORT
     *  
     * @return  "none" (no file needed) or javascript file necessary to implement the widget
     */
    abstract String getJavaScriptFileName();// returns the name of the

    // TODO add a method for getting list of image files probably should return
    // an array of strings.

  }

  class SpinOnWidget extends Widget {
    SpinOnWidget() {
      name = GT._("Spin on/off");
    }

    String getJavaScriptFileName() {
      return "JmolSpin.js";
    }

    String getJavaScript(int appletID, JmolInstance instance) {
      return "<input type=\"checkbox\""
          + (instance.spinOn ? " checked=\"\"" : "")
          + " onchange=\"jmol_spin(this.checked," + appletID + ");\" "
          + "title=\"" + GT._("enable/disable spin") + "\">"
          + GT._("Spin on") + "</input>";
    }
  }

  class BackgroundColorWidget extends Widget {
    BackgroundColorWidget() {
      name = GT._("Background Color");
    }

    String getJavaScriptFileName() {
      return ("JmolColorPicker.js");
    }

    String getJavaScript(int appletID, JmolInstance instance) {
      Point3f ptRGB = Graphics3D.colorPointFromInt2(instance.bgColor);
      return "<table><tbody><tr><td>"
          + GT._("background color:")
          + "</td><td><script type = 'text/javascript'>"
          + "var scriptStr = 'color background $COLOR$;';"
          + "JmolColorPickerBox(scriptStr, [" 
          + (int)ptRGB.x + "," + (int)ptRGB.y + "," + (int)ptRGB.z
          + "], 'backbox"
          + appletID + "',  '" + appletID + "');"
          + "</script></td></tr></tbody></table>";
    }
  }

  class StereoViewWidget extends Widget {
    StereoViewWidget() {
      name = GT._("Stereo Viewing");
    }

    String getJavaScriptFileName() {
      return "none";
    }

    String getJavaScript(int appletID, JmolInstance instance) {
      return "<select id=\"StereoMode" + appletID + "\" title=\""
          + GT._("select stereo type") + "\""
          + "onchange=\"void(jmolScriptWait((this.options[this.selectedIndex]).value,"
          + appletID + "));\">"
          + "\n<option selected=\"\" value=\"" + GT._("stereo off")
          + "\">" + GT._("Stereo Off") + " </option>"
          + "\n<option value=\"stereo REDBLUE\">" + GT._("Red/Blue")
          + "</option>"
          + "\n<option value=\"stereo REDCYAN\">" + GT._("Red/Cyan")
          + "</option>"
          + "\n<option value=\"stereo REDGREEN\">" + GT._("Red/Green")
          + "</option>"
          + "\n</select>";
    }
  }

  class DownLoadWidget extends Widget {
    DownLoadWidget() {
      name = GT._("Download view");
    }

    String getJavaScriptFileName() {
      // TODO
      return ("none");
    }

    String getJavaScript(int appletID, JmolInstance instance) {
      // TODO
      return (GT._("unimplemented"));
    }
  }

}
