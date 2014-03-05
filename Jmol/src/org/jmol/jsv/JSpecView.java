package org.jmol.jsv;

import java.util.Hashtable;


import org.jmol.api.JmolJSpecView;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import javajs.util.List;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.StatusManager;
import org.jmol.viewer.Viewer;

import javajs.util.PT;

public class JSpecView implements JmolJSpecView {

  private Viewer viewer;
  @Override
  public void setViewer(Viewer viewer) {
    this.viewer = viewer;
  }
  
  @Override
  public void atomPicked(int atomIndex) {
    if (atomIndex < 0)
      return;
    String peak = getPeakAtomRecord(atomIndex);
    if (peak != null)
      sendJSpecView(peak + " src=\"JmolAtomSelect\"");
  }
  
  @SuppressWarnings("unchecked")
  private String getPeakAtomRecord(int atomIndex) {
    Atom[] atoms = viewer.modelSet.atoms;
    int iModel = atoms[atomIndex].modelIndex;
    String type = null;
    switch (atoms[atomIndex].getElementNumber()) {
    case 1:
      type = "1HNMR";
      break;
    case 6:
      type = "13CNMR";
      break;
    default:
      return null;
    }
    List<String> peaks = (List<String>) viewer.getModelAuxiliaryInfoValue(iModel,
        "jdxAtomSelect_" + type);
    if (peaks == null)
      return null;
    //viewer.modelSet.htPeaks = null;
    //if (viewer.modelSet.htPeaks == null)
    viewer.modelSet.htPeaks = new Hashtable<String, BS>();
    Hashtable<String, BS> htPeaks = viewer.modelSet.htPeaks;
    for (int i = 0; i < peaks.size(); i++) {
      String peak = peaks.get(i);
      System.out.println("Jmol JSpecView.java peak="  + peak);
      BS bsPeak = htPeaks.get(peak);
      System.out.println("Jmol JSpecView.java bspeak="  + bsPeak);
      if (bsPeak == null) {
        htPeaks.put(peak, bsPeak = new BS());
        String satoms = PT.getQuotedAttribute(peak, "atoms");
        String select = PT.getQuotedAttribute(peak, "select");
        System.out.println("Jmol JSpecView.java satoms select " + satoms + " " + select);
        String script = "";
        if (satoms != null)
          script += "visible & (atomno="
              + PT.rep(satoms, ",", " or atomno=") + ")";
        else if (select != null)
          script += "visible & (" + select + ")";
        System.out.println("Jmol JSpecView.java script : " + script);
        bsPeak.or(viewer.getAtomBitSet(script));
      }
      System.out.println("Jmol JSpecView bsPeak now : " + bsPeak + " " + atomIndex);
      if (bsPeak.get(atomIndex))
        return peak;
    }
    return null;
  }


  private void sendJSpecView(String peak) {
    String msg = PT.getQuotedAttribute(peak, "title");
    if (msg != null)
      viewer.scriptEcho(Logger.debugging ? peak : msg);
    peak = viewer.fullName + "JSpecView: " + peak;
    Logger.info("Jmol.JSpecView.sendJSpecView Jmol>JSV " + peak);
    viewer.statusManager.syncSend(peak, ">", 0);
  }

  @Override
  public void setModel(int modelIndex) {
    int syncMode = ("sync on".equals(viewer.modelSet
        .getModelSetAuxiliaryInfoValue("jmolscript")) ? StatusManager.SYNC_DRIVER
        : viewer.statusManager.getSyncMode());
    if (syncMode != StatusManager.SYNC_DRIVER)
      return;
    String peak = (String) viewer.getModelAuxiliaryInfoValue(modelIndex, "jdxModelSelect");
    // problem is that SECOND load in jmol will not load new model in JSpecView
    if (peak != null)
      sendJSpecView(peak + " src=\"Jmol\"");
  }

  @Override
  public int getBaseModelIndex(int modelIndex) {
    String baseModel = (String) viewer.getModelAuxiliaryInfoValue(modelIndex,
        "jdxBaseModel");
    if (baseModel != null)
      for (int i = viewer.getModelCount(); --i >= 0;)
        if (baseModel
            .equals(viewer.getModelAuxiliaryInfoValue(i, "jdxModelID")))
          return i;
    return modelIndex;
  }

  @Override
  public String processSync(String script, int jsvMode) {
    switch (jsvMode) {
    default:
      return null;
    case JC.JSV_SEND:
      viewer.statusManager.syncSend(
          viewer.fullName + "JSpecView" + script.substring(9), ">", 0);
      return null;
    case JC.JSV_SETPEAKS:
      // JSpecView sending us the peak information it has
      String[] list = Escape.unescapeStringArray(script.substring(7));
      List<String> peaks = new List<String>();
      for (int i = 0; i < list.length; i++)
        peaks.addLast(list[i]);
      viewer.getModelSet().setModelAuxiliaryInfo(viewer.getCurrentModelIndex(),
          "jdxAtomSelect_1HNMR", peaks);
      return null;
    case JC.JSV_SELECT:
      // from JSpecView peak pick or possibly model change
      String filename = PT.getQuotedAttribute(script, "file");
      boolean isSimulation = filename.startsWith(FileManager.SIMULATION_PROTOCOL);
      //if (filename.startsWith(FileManager.SIMULATION_PROTOCOL + "MOL="))
      //filename = null; // from our sending; don't reload
      String modelID = (isSimulation ? "molfile" : PT.getQuotedAttribute(script, "model"));
      filename = PT.rep(filename, "#molfile", "");
      String baseModel = PT.getQuotedAttribute(script, "baseModel");
      String atoms = PT.getQuotedAttribute(script, "atoms");
      String select = PT.getQuotedAttribute(script, "select");
      String script2 = PT.getQuotedAttribute(script, "script");
      String id = (modelID == null ? null : (filename == null ? "" : filename
          + "#")
          + modelID);
      if ("".equals(baseModel))
        id += ".baseModel";
      int modelIndex = (id == null ? -3 : viewer.getModelIndexFromId(id));
      if (modelIndex == -2)
        return null; // file was found, or no file was indicated, but not this model -- ignore
      if (isSimulation)
        filename += "#molfile";
      script = (modelIndex == -1 && filename != null ? script = "load "
          + PT.esc(filename) : "");
      //script = PT.rep(script, FileManager.SIMULATION_PROTOCOL, "");
      if (id != null)
        script += ";model " + PT.esc(id);
      if (atoms != null)
        script += ";select visible & (@" + PT.rep(atoms, ",", " or @") + ")";
      else if (select != null)
        script += ";select visible & (" + select + ")";
      if (script2 != null)
        script += ";" + script2;
      return script;
    }
  }
}
