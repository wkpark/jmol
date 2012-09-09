package org.jmol.awt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.jmol.api.FileAdapterInterface;

public class FileAdapter implements FileAdapterInterface {

  public String urlEncode(String name) {
    try {
      return URLEncoder.encode(name, "utf-8");
    } catch (UnsupportedEncodingException e) {
      return name;
    }
  }

  public Object getBufferedURLInputStream(URL url, byte[] outputBytes,
                                          String post, boolean checkOnly) {
    URLConnection conn;
    try {
      conn = url.openConnection();
      if (outputBytes != null && !checkOnly) {
        conn.setRequestProperty("Content-Type", "application/octet-stream;");
        conn.setDoOutput(true);
        conn.getOutputStream().write(outputBytes);
        conn.getOutputStream().flush();
      } else if (post != null && !checkOnly) {
        conn.setRequestProperty("Content-Type",
            "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(post);
        wr.flush();
      }
      return new BufferedInputStream(conn.getInputStream());
    } catch (IOException e) {
      return e.getMessage();
    }
  }

  public Object getBufferedFileInputStream(String name) {
    File file = new File(name);
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (IOException e) {
      return e.getMessage();
    }
  }

}
