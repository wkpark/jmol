/*
 * JUnit TestCase for the Smarter Adapter
 */

package org.jmol.adapter.smarter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.jmol.util.JUnitLogger;
import org.jmol.util.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestSmarterJmolAdapter extends TestSuite {

  private String datafileDirectory = "../Jmol-datafiles";

  /**
   * @return Test suite containing tests for all files
   */
  public static Test suite() {
    TestSmarterJmolAdapter result = new TestSmarterJmolAdapter();
    result.datafileDirectory = System.getProperty("test.datafile.directory", result.datafileDirectory);
    //result.addDirectory(false, "abint", "out");
    //result.addDirectory(false, "aces2", "dat");
    //result.addDirectory(false, "aces2", "out");
    result.addDirectory(false, "adf", "adf", "Adf");
    result.addDirectory(false, "aminoacids", "mol");
    result.addDirectory(false, "aminoacids", "pdb");
    result.addDirectory(false, "animations", "cml", "Xml");
    result.addDirectory(false, "animations", "pdb");
    result.addDirectory(true,  "animations", "pdb.gz");
    result.addDirectory(false, "animations", "xyz");
    result.addDirectory(false, "cif", "cif");
    result.addDirectory(false, "c3xml", "c3xml", "Xml");
    result.addDirectory(false, "cml", "cml", "Xml");
    result.addDirectory(false, "crystals", "mol");
    result.addDirectory(false, "crystals", "pdb");
    result.addDirectory(true,  "cube", "cub.gz");
    result.addDirectory(true,  "cube", "cube.gz");
    result.addDirectory(false, "folding", "xyz");
    result.addDirectory(true,  "folding", "xyz.gz");
    result.addDirectory(false, "../Jmol-FAH/projects", "xyz");
    result.addDirectory(true,  "../Jmol-FAH/projects", "xyz.gz");
    result.addDirectory(false, "gamess", "log");
    result.addDirectory(false, "gamess", "out");
    result.addDirectory(false, "gaussian", "log");
    result.addDirectory(false, "gaussian", "out");
    result.addDirectory(false, "ghemical", "gpr", "GhemicalMM");
    result.addDirectory(false, "hin", "hin");
    result.addDirectory(false, "jaguar", "out");
    result.addDirectory(false, "modifiedGroups", "cif");
    result.addDirectory(false, "modifiedGroups", "pdb");
    result.addDirectory(false, "mol", "mol");
    result.addDirectory(false, "mol", "sdf");
    result.addDirectory(false, "mol2", "mol2");
    result.addDirectory(false, "molpro", "xml");
    result.addDirectory(false, "mopac", "out");
    result.addDirectory(false, "nwchem", "nwo");
    result.addDirectory(false, "pdb", "pdb");
    result.addDirectory(true,  "pdb", "pdb.gz");
    // result.pmesh files are not molecular data files
    result.addDirectory(false, "psi3", "out");
    result.addDirectory(false, "qchem", "out");
    result.addDirectory(false, "shelx", "res");
    result.addDirectory(false, "spartan", "smol", "SpartanSmol");
    result.addDirectory(false, "spartan", "txt", "Spartan");
    result.addDirectory(false, "sparchive", "sparchive", "Spartan");
    result.addDirectory(false, "v3000", "sdf");
    result.addDirectory(false, "xyz", "xyz");
    return result;
  }

  /**
   * Add tests for each file in a directory.
   * 
   * @param gzipped Compressed file ?
   * @param directory Directory where the files are (relative to Jmol-datafiles)
   * @param ext Extension
   */
  private void addDirectory(boolean gzipped, String directory, String ext) {
    addDirectory(gzipped, directory, ext, null);
  }

  /**
   * Add tests for each file in a directory.
   * 
   * @param gzipped Compressed file ?
   * @param directory Directory where the files are (relative to Jmol-datafiles)
   * @param ext Extension
   * @param typeAllowed Allowed file type
   */
  private void addDirectory(boolean gzipped,
                            String directory,
                            final String ext,
                            String typeAllowed) {

    // Checking files
    File dir = new File(datafileDirectory, directory);
    String[] files = dir.list(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        if (name.endsWith("." + ext)) {
          return true;
        }
        return false;
      }

    });
    if (files == null) {
      Logger.warn("No files in directory [" + directory + "] for extension [" + ext + "]");
    } else {
      for (int i = 0; i < files.length; i++) {
        addFile(gzipped, directory, files[i], typeAllowed);
      }
    }
  }

  /**
   * Add test for a file.
   * 
   * @param gzipped Compressed file ?
   * @param directory Directory where the files are (relative to Jmol-datafiles)
   * @param filename File name
   * @param typeAllowed string that must contain the determined file type
   */
  private void addFile(boolean gzipped,
                       String directory,
                       String filename,
                       String typeAllowed) {

    File file = new File(new File(datafileDirectory, directory), filename);
    Test test = new TestSmarterJmolAdapterImpl(file, gzipped, typeAllowed);
    addTest(test);
  }
}

/**
 * Implementation of a test reading only one file. 
 */
class TestSmarterJmolAdapterImpl extends TestCase {

  private File file;
  private boolean gzipped;
  private String typeAllowed;

  public TestSmarterJmolAdapterImpl(File file, boolean gzipped, String typeAllowed) {
    super("testFile");
    this.file = file;
    this.gzipped = gzipped;
    this.typeAllowed = typeAllowed;
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#runTest()
   */
  public void runTest() throws Throwable {
    testFile();
  }

  /**
   * Tests reading of one file.
   * 
   *  @throws FileNotFoundException
   *  @throws IOException
   */
  public void testFile() throws FileNotFoundException, IOException {
    JUnitLogger.setInformation(file.getPath());
    InputStream iStream = new FileInputStream(file);
    iStream = new BufferedInputStream(iStream);
    if (gzipped) {
      iStream = new GZIPInputStream(iStream);
    }
    BufferedReader bReader = new BufferedReader(new InputStreamReader(iStream));
    SmarterJmolAdapter adapter = new SmarterJmolAdapter();
    if (typeAllowed != null) {
      String fileType = adapter.getFileTypeName(bReader);
      if (!typeAllowed.equals(fileType)) {
        fail("Wrong type for " + file.getPath() + ": " + fileType + " instead of " + typeAllowed);
      }
    }
    Object result = adapter.openBufferedReader(file.getName(), bReader);
    assertNotNull("Nothing read for " + file.getPath(), result);
    assertFalse("Error returned for " + file.getPath() + ": " + result, result instanceof String);
    assertTrue("Not an AtomSetCollection for " + file.getPath(), result instanceof AtomSetCollection);
    AtomSetCollection collection = (AtomSetCollection) result;
    assertTrue("No atoms loaded for " + file.getPath(), collection.atomCount > 0);
    bReader.close();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#getName()
   */
  public String getName() {
    if (file != null) {
      return super.getName() + " [" + file.getPath() + "]";
    }
    return super.getName();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    JUnitLogger.activateLogger();
    JUnitLogger.setInformation(null);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    JUnitLogger.setInformation(null);
    file = null;
    typeAllowed = null;
  }
}
