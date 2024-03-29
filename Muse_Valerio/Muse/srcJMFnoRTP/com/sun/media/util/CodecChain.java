/*
 * @(#)CodecChain.java	1.11 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.awt.Component;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.Renderer;
import javax.media.ResourceUnavailableException;

import com.sun.media.ExtBuffer;
import com.sun.media.Log;
import com.sun.media.SimpleGraphBuilder;

public class CodecChain {

    static final int STAGES = 5;
    protected Codec [] codecs = null;
    protected Buffer [] buffers = null;
    protected Format [] formats = null;
    protected Renderer renderer = null;

    private boolean deallocated = true;
    protected boolean firstBuffer = true;  // reset this on reset
    private boolean rtpFormat = false;

    boolean isRawFormat(Format format) {
	return false;
    }

    public void reset() {
	firstBuffer = true;
	for (int i = 0; i < codecs.length; i++) {
	    if (codecs[i] != null)
		codecs[i].reset();
	}
    }

    /*
     * Warning, this buffer's attributes might be modified. So save it in the
     * calling routine if necessary.
     */
    public int process(Buffer buffer, boolean render) {
	int codecNo = 0;
	return doProcess(codecNo, buffer, render);
    }

    // Recursive method
    private int doProcess(int codecNo, Buffer input, boolean render) {

	Format format = input.getFormat();
	if (codecNo == codecs.length) {
	    // end of chain, render if necessary
	    if (render) {

		// Check for mid-stream format change.
		if (renderer != null &&
		    formats[codecNo] != null && 
		    formats[codecNo] != format && 
		    !formats[codecNo].equals(format) &&
		    !input.isDiscard()) {
		    // Input format changed.
		    if (renderer.setInputFormat(format) == null) {
			// Format change failed.
			Log.error("Monitor failed to handle mid-stream format change:");
			Log.error("  old: " + formats[codecNo]);
			Log.error("  new: " + format);
			return PlugIn.BUFFER_PROCESSED_FAILED;
		    }

		    // Format change handled successfully.
		    formats[codecNo] = format;
		}

		try {
		    return renderer.process(input);
		} catch (Exception e) {
		    Log.dumpStack(e);
		    return PlugIn.BUFFER_PROCESSED_FAILED;
		} catch (Error err) {
		    Log.dumpStack(err);
		    return PlugIn.BUFFER_PROCESSED_FAILED;
		}
		
	    } else
		return PlugIn.BUFFER_PROCESSED_OK;
	} else {
	    // If raw format, no need to decode just to keep state
	    if (isRawFormat(format)) {
		if (!render) {
		    return PlugIn.BUFFER_PROCESSED_OK;
		}
	    } else {
		// We need to assume these buffers might have Key/NonKey frames
		if (!rtpFormat && firstBuffer) {
		    if ((input.getFlags() & Buffer.FLAG_KEY_FRAME) == 0) {
			return PlugIn.BUFFER_PROCESSED_OK;
		    }
		    firstBuffer = false;
		}
	    }

	    // Decode to render or atleast to keep state
	    // Process this codec
	    Codec codec = codecs[codecNo];
	    int returnVal;

	    // Check for mid-stream format change.
	    if (codec != null &&
		formats[codecNo] != null && 
		formats[codecNo] != format && 
		!formats[codecNo].equals(format) &&
		!input.isDiscard()) {
		// Input format changed.
		if (codec.setInputFormat(format) == null) {
		    // Format change failed.
		    Log.error("Monitor failed to handle mid-stream format change:");
		    Log.error("  old: " + formats[codecNo]);
		    Log.error("  new: " + format);
		    return PlugIn.BUFFER_PROCESSED_FAILED;
		}

		// Format change handled successfully.
		formats[codecNo] = format;
	    }

	    do {
		//System.err.println("format = " + input.getFormat());

		try {
		    returnVal = codec.process(input, buffers[codecNo]);
		} catch (Exception e) {
		    Log.dumpStack(e);
		    return PlugIn.BUFFER_PROCESSED_FAILED;
		} catch (Error err) {
		    Log.dumpStack(err);
		    return PlugIn.BUFFER_PROCESSED_FAILED;
		}
		
		//System.err.println("codecNo: " + codecNo + " return val = " + returnVal);
		if (returnVal == PlugIn.BUFFER_PROCESSED_FAILED)
		    return PlugIn.BUFFER_PROCESSED_FAILED;
		if ((returnVal & PlugIn.OUTPUT_BUFFER_NOT_FILLED) == 0) {
		    //System.err.println("Calling process");
		    if (!(buffers[codecNo].isDiscard() || buffers[codecNo].isEOM()))
			doProcess(codecNo + 1, buffers[codecNo], render);
		    buffers[codecNo].setOffset(0);
		    buffers[codecNo].setLength(0);
		    buffers[codecNo].setFlags(0);
		}
	    } while ((returnVal & PlugIn.INPUT_BUFFER_NOT_CONSUMED) != 0);

	    return returnVal;
	}
    }

    public Component getControlComponent() {
	return null;
    }

    public boolean prefetch() {
	if (!deallocated)
	    return true;
        try {
	    renderer.open();
	} catch (ResourceUnavailableException e) { 
	    return false;
	}
	renderer.start();
	deallocated = false;
	return true;
    }

    public void deallocate() {
	if (deallocated)
	    return;
	if (renderer != null)
	    renderer.close();
	deallocated = true;
    }

    public void close() {
	for (int i = 0; i < codecs.length; i++) {
	    codecs[i].close();
	}
	if (renderer != null)
	    renderer.close();
    }

    protected boolean buildChain(Format input) {

	Vector pluginList;
	Vector formatList = new Vector(10);

	if ((pluginList = SimpleGraphBuilder.findRenderingChain(input, formatList)) == null)
	    return false;

	int len = pluginList.size();
	codecs = new Codec[len-1];
	buffers = new ExtBuffer[len-1];
    	formats = new Format[len];

	formats[0] = input;

	Log.comment("Monitor codec chain:");

	for (int j = 0; j < codecs.length; j++) {
	    codecs[j] = (Codec) pluginList.elementAt(len-j-1);
	    // Output format for each codec.
	    formats[j+1] = (Format) formatList.elementAt(len-j-2);
	    buffers[j] = new ExtBuffer();
	    buffers[j].setFormat(formats[j+1]);
	    Log.write("    codec: " + codecs[j]);
	    Log.write("      format: " + formats[j]);
	}

	renderer = (Renderer)pluginList.elementAt(0);

	Log.write("    renderer: " + renderer);
	Log.write("      format: " + formats[codecs.length] + "\n");

	if (input.getEncoding() != null) {
	    String enc = input.getEncoding().toUpperCase();
	    if (enc.endsWith("RTP"))
		rtpFormat = true;
	}

	return true;
    }
}
