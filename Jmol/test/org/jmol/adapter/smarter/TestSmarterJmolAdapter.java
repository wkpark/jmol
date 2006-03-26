/*
 * JUnit TestCase for the SmilesParser
 */

package org.jmol.adapter.smarter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

public class TestSmarterJmolAdapter extends TestCase {

  public TestSmarterJmolAdapter(String arg0) {
    super(arg0);
  }

  /**
   * Test for reading files in abint 
   */
  public void testAbint() {
    //checkOpenFile("abint", "pei2h2o.out");
    //checkOpenFile("abint", "Si_eband.out");
    //checkOpenFile("abint", "slab_7Si_3Vac_2x_relax_2x1.out");
    //checkOpenFile("abint", "t12.abinit.out");
    //checkOpenFile("abint", "t13.abinit.out");
    //checkOpenFile("abint", "zeolite_AFI.out");
  }
  /**
   * Test for reading files in aces2/
   */
  public void testAces2() {
    //checkOpenFile("aces2", "ch3oh_ace.out");
    //checkOpenFile("aces2", "output.dat");
  }
  
  /**
   * Test for reading files in aminoacids/
   */
  public void testAminoAcids() {
    checkOpenFile("aminoacids", "ala.mol");
    checkOpenFile("aminoacids", "ala.pdb");
    checkOpenFile("aminoacids", "alphahelix.pdb");
    checkOpenFile("aminoacids", "arg.mol");
    checkOpenFile("aminoacids", "arg.pdb");
    checkOpenFile("aminoacids", "asn.mol");
    checkOpenFile("aminoacids", "asn.pdb");
    checkOpenFile("aminoacids", "asp.mol");
    checkOpenFile("aminoacids", "asp.pdb");
    checkOpenFile("aminoacids", "betasheet.pdb");
    checkOpenFile("aminoacids", "cys.mol");
    checkOpenFile("aminoacids", "cys.pdb");
    checkOpenFile("aminoacids", "gln.mol");
    checkOpenFile("aminoacids", "gln.pdb");
    checkOpenFile("aminoacids", "glu.mol");
    checkOpenFile("aminoacids", "glu.pdb");
    checkOpenFile("aminoacids", "gly.mol");
    checkOpenFile("aminoacids", "gly.pdb");
    checkOpenFile("aminoacids", "his.mol");
    checkOpenFile("aminoacids", "his.pdb");
    checkOpenFile("aminoacids", "ile.mol");
    checkOpenFile("aminoacids", "ile.pdb");
    checkOpenFile("aminoacids", "leu.mol");
    checkOpenFile("aminoacids", "leu.pdb");
    checkOpenFile("aminoacids", "lys.mol");
    checkOpenFile("aminoacids", "lys.pdb");
    checkOpenFile("aminoacids", "met.mol");
    checkOpenFile("aminoacids", "met.pdb");
    checkOpenFile("aminoacids", "phe.mol");
    checkOpenFile("aminoacids", "phe.pdb");
    checkOpenFile("aminoacids", "pro.mol");
    checkOpenFile("aminoacids", "pro.pdb");
    checkOpenFile("aminoacids", "ser.mol");
    checkOpenFile("aminoacids", "ser.pdb");
    checkOpenFile("aminoacids", "thr.mol");
    checkOpenFile("aminoacids", "thr.pdb");
    checkOpenFile("aminoacids", "trp.mol");
    checkOpenFile("aminoacids", "trp.pdb");
    checkOpenFile("aminoacids", "tyr.mol");
    checkOpenFile("aminoacids", "tyr.pdb");
    checkOpenFile("aminoacids", "val.mol");
    checkOpenFile("aminoacids", "val.pdb");
  }
  
  /**
   * Test for reading files in animations/ 
   */
  public void testAnimations() {
    checkOpenFile("animations", "cyclohexane_movie.xyz");
    checkOpenFile("animations", "diels-alder_movie.xyz");
    checkOpenFile("animations", "Met_aq.movie.xyz");
    checkOpenFile("animations", "met-enkaphalin_movie.xyz");
    checkOpenFileGzip("animations", "movie2.pdb.gz");
    checkOpenFile("animations", "movie.pdb");
    checkOpenFile("animations", "SN1_reaction.cml");
    checkOpenFile("animations", "SN1_reaction.xyz");
    checkOpenFile("animations", "SN2_reaction.xyz");
  }
  
