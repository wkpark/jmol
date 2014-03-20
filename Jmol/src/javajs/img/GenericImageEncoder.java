package javajs.img;

import java.util.Map;

import javajs.util.OC;

public interface GenericImageEncoder {

  public void createImage(String type, OC out,
                             Map<String, Object> params) throws Exception;
}
