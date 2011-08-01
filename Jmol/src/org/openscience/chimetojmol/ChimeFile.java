package org.openscience.chimetojmol;

import java.io.File;

public class ChimeFile extends File {

  boolean processed;
  String level;
  public String newDir;
  
  public ChimeFile(String newDir, String name, String level) {
    super(name);
    this.level = level;
    this.newDir = newDir;
  }

  @Override
  public String toString() {
    return getName();
  }
  
  
}
