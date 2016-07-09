package org.jraf.android.ticker.app.main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import android.support.annotation.Nullable;

public class TextQueue {
    private final int mSize;
    private final ArrayList<CharSequence> mList;
    private final ArrayDeque<CharSequence> mUrgentQueue;
    private int mCurrentIndex = 0;

    public TextQueue(int size) {
        mSize = size;
        mList = new ArrayList<>(size * 2);
        mUrgentQueue = new ArrayDeque<>(size * 2);
    }

    @Nullable
    public synchronized CharSequence getNext() {
        // Try the urgent queue first
        if (!mUrgentQueue.isEmpty()) {
            return mUrgentQueue.pop();
        }

        // Try the normal list
        if (mList.isEmpty()) return null;
        CharSequence res = mList.get(mCurrentIndex);
        mCurrentIndex = (mCurrentIndex + 1) % mList.size();
        return res;
    }

    public synchronized void add(CharSequence... text) {
        mList.addAll(Arrays.asList(text));

        // Discard old items if any
        int elementsToDiscard = 0;
        if (mList.size() > mSize) {
            elementsToDiscard = mList.size() - mSize;
            mList.subList(0, elementsToDiscard).clear();
        }

        // Shift the index if elements were discarded
        mCurrentIndex -= elementsToDiscard;
        if (mCurrentIndex < 0) mCurrentIndex = 0;
    }

    public synchronized void addUrgent(CharSequence... text) {
        mUrgentQueue.addAll(Arrays.asList(text));
    }
}