import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Canvas;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

public class ellipse {
  public static void main(String[] argv) {
    JFrame jframe = new JFrame("ellipse");
    jframe.addWindowListener(new CloseListener());
    jframe.setSize(300, 300);
    jframe.getContentPane().add(new EllipseCanvas());
    jframe.setVisible(true);
  }

}

class CloseListener extends WindowAdapter {
  public void windowClosing(WindowEvent e) {
    System.exit(0);
  }
}

class EllipseCanvas extends Canvas {
  
  int ax[] = new int[3];
  int ay[] = new int[3];
  int i = 0;

  EllipseCanvas() {
    addMouseListener(new EllipseMouseAdapter());
  }

  Graphics g;

  Color transBlue = new Color(0x400000FF, true);

  public void paint(Graphics g) {
    this.g = g;
    g.setColor(Color.blue);
    g.drawLine(ax[0], ay[0], ax[1], ay[1]);
    g.setColor(Color.green);
    g.drawLine(ax[0], ay[0], ax[2], ay[2]);
    g.drawLine(ax[1], ay[1], ax[2], ay[2]);

    g.setColor(Color.red);
    ellipse0(50, 3, .25);
    ellipse1();
    ellipse2();
    g.setColor(transBlue);
    if (false) {
      ellipse3();
    } else {
      int scanlines = yN - yS + 1;
      byte[] ax = new byte[scanlines];
      byte[] an = new byte[scanlines];
      ellipse4(ax, an);
      int yCurrent = yN;
      for (int i = 0; i < scanlines; ++i, --yCurrent) {
        setPixels(ax[i], yCurrent, an[i]);
        setPixels(-ax[i], -yCurrent, -an[i]);
      }
    }
  }

  int one;
  int two;
  int three;
  int four;

  int px[] = new int[4];
  int py[] = new int[4];
  int f[] = new int[4];
  int fmid[] = new int[4];
  int slx[] = new int[4];
  int sly[] = new int[4];
  int slmid[] = new int[4];
  int disx[] = new int[4];
  int disy[] = new int[4];
  int disxsl[] = new int[4];
  int disysl[] = new int[4];

  int a = 20*20 - 15*15;
  int b = 400;
  int c = 0;
  int d = -(20*20*10*10);

  int a2; //2a
  int b2; //2b
  int c4; //4c
  int a4; //4a
  int b4; //4b
  int c_ab; //c**2 - ab;
  int c_ab2; //2(c**2 - ab)
  int oldpoint;

  int A, B, C, F;
  int twoA, twoB, twoC, CplusAdiv4, CplusBdiv4;
 
  int eval(int x, int y) {
    return A*x*x + B*y*y + 2*C*x*y + F;
  }

  int evalNextMidpointNNE(int x, int y) {
    //return A*x*x + B*y*y + 2*C*x*y+ F + (2*A-C)*x + (2*C-B)*y + A - C + B/4;
    return A*x*x + B*y*y + twoC*x*y+ F +
      (twoA-C)*x + (twoC-B)*y + A - CplusBdiv4;

  }

  int evalNextMidpointENE(int x, int y) {
    //return A*x*x + B*y*y + 2*C*x*y+ F + (A-2*C)*x + (C-2*B)*y + B - C + A/4;
    return A*x*x + B*y*y + twoC*x*y+ F +
      (A-twoC)*x + (C-twoB)*y + B - CplusAdiv4;
  }

  int evalNextMidpointESE(int x, int y) {
    //return A*x*x + B*y*y + 2*C*x*y+ F - (A+2*C)*x - (2*B+C)*y + B + C + A/4;
    return A*x*x + B*y*y + twoC*x*y+ F +
      -(A+twoC)*x - (twoB+C)*y + B + CplusAdiv4;
  }

  int evalNextMidpointSSE(int x, int y) {
    //return A*x*x + B*y*y + 2*C*x*y+ F - (2*A+C)*x - (B+2*C)*y + A + C + B/4;
    return A*x*x + B*y*y + twoC*x*y+ F +
      -(twoA+C)*x - (B+twoC)*y + A + CplusBdiv4;
  }

  void ellipse0(int majorAxisLength, int minorAxisLength,
                int dx, int dy) {
    ellipse0(majorAxisLength, minorAxisLength,
             dx, dy, (int)(Math.sqrt(dx*dx + dy*dy) + .5));
  }

