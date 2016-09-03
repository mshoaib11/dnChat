import java.io.IOException;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;


public class ReadMessage {
	static boolean fin;
	static byte opcode;
	static byte[] payload;
	static int payloadLength;
	static ConcurrentHashMap<String, Integer> dataAndOpcode = new ConcurrentHashMap<String, Integer>();
	
 	public static ConcurrentHashMap<String, Integer> readMessage(byte raw[], int length, Socket connection) throws InterruptedException
    {
 	
 	try{
 		dataAndOpcode.clear();
        // easier to do this via ByteBuffer
        ByteBuffer buf = ByteBuffer.wrap(raw);
        
        while(true){

	        // Fin + RSV + OpCode byte
	        byte b = buf.get();
	
	        fin = ((b & 0x80) != 0);
	        opcode = (byte)(b & 0x0F);
	        
	        if(opcode == 8 && (ServerThread.clientConnectionArray.contains(connection) || ClientThread.serverConnectionArray.contains(connection))){
	        	Commands.serverDisconnetRecieved(connection);
	        	if(ServerThread.clientConnectionArray.contains(connection)){
	        		ServerThread.clientConnectionArray.remove(connection);
	        	}
	        	else if(ClientThread.serverConnectionArray.contains(connection)){
	        		ClientThread.serverConnectionArray.remove(connection);
	        	}
	        	connection.getInputStream().close();
	        	connection.getOutputStream().close();
	        	connection.close();
	        	break;
	        }
	                       
	        // Masked + Payload Length
	        b = buf.get();
	        boolean masked = ((b & 0x80) != 0);
	        payloadLength = (byte)(0x7F & b);
	        int byteCount = 0;
	        
	        if (payloadLength == 0x7F)
	        {
	            // 8 byte extended payload length
	            byteCount = 8;
	        }
	        else if (payloadLength == 0x7E)
	        {
	            // 2 bytes extended payload length
	            byteCount = 2;
	        }
	     
	        // Decode Payload Length
	        while (--byteCount > 0)
	        {
	            b = buf.get();
	            payloadLength |= (b & 0xFF) << (8 * byteCount);
	        }
	
	        // Add control frame payload length validation here
	
	        byte maskingKey[] = null;
	        if (masked)
	        {
	            // Masking Key
	            maskingKey = new byte[4];
	            buf.get(maskingKey,0,4);
	        }
	
	        // Add masked + maskingkey validation here
	
	        // Payload itself
	        payload = new byte[payloadLength];
	        buf.get(payload,0,payloadLength);
	
	        // Demask (if needed)
	        if (masked)
	        {
	            for (int i = 0; i < payload.length; i++)
	            {
	                payload[i] ^= maskingKey[i % 4];
	            }
	        }
	        
	        if(opcode == 9 && (ServerThread.clientConnectionArray.contains(connection) || ClientThread.serverConnectionArray.contains(connection))){
	        	Commands.pingRecievedFromOtherServer(connection, payload); 
	        }
	        
	        if(opcode == 10 && (ServerThread.clientConnectionArray.contains(connection) || ClientThread.serverConnectionArray.contains(connection))){
	        	Commands.pongRecievedFromOtherServer(connection, payload); 
	        }
	                
	        dataAndOpcode.put(new String(payload), (int) opcode);
	        
	        if(buf.position() == buf.limit()){ //checking if all the data has been read from the input stream
	        	break;
	        	}
        }
 	}
 	catch(IOException | BufferUnderflowException e){
 		
 	}
	return dataAndOpcode;
    }
}