  /**
   * Test for reading files in cif/
   */
  public void testCif() {
    checkOpenFile("cif", "114D.cif");
    checkOpenFile("cif", "1A00.cif");
    checkOpenFile("cif", "1ARJ.cif");
    checkOpenFile("cif", "1B07.cif");
    checkOpenFile("cif", "1CRN.cif");
    checkOpenFile("cif", "1D66.cif");
    checkOpenFile("cif", "1D68.cif");
    checkOpenFile("cif", "1EBL.cif");
    checkOpenFile("cif", "1HJE.cif");
    checkOpenFile("cif", "1NE6.cif");
    checkOpenFile("cif", "1OHG.cif");
    checkOpenFile("cif", "233D.cif");
    checkOpenFile("cif", "3DFR.cif");
    checkOpenFile("cif", "3OVO.cif");
    checkOpenFile("cif", "409686.cff");
    checkOpenFile("cif", "CIJQOB.cif");
    checkOpenFile("cif", "k04041.cif");
    checkOpenFile("cif", "wm6029.cif");
  }

  /**
   * Test for reading files in cml/ 
   */
  public void testCml() {
    checkOpenFile("cml", "acetate.cml");
    checkOpenFile("cml", "aceticacid.cml");
    checkOpenFile("cml", "benzene.cml");
    checkOpenFile("cml", "ci6455_I_flattened.cml");
    checkOpenFile("cml", "ci6455_I_symm.cml");
    checkOpenFile("cml", "estron.cml");
    checkOpenFile("cml", "list2.cml");
    checkOpenFile("cml", "list.cml");
    checkOpenFile("cml", "long4lines.cml");
    checkOpenFile("cml", "methanol1.cml");
    checkOpenFile("cml", "methanol2.cml");
    checkOpenFile("cml", "nsc202.cml");
    checkOpenFile("cml", "nsc244.cml");
    checkOpenFile("cml", "nsc244a.cml");
    checkOpenFile("cml", "nsc300.cml");
    checkOpenFile("cml", "nsc484.cml");
    checkOpenFile("cml", "nsc2582.cml");
  }

  /**
   * Test for reading files in crystals/
   */
  public void testCrystals() {
    checkOpenFile("crystals", "3CRO.pdb");
    checkOpenFile("crystals", "clathrate.mol");
    checkOpenFile("crystals", "kaolinite_big.pdb");
    checkOpenFile("crystals", "kaolinite_big_noconnect.pdb");
    checkOpenFile("crystals", "kaolinite_small.mol");
    checkOpenFile("crystals", "mtbe.mol");
  }

  /**
   * Test for reading files in cube/
   */
  public void testCube() {
    checkOpenFileGzip("cube", "2px.cub.gz");
    checkOpenFileGzip("cube", "benzene-homo.cub.gz");
    checkOpenFileGzip("cube", "ch3cl-density.cub.gz");
    checkOpenFileGzip("cube", "ch3cl-esp.cub.gz");
    checkOpenFileGzip("cube", "o2h2_1_04.cube.gz");
    checkOpenFileGzip("cube", "o2h2_1_05.cube.gz");
    checkOpenFileGzip("cube", "o2h2_1_06.cube.gz");
    checkOpenFileGzip("cube", "o2h2_1_07.cube.gz");
    checkOpenFileGzip("cube", "o2h2_1_08.cube.gz");
  }

  /**
   * Test for reading files in folding/
   */
  public void testFolding() {
    checkOpenFile("folding", "amber_linux_p1800.xyz");
    checkOpenFile("folding", "gromacs_linux_p729.xyz");
    checkOpenFile("folding", "gromacs_linux_p731.xyz");
    checkOpenFile("folding", "gromacs_mac_p264.xyz");
    checkOpenFile("folding", "gromacs_mac_p265.xyz");
    checkOpenFile("folding", "gromacs_mac_p268.xyz");
    checkOpenFile("folding", "gromacs_mac_p294.xyz");
    checkOpenFile("folding", "gromacs_mac_p730.xyz");
    checkOpenFile("folding", "gromacs_p1301.xyz");
    checkOpenFile("folding", "gromacs_win_p262.xyz");
    checkOpenFile("folding", "gromacs_win_p267.xyz");
    checkOpenFileGzip("folding", "qmd_linux_p1900.xyz.gz");
    checkOpenFile("folding", "tinker_linux_p678.xyz");
    checkOpenFile("folding", "tinker_linux_p1106.xyz");
    checkOpenFile("folding", "tinker_mac_p678.xyz");
    checkOpenFile("folding", "tinker_mac_p1218.xyz");
    checkOpenFile("folding", "tinker_win_p1108.xyz");
    checkOpenFile("folding", "tinker_win_p1114.xyz");
  }

