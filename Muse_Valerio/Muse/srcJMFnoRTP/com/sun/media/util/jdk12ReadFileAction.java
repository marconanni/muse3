/*
 * @(#)jdk12ReadFileAction.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.io.FileInputStream;
import java.lang.reflect.Constructor;

public
class jdk12ReadFileAction  implements java.security.PrivilegedAction {

    public static Constructor cons;
    private String name;

    static {
	try {
	    cons = jdk12ReadFileAction.class.getConstructor(new Class[] {
		String.class});
	} catch (Throwable e) {
	}
    }


    public jdk12ReadFileAction(String name) {
	
	try {
	    this.name = name;
	} catch (Throwable e) {
	}
    }

    public Object run() {
	try {
	    return new FileInputStream(name);
	} catch (Throwable t) {
	    return null;
	}
    }

}
