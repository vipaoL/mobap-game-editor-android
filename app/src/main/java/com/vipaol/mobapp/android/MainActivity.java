package com.vipaol.mobapp.android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;

import mobileapplication3.editor.MainScreenUI;
import mobileapplication3.editor.setup.SetupWizard;
import mobileapplication3.editor.ui.UISettings;
import mobileapplication3.editor.ui.platform.Platform;
import mobileapplication3.editor.ui.platform.RootContainer;
import mobileapplication3.utils.EditorSettings;

public class MainActivity extends Activity {
    private RootContainer currentRoot;
    public static MainActivity inst;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inst = this;
        try {
            final UISettings uiSettings = new UISettings() {
                public boolean getKeyRepeatedInListsEnabled() {
                    return EditorSettings.getKeyRepeatedInListsEnabled(false);
                }

                public boolean getAnimsEnabled() {
                    return EditorSettings.getAnimsEnabled(true);
                }

                public void onChange() {
                    try {
                        currentRoot.init();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            if (EditorSettings.isSetupWizardCompleted()) {
                setRootComponent(new RootContainer(inst, new MainScreenUI(), uiSettings));
            } else {
                setRootComponent(new RootContainer(this, new SetupWizard(new SetupWizard.FinishSetup() {
                    public void onFinish() {
                        inst.setRootComponent(new RootContainer(inst, new MainScreenUI(), uiSettings));
                    }
                }), uiSettings));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            Platform.showError(ex);
        }
    }

    public void setRootComponent(RootContainer currentRoot) {
        this.currentRoot = currentRoot;
        MainActivity.inst.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setContentView(currentRoot);
            }
        });
    }
}
