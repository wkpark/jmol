/*
 * JUnit TestCase for running various Jmol scripts
 */

package org.jmol.api;

import java.io.File;
import java.io.FilenameFilter;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.util.JUnitLogger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestSuite for running various Jmol scripts. 
 */
public class TestScripts extends TestSuite {

  public TestScripts() {
    super();
  }

  public TestScripts(Class theClass, String name) {
    super(theClass, name);
  }

  public TestScripts(Class theClass) {
    super(theClass);
  }

  public TestScripts(String name) {
    super(name);
  }

  /**
   * @return Test suite containing tests for all scripts.
   */
  public static Test suite() {
    TestScripts result = new TestScripts("Test for scripts");
    String datafileDirectory = System.getProperty(
        "test.datafile.script.directory",
        "../Jmol-datafiles/tests/scripts");
    TestScripts resultCheck = new TestScripts("Test for checking scripts");
    resultCheck.addDirectory(datafileDirectory + "/check", true);
    if (resultCheck.countTestCases() > 0) {
      result.addTest(resultCheck);
    }
    TestScripts resultRun = new TestScripts("Test for running scripts");
    resultRun.addDirectory(datafileDirectory + "/run", false);
    if (resultRun.countTestCases() > 0) {
      result.addTest(resultRun);
    }
    return result;
  }

  /**
   * Add tests for each script in a directory.
   * 
   * @param directory Directory where the files are
   * @param checkOnly Flag for checking syntax only
   */
  private void addDirectory(String directory, boolean checkOnly) {

    // Checking files
    File dir = new File(directory);
    String[] files = dir.list(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        if (name.endsWith(".spt")) {
          return true;
        }
        return false;
      }

    });
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        addFile(directory, files[i], checkOnly);
      }
    }

    // Checking sub directories
    String[] dirs = dir.list(new FilenameFilter() {

      public boolean accept(File dir, String name) {
        File file = new File(dir, name);
        return file.isDirectory();
      }

    });
    if (dirs != null) {
      for (int i = 0; i < files.length; i++) {
        addDirectory(
            new File(directory, files[i]).getAbsolutePath(),
            checkOnly);
      }
    }
  }

  /**
   * Add test for a file.
   * 
   * @param directory Directory where the files are
   * @param filename File name
   * @param checkOnly Flag for checking syntax only
   */
  private void addFile(String directory,
                       String filename,
                       boolean checkOnly) {

    File file = new File(directory, filename);
    Test test = new TestScriptsImpl(file, checkOnly);
    addTest(test);
  }
}

/**
 * Implementation of a test running only one script. 
 */
class TestScriptsImpl extends TestCase {

  private File file;
  private boolean checkOnly;

  public TestScriptsImpl(File file, boolean checkOnly) {
    super("testFile");
    this.file = file;
    this.checkOnly = checkOnly;
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#runTest()
   */
  public void runTest() throws Throwable {
    testScript();
  }

  /**
   * Tests reading of one file.
   */
  public void testScript() {
    JUnitLogger.setInformation(file.getPath());

    SmarterJmolAdapter adapter = new SmarterJmolAdapter();
    JmolViewer viewer = JmolViewer.allocateViewer(null, adapter);
    if (checkOnly) {
      viewer.setAppletContext("", null, null, "-n -c "); // set no display; checkOnly
    } else {
      viewer.setAppletContext("", null, null, "-n "); // set no display
    }
    String s = viewer.evalFile(file.getPath() + " -nowait");
    assertNull("Error in script [" + file.getPath() + ":\n" + s, s);
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
  }
}
