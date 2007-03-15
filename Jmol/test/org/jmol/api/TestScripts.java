/*
 * JUnit TestCase for running various Jmol scripts
 */

package org.jmol.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Vector;

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
    boolean performance = Boolean.getBoolean("test.performance");
    TestScripts result = new TestScripts("Test for scripts");
    String datafileDirectory = System.getProperty(
        "test.datafile.script.directory",
        "../Jmol-datafiles/tests/scripts");
    TestScripts resultCheck = new TestScripts("Test for checking scripts");
    resultCheck.addDirectory(datafileDirectory + "/check", true, performance);
    if (resultCheck.countTestCases() > 0) {
      result.addTest(resultCheck);
    }
    TestScripts resultCheckPerformance = new TestScripts("Test for checking scripts with performance testing");
    resultCheckPerformance.addDirectory(datafileDirectory + "/check_performance", true, true);
    if (resultCheckPerformance.countTestCases() > 0) {
      result.addTest(resultCheckPerformance);
    }
    TestScripts resultRun = new TestScripts("Test for running scripts");
    resultRun.addDirectory(datafileDirectory + "/run", false, performance);
    if (resultRun.countTestCases() > 0) {
      result.addTest(resultRun);
    }
    TestScripts resultRunPerformance = new TestScripts("Test for running scripts with performance testing");
    resultRunPerformance.addDirectory(datafileDirectory + "/run_performance", false, true);
    if (resultRunPerformance.countTestCases() > 0) {
      result.addTest(resultRunPerformance);
    }
    return result;
  }

  /**
   * Add tests for each script in a directory.
   * 
   * @param directory Directory where the files are
   * @param checkOnly Flag for checking syntax only
   * @param performance Flag for checking performance
   */
  private void addDirectory(String directory,
                            boolean checkOnly,
                            boolean performance) {

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
        addFile(directory, files[i], checkOnly, performance);
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
            checkOnly, performance);
      }
    }
  }

  /**
   * Add test for a file.
   * 
   * @param directory Directory where the files are
   * @param filename File name
   * @param checkOnly Flag for checking syntax only
   * @param performance Flag for checking performance
   */
  private void addFile(String directory,
                       String filename,
                       boolean checkOnly,
                       boolean performance) {

    File file = new File(directory, filename);
    Test test = new TestScriptsImpl(file, checkOnly, performance);
    addTest(test);
  }
}

/**
 * Implementation of a test running only one script. 
 */
class TestScriptsImpl extends TestCase {

  private File file;
  private boolean checkOnly;
  private boolean performance;

  public TestScriptsImpl(File file, boolean checkOnly, boolean performance) {
    super("testFile");
    this.file = file;
    this.checkOnly = checkOnly;
    this.performance = performance;
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
      viewer.setAppletContext("", null, null, "-n -C "); // set no display; checkOnly; no file opening
    } else {
      viewer.setAppletContext("", null, null, "-n "); // set no display
    }
    if (performance) {
      int lineNum = 0;
      try {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        long beginFull = System.currentTimeMillis();
        while ((line = reader.readLine()) != null) {
          lineNum++;
          long begin = System.currentTimeMillis();
          Vector info = (Vector) viewer.scriptWaitStatus(line, "scriptTerminated");
          long end = System.currentTimeMillis();
          if ((info != null) && (info.size() > 0)) {
            String error = info.get(0).toString();
            /*if (info.get(0) instanceof Vector) {
              Vector vector = (Vector) info.get(0);
              if (vector.size() > 0) {
                if (vector.get(0) instanceof Vector) {
                  vector = (Vector) vector.get(0);
                  error = vector.get(vector.size() - 1).toString();
                }
              }
            }*/
            fail(
                "Error in script [" + file.getPath() + "] " +
                "at line " + lineNum + " (" + line + "):\n" +
                error);
          }
          if ((end - begin) > 0) {
            System.err.println("Time to execute [" + line + "]: " + (end - begin) + " milliseconds");
          }
        }
        long endFull = System.currentTimeMillis();
        System.err.println("Time to execute script [" + file.getPath() + "]: " + (endFull - beginFull) + " milliseconds");
      } catch (FileNotFoundException e) {
        fail("File " + file.getPath() + " not found");
      } catch (IOException e) {
        fail("Error reading line " + lineNum + " of " + file.getPath());
      }
    } else {
      String s = viewer.evalFile(file.getPath() + " -nowait");
      assertNull("Error in script [" + file.getPath() + ":\n" + s, s);
    }
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
