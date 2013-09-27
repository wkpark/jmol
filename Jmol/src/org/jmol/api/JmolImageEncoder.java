package org.jmol.api;

import java.util.Map;

import org.jmol.io.JmolOutputChannel;

public interface JmolImageEncoder {

  public boolean createImage(ApiPlatform apiPlatform, String type,
                             Object objImage, JmolOutputChannel out,
                             Map<String, Object> params, String[] errRet);
}
