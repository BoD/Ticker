package org.jraf.android.ticker.message;

public interface MessageQueueable {
    void add(CharSequence... messages);

    void addUrgent(CharSequence... messages);
}
