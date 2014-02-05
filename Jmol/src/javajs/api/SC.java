package javajs.api;

/**
 * SwingComponent interface common to javax.swing and javajs.swing
 * 
 * Can be augmented as needed, provided classes of javajs.swing are also
 * updated. (SwingComponents in javajs are subclasses of AbstractButton.)
 * 
 */

public interface SC {

  void add(SC item);

  void addActionListener(Object owner);

  void addItemListener(Object owner);

  void addMouseListener(Object owner);

  String getActionCommand();

  Object getComponent(int i);

  int getComponentCount();

  Object[] getComponents();

  String getName();

  Object getParent();

  Object getPopupMenu();

  String getText();

  void init(String text, Object icon, String actionCommand, SC popupMenu);

  boolean isEnabled();

  boolean isSelected();

  void remove(int i);

  void removeAll();

  void setActionCommand(String script);

  void setAutoscrolls(boolean b);

  void setEnabled(boolean enable);

  void setName(String string);

  void setSelected(boolean state);

  void setText(String entry);

  void insert(SC subMenu, int index);

}
