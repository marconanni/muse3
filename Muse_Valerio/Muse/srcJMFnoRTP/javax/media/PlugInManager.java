/*
 * @(#)PlugInManager.java	1.20 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package javax.media;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

/**
 * The <CODE>PlugInManager</CODE> is used to search for installed plug-ins and
 * register new plug-ins. <P>
 * <H3>Plug-in Types</H3>
 * JMF defines several types of plug-ins, such as codecs, demultiplexers, and renderers.
 * Custom plug-in types can also be registered. 
 * The predefined plug-in types are: 
 * <ul>
 * <li>DEMULTIPLEXER = 1<BR></li>
 * <li>CODEC = 2<BR></li>
 * <li>EFFECT = 3<BR></li>
 * <li>RENDERER = 4<BR></li>
 * <li>MULTIPLEXER = 5<BR></li>
 * </ul>
 * <p>
 * This <CODE>PlugInManager</CODE> is a wrapper for the actual implementation, which it
 * expects to find in <CODE>javax.media.pim.PlugInManager</CODE>. If this implementation
 * exists and is an instance of <CODE>javax.media.PlugInManager</CODE>, all calls to
 * <CODE>javax.media.PlugInManager</CODE> are redirected to <CODE>javax.media.pim.PlugInManager</CODE>.
 * If the implementation is not found, all calls to <CODE>javax.media.PlugInManager</CODE> methods 
 * will fail and return null or invalid data.
 * @since JMF 2.0
 */
public class PlugInManager {

    private static PlugInManager pim = null;

    /** Demultiplexer plug-in type. */
    public static final int DEMULTIPLEXER = 1;
    /** Codec plug-in type. */
    public static final int CODEC         = 2;
    /** Effect plug-in type. */
    public static final int EFFECT        = 3;
    /** Renderer plug-in type. */
    public static final int RENDERER      = 4;
    /** Multiplexer plug-in type. */
    public static final int MULTIPLEXER   = 5;

    private static Method mGetPlugInList = null;
    private static Method mSetPlugInList = null;
    private static Method mCommit        = null;
    private static Method mAddPlugIn     = null;
    private static Method mRemovePlugIn  = null;
    private static Method mGetSupportedInputFormats = null;
    private static Method mGetSupportedOutputFormats = null;
    private static Format [] emptyFormat = new Format[0];

    static {
	// Look for javax.media.pim.PlugInManager
	Class classPIM = null;
	try {
	    classPIM = Class.forName("javax.media.pim.PlugInManager");
	    if (classPIM != null) {
		Object tryPIM = classPIM.newInstance();
		if (tryPIM instanceof PlugInManager) {
		    pim = (PlugInManager) tryPIM;

 		    mGetSupportedInputFormats =
 			PackageManager.getDeclaredMethod(classPIM, "getSupportedInputFormats",
 				     new Class[] {String.class, int.class});


 		    mGetSupportedOutputFormats =
 			PackageManager.getDeclaredMethod(classPIM, "getSupportedOutputFormats",
 				     new Class[] {String.class, int.class});



		    mGetPlugInList =
			PackageManager.getDeclaredMethod(classPIM, "getPlugInList",
				     new Class[] { Format.class,
						   Format.class,
						  int.class} );

		    mSetPlugInList = 
			PackageManager.getDeclaredMethod(classPIM, "setPlugInList",
				     new Class[] {Vector.class, int.class});

 		    mAddPlugIn =
 			PackageManager.getDeclaredMethod(classPIM, "addPlugIn",
 				     new Class[] {String.class,
						  Format.formatArray,
						  Format.formatArray,
						  int.class});

 		    mRemovePlugIn =
 			PackageManager.getDeclaredMethod(classPIM, "removePlugIn",
 				     new Class[] {String.class, int.class});

		    mCommit =
 			PackageManager.getDeclaredMethod(classPIM, "commit", null);
		}
	    }
	} catch (ClassNotFoundException e) {
	    System.err.println(e);
	} catch (InstantiationException e) {
	    System.err.println(e);
	} catch (IllegalAccessException e) {
	    System.err.println(e);
	} catch (SecurityException e) {
	    System.err.println(e);
	} catch (NoSuchMethodException e) {
	    System.err.println(e);
	}
    }

