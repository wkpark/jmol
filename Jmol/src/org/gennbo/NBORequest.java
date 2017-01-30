package org.gennbo;

import javajs.util.PT;

/**
 * A class to manage NBOServer requests.
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
  String statusInfo;
  Runnable returnMethod;
  private String reply;

  NBORequest(){}
  
  void set(Runnable returnMethod, String statusInfo, String... fileData) {
    this.fileData = fileData;
    this.statusInfo = statusInfo;
    this.returnMethod = returnMethod;
  }

  /**
   * Set the reply and notify originator that it can pick it up.
   * 
   * @param reply
   */
  void sendReply(String reply) {
    this.reply = reply;
    if (returnMethod != null)
      returnMethod.run();
  }
  
  String getReply() {
    return reply;
  }

  public String[] getReplyLines() {
    return PT.trim(reply, "\n").split("\n");
  }

}