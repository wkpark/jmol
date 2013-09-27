/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-06-02 12:14:13 -0500 (Sat, 02 Jun 2007) $
 * $Revision: 7831 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolImageEncoder;
import org.jmol.i18n.GT;
import org.jmol.io.JmolBinary;
import org.jmol.io.JmolOutputChannel;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.Viewer.ACCESS;

abstract class OutputManagerAll extends OutputManager {
  
  protected Viewer viewer;
  protected double privateKey;
  
  @Override
  OutputManager setViewer(Viewer viewer, double privateKey) {
    this.viewer = viewer;
    this.privateKey = privateKey;
    return this;
  }
  
  /**
   * This method does too much. It can create byte data, it can
   * save text, it can save image data, it can do way more than just create images
   * 
   * @param params
   * @return null (canceled) or a message starting with OK or an error message
   */
  @Override
   Object createImage(Map<String, Object> params) {
    // this method may not be accessed, though public, unless 
    // accessed via viewer, which provides its private key.
    String type = (String) params.get("type");
    String fileName = (String) params.get("fileName");
    String text = (String) params.get("text");
    byte[] bytes = (byte[]) params.get("bytes");
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    JmolOutputChannel out = (JmolOutputChannel) params.get("outputChannel");
    boolean closeStream = (out == null);
    int len = -1;
    try {
      if (!viewer.checkPrivateKey(privateKey))
        return "ERROR: SECURITY";
      if (params.get("image") != null) {
        // _ObjExport needs to save the texture file
        getOrSaveImage(params);
        return fileName;
      } 
      // returns message starting with OK or an error message
      if (bytes != null) {
        if (out == null)
          out = viewer.openOutputChannel(privateKey, fileName, false);
        out.write(bytes, 0, bytes.length);
      } else if (text != null) {
        if (out == null)
          out = viewer.openOutputChannel(privateKey, fileName, true);
        out.append(text);
      } else {
        len = 1;
        Object bytesOrError = getOrSaveImage(params);
        if (bytesOrError instanceof String)
          return bytesOrError;
        bytes = (byte[]) bytesOrError;
        if (bytes != null)
          return (fileName == null ? bytes : new String(bytes));
        len = ((Integer) params.get("byteCount")).intValue();
      }
    } catch (IOException exc) {
      Logger.errorEx("IO Exception", exc);
      return exc.toString();
    } finally {
      if (out != null) {
        if (closeStream)
          out.closeChannel();
        len = out.getByteCount();
      }
    }
    return (len < 0 ? "Creation of " + fileName + " failed: "
        + viewer.getErrorMessageUn() : "OK " + type + " "
        + (len > 0 ? len + " " : "") + fileName
        + (quality == Integer.MIN_VALUE ? "" : "; quality=" + quality));
  }

  /**
   * 
   * @param params
   * @return bytes[] or (String) error or null
   * @throws IOException
   * 
   */
  @Override
  Object getOrSaveImage(Map<String, Object> params) throws IOException {
    byte[] bytes = null;
    String errMsg = null;
    String type = ((String) params.get("type")).toUpperCase();
    String fileName = (String) params.get("fileName");
    String[] scripts = (String[]) params.get("scripts");
    Object objImage = params.get("image");
    JmolOutputChannel channel = (JmolOutputChannel) params.get("outputChannel");
    boolean asBytes = (channel == null && fileName == null);
    boolean closeChannel = (channel == null && fileName != null);
    boolean releaseImage = (objImage == null);
    Object image = (objImage == null ? viewer.getScreenImageBuffer(null, true)
        : objImage);
    boolean isOK = false;
    try {
      if (image == null)
        return errMsg = viewer.getErrorMessage();
      if (channel == null)
        channel = viewer.openOutputChannel(privateKey, fileName, false);
      if (channel == null)
        return errMsg = "ERROR: canceled";
      String comment = null;
      Object stateData = null;
      params.put("date", viewer.apiPlatform.getDateFormat());
      if (type.startsWith("JP")) {
        type = TextFormat.simpleReplace(type, "E", "");
        if (type.equals("JPG64")) {
          params.put("outputChannelTemp", getOutputChannel(null, null));
          comment = "";
        } else {
          comment = (!asBytes ? (String) getWrappedState(null, null, image,
              false) : "");
        }
      } else if (type.startsWith("PNG")) {
        comment = "";
        boolean isPngj = type.equals("PNGJ");
        if (isPngj) {// get zip file data
          stateData = getWrappedState(fileName, scripts, image, true);
          if (stateData instanceof String)
            stateData = Viewer.getJmolVersion().getBytes();
        } else if (!asBytes) {
          stateData = ((String) getWrappedState(null, scripts, image, false))
              .getBytes();
        }
        if (stateData != null) {
          params.put("applicationData", stateData);
          params.put("applicationPrefix", "Jmol Type");
        }
        if (type.equals("PNGT"))
          params.put("transparentColor", Integer.valueOf(viewer
              .getBackgroundArgb()));
        type = "PNG";
      }
      if (comment != null)
        params.put("comment", comment.length() == 0 ? Viewer.getJmolVersion()
            : comment);
      String[] errRet = new String[1];
      isOK = createTheImage(image, type, channel, params, errRet);
      if (closeChannel)
        channel.closeChannel();
      if (isOK) {
        if (asBytes)
          bytes = channel.toByteArray();
        else if (params.containsKey("captureByteCount"))
          errMsg = "OK: " + params.get("captureByteCount").toString()
              + " bytes";
      } else {
        errMsg = errRet[0];
      }
    } finally {
      if (releaseImage)
        viewer.releaseScreenImage();
      params.put("byteCount", Integer.valueOf(bytes != null ? bytes.length
          : isOK ? channel.getByteCount() : -1));
    }
    return (errMsg == null ? bytes : errMsg);
  }

