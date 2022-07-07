package com.vipaol.mobapgameeditor;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    int COLOR_LNDSCP = Color.parseColor("#4444ff");
    int COLOR_SELECTED = Color.parseColor("#8822ff");
    int centrPointRadius = 2;



    short currPlacingID = 0;
    int selectedOption = -1;
    int selectedOptionInUpperRow = -1;
    int selectedInList = 0;

    MgStruct mgStruct = new MgStruct();
    Elements elements = new Elements();
    Paint paint = new Paint();
    EditorCanvas editorCanvas;
    static Context contex;

    boolean inited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contex = this;
        editorCanvas = new EditorCanvas(this);
        setContentView(editorCanvas);
        editorCanvas.init();
        load();
    }

    class EditorCanvas extends View {
        public EditorCanvas(Context context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
        }

        int w = 0;
        int h = 0;

        int offsetX = 0, offsetY = 0;
        int dMouseXOnScreen = 0, dMouseYOnScreen = 0;
        int mouseX, mouseY;
        int mouseOnScreenX = 0, mouseOnScreenY = 0;
        int zoomBase = 1000, zoomOut = zoomBase;
        int btnW, btnH;
        int xBtnOffset = 0;

        void init() {
            w = getWidth();
            h = getHeight();
            btnW = w / mainBtns.length;
            btnH = h / 24;
            offsetX = w / 2;
            offsetY = h / 2;
            mouseX = 0;
            mouseY = 0;
            mouseOnScreenX = calcX(mouseX);
            mouseOnScreenY = calcY(mouseY);
            textSize = getOptimalTextSize(btnW, btnH, "XXXXXX");
            lesserTextSize = getOptimalTextSize(btnW, btnH / 3, "XXXXXX");
            //paint.setTextSize(24 * w * h / 1080 / 1920);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas g) {
            if (!inited) {
                init();
                inited = true;
            }

            g.drawColor(Color.BLACK);

            drawCar(g);

            for (int i = 0; i < mgStruct.getBufSize(); i++) { // draw all placed elements
                short structID = mgStruct.readBuf(i)[0];
                if (structID == 0) // id 0 is end of file
                    break;
                if (i == selectedInList) // draw selected element with different color
                    paint.setColor(COLOR_SELECTED);
                else
                    paint.setColor(COLOR_LNDSCP);
                drawElement(g, paint, structID, mgStruct.readBuf(i));
            }

            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            g.drawRect(calcX(0) - centrPointRadius, calcY(0) - centrPointRadius, // draw green start point
                    calcX(0) + centrPointRadius, calcY(0) + centrPointRadius, paint);


            paint.setStyle(Paint.Style.STROKE);
            if (applyZoom(1) > 1) {
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                if (dMouseXOnScreen != 0) {
                    paint.setAlpha(Math.min(255, ((dMouseXOnScreen > 0) ? 255 : -255) * dMouseXOnScreen / applyZoom(1)));
                    int x = mouseX + ((dMouseXOnScreen > 0) ? 1 : -1);
                    g.drawLine(calcX(x) - 5, calcY(mouseY), calcX(x) + 5, calcY(mouseY), paint);
                }
                if (dMouseYOnScreen != 0) {
                    paint.setAlpha(Math.min(255, ((dMouseYOnScreen > 0) ? 255 : -255) * dMouseYOnScreen / applyZoom(1)));
                    int y = mouseY + ((dMouseYOnScreen > 0) ? 1 : -1);
                    g.drawLine(calcX(mouseX), calcY(y) - 5, calcX(mouseX), calcY(y) + 5, paint);
                    //g.drawCircle(calcX(mouseX), calcY(mouseY + ((dMouseYOnScreen > 0) ? 1 : -1)), 3, paint);
                }
            }

            paint.setColor(Color.WHITE);
            g.drawCircle(calcX(mouseX), calcY(mouseY), 4, paint); // draw cursor

            String hint = mouseX + " " + mouseY;
            if (elements.isBusy) { // preview when placing an element
                if (elements.step > 1) {
                    short id = elements.currentPlacing[0];
                    int step = elements.step;
                    elements.calcArgs(id, step, (short) mouseX, (short) mouseY);
                    if (id == 3) {
                        hint = String.valueOf(elements.currentPlacing[3]);
                    }
                    drawElement(g, paint, id, elements.currentPlacing);
                }
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(lesserTextSize);
            g.drawText(hint, mouseOnScreenX + 30, mouseOnScreenY + 30, paint);
            paint.setStyle(Paint.Style.STROKE);
            drawGUI(g);
        }

        int xWhenPressed = 0, yWhenPressed = 0;
        int xMouseWhenPressed = 0, yMouseWhenPressed = 0;
        int prevPointerX = 0, prevPointerY = 0;
        int initialPointersDistance = 1, initialZoomOut = zoomOut;;
        boolean isInitPointersDistanceRefreshed = false, zooming = false, btnPressed = false;
        int initialOffsetX = offsetX, initialOffsetY = offsetY;
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();

            switch(e.getAction()) {
                case MotionEvent.ACTION_DOWN: /////////////////////////////////////////////
                    initialZoomOut = zoomOut;
                    initialOffsetX = offsetX;
                    initialOffsetY = offsetY;
                    xWhenPressed = x;
                    yWhenPressed = y;
                    prevPointerX = x;
                    prevPointerY = y;
                    xMouseWhenPressed = mouseOnScreenX;
                    yMouseWhenPressed = mouseOnScreenY;
                    if (input(Math.round(e.getX()), Math.round(e.getY())) != -1) {
                        btnPressed = true;
                    }
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE: //////////////////////////////////////////////////////
                    if (btnPressed) {
                        input(Math.round(e.getX()), Math.round(e.getY()));
                        invalidate();
                        break;
                    }
                    if (e.getPointerCount() > 1) { // zoom
                        if (!zooming) {
                            initialZoomOut = zoomOut;
                            initialOffsetX = offsetX;
                            initialOffsetY = offsetY;
                            initialPointersDistance = calcDistance((int) e.getX(0), (int) e.getY(0), (int) e.getX(1), (int) e.getY(1));
                            zooming = true;
                        }
                        zoomOut = initialZoomOut * initialPointersDistance / calcDistance((int) e.getX(0), (int) e.getY(0), (int) e.getX(1), (int) e.getY(1));
                        offsetX = mouseOnScreenX - applyZoom(mouseX);
                        offsetY = mouseOnScreenY - applyZoom(mouseY);
                        invalidate();
                        break;
                    } else {
                        if (zooming) {
                            prevPointerX = x;
                            prevPointerY = y;
                            zooming = false;
                        }
                    }

                    dMouseXOnScreen += x - prevPointerX; // cursor moving
                    dMouseYOnScreen += y - prevPointerY; //
                    prevPointerX = x;
                    prevPointerY = y;
                    int dMouseXAbs = revApplyZoom(dMouseXOnScreen);
                    int dMouseYAbs = revApplyZoom(dMouseYOnScreen);

                    if (dMouseXAbs != 0) {
                        mouseX += dMouseXAbs;
                        mouseOnScreenX = calcX(mouseX);
                        dMouseXOnScreen = 0;
                    }
                    if (dMouseYAbs != 0) {
                        mouseY += dMouseYAbs;
                        mouseOnScreenY = calcY(mouseY);
                        dMouseYOnScreen = 0;
                    }
                    if (mouseOnScreenX > w - w/5) {
                        offsetX -= mouseOnScreenX - w + w/5;
                        mouseOnScreenX = calcX(mouseX);
                    }
                    if (mouseOnScreenX < w/5) {
                        offsetX -= mouseOnScreenX - w/5;
                        mouseOnScreenX = calcX(mouseX);
                    }
                    if (mouseOnScreenY > h - h/5) {
                        offsetY -= mouseOnScreenY - h + h/5;
                        mouseOnScreenY = calcY(mouseY);
                    }
                    if (mouseOnScreenY < h/5) {
                        offsetY -= mouseOnScreenY - h/5;
                        mouseOnScreenY = calcY(mouseY);
                    }

                    invalidate();
                    break;
                case MotionEvent.ACTION_UP: ///////////////////////////////////////////////////
                    isInitPointersDistanceRefreshed = false;
                    if (e.getPointerCount() <= 1) {
                        zooming = false;
                    }
                    initialZoomOut = zoomOut;

                    if (btnPressed) {
                        btnPressed = false;
                        int touchedRow = input(Math.round(e.getX()), Math.round(e.getY()));
                        if (touchedRow != -1) {
                            if (touchedRow == MAIN_ROW) {
                                handleMainRowSelected();
                            } else if (touchedRow == UPPER_ROW) {
                                handleListMenuSelected();
                            } else if (touchedRow == LIST_ROW) {
                                if (selectedInList == prevSelected) {
                                    elements.edit(selectedInList);
                                }
                                prevSelected = selectedInList;
                            } else if (touchedRow == EXPANDED_MENU_ROW) {
                                selectedOptionInUpperRow = (x - xBtnOffset) / btnW;
                                handleExpandedMenuSelected();
                            }
                            invalidate();
                            break;
                        }
                        selectedOption = -1;
                        selectedOptionInUpperRow = -1;
                    }

                    if (currPlacingID != 0 & !zooming) { // next step of element placing
                        if (elements.clicked(mouseX, mouseY)) {
                            currPlacingID = 0;
                        }
                    }
                    invalidate();
                    break;
            }
            return true;
        }

        public void drawElement(Canvas g, Paint paint, short id, short[] data) {
            if (id == 1) {
                Paint p = new Paint();
                p.setStyle(Paint.Style.FILL);
                int x = data[1];
                int y = data[2];
                int r = centrPointRadius*3/2;
                paint.setColor(Color.RED);
                g.drawCircle(calcX(x), calcY(y), r, paint);
            } else if (id == 2) { // line
                int x1 = data[1];
                int y1 = data[2];
                int x2 = data[3];
                int y2 = data[4];
                g.drawLine(calcX(x1), calcY(y1), calcX(x2), calcY(y2), paint);
            } else if (id == 3) { // arc
                int x = data[1];
                int y = data[2];
                int r = data[3];
                int ang = data[4];
                int offset = data[5];
                int kx = data[6];
                int ky = data[7];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int zoomed2R = applyZoom(r) * 2;
                    g.drawArc(calcX(x - r * kx / 100), calcY(y - r * ky / 100), calcX(x + r * kx / 100), calcY(y + r * ky / 100), offset, ang, false, paint);
                } else {
                    g.drawCircle(calcX(x), calcY(y), applyZoom(r), paint);
                }
            } else if (id == 4) { // breakable line
                int x1 = data[1];
                int y1 = data[2];
                int x2 = data[3];
                int y2 = data[4];
                int ths = data[5];
                ths = applyZoom(ths);
                if (ths <= 0)
                    ths = 1;
                int platfL = data[6];
                int spacing = data[7];
                Paint p = new Paint();
                p.setColor(paint.getColor());
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(ths);
                int dx = x2 - x1;
                int dy = y2 - y1;

                int l;
                if (dy == 0) {
                    l = dx;
                } else if (dx == 0) {
                    l = dy;
                } else {
                    l = calcDistance(x1, y1, x2, y2);
                }

                if (l <= 0) {
                    l = 1;
                }

                if (platfL <= 0) {
                    platfL = 50;
                }

                int spX = spacing * dx / l;
                int spY = spacing * dy / l;

                int n = l / (platfL + spacing);
                for (int i = 0; i < n; i++) {
                    g.drawLine(calcX(x1 + i * dx / n), calcY(y1 + i * dy / n), calcX(x1 + (i + 1) * (dx / n) - spX), calcY(y1 + (i + 1) * (dy / n) - spY), p);
                }
            } else if (id == 5) { // breakable arc
                int x = data[1];
                int y = data[2];
                int r = data[3];
                int ang = data[4];
                int offset = data[5];
                int kx = data[6];
                int ky = data[7];
                int ths = data[8];
                r += ths;
                Paint p = new Paint();
                p.setColor(paint.getColor());
                p.setStyle(Paint.Style.STROKE);
                //paint.setAntiAlias(true);
                p.setStrokeWidth(ths);
                int zoomed2R = applyZoom(r) * 2;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    g.drawArc(calcX(x - r * kx / 100), calcY(y - r * ky / 100), calcX(x - r) + zoomed2R * kx / 100, calcY(y - r) + zoomed2R * ky / 100, offset, ang, false, p);
                }
            }
        }

        int carbodyLength = 240;
        int carbodyHeight = 40;
        int wheelRadius = 40;
        int carX = 0 - (carbodyLength / 2 - wheelRadius);
        int carY = 0 - wheelRadius / 2 * 3 - 2;
        int lwX = carX - (carbodyLength / 2 - wheelRadius);
        int lwY = carY + wheelRadius / 2;
        int rwX = carX + (carbodyLength / 2 - wheelRadius);
        int rwY = carY + wheelRadius / 2;

        void drawCar(Canvas g) {
            Paint p = new Paint();
            p.setColor(Color.DKGRAY);
            p.setStyle(Paint.Style.STROKE);
            g.drawRect(calcX(carX - carbodyLength / 2), calcY(carY - carbodyHeight / 2), calcX(carX - carbodyLength / 2 + carbodyLength), calcY(carY - carbodyHeight / 2 + carbodyHeight), p);
            lwX = calcX(carX - (carbodyLength / 2 - wheelRadius));
            lwY = calcY(carY + wheelRadius / 2);
            rwX = calcX(carX + (carbodyLength / 2 - wheelRadius));
            rwY = calcY(carY + wheelRadius / 2);
            int wheelRadiusSus = wheelRadius * 1000 / zoomOut;
            p.setColor(Color.BLACK);
            p.setStyle(Paint.Style.FILL);
            g.drawCircle(lwX, lwY, wheelRadiusSus, p);
            g.drawCircle(rwX, rwY, wheelRadiusSus, p);
            p.setColor(Color.DKGRAY);
            p.setStyle(Paint.Style.STROKE);
            g.drawCircle(lwX, lwY, wheelRadiusSus, p);
            g.drawCircle(rwX, rwY, wheelRadiusSus, p);
            int lineEndX = carX - carbodyLength / 2 - wheelRadius / 2;
            int lineStartX = lineEndX - wheelRadius;
            int lineY = carY + carbodyHeight / 3;
            g.drawLine(calcX(lineStartX), calcY(lineY), calcX(lineEndX), calcY(lineY), p);
            lineStartX += carbodyHeight / 3;
            lineEndX += carbodyHeight / 3;
            lineY += carbodyHeight / 3;
            g.drawLine(calcX(lineStartX), calcY(lineY), calcX(lineEndX), calcY(lineY), p);
            lineStartX -= carbodyHeight * 2 / 3;
            lineEndX -= carbodyHeight * 2 / 3;
            lineY -= carbodyHeight * 2 / 3;
            g.drawLine(calcX(lineStartX), calcY(lineY), calcX(lineEndX), calcY(lineY), p);
        }


        float textSize;
        float lesserTextSize;
        String[] mainBtns = {"Load", "Save", "Line", "Circle", "More", "List"};
        String[] editorBtns = {"Edit", "Delete"};
        String[] expandedMenuBtns = {"Breakable line"/*, "Brekable circle"*/};
        boolean isMenuExpanded = false;
        boolean isListShown = false;

        void drawGUI(Canvas g) {
            Paint paint = new Paint();
            paint.setTextSize(textSize);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.parseColor("#303080"));
            if (selectedOption > -1) {
                g.drawRect(selectedOption * btnW, h - btnH, (selectedOption + 1) * btnW, h - 1, paint);
            }
            paint.setColor(Color.WHITE);
            g.drawLine(0, h - btnH, w, h - btnH, paint);
            //g.drawLine(0, h - 1, w, h - 1, paint);
            Rect bounds = new Rect();
            for (int i = 0; i < mainBtns.length; i++) {
                paint.getTextBounds(mainBtns[i], 0, mainBtns[i].length(), bounds);
                g.drawText(mainBtns[i], i * btnW + btnW / 2, h - btnH / 2 + bounds.height() / 2, paint);
            }
            for (int i = 1; i < mainBtns.length; i++) {
                g.drawLine(i * btnW, h - btnH, i * btnW, h - 1, paint);
            }
            if (isListShown) {
                paint.setColor(Color.parseColor("#303080"));
                if (selectedInList > -1) {
                    g.drawRect(xBtnOffset + 2 * btnW, h - btnH * (selectedInList + 2), xBtnOffset + 4 * btnW, h - 1 - btnH * (selectedInList + 1), paint);
                }
                if (selectedOptionInUpperRow > -1) {
                    if (selectedOptionInUpperRow == 1 & (selectedInList == 0 | mgStruct.buffer[selectedInList][0] == 1)) {
                        paint.setColor(Color.DKGRAY);
                    }
                    g.drawRect(xBtnOffset + selectedOptionInUpperRow * btnW, h - btnH * 5 / 2, xBtnOffset + (selectedOptionInUpperRow + 1) * btnW, h - 1 - btnH * 3 / 2, paint);
                }
                paint.setColor(Color.WHITE);
                g.drawLine(xBtnOffset, h - btnH * 5 / 2, w - btnW * editorBtns.length, h - btnH * 5 / 2, paint);
                g.drawLine(xBtnOffset, h - btnH * 3 / 2, w - btnW * editorBtns.length, h - btnH * 3 / 2, paint);
                g.drawLine(xBtnOffset + btnW * editorBtns.length, h - btnH, xBtnOffset + btnW * editorBtns.length, h - btnH * (listData.length + 1), paint);

                for (int i = 0; i < editorBtns.length + 1; i++) {
                    g.drawLine(xBtnOffset + i * btnW, h - btnH * 5 / 2, xBtnOffset + i * btnW, h - 1 - btnH * 3 / 2, paint);
                }
                for (int i = 0; i < listData.length; i++) {
                    g.drawLine(w - btnW * 2, h - btnH * (i + 2), w, h - btnH * (i + 2), paint);
                }

                for (int i = 0; i < listData.length; i++) {
                    paint.getTextBounds(listData[i], 0, listData[i].length(), bounds);
                    g.drawText(listData[i], w - btnW, h - btnH - btnH / 2 + bounds.height() / 2 - i * btnH, paint);
                }
                for (int i = 0; i < editorBtns.length; i++) {
                    try {
                        if (i == 1 & (selectedInList == 0 | mgStruct.buffer[selectedInList][0] == 1)) {
                            paint.setColor(Color.GRAY);
                        }
                    } catch (NullPointerException ex) {

                    }
                    paint.getTextBounds(editorBtns[i], 0, editorBtns[i].length(), bounds);

                    g.drawText(editorBtns[i], xBtnOffset + i * btnW + btnW / 2, h - btnH * 2 + bounds.height() / 2, paint);
                }

            }
            ////////
            if (isMenuExpanded) {
                paint.setTextSize(lesserTextSize);
                Log.i("SIIIIIIIIIIIIIIIIZE!!!!", lesserTextSize + " " + textSize);
                if (selectedOptionInUpperRow > -1) {
                    paint.setColor(Color.parseColor("#303080"));
                    g.drawRect(xBtnOffset + selectedOptionInUpperRow * btnW, h - btnH * 2, xBtnOffset + (selectedOptionInUpperRow + 1) * btnW, h - 1 - btnH, paint);
                }
                paint.setColor(Color.WHITE);
                g.drawLine(xBtnOffset, h - btnH * 2, xBtnOffset + btnW * expandedMenuBtns.length, h - btnH * 2, paint);
                for (int i = 0; i < expandedMenuBtns.length + 1; i++) {
                    g.drawLine(xBtnOffset + i * btnW, h - btnH * 2, xBtnOffset + i * btnW, h - 1 - btnH, paint);
                }
                for (int i = 0; i < expandedMenuBtns.length; i++) {
                    paint.getTextBounds(expandedMenuBtns[i], 0, expandedMenuBtns[i].length(), bounds);
                    if (bounds.width() < btnW - btnW / 8) {
                        g.drawText(expandedMenuBtns[i], xBtnOffset + i * btnW + btnW / 2, h - btnH * 3 / 2 + bounds.height() / 2, paint);
                        //Log.i("<", "<");
                    } else {
                        String[] text = expandedMenuBtns[expandedMenuBtns.length - 1 - i].split(" ");
                        //Log.i("l", String.valueOf(text.length));
                        for (int j = 0; j < text.length; j++) {
                            paint.getTextBounds(text[j], 0, text[j].length(), bounds);
                            int x = xBtnOffset + i * btnW + btnW / 2;
                            int y = h - btnH *2 + bounds.height()/2 + (btnH * j / text.length) + btnH / text.length / 2;
                            g.drawText(text[j], x, y, paint);
                        }
                    }
                }
            }
        }

        int MAIN_ROW = 0;
        int UPPER_ROW = 1;
        int LIST_ROW = 2;
        int EXPANDED_MENU_ROW = 3;
        int prevSelected = selectedInList;
        int input(int x, int y) { // sorting touch events
            if (y >= h - btnH) {
                selectedOption = x * mainBtns.length / w;
                return MAIN_ROW;
            } else if (isListShown) {
                if (y >= h - btnH * 5 / 2 & y < h - btnH * 3 / 2 & x < xBtnOffset + btnW * editorBtns.length & (x - xBtnOffset) >= 0) {
                    selectedOptionInUpperRow = (x - xBtnOffset) / btnW;
                    return UPPER_ROW;
                } else if (y < h - btnH & x > xBtnOffset + btnW * editorBtns.length) {
                    selectedInList = (h - y - btnH) / btnH;
                    if (selectedInList >= listData.length) {
                        selectedInList = listData.length - 1;
                        return -1;
                    }
                    return LIST_ROW;
                }
            } else if (isMenuExpanded) {
                if (y >= h - btnH * 2 & y < h - btnH & x < xBtnOffset + btnW * expandedMenuBtns.length & (x - xBtnOffset) >= 0) {
                    selectedOptionInUpperRow = (x - xBtnOffset) / btnW;
                    return EXPANDED_MENU_ROW;
                }
            }
            return -1;
        }

        int inToolbarActionBtns = 2;
        int inToolbarElementBtnsN = 2;
        void handleMainRowSelected() {
            if (selectedOption >= inToolbarActionBtns & selectedOption < inToolbarActionBtns + inToolbarElementBtnsN) {
                elements.place(2 + selectedOption - inToolbarActionBtns, mouseX, mouseY);
            } else {
                elements.cancel();
                if (selectedOption == 0) {
                    load();
                    requestPerms();
                    selectedOption = -1;
                    init();
                } else if (selectedOption == 1) {
                    mgStruct.saveToFile();
                    requestPerms();
                    selectedOption = -1;
                } else if (selectedOption == mainBtns.length - 2) {
                    showHideExpandedMenu();
                } else if (selectedOption == mainBtns.length - 1) {
                    showHideList();
                }
            }
        }

        void handleListMenuSelected() {
            elements.cancel();
            if (selectedOptionInUpperRow == 0) {
                elements.edit(selectedInList);
            } else if (selectedOptionInUpperRow == 1) {
                elements.delete(selectedInList);
            }
            selectedOptionInUpperRow = -1;
        }
        void handleExpandedMenuSelected() {
            elements.cancel();
            elements.place(4 + selectedOptionInUpperRow, mouseX, mouseY);
        }

        void showHideList() {
            isListShown = !isListShown;
            isMenuExpanded = false;
            if (!isListShown) {
                selectedOption = -1;
            } else {
                xBtnOffset = w - btnW * (editorBtns.length + 2);
                reloadList();
            }
        }

        void showHideExpandedMenu() {
            isListShown = false;
            isMenuExpanded = !isMenuExpanded;
            if (!isMenuExpanded) {
                selectedOption = -1;
            } else {
                xBtnOffset = w - btnW * Math.max(2, expandedMenuBtns.length);
            }
        }
        /**
         * Sets the text size for a Paint object so a given string of text will be a
         * given width.
         *
         * //@param paint
         *            the Paint to set the text size for
         * @param desiredWidth
         *            the desired width
         * @param text
         *            the text that should be that width
         */
        private float getOptimalTextSize(float desiredWidth, float desiredHeight,
                                                String text) {

            // Pick a reasonably large value for the test. Larger values produce
            // more accurate results, but may cause problems with hardware
            // acceleration. But there are workarounds for that, too; refer to
            // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            final float testTextSize = 128f;

            // Get the bounds of the text, using our testTextSize.
            Paint paint = new Paint();
            paint.setTextSize(testTextSize);
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            // Calculate the desired size as a proportion of our testTextSize.
            float desiredTextSize = testTextSize * desiredWidth / bounds.width();
            desiredTextSize = Math.min(desiredTextSize, testTextSize * desiredHeight / bounds.height());

            // Set the paint for that size.
            return desiredTextSize;
        }
        private int calcX(int x) {
            return x * 1000 / zoomOut + offsetX;
        }

        private int calcY(int y) {
            return y * 1000 / zoomOut + offsetY;
        }

        private int revCalcX(int x) {
            return (x - offsetX) * zoomOut / 1000;
        }

        private int revCalcY(int y) {
            return (y - offsetY) * zoomOut / 1000;
        }

        private int applyZoom(int a) {
            return a * 1000 / zoomOut;
        }

        private int revApplyZoom(int a) {
            return a * zoomOut / 1000;
        }
    }

    public class Elements {

        short[] currentPlacing;
        private boolean isBusy = false;
        private int step = 0;
        short x0 = 0;
        short y0 = 0;

        public boolean place(int id, int x, int y) {
            if (id == 0) {
                currPlacingID = (short) id;
                isBusy = false;
                step = 0;
            } else if (!isBusy) {
                isBusy = true;
                currPlacingID = (short) id;
                step = 1;
                currentPlacing = new short[mgStruct.args[currPlacingID] + 1];
                currentPlacing[0] = currPlacingID;
                elements.clicked(x, y);
                return true;
            } else {
                int currID = currPlacingID;
                cancel();
                if (id != currID) {
                    place(id, x, y);
                } else {
                    selectedOption = -1;
                    selectedOptionInUpperRow = -1;
                }
            }
            return false;
        }

        public boolean clicked(int x, int y) {
            short xShort = (short) x;
            short yShort = (short) y;
            if (isBusy) {
                if (step >= mgStruct.clicks[currPlacingID]) {
                    selectedOption = -1;
                    selectedOptionInUpperRow = -1;
                    isBusy = false;
                    for (int i : currentPlacing) {
                        System.out.print(i + " ");
                    }
                    mgStruct.saveToBuffer(currentPlacing);
                    selectedInList = mgStruct.length - 1;
                    reloadList();
                    calcEndPoint();
                    currPlacingID = 0;
                    return true;
                }
                calcArgs(currPlacingID, step, xShort, yShort);
                step++;
            }
            return false;
        }

        void calcArgs(short id, int step, short x, short y) {
            /*if (mouseOnCanvX < 0 | mouseOnCanvX > w | mouseOnCanvY < 0 | mouseOnCanvY > h) {
                cancel();
                return;
            }*/
            if (currPlacingID == 1) {
                currentPlacing[1] = x;
                currentPlacing[2] = y;
            } else
            if (currPlacingID == 2) {
                if (step == 1) {
                    currentPlacing[1] = x;
                    currentPlacing[2] = y;
                } else if (step == 2) {
                    currentPlacing[3] = x;
                    currentPlacing[4] = y;
                }
            } else
            if (currPlacingID == 3) {
                if (step == 1) {
                    currentPlacing[1] = x;
                    currentPlacing[2] = y;
                }
                if (step == 2) {
                    x0 = currentPlacing[1];
                    y0 = currentPlacing[2];
                    short dx = (short) (x - x0);
                    short dy = (short) (y - y0);
                    //System.out.println(dx + " " + dy);
                    currentPlacing[3] = (short) Math.sqrt(dx * dx + dy * dy);
                    //System.out.println("r:" + currentPlacing[carriage]);
                    currentPlacing[4] = 360;
                    currentPlacing[5] = 0;
                    currentPlacing[6] = 100;
                    currentPlacing[7] = 100;
                }
            } else
            if (currPlacingID == 4) {
                if (step == 1) {
                    currentPlacing[1] = x;
                    currentPlacing[2] = y;
                } else if (step == 2) {
                    currentPlacing[3] = x;
                    currentPlacing[4] = y;
                    currentPlacing[5] = 20;
                    int dx = currentPlacing[3] - currentPlacing[1];
                    int dy = currentPlacing[4] - currentPlacing[2];
                    int l;
                    int spacing = 10;
                    if (dy == 0) {
                        l = dx;
                    } else if (dx == 0) {
                        l = dy;
                    } else {
                        l = calcDistance(dx, dy);
                    }
                    l += spacing;
                    if (l <= 0) {
                        l = 1;
                    }
                    int optimalPlatfL = 100;
                    int platfL = optimalPlatfL;
                    if (platfL > l) {
                        platfL = l;
                    }
                    int platfL1 = platfL;
                    while (l%platfL != 0 & platfL < l & l%platfL1 != 0) {
                        platfL++;
                        platfL1--;
                    }
                    if (l%platfL == 0) {
                        platfL1 = platfL;
                    }
                    platfL = platfL1;
                    platfL -= spacing;
                    currentPlacing[6] = (short) platfL;
                    currentPlacing[7] = (short) spacing;
                    currentPlacing[8] = (short) (l - spacing);
                    currentPlacing[9] = (short) Math.toDegrees(Math.atan2(dy, dx));
                }
            } else
            if (currPlacingID == 5) {
                if (step == 1) {
                    currentPlacing[1] = x;
                    currentPlacing[2] = y;
                }
                if (step == 2) {
                    x0 = currentPlacing[1];
                    y0 = currentPlacing[2];
                    //short dx = (short) (x - x0);
                    //short dy = (short) (y - y0);
                    //System.out.println(dx + " " + dy);
                    currentPlacing[3] = (short) calcDistance(x0, y0, x, y);
                    //System.out.println("r:" + currentPlacing[carriage]);
                    currentPlacing[4] = 360;
                    currentPlacing[5] = 0;
                    currentPlacing[6] = 100;
                    currentPlacing[7] = 100;
                    currentPlacing[8] = 20;
                }
            }
        }

        public void calcEndPoint() {
            short x = 0;
            short y = 0;
            for (int i = 1; i < mgStruct.length; i++) {
                short currCheckingX = mgStruct.buffer[i][1];
                short currCheckingY = mgStruct.buffer[i][2];
                if (mgStruct.buffer[i][0] == 2) {
                    short x2nd = mgStruct.buffer[i][3];
                    short y2nd = mgStruct.buffer[i][4];
                    if (compareAsEnds(currCheckingX, currCheckingY, x2nd, y2nd)) {
                        currCheckingX = x2nd;
                        currCheckingY = y2nd;
                    }
                }
                if (compareAsEnds(x, y, currCheckingX, currCheckingY)) {
                    x = currCheckingX;
                    y = currCheckingY;
                }
            }
            mgStruct.buffer[0][1] = x;
            mgStruct.buffer[0][2] = y;
        }
        private boolean compareAsEnds(short x, short y, short currCheckingX, short currCheckingY) {
            if (currCheckingX >= x) {
                if (currCheckingX > x | (currCheckingY > y)) {
                    x = currCheckingX;
                    y = currCheckingY;
                    return true;
                }
            }
            return false;
        }

        public void cancel() {
            place(0, 0, 0);
        }

        void delete(int i) {
            if (i == 0 | mgStruct.buffer[i][0] == 1) {
                return;
            }
            mgStruct.length--;
            for (int j = i; j < mgStruct.length; j++) {
                mgStruct.buffer[j] = mgStruct.buffer[j + 1];
            }
            selectedInList = mgStruct.length - 1;
            calcEndPoint();
            reloadList();
        }
        void edit(int i) {
            Intent intent = new Intent(MainActivity.this, DialogActivity.class);
            short[] shape = mgStruct.readBuf(i);/*new short[c.mgStruct.readBuf(i).length-1];
        for (int v = 0; v <  c.mgStruct.readBuf(i).length - 1; v++) {
            System.out.println(v);
            shape[v] = c.mgStruct.readBuf(i)[v+1];
        }*/
            short id = shape[0];
            String str = Arrays.toString(shape);
            int idLength = String.valueOf(id).length() + 2; // [id] + "," + " "
            str = str.substring(1+idLength, str.length() - 1);
            intent.putExtra("DIALOG_TYPE", 1);
            intent.putExtra("SUBTITLE", mgStruct.argsDescriptions[id]);
            intent.putExtra("TITLE", "Edit: " + mgStruct.shapeNames[id]);
            intent.putExtra("ID", selectedInList);
            intent.putExtra("ELEMENT_ID", id);
            intent.putExtra("DATA", str);
            startActivityForResult(intent, 1);
        }
    }

    void load() {
        mgStruct.loadFile();
        selectedInList = reloadList() - 1;
        editorCanvas.invalidate();
        editorCanvas.prevSelected = selectedInList;
        selectedOption = -1;
        selectedOptionInUpperRow = -1;
        editorCanvas.isListShown = false;
        editorCanvas.isMenuExpanded = false;
    }

    String[] listData;
    int reloadList() {
        listData = new String[mgStruct.getBufSize()];
        for (int i = 0; i < mgStruct.getBufSize(); i++) {
            listData[i] = getShapeName(mgStruct.readBuf(i)[0]);
        }
        return listData.length;
    }

    public void requestPerms() {
        if (Build.VERSION.SDK_INT >= 23) {
            MainActivity.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    int calcDistance(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return calcDistance(dx, dy);
    }
    int calcDistance(int dx, int dy) {
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    String getShapeName(int id) {
        return mgStruct.shapeNames[id];
    }

    static boolean showDialog(String title, String question) {
        return true;
    }

    public static void showToast(String message) {
        Toast.makeText(contex, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        inited = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        String[] strippedStr = data.getStringExtra("RESULT").split(", ");
        short elID = data.getShortExtra("EL_ID", (short) -1);
        int idInList = data.getIntExtra("ID_IN_LIST", -1);
        short[] shape = mgStruct.readBuf(idInList);

        try {
            int it = 1;
            if (shape[0] != elID) {
                Log.e("Error", "Wrong element ID");
                return;
            }
            for (String s : strippedStr) {
                try {
                    int v = Integer.parseInt(s);
                    shape[it] = (short) v;
                    System.out.println("v:" + v);
                    it++;
                } catch (NumberFormatException ex) {
                    Log.e("Error", "Wrong number format");
                }
            }
            mgStruct.buffer[idInList] = shape;
            if (elID != 1) {
                elements.calcEndPoint();
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            Log.e("Error", "too many arguments");
        }
    }
}