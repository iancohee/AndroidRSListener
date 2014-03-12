import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Listener {
	int port;
	SSLServerSocket listeningSocket;
	SSLContext sslContext;
	DataOutputStream logOut;
	
	public Listener(String keyStorePath, int port) throws Exception {
		this.port = port;

		// We need to build the SSL Context before
		// we create the socket
		FileInputStream keyStream = new FileInputStream(keyStorePath);
		KeyStore keyStore = KeyStore.getInstance("BKS");

		// Get the password, either in Eclipse (bleh),
		// or via command line :)
		char[] pass = null;
		if(System.console() != null)
			pass = System.console().readPassword("[>] Enter password for keystore '"+keyStorePath+"': ");
		else {
			System.out.print("[>] Enter password for keystore: '"+keyStorePath+"': ");
			pass = new BufferedReader(new InputStreamReader(System.in)).readLine().toCharArray();
		}
		
		// Load the keystore and
		// zero the password in memory
		keyStore.load(keyStream, pass);
		Arrays.fill(pass, '0');
		keyStream.close();

		// This is used to create the SSL Context
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);

		// Create SSL Context
		sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tmf.getTrustManagers(), null);
		SSLServerSocketFactory sslsf = sslContext.getServerSocketFactory();
		listeningSocket = (SSLServerSocket) sslsf.createServerSocket(this.port);

		// Enable cipher suites
		String[] supported = listeningSocket.getSupportedCipherSuites();
		listeningSocket.setEnabledCipherSuites(supported);
	}

	public void runListener() throws Exception {
		System.out.println("[>] Server listening on port: "+port);

		// Setup I/O
		Socket client = listeningSocket.accept();
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		IOThread t1 = new IOThread(client.getInputStream(), System.out);
		IOThread t2 = new IOThread(System.in, client.getOutputStream());
		t1.start();
		t2.start();

		// Set up logging
		String clientStr = client.getInetAddress().toString().replace("/", "");
		System.out.println("[>] New Connection: "+clientStr);
		logOut = new DataOutputStream(new FileOutputStream(clientStr.replace(".", "-")+".out"));
		
		// Listener loop
		while(true) {
			while(!t1.running || !t2.running)
				Thread.sleep(100);
			while(t1.running || t2.running) {
				Thread.sleep(100);
				Thread.yield();
			}
			
			String line = buffReader.readLine();
			System.out.println(line);
			logOut.writeBytes(line+"\n");		
		}
	}

	// ---------------------------------------------- //
	// Subclass: Used to duplex socket IO/Process IO  //
	// ---------------------------------------------- //
	private class IOThread extends Thread {

		private InputStream input;
		private OutputStream output;
		boolean running;

		public IOThread(InputStream i, OutputStream o) {
			running = false;
			input = i;
			output = o;
		}

		@Override 
		public void run() {
			running = true;
			try {
				byte buff[] = new byte[8192];
				int count = input.read(buff);
				while(count > 0) {
					output.write(buff, 0, count);
					output.flush();
					count = input.read(buff);
				}

			} catch(Exception e) {
				e.printStackTrace();
			}

			running = false;
		}
	} 

	// Entry point for program
	public static void main(String[] args) {

		// Check for args
		if(args.length < 2) {
			System.err.println("Usage: java -classpath /path/to/bcprov-jdk15on-146.jar Listener </path/to/server.keystore> <port>");
			System.exit(1);
		}

		try {
			Listener listener = new Listener(args[0], Integer.parseInt(args[1]));
			listener.runListener();
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Failure. Goodbye.");
			System.exit(1);;
		}
	}
}