  void ellipse0(int majorAxisLength, int minorAxisLength,
                int dxTheta, int dyTheta, int mag2dTheta) {
    if (majorAxisLength < minorAxisLength) {
      int t = majorAxisLength;
      majorAxisLength = minorAxisLength;
      minorAxisLength = t;
      t = dxTheta;
      dxTheta = dyTheta;
      dyTheta = t;
    }
    double a = majorAxisLength / 2.0;
    double b = minorAxisLength / 2.0;
    double c = Math.sqrt(a*a - b*b);
    double xC = c * dxTheta / mag2dTheta;
    double yC = c * dyTheta / mag2dTheta;
    double a2 = a*a;
    double dblA = a2 - xC*xC;
    int adiv4 = (int)dblA;
    A = adiv4 * 4;
    twoA = A * 2;
    double dblB = a2 - yC*yC;
    int bdiv4 = (int)dblB;
    B = bdiv4 * 4;
    twoB = B * 2;
    double dblC = -xC * yC;
    C = (int)dblC * 4;
    CplusAdiv4 = C + adiv4;
    CplusBdiv4 = C + bdiv4;
    twoC = C * 2;
    double dblF = -(a2*b*b);
    F = (int)dblF * 4;
  }

  void ellipse0(int majorAxisLength, int minorAxisLength, double theta) {
    /* FIXME mth
       with a little more work I think I can get rid of all these doubles
    */
    if (majorAxisLength < minorAxisLength) {
      int t = majorAxisLength;
      majorAxisLength = minorAxisLength;
      minorAxisLength = t;
      theta += Math.PI/2;
    }
    double a = majorAxisLength / 2.0;
    double b = minorAxisLength / 2.0;
    double c = Math.sqrt(a*a - b*b);
    double xC = c * Math.cos(theta);
    double yC = c * Math.sin(theta);
    double a2 = a*a;
    double dblA = a2 - xC*xC;
    int adiv4 = (int)dblA;
    // should these be rounded instead of truncated?
    A = adiv4 * 4;
    twoA = A * 2;
    double dblB = a2 - yC*yC;
    int bdiv4 = (int)dblB;
    B = bdiv4 * 4;
    twoB = B * 2;
    double dblC = -xC * yC;
    C = (int)dblC * 4;
    CplusAdiv4 = C + adiv4;
    CplusBdiv4 = C + bdiv4;
    twoC = C * 2;
    double dblF = -(a2*b*b);
    F = (int)dblF * 4;
    //    System.out.println("A=" + A + " B=" + B + " C=" + C + " F=" + F);
  }

  void ellipse1() {
    int kmax = 0;
    int kmin = 0;
    for (int i = -127; i < 127; ++i)
      for (int j = - 127; j < 127; ++j) {
        int k = eval(i, j);
        if (k > 0)
          continue;
        g.setColor(Color.red);
        set_pixel(i, j);
      }
  }

  int xN, yN, xNE, yNE, xE, yE, xSE, ySE, xS, yS;

  void ellipse2() {
    //    g.setColor(transBlue);
    //    int yN = (int)Math.sqrt(A*F/(C*C-A*B));
    //    int xN = (int)(-C/A*yN);
    double sqrtA = Math.sqrt(A);
    double dblxN = -C/sqrtA;
    double dblyN = sqrtA;
    dblxN /= 2;
    dblyN /= 2;
    xN = (int)(dblxN - 0.5);
    yN = (int)(dblyN + 0.5);

    double sqrtAplusBminus2C = Math.sqrt(A + B - 2*C);
    double dblxNE = (B - C)/sqrtAplusBminus2C;
    double dblyNE = (A - C)/sqrtAplusBminus2C;
    dblxNE /= 2;
    dblxNE += (dblxNE < 0) ? -0.5 : 0.5;
    dblyNE /= 2;
    dblyNE += (dblyNE < 0) ? -0.5 : 0.5;
    xNE = (int)dblxNE;
    yNE = (int)dblyNE;
    //    set_pixel(xNE, yNE);

    double sqrtB = Math.sqrt(B);
    double dblxE = sqrtB;
    dblxE /= 2;
    dblxE += 0.5;
    double dblyE = -C/sqrtB;
    dblyE /= 2;
    dblyE += (dblyE < 0) ? -0.5 : 0.5;
    xE = (int)dblxE;
    yE = (int)dblyE;
    //    set_pixel(xE, yE);

    double sqrtAplusBplus2C = Math.sqrt(A + B + 2*C);
    double dblxSE = (B + C)/sqrtAplusBplus2C;
    dblxSE /= 2;
    dblxSE += (dblxSE < 0) ? -0.5 : 0.5;
    double dblySE = -(A + C)/sqrtAplusBplus2C;
    dblySE /= 2;
    dblySE += (dblySE < 0) ? -0.5 : 0.5;
    xSE = (int)dblxSE;
    ySE = (int)dblySE;

    yS = -yN;
    xS = -xN;
    //    set_pixel(xS, yS);

  }

