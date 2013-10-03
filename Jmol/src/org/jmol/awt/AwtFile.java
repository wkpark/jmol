package org.jmol.awt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.jmol.api.JmolFileInterface;

/**
 * a subclass of File allowing extension to JavaScript
 * 
 * private to org.jmol.awt
 * 
 */

class AwtFile extends File implements JmolFileInterface {

  AwtFile(String name) {
    super(name);
  }

  public JmolFileInterface getParentAsFile() {
    File file = getParentFile();
    return (file == null ? null : new AwtFile(file.getAbsolutePath()));
  }

  static Object getBufferedFileInputStream(String name) {
    File file = new File(name);
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (IOException e) {
      return e.toString();//e.getMessage();
    }
  }

  static Object getBufferedURLInputStream(URL url, byte[] outputBytes,
                                          String post) {
    try {
      URLConnection conn = url.openConnection();
      String type = null;
      if (outputBytes != null) {
        type = "application/octet-stream;";
      } else if (post != null) {
        type = "application/x-www-form-urlencoded";
      }
      if (type != null) {
        conn.setRequestProperty("Content-Type", type);
        conn.setDoOutput(true);
        if (outputBytes == null) {
          OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
          wr.write(post);
          wr.flush();
        } else {
          conn.getOutputStream().write(outputBytes);
          conn.getOutputStream().flush();
        }
      }
      return new BufferedInputStream(conn.getInputStream());
    } catch (IOException e) {
      return e.getMessage();
    }
  }

}
