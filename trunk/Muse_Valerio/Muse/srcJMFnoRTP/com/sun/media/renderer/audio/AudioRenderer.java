/*
 * @(#)AudioRenderer.java	1.70 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */
package com.sun.media.renderer.audio;

import javax.media.Buffer;
import javax.media.Clock;
import javax.media.ClockStoppedException;
import javax.media.Control;
import javax.media.Drainable;
import javax.media.GainControl;
import javax.media.IncompatibleTimeBaseException;
import javax.media.Owned;
import javax.media.PlugIn;
import javax.media.Prefetchable;
import javax.media.Renderer;
import javax.media.Time;
import javax.media.TimeBase;
import javax.media.control.BufferControl;
import javax.media.format.AudioFormat;

import com.sun.media.BasicPlugIn;
import com.sun.media.Log;
import com.sun.media.MediaTimeBase;
import com.sun.media.renderer.audio.device.AudioOutput;


/**
 * AudioRenderer
 * @version 1.23, 98/12/23
 */

public abstract class AudioRenderer extends BasicPlugIn implements Renderer, Prefetchable, Drainable, Clock {

    javax.media.Format supportedFormats[];
    protected javax.media.format.AudioFormat inputFormat;
    protected javax.media.format.AudioFormat devFormat;

    protected AudioOutput device = null;
    protected TimeBase timeBase = null;

    protected boolean started = false;
    protected boolean prefetched = false;
    protected boolean resetted = false;
    protected boolean devicePaused = true;

    protected GainControl gainControl;
    protected BufferControl bufferControl;
    protected Control     peakVolumeMeter = null;

    protected long bytesWritten = 0;
    protected int bytesPerSec;
    
    private Object writeLock = new Object();

    public AudioRenderer() {
        timeBase = new AudioTimeBase(this);
	bufferControl = new BC(this);
    }


    public javax.media.Format [] getSupportedInputFormats() {
	return supportedFormats;
    }

    public javax.media.Format setInputFormat(javax.media.Format format) {
	for (int i = 0; i < supportedFormats.length; i++) {
	    if (supportedFormats[i].matches(format)) {
		inputFormat = (AudioFormat)format;
		return format;
	    }
	}
	return null;
    }


    public void close() {
	stop();
	if (device != null) {
	    pauseDevice();
	    device.flush();
	    mediaTimeAnchor = getMediaNanoseconds();
	    ticksSinceLastReset = 0;
	    device.dispose();
	}
	device = null;
    }

    public void reset() {
	resetted = true;

	// Mark the media time before reset.
	mediaTimeAnchor = getMediaNanoseconds();

	if (device != null) {
	    device.flush();
	    ticksSinceLastReset = device.getMediaNanoseconds();
	} else
	    ticksSinceLastReset = 0;

	prefetched = false;
    }


    synchronized void pauseDevice() {
	if (!devicePaused && device != null) {
	    device.pause();
	    devicePaused = true;
	}
	if (timeBase instanceof AudioTimeBase)
	    ((AudioTimeBase)timeBase).mediaStopped();
    }


    synchronized void resumeDevice() {
	if (timeBase instanceof AudioTimeBase)
	    ((AudioTimeBase)timeBase).mediaStarted();
	if (devicePaused && device != null) {
	    device.resume();
	    devicePaused = false;
	}
    }


    public void start() {
	syncStart(getTimeBase().getTime());
    }

    public synchronized void drain() {
	if (started && device != null)
	    device.drain();
	prefetched = false;
    }

    public int process(Buffer buffer) {
	int rtn = processData(buffer);
	if (buffer.isEOM() && rtn != INPUT_BUFFER_NOT_CONSUMED) {
	    // EOM.
	    drain();
	    pauseDevice();
	}
	return rtn;
    }

    protected boolean checkInput(Buffer buffer) {

	javax.media.Format format = buffer.getFormat();

	// Here comes the real JavaSound processing.

	// Initialize the device if it's not already initialized; or if the
	// input format changes.
	if (device == null || devFormat == null || !devFormat.equals(format)) {
	    if (!initDevice((javax.media.format.AudioFormat)format)) {
		// failed to initialize the device.
		buffer.setDiscard(true);
		return false;
	    }
	    devFormat = (AudioFormat)format;
	}

	return true;
    }

    protected int processData(Buffer buffer) {

	if (!checkInput(buffer))
	    return BUFFER_PROCESSED_FAILED;

	return doProcessData(buffer);
    }

