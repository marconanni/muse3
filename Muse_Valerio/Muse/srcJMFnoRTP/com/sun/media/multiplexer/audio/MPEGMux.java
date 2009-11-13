/*
 * @(#)Handler.java	1.4 99/03/22
 *
 * Copyright 1996-1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.media.multiplexer.audio;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;

public class MPEGMux extends com.sun.media.multiplexer.BasicMux {

    public MPEGMux() {
	supportedInputs = new Format[2];
	supportedInputs[0] = new AudioFormat(AudioFormat.MPEGLAYER3);
	supportedInputs[1] = new AudioFormat(AudioFormat.MPEG);
	//System.err.println("MPEGMux.<init>");
	supportedOutputs = new ContentDescriptor[1];
	supportedOutputs[0] = new FileTypeDescriptor(FileTypeDescriptor.MPEG_AUDIO);
    }

    public String getName() {
	return "MPEG Audio Multiplexer";
    }

    public Format setInputFormat(Format input, int trackID) {
	if (!(input instanceof AudioFormat))
	    return null;
	AudioFormat format = (AudioFormat) input;
	double sampleRate =  format.getSampleRate();

	String reason = null;
	double epsilon = 0.25;

	// Check to see if some of these restrictions can be removed
 	if (!format.getEncoding().equalsIgnoreCase(AudioFormat.MPEGLAYER3) &&
	    !format.getEncoding().equalsIgnoreCase(AudioFormat.MPEG))
	    reason = "Encoding has to be MPEG audio";
	/*
	else if ( Math.abs(sampleRate - 8000.0) > epsilon )
	    reason = "Sample rate should be 8000. Cannot handle sample rate " + sampleRate;
 	else if (format.getFrameSizeInBits() != (33*8))
 	    reason = "framesize should be 33 bytes";
	else if (format.getChannels() != 1)
	    reason = "Number of channels should be 1";		
	*/
	if (reason != null) {
	    return null;
	} else {
	    inputs[0] = format;
	    return format;
	}
    }
}




