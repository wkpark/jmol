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
          "modelSetInfoMenu FRAMESbyModelComputedMenu - selectMenu colorMenu renderMenu surfaceMenu SYMMETRYunitCellMenu - "
              + "zoomMenu spinMenu VIBRATIONMenu "
              + "FRAMESanimateMenu - "
              + "measureMenu pickingMenu - showConsole showMenu - "
//              + "- optionsMenu "
              + "aboutMenu" },

      {
          "selectMenu",
          "showSelectionsCheckbox - selectAll selectNone elementsComputedMenu - "
              + "PDBproteinMenu PDBnucleicMenu PDBheteroMenu PDBcarbohydrate PDBnoneOfTheAbove - "
              + "invertSelection restrictToSelection" },
      { "selectAll", "all" },
      { "selectNone", "none" },

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
      { "positiveCharge", "protein and basic" },
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

      { "invertSelection", "not selected" },

      { "restrictToSelection", "selected; restrict selected" },

      {
          "renderMenu",
          "perspectiveDepthCheckbox stereoMenu - renderSchemeMenu - atomMenu labelMenu bondMenu hbondMenu ssbondMenu - "
              + "PDBstructureMenu setAxesMenu setBoundBoxMenu setUnitCellMenu" },
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
          "colorrasmolCheckbox - colorAtomMenu colorBondMenu colorHbondMenu colorSSbondMenu colorPDBstructuresMenu colorIsoSurfaceMenu"
              + " - colorLabelMenu colorVectorMenu - colorAxesMenu colorBoundBoxMenu colorUnitCellMenu colorBackgroundMenu" },
      {
          "colorPDBstructuresMenu",
          "colorBackboneMenu colorCartoonMenu colorRibbonsMenu colorRocketsMenu colorStrandsMenu colorTraceMenu" },
      { "colorAtomMenu", "@ SCHEME COLOR TRANSLUCENCY" },
      { "colorBondMenu", "@ INHERIT COLOR TRANSLUCENCY" },
      { "colorHbondMenu", null },
      { "colorSSbondMenu", null },
      { "colorLabelMenu", null },
      { "colorVectorMenu", null },
      { "colorBackboneMenu", "@ INHERIT SCHEME COLOR TRANSLUCENCY" },
      { "colorCartoonMenu", null },
      { "colorRibbonsMenu", null },
      { "colorRocketsMenu", null },
      { "colorStrandsMenu", null },
      { "colorTraceMenu", null },
      { "colorBackgroundMenu", "@ COLOR" },
      { "colorIsoSurfaceMenu", "@ COLOR TRANSLUCENCY" },
      { "colorAxesMenu", "@ AXESCOLOR" },
      { "colorBoundBoxMenu", null },
      { "colorUnitCellMenu", null },

      { "SchemeMenu", "cpk amino structure chain formalcharge partialcharge" },
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
          "setAnimModeMenu - " + "play stop nextframe prevframe rewind - "
              + "setAnimFpsMenu" },
      { "setAnimModeMenu", "OnceThrough Palindrome Loop" },
      { "OnceThrough", "anim mode once" },
      { "Palindrome", "anim mode palindrome" },
      { "Loop", "anim mode loop" },
      { "play", "anim on" },
      { "stop", "anim off" },
      { "nextframe", "frame next" },
      { "prevframe", "frame prev" },
      { "rewind", "frame 1" },
      { "setAnimFpsMenu", "animfps5 animfps10 animfps20 animfps30 animfps50" },
      { "animfps5", "anim fps 5" },
      { "animfps10", "anim fps 10" },
      { "animfps20", "anim fps 20" },
      { "animfps30", "anim fps 30" },
      { "animfps50", "anim fps 50" },

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

      { "showMenu", "showHistory showFile showFileHeader - "
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

      { "surfaceMenu",
          "surfDots surfVDW surfSolventAccessible14 surfSolvent14 surfMolecular surfMEP surfMoComputedMenu - surfOpaque surfTranslucent surfOff" },
      { "surfDots", "select *;dots on" },
      { "surfVDW", "isosurface delete resolution 0 solvent 0 translucent" },
      { "surfMolecular", "isosurface delete resolution 0 molecular translucent" },
      { "surfSolvent14",
          "isosurface delete resolution 0 solvent 1.4 translucent" },
      { "surfSolventAccessible14",
          "isosurface delete resolution 0 sasurface 1.4 translucent" },
      { "surfMEP",
          "isosurface delete resolution 0 molecular map MEP translucent" },
      { "surfOpaque", "isosurface opaque" },
      { "surfTranslucent", "isosurface translucent" },
      { "surfOff", "isosurface delete;select *;dots off" },

      { "SYMMETRYunitCellMenu", "oneUnitCell fourUnitCells nineUnitCells nineUnitCellsRestricted" },
      
      { "oneUnitCell", "save orientation;load \"\" {1 1 1} ;restore orientation" },
      { "fourUnitCells", "save orientation;load \"\" {2 2 2} ;restore orientation" },
      { "nineUnitCells", "save orientation;load \"\" {3 3 3} ;restore orientation" },
      { "nineUnitCellsRestricted", "save orientation;load \"\" {3 3 3} ;restore orientation;unitCell {1 1 1}; restrict cell=666; polyhedra 4,6 (visible);center visible;zoom 200" },
      
      { "setAxesMenu", "off dotted - byPixelMenu byAngstromMenu" },
      { "setBoundBoxMenu", null },
      { "setUnitCellMenu", null },

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
      { "rasmolChimeCompatibility",
          "set color rasmol; set zeroBasedXyzRasmol on; "
              + "set axesOrientationRasmol on; load \"\"; select *; cpk off; wireframe on; " },

      { "aboutMenu", "jmolUrl mouseManualUrl translatingUrl" },
      { "jmolUrl", "http://www.jmol.org" },
      { "mouseManualUrl", "http://wiki.jmol.org/index.php/Mouse_Manual" },
      { "translatingUrl", "http://wiki.jmol.org/index.php/Internationalisation" }, };

  private static final String[][] wordContents = {
      { "modelSetInfoMenu", GT._("No atoms loaded") },
      { "hiddenModelSetName", GT._("Model information") },

      { "selectMenu", GT._("Select") },
      { "elementsComputedMenu", GT._("Element") },
      { "selectAll", GT._("All") },
      { "selectNone", GT._("None") },

      { "PDBproteinMenu", GT._("Protein") },
      { "allProtein", GT._("All") },
      { "proteinBackbone", GT._("Backbone") },
      { "proteinSideChains", GT._("Side Chains") },
      { "polar", GT._("Polar Residues") },
      { "nonpolar", GT._("Nonpolar Residues") },
      { "positiveCharge", GT._("Basic Residues (+)") },
      { "negativeCharge", GT._("Acidic Residues (-)") },
      { "noCharge", GT._("Uncharged Residues") },
      { "aaresiduesComputedMenu", GT._("By Residue Name") },
      { "heteroComputedMenu", GT._("By HETATM") },
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
          GT._("Nonaqueous Solvent") + " (solvent and not water)" },
      { "exceptWater", GT._("Nonaqueous HETATM") + " (hetero and not water)" },
      { "Ligand", GT._("Ligand") + " (hetero and not solvent)" },
      { "PDBcarbohydrate", GT._("Carbohydrate") },
      { "PDBnoneOfTheAbove", GT._("None of the above") },

      { "FRAMESbyModelComputedMenu", GT._("Model/Frame") },
      { "invertSelection", GT._("Invert Selection") },

      { "restrictToSelection", GT._("Display Selected Only") },

      { "renderMenu", GT._("Style") },
      { "renderSchemeMenu", GT._("Scheme") },
      { "renderCpkSpacefill", GT._("CPK Spacefill") },
      { "renderBallAndStick", GT._("Ball and Stick") },
      { "renderSticks", GT._("Sticks") },
      { "renderWireframe", GT._("Wireframe") },
      { "renderBackbone", GT._("Backbone") },

      { "atomMenu", GT._("Atoms") },
      { "atomNone", GT._("Off") },
      { "atom15", GT._("{0}% van der Waals", new Object[] { "15" }) },
      { "atom20", GT._("{0}% van der Waals", new Object[] { "20" }) },
      { "atom25", GT._("{0}% van der Waals", new Object[] { "25" }) },
      { "atom50", GT._("{0}% van der Waals", new Object[] { "50" }) },
      { "atom75", GT._("{0}% van der Waals", new Object[] { "75" }) },
      { "atom100", GT._("{0}% van der Waals", new Object[] { "100" }) },

      { "bondMenu", GT._("Bonds") },
      { "bondNone", GT._("Off") },
      { "bondWireframe", GT._("On") },
      { "bond100", GT._("{0} \u00C5", new Object[] { "0.10" }) },
      { "bond150", GT._("{0} \u00C5", new Object[] { "0.15" }) },
      { "bond200", GT._("{0} \u00C5", new Object[] { "0.20" }) },
      { "bond250", GT._("{0} \u00C5", new Object[] { "0.25" }) },
      { "bond300", GT._("{0} \u00C5", new Object[] { "0.30" }) },

      { "hbondMenu", GT._("Hydrogen Bonds") },
      { "hbondNone", GT._("Off") },
      { "PDBhbondCalc", GT._("Calculate") },
      { "hbondWireframe", GT._("On") },
      { "PDBhbondSidechain", GT._("Set H-Bonds Side Chain") },
      { "PDBhbondBackbone", GT._("Set H-Bonds Backbone") },
      { "hbond100", GT._("{0} \u00C5", new Object[] { "0.10" }) },
      { "hbond150", GT._("{0} \u00C5", new Object[] { "0.15" }) },
      { "hbond200", GT._("{0} \u00C5", new Object[] { "0.20" }) },
      { "hbond250", GT._("{0} \u00C5", new Object[] { "0.25" }) },
      { "hbond300", GT._("{0} \u00C5", new Object[] { "0.30" }) },

      { "ssbondMenu", GT._("Disulfide Bonds") },
      { "ssbondNone", GT._("Off") },
      { "ssbondWireframe", GT._("On") },
      { "PDBssbondSidechain", GT._("Set SS-Bonds Side Chain") },
      { "PDBssbondBackbone", GT._("Set SS-Bonds Backbone") },
      { "ssbond100", GT._("{0} \u00C5", new Object[] { "0.10" }) },
      { "ssbond150", GT._("{0} \u00C5", new Object[] { "0.15" }) },
      { "ssbond200", GT._("{0} \u00C5", new Object[] { "0.20" }) },
      { "ssbond250", GT._("{0} \u00C5", new Object[] { "0.25" }) },
      { "ssbond300", GT._("{0} \u00C5", new Object[] { "0.30" }) },

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
      { "vectorMenu", GT._("Vectors") },
      { "vectorOff", GT._("Off") },
      { "vectorOn", GT._("On") },
      { "vector3", GT._("{0} pixels", new Object[] { "3" }) },
      { "vector005", GT._("{0} \u00C5", new Object[] { "0.05" }) },
      { "vector01", GT._("{0} \u00C5", new Object[] { "0.10" }) },
      { "vectorScale02", GT._("Scale {0}", new Object[] { "0.2" }) },
      { "vectorScale05", GT._("Scale {0}", new Object[] { "0.5" }) },
      { "vectorScale1", GT._("Scale {0}", new Object[] { "1" }) },
      { "vectorScale2", GT._("Scale {0}", new Object[] { "2" }) },
      { "vectorScale5", GT._("Scale {0}", new Object[] { "5" }) },

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
      { "colorAtomMenu", GT._("Atoms") },

      { "SchemeMenu", GT._("By Scheme") },
      { "cpk", GT._("Element (CPK)") },
      { "amino", GT._("Amino") },
      { "structure", GT._("Secondary Structure") },
      { "chain", GT._("Chain") },
      { "formalcharge", GT._("Formal Charge") },
      { "partialcharge", GT._("Partial Charge") },

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

      { "colorBondMenu", GT._("Bonds") },
      { "colorHbondMenu", GT._("Hydrogen Bonds") },
      { "colorSSbondMenu", GT._("Disulfide Bonds") },
      { "colorPDBstructuresMenu", GT._("Structure") },
      { "colorBackboneMenu", GT._("Backbone") },
      { "colorTraceMenu", GT._("Trace") },
      { "colorCartoonMenu", GT._("Cartoon") },
      { "colorRibbonsMenu", GT._("Ribbons") },
      { "colorRocketsMenu", GT._("Rockets") },
      { "colorStrandsMenu", GT._("Strands") },
      { "colorLabelMenu", GT._("Labels") },
      { "colorBackgroundMenu", GT._("Background") },
      { "colorIsoSurfaceMenu", GT._("Surface") },
      { "colorVectorMenu", GT._("Vectors") },
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

      { "FRAMESanimateMenu", GT._("Animate") },
      { "setAnimModeMenu", GT._("Animation Mode") },
      { "OnceThrough", GT._("Play Once") },
      { "Palindrome", GT._("Palindrome") },
      { "Loop", GT._("Loop") },
      { "play", GT._("Play") },
      { "stop", GT._("Stop") },
      { "nextframe", GT._("Next Frame") },
      { "prevframe", GT._("Previous Frame") },
      { "rewind", GT._("Rewind") },
      { "revplay", GT._("Reverse Play") },
      { "setAnimFpsMenu", GT._("Set FPS") },
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
      { "pickChain", GT._("Select chain") },
      { "pickElement", GT._("Select element") },
      { "pickGroup", GT._("Select group") },
      { "pickMolecule", GT._("Select molecule") },
      { "pickSite", GT._("Select site") },
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
      { "showUnitCell", GT._("Unit cell") },
      { "extractMOL", GT._("Extract MOL data") },

      { "surfaceMenu", GT._("Surfaces") },
      { "surfDots", GT._("Dot Surface") },
      { "surfVDW", GT._("van der Waals Surface") },
      { "surfMolecular", GT._("Molecular Surface") },
      { "surfSolvent14",
          GT._("Solvent Surface ({0}-Angstrom probe)", new Object[] { "1.4" }) },
      {
          "surfSolventAccessible14",
          GT._("Solvent-Accessible Surface (VDW + {0} Angstrom)",
              new Object[] { "1.4" }) },
      { "surfMEP", GT._("Molecular Electrostatic Potential") },
      { "surfMoComputedMenu", GT._("Molecular Orbitals") },
      { "surfOpaque", GT._("Make Opaque") },
      { "surfTranslucent", GT._("Make Translucent") },
      { "surfOff", GT._("Off") },

      { "SYMMETRYunitCellMenu", GT._("Symmetry") },
      { "oneUnitCell", "{1 1 1}" },
      { "fourUnitCells", "{2 2 2}" },
      { "nineUnitCells", "{3 3 3}" },
      { "nineUnitCellsRestricted", GT._("Polyhedra") },

      { "setAxesMenu", GT._("Axes") }, { "setBoundBoxMenu", GT._("Boundbox") },
      { "setUnitCellMenu", GT._("Unitcell") },

      { "off", GT._("Hide") }, { "dotted", GT._("Dotted") },

      { "byPixelMenu", GT._("Pixel Width") },
      { "1p", GT._("{0} px", new Object[] { "1" }) },
      { "3p", GT._("{0} px", new Object[] { "3" }) },
      { "5p", GT._("{0} px", new Object[] { "5" }) },
      { "10p", GT._("{0} px", new Object[] { "10" }) },

      { "byAngstromMenu", GT._("Angstrom Width") },
      { "10a", GT._("{0} \u00C5", new Object[] { "0.10" }) },
      { "20a", GT._("{0} \u00C5", new Object[] { "0.20" }) },
      { "25a", GT._("{0} \u00C5", new Object[] { "0.25" }) },
      { "50a", GT._("{0} \u00C5", new Object[] { "0.50" }) },
      { "100a", GT._("{0} \u00C5", new Object[] { "1.0" }) },

      { "optionsMenu", GT._("Compatibility") },
      { "showSelectionsCheckbox", GT._("Selection Halos") },
      { "showHydrogensCheckbox", GT._("Show Hydrogens") },
      { "showMeasurementsCheckbox", GT._("Show Measurements") },
      { "perspectiveDepthCheckbox", GT._("Perspective Depth") },
      { "rasmolChimeCompatibility", GT._("RasMol/Chime Settings") },
      { "colorrasmolCheckbox", GT._("RasMol Colors") },
      { "axesOrientationRasmolCheckbox", GT._("Axes RasMol/Chime") },
      { "zeroBasedXyzRasmolCheckbox", GT._("Zero-Based Xyz Rasmol") },

      { "aboutMenu", GT._("About Jmol") }, { "jmolUrl", "www.jmol.org" },
      { "mouseManualUrl", GT._("Mouse Manual") },
      { "translatingUrl", GT._("Translations") }, };

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
