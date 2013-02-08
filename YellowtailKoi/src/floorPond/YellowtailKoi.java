package floorPond;

import TUIO.*;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YellowtailKoi was created by Scott Gilroy based on work by Ricardo Sanchez (June 2009), Xiao Xiao and Michael Bernstein (2010), and others.
 * <p/>
 * KOY FISH POND
 * original code by Ricardo Sanchez (June 2009),
 * modified by Xiao Xiao and Michael Bernstein (2010)
 */
public class YellowtailKoi extends PApplet implements TuioListener
{
	// should be greater than the maximum boid length to avoid having a boid appear to "pop" when wrapping
	public static final int OFF_SCREEN_GUTTER = 200;

	public static final float BOID_SPEED_RANGE_MIN = 3.5f;
	public static final float BOID_SPEED_RANGE_MAX = 3.9f;
	public static final float BOID_MAX_FORCE = .2f;
	//	public static final float BOID_SPEED_RANGE_MIN = .2f;
//	public static final float BOID_SPEED_RANGE_MAX = 2.5f;
//	public static final float BOID_MAX_FORCE = .2f;

	/**
	 * Number of cycles (pulses) for animating the "charging" (delayed start) of the TUIO cursor
	 */
	public static final int CHARGE_ANIMATION_CYCLES = 3;
	private static final float CHARGE_UP_DIAMETER_MAX = 1200;
	private static final float CHARGE_UP_DIAMETER_MIN = 300;
	public static final float CURSOR_CIRCLE_DIAMETER = 300;
	/**
	 * Time (in milliseconds) before automatically starting a gesture created using a TUIO cursor
	 */
	private int delayStartGestureForCursor = 1000 * 3 / 2;
	/**
	 * Time (in milliseconds) before automatically ending a gesture created using a TUIO cursor
	 */
	public static final double delayEndGestureForCursor = 1000 * 15;
	/**
	 * Countdown time (in milliseconds) to warn the user before automatically ending a gesture created using a TUIO cursor
	 */
	public static final int endGestureForCursorWarningTime = 1500;
	/**
	 * Time (in milliseconds) after a TUIO cursor automatically stops and before it can be reused to start a new gesture
	 */
	private int reuseCursorCooldownDelay = 1000 * 3;

	// and declare a TuioProcessing client variable
	TuioProcessing tuioClient;

	/**
	 * The maximum number of boids to use.
	 */
	int numBoids;
	/**
	 * Number of boids per pixel (boids = area * density). For example, 1.0f / 15681 is equivalent to 20 boids on a 640x480.
	 * Specify -1 to
	 */
	float BOID_DENSITY = 1.0f / 15681;

	int lastBirthTimecheck = 0;				// birth time interval
	int addKoiCounter = 0;

	ArrayList<Boid> boids = new ArrayList<Boid>();	 // stores wander behavior objects
	ArrayList<Boid> availableBoids = new ArrayList<Boid>();
	PVector mouseAvoidTarget;				  // use mouse location as object to evade
	boolean press = false;					 // check is mouse is press
	int minScope = 20;
	int maxScope = 300;

	int minSkinIndex = 0;
	int maxSkinIndex = 10;
	String[] skin = new String[maxSkinIndex + 1];

	PImage canvas;
	Ripple ripples;
	boolean isRipplesActive = false;

	PImage ripple;

	BallPositionSensor sensor;
	PVector hitPixels = new PVector(0, 0);

	float pressTime = 0;
	int pressInterval = 2000;

//	String winSerial = "COM7";
	String macSerial = "/dev/tty.usbserial-A9005d9p";

	boolean showBoidTargetingLines = false;
	boolean showTuioCursors = true;

	Yellowtail yellowtail;
	WrappedView wrappedView;
	private static final int IDLE_CURSOR_AUTOMATIC_DISPOSE_DELAY = 3000;
	private static boolean fullScreen = false;
	/**
	 * If true, boids will wander aimlessly when not following a gesture; otherwise, boids will flock
	 */
	private static boolean useWanderBehavior = false;
	private boolean useBrightnessToSimulateDepth = false;

	private boolean showTweets = false;
	private TextParade textParade;
	private TwitterReader twitterReader;
	private boolean flagellumStructureVisible;
	private boolean outlinesIgnored;

	public YellowtailKoi()
	{

	}

