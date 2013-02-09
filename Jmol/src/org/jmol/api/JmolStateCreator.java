package org.jmol.api;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.jmol.modelset.Group;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.TickInfo;
import org.jmol.shape.AtomShape;
import org.jmol.shape.Shape;
import org.jmol.util.BitSet;
import org.jmol.util.JmolFont;
import org.jmol.util.StringXBuilder;
import org.jmol.viewer.Viewer;

public interface JmolStateCreator {

  void setViewer(Viewer viewer);

  Object getWrappedState(String fileName, String[] scripts, boolean isImage,
                         boolean asJmolZip, int width, int height);

  String getStateScript(String type, int width, int height);

  String getSpinState(boolean b);
  
  Map<String, Object> getInfo(Object manager);

  String getSpecularState();
  
  String getLoadState(Map<String, Object> htParams);

  String getModelState(StringXBuilder sfunc, boolean isAll,
                               boolean withProteinStructure);

  String getFontState(String myType, JmolFont font3d);

  String getFontLineShapeState(String s, String myType, TickInfo[] tickInfos);

  void getShapeSetState(AtomShape atomShape, Shape shape, int monomerCount, Group[] monomers,
                     BitSet bsSizeDefault, Map<String, BitSet> temp, Map<String, BitSet> temp2);

  String getMeasurementState(AtomShape as, List<Measurement> mList, int measurementCount,
                             JmolFont font3d, TickInfo tickInfo);

  String getBondState(Shape shape, BitSet bsOrderSet, boolean reportAll);

  String getAtomShapeSetState(Shape shape, AtomShape[] shapes);

  String getShapeState(Shape shape);

  String getCommands(Map<String, BitSet> htDefine, Map<String, BitSet> htMore,
                     String select);

  String getAllSettings(String prefix);

  String getAtomShapeState(AtomShape shape);

  String getTrajectoryState();

  String getFunctionCalls(String selectedFunction);

  String getAtomicPropertyState(byte taintCoord, BitSet bsSelected);

  void getAtomicPropertyStateBuffer(StringXBuilder commands, byte type,
                                    BitSet bs, String name, float[] data);

  void undoMoveAction(int action, int n);

  void undoMoveActionClear(int taintedAtom, int type, boolean clearRedo);

  String createImageSet(String fileName, String type, String text,
                        byte[] bytes, String[] scripts, int quality, int width,
                        int height, BitSet bsFrames, int nVibes,
                        String[] fullPath);

  Object createImagePathCheck(String fileName, String type,
                                     String text, byte[] bytes,
                                     String[] scripts, Object appendix,
                                     int quality, int width, int height,
                                     String[] fullPath, boolean doCheck);

  void syncScript(String script, String applet, int port);

  String generateOutputForExport(String type, String[] fileName, int width,
                                 int height);

  Object getImageAsWithComment(String type, int quality, int width, int height,
                               String fileName, String[] scripts,
                               OutputStream os, String comment);

  String streamFileData(String fileName, String type, String type2,
                        int modelIndex, Object[] parameters);

  OutputStream getOutputStream(String localName, String[] fullPath);

  void openFileAsync(String fileName, boolean pdbCartoons);

  void showEditor(String[] file_text);

  void log(String data);
  
  public void quickScript(String script);
  
}
