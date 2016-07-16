package org.jraf.android.ticker.provider.manager;

import org.jraf.android.ticker.message.MessageQueueable;
import org.jraf.android.ticker.provider.ProviderException;

public interface ProviderManagerCallbacks extends MessageQueueable {
    void onException(ProviderException e);

    void onStart();

    void onStop();
}
