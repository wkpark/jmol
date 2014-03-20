package org.jmol.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.i18n.GT;
import org.jmol.io.JmolBinary;
import org.jmol.java.BS;

import javajs.api.GenericImageEncoder;
import javajs.util.Binary;
import javajs.util.OC;
import javajs.util.List;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Logger;
import org.jmol.util.Txt;
import org.jmol.viewer.Viewer.ACCESS;

abstract class OutputManager {

  abstract protected String getLogPath(String fileName);

  abstract String clipImageOrPasteText(String text);

  abstract String getClipboardText();

  abstract OC openOutputChannel(double privateKey,
                                               String fileName,
                                               boolean asWriter,
                                               boolean asAppend)
      throws IOException;

  abstract protected String createSceneSet(String sceneFile, String type, int width,
                                           int height);
           
  protected Viewer vwr;
  protected double privateKey;

  OutputManager setViewer(Viewer vwr, double privateKey) {
    this.vwr = vwr;
    this.privateKey = privateKey;
    return this;
  }

  /**
   * From handleOutputToFile, write text, byte[], or image data to a 
   * file; 
   * 
   * @param params
   * @return null (canceled) or byte[] or String message starting with OK or an error
   *         message; in the case of params.image != null, return the fileName
   */

  private String writeToOutputChannel(Map<String, Object> params) {
    String type = (String) params.get("type");
    String fileName = (String) params.get("fileName");
    String text = (String) params.get("text");
    byte[] bytes = (byte[]) params.get("bytes");
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    OC out = (OC) params.get("outputChannel");
    boolean closeStream = (out == null);
    int len = -1;
    String ret = null;
    try {
      if (!vwr.checkPrivateKey(privateKey))
        return "ERROR: SECURITY";
      if (bytes != null) {
        if (out == null)
          out = openOutputChannel(privateKey, fileName, false, false);
        out.write(bytes, 0, bytes.length);
      } else if (text != null && !type.equals("ZIPDATA")) {
        if (out == null)
          out = openOutputChannel(privateKey, fileName, true, false);
        out.append(text);
      } else {
        String errMsg = (String) getOrSaveImage(params);
        if (errMsg != null)
          return errMsg;
        len = ((Integer) params.get("byteCount")).intValue();
      }
    } catch (Exception exc) {
      Logger.errorEx("IO Exception", exc);
      return exc.toString();
    } finally {
      if (out != null) {
        if (closeStream)
          ret = out.closeChannel();
        len = out.getByteCount();
      }
    }
    int pt = fileName.indexOf("?POST?");
    if (pt >= 0)
    	fileName = fileName.substring(0,  pt);
    return (len < 0 ? "Creation of " + fileName + " failed: "
        + (ret == null ? vwr.getErrorMessageUn() : ret) : "OK " + type + " "
        + (len > 0 ? len + " " : "") + fileName
        + (quality == Integer.MIN_VALUE ? "" : "; quality=" + quality));
  }

  /**
   * 
   * Creates an image of params.type form -- PNG, PNGJ, PNGT, JPG, JPG64, PDF,
   * PPM.
   * 
   * From createImage and getImageAsBytes
   * 
   * @param params
   *        include fileName, type, text, bytes, image, scripts, appendix,
   *        quality, outputStream, and type-specific parameters. If
   *        params.outputChannel != null, then we are passing back the data, and
   *        the channel will not be closed.
   * 
   * @return bytes[] if params.fileName==null and params.outputChannel==null
   *         otherwise, return a message string or null
   * @throws Exception
   * 
   */

