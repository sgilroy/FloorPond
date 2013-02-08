package floorPond;

import processing.core.PApplet;
import processing.core.PImage;

import java.util.ArrayList;

public class SkinOutline
{
	public static final int BOTTOM = 0;
	public static final int TOP = 1;
	private PApplet applet;
	private PImage skin;
	private final int numNodes;
	private final float skinXspacing;
	private boolean outlineIgnored;
	private ArrayList<ArrayList<Integer>> maxDelta;
	private ArrayList<ArrayList<Integer>> vertices;

	public SkinOutline(PApplet applet, PImage skin, int numNodes, float skinXspacing)
	{
		this.applet = applet;
		this.skin = skin;
		this.numNodes = numNodes;
		this.skinXspacing = skinXspacing;

		scanSkin();
	}

	private void scanSkin()
	{
		maxDelta = new ArrayList<ArrayList<Integer>>(2);
		vertices = new ArrayList<ArrayList<Integer>>(2);

		skin.loadPixels();
		for (int side = 0; side < 2; side++)
		{
			ArrayList<Integer> currentSideMaxDeltaArray = new ArrayList<Integer>(numNodes);
			for (int quad = 0; quad < numNodes; quad++)
			{
				currentSideMaxDeltaArray.add(0);
			}
			maxDelta.add(currentSideMaxDeltaArray);
			int direction = side == BOTTOM ? -1 : 1;

			for (int x = 0; x < skin.width; x++)
			{
				int quad = (int) (x / skinXspacing);
				for (int j = skin.height / 2 - 1; j >= 0; j--)
				{
					int delta = j * direction;
					int y = skin.height / 2 + delta;

					// scan vertically from the edge towards the center of the skin until we find a non-transparent pixel
					if (applet.alpha(skin.get(x, y)) > 0)
					{
						currentSideMaxDeltaArray.set(quad, Math.max(currentSideMaxDeltaArray.get(quad), Math.abs(delta)));
						break;
					}
				}
			}

			ArrayList<Integer> currentSideVerticesArray = new ArrayList<Integer>(numNodes);
			for (int quad = 0; quad < numNodes; quad++)
			{
				currentSideVerticesArray.add(Math.max(currentSideMaxDeltaArray.get(quad), currentSideMaxDeltaArray.get(quad - (quad == 0 ? 0 : 1))));
			}
			vertices.add(currentSideVerticesArray);
		}
	}

	public float getVertexY(int vertexIndex, int side)
	{
		int direction = side == BOTTOM ? -1 : 1;
		if (isOutlineIgnored())
		{
			return skin.height / 2 * direction;
		}
		else
		{
			return vertices.get(side).get(vertexIndex) * direction;
		}
	}

	public float getSkinHeight(int vertexIndex)
	{
		return getVertexY(vertexIndex, TOP) - getVertexY(vertexIndex, BOTTOM);
	}

	public float getTextureCoord(int vertexIndex, int side)
	{
		return getVertexY(vertexIndex, side) + skin.height / 2;
	}

	public boolean isOutlineIgnored()
	{
		return outlineIgnored;
	}

	public void setOutlineIgnored(boolean outlineIgnored)
	{
		this.outlineIgnored = outlineIgnored;
	}
}
