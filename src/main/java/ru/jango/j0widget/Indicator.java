package ru.jango.j0widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

/**
 * Класс индикатора. Просто некая маленькая штука, которая должна болтаться 
 * на экране, сигнализируя о чем-то.
 * <br><br>
 * Умеет:<br>
 * - анимироваться в стиле кругов на воде <br> // TODO
 * - появляться/исчезать по таймеру // TODO
 */
public class Indicator extends View {
	
	public static final int DEFAULT_INDICATOR_COLOR = Color.parseColor("#aaff4444");
	
	private Object value;
	private Paint paint;
	
    public Indicator(Context context) { super(context); init(context); }
    public Indicator(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public Indicator(Context context, AttributeSet attrs, int style) { super(context, attrs, style); init(context); }

    private void init(Context context) {
    	paint = new Paint();
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL_AND_STROKE);
   	
    	setIndicatorColor(DEFAULT_INDICATOR_COLOR);
    }

	@Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	
    	canvas.drawCircle(getWidth()/2, 
    			getHeight()/2, 
    			Math.min(getWidth(), getHeight())/2, 
    			paint);
    }
    
    /**
     * Делает индикатор видимым. Одновременно запускает анимацию.
     */
    public void show() {
    	setVisibility(View.VISIBLE);
    }
    
    /**
     * Делает индикатор невидимым. Одновременно останавливает анимацию.
     */
    public void hide() {
    	setVisibility(View.GONE);
    }
    
	///////////////////////////////////////////////////////////////
	//
	// 					Getters and setters
	//
	///////////////////////////////////////////////////////////////
	
	public int getIndicatorColor() {
		return paint.getColor();
	}
	
	public void setIndicatorColor(int color) {
		paint.setColor(color);
	}
	
	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}

}
