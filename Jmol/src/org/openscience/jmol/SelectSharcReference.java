
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

/**
 *
 */
public class SelectSharcReference extends javax.swing.JDialog
    implements java.awt.event.ActionListener {

  private javax.swing.JPanel buttonPanel;
  private javax.swing.JButton okButton;
  private javax.swing.JButton cancelButton;
  private javax.swing.JScrollPane listScrollPane;
  private javax.swing.JList list;
  private javax.swing.JLabel messageLabel;

  /**
   *  Creates new form SelectSharcReference
   */
  public SelectSharcReference(java.awt.Frame parent, Object[] values,
      boolean modal) {
    super(parent, modal);
    initComponents(values);
    pack();
    setVisible(false);
  }

  /**
   *  This method is called from within the constructor to
   *  initialize the form.
   */
  private void initComponents(Object[] values) {

    buttonPanel = new javax.swing.JPanel();
    okButton = new javax.swing.JButton();
    okButton.addActionListener(this);
    cancelButton = new javax.swing.JButton();
    cancelButton.addActionListener(this);
    listScrollPane = new javax.swing.JScrollPane();
    list = new javax.swing.JList(values);
    list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    messageLabel = new javax.swing.JLabel();
    setTitle("Select SHARC reference.");
    addWindowListener(new java.awt.event.WindowAdapter() {

      public void windowClosing(java.awt.event.WindowEvent evt) {
        closeDialog();
      }
    });

    okButton.setText("jButton2");
    okButton.setText("OK");
    buttonPanel.add(okButton);

    cancelButton.setText("jButton3");
    cancelButton.setText("Cancel");
    buttonPanel.add(cancelButton);

    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

    listScrollPane.setViewportView(list);
    getContentPane().add(listScrollPane, java.awt.BorderLayout.CENTER);

    messageLabel.setText("Please select the NMR reference level of theory.");
    getContentPane().add(messageLabel, java.awt.BorderLayout.NORTH);

  }

  public Object getValue() {
    return list.getSelectedValue();
  }

  /**
   *  Closes the dialog
   */
  private void closeDialog() {
    setVisible(false);
    dispose();
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    new SelectSharcReference(new javax.swing.JFrame(), new String[] {
      "a", "RHF/6-31G*", "foo"
    }, true).show();
  }


  public void actionPerformed(java.awt.event.ActionEvent ev) {
    if (cancelButton == ev.getSource()) {
      list.clearSelection();
    }
    closeDialog();
  }
}
