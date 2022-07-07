package com.vipaol.mobapgameeditor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class DialogActivity extends AppCompatActivity {

    EditText editText;
    int id = -1;
    short structID = -1;
    int windowType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        windowType = getIntent().getIntExtra("DIALOG_TYPE", 0);
        setTitle(getIntent().getStringExtra("TITLE"));
        TextView textView = findViewById(R.id.textview);
        textView.setText(getIntent().getStringExtra("SUBTITLE"));
        editText = findViewById(R.id.edit_text);
        if (windowType == 1) {
            editText.setText(getIntent().getStringExtra("DATA"));
            id = getIntent().getIntExtra("ID", -1);
            structID = getIntent().getShortExtra("ELEMENT_ID", (short) -1);
        }
    }/*

    static boolean dialAns = false;
    static boolean isDialAlreadyAnswered = false;
    public static boolean showDialog(String title, String question) {
        dialAns = false;
        isDialAlreadyAnswered = false;
        AlertDialog dialog = new AlertDialog.Builder(inst) {}
                .setTitle(title)
                .setMessage(question)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialAns = true;
                        isDialAlreadyAnswered = true;
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        isDialAlreadyAnswered = true;
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        isDialAlreadyAnswered = true;
                    }
                }).show();
        return dialAns;
    }*/
    public void ok_clicked(View view) {
        Intent returnIntent = new Intent();
        if (windowType == 1) {
            returnIntent.putExtra("RESULT", editText.getText().toString());
            /*Log.i("result", editText.getText().toString());*/
            returnIntent.putExtra("ID_IN_LIST", id);
            returnIntent.putExtra("EL_ID", structID);
        }
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
    public void cancel_clicked(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}