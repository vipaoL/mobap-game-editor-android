/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.editor.platform;

import java.util.Enumeration;
import java.util.Vector;

/**
 *
 * @author vipaol
 */
public class Paths {
    private static final String SEP = "/";
    private static final String[] FOLDERS_ON_EACH_DRIVE = {"", "other" + SEP};
    public static final String GAME_FOLDER_NAME = "MGStructs";
    
    public static String[] getAllMGStructsFolders() {
        String[] roots = FileUtils.getRoots();
        String[] paths = new String[roots.length * FOLDERS_ON_EACH_DRIVE.length];
        
        for (int i = 0; i < roots.length; i++) {
            for (int j = 0; j < FOLDERS_ON_EACH_DRIVE.length; j++) {
                paths[i*FOLDERS_ON_EACH_DRIVE.length + j] = roots[i] + FOLDERS_ON_EACH_DRIVE[j] + GAME_FOLDER_NAME + SEP;
            }
        }
        
        return paths;
    }
}
