package org.jmol.api;

public interface JmolFileInterface {

  String getFullPath();

  String getName();

  long length();

  boolean isDirectory();

  JmolFileInterface getParentAsFile();

}
