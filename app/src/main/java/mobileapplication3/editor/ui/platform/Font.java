package mobileapplication3.editor.ui.platform;

import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.vipaol.mobapp.android.MainActivity;

import java.util.Vector;

public class Font {
	public static final int
		STYLE_PLAIN = 0,
		STYLE_BOLD = 1,
		STYLE_ITALIC = 2,
		STYLE_UNDERLINED = 4,
		SIZE_SMALL = 8,
		SIZE_MEDIUM = 0,
		SIZE_LARGE = 16,
		FACE_SYSTEM = 0,
		FACE_MONOSPACE = 32,
		FACE_PROPORTIONAL = 64,
		FONT_STATIC_TEXT = 0,
		FONT_INPUT_TEXT = 1;

	private Paint p;
	private int size;
	
	public Font() {
		this(SIZE_MEDIUM);
	}
	
	public Font(int size) {
		this.size = size;
		p = new Paint();
        float density = MainActivity.inst.getResources().getDisplayMetrics().density;
        switch (size) {
            case SIZE_SMALL:
                p.setTextSize(20 * density);
                break;
            case SIZE_MEDIUM:
                p.setTextSize(24 * density);
                break;
            case SIZE_LARGE:
                p.setTextSize(38 * density);
                break;
        }
	}
	
	protected Font(Paint p) {
		this.p = p;
	}
	
	protected Paint getPaint() {
		return p;
	}
	
	public int getFace() {
		return FACE_SYSTEM;
	}

	public int getStyle() {
		return STYLE_PLAIN;
	}

	public int getSize() {
		return size;
	}

	public int getHeight() {
        Rect bounds = new Rect();
        p.getTextBounds("Aa", 0, 2, bounds);
        return bounds.height();
	}

	public int stringWidth(String str) {
		return substringWidth(str, 0, str.length());
	}
	
	public int substringWidth(String str, int offset, int len) {
		Rect bounds = new Rect();
		p.getTextBounds(str, offset, offset + len, bounds);
		return bounds.width();
	}
	
	public static Font getDefaultFont() {
		return new Font(SIZE_MEDIUM);
	}
	
	public static int defaultFontStringWidth(String str) {
		return getDefaultFont().stringWidth(str);
	}
	
	public static int defaultFontSubstringWidth(String str, int offset, int len) {
		return getDefaultFont().substringWidth(str, offset, len);
	}
	
	public static int getDefaultFontHeight() {
		return getDefaultFont().getHeight();
	}
	
	public static int getDefaultFontSize() {
		return getDefaultFont().getSize();
	}
	
	public int[][] getLineBounds(String text, int w, int padding) {
        Vector lineBoundsVector = new Vector(text.length() / 5);
        int charOffset = 0;
        if (stringWidth(text) <= w - padding * 2 && text.indexOf('\n') == -1) {
            lineBoundsVector.addElement(new int[]{0, text.length()});
        } else {
            while (charOffset < text.length()) {
                int maxSymsInCurrLine = 1;
                boolean maxLineLengthReached = false;
                boolean lineBreakSymFound = false;
                for (int lineLength = 1; lineLength <= text.length() - charOffset; lineLength++) {
                    if (substringWidth(text, charOffset, lineLength) > w - padding * 2) {
                        maxLineLengthReached = true;
                        break;
                    }
                    
                    maxSymsInCurrLine = lineLength;
                    
                    if (charOffset + lineLength < text.length()) {
                        if (text.charAt(charOffset+lineLength) == '\n') {
                            lineBoundsVector.addElement(new int[]{charOffset, lineLength});
                            charOffset = charOffset + lineLength + 1;
                            lineBreakSymFound = true;
                            break;
                        }
                    }
                }
                
                if (lineBreakSymFound) {
                    continue;
                }
                

                boolean spaceFound = false;

                int maxRightBorder = charOffset + maxSymsInCurrLine;
                
                if (maxRightBorder >= text.length()) {
                    lineBoundsVector.addElement(new int[]{charOffset, maxSymsInCurrLine});
                    break;
                }
                
                if (!maxLineLengthReached) {
                    lineBoundsVector.addElement(new int[]{charOffset, maxSymsInCurrLine}); //
                    charOffset = maxRightBorder;
                } else {
                    for (int i = maxRightBorder; i > charOffset; i--) {
                        if (text.charAt(i) == ' ') {
                            lineBoundsVector.addElement(new int[]{charOffset, i - charOffset});
                            charOffset = i + 1;
                            spaceFound = true;
                            break;
                        }
                    }

                    if (!spaceFound) {
                        lineBoundsVector.addElement(new int[]{charOffset, maxRightBorder - charOffset});
                        charOffset = maxRightBorder;
                    }
                }
            }
        }
        
        int[][] lineBounds = new int[lineBoundsVector.size()][];
        for (int i = 0; i < lineBoundsVector.size(); i++) {
            lineBounds[i] = (int[]) lineBoundsVector.elementAt(i);
        }
        return lineBounds;
    }
}
