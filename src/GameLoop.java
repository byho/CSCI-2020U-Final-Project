//based on http://www.java-gaming.org/index.php?topic=24220.0

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameLoop extends JFrame implements ActionListener {
	protected GamePanel gamePanel = new GamePanel();
	private JButton startButton = new JButton("Start");
	private JButton quitButton = new JButton("Quit");

	private boolean running = false;
	private boolean paused = false;
	private int fps = 60;
	private int frameCount = 0;

	private boolean fireBullet = false;
	private int bulletOwnerID = 0;

	private boolean isServer = true;
	private boolean gameStarted = false;

	private float remoteX = 250;
	private float remoteY = 250;
	private float remoteRot = 0;

	private static GameLoop instance = null;
	
	public synchronized boolean getGameStarted(){
		return gameStarted;
	}
	public synchronized void setGameStarted(boolean startBool){
		gameStarted = startBool;
	}
	public synchronized boolean getRunning(){
		return running;
	}

	public synchronized float getRemoteX() {
		return remoteX;
	}

	public synchronized void setRemoteX(float remoteX) {
		this.remoteX = remoteX;
	}

	public synchronized float getRemoteY() {
		return remoteY;
	}

	public synchronized void setRemoteY(float remoteY) {
		this.remoteY = remoteY;
	}

	public synchronized float getRemoteRot() {
		return remoteRot;
	}

	public synchronized void setRemoteRot(float remoteRot) {
		this.remoteRot = remoteRot;
	}

	public synchronized boolean isFireBullet() {
		return fireBullet;
	}

	public synchronized void setFireBullet(boolean fireBullet) {
		this.fireBullet = fireBullet;
	}

	public synchronized int getBulletOwner() {
		return bulletOwnerID;
	}

	public synchronized void setBulletOwner(int bulletOwner) {
		this.bulletOwnerID = bulletOwnerID;
	}

	public synchronized boolean isServer() {
		return isServer;
	}

	public synchronized void setServer(boolean isServer) {
		this.isServer = isServer;
	}
	


	protected GameLoop() {
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

	public synchronized static GameLoop getInstance() {
		if (instance == null) {
			instance = new GameLoop();
		}
		return instance;
	}

	public void actionPerformed(ActionEvent e) {
		Object s = e.getSource();
		if (isServer) {
			if (s == startButton) {
				running = !running;
				gameStarted = true;
			}
		}
		if (running) {
			startButton.setText("Stop");
			runGameLoop();
		} else {
			startButton.setText("Start");
		}
		if (s == quitButton) {
			System.exit(0);
		}
	}

	
	public void startGame(){
		running = true;
		
		runGameLoop();
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
					if (updateGame()) {
						running = false;
						startButton.setText("Restart");
					}
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

	private boolean updateGame() {
		gamePanel.requestFocus();
		return gamePanel.update();
	}

	private void drawGame(float interpolation) {
		gamePanel.setInterpolation(interpolation);
		gamePanel.repaint();
	}

	protected class GamePanel extends JPanel {
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
				fireBullet = true;
				bulletOwnerID = triangle.id;
				break;
			default:
				return;
			}

		}

		public void initTriangle() {
			triangle = new Triangle(0);
		}

		public void setInterpolation(float interp) {
			interpolation = interp;
		}

		public boolean update() {
			triangle.update();
			if (fireBullet) {
				if (bulletOwnerID == triangle.id) {
					bulletList.add(new Bullet(triangle.x, triangle.y,
							triangle.rot, triangle.id));
				} else {
					bulletList.add(new Bullet(remoteX, remoteY, remoteRot,
							bulletOwnerID));
				}
			}
			Iterator<Bullet> it = bulletList.iterator();
			while (it.hasNext()) {
				Bullet tempBullet = it.next();
				tempBullet.update();
				if (isColliding(triangle, tempBullet)) {
					it.remove();
					triangle.isDead = true;
				}
			}
			if (triangle.deathCount > 15) {
				ScoreWriter recordScore = new ScoreWriter(triangle.name,
						triangle.winCount);
				return true;
			}
			return false;
		}

		private boolean isColliding(Triangle player, Bullet collidingBullet) {

			float pX1 = player.x0 + player.x;
			float pY1 = player.y0 + player.y;
			float pX2 = player.x1 + player.x;
			float pY2 = player.y1 + player.y;
			float pX3 = player.x2 + player.x;
			float pY3 = player.y2 + player.y;
			float bX = collidingBullet.x;
			float bY = collidingBullet.y;

			float P12 = Math.abs((bX * pY1) + (pX1 * pY2) + (pX2 * bY)
					- (bX * pY2) - (pX2 * pY1) - (pX1 * bY)) / 2;
			float P23 = Math.abs((bX * pY2) + (pX2 * pY3) + (pX3 * bY)
					- (bX * pY3) - (pX3 * pY2) - (pX2 * bY)) / 2;
			float P13 = Math.abs((bX * pY1) + (pX1 * pY3) + (pX3 * bY)
					- (bX * pY3) - (pX3 * pY1) - (pX1 * bY)) / 2;
			float pArea = Math.abs((pX1 * pY2) + (pX2 * pY3) + (pX3 * pY1)
					- (pX1 * pY3) - (pX3 * pY2) - (pX2 * pY1)) / 2;

			if (Math.abs((P12 + P23 + P13) - pArea) < 0.0005
					&& player.id != collidingBullet.ownerID)
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
							  (int) (drawX + triangle.x1), 
							  (int) (drawX + triangle.x2) };
			int[] yPoints = { (int) (drawY + triangle.y0),
							  (int) (drawY + triangle.y1), 
							  (int) (drawY + triangle.y2) };
			g.drawPolygon(xPoints, yPoints, 3);

			g.setColor(Color.DARK_GRAY);

			int[] xRPoints = {
					(int) (remoteX + triangle.rotateX(0, 10, remoteRot)),
					(int) (remoteX + triangle.rotateX(5, -10, remoteRot)),
					(int) (remoteX + triangle.rotateX(-5, -10, remoteRot)) };
			int[] yRPoints = {
					(int) (remoteY + triangle.rotateY(0, 10, remoteRot)),
					(int) (remoteY + triangle.rotateY(5, -10, remoteRot)),
					(int) (remoteY + triangle.rotateY(-5, -10, remoteRot)) };
			g.drawPolygon(xRPoints, yRPoints, 3);

			lastDrawX = drawX;
			lastDrawY = drawY;

			g.setColor(Color.BLUE);
			Iterator<Bullet> it = bulletList.iterator();
			while (it.hasNext()) {
				Bullet temp = it.next();
				g.drawOval((int) temp.x, (int) temp.y, 2, 2);
			}

			g.setColor(Color.BLACK);
			if (triangle.deathCount < 15) {
				g.drawString("Death Count: " + triangle.deathCount, 5, 10);
			} else {
				g.drawString("GAME OVER", 250, 250);
			}
			frameCount++;
		}
	}

	private class Bullet { // refactor
		float x, y;
		float x1, y1;
		float rot;
		float speed;
		boolean isDead = false;
		int ownerID;

		public Bullet(float initX, float initY, float initRot, int newOwnerID) {
			ownerID = newOwnerID;
			rot = initRot;
			x1 = rotateX(0, 15, rot);
			y1 = rotateY(0, 15, rot);
			x = initX + x1;
			y = initY + y1;
			speed = (float) 0.2;
			fireBullet = false;
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

		public int rotateX(float px, float py, float angle) {
			int result = 0;

			result = (int) (px * Math.cos(Math.toRadians(angle)) - py
					* Math.sin(Math.toRadians(angle)));

			return result;
		}

		public int rotateY(float px, float py, float angle) {
			int result = 0;

			result = (int) (py * Math.cos(Math.toRadians(angle)) + px
					* Math.sin(Math.toRadians(angle)));

			return result;
		}

		public void draw(Graphics g) {
			// refactor
		}
	}

	protected class Triangle // refactor
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
		String name = "sleepy";
		int winCount = 0;
		int id = 0;

		public Triangle(int newID) {
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
			id = newID;
			ScoreReader scoreReader = new ScoreReader(this);
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
			if (isServer == false)
				id = 1;
			if (isDead == false) {
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

	private class ScoreWriter {
		public ScoreWriter(String player, int winCount) {
			// The name of the file to open.
			String fileName = "Score.txt";

			try {

				File file = new File(fileName);

				if (file.createNewFile()) {
					System.out.println("File is created!");
				} else {
					System.out.println("File already exists.");
				}
				// Assume default encoding.
				FileWriter fileWriter = new FileWriter(fileName);

				// Always wrap FileWriter in BufferedWriter.
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

				// Note that write() does not automatically
				// append a newline character.
				bufferedWriter.write("Name : ");
				bufferedWriter.write(player);
				bufferedWriter.newLine();
				bufferedWriter.write("Wins : ");
				bufferedWriter.write(Integer.toString(winCount));

				// Always close files.
				bufferedWriter.close();
			} catch (IOException ex) {
				System.out.println("Error writing to file '" + fileName + "'");
				// Or we could just do this:
				// ex.printStackTrace();
			}
		}
	}

	public class ScoreReader {

		public ScoreReader(Triangle player) {
			// The name of the file to open.
			String fileName = "Score.txt";

			// This will reference one line at a time
			String line = null;

			try {
				// FileReader reads text files in the default encoding.
				FileReader fileReader = new FileReader(fileName);

				// Always wrap FileReader in BufferedReader.
				BufferedReader bufferedReader = new BufferedReader(fileReader);

				Pattern nameP = Pattern.compile("([A-Z- a-z]+)");
				Pattern scoreP = Pattern.compile("([0-9]+)");

				while ((line = bufferedReader.readLine()) != null) {

					line.substring(6);
					Matcher m0 = nameP.matcher(line);
					Matcher m1 = scoreP.matcher(line);
					while (m0.find()) {
						player.name = m0.group(1);
					}
					while (m1.find()) {
						player.winCount = Integer.valueOf(m1.group(1));
					}

				}

				// Always close files.
				bufferedReader.close();
			} catch (FileNotFoundException ex) {
				System.out.println("Unable to open file '" + fileName + "'");
			} catch (IOException ex) {
				System.out.println("Error reading file '" + fileName + "'");
				// Or we could just do this:
				// ex.printStackTrace();
			}
		}
	}
}