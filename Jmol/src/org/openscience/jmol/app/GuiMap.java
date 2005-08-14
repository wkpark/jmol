/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;

import java.util.Hashtable;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBox;

import org.jmol.util.GT;

class GuiMap {

  Hashtable map = new Hashtable();
  
  Hashtable labels = null;
  
  private Hashtable setupLabels() {
      Hashtable labels = new Hashtable();
      labels.put("file", GT._("&File"));
      labels.put("newwin", GT._("New"));
      labels.put("open", GT._("&Open"));
      labels.put("openurl", GT._("Open &URL"));
      labels.put("script", GT._("Scrip&t..."));
      labels.put("atomsetchooser", GT._("AtomSetChooser..."));
      labels.put("saveas", GT._("&Save As..."));
      labels.put("exportMenu", GT._("&Export"));
      labels.put("export", GT._("Export Image..."));
      labels.put("povray", GT._("Render in pov-ray..."));
      labels.put("pdf", GT._("Export PDF..."));
      labels.put("print", GT._("&Print..."));
      labels.put("close", GT._("Close"));
      labels.put("exit", GT._("E&xit"));
      labels.put("recentFiles", GT._("Recent Files..."));
      labels.put("edit", GT._("&Edit"));
      labels.put("makecrystal", GT._("Make crystal..."));
      labels.put("selectall", GT._("Select All"));
      labels.put("deselectall", GT._("Deselect All"));
      labels.put("prefs", GT._("&Preferences..."));
      labels.put("editSelectAllScript", GT._("Select All"));
      labels.put("selectMenu", GT._("Select"));
      labels.put("selectAllScript", GT._("All"));
      labels.put("selectNoneScript", GT._("None"));
      labels.put("selectHydrogenScript", GT._("Hydrogen"));
      labels.put("selectCarbonScript", GT._("Carbon"));
      labels.put("selectNitrogenScript", GT._("Nitrogen"));
      labels.put("selectOxygenScript", GT._("Oxygen"));
      labels.put("selectPhosphorusScript", GT._("Phosphorus"));
      labels.put("selectSulphurScript", GT._("Sulphur"));
      labels.put("selectAminoScript", GT._("Amino"));
      labels.put("selectNucleicScript", GT._("Nucleic"));
      labels.put("selectWaterScript", GT._("Water"));
      labels.put("selectHeteroScript", GT._("Hetero"));
      labels.put("display", GT._("&Display"));
      labels.put("atomMenu", GT._("Atom"));
      labels.put("atomNoneScript", GT._("None"));
      labels.put("atom15Script", GT._("15% vanderWaals"));
      labels.put("atom20Script", GT._("20% vanderWaals"));
      labels.put("atom25Script", GT._("25% vanderWaals"));
      labels.put("atom100Script", GT._("100% vanderWaals"));
      labels.put("bondMenu", GT._("Bond"));
      labels.put("bondNoneScript", GT._("None"));
      labels.put("bondWireframeScript", GT._("Wireframe"));
      labels.put("bond100Script", GT._("0.10 \u00C5"));
      labels.put("bond150Script", GT._("0.15 \u00C5"));
      labels.put("bond200Script", GT._("0.20 \u00C5"));
      labels.put("labelMenu", GT._("Label"));
      labels.put("labelNoneScript", GT._("None"));
      labels.put("labelSymbolScript", GT._("Symbol"));
      labels.put("labelNameScript", GT._("Name"));
      labels.put("labelNumberScript", GT._("Number"));
      labels.put("labelCenteredScript", GT._("Centered"));
      labels.put("labelUpperRightScript", GT._("Upper right"));
      labels.put("vectorMenu", GT._("Vector"));
      labels.put("vectorOffScript", GT._("None"));
      labels.put("vectorOnScript", GT._("On"));
      labels.put("vector3Script", GT._("3 pixels"));
      labels.put("vector005Script", GT._("0.05 \u00C5"));
      labels.put("vector01Script", GT._("0.1 \u00C5"));
      labels.put("vectorScale02Script", GT._("Scale 0.2"));
      labels.put("vectorScale05Script", GT._("Scale 0.5"));
      labels.put("vectorScale1Script", GT._("Scale 1"));
      labels.put("vectorScale2Script", GT._("Scale 2"));
      labels.put("vectorScale5Script", GT._("Scale 5"));
      labels.put("zoomMenu", GT._("Zoom"));
      labels.put("zoom100Script", GT._("100%"));
      labels.put("zoom150Script", GT._("150%"));
      labels.put("zoom200Script", GT._("200%"));
      labels.put("zoom400Script", GT._("400%"));
      labels.put("zoom800Script", GT._("800%"));
      labels.put("wireframerotationCheck", GT._("Wi&reframe Rotation"));
      labels.put("perspectiveCheck", GT._("Perspective Depth"));
      labels.put("axesCheck", GT._("Axes"));
      labels.put("boundboxCheck", GT._("Bounding Box"));
      labels.put("hydrogensCheck", GT._("&Hydrogens"));
      labels.put("vectorsCheck", GT._("&Vectors"));
      labels.put("measurementsCheck", GT._("&Measurements"));
      labels.put("view", GT._("&View"));
      labels.put("front", GT._("Front"));
      labels.put("top", GT._("Top"));
      labels.put("bottom", GT._("Bottom"));
      labels.put("right", GT._("Right"));
      labels.put("left", GT._("Left"));
      labels.put("transform", GT._("Transform..."));
      labels.put("definecenter", GT._("Define Center"));
      labels.put("tools", GT._("Tools"));
      labels.put("viewMeasurementTable", GT._("Measurements..."));
      labels.put("distanceUnitsMenu", GT._("Distance Units"));
      labels.put("distanceNanometersScript", GT._("Nanometers 1E-9"));
      labels.put("distanceAngstromsScript", GT._("Angstroms 1E-10"));
      labels.put("distancePicometersScript", GT._("Picometers 1E-12"));
      labels.put("animateMenu", GT._("Animate..."));
      labels.put("vibrateMenu", GT._("Vibrate..."));
      labels.put("graph", GT._("&Graph..."));
      labels.put("chemicalShifts", GT._("Calculate chemical &shifts..."));
      labels.put("crystprop", GT._("&Crystal Properties"));
      labels.put("animateOnceScript", GT._("Once"));
      labels.put("animateLoopScript", GT._("Loop"));
      labels.put("animatePalindromeScript", GT._("Palindrome"));
      labels.put("animateStopScript", GT._("Stop animation"));
      labels.put("animateRewindScript", GT._("Rewind to first frame"));
      labels.put("animateNextScript", GT._("Go to next frame"));
      labels.put("animatePrevScript", GT._("Go to previous frame"));
      labels.put("vibrateStartScript", GT._("Start vibration"));
      labels.put("vibrateStopScript", GT._("Stop vibration"));
      labels.put("vibrateRewindScript", GT._("First frequency"));
      labels.put("vibrateNextScript", GT._("Next frequency"));
      labels.put("vibratePrevScript", GT._("Previous frequency"));
      labels.put("help", GT._("&Help"));
      labels.put("about", GT._("About Jmol"));
      labels.put("uguide", GT._("User Guide"));
      labels.put("whatsnew", GT._("What's New"));
      labels.put("console", GT._("Jmol Console"));
      labels.put("Prefs.showHydrogens", GT._("Hydrogens"));
      labels.put("Prefs.showMeasurements", GT._("Measurements"));
      labels.put("Prefs.wireframeRotation", GT._("Wireframe Rotation"));
      labels.put("Prefs.perspectiveDepth", GT._("Perspective Depth"));
      labels.put("Prefs.showAxes", GT._("Axes"));
      labels.put("Prefs.showBoundingBox", GT._("Bounding Box"));
      labels.put("Prefs.axesOrientationRasmol", GT._("RasMol/Chime compatible axes orientation/rotations"));
      labels.put("Prefs.openFilePreview", GT._("File Preview (needs restarting Jmol)"));
      labels.put("Prefs.isLabelAtomColor", GT._("Use Atom Color"));
      labels.put("Prefs.isBondAtomColor", GT._("Use Atom Color"));
      labels.put("openTip", GT._("Open a file."));
      labels.put("exportTip", GT._("Export view to an image file."));
      labels.put("povrayTip", GT._("Render in pov-ray."));
      labels.put("printTip", GT._("Print view."));
      labels.put("rotateTip", GT._("Rotate molecule."));
      labels.put("pickTip", GT._("Select an atom or region."));
      labels.put("viewMeasurementTableTip", GT._("View measurement table."));
      labels.put("homeTip", GT._("Return molecule to home position."));
      labels.put("animateRewindScriptTip", GT._("Rewind to first frame"));
      labels.put("animateNextScriptTip", GT._("Go to next frame"));
      labels.put("animatePrevScriptTip", GT._("Go to previous frame"));

      return labels;
  }

