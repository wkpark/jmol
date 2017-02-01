package org.gennbo;

import javajs.util.PT;

/**
 * A class to manage NBOServer requests. Manages file data that needs to 
 * be put in place prior to running the command and also keeps a handle 
 * to a Runnable that is to be run when the job is complete.
 * 
 */
class NBORequest {

  /**
   * File data that needs to be written to disk at the time the 
   * job is run. An even-length array with odd entries file names (no directory)
   * and even entries the data: [filename,data,filename,data,...]
   * 
   * element 0 is the command file name sent to NBO as <xxxxx.xxx>
   * 
   * element 1 is the set of metacommands to be put in that file
   * 
   */
  String[] fileData;  
  
  /**
   * A few words that will be flashed on the bottom line of the nboOutput
   * panel while the request is being processed.
   * 
   */
  String statusInfo;
  
  /**
   * A method to be called when the reply is complete.
   */
  Runnable callbackMethod;
  
  /**
   * The reply from NBOServe
   * 
   */
  private String reply;
  
  /**
   * Just an ad hoc flag that indicates that we might expect some garbage 
   * before the ***start*** flag; used to disregard this preliminary info.
   * 
   */
  boolean isRUN;

  NBORequest(){}
  
  void set(Runnable returnMethod, String statusInfo, String... fileData) {
    this.fileData = fileData;
    this.statusInfo = statusInfo;
    this.callbackMethod = returnMethod;
    // need to flag this so that not all of sysout is returned
    // from either RUN or SEARCH
    isRUN  = (statusInfo != null 
        && (statusInfo.indexOf("Running") >= 0 || statusInfo.indexOf("Getting value") >= 0));
  }

  /**
   * Set the reply and notify originator that it can pick it up.
   * 
   * @param reply
   */
  void sendReply(String reply) {
    this.reply = reply;
    if (callbackMethod != null)
      callbackMethod.run();
  }
  
  String getReply() {
    return reply;
  }
  /**
   * 
   * @return reply lines separated into lines and trimmed of 
   * first and last blank lines.
   */
  public String[] getReplyLines() {
    return PT.trim(reply, "\n").split("\n");
  }

}