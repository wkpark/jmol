
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class RecentFilesDialog extends JDialog
    implements java.awt.event.WindowListener, java.awt.event.ActionListener {

  private boolean ready = false;
  private String fileName = null;
  private String fileType = null;
  private static final int MAX_FILES = 10;
  private JButton okButton;
  private JButton cancelButton;
  private String[] files = new String[MAX_FILES];
  private String[] fileTypes = new String[MAX_FILES];
  private JList fileList;
  java.util.Properties props;

  /** Creates a hidden recent files dialog **/
  public RecentFilesDialog(java.awt.Frame boss) {

    super(boss, JmolResourceHandler.getInstance()
        .getString("RecentFiles.windowTitle"), true);
    props = new java.util.Properties();
    getFiles();
    getContentPane().setLayout(new java.awt.BorderLayout());
    JPanel buttonPanel = new JPanel();
    okButton =
        new JButton(JmolResourceHandler.getInstance()
          .getString("RecentFiles.okLabel"));
    okButton.addActionListener(this);
    buttonPanel.add(okButton);
    cancelButton =
        new JButton(JmolResourceHandler.getInstance()
          .getString("RecentFiles.cancelLabel"));
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton);
    getContentPane().add("South", buttonPanel);
    fileList = new JList(files);
    fileList.setSelectedIndex(0);
    fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    getContentPane().add("Center", fileList);
    setLocationRelativeTo(boss);
    pack();
  }

  private void getFiles() {

    props = Jmol.getHistoryFile().getProperties();
    for (int i = 0; i < MAX_FILES; i++) {
      files[i] = props.getProperty("recentFilesFile" + i);
      fileTypes[i] = props.getProperty("recentFilesType" + i);
    }
  }

  /**
   * Adds this file and type to the history. If already present,
   * this file is premoted to the top position.
   */
  public void addFile(String name, String type) {

    int currentPosition = -1;

    //Find if file is already present
    for (int i = 0; i < MAX_FILES; i++) {
      if ((files[i] != null) && files[i].equals(name)) {
        currentPosition = i;
      }
    }

    //No change so cope out
    if (currentPosition == 0) {
      return;
    }

    //present so shift files below current position up one, removing current position
    if (currentPosition > 0) {
      for (int i = currentPosition; i < MAX_FILES - 1; i++) {
        files[i] = files[i + 1];
        fileTypes[i] = fileTypes[i + 1];
      }
    }

    // Shift everything down one
    for (int j = MAX_FILES - 2; j >= 0; j--) {
      files[j + 1] = files[j];
      fileTypes[j + 1] = fileTypes[j];
    }

    //Insert file at head of list
    files[0] = name;
    fileTypes[0] = type;
    fileList.setListData(files);
    fileList.setSelectedIndex(0);
    pack();
    saveList();
  }

  /** Saves the list to the history file. Called automaticaly when files are added **/
  public void saveList() {

    for (int i = 0; i < 10; i++) {
      if (files[i] != null) {
        props.setProperty("recentFilesFile" + i, files[i]);
        props.setProperty("recentFilesType" + i, fileTypes[i]);
      }
    }

    Jmol.getHistoryFile().addProperties(props);
  }

  /**
   *   @returns String The name of the file picked or null if the action was aborted.
  **/
  public String getFile() {
    return fileName;
  }

  /**
          @returns String The type of the file picked or null if the action was aborted.
  **/
  public String getFileType() {
    return fileType;
  }

  public void windowClosing(java.awt.event.WindowEvent e) {
    cancel();
    close();
  }

  void cancel() {
    fileName = null;
    fileType = null;
  }

  void close() {
    ready = true;
    hide();
  }

  public void actionPerformed(java.awt.event.ActionEvent e) {

    if (e.getSource() == okButton) {
      int fileIndex = fileList.getSelectedIndex();
      if (fileIndex < files.length) {
        fileName = files[fileIndex];
        fileType = fileTypes[fileIndex];
        close();
      }
    } else if (e.getSource() == cancelButton) {
      cancel();
      close();
    }
  }

  public void windowClosed(java.awt.event.WindowEvent e) {
  }

  public void windowOpened(java.awt.event.WindowEvent e) {
    ready = false;
  }

  public void windowIconified(java.awt.event.WindowEvent e) {
  }

  public void windowDeiconified(java.awt.event.WindowEvent e) {
  }

  public void windowActivated(java.awt.event.WindowEvent e) {
  }

  public void windowDeactivated(java.awt.event.WindowEvent e) {
  }

}
