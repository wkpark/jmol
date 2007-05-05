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

class PopupResourceBundle {

  
  PopupResourceBundle() {
    resetMenu();
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
  
  /*
   * OK, a slight of hand here:
   * 
   * GT._() a method in the popup package that does nothing. 
   * 
   */
  private static final String[][] structureContents = {
      {
          "popupMenu",
          "modelSetMenu FRAMESbyModelComputedMenu CONFIGURATIONComputedMenu - selectMenu viewMenu renderMenu colorMenu - surfaceMenu SYMMETRYunitCellMenu - "
              + "zoomMenu spinMenu VIBRATIONMenu "
              + "FRAMESanimateMenu - "
              + "measureMenu pickingMenu - showConsole showMenu - "
              + "languageComputedMenu "
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
      { "positiveCharge", "protein and basic" },
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
    { "modelSetMenu", GT._("No atoms loaded") },
    { "CONFIGURATIONComputedMenu", GT._("Configurations") },
    { "hiddenModelSetName", GT._("Model information") },

    { "selectMenu", GT._("Select") },
    { "elementsComputedMenu", GT._("Element") },
    { "selectAll", GT._("All") },
    { "selectNone", GT._("None") },
    { "hideNotSelected;hide(none)Checkbox", GT._("Display Selected Only") },
    { "invertSelection", GT._("Invert Selection") },

    { "viewMenu", GT._("View") },
    { "front", GT._("Front") },
    { "left", GT._("Left") },
    { "right", GT._("Right") },
    { "top", GT._("Top") },
    { "bottom", GT._("Bottom") },
    { "back", GT._("Back") },

    { "PDBproteinMenu", GT._("Protein") },
    { "allProtein", GT._("All") },
    { "proteinBackbone", GT._("Backbone") },
    { "proteinSideChains", GT._("Side Chains") },
    { "polar", GT._("Polar Residues") },
    { "nonpolar", GT._("Nonpolar Residues") },
    { "positiveCharge", GT._("Basic Residues (+)") },
    { "negativeCharge", GT._("Acidic Residues (-)") },
    { "noCharge", GT._("Uncharged Residues") },
    { "PDBaaResiduesComputedMenu", GT._("By Residue Name") },
    { "PDBnucleicResiduesComputedMenu", GT._("By Residue Name") },
    { "PDBcarboResiduesComputedMenu", GT._("By Residue Name") },
    { "PDBheteroComputedMenu", GT._("By HETATM") },
    { "PDBnucleicMenu", GT._("Nucleic") },
    { "allNucleic", GT._("All") },
    { "DNA", GT._("DNA") },
    { "RNA", GT._("RNA") },
    { "nucleicBackbone", GT._("Backbone") },
    { "nucleicBases", GT._("Bases") },
    { "atPairs", GT._("AT pairs") },
    { "gcPairs", GT._("GC pairs") },
    { "auPairs", GT._("AU pairs") },
    { "A", "A" },
    { "C", "C" },
    { "G", "G" },
    { "T", "T" },
    { "U", "U" },

    { "PDBheteroMenu", GT._("Hetero") },
    { "allHetero", GT._("All PDB \"HETATM\"") },
    { "Solvent", GT._("All Solvent") },
    { "Water", GT._("All Water") },
    { "nonWaterSolvent",
        GT._("Nonaqueous Solvent", "(solvent and not water)") },
    { "exceptWater", GT._("Nonaqueous HETATM", "(hetero and not water)") },
    { "Ligand", GT._("Ligand", "(hetero and not solvent)") },

    { "allCarbo", GT._("All") },
    { "PDBcarboMenu", GT._("Carbohydrate") },
    { "PDBnoneOfTheAbove", GT._("None of the above") },

    { "FRAMESbyModelComputedMenu", GT._("Model/Frame") },

    { "renderMenu", GT._("Style") },
    { "renderSchemeMenu", GT._("Scheme") },
    { "renderCpkSpacefill", GT._("CPK Spacefill") },
    { "renderBallAndStick", GT._("Ball and Stick") },
    { "renderSticks", GT._("Sticks") },
    { "renderWireframe", GT._("Wireframe") },
    { "renderPDBCartoonsOnly", GT._("Cartoon") },
    { "renderPDBTraceOnly", GT._("Trace") },

    { "atomMenu", GT._("Atoms") },
    { "atomNone", GT._("Off") },
    { "atom15", GT._("{0}% van der Waals", "15") },
    { "atom20", GT._("{0}% van der Waals", "20") },
    { "atom25", GT._("{0}% van der Waals", "25") },
    { "atom50", GT._("{0}% van der Waals", "50") },
    { "atom75", GT._("{0}% van der Waals", "75") },
    { "atom100", GT._("{0}% van der Waals", "100") },

    { "bondMenu", GT._("Bonds") },
    { "bondNone", GT._("Off") },
    { "bondWireframe", GT._("On") },
    { "bond100", GT._("{0} \u00C5", "0.10") },
    { "bond150", GT._("{0} \u00C5", "0.15") },
    { "bond200", GT._("{0} \u00C5", "0.20") },
    { "bond250", GT._("{0} \u00C5", "0.25") },
    { "bond300", GT._("{0} \u00C5", "0.30") },

    { "hbondMenu", GT._("Hydrogen Bonds") },
    { "hbondNone", GT._("Off") },
    { "PDBhbondCalc", GT._("Calculate") },
    { "hbondWireframe", GT._("On") },
    { "PDBhbondSidechain", GT._("Set H-Bonds Side Chain") },
    { "PDBhbondBackbone", GT._("Set H-Bonds Backbone") },
    { "hbond100", GT._("{0} \u00C5", "0.10") },
    { "hbond150", GT._("{0} \u00C5", "0.15") },
    { "hbond200", GT._("{0} \u00C5", "0.20") },
    { "hbond250", GT._("{0} \u00C5", "0.25") },
    { "hbond300", GT._("{0} \u00C5", "0.30") },

    { "ssbondMenu", GT._("Disulfide Bonds") },
    { "ssbondNone", GT._("Off") },
    { "ssbondWireframe", GT._("On") },
    { "PDBssbondSidechain", GT._("Set SS-Bonds Side Chain") },
    { "PDBssbondBackbone", GT._("Set SS-Bonds Backbone") },
    { "ssbond100", GT._("{0} \u00C5", "0.10") },
    { "ssbond150", GT._("{0} \u00C5", "0.15") },
    { "ssbond200", GT._("{0} \u00C5", "0.20") },
    { "ssbond250", GT._("{0} \u00C5", "0.25") },
    { "ssbond300", GT._("{0} \u00C5", "0.30") },

    { "PDBstructureMenu", GT._("Structures") },
    { "structureNone", GT._("Off") },
    { "backbone", GT._("Backbone") },
    { "cartoon", GT._("Cartoon") },
    { "cartoonRockets", GT._("Cartoon Rockets") },
    { "ribbons", GT._("Ribbons") },
    { "rockets", GT._("Rockets") },
    { "strands", GT._("Strands") },
    { "trace", GT._("Trace") },

    { "VIBRATIONMenu", GT._("Vibration") },
    { "vibrationOff", GT._("Off") },
    { "vibrationOn", GT._("On") },
    { "VIBRATIONvectorMenu", GT._("Vectors") },
    { "vectorOff", GT._("Off") },
    { "vectorOn", GT._("On") },
    { "vector3", GT._("{0} pixels", "3") },
    { "vector005", GT._("{0} \u00C5", "0.05") },
    { "vector01", GT._("{0} \u00C5", "0.10") },
    { "vectorScale02", GT._("Scale {0}", "0.2") },
    { "vectorScale05", GT._("Scale {0}", "0.5") },
    { "vectorScale1", GT._("Scale {0}", "1") },
    { "vectorScale2", GT._("Scale {0}", "2") },
    { "vectorScale5", GT._("Scale {0}", "5") },

    { "stereoMenu", GT._("Stereographic") },
    { "stereoNone", GT._("None") },
    { "stereoRedCyan", GT._("Red+Cyan glasses") },
    { "stereoRedBlue", GT._("Red+Blue glasses") },
    { "stereoRedGreen", GT._("Red+Green glasses") },
    { "stereoCrossEyed", GT._("Cross-eyed viewing") },
    { "stereoWallEyed", GT._("Wall-eyed viewing") },

    { "labelMenu", GT._("Labels") },

    { "labelNone", GT._("None") },
    { "labelSymbol", GT._("With Element Symbol") },
    { "labelName", GT._("With Atom Name") },
    { "labelNumber", GT._("With Atom Number") },

    { "labelPositionMenu", GT._("Position Label on Atom") },
    { "labelCentered", GT._("Centered") },
    { "labelUpperRight", GT._("Upper Right") },
    { "labelLowerRight", GT._("Lower Right") },
    { "labelUpperLeft", GT._("Upper Left") },
    { "labelLowerLeft", GT._("Lower Left") },

    { "colorMenu", GT._("Color") },
    { "colorAtomsMenu", GT._("Atoms") },

    { "SchemeMenu", GT._("By Scheme") },
    { "cpk", GT._("Element (CPK)") },
    { "altloc", GT._("Alternative Location") },
    { "molecule", GT._("Molecule") },
    { "formalcharge", GT._("Formal Charge") },
    { "partialCHARGE", GT._("Partial Charge") },

    { "amino#PDB", GT._("Amino Acid") },
    { "structure#PDB", GT._("Secondary Structure") },
    { "chain#PDB", GT._("Chain") },

    { "none", GT._("Inherit") },
    { "black", GT._("Black") },
    { "white", GT._("White") },
    { "cyan", GT._("Cyan") },

    { "red", GT._("Red") },
    { "orange", GT._("Orange") },
    { "yellow", GT._("Yellow") },
    { "green", GT._("Green") },
    { "blue", GT._("Blue") },
    { "indigo", GT._("Indigo") },
    { "violet", GT._("Violet") },

    { "salmon", GT._("Salmon") },
    { "olive", GT._("Olive") },
    { "maroon", GT._("Maroon") },
    { "gray", GT._("Gray") },
    { "slateblue", GT._("Slate Blue") },
    { "gold", GT._("Gold") },
    { "orchid", GT._("Orchid") },

    { "opaque", GT._("Make Opaque") },
    { "translucent", GT._("Make Translucent") },

    { "colorBondsMenu", GT._("Bonds") },
    { "colorHbondsMenu", GT._("Hydrogen Bonds") },
    { "colorSSbondsMenu", GT._("Disulfide Bonds") },
    { "colorPDBstructuresMenu", GT._("Structure") },
    { "colorBackboneMenu", GT._("Backbone") },
    { "colorTraceMenu", GT._("Trace") },
    { "colorCartoonsMenu", GT._("Cartoon") },
    { "colorRibbonsMenu", GT._("Ribbons") },
    { "colorRocketsMenu", GT._("Rockets") },
    { "colorStrandsMenu", GT._("Strands") },
    { "colorLabelsMenu", GT._("Labels") },
    { "colorBackgroundMenu", GT._("Background") },
    { "colorIsoSurfaceMenu", GT._("Surface") },
    { "colorVectorsMenu", GT._("Vectors") },
    { "colorAxesMenu", GT._("Axes") },
    { "colorBoundBoxMenu", GT._("Boundbox") },
    { "colorUnitCellMenu", GT._("Unitcell") },

    { "zoomMenu", GT._("Zoom") },
    { "zoom50", "50%" },
    { "zoom100", "100%" },
    { "zoom150", "150%" },
    { "zoom200", "200%" },
    { "zoom400", "400%" },
    { "zoom800", "800%" },
    { "zoomIn", GT._("Zoom In") },
    { "zoomOut", GT._("Zoom Out") },

    { "spinMenu", GT._("Spin") },
    { "spinOn", GT._("On") },
    { "spinOff", GT._("Off") },

    { "setSpinXMenu", GT._("Set X Rate") },
    { "setSpinYMenu", GT._("Set Y Rate") },
    { "setSpinZMenu", GT._("Set Z Rate") },
    { "setSpinFpsMenu", GT._("Set FPS") },

    { "s0", "0" },
    { "s5", "5" },
    { "s10", "10" },
    { "s20", "20" },
    { "s30", "30" },
    { "s40", "40" },
    { "s50", "50" },

    { "FRAMESanimateMenu", GT._("Animation") },
    { "animModeMenu", GT._("Animation Mode") },
    { "onceThrough", GT._("Play Once") },
    { "palindrome", GT._("Palindrome") },
    { "loop", GT._("Loop") },
    
    { "play", GT._("Play") },
    { "pause", GT._("Pause") },
    { "resume", GT._("Resume") },
    { "stop", GT._("Stop") },
    { "nextframe", GT._("Next Frame") },
    { "prevframe", GT._("Previous Frame") },
    { "rewind", GT._("Rewind") },
    { "playrev", GT._("Reverse") },
    { "restart", GT._("Restart") },

    { "FRAMESanimFpsMenu", GT._("Set FPS") },
    { "animfps5", "5" },
    { "animfps10", "10" },
    { "animfps20", "20" },
    { "animfps30", "30" },
    { "animfps50", "50" },

    { "measureMenu", GT._("Measurement") },
    { "measureOff", GT._("Double-Click begins and ends all measurements") },
    { "measureDistance", GT._("Click for distance measurement") },
    { "measureAngle", GT._("Click for angle measurement") },
    { "measureTorsion", GT._("Click for torsion (dihedral) measurement") },
    { "measureDelete", GT._("Delete measurements") },
    { "measureList", GT._("List measurements") },
    { "distanceNanometers", GT._("Distance units nanometers") },
    { "distanceAngstroms", GT._("Distance units Angstroms") },
    { "distancePicometers", GT._("Distance units picometers") },

    { "pickingMenu", GT._("Set picking") },
    { "pickOff", GT._("Off") },
    { "pickCenter", GT._("Center") },
    //    { "pickDraw" , GT._("moves arrows") },
    { "pickLabel", GT._("Label") },
    { "pickAtom", GT._("Select atom") },
    { "pickPDBChain", GT._("Select chain") },
    { "pickElement", GT._("Select element") },
    { "pickPDBGroup", GT._("Select group") },
    { "pickMolecule", GT._("Select molecule") },
    { "pickSYMMETRYSite", GT._("Select site") },
    { "pickSpin", GT._("Spin") },

    { "showMenu", GT._("Show") },
    { "showConsole", GT._("Console") },
    { "showFile", GT._("File Contents") },
    { "showFileHeader", GT._("File Header") },
    { "showHistory", GT._("History") },
    { "showIsosurface", GT._("Isosurface JVXL data") },
    { "showMeasure", GT._("Measure") },
    { "showMo", GT._("Molecular orbital JVXL data") },
    { "showModel", GT._("Model") },
    { "showOrient", GT._("Orientation") },
    { "showSpacegroup", GT._("Space group") },
    { "SYMMETRYshowSymmetry", GT._("Symmetry") },
    { "showState", GT._("Current state") },
    { "SYMMETRYComputedMenu", GT._("Symmetry") },
    { "showUnitCell", GT._("Unit cell") },
    { "extractMOL", GT._("Extract MOL data") },

    { "surfaceMenu", GT._("Surfaces") },
    { "surfDots", GT._("Dot Surface") },
    { "surfVDW", GT._("van der Waals Surface") },
    { "surfMolecular", GT._("Molecular Surface") },
    { "surfSolvent14", GT._("Solvent Surface ({0}-Angstrom probe)", "1.4") },
    { "surfSolventAccessible14",
        GT._("Solvent-Accessible Surface (VDW + {0} Angstrom)", "1.4") },
    { "CHARGEsurfMEP", GT._("Molecular Electrostatic Potential") },
    { "surfMoComputedMenu", GT._("Molecular Orbitals") },
    { "surfOpaque", GT._("Make Opaque") },
    { "surfTranslucent", GT._("Make Translucent") },
    { "surfOff", GT._("Off") },

    { "SYMMETRYunitCellMenu", GT._("Symmetry") },
    { "oneUnitCell", GT._("Reload {0}", "{1 1 1}") },
    { "nineUnitCells", GT._("Reload {0}", "{444 666 1}") },
    { "nineUnitCellsRestricted", GT._("Reload {0}", "{444 666 1};display 555") },
    { "nineUnitCellsPoly", GT._("Reload + Polyhedra") },
    

    { "_AxesMenu", GT._("Axes") }, { "_BoundBoxMenu", GT._("Boundbox") },
    { "_UnitCellMenu", GT._("Unitcell") },

    { "off", GT._("Hide") }, { "dotted", GT._("Dotted") },

    { "byPixelMenu", GT._("Pixel Width") }, { "1p", GT._("{0} px", "1") },
    { "3p", GT._("{0} px", "3") }, { "5p", GT._("{0} px", "5") },
    { "10p", GT._("{0} px", "10") },

    { "byAngstromMenu", GT._("Angstrom Width") },
    { "10a", GT._("{0} \u00C5", "0.10") },
    { "20a", GT._("{0} \u00C5", "0.20") },
    { "25a", GT._("{0} \u00C5", "0.25") },
    { "50a", GT._("{0} \u00C5", "0.50") },
    { "100a", GT._("{0} \u00C5", "1.0") },

    { "optionsMenu", GT._("Compatibility") },
    { "showSelectionsCheckbox", GT._("Selection Halos") },
    { "showHydrogensCheckbox", GT._("Show Hydrogens") },
    { "showMeasurementsCheckbox", GT._("Show Measurements") },
    { "perspectiveDepthCheckbox", GT._("Perspective Depth") },      
    { "showBoundBoxCheckbox", GT._("Bound Box") },
    { "showAxes;set_axesMolecularCheckbox", GT._("Axes") },
    { "showUnitCellCheckbox", GT._("Unit Cell") },      
    { "rasmolChimeCompatibility", GT._("RasMol/Chime Settings") },
    { "colorrasmolCheckbox", GT._("RasMol Colors") },
    { "axesOrientationRasmolCheckbox", GT._("Axes RasMol/Chime") },
    { "zeroBasedXyzRasmolCheckbox", GT._("Zero-Based Xyz Rasmol") },

    { "languageComputedMenu", GT._("Language") },
    { "aboutComputedMenu", GT._("About Jmol") },
    { "jmolUrl", "http://www.jmol.org" },
    { "mouseManualUrl", GT._("Mouse Manual") },
    { "translatingUrl", GT._("Translations") }, };

  public static void resetMenu() {
    for (int i = 0; i < wordContents.length; i++) {
      String[] info = wordContents[i];
      String data = info[1];
      boolean doReplace = (data.indexOf("{") >= 0);
      int ipt = data.indexOf("|");
      if (data.charAt(0) == '_') {
        data = org.jmol.i18n.GT._(data.substring(1), true);
      } else if (ipt >= 0) {
        String trailer = data.substring(ipt + 1);
        data = data.substring(0, ipt);
        if (doReplace)
          data = org.jmol.i18n.GT._(data, trailer, true);
        else
          data = org.jmol.i18n.GT._(data,true) + trailer;
      }
      words.setProperty(info[0], data);
    }
  }
  
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
/*    for (int i = 0; i < wordContents.length; i++) {
      words.setProperty(wordContents[i][0], wordContents[i][1]);
    }
*/
  }  
  
}
