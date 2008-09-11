package org.jmol.api;

public interface JmolImageCreatorInterface {

  abstract public void setViewer(JmolViewer viewer);
  
  abstract public void clipImage(String text);
  
  abstract public String getClipboardText();
  
  abstract public String createImage(String fileName, String type, Object text_bytes, int quality);

}
