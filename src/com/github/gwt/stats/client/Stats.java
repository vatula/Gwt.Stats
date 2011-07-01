package com.github.gwt.stats.client;
import com.github.gwt.stats.client.domain.Color;
import com.github.gwt.stats.client.domain.Layer;

import java.util.Date;
import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.DOM;

public class Stats {
	static class Monitor {
		public Element div;
		public Element text;
		public CanvasElement canvas;
		public Context2d context;
		public ImageData imageData;
		public Monitor(){}
		public Monitor(Element div, Element text, CanvasElement canvas, Context2d context, ImageData imageData){
			this.div = div;
			this.text = text;
			this.canvas = canvas;
			this.context = context;
			this.imageData = imageData;
		}
	}
	private long time = new Date().getTime(),
	timeLastFrame = time,
	timeLastSecond = time;
	
	private int mode = 0,
	modesCount = 2,
	frames = 0,
	fps = 0,
	fpsMin = 1000,
	fpsMax = 0,
	ms, msMin = 1000,
	msMax = 0,
	mb = 0,
	mbMin = 1000,
	mbMax = 0;
	
	private final String FPS = "fps";
	private final String MS = "ms";
	private final String MB = "mb";

	
	private HashMap<String, Layer> colors = Maps.<String, Layer>newHashMap();
	private Element container, fpsDiv, fpsText, msDiv, msText, mbDiv, mbText;
	private CanvasElement fpsCanvas, msCanvas, mbCanvas;
	private Context2d fpsContext, msContext, mbContext;
	private ImageData fpsImageData, msImageData, mbImageData;
	
	private static native boolean hasPerformanceObject()/*-{
		return $wnd.performance && $wnd.performance.memory && $wnd.performance.memory.totalJSHeapSize;
	}-*/;
	
	private static native double getUsedJSHeapSize()/*-{
		return $wnd.performance.memory.usedJSHeapSize;
	}-*/;
	
	private void checkMB(){
		try {
			if (hasPerformanceObject()){
				this.modesCount = 3;
			}
		} catch(Exception e){}
	}
	
	private Monitor init(String prefix, Element initializedContainer) {
		Element div = createDiv(prefix);
		initializedContainer.appendChild(div);
		Element text = createText(prefix);
		div.appendChild(text);
		CanvasElement canvas = createCanvas();
		div.appendChild(canvas);
		Context2d context = createContext(prefix, canvas);
		ImageData imageData = context.getImageData(0, 0, canvas.getWidth(), canvas.getHeight());
		return new Monitor(div, text, canvas, context, imageData);
	}
	private Context2d createContext(String prefix, CanvasElement canvas){
		Context2d context = canvas.getContext2d();
		Layer fc = this.colors.get(prefix.toLowerCase());
		int r = fc.background.r,
		g = fc.background.g,
		b = fc.background.b;
		context.setFillStyle("rgb("+r+","+g+","+b+")");
		context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		return context;
	}

	private CanvasElement createCanvas(){
		CanvasElement canvas = Canvas.createIfSupported().getCanvasElement();
		canvas.setWidth(74);
		canvas.setHeight(30);
		Style cs = canvas.getStyle();
		cs.setDisplay(Display.BLOCK);
		cs.setMarginLeft(3, Unit.PX);
		return canvas;
	}
	
	private Element createText(String prefix){
		Element text = DOM.createDiv();
		Style fs = text.getStyle();
		fs.setProperty("fontFamily", "Helvetica, Arial, sans-serif");
		fs.setProperty("textAlign", "left");
		fs.setFontSize(9, Unit.PX);
		Layer fc = this.colors.get(prefix.toLowerCase());
		int r = fc.foreground.r,
		g = fc.foreground.g,
		b = fc.foreground.b;
		fs.setColor("rgb("+r+","+g+","+b+")");
		fs.setMarginBottom(1, Unit.PX);
		fs.setMarginLeft(3, Unit.PX);
		text.setInnerHTML("<span style=\"font-weight:bold\">"+prefix.toUpperCase()+"</span>");
		return text;
	}
	
	private Element createDiv(String prefix){
		Element div = DOM.createDiv();
		Style fs = div.getStyle();
		Layer fc = this.colors.get(prefix.toLowerCase());
		int r = (int) Math.floor(fc.background.r/2),
		g = (int) Math.floor(fc.background.g/2),
		b = (int) Math.floor(fc.background.b/2);
		fs.setBackgroundColor("rgb("+r+","+g+","+b+")");
		fs.setPaddingTop(2, Unit.PX);
		fs.setPaddingBottom(3, Unit.PX);
		return div;
	}
	
	private Element createContainer(){
		Element container = DOM.createDiv();
		Style cs = container.getStyle();
		cs.setCursor(Cursor.POINTER);
		cs.setWidth(80, Unit.PX);
		cs.setOpacity(.9);
		cs.setZIndex(10001);
		DOM.sinkEvents(container, Event.ONCLICK);
		DOM.setEventListener(container, new EventListener() {
			@Override
			public void onBrowserEvent(Event event) {
				switch (DOM.eventGetType(event)){
				case Event.ONCLICK:
					swapMode();
					break;
				}
			}
		});
		return container;
	}
	
