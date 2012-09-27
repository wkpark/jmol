package org.jmol.awtjs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * A method to allow a JavaScript Ajax 
 * 
 */
public class JmolURLConnection extends URLConnection {

	protected JmolURLConnection(URL url) {
		super(url);
	}

	byte[] bytesOut;
	String postOut = "";

	/**
	 * 
	 * doAjax() is where the synchronous call to AJAX is to happen.
	 * or at least where we wait for the asynchronous call to return.	
   * This method should fill the dataIn field with either a string or
   * byte array, or null if you want to throw an error.
   * 
   * url, bytesOut, and postOut are all available for use
   * 
   * the method is "private", but in JavaScript that can still be overloaded.
   * Just set something to org.jmol.awtjs.JmolURLConnection.prototype.doAjax
   * 
   * 
   */
  private Object doAjax() {
  	return null;
	}

  @Override
	public void connect() throws IOException {
		// not expected to be used. 
	}

	void outputBytes(byte[] bytes) {
  	//      type = "application/octet-stream;";
		bytesOut = bytes;
  }

  void outputString(String post) {
  	postOut = post;
  	//     type = "application/x-www-form-urlencoded";
  }

	public Object getStringBuffer() {
		return doAjax();
	}

}
