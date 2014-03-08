package jspecview.api;

import jspecview.common.ScaleData;

public interface XYScaleConverter {
	
  int fixX(int xPixel);
  double toX(int xPixel);  
  int toPixelX(double x);
	int getXPixels();

  int fixY(int yPixel);
  double toY(int yPixel);  
  int toPixelY(double y);
	int getYPixels();
	int getXPixel0();

	ScaleData getScale();
}