    protected int doProcessData(Buffer buffer) {

	byte data[] = (byte [])buffer.getData();
	int remain = buffer.getLength();
	int off = buffer.getOffset();
	int len = 0;

	synchronized (this) {
	  if (!started) {

	    // If we are not marked as started, we'll pause the device here.
	    if (!devicePaused)
		pauseDevice();

	    // This is in the prefetching state
	    // We'll try to write as much as needed so we won't
	    // block.

	    // We are in prefetching cycle now.  Turn of resetted.
	    resetted = false;

	    int available = device.bufferAvailable();

	    if (available > remain)
		available = remain;

	    if (available > 0) {
		len = device.write(data, off, available);
		bytesWritten += len;
	    }

	    buffer.setLength(remain - len);
	    if (buffer.getLength() > 0 || buffer.isEOM()) {
		buffer.setOffset(off + len);
		prefetched = true;
		return INPUT_BUFFER_NOT_CONSUMED;
	    } else {
		return BUFFER_PROCESSED_OK;
	    }
	  }
	}

	// Guard against pausing the device in the middle of a write
	// thus blocking the entire thread.
	synchronized (writeLock) {

	    if (devicePaused)
		return PlugIn.INPUT_BUFFER_NOT_CONSUMED;

	    try {
		while (remain > 0 && !resetted) {
		    // device.write is blocking.  If the device
		    // has not been started and the device's
		    // internal buffer is filled, then it will block.
		    len = device.write(data, off, remain);
		    bytesWritten += len;
		    off += len; remain -= len;
		}
	    } catch (NullPointerException e) {
		return BUFFER_PROCESSED_OK;
	    }
	}

	buffer.setLength(0);
	buffer.setOffset(0);

	return BUFFER_PROCESSED_OK;
    }

    protected boolean initDevice(AudioFormat format) {

	if (format == null) {
	    System.err.println("AudioRenderer: ERROR: Unknown AudioFormat");
	    return false;
	}

	if (format.getSampleRate() == AudioFormat.NOT_SPECIFIED ||
	    format.getSampleSizeInBits() == AudioFormat.NOT_SPECIFIED) {
	    Log.error("Cannot initialize audio renderer with format: " + format);
	    return false;
	}

	// Close the old device.
	if (device != null) {

	    device.drain();
	    pauseDevice();

	    // Adjust for the media time since the device as well as the
	    // sample count is re-initialized.
	    mediaTimeAnchor = getMediaNanoseconds();
	    ticksSinceLastReset = 0;

	    device.dispose();
	    device = null;
	}

	/*
	System.out.println("sampleRate is " + format.getSampleRate());
	System.out.println("sampleSize is " + sampleSize);
	System.out.println("SamplePerUnit is " + SamplePerUnit);
	System.out.println("channels is " + format.getChannels());
	System.out.println("encoding is " + format.getEncoding());
	System.out.println("bigendian is " + format.isBigEndian());
	System.out.println("signed is " + format.isSigned());
	*/

	// Create AudioPlay based on the format and the current platform.
	AudioFormat audioFormat = new
	    AudioFormat(
		   format.getEncoding(),
		   format.getSampleRate(),
		   format.getSampleSizeInBits(),
		   format.getChannels(),
		   format.getEndian(),
		   format.getSigned()
		 );

	device = createDevice(audioFormat);

	if (device == null || 
	    !device.initialize(audioFormat, computeBufferSize(audioFormat))) {
	    device = null;
	    return false;
	}

        device.setMute(gainControl.getMute());
	device.setGain(gainControl.getDB());

	if (rate != 1.0f) {
	    if (rate != device.setRate(rate)) {
		System.err.println("The AudioRenderer does not support the given rate: " + rate);
		device.setRate(1.0f);
	    }
	}

	if ( started )
	    resumeDevice();

	bytesPerSec = (int)(format.getSampleRate() * format.getChannels() * 
			format.getSampleSizeInBits() / 8);

	return true;
    }

    protected abstract AudioOutput createDevice(AudioFormat format);

    protected void processByWaiting(Buffer buffer) {

	synchronized (this) {
	    // If not yet started, it's in the prefetching state,
	    // do not consume the data bits.
	    if (!started) {
		prefetched = true;
		return;
	    }
	}

	javax.media.format.AudioFormat format =
		(javax.media.format.AudioFormat)buffer.getFormat();

	int sampleRate = (int)format.getSampleRate();
	int sampleSize = format.getSampleSizeInBits();
	int channels = format.getChannels();
	int timeToWait;
	long duration;

	duration = buffer.getLength() * 1000 /
			((sampleSize/8) * sampleRate * channels);
	timeToWait = (int)((float)duration/getRate());
	/*
	System.err.println("sampleSize = " + sampleSize +
		" sampleRate = " + sampleRate +
		" channels = " + channels +
		" length = " + buffer.getLength() +
		" offset = " + buffer.getOffset() +
		" timeToWait = " + timeToWait);
	*/
	try {
	    Thread.currentThread().sleep(timeToWait);
	} catch (Exception e) {}

	buffer.setLength(0);
	buffer.setOffset(0);

	mediaTimeAnchor += duration * 1000000;
    }

    public Object [] getControls() {
	Control c[] = new Control[] { 
				gainControl,
				bufferControl
				};
	return c;
    }



    /////////////////////////
    //
    // Prefetchable methods
    /////////////////////////
    public boolean isPrefetched() {
	return prefetched;
    }



