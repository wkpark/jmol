package org.jmol.api;

import java.util.List;
import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.script.ScriptException;

import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptVariable;
import org.jmol.util.BitSet;
import org.jmol.util.Point3f;
import org.jmol.util.StringXBuilder;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public interface JmolScriptEvaluator {

  JmolScriptEvaluator setViewer(Viewer viewer);

  ScriptContext getThisContext();

  void pushContextDown();

  void resumeEval(ScriptContext sc);

  boolean getAllowJSThreads();

  void setCompiler();

  BitSet getAtomBitSet(Object atomExpression);

  boolean isStopped();

  void notifyResumeStatus();

  List<Integer> getAtomBitSetVector(int atomCount, Object atomExpression);

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

  ScriptContext getScriptContext();

  Object evaluateExpression(Object stringOrTokens, boolean asVariable);

  void deleteAtomsInVariables(BitSet bsDeleted);

  Map<String, ScriptVariable> getContextVariables();

  boolean evaluateParallel(ScriptContext context, ShapeManager shapeManager);

  void runScript(String script) throws ScriptException;

  void runScriptBuffer(String string, StringXBuilder outputBuffer) throws ScriptException;

  float evalFunctionFloat(Object func, Object params, float[] values);

  void setException(ScriptException sx, String msg, String untranslated);

  BitSet addHydrogensInline(BitSet bsAtoms, List<Atom> vConnections, Point3f[] pts) throws Exception;

  void evaluateCompiledScript(boolean isSyntaxCheck,
                              boolean isSyntaxAndFileCheck,
                              boolean historyDisabled, boolean listCommands,
                              StringXBuilder outputBuffer, boolean allowThreads);

}
