/*
 * @(#)RecentFilesDialog.java    1.0 98/08/27
 *
 * Copyright (c) 2000 Thomas James Grey All Rights Reserved.
 *
 * Thomas James Grey grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  THOMAS JAMES GREY AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL THOMAS JAMES GREY OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF THOMAS JAMES GREY HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */
package org.openscience.jmol;

import javax.swing.*;

public class RecentFilesDialog extends JDialog implements java.awt.event.WindowListener, java.awt.event.ActionListener{

    private static JmolResourceHandler jrh;
    private boolean ready = false;
    private String fileName = "BUG";
    private String fileType = "BUG"; 
    private JButton okBut;
    private String[] files = new String[10];
    private String[] fileTypes = new String[10];
    private JList fileList;
//    static {
//        jrh = new JmolResourceHandler("Jmol");
//    }
    private static JmolResourceHandler rch = new JmolResourceHandler("RecentFiles");
    private int numFiles = 0;

/** Creates a hidden recent files dialog **/
       public RecentFilesDialog(java.awt.Frame boss){
            super(boss,rch.getString("windowTitle"),true);
            getFiles();
             getContentPane().setLayout(new java.awt.BorderLayout());
             okBut = new JButton(rch.getString("okLabel"));
     okBut.addActionListener(this);
     getContentPane().add("South",okBut);
     fileList = new JList(files);
     fileList.setSelectedIndex(0);
     fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
     getContentPane().add("Center",fileList);
     setLocationRelativeTo(boss);
     pack();
       }

  private void getFiles(){
    try{
      java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(Jmol.FileHistoryFile));
      String file;
      String type;
      numFiles = 0;
      boolean carryon = true;
      while(carryon){
        file = in.readLine();
        type = in.readLine();
        if (file != null && type != null){
          files[numFiles] = file;
          fileTypes[numFiles]=type;
          numFiles++;
          if (numFiles >= 10){
            carryon = false;
          }
        }else{
          carryon = false;
        }
      }
      in.close();
    }catch (java.io.FileNotFoundException e){
      files[0]=" ";
      fileTypes[0]=" ";
    }catch(java.io.IOException e){
      files[0] = "Error opening history!!";
      files[1] = e.toString();
    }
     }

  /** Adds this file and type to the history. If already present, this file is premoted to the top position. **/
   public void addFile(String name, String type){
           int currentPosition = -1;
           for (int i =0; i< numFiles; i++){
                if (files[i].equals(name)){
                   currentPosition = i;
               }
           }
           if (currentPosition > 0){
             for (int i=currentPosition;i<numFiles;i++){
                files[i] = files[i+1];
               fileTypes[i]=fileTypes[i+1];
            }
          }
           for (int j=8; j>=0; j--){
              files[j+1] = files[j];
              fileTypes[j+1]=fileTypes[j];
    }
    files[0] = name;
    fileTypes[0]=type;
    if(numFiles <10 && currentPosition < 0){
      numFiles++;
    }
    fileList.setListData(files);
    fileList.setSelectedIndex(0);
    pack();
    saveList();
  }

  /** Saves the list to the history file. Called automaticaly when files are added **/
   public void saveList(){
    try{
      java.io.Writer out = new java.io.FileWriter(Jmol.FileHistoryFile);
      for (int i=0; i< numFiles;i++){
        out.write(files[i]+"\n"); 
        out.write(fileTypes[i]+"\n");
      }
      out.close();
    }catch(java.io.IOException e){
      System.err.println("Error saving history!! "+e);
    }
  }

/** This method will block until a file has been picked.
@returns String The name of the file picked or null if the action was aborted.
**/
      public String getFile(){
         if (!ready){};
         return fileName;
     }

/** This method will block until a file has been picked.
@returns String The type of the file picked or null if the action was aborted.
**/
    public String getFileType(){
         if (!ready){};
         return fileType;
    }

   public void windowClosing(java.awt.event.WindowEvent e){
      fileName = null;
      fileType = null;
      hide();
      ready=true;
   }

   public void actionPerformed(java.awt.event.ActionEvent e){
      fileName =  files[fileList.getSelectedIndex()];
      fileType = fileTypes[fileList.getSelectedIndex()];
      hide();
      ready=true;
   }

   public void windowClosed(java.awt.event.WindowEvent e){
   }
   public void windowOpened(java.awt.event.WindowEvent e){
      ready = false;
   }
   public void windowIconified(java.awt.event.WindowEvent e){
   }
   public void windowDeiconified(java.awt.event.WindowEvent e){
   }
   public void windowActivated(java.awt.event.WindowEvent e){
   }
   public void windowDeactivated(java.awt.event.WindowEvent e){
   }

}