	public static void main(String args[])
	{
		if (args.length > 0 && args[0].equals("--present"))
		{
			fullScreen = true;
			PApplet.main(new String[]{"--present", "floorPond.YellowtailKoi"});
		}
		else
		{
			PApplet.main(new String[]{"floorPond.YellowtailKoi"});
		}
	}

	public void setup()
	{
		//size(screen.width, screen.height, OPENGL);
		//size(int(screen.width * 0.8), int(screen.height * 0.8), OPENGL);
		// note that we undersize by 2 pixels because there is currently some issue with ScalableDispaly which results in flickering
		if (fullScreen)
		{
			size(screen.width - 2, screen.height - 2, OPENGL);
		}
		else
		{
			size(640, 480, OPENGL);
			//size(int(2 * 10 * 54 * screenMultiplier), int(10 * 60 * screenMultiplier), OPENGL);
		}

		smooth();
		background(0);
		//frameRate(30);

		if (BOID_DENSITY != -1)
			numBoids = round(width * height * BOID_DENSITY);
		println("Using " + numBoids + " fish");

		wrappedView = new WrappedView(width, height, OFF_SCREEN_GUTTER);
		yellowtail = new Yellowtail(this, numBoids, wrappedView);

		sensor = new BallPositionSensor(this, macSerial, "coefficients-left.txt", "coefficients-right.txt");

		//rocks = loadImage("rocks.jpg");
		//innerShadow = loadImage("pond.png");

		// init skin array images
		for (int n = 0; n < maxSkinIndex + 1; n++) skin[n] = "skin-" + n + ".png";

		// this is the ripples code
		canvas = createImage(width, height, RGB);
		ripples = new Ripple(this, canvas);
		ripple = loadImage("ripple.png");

		// we create an instance of the TuioProcessing client
		// since we add "this" class as an argument the TuioProcessing class expects
		// an implementation of the TUIO callback methods (see below)
		tuioClient = new TuioProcessing(this);

		if (showTweets)
		{
			textParade = new TextParade(this);
			twitterReader = new TwitterReader();
			textParade.addMessages(twitterReader.update());
		}
	}

	boolean pause = false;
	boolean advanceOneFrame = false;
	boolean automaticBoidBirths = false;

