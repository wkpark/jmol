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
  int iMouse = 0;

  EllipseCanvas() {
    addMouseListener(new EllipseMouseAdapter());
    ax[1] = ay[1] = 10;
    ax[2] = 5;
  }

  Graphics g;

  Color transBlue = new Color(0x400000FF, true);
  Color transGrn = new Color(0x4000FF00, true);
  Color transRed = new Color(0x40FF0000, true);

  public void paint(Graphics g) {
    this.g = g;
    g.setColor(Color.blue);
    g.drawLine(ax[0], ay[0], ax[1], ay[1]);
    g.setColor(Color.green);
    g.drawLine(ax[0], ay[0], ax[2], ay[2]);
    g.drawLine(ax[1], ay[1], ax[2], ay[2]);

    int dx = ax[0] - ax[1];
    int dy = ay[0] - ay[1];
    int len = (int)Math.sqrt(dx*dx + dy*dy);

    int xMid = (ax[0] + ax[1]) / 2;
    int yMid = (ay[0] + ay[1]) / 2;

    int dxTheta = ax[2] - xMid;
    int dyTheta = ay[2] - yMid;

    for (int i = 3; i < 15; ++ i) {
      xOrigin = yOrigin = 50 + i * 10;
      //EllipseShape ellipse = new EllipseShape(i, i, 100, 100);
      //ellipse.render(g, xOrigin, yOrigin);
      doit(i, i, 100, 100);
    }
  }

  void doit(int major, int minor, int dx, int dy) {
    ellipse0a(major, minor, dx, dy);
    g.setColor(transRed);
    //    ellipse1();
    calcCriticalPoints();
    g.setColor(transBlue);
    int nScanlines = yN - yS + 1;
    byte[] ax = new byte[nScanlines];
    byte[] an = new byte[nScanlines];
    ellipse4(ax, an);
    renderRaster(ax, an, yN, nScanlines);
  }

  void renderRaster(byte[] ax, byte[] an, int yN, int nScanlines) {
    int yCurrent = yN;
    for (int i = 0; i < nScanlines; ++i, --yCurrent) {
      setPixels(ax[i], yCurrent, an[i]);
      setPixels(-ax[i], -yCurrent, -an[i]);
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

  long A, B, C, F;
  long twoA, twoB, twoC, _Adiv4, _Bdiv4;
  double scaleFactor;
 
  boolean isInside(int x, int y) {
    return (A*(x*x) + B*(y*y) + C*(2*x*y) + F) <= 0;
  }

  long evalNextMidpointNNE(int x, int y) {
    long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y - 1)+ F +
      (twoA-C)*x + (twoC-B)*y + _Bdiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F + (2*A-C)*x + (2*C-B)*y + A - C + B/4;
    return n;
  }

  long evalNextMidpointENE(int x, int y) {
    long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y - 1)+ F +
      (A-twoC)*x + (C-twoB)*y + _Adiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F + (A-2*C)*x + (C-2*B)*y + B - C + A/4;
    return n;
  }

  long evalNextMidpointESE(int x, int y) {
    long m = A*x*x + B*y*y + 2*C*x*y+ F - (A+2*C)*x - (2*B+C)*y + B + C + A/4;
    long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y + 1)+ F +
      -(A+twoC)*x - (twoB+C)*y + _Adiv4;
    assert m==n;
    return n;
  }

  long evalNextMidpointSSE(int x, int y) {
    long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y + 1)+ F +
      -(twoA+C)*x - (B+twoC)*y + _Bdiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F - (2*A+C)*x - (B+2*C)*y + A + C + B/4;
    return n;
  }

  void ellipse0a(int majorAxisLength, int minorAxisLength, int dx, int dy) {
    if (majorAxisLength < minorAxisLength) {
      int t = majorAxisLength;
      majorAxisLength = minorAxisLength;
      minorAxisLength = t;
      t = dx;
      dx = t;
      dy = dx;
    }
    int dx2 = dx*dx;
    int dy2 = dy*dy;
    int dx2_plus_dy2=dx2+dy2;
    long _2a = majorAxisLength;
    long _2b = minorAxisLength;

    long _4a2 = _2a*_2a;
    long _4b2 = _2b*_2b;

    long mthA = (_4a2*dx2 + _4b2*dy2);
    long mthB = (_4a2*dy2 + _4b2*dx2);
    long mthC = -((_4a2 - _4b2)*(dx*dy));
    long mthF = -(_4a2*_4b2) * dx2_plus_dy2;

    _Adiv4 = mthA;
    A = mthA * 4;
    twoA = A * 2;

    _Bdiv4 = mthB;
    B = mthB * 4;
    twoB = B * 2;

    C = mthC * 4;
    twoC = C * 2;

    F = mthF;

    scaleFactor = 4 * Math.sqrt(dx2_plus_dy2);

    System.out.println("ellipse0a");
    System.out.println(" _2a=" + _2a + " _2b=" + _2b);
    System.out.println("angle: " +
                       " A=" + A + " B=" + B + " C=" + C + " F=" + F +
                       " A/4=" + _Adiv4 + " B/4=" + _Bdiv4);
  }

  void ellipse1() {
    int kmax = 0;
    int kmin = 0;
    for (int i = -127; i < 127; ++i)
      for (int j = - 127; j < 127; ++j) {
        if (isInside(i, j))
          set_pixel(i, j);
      }
  }

  int xN, yN, xNE, yNE, xE, yE, xSE, ySE, xS, yS;

  void calcCriticalPoints() {
    //    g.setColor(transBlue);
    //    int yN = (int)Math.sqrt(A*F/(C*C-A*B));
    //    int xN = (int)(-C/A*yN);
    double sqrtA = Math.sqrt(A);
    double dblxN = -C/sqrtA;
    double dblyN = sqrtA;
    dblxN /= scaleFactor;
    dblyN /= scaleFactor;
    xN = (int)(dblxN - 0.5);
    yN = (int)(dblyN + 0.5);

    double sqrtAplusBminus2C = Math.sqrt(A + B - 2*C);
    double dblxNE = (B - C)/sqrtAplusBminus2C;
    double dblyNE = (A - C)/sqrtAplusBminus2C;
    dblxNE /= scaleFactor;
    dblxNE += (dblxNE < 0) ? -0.5 : 0.5;
    dblyNE /= scaleFactor;
    dblyNE += (dblyNE < 0) ? -0.5 : 0.5;
    xNE = (int)dblxNE;
    yNE = (int)dblyNE;
    //    set_pixel(xNE, yNE);

    double sqrtB = Math.sqrt(B);
    double dblxE = sqrtB;
    dblxE /= scaleFactor;
    dblxE += 0.5;
    double dblyE = -C/sqrtB;
    dblyE /= scaleFactor;
    dblyE += (dblyE < 0) ? -0.5 : 0.5;
    xE = (int)dblxE;
    yE = (int)dblyE;
    //    set_pixel(xE, yE);

    double sqrtAplusBplus2C = Math.sqrt(A + B + 2*C);
    double dblxSE = (B + C)/sqrtAplusBplus2C;
    dblxSE /= scaleFactor;
    dblxSE += (dblxSE < 0) ? -0.5 : 0.5;
    double dblySE = -(A + C)/sqrtAplusBplus2C;
    dblySE /= scaleFactor;
    dblySE += (dblySE < 0) ? -0.5 : 0.5;
    xSE = (int)dblxSE;
    ySE = (int)dblySE;

    yS = -yN;
    xS = -xN;
    //    set_pixel(xS, yS);


    //System.out.println("  N=" + xN  + "," + yN  +
    //                 " NE=" + xNE + "," + yNE +
    //                 "  E=" + xE  + "," + yE  +
    //                 " SE=" + xSE + "," + ySE +
    //                 "  S=" + xS  + "," + yS);
  }

  void ellipse4(byte[] ax, byte[] an) {
    int xCurrent = xN;
    int yCurrent = yN;
    long discriminator;
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
          discriminator += A*(2*xCurrent + 1) + C*(2*yCurrent - 1);
        } else {
          recordCoordinate(xCurrent, --yCurrent);
          discriminator += (A-C)*(2*xCurrent + 1) + (C-B)*2*yCurrent;
        }
        assert discriminator == evalNextMidpointNNE(xCurrent, yCurrent);
      }
      while (xCurrent < xNE)
        recordCoordinate(++xCurrent, yNE);
      yCurrent = yNE;
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
          discriminator += (A-C)*2*xCurrent + (C-B)*(2*yCurrent - 1);
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -( C*(2*xCurrent + 1) + B*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointENE(xCurrent, yCurrent);
      }
      while (yCurrent > yE)
        recordCoordinate(xE, --yCurrent);
      xCurrent = xE;
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
          discriminator += -( C*(2*xCurrent - 1) + B*(2*yCurrent - 1) );
        } else {
          recordCoordinate(--xCurrent, yCurrent);
          discriminator += -( (A+C)*2*xCurrent + (B+C)*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointESE(xCurrent, yCurrent);
      }
      while (yCurrent > ySE)
        recordCoordinate(xSE, --yCurrent);
      xCurrent = xSE;
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
          discriminator += -( (A+C)*(2*xCurrent - 1) + 2*(B+C)*yCurrent );
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -( A*(2*xCurrent - 1) + C*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointSSE(xCurrent, yCurrent);
      }
      while (xCurrent > xS)
        recordCoordinate(--xCurrent, yS);
      yS = yCurrent;
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

  int xOrigin;
  int yOrigin;

  void set_pixel(int x, int y) {
    x += xOrigin;
    y += xOrigin;
    g.drawLine(x, y, x, y);
  }

  void setPixels(int x, int y, int n) {
    x += xOrigin;
    y += yOrigin;
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
    System.out.println(" init  N=" + xN + "," + yN);
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
      ax[iMouse] = xMouse;
      ay[iMouse] = yMouse;
      iMouse = (iMouse + 1) % 3;
      repaint();
    }
  }
}

