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
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;

/**
 * Decodes messages received in MRCPv2 format into {@link org.mrcp4j.message.MrcpMessage} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class MrcpMessageDecoder {

    private static Log _log = LogFactory.getLog(MrcpMessageDecoder.class);

    private MrcpResponseDecoder _responseDecoder = new MrcpResponseDecoder();
    private MrcpEventDecoder _eventDecoder = new MrcpEventDecoder();


    private static final int RESPONSE_LINE_REQUEST_ID_PART  = 2;
    private static final int START_LINE_PART_COUNT          = 5;

	private static Charset utf8 = Charset.forName("UTF-8");

	private static int utf8Length(char c) {
		// *vomit* *vomit* *vomit* !!!
		char[] cc = {c};
		return new String(cc).getBytes(utf8).length;
	}

    // TODO: change ParseException to MrcpProtocolException
    public MrcpMessage decode(BufferedReader in) throws IOException, ParseException {

        // read until the first non-empty line to get the start-line
        String line = null;
		while ((line = in.readLine()) == null || (line = line.trim()).equals("")) {
            _log.debug((line == null) ? "MrcpMessageDecoder: null line" : "MrcpMessageDecoder: empty line");
        }

        // verify the start-line contains the correct number of parts
        String[] startLineParts = line.split(" ");
        if (startLineParts.length != START_LINE_PART_COUNT) {
            throw new ParseException("Incorrect start-line format, got " + startLineParts.length + " parts in '" + line + "'" , -1);
        }

        // determine if the message is a response or an event message
        boolean isResponse = false;
        try {
            Long.parseLong(startLineParts[RESPONSE_LINE_REQUEST_ID_PART]);
            isResponse = true;
        } catch (NumberFormatException e){
            // ignore, message should be event
        }

        // create the message from the start-line
        MrcpMessage message = null;
        if (isResponse) {
            message = _responseDecoder.createResponse(line);
        } else {
            message = _eventDecoder.createEvent(line);
        }

        // populate message headers
        while ((line = in.readLine()) != null && !(line = line.trim()).equals("")) {
            // TODO: handle multi-line headers
            int index = line.indexOf(':');
            if (index < 1) {
                throw new ParseException("Incorrect message-header format!", -1);
            }
            String name = line.substring(0, index);
            String value = line.substring(index + 1).trim();
            MrcpHeader header = MrcpHeaderName.createHeader(name, value);
            message.addHeader(header);
        }

        // read message content if present
        MrcpHeader contentLengthHeader = message.getHeader(MrcpHeaderName.CONTENT_LENGTH);
        int contentLength = 0;
        try {
            contentLength = (contentLengthHeader == null) ? 0 : ((Integer) contentLengthHeader.getValueObject()).intValue();
        } catch (IllegalValueException e) {
            throw new ParseException(e.getMessage(), -1);
        }
        if (contentLength > 0) {
			int remainingBytes = contentLength;

			StringBuilder sb = new StringBuilder();

			while (remainingBytes > 0) {
				int i = in.read();

				if (i < 0) {
					throw new IOException("Unexpected end of stream (" + remainingBytes + " bytes remaining to be read)");
				}

				char c = (char) i;
				sb.append(c);

				remainingBytes -= utf8Length(c);
			}

			if (remainingBytes < 0) {
				_log.warn("read " + -remainingBytes + " more bytes than we were supposed to!");
			}

			message.setContent(sb.toString());
        }

        return message;
    }


}