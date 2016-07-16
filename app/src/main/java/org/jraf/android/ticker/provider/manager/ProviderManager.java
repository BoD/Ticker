package org.jraf.android.ticker.provider.manager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import org.jraf.android.ticker.message.MessageQueueable;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.datetimeweather.DateTimeWeatherProvider;
import org.jraf.android.ticker.provider.twitter.TwitterProvider;
import org.jraf.android.ticker.provider.udp.UdpProvider;

public class ProviderManager {
    private static final List<Class<? extends Provider>> PROVIDER_CLASSES = new ArrayList<>();

    static {
        PROVIDER_CLASSES.add(UdpProvider.class);
        PROVIDER_CLASSES.add(TwitterProvider.class);
        PROVIDER_CLASSES.add(DateTimeWeatherProvider.class);
    }

    private static ProviderManager INSTANCE;
    private List<ProviderEntry> mProviderEntries = new ArrayList<>(4);
    private boolean mStarted;

    public static ProviderManager get() {
        if (INSTANCE == null) {
            INSTANCE = new ProviderManager();
        }
        return INSTANCE;
    }

    public void startListeners(Context context, MessageQueueable messageQueueable) {
        if (mStarted) return;
        mStarted = true;

        for (Class<? extends Provider> providerClass : PROVIDER_CLASSES) {
            ProviderEntry providerEntry = new ProviderEntry(context, providerClass, messageQueueable);
            mProviderEntries.add(providerEntry);
            providerEntry.start();
        }
    }

    public void stopListeners() {
        if (!mStarted) return;
        mStarted = false;
        for (ProviderEntry providerEntry : mProviderEntries) {
            providerEntry.stop();
        }
    }
}
