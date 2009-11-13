/*
 * @(#)BooleanControlAdapter.java	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */


package com.sun.media.controls;

import java.awt.Component;

import javax.media.Control;

public class BooleanControlAdapter extends AtomicControlAdapter
implements BooleanControl {

    public BooleanControlAdapter() {
	super( null, false, null );
    }
    
    public BooleanControlAdapter(Component c, boolean def, Control parent) {
	super(c, def, parent);
    }

    public boolean setValue(boolean val) {
	// dummy
	return val;
    }

    public boolean getValue() {
	// dummy
	return false;
    }
}
