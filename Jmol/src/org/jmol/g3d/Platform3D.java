/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.jmol.g3d;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;

abstract class Platform3D {

  int width, height;
  int size;
  Image imagePixelBuffer;
  int[] pBuffer;
  short[] zBuffer;
  int argbBackground;

  int widthOffscreen, heightOffscreen;
  Image imageOffscreen;
  Graphics gOffscreen;

  final static boolean useClearingThread = true;

  ClearingThread clearingThread;

  final void initialize() {
    if (useClearingThread) {
      clearingThread = new ClearingThread();
      clearingThread.start();
    }
  }

  final static short ZBUFFER_BACKGROUND = 32767;

  abstract void allocatePixelBuffer();

  void allocateBuffers(int width, int height) {
    this.width = width;
    this.height = height;
    size = width * height;
    zBuffer = new short[size];
    allocatePixelBuffer();
  }

  void releaseBuffers() {
    width = height = size = -1;
    if (imagePixelBuffer != null) {
      imagePixelBuffer.flush();
      imagePixelBuffer = null;
    }
    pBuffer = null;
    zBuffer = null;
  }

  void setBackground(int argbBackground) {
    if (this.argbBackground != argbBackground) {
      this.argbBackground = argbBackground;
      if (useClearingThread)
        clearingThread.notifyBackgroundChange(argbBackground);
    }
  }

  abstract void clearScreenBuffer(int argbBackground);
  
  final void obtainScreenBuffer() {
    if (useClearingThread) {
      clearingThread.obtainBufferForClient();
    } else {
      clearScreenBuffer(argbBackground);
    }
  }

  final void clearScreenBufferThreaded() {
    if (useClearingThread)
      clearingThread.releaseBufferForClearing();
  }
  
  void notifyEndOfRendering() {
  }

  FontMetrics getFontMetrics(Font font) {
    if (gOffscreen == null)
      checkOffscreenSize(16, 64);
    return gOffscreen.getFontMetrics(font);
  }
  
  abstract Image allocateOffscreenImage(int width, int height);
  
  void checkOffscreenSize(int width, int height) {
    if (width <= widthOffscreen && height <= heightOffscreen)
      return;
    if (imageOffscreen != null) {
      gOffscreen.dispose();
      imageOffscreen.flush();
    }
    if (width > widthOffscreen)
      widthOffscreen = (width + 63) & ~63;
    if (height > heightOffscreen)
      heightOffscreen = (height + 15) & ~15;
    imageOffscreen = allocateOffscreenImage(widthOffscreen, heightOffscreen);
    gOffscreen = imageOffscreen.getGraphics();
  }

  class ClearingThread extends Thread implements Runnable {


    boolean bufferHasBeenCleared = false;
    boolean clientHasBuffer = false;

    synchronized void notifyBackgroundChange(int argbBackground) {
      //      System.out.println("notifyBackgroundChange");
      bufferHasBeenCleared = false;
      notify();
      // for now do nothing
    }

    synchronized void obtainBufferForClient() {
      //      System.out.println("obtainBufferForClient()");
      while (! bufferHasBeenCleared)
        try { wait(); } catch (InterruptedException ie) {}
      clientHasBuffer = true;
    }

    synchronized void releaseBufferForClearing() {
      //      System.out.println("releaseBufferForClearing()");
      clientHasBuffer = false;
      bufferHasBeenCleared = false;
      notify();
    }

    synchronized void waitForClientRelease() {
      //      System.out.println("waitForClientRelease()");
      while (clientHasBuffer || bufferHasBeenCleared)
        try { wait(); } catch (InterruptedException ie) {}
    }

    synchronized void notifyBufferReady() {
      //      System.out.println("notifyBufferReady()");
      bufferHasBeenCleared = true;
      notify();
    }

    public void run() {
      System.out.println("running clearing thread:" +
                         Thread.currentThread().getPriority());
      while (true) {
        waitForClientRelease();
        int bg;
        do {
          bg = argbBackground;
          clearScreenBuffer(bg);
        } while (bg != argbBackground); // color changed underneath us
        notifyBufferReady();
      }
    }
  }
}
