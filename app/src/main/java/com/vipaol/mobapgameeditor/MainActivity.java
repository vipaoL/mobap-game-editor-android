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
    int selectedIn3Row = -1;

    MgStruct mgStruct = new MgStruct();
    Elements elements = new Elements();
    Paint paint = new Paint();
    EditorCanvas editorCanvas;
    static Context contex;

    boolean inited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPerms();
        contex = this;
        editorCanvas = new EditorCanvas(this);
        setContentView(editorCanvas);
        editorCanvas.init();
        reset();
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
        int xUpperBtnsOffset = 0;

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

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#aaaa00"));
            paint.setTextSize(textSize);
            paint.setTextAlign(Paint.Align.CENTER);
            if (elements.wrongStartPointWarning | elements.wrongStartOfCurrPlacing) {
                g.drawText("Warn: start point should be on (x,y) 0 0", w/2, h/2 + h / 6, paint);
            }
            paint.setStyle(Paint.Style.STROKE);

            drawCar(g);

            for (int i = 0; i < mgStruct.getBufSize(); i++) { // draw all placed elements
                if (elements.isEditing & i == elements.currEditingIdInList) {
                    continue;
                }
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
            paint.setStyle(Paint.Style.STROKE);

            short x = (short) mouseX;
            short y = (short) mouseY;
            String hint = x + " " + y;
            if (elements.isBusy) { // preview when placing an element
                short id = elements.currentPlacing[0];
                int step = elements.step;
                if ((step > 1 | mgStruct.clicks[id] < 2) | elements.isEditing) {
                    elements.calcArgs(id, step, x, y);
                    if ((id == 3 | id == 5 | id == 7) & step > 1) {
                        hint = String.valueOf(elements.currentPlacing[3]);
                    }
                    drawElement(g, paint, id, elements.currentPlacing);
                }
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(lesserTextSize);
            g.drawText(hint, mouseOnScreenX + 30, mouseOnScreenY + 30, paint);
            paint.setStyle(Paint.Style.STROKE);
            try {
                drawGUI(g);
            } catch (NullPointerException ex) {

            }
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
                                handleEditorMenuSelected();
                            } else if (touchedRow == LIST_ROW) {
                                if (selectedInList == prevSelected) {
                                    elements.edit(selectedInList);
                                }
                                prevSelected = selectedInList;
                            } else if (touchedRow == EXPANDED_MENU_ROW) {
                                selectedOptionInUpperRow = (x - xUpperBtnsOffset) / btnW;
                                handleExpandedMenuSelected();
                            } else if (touchedRow == MOVE_TO_ZERO_BUTTON) {
                                selectedIn3Row = x / (btnW * 2);
                                handle3RowSelected();
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

                int dx = x2 - x1;
                int dy = y2 - y1;

                int l = data[8];

                if (l <= 0 | platfL <= 0 | spacing < 0 | data[5] <= 0) {
                    p.setColor(Color.RED);
                    g.drawLine(calcX(x1), calcY(y1), calcX(x2), calcY(y2), p);
                    return;
                }

                p.setColor(paint.getColor());
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(ths);

//                if (platfL <= 0) {
//                    platfL = 50;
//                }

                int n = (l + spacing) / (platfL+spacing);

                int spX = spacing * dx / l;
                int spY = spacing * dy / l;

                int platfDx = (dx+spX) / n;
                int platfDy = (dy+spY) / n;

                for (int i = 0; i < n; i++) {
                    g.drawLine(calcX(x1 + i * platfDx), calcY(y1 + i * platfDy), calcX(x1 + (i + 1) * platfDx - spX), calcY(y1 + (i + 1) * platfDy - spY), p);
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
            } else if (id == 6) { // sinus

            } else if (id == 7) { // speed multiplier
                int x = data[1];
                int y = data[2];
                int l = data[3];
                int h = data[4];
                int ang = data[5];
                Paint p = new Paint();
                p.setStyle(Paint.Style.FILL);
                //p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(h);
                p.setColor(Color.RED);
                int dx = (int) (l * Math.cos(Math.toRadians(ang)));
                int dy = (int) (l * Math.sin(Math.toRadians(ang)));
                g.drawLine(calcX(x), calcY(y), calcX(x + dx), calcY(y + dy), p);
                int vectorX = (int) (data[7] * Math.cos(Math.toRadians((ang + 15) + data[6])));
                int vectorY = (int) (data[7] * Math.sin(Math.toRadians((ang + 15) + data[6])));
                g.drawLine(calcX(x + dx/2), calcY(y + dy/2), calcX(x + dx/2 + vectorX), calcY(y + dy/2 + vectorY), p);
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
        String[] mainBtns = {"Name", "Load", "Save", "Line", "Circle", "More", "List"};
        String[] editorBtns = {/*"Undo/ Redo", */"Move point-1", "Move point-2", "Edit", "Delete"};
        String[] expandedMenuBtns = {"Breakable line", "Breakable circle", "Sinus", "Speed multiplier"};
        boolean isMenuExpanded = false;
        boolean isListShown = false;
        int listWidthInBtns = 2;

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

            // draw list interface when it's opened
            if (isListShown) {
                int btnH = Math.min(this.btnH, h / (listData.length + 1));
                // highlight selected placed element in list
                paint.setColor(Color.parseColor("#303080"));
                if (selectedInList > -1) {
                    g.drawRect(w - + listWidthInBtns * btnW, h - this.btnH - btnH * (selectedInList + 1), w, h - 1 - this.btnH - btnH * (selectedInList), paint);
                }

                // draw gray background for "delete" button if it's inactive
                if (selectedInList > -1) {
                    if (selectedOptionInUpperRow > -1) {
                        if (selectedOptionInUpperRow == editorBtns.length - 1 & mgStruct.buffer[selectedInList][0] == 1) {
                            paint.setColor(Color.DKGRAY);
                        }
                        if (selectedOptionInUpperRow == editorBtns.length - 3 & (mgStruct.clicks[mgStruct.buffer[selectedInList][0]] <= 1)) {
                            paint.setColor(Color.DKGRAY);
                        }
                        g.drawRect(xUpperBtnsOffset + selectedOptionInUpperRow * btnW, h - this.btnH - btnH * 3 / 2, xUpperBtnsOffset + (selectedOptionInUpperRow + 1) * btnW, h - 1 - btnH * 3 / 2, paint);
                    }
                }

                // draw upper and lower sides of editor buttons (edit/delete)
                paint.setColor(Color.WHITE);
                g.drawLine(xUpperBtnsOffset, h - this.btnH - btnH * 3 / 2 - 1, w - btnW * listWidthInBtns, h - this.btnH - btnH * 3 / 2 - 1, paint);
                g.drawLine(xUpperBtnsOffset, h - this.btnH - btnH / 2, w - btnW * listWidthInBtns, h - this.btnH - btnH / 2, paint);

                // draw left bound of the list
                g.drawLine(xUpperBtnsOffset + btnW * editorBtns.length, h - this.btnH, xUpperBtnsOffset + btnW * editorBtns.length, h - this.btnH - btnH * (listData.length), paint);

                // draw side bounds of editor buttons
                for (int i = 0; i < editorBtns.length + 1; i++) {
                    int x = xUpperBtnsOffset + i * btnW;
                    int y1 = h - this.btnH - btnH * 3 / 2;
                    int y2 = h - this.btnH - 1 - btnH / 2;
                    g.drawLine(x, y1, x, y2, paint);
                }

                // draw bounds of each element of the list
                for (int i = 0; i < listData.length; i++) {
                    g.drawLine(w - btnW * 2, h - this.btnH - btnH * (i + 1), w, h - this.btnH - btnH * (i + 1), paint);
                }

                // draw names of each list element
                for (int i = 0; i < listData.length; i++) {
                    paint.getTextBounds(listData[i], 0, listData[i].length(), bounds);
                    g.drawText(listData[i], w - btnW, h - this.btnH - btnH / 2 + bounds.height() / 2 - i * btnH, paint);
                }

                // draw editor buttons' names
                for (int i = 0; i < editorBtns.length; i++) {
                    try {
                        paint.setColor(Color.WHITE);
                        if (selectedInList > -1) {
                            if (i == editorBtns.length - 1 & (mgStruct.buffer[selectedInList][0] == 1)) {
                                paint.setColor(Color.GRAY);
                            }
                            if (i == editorBtns.length - 3 & (mgStruct.clicks[mgStruct.buffer[selectedInList][0]] <= 1)) {
                                paint.setColor(Color.GRAY);
                            }
                        }
                    } catch (NullPointerException ex) {

                    }
                    paint.getTextBounds(editorBtns[i], 0, editorBtns[i].length(), bounds);

                    //g.drawText(editorBtns[i], xUpperBtnsOffset + i * btnW + btnW / 2, h - btnH * 2 + bounds.height() / 2, paint);

                    paint.getTextBounds(editorBtns[i], 0, editorBtns[i].length(), bounds);
                    if (bounds.width() < btnW - btnW / 8) {
                        g.drawText(editorBtns[i], xUpperBtnsOffset + i * btnW + btnW / 2, h - this.btnH - btnH + bounds.height() / 2, paint);
                    } else {
                        String[] text = editorBtns[i].split(" ");
                        float prevTextSize = paint.getTextSize();
                        paint.setTextSize(prevTextSize / (text.length - 0.5f));
                        for (int j = 0; j < text.length; j++) {
                            paint.getTextBounds(text[j], 0, text[j].length(), bounds);
                            int x = xUpperBtnsOffset + i * btnW + btnW / 2;
                            int y = h - this.btnH - btnH * 3 / 2 + bounds.height()/2 + (btnH * j / text.length) + btnH / text.length / 2;
                            g.drawText(text[j], x, y, paint);
                        }
                        paint.setTextSize(prevTextSize);
                    }
                }

            }
            //////// draw expanded menu (on "more" button) if opened
            if (isMenuExpanded) {
                paint.setTextSize(lesserTextSize);
                if (selectedOptionInUpperRow > -1) {
                    paint.setColor(Color.parseColor("#303080"));
                    g.drawRect(xUpperBtnsOffset + selectedOptionInUpperRow * btnW, h - btnH * 2, xUpperBtnsOffset + (selectedOptionInUpperRow + 1) * btnW, h - 1 - btnH, paint);
                }
                paint.setColor(Color.WHITE);
                g.drawLine(xUpperBtnsOffset, h - btnH * 2, xUpperBtnsOffset + btnW * expandedMenuBtns.length, h - btnH * 2, paint);
                for (int i = 0; i < expandedMenuBtns.length + 1; i++) {
                    g.drawLine(xUpperBtnsOffset + i * btnW, h - btnH * 2, xUpperBtnsOffset + i * btnW, h - 1 - btnH, paint);
                }
                for (int i = 0; i < expandedMenuBtns.length; i++) {
                    paint.getTextBounds(expandedMenuBtns[i], 0, expandedMenuBtns[i].length(), bounds);
                    if (bounds.width() < btnW - btnW / 8) {
                        g.drawText(expandedMenuBtns[i], xUpperBtnsOffset + i * btnW + btnW / 2, h - btnH * 3 / 2 + bounds.height() / 2, paint);
                        //Log.i("<", "<");
                    } else {
                        String[] text = expandedMenuBtns[i].split(" ");
                        //Log.i("l", String.valueOf(text.length));
                        for (int j = 0; j < text.length; j++) {
                            paint.getTextBounds(text[j], 0, text[j].length(), bounds);
                            int x = xUpperBtnsOffset + i * btnW + btnW / 2;
                            int y = h - btnH *2 + bounds.height()/2 + (btnH * j / text.length) + btnH / text.length / 2;
                            g.drawText(text[j], x, y, paint);
                        }
                    }
                }
            }
            if (elements.wrongStartPointWarning) {
                paint.setColor(Color.parseColor("#308030"));
                if (selectedIn3Row > -1) {
                    g.drawRect(selectedIn3Row * btnW * 2, h - btnH * 4, (selectedIn3Row + 1) * btnW * 2, h - btnH * 3, paint);
                }
                paint.setColor(Color.WHITE);
                g.drawLine(0, h - btnH * 4, btnW * 2, h - btnH * 4, paint);
                g.drawLine(btnW * 2, h - btnH * 4, btnW * 2, h - btnH * 3, paint);
                g.drawLine(0, h - btnH * 3, btnW * 2, h - btnH * 3, paint);
                String str = "Move all to 0 0";
                paint.getTextBounds(str, 0, str.length(), bounds);
                g.drawText(str, btnW, h - btnH * 7 / 2 + bounds.height() / 2, paint);
            }
        }

        int MAIN_ROW = 0;
        int UPPER_ROW = 1;
        int LIST_ROW = 2;
        int EXPANDED_MENU_ROW = 3;
        int MOVE_TO_ZERO_BUTTON = 4;
        int prevSelected = selectedInList;
        int input(int x, int y) { // sorting touch events
            if (y >= h - btnH) {
                selectedOption = x * mainBtns.length / w;
                return MAIN_ROW;
            } else if (isListShown) {
                int btnH = Math.min(this.btnH, h / (listData.length + 1));
                if (y >= h - this.btnH - btnH * 3 / 2 & y < h - this.btnH - btnH / 2 & x < xUpperBtnsOffset + btnW * editorBtns.length & (x - xUpperBtnsOffset) >= 0) {
                    selectedOptionInUpperRow = (x - xUpperBtnsOffset) / btnW;
                    return UPPER_ROW;
                } else if (y < h - this.btnH & x > xUpperBtnsOffset + btnW * editorBtns.length) {
                    selectedInList = (h - y - this.btnH) / btnH;
                    if (selectedInList >= listData.length) {
                        selectedInList = listData.length - 1;
                        return -1;
                    }
                    return LIST_ROW;
                }
            } else if (isMenuExpanded) {
                if (y >= h - btnH * 2 & y < h - btnH & x < xUpperBtnsOffset + btnW * expandedMenuBtns.length & (x - xUpperBtnsOffset) >= 0) {
                    selectedOptionInUpperRow = (x - xUpperBtnsOffset) / btnW;
                    return EXPANDED_MENU_ROW;
                }
            }
            if (elements.wrongStartPointWarning) {
                if (y >= h - btnH * 4 & y <= h - btnH * 3 & x <= btnW * 2) {
                    selectedIn3Row = x / (btnW * 2);
                    return MOVE_TO_ZERO_BUTTON;
                }
            }
            return -1;
        }

        int inToolbarActionBtnsN = 3;
        int inToolbarPlaceBtnsN = 2;
        void handleMainRowSelected() {
            elements.cancel();
            if (selectedOption == mainBtns.length - 2) {
                showHideExpandedMenu();
            } else if (selectedOption == mainBtns.length - 1) {
                showHideList();
            } else {
                isMenuExpanded = false;
                isListShown = false;
                if (selectedOption == 0) {
                    changeName();
                    requestPerms();
                } else if (selectedOption == 1) {
                    load();
                    requestPerms();
                    selectedOption = -1;
                    init();
                } else if (selectedOption == 2) {
                    mgStruct.saveToFile();
                    requestPerms();
                    selectedOption = -1;
                } else if (selectedOption >= inToolbarActionBtnsN & selectedOption < inToolbarActionBtnsN + inToolbarPlaceBtnsN) {
                    elements.place(2 + selectedOption - inToolbarActionBtnsN, mouseX, mouseY);
                }
            }
        }

        void handleEditorMenuSelected() {
            elements.cancel();
            if (selectedOptionInUpperRow == editorBtns.length - 5) {
                mgStruct.undo();
                reloadList();
                selectedOptionInUpperRow = -1;
                Log.d("UNDO", "UNDO");
            } else if (selectedOptionInUpperRow == editorBtns.length - 4) {
                elements.edit(selectedInList, 1);
            } else if (selectedOptionInUpperRow == editorBtns.length - 3) {
                elements.edit(selectedInList, 2);
            } else if (selectedOptionInUpperRow == editorBtns.length - 2) {
                elements.edit(selectedInList);
            } else if (selectedOptionInUpperRow == editorBtns.length - 1) {
                selectedOptionInUpperRow = -1;
                elements.delete(selectedInList);
            }
        }
        void handleExpandedMenuSelected() {
            elements.cancel();
            elements.place(4 + selectedOptionInUpperRow, mouseX, mouseY);
        }
        void handle3RowSelected() {
            elements.moveAllToStartPoint();
        }

        void showHideList() {
            isListShown = !isListShown;
            isMenuExpanded = false;
            if (!isListShown) {
                selectedOption = -1;
            } else {
                xUpperBtnsOffset = w - btnW * (editorBtns.length + 2);
                reloadList();
            }
        }

        void showHideExpandedMenu() {
            isListShown = false;
            isMenuExpanded = !isMenuExpanded;
            if (!isMenuExpanded) {
                selectedOption = -1;
            } else {
                xUpperBtnsOffset = w - btnW * Math.max(2, expandedMenuBtns.length);
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
        private boolean isEditing = false;
        private int currEditingIdInList = -1;
        private int step = 0;
        short x0 = 0;
        short y0 = 0;

        public boolean place(int id) {
            if (id == 0) {
                currPlacingID = (short) id;
                isBusy = false;
                isEditing = false;
                step = 0;
            } else if (!isBusy) {
                mgStruct.updateHistory();
                isBusy = true;
                currPlacingID = (short) id;
                step = 1;
                currentPlacing = new short[mgStruct.args[currPlacingID] + 1];
                currentPlacing[0] = currPlacingID;
                return true;
            } else {
                int currID = currPlacingID;
                cancel();
                if (id != currID) {
                    place(id);
                } else {
                    selectedOption = -1;
                    selectedOptionInUpperRow = -1;
                }
            }
            return false;
        }

        public boolean place(int id, int x, int y) {
            if (id == 0) {
                currPlacingID = (short) id;
                isBusy = false;
                step = 0;
            } else if (!isBusy) {
                mgStruct.updateHistory();
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
            if (suppressFirstClick) {
                suppressFirstClick = false;
                return false;
            }
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
                    if (!isEditing) {
                        mgStruct.saveToBuffer(currentPlacing);
                        selectedInList = mgStruct.length - 1;
                    } else {
                        if (currEditingIdInList > -1) {
                            mgStruct.buffer[currEditingIdInList] = currentPlacing;
                        }
                    }
                    reloadList();
                    if (currPlacingID != 1)
                        calcEndPoint();
                    currPlacingID = 0;
                    isEditing = false;
                    wrongStartOfCurrPlacing = false;
                    checkStartPoint();
                    return true;
                }
                calcArgs(currPlacingID, step, xShort, yShort);
                if (isEditing) {
                    step = 3000;
                    clicked(x, y);
                }
                step++;
            }
            return false;
        }

        boolean wrongStartPointWarning = false;
        boolean wrongStartOfCurrPlacing = false;
        void calcArgs(short id, int step, short x, short y) {
            /*if (mouseOnCanvX < 0 | mouseOnCanvX > w | mouseOnCanvY < 0 | mouseOnCanvY > h) {
                cancel();
                return;
            }*/
            if (id == 1) {
                currentPlacing[1] = x;
                currentPlacing[2] = y;
            } else
            if (id == 2) {
                if (step == 1) {
                    currentPlacing[1] = x;
                    currentPlacing[2] = y;
                } else if (step == 2) {
                    currentPlacing[3] = x;
                    currentPlacing[4] = y;
                }
            } else
            if (id == 3) {
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
            if (id == 4) {
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
                    if (l <= 0) {
                        l = 1;
                    }
                    int optimalPlatfL = 260;
                    int platfL = optimalPlatfL;
                    if (platfL > l) {
                        platfL = l;
                    } else {
                        int platfL1 = platfL;
                        while ((l + spacing) % (platfL + spacing) != 0 & platfL < l & (l + spacing) % (platfL1 + spacing) != 0) {
                            platfL++;
                            if (platfL1 > 5)
                                platfL1--;
                        }
                        if ((l + spacing) % (platfL + spacing) == 0) {
                            platfL1 = platfL;
                        }
                        platfL = platfL1;
                    }
                    if (platfL <= 0)
                        platfL = l;
                    currentPlacing[6] = (short) platfL;
                    currentPlacing[7] = (short) spacing;
                    currentPlacing[8] = (short) l;
                    currentPlacing[9] = (short) Math.toDegrees(Math.atan2(dy, dx));
                }
            } else
            if (id == 5) {
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
            } else
            if (id == 6) {
                cancel();
            } else
            if (id == 7) {
                if (step == 1) {
                    currentPlacing[1] = x;
                    currentPlacing[2] = y;
                }
                if (step == 2) {
                    x0 = currentPlacing[1];
                    y0 = currentPlacing[2];
                    short dx = (short) (x - x0);
                    short dy = (short) (y - y0);
                    short l = (short) calcDistance(dx, dy);
                    short h = 1;
                    short ang = (short) Math.toDegrees(Math.atan2(dy, dx));
                    currentPlacing[3] = l;
                    currentPlacing[4] = h;
                    currentPlacing[5] = ang;
                }
                if (step == 3) {
                    int x1 = currentPlacing[1];
                    int y1 = currentPlacing[2];
                    int x2 = currentPlacing[3];
                    int y2 = currentPlacing[4];
                    short centerX = (short) ((x2 - x1) / 2 + x1);
                    short centerY = (short) ((y2 - y1) / 2 + y1);
                    short dx = (short) (x - centerX);
                    short dy = (short) (y - centerY);
                    currentPlacing[6] = (short) 0;
                    currentPlacing[7] = (short) calcDistance(dx, dy);
                    currentPlacing[8] = (short) 30;
                }
            }

            short currCheckingX = currentPlacing[1];
            short currCheckingY = currentPlacing[2];
            if (id == 2 | id == 4) {
                short x2nd = currentPlacing[3];
                short y2nd = currentPlacing[4];
                if (compareAsStarts(currCheckingX, currCheckingY, x2nd, y2nd)) {
                    currCheckingX = x2nd;
                    currCheckingY = y2nd;
                }
            } else if (id == 3 | id == 5) {
                int r = currentPlacing[3];
                currCheckingX -= r;
            }
            wrongStartOfCurrPlacing = (currCheckingX != 0 | currCheckingY != 0) & mgStruct.length < 2;
            if (currCheckingX < 0) {
                wrongStartOfCurrPlacing = true;
            }
        }

        public void calcEndPoint() {
            short x = 0;
            short y = 0;
            for (int i = 1; i < mgStruct.length; i++) {
                short currCheckingX = mgStruct.buffer[i][1];
                short currCheckingY = mgStruct.buffer[i][2];
                int id = mgStruct.buffer[i][0];
                if (id == 2 | id == 4 | id == 7) {
                    short x2nd = mgStruct.buffer[i][3];
                    short y2nd = mgStruct.buffer[i][4];

                    if (id == 7) {
                        short ang = mgStruct.buffer[i][5];
                        short l = mgStruct.buffer[i][3];
                        short dx = (short) (l * Math.cos(Math.toRadians(ang)));
                        short dy = (short) (l * Math.sin(Math.toRadians(ang)));
                        x2nd = (short) (currCheckingX + dx);
                        y2nd = (short) (currCheckingY + dy);
                    }

                    if (compareAsEnds(currCheckingX, currCheckingY, x2nd, y2nd)) {
                        currCheckingX = x2nd;
                        currCheckingY = y2nd;
                    }
                } else if (id == 3 | id == 5) {
                    int r = mgStruct.buffer[i][3];
                    currCheckingX += r;
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
            mgStruct.updateHistory();

            mgStruct.length--;
            for (int j = i; j < mgStruct.length; j++) {
                mgStruct.buffer[j] = mgStruct.buffer[j + 1];
            }
            selectedInList = mgStruct.length - 1;
            calcEndPoint();
            reloadList();
        }
        boolean suppressFirstClick = false;
        void edit(int idInList, int startWithStep) {
            mgStruct.updateHistory();
            currEditingIdInList = idInList;
            if (isBusy) {
                cancel();
            }
            int id = mgStruct.buffer[idInList][0];
            if (mgStruct.clicks[id] < startWithStep) {
                selectedOptionInUpperRow = -1;
                return;
            }
            isEditing = true;

            if (id == 0) {
                currPlacingID = (short) id;
                isBusy = false;
                step = 0;
                isEditing = false;
            } else if (!isBusy) {
                isBusy = true;
                currentPlacing = mgStruct.buffer[idInList];
                currPlacingID = (short) id;
                step = startWithStep;
                //return true;
            } else {
                int currID = currPlacingID;
                cancel();
                if (id != currID) {
                    edit(idInList, startWithStep);
                } else {
                    selectedOption = -1;
                    selectedOptionInUpperRow = -1;
                }
            }
            //suppressFirstClick = true;
            //return false;
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

        void moveAllToStartPoint() {
            mgStruct.updateHistory();
            short[] dxdy = checkStartPoint();
            int dx = -dxdy[0];
            int dy = -dxdy[1];

            for (int i = 0; i < mgStruct.length; i++) {
                int id = mgStruct.buffer[i][0];
                mgStruct.buffer[i][1] += dx;
                mgStruct.buffer[i][2] += dy;
                if (id == 2 | id == 4) {
                    mgStruct.buffer[i][3] += dx;
                    mgStruct.buffer[i][4] += dy;
                }
            }
            selectedIn3Row = -1;
            wrongStartPointWarning = false;
        }

        public short[] checkStartPoint() {
            short x = 0;
            short y = 0;
            boolean first = true;
            for (int i = 1; i < mgStruct.length; i++) {
                short currCheckingX = mgStruct.buffer[i][1];
                short currCheckingY = mgStruct.buffer[i][2];
                if (first) {
                    x = currCheckingX;
                    y = currCheckingY;
                    first = false;
                }
                int id = mgStruct.buffer[i][0];
                if (id == 2 | id == 4) {
                    short x2nd = mgStruct.buffer[i][3];
                    short y2nd = mgStruct.buffer[i][4];
                    if (compareAsStarts(currCheckingX, currCheckingY, x2nd, y2nd)) {
                        currCheckingX = x2nd;
                        currCheckingY = y2nd;
                    }
                } else if (id == 3 | id == 5) {
                    int r = mgStruct.buffer[i][3];
                    currCheckingX -= r;
                }
                if (compareAsStarts(x, y, currCheckingX, currCheckingY)) {
                    x = currCheckingX;
                    y = currCheckingY;
                }
            }
            Log.i(String.valueOf(x), String.valueOf(y));
            wrongStartPointWarning = (x != 0) | (y != 0);
            return new short[]{x, y};
        }
        private boolean compareAsStarts(short x, short y, short currCheckingX, short currCheckingY) {
            if (currCheckingX <= x) {
                if (currCheckingX < x | (currCheckingY < y)) {
                    return true;
                }
            }
            return false;
        }
    }

    void reset() {
        selectedInList = reloadList() - 1;
        editorCanvas.invalidate();
        editorCanvas.prevSelected = selectedInList;
        selectedOption = -1;
        selectedOptionInUpperRow = -1;
        editorCanvas.isListShown = false;
        editorCanvas.isMenuExpanded = false;
        elements.wrongStartPointWarning = false;
        elements.wrongStartOfCurrPlacing = false;
        elements.place(1, 0, 0);
    }

    void changeName() {
        Intent intent = new Intent(MainActivity.this, DialogActivity.class);
        intent.putExtra("DIALOG_TYPE", 2);
        intent.putExtra("TITLE", "Change name");
        intent.putExtra("SUBTITLE", mgStruct.path);
        intent.putExtra("name", mgStruct.name);
        startActivityForResult(intent, 2);
    }

    void load() {
        reset();
        requestPerms();
        mgStruct.loadFile();
        selectedInList = reloadList() - 1;
        mgStruct.updateHistory();
    }

    String[] listData;
    int reloadList() {
        listData = new String[mgStruct.getBufSize()];
        for (int i = 0; i < mgStruct.getBufSize(); i++) {
            listData[i] = getShapeName(mgStruct.readBuf(i)[0]);
        }
        if (selectedInList >= listData.length) {
            selectedInList = listData.length - 1;
        }
        elements.checkStartPoint();
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
        if (requestCode == 1) {
            selectedOptionInUpperRow = -1;
        } else if (requestCode == 2) {
            selectedOption = -1;
        }
        editorCanvas.invalidate();
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        } else {
            mgStruct.updateHistory();
        }
        if (requestCode == 1) {
            String[] strippedStr = data.getStringExtra("RESULT").split(", ");
            short elID = data.getShortExtra("EL_ID", (short) -1);
            int idInList = data.getIntExtra("ID_IN_LIST", -1);
            short[] shape = mgStruct.readBuf(idInList);

            try {
                int it = 1;
                if (shape[0] != elID) {
                    Log.e("Error", "Wrong element ID");
                    editorCanvas.invalidate();
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
        } else {
            mgStruct.name = data.getStringExtra("name");
        }
        elements.checkStartPoint();
        editorCanvas.invalidate();
    }
}