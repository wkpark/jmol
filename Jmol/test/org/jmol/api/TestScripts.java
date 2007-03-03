/*
 * JUnit TestCase for running various Jmol scripts
 */

package org.jmol.api;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.util.JUnitLogger;
import org.jmol.util.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestSuite for running various Jmol scripts. 
 */
public class TestScripts extends TestSuite {

  /**
   * @return Test suite containing tests for all scripts.
   */
  public static Test suite() {
    TestScripts result = new TestScripts();
    String datafileDirectory = System.getProperty(
        "test.datafile.script.directory",
        "../Jmol-datafiles/tests/scripts");
    result.addDirectory(datafileDirectory);
    return result;
  }

  /**
   * Add tests for each script in a directory.
   * 
   * @param directory Directory where the files are
   */
  private void addDirectory(String directory) {

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
        addFile(directory, files[i]);
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
      for (int i = 0; i < dirs.length; i++) {
        addDirectory(new File(directory, files[i]).getAbsolutePath());
      }
    }
  }

  /**
   * Add test for a file.
   * 
   * @param directory Directory where the files are
   * @param filename File name
   */
  private void addFile(String directory,
                       String filename) {

    File file = new File(directory, filename);
    Test test = new TestScriptsImpl(file);
    addTest(test);
  }
}

/**
 * Implementation of a test running only one script. 
 */
class TestScriptsImpl extends TestCase {

  private File file;

  public TestScriptsImpl(File file) {
    super("testFile");
    this.file = file;
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
    
    // TODO
    JPanel panel = new JPanel();
    SmarterJmolAdapter adapter = new SmarterJmolAdapter();
    JmolViewer viewer = JmolViewer.allocateViewer(panel, adapter);
    viewer.evalFile(file.getPath());
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
    Logger.setActiveLevel(Logger.LEVEL_DEBUG, true);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    JUnitLogger.setInformation(null);
    Logger.setActiveLevel(Logger.LEVEL_DEBUG, false);
    file = null;
  }
}
