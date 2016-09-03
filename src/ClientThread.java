import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class ClientThread implements Runnable {

	static ArrayList<Socket> serverConnectionArray = new ArrayList<Socket>();
	Socket serverConnection;
	OutputStream output;
	InputStream input;
	byte[] bytesRecieved; //data structure to store the bytes read from an input stream
	Charset UTF8 = Charset.forName("UTF-8");
	static Map<String, Integer> totalDataRecieved = new HashMap<String, Integer>();

	
	public ClientThread(Socket serverConnection) {
		this.serverConnection = serverConnection;
	}

	@Override
	public void run() {

		try {
			Commands.sendSRVRcommand(serverConnection, true);
			Thread.sleep(1000);
			Commands.sendARRVtoOtherServer_alreadyConnected(serverConnection, true);
		} catch (IOException | InterruptedException e) {
			//e.printStackTrace();
		}

		try {
			output = serverConnection.getOutputStream();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		try {
			input = serverConnection.getInputStream();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		
		try {
			whileCommunicating();
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
		
	
	private void whileCommunicating() throws IOException, InterruptedException, IndexOutOfBoundsException {
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
			        	totalDataRecieved = ReadMessage.readMessage(buffer, length, serverConnection);
			        	for (Entry<String, Integer> entry : totalDataRecieved.entrySet())
			     		{
			        		Thread.sleep(500);
			        		bytesRecieved = entry.getKey().getBytes();
				        	String command = new String(bytesRecieved,UTF8).split(" ")[0].trim();
				        	
				        	if(command.equals("ARRV")){
				        		Commands.ARRVcommandRecieved(new String(bytesRecieved), serverConnection);
							 }
							else if(command.equals("LEFT")){
								String userNumber = new String(bytesRecieved,UTF8).split(" ")[1].trim();
								Commands.LEFTcommandRecieved(userNumber, serverConnection);
							}
				        	if(command.equals("SEND")){
				        		String SENDcommand = new String(bytesRecieved,UTF8);
				        		String userNumberOfReciever = SENDcommand.split("\n")[1].trim();
				        		Commands.SENDcommandFromOtherServer(userNumberOfReciever, SENDcommand, serverConnection);
				        	}
				        	
					    	if(command.equals("ACKN")){
					    		Thread.sleep(2000);
					    		String ACKNcommand = new String(bytesRecieved,UTF8);
					    		Commands.ACKNrecievedFromOtherServer(ACKNcommand);
					    	}
				        }
					}
				}
			}
		
		catch (IOException e ) {
		} 
		finally {
		    closeSilently(serverConnection);
		}
	}
		
		private void closeSilently(Socket serverConnection) throws InterruptedException {
		    if (serverConnection != null) {
		        try {
		        	serverConnection.getInputStream().close();
		        	serverConnection.getOutputStream().close();
		        	serverConnection.close();
		        } catch (IOException e) {}
		   }
		}
}
