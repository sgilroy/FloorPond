package snowAngels;

import processing.core.*;

public class ParticleSystem
{
	public static final float PARTICLE_RADIUS = 10f;
	private Particle[] particles;
	private int numParticles;
	private static final float PARTICLE_DENSITY = 0.2f / 1000;
	private PApplet applet;
	private PImage particleImage;
	private Iterable<PVector> attractors;
	private boolean showVelocityVectors = false;

	public ParticleSystem(PApplet applet)
	{
		this.applet = applet;
		numParticles = (int) (applet.width * applet.height * PARTICLE_DENSITY);
		this.particles = new Particle[numParticles];
		applet.noStroke();
		applet.fill(0, 0, 255, 128);
		for (int i = 0; i < numParticles; i++)
		{
			particles[i] = new Particle(applet, new PVector(applet.random(applet.width), applet.random(applet.height)));
		}

		particleImage = applet.createImage((int)Math.ceil(PARTICLE_RADIUS * 2), (int)Math.ceil(PARTICLE_RADIUS * 2),
										   PConstants.ALPHA);
		PGraphics tempGraphics = applet.createGraphics(particleImage.width, particleImage.height, PConstants.P2D);
		tempGraphics.beginDraw();
		tempGraphics.colorMode(PConstants.ARGB, 255);
		// TODO: figure out a good way to do the alpha channel and smooth edges of the particles
		tempGraphics.noSmooth();
		tempGraphics.noStroke();
		tempGraphics.fill(255, 255);
		tempGraphics.ellipseMode(PConstants.CENTER);
		tempGraphics.ellipse(PARTICLE_RADIUS, PARTICLE_RADIUS, PARTICLE_RADIUS, PARTICLE_RADIUS);
		tempGraphics.endDraw();
		particleImage = tempGraphics.get();
	}

	public void draw()
	{
		if (showVelocityVectors)
		{
			applet.colorMode(PConstants.RGB);
			applet.strokeWeight(1);
		}
//		applet.tint(255, 64);
		for (Particle particle : particles)
		{
			int alpha = (int) PApplet.map(particle.velocity.mag(), 0.3f, particle.maxSpeed, 0, 255);
			applet.tint(255, alpha);
//			applet.ellipse(particle.location.x, particle.location.y, PARTICLE_RADIUS, PARTICLE_RADIUS);
//			applet.point(particle.location.x, particle.location.y);
			applet.image(particleImage, particle.location.x - PARTICLE_RADIUS, particle.location.y - PARTICLE_RADIUS);

			if (showVelocityVectors)
			{
				if (particle.velocity.mag() > 0.3f)
				{
					applet.stroke(0, 0, 255);
					PVector velocityVector = particle.velocity.get();
					velocityVector.mult(10f);
					PVector velocityTarget = PVector.add(particle.location, velocityVector);
					applet.line(particle.location.x, particle.location.y, velocityTarget.x, velocityTarget.y);
				}
			}
		}

		applet.noTint();
	}

	public void update()
	{
		for (Particle particle : particles)
		{
			particle.behave(particles);
			if (attractors != null)
			{
				particle.applyAttractionForces(attractors);
			}
			particle.update();
		}
	}

	public void setAttractors(Iterable<PVector> attractors)
	{
		this.attractors = attractors;
	}
}
