package floorPond;

import matrixMath.matrixMath;
import processing.core.PApplet;
import processing.core.PVector;
import processing.serial.Serial;

public class BallPositionSensor
{
	boolean debug = false;
	int TABLE_WIDTH = 60;   // inches
	int TABLE_HEIGHT = 54;  // inches

	Serial arduinoSerial;
	matrixMath mc1_l, mc2_l, mc3_l, mc4_l, mc1_r, mc2_r, mc3_r, mc4_r;
	private PApplet applet;

	BallPositionSensor(PApplet applet, String portName, String coefficientFileLeft, String coefficientFileRight)
	{
		this.applet = applet;
		importCoeffs(coefficientFileLeft, false); // "../../coefficients-left.txt"
		importCoeffs(coefficientFileRight, true); // "../../coefficients-right.txt"
		arduinoSerial = new Serial(applet, portName, 9600);
	}

	void importCoeffs(String coefficientFile, boolean isRightSide)
	{
		String[] coeffs = applet.loadStrings(coefficientFile);

		float[][] coeffs1 = new float[2][10];
		float[][] coeffs2 = new float[2][10];
		float[][] coeffs3 = new float[2][10];
		float[][] coeffs4 = new float[2][10];
		for (int i = 0; i < 10; i++)
		{
			coeffs1[0][i] = Float.valueOf(coeffs[i]);
			coeffs2[0][i] = Float.valueOf(coeffs[10 + i]);
			coeffs3[0][i] = Float.valueOf(coeffs[20 + i]);
			coeffs4[0][i] = Float.valueOf(coeffs[30 + i]);

			coeffs1[1][i] = Float.valueOf(coeffs[40 + i]);
			coeffs2[1][i] = Float.valueOf(coeffs[50 + i]);
			coeffs3[1][i] = Float.valueOf(coeffs[60 + i]);
			coeffs4[1][i] = Float.valueOf(coeffs[70 + i]);
		}
		if (isRightSide)
		{
			mc1_r = new matrixMath(coeffs1);
			mc2_r = new matrixMath(coeffs2);
			mc3_r = new matrixMath(coeffs3);
			mc4_r = new matrixMath(coeffs4);
		} else
		{
			mc1_l = new matrixMath(coeffs1);
			mc2_l = new matrixMath(coeffs2);
			mc3_l = new matrixMath(coeffs3);
			mc4_l = new matrixMath(coeffs4);
		}
	}

	Hit readHit()
	{
		String serialData = null;
		if (debug)
		{
			if (applet.random(100) <= 1)
			{
				serialData = "hit: {1908 1120 0 2004 r}";
			} else if (applet.random(100) <= 1)
			{
				serialData = "hit: {1544 668 0 1288 r}";
			} else
			{
				serialData = null;
			}
		} else if (arduinoSerial.available() > 0)
		{
			serialData = arduinoSerial.readStringUntil(10);  //10 is character for linefeed
		}


		if (serialData == null)
		{
			return null;
		} else
		{
			PApplet.println(serialData);
			Hit hit = getHit(serialData);
			// 54 x 60
			//PApplet.println("(" + position.x + ", " + position.y + ")");
			return hit;
		}
	}

	private Hit getHit(String serialData)
	{
		String[] values = PApplet.split(serialData, "{");
		String[] timings = PApplet.split(values[1], " ");

		float t12 = Float.valueOf(timings[0]) - Float.valueOf(timings[1]);
		float t13 = Float.valueOf(timings[0]) - Float.valueOf(timings[2]);
		float t14 = Float.valueOf(timings[0]) - Float.valueOf(timings[3]);
		float t23 = Float.valueOf(timings[1]) - Float.valueOf(timings[2]);
		float t24 = Float.valueOf(timings[1]) - Float.valueOf(timings[3]);
		float t34 = Float.valueOf(timings[2]) - Float.valueOf(timings[3]);

		/*int first_one = 0;
		   int first_two = 0;
		   int first_four = 0;
		   if (int(timings[0]) == 0) {
			 first_one = 1;
		   } else if (int(timings[1]) == 0) {
			 first_two = 1;
		   } else if (int(timings[3]) == 0) {
			 first_four = 1;
		   }*/

		float[] timings1 = {
				PApplet.pow(t12, 2), PApplet.pow(t13, 2), PApplet.pow(t14, 2),
				t12 * t13, t12 * t14, t13 * t14, t12, t13, t14, 1};
		float[] timings2 = {
				PApplet.pow(t12, 2), PApplet.pow(t23, 2), PApplet.pow(t24, 2),
				t12 * t23, t12 * t24, t23 * t24, t12, t23, t24, 1};
		float[] timings3 = {
				PApplet.pow(t13, 2), PApplet.pow(t23, 2), PApplet.pow(t34, 2),
				t13 * t23, t13 * t34, t23 * t34, t13, t23, t34, 1};
		float[] timings4 = {
				PApplet.pow(t14, 2), PApplet.pow(t24, 2), PApplet.pow(t34, 2),
				t14 * t24, t14 * t34, t24 * t34, t14, t24, t34, 1};

		PVector guess1 = null;
		PVector guess2 = null;
		PVector guess3 = null;
		PVector guess4 = null;

		boolean isRightSide = false;
		if (serialData.contains("r}"))
		{
			isRightSide = true;
			guess1 = calcXY(timings1, mc1_r);
			guess2 = calcXY(timings2, mc2_r);
			guess3 = calcXY(timings3, mc3_r);
			guess4 = calcXY(timings4, mc4_r);
		} else if (serialData.contains("l}"))
		{
			guess1 = calcXY(timings1, mc1_l);
			guess2 = calcXY(timings2, mc2_l);
			guess3 = calcXY(timings3, mc3_l);
			guess4 = calcXY(timings4, mc4_l);
			isRightSide = false;
		}

		PApplet.println("guess 1: " + guess1.x + ", " + guess1.y);
		PApplet.println("guess 2: " + guess2.x + ", " + guess2.y);
		PApplet.println("guess 3: " + guess3.x + ", " + guess3.y);
		PApplet.println("guess 4: " + guess4.x + ", " + guess4.y);

		float x_inches = (guess1.x + guess2.x + guess3.x + guess4.x) / 4;
		float y_inches = (guess1.y + guess2.y + guess3.y + guess4.y) / 4;
		PApplet.println(x_inches + ", " + y_inches);

		return new Hit(x_inches, y_inches, isRightSide, TABLE_WIDTH, TABLE_HEIGHT, applet.width, applet.height);
	}

	PVector calcXY(float[] timings, matrixMath mc)
	{
		matrixMath result = matrixMath.columnMultiplyMatrix(timings, mc);
		float x = Math.max(0, Math.min(TABLE_WIDTH, result.getNumber(0, 0)));
		float y = Math.max(0, Math.min(TABLE_HEIGHT, result.getNumber(0, 1)));
		return new PVector(x, y);
	}
}
