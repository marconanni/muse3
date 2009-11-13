/*
 * VolumeControl.java
 *
 */
package muse.client.gui;

import javax.media.Control;

public interface VolumeControl extends Control {
	
	public int getLevel();
	public boolean isMuted();
	public int setLevel(int level);
	public void setMute(boolean mute);

}
