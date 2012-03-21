package floorPond;

import net.unto.twitter.Api;
import net.unto.twitter.TwitterProtos;
import net.unto.twitter.TwitterProtos.Status;
import net.unto.twitter.methods.SearchRequest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

public class TwitterReader
{
	private Date lastSearchTime = null;
	private int frequency = 200;
	private TwitterReaderThread twitterReaderThread;

	public TwitterReader()
	{
		twitterReaderThread = new TwitterReaderThread();
	}

	ArrayList<String> update()
	{
		Date now = new Date();
		if (lastSearchTime == null || now.getTime() > lastSearchTime.getTime() + frequency)
		{
			synchronized (twitterReaderThread)
			{
				return twitterReaderThread.getNewMessages();
			}
		}

		return null;
	}
}
