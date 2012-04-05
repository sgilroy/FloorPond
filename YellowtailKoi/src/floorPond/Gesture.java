package floorPond;

import processing.core.PApplet;
import processing.core.PVector;

import java.awt.*;

public class Gesture
{
	public static final int COMPLETE_GESTURE_ALPHA = 128;
	public static final int INCOMPLETE_GESTURE_ALPHA = 255;
	float damp = 5.0f;
	float dampInv = 1.0f / damp;
	float damp1 = damp - 1;

	int w;
	int h;
	int capacity;

	Vec3f path[];
	int crosses[];
	Polygon polygons[];
	int advanceTimes[];
	int nPoints;
	int nPolys;

	float jumpDx, jumpDy;
	boolean exists;
	float INIT_TH = 14;
	float thickness = INIT_TH;
	WrappedView wrappedView;
	private PApplet applet;

	//	private long currentAge = 0;
	//  long invisibleAge = 60 * 60 * 2;
	/*
	 * Number of frames (age) after which the gesture will be faded to invisible
	 */
	long invisibleAge = 1000 * 60 * 5;
	private boolean complete;
	private int birthTime = -1;
	private int advanceIndex = 0;
	private int advanceRestarts = 0;
	private int clearTime;

	Gesture(int mw, int mh, WrappedView _wrappedView, PApplet applet)
	{
		w = mw;
		h = mh;
		wrappedView = _wrappedView;
		this.applet = applet;

		capacity = 600;
		path = new Vec3f[capacity];
		polygons = new Polygon[capacity];
		crosses = new int[capacity];
		advanceTimes = new int[capacity];
		for (int i = 0; i < capacity; i++)
		{
			polygons[i] = new Polygon();
			polygons[i].npoints = 4;
			path[i] = new Vec3f();
			crosses[i] = 0;
			advanceTimes[i] = 0;
		}
		nPoints = 0;
		nPolys = 0;

		exists = false;
		jumpDx = 0;
		jumpDy = 0;

		initializeGestureTimes();
	}

	void clear()
	{
		nPoints = 0;
		exists = false;
		thickness = INIT_TH;
		initializeGestureTimes();
		advanceIndex = 0;
		advanceRestarts = 0;
		complete = false;
	}

	private void initializeGestureTimes()
	{
		setBirthTime(-1);
		clearTime = applet.millis();
	}

	private void setBirthTime(int now)
	{
		birthTime = now;
	}

	void clearPolys()
	{
		nPolys = 0;
	}

	void addPoint(float x, float y)
	{

		if (nPoints >= capacity)
		{
			// there are all sorts of possible solutions here,
			// but for abject simplicity, I don't do anything.
		} else
		{
			advanceTimes[nPoints] = applet.millis() - clearTime;

			float v = distToLast(x, y);
			float p = getPressureFromVelocity(v);
			path[nPoints++].set(x, y, p);

			if (nPoints > 1)
			{
				exists = true;
				jumpDx = path[nPoints - 1].x - path[0].x;
				jumpDy = path[nPoints - 1].y - path[0].y;
			}
		}

	}

	float getPressureFromVelocity(float v)
	{
		final float scale = 18;
		final float minP = 0.02f;
		final float oldP = (nPoints > 0) ? path[nPoints - 1].p : 0;
		return ((minP + PApplet.max(0, 1.0f - v / scale)) + (damp1 * oldP)) * dampInv;
	}

	void setPressures()
	{
		// pressures vary from 0...1
		float pressure;
		Vec3f tmp;
		float t = 0;
		float u = 1.0f / (nPoints - 1) * PApplet.TWO_PI;
		for (int i = 0; i < nPoints; i++)
		{
			pressure = PApplet.sqrt((1.0f - PApplet.cos(t)) * 0.5f);
			path[i].p = pressure;
			t += u;
		}
	}

