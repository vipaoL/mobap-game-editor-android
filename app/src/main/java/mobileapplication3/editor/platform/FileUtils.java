/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.editor.platform;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;

import com.vipaol.mobapp.android.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import mobileapplication3.elements.Element;
import mobileapplication3.utils.Utils;

/**
 *
 * @author vipaol
 */
public class FileUtils {
    
    public static final String PREFIX = "";
    public static final char SEP = '/';
    private static final short[] TESTDATA = new short[]{0, 1, 2, 3};
    
    public static void saveShortArrayToFile(short[] arr, String path) throws IOException, SecurityException {
        File file = new File(path);

        ByteArrayOutputStream buf = new ByteArrayOutputStream(arr.length*2);
        DataOutputStream dos = new DataOutputStream(buf);
        for (int i = 0; i < arr.length; i++) {
            dos.writeShort(arr[i]);

        }

        dos.flush();
        buf.flush();
        byte[] data = buf.toByteArray();
        dos.close();
        buf.close();

        OutputStream fos = new FileOutputStream(path);
        fos.write(data);
        fos.close();
        fos.close();
    }
    
    public static Element[] readMGStruct(String path) throws FileNotFoundException {
        return readMGStruct(fileToDataInputStream(path));
    }
    
    public static Element[] readMGStruct(DataInputStream dis) {
        try {
            short fileVer = dis.readShort();
            System.out.println("mgstruct v" + fileVer);
            short elementsCount = dis.readShort();
            System.out.println("elements count: " + elementsCount);
            Element[] elements = new Element[elementsCount];
            for (int i = 0; i < elementsCount; i++) {
                elements[i] = readNextElement(dis);
                if (elements[i] == null) {
                    System.out.println("got null. stopping read");
                    return elements;
                }
            }
            return elements;
        } catch (NullPointerException ex) {
        	System.out.println("readMGStruct: caught NPE. nothing to read");
        	return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static Element readNextElement(DataInputStream is) {
        System.out.println("reading next element...");
        try {
            short id = is.readShort();
            System.out.print("id" + id + " ");
            if (id == 0) {
                System.out.println("id0 is EOF mark. stopping");
                return null;
            }
            Element element = Element.createTypedInstance(id);
            if (element == null) {
                return null;
            }
            
            int argsCount = element.getArgsCount();
            short[] args = new short[argsCount];
            for (int i = 0; i < argsCount; i++) {
                args[i] = is.readShort();
            }
            
            System.out.println(Utils.shortArrayToString(args));
            
            element.setArgs(args);
            return element;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static DataInputStream fileToDataInputStream(String path) throws FileNotFoundException {
        return new DataInputStream(new FileInputStream(path));
    }
    
    public static String[] getRoots() {
        return new String[]{
                Environment.getExternalStorageDirectory().getPath() + SEP,
                MainActivity.inst.getFilesDir().getPath() + SEP
        };
    }
    
    public static String[] list(String path) throws IOException {
        return new File(path).list();
    }
    
    public static String[] enumToArray(Enumeration en) {
        Vector tmp = new Vector(5);
        while (en.hasMoreElements()) {
            tmp.addElement(en.nextElement());
        }
        
        String[] arr = new String[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            arr[i] = (String) tmp.elementAt(i);
        }
        return arr;
    }

    public static void createFolder(String path) throws IOException {
        File f = new File(path);
        try {
            f.mkdirs();
        } catch (Exception ex) {
            openDirectory(Uri.parse(path));
            f.mkdirs();
        }
    }
    
    public static void checkFolder(String path) throws IOException {
        if (!(new File(path)).canWrite()) {
            openDirectory(Uri.parse(path));
        }

        path = path + "test.mgstruct";

        saveShortArrayToFile(TESTDATA, path);
        new File(path).delete();
    }

    public static void openDirectory(Uri uriToLoad) {
        checkAndRequestPermissions();
//        if (SDK_INT >= Build.VERSION_CODES.O) {
//            // Choose a directory using the system's file picker.
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//
//            // Optionally, specify a URI for the directory that should be opened in
//            // the system file picker when it loads.
//            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
//
//            MainActivity.inst.startActivityForResult(intent, Activity.RESULT_OK);
//            final int takeFlags = intent.getFlags()
//                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
//                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//            // Check for the freshest data.
//            MainActivity.inst.getContentResolver().takePersistableUriPermission(uriToLoad, takeFlags);
//        } else {
//            checkAndRequestPermissions();
//        }

    }

    private static void checkAndRequestPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) { //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", MainActivity.inst.getPackageName(), null);
                intent.setData(uri);
                MainActivity.inst.startActivity(intent);
            }
        } else {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                MainActivity.inst.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 123);
            }
        }
    }
    
}
