package com.github.gwt.stats.client.domain;

public final class Layer {
	public Color background;
	public Color foreground;
	public Layer(){
		this.background = new Color();
		this.foreground = new Color();
	}
	public Layer(int br, int bg, int bb, int fr, int fg, int fb){
		this();
		this.background.set(br, bg, bb);
		this.foreground.set(fr, fg, fb);
	}
}
