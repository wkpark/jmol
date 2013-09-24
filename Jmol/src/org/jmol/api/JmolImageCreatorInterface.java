package org.jmol.api;

import java.io.IOException;
import java.util.Map;

public interface JmolImageCreatorInterface {

  abstract public JmolImageCreatorInterface setViewer(JmolViewer viewer, double privateKey);

  abstract public String getClipboardText();

  /**
   * 
   * @param params include fileName, type, text, bytes, image, 
   *                       scripts, appendix, quality, 
   *                       outputStream, and type-specific parameters
   * @return null (canceled) or a message starting with OK or an error message
   */
  abstract public Object createImage(Map<String, Object> params);

  abstract public Object getImageBytes(Map<String, Object> params) throws IOException;

  abstract String clipImage(JmolViewer viewer, String text);

}
