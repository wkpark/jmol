package org.jmol.api;

public interface JmolPopupInterface {

  public void jpiDispose();
  public Object jpiGetMenuAsObject();
  public String jpiGetMenuAsString(String string);
  public void jpiInitialize(PlatformViewer viewer, String menu);
  public void jpiShow(int x, int y);
  public void jpiUpdateComputedMenus();

 
}
