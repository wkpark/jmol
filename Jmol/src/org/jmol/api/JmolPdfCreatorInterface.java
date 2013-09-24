package org.jmol.api;

import org.jmol.io.JmolOutputChannel;

public interface JmolPdfCreatorInterface {

  public String createPdfDocument(JmolOutputChannel out, Object image);
}
