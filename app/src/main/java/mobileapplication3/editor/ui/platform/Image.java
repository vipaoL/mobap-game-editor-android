package mobileapplication3.editor.ui.platform;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import com.vipaol.mobapp.android.MainActivity;
import com.vipaol.mobapp.android.R;

import java.io.IOException;

public class Image {
	private Bitmap image;
	
	public Image(Bitmap image) {
		if (image == null) {
			Log.d("new Image", "null");
		}
		this.image = image;
	}
	
	public static Image createImage(int width, int height) {
		return new Image(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
	}
	
	public Graphics getGraphics() {
		return new Graphics(new Canvas(image));
	}
	
	public Bitmap getImage() {
		return image;
	}

	public int getWidth() {
		return image.getWidth();
	}

	public int getHeight() {
		return image.getHeight();
	}

	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
		return new Image(Bitmap.createBitmap(rgb, width, height, processAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565));
	}

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
		if (image != null) {
			image.getPixels(rgbData, offset, scanlength, x, y, width, height);
		}
	}
	
	public Image scale(int newWidth, int newHeight) {
		if (image == null) {
			return null;
		}

        int[] rawInput = new int[image.getHeight() * image.getWidth()];
        getRGB(rawInput, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        int[] rawOutput = new int[newWidth * newHeight];

        // YD compensates for the x loop by subtracting the width back out
        int YD = (image.getHeight() / newHeight) * image.getWidth() - image.getWidth();
        int YR = image.getHeight() % newHeight;
        int XD = image.getWidth() / newWidth;
        int XR = image.getWidth() % newWidth;
        int outOffset = 0;
        int inOffset = 0;

        for (int y = newHeight, YE = 0; y > 0; y--) {
            for (int x = newWidth, XE = 0; x > 0; x--) {
                rawOutput[outOffset++] = rawInput[inOffset];
                inOffset += XD;
                XE += XR;
                if (XE >= newWidth) {
                    XE -= newWidth;
                    inOffset++;
                }
            }
            inOffset += YD;
            YE += YR;
            if (YE >= newHeight) {
                YE -= newHeight;
                inOffset += image.getWidth();
            }
        }
        return createRGBImage(rawOutput, newWidth, newHeight, true);

    }

	public static Image createImage(String source) throws IOException {
		if (source.startsWith("/")) {
			source = source.substring(1);
		}
		if (source.endsWith(".png")) {
			source = source.substring(0, source.length() - 4);
		}
		Log.d("Getting resource", source);
		Resources resources = MainActivity.inst.getResources();
		int id = resources.getIdentifier(source, "drawable", MainActivity.inst.getPackageName());
        return new Image(BitmapFactory.decodeResource(resources, id));
	}
}