class EllipseShape {
  int _2a;
  int _2b;
  int dxTheta;
  int dyTheta;

  long A, B, C, F;
  long twoA, twoB, twoC, _Adiv4, _Bdiv4;
  double scaleFactor;

  int xN, yN, xNE, yNE, xE, yE, xSE, ySE, xS, yS;

  int nScanlines;
  byte[] ax;
  byte[] an;
  

  public EllipseShape(int majorAxisLength, int minorAxisLength,
                      int dxTheta, int dyTheta) {
    if (majorAxisLength < minorAxisLength) {
      int t = majorAxisLength;
      majorAxisLength = minorAxisLength;
      minorAxisLength = t;
      t = dxTheta;
      dxTheta = dyTheta;
      dyTheta = t;
    }
    this._2a = majorAxisLength;
    this._2b = minorAxisLength;
    this.dxTheta = dxTheta;
    this.dyTheta = dyTheta;

    ellipse0a();
    calcCriticalPoints();
    nScanlines = yN - yS + 1;
    ax = new byte[nScanlines];
    an = new byte[nScanlines];
    rasterize();
  }

  void ellipse0a() {
    int dx2 = dxTheta*dxTheta;
    int dy2 = dyTheta*dyTheta;
    int dx2_plus_dy2=dx2+dy2;

    long _4a2 = _2a*_2a;
    long _4b2 = _2b*_2b;

    long tmpA = (_4a2*dx2 + _4b2*dy2);
    long tmpB = (_4a2*dy2 + _4b2*dx2);
    long tmpC = -((_4a2 - _4b2)*(dxTheta*dyTheta));
    long tmpF = -(_4a2*_4b2) * dx2_plus_dy2;

    _Adiv4 = tmpA;
    A = tmpA * 4;
    twoA = A * 2;

    _Bdiv4 = tmpB;
    B = tmpB * 4;
    twoB = B * 2;

    C = tmpC * 4;
    twoC = C * 2;

    F = tmpF;

    scaleFactor = 4 * Math.sqrt(dx2_plus_dy2);

    System.out.println("ellipse0a");
    System.out.println(" _2a=" + _2a + " _2b=" + _2b);
    System.out.println("angle: " +
                       " A=" + A + " B=" + B + " C=" + C + " F=" + F +
                       " A/4=" + _Adiv4 + " B/4=" + _Bdiv4);
  }

