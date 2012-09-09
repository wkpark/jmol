package org.jmol.api;

import java.net.URL;

public interface FileAdapterInterface {

  public String urlEncode(String name);

  public Object getBufferedURLInputStream(URL url, byte[] outputBytes,
                                          String post, boolean checkOnly);

  public Object getBufferedFileInputStream(String name);

}
