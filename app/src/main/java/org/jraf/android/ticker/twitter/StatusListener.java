package org.jraf.android.ticker.twitter;

import java.util.List;

import twitter4j.Status;

public interface StatusListener {
    void onNewStatuses(List<Status> statuses);
}
