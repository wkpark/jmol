package org.jmol.api;

import org.jmol.viewer.Viewer;

public interface JmolPopupInterface {

  public String getMenu(String string);

  public Object getJMenu();

  public void show(int x, int y);

  public void initialize(Viewer viewer, boolean doTranslate,
                                       String menu, boolean asPopup);

  public void updateComputedMenus();
 
}
