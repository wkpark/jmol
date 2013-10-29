package org.jmol.awtjs2d;

import java.net.URL;

import javajs.util.AjaxURLConnection;

import org.jmol.api.JmolFileInterface;
import org.jmol.util.Txt;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

/**
 * 
 * A class that mimics java.io.File
 * 
 */

class JSFile implements JmolFileInterface {

  private String name;
	private String fullName;

  static JmolFileInterface newFile(String name) {
    return new JSFile(name);
  }

	JSFile(String name) {
  	this.name = name.replace('\\','/');
  	fullName = name;
  	if (!fullName.startsWith("/") && FileManager.urlTypeIndex(name) < 0)
  		fullName = Viewer.jsDocumentBase + "/" + fullName;
  	fullName = Txt.simpleReplace(fullName, "/./", "/");
  	name = name.substring(name.lastIndexOf("/") + 1);
  }

  public JmolFileInterface getParentAsFile() {
  	int pt = fullName.lastIndexOf("/");
  	return (pt < 0 ? null : new JSFile(fullName.substring(0, pt)));
  }

	public String getFullPath() {
		return fullName;
	}

	public String getName() {
    return name;
	}

	public boolean isDirectory() {
		return fullName.endsWith("/");
	}

	public long length() {
		return 0; // can't do this, shouldn't be necessary
	}

  static Object getBufferedURLInputStream(URL url, byte[] outputBytes,
      String post) {
    try {
      AjaxURLConnection conn = (AjaxURLConnection) url.openConnection();
      if (outputBytes != null)
        conn.outputBytes(outputBytes);
      else if (post != null)
        conn.outputString(post);
      return conn.getSB();
    } catch (Exception e) {
      return e.toString();
    }
  }

}