  void calcCriticalPoints() {
    //    g.setColor(transBlue);
    //    int yN = (int)Math.sqrt(A*F/(C*C-A*B));
    //    int xN = (int)(-C/A*yN);
    double sqrtA = Math.sqrt(A);
    double dblxN = -C/sqrtA;
    double dblyN = sqrtA;
    dblxN /= scaleFactor;
    dblyN /= scaleFactor;
    xN = (int)(dblxN - 0.5);
    yN = (int)(dblyN + 0.5);

    double sqrtAplusBminus2C = Math.sqrt(A + B - 2*C);
    double dblxNE = (B - C)/sqrtAplusBminus2C;
    double dblyNE = (A - C)/sqrtAplusBminus2C;
    dblxNE /= scaleFactor;
    dblxNE += (dblxNE < 0) ? -0.5 : 0.5;
    dblyNE /= scaleFactor;
    dblyNE += (dblyNE < 0) ? -0.5 : 0.5;
    xNE = (int)dblxNE;
    yNE = (int)dblyNE;
    //    set_pixel(xNE, yNE);

    double sqrtB = Math.sqrt(B);
    double dblxE = sqrtB;
    dblxE /= scaleFactor;
    dblxE += 0.5;
    double dblyE = -C/sqrtB;
    dblyE /= scaleFactor;
    dblyE += (dblyE < 0) ? -0.5 : 0.5;
    xE = (int)dblxE;
    yE = (int)dblyE;
    //    set_pixel(xE, yE);

    double sqrtAplusBplus2C = Math.sqrt(A + B + 2*C);
    double dblxSE = (B + C)/sqrtAplusBplus2C;
    dblxSE /= scaleFactor;
    dblxSE += (dblxSE < 0) ? -0.5 : 0.5;
    double dblySE = -(A + C)/sqrtAplusBplus2C;
    dblySE /= scaleFactor;
    dblySE += (dblySE < 0) ? -0.5 : 0.5;
    xSE = (int)dblxSE;
    ySE = (int)dblySE;

    yS = -yN;
    xS = -xN;
    //    set_pixel(xS, yS);


    //System.out.println("  N=" + xN  + "," + yN  +
    //                 " NE=" + xNE + "," + yNE +
    //                 "  E=" + xE  + "," + yE  +
    //                 " SE=" + xSE + "," + ySE +
    //                 "  S=" + xS  + "," + yS);
  }

