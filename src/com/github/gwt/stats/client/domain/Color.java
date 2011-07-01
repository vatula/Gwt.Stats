package com.github.gwt.stats.client.domain;

public final class Color {
	public int r = 0;
	public int g = 0;
	public int b = 0;
	
	public Color(){
	}
	public Color(int r, int g, int b){
		this.set(r,g,b);
	}
	
	public void set(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
}