  /**
   * Test for reading files in gamess/
   */
  public void testGamess() {
    checkOpenFile("gamess", "Cl2O.log");
    checkOpenFile("gamess", "ch3oh_gam.out");
    checkOpenFile("gamess", "substrate.log");
    checkOpenFile("gamess", "water.out");
  }

  /**
   * Test for reading files in gaussian/
   */
  public void testGaussian() {
    checkOpenFile("gaussian", "ch2chfme_reagent.out");
    checkOpenFile("gaussian", "tms.log");
    checkOpenFile("gaussian", "4-cyanophenylnitrene-Benzazirine-TS.g94.out");
    checkOpenFile("gaussian", "ch3oh_g94.out");
    checkOpenFile("gaussian", "cyanine_PM3.out");
    checkOpenFile("gaussian", "g98.out");
    checkOpenFile("gaussian", "h2o.g03.log");
    checkOpenFile("gaussian", "H2O.out");
    checkOpenFile("gaussian", "H2O_3.log");
    checkOpenFile("gaussian", "H2O_G03.log");
    checkOpenFile("gaussian", "H2O_G03_opt.log");
    checkOpenFile("gaussian", "H2O_G03_zopt.log");
    checkOpenFile("gaussian", "H2O_NoSymm.out");
    checkOpenFile("gaussian", "H2O_NoSymm_G03.log");
    checkOpenFile("gaussian", "H2O_NoSymm_G03_opt.log");
    checkOpenFile("gaussian", "H2O_NoSymm_G03_zopt.log");
    checkOpenFile("gaussian", "phenol-without-para-H.g98.out");
    checkOpenFile("gaussian", "phenylnitrene.g94.out");
    checkOpenFile("gaussian", "RK_g03_freq.log");
    checkOpenFile("gaussian", "RK_g03_opt.log");
    checkOpenFile("gaussian", "RK_g03_scan.log");
  }

  /**
   * Test for reading files in ghemical/
   */
  public void testGhemical() {
    checkOpenFile("ghemical", "1BPI.gpr");
    checkOpenFile("ghemical", "2-fluoroethoxymethane.gpr");
    checkOpenFile("ghemical", "ethene.mm1gp");
    checkOpenFile("ghemical", "methane-ethane.gpr");
    checkOpenFile("ghemical", "peptide-hydrocarbon.gpr");
    checkOpenFile("ghemical", "propane.gpr");
  }

  /**
   * Test for reading files in hin/
   */
  public void testHin() {
    checkOpenFile("hin", "dan002.hin");
    checkOpenFile("hin", "dan031.hin");
    checkOpenFile("hin", "twoModels.hin");
  }

  /**
   * Test for reading files in jaguar/
   */
  public void testJaguar() {
    checkOpenFile("jaguar", "CH4_no_sym_ir.out");
    checkOpenFile("jaguar", "CH4_with_sym_ir.out");
  }

  /**
   * Test for reading files in modifiedGroups/ 
   */
  public void testModifiedGroups() {
    checkOpenFile("modifiedGroups", "1BV7.cif");
    checkOpenFile("modifiedGroups", "1BV7.pdb");
    checkOpenFile("modifiedGroups", "1CLV.cif");
    checkOpenFile("modifiedGroups", "1CLV.pdb");
    checkOpenFile("modifiedGroups", "1D2F.cif");
    checkOpenFile("modifiedGroups", "1D2F.pdb");
    checkOpenFile("modifiedGroups", "1EHZ.cif");
    checkOpenFile("modifiedGroups", "1EHZ.pdb");
    checkOpenFile("modifiedGroups", "6HBW.cif");
    checkOpenFile("modifiedGroups", "6HBW.pdb");
  }

  /**
   * Test for reading files in mol/ 
   */
  public void testMol() {
    checkOpenFile("mol", "aceticacid.mol");
    checkOpenFile("mol", "aromatic.mol");
    checkOpenFile("mol", "aspirina.mol");
    checkOpenFile("mol", "cefixime_syn.mol");
    checkOpenFile("mol", "cyclopropane.mol");
    checkOpenFile("mol", "f_socl2b.mol");
    checkOpenFile("mol", "jmol.mol");
    checkOpenFile("mol", "nsc202.mol");
    checkOpenFile("mol", "nsc244.sdf");
    checkOpenFile("mol", "nsc300.mol");
    checkOpenFile("mol", "nsc484.sdf");
    checkOpenFile("mol", "nsc2582.sdf");
    checkOpenFile("mol", "PAUL_SEARCH4.mol");
    checkOpenFile("mol", "pf5.mol");
    checkOpenFile("mol", "Rgroup_Example.mol");
    checkOpenFile("mol", "test.mol");
    checkOpenFile("mol", "test.sdf");
    checkOpenFile("mol", "triplebond.mol");
  }