  String getLabel(String key) {
    if (labels == null) {
      labels = setupLabels();
    }
    String label = (String)labels.get(key);
    if (label == null)
    // Use the previous system as backup
    if (label == null) {
      System.out.println("Missing i18n menu resource, trying old scheme for: " +key);
      JmolResourceHandler.getStringX(key+"Label");
    }
    return label;
  }

  JMenu newJMenu(String key) {
    String label = getLabel(key);
    return new KeyJMenu(key, getLabelWithoutMnemonic(label), getMnemonic(label));
  }
  
  JMenuItem newJMenuItem(String key) {
    String label = getLabel(key);
    return new KeyJMenuItem(key, getLabelWithoutMnemonic(label), getMnemonic(label));
  }
  JCheckBoxMenuItem newJCheckBoxMenuItem(String key, boolean isChecked) {
    String label = getLabel(key);
    return new KeyJCheckBoxMenuItem(key, getLabelWithoutMnemonic(label), getMnemonic(label), isChecked);
  }
  JRadioButtonMenuItem newJRadioButtonMenuItem(String key) {
    String label = getLabel(key);
    return new KeyJRadioButtonMenuItem(key, getLabelWithoutMnemonic(label), getMnemonic(label));
  }
  JCheckBox newJCheckBox(String key, boolean isChecked) {
    String label = getLabel(key);
    return new KeyJCheckBox(key, getLabelWithoutMnemonic(label), getMnemonic(label), isChecked);
  }

