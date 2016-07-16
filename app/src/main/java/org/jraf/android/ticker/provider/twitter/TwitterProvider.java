package org.jraf.android.ticker.provider.twitter;

import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import org.jraf.android.ticker.R;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.ProviderException;
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

public class TwitterProvider implements Provider {
    private Context mContext;
    private ProviderManagerCallbacks mCallbacks;
    private TwitterClient mTwitterClient;

    @Override
    public void init(Context context, ProviderManagerCallbacks callbacks) throws ProviderException {
        mContext = context;
        mCallbacks = callbacks;
    }

    @Override
    public void start() throws ProviderException {
        mTwitterClient = TwitterClient.getInstance(mContext);
        mTwitterClient.addListener(new StatusListener() {
            @Override
            public void onNewStatuses(List<Status> statuses) {
                for (Status status : statuses) {
                    String screenName = status.getUser().getScreenName();
                    String statusText = status.getText();

                    // Remove all urls
                    for (URLEntity urlEntity : status.getURLEntities()) {
                        statusText = statusText.replaceAll(Pattern.quote(urlEntity.getURL()), "");
                    }
                    for (MediaEntity mediaEntity : status.getMediaEntities()) {
                        statusText = statusText.replaceAll(Pattern.quote(mediaEntity.getURL()), "");
                    }

                    // Add author
                    String author = "@" + screenName;
                    statusText = author + " " + statusText;
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(statusText);
                    ForegroundColorSpan foregroundColorSpan =
                            new ForegroundColorSpan(ResourcesCompat.getColor(mContext.getResources(), R.color.tweetAuthor, null));
                    spannableStringBuilder.setSpan(foregroundColorSpan, 0, author.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    mCallbacks.add(spannableStringBuilder);
                }
            }
        });
        mTwitterClient.startClient();
        mCallbacks.onStart();
    }

    @Override
    public void stop() {
        if (mTwitterClient != null) {
            mTwitterClient.stopClient();
        }
        mCallbacks.onStop();
    }
}
