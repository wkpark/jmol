/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2007  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.fah.checkxyz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

/**
 * Hold configuration.
 */
public class Configuration {

  private String userName;
  private String mailServer;
  private String userMail;
  private String login;
  private String password;
  private Vector directories;
  private Vector sent;

  private File configFile;
  
  /**
   * Constructor.
   */
  public Configuration() {
    userName = "";
    mailServer = "";
    userMail = "";
    login = "";
    password = "";
    directories = new Vector();
    sent = new Vector();

    configFile = new File(new File(new File(System.getProperty("user.home")), ".jmol"), "fah.properties");
  }

  /**
   * Load configuration from file. 
   */
  public void loadConfiguration() {
    try {
      FileInputStream fis = new FileInputStream(configFile);
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      
      userName = props.getProperty("userName", userName);
      mailServer = props.getProperty("mailServer", mailServer);
      userMail = props.getProperty("userMail", userMail);
      login = props.getProperty("login", login);
      password = props.getProperty("password", password);
      directories = new Vector();
      int num = 0;
      while (props.containsKey("directory_" + num)) {
        directories.add(props.getProperty("directory_" + num));
        num++;
      }
      sent = new Vector();
      num = 0;
      while (props.containsKey("sent_" + num)) {
        sent.add(props.getProperty("sent_" + num));
        num++;
      }
    } catch (Exception e) {
      //
    }
  }


  /**
   * Save configuration in file.
   */
  public void saveConfiguration() {
    try {
      Properties props = new Properties();
      props.setProperty("userName", userName);
      props.setProperty("mailServer", mailServer);
      props.setProperty("userMail", userMail);
      props.setProperty("login", login);
      props.setProperty("password", password);
      Iterator iter = directories.iterator();
      int num = 0;
      while (iter.hasNext()) {
        props.setProperty("directory_" + num, iter.next().toString());
        num++;
      }
      iter = sent.iterator();
      num = 0;
      while (iter.hasNext()) {
        props.setProperty("sent_" + num, iter.next().toString());
        num++;
      }
      FileOutputStream fos = new FileOutputStream(configFile);
      props.store(fos, "Jmol FAH");
      fos.close();
    } catch (Exception e) {
      //
    }
  }

  /**
   * @return Indicates if the configuration is done.
   */
  public boolean isConfigured() {
    boolean configured = true;
    configured &= ((userName != null) && !userName.equals(""));
    configured &= ((mailServer != null) && !mailServer.equals(""));
    configured &= ((userMail != null) && !userMail.equals(""));
    configured &= ((directories != null) && !directories.isEmpty());
    return configured;
  }


  /**
   * @param name User name.
   */
  public void setUserName(String name) {
    if (name != null) {
      userName = name.trim();
    } else {
      userName = "";
    }
  }
  public String getUserName() {
    return userName;
  }


  /**
   * @param server Mail server.
   */
  public void setMailServer(String server) {
    if (server != null) {
      mailServer = server.trim();
    } else {
      mailServer = "";
    }
  }
  public String getMailServer() {
    return mailServer;
  }

  /**
   * @param address User mail address.
   */
  public void setUserMail(String address) {
    if (address != null) {
      userMail = address.trim();
    } else {
      userMail = "";
    }
  }
  public String getUserMail() {
    return userMail;
  }

  /**
   * @param user User login.
   */
  public void setLogin(String user) {
    if (user != null) {
      login = user.trim();
    } else {
      login = "";
    }
  }
  public String getLogin() {
    return login;
  }

  /**
   * @param pass User password.
   */
  public void setPassword(String pass) {
    if (pass != null) {
      password = pass.trim();
    } else {
      password = "";
    }
  }
  public String getPassword() {
    return password;
  }

  /**
   * @param dir Directories containing XYZ files.
   */
  public void setDirectories(Vector dir) {
    if (dir != null) {
      directories = dir;
    } else {
      directories = new Vector();
    }
  }
  public Vector getDirectories() {
    return directories;
  }

  /**
   * @param files Sent XYZ files.
   */
  public void setSentFiles(Vector files) {
    if (files != null) {
      sent = files;
    } else {
      sent = new Vector();
    }
  }
  public void addSentFile(String file) {
    sent.add(file);
    saveConfiguration();
  }
  public Vector getSentFiles() {
    return sent;
  }
  public boolean hasBeenSent(String file) {
    return sent.contains(file);
  }
}
