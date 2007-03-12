 /* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.popup;

import java.util.Properties;

import org.jmol.i18n.GT;

class PopupResourceBundle {

  
  PopupResourceBundle() {
    // Nothing
  }

  String getStructure(String key) {
    return structure.getProperty(key);
  }

  void addStructure(String key, String value) {
    structure.setProperty(key, value);
  }

  String getWord(String key) {
    String str = words.getProperty(key);
    if (str == null) {
      str = key;
    }
    return str;
  }

  // Properties to store menu structure and contents
  private final static Properties structure = new Properties();
  private final static Properties words = new Properties();

  final static String INHERIT = "none";
  final static String COLOR = "black white red orange yellow green cyan blue indigo violet";
  final static String SCHEME = "SchemeMenu";
  final static String TRANSLUCENCY = "opaque translucent";
  final static String AXESCOLOR = "gray salmon maroon olive slateblue gold orchid";

  
  static String Box(String cmd) {
    return "if not(showBoundBox);if not(showUnitcell);boundbox on;"+cmd+";boundbox off;else;"+cmd+";endif;endif;";
  }
  private static final String[][] structureContents = {
      {
          "popupMenu",
          "modelSetMenu FRAMESbyModelComputedMenu CONFIGURATIONComputedMenu - selectMenu viewMenu renderMenu colorMenu - surfaceMenu SYMMETRYunitCellMenu - "
              + "zoomMenu spinMenu VIBRATIONMenu "
              + "FRAMESanimateMenu - "
              + "measureMenu pickingMenu - showConsole showMenu - "
              + "aboutComputedMenu" },
              
      {
          "selectMenu",
          "hideNotSelected;hide(none)Checkbox showSelectionsCheckbox - selectAll selectNone invertSelection - elementsComputedMenu SYMMETRYComputedMenu - "
              + "PDBproteinMenu PDBnucleicMenu PDBheteroMenu PDBcarboMenu PDBnoneOfTheAbove" },
      { "selectAll", "all" },
      { "selectNone", "none" },
      { "invertSelection", "not selected" },
      { "hideNotSelected;hide(none)Checkbox", "selected; hide not selected" },

      {
          "PDBproteinMenu", "PDBaaResiduesComputedMenu - "
          + "allProtein proteinBackbone proteinSideChains - "
              + "polar nonpolar - "
              + "positiveCharge negativeCharge noCharge" },
      { "allProtein", "protein" },
      { "proteinBackbone", "protein and backbone" },
      { "proteinSideChains", "protein and not backbone" },
      { "polar", "protein and polar" },
      { "nonpolar", "protein and not polar" },
      { "positive ", "protein and basic" },
      { "negativeCharge", "protein and acidic" },
      { "noCharge", "protein and not (acidic,basic)" },
      { "allCarbo", "carbohydrate" },

      {
        "PDBcarboMenu",
        "PDBcarboResiduesComputedMenu - allCarbo" },

      {
          "PDBnucleicMenu",
          "PDBnucleicResiduesComputedMenu - allNucleic nucleicBackbone nucleicBases - " + "DNA RNA - "
              + "atPairs auPairs gcPairs" },
      { "allNucleic", "nucleic" },
      { "DNA", "dna" },
      { "RNA", "rna" },
      { "nucleicBackbone", "nucleic and backbone" },
      { "nucleicBases", "nucleic and not backbone" },
      { "atPairs", "a,t" },
      { "gcPairs", "g,c" },
      { "auPairs", "a,u" },
      { "A", "a" },
      { "C", "c" },
      { "G", "g" },
      { "T", "t" },
      { "U", "u" },

      {
          "PDBheteroMenu",
          "PDBheteroComputedMenu - allHetero Solvent Water - "
              + "Ligand exceptWater nonWaterSolvent" },
      { "allHetero", "hetero" },
      { "Solvent", "solvent" },
      { "Water", "water" },
      // same as ligand    { "exceptSolvent", "hetero and not solvent" },
      { "nonWaterSolvent", "solvent and not water" },
      { "exceptWater", "hetero and not water" },
      { "Ligand", "ligand" },

      // not implemented    { "Lipid", "lipid" },
      { "PDBnoneOfTheAbove", "not(hetero,protein,nucleic,carbohydrate)" },

      { "viewMenu","front left right top bottom back" },
      { "front", Box( "moveto 2.0 front;delay 1" ) },
      { "left", Box( "moveto 1.0 front;moveto 2.0 left;delay 1"  ) },
      { "right", Box( "moveto 1.0 front;moveto 2.0 right;delay 1"  ) },
      { "top", Box( "moveto 1.0 front;moveto 2.0 top;delay 1"  ) },
      { "bottom", Box( "moveto 1.0 front;moveto 2.0 bottom;delay 1"  ) },
      { "back", Box( "moveto 1.0 front;moveto 2.0 back;delay 1"  ) },
      {
          "renderMenu",
          "perspectiveDepthCheckbox showBoundBoxCheckbox showUnitCellCheckbox showAxes;set_axesMolecularCheckbox stereoMenu - renderSchemeMenu - atomMenu labelMenu bondMenu hbondMenu ssbondMenu - "
              + "PDBstructureMenu _AxesMenu _BoundBoxMenu _UnitCellMenu" },
      {
          "renderSchemeMenu",
          "renderCpkSpacefill renderBallAndStick "
              + "renderSticks renderWireframe renderPDBCartoonsOnly renderPDBTraceOnly" },
      { "renderCpkSpacefill", "restrict not selected;select not selected;spacefill 100%;color cpk" },
      { "renderBallAndStick", "restrict not selected;select not selected;spacefill 20%;wireframe 0.15;color cpk" },
      { "renderSticks", "restrict not selected;select not selected;wireframe 0.3;color cpk" },
      { "renderWireframe", "restrict not selected;select not selected;wireframe on;color cpk" },
      { "renderPDBCartoonsOnly", "restrict not selected;select not selected;cartoons on;color structure" },
      { "renderPDBTraceOnly", "restrict not selected;select not selected;trace on;color structure" },

      {
          "atomMenu",
          "showHydrogensCheckbox - atomNone - "
              + "atom15 atom20 atom25 atom50 atom75 atom100" },
      { "atomNone", "cpk off" },
      { "atom15", "cpk 15%" },
      { "atom20", "cpk 20%" },
      { "atom25", "cpk 25%" },
      { "atom50", "cpk 50%" },
      { "atom75", "cpk 75%" },
      { "atom100", "cpk on" },

      {
          "bondMenu",
          "bondNone bondWireframe - "
              + "bond100 bond150 bond200 bond250 bond300" },
      { "bondNone", "wireframe off" },
      { "bondWireframe", "wireframe on" },
      { "bond100", "wireframe .1" },
      { "bond150", "wireframe .15" },
      { "bond200", "wireframe .2" },
      { "bond250", "wireframe .25" },
      { "bond300", "wireframe .3" },

      {
          "hbondMenu",
          "PDBhbondCalc hbondNone hbondWireframe - "
              + "PDBhbondSidechain PDBhbondBackbone - "
              + "hbond100 hbond150 hbond200 hbond250 hbond300" },
      { "PDBhbondCalc", "hbonds calculate" },
      { "hbondNone", "hbonds off" },
      { "hbondWireframe", "hbonds on" },
      { "PDBhbondSidechain", "set hbonds sidechain" },
      { "PDBhbondBackbone", "set hbonds backbone" },
      { "hbond100", "hbonds .1" },
      { "hbond150", "hbonds .15" },
      { "hbond200", "hbonds .2" },
      { "hbond250", "hbonds .25" },
      { "hbond300", "hbonds .3" },

      {
          "ssbondMenu",
          "ssbondNone ssbondWireframe - "
              + "PDBssbondSidechain PDBssbondBackbone - "
              + "ssbond100 ssbond150 ssbond200 ssbond250 ssbond300" },
      { "ssbondNone", "ssbonds off" },
      { "ssbondWireframe", "ssbonds on" },
      { "PDBssbondSidechain", "set ssbonds sidechain" },
      { "PDBssbondBackbone", "set ssbonds backbone" },
      { "ssbond100", "ssbonds .1" },
      { "ssbond150", "ssbonds .15" },
      { "ssbond200", "ssbonds .2" },
      { "ssbond250", "ssbonds .25" },
      { "ssbond300", "ssbonds .3" },

      {
          "PDBstructureMenu",
          "structureNone - "
              + "backbone cartoon cartoonRockets ribbons rockets strands trace" },
      { "structureNone",
          "backbone off;cartoons off;ribbons off;rockets off;strands off;trace off;" },
      { "backbone", "restrict not selected;select not selected;backbone 0.3" },
      { "cartoon", "restrict not selected;select not selected;set cartoonRockets false;cartoons on" },
      { "cartoonRockets", "restrict not selected;select not selected;set cartoonRockets;cartoons on" },
      { "ribbons", "restrict not selected;select not selected;ribbons on" },
      { "rockets", "restrict not selected;select not selected;rockets on" },
      { "strands", "restrict not selected;select not selected;strands on" },
      { "trace", "restrict not selected;select not selected;trace 0.3" },

      { "VIBRATIONMenu", "vibrationOff vibrationOn VIBRATIONvectorMenu" },
      { "vibrationOff", "vibration off" },
      { "vibrationOn", "vibration on" },
      {
          "VIBRATIONvectorMenu",
          "vectorOff vectorOn vector3 vector005 vector01 - "
              + "vectorScale02 vectorScale05 vectorScale1 vectorScale2 vectorScale5" },
      { "vectorOff", "vectors off" },
      { "vectorOn", "vectors on" },
      { "vector3", "vectors 3" },
      { "vector005", "vectors 0.05" },
      { "vector01", "vectors 0.1" },
      { "vectorScale02", "vector scale 0.2" },
      { "vectorScale05", "vector scale 0.5" },
      { "vectorScale1", "vector scale 1" },
      { "vectorScale2", "vector scale 2" },
      { "vectorScale5", "vector scale 5" },

      {
          "stereoMenu",
          "stereoNone stereoRedCyan stereoRedBlue stereoRedGreen stereoCrossEyed stereoWallEyed" },
      { "stereoNone", "stereo off" },
      { "stereoRedCyan", "stereo redcyan 3" },
      { "stereoRedBlue", "stereo redblue 3" },
      { "stereoRedGreen", "stereo redgreen 3" },
      { "stereoCrossEyed", "stereo 5" },
      { "stereoWallEyed", "stereo -5" },

      {
          "labelMenu",
          "labelNone - " + "labelSymbol labelName labelNumber - "
              + "labelPositionMenu" },

      { "labelNone", "label off" },
      { "labelSymbol", "label %e" },
      { "labelName", "label %a" },
      { "labelNumber", "label %i" },

      { "labelPositionMenu",
          "labelCentered labelUpperRight labelLowerRight labelUpperLeft labelLowerLeft" },
      { "labelCentered", "set labeloffset 0 0" },
      { "labelUpperRight", "set labeloffset 4 4" },
      { "labelLowerRight", "set labeloffset 4 -4" },
      { "labelUpperLeft", "set labeloffset -4 4" },
      { "labelLowerLeft", "set labeloffset -4 -4" },

      {
          "colorMenu",
          "colorrasmolCheckbox - colorAtomsMenu colorBondsMenu colorHbondsMenu colorSSbondsMenu colorPDBstructuresMenu colorIsoSurfaceMenu"
              + " - colorLabelsMenu colorVectorsMenu - colorAxesMenu colorBoundBoxMenu colorUnitCellMenu colorBackgroundMenu" },
      {
          "colorPDBstructuresMenu",
          "colorBackboneMenu colorCartoonsMenu colorRibbonsMenu colorRocketsMenu colorStrandsMenu colorTraceMenu" },
      { "colorAtomsMenu", "@ SCHEME COLOR TRANSLUCENCY" },
      { "colorBondsMenu", "@ INHERIT_ALL COLOR TRANSLUCENCY" },
      { "colorHbondsMenu", null },
      { "colorSSbondsMenu", null },
      { "colorLabelsMenu", null },
      { "colorVectorsMenu", null },
      { "colorBackboneMenu", "@ INHERIT_ALL SCHEME COLOR TRANSLUCENCY" },
      { "colorCartoonsMenu", null },
      { "colorRibbonsMenu", null },
      { "colorRocketsMenu", null },
      { "colorStrandsMenu", null },
      { "colorTraceMenu", null },
      { "colorBackgroundMenu", "@ COLOR" },
      { "colorIsoSurfaceMenu", "@ COLOR TRANSLUCENCY" },
      { "colorAxesMenu", "@ AXESCOLOR" },
      { "colorBoundBoxMenu", null },
      { "colorUnitCellMenu", null },

      {
          "SchemeMenu",
          "cpk altloc molecule formalcharge partialCHARGE - amino#PDB structure#PDB chain#PDB " },
      {
          "zoomMenu",
          "zoom50 zoom100 zoom150 zoom200 zoom400 zoom800 - "
              + "zoomIn zoomOut" },
      { "zoom50", "zoom 50" },
      { "zoom100", "zoom 100" },
      { "zoom150", "zoom 150" },
      { "zoom200", "zoom 200" },
      { "zoom400", "zoom 400" },
      { "zoom800", "zoom 800" },
      { "zoomIn", "move 0 0 0 40 0 0 0 0 1" },
      { "zoomOut", "move 0 0 0 -40 0 0 0 0 1" },

      {
          "spinMenu",
          "spinOn spinOff - " + "setSpinXMenu setSpinYMenu setSpinZMenu - "
              + "setSpinFpsMenu" },
      { "spinOn", "spin on" },
      { "spinOff", "spin off" },

      { "setSpinXMenu", "s0 s5 s10 s20 s30 s40 s50" },
      { "setSpinYMenu", null },
      { "setSpinZMenu", null },
      { "setSpinFpsMenu", null },
      { "s0", "0" },
      { "s5", "5" },
      { "s10", "10" },
      { "s20", "20" },
      { "s30", "30" },
      { "s40", "40" },
      { "s50", "50" },

      {
          "FRAMESanimateMenu",
          "animModeMenu - play pause resume stop - nextframe prevframe rewind - playrev restart - "
              + "FRAMESanimFpsMenu" },
      { "animModeMenu", "onceThrough palindrome loop" },
      { "onceThrough", "anim mode once#" },
      { "palindrome", "anim mode palindrome#" },
      { "loop", "anim mode loop#" },
      { "play", "anim play#" },
      { "pause", "anim pause#" },
      { "resume", "anim resume#" },
      { "stop", "anim off#" },
      
      { "nextframe", "frame next#" },
      { "prevframe", "frame prev#" },
      { "playrev", "anim playrev#" },
      
      { "rewind", "anim rewind#" },
      { "restart", "anim on#" },
      
      { "FRAMESanimFpsMenu", "animfps5 animfps10 animfps20 animfps30 animfps50" },
      { "animfps5", "anim fps 5#" },
      { "animfps10", "anim fps 10#" },
      { "animfps20", "anim fps 20#" },
      { "animfps30", "anim fps 30#" },
      { "animfps50", "anim fps 50#" },

      {
          "measureMenu",
          "showMeasurementsCheckbox - "
              + "measureOff measureDistance measureAngle measureTorsion - "
              + "measureDelete measureList - distanceNanometers distanceAngstroms distancePicometers" },
      { "measureOff", "set pickingstyle MEASURE OFF; set picking OFF" },
      { "measureDistance",
          "set pickingstyle MEASURE; set picking MEASURE DISTANCE" },
      { "measureAngle", "set pickingstyle MEASURE; set picking MEASURE ANGLE" },
      { "measureTorsion",
          "set pickingstyle MEASURE; set picking MEASURE TORSION" },
      { "measureDelete", "measure delete" },
      { "measureList", "console on;show measurements" },
      { "distanceNanometers", "select *; set measure nanometers" },
      { "distanceAngstroms", "select *; set measure angstroms" },
      { "distancePicometers", "select *; set measure picometers" },

      {
          "pickingMenu",
          "pickOff pickCenter pickLabel pickAtom "
              + "pickMolecule pickElement pickPDBChain pickPDBGroup pickSYMMETRYSite pickSpin" },
      { "pickOff", "set picking off" },
      { "pickCenter", "set picking center" },
      //    { "pickDraw" , "set picking draw" },
      { "pickLabel", "set picking label" },
      { "pickAtom", "set picking atom" },
      { "pickPDBChain", "set picking chain" },
      { "pickElement", "set picking element" },
      { "pickPDBGroup", "set picking group" },
      { "pickMolecule", "set picking molecule" },
      { "pickSYMMETRYSite", "set picking site" },
      { "pickSpin", "set picking spin" },

      {
          "showMenu",
          "showHistory showFile showFileHeader - "
              + "showOrient showMeasure - "
              + "showSpacegroup showState SYMMETRYshowSymmetry showUnitCell - showIsosurface showMo - extractMOL" },
      { "showConsole", "console" },
      { "showFile", "console on;show file" },
      { "showFileHeader", "console on;getProperty FileHeader" },
      { "showHistory", "console on;show history" },
      { "showIsosurface", "console on;show isosurface" },
      { "showMeasure", "console on;show measure" },
      { "showMo", "console on;show mo" },
      { "showModel", "console on;show model" },
      { "showOrient", "console on;show orientation" },
      { "showSpacegroup", "console on;show spacegroup" },
      { "showState", "console on;show state" },
      { "SYMMETRYshowSymmetry", "console on;show symmetry" },
      { "showUnitCell", "console on;show unitcell" },
      { "extractMOL", "console on;getproperty extractModel \"visible\" " },

      {
          "surfaceMenu",
          "surfDots surfVDW surfSolventAccessible14 surfSolvent14 surfMolecular CHARGEsurfMEP surfMoComputedMenu - surfOpaque surfTranslucent surfOff" },
      { "surfDots", "dots on" },
      { "surfVDW", "isosurface delete resolution 0 solvent 0 translucent" },
      { "surfMolecular", "isosurface delete resolution 0 molecular translucent" },
      { "surfSolvent14",
          "isosurface delete resolution 0 solvent 1.4 translucent" },
      { "surfSolventAccessible14",
          "isosurface delete resolution 0 sasurface 1.4 translucent" },
      { "CHARGEsurfMEP",
          "isosurface delete resolution 0 molecular map MEP translucent" },
      { "surfOpaque", "mo opaque;isosurface opaque" },
      { "surfTranslucent", "mo translucent;isosurface translucent" },
      { "surfOff", "mo delete;isosurface delete;select *;dots off" },

      { "SYMMETRYunitCellMenu",
          "oneUnitCell nineUnitCells nineUnitCellsRestricted nineUnitCellsPoly" },

      { "oneUnitCell",
          "save orientation;load \"\" {1 1 1} ;restore orientation;center" },
      { "nineUnitCells",
          "save orientation;load \"\" {444 666 1} ;restore orientation;center" },
      {"nineUnitCellsRestricted",
          "save orientation;load \"\" {444 666 1} ;restore orientation; unitcell on; display cell=555;center visible;zoom 200" },

      {"nineUnitCellsPoly",
          "save orientation;load \"\" {444 666 1} ;restore orientation; unitcell on; display cell=555; polyhedra 4,6 (displayed);center (visible);zoom 200" },

      { "_AxesMenu", "off dotted - byPixelMenu byAngstromMenu" },
      { "_BoundBoxMenu", null },
      { "_UnitCellMenu", null },

      { "byPixelMenu", "1p 3p 5p 10p" },
      { "1p", "on" },
      { "3p", "3" },
      { "5p", "5" },
      { "10p", "10" },

      { "byAngstromMenu", "10a 20a 25a 50a 100a" },
      { "10a", "0.1" },
      { "20a", "0.20" },
      { "25a", "0.25" },
      { "50a", "0.50" },
      { "100a", "1.0" },

      //      { "optionsMenu", "rasmolChimeCompatibility" },
      {
          "rasmolChimeCompatibility",
          "set color rasmol; set zeroBasedXyzRasmol on; "
              + "set axesOrientationRasmol on; load \"\"; select *; cpk off; wireframe on; " },

      { "aboutComputedMenu", "jmolUrl mouseManualUrl translatingUrl" },
      { "jmolUrl", "http://www.jmol.org" },
      { "mouseManualUrl", "http://wiki.jmol.org/index.php/Mouse_Manual" },
      { "translatingUrl", "http://wiki.jmol.org/index.php/Internationalisation" }, };
  
  private static final String[][] wordContents = {
      { "modelSetMenu", GT._("No atoms loaded", true) },
      { "CONFIGURATIONComputedMenu", GT._("Configurations", true) },
      { "hiddenModelSetName", GT._("Model information", true) },

      { "selectMenu", GT._("Select", true) },
      { "elementsComputedMenu", GT._("Element", true) },
      { "selectAll", GT._("All", true) },
      { "selectNone", GT._("None", true) },
      { "hideNotSelected;hide(none)Checkbox", GT._("Display Selected Only", true) },
      { "invertSelection", GT._("Invert Selection", true) },

      { "viewMenu", GT._("View", true) },
      { "front", GT._("Front", true) },
      { "left", GT._("Left", true) },
      { "right", GT._("Right", true) },
      { "top", GT._("Top", true) },
      { "bottom", GT._("Bottom", true) },
      { "back", GT._("Back", true) },

      { "PDBproteinMenu", GT._("Protein", true) },
      { "allProtein", GT._("All", true) },
      { "proteinBackbone", GT._("Backbone", true) },
      { "proteinSideChains", GT._("Side Chains", true) },
      { "polar", GT._("Polar Residues", true) },
      { "nonpolar", GT._("Nonpolar Residues", true) },
      { "positiveCharge", GT._("Basic Residues (+)", true) },
      { "negativeCharge", GT._("Acidic Residues (-)", true) },
      { "noCharge", GT._("Uncharged Residues", true) },
      { "PDBaaResiduesComputedMenu", GT._("By Residue Name", true) },
      { "PDBnucleicResiduesComputedMenu", GT._("By Residue Name", true) },
      { "PDBcarboResiduesComputedMenu", GT._("By Residue Name", true) },
      { "PDBheteroComputedMenu", GT._("By HETATM", true) },
      { "PDBnucleicMenu", GT._("Nucleic", true) },
      { "allNucleic", GT._("All", true) },
      { "DNA", GT._("DNA", true) },
      { "RNA", GT._("RNA", true) },
      { "nucleicBackbone", GT._("Backbone", true) },
      { "nucleicBases", GT._("Bases", true) },
      { "atPairs", GT._("AT pairs", true) },
      { "gcPairs", GT._("GC pairs", true) },
      { "auPairs", GT._("AU pairs", true) },
      { "A", "A" },
      { "C", "C" },
      { "G", "G" },
      { "T", "T" },
      { "U", "U" },

      { "PDBheteroMenu", GT._("Hetero", true) },
      { "allHetero", GT._("All PDB \"HETATM\"", true) },
      { "Solvent", GT._("All Solvent", true) },
      { "Water", GT._("All Water", true) },
      { "nonWaterSolvent",
          GT._("Nonaqueous Solvent") + " (solvent and not water)" },
      { "exceptWater", GT._("Nonaqueous HETATM") + " (hetero and not water)" },
      { "Ligand", GT._("Ligand") + " (hetero and not solvent)" },

      { "allCarbo", GT._("All", true) },
      { "PDBcarboMenu", GT._("Carbohydrate", true) },
      { "PDBnoneOfTheAbove", GT._("None of the above", true) },

      { "FRAMESbyModelComputedMenu", GT._("Model/Frame", true) },

      { "renderMenu", GT._("Style", true) },
      { "renderSchemeMenu", GT._("Scheme", true) },
      { "renderCpkSpacefill", GT._("CPK Spacefill", true) },
      { "renderBallAndStick", GT._("Ball and Stick", true) },
      { "renderSticks", GT._("Sticks", true) },
      { "renderWireframe", GT._("Wireframe", true) },
      { "renderPDBCartoonsOnly", GT._("Cartoon", true) },
      { "renderPDBTraceOnly", GT._("Trace", true) },

      { "atomMenu", GT._("Atoms", true) },
      { "atomNone", GT._("Off", true) },
      { "atom15", GT._("{0}% van der Waals", "15", true) },
      { "atom20", GT._("{0}% van der Waals", "20", true) },
      { "atom25", GT._("{0}% van der Waals", "25", true) },
      { "atom50", GT._("{0}% van der Waals", "50", true) },
      { "atom75", GT._("{0}% van der Waals", "75", true) },
      { "atom100", GT._("{0}% van der Waals", "100", true) },

      { "bondMenu", GT._("Bonds", true) },
      { "bondNone", GT._("Off", true) },
      { "bondWireframe", GT._("On", true) },
      { "bond100", GT._("{0} \u00C5", "0.10", true) },
      { "bond150", GT._("{0} \u00C5", "0.15", true) },
      { "bond200", GT._("{0} \u00C5", "0.20", true) },
      { "bond250", GT._("{0} \u00C5", "0.25", true) },
      { "bond300", GT._("{0} \u00C5", "0.30", true) },

      { "hbondMenu", GT._("Hydrogen Bonds", true) },
      { "hbondNone", GT._("Off", true) },
      { "PDBhbondCalc", GT._("Calculate", true) },
      { "hbondWireframe", GT._("On", true) },
      { "PDBhbondSidechain", GT._("Set H-Bonds Side Chain", true) },
      { "PDBhbondBackbone", GT._("Set H-Bonds Backbone") },
      { "hbond100", GT._("{0} \u00C5", "0.10", true) },
      { "hbond150", GT._("{0} \u00C5", "0.15", true) },
      { "hbond200", GT._("{0} \u00C5", "0.20", true) },
      { "hbond250", GT._("{0} \u00C5", "0.25", true) },
      { "hbond300", GT._("{0} \u00C5", "0.30", true) },

      { "ssbondMenu", GT._("Disulfide Bonds", true) },
      { "ssbondNone", GT._("Off", true) },
      { "ssbondWireframe", GT._("On", true) },
      { "PDBssbondSidechain", GT._("Set SS-Bonds Side Chain", true) },
      { "PDBssbondBackbone", GT._("Set SS-Bonds Backbone", true) },
      { "ssbond100", GT._("{0} \u00C5", "0.10", true) },
      { "ssbond150", GT._("{0} \u00C5", "0.15", true) },
      { "ssbond200", GT._("{0} \u00C5", "0.20", true) },
      { "ssbond250", GT._("{0} \u00C5", "0.25", true) },
      { "ssbond300", GT._("{0} \u00C5", "0.30", true) },

      { "PDBstructureMenu", GT._("Structures", true) },
      { "structureNone", GT._("Off", true) },
      { "backbone", GT._("Backbone", true) },
      { "cartoon", GT._("Cartoon", true) },
      { "cartoonRockets", GT._("Cartoon Rockets", true) },
      { "ribbons", GT._("Ribbons", true) },
      { "rockets", GT._("Rockets", true) },
      { "strands", GT._("Strands", true) },
      { "trace", GT._("Trace", true) },

      { "VIBRATIONMenu", GT._("Vibration", true) },
      { "vibrationOff", GT._("Off", true) },
      { "vibrationOn", GT._("On", true) },
      { "VIBRATIONvectorMenu", GT._("Vectors", true) },
      { "vectorOff", GT._("Off", true) },
      { "vectorOn", GT._("On", true) },
      { "vector3", GT._("{0} pixels", "3", true) },
      { "vector005", GT._("{0} \u00C5", "0.05", true) },
      { "vector01", GT._("{0} \u00C5", "0.10", true) },
      { "vectorScale02", GT._("Scale {0}", "0.2", true) },
      { "vectorScale05", GT._("Scale {0}", "0.5", true) },
      { "vectorScale1", GT._("Scale {0}", "1", true) },
      { "vectorScale2", GT._("Scale {0}", "2", true) },
      { "vectorScale5", GT._("Scale {0}", "5", true) },

      { "stereoMenu", GT._("Stereographic", true) },
      { "stereoNone", GT._("None", true) },
      { "stereoRedCyan", GT._("Red+Cyan glasses", true) },
      { "stereoRedBlue", GT._("Red+Blue glasses", true) },
      { "stereoRedGreen", GT._("Red+Green glasses", true) },
      { "stereoCrossEyed", GT._("Cross-eyed viewing", true) },
      { "stereoWallEyed", GT._("Wall-eyed viewing", true) },

      { "labelMenu", GT._("Labels", true) },

      { "labelNone", GT._("None", true) },
      { "labelSymbol", GT._("With Element Symbol", true) },
      { "labelName", GT._("With Atom Name", true) },
      { "labelNumber", GT._("With Atom Number", true) },

      { "labelPositionMenu", GT._("Position Label on Atom", true) },
      { "labelCentered", GT._("Centered", true) },
      { "labelUpperRight", GT._("Upper Right", true) },
      { "labelLowerRight", GT._("Lower Right", true) },
      { "labelUpperLeft", GT._("Upper Left", true) },
      { "labelLowerLeft", GT._("Lower Left", true) },

      { "colorMenu", GT._("Color", true) },
      { "colorAtomsMenu", GT._("Atoms", true) },

      { "SchemeMenu", GT._("By Scheme", true) },
      { "cpk", GT._("Element (CPK)", true) },
      { "altloc", GT._("Alternative Location", true) },
      { "molecule", GT._("Molecule", true) },
      { "formalcharge", GT._("Formal Charge", true) },
      { "partialCHARGE", GT._("Partial Charge", true) },

      { "amino#PDB", GT._("Amino Acid", true) },
      { "structure#PDB", GT._("Secondary Structure", true) },
      { "chain#PDB", GT._("Chain", true) },

      { "none", GT._("Inherit", true) },
      { "black", GT._("Black", true) },
      { "white", GT._("White", true) },
      { "cyan", GT._("Cyan", true) },

      { "red", GT._("Red", true) },
      { "orange", GT._("Orange", true) },
      { "yellow", GT._("Yellow", true) },
      { "green", GT._("Green", true) },
      { "blue", GT._("Blue", true) },
      { "indigo", GT._("Indigo", true) },
      { "violet", GT._("Violet", true) },

      { "salmon", GT._("Salmon", true) },
      { "olive", GT._("Olive", true) },
      { "maroon", GT._("Maroon", true) },
      { "gray", GT._("Gray", true) },
      { "slateblue", GT._("Slate Blue", true) },
      { "gold", GT._("Gold", true) },
      { "orchid", GT._("Orchid", true) },

      { "opaque", GT._("Make Opaque", true) },
      { "translucent", GT._("Make Translucent", true) },

      { "colorBondsMenu", GT._("Bonds", true) },
      { "colorHbondsMenu", GT._("Hydrogen Bonds", true) },
      { "colorSSbondsMenu", GT._("Disulfide Bonds", true) },
      { "colorPDBstructuresMenu", GT._("Structure", true) },
      { "colorBackboneMenu", GT._("Backbone", true) },
      { "colorTraceMenu", GT._("Trace", true) },
      { "colorCartoonsMenu", GT._("Cartoon", true) },
      { "colorRibbonsMenu", GT._("Ribbons", true) },
      { "colorRocketsMenu", GT._("Rockets", true) },
      { "colorStrandsMenu", GT._("Strands", true) },
      { "colorLabelsMenu", GT._("Labels", true) },
      { "colorBackgroundMenu", GT._("Background", true) },
      { "colorIsoSurfaceMenu", GT._("Surface", true) },
      { "colorVectorsMenu", GT._("Vectors", true) },
      { "colorAxesMenu", GT._("Axes", true) },
      { "colorBoundBoxMenu", GT._("Boundbox", true) },
      { "colorUnitCellMenu", GT._("Unitcell", true) },

      { "zoomMenu", GT._("Zoom", true) },
      { "zoom50", "50%" },
      { "zoom100", "100%" },
      { "zoom150", "150%" },
      { "zoom200", "200%" },
      { "zoom400", "400%" },
      { "zoom800", "800%" },
      { "zoomIn", GT._("Zoom In", true) },
      { "zoomOut", GT._("Zoom Out", true) },

      { "spinMenu", GT._("Spin", true) },
      { "spinOn", GT._("On", true) },
      { "spinOff", GT._("Off", true) },

      { "setSpinXMenu", GT._("Set X Rate", true) },
      { "setSpinYMenu", GT._("Set Y Rate", true) },
      { "setSpinZMenu", GT._("Set Z Rate", true) },
      { "setSpinFpsMenu", GT._("Set FPS", true) },

      { "s0", "0" },
      { "s5", "5" },
      { "s10", "10" },
      { "s20", "20" },
      { "s30", "30" },
      { "s40", "40" },
      { "s50", "50" },

      { "FRAMESanimateMenu", GT._("Animation", true) },
      { "animModeMenu", GT._("Animation Mode", true) },
      { "onceThrough", GT._("Play Once", true) },
      { "palindrome", GT._("Palindrome", true) },
      { "loop", GT._("Loop", true) },
      
      { "play", GT._("Play", true) },
      { "pause", GT._("Pause", true) },
      { "resume", GT._("Resume", true) },
      { "stop", GT._("Stop", true) },
      { "nextframe", GT._("Next Frame", true) },
      { "prevframe", GT._("Previous Frame", true) },
      { "rewind", GT._("Rewind", true) },
      { "playrev", GT._("Reverse", true) },
      { "restart", GT._("Restart", true) },

      { "FRAMESanimFpsMenu", GT._("Set FPS", true) },
      { "animfps5", "5" },
      { "animfps10", "10" },
      { "animfps20", "20" },
      { "animfps30", "30" },
      { "animfps50", "50" },

      { "measureMenu", GT._("Measurement", true) },
      { "measureOff", GT._("Double-Click begins and ends all measurements", true) },
      { "measureDistance", GT._("Click for distance measurement", true) },
      { "measureAngle", GT._("Click for angle measurement", true) },
      { "measureTorsion", GT._("Click for torsion (dihedral) measurement", true) },
      { "measureDelete", GT._("Delete measurements", true) },
      { "measureList", GT._("List measurements", true) },
      { "distanceNanometers", GT._("Distance units nanometers", true) },
      { "distanceAngstroms", GT._("Distance units Angstroms", true) },
      { "distancePicometers", GT._("Distance units picometers", true) },

      { "pickingMenu", GT._("Set picking", true) },
      { "pickOff", GT._("Off", true) },
      { "pickCenter", GT._("Center", true) },
      //    { "pickDraw" , GT._("moves arrows", true) },
      { "pickLabel", GT._("Label", true) },
      { "pickAtom", GT._("Select atom", true) },
      { "pickPDBChain", GT._("Select chain", true) },
      { "pickElement", GT._("Select element", true) },
      { "pickPDBGroup", GT._("Select group", true) },
      { "pickMolecule", GT._("Select molecule", true) },
      { "pickSYMMETRYSite", GT._("Select site", true) },
      { "pickSpin", GT._("Spin", true) },

      { "showMenu", GT._("Show", true) },
      { "showConsole", GT._("Console", true) },
      { "showFile", GT._("File Contents", true) },
      { "showFileHeader", GT._("File Header", true) },
      { "showHistory", GT._("History", true) },
      { "showIsosurface", GT._("Isosurface JVXL data", true) },
      { "showMeasure", GT._("Measure", true) },
      { "showMo", GT._("Molecular orbital JVXL data", true) },
      { "showModel", GT._("Model", true) },
      { "showOrient", GT._("Orientation", true) },
      { "showSpacegroup", GT._("Space group", true) },
      { "SYMMETRYshowSymmetry", GT._("Symmetry", true) },
      { "showState", GT._("Current state", true) },
      { "SYMMETRYComputedMenu", GT._("Symmetry", true) },
      { "showUnitCell", GT._("Unit cell", true) },
      { "extractMOL", GT._("Extract MOL data", true) },

      { "surfaceMenu", GT._("Surfaces", true) },
      { "surfDots", GT._("Dot Surface", true) },
      { "surfVDW", GT._("van der Waals Surface", true) },
      { "surfMolecular", GT._("Molecular Surface", true) },
      { "surfSolvent14", GT._("Solvent Surface ({0}-Angstrom probe)", "1.4", true) },
      { "surfSolventAccessible14",
          GT._("Solvent-Accessible Surface (VDW + {0} Angstrom)", "1.4", true) },
      { "CHARGEsurfMEP", GT._("Molecular Electrostatic Potential", true) },
      { "surfMoComputedMenu", GT._("Molecular Orbitals", true) },
      { "surfOpaque", GT._("Make Opaque", true) },
      { "surfTranslucent", GT._("Make Translucent", true) },
      { "surfOff", GT._("Off", true) },

      { "SYMMETRYunitCellMenu", GT._("Symmetry", true) },
      { "oneUnitCell", GT._("Reload {0}", "{1 1 1}", true) },
      { "nineUnitCells", GT._("Reload {0}", "{444 666 1}", true) },
      { "nineUnitCellsRestricted", GT._("Reload {0}", "{444 666 1};display 555", true) },
      { "nineUnitCellsPoly", GT._("Reload + Polyhedra", true) },
      

      { "_AxesMenu", GT._("Axes", true) }, { "_BoundBoxMenu", GT._("Boundbox", true) },
      { "_UnitCellMenu", GT._("Unitcell", true) },

      { "off", GT._("Hide", true) }, { "dotted", GT._("Dotted", true) },

      { "byPixelMenu", GT._("Pixel Width", true) }, { "1p", GT._("{0} px", "1", true) },
      { "3p", GT._("{0} px", "3", true) }, { "5p", GT._("{0} px", "5", true) },
      { "10p", GT._("{0} px", "10", true) },

      { "byAngstromMenu", GT._("Angstrom Width", true) },
      { "10a", GT._("{0} \u00C5", "0.10", true) },
      { "20a", GT._("{0} \u00C5", "0.20", true) },
      { "25a", GT._("{0} \u00C5", "0.25", true) },
      { "50a", GT._("{0} \u00C5", "0.50", true) },
      { "100a", GT._("{0} \u00C5", "1.0", true) },

      { "optionsMenu", GT._("Compatibility", true) },
      { "showSelectionsCheckbox", GT._("Selection Halos", true) },
      { "showHydrogensCheckbox", GT._("Show Hydrogens", true) },
      { "showMeasurementsCheckbox", GT._("Show Measurements", true) },
      { "perspectiveDepthCheckbox", GT._("Perspective Depth", true) },      
      { "showBoundBoxCheckbox", GT._("Bound Box", true) },
      { "showAxes;set_axesMolecularCheckbox", GT._("Axes", true) },
      { "showUnitCellCheckbox", GT._("Unit Cell", true) },      
      { "rasmolChimeCompatibility", GT._("RasMol/Chime Settings", true) },
      { "colorrasmolCheckbox", GT._("RasMol Colors", true) },
      { "axesOrientationRasmolCheckbox", GT._("Axes RasMol/Chime", true) },
      { "zeroBasedXyzRasmolCheckbox", GT._("Zero-Based Xyz Rasmol", true) },

      { "aboutComputedMenu", GT._("About Jmol", true) },
      { "jmolUrl", "http://www.jmol.org" },
      { "mouseManualUrl", GT._("Mouse Manual", true) },
      { "translatingUrl", GT._("Translations", true) }, };

  // Initialize properties
  static {
    String previous = "";
    for (int i = 0; i < structureContents.length; i++) {
      String str = structureContents[i][1];
      if (str == null)
        str = previous;
      previous = str;
      structure.setProperty(structureContents[i][0], str);
    }
    for (int i = 0; i < wordContents.length; i++) {
      words.setProperty(wordContents[i][0], wordContents[i][1]);
    }
  }
}
