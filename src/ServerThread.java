import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ServerThread implements Runnable {
	
	InputStream input; //data structure for input stream for a connection
	OutputStream output; //data structure for output stream for a connection
	Socket connection;
	ArrayList<Socket> userConnectionArray = new ArrayList<Socket>(); //data structure for holding all the alive connections
	String UTF8_message;
	String nonUTF8_message;
	byte[] bytesRecieved; //data structure to store the bytes read from an input stream
	static Map<String, Integer> totalDataRecieved = new HashMap<String, Integer>();
	Charset UTF8 = Charset.forName("UTF-8");
	static ArrayList<Socket> clientConnectionArray = new ArrayList<Socket>();
	
	public ServerThread(Socket connection, ArrayList<Socket> connectionArray) {
		this.connection = connection;
		this.userConnectionArray = connectionArray;
	}

	public void run() {
		try {
			output = connection.getOutputStream(); //output stream for an incoming connection
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

		try {
			input = connection.getInputStream(); //input stream for an incoming connection
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			HandShake.sendHandshakeResponse(output, input);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			whileChatting();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //function to execute when handshake response is sent to client and connection is successfully established

}

	private void whileChatting() throws InterruptedException {

	try{
		while(true){
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] readBuffer=new byte[1024];
		int length = input.read(readBuffer);
		
		if(length != -1){
			Thread.sleep(500); //waiting for more data from the input stream of a connection
			byteArrayOutputStream.write(readBuffer, 0, length);
			while (input.available() > 0) {
			    length = input.read(readBuffer);
			    byteArrayOutputStream.write(readBuffer, 0, length);
				}
					byte[] buffer = byteArrayOutputStream.toByteArray();
					byteArrayOutputStream.flush();
		        	totalDataRecieved = ReadMessage.readMessage(buffer, length, connection);
	
		        	for (Entry<String, Integer> entry : totalDataRecieved.entrySet())
		     		{
		        		Thread.sleep(500);
		        		bytesRecieved = entry.getKey().getBytes();	
		        		int opcode = entry.getValue();
			        	String command = new String(bytesRecieved,UTF8).split(" ")[0].trim();
			        	
			        	if(command.equals("SRVR")){
			        		this.userConnectionArray.remove(connection);
			        		Server.userConnectionArray.remove(connection);
			        		clientConnectionArray.add(connection);
			        		Commands.sendARRVtoOtherServer_alreadyConnected(connection, true);
						}
			        	
			        	if(command.equals("ARRV") && clientConnectionArray.contains(connection)){
							String ARRVcommand = new String(bytesRecieved,UTF8);
							Commands.ARRVcommandRecieved(ARRVcommand, connection);
						}
				    	
				    	if(command.equals("LEFT") && clientConnectionArray.contains(connection)){
							String userNumber = new String(bytesRecieved,UTF8).split(" ")[1].trim();
							Commands.LEFTcommandRecieved(userNumber, connection);
						}
				    	
				    	if(command.equals("SEND") && clientConnectionArray.contains(connection)){
				    		String SENDcommand = new String(bytesRecieved,UTF8);
				    		String userNumberOfReciever = SENDcommand.split("\n")[1].trim();
				    		Commands.SENDcommandFromOtherServer(userNumberOfReciever, SENDcommand, connection);
				    	}
				    	
				    	if(command.equals("ACKN") && clientConnectionArray.contains(connection)){
				    		Thread.sleep(2000);
				    		String ACKNcommand = new String(bytesRecieved,UTF8);
				    		Commands.ACKNrecievedFromOtherServer(ACKNcommand);
				    	}

				    	
				    	if(command.equals("AUTH") && HandleReadMessage.connectionToNameMapping.containsKey(connection) && userConnectionArray.contains(connection)){ //somebody trying to login twice from same connection
							Commands.sendLEFTcommandToOtherServers(connection);
							malfomedTextRecieved();
						}
						
						else if(opcode == 0 && userConnectionArray.contains(connection) && userConnectionArray.contains(connection)){
							Commands.sendLEFTcommandToOtherServers(connection);
							fragmentedMessageRecieved();
						}
						
						else if(opcode == 2 && userConnectionArray.contains(connection) && userConnectionArray.contains(connection)){
							Commands.sendLEFTcommandToOtherServers(connection);
							binaryMessageRecieved();
						}
						
						else if(opcode == 9 && userConnectionArray.contains(connection) && userConnectionArray.contains(connection)){
							pingRecieved(bytesRecieved);
						}
						
						else if(opcode == 10 && userConnectionArray.contains(connection) && userConnectionArray.contains(connection)){
							pongRecieved(bytesRecieved);
						}
						
						else if(bytesRecieved.length > 300 && opcode ==1 && userConnectionArray.contains(connection)){ //if number of bytes > 300, send a LENGTH failure message to client
							Commands.sendLEFTcommandToOtherServers(connection);
							textLengthExceeded();
						}
						
						else if(opcode == 1 || opcode == 8 && userConnectionArray.contains(connection)){ //if number of bytes < 300, convert the bytes recieved to string and proceed accordingly
							UTF8_message = new String(bytesRecieved,UTF8);
							nonUTF8_message = new String(bytesRecieved);
							
							if(!UTF8_message.equals(nonUTF8_message) && opcode == 1 && userConnectionArray.contains(connection)){ //malformed message recieved
								Commands.sendLEFTcommandToOtherServers(connection);
								malfomedTextRecieved();
							}
							else if ((!UTF8_message.equals(nonUTF8_message) || UTF8_message.equals("")) && opcode == 8 && userConnectionArray.contains(connection)){ 
								Commands.sendLEFTcommandToOtherServers(connection);
								closeConnectionRequest();
							}
							
							else if (UTF8_message.equals(nonUTF8_message) && opcode == 1 && userConnectionArray.contains(connection)){ 
								HandleReadMessage.handleReadMessage(UTF8_message, connection); //handleReadMessage function of HandleReadMessage class handles commands from client
																							   //such as AUTH, SEND, ACKN 
							}		
						}
						else{
							Commands.sendLEFTcommandToOtherServers(connection);
							malfomedTextRecieved();
						}
			        }
				}
			}
		}
	

	catch (IOException | IndexOutOfBoundsException e ) {
	} 
	finally {
	    closeSilently(connection);
	}
}
	
	private void closeSilently(Socket connection) throws InterruptedException {
	    if (connection != null) {
	        try {
	        	
	        	connection.getInputStream().close();
	        	connection.getOutputStream().close();
	        	connection.close();
	        } catch (IOException e) {}
	   }
}

	private void closeConnectionRequest(){ //method to execute when connection close request is recieved from client at opcoode 8 and empty message
		if(HandleReadMessage.connectionToNameMapping.get(connection)!= null){
			String userName = HandleReadMessage.connectionToNameMapping.get(connection);
			//System.out.println("Close connection request recieved from user "+ userName+ ". Closing Stuff for this connection!");
		}
		else{
			//System.out.println("Close connection request recieved from connection listening on port "+ connection.getPort()+ ". Closing Stuff for this connection!");
		}
		closeStuff(connection, output, input, userConnectionArray); //closing all the stuff associated to this client 
	}

	private void textLengthExceeded() {
		if(HandleReadMessage.connectionToNameMapping.get(connection)!= null){
			String userName = HandleReadMessage.connectionToNameMapping.get(connection);
			//System.out.println("Text length exceeded by user "+ userName + "!");
		}
		else{
			//System.out.println("Text length exceeded by connection listening on port "+ connection.getPort() + "!");
		}
		String chatNumber = new String(bytesRecieved, UTF8).split("\n")[0].split(" ")[1].trim();
		SendMessage.sendMessage("FAIL "+chatNumber+"\r\nLENGTH", output, false);
		
	}

	private void pongRecieved(byte[] bytesRecieved) {
		
		byte ping = 128 & 10;
		byte[] frame = appendData(ping, bytesRecieved);
		try {
			output.write(frame);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	protected byte[] appendData(byte firstObject,byte[] secondObject){
	    byte[] byteArray= {firstObject};
	    byte[] combined = new byte[byteArray.length + secondObject.length];
	    System.arraycopy(byteArray, 0, combined, 0, byteArray.length);
		return combined;
	    
	}

	private void pingRecieved(byte[] bytesRecieved) {
		byte pong = 128 & 10;
		byte[] frame = appendData(pong, bytesRecieved);
		try {
			output.write(frame);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
	}

	private void binaryMessageRecieved() {
		malfomedTextRecieved();
		
	}

	private void fragmentedMessageRecieved() {
		malfomedTextRecieved();
		
	}

	private void malfomedTextRecieved() {
		SendMessage.sendMessage("INVD 0", output, false); 
		if(HandleReadMessage.connectionToNameMapping.get(connection)!= null){
			String userName = HandleReadMessage.connectionToNameMapping.get(connection);
			//System.out.println("Malformed text recieved from user "+ userName+ ". Closing Stuff for this connection!");
		}
		else{
			//System.out.println("Malformed text recieved from connection listening on port "+ connection.getPort()+ ". Closing Stuff for this connection!");
		}
		closeStuff(connection, output, input, userConnectionArray); //closing all the stuff associated to this client 
	}

	private void closeStuff(Socket connection, OutputStream output, InputStream input, ArrayList<Socket> userConnectionArray){
		HandleReadMessage.closeStuff(connection);
		try {
			connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			input.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		userConnectionArray.remove(connection);
		
	}

	public static void sendDisconnetToOtherServers() throws IOException, InterruptedException {
		//Commands.sendLEFTonDisconnect();
		byte[] closeFrame = {(byte) 136 , 28};
		OutputStream output = null;
		for(int i=0; i<clientConnectionArray.size(); i++){
			try {
				output = clientConnectionArray.get(i).getOutputStream();
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
		
		for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
			try {
				output = ClientThread.serverConnectionArray.get(i).getOutputStream();
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
	}
}
