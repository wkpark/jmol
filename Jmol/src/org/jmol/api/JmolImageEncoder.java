package org.jmol.api;

import java.util.Map;

import javajs.util.OutputChannel;


public interface JmolImageEncoder {

  public boolean createImage(ApiPlatform apiPlatform, String type,
                             Object objImage, OutputChannel out,
                             Map<String, Object> params, String[] errRet);
}
