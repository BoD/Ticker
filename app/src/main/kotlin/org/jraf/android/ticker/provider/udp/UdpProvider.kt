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
package org.jraf.android.ticker.provider.udp

import android.content.Context
import org.jraf.android.ticker.provider.Provider
import org.jraf.android.ticker.provider.ProviderException
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks
import org.jraf.android.util.log.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.nio.charset.Charset
import java.util.Arrays

class UdpProvider : Provider {
    companion object {
        private const val VERSION_CODE: Byte = 1
        private val HEADER = byteArrayOf('T'.toByte(), 'i'.toByte(), 'c'.toByte(), 'k'.toByte(), 'e'.toByte(), 'r'.toByte(), VERSION_CODE)

        private const val PORT = 2130
        private const val DATA_LENGTH = 1024

        private const val FLAG_URGENT: Byte = 1
    }

    private lateinit var mCallbacks: ProviderManagerCallbacks
    private var mDatagramSocket: DatagramSocket? = null
    @Volatile private var mStopping: Boolean = false

    @Throws(ProviderException::class)
    override fun init(context: Context, callbacks: ProviderManagerCallbacks) {
        mCallbacks = callbacks
    }

    @Throws(ProviderException::class)
    override fun start() {
        try {
            mDatagramSocket = DatagramSocket(PORT)
        } catch (e: SocketException) {
            throw ProviderException(e)
        }

        mCallbacks.onStart()
        startListenThread()
    }

    private fun startListenThread() {
        object : Thread() {
            override fun run() {
                while (!mStopping) {
                    val packet = DatagramPacket(ByteArray(DATA_LENGTH), DATA_LENGTH)
                    try {
                        mDatagramSocket!!.receive(packet)
                    } catch (e: IOException) {
                        if (!mStopping) {
                            mCallbacks.onException(ProviderException(e))
                        }
                        this@UdpProvider.stop()
                        break
                    }

                    // Take only actual data, not the whole allocated array
                    var data = Arrays.copyOfRange(packet.data, packet.offset, packet.offset + packet.length)

                    // Data must be at least sizeof(header) + 1 byte for the flags + at least 1 byte for the message
                    if (data.size < HEADER.size + 2) {
                        Log.d("Received an invalid packet: ignore it")
                        continue
                    }

                    // Check for header
                    val header = Arrays.copyOf(data, HEADER.size)
                    if (!Arrays.equals(header, HEADER)) {
                        Log.d("Received an invalid packet: ignore it")
                        continue
                    }

                    // Look for flags
                    val flags = data[HEADER.size]
                    val urgent = flags.toInt() and FLAG_URGENT.toInt() == FLAG_URGENT.toInt()

                    // Remove header
                    data = Arrays.copyOfRange(data, HEADER.size + 1, data.size)
                    val message: String
                    try {
                        message = String(data, Charset.forName("utf-8"))
                    } catch (t: Throwable) {
                        Log.d("Received an invalid message: ignore it")
                        continue
                    }

                    Log.d("Received new message: '%s', urgent=%s", message, urgent)

                    if (urgent) {
                        mCallbacks.addUrgent(message)
                    } else {
                        mCallbacks.add(message)
                    }
                }

                mCallbacks.onStop()
            }
        }.start()
    }

    override fun stop() {
        mStopping = true
        mDatagramSocket?.disconnect()
        mDatagramSocket?.close()
    }
}
