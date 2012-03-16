package floorPond;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * Steer behavior class, to control/simulate natural movement
 * the idea is to make some behaviors interactive like
 */
public class Boid extends Flagellum
{

	public static final float STRICT_PURSUIT_MAX_SPEED = 8f;
	public static final float STRICT_PURSUIT_MAX_FORCE = 1f;
	PVector location;
	PVector velocity;
	PVector acceleration;
	float maxForce;
	float maxSpeed;
	float wanderTheta;
	float rushSpeed;

	boolean isRushingToEvade = false;				 // check if time interval is complete
	int timeCount = 0;						 // time cicle index
	int lastTimeCheck = 0;					 // stores last time check
	int timeCountLimit = 10;				   // max time cicles

	private boolean strictPursuit = false;
	WrappedView wrappedView;
	private PApplet applet;
	private ArrayList<PVector> previousVelocities = new ArrayList<PVector>();

	float neighborDistance = 500.0f;
	float desiredSeparation = 80.0f;
	private PVector averageVelocity;
	private float desiredMaxSpeed;
	private float defaultMaxSpeed;
	private float defaultMaxForce;
	private final boolean showBoidTrajectories = false;

	private Gesture gesture;

	Boid(PApplet applet, String _skin, PVector _location, float _maxSpeed, float _maxForce, WrappedView _wrappedView)
	{
		super(applet, _skin);
		this.applet = applet;

		location = _location.get();
		velocity = new PVector(applet.random(-1, 1), applet.random(-1, 1));
		velocity.normalize();
		velocity.mult(_maxSpeed);
		acceleration = new PVector(0, 0);
		defaultMaxForce = maxForce = _maxForce;
		defaultMaxSpeed = desiredMaxSpeed = maxSpeed = _maxSpeed;
		wrappedView = _wrappedView;
		rushSpeed = applet.random(6, 10);
	}


	PVector steer(PVector target, boolean slowdown)
	{
		PVector steer;
		PVector desired = PVector.sub(target, location);
		float d = desired.mag();

		if (d > 0)
		{
			desired.normalize();

			if (slowdown && d < 100)
			{
				desired.mult(maxSpeed * (d / 100));
			} else
			{
				desired.mult(maxSpeed);
			}

			steer = PVector.sub(desired, velocity);
			steer.limit(maxForce);
		} else
		{
			steer = new PVector(0, 0);
		}

		return steer;
	}


	/*  SEEK - FLEE  */
	void seek(PVector target)
	{
		acceleration.add(steer(target, false));
//		PApplet.println(
//				"Boid.seek " + this.toString() + " to " + target.x + ", " + target.y + " maxSpeed " + maxSpeed + " maxForce " + maxForce + " acceleration " + acceleration.x + ", " + acceleration.y + " velocity " + velocity.toString() + " theta " + theta);
	}

	void arrive(PVector target)
	{
		acceleration.add(steer(target, true));
	}

	void flee(PVector target)
	{
		acceleration.sub(steer(target, false));
	}


	/*  PURSUE - EVADE  */
	void pursue(PVector target)
	{
		float lookAhead = location.dist(target) / maxSpeed;
		PVector predictedTarget = new PVector(target.x + lookAhead, target.y + lookAhead);
		seek(predictedTarget);
	}

	void evade(PVector target)
	{
		isRushingToEvade = true;
		if (PApplet.dist(target.x, target.y, location.x, location.y) < 100)
		{
			float lookAhead = location.dist(target) / (maxSpeed * 2);
			PVector predictedTarget = new PVector(target.x - lookAhead, target.y - lookAhead);
			flee(predictedTarget);
		}
	}


	/*  WANDER  */
	void wander()
	{
		float wanderR = 5;
		float wanderD = 100;
		float change = 0.05f;

		wanderTheta += applet.random(-change, change);

		PVector circleLocation = velocity.get();
		circleLocation.normalize();
		circleLocation.mult(wanderD);
		circleLocation.add(location);

		PVector circleOffset = new PVector(wanderR * PApplet.cos(wanderTheta), wanderR * PApplet.sin(wanderTheta));
		PVector target = PVector.add(circleLocation, circleOffset);
		seek(target);
	}


	void run()
	{
		update();
		borders();
		display();
	}


