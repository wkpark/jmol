package org.jmol.api;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

public interface JmolImageTyperInterface {

  public abstract void createPanel(JFileChooser fc, String[] choices,
                                   String[] extensions, String defaultExt);

  /**
   * Memorize the default type for the next time.
   */
  public abstract void memorizeDefaultType();

  /**
   * @return The file type which contains the user's choice
   */
  public abstract String getType();

  /**
   * @return The file extension which contains the user's choice
   */
  public abstract String getExtension();

  /**
   * @param sType JPG or PNG
   * @return The quality (on a scale from 0 to 10) of the JPEG
   * image that is to be generated.  Returns -1 if choice was not JPEG.
   */
  public abstract int getQuality(String sType);

  public abstract File setSelectedFile(Component awtComponent, File file);

}
