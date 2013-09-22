package org.jmol.awtjs2d;

import java.io.ByteArrayOutputStream;

import org.jmol.api.JmolOutputChannel;
import org.jmol.util.SB;

/**
 * 
 * A surrogate for FileOutputStream, allowing collection of byte
 * or character data for final posting to a server as bytes to
 * be returned to the user via a browser dialog. HTML5 JS only.
 *  
 */

public class WebOutputChannel extends ByteArrayOutputStream  implements JmolOutputChannel {
 
  private String fileName;
  
  public String getFileName() {
    return fileName;
  }
  
  private SB sb;
  
  public WebOutputChannel(String fileName) {
    this.fileName = fileName;
  }

  public void cancel() {
    fileName = null;
    sb = null;
  }
  
  public void write(String data) {
    if (sb == null)
      sb = new SB();
    sb.append(data);
  }

  @Override
  public void close() {
    if (fileName == null)
      return;

    /**
     * @j2sNative
     * 
     *     Jmol._doAjax(this.fileName, null, (this.sb == null ? this.toByteArray() : this.sb.toString()));
     * 
     */
    {
    }
  }
}
