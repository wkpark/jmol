/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.app;

import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * Manages a list of recently opened files.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class RecentFilesDialog extends JDialog implements ActionListener,
    WindowListener, PropertyChangeListener {

  private boolean ready = false;
  private String fileName = null;
  private static final int MAX_FILES = 10;
  private JButton okButton;
  private JButton cancelButton;
  private String[] files = new String[MAX_FILES];
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
    }
  }

  /**
   * Adds this file to the history. If already present,
   * this file is premoted to the top position.
   */
  public void addFile(String name) {

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
      }
    }

    // Shift everything down one
    for (int j = MAX_FILES - 2; j >= 0; j--) {
      files[j + 1] = files[j];
    }

    //Insert file at head of list
    files[0] = name;
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
      }
    }

    Jmol.getHistoryFile().addProperties(props);
  }

  /**
   *   @return String The name of the file picked or null if the action was aborted.
  **/
  public String getFile() {
    return fileName;
  }

  public void windowClosing(java.awt.event.WindowEvent e) {
    cancel();
    close();
  }

  void cancel() {
    fileName = null;
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

  /**
   * Listens for opening files and adds them to the recent files list.
   *
   * @param event a property change event
   */
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(Jmol.openFileProperty)) {
      File newFile = (File) event.getNewValue();
      addFile(newFile.toString());
    }
  }

  
}
