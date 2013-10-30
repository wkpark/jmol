package org.jmol.api;

import javajs.util.OutputChannel;

public interface JmolPdfCreatorInterface {

  public String createPdfDocument(OutputChannel out, Object image);
}
