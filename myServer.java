import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class myServer {
	private static int port = 3333;
	
	public static void main (String[] args) throws IOException {
		ServerSocket serverSock = null;
		
		//try to listen on the port specified above
		try {
			serverSock = new ServerSocket(port);
		} catch (IOException portfail) {
			System.err.println("Couldn't listen on port " + port);
			System.err.println(portfail);
			System.exit(1);
		}
		
		//try to accept a client connection
		Socket clientSock = null;
		while(true) {
			try {
				clientSock = serverSock.accept();
			} catch (IOException acceptfail) {
				System.err.println("Couldn't accept connection");
				System.err.println(acceptfail);
				System.exit(1);
			}
					System.out.println("accepted connection :)");
		final Socket acceptedSock = clientSock;
		
		//start a new thread to deal with this new client
		Thread serverThread = new Thread(new Runnable() {
			private Socket thisSock = acceptedSock;
			private BufferedReader input = null;
			private PrintWriter output = null;
			@Override
			public void run() {
				//code run by thread ie server code once one client sock accepted
				String msg;
				System.out.println("in new thread");
				Protocol pro = new Protocol(thisSock);
				
				//make a new input stream reader and output writer to client
				try {
					input = new BufferedReader(new InputStreamReader(thisSock.getInputStream()));
					output = new PrintWriter(thisSock.getOutputStream());
				} catch (IOException streamerr) {
					System.err.println("Failed to create input/output streams to client");
					System.err.println(streamerr);
					System.exit(1);
				}
				System.out.println("made the input/output streams");

				while(true) { //or do i need to do this until the next line is blank?
					try {
						if((msg = input.readLine()) != null) {
							//process the client's message according to the protocol
							System.out.println("trying to parse " + msg);
							String reply = pro.process(msg);
							output.println(reply);
							System.out.println(reply);
						}
					} catch (IOException readerr){
						System.err.println("Error when reading input stream");
						System.err.println(readerr);
						System.exit(1);
					}
				}
				
			}
		});
		
		serverThread.start();
		
		}
		
	}

}

