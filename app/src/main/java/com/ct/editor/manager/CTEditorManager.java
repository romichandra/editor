package com.ct.editor.manager;

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.widget.EditText;

/**
 * {@code This is the Editor Manager, responsible for holding the {@link CTEditHistory}
 *          and managing the undo/redo states for the actions {@link ActionType}}
 *
 * @author Name:    Romi Chandra,
 * Email:   romi.d.nerd@gmail.com.
 * @version 1.0
 * @since 30, May, 2020
 */
public class CTEditorManager {
    private boolean isHistoryAvailable = false;
    private CTEditHistory mCTEditHistory;
    private EditTextChangeListener mChangeListener;
    private EditText mEditText;


    public CTEditorManager(EditText editText) {
        mEditText = editText;
        mCTEditHistory = new CTEditHistory();
        mChangeListener = new EditTextChangeListener();
        mEditText.addTextChangedListener(mChangeListener);
    }

    /**
     * Disconnects the TextWatcher from the edittext
     */
    public void disconnect() {
        mEditText.removeTextChangedListener(mChangeListener);
    }

    /**
     * Sets the Maximum size for Edit History
     */
    public void setMaxHistorySize(int maxHistorySize) {
        mCTEditHistory.setMaxHistorySize(maxHistorySize);
    }

    /**
     * Clears Edit History
     */
    public void clearHistory() {
        mCTEditHistory.clear();
    }

    /**
     * @return true if undo option is present in EditHistory
     */
    public boolean getCanUndo() {
        return (mCTEditHistory.mPosition > 0);
    }

    /**
     * Performs the undo operation on the Edittext, updates the text too.
     */
    public void undo() {
        CTEditorHistoryItem edit = mCTEditHistory.getPrevious();
        if (edit == null) {
            return;
        }

        Editable text = mEditText.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmAfter != null ? edit.mmAfter.length() : 0);

        isHistoryAvailable = true;
        text.replace(start, end, edit.mmBefore);
        isHistoryAvailable = false;

        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmBefore == null ? start : (start + edit.mmBefore.length()));
    }

    /**
     * @return true if redo option is present in EditHistory
     */
    public boolean getCanRedo() {
        return (mCTEditHistory.mPosition < mCTEditHistory.mHistory.size());
    }

    /**
     * Performs the redo operation on the Edittext, updates the text too.
     */
    public void redo() {
        CTEditorHistoryItem edit = mCTEditHistory.getNext();
        if (edit == null) {
            return;
        }

        Editable text = mEditText.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmBefore != null ? edit.mmBefore.length() : 0);

        isHistoryAvailable = true;
        text.replace(start, end, edit.mmAfter);
        isHistoryAvailable = false;

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmAfter == null ? start
                : (start + edit.mmAfter.length()));
    }

    /**
     * Saves EditHistory in the Shared Preferences of the application
     *
     * @param editor the Shared Preferences Editor of the application
     * @param prefix the prefix to identify the history
     */
    public void saveEditorHistoryState(SharedPreferences.Editor editor, String prefix) {
        // Store hash code of text in the editor so that we can check if the
        // editor contents has changed.
        editor.putString(prefix + ".hash",
                String.valueOf(mEditText.getText().toString().hashCode()));
        editor.putInt(prefix + ".maxSize", mCTEditHistory.MAX_HISTORY_SIZE);
        editor.putInt(prefix + ".position", mCTEditHistory.mPosition);
        editor.putInt(prefix + ".size", mCTEditHistory.mHistory.size());

        int i = 0;
        for (CTEditorHistoryItem ei : mCTEditHistory.mHistory) {
            String pre = prefix + "." + i;

            editor.putInt(pre + ".start", ei.mmStart);
            editor.putString(pre + ".before", ei.mmBefore.toString());
            editor.putString(pre + ".after", ei.mmAfter.toString());

            i++;
        }
    }

    /**
     * Restores the EditHistory in the Shared Preferences of the application
     *
     * @param sp the Shared Preferences Editor of the application
     * @param prefix the prefix to identify the history
     * @return
     * @throws IllegalStateException
     */
    public boolean restoreEditorHistoryState(SharedPreferences sp, String prefix)
            throws IllegalStateException {

        boolean ok = restoreEditorHistoryStateInternal(sp, prefix);
        if (!ok) {
            mCTEditHistory.clear();
        }

        return ok;
    }

    /**
     * Restores the EditHistory in the Shared Preferences of the application
     *
     * @param sp the Shared Preferences Editor of the application
     * @param prefix the prefix to identify the history
     * @return
     * @throws IllegalStateException
     */
    private boolean restoreEditorHistoryStateInternal(SharedPreferences sp, String prefix) {
        String hash = sp.getString(prefix + ".hash", null);
        if (hash == null) {
            // No state to be restored.
            return true;
        }

        if (Integer.valueOf(hash) != mEditText.getText().toString().hashCode()) {
            return false;
        }

        mCTEditHistory.clear();
        mCTEditHistory.MAX_HISTORY_SIZE = sp.getInt(prefix + ".maxSize", -1);

        int count = sp.getInt(prefix + ".size", -1);
        if (count == -1) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            String pre = prefix + "." + i;

            int start = sp.getInt(pre + ".start", -1);
            String before = sp.getString(pre + ".before", null);
            String after = sp.getString(pre + ".after", null);

            if (start == -1 || before == null || after == null) {
                return false;
            }
            mCTEditHistory.add(new CTEditorHistoryItem(start, before, after));
        }

        mCTEditHistory.mPosition = sp.getInt(prefix + ".position", -1);
        if (mCTEditHistory.mPosition == -1) {
            return false;
        }

        return true;
    }

    enum ActionType {
        INSERT, DELETE, PASTE, NOT_DEF;
    }

    private final class EditTextChangeListener implements TextWatcher {
        private CharSequence mBeforeChange;
        private CharSequence mAfterChange;
        private ActionType lastActionType = ActionType.NOT_DEF;
        private long lastActionTime = 0;

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (isHistoryAvailable) {
                return;
            }

            mBeforeChange = s.subSequence(start, start + count);
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (isHistoryAvailable) {
                return;
            }

            mAfterChange = s.subSequence(start, start + count);
            makeBatch(start);
        }

        private void makeBatch(int start) {
            ActionType at = getActionType();
            CTEditorHistoryItem CTEditorHistoryItem = mCTEditHistory.getCurrent();
            if ((lastActionType != at || ActionType.PASTE == at || System.currentTimeMillis() - lastActionTime > 1000) || CTEditorHistoryItem == null) {
                mCTEditHistory.add(new CTEditorHistoryItem(start, mBeforeChange, mAfterChange));
            } else {
                if (at == ActionType.DELETE) {
                    CTEditorHistoryItem.mmStart = start;
                    CTEditorHistoryItem.mmBefore = TextUtils.concat(mBeforeChange, CTEditorHistoryItem.mmBefore);
                } else {
                    CTEditorHistoryItem.mmAfter = TextUtils.concat(CTEditorHistoryItem.mmAfter, mAfterChange);
                }
            }
            lastActionType = at;
            lastActionTime = System.currentTimeMillis();
        }

        private ActionType getActionType() {
            if (!TextUtils.isEmpty(mBeforeChange) && TextUtils.isEmpty(mAfterChange)) {
                return ActionType.DELETE;
            } else if (TextUtils.isEmpty(mBeforeChange) && !TextUtils.isEmpty(mAfterChange)) {
                return ActionType.INSERT;
            } else {
                return ActionType.PASTE;
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }
}