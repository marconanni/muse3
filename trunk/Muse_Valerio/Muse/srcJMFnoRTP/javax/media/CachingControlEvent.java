/*
 * @(#)CachingControlEvent.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

/**
 * This event is generated by a <CODE>Controller</CODE> that supports
 * the <code>CachingControl</code> interface.  It is posted when the caching 
 * state changes.
 *
 * @see Controller
 * @see ControllerListener
 * @see CachingControl
 * @version 1.2, 02/08/21.
 */
public class CachingControlEvent extends ControllerEvent {

    CachingControl control;
    long progress;
    
    /**
     * Construct a <CODE>CachingControlEvent</CODE> from the required elements.
     */
    public CachingControlEvent(Controller from, CachingControl cacheControl,
			       long progress) {
	super(from);
	control = cacheControl;
	this.progress = progress;
    }

    /**
     * Get the <code>CachingControl</code> object that generated
     * the event.
     *
     * @return The <code>CachingControl</code> object.
     */

    public CachingControl getCachingControl() {
	return control;
    }
    
    /**
     * Get the total number of bytes of media data that have been downloaded so far.
     *
     * @return The number of bytes of media data downloaded.
    */
    public long getContentProgress() {
	return progress;
    }

    /**
     * Returns the String representation of this event's values.
     */
    public String toString() {
	return getClass().getName() + "[source=" + eventSrc + 
	    ",cachingControl=" + control + 
	    ",progress=" + progress + "]";
    }
}