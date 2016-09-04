import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class HandleReadMessage {

	static Map<String, String> currentUsers = new HashMap<String, String>(); //data structure to hold the users currently logged in to server
	static Map<String, Socket> connectionArray = new HashMap<String, Socket>(); //data structure for holding all the alive connections
	static Map<String, Socket> connectionOutputArray = new HashMap<String, Socket>(); //data structure for holding user number along with associated connections
	static Map<Socket, String> connectionToNameMapping = new HashMap<Socket, String>(); //data structure for holding connection along with associated user name
	static Map<Socket, String> connectionToUserNumberMapping = new HashMap<Socket, String>(); //data structure for holding user number along with associated connection
	static Map<String, Socket> messageRecievers = new HashMap<String, Socket>(); //data structure for holding user number of message recievers along with associated connection
	static Map<String, Socket> messageSenders = new HashMap<String, Socket>(); //data structure for holding chat number used by message sender along with associated connection
	static Socket connection;
	static String userNumber;
	static String userName;
	
	public static void handleReadMessage(String message, Socket currentConnection) throws IOException, InterruptedException {
		connection = currentConnection;	
		String command = message.split(" ")[0].trim();
		if(command.equals("AUTH")){
			loginRequest(message);
		}
		if(command.equals("SEND")){
			sendMessageRequest(message);
		}
		if(command.equals("ACKN")){
			messageAcknowledged(message);
		}
	}

	private static void messageAcknowledged(String message) throws IOException, InterruptedException {
		String chatNumber = message.split(" ")[1].trim();
		String userNumberOfAcknowledger = null;
		for (Entry<String, Socket> entrySenders : messageSenders.entrySet()){
			if(entrySenders.getKey().equals(chatNumber)){
				for (Entry<String, Socket> entry : messageRecievers.entrySet()){
					if(entry.getValue().equals(connection)){
						userNumberOfAcknowledger = entry.getKey();
						try {
							SendMessage.sendMessage("ACKN "+chatNumber+"\r\n"+ userNumberOfAcknowledger, entrySenders.getValue().getOutputStream(), false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
						}
					}
				}
			}
		}
		String UserNumberOfAcknowledger = connectionToUserNumberMapping.get(connection);
		Commands.sendACKNtoNonLocalUser(chatNumber, UserNumberOfAcknowledger);
	}

	private static void sendMessageRequest(String message) throws IOException, InterruptedException {
		String chatNumber = message.split("\n")[0].split(" ")[1].trim();
		String messageRecepients = message.split("\n")[1].trim();
		String messageToSend = message.split("\n")[2].trim();
		String userNumberOfSender = null;
		Long currentChatNumber = new Long(chatNumber);
		Long MaxChatNumber = new Long("9007199254740991");
		
	if(!messageSenders.containsKey(chatNumber)){
			
		if(currentChatNumber < MaxChatNumber){
			for (Entry<String, Socket> entry : connectionOutputArray.entrySet()) {
				if(entry.getValue().equals(connection)){
					userNumberOfSender = entry.getKey();
					messageSenders.put(chatNumber, connection);
				}
			}
			if(messageRecepients.equals("*")){
					String SENDcommand = "SEND "+chatNumber+"\r\n"+ userNumberOfSender+"\r\n"+messageToSend;
					//System.out.println("User "+currentUsers.get(userNumberOfSender)+" sends a message to all logged in! ");
					for (Entry<String, Socket> entry : connectionOutputArray.entrySet()) {
						if(!entry.getValue().equals(connection)){
							String userNumberOfRecievers = entry.getKey();
							try {
								SendMessage.sendMessage(SENDcommand, entry.getValue().getOutputStream(), false);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
							}						
							messageRecievers.put(userNumberOfRecievers, entry.getValue());
						}
					}
					Commands.SENDcommandToOtherServers_broadcast(SENDcommand, connection);
					try {
						SendMessage.sendMessage("OKAY "+chatNumber, connection.getOutputStream(), false);
					} catch (IOException e) {
						// TODO Auto-generated catch block
					}
				}

			else if (currentUsers.containsKey(messageRecepients)){
				Socket recepientConnection = connectionOutputArray.get(messageRecepients);
				try {
					SendMessage.sendMessage("SEND "+chatNumber+"\r\n"+ userNumberOfSender+"\r\n"+messageToSend, recepientConnection.getOutputStream(), false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
				try {
					SendMessage.sendMessage("OKAY "+chatNumber, connection.getOutputStream(), false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
				messageRecievers.put(messageRecepients, recepientConnection);
			}
			else if(Commands.ARRVfromOtherServers.containsKey(messageRecepients)){
				String SENDcommand = "SEND "+chatNumber+"\r\n"+ userNumberOfSender+"\r\n"+messageToSend;
				Commands.SENDcommandToOtherServers_unitcast(SENDcommand, messageRecepients, connection);
				SendMessage.sendMessage("OKAY "+chatNumber, connection.getOutputStream(), false);
			}
			else{
				try {
					SendMessage.sendMessage("FAIL "+chatNumber+"\r\n"+"NAME", connection.getOutputStream(), false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				} //user does not exist
			}
		}
		else{
			try {
				SendMessage.sendMessage("FAIL "+chatNumber+"\r\n"+"NUMBER", connection.getOutputStream(), false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
			} //chat number exceeded the allocated limit
		}
	}
	else{
		try {
			SendMessage.sendMessage("FAIL "+chatNumber+"\r\n"+"NUMBER", connection.getOutputStream(), false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
		} //chat number already in use
	}
}

	private static void loginRequest(String message) throws InterruptedException {
		userNumber = message.split("\n")[0].split(" ")[1].trim();;
		userName = message.split("\n")[1].trim();
		String password = message.split("\n")[2].trim();
		Long currentUserNumber = new Long(userNumber);
		Long MaxUserNumber = new Long("9007199254740991");
		if(currentUserNumber < MaxUserNumber){
			if(currentUsers.containsKey(userNumber)){ //sending NUMBER failure if user number already in use by another user
				try {
					SendMessage.sendMessage("FAIL "+userNumber+"\r\nNUMBER", connection.getOutputStream(), false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
			}
			else if(currentUsers.containsValue(userName)){ //sending NAME failure if user name already in use by another user
				try {
					SendMessage.sendMessage("FAIL "+userNumber+"\r\nNAME", connection.getOutputStream(), false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
			}
			//successfully logging in the user if all constraints satisfy
			else if(!currentUsers.containsValue(userName) && !currentUsers.containsKey(userNumber)){
				if(password.equals("WWkcqZVZ")){
					try {
						SendMessage.sendMessage("OKAY "+userNumber, new DataOutputStream(connection.getOutputStream()), false);
						Commands.sendARRVtoOtherServer_newlyConnected(userNumber, userName);
						Commands.SendARRVtoUser_newlyConnected(connection);
						//System.out.println("User "+userName+" logged in! ");
						for (Entry<String, Socket> entry : connectionOutputArray.entrySet()){ //sending ARRV (of this user) to all users currently logged in 
							SendMessage.sendMessage("ARRV "+userNumber+"\r\n"+ userName+"\r\nGroup6", entry.getValue().getOutputStream(), false);
						}
						
				        for (Entry<String, String> entryUsers : currentUsers.entrySet()){ //sending ARRV of all users (currently logged in) to the user who made login request
				        	SendMessage.sendMessage("ARRV "+entryUsers.getKey()+"\r\n"+ entryUsers.getValue()+"\r\nGroup6", connection.getOutputStream(), false);   	
				        }
				        currentUsers.put(userNumber, userName);
				        connectionOutputArray.put(userNumber, connection);
				        connectionToNameMapping.put(connection, userName);
				        connectionToUserNumberMapping.put(connection, userNumber);	
					} catch (IOException e) {
						// TODO Auto-generated catch block
					}
				}
				else{
					try {
						SendMessage.sendMessage("FAIL "+userNumber+"\r\nPASSWORD", connection.getOutputStream(), false); //sending PASSWORD failure if password incorrect 
					} catch (IOException e) {
						// TODO Auto-generated catch block
					}
				}
			}	
		}
		else{
			try {
				SendMessage.sendMessage("FAIL "+userNumber+"\r\n"+"NUMBER", connection.getOutputStream(), false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	public static void closeStuff(Socket connection) {
		if(connectionToUserNumberMapping.get(connection) != null){
			for (Entry<String, Socket> entry : connectionOutputArray.entrySet()) {
				if(!entry.getValue().equals(connection)){
					String userNumberLeft = connectionToUserNumberMapping.get(connection);
					try {
						SendMessage.sendMessage("LEFT "+userNumberLeft, entry.getValue().getOutputStream(), false); //sending LEFT command to all users currently logged in
					} catch (IOException e) {
						// TODO Auto-generated catch block
					} 
				}
			}
		}
		String userNameToRemove = connectionToNameMapping.get(connection);
		connectionToNameMapping.remove(connection);
		connectionArray.values().removeAll(Collections.singleton(connection));
		connectionOutputArray.values().removeAll(Collections.singleton(connection));
		currentUsers.values().removeAll(Collections.singleton(userNameToRemove));
	}
}
