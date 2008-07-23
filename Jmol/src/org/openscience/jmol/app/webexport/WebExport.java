/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
package org.openscience.jmol.app.webexport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import javax.swing.*;

import org.jmol.api.JmolViewer;
import org.openscience.jmol.app.HistoryFile;

public class WebExport extends JPanel {

  private static boolean showMoleculesAndOrbitals = false; //not implemented

  //run status
  private final static int STAND_ALONE = 0;
  private final static int IN_JMOL = 1;

  private static int runStatus = IN_JMOL; //assume running inside Jmol

  private static HistoryFile historyFile;

  private static WebPanel[] webPanels;
  private static WebExport webExport;
  private static JFrame webFrame;
  private static String windowName;

  
  private WebExport(JmolViewer viewer, HistoryFile hFile) {
    super(new BorderLayout());

    historyFile = hFile;
    remoteAppletPath = historyFile.getProperty("webMakerAppletPath", "..");
    localAppletPath = historyFile.getProperty("webMakerLocalAppletPath", "..");
    pageAuthorName = historyFile.getProperty("webMakerPageAuthorName", "Jmol Web Export");


    //Define the tabbed pane
    JTabbedPane mainTabs = new JTabbedPane();

    //Create file chooser
    JFileChooser fc = new JFileChooser();

    webPanels = new WebPanel[2];

    if (runStatus != STAND_ALONE) {
      //Add tabs to the tabbed pane

      JPanel introPanel = new JPanel();
      URL url = getResource(this, "WebExportIntro.html");
      if (url == null) {
        System.err.println("Couldn't find file: WebExportIntro.html");
      }
      JEditorPane intro = new JEditorPane();
      if (url != null) {
        try {
          intro.setPage(url);
        } catch (IOException e) {
          System.err.println("Attempted to read a bad URL: " + url);
        }
      }
      intro.setEditable(false);
      JScrollPane introPane = new JScrollPane(intro);
      introPane.setMaximumSize(new Dimension(450,350));
      introPane.setPreferredSize(new Dimension(400,300));
      introPanel.setLayout(new BorderLayout());
      introPanel.add(introPane);
      introPanel.setMaximumSize(new Dimension(450,350));
      introPanel.setPreferredSize(new Dimension(400,300));

      mainTabs.add("Introduction",introPanel);

      webPanels[0] = new PopInJmol(viewer, fc, webPanels, 0);
      webPanels[1] = new ScriptButtons(viewer, fc, webPanels, 1);

      int w = Integer.parseInt(historyFile.getProperty("webMakerInfoWidth",
          "300"));
      int h = Integer.parseInt(historyFile.getProperty("webMakerInfoHeight",
          "350"));

      mainTabs.addTab("Pop-In Jmol", webPanels[0].getPanel(w, h));
      mainTabs.addTab("ScriptButton Jmol", webPanels[1].getPanel(w, h));

      // Uncomment to activate the test panel
      //    Test TestCreator = new Test((Viewer)viewer);
      //    JComponent Test = TestCreator.Panel();
      //    Maintabs.addTab("Tests",Test);
    }

    showMoleculesAndOrbitals = (runStatus == STAND_ALONE || JmolViewer
        .checkOption(viewer, "webMakerAllTabs"));
    if (showMoleculesAndOrbitals) {
      mainTabs.addTab("Orbitals", (new Orbitals()).getPanel());
      mainTabs.addTab("Molecules", (new Molecules()).getPanel());
    }

    //The LogPanel should always be the last one

    mainTabs.addTab("Log", LogPanel.getPanel());

    //Add the tabbed pane to this panel
    add(mainTabs);
    
    //Create the small log

    add(LogPanel.getMiniPanel(), BorderLayout.SOUTH);


    //Uncomment the following line to use scrolling tabs.
    //tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

  }

  static String TimeStamp_WebLink() {
    String out = "      <small>Page skeleton and <a href=\"http://www.java.com\">JavaScript</a>";
    Date now = new Date();
    String now_string = DateFormat.getDateInstance().format(now);//Specify medium verbosity on the date and time
    out = out
        + " generated by export to web function of (<a href=\"http://jmol.sourceforge.net\">Jmol "
        + JmolViewer.getJmolVersion() + "</a>) on " + now_string
        + ". </small><br />";
    return out;
  }

