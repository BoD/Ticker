package org.jraf.android.ticker.provider;

import android.content.Context;

import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks;

public interface Provider {
    void init(Context context, ProviderManagerCallbacks callbacks) throws ProviderException;

    void start() throws ProviderException;

    void stop();
}
