package org.jraf.android.ticker.twitter;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;

import org.jraf.android.ticker.R;
import org.jraf.android.util.listeners.Listeners;
import org.jraf.android.util.log.Log;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterClient {
    private static final long CHECK_PERIOD = TimeUnit.MINUTES.toMillis(3);
    private static final int RETRIEVE_COUNT = 30;

    private static final Comparator<Status> STATUS_COMPARATOR = new Comparator<Status>() {
        @Override
        public int compare(Status o1, Status o2) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        }
    };

    private Context mContext;
    private Twitter mTwitter;
    private ScheduledExecutorService mScheduledExecutorService;
    private Listeners<StatusListener> mListeners = new Listeners<>();

    public static TwitterClient getInstance(Context context) {
        TwitterClient res = new TwitterClient();
        res.mContext = context.getApplicationContext();
        return res;
    }

    public void addListener(StatusListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(StatusListener listener) {
        mListeners.remove(listener);
    }

    public void startClient() {
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mScheduledExecutorService.scheduleAtFixedRate(new CheckForNewTweetsRunnable(), 0, CHECK_PERIOD, TimeUnit.MILLISECONDS);
    }

    public void stopClient() {
        if (mScheduledExecutorService != null) mScheduledExecutorService.shutdownNow();
        mScheduledExecutorService = null;
    }

    private Twitter getTwitter() {
        if (mTwitter == null) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setDebugEnabled(true).setOAuthConsumerKey(mContext.getString(R.string.apiKeyTwitterOauthConsumerKey));
            configurationBuilder.setOAuthConsumerSecret(mContext.getString(R.string.apiKeyTwitterOauthConsumerSecret));
            configurationBuilder.setOAuthAccessToken(mContext.getString(R.string.apiKeyTwitterOauthAccessToken));
            configurationBuilder.setOAuthAccessTokenSecret(mContext.getString(R.string.apiKeyTwitterOauthAccessTokenSecret));
            TwitterFactory twitterFactory = new TwitterFactory(configurationBuilder.build());
            mTwitter = twitterFactory.getInstance();
        }
        return mTwitter;
    }

    private class CheckForNewTweetsRunnable implements Runnable {
        private ResponseList<Status> mLatestStatuses;

        @Override
        public void run() {
            try {
                Log.d("Checking for new tweets");
                final ResponseList<Status> statusList = getTwitter().getUserListStatuses("bod", "news", new Paging(1, RETRIEVE_COUNT));
                if (statusList.isEmpty()) return;
                Collections.sort(statusList, STATUS_COMPARATOR);
                ResponseList<Status> latestStatuses = statusList;
                if (mLatestStatuses != null) statusList.removeAll(mLatestStatuses);
                mLatestStatuses = latestStatuses;

                if (statusList.isEmpty()) {
                    // No change
                    Log.d("No new tweets");
                    return;
                }

                // New tweets (or first time)
                Log.d("%d new tweets", statusList.size());
                mListeners.dispatch(new Listeners.Dispatcher<StatusListener>() {
                    @Override
                    public void dispatch(StatusListener listener) {
                        listener.onNewStatuses(statusList);
                    }
                });
            } catch (Exception e) {
                Log.w(e, "Could not retrieve tweets");
            }
        }
    }
}
