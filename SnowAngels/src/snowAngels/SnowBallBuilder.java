package snowAngels;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;
import tsps.TSPS;
import tsps.TSPSPerson;

import java.util.ArrayList;

public class SnowballBuilder
{
	private final PApplet applet;
	private final TSPS tspsReceiver;
	private final ParticleSystem particleSystem;

//	private final ArrayList<Snowball> snowballs = new ArrayList<Snowball>();
	private static final float SNOWBALL_THRESHOLD_DISTANCE = 90f;
	private static final float SNOWBALL_RADIUS = 10f;

	public SnowballBuilder(PApplet applet, TSPS tspsReceiver)
	{
		this.applet = applet;
		this.tspsReceiver = tspsReceiver;
		particleSystem = new ParticleSystem(applet);
	}

	public void update()
	{
		particleSystem.update();
	}

	public void draw()
	{
		ArrayList<PVector> attractors = new ArrayList<PVector>();
		synchronized (tspsReceiver.people)
		{
			for (TSPSPerson person : tspsReceiver.people.values())
			{
				PVector centroid = person.centroid;
				PVector centroidScreen = toScreenCoordinate(centroid);
				PVector furthestPoint = null;
				float furthestDistance = 0;

				synchronized (person.contours)
				{
					ArrayList<PVector> contours = person.contours;
					for (PVector point : contours)
					{
						if (point != null)
						{
							PVector pointScreen = toScreenCoordinate(point);
							float distance = pointScreen.dist(centroidScreen);
							if (furthestPoint == null || distance > furthestDistance)
							{
								furthestPoint = pointScreen;
								furthestDistance = distance;
							}
						}
					}
				}

				if (furthestPoint != null && furthestDistance > SNOWBALL_THRESHOLD_DISTANCE)
				{
					attractors.add(furthestPoint);
					applet.colorMode(PConstants.RGB);
					applet.fill(128, 128, 255);
					applet.noStroke();
					applet.ellipse(furthestPoint.x, furthestPoint.y, SNOWBALL_RADIUS, SNOWBALL_RADIUS);
				}
			}
		}
		particleSystem.setAttractors(attractors);
		particleSystem.draw();
	}

	private PVector toScreenCoordinate(PVector point)
	{
		return new PVector(point.x * applet.width, point.y * applet.height);
	}
}
