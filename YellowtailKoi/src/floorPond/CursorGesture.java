package floorPond;

import TUIO.TuioCursor;

public class CursorGesture
{
	private int birthTime;
	private TuioCursor cursor;
	private Gesture gesture;
	private int lastUpdateTime;

	public CursorGesture(int birthTime, TuioCursor cursor, Gesture gesture)
	{
		this.birthTime = birthTime;
		this.cursor = cursor;
		this.gesture = gesture;
		lastUpdateTime = birthTime;
	}

	public Gesture getGesture()
	{
		return gesture;
	}

	public void setGesture(Gesture gesture)
	{
		this.gesture = gesture;
	}

	public TuioCursor getCursor()
	{
		return cursor;
	}

	public void setCursor(TuioCursor cursor)
	{
		this.cursor = cursor;
	}

	public int getBirthTime()
	{
		return birthTime;
	}
	
	public int age(int nowTime)
	{
		return nowTime - birthTime;
	}

	public void reset(int nowTime)
	{
		setGesture(null);
		birthTime = nowTime;
	}

	public void setLastUpdateTime(int nowTime)
	{
		lastUpdateTime = nowTime;
	}

	public int getLastUpdateTime()
	{
		return lastUpdateTime;
	}
}
