
public class main {

	public static void main(String[] args) {
		Server server = null;
		Client client = null;
		GameLoop.getInstance();
		GameLoop.getInstance().setVisible(true);
		if(args.length == 1){
			server = new Server(Integer.parseInt(args[0]));
		} else if (args.length == 3){
			GameLoop.getInstance().setServer(false);
			client = new Client(args[0], Integer.parseInt(args[1]), args[2]);
		}
	}

}
