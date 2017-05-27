package org.jmol.util;


public interface SimpleEdge {

  int getCovalentOrder();

  SimpleNode getOtherNode(SimpleNode a);

  boolean isCovalent();

}
