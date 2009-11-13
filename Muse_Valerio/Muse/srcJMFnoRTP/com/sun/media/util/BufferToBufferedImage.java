/*
 * @(#)BufferToBufferedImage.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import javax.media.Buffer;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;

public class BufferToBufferedImage extends javax.media.util.BufferToImage {


    public BufferToBufferedImage() throws ClassNotFoundException {
	super(null);
	Class.forName("java.awt.Graphics2D");
    }
    
    public BufferToBufferedImage(VideoFormat format) throws ClassNotFoundException {
	super(format);
	Class.forName("java.awt.Graphics2D");
    }

    public Image createImage(Buffer buffer) {
	RGBFormat format = (RGBFormat) buffer.getFormat();
	int rMask, gMask, bMask;
	Object data = buffer.getData();
	DirectColorModel dcm;

	rMask = format.getRedMask();
	gMask = format.getGreenMask();
	bMask = format.getBlueMask();
	int [] masks = new int[3];
	masks[0] = rMask;
	masks[1] = gMask;
	masks[2] = bMask;

	DataBuffer db = new DataBufferInt((int[])data,
					  format.getLineStride() *
					  format.getSize().height);

	SampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
							  format.getLineStride(),
							  format.getSize().height,
							  masks);
	WritableRaster wr = Raster.createWritableRaster(sm, db, new Point(0, 0));

	dcm = new DirectColorModel(24, rMask, gMask, bMask);
	BufferedImage sourceImage = new BufferedImage((ColorModel)dcm, wr, true, null);
	
	AffineTransform at = new AffineTransform(1.0f, 0,
						 0   , 1.0f,
						 0   , 0);
	AffineTransformOp ato = new AffineTransformOp(at, null);

	BufferedImage outputImage = ato.createCompatibleDestImage((BufferedImage)sourceImage,
								  (ColorModel)dcm);
	

	ato.filter(sourceImage, outputImage);
	//System.err.println("Filtered");
	return outputImage;
    }
}


