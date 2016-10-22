package org.openscience.jmol.app.nbo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

/**
 * Builds the input file box found in all 4 modules
 *  JPanel containing file input box
 */
class FileHndlr extends JPanel {

  protected static final String sep = System.getProperty("line.separator");
  
  protected static final String EXTENSIONS = "31;32;33;34;35;36;37;38;39;40;41;42;46;nbo";
  protected JTextField tfDir,tfName,tfExt;
  protected File inputFile;
  protected String fileDir, jobStem;
  protected String useExt;
  protected JButton browse;
  protected NBODialog dialog;
  protected boolean canReRun;
  
  
  public FileHndlr(String name, String ext,final int mode, String useExt, NBODialog d){
    dialog = d;
    canReRun = true;
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    fileDir = (props.getProperty("workingPath",
        System.getProperty("user.home")));
    this.useExt = useExt;
    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx=0;
    c.gridy=0;
    c.fill=GridBagConstraints.BOTH;
    (tfDir = new JTextField()).setPreferredSize(new Dimension(110,20));
    tfDir.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        browsePressed();
      }
    });
    tfDir.setText(fileDir);
    add(tfDir,c);
    c.gridx=1;
    (tfName = new JTextField()).setPreferredSize(new Dimension(100,20));
    tfName.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
//        if(mode == MODEL)
//          showWorkpathDialogM(null,null);
       browsePressed();
      }
    });
    tfName.setText(name);
    add(tfName,c);
    c.gridx=0;
    c.gridy=1;
    add(new JLabel("         folder"),c);
    c.gridx=1;
    add(new JLabel("          name"),c);
    c.gridx=2;
    c.gridy=0;
    (tfExt = new JTextField()).setPreferredSize(new Dimension(40,20));
    tfExt.setText(ext);
    tfExt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        browsePressed();
      }
    });
    if(mode != 3 && mode != 4){
      add(tfExt,c);
      c.gridy=1;
      add(new JLabel("  ext"),c);
    }
    c.gridx=3;
    c.gridy=0;
    c.gridheight=2;
    if(mode != 5)
      browse = new JButton("Browse");
    else
      browse = new JButton("Save");
    browse.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent arg0) {
        browsePressed();
      }
    });
    add(browse,c);
    jobStem = name;
    setInput(fileDir, name, ext);
  }
  
  protected boolean browsePressed(){
    if(dialog.nboService.isWorking 
        && dialog.statusLab.getText().startsWith("Running")){
      int i = JOptionPane.showConfirmDialog(dialog, "Warning, changing jobs while running GenNBO can effect output files." +
      		"\nContinue anyway?");
      if(i == JOptionPane.NO_OPTION)
        return false;
    }
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
    myChooser.setFileHidingEnabled(true);
    
    String folder = tfDir.getText();
    String name = tfName.getText();
    if(!folder.equals("")){
      if(!folder.contains(":")) folder = "C:/"+folder;
      fileDir = folder + "/"+(name.equals("") ? " ":name);
    }
    myChooser.setSelectedFile(new File(fileDir));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      dialog.nboService.restart();
      inputFile = myChooser.getSelectedFile();
      //if(!useExt.equals("47")&&!useExt.equals("31")&&!useExt.equals("nbo")) 
        //return false;
      if(dialog.dialogMode == NBODialog.DIALOG_MODEL) return true;
      if(!useExt.equals("47")){
        dialog.loadModelFromNBO(fileDir, 
            (jobStem = FileHndlr.getJobStem(inputFile)), useExt);
        tfName.setText(jobStem);
        tfExt.setText(useExt);
        return true;
      }

      canReRun = true;
      setInputFile(inputFile);
      dialog.appendOutputWithCaret("Job: " + jobStem,'b');
      fileDir = inputFile.getParent();
      saveWorkHistory();
    }
    return true;
    
  }
  
  /**
   * Sets up the input file, currently only support for .47/model input file types
   * @param inputFile
   */
  protected void setInputFile(File inputFile){
    clearInputFile();
    //dialog.nboService.restart();
    dialog.isOpenShell = false;
    this.inputFile = inputFile;
    if (inputFile.getName().indexOf(".") > 0)
      jobStem = getJobStem(inputFile);
    setInput(inputFile.getParent(),jobStem,useExt);
    if (getExt(inputFile).equals("47")){
      if((inputFile.getParent()+"/").equals(dialog.nboService.serverDir)){
        JOptionPane.showMessageDialog(this, "Select a directory that does not contain the NBOServe executable," +
            "\nor select a new location for your NBOServe executable");
        return;
      }
      dialog.isJmolNBO = true;
      fileDir = inputFile.getParent();

      //TODO

      boolean canLoad = true;
      
      for(String x : EXTENSIONS.split(";")){
        File f3 = newNBOFile(inputFile,x);
        if(!f3.exists() && (dialog.dialogMode != NBODialog.DIALOG_MODEL)){
          if(dialog.dialogMode != NBODialog.DIALOG_RUN){
            if(canReRun){
              canReRun = false;
              dialog.runJob("PLOT",inputFile,"gennbo");
            }else
              dialog.vwr.alert("Error occurred during run");
            return;
          }
          canLoad = false;
        }
      }
      if(canLoad){
        dialog.isNewJob = true;
        if(dialog.dialogMode == NBODialog.DIALOG_VIEW) {
          if(dialog.basis.getSelectedIndex()!=9)
            dialog.basis.setSelectedIndex(5);
          else
            dialog.basis.setSelectedIndex(9);

        }else
          dialog.runScriptQueued("load " + fileDir+"/"+jobStem + ".47" );
        
      }else if(dialog.dialogMode == NBODialog.DIALOG_RUN){
        dialog.loadModelFromNBO(fileDir,jobStem,useExt);
        tfName.setText(jobStem);
        tfExt.setText("47");
      } 
    }
  }
  
  protected String[] read47File() {
      String [] fileData = new String[3];
      String nboKeywords = "";
      
      BufferedReader b = null;
      try {
        b = new BufferedReader(new FileReader(inputFile));
      } catch (FileNotFoundException e1) {
        return null;
      }
      SB data = new SB();
      String line;
      try {
        while((line = b.readLine()) != null){
          data.append(line+sep);
        }
        b.close();
      } catch (IOException e) {
        return null;
      }
      String[] tokens = PT.split(data.toString(), "$END");
      boolean atParams = false;
      SB fout = new SB(), fout2 = new SB();
      if(tokens.length<=0) return null;
      for (int i = 0;;) {
        String s = tokens[i];
        s = PT.trim(s, "\t\r\n ");
        if(!atParams){
          if (s.indexOf("$NBO") >= 0) {
            atParams=true;
            if(PT.split(s, "$NBO").length>1)
              nboKeywords = (PT.split(s, "$NBO")[1]);
            else
              nboKeywords = "";
              //cleanNBOKeylist("");
            s = PT.split(s, "$NBO")[0];
            Logger.info("Params read: " + nboKeywords);
          }
          if(!s.equals(""))
            fout.append(s).append(sep);
          if (++i == tokens.length)
            break;
          if(!atParams)
            fout.append("$END").append(sep);
        }else{
            fout2.append(s).append(sep);
            if (++i == tokens.length)
              break;
            fout2.append("$END").append(sep);
          
        }
      }
      fileData[0] = fout.toString();
      fileData[1] = nboKeywords;
      fileData[2] = fout2.toString();
      return fileData;
    }
  
  /**
   * gets a valid $CHOOSE list from nbo file if it exists and corrects the bonds in the jmol model
   * @return false if output contains error
   */
  protected boolean getChooseList(){
    File f = newNBOFile(inputFile,"nbo");
    if(!f.exists()||f.length()==0)
      return false;
    String[] tokens = PT.split(getFileData(f.toString()), "\n $CHOOSE");
    int i = 1;
    if(tokens.length<2){
      dialog.alert("An error occurred during run, view .nbo output?");
      return false;
    }
    if(tokens[1].trim().startsWith("keylist")){
      if(!tokens[1].contains("Structure accepted:")){
        if(tokens[1].contains("missing END?")){
          dialog.alert("Plot files not found. Run now with PLOT keyword?");
          return false;
        }else if(tokens[2].contains("ignoring")){
          dialog.alert("Ignoring $CHOOSE list"); 
        }else{
          return false;
        }
      }
      i = 3;
    }
    String data = tokens[i].substring(0,tokens[i].indexOf("$END"));
      
    dialog.setChooseList(data);
    
    return true;
  }
  
  protected String [] getRSList(){
    String data = getFileData(newNBOFile(inputFile,"nbo").toString());
    String[] toks = PT.split(data, "TOPO matrix for the leading resonance structure:\n");
    if(toks.length < 2){
      if(toks[0].contains("0 candidate reference structure(s)"))
        dialog.vwr.alert("0 candidate reference structure(s) calculated by SR LEWIS" +
            "Candidate reference structure taken from NBO search");
      return null;
    }
    String[] toks2 = PT.split(toks[1], 
        "---------------------------------------------------------------------------");
    String [] rsList = new String[2];
    rsList[0] = toks2[0].substring(
        toks2[0].lastIndexOf('-'),toks2[0].indexOf("Res")).trim();
    rsList[0] = rsList[0].replace("-\n", "");
    rsList[1] = toks2[1];
    return rsList;
  }

  
  public void clear(){
    tfName.setText("");
    tfExt.setText("");
  }  
  
  
  
