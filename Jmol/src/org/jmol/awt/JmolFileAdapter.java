package org.jmol.awt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.jmol.api.JmolFileAdapterInterface;
import org.jmol.api.JmolFileInterface;
import org.jmol.io.JmolOutputChannel;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public class JmolFileAdapter implements JmolFileAdapterInterface {

  private Viewer viewer;

  public JmolFileAdapter(Viewer viewer) {
    this.viewer = viewer;
  }

  public Object getBufferedURLInputStream(URL url, byte[] outputBytes,
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
        if (outputBytes == null)
          outputString(conn, post);
        else
          outputBytes(conn, outputBytes);
      }
      return new BufferedInputStream(conn.getInputStream());
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  private void outputBytes(URLConnection conn, byte[] bytes) throws IOException {
    conn.getOutputStream().write(bytes);
    conn.getOutputStream().flush();
    //??conn.getOutputStream().close();
  }

  private void outputString(URLConnection conn, String post) throws IOException {
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    wr.write(post);
    wr.flush();
    //??wr.close();
  }

  public Object getBufferedFileInputStream(String name) {
    File file = new File(name);
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (IOException e) {
      return e.toString();//e.getMessage();
    }
  }

  public static JmolFileInterface newFile(String name) {
    return new JmolFile(name);
  }

  public JmolOutputChannel openOutputChannel(double privateKey, FileManager fm,
                                             String fileName, boolean asWriter,
                                             boolean asAppend)
      throws IOException {
    boolean isLocal = (fileName != null && !fileName.startsWith("http://") && !fileName
        .startsWith("https://"));
    if (asAppend && isLocal && fileName.indexOf("JmolLog_") < 0)
      asAppend = false;
    return (viewer.checkPrivateKey(privateKey) ? (new JmolOutputChannel())
        .setParams(fm, fileName, asWriter, (isLocal ? new FileOutputStream(
            fileName, asAppend) : null)) : null);
  }

  public InputStream openFileInputStream(double privateKey, String fileName)
      throws IOException {    
    return (viewer.checkPrivateKey(privateKey) ? new FileInputStream(fileName) : null);
  }

  public String getAbsolutePath(double privateKey, String fileName) {
    return (viewer.isApplet() || !viewer.checkPrivateKey(privateKey) ? fileName
        : (new File(fileName).getAbsolutePath()));
  }

}
