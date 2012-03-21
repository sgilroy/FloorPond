package floorPond;

import processing.core.PApplet;
import processing.core.PConstants;

import java.util.ArrayList;
import java.util.Stack;

public class TextParade
{
	private PApplet applet;
	private float paradeX = 0;
	private float scrollRate = 100f / 1000; // pixels per millisecond
	private String text;
	private final int textSize;
	private int textGutter;
	private ArrayList<String> messagesStack = new ArrayList<String>();
	private int nextMessageIndex = 0;
	private int lastMessageChangeTime;
	private int maxMessageCount = 10;

	public TextParade(PApplet applet)
	{
		this.applet = applet;
		text = "";
		textSize = 40;
		textGutter = 20;
	}

	void draw()
	{
		int now = applet.millis();
		paradeX = (now - lastMessageChangeTime) * scrollRate;
		if (paradeX > getParadeWidth())
		{
			lastMessageChangeTime = now;
			paradeX = 0;
			if (messagesStack.size() > 0)
			{
				text = messagesStack.get(nextMessageIndex);
				nextMessageIndex = (nextMessageIndex + 1) % messagesStack.size();
			}
		}

		applet.textSize(textSize);

		applet.pushMatrix();
		applet.fill(0);
		applet.text(text, applet.width - paradeX + 2, textSize + textGutter + 2);
		applet.fill(255);
		applet.text(text, applet.width - paradeX, textSize + textGutter);
		applet.popMatrix();

		applet.rotate(PConstants.PI);
		applet.fill(0);
		applet.text(text, -paradeX + 2, -applet.height + textSize + textGutter + 2);
		applet.fill(255);
		applet.text(text, -paradeX, -applet.height + textSize + textGutter);
	}

	private int getParadeWidth()
	{
		int textWidth = getTextWidth();
		return textWidth > 0 ? applet.width + textWidth : 0;
	}

	public int getTextWidth()
	{
		// approximate character width is assumed to be textSize / 2
		return text.length() * textSize / 2;
	}

	public void addMessages(ArrayList<String> messages)
	{
		if (messages != null)
		{
			for (int i = messages.size() - 1; i >= 0; i--)
			{
				String message = messages.get(i);
				messagesStack.add(0, message);
				nextMessageIndex = 0;

				if (messagesStack.size() > maxMessageCount)
				{
					messagesStack.remove(messagesStack.size() - 1);
				}
			}
		}
	}
}
