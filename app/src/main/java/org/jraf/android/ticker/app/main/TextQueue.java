package org.jraf.android.ticker.app.main;

import java.util.ArrayList;
import java.util.Arrays;

import android.support.annotation.Nullable;

public class TextQueue {
    private final int mSize;
    private final ArrayList<CharSequence> mList;
    private int mCurrentIndex;

    public TextQueue(int size) {
        mSize = size;
        mList = new ArrayList<>(size * 2);
    }

    @Nullable
    public synchronized CharSequence getNext() {
        if (mList.isEmpty()) return null;
        CharSequence res = mList.get(mCurrentIndex);
        mCurrentIndex++;
        if (mCurrentIndex == mList.size()) mCurrentIndex = 0;
        return res;
    }

    public synchronized void add(CharSequence... text) {
        mList.addAll(Arrays.asList(text));
        if (mList.size() > mSize) {
            // Too many items: discard old ones
            int elementsToDiscard = mList.size() - mSize;
            mList.subList(0, elementsToDiscard).clear();

            mCurrentIndex -= elementsToDiscard;
            if (mCurrentIndex < 0) mCurrentIndex = 0;
        }
    }
}