  void ellipse3() {
    int xCurrent = xN;
    int yCurrent = yN;
    int discriminator;
    set_pixel(xCurrent, yCurrent);
    if (xCurrent != xNE || yCurrent != yNE) {
      discriminator = evalNextMidpointNNE(xN, yN);
      int xStop = xNE - 1;
      while (xCurrent < xStop && yCurrent > yNE) {
        ++xCurrent;
        if (discriminator < 0) {
          set_pixel(xCurrent, yCurrent);
          discriminator += 2*A*xCurrent + 2*C*yCurrent + A - C;
        } else {
          set_pixel(xCurrent, --yCurrent);
          discriminator += 2*(A-C)*xCurrent + 2*(C-B)*yCurrent + A - C;
        }
      }
      while (xCurrent < xNE)
        set_pixel(++xCurrent, yNE);
      yCurrent = yNE;
    }
    if (xCurrent != xE || yCurrent != yE) {
      discriminator = evalNextMidpointENE(xNE, yNE);
      int yStop = yE + 1;
      while (yCurrent > yStop && xCurrent < xE) {
        --yCurrent;
        if (discriminator < 0) {
          set_pixel(++xCurrent, yCurrent);
          discriminator += 2*(A-C)*xCurrent + 2*(C-B)*yCurrent + B - C;
        } else {
          set_pixel(xCurrent, yCurrent);
          discriminator += -2*C*xCurrent - 2*B*yCurrent + B - C;
        }
      }
      while (yCurrent > yE)
        set_pixel(xE, --yCurrent);
      xCurrent = xE;
    }
    if (xCurrent != xSE || yCurrent != ySE) {
      discriminator = evalNextMidpointESE(xE, yE);
      int yStop = ySE + 1;
      while (yCurrent > yStop && xCurrent > xSE) {
        --yCurrent;
        if (discriminator < 0) {
          set_pixel(xCurrent, yCurrent);
          discriminator += -2*C*xCurrent - 2*B*yCurrent + B + C;
        } else {
          set_pixel(--xCurrent, yCurrent);
          discriminator += -2*(A+C)*xCurrent - 2*(B+C)*yCurrent + B + C;
        }
      }
      while (yCurrent > ySE)
        set_pixel(xSE, --yCurrent);
      xCurrent = xSE;
    }
    if (xCurrent != xS || yCurrent != xS) {
      discriminator = evalNextMidpointSSE(xSE, ySE);
      int xStop = xS + 1;
      while (xCurrent > xStop && yCurrent > yS) {
        --xCurrent;
        if (discriminator < 0) {
          set_pixel(xCurrent, --yCurrent);
          discriminator += -2*(A+C)*xCurrent - 2*(B+C)*yCurrent + A + C;
        } else {
          set_pixel(xCurrent, yCurrent);
          discriminator += -2*A*xCurrent - 2*C*yCurrent + A + C;
        }
      }
      while (xCurrent > xS)
        set_pixel(--xCurrent, yS);
      yS = yCurrent;
    }
  }

