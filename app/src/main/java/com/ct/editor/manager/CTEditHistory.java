package com.ct.editor.manager;

import java.util.LinkedList;

/**
 * {@code This is the Editor History Entity, uses a LinkedList of {@link CTEditorHistoryItem}}
 *
 * @author Name:    Romi Chandra,
 * Email:   romi.d.nerd@gmail.com.
 * @version 1.0
 * @since 30, May, 2020
 */
public final class CTEditHistory {
    // current edit item position, changes with undo, redo and trim
    public int mPosition = 0;
    public int MAX_HISTORY_SIZE = -1;

    public final LinkedList<CTEditorHistoryItem> mHistory = new LinkedList<CTEditorHistoryItem>();

    public void clear() {
        mPosition = 0;
        mHistory.clear();
    }

    public void add(CTEditorHistoryItem item) {
        while (mHistory.size() > mPosition) {
            mHistory.removeLast();
        }
        mHistory.add(item);
        // increase position if adding item
        mPosition++;

        if (MAX_HISTORY_SIZE >= 0) {
            trimHistory();
        }
    }

    public void setMaxHistorySize(int maxHistorySize) {
        MAX_HISTORY_SIZE = maxHistorySize;
        if (MAX_HISTORY_SIZE >= 0) {
            trimHistory();
        }
    }

    public void trimHistory() {
        while (mHistory.size() > MAX_HISTORY_SIZE) {
            mHistory.removeFirst();
            // decrease position if removing item
            mPosition--;
        }

        // safe check if position goes negative make it 0
        if (mPosition < 0) {
            mPosition = 0;
        }
    }

    public CTEditorHistoryItem getCurrent() {
        // safe position overflow check
        if (mPosition == 0) {
            return null;
        }
        return mHistory.get(mPosition - 1);
    }

    public CTEditorHistoryItem getPrevious() {
        // safe position overflow check
        if (mPosition == 0) {
            return null;
        }
        mPosition--;
        return mHistory.get(mPosition);
    }

    public CTEditorHistoryItem getNext() {
        // safe position overflow check
        if (mPosition >= mHistory.size()) {
            return null;
        }

        CTEditorHistoryItem item = mHistory.get(mPosition);
        mPosition++;
        return item;
    }
}
