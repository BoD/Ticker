package org.jraf.android.ticker.provider.manager;

import android.content.Context;

import org.jraf.android.ticker.message.MessageQueueable;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.ProviderException;
import org.jraf.android.util.log.Log;

class ProviderEntry implements ProviderManagerCallbacks {
    private final Context mContext;
    private final Class<? extends Provider> mProviderClass;
    private final MessageQueueable mMessageQueueable;
    private Provider mProvider;

    ProviderEntry(Context context, Class<? extends Provider> providerClass, MessageQueueable messageQueueable) {
        mContext = context.getApplicationContext();
        mProviderClass = providerClass;
        mMessageQueueable = messageQueueable;
    }

    void start() {
        try {
            mProvider = mProviderClass.newInstance();
        } catch (Exception ignored) {}
        assert mProvider != null;
        try {
            Log.i("Initializing provider " + mProviderClass);
            mProvider.init(mContext, this);
        } catch (ProviderException e) {
            Log.w(e, "Could not init %s: give up", mProviderClass);
            return;
        }

        try {
            Log.i("Starting provider " + mProviderClass);
            mProvider.start();
        } catch (ProviderException e) {
            Log.w(e, "Could not start %s: give up", mProviderClass);
        }
    }

    @Override
    public void onException(ProviderException e) {
        Log.w(e, "Exception occurred in %s: try to start again", mProviderClass);
        start();
    }

    @Override
    public void onStart() {
        Log.d("%s started", mProviderClass);
    }

    @Override
    public void onStop() {
        Log.d("%s stopped", mProviderClass);
    }

    @Override
    public void add(CharSequence... messages) {
        mMessageQueueable.add(messages);
    }

    @Override
    public void addUrgent(CharSequence... messages) {
        mMessageQueueable.addUrgent(messages);
    }

    void stop() {
        if (mProvider != null) {
            mProvider.stop();
        }
    }
}