	public void draw()
	{
		if (showTweets)
		{
			textParade.addMessages(twitterReader.update());
		}

		boolean shouldUpdate = true;
		if (advanceOneFrame)
			advanceOneFrame = false;
		else if (pause)
			shouldUpdate = false;

		updateCursors();

		background(0);

		if (shouldUpdate && automaticBoidBirths)
		{
			// adds new koi on a interval of time
			if (millis() > lastBirthTimecheck + 200)
			{
				lastBirthTimecheck = millis();
				if (addKoiCounter < numBoids) addKoi();
			}
		}

		Hit ballLocation = sensor.readHit();
		if (ballLocation != null)
		{
			hitPixels = ballLocation.getPixelVector();
			println("blah");
			println("Pixels: (" + hitPixels.x + ", " + hitPixels.y + ")");
			println("uploading turned off");

			press = true;
			pressTime = millis();
		}

		for (int n = 0; n < boids.size(); n++)
		{
			Boid boid = boids.get(n);
			boolean boidHasGoal = false;

			if (shouldUpdate)
			{
				if (boid.getGesture() != null)
				{
					Gesture gesture = boid.getGesture();

					if (gesture.exists)
					{
						boid.startStrictPursuit();
						Vec3f gesturePoint = gesture.path[gesture.nPoints - 1];

						PVector targetPosition = gesture.getViewCoordinate(gesturePoint.x, gesturePoint.y);
	//					println("Boid " + n + " at " + boid.location.x + ", " + boid.location.y + " pursuing yellowtail at " + targetPosition.x + ", " + targetPosition.y + "  based on " + gesturePoint.x + ", " + gesturePoint.y);
						targetPosition = wrappedView.targetForWrapping(targetPosition, boid.location);

	//      println("  adjusted: " + targetPosition.x + ", " + targetPosition.y);

						boid.arrive(targetPosition);
						boid.run();
						boidHasGoal = true;
					}
				}

				if (!boidHasGoal)
				{
					boid.stopStrictPursuit();

					float closestBoidDist = -1;
					PVector closestTargetPosition = null;
					boolean shouldEvade = false;

					// touch/bodies (TUIO)
					for (CursorGesture cursorGesture : tuioCursorMap.values())
					{
						TuioCursor tcur = cursorGesture.getCursor();

						// for each tuio cursor, pick objects inside the mouseAvoidScope
						// and convert them in pursuers
						PVector cursorPosition = new PVector(tcur.getScreenX(width), tcur.getScreenY(height));

						float boidDist = dist(cursorPosition.x, cursorPosition.y, boid.location.x,
											  boid.location.y);
						if (closestTargetPosition == null || boidDist < closestBoidDist)
						{
							closestBoidDist = boidDist;
							closestTargetPosition = cursorPosition;
							shouldEvade = determineTuioCursorShouldEvade(tcur);
						}
					}

					// mouse/ball
					if (press)
					{
						float boidDist = dist(hitPixels.x, hitPixels.y, boid.location.x, boid.location.y);
						if (closestTargetPosition == null || boidDist < closestBoidDist)
						{
							closestBoidDist = boidDist;
							closestTargetPosition = hitPixels;
						}
					}

					if ((closestTargetPosition != null) && (closestBoidDist > minScope) && (closestBoidDist < maxScope))
					{
	//					println("Boid " + n + " pursuing " + closestTargetPosition.x + ", " + closestTargetPosition.y + " at distance of " + closestBoidDist);
						boid.timeCount = 0;

						if (shouldEvade)
							boid.evade(closestTargetPosition);
						else
							boid.pursue(closestTargetPosition);

						if (showBoidTargetingLines)
						{
							// red for evade
							if (shouldEvade)
								stroke(255, 90, 90, 200);
							else
								stroke(255, 200);

							noFill();
							strokeWeight(3);
							line(boid.location.x, boid.location.y, closestTargetPosition.x,
								 closestTargetPosition.y);
						}
					} else
					{
						if (useWanderBehavior)
						{
							boid.wander();
						}
						else
						{
							boid.flock(boids);
						}
					}
					boid.run();
				}
			}
			else
			{
				boid.display();
			}
		}

		if (showTuioCursors)
		{
			// render the touch/bodies cursors (TUIO)
			for (CursorGesture cursorGesture : tuioCursorMap.values())
			{
				TuioCursor tcur = cursorGesture.getCursor();

				// for each tuio cursor, pick objects inside the mouseAvoidScope
				// and convert them in pursuers
				PVector cursorPosition = new PVector(tcur.getScreenX(width), tcur.getScreenY(height));

				noFill();
				strokeWeight(3);
				int age = cursorGesture.age(millis());
				if (cursorGesture.getGesture() != null)
				{
					if (age >= delayStartGestureForCursor + delayEndGestureForCursor - endGestureForCursorWarningTime)
					{
						int chargeAnimationPeriod = endGestureForCursorWarningTime / CHARGE_ANIMATION_CYCLES;
						float chargeAnimationPhase = (float)((delayStartGestureForCursor + delayEndGestureForCursor - age) % chargeAnimationPeriod) / chargeAnimationPeriod;
						drawChargeAnimation(cursorPosition, chargeAnimationPhase);
					}

					tint(200, 200, 255, 255);
				}
				else
				{
					if (age >= 0)
					{
						int chargeAnimationPeriod = delayStartGestureForCursor / CHARGE_ANIMATION_CYCLES;
						float chargeAnimationPhase = (float)(age % chargeAnimationPeriod) / chargeAnimationPeriod;
						drawChargeAnimation(cursorPosition, chargeAnimationPhase);
					}

					tint(255, 128);
					stroke(255, 200);
				}
				image(ripple, cursorPosition.x - (CURSOR_CIRCLE_DIAMETER / 2), cursorPosition.y - (CURSOR_CIRCLE_DIAMETER / 2),
					  CURSOR_CIRCLE_DIAMETER, CURSOR_CIRCLE_DIAMETER);
			}
		}
		if (shouldUpdate)
		{
			yellowtail.draw();
		}
		else
		{
			yellowtail.display();
		}


		if (press)
		{
			stroke(255, 200);
			noFill();
			strokeWeight(3);
			float radius1 = (millis() - pressTime) / 4;
			float radius2 = radius1 - 70;
			float radius3 = radius2 - 70;
			//tint(255, (pressInterval - (millis() - pressTime)) / pressInterval * 255);
			if ((radius1 > 20) && (radius1 < 300))
			{
				//ellipse(hitPixels.x, hitPixels.y, radius1, radius1);
				tint(255, (300 - radius1) / 300 * 255);
				image(ripple, hitPixels.x - (radius1 / 2), hitPixels.y - (radius1 / 2), radius1, radius1);
			}
			if ((radius2 > 20) && (radius2 < 300))
			{
				//ellipse(hitPixels.x, hitPixels.y, radius2, radius2);
				tint(255, (300 - radius2) / 300 * 255);
				image(ripple, hitPixels.x - (radius2 / 2), hitPixels.y - (radius2 / 2), radius2, radius2);
			}
			if ((radius3 > 20) && (radius3 < 300))
			{
				//ellipse(hitPixels.x, hitPixels.y, radius3, radius3);
				tint(255, (300 - radius3) / 300 * 255);
				image(ripple, hitPixels.x - (radius3 / 2), hitPixels.y - (radius3 / 2), radius3, radius3);
			}
			if (millis() - pressTime > pressInterval)
			{
				press = false;
			}
		}

		// ripples code
		if (isRipplesActive)
		{
			refreshCanvas();
			ripples.update();
		}

		if (showTweets)
		{
			textParade.draw();
		}
	}

