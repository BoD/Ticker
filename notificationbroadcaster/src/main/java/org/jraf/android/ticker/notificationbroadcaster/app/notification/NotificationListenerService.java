/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.android.ticker.notificationbroadcaster.app.notification;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Notification;
import android.os.AsyncTask;
import android.service.notification.StatusBarNotification;

import org.jraf.android.ticker.notificationbroadcaster.R;
import org.jraf.android.util.log.Log;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    private static final byte VERSION_CODE = 1;
    private static final byte[] HEADER = {'T', 'i', 'c', 'k', 'e', 'r', VERSION_CODE};
    private static final int PORT = 2130;
    private static final byte FLAG_URGENT = 0b1;

    private DatagramSocket mSocket;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Notification notification = sbn.getNotification();
        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            // Ignore ongoing notifications
            return;
        }
        final String title = notification.extras.getString(Notification.EXTRA_TITLE);
        final String text = notification.extras.getCharSequence(Notification.EXTRA_TEXT, "").toString();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                sendBroadcastMessage(title, text);
                return null;
            }
        }.execute();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }


    @Override
    public void onDestroy() {
        Log.d();
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
        super.onDestroy();
    }

    private DatagramSocket getSocket() {
        if (mSocket == null) {
            try {
                mSocket = new DatagramSocket(PORT);
                mSocket.setBroadcast(true);
                mSocket.connect(InetAddress.getByName("255.255.255.255"), PORT);
            } catch (IOException e) {
                Log.e(e, "Could not create a socket");
            }
        }
        return mSocket;
    }

    private void sendBroadcastMessage(String title, String text) {
        String messageText = getString(R.string.notificationService_messageText, title, text);
        byte[] messageTextBytes = null;
        try {
            messageTextBytes = messageText.getBytes("utf-8");
        } catch (UnsupportedEncodingException ignored) {}
        assert messageTextBytes != null;
        int messageTextLength = messageTextBytes.length;
        byte[] data = new byte[HEADER.length + 1 + messageTextLength];

        // Header
        System.arraycopy(HEADER, 0, data, 0, HEADER.length);

        // Urgent flag
        data[HEADER.length] = FLAG_URGENT;

        // Message text
        System.arraycopy(messageTextBytes, 0, data, HEADER.length + 1, messageTextLength);

        // Send it
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            getSocket().send(packet);
        } catch (IOException e) {
            Log.e(e, "Could not send packet");
        }
    }
}
