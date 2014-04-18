package org.jmol.api;


import java.util.Map;

import org.jmol.java.BS;
import org.jmol.script.ScriptException;

import org.jmol.script.ScriptContext;
import org.jmol.script.SV;
import org.jmol.script.T;

import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public interface JmolScriptEvaluator {

  JmolScriptEvaluator setViewer(Viewer vwr);

  ScriptContext getThisContext();

  void pushContextDown(String why);

  void resumeEval(ScriptContext sc);

  boolean getAllowJSThreads();

  void setCompiler();

  BS getAtomBitSet(Object atomExpression);

  boolean isStopped();

  void notifyResumeStatus();

  Lst<Integer> getAtomBitSetVector(int ac, Object atomExpression);

  boolean isPaused();

  String getNextStatement();

  void resumePausedExecution();

  void stepPausedExecution();

  void pauseExecution(boolean b);

  boolean isExecuting();

  void haltExecution();

  boolean compileScriptFile(String strScript, boolean isQuiet);

  boolean compileScriptString(String strScript, boolean isQuiet);

  String getErrorMessage();

  String getErrorMessageUntranslated();

  ScriptContext checkScriptSilent(String strScript);

  String getScript();

  void setDebugging();

  boolean isStepping();

  ScriptContext getScriptContext(String why);

  Object evaluateExpression(Object stringOrTokens, boolean asVariable, boolean compileOnly);

  void deleteAtomsInVariables(BS bsDeleted);

  Map<String, SV> getContextVariables();

  boolean evalParallel(ScriptContext context, ShapeManager shapeManager);

  void runScript(String script) throws ScriptException;

  void runScriptBuffer(String string, SB outputBuffer) throws ScriptException;

  float evalFunctionFloat(Object func, Object params, float[] values);

  void evaluateCompiledScript(boolean isSyntaxCheck,
                              boolean isSyntaxAndFileCheck,
                              boolean historyDisabled, boolean listCommands,
                              SB outputBuffer, boolean allowThreads);

  Map<String, Object> getDefinedAtomSets();

  String setObjectPropSafe(String id, int tokCommand);

  void stopScriptThreads();

  boolean isStateScript();

  boolean checkSelect(Map<String, SV> h, T[] where);

}
