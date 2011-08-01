package org.openscience.chimetojmol;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChimePanel extends JPanel implements ItemListener, ActionListener {

  private JTextField chimePath;
  private Checkbox checkSubs;
  private boolean doSubs;
  private JButton goButton, browseButton;
  private JTextArea logArea;
  private JScrollPane logScrollPane;
  private JFileChooser chooser;

  ChimePanel() {

    chooser = new JFileChooser();
    chooser.setCurrentDirectory(new File("."));
    chooser.setDialogTitle("Select a Directory");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setAcceptAllFileFilterUsed(false);

    setLayout(new BorderLayout());

    chimePath = new JTextField(50);
    chimePath.addActionListener(this);
    chimePath.setText("c:/temp/teaching");

    JPanel pathPanel = new JPanel();
    pathPanel.setLayout(new BorderLayout());
    pathPanel
        .setBorder(BorderFactory
            .createTitledBorder("Directory containing Chime-based HTML pages to convert"));
    pathPanel.add("West", chimePath);
    browseButton = new JButton("browse...");
    browseButton.addActionListener(this);
    pathPanel.add("East", browseButton);
    add("North", pathPanel);

    checkSubs = new Checkbox("include subdirectories");
    checkSubs.addItemListener(this);
    add("Center", checkSubs);

    JPanel lowerPanel = new JPanel();
    lowerPanel.setLayout(new BorderLayout());
    JPanel goPane = new JPanel();
    goPane.setSize(30, 10);
    goButton = new JButton("Convert Page(s)");
    goButton.addActionListener(this);
    goPane.add(goButton);
    lowerPanel.add("North", goPane);

    logArea = new JTextArea(30, 20);
    logArea.setMargin(new Insets(5, 5, 5, 5));
    logArea.setEditable(false);
    logScrollPane = new JScrollPane(logArea);
    logScrollPane.setBorder(BorderFactory.createTitledBorder("0 files"));

    lowerPanel.add("South", logScrollPane);

    add("South", lowerPanel);

  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == goButton) {
      doGo();
    } else if (source == browseButton) {
      doBrowse();
    }
  }

  public void itemStateChanged(ItemEvent e) {
    Object source = e.getSource();
    int stateChange = e.getStateChange();
    if (source == checkSubs) {
      doSubs = (stateChange == ItemEvent.SELECTED);
      getFileList();
    }
  }

  private void log(String string) {
    logArea.setText(logArea.getText() + string + "\n");
  }

  private String oldDir;
  private List<File> files;

  void getFileList() {
    logArea.setText("");
    files = new ArrayList<File>();
    String dir = chimePath.getText();
    dir = dir.replace('\\', '/');
    while (dir.endsWith("/"))
      dir = dir.substring(0, dir.length() - 1);
    if (dir.length() < 4)
      return;
    oldDir = dir;
    try {
      copyDirectory("", new File(oldDir), new File(oldDir + "_jmol"), true);
    } catch (IOException e) {
      log(e.getMessage());
    }
  }

  private void doGo() {
    logArea.setText("");
    try {
      copyDirectory("", new File(oldDir), new File(oldDir + "_jmol"), false);
    } catch (IOException e) {
      logArea.setText(e.getMessage());
    }
  }

  private void doBrowse() {
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      String dir = chooser.getSelectedFile().toString();
      chimePath.setText(dir);
      getFileList();
    }
  }

  public void copyDirectory(String level, File sourceLocation,
                            File targetLocation, boolean justChecking)
      throws IOException {

    if (sourceLocation.isDirectory()) {
      if (!doSubs && !level.equals(""))
        return;
        if (!targetLocation.exists() && !justChecking)
          targetLocation.mkdir();
      String[] children = sourceLocation.list();
      for (int i = 0; i < children.length; i++)
        copyDirectory((level.equals("") ? "." : level.equals(".") ? ".."
            : level + "/.."), new File(sourceLocation, children[i]), new File(
            targetLocation, children[i]), justChecking);
    } else {
      if (!copyFile(level, sourceLocation, targetLocation, justChecking))
        log("Hmm..." + sourceLocation + " --> " + targetLocation);
    }
  }

  private boolean copyFile(String level, File f1, File f2, boolean justChecking) {
    if (f1.getName().endsWith(".htm") || f1.getName().endsWith(".html")) {
      if (justChecking) {
        files.add(f1);
        log(f1.getAbsolutePath());
        logScrollPane.setBorder(BorderFactory.createTitledBorder(files.size()
            + " files"));
        return true;
      }
      log(f1.getAbsolutePath() + " --> " + f2.getAbsolutePath());
      return processFile(level, f1, f2);
    }
    if (justChecking)
      return true;
    try {
      InputStream in = new FileInputStream(f1);
      OutputStream out = new FileOutputStream(f2);

      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private Pattern embed1 = Pattern.compile("<embed", Pattern.CASE_INSENSITIVE);
  private Pattern embed2 = Pattern.compile("</embed", Pattern.CASE_INSENSITIVE);

  private boolean processFile(String level, File f1, File f2) {
    String html = getFileContents(f1);
    if (html == null) {
      log("?error reading " + f1.getAbsolutePath());
      return false;
    }
    if (html.indexOf("Jmol.js") < 0) {
      String opener = "\n<script type=\"text/javascript\"";
      String s = opener + " src=\"" + level + "/Jmol.js\"></script>";
      s += opener + ">jmolInitialize('" + level + "')</script>";
      s += opener + " src=\"" + level + "/ChimeToJmol.js\"></script>";
      int i = html.toLowerCase().indexOf("<head>");
      if (i < 0) {
        html = "<head></head>" + html;
        i = 0;
      }
      html = html.substring(0, i + 6) + s + "\n" + html.substring(i + 6);
      html = embed1.matcher(html).replaceAll("<xembed");
      html = embed2.matcher(html).replaceAll("</xembed");
      if (!putFileContents(f2, html)) {
        log("?error creating " + f2);
        return false;
      }
      return true;
    }
    return true;
  }

  private String getFileContents(File f) {
    StringBuffer sb = new StringBuffer(8192);
    String line;
    try {
      BufferedReader br = new BufferedReader(new FileReader(f));
      while ((line = br.readLine()) != null)
        sb.append(line).append('\n');
      br.close();
    } catch (IOException e) {
      return null;
    }
    return sb.toString();
  }

  private boolean putFileContents(File f, String html) {
    FileWriter fstream;
    try {
      fstream = new FileWriter(f, false);
      BufferedWriter out = new BufferedWriter(fstream);
      out.write(html);
      out.close();
    } catch (IOException e) {
      return false;
    }
    return true;
  }

}
