package floorPond;

/*
  how this works can be found here
  http://www.gamedev.net/reference/articles/article915.asp

  this end up as a simplified version of
  Riu Gil water sketch in openprocessing site
  http://www.openprocessing.org/visuals/?visualID=668
*/

import processing.core.PApplet;
import processing.core.PImage;

class Ripple
{

	int heightMap[][][];			 // water surface (2 pages).
	int turbulenceMap[][];		   // turbulence map
	int lineOptimizer[];			 // line optimizer;
	int space;
	int radius, heightMax, density;
	int page = 0;

	PImage water;
	private PApplet applet;


	Ripple(PApplet applet, PImage _water)
	{
		this.applet = applet;
		water = _water;
		density = 4;
		radius = 20;
		space = applet.width * applet.height - 1;

		initMap();
	}


	void update()
	{
		waterFilter();
		updateWater();
		page ^= 1;
	}

	void initMap()
	{
		// the applet.height map is made of two "pages"
		// one to calculate the current state, and another to keep the previous state
		heightMap = new int[2][applet.width][applet.height];

	}


	void makeTurbulence(int cx, int cy)
	{
		int r = radius * radius;
		int left = cx < radius ? -cx + 1 : -radius;
		int right = cx > (applet.width - 1) - radius ? (applet.width - 1) - cx : radius;
		int top = cy < radius ? -cy + 1 : -radius;
		int bottom = cy > (applet.height - 1) - radius ? (applet.height - 1) - cy : radius;

		for (int x = left; x < right; x++)
		{
			int xsqr = x * x;
			for (int y = top; y < bottom; y++)
			{
				if (xsqr + (y * y) < r)
					heightMap[page ^ 1][cx + x][cy + y] += 100;
			}
		}
	}


	void waterFilter()
	{
		for (int x = 0; x < applet.width; x++)
		{
			for (int y = 0; y < applet.height; y++)
			{
				int n = y - 1 < 0 ? 0 : y - 1;
				int s = y + 1 > applet.height - 1 ? applet.height - 1 : y + 1;
				int e = x + 1 > applet.width - 1 ? applet.width - 1 : x + 1;
				int w = x - 1 < 0 ? 0 : x - 1;
				int value = ((heightMap[page][w][n] + heightMap[page][x][n]
						+ heightMap[page][e][n] + heightMap[page][w][y]
						+ heightMap[page][e][y] + heightMap[page][w][s]
						+ heightMap[page][x][s] + heightMap[page][e][s]) >> 2)
						- heightMap[page ^ 1][x][y];

				heightMap[page ^ 1][x][y] = value - (value >> density);
			}
		}
	}

	void updateWater()
	{
		applet.loadPixels();
		for (int y = 0; y < applet.height - 1; y++)
		{
			for (int x = 0; x < applet.width - 1; x++)
			{
				int deltax = heightMap[page][x][y] - heightMap[page][x + 1][y];
				int deltay = heightMap[page][x][y] - heightMap[page][x][y + 1];
				int offsetx = (deltax >> 3) + x;
				int offsety = (deltay >> 3) + y;

				offsetx = offsetx > applet.width ? applet.width - 1 : offsetx < 0 ? 0 : offsetx;
				offsety = offsety > applet.height ? applet.height - 1 : offsety < 0 ? 0 : offsety;

				int offset = (offsety * applet.width) + offsetx;
				offset = offset < 0 ? 0 : offset > space ? space : offset;
				int pixel = water.pixels[offset];
				applet.pixels[y * applet.width + x] = pixel;
			}
		}
		applet.updatePixels();
	}

}
