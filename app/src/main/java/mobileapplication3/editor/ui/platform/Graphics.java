package mobileapplication3.editor.ui.platform;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;

public class Graphics {
	public static final int HCENTER = 1;
	public static final int VCENTER = 2;
	public static final int LEFT = 4;
	public static final int RIGHT = 8;
	public static final int TOP = 16;
	public static final int BOTTOM = 32;
	public static final int BASELINE = 64;

	private Canvas c;
	private Paint p;
	private Font currentFont;
	
	public Graphics(Canvas c) {
		c.save();
		this.c = c;
		p = new Paint();
		p.setStrokeWidth(1);
		p.setTextSize(88);
		currentFont = new Font();
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		drawArc(x, y, width, height, startAngle, arcAngle, false);
	}

	private void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
		if (fill) {
			p.setStyle(Paint.Style.FILL);
		} else {
			p.setStyle(Paint.Style.STROKE);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			c.drawArc(x, y, x + width, y + height, startAngle, arcAngle, false, p);
		} else {
			c.drawCircle(x + width / 2, y + height / 2, width / 2, p);
			c.drawCircle(x + width / 2, y + height / 2, height / 2, p);
		}
	}

	public void drawImage(Image img, int x, int y, int anchor) {
		if (img != null && img.getImage() != null) {
			int w = img.getWidth();
			int h = img.getHeight();
			if ((anchor & HCENTER) != 0) {
				x -= w / 2;
			} else if ((anchor & RIGHT) != 0) {
				x -= w;
			}

			if ((anchor & VCENTER) != 0) {
				y -= h / 2;
			} else if ((anchor & BOTTOM) != 0) {
				y -= h;
			}

			c.drawBitmap(img.getImage(), x + 0.5f, y + 0.5f, p);
		}
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		p.setStyle(Paint.Style.STROKE);
		c.drawLine(x1 + 0.5f, y1 + 0.5f, x2 + 0.5f, y2 + 0.5f, p);
	}

	public void drawRect(int x, int y, int width, int height) {
		drawRoundRect(x, y, width, height, 0, 0, false);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		drawRoundRect(x, y, width, height, arcWidth, arcHeight, false);
	}

	private void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean fill) {
		if (fill) {
			p.setStyle(Paint.Style.FILL);
		} else {
			p.setStyle(Paint.Style.STROKE);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && arcWidth > 0 && arcHeight > 0) {
			c.drawRoundRect(x + 0.5f, y + 0.5f, x + width + 0.5f, y + height + 0.5f, arcWidth / 2f, arcHeight / 2f, p);
		} else {
			c.drawRect(x + 0.5f, y + 0.5f, x + width + 0.5f, y + height + 0.5f, p);
		}
	}

	public void drawString(String str, int x, int y, int anchor) {
		drawSubstring(str, 0, str.length(), x, y, anchor);
	}

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
		Paint p = currentFont.getPaint();
		p.setStyle(Paint.Style.FILL);
		p.setColor(this.p.getColor());
		Rect textBounds = new Rect();
		p.getTextBounds(str, offset, offset + len, textBounds);
		int textW = substringWidth(str, offset, len);
		int textH = currentFont.getHeight();
//		textW = textBounds.width();
//		textH = textBounds.height();
		y += textH;
		if ((anchor & HCENTER) != 0) {
			x -= textW/2;
		}
		if ((anchor & VCENTER) != 0) {
			y -= textH/2;
		}
		c.drawText(str, offset, offset + len, x, y, p);
	}

	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		drawArc(x, y, width, height, startAngle, arcAngle, true);
	}

	public void fillRect(int x, int y, int width, int height) {
		drawRoundRect(x, y, width, height, 0, 0, true);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		drawRoundRect(x, y, width, height, arcWidth, arcHeight, true);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		p.setStyle(Paint.Style.FILL);
		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD); // TODO
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
		path.lineTo(x3, y3);
		path.lineTo(x1, y1);
		path.close();

		c.drawPath(path, p);
		p.setStyle(Paint.Style.STROKE);
	}

	public void setClip(int x, int y, int width, int height) {
		try {
			c.restore();
		} catch (IllegalStateException ignored) { }
		c.save();
		c.clipRect(x, y, x + width, y + height);
	}

	public void setColor(int RGB) {
		p.setColor(RGB + 0xff000000);
	}

	public void setColor(int red, int green, int blue) {
		p.setARGB(255, red, green, blue);
	}
	
	public void setFontSize(int size) {
		currentFont = new Font(size);
	}

	public void setFont(int face, int style, int size) {
		currentFont = new Font(size); // TODO
	}
	
	public void setFont(Font font) {
		currentFont = font;
	}
	
	public Font getFont() {
		return currentFont;
	}

	public int getFontFace() {
		return currentFont.getFace();
	}

	public int getFontStyle() {
		return currentFont.getStyle();
	}

	public int getFontSize() {
		return currentFont.getSize();
	}

	public int getFontHeight() {
		return currentFont.getHeight();
	}

	public int stringWidth(String str) {
		return currentFont.stringWidth(str);
	}

	public int substringWidth(String str, int offset, int len) {
		return currentFont.substringWidth(str, offset, len);
	}

	public int getFontHeight(int face, int style, int size) {
		return currentFont.getHeight();
	}

	public int getClipWidth() {
		return c.getClipBounds().width();
	}

	public int getClipHeight() {
		return c.getClipBounds().height();
	}

	public int getClipX() {
		return c.getClipBounds().left;
	}

	public int getClipY() {
		return c.getClipBounds().top;
	}

	public int getColor() {
		return p.getColor() % 0xff000000;
	}
}
