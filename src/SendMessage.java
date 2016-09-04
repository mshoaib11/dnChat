import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class SendMessage {

	static void sendMessage(String msg, OutputStream output, boolean masking){ //frame encoding function
		byte[] message = msg.getBytes();
	    int indexDataStart  = 0; //index of start of data
	    byte[] frame = new byte[10];
	    frame[0] = (byte) 129; //the first byte is for type of data i.e. 129 for a text frame
	    if(message.length <= 125){ //no need for additional bytes
	        frame[1] = (byte) message.length; //second byte is for length of data
	        indexDataStart = 2;
	    }
	    else if(message.length >= 126 && message.length <= 65535){ //need two additional bytes
	    	//splicing the length of message into individual bytes... need to bit-shift to the right (with an amount of eight bits)
	    	//only retain the last eight bits by doing AND 255
	        frame[1] = (byte) 126;
	        int messageLength = message.length;
	        frame[2] = (byte)((messageLength >> 8 ) & (byte)255);
	        frame[3] = (byte)(messageLength & (byte)255); 
	        indexDataStart = 4;
	    }
	    else{
	        frame[1] = (byte) 127;
	        int messageLength = message.length;
	        frame[2] = (byte)((messageLength >> 56 ) & (byte)255);
	        frame[3] = (byte)((messageLength >> 48 ) & (byte)255);
	        frame[4] = (byte)((messageLength >> 40 ) & (byte)255);
	        frame[5] = (byte)((messageLength >> 32 ) & (byte)255);
	        frame[6] = (byte)((messageLength >> 24 ) & (byte)255);
	        frame[7] = (byte)((messageLength >> 16 ) & (byte)255);
	        frame[8] = (byte)((messageLength >> 8 ) & (byte)255);
	        frame[9] = (byte)(messageLength & (byte)255);
	        indexDataStart = 10;
	    }
	    
	    if(masking){
            if ((frame[1] & 128) == 0) {
                frame[1] |= 128;
            }
            Random rand = new Random();
            int maskValue = rand.nextInt(Integer.MAX_VALUE);
            Byte[] mask = new Byte[4];
            int splitter = 0xFF;
            for (int i = 0; i < 4; i++) {
                mask[3 - i] = (byte)((maskValue & splitter) >> (i * 8));
                splitter <<= 8;
            }
            for(int i= indexDataStart; i<indexDataStart+4;i++){
                frame[i] = mask[i-indexDataStart];
            }
            indexDataStart += 4;
            for (int i = 0, j = 0; i < message.length; i++, j++) {
            	message[i] = (byte) (message[i] ^ mask[j % 4]);
            }
	    }
	    //putting bytes of message at the right index
	    int byteLength = indexDataStart + message.length;
	    byte[] bytesToSend = new byte[byteLength];
	    int byteIndex = 0;
	    for(int i=0; i<indexDataStart;i++){
	    	bytesToSend[byteIndex] = frame[i];
	    	byteIndex++;
	    }
	    for(int i=0; i<message.length;i++){
	    	bytesToSend[byteIndex] = (byte) message[i];
	    	byteIndex++;
	    }
        try {
			output.write(bytesToSend);
			output.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}   
    }
}