	void update()
	{
		velocity.add(acceleration);
		velocity.limit(maxSpeed);
		averageVelocity = new PVector();
		previousVelocities.add(velocity.get());
		for (PVector previousVelocity : previousVelocities)
		{
			averageVelocity.add(previousVelocity);
		}
		averageVelocity.div(previousVelocities.size());
		while (previousVelocities.size() >= 4)
			previousVelocities.remove(0);

		location.add(velocity);
		updateRotation();
		acceleration.mult(0);

		// sets flagellum muscleFreq in relation to velocity
		//super.muscleRange = norm(velocity.mag(), 0, 1) * 2.5;
		super.muscleFreq = PApplet.norm(velocity.mag(), 0, 1) * 0.06f;
		super.move();

		if (isRushingToEvade)
		{
			if (applet.millis() > lastTimeCheck + 200)
			{
				lastTimeCheck = applet.millis();

				if (timeCount <= timeCountLimit)
				{
					// derease maxSpeed in relation with time cicles
					// this formula needs a proper look
					maxSpeed = rushSpeed - (PApplet.norm(timeCount, 0, timeCountLimit) * 3);
					timeCount++;
				} else if (timeCount >= timeCountLimit)
				{
					// once the time cicle is complete
					// resets timer variables,
					timeCount = 0;
					isRushingToEvade = false;

					// set default speed values
					maxSpeed = defaultMaxSpeed;
					maxForce = defaultMaxForce;
				}
			}
		}
	}

	private void updateRotation()
	{
		// update location and direction
		float thetaRadians = averageVelocity.heading2D() + PApplet.radians(180);

		// update flagellum body rotation
		float goalThetaDegrees = PApplet.degrees(thetaRadians);
		goalThetaDegrees += 180;

//		float thetaDegreesDelta = goalThetaDegrees - super.theta;
//		float maxThetaDegreesDelta = 10f * acceleration.mag();
//		super.theta += PApplet.min(PApplet.max(thetaDegreesDelta, -maxThetaDegreesDelta), maxThetaDegreesDelta);
		super.theta = goalThetaDegrees;
	}

	// control skin tint, for now it picks a random dark grey color
	int opacity = 0;

	int maxOpacity = 0;

	void display()
	{
		if (opacity < 255) opacity += 1;
		else opacity = 255;
		applet.tint(maxOpacity, maxOpacity, maxOpacity, opacity);

		applet.pushMatrix();
		applet.translate(location.x, location.y);
		//rotate(theta);
		super.display();
		applet.popMatrix();
		applet.noTint();

		if (showBoidTrajectories)
		{
			PVector exaggeratedVelocity = velocity.get();
			exaggeratedVelocity.mult(20f);
			PVector trajectory = PVector.add(location, exaggeratedVelocity);

			applet.stroke(0, 0, 255, 200);
			applet.noFill();
			applet.strokeWeight(3);
			applet.line(location.x, location.y, trajectory.x, trajectory.y);
			applet.noStroke();
		}
	}

	// wrapper, appear opposite side
	void borders()
	{
//    if (location.x < -skin.width) location.x = applet.width;
//    if (location.x > applet.width + skin.width) location.x = 0;
//    if (location.y < -skin.width) location.y = applet.height;
//    if (location.y > applet.height + skin.width) location.y = 0;
		location.x = wrappedView.getViewX(location.x);
		location.y = wrappedView.getViewY(location.y);
	}

	public void startStrictPursuit()
	{
		strictPursuit = true;
		maxSpeed = desiredMaxSpeed = STRICT_PURSUIT_MAX_SPEED;
		maxForce = STRICT_PURSUIT_MAX_FORCE;
	}

	void stopStrictPursuit()
	{
		if (strictPursuit)
		{
			strictPursuit = false;

			// set default speed values
			maxSpeed = desiredMaxSpeed = defaultMaxSpeed;
			maxForce = defaultMaxForce;
		}
	}

	// We accumulate a new acceleration each time based on three rules
	void flock(ArrayList boids)
	{
//		maxSpeed = mimicSpeed(boids);
		PVector sep = separate(boids);   // Separation
		PVector ali = align(boids);	  // Alignment
		PVector coh = cohesion(boids);   // Cohesion
		// Arbitrarily weight these forces
		sep.mult(1.5f);
		ali.mult(1.0f);
		coh.mult(1.0f);
		// Add the force vectors to acceleration
		acceleration.add(sep);
		acceleration.add(ali);
		acceleration.add(coh);
	}