  /**
   * Test for reading files in molpro/
   */
  public void testMolpro() {
    //checkOpenFile("molpro", "vib.xml");
  }

  /**
   * Test for reading files in mopac/ 
   */
  public void testMopac() {
    checkOpenFile("mopac", "2-aminodiphenyl.out");
    checkOpenFile("mopac", "mirtaz.out");
    checkOpenFile("mopac", "mopac2002-2hb-opt.out");
    checkOpenFile("mopac", "mopac2002-2hb-vib.out");
    checkOpenFile("mopac", "mopac2002-h2o-vib.out");
    checkOpenFile("mopac", "nci3d_006_p3_001.out");
    checkOpenFile("mopac", "reaction.out");
  }

  /**
   * Test for reading files in nwchem/
   */
  public void testNwchem() {
    checkOpenFile("nwchem", "H2O_1.nwo");
    checkOpenFile("nwchem", "H2O_2.nwo");
    checkOpenFile("nwchem", "H2O_3.nwo");
  }

  /**
   * Test for reading files in pdb/
   */
  public void testPdb() {
    checkOpenFile("pdb", "1A00.pdb");
    checkOpenFile("pdb", "1ALE.pdb");
    checkOpenFile("pdb", "1ALM.pdb");
    checkOpenFile("pdb", "1ARJ.pdb");
    checkOpenFile("pdb", "1B07.pdb");
    checkOpenFile("pdb", "1CRN.pdb");
    checkOpenFile("pdb", "1CRN_noSS.pdb");
    checkOpenFile("pdb", "1D66.pdb");
    checkOpenFile("pdb", "1D68.pdb");
    checkOpenFile("pdb", "1EBL.pdb");
    checkOpenFile("pdb", "1GFL.pdb");
    checkOpenFile("pdb", "1HJE.pdb");
    checkOpenFile("pdb", "1IHA.pdb");
    checkOpenFile("pdb", "1JGQ.pdb");
    checkOpenFile("pdb", "1LCD.pdb");
    checkOpenFile("pdb", "1MBO.pdb");
    checkOpenFile("pdb", "1NE6.pdb");
    checkOpenFile("pdb", "1NE6_noSS.pdb");
    checkOpenFile("pdb", "1OHG.pdb");
    checkOpenFile("pdb", "1PN8.pdb");
    checkOpenFile("pdb", "3DFR.pdb");
    checkOpenFile("pdb", "3DFR_noSS.pdb");
    checkOpenFile("pdb", "3OVO.pdb");
    checkOpenFile("pdb", "5CRO.pdb");
    checkOpenFile("pdb", "114D.pdb");
    checkOpenFile("pdb", "233D.pdb");
    checkOpenFile("pdb", "chainColors.pdb");
    checkOpenFile("pdb", "demoanims.pdb");
    checkOpenFile("pdb", "diffGear.pdb");
    checkOpenFile("pdb", "finalPump96.09.06.pdb");
    checkOpenFile("pdb", "fineMotion970116.pdb");
    //checkOpenFileGzip("pdb", "fullRhinovirus.pdb.gz");
    checkOpenFile("pdb", "hemmth.pdb");
    checkOpenFile("pdb", "hemoglobin.pdb");
    checkOpenFileGzip("pdb", "hugeMembrane.pdb.gz");
    checkOpenFile("pdb", "PeriodicTable.pdb");
    checkOpenFile("pdb", "toluene.pdb");
  }

  /**
   * Test for reading files in pmesh/ 
   */
  public void testPmesh() {
    // pmesh files are not molecular data files
  }

  /**
   * Test for reading files in qchem/
   */
  public void testQchem() {
    checkOpenFile("qchem", "ch3oh_qchem.out");
  }

  /**
   * Test for reading files in shelx/
   */
  public void testShelx() {
    checkOpenFile("shelx", "6063.res");
    checkOpenFile("shelx", "complexSFAC.res");
    checkOpenFile("shelx", "frame_1.res");
    checkOpenFile("shelx", "k04041.res");
    checkOpenFile("shelx", "vmdtest.res");
  }

