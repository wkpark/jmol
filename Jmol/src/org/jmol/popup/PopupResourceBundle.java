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

  private static final String[][] structureContents = {
      {
          "popupMenu",
          "modelSetComputedMenu - selectMenu colorMenu renderMenu surfaceMenu SYMMETRYunitCellMenu - "
              + "zoomMenu spinMenu VIBRATIONMenu "
              + "FRAMESanimateMenu - "
              + "measureMenu pickingMenu - showConsole showMenu - "
              //              + "- optionsMenu "
              + "aboutComputedMenu" },

      {
          "selectMenu",
          "hideNotSelected;hide(none)Checkbox showSelectionsCheckbox - selectAll selectNone invertSelection - elementsComputedMenu - "
              + "PDBproteinMenu PDBnucleicMenu PDBheteroMenu PDBcarbohydrate PDBnoneOfTheAbove" },
      { "selectAll", "all" },
      { "selectNone", "none" },
      { "invertSelection", "not selected" },
      { "hideNotSelected;hide(none)Checkbox", "selected; hide not selected" },

      {
          "PDBproteinMenu",
          "allProtein proteinBackbone proteinSideChains - "
              + "polar nonpolar - "
              + "positiveCharge negativeCharge noCharge - "
              + "aaresiduesComputedMenu" },
      { "allProtein", "protein" },
      { "proteinBackbone", "protein and backbone" },
      { "proteinSideChains", "protein and not backbone" },
      { "polar", "protein and polar" },
      { "nonpolar", "protein and not polar" },
      { "positive ", "protein and basic" },
      { "negativeCharge", "protein and acidic" },
      { "noCharge", "protein and not (acidic,basic)" },

      {
          "PDBnucleicMenu",
          "allNucleic nucleicBackbone nucleicBases - " + "DNA RNA - "
              + "A C G T U - " + "atPairs auPairs gcPairs" },
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
          "heteroComputedMenu - allHetero Solvent Water - "
              + "Ligand exceptWater nonWaterSolvent" },
      { "allHetero", "hetero" },
      { "Solvent", "solvent" },
      { "Water", "water" },
      // same as ligand    { "exceptSolvent", "hetero and not solvent" },
      { "nonWaterSolvent", "solvent and not water" },
      { "exceptWater", "hetero and not water" },
      { "Ligand", "ligand" },

      { "PDBcarbohydrate", "carbohydrate" },
      // not implemented    { "Lipid", "lipid" },
      { "PDBnoneOfTheAbove", "not(hetero,protein,nucleic,carbohydrate)" },

      {
          "renderMenu",
          "perspectiveDepthCheckbox stereoMenu - renderSchemeMenu - atomMenu labelMenu bondMenu hbondMenu ssbondMenu - "
              + "PDBstructureMenu _AxesMenu _BoundBoxMenu _UnitCellMenu" },
      {
          "renderSchemeMenu",
          "renderCpkSpacefill renderBallAndStick "
              + "renderSticks renderWireframe" },
      { "renderCpkSpacefill", "backbone off;wireframe off;spacefill 100%" },
      { "renderBallAndStick", "backbone off;spacefill 20%;wireframe 0.15" },
      { "renderSticks", "backbone off;spacefill off;wireframe 0.3" },
      { "renderWireframe", "backbone off;spacefill off;wireframe on" },
      { "renderBackbone", "spacefill off;wireframe off;backbone on" },

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
      { "backbone", "backbone 0.3" },
      { "cartoon", "set cartoonRockets false;cartoons on" },
      { "cartoonRockets", "set cartoonRockets;cartoons on" },
      { "ribbons", "ribbons on" },
      { "rockets", "rockets on" },
      { "strands", "strands on" },
      { "trace", "trace 0.3" },

      { "VIBRATIONMenu", "vibrationOff vibrationOn vectorMenu" },
      { "vibrationOff", "vibration off" },
      { "vibrationOn", "vibration on" },
      {
          "vectorMenu",
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
      { "colorBondsMenu", "@ INHERIT COLOR TRANSLUCENCY" },
      { "colorHbondsMenu", null },
      { "colorSSbondsMenu", null },
      { "colorLabelsMenu", null },
      { "colorVectorsMenu", null },
      { "colorBackboneMenu", "@ INHERIT SCHEME COLOR TRANSLUCENCY" },
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
              + "animFpsMenu" },
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
      
      { "animFpsMenu", "animfps5 animfps10 animfps20 animfps30 animfps50" },
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
          "pickOff pickCenter pickLabel pickAtom pickChain "
              + "pickElement pickGroup pickMolecule pickSite pickSpin" },
      { "pickOff", "set picking off" },
      { "pickCenter", "set picking center" },
      //    { "pickDraw" , "set picking draw" },
      { "pickLabel", "set picking label" },
      { "pickAtom", "set picking atom" },
      { "pickChain", "set picking chain" },
      { "pickElement", "set picking element" },
      { "pickGroup", "set picking group" },
      { "pickMolecule", "set picking molecule" },
      { "pickSite", "set picking site" },
      { "pickSpin", "set picking spin" },

      {
          "showMenu",
          "showHistory showFile showFileHeader - "
              + "showOrient showMeasure - "
              + "showSpacegroup SYMMETRYshowSymmetry showUnitCell - showIsosurface showMo - extractMOL" },
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
      { "surfOpaque", "isosurface opaque" },
      { "surfTranslucent", "isosurface translucent" },
      { "surfOff", "isosurface delete;select *;dots off" },

      { "SYMMETRYunitCellMenu",
          "oneUnitCell fourUnitCells nineUnitCells nineUnitCellsRestricted" },

      { "oneUnitCell",
          "save orientation;load \"\" {1 1 1} ;restore orientation;center" },
      { "fourUnitCells",
          "save orientation;load \"\" {2 2 2} ;restore orientation;center" },
      { "nineUnitCells",
          "save orientation;load \"\" {3 3 3} ;restore orientation;center" },
      {
          "nineUnitCellsRestricted",
          "save orientation;load \"\" {3 3 3} ;restore orientation;unitCell {1 1 1}; restrict cell=666; polyhedra 4,6 (visible);center visible;zoom 200" },

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
      { "modelSetComputedMenu", GT.T("No atoms loaded") },
      { "CONFIGURATIONComputedMenu", GT.T("Configurations") },
      { "hiddenModelSetName", GT.T("Model information") },

      { "selectMenu", GT.T("Select") },
      { "elementsComputedMenu", GT.T("Element") },
      { "selectAll", GT.T("All") },
      { "selectNone", GT.T("None") },
      { "hideNotSelected;hide(none)Checkbox", GT.T("Display Selected Only") },
      { "invertSelection", GT.T("Invert Selection") },

      { "PDBproteinMenu", GT.T("Protein") },
      { "allProtein", GT.T("All") },
      { "proteinBackbone", GT.T("Backbone") },
      { "proteinSideChains", GT.T("Side Chains") },
      { "polar", GT.T("Polar Residues") },
      { "nonpolar", GT.T("Nonpolar Residues") },
      { "positiveCharge", GT.T("Basic Residues (+)") },
      { "negativeCharge", GT.T("Acidic Residues (-)") },
      { "noCharge", GT.T("Uncharged Residues") },
      { "aaresiduesComputedMenu", GT.T("By Residue Name") },
      { "heteroComputedMenu", GT.T("By HETATM") },
      { "PDBnucleicMenu", GT.T("Nucleic") },
      { "allNucleic", GT.T("All") },
      { "DNA", GT.T("DNA") },
      { "RNA", GT.T("RNA") },
      { "nucleicBackbone", GT.T("Backbone") },
      { "nucleicBases", GT.T("Bases") },
      { "atPairs", GT.T("AT pairs") },
      { "gcPairs", GT.T("GC pairs") },
      { "auPairs", GT.T("AU pairs") },
      { "A", "A" },
      { "C", "C" },
      { "G", "G" },
      { "T", "T" },
      { "U", "U" },

      { "PDBheteroMenu", GT.T("Hetero") },
      { "allHetero", GT.T("All PDB \"HETATM\"") },
      { "Solvent", GT.T("All Solvent") },
      { "Water", GT.T("All Water") },
      { "nonWaterSolvent",
          GT.T("Nonaqueous Solvent") + " (solvent and not water)" },
      { "exceptWater", GT.T("Nonaqueous HETATM") + " (hetero and not water)" },
      { "Ligand", GT.T("Ligand") + " (hetero and not solvent)" },
      { "PDBcarbohydrate", GT.T("Carbohydrate") },
      { "PDBnoneOfTheAbove", GT.T("None of the above") },

      { "FRAMESbyModelComputedMenu", GT.T("Model/Frame") },

      { "renderMenu", GT.T("Style") },
      { "renderSchemeMenu", GT.T("Scheme") },
      { "renderCpkSpacefill", GT.T("CPK Spacefill") },
      { "renderBallAndStick", GT.T("Ball and Stick") },
      { "renderSticks", GT.T("Sticks") },
      { "renderWireframe", GT.T("Wireframe") },
      { "renderBackbone", GT.T("Backbone") },

      { "atomMenu", GT.T("Atoms") },
      { "atomNone", GT.T("Off") },
      { "atom15", GT.T("{0}% van der Waals", "15") },
      { "atom20", GT.T("{0}% van der Waals", "20") },
      { "atom25", GT.T("{0}% van der Waals", "25") },
      { "atom50", GT.T("{0}% van der Waals", "50") },
      { "atom75", GT.T("{0}% van der Waals", "75") },
      { "atom100", GT.T("{0}% van der Waals", "100") },

      { "bondMenu", GT.T("Bonds") },
      { "bondNone", GT.T("Off") },
      { "bondWireframe", GT.T("On") },
      { "bond100", GT.T("{0} \u00C5", "0.10") },
      { "bond150", GT.T("{0} \u00C5", "0.15") },
      { "bond200", GT.T("{0} \u00C5", "0.20") },
      { "bond250", GT.T("{0} \u00C5", "0.25") },
      { "bond300", GT.T("{0} \u00C5", "0.30") },

      { "hbondMenu", GT.T("Hydrogen Bonds") },
      { "hbondNone", GT.T("Off") },
      { "PDBhbondCalc", GT.T("Calculate") },
      { "hbondWireframe", GT.T("On") },
      { "PDBhbondSidechain", GT.T("Set H-Bonds Side Chain") },
      { "PDBhbondBackbone", GT.T("Set H-Bonds Backbone") },
      { "hbond100", GT.T("{0} \u00C5", "0.10") },
      { "hbond150", GT.T("{0} \u00C5", "0.15") },
      { "hbond200", GT.T("{0} \u00C5", "0.20") },
      { "hbond250", GT.T("{0} \u00C5", "0.25") },
      { "hbond300", GT.T("{0} \u00C5", "0.30") },

      { "ssbondMenu", GT.T("Disulfide Bonds") },
      { "ssbondNone", GT.T("Off") },
      { "ssbondWireframe", GT.T("On") },
      { "PDBssbondSidechain", GT.T("Set SS-Bonds Side Chain") },
      { "PDBssbondBackbone", GT.T("Set SS-Bonds Backbone") },
      { "ssbond100", GT.T("{0} \u00C5", "0.10") },
      { "ssbond150", GT.T("{0} \u00C5", "0.15") },
      { "ssbond200", GT.T("{0} \u00C5", "0.20") },
      { "ssbond250", GT.T("{0} \u00C5", "0.25") },
      { "ssbond300", GT.T("{0} \u00C5", "0.30") },

      { "PDBstructureMenu", GT.T("Structures") },
      { "structureNone", GT.T("Off") },
      { "backbone", GT.T("Backbone") },
      { "cartoon", GT.T("Cartoon") },
      { "cartoonRockets", GT.T("Cartoon Rockets") },
      { "ribbons", GT.T("Ribbons") },
      { "rockets", GT.T("Rockets") },
      { "strands", GT.T("Strands") },
      { "trace", GT.T("Trace") },

      { "VIBRATIONMenu", GT.T("Vibration") },
      { "vibrationOff", GT.T("Off") },
      { "vibrationOn", GT.T("On") },
      { "vectorMenu", GT.T("Vectors") },
      { "vectorOff", GT.T("Off") },
      { "vectorOn", GT.T("On") },
      { "vector3", GT.T("{0} pixels", "3") },
      { "vector005", GT.T("{0} \u00C5", "0.05") },
      { "vector01", GT.T("{0} \u00C5", "0.10") },
      { "vectorScale02", GT.T("Scale {0}", "0.2") },
      { "vectorScale05", GT.T("Scale {0}", "0.5") },
      { "vectorScale1", GT.T("Scale {0}", "1") },
      { "vectorScale2", GT.T("Scale {0}", "2") },
      { "vectorScale5", GT.T("Scale {0}", "5") },

      { "stereoMenu", GT.T("Stereographic") },
      { "stereoNone", GT.T("None") },
      { "stereoRedCyan", GT.T("Red+Cyan glasses") },
      { "stereoRedBlue", GT.T("Red+Blue glasses") },
      { "stereoRedGreen", GT.T("Red+Green glasses") },
      { "stereoCrossEyed", GT.T("Cross-eyed viewing") },
      { "stereoWallEyed", GT.T("Wall-eyed viewing") },

      { "labelMenu", GT.T("Labels") },

      { "labelNone", GT.T("None") },
      { "labelSymbol", GT.T("With Element Symbol") },
      { "labelName", GT.T("With Atom Name") },
      { "labelNumber", GT.T("With Atom Number") },

      { "labelPositionMenu", GT.T("Position Label on Atom") },
      { "labelCentered", GT.T("Centered") },
      { "labelUpperRight", GT.T("Upper Right") },
      { "labelLowerRight", GT.T("Lower Right") },
      { "labelUpperLeft", GT.T("Upper Left") },
      { "labelLowerLeft", GT.T("Lower Left") },

      { "colorMenu", GT.T("Color") },
      { "colorAtomsMenu", GT.T("Atoms") },

      { "SchemeMenu", GT.T("By Scheme") },
      { "cpk", GT.T("Element (CPK)") },
      { "altloc", GT.T("Alternative Location") },
      { "molecule", GT.T("Molecule") },
      { "formalcharge", GT.T("Formal Charge") },
      { "partialCHARGE", GT.T("Partial Charge") },

      { "amino#PDB", GT.T("Amino Acid") },
      { "structure#PDB", GT.T("Secondary Structure") },
      { "chain#PDB", GT.T("Chain") },

      { "none", GT.T("Inherit") },
      { "black", GT.T("Black") },
      { "white", GT.T("White") },
      { "cyan", GT.T("Cyan") },

      { "red", GT.T("Red") },
      { "orange", GT.T("Orange") },
      { "yellow", GT.T("Yellow") },
      { "green", GT.T("Green") },
      { "blue", GT.T("Blue") },
      { "indigo", GT.T("Indigo") },
      { "violet", GT.T("Violet") },

      { "salmon", GT.T("Salmon") },
      { "olive", GT.T("Olive") },
      { "maroon", GT.T("Maroon") },
      { "gray", GT.T("Gray") },
      { "slateblue", GT.T("Slate Blue") },
      { "gold", GT.T("Gold") },
      { "orchid", GT.T("Orchid") },

      { "opaque", GT.T("Make Opaque") },
      { "translucent", GT.T("Make Translucent") },

      { "colorBondsMenu", GT.T("Bonds") },
      { "colorHbondsMenu", GT.T("Hydrogen Bonds") },
      { "colorSSbondsMenu", GT.T("Disulfide Bonds") },
      { "colorPDBstructuresMenu", GT.T("Structure") },
      { "colorBackboneMenu", GT.T("Backbone") },
      { "colorTraceMenu", GT.T("Trace") },
      { "colorCartoonsMenu", GT.T("Cartoon") },
      { "colorRibbonsMenu", GT.T("Ribbons") },
      { "colorRocketsMenu", GT.T("Rockets") },
      { "colorStrandsMenu", GT.T("Strands") },
      { "colorLabelsMenu", GT.T("Labels") },
      { "colorBackgroundMenu", GT.T("Background") },
      { "colorIsoSurfaceMenu", GT.T("Surface") },
      { "colorVectorsMenu", GT.T("Vectors") },
      { "colorAxesMenu", GT.T("Axes") },
      { "colorBoundBoxMenu", GT.T("Boundbox") },
      { "colorUnitCellMenu", GT.T("Unitcell") },

      { "zoomMenu", GT.T("Zoom") },
      { "zoom50", "50%" },
      { "zoom100", "100%" },
      { "zoom150", "150%" },
      { "zoom200", "200%" },
      { "zoom400", "400%" },
      { "zoom800", "800%" },
      { "zoomIn", GT.T("Zoom In") },
      { "zoomOut", GT.T("Zoom Out") },

      { "spinMenu", GT.T("Spin") },
      { "spinOn", GT.T("On") },
      { "spinOff", GT.T("Off") },

      { "setSpinXMenu", GT.T("Set X Rate") },
      { "setSpinYMenu", GT.T("Set Y Rate") },
      { "setSpinZMenu", GT.T("Set Z Rate") },
      { "setSpinFpsMenu", GT.T("Set FPS") },

      { "s0", "0" },
      { "s5", "5" },
      { "s10", "10" },
      { "s20", "20" },
      { "s30", "30" },
      { "s40", "40" },
      { "s50", "50" },

      { "FRAMESanimateMenu", GT.T("Animate") },
      { "animModeMenu", GT.T("Animation Mode") },
      { "onceThrough", GT.T("Play Once") },
      { "palindrome", GT.T("Palindrome") },
      { "loop", GT.T("Loop") },
      
      { "play", GT.T("Play") },
      { "pause", GT.T("Pause") },
      { "resume", GT.T("Resume") },
      { "stop", GT.T("Stop") },
      { "nextframe", GT.T("Next Frame") },
      { "prevframe", GT.T("Previous Frame") },
      { "rewind", GT.T("Rewind") },

      { "playrev", GT.T("Reverse") },
      { "animOn", GT.T("Restart") },

      { "animFpsMenu", GT.T("Set FPS") },
      { "animfps5", "5" },
      { "animfps10", "10" },
      { "animfps20", "20" },
      { "animfps30", "30" },
      { "animfps50", "50" },

      { "measureMenu", GT.T("Measurement") },
      { "measureOff", GT.T("Double-Click begins and ends all measurements") },
      { "measureDistance", GT.T("Click for distance measurement") },
      { "measureAngle", GT.T("Click for angle measurement") },
      { "measureTorsion", GT.T("Click for torsion (dihedral) measurement") },
      { "measureDelete", GT.T("Delete measurements") },
      { "measureList", GT.T("List measurements") },
      { "distanceNanometers", GT.T("Distance units nanometers") },
      { "distanceAngstroms", GT.T("Distance units Angstroms") },
      { "distancePicometers", GT.T("Distance units picometers") },

      { "pickingMenu", GT.T("Set picking") },
      { "pickOff", GT.T("Off") },
      { "pickCenter", GT.T("Center") },
      //    { "pickDraw" , GT.T("moves arrows") },
      { "pickLabel", GT.T("Label") },
      { "pickAtom", GT.T("Select atom") },
      { "pickChain", GT.T("Select chain") },
      { "pickElement", GT.T("Select element") },
      { "pickGroup", GT.T("Select group") },
      { "pickMolecule", GT.T("Select molecule") },
      { "pickSite", GT.T("Select site") },
      { "pickSpin", GT.T("Spin") },

      { "showMenu", GT.T("Show") },
      { "showConsole", GT.T("Console") },
      { "showFile", GT.T("File Contents") },
      { "showFileHeader", GT.T("File Header") },
      { "showHistory", GT.T("History") },
      { "showIsosurface", GT.T("Isosurface JVXL data") },
      { "showMeasure", GT.T("Measure") },
      { "showMo", GT.T("Molecular orbital JVXL data") },
      { "showModel", GT.T("Model") },
      { "showOrient", GT.T("Orientation") },
      { "showSpacegroup", GT.T("Space group") },
      { "SYMMETRYshowSymmetry", GT.T("Symmetry") },
      { "showUnitCell", GT.T("Unit cell") },
      { "extractMOL", GT.T("Extract MOL data") },

      { "surfaceMenu", GT.T("Surfaces") },
      { "surfDots", GT.T("Dot Surface") },
      { "surfVDW", GT.T("van der Waals Surface") },
      { "surfMolecular", GT.T("Molecular Surface") },
      { "surfSolvent14", GT.T("Solvent Surface ({0}-Angstrom probe)", "1.4") },
      { "surfSolventAccessible14",
          GT.T("Solvent-Accessible Surface (VDW + {0} Angstrom)", "1.4") },
      { "CHARGEsurfMEP", GT.T("Molecular Electrostatic Potential") },
      { "surfMoComputedMenu", GT.T("Molecular Orbitals") },
      { "surfOpaque", GT.T("Make Opaque") },
      { "surfTranslucent", GT.T("Make Translucent") },
      { "surfOff", GT.T("Off") },

      { "SYMMETRYunitCellMenu", GT.T("Symmetry") },
      { "oneUnitCell", GT.T("Reload {0}", "{1 1 1}") },
      { "fourUnitCells", GT.T("Reload {0}", "{2 2 2}") },
      { "nineUnitCells", GT.T("Reload {0}", "{3 3 3}") },
      { "nineUnitCellsRestricted", GT.T("Reload {0}", "Polyhedra") },

      { "_AxesMenu", GT.T("Axes") }, { "_BoundBoxMenu", GT.T("Boundbox") },
      { "_UnitCellMenu", GT.T("Unitcell") },

      { "off", GT.T("Hide") }, { "dotted", GT.T("Dotted") },

      { "byPixelMenu", GT.T("Pixel Width") }, { "1p", GT.T("{0} px", "1") },
      { "3p", GT.T("{0} px", "3") }, { "5p", GT.T("{0} px", "5") },
      { "10p", GT.T("{0} px", "10") },

      { "byAngstromMenu", GT.T("Angstrom Width") },
      { "10a", GT.T("{0} \u00C5", "0.10") },
      { "20a", GT.T("{0} \u00C5", "0.20") },
      { "25a", GT.T("{0} \u00C5", "0.25") },
      { "50a", GT.T("{0} \u00C5", "0.50") },
      { "100a", GT.T("{0} \u00C5", "1.0") },

      { "optionsMenu", GT.T("Compatibility") },
      { "showSelectionsCheckbox", GT.T("Selection Halos") },
      { "showHydrogensCheckbox", GT.T("Show Hydrogens") },
      { "showMeasurementsCheckbox", GT.T("Show Measurements") },
      { "perspectiveDepthCheckbox", GT.T("Perspective Depth") },
      { "rasmolChimeCompatibility", GT.T("RasMol/Chime Settings") },
      { "colorrasmolCheckbox", GT.T("RasMol Colors") },
      { "axesOrientationRasmolCheckbox", GT.T("Axes RasMol/Chime") },
      { "zeroBasedXyzRasmolCheckbox", GT.T("Zero-Based Xyz Rasmol") },

      { "aboutComputedMenu", GT.T("About Jmol") },
      { "jmolUrl", "http://www.jmol.org" },
      { "mouseManualUrl", GT.T("Mouse Manual") },
      { "translatingUrl", GT.T("Translations") }, };

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
