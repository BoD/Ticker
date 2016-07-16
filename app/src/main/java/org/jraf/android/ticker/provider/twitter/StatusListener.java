package org.jraf.android.ticker.provider.twitter;

import java.util.List;

import twitter4j.Status;

public interface StatusListener {
    void onNewStatuses(List<Status> statuses);
}