  void ellipse4(byte[] ax, byte[] an) {
    int xCurrent = xN;
    int yCurrent = yN;
    int discriminator;
    initializeRecorder(xN, yN, yS, ax, an);
    // NNE octant
    assert xCurrent == xN && yCurrent == yN;
    if (xCurrent != xNE || yCurrent != yNE) {
      discriminator = evalNextMidpointNNE(xN, yN);
      int xStop = xNE - 1;
      while (xCurrent < xStop && yCurrent > yNE) {
        ++xCurrent;
        if (discriminator < 0) {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += 2*A*xCurrent + 2*C*yCurrent + A - C;
        } else {
          recordCoordinate(xCurrent, --yCurrent);
          discriminator += 2*(A-C)*xCurrent + 2*(C-B)*yCurrent + A - C;
        }
      }
      while (xCurrent < xNE)
        recordCoordinate(++xCurrent, yNE);
      yCurrent = yNE;
      if (xCurrent != xNE || yCurrent != yNE) {
        System.out.println("ellipse error #8a");
      }
    }

    // ENE octant
    assert xCurrent == xNE && yCurrent == yNE;
    if (xCurrent != xE || yCurrent != yE) {
      discriminator = evalNextMidpointENE(xNE, yNE);
      int yStop = yE + 1;
      while (yCurrent > yStop && xCurrent < xE) {
        --yCurrent;
        if (discriminator < 0) {
          recordCoordinate(++xCurrent, yCurrent);
          discriminator += 2*(A-C)*xCurrent + 2*(C-B)*yCurrent + B - C;
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -2*C*xCurrent - 2*B*yCurrent + B - C;
        }
      }
      while (yCurrent > yE)
        recordCoordinate(xE, --yCurrent);
      xCurrent = xE;
      if (xCurrent != xE || yCurrent != yE)
        System.out.println("ellipse error #9a");
    }

    // ESE octant
    assert xCurrent == xE && yCurrent == yE;
    if (xCurrent != xSE || yCurrent != ySE) {
      discriminator = evalNextMidpointESE(xE, yE);
      int yStop = ySE + 1;
      while (yCurrent > yStop && xCurrent > xSE) {
        --yCurrent;
        if (discriminator < 0) {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -2*C*xCurrent - 2*B*yCurrent + B + C;
        } else {
          recordCoordinate(--xCurrent, yCurrent);
          discriminator += -2*(A+C)*xCurrent - 2*(B+C)*yCurrent + B + C;
        }
      }
      while (yCurrent > ySE)
        recordCoordinate(xSE, --yCurrent);
      xCurrent = xSE;
      if (xCurrent != xSE || yCurrent != ySE)
        System.out.println("ellipse error #10");
    }

    // SSE octant
    assert xCurrent == xSE && yCurrent == ySE;
    if (xCurrent != xS || yCurrent != xS) {
      discriminator = evalNextMidpointSSE(xSE, ySE);
      int xStop = xS + 1;
      while (xCurrent > xStop && yCurrent > yS) {
        --xCurrent;
        if (discriminator < 0) {
          recordCoordinate(xCurrent, --yCurrent);
          discriminator += -2*(A+C)*xCurrent - 2*(B+C)*yCurrent + A + C;
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -2*A*xCurrent - 2*C*yCurrent + A + C;
        }
      }
      while (xCurrent > xS)
        recordCoordinate(--xCurrent, yS);
      yS = yCurrent;
      if (xCurrent != xS || yCurrent != yS)
        System.out.println("ellipse error #11");
    }
    assert xCurrent == xS && yCurrent == yS;
  }

  void ellipse() {
    a2 = 2 * a;
    b2 = 2 * b;
    c4 = 4 * c;
    a4 = 4 * a;
    b4 = 4 * b;
    c_ab = c*c - a*b;
    c_ab2 = 2 * c_ab;
    oldpoint = -1;

    int xOneStart = ax[one], yOneStart = ay[one];
    int xTwoStart = ax[two], yTwoStart = ay[two];
    do {
      System.out.println("starting ellipse");
      int dx = px[two] - px[one];
      int dy = py[two] - py[one];
      nextpoint(three, two, -dy, dx);
      nextpoint(four, one, -dy, dx);
      midpoint(one, dx, dy);
      midpoint(two, -dy, dx);
      midpoint(three, -dx, -dy);
      midpoint(four, dy, -dx);
      if (edge(three, two, dx)) {
        { int t = one; one = three; three = t; }
        int dT = dx;
        dx = dy;
        dy = -dT;
      } else if (edge(four, three, dy)) {
        { int t = one; one = four; four = t; }
        { int t = two; two = three; three = t; }
      } else if (edge(one, four, dx)) {
        { int t = two; two = four; four = t; }
        int dT = dx;
        dx = -dy;
        dy = dT;
      } else {
        edge(two, one, dy);
        { int t = one; one = two; two = t; }
        dx = -dx;
        dy = -dy;
      }
    } while (ax[one] != xOneStart || ay[one] != yOneStart ||
             ax[two] != xTwoStart || ay[two] != yTwoStart);
  }

  void nextpoint(int next, int old, int dx, int dy) {
    px[next] = px[old] + dx; py[next] = py[old] + dy;
    f[next] = f[old] + dx*2*slx[old] + dy*2*sly[old] + dx*dx*a4 + dy*dy*b4;
    slx[next] = slx[old] + dx*a4 + dy*c4;
    sly[next] = sly[old] + dx*c4 + dy*b4;

    disx[next]   = disx[old]   + dy*disxsl[old] + dy*dy*c_ab;
    disxsl[next] = disxsl[old] + dy*c_ab2;
    disy[next]   = disy[old]   + dx*disysl[old] + dx*dx*c_ab;
    disysl[next] = disysl[old] + dx*c_ab2;
  }