  /**
   * Test for reading files in spartan/
   */
  public void testSpartan() {
    checkOpenFile("spartan", "benzenevib.smol");
    checkOpenFile("spartan", "carbon dioxide.smol");
    checkOpenFile("spartan", "CH3FCl_TS_PM3_FREQ.smol");
    checkOpenFile("spartan", "CH3FCl_TS_RHF.smol");
    checkOpenFile("spartan", "CH3FCl_TS_RHF_FREQ.smol");
    checkOpenFile("spartan", "H2_DFT_FREQ.smol");
    checkOpenFile("spartan", "H2_PM3.smol");
    checkOpenFile("spartan", "H2_PM3_FREQ.smol");
    checkOpenFile("spartan", "H2_RHF.smol");
    checkOpenFile("spartan", "H2_RHF_FREQ.smol");
    checkOpenFile("spartan", "methane.smol");
    checkOpenFile("spartan", "spartan01.txt");
    checkOpenFile("spartan", "sulfur hexafluoride.smol");
    checkOpenFile("spartan", "water.smol");
  }

  /**
   * Test for reading files in v3000/ 
   */
  public void testV3000() {
    checkOpenFile("v3000", "WO0119816T.sdf");
  }

  /**
   * Test for reading files in xyz/
   */
  public void testXyz() {
    checkOpenFile("xyz", "axes.xyz");
    checkOpenFile("xyz", "azt.xyz");
    checkOpenFile("xyz", "caffeine.xyz");
    checkOpenFile("xyz", "CdSe_nanocrystal.xyz");
    checkOpenFile("xyz", "cholesterol.xyz");
    checkOpenFile("xyz", "cs2.xyz");
    checkOpenFile("xyz", "cyclohexane_chair.xyz");
    checkOpenFile("xyz", "ddt.xyz");
    checkOpenFile("xyz", "dna.xyz");
    checkOpenFile("xyz", "ethanol.xyz");
    checkOpenFile("xyz", "nanotube.xyz");
    checkOpenFile("xyz", "relax.xyz");
    checkOpenFile("xyz", "surfaceProblem.xyz");
    checkOpenFile("xyz", "taxol.xyz");
    checkOpenFile("xyz", "test8.xyz");
    checkOpenFile("xyz", "viagra.xyz");
    checkOpenFile("xyz", "zoloft.xyz");
  }

  /**
   * Check that the file can be read
   * 
   * @param directory Directory where the file is (relative to Jmol-datafiles)
   * @param filename File name
   */
  private void checkOpenFile(String directory, String filename) {
    
    // Open file 
    Object result = null;
    try {
      SmarterJmolAdapter adapter = new SmarterJmolAdapter(null);
      File file = new File(new File("../Jmol-datafiles", directory), filename);
      InputStream iStream = new FileInputStream(file);
      BufferedInputStream biStream = new BufferedInputStream(iStream);
      BufferedReader bReader = new BufferedReader(new InputStreamReader(biStream));
      result = adapter.openBufferedReader(filename, bReader);
    } catch (Exception e) {
      fail("checkFile (" + directory + "/" + filename + "): " + e.getMessage());
    }
    
    // Check result
    if (result == null) {
      fail("checkFile (" + directory + "/" + filename + "): returns null");
    }
    if (result instanceof String) {
      fail("checkFile (" + directory + "/" + filename + ") :" + result);
    }
  }

  /**
   * Check that the file can be read
   * 
   * @param directory Directory where the file is (relative to Jmol-datafiles)
   * @param filename File name
   */
  private void checkOpenFileGzip(String directory, String filename) {
    
    // Open file 
    Object result = null;
    try {
      SmarterJmolAdapter adapter = new SmarterJmolAdapter(null);
      File file = new File(new File("../Jmol-datafiles", directory), filename);
      InputStream iStream = new FileInputStream(file);
      BufferedInputStream biStream = new BufferedInputStream(iStream);
      BufferedReader bReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(biStream)));
      result = adapter.openBufferedReader(filename, bReader);
    } catch (Exception e) {
      fail("checkFile (" + directory + "/" + filename + "): " + e.getMessage());
    }
    
    // Check result
    if (result == null) {
      fail("checkFile (" + directory + "/" + filename + "): returns null");
    }
    if (result instanceof String) {
      fail("checkFile (" + directory + "/" + filename + ") :" + result);
    }
    
    result = null;
    System.gc();
  }
}
