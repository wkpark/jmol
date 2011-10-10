package org.openscience.jmolandroid.api;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class AndroidSurface {

  Canvas canvas;
	
	public AndroidSurface(Canvas canvas) {
		this.canvas = canvas;
	}

	public AndroidSurface(){}
	
	public void drawImage(Object image, int x, int y) {
		canvas.drawBitmap((Bitmap) image, x, y, null);
	}

	public void drawImage(Image image, Rectangle source, Rectangle destination) {
		canvas.drawBitmap(((AndroidImage)image).bitmap, ((AndroidRectangle)source).rect, ((AndroidRectangle)destination).rect, null);
	}

	public void erase() {
		Paint paint = new Paint();
		paint.setColor(background);
		
		canvas.drawPaint(paint);
	}

	public void drawText(String text, Object font, int x, int y, int color) {
		// TODO: map color argument to the correspondent Android color
	  Paint paint = (Paint) font;
	        paint.setColor(Color.WHITE);
		canvas.drawText(text, x, y, paint);
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