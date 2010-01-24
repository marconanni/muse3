/*
 * @(#)ImageGen.java	1.3 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;



/**
 * A simple utility to turn binary images into java code which can
 * then be compiled into classes.
 * To run:
 *	% javac ImageGen.java
 *	% java ImageGen [-d <destdir>] <imagelib> <image1> <image2> ...
 * The output is a file call imagelib.java at directory <destdir>
 *
 * enhanced by Shay Ben-David (bendavid@haifa.vnet.ibm.com) to reduce the footprint
 * of the generated code.
 */
public class ImageGen {

    static String arname;
    static String pkgname;
    static String destdir;
    static String names[];
    static byte[][] images;
    static DataOutputStream ds;  // I prefer dos to fos because it have write string method

    public static void main(String[] args) {
	int i, j;

	// Parse the arguments.
	names = new String[args.length+1];
	images = new byte[args.length+1][];
	for (i = 0, j = 0; i < args.length; i++) {
	    if (args[i].equals("-d")) {
		if (i++ >= args.length) {
		    printUsage();
		    return;
		}
		destdir = args[i];
	    } else
		names[j++] = args[i];
	}
	names[j] = null;

	if (j == 0) {
	    printUsage();
	    return;
	}

	// Determines the package and library name.
	i = names[0].lastIndexOf(".");
	if (i == -1) {
	    pkgname = null;
	    arname = names[0];
	} else {
	    pkgname = names[0].substring(0, i);
	    arname = names[0].substring(i + 1);
	}

	// Opens the destination file.
	String filename = null;
	try {
	    if (destdir == null)
		filename = arname + ".java";
	    else
		filename = destdir + File.separator + arname + ".java";
	    ds = new DataOutputStream(new FileOutputStream(filename));
	} catch (IOException e) {
	    System.err.println("Cannot open file: " + filename + e);
	}

	if (j == 1) {
	    // No file is specified.  Generate just the ImageLib interface.
	    writeInterface();
	} else {
	    // Write the imagelib class.
	    writeClass();
	}
    }

    static void printUsage() {
       System.err.println("java ImageGen [-d <destdir>] <imagelib> image1 image2 ...");
    }

    static void writeInterface() {
	try {
	    ds.writeBytes("/* Generated by ImageGen.\n   DO NOT EDIT.*/\n\n");
	    if (pkgname != null) {
		ds.writeBytes("package ");
		ds.writeBytes(pkgname);
		ds.writeBytes(";\n\n");
	    }
	    ds.writeBytes("public interface ImageLib {\n");
	    ds.writeBytes("    public byte[] getImage(String name);\n");
	    ds.writeBytes("}\n");
	} catch (IOException e) {
	}
    }

    static void writeClass() {
	int i,j;
	int accBytes=0;
	String name;
	try {
	    readImages();

	    ds.writeBytes("/* Generated by ImageGen.\n   DO NOT EDIT.*/\n\n");
	    if (pkgname != null) {
 		  ds.writeBytes("package ");
		  ds.writeBytes(pkgname);
		  ds.writeBytes(";\n\n");
	    }


	    ds.writeBytes("public abstract class ");
	    ds.writeBytes(arname);
	    ds.writeBytes(" {\n\n");
        ds.writeBytes("    private static byte[] m(int from,int to){\n");
        ds.writeBytes("       int i;\n");
        ds.writeBytes("       byte[] b= new byte[to - from];\n");
        ds.writeBytes("       for (i=0;i<b.length;i++)\n");
        ds.writeBytes("          b[i] = (byte)(s.charAt(i+from)-1);\n");

        ds.writeBytes("       return b;\n");
        ds.writeBytes("    }\n");

	    ds.writeBytes("    private static String s = \n        ");



        for (i = 1; names[i] != null; i++) {
          ds.writeBytes("\"");
          int len= images[i].length;
 	      for (j = 0; j < len; j++) {
	        ds.writeBytes( ("\\"+byte2oct((byte)(1+images[i][j]))) );
	        if ((j%16)==15) {
	           ds.writeBytes("\"+\n        \""  );
	        }
	      }
          ds.writeBytes("\""  );

    	  if (names[i+1]!=null)
	          ds.writeBytes("+\n        "  );
        }
	    ds.writeBytes(";\n\n");
	    ds.writeBytes("    public static byte[] getImage(String name) {\n");
	    for (i = 1; names[i] != null; i++) {
   		  ds.writeBytes("        if (name.equals(\"" + fileName(names[i]) + "\"))\n" );
  		  ds.writeBytes("            return m("+accBytes+"," + (accBytes += images[i].length) + ");\n");
	    }
	    ds.writeBytes("        return null;\n");
	    ds.writeBytes("    }\n\n");

	    ds.writeBytes("}\n"); //trailer
	} catch (IOException e) {
	}

    }


    static void readImages() {
      FileInputStream fi;
	  int len, b,i;
      for (i = 1; names[i] != null; i++) {
         String imageFile=names[i];
         try {
           fi = new FileInputStream(imageFile);
	       len = fi.available();
	       images[i]=new byte[len];
	       fi.read(images[i]);
         } catch (IOException e) {
	       System.err.println("Cannot open image file: " + imageFile);
	     }
      }
    }

    // return the file name (without path)
    static private String fileName(String name) {
	  int i = name.lastIndexOf(File.separator);
   	  return name.substring(i+1);
    }

    // convert byte to its octal presentation (always 3 characters)
    private static String byte2oct(byte b) {
      int i=b&0xff;
      int dig3=i%8;
      int dig2=(i/8)%8;
      int dig1=i/64;
      return (""+dig1+""+dig2+""+dig3);
    }

}