  long evalNextMidpointNNE(int x, int y) {
    long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y - 1)+ F +
      (twoA-C)*x + (twoC-B)*y + _Bdiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F + (2*A-C)*x + (2*C-B)*y + A - C + B/4;
    return n;
  }

  long evalNextMidpointENE(int x, int y) {
    long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y - 1)+ F +
      (A-twoC)*x + (C-twoB)*y + _Adiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F + (A-2*C)*x + (C-2*B)*y + B - C + A/4;
    return n;
  }

  long evalNextMidpointESE(int x, int y) {
    long m = A*x*x + B*y*y + 2*C*x*y+ F - (A+2*C)*x - (2*B+C)*y + B + C + A/4;
    long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y + 1)+ F +
      -(A+twoC)*x - (twoB+C)*y + _Adiv4;
    assert m==n;
    return n;
  }

  long evalNextMidpointSSE(int x, int y) {
    long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y + 1)+ F +
      -(twoA+C)*x - (B+twoC)*y + _Bdiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F - (2*A+C)*x - (B+2*C)*y + A + C + B/4;
    return n;
  }

  void rasterize() {
    int xCurrent = xN;
    int yCurrent = yN;
    long discriminator;
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
          discriminator += A*(2*xCurrent + 1) + C*(2*yCurrent - 1);
        } else {
          recordCoordinate(xCurrent, --yCurrent);
          discriminator += (A-C)*(2*xCurrent + 1) + (C-B)*2*yCurrent;
        }
        assert discriminator == evalNextMidpointNNE(xCurrent, yCurrent);
      }
      while (xCurrent < xNE)
        recordCoordinate(++xCurrent, yNE);
      yCurrent = yNE;
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
          discriminator += (A-C)*2*xCurrent + (C-B)*(2*yCurrent - 1);
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -( C*(2*xCurrent + 1) + B*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointENE(xCurrent, yCurrent);
      }
      while (yCurrent > yE)
        recordCoordinate(xE, --yCurrent);
      xCurrent = xE;
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
          discriminator += -( C*(2*xCurrent - 1) + B*(2*yCurrent - 1) );
        } else {
          recordCoordinate(--xCurrent, yCurrent);
          discriminator += -( (A+C)*2*xCurrent + (B+C)*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointESE(xCurrent, yCurrent);
      }
      while (yCurrent > ySE)
        recordCoordinate(xSE, --yCurrent);
      xCurrent = xSE;
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
          discriminator += -( (A+C)*(2*xCurrent - 1) + 2*(B+C)*yCurrent );
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -( A*(2*xCurrent - 1) + C*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointSSE(xCurrent, yCurrent);
      }
      while (xCurrent > xS)
        recordCoordinate(--xCurrent, yS);
      yS = yCurrent;
    }
    assert xCurrent == xS && yCurrent == yS;
  }

  void render(Graphics g, int xOrigin, int yOrigin) {
    int yCurrent = yN;
    for (int i = 0; i < nScanlines; ++i, --yCurrent) {
      setPixels(g, xOrigin, yOrigin, ax[i], yCurrent, an[i]);
      setPixels(g, xOrigin, yOrigin, -ax[i], -yCurrent, -an[i]);
    }
  }

  void setPixels(Graphics g, int xOrigin, int yOrigin, int x, int y, int n) {
    x += xOrigin;
    y += yOrigin;
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
    System.out.println(" init  N=" + xN + "," + yN);
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

}

