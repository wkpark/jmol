/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.appletwrapper;

import netscape.javascript.JSObject;

import java.applet.*;
import java.awt.*;
import java.net.URL;

public class AppletWrapper extends Applet {

  String wrappedAppletClassName;
  int preloadThreadCount;
  String[] preloadClassNames;

  int preloadClassIndex;
  String previousClassName;

  boolean needToLoadImage;
  String preloadImageName;
  Image preloadImage;
  boolean preloadImageReadyForDisplay;
  MediaTracker mediaTracker;

  WrappedApplet wrappedApplet;
  int percentage;
  long startTime;

  public AppletWrapper(String wrappedAppletClassName,
                       int preloadThreadCount,
                       String[] preloadClassNames,
                       String preloadImageName) {
    this.wrappedAppletClassName = wrappedAppletClassName;
    this.preloadThreadCount = preloadThreadCount;
    this.preloadClassNames = preloadClassNames;
    this.preloadImageName = preloadImageName;
    needToLoadImage = preloadImageName != null;
  }

  public String getAppletInfo() {
    return (wrappedApplet != null ? wrappedApplet.getAppletInfo() : null);
  }

  public void init() {
    startTime = System.currentTimeMillis();
    new WrappedAppletLoader(this, wrappedAppletClassName).start();
    for (int i = preloadThreadCount; --i >= 0; )
      new ClassPreloader(this).start();
  }
  
  public void update(Graphics g) {
    if (wrappedApplet != null) {
      wrappedApplet.update(g);
      return;
    }
    if (needToLoadImage) {
      needToLoadImage = false;
      try {
        System.out.println("loadImage:" + preloadImageName);
        URL urlImage =
          getClass().getClassLoader().getResource(preloadImageName);
        System.out.println("urlImage=" + urlImage);
        if (urlImage != null) {
          preloadImage =
            Toolkit.getDefaultToolkit().getImage(urlImage);
          System.out.println("successfully loaded " + preloadImageName);
          System.out.println("preloadImage=" + preloadImage);
          mediaTracker = new MediaTracker(this);
          mediaTracker.addImage(preloadImage, 0);
        }
      } catch (Exception e) {
        System.out.println("getImage failed: " + e);
      }
    }

    g.setColor(Color.yellow);
    g.fillRect(0, 0, 1000, 1000);
    
    g.setColor(Color.black);
    
    g.drawString("applet wrapper test", 15, 20);

    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;

    g.drawString("" + elapsedTime + " seconds", 15, 40);

    if (preloadImageReadyForDisplay)
      g.drawImage(preloadImage, 15, 50, null);
    else if (mediaTracker != null)
      preloadImageReadyForDisplay = mediaTracker.checkID(0);
  }

  public void paint(Graphics g) {
    if (wrappedApplet != null) {
      wrappedApplet.paint(g);
      return;
    }
    update(g);
  }

  public boolean handleEvent(Event e) {
    if (wrappedApplet != null)
      return wrappedApplet.handleEvent(e);
    return false;
  }
  
  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback) {
    if (wrappedApplet != null)
      wrappedApplet.scriptButton(buttonWindow, buttonName,
                              script, buttonCallback);
  }
  
  public void script(String script) {
    if (wrappedApplet != null)
      wrappedApplet.script(script);
  }
  
  public void loadInline(String strModel) {
    if (wrappedApplet != null)
      wrappedApplet.loadInline(strModel);
  }

  synchronized String getNextPreloadClassName() {
    if (preloadClassNames == null ||
        preloadClassIndex == preloadClassNames.length)
      return null;
    String className = preloadClassNames[preloadClassIndex++];
    if (className.charAt(0) == '.') {
      int lastDot = previousClassName.lastIndexOf('.');
      String previousPackageName = previousClassName.substring(0, lastDot);
      className = previousPackageName + className;
    }
    return previousClassName = className;
  }
}

