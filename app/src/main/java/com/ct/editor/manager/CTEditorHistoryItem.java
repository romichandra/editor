package com.ct.editor.manager;

/**
 * {@code Entity Model class for Editor History Item}
 *
 * @author Name:    Romi Chandra,
 * Email:   romi.d.nerd@gmail.com.
 * @version 1.0
 * @since 30, May, 2020
 */
public final class CTEditorHistoryItem {
    public int mmStart;
    public CharSequence mmBefore;
    public CharSequence mmAfter;

    public CTEditorHistoryItem(int start, CharSequence before, CharSequence after) {
        mmStart = start;
        mmBefore = before;
        mmAfter = after;
    }

    @Override
    public String toString() {
        return "CTEditorHistoryItem{" +
                "mmStart=" + mmStart +
                ", mmBefore=" + mmBefore +
                ", mmAfter=" + mmAfter +
                '}';
    }
}
