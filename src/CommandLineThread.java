import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class CommandLineThread implements Runnable {
	static Map<String, Integer> hostToPortMapping = new HashMap<String, Integer>(); 
	Socket serverConnection;
	String input;
	boolean connected;

	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			do{
				System.out.println("Type exit to shutdown the server OR Type connect <host name || IPv4 address> <port>(optional) to connect to another server: ");
				input = reader.readLine();
				int args = input.split(" ").length;
				if(!input.equals("exit")){
					if(args<2 || args>3){
						System.out.println("Incorrect number of arguments");
						continue;
					}
					else{
						if(args==2){
							String command=input.split(" ")[0].trim();
							String IP_or_HOST = input.split(" ")[1].trim();
							if(command.equals("connect") && (validateIP(IP_or_HOST) || validateHost(IP_or_HOST))){
								int PORT = 42015;
								try{
									if(hostToPortMapping.containsKey(IP_or_HOST) && hostToPortMapping.containsValue(PORT)){
										System.out.println("Already connected.");
									}
									else{
									 serverConnection = new Socket(IP_or_HOST, PORT);
									 String response = sendHandShakeRequest(serverConnection, IP_or_HOST, PORT);
									 if(response.contains("101")){
										ClientThread.serverConnectionArray.add(serverConnection);
										System.out.println("Connection successful.");
										hostToPortMapping.put(IP_or_HOST, PORT);
										new Thread(new ClientThread(serverConnection)).start();
									}
									else if(response.contains("400")){
										System.out.println("Can't connect");
									}
								} 
							}
						catch (java.io.IOException e) {
						         System.out.println("Can't connect.");
						         //System.out.println(e);
						    }
						}
						else{
							System.out.println("Error in arguments. Try again.");
							continue;
						}
					}
					
					else if(args==3){ 
						String command = input.split(" ")[0].trim();
						String IP_or_HOST = input.split(" ")[1].trim();
						String port = input.split(" ")[2].trim();
						if(command.equals("connect") && (validateIP(IP_or_HOST) || validateHost(IP_or_HOST)) && validatePort(port)){
							int PORT = Integer.parseInt(port);
							try{
								if(hostToPortMapping.containsKey(IP_or_HOST) && hostToPortMapping.containsValue(PORT)){
								System.out.println("Already connected.");
								}
								else{
								 serverConnection = new Socket(IP_or_HOST, PORT);
								 String response = sendHandShakeRequest(serverConnection, IP_or_HOST, PORT);
								 if(response.contains("101")){
									ClientThread.serverConnectionArray.add(serverConnection);
									System.out.println("Connection successful.");
									hostToPortMapping.put(IP_or_HOST, PORT);
									new Thread(new ClientThread(serverConnection)).start();
								}
								else if(response.contains("400")){
									System.out.println("Can't connect");
								}
							} 
						}
						catch (java.io.IOException e) {
						         System.out.println("Can't connect.");
						         //System.out.println(e);
						      }
						}
					}
					else{
						System.out.println("Error in arguments. Try again");
						continue;
						}
					}
				}
			}while(!input.equals("exit"));
				Server.shutdown();
				System.exit(0);
		}catch (IOException | InterruptedException e) {
		
	}
}

	private String sendHandShakeRequest(Socket serverConnection, String IP_or_HOST, int PORT) throws IOException, InterruptedException {
		String request = "GET / HTTP/1.1\r\n"+
		"Host: "+ IP_or_HOST + ":" + PORT + "\r\n"+
		"Connection: Upgrade\r\n"+
		"Pragma: no-cache\r\n"+
		"Cache-Control: no-cache\r\n" +
		"Upgrade: websocket\r\n" +
		"Origin: null\r\n" +
		"Sec-WebSocket-Version: 13\r\n" +
		"Accept-Encoding: gzip, deflate, sdch\r\n" +
		"Accept-Language: en-US,en;q=0.8,ru;q=0.6,de;q=0.4\r\n" +
		"Sec-WebSocket-Key: AQIDBAUGBwgJCgsMDQ4PEC==\r\n" +
		"Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits\r\n\r\n";
		byte[] requestBytes = request.getBytes("UTF-8");
		OutputStream output = serverConnection.getOutputStream();
		output.write(requestBytes);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] readBuffer=new byte[1024];
		InputStream input = serverConnection.getInputStream();
		int length = input.read(readBuffer);
		byteArrayOutputStream.write(readBuffer, 0, length);
		while (input.available() > 0) {
		    length = input.read(readBuffer);
		    byteArrayOutputStream.write(readBuffer, 0, length);
			}
				byte[] buffer = byteArrayOutputStream.toByteArray();
				byteArrayOutputStream.flush();
				String response = new String(buffer);
				return response;
	}
	
	private boolean validateIP(String ip) {
	    String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	    return ip.matches(PATTERN);
	}
	
	boolean validateHost(String host){
		String domainIdentifier = "((\\p{Alnum})([-]|(\\p{Alnum}))*(\\p{Alnum}))|(\\p{Alnum})";
		String domainNameRule = "("+ domainIdentifier + ")((\\.)(" + domainIdentifier + "))*";
		String oneAlpha = "(.)*((\\p{Alpha})|[-])(.)*";
		
		if((host == null) || (host.length()>63)) {
			System.out.println("Host not validated.");
			return false;
		}
			return host.matches(domainNameRule) && host.matches(oneAlpha);
		}
	
	private boolean validatePort(String port) {
		if(Integer.parseInt(port)>1024 && Integer.parseInt(port)<=65535){
			return true;
		}else{
			System.out.println("Port not validated.");
			return false;
		}
	}
}