  /*
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event-dispatching thread.
   */
  public static WebExport createAndShowGUI(JmolViewer viewer,
                                        HistoryFile historyFile, String wName) {

    if (viewer == null)
      runStatus = STAND_ALONE;

    //Create and set up the window.
    if (webFrame != null) {
      webFrame.setVisible(true);
      webFrame.toFront();
      return webExport;
    }
    webFrame = new JFrame("Jmol Web Page Maker");
    windowName = wName;
    historyFile.repositionWindow(windowName, webFrame, 700, 400);
    if (runStatus == STAND_ALONE) {
      //Make sure we have nice window decorations.
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
      webFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    } else {
      webFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    //Create and set up the content pane.
    webExport = new WebExport(viewer, historyFile);
    webExport.setOpaque(true); //content panes must be opaque
    webFrame.setContentPane(webExport);

    //Display the window.
    webFrame.pack();
    webFrame.setVisible(true);
    if (runStatus == STAND_ALONE) {
      //LogPanel.Log("Jmol_Web_Page_Maker is running as a standalone application");
    } else {
      //LogPanel.Log("Jmol_Web_Page_Maker is running as a plug-in");
    }

    return webExport;
  }

  public static void saveHistory() {
    historyFile.addWindowInfo(windowName, webFrame, null);
//    prop.setProperty("webMakerInfoWidth", "" + webPanels[0].getInfoWidth());
//    prop.setProperty("webMakerInfoHeight", "" + webPanels[0].getInfoHeight());
    prop.setProperty("webMakerAppletPath", remoteAppletPath);
    prop.setProperty("webMakerLocalAppletPath", localAppletPath);
    prop.setProperty("webMakerPageAuthorName", pageAuthorName);
   historyFile.addProperties(prop);
  }

  static String remoteAppletPath, localAppletPath;

  static String getAppletPath(boolean isRemote) {
    return (isRemote ? remoteAppletPath : localAppletPath);
  }

  static Properties prop = new Properties();

  static void setAppletPath(String path, boolean isRemote) {
    if (path == null)
      path = "..";
    if (isRemote) {
      remoteAppletPath = path;
      prop.setProperty("webMakerAppletPath", remoteAppletPath);
      historyFile.addProperties(prop);
    } else {
      localAppletPath = path;
      prop.setProperty("webMakerLocalAppletPath", localAppletPath);
      historyFile.addProperties(prop);      
    }
  }
  
  static String pageAuthorName;
  
  static String getPageAuthorName() {
    return pageAuthorName;
  }
 
  static void setWebPageAuthor(String pageAuthor) {
    if (pageAuthor == null)
      pageAuthor = "Jmol Web Export";
    pageAuthorName = pageAuthor;
    prop.setProperty("webMakerPageAuthorName", pageAuthorName);
    historyFile.addProperties(prop);
  }
  
  static JFrame getFrame(){
    return webFrame;
  }
  
  static URL getResource(Object object, String fileName) {
    URL url = null;
    try {
      if ((url = object.getClass().getResource("html/" + fileName)) == null)
        System.err.println("Couldn't find file: " + fileName);
    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage() + " in getResource "
          + fileName);
    }
    return url;
  }

  static String getResourceString(Object object, String name) throws IOException {
    URL url = WebExport.getResource(object, name);
    if (url == null) {
      throw new FileNotFoundException("Error loading resource " + name);
    }
    StringBuffer sb = new StringBuffer();
    try {
      //turns out from the Jar file
      // it's a sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
      // and within Eclipse it's a BufferedInputStream
      //LogPanel.log(name + " : " + url.getContent().toString());
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent()));
      String line;
      while ((line = br.readLine()) != null)
        sb.append(line).append("\n");
      br.close();
    } catch (Exception e) {
      LogPanel.log(e.getMessage());
    }
    String str = sb.toString();
    //LogPanel.log("Loading resource " + name + "("
    //  + str.length() + " bytes)");
    return str;
  }
}
