package org.openscience.jmolandroid.api;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class AndroidSurface {

  Canvas canvas;
  private static int BACKGROUND = 0x0;
	
	public AndroidSurface(Canvas canvas) {
		this.canvas = canvas;
	}

	public AndroidSurface(){}
	
	public void drawImage(Object image, int x, int y) {
		canvas.drawBitmap(((Image) image).bitmap, x, y, null);
	}

	public void drawImage(Image image, Rectangle source, Rectangle destination) {
		canvas.drawBitmap(((AndroidImage)image).bitmap, ((AndroidRectangle)source).rect, ((AndroidRectangle)destination).rect, null);
	}

	public void erase() {
		Paint paint = new Paint();
		paint.setColor(BACKGROUND );
		
		canvas.drawPaint(paint);
	}

	public void setPixels(int[] pixels) {
		if (canvas == null) {
			Log.w("AMOL", "Requested to paint on a null canvas");
			return;
		}		
		canvas.drawBitmap(pixels, 0, canvas.getWidth(), 0, 0, canvas.getWidth(), canvas.getHeight(), true, null);
		//bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
	}

}