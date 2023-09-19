package com.vipaol.mobapgameeditor;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author vipaol
 */
public class MgStruct {
    int[] clicks = {0, /*1*/1, /*2*/2, /*3*/2, /*4*/2, /*5*/2, /*6*/4, /*7*/3};
    int[] args = {0, /*1*/2, /*2*/4, /*3*/7, /*4*/9, /*5*/10, /*6*/5, /*7*/8};
    String[] shapeNames = {"EOF", /*1*/"End point", /*2*/"Line", /*3*/"Circle", /*4*/"Breakable line", /*5*/"Breakable circle", /*6*/"Sinus", /*7*/"Speed multiplier"};
    String[] argsDescriptions = {"0",
            /*1*/"x, y",
            /*2*/"x1, y1, x2, y2",
            /*3*/"x, y, radius, arc-angle, arc-offset, x-coefficient, y-coefficient",
            /*4*/"x1, y1, x2, y2, thickness, platform length, spacing, length, angle",
            /*5*/"x, y, radius, arc-angle, arc-offset, x-coefficient, y-coefficient, thickness, platform length, spacing",
            /*6*/"x1, y1, l, amplitude, periodOffset, period",
            /*7*/"x, y, l, h, angle, direction offset, speed multiplier, duration"};

    short supportedFileVer = 1;
    short fileVersion = supportedFileVer;
    String name = "test" + ".mgstruct";
    String parentPath = Environment.getExternalStorageDirectory().getPath() + "/MGStructs/";
    String path = parentPath + name;
    short[][] buffer = new short[256][];
    int bufSizeInShort = 0;
    boolean changed = true;
    int length = 0;
    short[][] history = new short[buffer.length][];
    int oldLength = length;

    public boolean loadFile() {
        path = parentPath + name;
        length = 0;
        try {
            File f = new File(path);
            if (f.exists()) {
                InputStream is = new FileInputStream(f);
                DataInputStream dis = new DataInputStream(is);

                fileVersion = dis.readShort();
                boolean cancelled = false;

                if (fileVersion != supportedFileVer) {
                    if (fileVersion == 0) {
                        cancelled = !MainActivity.showDialog("Older file version", "Older file ver: " + fileVersion + ". Actual is: " + supportedFileVer + ". This file can be converted and will be signed with version nubmer " + supportedFileVer + " on the next save. Open?");
                    } else {
                        cancelled = !MainActivity.showDialog("Unsupported file version", "Unsupported file ver: " + fileVersion + ". Supported is: " + supportedFileVer + ". Try to open anyway?");
                    }
                }
                if (!cancelled) {
                    if (fileVersion == 0) {
                        length = 16;
                    } else {
                        length = dis.readShort();
                    }

                    buffer = new short[Math.max(length * 16, 256)][];
                    length = 0;

                    while (true) {
                        short id = dis.readShort();
                        Log.d("read: id=", String.valueOf(id));
                        if (id == 0) {
                            break;
                        } else {
                            int argsN = args[id];
                            short[] data = new short[argsN + 1];
                            /*if (id == 4 | id == 5) {
                                argsN -= 2;
                            }*/
                            data[0] = id;
                            for (int i = 1; i < argsN + 1; i++) {
                                data[i] = dis.readShort();
                            }
                            saveToBuffer(data);
                            for (int i : data) {
                                System.out.print(i + " ");
                            }
                            System.out.println(".");
                        }
                    }
                }
                dis.close();
                is.close();
                return true;
            } else {
                Log.d("not exists", "");
                short[] data = {1, 0, 0};
                saveToBuffer(data);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean saveToFile() {
        path = parentPath + name;
        try {
            File f = new File(parentPath);
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    MainActivity.showToast("Unable to create directory: " + f.getAbsolutePath());
                }
            }
            f = new File(path);
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    MainActivity.showToast("Unable to create file: " + f.getAbsolutePath());
                }
            } else {
                if (!MainActivity.showDialog("File already exists", "Replace " + name + "?")) {
                    return false;
                }
            }
            if (f.exists()) {
                OutputStream os = new FileOutputStream(f);
                DataOutputStream dis = new DataOutputStream(os);
                dis.writeShort(supportedFileVer);
                dis.writeShort(length);
                for (int j = 0; j < length; j++) {
                    short[] data = buffer[j];
                    for (int i : data) {
                        dis.writeShort(i);
                    }
                }
                dis.writeShort(0);
                dis.close();
                os.close();
                return true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    void saveToBuffer(short[] data) {
        buffer[length] = data;
        length++;
        bufSizeInShort += data.length;
        changed = true;
    }
    void updateHistory() {
        oldLength = length;
        history = buffer.clone();
        Log.d("HISTORY", "UPDATED");
    }
    void undo() {
        int tmp = length;
        short[][] tmpArr = buffer.clone();
        buffer = history.clone();
        length = oldLength;
        history = tmpArr.clone();
        oldLength = tmp;
        Log.i("Length =", String.valueOf(length));
    }
    void redo() {
        //TODO
    }
    boolean isUndoAvailable() {
        return true;
    }
    boolean isRedoAvailable() {
        return false; //TODO
    }

    short[] readBuf(int i) {
        return buffer[i];
    }
    int getBufSize() {
        return length;
    }
}