  /**
   * @param fileName
   * @param scripts
   * @param objImage 
   * @param asJmolZip
   * @return either byte[] (a full ZIP file) or String (just an embedded state
   *         script)
   * 
   */
  @Override
  Object getWrappedState(String fileName, String[] scripts, Object objImage, boolean asJmolZip) {
    int width = viewer.apiPlatform.getImageWidth(objImage);
    int height = viewer.apiPlatform.getImageHeight(objImage);
    if (width > 0 && !viewer.global.imageState && !asJmolZip
        || !viewer.global.preserveState)
      return "";
    String s = viewer.getStateInfo3(null, width, height);
    if (asJmolZip) {
      if (fileName != null)
        viewer.fileManager.clearPngjCache(fileName);
      // when writing a file, we need to make sure
      // the pngj cache for that file is cleared
      return JmolBinary.createZipSet(privateKey, viewer.fileManager, viewer,
          null, s, scripts, true);
    }
    // we remove local file references in the embedded states for images
    try {
      s = JC.embedScript(FileManager
          .setScriptFileReferences(s, ".", null, null));
    } catch (Throwable e) {
      // ignore if this uses too much memory
      Logger.error("state could not be saved: " + e.toString());
      s = "Jmol " + Viewer.getJmolVersion();
    }
    return s;
  }

  /**
   * @param objImage
   * @param type
   * @param out
   * @param params 
   * @param errRet
   * @return byte array if needed
   * @throws IOException
   */
  private boolean createTheImage(Object objImage, String type,
                                 JmolOutputChannel out,
                                 Map<String, Object> params, String[] errRet)
      throws IOException {
    type = type.substring(0, 1) + type.substring(1).toLowerCase();
    /**
     * @j2sNative
     * 
     * if (type == "Pdf")
     *  type += ":";
     *  
     */
    {
      // JSmol spoiler for PDF -- even if it is present, we couldn't run it.
    }

    JmolImageEncoder ie = (JmolImageEncoder) Interface
        .getInterface("org.jmol.image." + type + "Encoder");
    if (ie == null) {
      errRet[0] = "Image encoder type " + type + " not available";
      return false;
    } 
    return ie.createImage(viewer.apiPlatform, type, objImage, out, params, errRet);     
  }

  /////////////////////// general output including logging //////////////////////
  
  @Override
  String outputToFile(Map<String, Object> params) {
    return handleOutputToFile(params, true);
  }
  
  @Override
  JmolOutputChannel getOutputChannel(String fileName, String[] fullPath) {
    if (!viewer.haveAccess(ACCESS.ALL))
      return null;
    if (fileName != null) {
      fileName = getOutputFileNameFromDialog(fileName, Integer.MIN_VALUE);
      if (fileName == null)
        return null;
    }
    if (fullPath != null)
      fullPath[0] = fileName;
    String localName = (FileManager.isLocal(fileName) ? fileName : null);
    try {
      return viewer.openOutputChannel(privateKey, localName, false);
    } catch (IOException e) {
      Logger.info(e.toString());
      return null;
    }
  }

  /////////////////////// WRITE and CAPTURE command processing /////////////