  private Object getOrSaveImage(Map<String, Object> params) throws Exception {
    byte[] bytes = null;
    String errMsg = null;
    String type = ((String) params.get("type")).toUpperCase();
    String fileName = (String) params.get("fileName");
    String[] scripts = (String[]) params.get("scripts");
    Object objImage = params.get("image");
    int[] rgbbuf = (int[]) params.get("rgbbuf");
    OC out = (OC) params.get("outputChannel");
    boolean asBytes = (out == null && fileName == null);
    boolean closeChannel = (out == null && fileName != null);
    boolean releaseImage = (objImage == null);
    Object image = (type.equals("ZIPDATA") ? "" : rgbbuf != null ? rgbbuf
        : objImage != null ? objImage : vwr.getScreenImageBuffer(null, true));
    boolean isOK = false;
    try {
      if (image == null)
        return errMsg = vwr.getErrorMessage();
      if (out == null)
        out = openOutputChannel(privateKey, fileName, false, false);
      if (out == null)
        return errMsg = "ERROR: canceled";
      fileName = out.getFileName();
      String comment = null;
      Object stateData = null;
      params.put("date", vwr.apiPlatform.getDateFormat(false));
      if (type.startsWith("JP")) {
        type = PT.rep(type, "E", "");
        if (type.equals("JPG64")) {
          params.put("outputChannelTemp", getOutputChannel(null, null));
          comment = "";
        } else {
          comment = (!asBytes ? (String) getWrappedState(null, null, image,
              null) : "");
        }
        params.put("jpgAppTag", JmolBinary.JPEG_CONTINUE_STRING);
      } else if (type.equals("PDF")) {
        comment = "";
      } else if (type.startsWith("PNG")) {
        comment = "";
        boolean isPngj = type.equals("PNGJ");
        if (isPngj) {// get zip file data
          OC outTemp = getOutputChannel(null, null);
          getWrappedState(fileName, scripts, image, outTemp);
          stateData = outTemp.toByteArray();
        } else if (rgbbuf == null && !asBytes) {
          stateData = ((String) getWrappedState(null, scripts, image, null))
              .getBytes();
        }
        if (stateData != null) {
          params.put("pngAppData", stateData);
          params.put("pngAppPrefix", "Jmol Type");
        }
        if (type.equals("PNGT"))
          params.put("transparentColor",
              Integer.valueOf(vwr.getBackgroundArgb()));
        type = "PNG";
      }
      if (comment != null)
        params.put("comment", comment.length() == 0 ? Viewer.getJmolVersion()
            : comment);
      String[] errRet = new String[1];
      isOK = createTheImage(image, type, out, params, errRet);
      if (closeChannel)
        out.closeChannel();
      if (isOK) {
        if (params.containsKey("captureMsg"))
          vwr.prompt((String) params.get("captureMsg"), "OK", null, true);

        if (asBytes)
          bytes = out.toByteArray();
        else if (params.containsKey("captureByteCount"))
          errMsg = "OK: " + params.get("captureByteCount").toString()
              + " bytes";
      } else {
        errMsg = errRet[0];
      }
    } finally {
      if (releaseImage)
        vwr.releaseScreenImage();
      params.put("byteCount", Integer.valueOf(bytes != null ? bytes.length
          : isOK ? out.getByteCount() : -1));
      if (objImage != null) {
        // _ObjExport is saving the texture file -- just return file name, regardless of whether there is an error
        return fileName;
      }
    }
    return (errMsg == null ? bytes : errMsg);
  }

  /**
   * @param fileName
   * @param scripts
   * @param objImage
   * @param out
   * @return either byte[] (a full ZIP file) or String (just an embedded state
   *         script)
   * 
   */