	private void drawChargeAnimation(PVector cursorPosition, float chargeAnimationPhase)
	{
		chargeAnimationPhase *= chargeAnimationPhase;
		float radiusChargeUp = lerp(CHARGE_UP_DIAMETER_MAX, CHARGE_UP_DIAMETER_MIN, chargeAnimationPhase);
		tint(255, lerp(0, 128, chargeAnimationPhase));
		image(ripple, cursorPosition.x - (radiusChargeUp / 2), cursorPosition.y - (radiusChargeUp / 2), radiusChargeUp, radiusChargeUp);
	}

	private void updateCursors()
	{
		for (CursorGesture cursorGesture : tuioCursorMap.values())
		{
			TuioCursor tcur = cursorGesture.getCursor();
			if (cursorGesture.getGesture() == null && cursorGesture.age(millis()) > delayStartGestureForCursor)
			{
				cursorGesture.setGesture(startGestureForCursor(tcur));
			}
			else if (cursorGesture.getGesture() != null && cursorGesture.age(millis()) > delayStartGestureForCursor + delayEndGestureForCursor)
			{
				endGestureForCursor(cursorGesture);
				cursorGesture.reset(millis() + reuseCursorCooldownDelay);
			}
			else if (millis() - cursorGesture.getLastUpdateTime() > IDLE_CURSOR_AUTOMATIC_DISPOSE_DELAY)
			{
				removeTuioCursor(tcur);
			}
		}
	}

	// Every other cursor is treated as something to evade or pursue
	boolean determineTuioCursorShouldEvade(TuioCursor tcur)
	{
		return (tcur.getSessionID() % 2 == 0);
	}

	// increments number of koi by 1
	Boid addKoi()
	{
		int id = (int) (random(minSkinIndex, maxSkinIndex + 1));
		Boid boid = new Boid(this, skin[id],
						  new PVector(random(100, width - 100), random(100, height - 100)),
						  random(BOID_SPEED_RANGE_MIN, BOID_SPEED_RANGE_MAX), BOID_MAX_FORCE,
						  wrappedView);
		boid.setFlagellumStructureVisible(isFlagellumStructureVisible());
		// put the new boid at the front of the stack so that it will get picked first
		boids.add(boid);
		availableBoids.add(0, boid);
		// sets brightness to simulate depth
		if (useBrightnessToSimulateDepth)
			boid.brightness = (int) (map(addKoiCounter, 0, numBoids, 150, 255));

		addKoiCounter++;
		return boid;
	}

	// use for the ripple effect to refresh the canvas
	void refreshCanvas()
	{
		loadPixels();
		System.arraycopy(pixels, 0, canvas.pixels, 0, pixels.length);
		updatePixels();
	}

	public void mousePressed()
	{
		hitPixels = new PVector(mouseX, mouseY);
		press = true;
		pressTime = millis();
		mouseAvoidTarget = new PVector(mouseX, mouseY);

		if (isRipplesActive)
			ripples.makeTurbulence(mouseX, mouseY);

		Gesture gesture = yellowtail.mousePressed();
		if (gesture != null)
		{
			Boid boid = getTrainableBoid();
			if (boid != null)
			{
				boid.setGesture(gesture);
			}
		}
	}

	Boid getTrainableBoid()
	{
		if (addKoiCounter < numBoids)
		{
			addKoi();
		}

		if (availableBoids.size() > 0)
		{
			return availableBoids.remove(0);
		}
		return null;
	}

