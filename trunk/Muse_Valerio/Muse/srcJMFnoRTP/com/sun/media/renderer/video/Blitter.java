/*
 * @(#)Blitter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.renderer.video;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;

import javax.media.Buffer;

public interface Blitter {

    int newData(Buffer buffer,
		Vector cacheInputImage,
		Vector cacheOutputImage,
		Vector cacheInputData);

    Image process(Buffer buffer,
		  Object cacheInputImage,
		  Object cacheOutputImage,
		  Dimension size);

    void draw(Graphics g, Component component, Image lastImage,
	      int dx, int dy, int dw, int dh,
	      int sx, int sy, int sw, int sh);
    
    void resized(Component c);
}
    

