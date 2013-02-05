package floorPond;

/**
 * Yellowtail
 * by Golan Levin (www.flong.com).
 *
 * Click, drag, and release to create a kinetic gesture.
 *
 * Yellowtail (1998-2000) is an interactive software system for the gestural
 * creation and performance of real-time abstract animation. Yellowtail repeats
 * a user's strokes end-over-end, enabling simultaneous specification of a
 * line's shape and quality of movement. Each line repeats according to its
 * own period, producing an ever-changing and responsive display of lively,
 * worm-like textures.
 */

import processing.core.PApplet;
import processing.core.PConstants;

import java.awt.*;
import java.util.ArrayList;

class Yellowtail
{
	Gesture gestureArray[];
	ArrayList<Gesture> availableGestures = new ArrayList<Gesture>();
	int nGestures = 36;  // Number of gestures
	final int minMove = 3;	 // Minimum travel for a new point
	int currentGestureId;

	private WrappedView wrappedView;
	private PApplet applet;
	private Gesture mouseGesture;
	private boolean moveForward = true;


	Yellowtail(PApplet applet, int _nGestures, WrappedView _wrappedView)
	{
		//size(1024, 768, OPENGL);
		//background(0, 0, 0);
		//noStroke();

		this.applet = applet;
		nGestures = _nGestures;
		wrappedView = _wrappedView;
		currentGestureId = -1;
		gestureArray = new Gesture[nGestures];
		for (int i = 0; i < nGestures; i++)
		{
			gestureArray[i] = new Gesture(applet.width, applet.height, wrappedView, applet);
			availableGestures.add(gestureArray[i]);
		}
		clearGestures();
		PApplet.println("Yellowtail initialized");
	}

	void draw()
	{
		updateGeometry();
		display();
	}

	public void display()
	{
		//fill(255, 255, 245, 64);
		applet.noStroke();
		for (int i = 0; i < nGestures; i++)
		{
			renderGesture(gestureArray[i]);
		}
	}

	Gesture mousePressed()
	{
		mouseGesture = startGesture((float) applet.mouseX, (float) applet.mouseY);
		return mouseGesture;
	}
	
	Gesture mouseReleased()
	{
		Gesture gesture = mouseGesture;
		if (mouseGesture != null)
		{
			endGesture(mouseGesture);
			mouseGesture = null;
		}
		return gesture;
	}

	public void endGesture(Gesture gesture)
	{
		gesture.end();
		if (!availableGestures.contains(gesture))
			availableGestures.add(gesture);
	}

	public Gesture startGesture(float x, float y)
	{
		if (availableGestures.size() > 0)
		{
			Gesture gesture = availableGestures.remove(0);

//			currentGestureId = (currentGestureId + 1) % nGestures;
//			Gesture gesture = gestureArray[currentGestureId];
//			if (!gesture.exists || gesture.isComplete())
			gesture.clear();
			gesture.clearPolys();
			gesture.addPoint(x, y);
			return gesture;
		}

		return null;
	}


	void mouseDragged()
	{
		if (mouseGesture != null)
			updateGesture(mouseGesture, applet.mouseX, applet.mouseY);
	}

	public void updateGesture(Gesture gesture, float x, float y)
	{
		if (gesture != null && gesture.distToLast(x, y) > minMove)
		{
			gesture.addPoint(x, y);
			gesture.smooth();
			gesture.compile();
		}
	}


	void keyPressed()
	{
		if (applet.key == '+' || applet.key == '=')
		{
			if (mouseGesture != null)
			{
				float th = mouseGesture.thickness;
				mouseGesture.thickness = PApplet.min(96, th + 1);
				mouseGesture.compile();
			}
		} else if (applet.key == '-')
		{
			if (mouseGesture != null)
			{
				float th = mouseGesture.thickness;
				mouseGesture.thickness = PApplet.max(2, th - 1);
				mouseGesture.compile();
			}
		} else if (applet.key == ' ')
		{
			clearGestures();
		}
	}


