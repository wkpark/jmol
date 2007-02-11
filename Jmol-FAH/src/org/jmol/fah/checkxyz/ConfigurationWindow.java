/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.fah.checkxyz;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;


/**
 * Configuration Window.
 */
public class ConfigurationWindow extends JDialog implements ActionListener {

  private Configuration configuration = null;

  private JTextField textUser = null;
  private JTextField textMailServer = null;
  private JTextField textUserMail = null;
  private JTextField textUserLogin = null;
  private JTextField textUserPassword = null;
  private JList listDirectories = null;
  private DefaultListModel listModel = null;
  private JButton buttonAddDirectories = null;
  private JButton buttonRemoveDirectories = null;
  private JButton buttonTestMail = null;

  private static final String actionAddDirectory = "AddDirectory";
  private static final String actionRemoveDirectory = "RemoveDirectory";
  private static final String actionOk = "Ok";
  private static final String actionCancel = "Cancel";
  private static final String actionTestMail = "TestMail";

  /**
   * Constructor.
   * 
   * @param config Configuration.
   */
  public ConfigurationWindow(Configuration config) {
    super((JFrame) null, true);
    configuration = config;
    createWindowContent();
  }

  /**
   * Create the window components.
   */
  private void createWindowContent() {

    // Various settings
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setTitle("Current.xyz files for Jmol");

    // Layout
    BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
    getContentPane().setLayout(layout);

    // Preferences component
    JComponent preferences = createPreferencesComponent();
    getContentPane().add(preferences);

    // Contributions
    if ((configuration.getSentFiles() != null) &&
        (!configuration.getSentFiles().isEmpty())) {
      StringBuffer buffer = new StringBuffer("You have already sent the following files:");
      Iterator iter = configuration.getSentFiles().iterator();
      int num = 0;
      while (iter.hasNext()) {
        if (num % 20 == 0) {
          buffer.append("\n" + iter.next().toString());
        } else {
          buffer.append(" " + iter.next().toString());
        }
        num++;
      }
      JTextArea textContributions = new JTextArea(buffer.toString());
      textContributions.setEditable(false);
      textContributions.setBackground(getBackground());
      JScrollPane scrollPane = new JScrollPane(
          textContributions,
          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setBorder(null);
      getContentPane().add(scrollPane);
    }
    
    // Commands component
    JComponent commands = createCommandsComponent();
    getContentPane().add(commands);

    pack();
  }

  /**
   * @return Component for the Preferences.
   */
  private JComponent createPreferencesComponent() {
    JPanel panel = new JPanel();
    BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
    panel.setLayout(layout);

    // Fields
    JComponent fieldsComponent = createFieldsComponent();
    panel.add(fieldsComponent);

    // Directories
    JComponent directoriesComponent = createDirectoriesComponent();
    panel.add(directoriesComponent);

    return panel;
  }

  /**
   * @return Component for the Fields.
   */
  private JComponent createFieldsComponent() {
    JPanel panel = new JPanel();
    GridBagLayout layout = new GridBagLayout();
    panel.setLayout(layout);
    panel.setBorder(new TitledBorder("Settings"));
    panel.setMinimumSize(new Dimension(150, 200));
    panel.setMaximumSize(new Dimension(200, 800));

    // Constraints
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridheight = 1;
    constraints.gridwidth = 1;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.insets = new Insets(1, 1, 1, 1);
    constraints.ipadx = 1;
    constraints.ipady = 1;
    constraints.weightx = 1;
    constraints.weighty = 0;

    // User name
    JLabel labelUser = new JLabel("User name :", SwingConstants.RIGHT);
    constraints.gridx = 0;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(labelUser, constraints);

    textUser = new JTextField(configuration.getUserName(), 15);
    textUser.setToolTipText("The User Name that will be credited for the .xyz files");
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(textUser, constraints);

    constraints.gridy++;

    // Mail server
    JLabel labelMailServer = new JLabel("Mail server :", SwingConstants.RIGHT);
    constraints.gridx = 0;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(labelMailServer, constraints);

    textMailServer = new JTextField(configuration.getMailServer(), 15);
    textMailServer.setToolTipText("Your mail server, maybe something like smtp....");
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(textMailServer, constraints);

    constraints.gridy++;

    // User mail
    JLabel labelUserMail = new JLabel("Mail address :", SwingConstants.RIGHT);
    constraints.gridx = 0;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(labelUserMail, constraints);

    textUserMail = new JTextField(configuration.getUserMail(), 15);
    textUserMail.setToolTipText("Your email address");
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(textUserMail, constraints);

    constraints.gridy++;

    // User login
    JLabel labelUserLogin = new JLabel("Login :", SwingConstants.RIGHT);
    constraints.gridx = 0;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(labelUserLogin, constraints);

    textUserLogin = new JTextField(configuration.getLogin(), 15);
    textUserLogin.setToolTipText("You only need to fill this field if your mail server requires authentication.");
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(textUserLogin, constraints);

    constraints.gridy++;

    // User password
    JLabel labelUserPassword = new JLabel("Password :", SwingConstants.RIGHT);
    constraints.gridx = 0;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(labelUserPassword, constraints);

    textUserPassword = new JPasswordField(configuration.getPassword(), 15);
    textUserPassword.setToolTipText("You only need to fill this field if your mail server requires authentication.");
    constraints.gridx++;
    constraints.weightx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(textUserPassword, constraints);

    constraints.gridy++;

    // Test mail
    buttonTestMail = new JButton("Test mail ...");
    buttonTestMail.setActionCommand(actionTestMail);
    buttonTestMail.addActionListener(this);
    constraints.gridx = 0;
    constraints.weightx = 1;
    constraints.gridwidth = 2;
    constraints.anchor = GridBagConstraints.CENTER;
    panel.add(buttonTestMail, constraints);

    constraints.gridy++;

    // Filler
    JLabel labelFiller = new JLabel();
    constraints.gridx = 0;
    constraints.gridwidth = 2;
    constraints.weighty = 1;
    panel.add(labelFiller, constraints);

    return panel;
  }


  /**
   * @return Component for the Directories
   */
  private JComponent createDirectoriesComponent() {
    JPanel panel = new JPanel();
    BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
    panel.setLayout(layout);
    panel.setBorder(new TitledBorder("Directories"));

    // List
    listModel = new DefaultListModel();
    for (int i = 0; i < configuration.getDirectories().size(); i++) {
      listModel.addElement(configuration.getDirectories().get(i));
    }
    listDirectories = new JList(listModel);
    if (configuration.getDirectories().size() > 0) {
      listDirectories.setSelectedIndex(0);
    }
    listDirectories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listDirectories.setLayoutOrientation(JList.VERTICAL);
    //listDirectories.setMinimumSize(new Dimension(200, 100));
    JScrollPane listScroller = new JScrollPane(listDirectories);
    listScroller.setPreferredSize(new Dimension(200, 200));
    listScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    listScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    panel.add(listScroller);

    // Panel for commands
    panel.add(createDirectoriesCommands());

    return panel;
  }


  /**
   * @return Component for the commands on the directories
   */
  private JComponent createDirectoriesCommands() {
    JPanel panel = new JPanel();
    BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
    panel.setLayout(layout);

    // Add
    buttonAddDirectories = new JButton("Add");
    buttonAddDirectories.setActionCommand(actionAddDirectory);
    buttonAddDirectories.addActionListener(this);
    panel.add(buttonAddDirectories);

    // Remove
    buttonRemoveDirectories = new JButton("Remove");
    buttonRemoveDirectories.setActionCommand(actionRemoveDirectory);
    buttonRemoveDirectories.addActionListener(this);
    if (configuration.getDirectories().size() == 0) {
      buttonRemoveDirectories.setEnabled(false);
    }
    panel.add(buttonRemoveDirectories);

    return panel;
  }


  /**
   * @return Component for the Commands.
   */
  private JComponent createCommandsComponent() {
    JPanel panel = new JPanel();
    BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
    panel.setLayout(layout);

    // OK
    JButton buttonOk = new JButton("OK");
    buttonOk.setActionCommand(actionOk);
    buttonOk.addActionListener(this);
    panel.add(buttonOk);

    // Cancel
    JButton buttonCancel = new JButton("Cancel");
    buttonCancel.setActionCommand(actionCancel);
    buttonCancel.addActionListener(this);
    panel.add(buttonCancel);

    return panel;
  }


  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    if (actionOk.equals(e.getActionCommand())) {

      // Validate configuration
      configuration.setUserName(textUser.getText());
      configuration.setMailServer(textMailServer.getText());
      configuration.setUserMail(textUserMail.getText());
      configuration.setLogin(textUserLogin.getText());
      configuration.setPassword(textUserPassword.getText());
      Vector directories = new Vector(listModel.size());
      for (int i = 0; i < listModel.size(); i++) {
        directories.add(listModel.get(i));
      }
      configuration.setDirectories(directories);
      configuration.saveConfiguration();
      this.hide();

    } else if (actionCancel.equals(e.getActionCommand())) {

      // Cancel
      this.hide();

    } else if (actionAddDirectory.equals(e.getActionCommand())) {

      // Add a directory
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      fileChooser.setApproveButtonText("Add directory");
      fileChooser.setApproveButtonToolTipText("Add this directory to the list of directories containing Folding@Home XYZ files");
      int returnVal = fileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File directory = fileChooser.getSelectedFile();
        if ((directory != null) && (directory.isDirectory())) {
          listModel.addElement(directory.getAbsolutePath());
          listDirectories.setSelectedIndex(listModel.size() - 1);
          buttonRemoveDirectories.setEnabled(true);
        }
      }

    } else if (actionRemoveDirectory.equals(e.getActionCommand())) {

      // Remove a directory
      int index = listDirectories.getSelectedIndex();
      if (index >= 0) {
        listModel.remove(index);
        int size = listModel.getSize();
        if (size == 0) {
          buttonRemoveDirectories.setEnabled(false);
        } else {
          if (index == listModel.getSize()) {
            index--;
          }
          listDirectories.setSelectedIndex(index);
          listDirectories.ensureIndexIsVisible(index);
        }
      }

    } else if (actionTestMail.equals(e.getActionCommand())) {

      // Test mail configuration
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setApproveButtonText("Use this file for testing");
      fileChooser.setApproveButtonToolTipText("Use this file in the testing mail sent to yourself");
      fileChooser.setDialogTitle("Select file for testing");
      int returnVal = fileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File testFile = fileChooser.getSelectedFile();
        if ((testFile != null) && (testFile.isFile())) {
          Configuration config = new Configuration();
          config.setLogin(textUserLogin.getText());
          config.setMailServer(textMailServer.getText());
          config.setPassword(textUserPassword.getText());
          config.setUserMail(textUserMail.getText());
          config.setUserName(textUser.getText());
          MailSender sender = new MailSender(config, "XXXX", testFile, true);
          try {
            sender.sendMail();
            JOptionPane.showMessageDialog(
                this, "The test mail has been sent.", "Message sent", JOptionPane.INFORMATION_MESSAGE);
          } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "The following error was encountered when sending the mail:\n" +
                ex.getClass().getName() + ": " + ex.getMessage(),
                "Error while sending mail",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      }

    }
  }
}