    /////////////////////////
    //
    // Clock methods.
    /////////////////////////

    long mediaTimeAnchor = 0;
    long startTime = Long.MAX_VALUE;
    long stopTime = Long.MAX_VALUE;
    long ticksSinceLastReset = 0;
    float rate = 1.0f;

    // This would be the time base if there's a master set.
    TimeBase master = null;  


    public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException {
	if (!(master instanceof AudioTimeBase)) {
	    Log.warning("AudioRenderer cannot be controlled by time bases other than its own: " + master);
	   /**
	    Silently allows the time base to be set to make
	    addController slightly more useful.
	    --ivg
	    throw new IncompatibleTimeBaseException();
	    */
	}

	this.master = master;
    }

    public synchronized void syncStart(Time at) {
	// It doesn't really do syncStart right now.  It just starts
	// it right away.
	started = true;
	prefetched = true;
	resetted = false;
	resumeDevice();
	startTime = at.getNanoseconds();
    }

    public synchronized void stop() {
	started = false;
	prefetched = false;

	// Guard against pausing in the middle of a write.
	synchronized (writeLock) {
	    pauseDevice();
	}
    }

    public void setStopTime(Time t) {
	stopTime = t.getNanoseconds();
    }

    public Time getStopTime() {
	return new Time(stopTime);
    }

    public void setMediaTime(Time now) {
	mediaTimeAnchor = now.getNanoseconds();
    }

    public Time getMediaTime() {
	return new Time(getMediaNanoseconds());
    }

    public long getMediaNanoseconds() {
	return mediaTimeAnchor + 
		(device != null ? device.getMediaNanoseconds() : 0) - 
		ticksSinceLastReset;
    }

    public long getLatency() {
	long ts = bytesWritten * 1000/bytesPerSec * 1000000;
	return ts - getMediaNanoseconds();
    }

    public Time getSyncTime() {
	return new Time(0);
    }

    public TimeBase getTimeBase() {
	if (master != null)
	    return master;
	else
	    return timeBase;
    }

    public Time mapToTimeBase(Time t) throws ClockStoppedException {
	return new Time((long)((t.getNanoseconds() - mediaTimeAnchor)/rate) + startTime);
    }

    public float getRate() {
	return rate;
    }

    public float setRate(float factor) {
	if (device != null)
	    rate = device.setRate(factor);
	else
	    rate = 1.0f;
	return rate;
    }


    public int computeBufferSize(javax.media.format.AudioFormat f) {

	long bytesPerSecond = (long)(f.getSampleRate() * f.getChannels() * 
					f.getSampleSizeInBits() / 8);
	long bufSize;
	long bufLen;   // in millsec.

	// System.out.println("bytesPerSecond is " + bytesPerSecond);

	if (bufLenReq < DefaultMinBufferSize)
	    bufLen = DefaultMinBufferSize;
	else if (bufLenReq > DefaultMaxBufferSize)
	    bufLen = DefaultMaxBufferSize;
	else
	    bufLen = bufLenReq;

	float r = bufLen/1000f;

	bufSize = (long)(bytesPerSecond * r);

	//System.out.println("Render buffer size: " + bufSize);

	return (int)bufSize;
    }



    //////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //////////////////////////////////////////////////////////////////////////

    class AudioTimeBase extends MediaTimeBase {
	AudioRenderer renderer;

	AudioTimeBase(AudioRenderer r) {
	    renderer = r;
	}

	public long getMediaTime() {

	    // The usual does not require division.
	    if (rate == 1.0f || rate == 0.0f)
		return (device != null ? device.getMediaNanoseconds() : 0);

	    return (long)((device != null ? device.getMediaNanoseconds() : 0) / rate);
	}
    }


    static int DefaultMinBufferSize = 62;	// millisecs.
    static int DefaultMaxBufferSize = 4000;	// millisecs.
    long bufLenReq = 200;

    /**
     * BufferControl for the renderer.
     */
    class BC implements BufferControl, Owned {

	AudioRenderer renderer;

	BC(AudioRenderer ar) {
	   renderer = ar;
	} 
  
	public long getBufferLength() {
	    return bufLenReq;
	} 

	public long setBufferLength(long time) {
	    if (time < DefaultMinBufferSize)
		bufLenReq = DefaultMinBufferSize;
	    else if (time > DefaultMaxBufferSize)
		bufLenReq = DefaultMaxBufferSize;
	    else
		bufLenReq = time;
	    //System.err.println("Render buffer length set: " + bufLenReq);
	    return bufLenReq;
	}
  
	public long getMinimumThreshold() {
	    return 0;
	}

	public long setMinimumThreshold(long time) {
	    return 0;
	}
  
	public void setEnabledThreshold(boolean b) {
	}
  
	public boolean getEnabledThreshold() {
	    return false;
	}

	public java.awt.Component getControlComponent() {
	    return null;
	}

	public Object getOwner() {
	    return renderer;
	}
    }
}
