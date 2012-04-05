package snowAngels;

import processing.core.PApplet;
import processing.core.PVector;

public class Particle
{
	private static final float CLOSE_WALL_OFFSET = 0.1f;
	private float desiredSeparation = 60.0f;
	private float separationForceFactor = 2f;

	public PVector location;
	public PVector velocity = new PVector();
	public PVector acceleration = new PVector();
	public float maxForce = 2f;
	public float maxSpeed = 30f;
	private PApplet applet;
	private float wanderMaxSpeed = 0.5f;
	private float wanderMaxForce = 0.01f;
	private float drag = 0.9f;
	private float attractorRange = 300f;
	private final float attractionForceFactor = 30f;

	public Particle(PApplet applet, PVector location)
	{
		this.applet = applet;
		this.location = location;
	}

	PVector steer(PVector target, boolean slowdown, float steerMaxSpeed, float steerMaxForce)
	{
		PVector steer;
		PVector desired = PVector.sub(target, location);
		float d = desired.mag();

		if (d > 0)
		{
			desired.normalize();

			if (slowdown && d < 100)
			{
				desired.mult(steerMaxSpeed * (d / 100));
			} else
			{
				desired.mult(steerMaxSpeed);
			}

			steer = PVector.sub(desired, velocity);
			steer.limit(steerMaxForce);
		} else
		{
			steer = new PVector(0, 0);
		}

		return steer;
	}


	/*  SEEK - FLEE  */
	void seek(PVector target, float seekMaxSpeed, float seekMaxForce)
	{
		acceleration.add(steer(target, false, seekMaxSpeed, seekMaxForce));
//		PApplet.println(
//				"Particle.seek " + this.toString() + " to " + target.x + ", " + target.y + " maxSpeed " + maxSpeed + " maxForce " + maxForce + " acceleration " + acceleration.x + ", " + acceleration.y + " velocity " + velocity.toString() + " theta " + theta);
	}

	void arrive(PVector target)
	{
		acceleration.add(steer(target, true, maxSpeed, maxForce));
	}

	void flee(PVector target)
	{
		acceleration.sub(steer(target, false, maxSpeed, maxForce));
	}


	/*  PURSUE - EVADE  */
	void pursue(PVector target)
	{
		float lookAhead = location.dist(target) / maxSpeed;
		PVector predictedTarget = new PVector(target.x + lookAhead, target.y + lookAhead);
		seek(predictedTarget, maxSpeed, maxForce);
	}

	/*  WANDER  */
	void wander()
	{
		PVector wanderTarget = location.get();
		wanderTarget.x += applet.random(-5, 5);
		wanderTarget.y += applet.random(-5, 5);
		seek(wanderTarget, wanderMaxSpeed, wanderMaxForce);
	}

	void update()
	{
		acceleration.limit(maxForce);
		velocity.add(acceleration);
		velocity.limit(maxSpeed);
		velocity.mult(drag);
		location.add(velocity);
		acceleration.mult(0);
	}

	// Separation
	// Method checks for nearby particles and steers away
	PVector separate(Particle[] particles)
	{
		PVector steer = new PVector(0, 0, 0);
		int count = 0;
		// For every particle in the system, check if it's too close
		for (Particle other : particles)
		{
			if (other != this)
			{
				PVector targetPosition = other.location;
				count = calculateSeparationSteering(steer, count, targetPosition);
			}
		}

		for (int wall = 0; wall < 4; wall++)
		{
			PVector targetPosition = location.get();
			switch (wall)
			{
				case 0:
					targetPosition.x = Math.min(0, targetPosition.x - CLOSE_WALL_OFFSET);
					break;
				case 1:
					targetPosition.x = Math.max(applet.width, targetPosition.x + CLOSE_WALL_OFFSET);
					break;
				case 2:
					targetPosition.y = Math.min(0, targetPosition.y - CLOSE_WALL_OFFSET);
					break;
				case 3:
					targetPosition.y = Math.max(applet.height, targetPosition.y + CLOSE_WALL_OFFSET);
			}
			count = calculateSeparationSteering(steer, count, targetPosition);
		}

		// Average -- divide by how many
		if (count > 0)
		{
//			steer.div((float) count);
		}

		// As long as the vector is greater than 0
		if (steer.mag() > 0)
		{
			// Implement Reynolds: Steering = Desired - Velocity
//			steer.normalize();
//			steer.mult(maxSpeed);
			steer.limit(maxForce * separationForceFactor * count);
			steer.sub(velocity);
		}

		return steer;
	}

	private int calculateSeparationSteering(PVector steer, int count, PVector targetPosition)
	{
		float d = PVector.dist(location, targetPosition);
		// If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
		if ((d > 0) && (d < desiredSeparation))
		{
			// Calculate vector pointing away from neighbor
			PVector diff = PVector.sub(location, targetPosition);
			diff.normalize();
			diff.mult(PApplet.map(d, 0.1f, desiredSeparation, maxForce * separationForceFactor, 0));		// Weight by distance
			steer.add(diff);
			count++;			// Keep track of how many
		}
		return count;
	}

	public void behave(Particle[] particles)
	{
		wander();
		acceleration.add(separate(particles));
	}

	public void applyAttractionForces(Iterable<PVector> attractors)
	{
		PVector steer = new PVector();
		for (PVector attractor : attractors)
		{
			PVector diff = PVector.sub(attractor, location);
			float distance = diff.mag();
			if (distance < attractorRange)
			{
				diff.normalize();
				diff.mult(PApplet.map(distance, 0.1f, attractorRange, maxForce * attractionForceFactor, 0));
				steer.add(diff);
			}
		}

		// As long as the vector is greater than 0
		if (steer.mag() > 0)
		{
			// Implement Reynolds: Steering = Desired - Velocity
			steer.limit(maxForce * attractionForceFactor);
			steer.sub(velocity);
			acceleration.add(steer);
		}
	}
}
