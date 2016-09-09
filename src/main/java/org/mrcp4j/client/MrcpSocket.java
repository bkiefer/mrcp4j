/*
 * MRCP4J - Java API implementation of MRCPv2 specification
 *
 * Copyright (C) 2005-2006 SpeechForge - http://www.speechforge.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 *
 * Contact: ngodfredsen@users.sourceforge.net
 *
 */
package org.mrcp4j.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mrcp4j.message.MrcpMessage;
import org.mrcp4j.message.header.ChannelIdentifier;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.request.MrcpRequest;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides an endpoint for communication between the MRCPv2 client and the MRCPv2 server (for internal library use only).
 *
 * <p>This class is intended for internal use by the MRCP4J implementation code.  Please see
 * {@link org.mrcp4j.client.MrcpProvider#createChannel(java.lang.String, java.net.InetAddress, int, java.lang.String)}
 * for constructing an {@link org.mrcp4j.client.MrcpChannel} that can be used to send control messages to the media
 * resource on the MRCP server.</p>
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class MrcpSocket {

    private static Log _log = LogFactory.getLog(MrcpSocket.class);

    private MrcpRequestEncoder _requestEncoder = new MrcpRequestEncoder();
    Map<ChannelIdentifier , MrcpMessageHandler> _handlers = Collections.synchronizedMap(new HashMap<ChannelIdentifier , MrcpMessageHandler>());

    private Socket _socket;
    BufferedReader _in;
    private PrintWriter _out;

    MrcpSocket(InetAddress host, int port) throws IOException {
        _socket = new Socket(host, port);
        _in = new BufferedReader(new InputStreamReader(_socket.getInputStream(), "UTF-8"));
        _out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(_socket.getOutputStream())));
//		_out = new PrintWriter(_socket.getOutputStream(), true);
        new ReadThread().start();
    }

    public void sendRequest(MrcpRequest request) throws IOException {
        try {
            _requestEncoder.encode(request, _out);
            _out.flush();
        } catch (IOException e){
            // TODO: may need to reset socket here...
            _log.debug(e, e);
            throw e;
        }
    }

    public void addMessageHandler(ChannelIdentifier channelID, MrcpMessageHandler handler) {
        _handlers.put(channelID, handler);
    }

    public void removeMessageHandler(ChannelIdentifier channelID) {
        _handlers.remove(channelID);
    }

    private final AtomicBoolean _shouldRun = new AtomicBoolean(true);

    public void close() {
        try {
            _shouldRun.set(false);
            _socket.close();
        } catch (IOException e) {
            _log.warn(e, e);
            e.printStackTrace();
        }
    }

    private class ReadThread extends Thread {

		public ReadThread() {
			setName("MRCP socket reader");
		}

        private MrcpMessageDecoder _messageDecoder = new MrcpMessageDecoder();

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            while (_shouldRun.get()) {
                // TODO: switch to nio so that the following statement doesn't block forever when the socket is closed
                try {
                    MrcpMessage message = _messageDecoder.decode(_in);
                    ChannelIdentifier channelID = message.getChannelIdentifier();
                    MrcpMessageHandler handler = _handlers.get(channelID);
                    if (handler != null) {
                        handler.handleMessage(message);
                    } else if (_log.isDebugEnabled()) {
                        _log.debug("No handler found for channel: " + channelID);
                    }
                } catch (IOException e) {
                    if (_shouldRun.compareAndSet(true, false)) {
                        // this is an unexpected one
                        _log.warn(e, e);
                    }
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    _log.warn(e, e);
                } catch (IllegalValueException e) {
                    // TODO Auto-generated catch block
                    _log.warn(e, e);
                }
            }
        }

    }

}