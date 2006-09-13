/*
 * JUnit TestCase for the SmilesParser
 */

package org.jmol.adapter.smarter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.jmol.api.JmolAdapter;
import org.jmol.util.JUnitLogger;

import junit.framework.TestCase;

public class TestSmarterJmolAdapter extends TestCase {

  public TestSmarterJmolAdapter(String arg0) {
    super(arg0);
  }

  /**
   * Test for reading files in abint 
   */
  public void testAbint() {
    //checkDirectory("abint", "out", "");
  }
  /**
   * Test for reading files in aces2/
   */
  public void testAces2() {
    //checkDirectory("aces2", "dat,out", "");
  }
  
  /**
   * Test for reading files in aminoacids/
   */
  public void testAminoAcids() {
    checkDirectory("aminoacids", "mol,pdb", "");
  }
  
  /**
   * Test for reading files in animations/ 
   */
  public void testAnimations() {
    checkDirectory("animations", "cml,pdb,xyz", "pdb.gz");
  }
  
  /**
   * Test for reading files in cif/
   */
  public void testCif() {
    checkDirectory("cif", "cif", "");
  }

  /**
   * Test for reading files in cml/ 
   */
  public void testCml() {
    checkDirectory("cml", "cml", "");
  }

  /**
   * Test for reading files in crystals/
   */
  public void testCrystals() {
    checkDirectory("crystals", "pdb,mol", "");
  }

  /**
   * Test for reading files in cube/
   */
  public void testCube() {
    checkDirectory("cube", "", "cub.gz,cube.gz");
  }

  /**
   * Test for reading files in folding/
   */
  public void testFolding() {
    checkDirectory("folding", "xyz", "xyz.gz");
  }

  /**
   * Test for reading files in gamess/
   */
  public void testGamess() {
    checkDirectory("gamess", "log,out", "");
  }

  /**
   * Test for reading files in gaussian/
   */
  public void testGaussian() {
    checkDirectory("gaussian", "log,out", "");
  }

  /**
   * Test for reading files in ghemical/
   */
  public void testGhemical() {
    checkDirectory("ghemical", "gpr", "");
  }

  /**
   * Test for reading files in hin/
   */
  public void testHin() {
    checkDirectory("hin", "hin", "");
  }

  /**
   * Test for reading files in jaguar/
   */
  public void testJaguar() {
    checkDirectory("jaguar", "out", "");
  }

  /**
   * Test for reading files in modifiedGroups/ 
   */
  public void testModifiedGroups() {
    checkDirectory("modifiedGroups", "cif,pdb", "");
  }

  /**
   * Test for reading files in mol/ 
   */
  public void testMol() {
    checkDirectory("mol", "mol,sdf", "");
  }

  /**
   * Test for reading files in molpro/
   */
  public void testMolpro() {
    checkDirectory("molpro", "xml", "");
  }

  /**
   * Test for reading files in mopac/ 
   */
  public void testMopac() {
    checkDirectory("mopac", "out", "");
  }

  /**
   * Test for reading files in nwchem/
   */
  public void testNwchem() {
    checkDirectory("nwchem", "nwo", "");
  }

  /**
   * Test for reading files in pdb/
   */
  public void testPdb() {
    checkDirectory("pdb", "pdb", "pdb.gz");
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
    checkDirectory("qchem", "out", "");
  }

  /**
   * Test for reading files in shelx/
   */
  public void testShelx() {
    checkDirectory("shelx", "res", "");
  }

  /**
   * Test for reading files in spartan/
   */
  public void testSpartan() {
    checkDirectory("spartan", "smol,txt", "");
  }

  /**
   * Test for reading files in v3000/ 
   */
  public void testV3000() {
    checkDirectory("v3000", "sdf", "");
  }

  /**
   * Test for reading files in xyz/
   */
  public void testXyz() {
    checkDirectory("xyz", "xyz", "");
  }

