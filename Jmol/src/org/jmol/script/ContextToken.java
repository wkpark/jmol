package org.jmol.script;

import java.util.Hashtable;

class ContextToken extends Token {
  Hashtable contextVariables;

  ContextToken(int tok, int intValue, Object value) {
    super(tok, intValue, value);
  }

  ContextToken(int tok, Object value) {
    super(tok, value);
  }

  ContextToken(Token token) {
    tok = token.tok;
    intValue = token.intValue;
    value = token.value;
  }

  void addName(String name) {
    if (contextVariables == null)
      contextVariables = new Hashtable();
    ScriptCompiler.addContextVariable(contextVariables, name);
  }
  
}
