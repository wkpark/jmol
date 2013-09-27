package org.jmol.awtjs2d;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownServiceException;

import org.jmol.api.JmolFileAdapterInterface;
import org.jmol.io.JmolOutputChannel;
import org.jmol.viewer.FileManager;

public class JmolFileAdapter implements JmolFileAdapterInterface {

  public Object getBufferedFileInputStream(String name) {
  	// this could be replaced by JavaScript
    try {
      throw new UnknownServiceException("No local file reading in JavaScript version of Jmol");
    } catch (IOException e) {
      return e.toString();
    }
  }

	public Object getBufferedURLInputStream(URL url, byte[] outputBytes,
			String post) {
		try {
			JmolURLConnection conn = (JmolURLConnection) url.openConnection();
			if (outputBytes != null)
				conn.outputBytes(outputBytes);
			else if (post != null)
				conn.outputString(post);
			return conn.getStringXBuilder();
		} catch (IOException e) {
			return e.toString();
		}
	}

  public JmolOutputChannel openOutputChannel(double privateKey, FileManager fm, String fileName, boolean asWriter, boolean asAppend) {
    // ignoring  append
    return (new JmolOutputChannel()).setParams(fm, fileName, asWriter, null);
  }

  public InputStream openFileInputStream(double privateKey, String fileName)
      throws IOException {
    // unused (to date) in JSmol/HTML5
    return null;
  }

  public String getAbsolutePath(double privateKey, String fileName) {
    return fileName;
  }

}
