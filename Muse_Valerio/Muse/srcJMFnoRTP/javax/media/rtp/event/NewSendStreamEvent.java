/*
 * @(#)NewSendStreamEvent.java	1.6 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp.event;

import javax.media.rtp.SendStream;
import javax.media.rtp.SessionManager;

/**
 * Informs the RTP listener that a new transmitting stream has been
 * created in this SessionManager.  
 */
public class NewSendStreamEvent extends SendStreamEvent{
    public NewSendStreamEvent(SessionManager from,
			      SendStream sendStream){
	super(from, sendStream, sendStream.getParticipant());
    }
}
