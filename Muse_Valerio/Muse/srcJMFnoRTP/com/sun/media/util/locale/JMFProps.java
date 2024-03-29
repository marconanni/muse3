/*
 * @(#)JMFProps.java	1.36 03/04/30
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package com.sun.media.util.locale;

public class JMFProps extends java.util.ListResourceBundle  {

    public Object[][] getContents(){
	return contents;
    }

    static final Object[][] contents = {

	{"mediaengine.0_kbps" , "0 kbps"},
	{"mediaengine.unsupported/disabled" , " Unsupported/Disabled"},
	{"mediaengine.stereo" , "stereo"},
	{"mediaengine.mono" , "mono"},
	{"mediaengine.none" , "None"},
	{"mediaengine.khz" , " KHz"},
	{"mediaengine.kbps" , " kbps"},
	{"mediaengine.-bit" , "-bit"},
	{"mediaengine.frames/sec" , " frames/sec"},

	{"mediaplayer.N/A" , " N/A"},
	{"mediaplayer.version" , "Version 2.1.1e"},
	{"mediaplayer.download", "Loading media:"},
	{"mediaplayer.rate", "Rate"},
	{"mediaplayer.rate.tenth", "1/10 speed"},
	{"mediaplayer.rate.half", "Half speed"},
	{"mediaplayer.rate.normal", "Normal speed"},
	{"mediaplayer.rate.double", "Double speed"},
	{"mediaplayer.rate.triple", "Triple speed"},
	{"mediaplayer.rate.quadruple", "Quadruple speed"},

	{"mediaplayer.windowtitle" , "Java Media Player"},
	{"mediaplayer.menu.media", "Media"},
	{"mediaplayer.menu.audio", "Audio"},
	{"mediaplayer.menu.video", "Video"},
	{"mediaplayer.menu.zoom", "Zoom"},
	{"mediaplayer.properties", "Properties"},
	{"mediaplayer.rate.1:4", "Rate 1:4"},
	{"mediaplayer.rate.1:2", "Rate 1:2"},
	{"mediaplayer.rate.1:1", "Rate 1:1"},
	{"mediaplayer.rate.2:1", "Rate 2:1"},
	{"mediaplayer.rate.4:1", "Rate 4:1"},
	{"mediaplayer.rate.8:1", "Rate 8:1"},
	{"mediaplayer.zoom.1:2", "Scale 1:2"},
	{"mediaplayer.zoom.1:1", "Scale 1:1"},
	{"mediaplayer.zoom.2:1", "Scale 2:1"},
	{"mediaplayer.zoom.4:1", "Scale 4:1"},

	{"formatchooser.enabletrack", "Enable Track"},
	{"formatchooser.encoding", "Encoding:"},
	{"formatchooser.samplerate", "Sample Rate:"},
	{"formatchooser.bitspersample", "Bits per Sample:"},
	{"formatchooser.channels", "Channels:"},
	{"formatchooser.endian", "Endian:"},
	{"formatchooser.signed", "Signed"},
	{"formatchooser.videosize", "Video Size:"},
	{"formatchooser.framerate", "Frame Rate:"},
	{"formatchooser.bitsperpixel", "Bits per Pixel:"},
	{"formatchooser.default", "<default>"},
	{"formatchooser.custom", "<custom>"},
	{"formatchooser.yuvtype", "YUV Type:"},
	{"formatchooser.hz", "Hz"},
	{"formatchooser.8bit", "8 bit"},
	{"formatchooser.16bit", "16 bit"},
	{"formatchooser.mono", "mono"},
	{"formatchooser.stereo", "stereo"},
	{"formatchooser.endian.big", "big"},
	{"formatchooser.endian.little", "little"},
	{"formatchooser.yuv.4:2:0", "4:2:0"},
	{"formatchooser.yuv.4:2:2", "4:2:2"},
	{"formatchooser.yuv.YUYV", "YUYV"},
	{"formatchooser.yuv.1:1:1", "1:1:1"},
	{"formatchooser.yuv.4:1:1", "4:1:1"},
	{"formatchooser.yuv.YVU9", "YVU9"},

	//	{"propertysheet.close", "Close"},
	//	{"propertysheet.unknown", "Unknown"},
	{"propertysheet.url", "URL:"},
	{"propertysheet.duration", "Duration:"},
	{"propertysheet.bit.rate", "Bit rate:"},
	{"propertysheet.video", "Video:"},
	{"propertysheet.video.codec", "Encoding:"},
	//	{"propertysheet.video.size", "Size:"},
	{"propertysheet.video.rate", "Rate:"},
	{"propertysheet.audio", "Audio:"},
	{"propertysheet.audio.codec", "Encoding:"},
	{"propertysheet.audio.quality", "Quality:"},
	{"propertysheet.JavaMediaPlayer", "Java Media Player"},
	{"propertysheet.JMF.version", "JMF version:"},
	{"propertysheet.brightness", "brightness:"},
	{"propertysheet.contrast", "contrast:"},
	{"propertysheet.saturation", "saturation:"},
	{"propertysheet.hue", "hue:"},

	{"propertysheet.title", "Media Properties"},
	{"propertysheet.close", "Close"},
	{"propertysheet.kbps", "kbps"},
	{"propertysheet.fps", "fps"},
	{"propertysheet.unknown", "<unknown>"},
	{"propertysheet.unbounded", "<unbounded>"},
	{"propertysheet.tab.general", "General"},
	{"propertysheet.tab.audio", "Audio"},
	{"propertysheet.tab.video", "Video"},
	{"propertysheet.tab.misc", "Plug-in Settings"},
	{"propertysheet.general.medialocation", "Media Location:"},
	{"propertysheet.general.contenttype", "Content Type:"},
	{"propertysheet.general.duration", "Duration:"},
	{"propertysheet.general.position", "Position:"},
	{"propertysheet.general.bitrate", "Bit Rate:"},
	{"propertysheet.general.framerate", "Frame Rate:"},
	{"propertysheet.video.track", "Track #"},
	{"propertysheet.video.encoding", "Encoding"},
	{"propertysheet.video.size", "Size"},
	{"propertysheet.video.framerate", "Frame Rate"},
	{"propertysheet.audio.track", "Track #"},
	{"propertysheet.audio.encoding", "Encoding"},
	{"propertysheet.audio.samplerate", "Sample Rate"},
	{"propertysheet.audio.bitspersample", "Bits per Sample"},
	{"propertysheet.audio.channels", "Channels"},
	{"propertysheet.audio.channels.mono", "mono"},
	{"propertysheet.audio.channels.stereo", "stereo"},

	{"streamingaudiosourcenode.rtp_audio" , "RTP Audio"},
	{"streamingaudiosourcenode.kbps" , " kbps"},
	{"streamingaudiosourcenode.rtp" , "RTP "},
	{"streamingaudiosourcenode.rtp_gsm" , "RTP GSM"},
	{"streamingaudiosourcenode.0_kbps" , "0 kbps"},
	{"streamingaudiosourcenode.rtp_g711" , "RTP G711"},
	{"streamingaudiosourcenode.rtp_g723" , "RTP G723"},
	{"streamingaudiosourcenode.khz" , " KHz"},
	{"streamingsourcenode.rtp" , "RTP"},
	{"streamingsourcenode.kbps" , " kbps"},
	{"streamingsourcenode.rtp_jpeg" , "RTP JPEG"},
	{"streamingsourcenode.rtp_h261" , "RTP H261"},
	{"streamingsourcenode.rtp_h263" , "RTP H263"},
	{"streamingsourcenode.0_kbps" , "0 kbps"},

	{"jmpx.MPEG1-Audio", "MPEG-1 Audio"},
	{"jmpx.MPEG-1", " MPEG-1"},

	{"codec.linear", "Linear"},
	{"codec.unknown", "Unknown"},
	{"codec.ulaw", "G.711 ulaw"},
	{"codec.g711_ulaw", "G.711 ulaw"},
	{"codec.g711_alaw", "G.711 alaw"},
	{"codec.g722", "G.722"},
	{"codec.g723", "G.723"},
	{"codec.g721_adpcm", "G.721 ADPCM"},
	{"codec.dbi_adpcm", "DBI ADPCM"},
	{"codec.oki_adpcm", "OKI ADPCM"},
	{"codec.msadpcm", "MS ADPCM"},
	{"codec.digistd", "digistd"},
	{"codec.digifix", "digifix"},
	{"codec.dvi", "DVI ADPCM"},
	{"codec.ima4", "IMA4 ADPCM"},
	{"codec.linear8", "Linear 8-bit"},
	{"codec.mac6", "Mac 6"},
	{"codec.mac3", "Mac 3"},
	{"codec.gsm", "GSM"},
	{"codec.sx7383", "SX7383"},
	{"codec.cvid", "Cinepak"},
	{"codec.iv32", "Indeo 3.2"},
	{"codec.iv41", "Indeo 4.1"},
	{"codec.iv50", "Indeo 5.0"},
	{"codec.rle", "RLE"},
	{"codec.raw", "None"},
	{"codec.h263", "H.263"},
	{"codec.h261", "H.261"},
	{"codec.mpeg", "MPEG"},
	{"codec.jpeg", "JPEG"},
	{"codec.smc", "SMC"},
	{"error.filenotfound", "File Not Found"},

	/** Don't translate the following strings **/
	{"jmf.properties", "jmf.properties"},
	{"jmf.default.font", "Helvetica"}

    };
}


