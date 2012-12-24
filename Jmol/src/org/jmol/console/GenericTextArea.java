package org.jmol.console;

public interface GenericTextArea {

  int getCaretPosition();

  String getText();

  void setText(String text);

  int getLength();

  void insertString(int length, String message, Object att) throws Exception;

  void setCaretPosition(int pt);

}
