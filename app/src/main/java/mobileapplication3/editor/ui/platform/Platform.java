package mobileapplication3.editor.ui.platform;

import static mobileapplication3.editor.platform.FileUtils.SEP;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import com.vipaol.mobapp.android.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import mobileapplication3.editor.StructureBuilder;
import mobileapplication3.editor.platform.FileUtils;
import mobileapplication3.elements.Element;

public class Platform {
	private static final String AUTO_SAVE_FILE_NAME = "autosave.mgstruct";

	public static void showError(String message) {
		MainActivity.inst.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.inst, "Error: " + message, Toast.LENGTH_LONG).show();
			}
		});
    }

	public static void showError(Exception ex) {
		showError(ex.toString());
		ex.printStackTrace();
	}

	public static void vibrate(int ms) {
		Vibrator v = (Vibrator) MainActivity.inst.getSystemService(Context.VIBRATOR_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
		} else {
			//deprecated in API 26
			v.vibrate(ms);
		}
	}

	public static void autoSaveWrite(StructureBuilder data) throws IOException {
		FileUtils.saveShortArrayToFile(data.asShortArray(), getAutoSavePath());
	}

	public static Element[] autoSaveRead() throws FileNotFoundException {
		return FileUtils.readMGStruct(getAutoSavePath());
	}

	public static void deleteAutoSave() {
		new File(getAutoSavePath()).delete();
	}

	private static String getAutoSavePath() {
		return MainActivity.inst.getFilesDir().getPath() + SEP + AUTO_SAVE_FILE_NAME;
	}

}