    static Object runMethod(Method m, Object [] params) {
	try {
	    return m.invoke(null, params);
	} catch (IllegalAccessException iae) {
	    System.err.println(iae);
	} catch (IllegalArgumentException iare) {
	    System.err.println(iare);
	} catch (InvocationTargetException ite) {
	    System.err.println(ite);
	}
	return null;
    }

    /**
     * Builds a list of plug-ins that satisfy the specified plug-in type and input and
     * output formats. Either or both of the formats can be null. 
     * If <code>input</code> is null, <CODE>getPlugInList</CODE>
     * returns a list of plug-ins of the specified type that match the output format.  If <code>output</code> is 
     * null, <CODE>getPlugInList</CODE> returns a list of plug-ins of the specified type that match the input format.
     * If both parameters are null, <CODE>getPlugInList</CODE> returns a list of all of the 
     * plug-ins of the specified type.
     * @param input The input <CODE>Format</CODE> to be supported by the plug-in.
     * @param output The output <CODE>Format</CODE> to be generated by the plug-in.
     * @param type The type of plug-in to search for, for example: 
     * <CODE>DEMULTIPLEXER</CODE>, <CODE>CODEC</CODE>, <CODE>EFFECT</CODE>, 
     * <CODE>MULTIPLEXER</CODE>, or <CODE>RENDERER</CODE>.
     * @return A <CODE>Vector</CODE> that contains the plug-in list.
     */
    public static Vector getPlugInList(Format input, Format output, int type) {
	if (pim != null && mGetPlugInList != null) {
	    Object params[] = new Object[3];
	    params[0] = input;
	    params[1] = output;
	    params[2] = new Integer(type);
	    return (Vector) runMethod(mGetPlugInList, params);
	} else
	    return new Vector(1);
    }

    /**
     * Sets the search order for the plug-ins of the specified type. This enables
     * you to control what plug-in is selected when multiple plug-ins of a particular 
     * type support the same input and output formats. (The first match is selected by
     * the <CODE>Processor</CODE>.)
     * This list is valid
     * for the duration of the session only, unless <code>commit</code> is
     * called.
     * @param plugins A <CODE>Vector</CODE> that lists the plug-ins in the order that they
     * should be searched. 
     * @param type The type of plug-in contained in the search list, for example: 
     * <CODE>DEMULTIPLEXER</CODE>, <CODE>CODEC</CODE>, <CODE>EFFECT</CODE>, 
     * <CODE>MULTIPLEXER</CODE>, or <CODE>RENDERER</CODE>.
      * @see #commit
     */
    public static void setPlugInList(Vector plugins, int type) {
	if (pim != null && mSetPlugInList != null) {
	    Object params[] = new Object[2];
	    params[0] = plugins;
	    params[1] = new Integer(type);
	    runMethod(mSetPlugInList, params);
	}
    }

    /**
     * Commits any changes made to the plug-in list. 
     * The <CODE>commit</CODE> method must be called when a plug-in is added or removed
     * to make the change permanent.
     * Changes to the search
     * order can also be made permanent by calling <CODE>commit</CODE>.
     */
    public static void commit() throws java.io.IOException {
	if (pim != null && mCommit != null) {
	    runMethod(mCommit, null);
	}
    }

