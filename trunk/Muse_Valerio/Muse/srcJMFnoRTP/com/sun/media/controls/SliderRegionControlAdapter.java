/*
 * @(#)SliderRegionControlAdapter.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.controls;

import java.awt.Component;

import javax.media.Control;

public class SliderRegionControlAdapter extends AtomicControlAdapter
implements SliderRegionControl {

    long min, max;
    boolean enable;
    
    public SliderRegionControlAdapter() {
	super(null, true, null);
	enable = true;
    }

    public SliderRegionControlAdapter(Component c, boolean def, 
				      Control parent) {
	super(c, def, parent);
    }

    public long setMinValue(long value) {
	// this.min = value / 1000000L;
	this.min = value;
	informListeners();
	return min;
    }

    public long getMinValue() {
	return min;
    }

    public long setMaxValue(long value) {
	//	this.max = value / 1000000L;
	this.max = value;
	informListeners();
	return max;
    }

    public long getMaxValue() {
	return max;
    }

    public boolean isEnable() {
	return enable;
    }

    public void setEnable(boolean f) {
	enable = f;
    }
}
