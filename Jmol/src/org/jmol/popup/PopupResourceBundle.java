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
  
  private static final String[][] structureContents = {
    { "popupMenu", "modelSetInfoMenu - selectMenu renderMenu labelMenu colorMenu - " +
                   "zoomMenu spinMenu animateMenu - " +
                   "measureMenu pickingMenu crystalMenu optionsMenu - showMenu - " +
                   "aboutMenu" },

    { "selectMenu", "selectAll selectNone - " +
                    "proteinMenu nucleicMenu heteroMenu Carbohydrate NoneOfTheAbove - " +
                    "elementsComputedMenu byModelMenu byFrameMenu - " +
                    "invertSelection restrictToSelection" },
    { "selectAll", "select all" },
    { "selectNone", "select none" },

    { "proteinMenu", "allProtein proteinBackbone proteinSideChains - " +
                     "polar nonpolar - " +
                     "positiveCharge negativeCharge noCharge - " +
                     "aaresiduesComputedMenu" },
    { "allProtein", "select protein" },
    { "proteinBackbone", "select protein and backbone" },
    { "proteinSideChains", "select protein and not backbone" },
    { "polar", "select protein and polar" },
    { "nonpolar", "select protein and not polar" },
    { "positiveCharge", "select protein and basic" },
    { "negativeCharge", "select protein and acidic" },
    { "noCharge", "select protein and not (acidic,basic)" },

    { "nucleicMenu", "allNucleic nucleicBackbone nucleicBases - " +
                     "DNA RNA - " +
                     "A C G T U - " +
                     "atPairs auPairs gcPairs" },
    { "allNucleic", "select nucleic" },
    { "DNA", "select dna" },
    { "RNA", "select rna" },
    { "nucleicBackbone", "select nucleic and backbone" },
    { "nucleicBases", "select nucleic and not backbone" },
    { "atPairs", "select a,t" },
    { "gcPairs", "select g,c" },
    { "auPairs", "select a,u" },
    { "A", "select a" },
    { "C", "select c" },
    { "G", "select g" },
    { "T", "select t" },
    { "U", "select u" },

    { "heteroMenu", "allHetero Solvent Water - " +
                    "Ligand exceptWater nonWaterSolvent" },
    { "allHetero", "select hetero" },
    { "Solvent", "select solvent" },
    { "Water", "select water" },
// same as ligand    { "exceptSolvent", "select hetero and not solvent" },
    { "nonWaterSolvent", "select solvent and not water" },
    { "exceptWater", "select hetero and not water" },
    { "Ligand", "select ligand" },

    { "Carbohydrate", "select carbohydrate" },
// not implemented    { "Lipid", "select lipid" },
    { "NoneOfTheAbove", "select not(hetero,protein,nucleic,carbohydrate)" },


    { "byModelMenu", "allModels" },
    { "allModels", "select all" },

    { "byFrameMenu", "allFrames" },
    { "allFrames", "select all" },

    { "invertSelection", "select not selected" },

    { "restrictToSelection", "restrict selected" },

    { "setSelectModeMenu", "replace add narrow" },
    { "replace", "#replace selected" },
    { "add", "#or selected" },
    { "narrow", "#and selected" },

    { "renderMenu", "renderSchemeMenu - atomMenu bondMenu hbondMenu ssbondMenu - " +
                    "structureMenu - vectorMenu - stereoMenu" },
    { "renderSchemeMenu", "renderCpkSpacefill renderBallAndStick " +
                          "renderSticks renderWireframe" },
    { "renderCpkSpacefill", "backbone off;wireframe off;spacefill 100%" },
    { "renderBallAndStick", "backbone off;spacefill 20%;wireframe 0.15" },
    { "renderSticks", "backbone off;spacefill off;wireframe 0.3" },
    { "renderWireframe", "backbone off;spacefill off;wireframe on" },
    { "renderBackbone", "spacefill off;wireframe off;backbone on" },

    { "atomMenu", "atomNone - " +
                  "atom15 atom20 atom25 atom50 atom75 atom100" },
    { "atomNone", "cpk off" },
    { "atom15", "cpk 15%" },
    { "atom20", "cpk 20%" },
    { "atom25", "cpk 25%" },
    { "atom50", "cpk 50%" },
    { "atom75", "cpk 75%" },
    { "atom100", "cpk on" },

    { "bondMenu", "bondNone bondWireframe - " +
                  "bond100 bond150 bond200 bond250 bond300" },
    { "bondNone", "wireframe off" },
    { "bondWireframe", "wireframe on" },
    { "bond100", "wireframe .1" },
    { "bond150", "wireframe .15" },
    { "bond200", "wireframe .2" },
    { "bond250", "wireframe .25" },
    { "bond300", "wireframe .3" },

    { "hbondMenu", "hbondCalc hbondNone hbondWireframe - " +
                   "hbondSidechain hbondBackbone - " +
                   "hbond100 hbond150 hbond200 hbond250 hbond300" },
    { "hbondCalc", "hbonds calculate" },
    { "hbondNone", "hbonds off" },
    { "hbondWireframe", "hbonds on" },
    { "hbondSidechain", "set hbonds sidechain" },
    { "hbondBackbone", "set hbonds backbone" },
    { "hbond100", "hbonds .1" },
    { "hbond150", "hbonds .15" },
    { "hbond200", "hbonds .2" },
    { "hbond250", "hbonds .25" },
    { "hbond300", "hbonds .3" },

    { "ssbondMenu", "ssbondNone ssbondWireframe - " +
                    "ssbondSidechain ssbondBackbone - " +
                    "ssbond100 ssbond150 ssbond200 ssbond250 ssbond300" },
    { "ssbondNone", "ssbonds off" },
    { "ssbondWireframe", "ssbonds on" },
    { "ssbondSidechain", "set ssbonds sidechain" },
    { "ssbondBackbone", "set ssbonds backbone" },
    { "ssbond100", "ssbonds .1" },
    { "ssbond150", "ssbonds .15" },
    { "ssbond200", "ssbonds .2" },
    { "ssbond250", "ssbonds .25" },
    { "ssbond300", "ssbonds .3" },

    { "structureMenu", "structureNone - " +
                       "Backbone Trace Cartoon Ribbons" },
    { "structureNone", "cartoon off;ribbons off;trace off;backbone off" },
    { "Backbone", "backbone 0.3" },
    { "Trace", "trace 0.3" },
    { "Cartoon", "cartoon on" },
    { "Ribbons", "ribbons on" },

    { "vectorMenu", "vectorOff vectorOn vector3 vector005 vector01 - " +
                    "vectorScale02 vectorScale05 vectorScale1 vectorScale2 vectorScale5" },
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

    { "stereoMenu", "stereoNone stereoRedCyan stereoRedBlue stereoRedGreen stereoCrossEyed stereoWallEyed" },
    { "stereoNone", "stereo off" },
    { "stereoRedCyan", "stereo redcyan 3" },
    { "stereoRedBlue", "stereo redblue 3" },
    { "stereoRedGreen", "stereo redgreen 3" },
    { "stereoCrossEyed", "stereo 5" },
    { "stereoWallEyed", "stereo -5" },

    { "labelMenu", "labelNone - " +
                   "labelSymbol labelName labelNumber - " +
                    "labelPositionMenu" },

    { "labelNone", "label off" },
    { "labelSymbol", "label %e" },
    { "labelName", "label %a" },
    { "labelNumber", "label %i" },

    { "labelPositionMenu", "labelCentered labelUpperRight labelLowerRight labelUpperLeft labelLowerLeft" },
    { "labelCentered", "set labeloffset 0 0" },
    { "labelUpperRight", "set labeloffset 4 4" },
    { "labelLowerRight", "set labeloffset 4 -4" },
    { "labelUpperLeft", "set labeloffset -4 4" },
    { "labelLowerLeft", "set labeloffset -4 -4" },

    { "colorMenu", "colorAtomMenu colorBondMenu colorHbondMenu colorSSbondMenu - " +
                   "colorBackboneMenu colorTraceMenu colorCartoonMenu colorRibbonsMenu - " +
                   "colorLabelMenu colorBackgroundMenu - " +
                   "colorVectorMenu" },
    { "colorAtomMenu", "colorAtomSchemeMenu - " +
                       "colorAtomBlack colorAtomWhite - " +
                       "colorAtomRedMenu colorAtomYellowMenu colorAtomGreenMenu colorAtomBlueMenu" },

    { "colorAtomSchemeMenu", "colorAtomElement colorAtomAminoAcids colorAtomSecondaryStructure " +
                             "colorAtomChain colorAtomFormalCharge colorAtomPartialCharge" },
    { "colorAtomElement", "color atoms cpk" },
    { "colorAtomAminoAcids", "color atoms amino" },
    { "colorAtomSecondaryStructure", "color atoms structure" },
    { "colorAtomChain", "color atoms chain" },
    { "colorAtomFormalCharge", "color atoms formalCharge" },
    { "colorAtomPartialCharge", "color atoms partialCharge" },

    { "colorAtomBlack", "color black" },
    { "colorAtomWhite", "color white" },

    { "colorAtomRedMenu", "colorAtomRed colorAtomCrimson colorAtomDarkRed colorAtomFireBrick " +
                          "colorAtomIndianRed colorAtomDarkMagenta colorAtomDarkSalmon " +
                          "colorAtomLightSalmon colorAtomDeepPink colorAtomLightPink" },
    { "colorAtomRed", "color red" },
    { "colorAtomCrimson", "color crimson" },
    { "colorAtomDarkRed", "color darkred" },
    { "colorAtomFireBrick", "color firebrick" },
    { "colorAtomIndianRed", "color indianred" },
    { "colorAtomDarkMagenta", "color darkmagenta" },
    { "colorAtomDarkSalmon", "color darksalmon" },
    { "colorAtomLightSalmon", "color lightsalmon" },
    { "colorAtomDeepPink", "color deeppink" },
    { "colorAtomLightPink", "color lightpink" },

    { "colorAtomYellowMenu", "colorAtomYellow colorAtomGold colorAtomGoldenrod colorAtomLemonChiffon " +
                             "colorAtomYellowGreen" },
    { "colorAtomYellow", "color yellow" },
    { "colorAtomGold", "color gold" },
    { "colorAtomGoldenrod", "color goldenrod" },
    { "colorAtomLemonChiffon", "color lemonchiffon" },
    { "colorAtomYellowGreen", "color yellowgreen" },

    { "colorAtomGreenMenu", "colorAtomGreen colorAtomLime colorAtomSeaGreen colorAtomGreenBlue " +
                            "colorAtomSpringGreen" },
    { "colorAtomGreen", "color green" },
    { "colorAtomLime", "color lime" },
    { "colorAtomSeaGreen", "color seagreen" },
    { "colorAtomGreenBlue", "color greenblue" },
    { "colorAtomSpringGreen", "color springgreen" },

    { "colorAtomBlueMenu", "colorAtomBlue colorAtomAqua colorAtomAzure colorAtomCarolinaBlue " +
                           "colorAtomCadetBlue colorAtomCornflowerBlue colorAtomDarkSlateBlue " +
                           "colorAtomLightSteelBlue" },
    { "colorAtomBlue", "color blue" },
    { "colorAtomAqua", "color aqua" },
    { "colorAtomAzure", "color azure" },
    { "colorAtomCarolinaBlue", "color dodgerblue" },
    { "colorAtomCadetBlue", "color cadetblue" },
    { "colorAtomCornflowerBlue", "color cornflowerblue" },
    { "colorAtomDarkSlateBlue", "color darkslateblue" },
    { "colorAtomLightSteelBlue", "color lightsteelblue" },

    { "colorBondMenu", "colorBondInherit - " +
                       "colorBondBlack colorBondWhite colorBondCyan - " +
                       "colorBondRed colorBondOrange colorBondYellow colorBondGreen " +
                       "colorBondBlue colorBondIndigo colorBondViolet" },
    { "colorBondInherit", "color bonds none" },
    { "colorBondRed", "color bonds red" },
    { "colorBondOrange", "color bonds orange" },
    { "colorBondYellow", "color bonds yellow" },
    { "colorBondGreen", "color bonds green" },
    { "colorBondBlue", "color bonds blue" },
    { "colorBondIndigo", "color bonds indigo" },
    { "colorBondViolet","color bonds violet" },
    { "colorBondBlack", "color bonds black" },
    { "colorBondWhite", "color bonds white" },
    { "colorBondCyan", "color bonds cyan" },

    { "colorHbondMenu", "colorHbondInherit - " +
                        "colorHbondBlack colorHbondWhite colorHbondCyan - " +
                        "colorHbondRed colorHbondOrange colorHbondYellow colorHbondGreen " +
                        "colorHbondBlue colorHbondIndigo colorHbondViolet" },
    { "colorHbondInherit", "color hbonds none" },
    { "colorHbondRed", "color hbonds red" },
    { "colorHbondOrange", "color hbonds orange" },
    { "colorHbondYellow", "color hbonds yellow" },
    { "colorHbondGreen", "color hbonds green" },
    { "colorHbondBlue", "color hbonds blue" },
    { "colorHbondIndigo", "color hbonds indigo" },
    { "colorHbondViolet", "color hbonds violet" },
    { "colorHbondBlack", "color hbonds black" },
    { "colorHbondWhite", "color hbonds white" },
    { "colorHbondCyan", "color hbonds cyan" },

    { "colorSSbondMenu", "colorSSbondInherit - " +
                         "colorSSbondBlack colorSSbondWhite colorSSbondCyan - " +
                         "colorSSbondRed colorSSbondOrange colorSSbondYellow colorSSbondGreen " +
                         "colorSSbondBlue colorSSbondIndigo colorSSbondViolet" },
    { "colorSSbondInherit", "color ssbonds none" },
    { "colorSSbondRed", "color ssbonds red" },
    { "colorSSbondOrange", "color ssbonds orange" },
    { "colorSSbondYellow", "color ssbonds yellow" },
    { "colorSSbondGreen", "color ssbonds green" },
    { "colorSSbondBlue", "color ssbonds blue" },
    { "colorSSbondIndigo", "color ssbonds indigo" },
    { "colorSSbondViolet", "color ssbonds violet" },
    { "colorSSbondBlack", "color ssbonds black" },
    { "colorSSbondWhite", "color ssbonds white" },
    { "colorSSbondCyan", "color ssbonds cyan" },

    { "colorBackboneMenu", "colorBackboneInherit colorBackboneSchemeMenu - " +
                           "colorBackboneBlack colorBackboneWhite colorBackboneCyan - " +
                           "colorBackboneRed colorBackboneOrange colorBackboneYellow " +
                           "colorBackboneGreen colorBackboneBlue colorBackboneIndigo colorBackboneViolet" },
    { "colorBackboneInherit", "color backbone none" },

    { "colorBackboneSchemeMenu", "colorBackboneElement colorBackboneAminoAcids " +
                                 "colorBackboneSecondaryStructure colorBackboneChain colorBackboneCharge" },
    { "colorBackboneElement", "color backbone cpk" },
    { "colorBackboneAminoAcids", "color backbone amino" },
    { "colorBackboneSecondaryStructure", "color backbone structure" },
    { "colorBackboneChain", "color backbone chain" },
    { "colorBackboneCharge", "color backbone charge" },

    { "colorBackboneBlack", "color backbone black" },
    { "colorBackboneWhite", "color backbone white" },
    { "colorBackboneRed", "color backbone red" },
    { "colorBackboneOrange", "color backbone orange" },
    { "colorBackboneYellow", "color backbone yellow" },
    { "colorBackboneGreen", "color backbone green" },
    { "colorBackboneBlue", "color backbone blue" },
    { "colorBackboneIndigo", "color backbone indigo" },
    { "colorBackboneViolet", "color backbone violet" },
    { "colorBackboneCyan", "color backbone cyan" },

    { "colorTraceMenu", "colorTraceInherit colorTraceSchemeMenu - " +
                        "colorTraceBlack colorTraceWhite colorTraceCyan - " +
                        "colorTraceRed colorTraceOrange colorTraceYellow colorTraceGreen " +
                        "colorTraceBlue colorTraceIndigo colorTraceViolet" },
    { "colorTraceInherit", "color trace none" },
    
    { "colorTraceSchemeMenu", "colorTraceElement colorTraceAminoAcids colorTraceSecondaryStructure " +
                              "colorTraceChain colorTraceCharge" },
    { "colorTraceElement", "color trace cpk" },
    { "colorTraceAminoAcids", "color trace amino" },
    { "colorTraceSecondaryStructure", "color trace structure" },
    { "colorTraceChain", "color trace chain" },
    { "colorTraceCharge", "color trace charge" },

    { "colorTraceBlack", "color trace black" },
    { "colorTraceWhite", "color trace white" },
    { "colorTraceRed", "color trace red" },
    { "colorTraceOrange", "color trace orange" },
    { "colorTraceYellow", "color trace yellow" },
    { "colorTraceGreen", "color trace green" },
    { "colorTraceBlue", "color trace blue" },
    { "colorTraceIndigo", "color trace indigo" },
    { "colorTraceViolet", "color trace violet" },
    { "colorTraceCyan", "color trace cyan" },

    { "colorCartoonMenu", "colorCartoonInherit colorCartoonSchemeMenu - " +
                          "colorCartoonBlack colorCartoonWhite colorCartoonCyan - " +
                          "colorCartoonRed colorCartoonOrange colorCartoonYellow colorCartoonGreen " +
                          "colorCartoonBlue colorCartoonIndigo colorCartoonViolet" },
    { "colorCartoonInherit", "color cartoon none" },

    { "colorCartoonSchemeMenu", "colorCartoonElement colorCartoonAminoAcids " +
                                "colorCartoonSecondaryStructure colorCartoonChain colorCartoonCharge" },
    { "colorCartoonElement", "color cartoon cpk" },
    { "colorCartoonAminoAcids", "color cartoon amino" },
    { "colorCartoonSecondaryStructure", "color cartoon structure" },
    { "colorCartoonChain", "color cartoon chain" },
    { "colorCartoonCharge", "color cartoon charge" },

    { "colorCartoonBlack", "color cartoon black" },
    { "colorCartoonWhite", "color cartoon white" },
    { "colorCartoonRed", "color cartoon red" },
    { "colorCartoonOrange", "color cartoon orange" },
    { "colorCartoonYellow", "color cartoon yellow" },
    { "colorCartoonGreen", "color cartoon green" },
    { "colorCartoonBlue", "color cartoon blue" },
    { "colorCartoonIndigo", "color cartoon indigo" },
    { "colorCartoonViolet", "color cartoon violet" },
    { "colorCartoonCyan", "color cartoon cyan" },

    { "colorRibbonsMenu", "colorRibbonsInherit colorRibbonsSchemeMenu - " +
                          "colorRibbonsBlack colorRibbonsWhite colorRibbonsCyan - " +
                          "colorRibbonsRed colorRibbonsOrange colorRibbonsYellow colorRibbonsGreen " +
                          "colorRibbonsBlue colorRibbonsIndigo colorRibbonsViolet" },
    { "colorRibbonsInherit", "color ribbons none" },

    { "colorRibbonsSchemeMenu", "colorRibbonsElement colorRibbonsAminoAcids " +
                                "colorRibbonsSecondaryStructure colorRibbonsChain colorRibbonsCharge" },
    { "colorRibbonsElement", "color ribbons cpk" },
    { "colorRibbonsAminoAcids", "color ribbons amino" },
    { "colorRibbonsSecondaryStructure", "color ribbons structure" },
    { "colorRibbonsChain", "color ribbons chain" },
    { "colorRibbonsCharge", "color ribbons charge" },

    { "colorRibbonsBlack", "color ribbons black" },
    { "colorRibbonsWhite", "color ribbons white" },
    { "colorRibbonsRed", "color ribbons red" },
    { "colorRibbonsOrange", "color ribbons orange" },
    { "colorRibbonsYellow", "color ribbons yellow" },
    { "colorRibbonsGreen", "color ribbons green" },
    { "colorRibbonsBlue", "color ribbons blue" },
    { "colorRibbonsIndigo", "color ribbons indigo" },
    { "colorRibbonsViolet", "color ribbons violet" },
    { "colorRibbonsCyan", "color ribbons cyan" },

    { "colorLabelMenu", "colorLabelInherit - " +
                        "colorLabelBlack colorLabelWhite colorLabelCyan - " +
                        "colorLabelRed colorLabelOrange colorLabelYellow colorLabelGreen " +
                        "colorLabelBlue colorLabelIndigo colorLabelViolet" },
    { "colorLabelInherit", "color labels none" },
    { "colorLabelBlack", "color labels black" },
    { "colorLabelWhite", "color labels white" },
    { "colorLabelRed", "color labels red" },
    { "colorLabelOrange", "color labels orange" },
    { "colorLabelYellow", "color labels yellow" },
    { "colorLabelGreen", "color labels green" },
    { "colorLabelBlue", "color labels blue" },
    { "colorLabelIndigo", "color labels indigo" },
    { "colorLabelViolet", "color labels violet" },
    { "colorLabelCyan", "color labels cyan" },

    { "colorBackgroundMenu", "colorBackgroundBlack colorBackgroundWhite - " +
                             "colorBackgroundRed colorBackgroundOrange colorBackgroundYellow " +
                             "colorBackgroundGreen colorBackgroundBlue colorBackgroundIndigo " +
                             "colorBackgroundViolet" },
    { "colorBackgroundBlack", "background black" },
    { "colorBackgroundWhite", "background white" },
    { "colorBackgroundRed", "background red" },
    { "colorBackgroundOrange", "background orange" },
    { "colorBackgroundYellow", "background yellow" },
    { "colorBackgroundGreen", "background green" },
    { "colorBackgroundBlue", "background blue" },
    { "colorBackgroundIndigo", "background indigo" },
    { "colorBackgroundViolet", "background violet" },
    { "colorBackgroundCyan", "background cyan" },

    { "colorVectorMenu", "colorVectorInherit - " +
                         "colorVectorBlack colorVectorWhite colorVectorCyan - " +
                         "colorVectorRed colorVectorOrange colorVectorYellow colorVectorGreen " +
                         "colorVectorBlue colorVectorIndigo colorVectorViolet" },
    { "colorVectorInherit", "color vectors none" },
    { "colorVectorBlack", "color vectors black" },
    { "colorVectorWhite", "color vectors white" },
    { "colorVectorRed", "color vectors red" },
    { "colorVectorOrange", "color vectors orange" },
    { "colorVectorYellow", "color vectors yellow" },
    { "colorVectorGreen", "color vectors green" },
    { "colorVectorBlue", "color vectors blue" },
    { "colorVectorIndigo", "color vectors indigo" },
    { "colorVectorViolet", "color vectors violet" },
    { "colorVectorCyan", "color vectors cyan" },

    { "zoomMenu", "zoom50 zoom100 zoom150 zoom200 zoom400 zoom800 - " +
                  "zoomIn zoomOut" },
    { "zoom50", "zoom 50" },
    { "zoom100", "zoom 100" },
    { "zoom150", "zoom 150" },
    { "zoom200", "zoom 200" },
    { "zoom400", "zoom 400" },
    { "zoom800", "zoom 800" },
    { "zoomIn", "move 0 0 0 40 0 0 0 0 1" },
    { "zoomOut", "move 0 0 0 -40 0 0 0 0 1" },

    { "spinMenu", "spinOn spinOff - " +
                  "setSpinXMenu setSpinYMenu setSpinZMenu - " +
                  "setSpinFpsMenu" },
    { "spinOn", "spin on" },
    { "spinOff", "spin off" },

    { "setSpinXMenu", "spinx0 spinx5 spinx10 spinx20 spinx30 spinx40 spinx50" },
    { "spinx0", "set spin x 0" },
    { "spinx5", "set spin x 5" },
    { "spinx10", "set spin x 10" },
    { "spinx20", "set spin x 20" },
    { "spinx30", "set spin x 30" },
    { "spinx40", "set spin x 40" },
    { "spinx50", "set spin x 50" },

    { "setSpinYMenu", "spiny0 spiny5 spiny10 spiny20 spiny30 spiny40 spiny50" },
    { "spiny0", "set spin y 0" },
    { "spiny5", "set spin y 5" },
    { "spiny10", "set spin y 10" },
    { "spiny20", "set spin y 20" },
    { "spiny30", "set spin y 30" },
    { "spiny40", "set spin y 40" },
    { "spiny50", "set spin y 50" },

    { "setSpinZMenu", "spinz0 spinz5 spinz10 spinz20 spinz30 spinz40 spinz50" },
    { "spinz0", "set spin z 0" },
    { "spinz5", "set spin z 5" },
    { "spinz10", "set spin z 10" },
    { "spinz20", "set spin z 20" },
    { "spinz30", "set spin z 30" },
    { "spinz40", "set spin z 40" },
    { "spinz50", "set spin z 50" },

    { "setSpinFpsMenu", "spinfps5 spinfps10 spinfps20 spinfps30 spinfps50" },
    { "spinfps5", "set spin fps 5" },
    { "spinfps10", "set spin fps 10" },
    { "spinfps20", "set spin fps 20" },
    { "spinfps30", "set spin fps 30" },
    { "spinfps50", "set spin fps 50" },

    { "animateMenu", "setAnimModeMenu - " +
                     "play stop nextframe prevframe rewind - " +
                     "setAnimFpsMenu" },
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

    { "measureMenu", "measureOff measureDistance measureAngle measureTorsion measureDelete measureList distanceNanometers distanceAngstroms distancePicometers" },
    { "measureOff", "set pickingstyle MEASURE OFF; set picking OFF" },
    { "measureDistance", "set pickingstyle MEASURE; set picking MEASURE DISTANCE" },
    { "measureAngle", "set pickingstyle MEASURE; set picking MEASURE ANGLE" },
    { "measureTorsion", "set pickingstyle MEASURE; set picking MEASURE TORSION" },
    { "measureDelete", "measure delete" },
    { "measureList", "console;show measurements" },
    { "distanceNanometers", "select *; set measure nanometers" },
    { "distanceAngstroms", "select *; set measure angstroms" },
    { "distancePicometers", "select *; set measure picometers" },

    { "pickingMenu", "pickOff pickCenter pickLabel pickAtom pickChain " + 
      "pickElement pickGroup pickMolecule pickSite pickSpin" },
    { "pickOff" , "set picking off" },
    { "pickCenter" , "set picking center" },
//    { "pickDraw" , "set picking draw" },
    { "pickLabel" , "set picking label" },
    { "pickAtom" , "set picking atom" },
    { "pickChain" , "set picking chain" },
    { "pickElement" , "set picking element" },
    { "pickGroup" , "set picking group" },
    { "pickMolecule" , "set picking molecule" },
    { "pickSite" , "set picking site" },
    { "pickSpin" , "set picking spin" },
    
    { "showMenu" , "showConsole showFile showFileHeader showIsosurface "
      + "showMeasure showMo showModel " + 
      "showOrient showSpacegroup showSymmetry showUnitcell extractMOL" },
    { "showConsole", "console" },
    { "showFile", "show file"},
    { "showIsosurface", "show isosurface"},
    { "showMeasure", "show measure"},
    { "showMo", "show mo"},
    { "showModel", "show model"},
    { "showOrient", "show orientation"},
    { "showPDBHeader", "show PDBHeader"},
    { "showSpacegroup", "show spacegroup"},
    { "showSymmetry", "show symmetry"},
    { "showUnitcell", "show unitcell"},
    { "extractMOL", "getproperty extractModel \"visible\" "},
      
    { "crystalMenu", "axesMenu bbcageMenu uccageMenu" },

    { "axesMenu", "axesOff axesDotted axesByPixelMenu axesByAngstromMenu colorAxesMenu" },
    { "axesOff", "set axes off" },
    { "axesDotted", "set axes dotted" },

    { "axesByPixelMenu", "axes1p axes3p axes5p axes10p" },
    { "axes1p", "set axes on" },
    { "axes3p", "set axes 3" },
    { "axes5p", "set axes 5" },
    { "axes10p", "set axes 10" },

    { "axesByAngstromMenu", "axes10a axes20a axes25a axes50a axes100a" },
    { "axes10a", "set axes 0.1" },
    { "axes20a", "set axes 0.20" },
    { "axes25a", "set axes 0.25" },
    { "axes50a", "set axes 0.50" },
    { "axes100a", "set axes 1.0" },

    { "colorAxesMenu", "colorAxesGray colorAxesSlateBlue colorAxesGold colorAxesOrchid" },
    { "colorAxesGray", "color axes gray" },
    { "colorAxesSlateBlue", "color axes SlateBlue" },
    { "colorAxesGold", "color axes gold" },
    { "colorAxesOrchid", "color axes orchid" },

    { "bbcageMenu", "bbcageOff bbcageDotted bbcageByPixelMenu bbcageByAngstromMenu colorBbcageMenu" },
    { "bbcageOff", "set boundbox off" },
    { "bbcageDotted", "set boundbox dotted" },

    { "bbcageByPixelMenu", "bbcage1p bbcage3p bbcage5p bbcage10p" },
    { "bbcage1p", "set boundbox on" },
    { "bbcage3p", "set boundbox 3" },
    { "bbcage5p", "set boundbox 5" },
    { "bbcage10p", "set boundbox 10" },

    { "bbcageByAngstromMenu", "bbcage10a bbcage20a bbcage25a bbcage50a bbcage100a" },
    { "bbcage10a", "set boundbox 0.1" },
    { "bbcage20a", "set boundbox 0.20" },
    { "bbcage25a", "set boundbox 0.25" },
    { "bbcage50a", "set boundbox 0.50" },
    { "bbcage100a", "set boundbox 1.0" },

    { "colorBbcageMenu", "colorBbcageGray colorBbcageSalmon colorBbcageOlive colorBbcageMaroon" },
    { "colorBbcageGray", "color boundbox gray" },
    { "colorBbcageSalmon", "color boundbox salmon" },
    { "colorBbcageOlive", "color boundbox olive" },
    { "colorBbcageMaroon", "color boundbox maroon" },

    { "uccageMenu", "uccageOff uccageDotted uccageByPixelMenu uccageByAngstromMenu colorUccageMenu" },
    { "uccageOff", "set unitcell off" },
    { "uccageDotted", "set unitcell dotted" },

    { "uccageByPixelMenu", "uccage1p uccage3p uccage5p uccage10p" },
    { "uccage1p", "set unitcell on" },
    { "uccage3p", "set unitcell 3" },
    { "uccage5p", "set unitcell 5" },
    { "uccage10p", "set unitcell 10" },

    { "uccageByAngstromMenu", "uccage10a uccage20a uccage25a uccage50a uccage100a" },
    { "uccage10a", "set unitcell 0.1" },
    { "uccage20a", "set unitcell 0.20" },
    { "uccage25a", "set unitcell 0.25" },
    { "uccage50a", "set unitcell 0.50" },
    { "uccage100a", "set unitcell 1.0" },

    { "colorUccageMenu", "colorUccageGray colorUccageAquamarine colorUccageForestGreen colorUccageHotPink" },
    { "colorUccageGray", "color unitcell gray" },
    { "colorUccageAquamarine", "color unitcell aquamarine" },
    { "colorUccageForestGreen", "color unitcell forestgreen" },
    { "colorUccageHotPink", "color unitcell hotpink" },

    { "optionsMenu", "showSelectionsCheckbox showHydrogensCheckbox " +
                     "showMeasurementsCheckbox " +
                     "perspectiveDepthCheckbox - " +
                     "rasmolChimeCompatibility - " +
                     "rasmolColors jmolColors " +
                     "axesOrientationRasmolCheckbox zeroBasedXyzRasmolCheckbox" },
    { "rasmolChimeCompatibility", "set color rasmol; set zeroBasedXyzRasmol on; " +
                                  "set axesOrientationRasmol on; select *; cpk off; wireframe on" },
    { "rasmolColors", "set color rasmol" },
    { "jmolColors", "set color jmol" },

    { "aboutMenu", "jmolUrl mouseManualUrl translatingUrl" },
    { "jmolUrl", "http://www.jmol.org" },
    { "mouseManualUrl", "http://wiki.jmol.org/index.php/Mouse_Manual" },
    { "translatingUrl", "http://wiki.jmol.org/index.php/Internationalisation" },
  };

  private static final String[][] wordContents = {
    { "modelSetInfoMenu", GT._("No atoms loaded") },
    { "hiddenModelSetName", GT._("Model information") },

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

    { "heteroMenu", GT._("Hetero") },
    { "allHetero", GT._("All PDB \"HETATOM\"") },
    { "Solvent", GT._("All Solvent") },
    { "Water", GT._("All Water") },
    { "nonWaterSolvent", GT._("Nonaqueous Solvent") + " (solvent and not water)" },
    { "exceptWater", GT._("Nonaqueous HETATM") + " (hetero and not water)" },
    { "Ligand", GT._("Ligand") + " (hetero and not solvent)" },
    { "Carbohydrate", GT._("Carbohydrate") },
    { "NoneOfTheAbove", GT._("None of the above") },

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
    { "hbondCalc", GT._("Calculate") },
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

    { "measureMenu", GT._("Measurement") },
    { "measureOff", GT._("Double-Click starts all measurements") },
    { "measureDistance", GT._("Click for distance measurement") },
    { "measureAngle", GT._("Click for angle measurement") },
    { "measureTorsion", GT._("Click for torsion (dihedral) measurement") },
    { "measureDelete", GT._("Delete measurements") },
    { "measureList", GT._("List measurements") },
    { "distanceNanometers", GT._("Distance units nanometers") },
    { "distanceAngstroms", GT._("Distance units Angstroms") },
    { "distancePicometers", GT._("Distance units picometers") },

    { "pickingMenu", GT._("Set picking") },
    { "pickOff" , GT._("Off") },
    { "pickCenter" , GT._("Center") },
//    { "pickDraw" , GT._("moves arrows") },
    { "pickLabel" , GT._("Label") },
    { "pickAtom" , GT._("Select atom") },
    { "pickChain" , GT._("Select chain") },
    { "pickElement" , GT._("Select element") },
    { "pickGroup" , GT._("Select group") },
    { "pickMolecule" , GT._("Select molecule") },
    { "pickSite" , GT._("Select site") },
    { "pickSpin" , GT._("Spin") },
    
    { "showMenu" , GT._( "Show") },
    { "showConsole", GT._("Console") },
    { "showFile", GT._("File Contents")},
    { "showFileHeader",  GT._("File Header")},
    { "showIsosurface", GT._("Isosurface JVXL data")},
    { "showMeasure",  GT._("Measure")},
    { "showMo",  GT._("Molecular orbital JVXL data")},
    { "showModel",  GT._("Model")},
    { "showOrient",  GT._("Orientation")},
    { "showSpacegroup",  GT._("Space group")},
    { "showSymmetry",  GT._("Symmetry")},
    { "showUnitcell",  GT._("Unit cell")},    
    { "extractMOL",  GT._("Extract MOL data")},

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
    { "showSelectionsCheckbox", GT._("Show Selected Atoms" ) },
    { "showHydrogensCheckbox", GT._("Show Hydrogens") },
    { "showMeasurementsCheckbox", GT._("Show Measurements") },
    { "perspectiveDepthCheckbox", GT._("Perspective Depth") },
    { "rasmolChimeCompatibility", GT._("RasMol/Chime Compatibility") },
    { "rasmolColors", GT._("RasMol Colors") },
    { "jmolColors", GT._("Jmol Colors") },
    { "axesOrientationRasmolCheckbox", GT._("Axes RasMol/Chime") },
    { "zeroBasedXyzRasmolCheckbox", GT._("Zero Based Xyz Rasmol") },

    { "aboutMenu", GT._("About Jmol") },
    { "jmolUrl", "www.jmol.org" },
    { "mouseManualUrl", GT._("Mouse Manual") },
    { "translatingUrl", GT._("Translations") },
  };

  // Initialize properties
  static {
    for (int i = 0; i < structureContents.length; i++) {
      structure.setProperty(structureContents[i][0], structureContents[i][1]);
    }
    for (int i = 0; i < wordContents.length; i++) {
      words.setProperty(wordContents[i][0], wordContents[i][1]);
    }
  }
}
