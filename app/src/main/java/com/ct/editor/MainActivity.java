package com.ct.editor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ct.editor.manager.CTEditorManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    CTEditorManager mHelper;
    SharedPreferences mPrefs;

    ScrollView mLayoutMain;
    EditText mEditText;
    TextView mTextWordCount;
    Button mUndo, mRedo, mSave;

    final String SAVE_NOTE_FILE_NAME = "ctnote.txt";
    final String SAVE_NOTE_PREFIX = "CT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPrefs = getSharedPreferences("CTEditor", MODE_PRIVATE);

        mLayoutMain = findViewById(R.id.layoutMain);
        mEditText = findViewById(R.id.text);
        mTextWordCount = findViewById(R.id.textWordCount);
        mUndo = findViewById(R.id.btnUndo);
        mRedo = findViewById(R.id.btnRedo);
        mSave = findViewById(R.id.btnSave);

        mHelper = new CTEditorManager(mEditText);

        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHelper.getCanUndo()) {
                    mHelper.undo();
                    updateWordCount(mEditText);
                } else {
                    mUndo.setEnabled(false);
                }
            }
        });

        mRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHelper.getCanRedo()) {
                    mHelper.redo();
                    updateWordCount(mEditText);
                } else {
                    mRedo.setEnabled(false);
                }
            }
        });

        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote(SAVE_NOTE_FILE_NAME);
            }
        });

        mLayoutMain.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                mLayoutMain.getWindowVisibleDisplayFrame(rect);
                float screenHeight = mLayoutMain.getRootView().getHeight();
                float keyboardHeight = screenHeight - rect.bottom;
                if (keyboardHeight > screenHeight * 0.15) {
                    // keyboard visible
                } else {
                    // keyboard hidden
                    updateWordCount(mEditText);
                }
            }
        });


        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d("focus", "hasFocus: " + hasFocus);

                if(!hasFocus){
                    EditText et = (EditText)v;
                    updateWordCount(et);
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateButtons();
            }
        });

        String savedText = openNote(SAVE_NOTE_FILE_NAME);
        if (savedText != null && !savedText.isEmpty()) {
            mEditText.setText(savedText);
        }

    }

    private void validateButtons() {
        if (mHelper.getCanRedo()) {
            mRedo.setEnabled(true);
        } else {
            mRedo.setEnabled(false);
        }
        if (mHelper.getCanUndo()) {
            mUndo.setEnabled(true);
        } else {
            mUndo.setEnabled(false);
        }
        if (mEditText.getText().length() > 0) {
            mSave.setEnabled(true);
        } else {
            mSave.setEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            mHelper.saveEditorHistoryState(mPrefs.edit(), SAVE_NOTE_PREFIX);
        } catch (Exception e) {}

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try {
            mHelper.restoreEditorHistoryState(mPrefs, SAVE_NOTE_PREFIX);
            updateWordCount(mEditText);
        } catch (Exception e) {}
    }

    private void updateWordCount(EditText et) {
        String text = et.getText().length() + "";
        mTextWordCount.setText(text);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    public void saveNote(String fileName) {
        try {
            OutputStreamWriter out =
                    new OutputStreamWriter(openFileOutput(fileName, 0));
            out.write(mEditText.getText().toString());
            out.close();
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public String openNote(String fileName) {
        String content = "";
        if (isFileExist(fileName)) {
            try {
                InputStream in = openFileInput(fileName);
                if ( in != null) {
                    InputStreamReader tmp = new InputStreamReader( in );
                    BufferedReader reader = new BufferedReader(tmp);
                    String str;
                    StringBuilder buf = new StringBuilder();
                    while ((str = reader.readLine()) != null) {
                        buf.append(str + "\n");
                    } in .close();
                    content = buf.toString();
                }
                File file = getBaseContext().getFileStreamPath(fileName);
                file.delete();
            } catch (java.io.FileNotFoundException e) {} catch (Throwable t) {
                Toast.makeText(this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
            }
        }
        return content;
    }

    public boolean isFileExist(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }
}
