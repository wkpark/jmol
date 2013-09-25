package org.jmol.viewer;

import java.io.IOException;
import java.util.Map;

import org.jmol.io.JmolOutputChannel;

abstract class OutputManager {

  abstract OutputManager setViewer(Viewer viewer, double privateKey);

  abstract Object getWrappedState(String fileName, String[] scripts, int width,
                         int height, boolean asJmolZip);


  abstract String processWriteOrCapture(Map<String, Object> params);
  abstract String outputToFile(Map<String, Object> params);

  abstract Object getImageAsBytes(Map<String, Object> params);

  abstract String getOutputFromExport(Map<String, Object> params);

  abstract String writeFileData(String fileName, String type, 
                        int modelIndex, Object[] parameters);

  abstract JmolOutputChannel getOutputChannel(String localName, String[] fullPath);

  abstract void logToFile(String data);
  
  abstract String setLogFile(String name);

  /**
   * 
   * @param params include fileName, type, text, bytes, image, 
   *                       scripts, appendix, quality, 
   *                       outputStream, and type-specific parameters
   * @return null (canceled) or a message starting with OK or an error message
   */
  
  abstract Object createImage(Map<String, Object> params);

  abstract Object getOrSaveImage(Map<String, Object> params) throws IOException;

  abstract String clipImageOrPasteText(String text);

  abstract String getClipboardText();
}
