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
