import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HandShake {

	public static void sendHandshakeResponse(OutputStream output, InputStream input) throws IOException {
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] readBuffer=new byte[1024];
		int length = input.read(readBuffer);
		byteArrayOutputStream.write(readBuffer, 0, length);
		while (input.available() > 0) {
		    length = input.read(readBuffer);
		    byteArrayOutputStream.write(readBuffer, 0, length);
		}
		byte[] buffer = byteArrayOutputStream.toByteArray();
		String request = new String(buffer);
		byteArrayOutputStream.flush();
		String pattern = "Sec-WebSocket-Key: (.+)";
		Pattern r = Pattern.compile(pattern);
	    Matcher m = r.matcher(request);
	    if (m.find()) {
	    	String SecWebSocketKey = m.group(0).split(" ")[1].trim();
			String SecWebSocketKey_Concat = SecWebSocketKey.concat("258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
			byte[] SHA1key = SHA1(SecWebSocketKey_Concat);
			String SecWebSocketAcceptKey = new String(Base64.getEncoder().encode(SHA1key)); //base64 encoding of the hash value
			String response = "HTTP/1.1 101 Switching Protocols\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+SecWebSocketAcceptKey+"\r\nUpgrade: websocket\r\n\r\n";
			byte[] responseBytes = response.getBytes("UTF-8");
			output.write(responseBytes);
			output.flush();
	    }		
	}

	private static byte[] SHA1(String x) {
		java.security.MessageDigest d = null;
	    try {
			d = java.security.MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	    d.reset();
	    d.update(x.getBytes());
	    return d.digest();
	}
}