    /**
     * Registers a new plug-in. This plug-in is automatically appended
     * to the list of plug-ins searched when a <CODE>Processor</CODE> is created. 
     * Registration will fail if a plug-in of the same name
     * already exists. The <code>commit</code> method has to be called to make the
     * addition permanent.
     * @param classname A <CODE>String</CODE> that contains the class name of the new plug-in.
     * @param in A <CODE>Format</CODE> array that contains the input formats that the plug-in supports.
     * @param out A <CODE>Format</CODE> array that contains the output formats that the plug-in supports.
     * @param type The type of the new plug-in, for example: 
     * <CODE>DEMULTIPLEXER</CODE>, <CODE>CODEC</CODE>, <CODE>EFFECT</CODE>, 
     * <CODE>MULTIPLEXER</CODE>, or <CODE>RENDERER</CODE>.     
     * @return <CODE>true</CODE> if the plug-in is registered successfully, <CODE>false</CODE> if it could
     * not be registered. 
     */
    public static boolean addPlugIn(String classname, Format [] in,
				    Format [] out, int type ) {
	if (pim != null && mAddPlugIn != null) {
	    Object params[] = new Object[4];
	    params[0] = classname;
	    params[1] = in;
	    params[2] = out;
	    params[3] = new Integer(type);
	    Object result = runMethod(mAddPlugIn, params);
	    if (result != null)
		return ((Boolean)result).booleanValue();
	    else
		return false;
	} else
	    return false;
    }

    /**
     * Removes an existing plug-in from the registry. The <code>commit</code> method has
     * to be called to make this change permanent.
     * @param classname A <CODE>String</CODE> that contains the class name of the plug-in to be removed.
     * @param type The type of the new plug-in, for example: 
     * <CODE>DEMULTIPLEXER</CODE>, <CODE>CODEC</CODE>, <CODE>EFFECT</CODE>, 
     * <CODE>MULTIPLEXER</CODE>, or <CODE>RENDERER</CODE>.     
     * @return <CODE>true</CODE> if the plug-in is succesfully removed, <CODE>false</CODE> if
     * no plug-in with the specified name could be found. 
     */
    public static boolean removePlugIn(String classname, int type) {
	if (pim != null && mRemovePlugIn != null) {
	    Object params[] = new Object[2];
	    params[0] = classname;
	    params[1] = new Integer(type);
	    Object result = runMethod(mRemovePlugIn, params);
	    if (result != null)
		return ((Boolean)result).booleanValue();
	    else
		return false;
	} else
	    return false;
    }

    /**
     * Gets a list of the input formats that the specified plug-in supports.
     * @param className The plug-in class name. For example: <CODE>com.sun.media.codec.MPEG</CODE>
     * @param type The type of the specified plug-in, for example: 
     * <CODE>DEMULTIPLEXER</CODE>, <CODE>CODEC</CODE>, <CODE>EFFECT</CODE>, 
     * <CODE>MULTIPLEXER</CODE>, or <CODE>RENDERER</CODE>.
     * @return An array of <CODE>Format</CODE> objects that the specified plug-in can accept
     * as input. Returns an array of zero elements if
     * specified plug-in is not registered or has no inputs.
     */
    public static Format [] getSupportedInputFormats(String className, int type) {
	if (pim != null && mGetSupportedInputFormats != null) {
	    Object params[] = new Object[2];
	    params[0] = className;
	    params[1] = new Integer(type);
	    Object result = runMethod(mGetSupportedInputFormats, params);
	    return (Format[]) result;
	} else
	    return emptyFormat;
    }

    /**
     * Gets a list of the output formats that the specified plug-in supports.
     * @param className The plug-in class name. For example: <CODE>com.sun.media.codec.MPEG</CODE>
     * @param type The type of the specified plug-in, for example: 
     * <CODE>DEMULTIPLEXER</CODE>, <CODE>CODEC</CODE>, <CODE>EFFECT</CODE>, 
     * <CODE>MULTIPLEXER</CODE>, or <CODE>RENDERER</CODE>.
     * @return An array of <CODE>Format</CODE> objects that the specified plug-in can generate
     * as output. Returns an array of zero elements if
     * specified plug-in is not registered or has no outputs.
      */
    public static Format [] getSupportedOutputFormats(String className, int type) {
	if (pim != null && mGetSupportedOutputFormats != null) {
	    Object params[] = new Object[2];
	    params[0] = className;
	    params[1] = new Integer(type);
	    Object result = runMethod(mGetSupportedOutputFormats, params);
	    return (Format[]) result;
	} else
	    return emptyFormat;
    }
}
