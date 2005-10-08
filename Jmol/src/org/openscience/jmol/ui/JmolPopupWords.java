/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.openscience.jmol.ui;

import java.util.Properties;

import org.jmol.util.GT;

/**
 * Class containing the structure of the popup menu
 */
public class JmolPopupWords {

  /**
   * @param key Structure element
   * @return Detail of the structure element
   */
  public static String getString(String key) {
    return properties.getProperty(key);
  }
  
  private final static Properties properties = new Properties();
  
  static final String[][] contents = {
    { "modelSetInfoMenu", GT._("No atoms loaded") },
    { "hiddenModelSetName", GT._("Name hidden") },

    { "selectMenu", GT._("Select") },
    { "elementsComputedMenu", GT._("Element") },
    { "selectAll", GT._("All") },
    { "selectNone", GT._("None") },

    { "proteinMenu", GT._("Protein") },
    { "allProtein", GT._("All") },
    { "proteinBackbone", GT._("Backbone") },
    { "proteinSideChains", GT._("Side Chains") },
    { "polar", GT._("Polar Residues") },
    { "nonpolar", GT._("Nonpolar Residues") },
    { "positiveCharge", GT._("Basic Residues (+)") },
    { "negativeCharge", GT._("Acidic Residues (-)") },
    { "noCharge", GT._("Uncharged Residues") },
    { "aaresiduesComputedMenu", GT._("By Residue Name") },

    { "nucleicMenu", GT._("Nucleic") },
    { "allNucleic", GT._("All") },
    { "DNA", GT._("DNA") },
    { "RNA", GT._("ARN") },
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

    { "heteroMenu", GT._("hetero") },
    { "allHetero", GT._("All") },
    { "Solvent", GT._("Solvent") },
    { "Water", GT._("Water") },
    { "exceptSolvent", GT._("Except Solvent") },
    { "exceptWater", GT._("Except Water") },
    { "Ligand", GT._("Ligand") },

    { "otherMenu", GT._("Other") },
    { "Carbohydrate", GT._("Carbohydrate") },
    { "Lipid", GT._("Lipid") },
    { "Other", GT._("Other") },

    { "byModelMenu", GT._("Model") },
    { "allModels", GT._("All Models") },

    { "byFrameMenu", GT._("Frame") },
    { "allFrames", GT._("All Frames") },

    { "invertSelection", GT._("Invert Selection") },

    { "restrictToSelection", GT._("Display Selected Only") },

    { "setSelectModeMenu", GT._("Set Select Mode") },
    { "replace", GT._("Replace Selection") },
    { "add", GT._("Add to Selection (OR)") },
    { "narrow", GT._("Narrow Selection (AND)") },

    { "renderMenu", GT._("Render") },
    { "renderSchemeMenu", GT._("Scheme") },
    { "renderCpkSpacefill", GT._("CPK Spacefill") },
    { "renderBallAndStick", GT._("Ball and Stick") },
    { "renderSticks", GT._("Sticks") },
    { "renderWireframe", GT._("Wireframe") },
    { "renderBackbone", GT._("Backbone") },

    { "atomMenu", GT._("Atoms") },
    { "atomNone", GT._("Off") },
    { "atom15", GT._("{0}% vanderWaals", new Object[]{ "15" }) },
    { "atom20", GT._("{0}% vanderWaals", new Object[]{ "20" }) },
    { "atom25", GT._("{0}% vanderWaals", new Object[]{ "25" }) },
    { "atom50", GT._("{0}% vanderWaals", new Object[]{ "50" }) },
    { "atom75", GT._("{0}% vanderWaals", new Object[]{ "75" }) },
    { "atom100", GT._("{0}% vanderWaals", new Object[]{ "100" }) },

    { "bondMenu", GT._("Bonds") },
    { "bondNone", GT._("Off") },
    { "bondWireframe", GT._("On") },
    { "bond100", "0.10 \u00C5" },
    { "bond150", "0.15 \u00C5" },
    { "bond200", "0.20 \u00C5" },
    { "bond250", "0.25 \u00C5" },
    { "bond300", "0.30 \u00C5" },

    { "hbondMenu", GT._("Hydrogen Bonds") },
    { "hbondNone", GT._("Off") },
    { "hbondWireframe", GT._("On") },
    { "hbondSidechain", GT._("Set H-Bonds Side Chain") },
    { "hbondBackbone", GT._("Set H-Bonds Backbone") },
    { "hbond100", "0.10 \u00C5" },
    { "hbond150", "0.15 \u00C5" },
    { "hbond200", "0.20 \u00C5" },
    { "hbond250", "0.25 \u00C5" },
    { "hbond300", "0.30 \u00C5" },

    { "ssbondMenu", GT._("Disulfide Bonds") },
    { "ssbondNone", GT._("Off") },
    { "ssbondWireframe", GT._("On") },
    { "ssbondSidechain", GT._("Set SS-Bonds Side Chain") },
    { "ssbondBackbone", GT._("Set SS-Bonds Backbone") },
    { "ssbond100", "0.10 \u00C5" },
    { "ssbond150", "0.15 \u00C5" },
    { "ssbond200", "0.20 \u00C5" },
    { "ssbond250", "0.25 \u00C5" },
    { "ssbond300", "0.30 \u00C5" },

    { "structureMenu", GT._("Structures") },
    { "structureNone", GT._("Off") },
    { "Backbone", GT._("Backbone") },
    { "Trace", GT._("Trace") },
    { "Cartoon", GT._("Cartoon") },
    { "Ribbons", GT._("Ribbons") },

    { "vectorMenu", GT._("Vectors") },
    { "vectorOff", GT._("Off") },
    { "vectorOn", GT._("On") },
    { "vector3", GT._("{0} pixels", new Object[]{ "3" }) },
    { "vector005", "0.05 \u00C5" },
    { "vector01", "0.1 \u00C5" },
    { "vectorScale02", GT._("Scale {0}", new Object[]{ "0.2" }) },
    { "vectorScale05", GT._("Scale {0}", new Object[]{ "0.5" }) },
    { "vectorScale1", GT._("Scale {0}", new Object[]{ "1" }) },
    { "vectorScale2", GT._("Scale {0}", new Object[]{ "2" }) },
    { "vectorScale5", GT._("Scale {0}", new Object[]{ "5" }) },

    { "stereoMenu", "Stereographic" },
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

    { "colorAtomSchemeMenu", GT._("By Scheme") },
    { "colorAtomElement", GT._("Element (CPK)") },
    { "colorAtomAminoAcids", GT._("Amino") },
    { "colorAtomSecondaryStructure", GT._("Secondary Structure") },
    { "colorAtomChain", GT._("Chain") },
    { "colorAtomFormalCharge", GT._("Formal Charge") },
    { "colorAtomPartialCharge", GT._("Partial Charge") },

    { "colorAtomBlack", GT._("Black") },
    { "colorAtomWhite", GT._("White") },

    { "colorAtomRedMenu", GT._("Red") },
    { "colorAtomRed", GT._("Red") },
    { "colorAtomCrimson", GT._("Crimson") },
    { "colorAtomDarkRed", GT._("Dark Red") },
    { "colorAtomFireBrick", GT._("Firebrick") },
    { "colorAtomIndianRed", GT._("Indian Red") },
    { "colorAtomDarkMagenta", GT._("Dark Magenta") },
    { "colorAtomDarkSalmon", GT._("Dark Salmon") },
    { "colorAtomLightSalmon", GT._("Light Salmon") },
    { "colorAtomDeepPink", GT._("Deep Pink") },
    { "colorAtomLightPink", GT._("Light Pink") },

    { "colorAtomYellowMenu", GT._("Yellow") },
    { "colorAtomYellow", GT._("Yellow") },
    { "colorAtomGold", GT._("Gold") },
    { "colorAtomGoldenrod", GT._("Goldenrod") },
    { "colorAtomLemonChiffon", GT._("Lemon Chiffon") },
    { "colorAtomYellowGreen", GT._("Yellow-Green") },

    { "colorAtomGreenMenu", GT._("Green") },
    { "colorAtomGreen", GT._("Green") },
    { "colorAtomLime", GT._("Lime") },
    { "colorAtomSeaGreen", GT._("Seagreen") },
    { "colorAtomGreenBlue", GT._("Green-Blue") },
    { "colorAtomSpringGreen", GT._("Spring Green") },

    { "colorAtomBlueMenu", GT._("Blue") },
    { "colorAtomBlue", GT._("Blue") },
    { "colorAtomAqua", GT._("Aqua") },
    { "colorAtomAzure", GT._("Azure") },
    { "colorAtomCarolinaBlue", GT._("Carolina Blue") },
    { "colorAtomCadetBlue", GT._("Cadet Blue") },
    { "colorAtomCornflowerBlue", GT._("Cornflower") },
    { "colorAtomDarkSlateBlue", GT._("Dark Slate Blue") },
    { "colorAtomLightSteelBlue", GT._("Light Steel Blue") },

    { "colorBondMenu", GT._("Bonds") },
    { "colorBondInherit", GT._("Inherit") },
    { "colorBondRed", GT._("Red") },
    { "colorBondOrange", GT._("Orange") },
    { "colorBondYellow", GT._("Yellow") },
    { "colorBondGreen", GT._("Green") },
    { "colorBondBlue", GT._("Blue") },
    { "colorBondIndigo", GT._("Indigo") },
    { "colorBondViolet", GT._("Violet") },
    { "colorBondBlack", GT._("Black") },
    { "colorBondWhite", GT._("White") },
    { "colorBondCyan", GT._("Cyan") },

    { "colorHbondMenu", GT._("Hydrogen Bonds") },
    { "colorHbondInherit", GT._("Inherit") },
    { "colorHbondRed", GT._("Red") },
    { "colorHbondOrange", GT._("Orange") },
    { "colorHbondYellow", GT._("Yellow") },
    { "colorHbondGreen", GT._("Green") },
    { "colorHbondBlue", GT._("Blue") },
    { "colorHbondIndigo", GT._("Indigo") },
    { "colorHbondViolet", GT._("Violet") },
    { "colorHbondBlack", GT._("Black") },
    { "colorHbondWhite", GT._("White") },
    { "colorHbondCyan", GT._("Cyan") },

    { "colorSSbondMenu", GT._("Disulfide Bonds") },
    { "colorSSbondInherit", GT._("Inherit") },
    { "colorSSbondRed", GT._("Red") },
    { "colorSSbondOrange", GT._("Orange") },
    { "colorSSbondYellow", GT._("Yellow") },
    { "colorSSbondGreen", GT._("Green") },
    { "colorSSbondBlue", GT._("Blue") },
    { "colorSSbondIndigo", GT._("Indigo") },
    { "colorSSbondViolet", GT._("Violet") },
    { "colorSSbondBlack", GT._("Black") },
    { "colorSSbondWhite", GT._("White") },
    { "colorSSbondCyan", GT._("Cyan") },

    { "colorBackboneMenu", "Backbone" },
    { "colorBackboneInherit", GT._("Inherit") },

    { "colorBackboneSchemeMenu", GT._("By Scheme") },
    { "colorBackboneElement", GT._("Element (CPK)") },
    { "colorBackboneAminoAcids", GT._("Amino") },
    { "colorBackboneSecondaryStructure", GT._("Secondary Structure") },
    { "colorBackboneChain", GT._("Chain") },
    { "colorBackboneCharge", GT._("Charge") },

    { "colorBackboneBlack", GT._("Black") },
    { "colorBackboneWhite", GT._("White") },
    { "colorBackboneRed", GT._("Red") },
    { "colorBackboneOrange", GT._("Orange") },
    { "colorBackboneYellow", GT._("Yellow") },
    { "colorBackboneGreen", GT._("Green") },
    { "colorBackboneBlue", GT._("Blue") },
    { "colorBackboneIndigo", GT._("Indigo") },
    { "colorBackboneViolet", GT._("Violet") },
    { "colorBackboneCyan", GT._("Cyan") },

    { "colorTraceMenu", GT._("Trace") },
    { "colorTraceInherit", GT._("Inherit") },
    
    { "colorTraceSchemeMenu", GT._("By Scheme") },
    { "colorTraceElement", GT._("Element (CPK)") },
    { "colorTraceAminoAcids", GT._("Amino") },
    { "colorTraceSecondaryStructure", GT._("Secondary Structure") },
    { "colorTraceChain", GT._("Chain") },
    { "colorTraceCharge", GT._("Charge") },

    { "colorTraceBlack", GT._("Black") },
    { "colorTraceWhite", GT._("White") },
    { "colorTraceRed", GT._("Red") },
    { "colorTraceOrange", GT._("Orange") },
    { "colorTraceYellow", GT._("Yellow") },
    { "colorTraceGreen", GT._("Green") },
    { "colorTraceBlue", GT._("Blue") },
    { "colorTraceIndigo", GT._("Indigo") },
    { "colorTraceViolet", GT._("Violet") },
    { "colorTraceCyan", GT._("Cyan") },

    { "colorCartoonMenu", GT._("Cartoon") },
    { "colorCartoonInherit", GT._("Inherit") },

    { "colorCartoonSchemeMenu", GT._("By Scheme") },
    { "colorCartoonElement", GT._("Element (CPK)") },
    { "colorCartoonAminoAcids", GT._("Amino") },
    { "colorCartoonSecondaryStructure", GT._("Secondary Structure") },
    { "colorCartoonChain", GT._("Chain") },
    { "colorCartoonCharge", GT._("Charge") },

    { "colorCartoonBlack", GT._("Black") },
    { "colorCartoonWhite", GT._("White") },
    { "colorCartoonRed", GT._("Red") },
    { "colorCartoonOrange", GT._("Orange") },
    { "colorCartoonYellow", GT._("Yellow") },
    { "colorCartoonGreen", GT._("Green") },
    { "colorCartoonBlue", GT._("Blue") },
    { "colorCartoonIndigo", GT._("Indigo") },
    { "colorCartoonViolet", GT._("Violet") },
    { "colorCartoonCyan", GT._("Cyan") },

    { "colorRibbonsMenu", GT._("Ribbons") },
    { "colorRibbonsInherit", GT._("Inherit") },

    { "colorRibbonsSchemeMenu", GT._("By Scheme") },
    { "colorRibbonsElement", GT._("Element (CPK)") },
    { "colorRibbonsAminoAcids", GT._("Amino") },
    { "colorRibbonsSecondaryStructure", GT._("Secondary Structure") },
    { "colorRibbonsChain", GT._("Chain") },
    { "colorRibbonsCharge", GT._("Charge") },

    { "colorRibbonsBlack", GT._("Black") },
    { "colorRibbonsWhite", GT._("White") },
    { "colorRibbonsRed", GT._("Red") },
    { "colorRibbonsOrange", GT._("Orange") },
    { "colorRibbonsYellow", GT._("Yellow") },
    { "colorRibbonsGreen", GT._("Green") },
    { "colorRibbonsBlue", GT._("Blue") },
    { "colorRibbonsIndigo", GT._("Indigo") },
    { "colorRibbonsViolet", GT._("Violet") },
    { "colorRibbonsCyan", GT._("Cyan") },

    { "colorLabelMenu", GT._("Labels") },
    { "colorLabelInherit", GT._("Inherit") },
    { "colorLabelBlack", GT._("Black") },
    { "colorLabelWhite", GT._("White") },
    { "colorLabelRed", GT._("Red") },
    { "colorLabelOrange", GT._("Orange") },
    { "colorLabelYellow", GT._("Yellow") },
    { "colorLabelGreen", GT._("Green") },
    { "colorLabelBlue", GT._("Blue") },
    { "colorLabelIndigo", GT._("Indigo") },
    { "colorLabelViolet", GT._("Violet") },
    { "colorLabelCyan", GT._("Cyan") },

    { "colorBackgroundMenu", GT._("Background") },
    { "colorBackgroundBlack", GT._("Black") },
    { "colorBackgroundWhite", GT._("White") },
    { "colorBackgroundRed", GT._("Red") },
    { "colorBackgroundOrange", GT._("Orange") },
    { "colorBackgroundYellow", GT._("Yellow") },
    { "colorBackgroundGreen", GT._("Green") },
    { "colorBackgroundBlue", GT._("Blue") },
    { "colorBackgroundIndigo", GT._("Indigo") },
    { "colorBackgroundViolet", GT._("Violet") },
    { "colorBackgroundCyan", GT._("Cyan") },

    { "colorVectorMenu", GT._("Vectors") },
    { "colorVectorInherit", GT._("Inherit") },
    { "colorVectorBlack", GT._("Black") },
    { "colorVectorWhite", GT._("White") },
    { "colorVectorRed", GT._("Red") },
    { "colorVectorOrange", GT._("Orange") },
    { "colorVectorYellow", GT._("Yellow") },
    { "colorVectorGreen", GT._("Green") },
    { "colorVectorBlue", GT._("Blue") },
    { "colorVectorIndigo", GT._("Indigo") },
    { "colorVectorViolet", GT._("Violet") },
    { "colorVectorCyan", GT._("Cyan") },

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
    { "spinx0", "0" },
    { "spinx5", "5" },
    { "spinx10", "10" },
    { "spinx20", "20" },
    { "spinx30", "30" },
    { "spinx40", "40" },
    { "spinx50", "50" },

    { "setSpinYMenu", GT._("Set Y Rate") },
    { "spiny0", "set spin y 0" },
    { "spiny5", "5" },
    { "spiny10", "10" },
    { "spiny20", "20" },
    { "spiny30", "30" },
    { "spiny40", "40" },
    { "spiny50", "50" },

    { "setSpinZMenu", GT._("Set Z Rate") },
    { "spinz0", "0" },
    { "spinz5", "5" },
    { "spinz10", "10" },
    { "spinz20", "20" },
    { "spinz30", "30" },
    { "spinz40", "40" },
    { "spinz50", "50" },

    { "setSpinFpsMenu", GT._("Set FPS") },
    { "spinfps5", "5" },
    { "spinfps10", "10" },
    { "spinfps20", "20" },
    { "spinfps30", "30" },
    { "spinfps50", "50" },

    { "animateMenu", GT._("Animate") },
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

    { "measurementsMenu", GT._("Measurements") },
    { "distanceNanometers", GT._("Nanometers") },
    { "distanceAngstroms", GT._("Angstroms") },
    { "distancePicometers", GT._("Picometers") },

    { "crystalMenu", GT._("Crystal") },

    { "axesMenu", GT._("Axes") },
    { "axesOff", GT._("Hide") },
    { "axesDotted", GT._("Dotted") },

    { "axesByPixelMenu", GT._("Pixel Width") },
    { "axes1p", GT._("{0} px", new Object[]{ "1" }) },
    { "axes3p", GT._("{0} px", new Object[]{ "3" }) },
    { "axes5p", GT._("{0} px", new Object[]{ "5" }) },
    { "axes10p", GT._("{0} px", new Object[] { "10" }) },

    { "axesByAngstromMenu", "Angstrom Width" },
    { "axes10a", "0.10 \u00C5" },
    { "axes20a", "0.20 \u00C5" },
    { "axes25a", "0.25 \u00C5" },
    { "axes50a", "0.50 \u00C5" },
    { "axes100a", "1.0 \u00C5" },

    { "colorAxesMenu", GT._("Color") },
    { "colorAxesGray", GT._("Gray") },
    { "colorAxesSlateBlue", GT._("Slate Blue") },
    { "colorAxesGold", GT._("Gold") },
    { "colorAxesOrchid", GT._("Orchid") },

    { "bbcageMenu", GT._("Boundbox") },
    { "bbcageOff", GT._("Hide") },
    { "bbcageDotted", GT._("Dotted") },

    { "bbcageByPixelMenu", "Pixel Width" },
    { "bbcage1p", GT._("{0} px", new Object[]{ "1" }) },
    { "bbcage3p", GT._("{0} px", new Object[]{ "3" }) },
    { "bbcage5p", GT._("{0} px", new Object[]{ "5" }) },
    { "bbcage10p", GT._("{0} px", new Object[]{ "10" }) },

    { "bbcageByAngstromMenu", GT._("Angstrom Width") },
    { "bbcage10a", "0.10 \u00C5" },
    { "bbcage20a", "0.20 \u00C5" },
    { "bbcage25a", "0.25 \u00C5" },
    { "bbcage50a", "0.50 \u00C5" },
    { "bbcage100a", "1.0 \u00C5" },

    { "colorBbcageMenu", GT._("Color") },
    { "colorBbcageGray", GT._("Gray") },
    { "colorBbcageSalmon", GT._("Salmon") },
    { "colorBbcageOlive", GT._("Olive") },
    { "colorBbcageMaroon", GT._("Maroon") },

    { "uccageMenu", GT._("Unitcell") },
    { "uccageOff", GT._("Hide") },
    { "uccageDotted", GT._("Dotted") },

    { "uccageByPixelMenu", GT._("Pixel Width") },
    { "uccage1p", GT._("{0} px", new Object[]{ "1" }) },
    { "uccage3p", GT._("{0} px", new Object[]{ "3" }) },
    { "uccage5p", GT._("{0} px", new Object[]{ "5" }) },
    { "uccage10p", GT._("{0} px", new Object[]{ "10" }) },

    { "uccageByAngstromMenu", "Angstrom Width" },
    { "uccage10a", "0.10 \u00C5" },
    { "uccage20a", "0.20 \u00C5" },
    { "uccage25a", "0.25 \u00C5" },
    { "uccage50a", "0.50 \u00C5" },
    { "uccage100a", "1.0 \u00C5" },

    { "colorUccageMenu", GT._("Color") },
    { "colorUccageGray", GT._("Gray") },
    { "colorUccageAquamarine", GT._("Aquamarine") },
    { "colorUccageForestGreen", GT._("Forest Green") },
    { "colorUccageHotPink", GT._("Hot Pink") },

    { "optionsMenu", GT._("Options") },
    { "showSelectionsCheckbox", GT._("Show Selection Halos" ) },
    { "showHydrogensCheckbox", GT._("Show Hydrogens") },
    { "showMeasurementsCheckbox", GT._("Show Measurements") },
    { "perspectiveDepthCheckbox", GT._("Perspective Depth") },
    { "rasmolChimeCompatibility", GT._("RasMol/Chime Compatibility") },
    { "rasmolColors", GT._("RasMol Colors") },
    { "jmolColors", GT._("Jmol Colors") },
    { "axesOrientationRasmolCheckbox", GT._("Axes RasMol/Chime") },
    { "zeroBasedXyzRasmolCheckbox", GT._("Zero Based Xyz Rasmol") },
    { "wireframeRotationCheckbox", GT._("Wireframe Rotation") },

    { "consoleMenu", GT._("Console...") },
    { "consoleOn", GT._("Open") },
    { "consoleOff", GT._("Close") },

    { "aboutMenu", GT._("About Jmol") },
    { "jmolUrl", "http://www.jmol.org" },
  };

  // Initialize properties
  static {
    for (int i = 0; i < contents.length; i++) {
      properties.setProperty(contents[i][0], contents[i][1]);
    }
  }
}
