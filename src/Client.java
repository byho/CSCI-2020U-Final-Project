import java.net.*;
import java.io.*;

public class Client implements Runnable {
	private Socket socket = null;
	private Thread thread = null;
	private DataInputStream console = null;
	private DataOutputStream streamOut = null;
	private ClientThread client = null;
	private String name = null;
	private boolean isLoggedIn = false;

	public Client(String serverName, int serverPort, String clientName) {
		
		name = clientName;
		System.out.println("Establishing connection. Please wait ...");
		try {
			socket = new Socket(serverName, serverPort);
			System.out.println("Connected: " + socket);
			start();
		} catch (UnknownHostException uhe) {
			System.out.println("Host unknown: " + uhe.getMessage());
		} catch (IOException ioe) {
			System.out.println("Unexpected exception: " + ioe.getMessage());
		}
	}

	public void run() {
		while (thread != null) {
			try {
				if (isLoggedIn == false) {
					isLoggedIn = true;
				} else {
					String dataString = "";
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
					streamOut.writeUTF(dataString);
				}
				streamOut.flush();
			} catch (IOException ioe) {
				System.out.println("Sending error: " + ioe.getMessage());
				stop();
			}
		}
	}

	public void handle(String msg) {
		String delims = "[ ]+";
		
		String[] tokens = msg.split(delims);
		
		if (msg.equals(".bye")) {
			System.out.println("Good bye. Press RETURN to exit ...");
			stop();
		} else if(tokens[0].equals("start")){
			GameLoop.getInstance().startGame();
		}
		else
		if (msg.equals(tokens[0].equals("data"))) {
			GameLoop.getInstance().setFireBullet(Boolean.valueOf(tokens[1]));
			GameLoop.getInstance().setBulletOwner(Integer.valueOf(tokens[2]));
			GameLoop.getInstance().setRemoteX(Integer.valueOf(tokens[3]));
			GameLoop.getInstance().setRemoteY(Integer.valueOf(tokens[4]));
			GameLoop.getInstance().setRemoteRot(Integer.valueOf(tokens[5]));
		}
	
		//fireBullet ID X Y Rot
	}

	public void start() throws IOException {
		console = new DataInputStream(System.in);
		streamOut = new DataOutputStream(socket.getOutputStream());
		if (thread == null) {
			client = new ClientThread(this, socket);
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
		try {
			if (console != null)
				console.close();
			if (streamOut != null)
				streamOut.close();
			if (socket != null)
				socket.close();
		} catch (IOException ioe) {
			System.out.println("Error closing ...");
		}
		client.close();
		client.stop();
	}

	
}
