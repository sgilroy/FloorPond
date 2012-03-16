package floorPond;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

/*
	Fish locomotion class
	Logic from levitated.com, simulates wave propagation through a kinetic array of nodes
	also some bits from flight404 blog
*/
public class Flagellum
{

	private int numNodes = 16;
	private float skinXspacing, skinYspacing;		  // store distance for vertex points that builds the shape
	private float muscleRange = 6;					 // controls rotation angle of the neck
	protected float muscleFreq;	 //
	private float theta_vel;
	protected float theta = 180;
	private float theta_friction = 0.6f;
	private float count = 0;

	private Node[] node = new Node[numNodes];

	private PImage skin;
	private PApplet applet;


	Flagellum(PApplet applet, String _skin)
	{
		this.applet = applet;
		skin = applet.loadImage(_skin);

		// random image resize
		float scalar = applet.random(0.75f, 1);
		skin.resize((int)(skin.width * scalar),(int)(skin.height * scalar));

		// nodes spacing
		skinXspacing = skin.width /(float)(numNodes) + 0.5f;
		skinYspacing = skin.height / 2;

		// initialize nodes
		for (int n = 0; n < numNodes; n++) node[n] = new Node();

		muscleFreq = applet.random(0.06f, 0.07f);
	}


	void move()
	{

		// head node
		node[0].x = PApplet.cos(PApplet.radians(theta));
		node[0].y = PApplet.sin(PApplet.radians(theta));

		// muscular node (neck)
		count += muscleFreq;
		float thetaMuscle = muscleRange * PApplet.sin(count);
		node[1].x = -skinXspacing * PApplet.cos(PApplet.radians(theta + thetaMuscle)) + node[0].x;
		node[1].y = -skinXspacing * PApplet.sin(PApplet.radians(theta + thetaMuscle)) + node[0].y;

		// apply kinetic force trough body nodes (spine)
		for (int n = 2; n < numNodes; n++)
		{
			float dx = node[n].x - node[n - 2].x;
			float dy = node[n].y - node[n - 2].y;
			float d = PApplet.sqrt(dx * dx + dy * dy);
			node[n].x = node[n - 1].x + (dx * skinXspacing) / d;
			node[n].y = node[n - 1].y + (dy * skinXspacing) / d;
		}
	}


	void display()
	{
		applet.noStroke();
		applet.beginShape(PConstants.QUAD_STRIP);
		applet.texture(skin);
		for (int n = 0; n < numNodes; n++)
		{
			float dx;
			float dy;
			if (n == 0)
			{
				dx = node[1].x - node[0].x;
				dy = node[1].y - node[0].y;
			} else
			{
				dx = node[n].x - node[n - 1].x;
				dy = node[n].y - node[n - 1].y;
			}
			float angle = -PApplet.atan2(dy, dx);
			float x1 = node[n].x + PApplet.sin(angle) * -skinYspacing;
			float y1 = node[n].y + PApplet.cos(angle) * -skinYspacing;
			float x2 = node[n].x + PApplet.sin(angle) * skinYspacing;
			float y2 = node[n].y + PApplet.cos(angle) * skinYspacing;
			float u = skinXspacing * n;
			applet.vertex(x1, y1, u, 0);
			applet.vertex(x2, y2, u, skin.height);
		}
		applet.endShape();
	}
}
