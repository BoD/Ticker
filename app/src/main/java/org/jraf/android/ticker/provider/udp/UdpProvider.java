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
package org.jraf.android.ticker.provider.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import android.content.Context;

import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.ProviderException;
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks;
import org.jraf.android.util.log.Log;

public class UdpProvider implements Provider {
    private static final byte VERSION_CODE = 1;
    private static final byte[] HEADER = {'T', 'i', 'c', 'k', 'e', 'r', VERSION_CODE};

    private static final int PORT = 2130;
    private static final int DATA_LENGTH = 1024;

    private static final byte FLAG_URGENT = 0b1;

    private ProviderManagerCallbacks mCallbacks;
    private DatagramSocket mDatagramSocket;
    private volatile boolean mStopping;

    @Override
    public void init(Context context, ProviderManagerCallbacks callbacks) throws ProviderException {
        mCallbacks = callbacks;
    }

    @Override
    public void start() throws ProviderException {
        try {
            mDatagramSocket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            throw new ProviderException(e);
        }
        mCallbacks.onStart();
        startListenThread();
    }

    private void startListenThread() {
        new Thread() {
            @Override
            public void run() {
                while (!mStopping) {
                    DatagramPacket packet = new DatagramPacket(new byte[DATA_LENGTH], DATA_LENGTH);
                    try {
                        mDatagramSocket.receive(packet);
                    } catch (IOException e) {
                        if (!mStopping) {
                            mCallbacks.onException(new ProviderException(e));
                        }
                        UdpProvider.this.stop();
                        break;
                    }

                    // Take only actual data, not the whole allocated array
                    byte[] data = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());

                    // Data must be at least sizeof(header) + 1 byte for the flags + at least 1 byte for the message
                    if (data.length < HEADER.length + 2) {
                        Log.d("Received an invalid packet: ignore it");
                        continue;
                    }

                    // Check for header
                    byte[] header = Arrays.copyOf(data, HEADER.length);
                    if (!Arrays.equals(header, HEADER)) {
                        Log.d("Received an invalid packet: ignore it");
                        continue;
                    }

                    // Look for flags
                    byte flags = data[HEADER.length];
                    boolean urgent = (flags & FLAG_URGENT) == FLAG_URGENT;

                    // Remove header
                    data = Arrays.copyOfRange(data, HEADER.length + 1, data.length);
                    String message;
                    try {
                        message = new String(data, "utf-8");
                    } catch (Throwable t) {
                        Log.d("Received an invalid message: ignore it");
                        continue;
                    }

                    Log.d("Received new message: '%s', urgent=%s", message, urgent);

                    if (urgent) {
                        mCallbacks.addUrgent(message);
                    } else {
                        mCallbacks.add(message);
                    }
                }

                mCallbacks.onStop();
            }
        }.start();
    }

    @Override
    public void stop() {
        mStopping = true;
        mDatagramSocket.disconnect();
        mDatagramSocket.close();
    }
}