  /**
   * Check that files in a directory can be read.
   * 
   * @param directory Directory where the files are (relative to Jmol-datafiles)
   * @param exts Comma separated list of extensions
   * @param extsZ Comma separated list of extensions (compressed files)
   */
  private void checkDirectory(String directory, String exts, String extsZ) {
    File dir = new File("../Jmol-datafiles", directory);
    
    String message = "";
    
    // Checking uncompressed files
    final String[] ext = exts.split("[,]");
    String[] files = dir.list(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        for (int i = 0; i < ext.length; i++) {
          if (name.endsWith("." + ext[i])) {
            return true;
          }
        }
        return false;
      }
      
    });
    for (int i = 0; i < files.length; i++) {
      try {
        String error = checkOpenFile(directory, files[i]);
        if (error != null) {
          message += error + "\n";
        }
      } catch (Exception e) {
        message += "Exception " + e.getClass().getName() + ": " + e.getMessage();
      }
    }
    
    // Checking compressed files
    final String[] extZ = extsZ.split("[,]");
    String[] filesZ = dir.list(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        for (int i = 0; i < extZ.length; i++) {
          if (name.endsWith("." + extZ[i])) {
            return true;
          }
        }
        return false;
      }
      
    });
    for (int i = 0; i < filesZ.length; i++) {
      try {
        String error = checkOpenFileGzip(directory, filesZ[i]);
        if (error != null) {
          message += error + "\n";
        }
      } catch (Exception e) {
        message += "Exception " + e.getClass().getName() + ": " + e.getMessage();
      }
    }
    
    // Checking error messages
    if (message.length() > 0) {
      System.out.flush();
      System.err.println(message);
      System.err.flush();
      fail(message);
    }
  }

  /**
   * Check that the file can be read
   * 
   * @param directory Directory where the file is (relative to Jmol-datafiles)
   * @param filename File name
   * @return Error message or null if OK
   */
  private String checkOpenFile(String directory, String filename) {
    
    // Open file
    JUnitLogger.setInformation(null);
    Object result = null;
    try {
      JUnitLogger.activateLogger();
      SmarterJmolAdapter adapter = new SmarterJmolAdapter(null);
      adapter.logger = new TestLogger(adapter);
      File file = new File(new File("../Jmol-datafiles", directory), filename);
      JUnitLogger.setInformation(file.getPath());
      InputStream iStream = new FileInputStream(file);
      BufferedInputStream biStream = new BufferedInputStream(iStream);
      BufferedReader bReader = new BufferedReader(new InputStreamReader(biStream));
      result = adapter.openBufferedReader(filename, bReader);
    } catch (Exception e) {
      return "checkFile (" + directory + "/" + filename + "): " + e.getMessage();
    }
    
    // Check result
    if (result == null) {
      return "checkFile (" + directory + "/" + filename + "): returns null";
    }
    if (result instanceof String) {
      return "checkFile (" + directory + "/" + filename + ") :" + result;
    }
    
    return null;
  }

  /**
   * Check that the file can be read
   * 
   * @param directory Directory where the file is (relative to Jmol-datafiles)
   * @param filename File name
   * @return Error message or null if OK
   */
  private String checkOpenFileGzip(String directory, String filename) {
    
    // Open file
    JUnitLogger.setInformation(null);
    Object result = null;
    try {
      SmarterJmolAdapter adapter = new SmarterJmolAdapter(null);
      adapter.logger = new TestLogger(adapter);
      File file = new File(new File("../Jmol-datafiles", directory), filename);
      JUnitLogger.setInformation(file.getPath());
      InputStream iStream = new FileInputStream(file);
      BufferedInputStream biStream = new BufferedInputStream(iStream);
      BufferedReader bReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(biStream)));
      result = adapter.openBufferedReader(filename, bReader);
    } catch (Exception e) {
      return "checkFile (" + directory + "/" + filename + "): " + e.getMessage();
    }
    
    // Check result
    if (result == null) {
      return "checkFile (" + directory + "/" + filename + "): returns null";
    }
    if (result instanceof String) {
      return "checkFile (" + directory + "/" + filename + ") :" + result;
    }
    
    result = null;
    System.gc();
    
    return null;
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    JUnitLogger.activateLogger();
    JUnitLogger.setInformation(null);
  }

  /**
   * Logger class to remove readers log from JUnit output
   */
  public class TestLogger extends JmolAdapter.Logger {

    public TestLogger(JmolAdapter adapter) {
      adapter.super();
    }

    public void log(String str1) {
      //
    }

    public void log(String str1, Object obj1) {
      //
    }

    public void log(String str1, Object obj1, Object obj2) {
      //
    }
  }
}
