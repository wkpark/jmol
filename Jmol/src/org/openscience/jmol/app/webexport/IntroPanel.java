package org.openscience.jmol.app.webexport;

import java.awt.*;
import java.io.IOException;
import java.net.URL;

import javax.swing.*;

public class IntroPanel {
  
  JPanel getPanel(){
    URL url = this.getClass().getResource("WebExportIntro.html");
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
    JPanel introPanel = new JPanel();
    introPanel.setLayout(new BorderLayout());
    introPanel.add(introPane);
    introPanel.setMaximumSize(new Dimension(450,350));
    introPanel.setPreferredSize(new Dimension(400,300));

    return (introPanel);
  }
}
