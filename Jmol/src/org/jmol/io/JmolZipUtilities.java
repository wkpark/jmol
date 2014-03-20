package org.jmol.io;

import java.io.InputStream;

import java.util.Map;

import org.jmol.api.JmolAdapter;

public interface JmolZipUtilities {

  public Object getAtomSetCollectionOrBufferedReaderFromZip(JmolZipTools zpt, JmolAdapter adapter,
                                                            InputStream is,
                                                            String fileName,
                                                            String[] zipDirectory,
                                                            Map<String, Object> htParams,
                                                            int i,
                                                            boolean asBufferedReader);

  public String[] spartanFileList(JmolZipTools zpt, String name, String zipDirectory);

  public byte[] getCachedPngjBytes(JmolBinary jmb, String pathName);

  public boolean cachePngjFile(JmolBinary jmb, String[] data);  

}