  void midpoint(int p, int dx, int dy) {
    fmid[p] = f[p] + dx*slx[p] + dy*sly[p] + dx*dx*a + dy*dy*b;
    slmid[p] = slx[p] + ((dy == 0) ? dx*a2 : dy*b2);
  }

  boolean edge(int i1, int i2, int flag) {
    boolean ret = false;
    if (flag == 0) { // edge is horizontal
      if (disx[i1] >= 0) {
        if (f[i1] <= 0) {
          ret = true;
          if (fmid[i2] < 0) {
            if (i2 != oldpoint)
              set_pixel(px[i2], py[i2]);
            oldpoint = i2;
          } else {
            if (i1 != oldpoint)
              set_pixel(px[i1], py[i1]);
            oldpoint = i1;
          }
        } else if ((slx[i1] > 0 && slx[i2] < 0) ||
                   (slx[i1] < 0 && slx[i2] > 0)) {
          ret = true;
          if (fmid[i2] < 0) {
            if (i2 != oldpoint)
              set_pixel(px[i2], py[i2]);
            oldpoint = i2;
          } else if ((slmid[i2] > 0 && slx[i2] > 0) ||
                     (slmid[i2] < 0 && slx[i2] < 0)) {
            if (i1 != oldpoint)
              set_pixel(px[i1], py[i1]);
            oldpoint = i1;
          } else {
            if (i2 != oldpoint)
              set_pixel(px[i2], py[i2]);
            oldpoint = i2;
          }
        }
      }
    } else { // edge is vertical
      if (disy[i1] >= 0) {
        if (f[i1] <= 0) {
          ret = true;
          if (fmid[i2] < 0) {
            if (i2 != oldpoint)
              set_pixel(px[i2], py[i2]);
            oldpoint = i2;
          } else {
            if (i1 != oldpoint)
              set_pixel(px[i1], py[i1]);
            oldpoint = i1;
          }
        } else if ((sly[i1] > 0 && sly[i2] < 0) ||
                   (sly[i1] < 0 && sly[i2] > 0)) {
          ret = true;
          if (fmid[i2] < 0) {
            if (i2 != oldpoint)
              set_pixel(px[i2], py[i2]);
            oldpoint = i2;
          } else if ((slmid[i2] > 0 && sly[i2] > 0) ||
                     (slmid[i2] < 0 && sly[i2] < 0)) {
            if (i1 != oldpoint)
              set_pixel(px[i1], py[i1]);
            oldpoint = i1;
          } else {
            if (i2 != oldpoint)
              set_pixel(px[i2], py[i2]);
            oldpoint = i2;
          }
        }
      }
    }
    return ret;
  }

  void set_pixel(int x, int y) {
    x += 100;
    y += 100;
    g.drawLine(x, y, x, y);
  }

  void setPixels(int x, int y, int n) {
    x += 100;
    y += 100;
    if (n < 0) {
      n = -n;
      x -= (n - 1);
    }
    g.drawLine(x, y, x+n-1, y);
  }

  byte[] axRaster, anRaster;
  int yRaster;
  int iaxRaster = 0;

  void initializeRecorder(int xN, int yN, int yS, byte[] ax, byte[] an) {
    for (int i = yN - yS + 1; --i > 0; )
      ax[i] = an[i] = 0;
    ax[0] = (byte)xN;
    an[0] = 1;
    iaxRaster = 0;
    axRaster = ax;
    anRaster = an;
    yRaster = yN;
  }

  void recordCoordinate(int x, int y) {
    if (y != yRaster) {
      --yRaster;
      assert yRaster == y;
      ++iaxRaster;
      axRaster[iaxRaster] = (byte)x;
      anRaster[iaxRaster] = 1;
    } else {
      if (x < axRaster[iaxRaster]) {
        --axRaster[iaxRaster];
        assert axRaster[iaxRaster] == x;
      }
      ++anRaster[iaxRaster];
    }
  }

  class EllipseMouseAdapter extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      int xMouse = e.getX();
      int yMouse = e.getY();
      ax[i] = xMouse;
      ay[i] = yMouse;
      i = (i + 1) % 3;
      repaint();
    }
  }
}