class CylinderShape {
  int _2a;
  int _2b;
  int dxTheta;
  int dyTheta;

  long A, B, C, F;
  long twoA, twoB, twoC, _Adiv4, _Bdiv4;
  double scaleFactor;

  int xN, yN, xNE, yNE, xE, yE, xSE, ySE, xS, yS;

  int nScanlines;
  byte[] ax;
  byte[] an;
  

  public CylinderShape(int x1, int y1, int z1, int x2, int y2, int z2,
                       int width, Color co, 
int majorAxisLength, int minorAxisLength,
                      int dxTheta, int dyTheta) {
    if (majorAxisLength < minorAxisLength) {
      int t = majorAxisLength;
      majorAxisLength = minorAxisLength;
      minorAxisLength = t;
      t = dxTheta;
      dxTheta = dyTheta;
      dyTheta = t;
    }
    this._2a = majorAxisLength;
    this._2b = minorAxisLength;
    this.dxTheta = dxTheta;
    this.dyTheta = dyTheta;

    ellipse0a();
    calcCriticalPoints();
    nScanlines = yN - yS + 1;
    ax = new byte[nScanlines];
    an = new byte[nScanlines];
    rasterize();
  }

  void ellipse0a() {
    int dx2 = dxTheta*dxTheta;
    int dy2 = dyTheta*dyTheta;
    int dx2_plus_dy2=dx2+dy2;

    long _4a2 = _2a*_2a;
    long _4b2 = _2b*_2b;

    long tmpA = (_4a2*dx2 + _4b2*dy2);
    long tmpB = (_4a2*dy2 + _4b2*dx2);
    long tmpC = -((_4a2 - _4b2)*(dxTheta*dyTheta));
    long tmpF = -(_4a2*_4b2) * dx2_plus_dy2;

    _Adiv4 = tmpA;
    A = tmpA * 4;
    twoA = A * 2;

    _Bdiv4 = tmpB;
    B = tmpB * 4;
    twoB = B * 2;

    C = tmpC * 4;
    twoC = C * 2;

    F = tmpF;

    scaleFactor = 4 * Math.sqrt(dx2_plus_dy2);

    System.out.println("ellipse0a");
    System.out.println(" _2a=" + _2a + " _2b=" + _2b);
    System.out.println("angle: " +
                       " A=" + A + " B=" + B + " C=" + C + " F=" + F +
                       " A/4=" + _Adiv4 + " B/4=" + _Bdiv4);
  }

  void calcCriticalPoints() {
    //    g.setColor(transBlue);
    //    int yN = (int)Math.sqrt(A*F/(C*C-A*B));
    //    int xN = (int)(-C/A*yN);
    double sqrtA = Math.sqrt(A);
    double dblxN = -C/sqrtA;
    double dblyN = sqrtA;
    dblxN /= scaleFactor;
    dblyN /= scaleFactor;
    xN = (int)(dblxN - 0.5);
    yN = (int)(dblyN + 0.5);

    double sqrtAplusBminus2C = Math.sqrt(A + B - 2*C);
    double dblxNE = (B - C)/sqrtAplusBminus2C;
    double dblyNE = (A - C)/sqrtAplusBminus2C;
    dblxNE /= scaleFactor;
    dblxNE += (dblxNE < 0) ? -0.5 : 0.5;
    dblyNE /= scaleFactor;
    dblyNE += (dblyNE < 0) ? -0.5 : 0.5;
    xNE = (int)dblxNE;
    yNE = (int)dblyNE;
    //    set_pixel(xNE, yNE);

    double sqrtB = Math.sqrt(B);
    double dblxE = sqrtB;
    dblxE /= scaleFactor;
    dblxE += 0.5;
    double dblyE = -C/sqrtB;
    dblyE /= scaleFactor;
    dblyE += (dblyE < 0) ? -0.5 : 0.5;
    xE = (int)dblxE;
    yE = (int)dblyE;
    //    set_pixel(xE, yE);

    double sqrtAplusBplus2C = Math.sqrt(A + B + 2*C);
    double dblxSE = (B + C)/sqrtAplusBplus2C;
    dblxSE /= scaleFactor;
    dblxSE += (dblxSE < 0) ? -0.5 : 0.5;
    double dblySE = -(A + C)/sqrtAplusBplus2C;
    dblySE /= scaleFactor;
    dblySE += (dblySE < 0) ? -0.5 : 0.5;
    xSE = (int)dblxSE;
    ySE = (int)dblySE;

    yS = -yN;
    xS = -xN;
    //    set_pixel(xS, yS);


    //System.out.println("  N=" + xN  + "," + yN  +
    //                 " NE=" + xNE + "," + yNE +
    //                 "  E=" + xE  + "," + yE  +
    //                 " SE=" + xSE + "," + ySE +
    //                 "  S=" + xS  + "," + yS);
  }

