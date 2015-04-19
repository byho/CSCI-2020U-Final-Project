
public class main {

	public static void main(String[] args) {
		Server server = null;
		Client client = null;
		GameLoop glt = new GameLoop();
		glt.setVisible(true);
		if(args.length == 1){
			server = new Server(Integer.parseInt(args[0]));
		} else if (args.length == 2){
			client = new Client(args[0], Integer.parseInt(args[1]));
		}
	}

}
