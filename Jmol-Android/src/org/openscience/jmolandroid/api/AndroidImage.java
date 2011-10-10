package org.openscience.jmolandroid.api;

import java.io.OutputStream;

import android.graphics.Bitmap;

public class AndroidImage {

	Bitmap bitmap;
	
	public AndroidImage(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	
	public int getWidth() {
		return bitmap.getWidth();
	}

	public int getHeight() {
		return bitmap.getHeight();
	}

	public void setPixel(int x, int y, int argb) {
		bitmap.setPixel(x, y, argb);
	}

	public void compress(Format format, int quality, OutputStream stream) {
		bitmap.compress(format == Format.JPEG ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, quality, stream);
	}

	public Image scaleImage(int width, int height) {
		throw new RuntimeException("Not Implemented");
	}

	public int[] getPixels(int offset, int stride, int x, int y, int width, int height) {
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, offset, stride, x, y, width, height);
		
		return pixels;
	}

}
