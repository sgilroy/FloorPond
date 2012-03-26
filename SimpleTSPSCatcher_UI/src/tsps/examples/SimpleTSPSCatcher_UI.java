package tsps.examples;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PVector;
import tsps.TSPS;
import tsps.TSPSPerson;

import java.util.ArrayList;

public class SimpleTSPSCatcher_UI extends PApplet
{
	TSPS tspsReceiver;

	//width of active area ( used to scale bounding boxes )
	float activeWidth;
	float activeHeight;

	UI ui;

	//custom people objects
	ArrayList<CustomPerson> customPeople;
	String backgrounds[] = new String[20];


	int currentBackground = 0;
	private static boolean fullScreen;

	public static void main(String args[])
	{
		if (args.length > 0 && args[0].equals("--present"))
		{
			fullScreen = true;
			PApplet.main(new String[]{"--present", "tsps.examples.SimpleTSPSCatcher_UI"});
		}
		else
		{
			PApplet.main(new String[]{"tsps.examples.SimpleTSPSCatcher_UI"});
		}
	}

	public void setup()
	{
		if (fullScreen)
		{
			size(screen.width, screen.height);
		}
		else
		{
			size(1024, 768);
		}

		activeWidth = width;
		activeHeight = height - 68; //to accomadate for status bar
		for (int i = 0; i < 20; i++)
		{
			backgrounds[i] = "backgrounds/background_" + (i + 1) + ".png";
		}

		//functions for drawing UI elements, etc
		ui = new UI(this);
		customPeople = new ArrayList<CustomPerson>();

		//all you need to do to start TSPS
		tspsReceiver = new TSPS(this, 12000);
	}

	public void draw()
	{
		tspsReceiver.update();
		ui.draw();

		//you can loop through all the people elements in TSPS if you choose
		/*
		for (int i=0; i<tspsReceiver.people.size(); i++)
		{
			//get person
			TSPSPerson person = (TSPSPerson) tspsReceiver.people.get(i);

			//draw rect + centroid scaled by activeWidth + activeHeight
			fill(120,120,0);
			rect(person.boundingRect.x*activeWidth, person.boundingRect.y*activeHeight, person.boundingRect.width*activeWidth, person.boundingRect.height*activeHeight);
			fill(255,255,255);
			ellipse(person.centroid.x*activeWidth, person.centroid.y*activeHeight, 10, 10);
			text("id: "+person.id+" age: "+person.age, person.boundingRect.x*activeWidth, (person.boundingRect.y*activeHeight + person.boundingRect.height*activeHeight) + 2);
		};
		*/

		//loop through custom person objects
		for (int i = customPeople.size() - 1; i >= 0; i--)
		{
			CustomPerson p = customPeople.get(i);
			if (!p.dead) p.draw();
			else customPeople.remove(i);
		}
	}

	public void personEntered(TSPSPerson p)
	{
		CustomPerson person = new CustomPerson(p, activeWidth, activeHeight);
		person.loadBackground(backgrounds[currentBackground]);

		currentBackground++;
		if (currentBackground >= 20) currentBackground = 0;
		customPeople.add(person);
		ui.personEntered();
	}

	public void personUpdated(TSPSPerson p)
	{
		for (int i = 0; i < customPeople.size(); i++)
		{
			CustomPerson lookupPerson = customPeople.get(i);
			if (p.id == lookupPerson.id)
			{
				lookupPerson.update(p);
				break;
			}
		}

		ui.personUpdated();
	}

	public void personLeft(TSPSPerson p)
	{
		println("person left with id " + p.id);
		for (int i = 0; i < customPeople.size(); i++)
		{
			CustomPerson lookupPerson = customPeople.get(i);
			if (p.id == lookupPerson.id)
			{
				//lookupPerson.update(p);
				lookupPerson.dead = true;
				//customPeople.remove(i);
				break;
			}
		}

		ui.personLeft();
	}


	//custom person for storage of custom variables
	class CustomPerson
	{
		int id;
		int age;
		PVector centroid;
		PVector velocity;
		tsps.Rectangle boundingRect;
		ArrayList contours;
		boolean dead;

		//custom vars
		PImage backgroundImage;
		PImage centroidImage;
		boolean backgroundLoaded = false;
		PFont timesBold18;
		PImage idAgeImage;
		PImage hBoundary1;
		PImage hBoundary2;
		PImage vBoundary1;
		PImage vBoundary2;
		float activeWidth, activeHeight;

		int[] mask1;
		int[] mask2;
		int[] mask3;
		int[] mask4;

		CustomPerson(TSPSPerson p, float _activeWidth, float _activeHeight)
		{
			activeWidth = _activeWidth;
			activeHeight = _activeHeight;
			id = p.id;
			contours = new ArrayList();

			timesBold18 = loadFont("TimesNewRomanPS-BoldItalicMT-18.vlw");
			textFont(timesBold18, 18);
			centroidImage = loadImage("CenterPoint.png");
			idAgeImage = loadImage("idAge.png");

			//load images + set up masks for bounding boxes
			hBoundary1 = loadImage("boundaries/HorizBoundry1.png");
			hBoundary2 = loadImage("boundaries/HorizBoundry2.png");
			vBoundary1 = loadImage("boundaries/VertBoundry1.png");
			vBoundary2 = loadImage("boundaries/VertBoundry2.png");
			mask1 = new int[hBoundary1.width * hBoundary1.height];
			mask2 = new int[hBoundary2.width * hBoundary2.height];
			mask3 = new int[vBoundary1.width * vBoundary1.height];
			mask4 = new int[vBoundary2.width * vBoundary2.height];

			//update all vars
			update(p);
		}

