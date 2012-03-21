package floorPond;

import net.unto.twitter.Api;
import net.unto.twitter.TwitterProtos;
import net.unto.twitter.methods.SearchRequest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class TwitterReaderThread extends Thread
{
	private long sinceId = -1;
	private final Api api;
	private ArrayList<String> newMessages = new ArrayList<String>();

	public TwitterReaderThread()
	{
		api = Api.builder().build();
		start();
	}

	public ArrayList<String> getNewMessages()
	{
		ArrayList<String> outgoingMessages = null;
		if (newMessages.size() > 0)
		{
			outgoingMessages = new ArrayList<String>();
			outgoingMessages.addAll(newMessages);
			newMessages.clear();
		}
		return outgoingMessages;
	}
				
	@Override
	public void run()
	{
		try
		{
			while (isAlive())
			{
				ArrayList<String> messages = searchForMessages();
				newMessages.addAll(messages);

				// Let the thread sleep for a while.
				Thread.sleep(5000);
			}
		} catch (InterruptedException e)
		{
			System.out.println("Child interrupted.");
		}
		System.out.println("Exiting child thread.");
	}

	private ArrayList<String> searchForMessages()
	{
		String query = "@ScottJGilroy";

		SearchRequest.Builder search = api.search(query);
		if (sinceId > 0)
		{
//			query += " since_id=" + sinceId;
			search.sinceId(sinceId);
		}

		TwitterProtos.Results results = search.build().get();
		ArrayList<String> messages = new ArrayList<String>();
		for (TwitterProtos.Results.Result result : results.getResultsList())
		{
//			results.getMaxId();
			String createdAt = "";
			DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
			Date createdAtDate = null;
			try
			{
				createdAtDate = dateFormat.parse(result.getCreatedAt());
			} catch (ParseException e)
			{
				e.printStackTrace();
			}

			if (createdAtDate != null)
			{
				DateFormat timeFormat = DateFormat.getTimeInstance();
				createdAt = " " + timeFormat.format(createdAtDate);
			}
			String message = String.format("%s%s: %s", result.getFromUser(), createdAt, result.getText());
			System.out.println(message);
			messages.add(message);
//			sinceId = Math.max(sinceId, result.getSinceId());
		}

		sinceId = results.getMaxId();

		return messages;
	}
}
