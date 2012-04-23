import ddf.minim.*;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

public class InteractiveFrequencyEnergy extends PApplet
{

	/**
	 * Frequency Energy
	 * by Damien Di Fede.
	 * <p/>
	 * This sketch demonstrates how to use the BeatDetect object in FREQ_ENERGY mode.
	 * You can use <code>isKick</code>, <code>isSnare</code>, </code>isHat</code>,
	 * <code>isRange</code>, and <code>isOnset(int)</code> to track whatever kind
	 * of beats you are looking to track, they will report true or false based on
	 * the state of the analysis. To "tick" the analysis you must call <code>detect</code>
	 * with successive buffers of audio. You can do this inside of <code>draw</code>,
	 * but you are likely to miss some audio buffers if you do this. The sketch implements
	 * an <code>AudioListener</code> called <code>BeatListener</code> so that it can call
	 * <code>detect</code> on every buffer of audio processed by the system without repeating
	 * a buffer or missing one.
	 * <p/>
	 * This sketch plays an entire song so it may be a little slow to load.
	 */


	Minim minim;
	AudioPlayer song;
	BeatDetect beat;
	BeatListener bl;
	FFT fft;

	float kickSize, snareSize, hatSize;

	//Particles
	particle[] Z = new particle[10000];
	particle[] Z2 = new particle[10000];
	float colour = random(1);
	boolean tracer = false;
	boolean night = true;


	//Variable for movement
	int partX = 0;
	int partY = 62;
	int frameCheck = 0;
	int partMove = 1;
	int partCount = 0;
	float partResp = 0;

	//Images
	PImage imgTop;
	PImage imgBright;
	PImage imgDull;
	PImage imgText;
	float imgTrans = 0;

	PFont font1;
	String playing = "playing:";
	int mp3Number = 1;
	private AudioSource audioSource;
	private boolean useSongFile = false;
	AudioInput in;

	private static boolean fullScreen = false;

	public static final String PROCESSING_RENDERER = P2D;
	private int gutterTop = 84;
	private int gutterBottom = 84;
	private static final String APPLET_CLASS = "InteractiveFrequencyEnergy";

	public void setup()
	{
//		size(136, 210, P2D);
		if (fullScreen)
		{
			size(screen.width, screen.height, PROCESSING_RENDERER);
		} else
		{
			size(512, 768, PROCESSING_RENDERER);
		}

		background(0);
		frameRate(30);
		smooth();

		imgTop = loadImage("imgTop.png");
		imgBright = loadImage("imgBright.png");
		imgDull = loadImage("imgDull.png");
		font1 = loadFont("VisitorTT2BRK-14.vlw");
		imgText = loadImage("imgText.png");
		textFont(font1, 7);

		minim = new Minim(this);

		if (useSongFile)
		{
			song = minim.loadFile("1.mp3", 2048);
			fft = new FFT(song.bufferSize(), song.sampleRate());
			song.play();
			audioSource = song;
		}
		else
		{
			in = minim.getLineIn(Minim.STEREO, 512);
			fft = new FFT(in.bufferSize(), in.sampleRate());
//			fft.linAverages(numFilaments * 2);
			audioSource = in;
		}


		// a beat detection object that is FREQ_ENERGY mode that
		// expects buffers the length of song's buffer size
		// and samples captured at songs's sample rate
		beat = new BeatDetect(audioSource.bufferSize(), audioSource.sampleRate());
		// set the sensitivity to 300 milliseconds
		// After a beat has been detected, the algorithm will wait for 300 milliseconds
		// before allowing another beat to be reported. You can use this to dampen the
		// algorithm if it is giving too many false-positives. The default value is 10,
		// which is essentially no damping. If you try to set the sensitivity to a negative value,
		// an error will be reported and it will be set to 10 instead.
		beat.setSensitivity(300);
		kickSize = snareSize = hatSize = 16;
		// make a new beat listener, so that we won't miss any buffers for the analysis
		bl = new BeatListener(beat, audioSource);
		textFont(createFont("SanSerif", 16));
		textAlign(CENTER);

		//Particles
		float r;
		float phi;

		for (int i = 0; i < Z.length; i++)
		{

			r = sqrt(random(sq(width / 2) + sq(height / 2)));
			phi = random(TWO_PI);
			Z[i] = new particle(r * cos(phi) + width / 2, r * sin(phi) + height / 2, 0, 0, random(2.5f) + 0.5f);
			Z2[i] = new particle(r * cos(phi) + width / 2, r * sin(phi) + height / 2, 0, 0, random(2.5f) + 0.5f);
		}
	}

