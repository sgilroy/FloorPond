package snowAngels;

import com.martiansoftware.jsap.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import tsps.TSPS;
import tsps.TSPSPerson;

import java.util.ArrayList;

public class SnowAngels extends PApplet
{
	public static final String APPLET_CLASS = "snowAngels.SnowAngels";
	public static final String PROCESSING_RENDERER = P2D;
	TSPS tspsReceiver;

	private static boolean fullScreen;
	int fadeCycle = 0;
	int fadeDelay = 60;
	int fadeAmount = 0;
	private int lastActiveTime = 0;
	private static final int DORMANT_DELAY = 200;
	PImage imageBuffer;
	PGraphics mainGraphics;
	private boolean shouldFade = true;
	private boolean shouldBlur = true;
	private ArrayList<Integer> colorTable = new ArrayList<Integer>();
	private float blurRadius = 0.1f;

	private static final int PARAMETER_RANDOMIZATION_DELAY = 10000;
	private int lastParameterRandomization = 0;
	private float saturationFactor = 1f;
	private float strokeWeight = 1f;

	private SnowballBuilder snowballBuilder;

	/**
	 * Copy of the frame buffer before non-persistent overlay rendering is added
	 */
	private PImage persistentImage;
	private static int _fillAlpha = 16;
	private static int _strokeAlpha = 34;

	public static void main(String args[])
	{
		SimpleJSAP jsap = null;
		try
		{
			jsap = new SimpleJSAP(
					"SnowAngels",
					"Draws contours of people as they move",
					new Parameter[]{
							new FlaggedOption("fillAlpha", JSAP.INTEGER_PARSER, Integer.toString(_fillAlpha), JSAP.NOT_REQUIRED, 'f', JSAP.NO_LONGFLAG,
											  "Alpha value for fills. Range is 0 to 255."),
							new FlaggedOption("strokeAlpha", JSAP.INTEGER_PARSER, Integer.toString(_strokeAlpha), JSAP.NOT_REQUIRED, 's', JSAP.NO_LONGFLAG,
											  "Alpha value for stroke (outline). Range is 0 to 255."),
							new QualifiedSwitch("present", JSAP.BOOLEAN_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'p',
												"present",
												"Present full screen."),
					}
			);
		} catch (JSAPException e)
		{
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}

		JSAPResult config = jsap.parse(args);
		if (jsap.messagePrinted()) System.exit(1);

		fullScreen = config.getBoolean("present");
		_fillAlpha = config.getInt("fillAlpha");
		_strokeAlpha = config.getInt("strokeAlpha");

		if (fullScreen)
		{
			PApplet.main(new String[]{"--present", APPLET_CLASS});
		} else
		{
			PApplet.main(new String[]{APPLET_CLASS});
		}
	}

	public void setup()
	{
		if (fullScreen)
		{
			size(screen.width, screen.height, PROCESSING_RENDERER);
		} else
		{
			size(1024, 768, PROCESSING_RENDERER);
		}

		//all you need to do to start TSPS
		tspsReceiver = new TSPS(this, 12000);

		snowballBuilder = new SnowballBuilder(this, tspsReceiver);

		smooth();
		background(0);

		colorMode(HSB, 255);
		for (int i = 0; i < 256; i++)
		{
			int c = color(random(255), random(128, 255), random(210, 255));
			colorTable.add(c);
		}

//		mainGraphics = createGraphics(width, height, PROCESSING_RENDERER);
		mainGraphics = this.g;
		mainGraphics.background(0);
		mainGraphics.smooth();
		mainGraphics.colorMode(HSB, 255);
		imageBuffer = mainGraphics.get();
	}

	public void draw()
	{
		tspsReceiver.update();
		snowballBuilder.update();

//		mainGraphics.beginDraw();
		mainGraphics.colorMode(HSB, 255);
		mainGraphics.smooth();
		if (persistentImage != null)
		{
			mainGraphics.image(persistentImage, 0, 0);
		}
		applyEffects();

		boolean activePeople = drawPeople(_fillAlpha, _strokeAlpha);

//		mainGraphics.endDraw();

		if (mainGraphics != g)
		{
			image(mainGraphics, 0, 0, width, height);
		} else
		{
			persistentImage = mainGraphics.get();
		}

		drawPeople(255, 255);

		if (activePeople)
			lastActiveTime = millis();

		if (PARAMETER_RANDOMIZATION_DELAY != -1 && millis() - lastParameterRandomization > PARAMETER_RANDOMIZATION_DELAY)
		{
			lastParameterRandomization = millis();
			randomizeRenderingParameters();
		}

		snowballBuilder.draw();
	}

	private void randomizeRenderingParameters()
	{
		blurRadius = (random(1f) > 0.5f) ? 0 : random(0, 0.3f);
		fadeAmount = (blurRadius > 0 || random(1f) > 0.5f) ? 0 : (int) random(0, 5);
		saturationFactor = (random(1f) > 0.5f) ? 0 : random(0, 1);
//		strokeWeight = random(1) > 0.5f ? 0 : random(0, 3);
	}

