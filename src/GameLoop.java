//based on http://www.java-gaming.org/index.php?topic=24220.0

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;

public class GameLoop extends JFrame implements ActionListener {
	private GamePanel gamePanel = new GamePanel();
	private JButton startButton = new JButton("Start");
	private JButton quitButton = new JButton("Quit");

	private boolean running = false;
	private boolean paused = false;
	private int fps = 60;
	private int frameCount = 0;

	public GameLoop() {
		super("Fixed Timestep Game Loop Test");
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(1, 2));
		p.add(startButton);

		p.add(quitButton);
		cp.add(gamePanel, BorderLayout.CENTER);
		cp.add(p, BorderLayout.SOUTH);
		setSize(500, 500);

		startButton.addActionListener(this);
		quitButton.addActionListener(this);


		gamePanel.initTriangle();

	}

	public static void main(String[] args) {
		GameLoop glt = new GameLoop();
		glt.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		Object s = e.getSource();
		if (s == startButton) {
			running = !running;
			if (running) {
				startButton.setText("Stop");
				runGameLoop();
			} else {
				startButton.setText("Start");
			}
		} else if (s == quitButton) {
			System.exit(0);
		}
	}

	// Starts a new thread and runs the game loop in it.
	public void runGameLoop() {
		Thread loop = new Thread() {
			public void run() {
				gameLoop();
			}
		};
		loop.start();
	}

	// Only run this in another Thread!
	private void gameLoop() {

		final double GAME_HERTZ = 30.0;

		final double TIME_BETWEEN_UPDATES = 1000000000 / GAME_HERTZ;

		final int MAX_UPDATES_BEFORE_RENDER = 5;

		double lastUpdateTime = System.nanoTime();

		double lastRenderTime = System.nanoTime();

		final double TARGET_FPS = 60;
		final double TARGET_TIME_BETWEEN_RENDERS = 1000000000 / TARGET_FPS;

		int lastSecondTime = (int) (lastUpdateTime / 1000000000);

		while (running) {
			double now = System.nanoTime();
			int updateCount = 0;

			if (!paused) {
				while (now - lastUpdateTime > TIME_BETWEEN_UPDATES
						&& updateCount < MAX_UPDATES_BEFORE_RENDER) {
					updateGame();
					lastUpdateTime += TIME_BETWEEN_UPDATES;
					updateCount++;
				}

				if (now - lastUpdateTime > TIME_BETWEEN_UPDATES) {
					lastUpdateTime = now - TIME_BETWEEN_UPDATES;
				}

				float interpolation = Math
						.min(1.0f,
								(float) ((now - lastUpdateTime) / TIME_BETWEEN_UPDATES));
				drawGame(interpolation);
				lastRenderTime = now;

				int thisSecond = (int) (lastUpdateTime / 1000000000);
				if (thisSecond > lastSecondTime) {
					System.out.println("NEW SECOND " + thisSecond + " "
							+ frameCount);
					fps = frameCount;
					frameCount = 0;
					lastSecondTime = thisSecond;
				}

				while (now - lastRenderTime < TARGET_TIME_BETWEEN_RENDERS
						&& now - lastUpdateTime < TIME_BETWEEN_UPDATES) {
					Thread.yield();

					try {
						Thread.sleep(1);
					} catch (Exception e) {
					}

					now = System.nanoTime();
				}
			}
		}
	}

	private void updateGame() {
		gamePanel.requestFocus();
		gamePanel.update();
	}

	private void drawGame(float interpolation) {
		gamePanel.setInterpolation(interpolation);
		gamePanel.repaint();
	}

	private class GamePanel extends JPanel {
		float interpolation;

		int lastDrawX, lastDrawY;

		Triangle triangle;
		ArrayList<Bullet> bulletList = new ArrayList<Bullet>();

		public GamePanel() {
			this.setFocusable(true);
			this.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					handleKey(e);
				}
			});

		}

		private void handleKey(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				gamePanel.triangle.speed += 0.05;
				break;
			case KeyEvent.VK_DOWN:
				gamePanel.triangle.speed -= 0.05;
				break;
			case KeyEvent.VK_LEFT:
				gamePanel.triangle.rot -= 5;
				break;
			case KeyEvent.VK_RIGHT:
				gamePanel.triangle.rot += 5;
				break;
			case KeyEvent.VK_SPACE:
				gamePanel.bulletList.add(new Bullet(triangle.x, triangle.y,
						triangle.rot));
			default:
				return;
			}

		}

		public void initTriangle() {
			triangle = new Triangle();
		}

		public void setInterpolation(float interp) {
			interpolation = interp;
		}

		public void update() {
			triangle.update();
			Iterator<Bullet> it = bulletList.iterator();
			while (it.hasNext()) {
				Bullet tempBullet = it.next();
				tempBullet.update();
				if(isColliding(triangle,tempBullet)){
					it.remove();
					triangle.isDead = true;
				}
			}
		}

		private boolean isColliding(Triangle player, Bullet collidingBullet) {

			float pX1 = player.x0 + player.x;
			float pY1 = player.y0 + player.y;
			float pX2 = player.x1+ player.x;
			float pY2 = player.y1+ player.y;
			float pX3 = player.x2+ player.x;
			float pY3 = player.y2+ player.y;
			float bX = collidingBullet.x;
			float bY = collidingBullet.y;

			float P12 = Math.abs((bX * pY1) + (pX1 * pY2) + (pX2 * bY) - (bX * pY2) - (pX2 * pY1) - (pX1 * bY)) / 2;
			float P23 = Math.abs((bX * pY2) + (pX2 * pY3) + (pX3 * bY) - (bX * pY3) - (pX3 * pY2) - (pX2 * bY)) / 2;
			float P13 = Math.abs((bX * pY1) + (pX1 * pY3) + (pX3 * bY) - (bX * pY3) - (pX3 * pY1) - (pX1 * bY)) / 2;
			float pArea = Math.abs((pX1 * pY2) + (pX2 * pY3) + (pX3 * pY1) - (pX1 * pY3) - (pX3 * pY2) - (pX2 * pY1)) / 2;
			
			if (Math.abs((P12+P23+P13) - pArea) < 0.0005)
				return true;

			return false;
		}

		public void paintComponent(Graphics g) {
			// erase last triangle
			g.setColor(getBackground());
			g.fillRect((int) triangle.lastX, (int) triangle.lastY,
					triangle.width + 2, triangle.height + 2);
			g.fillRect(0, 0, 500, 500);

			g.setColor(Color.RED);
			int drawX = (int) ((triangle.x - triangle.lastX) * interpolation + triangle.lastX);
			int drawY = (int) ((triangle.y - triangle.lastY) * interpolation + triangle.lastY);
			int[] xPoints = { (int) (drawX + triangle.x0),
					(int) (drawX + triangle.x1), (int) (drawX + triangle.x2) };
			int[] yPoints = { (int) (drawY + triangle.y0),
					(int) (drawY + triangle.y1), (int) (drawY + triangle.y2) };
			g.drawPolygon(xPoints, yPoints, 3);

			lastDrawX = drawX;
			lastDrawY = drawY;

			g.setColor(Color.BLUE);
			Iterator<Bullet> it = bulletList.iterator();
			while (it.hasNext()) {
				Bullet temp = it.next();
				g.drawOval((int) temp.x, (int) temp.y, 2, 2);
			}

			g.setColor(Color.BLACK);
			if(triangle.deathCount < 20){
				g.drawString("Death Count: " + triangle.deathCount, 5, 10);
			} else {
				g.drawString("GAME OVER" , 250, 250);
			}
			frameCount++;
		}
	}

	private class Bullet { // refactor
		float x, y;
		float x1,y1;
		float rot;
		float speed;
		boolean isDead = false;

		public Bullet(float initX, float initY, float initRot) {
			
			rot = initRot;
			x1 = rotateX(0,15,rot);
			y1 = rotateY(0,15,rot);
			x = initX + x1;
			y = initY + y1;
			speed = (float) 0.2;
		}

		public void update() {
			
			x += speed * (x1);
			y += speed * (y1);

			if (x > gamePanel.getWidth()) {

				x = 0;

			} else if (x < 0) {
				x = gamePanel.getWidth();
			}

			if (y > gamePanel.getHeight()) {

				y = 0;

			} else if (y < 0) {
				y = gamePanel.getHeight();
			}
		}

		private int rotateX(float px, float py, float angle) {
			int result = 0;

			result = (int) (px * Math.cos(Math.toRadians(angle)) - py
					* Math.sin(Math.toRadians(angle)));

			return result;
		}

		private int rotateY(float px, float py, float angle) {
			int result = 0;

			result = (int) (py * Math.cos(Math.toRadians(angle)) + px
					* Math.sin(Math.toRadians(angle)));

			return result;
		}

		public void draw(Graphics g) {
			// refactor
		}
	}

	private class Triangle // refactor
	{
		float x, y, lastX, lastY;
		int width, height;
		float xVelocity, yVelocity;
		float speed;
		float rot;// in degrees
		float x0, x1, x2;
		float y0, y1, y2;
		boolean isDead = false;
		int deathCount = 0;

		public Triangle() {
			width = 10;
			height = 20;
			x = (float) (Math.random() * (400) + 50);
			y = (float) (Math.random() * (400) + 50);
			lastX = x;
			lastY = y;
			xVelocity = 0;
			yVelocity = 0;
			rot = (float) 0;
			speed = 0;
			x0 = 0;
			x1 = 0;
			x2 = 0;
			y0 = 0;
			y1 = 0;
			y2 = 0;

		}

		private int rotateX(float px, float py, float angle) {
			int result = 0;

			result = (int) (px * Math.cos(Math.toRadians(angle)) - py
					* Math.sin(Math.toRadians(angle)));

			return result;
		}

		private int rotateY(float px, float py, float angle) {
			int result = 0;

			result = (int) (py * Math.cos(Math.toRadians(angle)) + px
					* Math.sin(Math.toRadians(angle)));

			return result;
		}

		public void update() {
			if(isDead == false){
			lastX = x;
			lastY = y;

			x0 = rotateX(0, height / 2, rot);
			x1 = rotateX(width / 2, -height / 2, rot);
			x2 = rotateX(-width / 2, -height / 2, rot);
			y0 = rotateY(0, height / 2, rot);
			y1 = rotateY(width / 2, -height / 2, rot);
			y2 = rotateY(-width / 2, -height / 2, rot);

			xVelocity += speed * rotateX(0, height / 2, rot);
			yVelocity += speed * rotateY(0, height / 2, rot);

			speed = 0;

			x += xVelocity;
			y += yVelocity;

			if (x > gamePanel.getWidth()) {

				x = 0;

			} else if (x < 0) {
				x = gamePanel.getWidth();
			}

			if (y > gamePanel.getHeight()) {

				y = 0;

			} else if (y < 0) {
				y = gamePanel.getHeight();
			}
			} else {
				deathCount++;
				isDead = false;
				x = 250;
				y = 250;
				lastX = x;
				lastY = y;
				xVelocity = 0;
				yVelocity = 0;
			}
		}

		public void draw(Graphics g) {
			// refactor
		}
	}
}