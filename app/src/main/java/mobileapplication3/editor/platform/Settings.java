/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.editor.platform;

import android.content.Context;
import android.content.SharedPreferences;

import com.vipaol.mobapp.android.MainActivity;

import java.util.Map;

import mobileapplication3.editor.ui.platform.Platform;
import mobileapplication3.utils.Utils;

/**
 *
 * @author vipaol
 */
public class Settings {
    public static final String
            TRUE = "1",
            FALSE = "0",
            UNDEF = "";

    private SharedPreferences prefs;
    
    public Settings(String[] keys, String storeName) {
        prefs = MainActivity.inst.getSharedPreferences(storeName, Context.MODE_PRIVATE);
    }
    
    public void resetSettings() {
        prefs.edit().clear().apply();
    }

    public boolean set(String key, String value) {
        prefs.edit().putString(key, value).apply();
        return true;
    }
    
    public boolean set(String key, boolean value) {
        return set(key, value ? TRUE : FALSE);
    }
    
    public String getStr(String key) {
        return prefs.getString(key, UNDEF);
    }

    public String getStr(String key, String defValue) {
        return prefs.getString(key, defValue);
    }
    
    public boolean toggleBool(String key) {
        boolean newValue = !getBool(key);
        set(key, newValue);
        return newValue;
    }
    
    public boolean getBool(String key) {
    	return TRUE.equals(getStr(key));
    }
    
    public boolean getBool(String key, boolean defaultValue) {
        return TRUE.equals(getStr(key, toStr(defaultValue)));
    }
    
    public int getInt(String key, int defaultValue) {
    	String value = getStr(key, String.valueOf(defaultValue));
    	if (UNDEF.equals(value)) {
    		return 0;
    	}
    	return Integer.parseInt(value);
    }
    
    private String toStr(boolean b) {
        return b ? TRUE : FALSE;
    }
}
