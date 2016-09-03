import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
public class Server {

	public static void main(String[] args) throws IOException {
		Server myServer = new Server();
		myServer.startRunning();
		}
	
	public static ArrayList<Socket> userConnectionArray = new ArrayList<Socket>(); //data structure for holding all the alive connections
	Socket connection;
	static ServerSocket server;
	
	public void startRunning() throws IOException{
		
		System.out.println("Enter the port: ");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String input = reader.readLine();
		int PORT = Integer.parseInt(input);
		try {
			server = new ServerSocket(PORT);
		}  catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} //starting the server on desired port
		System.out.println("Group 6's server is started. Listening on port "+PORT+".");
		System.out.println("User must give group 6 password to login.");
		
		new Thread(new CommandLineThread()).start();
		
		while(true){
			//if(connectionArray.size() <= 15){
				createConnection(server); //creates an incoming connection
				createThread(connection, userConnectionArray); //creates a thread for an incoming connection
			//}
		}
	}

	private void createThread(Socket connection, ArrayList<Socket> connectionArray) throws UnsupportedEncodingException, IOException {
			new Thread(new ServerThread(connection, connectionArray)).start();
	}

	private void createConnection(ServerSocket server) {
		try {
			connection = server.accept(); //accepting the incoming connection request
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} 
		userConnectionArray.add(connection);
		//System.out.println("Connected to "+connection.getInetAddress().getHostAddress()+" on port "+connection.getPort());
	}

	public static void shutdown() throws IOException, InterruptedException {
		byte[] closeFrame = {(byte) 136 , 28};
		OutputStream output = null;
		for(int i=0; i<userConnectionArray.size(); i++){
			try {
				output = userConnectionArray.get(i).getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			try {
				output.write(closeFrame);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			try {
				output.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		ServerThread.sendDisconnetToOtherServers();
	}
}
