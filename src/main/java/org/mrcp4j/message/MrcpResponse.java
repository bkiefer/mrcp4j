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
package org.mrcp4j.message;

/**
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class MrcpResponse extends MrcpServerMessage {

/*
    Success Codes
    +-----------+-------------------------------------------------------+
    | Code      | Meaning                                               |
    +-----------+-------------------------------------------------------+
    | 200       | Success                                               |
    | 201       | Success with some optional headers ignored            |
    +-----------+-------------------------------------------------------+
*/
    public static final short STATUS_SUCCESS = 200;
    public static final short STATUS_SUCCESS_SOME_OPTIONAL_HEADERS_IGNORED = 201;

/*
    Client Failure 4xx Codes
    +-----------+-------------------------------------------------------+
    | Code      | Meaning                                               |
    +-----------+-------------------------------------------------------+
    | 401       | Method not allowed                                    |
    | 402       | Method not valid in this state                        |
    | 403       | Unsupported Header                                    |
    | 404       | Illegal Value for Header                              |
    | 405       | Resource not allocated for this session or does not   |
    |           | exist                                                 |
    | 406       | Mandatory Header Missing                              |
    | 407       | Method or Operation Failed (e.g., Grammar compilation |
    |           | failed in the recognizer. Detailed cause codes MAY BE |
    |           | available through a resource specific header.)        |
    | 408       | Unrecognized or unsupported message entity            |
    | 409       | Unsupported Header Value                              |
    | 410       | Non-Monotonic or Out of order sequence number in      |
    |           | request.                                              |
    | 411-420   | Reserved                                              |
    +-----------+-------------------------------------------------------+
*/
    public static final short STATUS_METHOD_NOT_ALLOWED = 401;
    public static final short STATUS_METHOD_NOT_VALID_IN_STATE = 402;
    public static final short STATUS_UNSUPPORTED_HEADER = 403;
    public static final short STATUS_ILLEGAL_VALUE_FOR_HEADER = 404;
    public static final short STATUS_RESOURCE_NOT_ALLOCATED = 405;
    public static final short STATUS_MANDATORY_HEADER_MISSING = 406;
    public static final short STATUS_OPERATION_FAILED = 407;
    public static final short STATUS_UNRECOGNIZED_MESSAGE_ENTITY = 408;
    public static final short STATUS_UNSUPPORTED_HEADER_VALUE = 409;
    public static final short STATUS_NON_MONOTONIC_SEQUENCE_NUMBER = 410;

/*
    Server Failure 4xx Codes
    +-----------+-------------------------------------------------------+
    | Code      | Meaning                                               |
    +-----------+-------------------------------------------------------+
    | 501       | Server Internal Error                                 |
    | 502       | Protocol Version not supported                        |
    | 503       | Proxy Timeout. The MRCP Proxy did not receive a       |
    |           | response from the MRCP server.                        |
    | 504       | Message too large                                     |
    +-----------+-------------------------------------------------------+
*/
    public static final short STATUS_SERVER_INTERNAL_ERROR = 501;
    public static final short STATUS_PROTOCOL_VERSION_NOT_SUPPORTED = 502;
    public static final short STATUS_PROXY_TIMEOUT = 503;
    public static final short STATUS_MESSAGE_TOO_LARGE = 504;


    
    private short _statusCode = -1;

    public void setStatusCode(short statusCode) {
        _statusCode = statusCode;
    }

    public short getStatusCode() {
        return _statusCode;
    }

    @Override
    protected final StringBuilder appendStartLine(StringBuilder sb) {
        sb.append(getVersion());
        sb.append(' ').append(getMessageLength());
        sb.append(' ').append(getRequestID());
        sb.append(' ').append(getStatusCode());
        sb.append(' ').append(getRequestState());
        sb.append(CRLF);
        return sb;
    }

}