	public void mouseDragged()
	{
		mouseAvoidTarget.x = mouseX;
		mouseAvoidTarget.y = mouseY;

		if (isRipplesActive)
			ripples.makeTurbulence(mouseX, mouseY);

		yellowtail.mouseDragged();
	}

	public void mouseReleased()
	{
		Gesture gesture = yellowtail.mouseReleased();
		makeBoidAvailable(gesture);
	}

	public void keyPressed()
	{
		yellowtail.keyPressed();
		if (key == 'p')
		{
			pause = !pause;
		} else if (key == ']')
		{
			advanceOneFrame = true;
		} else if (key == 'f')
		{
			setFlagellumStructureVisible(!isFlagellumStructureVisible());
			for (Boid boid : boids)
			{
				boid.setFlagellumStructureVisible(isFlagellumStructureVisible());
			}
		} else if (key == '=')
		{
			for (Boid boid : boids)
			{
				boid.setScale(boid.getScale() * 1.2f);
			}
		} else if (key == '-')
		{
			for (Boid boid : boids)
			{
				boid.setScale(boid.getScale() / 1.2f);
			}
		} else if (key == 'o')
		{
			setOutlinesIgnored(!isOutlinesIgnored());
			for (Boid boid : boids)
			{
				boid.setOutlineIgnored(isOutlinesIgnored());
			}
		}
	}

	private ConcurrentHashMap<Long, CursorGesture> tuioCursorMap = new ConcurrentHashMap<Long, CursorGesture>();

	public void addTuioObject(TuioObject tuioObject)
	{
	}

	public void updateTuioObject(TuioObject tuioObject)
	{
	}

	public void removeTuioObject(TuioObject tuioObject)
	{
	}

	public void addTuioCursor(TuioCursor tcur)
	{
		Gesture gesture = null;
		if (delayStartGestureForCursor == 0)
			gesture = startGestureForCursor(tcur);
		tuioCursorMap.put(tcur.getSessionID(), new CursorGesture(millis(), tcur, gesture));
	}

	private Gesture startGestureForCursor(TuioCursor tcur)
	{
		Gesture gesture = yellowtail.startGesture(tcur.getScreenX(width), tcur.getScreenY(height));
		if (gesture != null)
		{
			Boid boid = getTrainableBoid();
			if (boid != null)
			{
				boid.setGesture(gesture);
			}

		}
		return gesture;
	}

	public void updateTuioCursor(TuioCursor tcur)
	{
		if (tuioCursorMap.containsKey(tcur.getSessionID()))
		{
			CursorGesture cursorGesture = tuioCursorMap.get(tcur.getSessionID());
			Gesture gesture = cursorGesture.getGesture();
			cursorGesture.setLastUpdateTime(millis());

			if (gesture != null)
			{
				// TODO: add better support for when there are more cursors than available gestures
				if (!gesture.isComplete())
					yellowtail.updateGesture(gesture, tcur.getScreenX(width), tcur.getScreenY(height));
			}
		}
	}

	public void removeTuioCursor(TuioCursor tcur)
	{
		if (tuioCursorMap.containsKey(tcur.getSessionID()))
		{
			CursorGesture cursorGesture = tuioCursorMap.get(tcur.getSessionID());
			endGestureForCursor(cursorGesture);
			tuioCursorMap.remove(tcur.getSessionID());
		}
	}

	private void endGestureForCursor(CursorGesture cursorGesture)
	{
		TuioCursor tcur = cursorGesture.getCursor();
		Gesture gesture = cursorGesture.getGesture();

		if (gesture != null)
		{
			yellowtail.endGesture(gesture);
			makeBoidAvailable(gesture);
		}
	}

	private void makeBoidAvailable(Gesture gesture)
	{
		for (Boid boid : boids)
		{
			if (boid.getGesture() == gesture)
			{
				if (!availableBoids.contains(boid))
				{
					availableBoids.add(boid);
				}
			}
		}
	}

	public void refresh(TuioTime tuioTime)
	{
	}

	public boolean isFlagellumStructureVisible()
	{
		return flagellumStructureVisible;
	}

	public void setFlagellumStructureVisible(boolean flagellumStructureVisible)
	{
		this.flagellumStructureVisible = flagellumStructureVisible;
	}

	public boolean isOutlinesIgnored()
	{
		return outlinesIgnored;
	}

	public void setOutlinesIgnored(boolean outlinesIgnored)
	{
		this.outlinesIgnored = outlinesIgnored;
	}
}
