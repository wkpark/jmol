/* $RCSfile$
 * $Author: nicove $
 * $Date: 2005-08-27 11:16:47 +0200 $
 * $Revision: 3966 $
 *
 * Copyright (C) 2007  The Jmol Development Team
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

package org.jmol.fah.checkxyz;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;

/**
 * Checking for missing XYZ files for http://www.jmol.org/fah
 *
 */
public class Check implements ActionListener {

  private boolean forceConfig = false;
  private boolean availableFilesDownloaded = false;
  private Configuration configuration = new Configuration();

  private FileFilter fileFilter = null;
  private File availableProjects = null;
  private Vector existingProjects = new Vector();
  private boolean showSentProjects = false;
  private Vector sentProjects = new Vector();

  /**
   * Constructor. 
   */
  public Check() {
    fileFilter = new FileFilter() {
      public boolean accept(File file) {
        if (file == null) {
          return false;
        }
        if (file.isDirectory()) {
          return true;
        }
        if (file.isFile()) {
          return file.getName().endsWith(".xyz");
        }
        return false;
      }
    };
    File configDirectory = new File(new File(System.getProperty("user.home")), ".jmol");
    configDirectory.mkdirs();
    availableProjects = new File(configDirectory, "availableProjects");
  }

  /**
   * Check for missing XYZ files.
   * 
   * @param args Command line arguments
   */
  public void process(String[] args) {
    // Process command line arguments
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        if ("-config".equalsIgnoreCase(args[i])) {
          forceConfig = true;
        }
      }
    }
    
    // Once configured, process for the checking
    process();
  }

  /**
   * Check for missing XYZ files.
   */
  private void process() {
    configuration.loadConfiguration();
    if (forceConfig || !configuration.isConfigured()) {
      showSentProjects = true;
      configure();
    } else {
      processDirectories();
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    processDirectories();
  }

  /**
   * Process all directories for XYZ files. 
   */
  public void processDirectories() {
    if (configuration.getDirectories() != null) {
      Iterator iter = configuration.getDirectories().iterator();
      while (iter.hasNext()) {
        processDirectory(iter.next().toString());
      }
    }
    String message = null;
    if ((sentProjects != null) && (!sentProjects.isEmpty())) {
      message = "" + sentProjects.size() + " sent (";
      for (int i = 0; i < sentProjects.size(); i++) {
        if (i != 0) {
          message += ", ";
        }
        message += sentProjects.get(i).toString();
      }
    } else {
      message = "No new projets found";
    }
    if (showSentProjects) {
      JOptionPane.showMessageDialog(
          null, message, "Result", JOptionPane.INFORMATION_MESSAGE);
      System.exit(0);
    } else {
      System.out.println(message);
    }
  }

  /**
   * Process a directory for XYZ files.
   * 
   * @param directory Directory to process.
   */
  private void processDirectory(String directory) {
    if (directory == null) {
      return;
    }
    processDirectory(new File(directory));
  }
  private void processDirectory(File directory) {
    if ((directory == null) || (!directory.isDirectory())) {
      return;
    }
    File[] files = directory.listFiles(fileFilter);
    if (files == null) {
      return;
    }
    System.out.println("Checking directory " + directory.getAbsolutePath());
    for (int i = 0; i < files.length; i++) {
      if (files[i].isFile()) {
        processFile(files[i]);
      }
    }
    for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory()) {
        processDirectory(files[i]);
      }
    }
  }

  /**
   * Check a XYZ file.
   * 
   * @param file XYZ file.
   */
  private void processFile(File file) {
    if ((file == null) || (!file.isFile())) {
      return;
    }
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      String line = reader.readLine();
      if (line == null) {
        return;
      }
      line = line.trim();

      // Removing the number of atoms
      int index = 0;
      while ((index < line.length()) && (Character.isDigit(line.charAt(index)))) {
        index++;
      }
      if ((index == 0) || (index >= line.length())) {
        return;
      }
      line = line.substring(index);
      
      // Removing the space characters
      index = 0;
      while ((index < line.length()) && (Character.isWhitespace(line.charAt(index)))) {
        index++;
      }
      if ((index == 0) || (index >= line.length())) {
        return;
      }
      line = line.substring(index);
      
      // Removing the "p"
      if ((line.length() == 0) || (!line.substring(0, 1).equalsIgnoreCase("p"))) {
        return;
      }
      line = line.substring(1);

      // Extracting the project number
      index = 0;
      while ((index < line.length()) && (Character.isDigit(line.charAt(index)))) {
        index++;
      }
      if ((index == 0) || (index >= line.length())) {
        return;
      }
      if (line.charAt(index) != '_') {
        return;
      }
      line = line.substring(0, index);

      processProjectNumber(file, line);
    } catch (FileNotFoundException e) {
      //
    } catch (IOException e) {
      //
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          //
        }
      }
    }
  }

  /**
   * Check a XYZ file.
   * 
   * @param file XYZ file.
   * @param project Project identifier.
   */
  private void processProjectNumber(File file, String project) {
    if (configuration.hasBeenSent(project)) {
      return;
    }
    if (!availableProjects.exists()) {
      downloadAvailableFiles();
    }
    updateExistingProjects();
    if (existingProjects.contains(project)) {
      return;
    }
    if (!availableFilesDownloaded) {
      downloadAvailableFiles();
      updateExistingProjects();
      if (existingProjects.contains(project)) {
        return;
      }
    }
    try {
      System.out.println(" Found one file for project " + project);
      MailSender sender = new MailSender(configuration, project, file, false);
      sender.sendMail();
      configuration.addSentFile(project);
    } catch (Throwable e) {
      outputError("Sending new file", e);
    }
  }

  /**
   * Update the list of existing projects in memory.
   */
  private void updateExistingProjects() {
    if ((!availableProjects.exists()) || (!existingProjects.isEmpty())) {
      return;
    }
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(availableProjects));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        existingProjects.add(line);
      }
    } catch (FileNotFoundException e) {
      outputError("Reading local available projects", e);
    } catch (IOException e) {
      outputError("Reading local available projects", e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          // Nothing
        }
      }
    }
  }

  /**
   * Download the list of available files from http://www.jmol.org/
   */
  private void downloadAvailableFiles() {
    if (availableFilesDownloaded) {
      return;
    }
    OutputStream os = null;
    InputStream is = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(availableProjects, false));
      URL url = new URL("http://www.jmol.org/fah/availableProjects.txt");
      is = new BufferedInputStream(url.openStream());
      int read = -1;
      while ((read = is.read()) != -1) {
        os.write(read);
      }
      availableFilesDownloaded = true;
      existingProjects.clear();
    } catch (MalformedURLException e) {
      outputError("Downloading available files", e);
    } catch (IOException e) {
      outputError("Downloading available files", e);
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          outputError("Closing OutputStream", e);
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          outputError("Closing InputStream", e);
        }
      }
    }
  }

  /**
   * Trace errors.
   * 
   * @param msg Message.
   * @param e Exception.
   */
  private void outputError(String msg, Throwable e) {
    if ((msg == null) && (e == null)) {
      return;
    }
    String text =
      ((msg != null) ? (msg + ": ") : "") +
      ((e != null) ? (e.getClass().getName() + " - " + e.getMessage()) : "");
    System.err.println(text);
  }

  /**
   * Update configuration. 
   */
  private void configure() {
    ConfigurationWindow window = new ConfigurationWindow(configuration, this);
    window.setVisible(true);
  }

  /**
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    Check check = new Check();
    check.process(args);
  }

}
