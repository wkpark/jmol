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

import org.jmol.util.Logger;
import org.jmol.i18n.GT;

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
      { "modelSetMenu", "No atoms loaded", null },
      { "CONFIGURATIONComputedMenu", "Configurations", null },
      { "hiddenModelSetName", "Model information", null },

      { "selectMenu", "Select", null },
      { "elementsComputedMenu", "Element", null },
      { "selectAll", "All", null },
      { "selectNone", "None", null },
      { "hideNotSelected;hide(none)Checkbox", "Display Selected Only", null },
      { "invertSelection", "Invert Selection", null },

      { "viewMenu", "View", null },
      { "front", "Front", null },
      { "left", "Left", null },
      { "right", "Right", null },
      { "top", "Top", null },
      { "bottom", "Bottom", null },
      { "back", "Back", null },

      { "PDBproteinMenu", "Protein", null },
      { "allProtein", "All", null },
      { "proteinBackbone", "Backbone", null },
      { "proteinSideChains", "Side Chains", null },
      { "polar", "Polar Residues", null },
      { "nonpolar", "Nonpolar Residues", null },
      { "positiveCharge", "Basic Residues (+)", null },
      { "negativeCharge", "Acidic Residues (-)", null },
      { "noCharge", "Uncharged Residues", null },
      { "PDBaaResiduesComputedMenu", "By Residue Name", null },
      { "PDBnucleicResiduesComputedMenu", "By Residue Name", null },
      { "PDBcarboResiduesComputedMenu", "By Residue Name", null },
      { "PDBheteroComputedMenu", "By HETATM", null },
      { "PDBnucleicMenu", "Nucleic", null },
      { "allNucleic", "All", null },
      { "DNA", "DNA", null },
      { "RNA", "RNA", null },
      { "nucleicBackbone", "Backbone", null },
      { "nucleicBases", "Bases", null },
      { "atPairs", "AT pairs", null },
      { "gcPairs", "GC pairs", null },
      { "auPairs", "AU pairs", null },
      { "A", "A" },
      { "C", "C" },
      { "G", "G" },
      { "T", "T" },
      { "U", "U" },

      { "PDBheteroMenu", "Hetero", null },
      { "allHetero", "All PDB \"HETATM\"", null },
      { "Solvent", "All Solvent", null },
      { "Water", "All Water", null },
      { "nonWaterSolvent", "Nonaqueous Solvent", null, " (solvent and not water)" },
      { "exceptWater", "Nonaqueous HETATM", null, " (hetero and not water)" },
      { "Ligand", "Ligand", null, " (hetero and not solvent)" },

      { "allCarbo", "All", null },
      { "PDBcarboMenu", "Carbohydrate", null },
      { "PDBnoneOfTheAbove", "None of the above", null },

      { "FRAMESbyModelComputedMenu", "Model/Frame", null },

      { "renderMenu", "Style", null },
      { "renderSchemeMenu", "Scheme", null },
      { "renderCpkSpacefill", "CPK Spacefill", null },
      { "renderBallAndStick", "Ball and Stick", null },
      { "renderSticks", "Sticks", null },
      { "renderWireframe", "Wireframe", null },
      { "renderPDBCartoonsOnly", "Cartoon", null },
      { "renderPDBTraceOnly", "Trace", null },

      { "atomMenu", "Atoms", null },
      { "atomNone", "Off", null },
      { "atom15", "{0}% van der Waals", "15" },
      { "atom20", "{0}% van der Waals", "20" },
      { "atom25", "{0}% van der Waals", "25" },
      { "atom50", "{0}% van der Waals", "50" },
      { "atom75", "{0}% van der Waals", "75" },
      { "atom100", "{0}% van der Waals", "100" },

      { "bondMenu", "Bonds", null },
      { "bondNone", "Off", null },
      { "bondWireframe", "On", null },
      { "bond100", "{0} \u00C5", "0.10" },
      { "bond150", "{0} \u00C5", "0.15" },
      { "bond200", "{0} \u00C5", "0.20" },
      { "bond250", "{0} \u00C5", "0.25" },
      { "bond300", "{0} \u00C5", "0.30" },

      { "hbondMenu", "Hydrogen Bonds", null },
      { "hbondNone", "Off", null },
      { "PDBhbondCalc", "Calculate", null },
      { "hbondWireframe", "On", null },
      { "PDBhbondSidechain", "Set H-Bonds Side Chain", null },
      { "PDBhbondBackbone", "Set H-Bonds Backbone", null },
      { "hbond100", "{0} \u00C5", "0.10" },
      { "hbond150", "{0} \u00C5", "0.15" },
      { "hbond200", "{0} \u00C5", "0.20" },
      { "hbond250", "{0} \u00C5", "0.25" },
      { "hbond300", "{0} \u00C5", "0.30" },

      { "ssbondMenu", "Disulfide Bonds", null },
      { "ssbondNone", "Off", null },
      { "ssbondWireframe", "On", null },
      { "PDBssbondSidechain", "Set SS-Bonds Side Chain", null },
      { "PDBssbondBackbone", "Set SS-Bonds Backbone", null },
      { "ssbond100", "{0} \u00C5", "0.10" },
      { "ssbond150", "{0} \u00C5", "0.15" },
      { "ssbond200", "{0} \u00C5", "0.20" },
      { "ssbond250", "{0} \u00C5", "0.25" },
      { "ssbond300", "{0} \u00C5", "0.30" },

      { "PDBstructureMenu", "Structures", null },
      { "structureNone", "Off", null },
      { "backbone", "Backbone", null },
      { "cartoon", "Cartoon", null },
      { "cartoonRockets", "Cartoon Rockets", null },
      { "ribbons", "Ribbons", null },
      { "rockets", "Rockets", null },
      { "strands", "Strands", null },
      { "trace", "Trace", null },

      { "VIBRATIONMenu", "Vibration", null },
      { "vibrationOff", "Off", null },
      { "vibrationOn", "On", null },
      { "VIBRATIONvectorMenu", "Vectors", null },
      { "vectorOff", "Off", null },
      { "vectorOn", "On", null },
      { "vector3", "{0} pixels", "3" },
      { "vector005", "{0} \u00C5", "0.05" },
      { "vector01", "{0} \u00C5", "0.10" },
      { "vectorScale02", "Scale {0}", "0.2" },
      { "vectorScale05", "Scale {0}", "0.5" },
      { "vectorScale1", "Scale {0}", "1" },
      { "vectorScale2", "Scale {0}", "2" },
      { "vectorScale5", "Scale {0}", "5" },

      { "stereoMenu", "Stereographic", null },
      { "stereoNone", "None", null },
      { "stereoRedCyan", "Red+Cyan glasses", null },
      { "stereoRedBlue", "Red+Blue glasses", null },
      { "stereoRedGreen", "Red+Green glasses", null },
      { "stereoCrossEyed", "Cross-eyed viewing", null },
      { "stereoWallEyed", "Wall-eyed viewing", null },

      { "labelMenu", "Labels", null },

      { "labelNone", "None", null },
      { "labelSymbol", "With Element Symbol", null },
      { "labelName", "With Atom Name", null },
      { "labelNumber", "With Atom Number", null },

      { "labelPositionMenu", "Position Label on Atom", null },
      { "labelCentered", "Centered", null },
      { "labelUpperRight", "Upper Right", null },
      { "labelLowerRight", "Lower Right", null },
      { "labelUpperLeft", "Upper Left", null },
      { "labelLowerLeft", "Lower Left", null },

      { "colorMenu", "Color", null },
      { "colorAtomsMenu", "Atoms", null },

      { "SchemeMenu", "By Scheme", null },
      { "cpk", "Element (CPK)", null },
      { "altloc", "Alternative Location", null },
      { "molecule", "Molecule", null },
      { "formalcharge", "Formal Charge", null },
      { "partialCHARGE", "Partial Charge", null },

      { "amino#PDB", "Amino Acid", null },
      { "structure#PDB", "Secondary Structure", null },
      { "chain#PDB", "Chain", null },

      { "none", "Inherit", null },
      { "black", "Black", null },
      { "white", "White", null },
      { "cyan", "Cyan", null },

      { "red", "Red", null },
      { "orange", "Orange", null },
      { "yellow", "Yellow", null },
      { "green", "Green", null },
      { "blue", "Blue", null },
      { "indigo", "Indigo", null },
      { "violet", "Violet", null },

      { "salmon", "Salmon", null },
      { "olive", "Olive", null },
      { "maroon", "Maroon", null },
      { "gray", "Gray", null },
      { "slateblue", "Slate Blue", null },
      { "gold", "Gold", null },
      { "orchid", "Orchid", null },

      { "opaque", "Make Opaque", null },
      { "translucent", "Make Translucent", null },

      { "colorBondsMenu", "Bonds", null },
      { "colorHbondsMenu", "Hydrogen Bonds", null },
      { "colorSSbondsMenu", "Disulfide Bonds", null },
      { "colorPDBstructuresMenu", "Structure", null },
      { "colorBackboneMenu", "Backbone", null },
      { "colorTraceMenu", "Trace", null },
      { "colorCartoonsMenu", "Cartoon", null },
      { "colorRibbonsMenu", "Ribbons", null },
      { "colorRocketsMenu", "Rockets", null },
      { "colorStrandsMenu", "Strands", null },
      { "colorLabelsMenu", "Labels", null },
      { "colorBackgroundMenu", "Background", null },
      { "colorIsoSurfaceMenu", "Surface", null },
      { "colorVectorsMenu", "Vectors", null },
      { "colorAxesMenu", "Axes", null },
      { "colorBoundBoxMenu", "Boundbox", null },
      { "colorUnitCellMenu", "Unitcell", null },

      { "zoomMenu", "Zoom", null },
      { "zoom50", "50%" },
      { "zoom100", "100%" },
      { "zoom150", "150%" },
      { "zoom200", "200%" },
      { "zoom400", "400%" },
      { "zoom800", "800%" },
      { "zoomIn", "Zoom In", null },
      { "zoomOut", "Zoom Out", null },

      { "spinMenu", "Spin", null },
      { "spinOn", "On", null },
      { "spinOff", "Off", null },

      { "setSpinXMenu", "Set X Rate", null },
      { "setSpinYMenu", "Set Y Rate", null },
      { "setSpinZMenu", "Set Z Rate", null },
      { "setSpinFpsMenu", "Set FPS", null },

      { "s0", "0" },
      { "s5", "5" },
      { "s10", "10" },
      { "s20", "20" },
      { "s30", "30" },
      { "s40", "40" },
      { "s50", "50" },

      { "FRAMESanimateMenu", "Animation", null },
      { "animModeMenu", "Animation Mode", null },
      { "onceThrough", "Play Once", null },
      { "palindrome", "Palindrome", null },
      { "loop", "Loop", null },
      
      { "play", "Play", null },
      { "pause", "Pause", null },
      { "resume", "Resume", null },
      { "stop", "Stop", null },
      { "nextframe", "Next Frame", null },
      { "prevframe", "Previous Frame", null },
      { "rewind", "Rewind", null },
      { "playrev", "Reverse", null },
      { "restart", "Restart", null },

      { "FRAMESanimFpsMenu", "Set FPS", null },
      { "animfps5", "5" },
      { "animfps10", "10" },
      { "animfps20", "20" },
      { "animfps30", "30" },
      { "animfps50", "50" },

      { "measureMenu", "Measurement", null },
      { "measureOff", "Double-Click begins and ends all measurements", null },
      { "measureDistance", "Click for distance measurement", null },
      { "measureAngle", "Click for angle measurement", null },
      { "measureTorsion", "Click for torsion (dihedral) measurement", null },
      { "measureDelete", "Delete measurements", null },
      { "measureList", "List measurements", null },
      { "distanceNanometers", "Distance units nanometers", null },
      { "distanceAngstroms", "Distance units Angstroms", null },
      { "distancePicometers", "Distance units picometers", null },

      { "pickingMenu", "Set picking", null },
      { "pickOff", "Off", null },
      { "pickCenter", "Center", null },
      //    { "pickDraw" , "moves arrows", null },
      { "pickLabel", "Label", null },
      { "pickAtom", "Select atom", null },
      { "pickPDBChain", "Select chain", null },
      { "pickElement", "Select element", null },
      { "pickPDBGroup", "Select group", null },
      { "pickMolecule", "Select molecule", null },
      { "pickSYMMETRYSite", "Select site", null },
      { "pickSpin", "Spin", null },

      { "showMenu", "Show", null },
      { "showConsole", "Console", null },
      { "showFile", "File Contents", null },
      { "showFileHeader", "File Header", null },
      { "showHistory", "History", null },
      { "showIsosurface", "Isosurface JVXL data", null },
      { "showMeasure", "Measure", null },
      { "showMo", "Molecular orbital JVXL data", null },
      { "showModel", "Model", null },
      { "showOrient", "Orientation", null },
      { "showSpacegroup", "Space group", null },
      { "SYMMETRYshowSymmetry", "Symmetry", null },
      { "showState", "Current state", null },
      { "SYMMETRYComputedMenu", "Symmetry", null },
      { "showUnitCell", "Unit cell", null },
      { "extractMOL", "Extract MOL data", null },

      { "surfaceMenu", "Surfaces", null },
      { "surfDots", "Dot Surface", null },
      { "surfVDW", "van der Waals Surface", null },
      { "surfMolecular", "Molecular Surface", null },
      { "surfSolvent14", "Solvent Surface ({0}-Angstrom probe)", "1.4"},
      { "surfSolventAccessible14",
          "Solvent-Accessible Surface (VDW + {0} Angstrom)", "1.4"},
      { "CHARGEsurfMEP", "Molecular Electrostatic Potential", null },
      { "surfMoComputedMenu", "Molecular Orbitals", null },
      { "surfOpaque", "Make Opaque", null },
      { "surfTranslucent", "Make Translucent", null },
      { "surfOff", "Off", null },

      { "SYMMETRYunitCellMenu", "Symmetry", null },
      { "oneUnitCell", "Reload {0}", "{1 1 1}" },
      { "nineUnitCells", "Reload {0}", "{444 666 1}" },
      { "nineUnitCellsRestricted", "Reload {0}", "{444 666 1};display 555" },
      { "nineUnitCellsPoly", "Reload + Polyhedra", null },
      

      { "_AxesMenu", "Axes", null }, { "_BoundBoxMenu", "Boundbox", null },
      { "_UnitCellMenu", "Unitcell", null },

      { "off", "Hide", null }, { "dotted", "Dotted", null },

      { "byPixelMenu", "Pixel Width", null }, 
      { "1p", "{0} px", "1" },
      { "3p", "{0} px", "3" }, 
      { "5p", "{0} px", "5" },
      { "10p", "{0} px", "10" },

      { "byAngstromMenu", "Angstrom Width", null },
      { "10a", "{0} \u00C5", "0.10" },
      { "20a", "{0} \u00C5", "0.20" },
      { "25a", "{0} \u00C5", "0.25" },
      { "50a", "{0} \u00C5", "0.50" },
      { "100a", "{0} \u00C5", "1.0" },

      { "optionsMenu", "Compatibility", null },
      { "showSelectionsCheckbox", "Selection Halos", null },
      { "showHydrogensCheckbox", "Show Hydrogens", null },
      { "showMeasurementsCheckbox", "Show Measurements", null },
      { "perspectiveDepthCheckbox", "Perspective Depth", null },      
      { "showBoundBoxCheckbox", "Bound Box", null },
      { "showAxes;set_axesMolecularCheckbox", "Axes", null },
      { "showUnitCellCheckbox", "Unit Cell", null },      
      { "rasmolChimeCompatibility", "RasMol/Chime Settings", null },
      { "colorrasmolCheckbox", "RasMol Colors", null },
      { "axesOrientationRasmolCheckbox", "Axes RasMol/Chime", null },
      { "zeroBasedXyzRasmolCheckbox", "Zero-Based Xyz Rasmol", null },

      { "languageComputedMenu", "Language", null },
      { "aboutComputedMenu", "About Jmol", null },
      { "jmolUrl", "http://www.jmol.org" },
      { "mouseManualUrl", "Mouse Manual", null },
      { "translatingUrl", "Translations", null }, };

  public static void resetMenu() {
    for (int i = 0; i < wordContents.length; i++) {
      String[] info = wordContents[i];
      String data;
      switch (info.length) {
      case 2:
        data = info[1];
        break;
      case 3:
        data = (info[2] == null ? GT._(info[1]) : GT._(info[1], info[2]));
        break;
      case 4:
        data = (info[2] == null ? GT._(info[1]) : GT._(info[1], info[2]))
            + info[3];
        break;
      default:
        Logger.error("Error in menu for " + info[0]);
        data = info[0];
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
*/  }  
}
