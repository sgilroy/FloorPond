package floorPond;

import processing.core.PVector;

class WrappedView
{
	int w;
	int h;
	int offScreenGutter = 0;
	int virtualW;
	int virtualH;

	WrappedView(int mw, int mh, int mOffScreenGutter)
	{
		w = mw;
		h = mh;
		offScreenGutter = mOffScreenGutter;
		virtualW = w + offScreenGutter * 2;
		virtualH = h + offScreenGutter * 2;
	}

	int getViewX(int axi)
	{
//    axi=(axi<0)?(w-((-axi)%w)):axi%w;
		return (axi < -offScreenGutter) ? (w + offScreenGutter - ((-axi - offScreenGutter) % virtualW)) : ((axi + offScreenGutter) % virtualW) - offScreenGutter;
	}

	int getViewY(int ayi)
	{
//    ayi=(ayi<0)?(h-((-ayi)%h)):ayi%h;
		return (ayi < -offScreenGutter) ? (h + offScreenGutter - ((-ayi - offScreenGutter) % virtualH)) : ((ayi + offScreenGutter) % virtualH) - offScreenGutter;
	}

	float getViewX(float axi)
	{
//    axi=(axi<0)?(w-((-axi)%w)):axi%w;
		return (axi < -offScreenGutter) ? (w + offScreenGutter - ((-axi - offScreenGutter) % virtualW)) : ((axi + offScreenGutter) % virtualW) - offScreenGutter;
	}

	float getViewY(float ayi)
	{
//    ayi=(ayi<0)?(h-((-ayi)%h)):ayi%h;
		return (ayi < -offScreenGutter) ? (h + offScreenGutter - ((-ayi - offScreenGutter) % virtualH)) : ((ayi + offScreenGutter) % virtualH) - offScreenGutter;
	}
	
	public PVector targetForWrapping(PVector targetPosition, PVector currentPosition)
	{
		// wrap targeting
		PVector wrappedTargetPosition = targetPosition.get();
		if (Math.abs(wrappedTargetPosition.x - currentPosition.x) > Math.abs(
				wrappedTargetPosition.x - virtualW - currentPosition.x))
			wrappedTargetPosition.x -= virtualW;
		else if (Math.abs(wrappedTargetPosition.x - currentPosition.x) > Math.abs(
				wrappedTargetPosition.x + virtualW - currentPosition.x))
			wrappedTargetPosition.x += virtualW;

		if (Math.abs(wrappedTargetPosition.y - currentPosition.y) > Math.abs(
				wrappedTargetPosition.y - virtualH - currentPosition.y))
			wrappedTargetPosition.y -= virtualH;
		else if (Math.abs(wrappedTargetPosition.y - currentPosition.y) > Math.abs(
				wrappedTargetPosition.y + virtualH - currentPosition.y))
			wrappedTargetPosition.y += virtualH;

		return wrappedTargetPosition;
	}

}