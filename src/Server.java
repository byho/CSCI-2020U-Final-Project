import java.net.*;
import java.io.*;

public class Server implements Runnable {
	private ServerThread clients[] = new ServerThread[50];
	private ServerSocket server = null;
	private Thread thread = null;
	private int clientCount = 0;
	private String clientNames[] = new String[50];

	public Server(int port) {
		try {
			System.out
					.println("Binding to port " + port + ", please wait  ...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port " + port + ": "
					+ ioe.getMessage());
		}
	}

	public void run() {
		while (thread != null) {
			try {
				System.out.println("Waiting for a client ...");
				addThread(server.accept());
			} catch (IOException ioe) {
				System.out.println("Server accept error: " + ioe);
				stop();
			}
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	private int findClient(int ID) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getID() == ID)
				return i;
		return -1;
	}

	private int findClient(String name) {
		for (int i = 0; i < clientCount; i++) {
			if (clientNames[i].equals(name))
				return i;
		}
		return -1;
	}

	public synchronized void handle(int ID, String input) {

		String delims = "[ ]+";
		String dataString = "";
		String[] tokens = input.split(delims);

		if (input.equals(".bye")) {
			clients[findClient(ID)].send(".bye");
			remove(ID);
		}
		if (input.equals(tokens[0].equals("data"))) {
			GameLoop.getInstance().setFireBullet(Boolean.valueOf(tokens[1]));
			GameLoop.getInstance().setBulletOwner(Integer.valueOf(tokens[2]));
			GameLoop.getInstance().setRemoteX(Integer.valueOf(tokens[3]));
			GameLoop.getInstance().setRemoteY(Integer.valueOf(tokens[4]));
			GameLoop.getInstance().setRemoteRot(Integer.valueOf(tokens[5]));
		}
		
		if(GameLoop.getInstance().getRunning())
			clients[findClient(ID)].send("start");

		
		String fireBullet = Boolean.toString(GameLoop.getInstance()
				.isFireBullet());
		String playerID = Integer
				.toString(GameLoop.getInstance().gamePanel.triangle.id);
		String playerX = Integer
				.toString((int) GameLoop.getInstance().gamePanel.triangle.x);
		String playerY = Integer
				.toString((int) GameLoop.getInstance().gamePanel.triangle.y);
		String playerRot = Integer
				.toString((int) GameLoop.getInstance().gamePanel.triangle.rot);

		dataString.concat("data " + fireBullet + " " + playerID + " " + playerX
				+ " " + playerY + " " + playerRot);

		clients[findClient(ID)].send(dataString);

		// data fireBullet ID X Y Rot
	}

	public synchronized void remove(int ID) {
		int pos = findClient(ID);
		if (pos >= 0) {
			ServerThread toTerminate = clients[pos];
			System.out.println("Removing client thread " + ID + " at " + pos);
			if (pos < clientCount - 1)
				for (int i = pos + 1; i < clientCount; i++)
					clients[i - 1] = clients[i];
			clientCount--;
			try {
				toTerminate.close();
			} catch (IOException ioe) {
				System.out.println("Error closing thread: " + ioe);
			}
			toTerminate.stop();
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("Client accepted: " + socket);
			clients[clientCount] = new ServerThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		} else
			System.out.println("Client refused: maximum " + clients.length
					+ " reached.");
	}

}