	public void partMoveing()
	{
		if (partMove == 1)
		{
			partX = partX + 1 + PApplet.parseInt(partResp * .01f);
			if (partX >= 132)
			{
				partMove = 2;
			}
		} else
		{
			partX = partX - 1 - PApplet.parseInt(partResp * .01f);
			if (partX <= 0)
			{
				partMove = 1;
			}
		}
	}

	public void draw()
	{

		if (imgTrans >= 20)
		{
			imgTrans = imgTrans - 20;
		}

		partResp = 0;
		for (int i = 0; i < fft.specSize(); i++)
		{
			// draw the line for frequency band i, scaling it by 4 so we can see it a bit better

			partResp = partResp + fft.getBand(i);
		}


		if (frameCheck == 5)
		{
			frameCheck = 0;
		}
		frameCheck = frameCheck + 1;
		if (partCount >= 1)
		{
			partCount = partCount - 1;
		}
		partMoveing();

		background(0);
		fill(255);
		fft.forward(audioSource.mix);


		if (beat.isKick()) kickSize = 32;
		if (beat.isSnare()) snareSize = 32;
		if (beat.isHat()) hatSize = 32;


		//Particles
		float r;

		if (night)
			filter(INVERT);

		if (tracer)
		{
			stroke(255, 100);
			fill(255, 100);
			rect(0, 0, width, height);
		} else
		{
			background(250);
		}

		//Snare brighten
//		image(imgDull, 0, 0);
//		if (imgTrans > 0)
//		{
//			tint(255, imgTrans);
//			image(imgBright, 0, 0);
//			tint(255, 255);
//		}


		colorMode(HSB, 1);
		for (int i = 0; i < Z.length; i++)
		{
			//if( beat.isKick() || beat.isHat() ) {
			if (beat.isSnare())
			{
				partCount = 15;
				Z[i].repel(new particle(partX, gutterTop + partResp * .01f, 0, 0, 50));
				Z2[i].repel(new particle(width - partX, height - gutterBottom - partResp * .01f, 0, 0, 50));
				imgTrans = 255;
			} else if (i <= Z.length && partCount == 0)
			{
				Z[i].gravitate(new particle(partX, gutterTop + partResp * .01f, 0, 0, 50));
				Z2[i].gravitate(new particle(width - partX, height - gutterBottom - partResp * .01f, 0, 0, 50));
			}
			Z[i].deteriorate();
			Z[i].update();
			Z2[i].deteriorate();
			Z2[i].update();
			r = PApplet.parseFloat(i) / Z.length;
			if (sq(Z[i].magnitude) / 50 < 0.15f)
			{
				stroke(colour, pow(r, 0.1f), 1 - r, sq(Z[i].magnitude) / 50);
			} else
			{
				stroke(colour, pow(r, 0.1f), 1 - r, 0.15f);
			}

			//if( i < Z.length-1 )
			//line( Z[i].x, Z[i].y, Z[i+1].x, Z[i+1].y );
			Z[i].display();
			Z2[i].display();
		}


		colorMode(RGB, 255);


		colour += random(0.01f);
		if (colour > 1)
		{
			colour = colour % 1;
		}

		if (night)
		{
//			image(imgTop, 0, 0);
//			image(imgText, 0, 0);
			filter(INVERT);
		}

		//Draw playing text
		// if(mp3Number == 1){
		//  textFont(font1,14);
		//  playing = "convex assymetry";
		//  text("playing:",30,190);
		//  text(playing,70,200);
		//}
	}

	public void stop()
	{
		// always close Minim audio classes when you are finished with them
		audioSource.close();
		// always stop Minim before exiting
		minim.stop();
		// this closes the sketch
		super.stop();
	}

