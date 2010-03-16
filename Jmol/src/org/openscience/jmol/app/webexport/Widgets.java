package org.openscience.jmol.app.webexport;

import org.jmol.api.JmolViewer;
import org.jmol.g3d.Graphics3D;
import javax.vecmath.Point3f;
import org.jmol.i18n.GT;

public class Widgets { // group of javascript widgets to allow user input to
  // Jmol

  abstract public class Widget {
    JmolViewer viewer;
    String name;
    String script;// jmol script sent by widget
    String javaScript; // html & javascript to define widget

    Widget(JmolViewer viewer) {
      this.viewer = viewer;
    }

    abstract public String getJavaScript(int appletID, String state);// returns
                                                                     // the
                                                                     // JavaScript
                                                                     // and html
                                                                     // to

    // implement the widget. Each Widget should implement this function and make
    // sure to use
    // the appletID number to specify the target applet ie.
    // "JmolApplet<appletID>"

    abstract public String getJavaScriptFileName();// returns the name of the
    // javascript file necessary to implement the widget. Each widget should
    // implement this function. If no file is needed return "none".
    // A COPY OF THIS .JS FILE MUST BE STORED IN THE html PART OF WEBEXPORT

    // TODO add a method for getting list of image files probably should return
    // an array of strings.

  }

  class SpinOnWidget extends Widget {
    SpinOnWidget(JmolViewer viewer) {
      super(viewer);
      this.name = GT._("Spin on/off");
    }

    public String getJavaScriptFileName() {
      return ("JmolSpin.js");
    }

    public String getJavaScript(int appletID, String state) {
      String htmlStr = "<input type=\"checkbox\"";
      if (state.contains("spin on"))
        htmlStr += " checked=\"\"";
      htmlStr += " onchange=\"jmol_spin(this.checked," + appletID + ");\" ";
      htmlStr += "title=\"" + GT._("enable/disable spin") + "\">";
      htmlStr += GT._("Spin on") + "</input>";
      return (htmlStr);
    }
  }

  class BackgroundColorWidget extends Widget {
    BackgroundColorWidget(JmolViewer viewer) {
      super(viewer);
      this.name = GT._("Background Color");
    }

    public String getJavaScriptFileName() {
      return ("JmolColorPicker.js");
    }

    public String getJavaScript(int appletID, String state) {
      String htmlStr = "<table><tbody><tr><td>";
      htmlStr += GT._("background color:");
      htmlStr += "</td><td><script type = 'text/javascript'>";
      htmlStr += "var scriptStr = 'color background $COLOR$;';";
      int beginIndex = state.indexOf("Background");
      beginIndex = state.indexOf("[", beginIndex);
      int endIndex = state.indexOf("]", beginIndex);
      String backColor = state.substring((beginIndex), (endIndex+1));
      Point3f ptRGB = Graphics3D.colorPointFromInt2(Graphics3D.getArgbFromString(backColor));
      backColor = "" + (int)ptRGB.x + "," + (int)ptRGB.y + "," + (int)ptRGB.z;
      htmlStr += "JmolColorPickerBox(scriptStr, [" + backColor + "], 'backbox"
          + appletID + "',  '" + appletID + "');";
      htmlStr += "</script></td></tr></tbody></table>";
      return (htmlStr);
    }
  }

  class StereoViewWidget extends Widget {
    StereoViewWidget(JmolViewer viewer) {
      super(viewer);
      this.name = GT._("Stereo Viewing");
    }

    public String getJavaScriptFileName() {
      return ("none");
    }

    public String getJavaScript(int appletID, String state) {
      String htmlStr = "<select id=\"StereoMode" + appletID + "\" title=\""
          + GT._("select stereo type") + "\"";
      htmlStr += "onchange=\"void(jmolScriptWait((this.options[this.selectedIndex]).value,"
          + appletID + "));\">";
      htmlStr += "\n<option selected=\"\" value=\"" + GT._("stereo off")
          + "\">" + GT._("Stereo Off") + " </option>";
      htmlStr += "\n<option value=\"stereo REDBLUE\">" + GT._("Red/Blue")
          + "</option>";
      htmlStr += "\n<option value=\"stereo REDCYAN\">" + GT._("Red/Cyan")
          + "</option>";
      htmlStr += "\n<option value=\"stereo REDGREEN\">" + GT._("Red/Green")
          + "</option>";
      htmlStr += "\n</select>";
      return (htmlStr);
    }
  }

  class DownLoadWidget extends Widget {
    DownLoadWidget(JmolViewer viewer) {
      super(viewer);
      this.name = GT._("Download view");
    }

    public String getJavaScriptFileName() {
      // TODO
      return ("none");
    }

    public String getJavaScript(int appletID, String state) {
      // TODO
      return (GT._("unimplemented"));
    }
  }

  // public Widget[] widgetList;
  Widget[] widgetList = new Widget[3];

  Widgets(JmolViewer viewer) {
    // this should just be a list of available widgets
    widgetList[0] = new SpinOnWidget(viewer);
    widgetList[1] = new BackgroundColorWidget(viewer);
    widgetList[2] = new StereoViewWidget(viewer);
    // widgetList[3] = new DownLoadWidget(viewer);
  }

}