		void loadBackground(String image)
		{
			backgroundImage = loadImage(image);
			backgroundLoaded = true;
		}

		void update(TSPSPerson p)
		{
			age = p.age;
			boundingRect = p.boundingRect;
			centroid = p.centroid;
			velocity = p.velocity;
			contours = p.contours;
			dead = p.dead;

			//update + setmasks

			for (int x = 0; x < hBoundary1.width; x++)
			{
				for (int y = 0; y < hBoundary1.height; y++)
				{
					if (x <= boundingRect.width * activeWidth)
					{
						mask1[x + y * hBoundary1.height] = 255;
						mask2[x + y * hBoundary1.height] = 255;
					} else
					{
						mask1[x + y * hBoundary1.height] = 0;
						mask2[x + y * hBoundary1.height] = 0;
					}
				}
			}
			for (int y = 0; y < vBoundary1.height; y++)
			{
				for (int x = 0; x < vBoundary1.width; x++)
				{
					if (y <= boundingRect.height * activeHeight)
					{
						mask3[y + x * vBoundary1.width] = 255;
						mask4[y + x * vBoundary1.width] = 255;
					} else
					{
						mask3[y + x * vBoundary1.width] = 0;
						mask4[y + x * vBoundary1.width] = 0;
					}
				}
			}
			hBoundary1.mask(mask1);
			hBoundary2.mask(mask2);
			vBoundary1.mask(mask3);
			vBoundary2.mask(mask4);
		}

		void draw()
		{
			//subtract age to delete eventually
			age--;
			if (age < -2) dead = true;
			tint(255, 175);
			if (backgroundLoaded) image(backgroundImage, boundingRect.x * activeWidth, boundingRect.y * activeHeight,
										boundingRect.width * activeWidth, boundingRect.height * activeHeight);
			tint(255);
			image(hBoundary1, boundingRect.x * activeWidth, boundingRect.y * activeHeight);
			image(hBoundary2, boundingRect.x * activeWidth,
				  boundingRect.y * activeHeight + boundingRect.height * activeHeight);
			image(vBoundary1, boundingRect.x * activeWidth, boundingRect.y * activeHeight - 1);
			image(vBoundary2, boundingRect.x * activeWidth + boundingRect.width * activeWidth,
				  boundingRect.y * activeHeight);

			imageMode(CENTER);
			image(centroidImage, centroid.x * activeWidth, centroid.y * activeHeight);
			imageMode(CORNER);

			image(idAgeImage, boundingRect.x * activeWidth + 5, boundingRect.y * activeHeight + 5);
			fill(255);
			text(Integer.toString(id), boundingRect.x * activeWidth + 22, boundingRect.y * activeHeight + 19);
			text(Integer.toString(age), boundingRect.x * activeWidth + 82, boundingRect.y * activeHeight + 19);

			noFill();
			stroke(255, 100);
			beginShape();
			for (int i = 0; i < contours.size(); i++)
			{
				PVector pt = (PVector) contours.get(i);
				vertex(pt.x * activeWidth, pt.y * activeHeight);
			}
			endShape(CLOSE);
			fill(255);
		}
	}

	class UI
	{
		PApplet parent;
		PFont timesBold18;
		PImage statusBar;
		PImage numberOfPeople;
		PImage personEnteredNotice;
		PImage personUpdatedNotice;
		PImage personLeftNotice;

		int bPersonEntered;
		int bPersonUpdated;
		int bPersonLeft;

		UI(PApplet _parent)
		{
			parent = _parent;
			hint(ENABLE_NATIVE_FONTS);
			timesBold18 = loadFont("TimesNewRomanPS-BoldItalicMT-18.vlw");
			textFont(timesBold18, 18);
			smooth();
			noStroke();

			bPersonEntered = bPersonUpdated = bPersonLeft = 0;

			loadImages();
			drawBackground();
		}

		void draw()
		{
			drawBackground();
			if (bPersonEntered > 0)
			{
				image(personEnteredNotice, 397, height - 40);
				bPersonEntered--;
				if (bPersonEntered < 0) bPersonEntered = 0;
			}

			if (bPersonUpdated > 0)
			{
				image(personUpdatedNotice, 533, height - 40);
				bPersonUpdated--;
				if (bPersonUpdated < 0) bPersonUpdated = 0;
			}

			if (bPersonLeft > 0)
			{
				image(personLeftNotice, 666, height - 40);
				bPersonLeft--;
				if (bPersonLeft < 0) bPersonLeft = 0;
			}

			//number of people
			fill(43, 150, 152);
			textFont(timesBold18, 18);
			text(Integer.toString(customPeople.size()), 347, height - 28);
			fill(255);
		}

		void personEntered()
		{
			bPersonEntered = 5;
		}

		void personUpdated()
		{
			bPersonUpdated = 5;
		}

		void personLeft()
		{
			bPersonLeft = 5;
		}

		void drawBackground()
		{
//			background(148, 129, 85);
			background(0);
			image(statusBar, 0, height - 68);
		}

		void loadImages()
		{
			statusBar = loadImage("bottomBar.png");
			personEnteredNotice = loadImage("triggers/PersonEntered_Active.png");
			personUpdatedNotice = loadImage("triggers/PersonUpdated_Active.png");
			personLeftNotice = loadImage("triggers/PersonLeft_Active.png");
		}
	}
}
