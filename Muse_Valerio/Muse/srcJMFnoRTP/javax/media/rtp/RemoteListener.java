/*
 * @(#)RemoteListener.java	1.7 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package javax.media.rtp;

import java.util.EventListener;

import javax.media.rtp.event.RemoteEvent;

/**
 * Interface that generates the callback for all
 * RTPRemoteEvents.RTPRemoteEvents are events that occur from remote
 * participants and could be used for monitoring statistics and quality
 * of the entire RTPSession without knowing about individual streams or
 * participants.  
 */

public interface RemoteListener extends EventListener{
    /** Method called back in the RemoteListener to notify
     * listener of all RTP Remote Events.RemoteEvents are one of
     ReceiverReportEvent, SenderReportEvent or RemoteCollisionEvent
     */
    public void update( RemoteEvent event);
}
    
