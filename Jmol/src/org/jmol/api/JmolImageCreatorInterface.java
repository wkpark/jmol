package org.jmol.api;

public interface JmolImageCreatorInterface {

  abstract public void setViewer(JmolViewer viewer);
  
  abstract public void clipImage(String text);
  
  abstract public String getClipboardText();
  
  /**
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return          null (canceled) or a message starting with OK or an error message
   */
  abstract public String createImage(String fileName, String type, Object text_or_bytes, int quality);

}