	// Separation
	// Method checks for nearby boids and steers away
	PVector separate(ArrayList boids)
	{
		PVector steer = new PVector(0, 0, 0);
		int count = 0;
		// For every boid in the system, check if it's too close
		for (int i = 0; i < boids.size(); i++)
		{
			Boid other = (Boid) boids.get(i);
			if (other != this)
			{
				PVector targetPosition = getOtherLocationWithWrapping(other);
				float d = PVector.dist(location, targetPosition);
				// If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
				if ((d > 0) && (d < desiredSeparation))
				{
					// Calculate vector pointing away from neighbor
					PVector diff = PVector.sub(location, targetPosition);
					diff.normalize();
					diff.div(d);		// Weight by distance
					steer.add(diff);
					count++;			// Keep track of how many
				}
			}
		}
		// Average -- divide by how many
		if (count > 0)
		{
			steer.div((float) count);
		}

		// As long as the vector is greater than 0
		if (steer.mag() > 0)
		{
			// Implement Reynolds: Steering = Desired - Velocity
			steer.normalize();
			steer.mult(maxSpeed);
			steer.sub(velocity);
			steer.limit(maxForce);
		}
		return steer;
	}

	// Alignment
	// For every nearby boid in the system, calculate the average velocity
	PVector align(ArrayList boids)
	{
		PVector steer = new PVector(0, 0, 0);
		int count = 0;
		for (int i = 0; i < boids.size(); i++)
		{
			Boid other = (Boid) boids.get(i);
			if (other != this)
			{
				PVector targetPosition = getOtherLocationWithWrapping(other);
				float d = PVector.dist(location, targetPosition);
				if ((d > 0) && (d < neighborDistance))
				{
					steer.add(other.velocity);
					count++;
				}
			}
		}
		if (count > 0)
		{
			steer.div((float) count);
		}

		// As long as the vector is greater than 0
		if (steer.mag() > 0)
		{
			// Implement Reynolds: Steering = Desired - Velocity
			steer.normalize();
			steer.mult(maxSpeed);
			steer.sub(velocity);
			steer.limit(maxForce);
		}
		return steer;
	}

	// Cohesion
	// For the average location (i.e. center) of all nearby boids, calculate steering vector towards that location
	PVector cohesion(ArrayList boids)
	{
		PVector sum = new PVector(0, 0);   // Start with empty vector to accumulate all locations
		int count = 0;
		for (int i = 0; i < boids.size(); i++)
		{
			Boid other = (Boid) boids.get(i);
			if (other != this)
			{
				PVector targetPosition = getOtherLocationWithWrapping(other);
				float d = location.dist(targetPosition);
				if ((d > 0) && (d < neighborDistance))
				{
					sum.add(other.location); // Add location
					count++;
				}
			}
		}
		if (count > 0)
		{
			sum.div((float) count);
			return steer(sum, false);  // Steer towards the location
		}
		return sum;
	}

	// TODO: figure out why this is causing boids to get "confused" and oscillate directions, stuck in one location
	float mimicSpeed(ArrayList boids)
	{
//		float sum = 0;   // Start with empty vector to accumulate all velocities
		Boid leader = null;
		int leaderIndex = -1;
		float leaderDist = 0;
		for (int i = 0; i < boids.size(); i++)
		{
			Boid other = (Boid) boids.get(i);
			if (other != this)
			{
				PVector targetPosition = getOtherLocationWithWrapping(other);
				float d = location.dist(targetPosition);
				if ((d > 0) && (d < neighborDistance) && other.strictPursuit && leader == null || d < leaderDist)
				{
					leaderDist = d;
					leader = other;
				}
			}
		}

		// is this boid moving towards (following) the leader's trajectory?
		//if (PVector.angleBetween() < )
		//float projectedSeparation

		if (leader != null)
			return leader.velocity.mag() * 1.1f;
		else
			return desiredMaxSpeed;
	}

	private PVector getOtherLocationWithWrapping(Boid other)
	{
//		return other.location;
		return wrappedView.targetForWrapping(other.location, location);
	}

	public Gesture getGesture()
	{
		return gesture;
	}

	public void setGesture(Gesture gesture)
	{
		this.gesture = gesture;
	}
}