	void renderGesture(Gesture gesture)
	{
		if (gesture.exists && !gesture.isInvisible())
		{
			if (gesture.nPolys > 0)
			{
				Polygon polygons[] = gesture.polygons;
				int crosses[] = gesture.crosses;

				int xpts[];
				int ypts[];
				Polygon p;
				int cr;

				applet.fill(255, 255, 245, gesture.getAlpha());
				// green for St. Patrick's Day
//				applet.fill(14, 158, 73, gesture.getAlpha());
				applet.beginShape(PConstants.QUADS);
				int gnp = gesture.nPolys;
				for (int i = 0; i < gnp; i++)
				{

					p = polygons[i];
					xpts = p.xpoints;
					ypts = p.ypoints;

					applet.vertex(xpts[0], ypts[0]);
					applet.vertex(xpts[1], ypts[1]);
					applet.vertex(xpts[2], ypts[2]);
					applet.vertex(xpts[3], ypts[3]);

					if ((cr = crosses[i]) > 0)
					{
						if ((cr & 3) > 0)
						{
							applet.vertex(xpts[0] + wrappedView.virtualW, ypts[0]);
							applet.vertex(xpts[1] + wrappedView.virtualW, ypts[1]);
							applet.vertex(xpts[2] + wrappedView.virtualW, ypts[2]);
							applet.vertex(xpts[3] + wrappedView.virtualW, ypts[3]);

							applet.vertex(xpts[0] - wrappedView.virtualW, ypts[0]);
							applet.vertex(xpts[1] - wrappedView.virtualW, ypts[1]);
							applet.vertex(xpts[2] - wrappedView.virtualW, ypts[2]);
							applet.vertex(xpts[3] - wrappedView.virtualW, ypts[3]);
						}
						if ((cr & 12) > 0)
						{
							applet.vertex(xpts[0], ypts[0] + wrappedView.virtualH);
							applet.vertex(xpts[1], ypts[1] + wrappedView.virtualH);
							applet.vertex(xpts[2], ypts[2] + wrappedView.virtualH);
							applet.vertex(xpts[3], ypts[3] + wrappedView.virtualH);

							applet.vertex(xpts[0], ypts[0] - wrappedView.virtualH);
							applet.vertex(xpts[1], ypts[1] - wrappedView.virtualH);
							applet.vertex(xpts[2], ypts[2] - wrappedView.virtualH);
							applet.vertex(xpts[3], ypts[3] - wrappedView.virtualH);
						}

						// I have knowingly retained the small flaw of not
						// completely dealing with the corner conditions
						// (the case in which both of the above are true).
					}
				}
				applet.endShape();
			}
		}
	}

	void updateGeometry()
	{
		Gesture J;
		for (int g = 0; g < nGestures; g++)
		{
			if ((J = gestureArray[g]).exists)
			{
				if (J.isComplete())
				{
					advanceGesture(J);
				}
			}
		}
	}

	void advanceGesture(Gesture gesture)
	{
		// Move a Gesture one step
		if (gesture.exists)
		{ // check
			int nPts = gesture.nPoints;
			int nPts1 = moveForward ? 0 : nPts - 1;
			Vec3f path[];
			float jx = moveForward ? gesture.jumpDx : -gesture.jumpDx;
			float jy = moveForward ? gesture.jumpDy : -gesture.jumpDy;

			if (nPts > 0)
			{
				if (gesture.startAdvance())
				{
					path = gesture.path;
					for (int i = nPts1; moveForward ? i < nPts - 1 : i > 0; i+= (moveForward ? 1 : -1))
					{
						path[i].x = path[i + (moveForward ? 1 : -1)].x;
						path[i].y = path[i + (moveForward ? 1 : -1)].y;
					}
					path[moveForward ? nPts - 1 : 0].x = path[nPts1].x + jx;
					path[moveForward ? nPts - 1 : 0].y = path[nPts1].y + jy;
					gesture.compile();
				}
			}
		}
	}

	void clearGestures()
	{
		for (int i = 0; i < nGestures; i++)
		{
			gestureArray[i].clear();
		}
	}
}