	float distToLast(float ix, float iy)
	{
		if (nPoints > 0)
		{
			Vec3f v = path[nPoints - 1];
			float dx = v.x - ix;
			float dy = v.y - iy;
			return PApplet.mag(dx, dy);
		} else
		{
			return 30;
		}
	}

	void compile()
	{
		// compute the polygons from the path of Vec3f's
		if (exists)
		{
			clearPolys();

			Vec3f p0, p1, p2;
			float radius0, radius1;
			float ax, bx, cx, dx;
			float ay, by, cy, dy;
			int axi, bxi, cxi, dxi, axip, axid;
			int ayi, byi, cyi, dyi, ayip, ayid;
			float p1x, p1y;
			float dx01, dy01, hp01, si01, co01;
			float dx02, dy02, hp02, si02, co02;
			float dx13, dy13, hp13, si13, co13;
			float taper = 1.0f;

			int nPathPoints = nPoints - 1;
			int lastPolyIndex = nPathPoints - 1;
			float npm1finv = 1.0f / PApplet.max(1, nPathPoints - 1);

			// handle the first point
			p0 = path[0];
			p1 = path[1];
			radius0 = p0.p * thickness;
			dx01 = p1.x - p0.x;
			dy01 = p1.y - p0.y;
			hp01 = PApplet.sqrt(dx01 * dx01 + dy01 * dy01);
			if (hp01 == 0)
			{
				hp02 = 0.0001f;
			}
			co01 = radius0 * dx01 / hp01;
			si01 = radius0 * dy01 / hp01;
			ax = p0.x - si01;
			ay = p0.y + co01;
			bx = p0.x + si01;
			by = p0.y - co01;

			int xpts[];
			int ypts[];

			int LC = -wrappedView.offScreenGutter + 20;
			int RC = w + wrappedView.offScreenGutter - 20;
			int TC = -wrappedView.offScreenGutter + 20;
			int BC = h + wrappedView.offScreenGutter - 20;
			float mint = 0.618f;
			float tapow = 0.4f;

			// handle the middle points
			int i = 1;
			Polygon apoly;
			for (i = 1; i < nPathPoints; i++)
			{
				taper = PApplet.pow((lastPolyIndex - i) * npm1finv, tapow);

				p0 = path[i - 1];
				p1 = path[i];
				p2 = path[i + 1];
				p1x = p1.x;
				p1y = p1.y;
				radius1 = Math.max(mint, taper * p1.p * thickness);

				// assumes all segments are roughly the same length...
				dx02 = p2.x - p0.x;
				dy02 = p2.y - p0.y;
				hp02 = (float) Math.sqrt(dx02 * dx02 + dy02 * dy02);
				if (hp02 != 0)
				{
					hp02 = radius1 / hp02;
				}
				co02 = dx02 * hp02;
				si02 = dy02 * hp02;

				// translate the integer coordinates to the viewing rectangle
				axi = axip = (int) ax;
				ayi = ayip = (int) ay;
				axi = getViewX(axi);
				axid = axi - axip;
				ayi = getViewY(ayi);
				ayid = ayi - ayip;

				// set the vertices of the polygon
				apoly = polygons[nPolys++];
				xpts = apoly.xpoints;
				ypts = apoly.ypoints;
				xpts[0] = axi = axid + axip;
				xpts[1] = bxi = axid + (int) bx;
				xpts[2] = cxi = axid + (int) (cx = p1x + si02);
				xpts[3] = dxi = axid + (int) (dx = p1x - si02);
				ypts[0] = ayi = ayid + ayip;
				ypts[1] = byi = ayid + (int) by;
				ypts[2] = cyi = ayid + (int) (cy = p1y - co02);
				ypts[3] = dyi = ayid + (int) (dy = p1y + co02);

				// keep a record of where we cross the edge of the screen
				crosses[i] = 0;
				if ((axi <= LC) || (bxi <= LC) || (cxi <= LC) || (dxi <= LC))
				{
					crosses[i] |= 1;
				}
				if ((axi >= RC) || (bxi >= RC) || (cxi >= RC) || (dxi >= RC))
				{
					crosses[i] |= 2;
				}
				if ((ayi <= TC) || (byi <= TC) || (cyi <= TC) || (dyi <= TC))
				{
					crosses[i] |= 4;
				}
				if ((ayi >= BC) || (byi >= BC) || (cyi >= BC) || (dyi >= BC))
				{
					crosses[i] |= 8;
				}

				//swap data for next time
				ax = dx;
				ay = dy;
				bx = cx;
				by = cy;
			}

			// handle the last point
			p2 = path[nPathPoints];
			apoly = polygons[nPolys++];
			xpts = apoly.xpoints;
			ypts = apoly.ypoints;

			xpts[0] = (int) ax;
			xpts[1] = (int) bx;
			xpts[2] = (int) (p2.x);
			xpts[3] = (int) (p2.x);

			ypts[0] = (int) ay;
			ypts[1] = (int) by;
			ypts[2] = (int) (p2.y);
			ypts[3] = (int) (p2.y);

		}
	}

