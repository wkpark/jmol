/*
 * 
 * Original code by Tony LaPaso
 *
 * Added to Jmol 11/04/99 by Charles Fulton
 *
 */

package org.openscience.jmol;

import java.io.*;

public class LoopedStreams {
    private PipedOutputStream pipedOS = new PipedOutputStream();
    private boolean keepRunning = true;
    private ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream() {
            public void close() {
                keepRunning = false;
                try {
                    super.close();
                    pipedOS.close();
                }
                catch(IOException e) {
                    // Do something to log the error -- perhaps invoke a 
                    // Runnable to log the error. For now we simply exit.
                    System.exit(1);
                }
            }
        };
    
    private PipedInputStream pipedIS = new PipedInputStream() {
            public void close() {
                keepRunning = false;
                try {
                    super.close();
                }
                catch(IOException e) {
                    // Do something to log the error -- perhaps invoke a 
                    // Runnable to log the error. For now we simply exit.
                    System.exit(1);
                }
            }
	};

    public LoopedStreams() throws IOException {
        pipedOS.connect(pipedIS);
        startByteArrayReaderThread();
    } // LoopedStreams()
    
    public InputStream getInputStream() {
        return pipedIS;
    } // getInputStream()
    
    public OutputStream getOutputStream() {
        return byteArrayOS;
    } // getOutputStream()
    
    private void startByteArrayReaderThread() {
        new Thread(new Runnable() {
                public void run() {
                    while(keepRunning) {
                        // Check for bytes in the stream.
                        if(byteArrayOS.size() > 0) {
                            byte[] buffer = null;
                            synchronized(byteArrayOS) {
                                buffer = byteArrayOS.toByteArray();
                                byteArrayOS.reset(); // Clear the buffer.
                            }
                            try {
                                // Send the extracted data to
                                // the PipedOutputStream.
                                pipedOS.write(buffer, 0, buffer.length);
                            }
                            catch(IOException e) {
                                // Do something to log the error -- perhaps 
                                // invoke a Runnable. For now we simply exit.
                                System.exit(1);
                            }
                        }
                        else // No data available, go to sleep.
                            try {
                                // Check the ByteArrayOutputStream every
                                // 1 second for new data.
                                Thread.sleep(1000);
                            }
                            catch(InterruptedException e) {}
                    }
                }
            }).start();
    } // startByteArrayReaderThread()
} // LoopedStreams