  /**
   * 
   * @param params
   *        include fileName, type, text, bytes, scripts, quality, width,
   *        height, bsFrames, nVibes, fullPath
   * @return message
   */
  
  @Override
  String processWriteOrCapture(Map<String, Object> params) {
    String fileName = (String) params.get("fileName");
    if (fileName == null)
      return viewer.clipImageOrPasteText((String) params.get("text"));
    BS bsFrames = (BS) params.get("bsFrames");
    int nVibes = getInt(params, "nVibes", 0);
    return (bsFrames != null || nVibes != 0 ? processMultiFrameOutput(fileName,
        bsFrames, nVibes, params) : handleOutputToFile(params, true));
  }  
  
  private static int getInt(Map<String, Object> params, String key, int def) {
    Integer p = (Integer) params.get(key);
    return (p == null ? def : p.intValue());
  }
  
  private String processMultiFrameOutput(String fileName, BS bsFrames,
                                         int nVibes, Map<String, Object> params) {
    String info = "";
    int n = 0;
    int quality = getInt(params, "quality", -1);
    fileName = setFullPath(params, getOutputFileNameFromDialog(fileName, quality));
    if (fileName == null)
      return null;
    int ptDot = fileName.indexOf(".");
    if (ptDot < 0)
      ptDot = fileName.length();

    String froot = fileName.substring(0, ptDot);
    String fext = fileName.substring(ptDot);
    SB sb = new SB();
    if (bsFrames == null) {
      viewer.transformManager.vibrationOn = true;
      sb = new SB();
      for (int i = 0; i < nVibes; i++) {
        for (int j = 0; j < 20; j++) {
          viewer.transformManager.setVibrationT(j / 20f + 0.2501f);
          if (!writeFrame(++n, froot, fext, params, sb))
            return "ERROR WRITING FILE SET: \n" + info;
        }
      }
      viewer.setVibrationOff();
    } else {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1)) {
        viewer.setCurrentModelIndex(i);
        if (!writeFrame(++n, froot, fext, params, sb))
          return "ERROR WRITING FILE SET: \n" + info;
      }
    }
    if (info.length() == 0)
      info = "OK\n";
    return info + "\n" + n + " files created";
  }

  private String setFullPath(Map<String, Object> params,
                             String fileName) {
    String[] fullPath = (String[]) params.get("fullPath");
    if (fullPath != null)
      fullPath[0] = fileName;    
    if (fileName == null)
      return null;
    params.put("fileName", fileName);
    return fileName;
  }

  @Override
  String getOutputFromExport(Map<String, Object> params) {
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    String fileName = (String) params.get("fileName");
    if (fileName != null) {
      fileName = setFullPath(params, getOutputFileNameFromDialog(fileName, Integer.MIN_VALUE));
      if (fileName == null)
        return null;
    }
    viewer.mustRender = true;
    int saveWidth = viewer.dimScreen.width;
    int saveHeight = viewer.dimScreen.height;
    viewer.resizeImage(width, height, true, true, false);
    viewer.setModelVisibility();
    String data = viewer.repaintManager.renderExport(viewer.gdata,
        viewer.modelSet, params);
    viewer.resizeImage(saveWidth, saveHeight, true, true, true);
    return data;
  }

  /**
   * 
   * 
   * @param params
   * @return byte[] image data (PNG or JPG) or error message
   */

  @Override
  Object getImageAsBytes(Map<String, Object> params) {
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    int saveWidth = viewer.dimScreen.width;
    int saveHeight = viewer.dimScreen.height;
    viewer.mustRender = true;
    viewer.resizeImage(width, height, true, false, false);
    viewer.setModelVisibility();
    viewer.creatingImage = true;
    Object bytesOrStr = null;
    try {
      bytesOrStr = getOrSaveImage(params);
    } catch (IOException e) {
      bytesOrStr = e;
      viewer.setErrorMessage("Error creating image: " + e, null);
    } catch (Error er) {
      viewer.handleError(er, false);
      viewer.setErrorMessage("Error creating image: " + er, null);
      bytesOrStr = viewer.getErrorMessage();
    }
    viewer.creatingImage = false;
    viewer.resizeImage(saveWidth, saveHeight, true, false, true);
    return bytesOrStr;
  }

  /**
   * Generates file data and passes it on either to a FileOuputStream (Java) or
   * via POSTing to a url using a ByteOutputStream (JavaScript)
   * 
   * @param fileName
   * @param type
   *        one of: PDB PQR FILE PLOT
   * @param modelIndex
   * @param parameters
   * @return "OK..." or "" or null
   * 
   */
  @Override
  String writeFileData(String fileName, String type, int modelIndex,
                              Object[] parameters) {
    String[] fullPath = new String[1];
    JmolOutputChannel out = getOutputChannel(fileName, fullPath);
    if (out == null)
      return "";
    fileName = fullPath[0];
    String pathName = (type.equals("FILE") ? viewer.getFullPathName() : null);
    boolean getCurrentFile = (pathName != null && (pathName.equals("string")
        || pathName.indexOf("[]") >= 0 || pathName.equals("JSNode")));
    boolean asBytes = (pathName != null && !getCurrentFile);
    if (asBytes) {
      pathName = viewer.getModelSetPathName();
      if (pathName == null)
        return null; // zapped
    }
    // The OutputStringBuilder allows us to create strings or byte arrays
    // of a given type, passing just one parameter and maintaining an 
    // output stream all along. For JavaScript, this will be a ByteArrayOutputStream
    // which will then be posted to a server for a return that allows saving.
    out.setType(type);
    String msg = (type.equals("PDB") || type.equals("PQR") ? viewer
        .getPdbAtomData(null, out) : type.startsWith("PLOT") ? viewer.modelSet
        .getPdbData(modelIndex, type.substring(5), viewer
            .getSelectionSet(false), parameters, out) : getCurrentFile ? out
        .append(viewer.getCurrentFileAsString()).toString() : (String) viewer
        .getFileAsBytes(pathName, out));
    out.closeChannel();
    if (msg != null)
      msg = "OK " + msg + " " + fileName;
    return msg;
  }

  private boolean writeFrame(int n, String froot, String fext,
                             Map<String, Object> params, SB sb) {
    String fileName = "0000" + n;
    fileName = setFullPath(params, froot + fileName.substring(fileName.length() - 4) + fext);
    String msg = handleOutputToFile(params, false);
    viewer.scriptEcho(msg);
    sb.append(msg).append("\n");
    return msg.startsWith("OK");
  }

  private String getOutputFileNameFromDialog(String fileName, int quality) {
    if (fileName == null || viewer.isKiosk)
      return null;
    boolean useDialog = (fileName.indexOf("?") == 0);
    if (useDialog)
      fileName = fileName.substring(1);
    useDialog |= viewer.isApplet() && (fileName.indexOf("http:") < 0);
    fileName = FileManager.getLocalPathForWritingFile(viewer, fileName);
    if (useDialog)
      fileName = viewer.dialogAsk(quality == Integer.MIN_VALUE ? "Save"
          : "Save Image", fileName);
    return fileName;
  }

  /**
   * general routine for creating an image or writing data to a file
   * 
   * passes request to statusManager to pass along to app or applet
   * jmolStatusListener interface
   * 
   * @param params
   *        include: fileName: starts with ? --> use file dialog; 
   *        type: PNG, JPG, etc.; text: String to output; bytes:
   *        byte[] or null if an image; scripts for scenes; 
   *        quality: for JPG and PNG; width: image width;
   *        height: image height; fullPath: String[] return
   *        
   * @param doCheck
   * @return null (canceled) or a message starting with OK or an error message
   */
  private String handleOutputToFile(Map<String, Object> params, boolean doCheck) {
    /*
     * 
     * org.jmol.image.AviCreator does create AVI animations from JPEGs
     * but these aren't read by standard readers, so that's pretty much useless.
     * 
     */

    Object ret = null;
    String fileName = (String) params.get("fileName");
    String type = (String) params.get("type");
    String text = (String) params.get("text");
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    
    int captureMode = getInt(params, "captureMode", Integer.MIN_VALUE);
    if (captureMode != Integer.MIN_VALUE && !viewer.allowCapture())
      return "ERROR: Cannot capture on this platform.";
    boolean mustRender = (quality != Integer.MIN_VALUE);
    // localName will be fileName only if we are able to write to disk.
    String localName = null;
    if (captureMode != Integer.MIN_VALUE) {
      doCheck = false; // will be checked later
      mustRender = false;
      type = "GIF";
    }
    if (doCheck)
      fileName = getOutputFileNameFromDialog(fileName, quality);
    fileName = setFullPath(params, fileName);
    if (fileName == null)
      return null;
    // JSmol/HTML5 WILL produce a localName now
    if (FileManager.isLocal(fileName))
      localName = fileName;
    int saveWidth = viewer.dimScreen.width;
    int saveHeight = viewer.dimScreen.height;
    viewer.creatingImage = true;
    if (mustRender) {
      viewer.mustRender = true;
      viewer.resizeImage(width, height, true, false, false);
      viewer.setModelVisibility();
    }
    try {
      if (type.equals("JMOL"))
        type = "ZIPALL";
      if (type.equals("ZIP") || type.equals("ZIPALL")) {
        String[] scripts = (String[]) params.get("scripts");
        if (scripts != null && type.equals("ZIP"))
          type = "ZIPALL";
        ret = JmolBinary.createZipSet(privateKey, viewer.fileManager, viewer,
            localName, text, scripts, type.equals("ZIPALL"));
      } else if (type.equals("SCENE")) {
        ret = (viewer.isJS ? "ERROR: Not Available" : createSceneSet(fileName,
            text, width, height));
      } else {
        // see if application wants to do it (returns non-null String)
        // both Jmol application and applet return null
        byte[] bytes = (byte[]) params.get("bytes");
        ret = viewer.statusManager.createImage(fileName, type, text, bytes,
            quality);
        if (ret == null) {
          // allow Jmol to do it            
          String msg = null;
          if (captureMode != Integer.MIN_VALUE) {
            JmolOutputChannel out = null;
            Map<String, Object> cparams = viewer.captureParams;
            switch (captureMode) {
            case T.movie:
              if (cparams != null)
                ((JmolOutputChannel) cparams.get("outputChannel"))
                    .closeChannel();
              out = getOutputChannel(localName, null);
              if (out == null) {
                ret = msg = "ERROR: capture canceled";
                viewer.captureParams = null;
              } else {
                localName = out.getFileName();
                msg = type + "_STREAM_OPEN " + localName;
                viewer.captureParams = params;
                params.put("captureFileName", localName);
                params.put("captureCount", Integer.valueOf(1));
                params.put("captureMode", Integer.valueOf(T.movie));
              }
              break;
            default:
              if (cparams == null) {
                ret = msg = "ERROR: capture not active";
              } else {
                params = cparams;
                switch (captureMode) {
                default:
                  ret = msg = "ERROR: CAPTURE MODE=" + captureMode + "?";
                  break;
                case T.add:
                  if (Boolean.FALSE == params.get("captureEnabled")) {
                    ret = msg = "capturing OFF; use CAPTURE ON/END/CANCEL to continue";
                  } else {
                    int count = getInt(params, "captureCount", 1);
                    params.put("captureCount", Integer.valueOf(++count));
                    msg = type + "_STREAM_ADD " + count;
                  }
                  break;
                case T.on:
                case T.off:
                  params = cparams;
                  params.put("captureEnabled",
                      (captureMode == T.on ? Boolean.TRUE : Boolean.FALSE));
                  ret = type + "_STREAM_"
                      + (captureMode == T.on ? "ON" : "OFF");
                  params.put("captureMode", Integer.valueOf(T.add));
                  break;
                case T.end:
                case T.cancel:
                  params = cparams;
                  params.put("captureMode", Integer.valueOf(captureMode));
                  fileName = (String) params.get("captureFileName");
                  msg = type + "_STREAM_"
                      + (captureMode == T.end ? "CLOSE " : "CANCEL ")
                      + params.get("captureFileName");
                  viewer.captureParams = null;
                  viewer.prompt(GT._("Capture")
                      + ": "
                      + (captureMode == T.cancel ? GT._("canceled") : GT._(
                          "{0} saved", new Object[] { fileName })), "OK", null,
                      true);
                }
                break;
              }
              break;
            }
            if (out != null)
              params.put("outputChannel", out);
          }
          params.put("fileName", localName);
          if (ret == null)
            ret = createImage(params);
          if (ret instanceof String)
            viewer.statusManager.createImage((String) ret, type, null, null,
                quality);
          if (msg != null)
            viewer.showString(msg + " (" + params.get("captureByteCount")
                + " bytes)", false);
        }
      }
      if (ret instanceof byte[])
        ret = "OK "
            + JmolBinary.postByteArray(viewer.fileManager, fileName,
                (byte[]) ret);
    } catch (Throwable er) {
      //er.printStackTrace();
      Logger.error(viewer.setErrorMessage(
          (String) (ret = "ERROR creating image??: " + er), null));
    }
    viewer.creatingImage = false;
    if (quality != Integer.MIN_VALUE) {
      viewer.resizeImage(saveWidth, saveHeight, true, false, true);
    }
    return (String) ret;
  }

  private String createSceneSet(String sceneFile, String type, int width,
                                int height) {
    String script0 = viewer.getFileAsString(sceneFile);
    if (script0 == null)
      return "no such file: " + sceneFile;
    sceneFile = TextFormat.simpleReplace(sceneFile, ".spt", "");
    String fileRoot = sceneFile;
    String fileExt = type.toLowerCase();
    String[] scenes = TextFormat.splitChars(script0, "pause scene ");
    Map<String, String> htScenes = new Hashtable<String, String>();
    JmolList<Integer> list = new JmolList<Integer>();
    String script = JmolBinary.getSceneScript(scenes, htScenes, list);
    if (Logger.debugging)
      Logger.debug(script);
    script0 = TextFormat.simpleReplace(script0, "pause scene", "delay "
        + viewer.animationManager.lastFrameDelay + " # scene");
    String[] str = new String[] { script0, script, null };
    viewer.saveState("_scene0");
    int nFiles = 0;
    if (scenes[0] != "")
      viewer.zap(true, true, false);
    int iSceneLast = -1;
    for (int i = 0; i < scenes.length - 1; i++) {
      try {
        int iScene = list.get(i).intValue();
        if (iScene > iSceneLast)
          viewer.showString("Creating Scene " + iScene, false);
        viewer.eval.runScript(scenes[i]);
        if (iScene <= iSceneLast)
          continue;
        iSceneLast = iScene;
        str[2] = "all"; // full PNGJ
        String fileName = fileRoot + "_scene_" + iScene + ".all." + fileExt;
        Map<String, Object> params = new Hashtable<String, Object>();
        params.put("fileName", fileName);
        params.put("type", "PNGJ");
        params.put("scripts", str);
        params.put("width", Integer.valueOf(width));
        params.put("height", Integer.valueOf(height));
        String msg = handleOutputToFile(params, false);
        str[0] = null; // script0 only saved in first file
        str[2] = "min"; // script only -- for fast loading
        fileName = fileRoot + "_scene_" + iScene + ".min." + fileExt;
        params.put("fileName", fileName);
        params.put("width", Integer.valueOf(Math.min(width, 200)));
        params.put("height", Integer.valueOf(Math.min(height, 200)));
        msg += "\n" + handleOutputToFile(params, false);
        viewer.showString(msg, false);
        nFiles += 2;
      } catch (Exception e) {
        return "script error " + e.toString();
      }
    }
    try {
      viewer.eval.runScript(viewer.getSavedState("_scene0"));
    } catch (Exception e) {
      // ignore
    }
    return "OK " + nFiles + " files created";
  }

  @Override
  String setLogFile(String value) {
    String path = null;
    String logFilePath = viewer.getLogFilePath();
    /**
     * @j2sNative
     * 
     * if (typeof value == "function") path = value;
     * 
     */
    if (logFilePath == null || value.indexOf("\\") >= 0) {
      value = null;
    } else if (value.startsWith("http://") || value.startsWith("https://")) {
      // allow for remote logging
      path = value;
    } else if (value.indexOf("/") >= 0) {
      value = null;
    } else if (value.length() > 0) {
      if (!value.startsWith("JmolLog_"))
        value = "JmolLog_" + value;
      path = viewer.getAbsolutePath(privateKey, logFilePath + value);
    }
    if (path == null)
      value = null;
    else
      Logger.info(GT._("Setting log file to {0}", path));
    if (value == null || !viewer.haveAccess(ACCESS.ALL)) {
      Logger.info(GT._("Cannot set log file path."));
      value = null;
    } else {
      viewer.logFileName = path;
      viewer.global.setS("_logFile", viewer.isApplet() ? value : path);
    }
    return value;
  }

  @Override
  void logToFile(String data) {
    try {
      boolean doClear = (data.equals("$CLEAR$"));
      if (data.indexOf("$NOW$") >= 0)
        data = TextFormat.simpleReplace(data, "$NOW$", viewer.apiPlatform
            .getDateFormat());
      if (viewer.logFileName == null) {
        Logger.info(data);
        return;
      }
      JmolOutputChannel out = viewer.openLogFile(privateKey, !doClear);
      if (!doClear) {
        int ptEnd = data.indexOf('\0');
        if (ptEnd >= 0)
          data = data.substring(0, ptEnd);
        out.append(data);
        if (ptEnd < 0)
          out.append("\n");
      }
      Logger.info(out.closeChannel());
    } catch (Exception e) {
      if (Logger.debugging)
        Logger.debug("cannot log " + data);
    }
  }  

}