//useful file manipulation methods /////////////////////////////
  
 protected static File newNBOFile(File f, String ext) {
   String fname = f.toString();
   if(fname.lastIndexOf(".")<0)
     return new File(fname+"."+ext);
   return new File(fname.substring(0, fname.lastIndexOf(".")) + "." + ext);
 }
 
 
 protected void clearInputFile(){
   for(String x: EXTENSIONS.split(";")){
     File f2 = new File(dialog.nboService.serverDir+"/"+jobStem + "." + x);
     f2.delete();
   }
   inputFile=null;
   if(dialog.dialogMode == 'v')
     dialog.resetView();
 }  
 
  protected static String getJobStem(File inputFile) {
    String fname = inputFile.getName();
    return fname.substring(0, fname.lastIndexOf("."));
  }
  
  protected void saveWorkHistory(){
    java.util.Properties props = new java.util.Properties();
    props.setProperty("workingPath", fileDir);
    JmolPanel.historyFile.addProperties(props);
  }
  
  protected void setInput(String f, String n, String e){
    tfName.setText(n);
    tfDir.setText(f);
    tfExt.setText(e);
    //System.out.println("-------" + f + n + "/" + e);
    inputFile = new File(f + "\\" + n + "." + e);
  }

  protected static String getExt(File newFile) {
    String fname = newFile.toString();
    return fname.substring(fname.lastIndexOf(".") + 1);
  }
  

  protected String getFileData(String fileName) {
    return dialog.vwr.getFileAsString4(fileName, Integer.MAX_VALUE,
        false, false, false, "nbo");
  }
  
  static void writeToFile(String s, File file) throws IOException {
    PrintWriter writer = new PrintWriter(file);
    writer.print(s);
    writer.close();
  }
  
}