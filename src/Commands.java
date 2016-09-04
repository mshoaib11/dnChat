import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Commands {
	
	static ConcurrentHashMap<String, String> ARRVfromOtherServers = new ConcurrentHashMap<String, String>(); //data structure to hold users from other servers
	static ConcurrentHashMap<String, Socket> ARRVfromDirectlyConnectedServers = new ConcurrentHashMap<String, Socket>(); //data structure to hold users from directly connected servers
	static Map<String, Integer> userNumberHopCountMapping = new HashMap<String, Integer>(); //data structure to keep the hop count of all users from other servers
	static Map<String, Socket> userNumberNextHopMapping = new HashMap<String, Socket>(); //data structure to hold the next hop connection of all the users from other servers
	static List<String> LEFTrecieved = new CopyOnWriteArrayList<String>(); //data structure to store LEFT recieved to ensure controlled flooding
	static ConcurrentHashMap<String, String> ACKNrecieved = new ConcurrentHashMap<String, String>(); //data structure to store ACKN recieved to ensure controlled flooding
	static List<String> SENDcommandToOtherServers = new CopyOnWriteArrayList<String>(); //data structure to store SEND commands that has been sent to users of other servers
	static List<String> chatNumberRecieved = new CopyOnWriteArrayList<String>(); //data structure to hold chat numbers that have been recieved to ensure controlled flooding
	static Map<String, String> SENDrecieved_chatNumToSenderNumMapping = new HashMap<String, String>(); //data structure to store SENd commands that has been recieved to ensure controlled flooding
	static Map<String, Socket> messageSentToOtherServer = new HashMap<String, Socket>(); //data structure to store messages that have been sent by users of this server
	

	public static void sendSRVRcommand(Socket connection, boolean masking) throws IOException {
		String number =  "49362979299699546";
		String SRVRcommand = "SRVR " + number;
		SendMessage.sendMessage(SRVRcommand, connection.getOutputStream(), masking);
	}
	
	public static void SendARRVtoUser_newlyConnected(Socket connection) throws IOException, InterruptedException {
		for (Entry<String, String> entry : ARRVfromOtherServers.entrySet())  {
			String ARRVcommand = entry.getValue();
			SendMessage.sendMessage(ARRVcommand, connection.getOutputStream(), false);
		}
	}
	
	public static void sendARRVtoOtherServer_alreadyConnected(Socket connection, boolean masking) throws IOException, InterruptedException { //sending ARRV commands to server when a connection is established
        for (Entry<String, String> entryUsers : HandleReadMessage.currentUsers.entrySet()) { //sending information of locally connected users
        	String ARRVcommand = "ARRV "+entryUsers.getKey()+"\r\n"+ entryUsers.getValue()+"\r\nGroup6\r\n"+"0";
        	SendMessage.sendMessage(ARRVcommand, connection.getOutputStream(), masking);
        }
                
        for (Entry<String, String> entry : ARRVfromOtherServers.entrySet()) {//sending information of users that arrived from other servers
        	String ARRVcommand = entry.getValue();
        	String line1 = ARRVcommand.split("\n")[0].trim();
        	String line2 = ARRVcommand.split("\n")[1].trim();
        	String line3 = ARRVcommand.split("\n")[2].trim();
        	int hopCount = Integer.parseInt(ARRVcommand.split("\n")[3].trim())+1;
        	String newARRVcommand = line1+"\r\n"+line2+"\r\n"+line3+"\r\n"+hopCount;
        	SendMessage.sendMessage(newARRVcommand, connection.getOutputStream(), masking);
		} 
	}
	
	public static void sendARRVtoOtherServer_newlyConnected(String userNumber, String userName) throws IOException, InterruptedException { //sending ARRV commands to all connected servers when a new user logs in
		for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
			String ARRVcommand = "ARRV "+userNumber+"\r\n"+ userName+"\r\nGroup6\r\n"+"0";
        	SendMessage.sendMessage(ARRVcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
		}
		for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
			String ARRVcommand = "ARRV "+userNumber+"\r\n"+ userName+"\r\nGroup6\r\n"+"0";
        	SendMessage.sendMessage(ARRVcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true);
		}
	}

	static void ARRVcommandRecieved(String ARRVcommand, Socket recievedFromConnection) throws IOException, InterruptedException { //manipulating the ARRV command that is recieved from other server
		String userNumber = ARRVcommand.split("\n")[0].split(" ")[1].trim();
		int hopCount = Integer.parseInt(ARRVcommand.split("\n")[3].trim());	
		if(hopCount==16){
			LEFTcommandRecieved(userNumber, recievedFromConnection);
		}
		else{
			if(!HandleReadMessage.currentUsers.containsKey(userNumber)){
				if(userNumberHopCountMapping.containsKey(userNumber)){
					int existingHopCount = userNumberHopCountMapping.get(userNumber);
					if(hopCount<existingHopCount){ //updating the hop count for this user and send the new ARRV with updated hop count to all the locally connected users and also all other connected servers
						String line1 = ARRVcommand.split("\n")[0].trim();
						String line2 = ARRVcommand.split("\n")[1].trim();
			        	String line3 = ARRVcommand.split("\n")[2].trim();
			        	String newARRVcommand = line1+"\r\n"+line2+"\r\n"+line3+"\r\n"+hopCount;
						ARRVfromOtherServers.put(userNumber, newARRVcommand);
						if(ClientThread.serverConnectionArray.contains(recievedFromConnection) || ServerThread.clientConnectionArray.contains(recievedFromConnection)){
							ARRVfromDirectlyConnectedServers.put(userNumber, recievedFromConnection);
						}
						userNumberHopCountMapping.put(userNumber, hopCount);
						userNumberNextHopMapping.put(userNumber, recievedFromConnection);
						SendARRVtoUsers_alreadyConnected(newARRVcommand); //sending ARRVcommand recieved from other server to all users currently logged in
						sendARRVtoOtherServer(ARRVcommand, recievedFromConnection); //sending ARRV of all non-local users to newly connected user
					}
				} 
				else{ //sending the ARRV recieved to all other connected servers
					ARRVfromOtherServers.put(userNumber, ARRVcommand);
					if(ClientThread.serverConnectionArray.contains(recievedFromConnection) || ServerThread.clientConnectionArray.contains(recievedFromConnection)){
						ARRVfromDirectlyConnectedServers.put(userNumber, recievedFromConnection);
					}
					userNumberHopCountMapping.put(userNumber, hopCount);
					userNumberNextHopMapping.put(userNumber, recievedFromConnection);
					SendARRVtoUsers_alreadyConnected(ARRVcommand); //sending ARRVcommand recieved from other server to all users currently logged in
					sendARRVtoOtherServer(ARRVcommand, recievedFromConnection); //sending ARRV of all non-local users to newly connected user
				}
			}
		}
	}
	
	public static void SendARRVtoUsers_alreadyConnected(String message) throws IOException, InterruptedException {
		if(HandleReadMessage.connectionOutputArray.size()>0){
			for (Entry<String, Socket> entry : HandleReadMessage.connectionOutputArray.entrySet())  {
				SendMessage.sendMessage(message, entry.getValue().getOutputStream(), false);
			}
		}
	}
		
	private static void sendARRVtoOtherServer(String message, Socket recievedFromConnection) throws IOException, InterruptedException {
		for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
			if(!ServerThread.clientConnectionArray.get(i).equals(recievedFromConnection)){
			String ARRVcommand = message;
			String line1 = ARRVcommand.split("\n")[0].trim();
        	String line2 = ARRVcommand.split("\n")[1].trim();
        	String line3 = ARRVcommand.split("\n")[2].trim();
        	int hopCount = Integer.parseInt(ARRVcommand.split("\n")[3].trim())+1;
        	String newARRVcommand = line1+"\r\n"+line2+"\r\n"+line3+"\r\n"+hopCount;
			SendMessage.sendMessage(newARRVcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
			}
		}
		for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
			if(!ClientThread.serverConnectionArray.get(i).equals(recievedFromConnection)){
				String ARRVcommand = message;
				String line1 = ARRVcommand.split("\n")[0].trim();
				String line2 = ARRVcommand.split("\n")[1].trim();
				String line3 = ARRVcommand.split("\n")[2].trim();
				int hopCount = Integer.parseInt(ARRVcommand.split("\n")[3].trim())+1;
				String newARRVcommand = line1+"\r\n"+line2+"\r\n"+line3+"\r\n"+hopCount;
				SendMessage.sendMessage(newARRVcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true);
			}
		}
			
	}

	static void LEFTcommandRecieved(String userNumber, Socket recievedFromConnection) throws IOException, InterruptedException { //manipulating the LEFT command that is recieved from other connected server
		String LEFTcommand = null;
		if(!LEFTrecieved.contains(userNumber)){ //ensuring controlled flooding
			if(ARRVfromOtherServers.containsKey(userNumber) && userNumberHopCountMapping.containsKey(userNumber)){
				Map<String, Socket> connectionOutputArray = HandleReadMessage.connectionOutputArray;
				LEFTcommand = "LEFT "+userNumber;
				for (Entry<String, Socket> entry : connectionOutputArray.entrySet())
				{
					try {
						SendMessage.sendMessage("LEFT "+userNumber, entry.getValue().getOutputStream(), false); //sending LEFT command to all users currently logged in
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} 
				}
			//sending LEFTcommand to other servers
			for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
				if(!ServerThread.clientConnectionArray.get(i).equals(recievedFromConnection)){
					SendMessage.sendMessage(LEFTcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
				}
			}	
			for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
				if(!ClientThread.serverConnectionArray.get(i).equals(recievedFromConnection)){
					SendMessage.sendMessage(LEFTcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true);
				}
			}
			ARRVfromOtherServers.remove(userNumber);
			ARRVfromDirectlyConnectedServers.remove(userNumber);
			userNumberHopCountMapping.remove(userNumber);
			userNumberNextHopMapping.remove(userNumber);
			LEFTrecieved.add(userNumber);
			}
		}
	}
	
	static void sendLEFTcommandToOtherServers(Socket connection) throws IOException, InterruptedException { //sending left command to all other connected servers when a locally connected user disconnects
		String userNumberLeft = HandleReadMessage.connectionToUserNumberMapping.get(connection);
		for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
			String LEFTcommand = "LEFT "+userNumberLeft;
			SendMessage.sendMessage(LEFTcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
		}
		for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
			String LEFTcommand = "LEFT "+userNumberLeft;
			SendMessage.sendMessage(LEFTcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true);
		}
		
	}
	
	public static void SENDcommandToOtherServers_broadcast(String SENDcommand, Socket senderConnection) throws IOException, InterruptedException { //sending broadcast message sent by locally connected user to other servers
		String chatNumber = SENDcommand.split("\n")[0].split(" ")[1].trim();
		String line1 = SENDcommand.split("\n")[0].trim();
		String line2 = "*";
		String line3 = SENDcommand.split("\n")[1].trim();
		String line4 = SENDcommand.split("\n")[2].trim();
		String newSENDcommand = line1+"\r\n"+line2+"\r\n"+line3+"\r\n"+line4;
		for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
			SendMessage.sendMessage(newSENDcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
		}
		
		for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
			SendMessage.sendMessage(newSENDcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true);
		}
		messageSentToOtherServer.put(chatNumber, senderConnection);
	}
	
	public static void SENDcommandToOtherServers_unitcast(String SENDcommand, String messageRecepient, Socket senderConnection) throws IOException, InterruptedException { //sending unicast message sent by locally connected user to all other connected servers
		for (Entry<String, String> entry : ARRVfromOtherServers.entrySet()) {
			String userNumberofReciever = entry.getKey();
			if(userNumberofReciever.equals(messageRecepient)){
				String chatNumber = SENDcommand.split("\n")[0].split(" ")[1].trim();
				String line1 = SENDcommand.split("\n")[0].trim();
				String line2 = userNumberofReciever;
				String line3 = SENDcommand.split("\n")[1].trim();
				String line4 = SENDcommand.split("\n")[2].trim();
				String newSENDcommand = line1+"\r\n"+line2+"\r\n"+line3+"\r\n"+line4;
				Socket nextHopConnection = userNumberNextHopMapping.get(userNumberofReciever);
				
				if(ServerThread.clientConnectionArray.contains(nextHopConnection)){
					SendMessage.sendMessage(newSENDcommand, nextHopConnection.getOutputStream(), false);
				}
				else if(ClientThread.serverConnectionArray.contains(nextHopConnection)){
					SendMessage.sendMessage(newSENDcommand, nextHopConnection.getOutputStream(), true);
				}	
			messageSentToOtherServer.put(chatNumber, senderConnection);
			}	
		}
	}
	
	public static void SENDcommandFromOtherServer(String userNumberOfReciever, String SENDcommand, Socket recieveFromConnection) throws IOException, InterruptedException { //manipulating the SEND command that is recieved from other servers
		String chatNumber = SENDcommand.split("\n")[0].trim().split(" ")[1].trim();
		String senderNumber = SENDcommand.split("\n")[2].trim();
		String line1 = SENDcommand.split("\n")[0].trim();
		String line2 = SENDcommand.split("\n")[2].trim();
		String line3 = SENDcommand.split("\n")[3].trim();
		String newSENDcommand = line1+"\r\n"+line2+"\r\n"+line3;		
		if(!chatNumberRecieved.contains(chatNumber)){ //ensuring controlled flooding
			if(userNumberOfReciever.equals("*")){ //checking if the recieved message is a broadcast message
				SENDtoLocalUsers(newSENDcommand, userNumberOfReciever);
				SENDtoOtherServers(SENDcommand, userNumberOfReciever, recieveFromConnection);
			}
			else{ //message recieved is unicast message
				if(HandleReadMessage.connectionToUserNumberMapping.containsValue(userNumberOfReciever)){ //sending message to locally connected user if destination user present in locally connected users.
					SENDtoLocalUsers(newSENDcommand, userNumberOfReciever);
				}
				else{ //sending message to other connected servers
					SENDtoOtherServers(SENDcommand, userNumberOfReciever, recieveFromConnection);
				}	
			}
		chatNumberRecieved.add(chatNumber);
		SENDrecieved_chatNumToSenderNumMapping.put(chatNumber, senderNumber);
		}
	}

	private static void SENDtoOtherServers(String SENDcommand, String userNumberOfReciever, Socket recieveFromConnection) throws IOException, InterruptedException { //sending SEND command recieved from other server to other connected servers
		if(userNumberOfReciever.equals("*")){ //broadcasting through the network
			for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
				if(!ServerThread.clientConnectionArray.get(i).equals(recieveFromConnection)){
					SendMessage.sendMessage(SENDcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false); 
				}
			}
			for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
				if(!ClientThread.serverConnectionArray.get(i).equals(recieveFromConnection)){
					SendMessage.sendMessage(SENDcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true); 
				}
			}
		}
		else{ //sending message over a path with a minimum number of intermediate servers
			Socket nextHopConnection = userNumberNextHopMapping.get(userNumberOfReciever);
			if(ServerThread.clientConnectionArray.contains(nextHopConnection)){
				SendMessage.sendMessage(SENDcommand, nextHopConnection.getOutputStream(), false); 
			}
			else if (ClientThread.serverConnectionArray.contains(nextHopConnection)){
				SendMessage.sendMessage(SENDcommand, nextHopConnection.getOutputStream(), true); 
			}
		}
	}

	private static void SENDtoLocalUsers(String newSENDcommand, String userNumberOfReciever) throws IOException, InterruptedException { //sending message recieved from other server to locally connected user
		if(userNumberOfReciever.equals("*")){//checking if message recieved is broadcast message and must be sent to all users
			for (Entry<String, Socket> entry : HandleReadMessage.connectionOutputArray.entrySet()){
				SendMessage.sendMessage(newSENDcommand, entry.getValue().getOutputStream(), false); 
			}	
		}
		else{ //message recieved is unicast message and must be sent to a specific user
			Socket connection = HandleReadMessage.connectionOutputArray.get(userNumberOfReciever);
			SendMessage.sendMessage(newSENDcommand, connection.getOutputStream(), false); 
		}

	}

	public static void sendACKNtoNonLocalUser(String chatNumber, String userNumberOfAcknowledger) throws IOException, InterruptedException { //sending ACKN to a message that was recieved from a user from another server
		if(chatNumberRecieved.contains(chatNumber)){ //ensuring controlled flooding
			String senderNumber = SENDrecieved_chatNumToSenderNumMapping.get(chatNumber);
			String ACKNcommand = "ACKN "+chatNumber+"\r\n"+userNumberOfAcknowledger+"\r\n"+senderNumber;
			Socket nextHopConnection = userNumberNextHopMapping.get(senderNumber);	
			//sending ACKN to all the connected servers
			if(ServerThread.clientConnectionArray.contains(nextHopConnection)){
				SendMessage.sendMessage(ACKNcommand, nextHopConnection.getOutputStream(), false);
			}
			else if(ClientThread.serverConnectionArray.contains(nextHopConnection)){
				SendMessage.sendMessage(ACKNcommand, nextHopConnection.getOutputStream(), true);
			}
		}
	}

	public static void ACKNrecievedFromOtherServer(String ACKNcommand) throws IOException, InterruptedException { //manipulating ACKN recieved from another server
		String chatNumber = ACKNcommand.split("\n")[0].split(" ")[1].trim();
		String userNumberOfAcknowledger = ACKNcommand.split("\n")[1].trim();
		String senderNumber = ACKNcommand.split("\n")[2].trim();
		String line1 = ACKNcommand.split("\n")[0].trim();
		String line2 = ACKNcommand.split("\n")[1].trim();
		String newACKNcommand = line1+"\r\n"+line2;
		 if (ACKNrecieved.size() > 0) {
			 for (Entry<String, String> entry : ACKNrecieved.entrySet()){ 
				 if(!(entry.getKey().equals(chatNumber) && entry.getValue().equals(userNumberOfAcknowledger))){ //ensuring controlled flooding
			          sendACKNtoLocalUsers(newACKNcommand, chatNumber); //sending ACKN to a locally connected user
			          sendACKNtoOtherServers(ACKNcommand, chatNumber, senderNumber); //sending ACKN to all other connected servers
			          ACKNrecieved.put(chatNumber, userNumberOfAcknowledger);
			          break;
				}
		    }
		}
		else {
		  sendACKNtoLocalUsers(newACKNcommand, chatNumber); //sending ACKN to a locally connected user
		  sendACKNtoOtherServers(ACKNcommand, chatNumber, senderNumber); //sending ACKN to all other connected servers
		  ACKNrecieved.put(chatNumber, userNumberOfAcknowledger);
		}
		Thread.sleep(500);
	}
	
	private static void sendACKNtoOtherServers(String ACKNcommand, String chatNumber, String senderNumber) throws IOException, InterruptedException {
	    if (SENDrecieved_chatNumToSenderNumMapping.containsKey(chatNumber)) {
	      Socket nextHopConnection = (Socket)userNumberNextHopMapping.get(senderNumber); //sending ACKN over a path with a minimum number of intermediate servers
	      if(ServerThread.clientConnectionArray.contains(nextHopConnection)){
				SendMessage.sendMessage(ACKNcommand, nextHopConnection.getOutputStream(), false);
			}
			else if(ClientThread.serverConnectionArray.contains(nextHopConnection)){
				SendMessage.sendMessage(ACKNcommand, nextHopConnection.getOutputStream(), true);
			}
	    }
	}

	private static void sendACKNtoLocalUsers(String newACKNcommand, String chatNumber) throws IOException, InterruptedException{
		if (messageSentToOtherServer.containsKey(chatNumber)) //checking if ACKN recieved is for a message that was sent by a locally connected user
	    {
	      Socket connection = messageSentToOtherServer.get(chatNumber);
	      SendMessage.sendMessage(newACKNcommand, connection.getOutputStream(), false);
	    }
	}

	public static void sendLEFTonDisconnect() throws IOException, InterruptedException { //sending LEFT command of locally connected users to all other connected servers when this server exits/disconnects 
		for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
			for (Entry<String, String> entry : HandleReadMessage.currentUsers.entrySet()){
				String userNumber = entry.getKey();
				String LEFTcommand = "LEFT "+userNumber;
				SendMessage.sendMessage(LEFTcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
			}
		}
		for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
			for (Entry<String, String> entry : HandleReadMessage.currentUsers.entrySet()){
				String userNumber = entry.getKey();
				String LEFTcommand = "LEFT "+userNumber;
				SendMessage.sendMessage(LEFTcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(),true);
			}
		}
	}

	public static void serverDisconnetRecieved(Socket disconnectedConnection) throws IOException, InterruptedException { //method to execute when another server exits/disconnects
		for (Entry<String, Socket> entryARRV : ARRVfromDirectlyConnectedServers.entrySet()){
			String userNumber = entryARRV.getKey();
			String LEFTcommand = "LEFT "+userNumber;
			for (Entry<String, Socket> entryConnection : HandleReadMessage.connectionOutputArray.entrySet()){
				if(entryARRV.getValue().equals(disconnectedConnection)){
					SendMessage.sendMessage(LEFTcommand, entryConnection.getValue().getOutputStream(), false);
					ARRVfromOtherServers.remove(userNumber);
					userNumberHopCountMapping.remove(userNumber);
					userNumberNextHopMapping.remove(userNumber);
					ARRVfromDirectlyConnectedServers.remove(userNumber);
					LEFTrecieved.add(userNumber);
				}
			}
			//sending LEFTcommand to other servers
			for(int i=0; i<ServerThread.clientConnectionArray.size(); i++){
				if(!ServerThread.clientConnectionArray.get(i).equals(disconnectedConnection)){
					SendMessage.sendMessage(LEFTcommand, ServerThread.clientConnectionArray.get(i).getOutputStream(), false);
				}
			}
			for(int i=0; i<ClientThread.serverConnectionArray.size(); i++){
				if(!ClientThread.serverConnectionArray.get(i).equals(disconnectedConnection)){
					SendMessage.sendMessage(LEFTcommand, ClientThread.serverConnectionArray.get(i).getOutputStream(), true);
				}
			}	
		}
		disconnectedConnection.close();
	}

	public static void pingRecievedFromOtherServer(Socket pingRecievedConnection, byte[] payload) {
		byte pong = 128 & 10;
		byte[] frame = appendData(pong, payload);
		try {
			pingRecievedConnection.getOutputStream().write(frame);
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}
	
	protected static byte[] appendData(byte firstObject,byte[] secondObject){
	    byte[] byteArray= {firstObject};
	    byte[] combined = new byte[byteArray.length + secondObject.length];
	    System.arraycopy(byteArray, 0, combined, 0, byteArray.length);
		return combined;
	}

	public static void pongRecievedFromOtherServer(Socket pongRecievedConnection, byte[] payload) {
		byte ping = 128 & 10;
		byte[] frame = appendData(ping, payload);
		try {
			pongRecievedConnection.getOutputStream().write(frame);
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}
}