	private boolean drawPeople(int fillAlpha, int strokeAlpha)
	{
		boolean activePeople = false;
		synchronized (tspsReceiver.people)
		{
			for (TSPSPerson person : tspsReceiver.people.values())
			{
				synchronized (person.contours)
				{
					ArrayList<PVector> contours = person.contours;
					if (contours.size() > 3)
					{
						activePeople = true;

						int color = getPersonColor(person);
						mainGraphics.fill(hue(color), saturation(color), (int) (brightness(color) * 0.8), fillAlpha);
						mainGraphics.stroke(hue(color), (int) (saturation(color) * saturationFactor),
											saturationFactor == 0 ? 255 : brightness(color),
											strokeAlpha);
						mainGraphics.strokeWeight(strokeWeight);
						drawContours(contours);
					}
				}
			}
		}
		return activePeople;
	}

	private int getPersonColor(TSPSPerson person)
	{
		return colorTable.get(person.id % colorTable.size());
	}

	private void drawContours(ArrayList<PVector> contours)
	{
		mainGraphics.beginShape();
		for (PVector point : contours)
		{
			mainGraphics.curveVertex(point.x * width, point.y * height);
		}
//						PVector point = contours.get(0);
//						mainGraphics.curveVertex(point.x * width, point.y * height);
		mainGraphics.endShape(CLOSE);
	}

	private void applyEffects()
	{
		// go dormant and don't fade after a certain time out
		if (millis() - lastActiveTime < DORMANT_DELAY)
		{
			fadeCycle = (fadeCycle + 1) % fadeDelay;

			if (fadeCycle == 0)
			{
				// Fade out the surface slowly over time
				if (shouldFade)
				{
					mainGraphics.stroke(0);
					mainGraphics.strokeWeight(0);
					mainGraphics.fill(0, fadeAmount);
					mainGraphics.rect(0, 0, width, height);
				}

			}
			if (shouldBlur)
			{
				//loadPixels(); //copy window contents -> pixels[]==g.pixels[]
				mainGraphics.loadPixels();
				fastSmallBlur(mainGraphics, imageBuffer
						, blurRadius
						, blurRadius
				); //g=PImage of main window
				imageBuffer.updatePixels();
				mainGraphics.image(imageBuffer, 0, 0); //draw results;
			}
		}
	}

	/**
	 * Superfast blur II. by dotlassie,
	 * licensed under Creative Commons Attribution-Share Alike 3.0 and GNU GPL license.
	 * Work: http://openprocessing.org/visuals/?visualID=36882
	 * License:
	 * http://creativecommons.org/licenses/by-sa/3.0/
	 * http://creativecommons.org/licenses/GPL/2.0/
	 * <p/>
	 * Fast: 40 times faster than filter(BLUR,1);
	 * Small: Available only in 1 pixel radius
	 * Shitty: Rounding errors make image dark soon
	 * What happens:
	 * 11111100 11111100 11111100 11111100 = mask == FF-3 = FCFCFCFC
	 * AAAAAAAA RRRRRRRR GGGGGGGG BBBBBBBB = PImage.pixel[i]
	 * AAAAAA00 RRRRRR00 GGGGGG00 BBBBBB00 = masked pixel
	 * AA AAAAAARR RRRRRRGG GGGGGGBB BBBBBB00 = sum of four masked pixel, alpha overflows, who cares
	 * 00AAAAAA RRRRRRRR GGGGGGGG BBBBBBBB 00 = shift results to right -> broken alpha, good RGB (rounded down) averages
	 */
	void fastSmallBlur(PImage a, PImage b, float hradius, float vradius)
	{
		//a=src, b=dest img
		int pa[] = a.pixels;
		int pb[] = b.pixels;
		int h = a.height;
		int w = a.width;
		int rowStart, rowEnd, y, i;
		int rounding;
		int hiradius = 0, viradius = 0;
		float hfradius = 0, vfradius = 0;

		// TODO: unrandomize, use frameCount if possible for less flicker
		hfradius = random(0, hradius);
		vfradius = random(0, vradius);
		hiradius += floor(hfradius) + (hfradius % 1 > random(0, 1) ? 1 : 0);
		viradius += floor(vfradius) + (vfradius % 1 > random(0, 1) ? 1 : 0);

		if (viradius + hiradius <= 0)
		{
			arrayCopy(pa, pb);
			return;
		}

		int rowStep = w * viradius;

		for (y = viradius; y < h - viradius; ++y)
		{ //edge pixels ignored
			rowStart = y * w + hiradius;
			rowEnd = y * w + w - hiradius;
			rounding = ((y ^ frameCount) & 1) == 0 ? 0x00010101 : 0x00020202; // add +1.5 (average) to prevent darkening, use ? 0xC0010101 : 0xC0020202 to fix 100% alpha if necessary
			for (i = rowStart; i < rowEnd; ++i)
			{
				pb[i] = ((
						((pa[i - rowStep] & 0x00FCFCFC) // sum of neighbours only, center pixel ignored
								+ (pa[i + rowStep] & 0x00FCFCFC)
								+ (pa[i - hiradius] & 0x00FCFCFC)
								+ (pa[i + hiradius] & 0x00FCFCFC)
						) >> 2)
						//|0xFF000000 // set opacity to 100%, use this or the rounding value if necessary
				)
						+ rounding;
				//pb[i]-=0x020202; // uncomment for glitch mode
			}
		}
	}


	public void personEntered(TSPSPerson p)
	{
	}

	public void personUpdated(TSPSPerson p)
	{
	}

	public void personLeft(TSPSPerson p)
	{
	}
}