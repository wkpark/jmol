/*
  
  Fire 'n' Forget Splash screen, instantiate A.S.A.P. in the main class
  
  Constructor:
  
      Splash splash = new Splash(this, ImageIcon ii);
  
  where 'this' is the main frame that will display on screen
  
  requires 1.1
  
  Originally by: Ralph (ralph@truleigh.demon.co.uk) May 98
  Modifications by: Dan Gezelter (gezelter@chem.columbia.edu) November 98
  Now uses ImageIcon which is loaded in the calling class...
  
*/

package org.openscience.jmol;

import javax.swing.Timer;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.event.*;

public class Splash extends Window {
    private Image splashImage;
    private int imgWidth, imgHeight;
    private String imgName;
    private static final int BORDERSIZE = 5;
    private static final Color BORDERCOLOR = Color.blue;
    Toolkit tk;
    
    public Splash(Frame parent, ImageIcon ii) {
        super(parent);
        tk = Toolkit.getDefaultToolkit();
        splashImage = ii.getImage();
        imgWidth = splashImage.getWidth(this);
        imgHeight = splashImage.getHeight(this);
        showSplashScreen();
        parent.addWindowListener(new WindowListener());
    }

    public void showSplashScreen() {
        Dimension screenSize = tk.getScreenSize();
        setBackground(BORDERCOLOR);
        int w = imgWidth + (BORDERSIZE * 2);
        int h = imgHeight + (BORDERSIZE * 2);
        int x = (screenSize.width - w) /2;
        int y = (screenSize.height - h) /2;
        setBounds(x, y, w, h);
        show();
    }
    
    public void paint(Graphics g) {
        g.drawImage(splashImage, BORDERSIZE, BORDERSIZE, imgWidth, imgHeight, 
                    this);
    }

    class WindowListener extends WindowAdapter {
        public void windowActivated(WindowEvent we) {
            setVisible(false);
            dispose();
        }
    }
}
