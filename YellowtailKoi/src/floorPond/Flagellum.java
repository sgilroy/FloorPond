package floorPond;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

/**
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
	protected float theta = 180;
	private float count = 0;

	private Node[] node = new Node[numNodes];

	private PImage skin;
	private PApplet applet;
	private String skinFileName;
	private boolean flagellumStructureVisible = false;
	private float scale;
	private SkinOutline skinOutline;

	Flagellum(PApplet applet, String skinFileName)
	{
		this.applet = applet;
		this.skinFileName = skinFileName;

		// random image resize
		setScale(applet.random(0.75f, 1));

		// initialize nodes
		for (int n = 0; n < numNodes; n++) node[n] = new Node();

		muscleFreq = applet.random(0.06f, 0.07f);
	}

	public float getScale()
	{
		return scale;
	}

	public void setScale(float scale)
	{
		this.scale = scale;

		skin = applet.loadImage(skinFileName);

		skin.resize((int)(skin.width * this.scale),(int)(skin.height * this.scale));

		// nodes spacing
		skinXspacing = skin.width /(float)(numNodes - 1);
		skinYspacing = skin.height / 2;

		skinOutline = new SkinOutline(applet, skin, numNodes, skinXspacing);
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
		if (flagellumStructureVisible)
		{
			applet.stroke(255);
			applet.strokeWeight(1);
		}
		else
		{
			applet.noStroke();
		}
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
			float x1 = node[n].x + PApplet.sin(angle) * skinOutline.getVertexY(n, SkinOutline.BOTTOM);
			float y1 = node[n].y + PApplet.cos(angle) * skinOutline.getVertexY(n, SkinOutline.BOTTOM);
			float x2 = node[n].x + PApplet.sin(angle) * skinOutline.getVertexY(n, SkinOutline.TOP);
			float y2 = node[n].y + PApplet.cos(angle) * skinOutline.getVertexY(n, SkinOutline.TOP);
			float u = skinXspacing * n;
			applet.vertex(x1, y1, u, skinOutline.getTextureCoord(n, SkinOutline.BOTTOM));
			applet.vertex(x2, y2, u, skinOutline.getTextureCoord(n, SkinOutline.TOP));
		}
		applet.endShape();
	}

	/**
	 * Normalizes an angle between 0 and PI
	 * @param angle
	 * @return
	 */
	public static float normalizeAngle(float angle)
	{
		if(angle < 0 || angle > Math.PI * 2)
			return Math.abs(((float)Math.PI * 2) - Math.abs(angle));
		else
			return angle;
	}

	/**
	 * Normalizes a delta between -PI and +PI
	 * @param angle
	 * @return
	 */
	public static float normalizeAngleDelta(float angle)
	{
		if(angle < -Math.PI || angle > Math.PI)
			return Math.abs(((float)Math.PI * 2) - Math.abs(angle + (float)Math.PI)) - (float)Math.PI;
		else
			return angle;
	}

	public boolean isFlagellumStructureVisible()
	{
		return flagellumStructureVisible;
	}

	public void setFlagellumStructureVisible(boolean flagellumStructureVisible)
	{
		this.flagellumStructureVisible = flagellumStructureVisible;
	}

	public boolean isOutlineIgnored()
	{
		return skinOutline.isOutlineIgnored();
	}

	public void setOutlineIgnored(boolean ignoreOutline)
	{
		skinOutline.setOutlineIgnored(ignoreOutline);
	}
}
