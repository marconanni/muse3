/*
 * @(#)CloneableCapturePushDataSource.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.ibm.media.protocol;

import java.io.IOException;

import javax.media.Time;
import javax.media.protocol.CaptureDevice;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceCloneable;

/**
 * This is a utility class for creating clones of PushDataSource. 
 * THe class reflects the functionality of a PushDataSource and provides
 * a getClone() method for generating clones. The generated clone will be of
 * type PushDataSource and its streams will generate a trasferData() call
 * each time the PushDataSource's streams call transferData().
 * This class also implements the CaptureDevice interface.
 */
public class CloneableCapturePushDataSource extends PushDataSource 
implements SourceCloneable, CaptureDevice {

    private SuperCloneableDataSource superClass;
  
    /**
     * Constructor
     *
     * @param source  the source to be cloned
     */
    public CloneableCapturePushDataSource(PushDataSource source) {

	superClass = new SuperCloneableDataSource(source);
    }
  
    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The <code>ContentDescriptor</code>
     * of this <CODE>DataSource</CODE> provides the only indication of
     * what streams can be available on this connection.
     *
     * @return The collection of streams for this source.
     */
    public PushSourceStream[] getStreams() {
    
	if (superClass.streams == null) {
	    superClass.streams = new PushSourceStream[superClass.streamsAdapters.length];
	    for (int i = 0; i < superClass.streamsAdapters.length; i++)
		superClass.streams[i] = (PushSourceStream)superClass.streamsAdapters[i].getAdapter();
	}

	return (PushSourceStream[])superClass.streams;
    }

  
    /**
     * Clone the original datasource, returning an object of the type
     * <code>PushDataSource</code> or <code>PushBufferDataSource</code>. 
     * If the original data source was a
     * PullDataSource, then this will be a PushDataSource which pushes at
     * the same rate at which the CloneableDataSource is being pulled.
     * @return a slave DataSource for this DataSource.
     */
    public DataSource createClone() {
  
	return superClass.createClone();
    }

    /**
     * Get a string that describes the content-type of the media
     * that the source is providing.
     * <p>
     * It is an error to call <CODE>getContentType</CODE> if the source is
     * not connected.
     *
     * @return The name that describes the media content.
     */
    public String getContentType() {
    
	return superClass.getContentType();
    }
  
    /**
     * Open a connection to the source described by
     * the <CODE>MediaLocator</CODE>.
     * <p>
     *
     * The <CODE>connect</CODE> method initiates communication with the source.
     *
     * @exception IOException Thrown if there are IO problems
     * when <CODE>connect</CODE> is called.
     */
    public void connect() throws IOException {
    
	superClass.connect();
    }
  
    /**
     * Close the connection to the source described by the locator.
     * <p>
     * The <CODE>disconnect</CODE> method frees resources used to maintain a
     * connection to the source.
     * If no resources are in use, <CODE>disconnect</CODE> is ignored.
     * If <CODE>stop</CODE> hasn't already been called,
     * calling <CODE>disconnect</CODE> implies a stop.
     *
     */
    public void disconnect() {
    
	superClass.disconnect();
    }
  
    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be
     * called before data is available.
     *(You must call <CODE>connect</CODE> before calling <CODE>start</CODE>.)
     *
     * @exception IOException Thrown if there are IO problems with the source
     * when <CODE>start</CODE> is called.
     */
    public void start() throws IOException {

	superClass.start(); 
    }
  
    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public void stop() throws IOException {
    
	superClass.stop();
    }
  
    /**
     * Obtain the collection of objects that
     * control the object that implements this interface.
     * <p>
     *
     * If no controls are supported, a zero length
     * array is returned.
     *
     * @return the collection of object controls
     */
    public Object[] getControls() {
    
	return superClass.getControls();
    }
  
    /**
     * Obtain the object that implements the specified
     * <code>Class</code> or <code>Interface</code>
     * The full class or interface name must be used.
     * <p>
     * 
     * If the control is not supported then <code>null</code>
     * is returned.
     *
     * @return the object that implements the control,
     * or <code>null</code>.
     */
    public Object getControl(String controlType) {
    
	return superClass.getControl(controlType);
    }

    /**
     * Get the duration of the media represented
     * by this object.
     * The value returned is the media's duration
     * when played at the default rate.
     * If the duration can't be determined  (for example, the media object is presenting live
     * video)  <CODE>getDuration</CODE> returns <CODE>DURATION_UNKNOWN</CODE>.
     *
     * @return A <CODE>Time</CODE> object representing the duration or DURATION_UNKNOWN.
     */
    public Time getDuration() {
    
	return superClass.getDuration();
    }
   /**
   * Return the <code>CaptureDeviceInfo</code> object that describes 
   * this device.
   * @return The <code>CaptureDeviceInfo</code> object that describes 
   * this device.
   */
  
  public javax.media.CaptureDeviceInfo getCaptureDeviceInfo() {
    
    return ((CaptureDevice)superClass.input).getCaptureDeviceInfo();
  }

  /**
   * Returns an array of <code>FormatControl</code> objects.  Each of
   * them can be used to set and get the format of each capture stream. 
   * This method can be used before connect to set and get the
   * capture formats. 
   * @return an array for FormatControls.
   */
  public javax.media.control.FormatControl[] getFormatControls() {
    
    return ((CaptureDevice)superClass.input).getFormatControls();
  }
}