  long evalNextMidpointNNE(int x, int y) {
    long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y - 1)+ F +
      (twoA-C)*x + (twoC-B)*y + _Bdiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F + (2*A-C)*x + (2*C-B)*y + A - C + B/4;
    return n;
  }

  long evalNextMidpointENE(int x, int y) {
    long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y - 1)+ F +
      (A-twoC)*x + (C-twoB)*y + _Adiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F + (A-2*C)*x + (C-2*B)*y + B - C + A/4;
    return n;
  }

  long evalNextMidpointESE(int x, int y) {
    long m = A*x*x + B*y*y + 2*C*x*y+ F - (A+2*C)*x - (2*B+C)*y + B + C + A/4;
    long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y + 1)+ F +
      -(A+twoC)*x - (twoB+C)*y + _Adiv4;
    assert m==n;
    return n;
  }

  long evalNextMidpointSSE(int x, int y) {
    long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y + 1)+ F +
      -(twoA+C)*x - (B+twoC)*y + _Bdiv4;
    assert
      n == A*x*x + B*y*y + 2*C*x*y+ F - (2*A+C)*x - (B+2*C)*y + A + C + B/4;
    return n;
  }

  void rasterize() {
    int xCurrent = xN;
    int yCurrent = yN;
    long discriminator;
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
          discriminator += A*(2*xCurrent + 1) + C*(2*yCurrent - 1);
        } else {
          recordCoordinate(xCurrent, --yCurrent);
          discriminator += (A-C)*(2*xCurrent + 1) + (C-B)*2*yCurrent;
        }
        assert discriminator == evalNextMidpointNNE(xCurrent, yCurrent);
      }
      while (xCurrent < xNE)
        recordCoordinate(++xCurrent, yNE);
      yCurrent = yNE;
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
          discriminator += (A-C)*2*xCurrent + (C-B)*(2*yCurrent - 1);
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -( C*(2*xCurrent + 1) + B*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointENE(xCurrent, yCurrent);
      }
      while (yCurrent > yE)
        recordCoordinate(xE, --yCurrent);
      xCurrent = xE;
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
          discriminator += -( C*(2*xCurrent - 1) + B*(2*yCurrent - 1) );
        } else {
          recordCoordinate(--xCurrent, yCurrent);
          discriminator += -( (A+C)*2*xCurrent + (B+C)*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointESE(xCurrent, yCurrent);
      }
      while (yCurrent > ySE)
        recordCoordinate(xSE, --yCurrent);
      xCurrent = xSE;
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
          discriminator += -( (A+C)*(2*xCurrent - 1) + 2*(B+C)*yCurrent );
        } else {
          recordCoordinate(xCurrent, yCurrent);
          discriminator += -( A*(2*xCurrent - 1) + C*(2*yCurrent - 1) );
        }
        assert discriminator == evalNextMidpointSSE(xCurrent, yCurrent);
      }
      while (xCurrent > xS)
        recordCoordinate(--xCurrent, yS);
      yS = yCurrent;
    }
    assert xCurrent == xS && yCurrent == yS;
  }

  void render(Graphics g, int xOrigin, int yOrigin) {
    int yCurrent = yN;
    for (int i = 0; i < nScanlines; ++i, --yCurrent) {
      setPixels(g, xOrigin, yOrigin, ax[i], yCurrent, an[i]);
      setPixels(g, xOrigin, yOrigin, -ax[i], -yCurrent, -an[i]);
    }
  }

  void setPixels(Graphics g, int xOrigin, int yOrigin, int x, int y, int n) {
    x += xOrigin;
    y += yOrigin;
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
    System.out.println(" init  N=" + xN + "," + yN);
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

}