  Object get(String key) {
    return map.get(key);
  }

  String getKey(Object obj) {
    return (((GetKey)obj).getKey());
  }

  public String getLabelWithoutMnemonic(String label) {
    if (label == null) {
      return null;
    }
    int index = label.indexOf('&');
    if (index == -1) {
      return label;
    }
    return label.substring(0, index) +
      ((index < label.length() - 1) ? label.substring(index + 1) : "");
  }
  
  public char getMnemonic(String label) {
    if (label == null) {
      return ' ';
    }
    int index = label.indexOf('&');
    if ((index == -1) || (index == label.length() - 1)){
      return ' ';
    }
    return label.charAt(index + 1);
  }
  
  void setSelected(String key, boolean b) {
    ((AbstractButton)get(key)).setSelected(b);
  }

  boolean isSelected(String key) {
    return ((AbstractButton)get(key)).isSelected();
  }


  interface GetKey {
    public String getKey();
  }

  class KeyJMenu extends JMenu implements GetKey {
    String key;
    KeyJMenu(String key, String label, char mnemonic) {
      super(label);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJMenuItem extends JMenuItem implements GetKey {
    String key;
    KeyJMenuItem(String key, String label, char mnemonic) {
      super(label);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJCheckBoxMenuItem
    extends JCheckBoxMenuItem implements GetKey {
    String key;
    KeyJCheckBoxMenuItem(String key, String label, char mnemonic, boolean isChecked) {
      super(label, isChecked);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJRadioButtonMenuItem
    extends JRadioButtonMenuItem implements GetKey {
    String key;
    KeyJRadioButtonMenuItem(String key, String label, char mnemonic) {
      super(label);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }

  class KeyJCheckBox
    extends JCheckBox implements GetKey {
    String key;
    KeyJCheckBox(String key, String label, char mnemonic, boolean isChecked) {
      super(label, isChecked);
      if (mnemonic != ' ') {
          setMnemonic(mnemonic);
      }
      this.key = key;
      map.put(key, this);
    }
    public String getKey() {
      return key;
    }
  }
}

