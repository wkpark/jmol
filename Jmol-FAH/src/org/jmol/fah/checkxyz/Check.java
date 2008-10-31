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
  private boolean availableProjectsDownloaded = false;
  private boolean availableAmbersDownloaded = false;
  private Configuration configuration = new Configuration();

  private FileFilter projectFilter = null;
  private File availableProjects = null;
  private File availableAmbers = null;
  private Vector existingProjects = new Vector();
  private Vector existingAmbers = new Vector();
  private boolean showSentProjects = false;
  private Vector sentProjects = new Vector();
  private Vector sentAmbers = new Vector();

  /**
   * Constructor. 
   */
  public Check() {
    projectFilter = new FileFilter() {
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
    availableAmbers = new File(configDirectory, "availableAmbers");
  }

  /**
   * Check for missing files.
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
   * Check for missing files.
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
   * Process all directories for files. 
   */
  public void processDirectories() {
    if (configuration.getDirectories() != null) {
      Iterator iter = configuration.getDirectories().iterator();
      while (iter.hasNext()) {
        processDirectory(iter.next().toString());
      }
    }
    StringBuffer message = new StringBuffer();
    if ((sentProjects != null) && (!sentProjects.isEmpty())) {
      message.append(sentProjects.size());
      message.append(" .xyz files sent (");
      for (int i = 0; i < sentProjects.size(); i++) {
        if (i != 0) {
          message.append(", ");
        }
        message.append(sentProjects.get(i).toString());
      }
    } else {
      message.append("No new .xyz files found");
    }
    message.append("\n");
    if ((sentAmbers != null) && (!sentAmbers.isEmpty())) {
      message.append(sentAmbers.size());
      message.append(" .top/.trj files sent (");
      for (int i = 0; i < sentAmbers.size(); i++) {
        if (i != 0) {
          message.append(", ");
        }
        message.append(sentAmbers.get(i).toString());
      }
    } else {
      message.append("No new .top/.trj files found");
    }
    if (showSentProjects) {
      JOptionPane.showMessageDialog(
          null, message.toString(), "Result", JOptionPane.INFORMATION_MESSAGE);
      System.exit(0);
    } else {
      System.out.println(message);
    }
  }

  /**
   * Process a directory for files.
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
    File[] files = directory.listFiles(projectFilter);
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
    System.out.print("    File " + file.getName() + " : ");
    String project = extractProjectNumber(file);
    if (project == null) {
      System.out.print("Unable to find project number");
    } else {
      System.out.print("Project n°" + project + " -> ");
      processProjectNumber(file, project);
    }
    System.out.println();
  }

  /**
   * Analyze the <code>file</code> to find the project number.
   * 
   * @param file File.
   * @return Project number.
   */
  private String extractProjectNumber(File file) {
    String project;
    project = extractProjectNumberFromContent(file);
    if (project != null) {
      return project;
    }
    project = extractProjectNumberFromName(file);
    if (project != null) {
      return project;
    }
    return null;
  }

  /**
   * Analyze the <code>file</code> name to find the project number.
   * 
   * @param file File.
   * @return Project number.
   */
  private String extractProjectNumberFromName(File file) {
    String fileName = file.getName();

    // Removing 'p'
    if ((fileName.length() > 0) && (fileName.substring(0, 1).equalsIgnoreCase("p"))) {
      fileName = fileName.substring(1);
    }

    // Extracting the project number
    int index = 0;
    while ((index < fileName.length()) && (Character.isDigit(fileName.charAt(index)))) {
      index++;
    }
    if ((index == 0) || (index >= fileName.length())) {
      return null;
    }
    if ((fileName.charAt(index) != '_') && (fileName.charAt(index) != '.')) {
      return null;
    }
    return fileName.substring(0, index);
  }

  /**
   * Analyze the <code>file</code> content to find the project number.
   * 
   * @param file File.
   * @return Project number.
   */
  private String extractProjectNumberFromContent(File file) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      String line = reader.readLine();
      if (line == null) {
        return null;
      }
      line = line.trim();

      // Removing the number of atoms
      int index = 0;
      while ((index < line.length()) && (Character.isDigit(line.charAt(index)))) {
        index++;
      }
      if ((index == 0) || (index >= line.length())) {
        return null;
      }
      line = line.substring(index);
      
      // Removing the space characters
      index = 0;
      while ((index < line.length()) && (Character.isWhitespace(line.charAt(index)))) {
        index++;
      }
      if ((index == 0) || (index >= line.length())) {
        return null;
      }
      line = line.substring(index);
      
      // Removing the "p"
      if ((line.length() == 0) || (!line.substring(0, 1).equalsIgnoreCase("p"))) {
        return null;
      }
      line = line.substring(1);

      // Extracting the project number
      index = 0;
      while ((index < line.length()) && (Character.isDigit(line.charAt(index)))) {
        index++;
      }
      if ((index == 0) || (index >= line.length())) {
        return null;
      }
      if (line.charAt(index) != '_') {
        return null;
      }
      return line.substring(0, index);
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
    return null;
  }

  /**
   * Check a XYZ file.
   * 
   * @param file XYZ file.
   * @param project Project identifier.
   */
  private void processProjectNumber(File file, String project) {
    if (configuration.hasBeenSent(project)) {
      System.out.print("Already sent by you");
      return;
    }
    if ((!availableProjects.exists()) || (!availableAmbersDownloaded)) {
      downloadAvailableFiles();
    }
    updateExistingProjects();
    if (existingProjects.contains(project)) {
      System.out.print("Project available on Jmol website");
      return;
    }
    if ((!availableProjectsDownloaded) || (!availableAmbersDownloaded)) {
      downloadAvailableFiles();
      updateExistingProjects();
      if (existingProjects.contains(project)) {
        System.out.print("Project available on Jmol website");
        return;
      }
    }
    try {
      System.out.print("Found new project :)");
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
    if (availableProjects.exists() && existingProjects.isEmpty()) {
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
    if (availableAmbers.exists() && existingAmbers.isEmpty()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(availableAmbers));
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          existingAmbers.add(line);
        }
      } catch (FileNotFoundException e) {
        outputError("Reading local available ambers", e);
      } catch (IOException e) {
        outputError("Reading local available ambers", e);
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
  }

  /**
   * Download a file locally.
   * 
   * @param inputFile Input file.
   * @param outputFile Output file.
   * @return Flag indicating if the download was successful.
   */
  private boolean downloadFile(String inputFile, File outputFile) {
    OutputStream os = null;
    InputStream is = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(outputFile, false));
      URL url = new URL(inputFile);
      is = new BufferedInputStream(url.openStream());
      int read = -1;
      while ((read = is.read()) != -1) {
        os.write(read);
      }
      return true;
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
    return false;
  }

  /**
   * Download the list of available files from http://www.jmol.org/
   */
  private void downloadAvailableFiles() {
    if (!availableProjectsDownloaded) {
      availableProjectsDownloaded = downloadFile("http://www.jmol.org/fah/availableProjects.txt", availableProjects);
      if (availableProjectsDownloaded) {
        existingProjects.clear();
      }
    }
    if (!availableAmbersDownloaded) {
      availableAmbersDownloaded = downloadFile("http://www.jmol.org/fah/availableAmber.txt", availableAmbers);
      if (availableAmbersDownloaded) {
        existingAmbers.clear();
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
