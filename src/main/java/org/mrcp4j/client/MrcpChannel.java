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
import org.mrcp4j.MrcpMethodName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpMessage;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.ChannelIdentifier;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;
import org.mrcp4j.message.request.MrcpRequest;
import org.mrcp4j.message.request.MrcpRequestFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Provides all primary functionality required for an MRCPv2 client to interact with an MRCPv2 resource.  Through an instance of this class
 * clients can construct and send MRCP requests, receive responses and be notified of events triggered by the MRCP resource.
 * <p/>
 * To construct a {@code MrcpChannel} instance use {@link org.mrcp4j.client.MrcpProvider#createChannel(java.lang.String, java.net.InetAddress, int, java.lang.String)}.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class MrcpChannel implements MrcpMessageHandler {

    private static Log _log = LogFactory.getLog(MrcpChannel.class);

    private MrcpResponse _response = new MrcpResponse();
    private Object _responseLock = new Object();
    private List<MrcpEventListener> eventListeners = Collections.synchronizedList(new ArrayList<MrcpEventListener>());
	private List<MrcpResponseListener> responseListeners = Collections.synchronizedList(new ArrayList<MrcpResponseListener>());

    private ChannelIdentifier _channelID;
    private MrcpSocket _socket;
    private long _requestID;

    MrcpChannel(String channelID, MrcpSocket socket) throws IllegalValueException {
        _channelID = (ChannelIdentifier) MrcpHeaderName.CHANNEL_IDENTIFIER.createHeaderValue(channelID);
        _socket = socket;
        _requestID = 101L; //System.currentTimeMillis();
        socket.addMessageHandler(_channelID, this);
    }

    /**
     * Retrieves the channel ID associated with this channel.
     * @return the channel ID associated with this channel.
     */
    public ChannelIdentifier getChannelID() {
        return _channelID;
    }

    /*public String getType() {
        return null;
    }*/

    /**
     * Creates a request object associated with this channel.  The request object can then be passed
     * to {@link org.mrcp4j.client.MrcpChannel#sendRequest(org.mrcp4j.message.request.MrcpRequest)}
     * (after setting content or other parameters) in order to actually invoke the request on the MRCP
     * resource accessed by this channel.
     *
     * @param  methodName   name of the method the desired request object should represent.
     * @return              request object representing the specified method call.
     */
    public MrcpRequest createRequest(MrcpMethodName methodName) {

        MrcpRequest request = MrcpRequestFactory.createRequest(methodName);

        // mrcp-version
        request.setVersion(MrcpMessage.MRCP_VERSION_2_0);

        // message-length not yet known

        // request-id
        // TODO: set this when message sent instead (to guarantee sequence)
        synchronized (this) {
            request.setRequestID(_requestID++);
        }

        MrcpHeader header = MrcpHeaderName.CHANNEL_IDENTIFIER.constructHeader(_channelID);
        request.addHeader(header);

        return request;
    }

    /**
     * Invokes a request on the MRCP resource associated with this channel.
     * @param  request                 specification of the request to be invoked.
     * @return                         the response provided by the MRCP resource to the specified request.
     * @throws IOException             if an I/O error occurs.
     * @throws MrcpInvocationException           if the MRCP resource returned a response error code
     * @throws InterruptedException    if another thread interrupted the current thread while the current thread
     *                                 was waiting for a response from the MRCP resource.
     */
	@Deprecated
    public synchronized MrcpResponse sendRequest(final MrcpRequest request)
      throws IOException, MrcpInvocationException, InterruptedException {

		final MrcpResponse[] result = {null};  // *vomit*
		final CountDownLatch ready = new CountDownLatch(1);

		addResponseListener(new MrcpResponseListener() {
			public void responseReceived(MrcpResponse response) {
				if (response.getRequestID() == request.getRequestID()) {
//					_log.debug("response for request " + response.getRequestID() + " " + request.getMethodName() + ": " + response.getStatusCode() + " " + response.getRequestState());
					if (response.getRequestState() != MrcpRequestState.PENDING) {
						result[0] = response;
						ready.countDown();
						removeResponseListener(this);
					}
				}
			}
		});
		sendRequestOneShot(request);

		ready.await();
		MrcpResponse response = result[0];

		if (response.getStatusCode() > 299) {
			throw new MrcpInvocationException(response);
		}
		return response;
    }

    public void sendRequestOneShot(MrcpRequest request)
      throws IOException, MrcpInvocationException {
        _socket.sendRequest(request);
    }

    /**
     * Registers an event listener that will be notified of any MRCP events received on this channel.
     * @param listener instance to be notified of MRCP events received on this channel.
     */
    public void addEventListener(MrcpEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * Unregisters an event listener that may have been registered to receive MRCP events from this channel.
     * @param listener instance to be removed from listeners to events received on this channel.
     */
    public void removeEventListener(MrcpEventListener listener) {
        eventListeners.remove(listener);
    }

	public void addResponseListener(MrcpResponseListener listener) {
		responseListeners.add(listener);
	}

	public void removeResponseListener(MrcpResponseListener listener) {
		responseListeners.remove(listener);
	}

    /* (non-Javadoc)
     * @see org.mrcp4j.client.MrcpMessageHandler#handleMessage(org.mrcp4j.message.MrcpMessage)
     */
    public void handleMessage(MrcpMessage message) {
        if (message instanceof MrcpResponse) {
			MrcpResponse response = (MrcpResponse) message;

			List<MrcpResponseListener> ls = new ArrayList<MrcpResponseListener>(responseListeners);
			for (MrcpResponseListener listener : ls) {
				listener.responseReceived(response);
			}
			if (ls.isEmpty()) {
				_log.warn("No response listeners registered, and yet a response was received:\n" + response.toString());
			}
        } else if (message instanceof MrcpEvent) {
			MrcpEvent event = (MrcpEvent) message;
			List<MrcpEventListener> ls = new ArrayList<MrcpEventListener>(eventListeners);
            for (MrcpEventListener listener : ls) {
                listener.eventReceived(event);
            }
			if (ls.isEmpty()) {
				_log.warn("No event listeners registered, and yet an event was received");
			}
        } else {
            _log.warn("Unknown message:\n" + message.toString());
        }
    }

    public void close() {
        _socket.close();
    }

}