package org.openscience.jmolandroid.api;

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
	
	@Override
	public void drawImage(Image image, int x, int y) {
		canvas.drawBitmap(((AndroidImage)image).bitmap, x, y, null);
	}

	@Override
	public void drawImage(Image image, Rectangle source, Rectangle destination) {
		canvas.drawBitmap(((AndroidImage)image).bitmap, ((AndroidRectangle)source).rect, ((AndroidRectangle)destination).rect, null);
	}

	@Override
	public void erase() {
		Paint paint = new Paint();
		paint.setColor(AndroidSurfaceFactory.background);
		
		canvas.drawPaint(paint);
	}

	@Override
	public void drawText(String text, Font font, int x, int y, int color) {
		// TODO: map color argument to the correspondent Android color
		((AndroidFont)font).paint.setColor(Color.WHITE);
		canvas.drawText(text, x, y, ((AndroidFont)font).paint);
	}

	@Override
	public void setPixels(int[] pixels) {
		if (canvas == null) {
			Log.w("AMOL", "Requested to paint on a null canvas");
			return;
		}
		
		canvas.drawBitmap(pixels, 0, canvas.getWidth(), 0, 0, canvas.getWidth(), canvas.getHeight(), true, null);
		//bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
	}

}