	private void updateGraph(CanvasPixelArray data, int value, String prefix){
		int x, y, index;
		for (y = 0; y < 30; y++) {
			for (x = 0; x < 73; x++) {
				index = (x + y*74)*4;
				data.set(index, data.get(index+4));
				data.set(index+1, data.get(index+5));
				data.set(index+2, data.get(index+6));
			}
		}

		for (y = 0; y < 30; y++) {
			index = (73 + y*74)*4;
			Layer color = this.colors.get(prefix.toLowerCase());
			Color c = (y < value) ? color.background : color.foreground;
			data.set(index, c.r);
			data.set(index+1, c.g);
			data.set(index+2, c.b);
		}
	}
	
	private void swapMode(){
		mode++;
		mode = mode == modesCount ? 0 : mode;
		this.fpsDiv.getStyle().setDisplay(Display.NONE);
		this.msDiv.getStyle().setDisplay(Display.NONE);
		this.mbDiv.getStyle().setDisplay(Display.NONE);
		switch (mode){
			case 0:
				this.fpsDiv.getStyle().setDisplay(Display.BLOCK);
				break;
			case 1:
				this.msDiv.getStyle().setDisplay(Display.BLOCK);
				break;
			case 2:
				this.mbDiv.getStyle().setDisplay(Display.BLOCK);
				break;
		}
	}
	
	public Stats(){
		Monitor monitor;
		this.colors.put(FPS, new Layer(16,16,48,0,255,255));
		this.colors.put(MS, new Layer(16,48,16,0,255,0));
		this.colors.put(MB, new Layer(48,16,16,255,0,128));
		
		this.container = createContainer();
		
		monitor = this.init(FPS, container);
		this.fpsDiv = monitor.div;
		this.fpsText = monitor.text;
		this.fpsCanvas = monitor.canvas;
		this.fpsContext = monitor.context;
		this.fpsImageData = monitor.imageData;
		
		monitor = this.init(MS, container);
		this.msDiv = monitor.div;
		this.msText = monitor.text;
		this.msCanvas = monitor.canvas;
		this.msContext = monitor.context;
		this.msImageData = monitor.imageData;

		this.checkMB();
		monitor = this.init(MB, container);
		this.mbDiv = monitor.div;
		this.mbText = monitor.text;
		this.mbCanvas = monitor.canvas;
		this.mbContext = monitor.context;
		this.mbImageData = monitor.imageData;
		
		this.msDiv.getStyle().setDisplay(Display.NONE);
		this.mbDiv.getStyle().setDisplay(Display.NONE);
	}
	
	public Element getDomElement(){
		return this.container;
	}
	
	public void update(){
		this.frames++;
		this.time = new Date().getTime();
		this.ms = (int) (this.time - this.timeLastFrame);
		this.msMin = Math.min(this.msMin, this.ms);
		this.msMax = Math.max(this.msMax, this.ms);
		int value = (int) Math.min(30, 30-((double)this.ms/200)*30);
		this.updateGraph(this.msImageData.getData(), value, MS);
		this.msText.setInnerHTML(
			"<span style=\"font-weight:bold\">"+
			this.ms+
			" MS</span> ("+
			this.msMin+"-"+this.msMax+")"
		);
		this.msContext.putImageData(this.msImageData, 0, 0);
		this.timeLastFrame = this.time;
		if (this.time > this.timeLastSecond+1000){
			this.fps = Math.round((this.frames*1000)/(this.time-this.timeLastSecond));
			this.fpsMin = Math.min(this.fpsMin, this.fps);
			this.fpsMax = Math.max(this.fpsMax, this.fps);
			value = (int) Math.min(30, 30-((double)this.fps/100)*30);
			this.updateGraph(this.fpsImageData.getData(), value, FPS);
			this.fpsText.setInnerHTML(
				"<span style=\"font-weight:bold\">"+
				this.fps+
				" FPS</span> ("+
				this.fpsMin+"-"+this.fpsMax+")"
			);
			this.fpsContext.putImageData(this.fpsImageData, 0, 0);
			
			if (this.modesCount == 3){
				this.mb = (int) (getUsedJSHeapSize()*0.000000954);
				this.mbMin = Math.min(this.mbMin, this.mb);
				this.mbMax = Math.max(this.mbMax, this.mb);
				value = (int) Math.min(30, 30-((double)this.mb/2));
				this.updateGraph(this.mbImageData.getData(), value, MB);
				this.mbText.setInnerHTML(
					"<span style=\"font-weight:bold\">"+
					this.mb+
					" MB</span> ("+
					this.mbMin+"-"+this.mbMax+")"
				);
				this.mbContext.putImageData(this.mbImageData, 0, 0);
			}
			this.timeLastSecond = this.time;
			this.frames = 0;
		}
	}
}