	public void keyPressed()
	{
		if (keyCode == ENTER)
		{
			tracer = !tracer;
			background(255);
		}
		if (keyCode == SHIFT)
		{
			night = !night;
		}
	}

	class BeatListener implements AudioListener
	{
		private BeatDetect beat;
		private AudioSource source;

		BeatListener(BeatDetect beat, AudioSource source)
		{
			this.source = source;
			this.source.addListener(this);
			this.beat = beat;
		}

		public void samples(float[] samps)
		{
			beat.detect(source.mix);
		}

		public void samples(float[] sampsL, float[] sampsR)
		{
			beat.detect(source.mix);
		}
	}

	class particle
	{

		float x;
		float y;
		float px;
		float py;
		float magnitude;
		float angle;
		float mass;

		particle(float dx, float dy, float V, float A, float M)
		{
			x = dx;
			y = dy;
			px = dx;
			py = dy;
			magnitude = V;
			angle = A;
			mass = M;
		}

		public void reset(float dx, float dy, float V, float A, float M)
		{
			x = dx;
			y = dy;
			px = dx;
			py = dy;
			magnitude = V;
			angle = A;
			mass = M;
		}

		public void gravitate(particle Z)
		{
			float F, mX, mY, A;
			if (sq(x - Z.x) + sq(y - Z.y) != 0)
			{
				F = mass * Z.mass;
				F /= sqrt(sq(x - Z.x) + sq(y - Z.y));
				//F /= ( sq( x - Z.x ) + sq( y - Z.y ) );
				if (sqrt(sq(x - Z.x) + sq(y - Z.y)) < 10)
				{
					F = 0.1f;
				}
				mX = (mass * x + Z.mass * Z.x) / (mass + Z.mass);
				mY = (mass * y + Z.mass * Z.y) / (mass + Z.mass);
				A = atan2(mY - y, mX - x);

				mX = F * cos(A);
				mY = F * sin(A);

				mX += magnitude * cos(angle);
				mY += magnitude * sin(angle);

				magnitude = sqrt(sq(mX) + sq(mY));
				angle = findAngle(mX, mY);
			}
		}

		public void repel(particle Z)
		{
			float F, mX, mY, A;
			if (sq(x - Z.x) + sq(y - Z.y) != 0)
			{
				F = mass * Z.mass;
				F /= sqrt(sq(x - Z.x) + sq(y - Z.y));
				if (sqrt(sq(x - Z.x) + sq(y - Z.y)) < 10)
				{
					F = 0.1f;
				}
				mX = (mass * x + Z.mass * Z.x) / (mass + Z.mass);
				mY = (mass * y + Z.mass * Z.y) / (mass + Z.mass);
				A = atan2(y - mY, x - mX);

				mX = F * cos(A);
				mY = F * sin(A);

				mX += magnitude * cos(angle);
				mY += magnitude * sin(angle);

				magnitude = sqrt(sq(mX) + sq(mY));
				angle = findAngle(mX, mY);
			}
		}

		public void deteriorate()
		{
			magnitude *= 0.9f;
		}

		public void update()
		{

			x += magnitude * cos(angle);
			y += magnitude * sin(angle);

		}

		public void display()
		{
			line(px, py, x, y);
			px = x;
			py = y;
		}


	}

	public float findAngle(float x, float y)
	{
		float theta;
		if (x == 0)
		{
			if (y > 0)
			{
				theta = HALF_PI;
			} else if (y < 0)
			{
				theta = 3 * HALF_PI;
			} else
			{
				theta = 0;
			}
		} else
		{
			theta = atan(y / x);
			if ((x < 0) && (y >= 0))
			{
				theta += PI;
			}
			if ((x < 0) && (y < 0))
			{
				theta -= PI;
			}
		}
		return theta;
	}


	static public void main(String args[])
	{
//		PApplet.main(new String[]{"--bgcolor=#ECE9D8", "InteractiveFrequencyEnergy"});
		if (args.length > 0 && args[0].equals("--present"))
		{
			fullScreen = true;
			PApplet.main(new String[]{"--present", APPLET_CLASS});
		} else
		{
			PApplet.main(new String[]{APPLET_CLASS});
		}
	}
}
