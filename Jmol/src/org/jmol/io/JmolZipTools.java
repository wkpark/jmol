package org.jmol.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

import javajs.api.ZInputStream;

public interface JmolZipTools {

  public ZInputStream newZipInputStream(InputStream is);
  
  public String getZipDirectoryAsStringAndClose(BufferedInputStream t);

  public InputStream newGZIPInputStream(InputStream bis) throws IOException;

  public Object getZipFileDirectory(BufferedInputStream bis,
                                          String[] subFileList, int listPtr, boolean asBufferedInputStream);

  public String[] getZipDirectoryAndClose(BufferedInputStream t,
                                                 boolean addManifest);

  public void getAllZipData(InputStream bis, String[] subFileList,
                                String replace, String string,
                                Map<String, String> fileData);

  public Object getZipFileContentsAsBytes(BufferedInputStream bis,
                                                 String[] subFileList, int i);

  public void addZipEntry(Object zos, String fileName) throws IOException;

  public void closeZipEntry(Object zos) throws IOException;

  public Object getZipOutputStream(Object bos);

  public int getCrcValue(byte[] bytes);

  public void readFileAsMap(BufferedInputStream is, Map<String, Object> bdata);

}