	void smooth()
	{
		// average neighboring points

		final float weight = 18;
		final float scale = 1.0f / (weight + 2);
		int nPointsMinusTwo = nPoints - 2;
		Vec3f lower, upper, center;

		for (int i = 1; i < nPointsMinusTwo; i++)
		{
			lower = path[i - 1];
			center = path[i];
			upper = path[i + 1];

			center.x = (lower.x + weight * center.x + upper.x) * scale;
			center.y = (lower.y + weight * center.y + upper.y) * scale;
		}
	}

	PVector getViewCoordinate(float ax, float ay)
	{
//    int   axi, ayi;
//
//    // translate the integer coordinates to the viewing rectangle
//    axi = (int)ax;
//    ayi = (int)ay;
//    axi=(axi<0)?(w-((-axi)%w)):axi%w;
//    ayi=(ayi<0)?(h-((-ayi)%h)):ayi%h;

		return new PVector(getViewX((int) ax), getViewY((int) ay));
	}

	int getViewX(int axi)
	{
//    axi=(axi<0)?(w-((-axi)%w)):axi%w;
//    return (axi<-offScreenGutter)?(w + offScreenGutter - ((-axi + offScreenGutter)%virtualW)) : ((axi + offScreenGutter) % virtualW) - offScreenGutter;
		return wrappedView.getViewX(axi);
	}

	int getViewY(int ayi)
	{
//    ayi=(ayi<0)?(h-((-ayi)%h)):ayi%h;
//    return (ayi<-offScreenGutter)?(h + offScreenGutter - ((-ayi + offScreenGutter)%virtualH)):ayi%virtualH;
		return wrappedView.getViewY(ayi);
	}

	int getAlpha()
	{
		return getCurrentAge() == 0 ? INCOMPLETE_GESTURE_ALPHA : (int) PApplet.lerp(COMPLETE_GESTURE_ALPHA, 0, (float) getCurrentAge() / invisibleAge);
	}

	boolean isInvisible()
	{
		return getCurrentAge() >= invisibleAge;
	}

	public void end()
	{
		complete = true;
		setBirthTime(applet.millis());
	}

	public boolean isComplete()
	{
		return complete;
	}

	public int getCurrentAge()
	{
		return birthTime == -1 ? 0 : applet.millis() - birthTime;
	}

	public boolean startAdvance()
	{
		int remainderAge = getCurrentAge() - (getRestartAge() * advanceRestarts);
		if (remainderAge >= advanceTimes[advanceIndex])
		{
			advanceIndex++;
			if (advanceIndex >= nPoints)
			{
				advanceIndex = 0;
				advanceRestarts++;
			}
			return true;
		}
		return false;
	}

	public int getRestartAge()
	{
		return advanceTimes[nPoints - 1];
	}
}