  Object getWrappedState(String fileName, String[] scripts, Object objImage,
                         OC out) {
    int width = vwr.apiPlatform.getImageWidth(objImage);
    int height = vwr.apiPlatform.getImageHeight(objImage);
    if (width > 0 && !vwr.g.imageState && out == null
        || !vwr.g.preserveState)
      return "";
    String s = vwr.getStateInfo3(null, width, height);
    if (out != null) {
      if (fileName != null)
        vwr.fileManager.clearPngjCache(fileName);
      // when writing a file, we need to make sure
      // the pngj cache for that file is cleared
      return createZipSet(s, scripts, true, out);
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
   */
  private boolean createTheImage(Object objImage, String type, OC out,
                                 Map<String, Object> params, String[] errRet) {
    type = type.substring(0, 1) + type.substring(1).toLowerCase();
    if (type.equals("Zipdata")) {
      @SuppressWarnings("unchecked")
      List<Object> v = (List<Object>) params.get("imageData");
      if (v.size() >= 2 && v.get(0).equals("_IMAGE_")) {
        objImage = null;
        v.remove(0);
        params.put("pngImgData", v.remove(0));
        OC oz = getOutputChannel(null, null);
        errRet[0] = writeZipFile(oz, v, "OK JMOL");
        params.put("type", "PNGJ");
        type = "Png";
        params.put("pngAppPrefix", "Jmol Type");
        params.put("pngAppData", oz.toByteArray());
      } else if (v.size() == 1) {
        byte[] b = (byte[]) v.remove(0);
        out.write(b, 0, b.length);
        return true;
      } else {
        errRet[0] = writeZipFile(out, v, "OK JMOL");
        return true;
      }
    }
    GenericImageEncoder ie = (GenericImageEncoder) Interface
        .getInterface("javajs.img." + type + "Encoder");
    if (ie == null) {
      errRet[0] = "Image encoder type " + type + " not available";
      return false;
    }
    boolean doClose = true;
    try {
      int w = objImage == null ? -1 : PT.isAI(objImage) ? ((Integer) params
          .get("width")).intValue() : vwr.apiPlatform
          .getImageWidth(objImage);
      int h = objImage == null ? -1 : PT.isAI(objImage) ? ((Integer) params
          .get("height")).intValue() : vwr.apiPlatform
          .getImageHeight(objImage);
      params.put("imageWidth", Integer.valueOf(w));
      params.put("imageHeight", Integer.valueOf(h));
      int[] pixels = encodeImage(w, h, objImage);
      if (pixels != null)
        params.put("imagePixels", pixels);
      params.put("logging", Boolean.valueOf(Logger.debugging));
      // GIF capture may not close output channel
      doClose = ie.createImage(type, out, params);
    } catch (Exception e) {
      errRet[0] = e.toString();
      out.cancel();
      doClose = true;
    } finally {
      if (doClose)
        out.closeChannel();
    }
    return (errRet[0] == null);
  }
  
  /**
   * general image encoder, allows for BufferedImage, int[], or HTML5 2D canvas
   * 
   * @param width 
   * @param height 
   * @param objImage
   * @return linear int[] array of ARGB values
   * @throws Exception
   */
  private int[] encodeImage(int width, int height, Object objImage)
      throws Exception {
    if (width < 0)
      return null;
    int[] pixels;
    if (PT.isAI(objImage)) {
      pixels = (int[]) objImage;
    } else {
      /**
       * @j2sNative
       * 
       *            pixels = null;
       * 
       */
      {
        pixels = new int[width * height];
      }
      pixels = vwr.apiPlatform.grabPixels(objImage, width, height, pixels, 0,
          height);
    }
    return pixels;
  }


  /////////////////////// general output including logging //////////////////////

  String outputToFile(Map<String, Object> params) {
    return handleOutputToFile(params, true);
  }

  OC getOutputChannel(String fileName, String[] fullPath) {
    if (!vwr.haveAccess(ACCESS.ALL))
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
      return openOutputChannel(privateKey, localName, false, false);
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

  String processWriteOrCapture(Map<String, Object> params) {
    String fileName = (String) params.get("fileName");
    if (fileName == null)
      return vwr.clipImageOrPasteText((String) params.get("text"));
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
    fileName = setFullPath(params, getOutputFileNameFromDialog(fileName,
        quality));
    if (fileName == null)
      return null;
    int ptDot = fileName.indexOf(".");
    if (ptDot < 0)
      ptDot = fileName.length();

    String froot = fileName.substring(0, ptDot);
    String fext = fileName.substring(ptDot);
    SB sb = new SB();
    if (bsFrames == null) {
      vwr.tm.vibrationOn = true;
      sb = new SB();
      for (int i = 0; i < nVibes; i++) {
        for (int j = 0; j < 20; j++) {
          vwr.tm.setVibrationT(j / 20f + 0.2501f);
          if (!writeFrame(++n, froot, fext, params, sb))
            return "ERROR WRITING FILE SET: \n" + info;
        }
      }
      vwr.setVibrationOff();
    } else {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1)) {
        vwr.setCurrentModelIndex(i);
        if (!writeFrame(++n, froot, fext, params, sb))
          return "ERROR WRITING FILE SET: \n" + info;
      }
    }
    if (info.length() == 0)
      info = "OK\n";
    return info + "\n" + n + " files created";
  }

  private String setFullPath(Map<String, Object> params, String fileName) {
    String[] fullPath = (String[]) params.get("fullPath");
    if (fullPath != null)
      fullPath[0] = fileName;
    if (fileName == null)
      return null;
    params.put("fileName", fileName);
    return fileName;
  }

  String getOutputFromExport(Map<String, Object> params) {
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    String fileName = (String) params.get("fileName");
    if (fileName != null) {
      fileName = setFullPath(params, getOutputFileNameFromDialog(fileName,
          Integer.MIN_VALUE));
      if (fileName == null)
        return null;
    }
    vwr.mustRender = true;
    int saveWidth = vwr.dimScreen.width;
    int saveHeight = vwr.dimScreen.height;
    vwr.resizeImage(width, height, true, true, false);
    vwr.setModelVisibility();
    String data = vwr.repaintManager.renderExport(vwr.gdata,
        vwr.ms, params);
    vwr.resizeImage(saveWidth, saveHeight, true, true, true);
    return data;
  }

  /**
   * Called when a simple image is required -- from x=getProperty("image") or
   * for a simple preview PNG image for inclusion in a ZIP file from write
   * xxx.zip or xxx.jmol, or for a PNGJ or PNG image that is being posted
   * because of a URL that contains "?POST?_PNG_" or
   * ?POST?_PNGJ_" or ?POST?_PNGJBIN_".
   * 
   * @param type
   * @param width
   * @param height
   * @param quality
   * @param errMsg
   * @return image bytes or, if an error, null and an error message
   */

  byte[] getImageAsBytes(String type, int width, int height, int quality,
                         String[] errMsg) {
    int saveWidth = vwr.dimScreen.width;
    int saveHeight = vwr.dimScreen.height;
    vwr.mustRender = true;
    vwr.resizeImage(width, height, true, false, false);
    vwr.setModelVisibility();
    vwr.creatingImage = true;
    byte[] bytes = null;
    try {
      Map<String, Object> params = new Hashtable<String, Object>();
      params.put("type", type);
      if (quality > 0)
        params.put("quality", Integer.valueOf(quality));
      Object bytesOrError = getOrSaveImage(params);
      if (bytesOrError instanceof String)
        errMsg[0] = (String) bytesOrError;
      else
        bytes = (byte[]) bytesOrError;
    } catch (Exception e) {
      errMsg[0] = e.toString();
      vwr.setErrorMessage("Error creating image: " + e, null);
    } catch (Error er) {
      vwr.handleError(er, false);
      vwr.setErrorMessage("Error creating image: " + er, null);
      errMsg[0] = vwr.getErrorMessage();
    }
    vwr.creatingImage = false;
    vwr.resizeImage(saveWidth, saveHeight, true, false, true);
    return bytes;
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

  String writeFileData(String fileName, String type, int modelIndex,
                       Object[] parameters) {
    String[] fullPath = new String[1];
    OC out = getOutputChannel(fileName, fullPath);
    if (out == null)
      return "";
    fileName = fullPath[0];
    String pathName = (type.equals("FILE") ? vwr.getFullPathName(false) : null);
    boolean getCurrentFile = (pathName != null && (pathName.equals("string")
        || pathName.indexOf("[]") >= 0 || pathName.equals("JSNode")));
    boolean asBytes = (pathName != null && !getCurrentFile);
    if (asBytes) {
      pathName = vwr.getModelSetPathName();
      if (pathName == null)
        return null; // zapped
    }
    // The OutputStringBuilder allows us to create strings or byte arrays
    // of a given type, passing just one parameter and maintaining an 
    // output stream all along. For JavaScript, this will be a ByteArrayOutputStream
    // which will then be posted to a server for a return that allows saving.
    out.setType(type);
    String msg = (type.equals("PDB") || type.equals("PQR") ? vwr
        .getPdbAtomData(null, out) : type.startsWith("PLOT") ? vwr.ms
        .getPdbData(modelIndex, type.substring(5), vwr
            .getSelectedAtoms(), parameters, out) : getCurrentFile ? out
        .append(vwr.getCurrentFileAsString()).toString() : (String) vwr
        .getFileAsBytes(pathName, out));
    out.closeChannel();
    if (msg != null)
      msg = "OK " + msg + " " + fileName;
    return msg;
  }

  private boolean writeFrame(int n, String froot, String fext,
                             Map<String, Object> params, SB sb) {
    String fileName = "0000" + n;
    fileName = setFullPath(params, froot
        + fileName.substring(fileName.length() - 4) + fext);
    String msg = handleOutputToFile(params, false);
    vwr.scriptEcho(msg);
    sb.append(msg).append("\n");
    return msg.startsWith("OK");
  }

  private String getOutputFileNameFromDialog(String fileName, int quality) {
    if (fileName == null || vwr.isKiosk)
      return null;
    boolean useDialog = fileName.startsWith("?");
    if (useDialog)
    	fileName = fileName.substring(1);
    useDialog |= vwr.isApplet() && (fileName.indexOf("http:") < 0);
    fileName = FileManager.getLocalPathForWritingFile(vwr, fileName);
    if (useDialog)
      fileName = vwr.dialogAsk(quality == Integer.MIN_VALUE ? "Save"
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
   *        include: fileName: starts with ? --> use file dialog; type: PNG,
   *        JPG, etc.; text: String to output; bytes: byte[] or null if an
   *        image; scripts for scenes; quality: for JPG and PNG; width: image
   *        width; height: image height; fullPath: String[] return
   * 
   * @param doCheck
   * @return null (canceled) or a message starting with OK or an error message
   */
  protected String handleOutputToFile(Map<String, Object> params,
                                      boolean doCheck) {

    // org.jmol.image.AviCreator does create AVI animations from JPEGs
    //but these aren't read by standard readers, so that's pretty much useless.

    String sret = null;
    String fileName = (String) params.get("fileName");
    if (fileName == null)
      return null;
    String type = (String) params.get("type");
    String text = (String) params.get("text");
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    String captureMode = (String) params.get("captureMode");
    if (captureMode != null && !vwr.allowCapture())
      return "ERROR: Cannot capture on this platform.";
    boolean mustRender = (quality != Integer.MIN_VALUE);
    // localName will be fileName only if we are able to write to disk.
    String localName = null;
    if (captureMode != null) {
      doCheck = false; // will be checked later
      mustRender = false;
      type = "GIF";
    }
    if (doCheck)
      fileName = getOutputFileNameFromDialog(fileName, quality);
    fileName = setFullPath(params, fileName);
    if (fileName == null)
      return null;
    params.put("fileName", fileName);
    // JSmol/HTML5 WILL produce a localName now
    if (FileManager.isLocal(fileName))
      localName = fileName;
    int saveWidth = vwr.dimScreen.width;
    int saveHeight = vwr.dimScreen.height;
    vwr.creatingImage = true;
    if (mustRender) {
      vwr.mustRender = true;
      vwr.resizeImage(width, height, true, false, false);
      vwr.setModelVisibility();
    }
    try {
      if (type.equals("JMOL"))
        type = "ZIPALL";
      if (type.equals("ZIP") || type.equals("ZIPALL")) {
        String[] scripts = (String[]) params.get("scripts");
        if (scripts != null && type.equals("ZIP"))
          type = "ZIPALL";
        OC out = getOutputChannel(fileName, null);
        sret = createZipSet(text, scripts, type.equals("ZIPALL"), out);
      } else if (type.equals("SCENE")) {
        sret = createSceneSet(fileName, text, width, height);
      } else {
        // see if application wants to do it (returns non-null String)
        // both Jmol application and applet return null
        byte[] bytes = (byte[]) params.get("bytes");
        // String return here
        sret = vwr.statusManager.createImage(fileName, type, text, bytes,
            quality);
        if (sret == null) {
          // allow Jmol to do it            
          String msg = null;
          if (captureMode != null) {
            OC out = null;
            Map<String, Object> cparams = vwr.captureParams;
            int imode = "ad on of en ca mo ".indexOf(captureMode
                .substring(0, 2));
            //           0  3  6  9  12 15
            switch (imode) {
            case 15:
              if (cparams != null)
                ((OC) cparams.get("outputChannel")).closeChannel();
              out = getOutputChannel(localName, null);
              if (out == null) {
                sret = msg = "ERROR: capture canceled";
                vwr.captureParams = null;
              } else {
                localName = out.getFileName();
                msg = type + "_STREAM_OPEN " + localName;
                vwr.captureParams = params;
                params.put("captureFileName", localName);
                params.put("captureCount", Integer.valueOf(1));
                params.put("captureMode", "movie");
              }
              break;
            default:
              if (cparams == null) {
                sret = msg = "ERROR: capture not active";
              } else {
                params = cparams;
                switch (imode) {
                default:
                  sret = msg = "ERROR: CAPTURE MODE=" + captureMode + "?";
                  break;
                case 0: //add:
                  if (Boolean.FALSE == params.get("captureEnabled")) {
                    sret = msg = "capturing OFF; use CAPTURE ON/END/CANCEL to continue";
                  } else {
                    int count = getInt(params, "captureCount", 1);
                    params.put("captureCount", Integer.valueOf(++count));
                    msg = type + "_STREAM_ADD " + count;
                  }
                  break;
                case 3: //on:
                case 6: //off:
                  params = cparams;
                  params
                      .put("captureEnabled",
                          (captureMode.equals("on") ? Boolean.TRUE
                              : Boolean.FALSE));
                  sret = type + "_STREAM_"
                      + (captureMode.equals("on") ? "ON" : "OFF");
                  params.put("captureMode", "add");
                  break;
                case 9:// end:
                case 12:// cancel:
                  params = cparams;
                  params.put("captureMode", captureMode);
                  fileName = (String) params.get("captureFileName");
                  msg = type + "_STREAM_"
                      + (captureMode.equals("end") ? "CLOSE " : "CANCEL ")
                      + params.get("captureFileName");
                  vwr.captureParams = null;
                  params.put("captureMsg",
                      GT._("Capture")
                          + ": "
                          + (captureMode.equals("cancel") ? GT._("canceled")
                              : GT.o(GT._("{0} saved"), fileName)));
                }
                break;
              }
              break;
            }
            if (out != null)
              params.put("outputChannel", out);
          }
          if (localName != null)
            params.put("fileName", localName);
          if (sret == null)
            sret = writeToOutputChannel(params);
          vwr.statusManager.createImage(sret, type, null, null, quality);
          if (msg != null)
            vwr.showString(msg + " (" + params.get("captureByteCount")
                + " bytes)", false);
        }
      }
    } catch (Throwable er) {
      //er.printStackTrace();
      Logger.error(vwr.setErrorMessage(sret = "ERROR creating image??: "
          + er, null));
    } finally {
      vwr.creatingImage = false;
      if (quality != Integer.MIN_VALUE)
        vwr.resizeImage(saveWidth, saveHeight, true, false, true);
    }
    return sret;
  }

  String setLogFile(String value) {
    String path = null;
    String logFilePath = vwr.getLogFilePath();
    /**
     * @j2sNative
     * 
     *            if (typeof value == "function") path = value;
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
      path = getLogPath(logFilePath + value);
    }
    if (path == null)
      value = null;
    else
      Logger.info(GT.o(GT._("Setting log file to {0}"), path));
    if (value == null || !vwr.haveAccess(ACCESS.ALL)) {
      Logger.info(GT._("Cannot set log file path."));
      value = null;
    } else {
      vwr.logFileName = path;
      vwr.g.setS("_logFile", vwr.isApplet() ? value : path);
    }
    return value;
  }

  void logToFile(String data) {
    try {
      boolean doClear = (data.equals("$CLEAR$"));
      if (data.indexOf("$NOW$") >= 0)
        data = PT.rep(data, "$NOW$", vwr.apiPlatform
            .getDateFormat(false));
      if (vwr.logFileName == null) {
        Logger.info(data);
        return;
      }
      @SuppressWarnings("resource")
      OC out = (vwr.haveAccess(ACCESS.ALL) ? openOutputChannel(privateKey,
          vwr.logFileName, true, !doClear) : null);
      if (!doClear) {
        int ptEnd = data.indexOf('\0');
        if (ptEnd >= 0)
          data = data.substring(0, ptEnd);
        out.append(data);
        if (ptEnd < 0)
          out.append("\n");
      }
      String s = out.closeChannel();
      Logger.info(s);
    } catch (Exception e) {
      if (Logger.debugging)
        Logger.debug("cannot log " + data);
    }
  }

  protected final static String SCENE_TAG = "###scene.spt###";

  private String createZipSet(String script, String[] scripts,
                              boolean includeRemoteFiles, OC out) {
    List<Object> v = new List<Object>();
    FileManager fm = vwr.fileManager;
    List<String> fileNames = new List<String>();
    Hashtable<Object, String> crcMap = new Hashtable<Object, String>();
    boolean haveSceneScript = (scripts != null && scripts.length == 3 && scripts[1]
        .startsWith(SCENE_TAG));
    boolean sceneScriptOnly = (haveSceneScript && scripts[2].equals("min"));
    if (!sceneScriptOnly) {
      JmolBinary.getFileReferences(script, fileNames);
      if (haveSceneScript)
        JmolBinary.getFileReferences(scripts[1], fileNames);
    }
    boolean haveScripts = (!haveSceneScript && scripts != null && scripts.length > 0);
    if (haveScripts) {
      script = wrapPathForAllFiles("script " + PT.esc(scripts[0]), "");
      for (int i = 0; i < scripts.length; i++)
        fileNames.addLast(scripts[i]);
    }
    int nFiles = fileNames.size();
    List<String> newFileNames = new List<String>();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name = fileNames.get(iFile);
      boolean isLocal = !vwr.isJS && FileManager.isLocal(name);
      String newName = name;
      // also check that somehow we don't have a local file with the same name as
      // a fixed remote file name (because someone extracted the files and then used them)
      if (isLocal || includeRemoteFiles) {
        int ptSlash = name.lastIndexOf("/");
        newName = (name.indexOf("?") > 0 && name.indexOf("|") < 0 ? PT
            .replaceAllCharacters(name, "/:?\"'=&", "_") : FileManager
            .stripPath(name));
        newName = PT.replaceAllCharacters(newName, "[]", "_");
        Map<String, byte[]> spardirCache = fm.getSpardirCache();
        boolean isSparDir = (spardirCache != null && spardirCache
            .containsKey(name));
        if (isLocal && name.indexOf("|") < 0 && !isSparDir) {
          v.addLast(name);
          v.addLast(newName);
          v.addLast(null); // data will be gotten from disk
        } else {
          // all remote files, and any file that was opened from a ZIP collection
          Object ret = (isSparDir ? spardirCache.get(name) : fm.getFileAsBytes(
              name, null, true));
          if (!PT.isAB(ret))
            return (String) ret;
          newName = addPngFileBytes(name, (byte[]) ret, iFile, crcMap,
              isSparDir, newName, ptSlash, v);
        }
        name = "$SCRIPT_PATH$" + newName;
      }
      crcMap.put(newName, newName);
      newFileNames.addLast(name);
    }
    if (!sceneScriptOnly) {
      script = Txt.replaceQuotedStrings(script, fileNames, newFileNames);
      v.addLast("state.spt");
      v.addLast(null);
      v.addLast(script.getBytes());
    }
    if (haveSceneScript) {
      if (scripts[0] != null) {
        v.addLast("animate.spt");
        v.addLast(null);
        v.addLast(scripts[0].getBytes());
      }
      v.addLast("scene.spt");
      v.addLast(null);
      script = Txt.replaceQuotedStrings(scripts[1], fileNames, newFileNames);
      v.addLast(script.getBytes());
    }
    String sname = (haveSceneScript ? "scene.spt" : "state.spt");
    v.addLast("JmolManifest.txt");
    v.addLast(null);
    String sinfo = "# Jmol Manifest Zip Format 1.1\n" + "# Created "
        + (new Date()) + "\n" + "# JmolVersion " + Viewer.getJmolVersion()
        + "\n" + sname;
    v.addLast(sinfo.getBytes());
    v.addLast("Jmol_version_"
        + Viewer.getJmolVersion().replace(' ', '_').replace(':', '.'));
    v.addLast(null);
    v.addLast(new byte[0]);
    if (out.getFileName() != null) {
      byte[] bytes = vwr.getImageAsBytes("PNG", 0, 0, -1, null);
      if (bytes != null) {
        v.addLast("preview.png");
        v.addLast(null);
        v.addLast(bytes);
      }
    }
    return writeZipFile(out, v, "OK JMOL");
  }

  private String addPngFileBytes(String name, byte[] ret, int iFile,
                                 Hashtable<Object, String> crcMap,
                                 boolean isSparDir, String newName, int ptSlash,
                                 List<Object> v) {
     Integer crcValue = Integer.valueOf(Binary.getCrcValue(ret));
     // only add to the data list v when the data in the file is new
     if (crcMap.containsKey(crcValue)) {
       // let newName point to the already added data
       newName = crcMap.get(crcValue);
     } else {
       if (isSparDir)
         newName = newName.replace('.', '_');
       if (crcMap.containsKey(newName)) {
         // now we have a conflict. To different files with the same name
         // append "[iFile]" to the new file name to ensure it's unique
         int pt = newName.lastIndexOf(".");
         if (pt > ptSlash) // is a file extension, probably
           newName = newName.substring(0, pt) + "[" + iFile + "]"
               + newName.substring(pt);
         else
           newName = newName + "[" + iFile + "]";
       }
       v.addLast(name);
       v.addLast(newName);
       v.addLast(ret);
       crcMap.put(crcValue, newName);
     }
     return newName;
   }

  /**
   * generic method to create a zip file based on
   * http://www.exampledepot.com/egs/java.util.zip/CreateZip.html
   * 
   * @param out
   * @param fileNamesAndByteArrays
   *        Vector of [filename1, bytes|null, filename2, bytes|null, ...]
   * @param msg
   * @return msg bytes filename or errorMessage or byte[]
   */

  private String writeZipFile(OC out, List<Object> fileNamesAndByteArrays,
                              String msg) {
    byte[] buf = new byte[1024];
    long nBytesOut = 0;
    long nBytes = 0;
    String outFileName = out.getFileName();
    Logger.info("creating zip file " + (outFileName == null ? "" : outFileName)
        + "...");
    String fileList = "";
    try {
      OutputStream bos;
      /**
       * 
       * no need for buffering here
       * 
       * @j2sNative
       * 
       *            bos = out;
       * 
       */
      {
        bos = new BufferedOutputStream(out);
      }
      FileManager fm = vwr.fileManager;
      OutputStream zos = (OutputStream) Binary.getZipOutputStream(bos);
      for (int i = 0; i < fileNamesAndByteArrays.size(); i += 3) {
        String fname = (String) fileNamesAndByteArrays.get(i);
        byte[] bytes = null;
        Object data = fm.cacheGet(fname, false);
        if (data instanceof Map<?, ?>)
          continue;
        if (fname.indexOf("file:/") == 0) {
          fname = fname.substring(5);
          if (fname.length() > 2 && fname.charAt(2) == ':') // "/C:..." DOS/Windows
            fname = fname.substring(1);
        } else if (fname.indexOf("cache://") == 0) {
          fname = fname.substring(8);
        }
        String fnameShort = (String) fileNamesAndByteArrays.get(i + 1);
        if (fnameShort == null)
          fnameShort = fname;
        if (data != null)
          bytes = (PT.isAB(data) ? (byte[]) data : ((String) data).getBytes());
        if (bytes == null)
          bytes = (byte[]) fileNamesAndByteArrays.get(i + 2);
        String key = ";" + fnameShort + ";";
        if (fileList.indexOf(key) >= 0) {
          Logger.info("duplicate entry");
          continue;
        }
        fileList += key;
        Binary.addZipEntry(zos, fnameShort);
        int nOut = 0;
        if (bytes == null) {
          // get data from disk
          BufferedInputStream in = vwr.getBufferedInputStream(fname);
          int len;
          while ((len = in.read(buf, 0, 1024)) > 0) {
            zos.write(buf, 0, len);
            nOut += len;
          }
          in.close();
        } else {
          // data are already in byte form
          zos.write(bytes, 0, bytes.length);
          nOut += bytes.length;
        }
        nBytesOut += nOut;
        Binary.closeZipEntry(zos);
        Logger.info("...added " + fname + " (" + nOut + " bytes)");
      }
      zos.flush();
      zos.close();
      Logger.info(nBytesOut + " bytes prior to compression");
      String ret = out.closeChannel();
      if (ret != null) {
        if (ret.indexOf("Exception") >= 0)
          return ret;
        msg += " " + ret;
      }
      nBytes = out.getByteCount();
    } catch (IOException e) {
      Logger.info(e.toString());
      return e.toString();
    }
    String fileName = out.getFileName();
    return (fileName == null ? null : msg + " " + nBytes + " " + fileName);
  }

  protected String wrapPathForAllFiles(String cmd, String strCatch) {
    String vname = "v__" + ("" + Math.random()).substring(3);
    return "# Jmol script\n{\n\tVar "
        + vname
        + " = pathForAllFiles\n\tpathForAllFiles=\"$SCRIPT_PATH$\"\n\ttry{\n\t\t"
        + cmd + "\n\t}catch(e){" + strCatch + "}\n\tpathForAllFiles = " + vname
        + "\n}\n";
  }

}
