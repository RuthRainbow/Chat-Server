import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class myClient {
	private static int port = 3339;
	private static String host = "localhost";
	public static List<String> userInput = Collections.synchronizedList(new ArrayList<String>());
	
	public static void main (String[] args) throws IOException {
		Socket serverSock = null;
		
		//try to connect to the server specified on the port specified
		try {
			serverSock = new Socket(host, port);
		} catch (UnknownHostException hostnotfound) {
			System.err.println("Failed to find host");
			System.err.println(hostnotfound);
			System.exit(1);
		} catch (IOException IOerr) {
			System.err.println("I/O error when creating socket");
			System.err.println(IOerr);
			System.exit(1);
		}

		final Socket finalSock = serverSock;

		//thread for stdIn to be run by clients that have established a server connection
		Thread stdInThread = new Thread(new Runnable() {
			
			private BufferedReader stdIn = null;
			
			public void run() {
				stdIn = new BufferedReader(new InputStreamReader(System.in)); 
				
				//to hold user input
				String usermsg = null;
				
				while(true) {
					try {
						//read from stdin, add to list when not null input
						if((usermsg = stdIn.readLine())!=null) {
							synchronized (userInput) {
								userInput.add(usermsg);
							}
						}
					}
					catch (IOException stdInerr) {
						System.err.println("Unable to read user input");
						System.err.println(stdInerr);
						System.exit(1);
					}
				}
			}
		});
		
		//client thread for reading/writing to server
		Thread clientThread = new Thread(new Runnable() {
			
			private BufferedReader fromServer = null;
			private PrintWriter toServer = null;
			private Socket clientSock = finalSock;
			
			@Override
			public void run() {
				
				//set up the communication streams with the server
				try {
					fromServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
					toServer = new PrintWriter(clientSock.getOutputStream(), true);
					System.out.println("successfully made server streams");
				} catch (IOException streamerr) {
					System.err.println("error creating streams to/from server");
					System.err.println(streamerr);
					System.exit(1);
				} 
				
				//to hold server input
				String servermsg = null;
				int clock = 0;
				
				while (true) {
					//clock gives reading/writing to socket equal time slices
					if (clock < 250) {
						//write to the socket whilst the list has stuff to write
						synchronized(userInput) {
							while((userInput.isEmpty())==false) {
								toServer.println(userInput.remove(0));
							}			
						} 
					}
					else {
						//read from the socket, while the input isn't null print the message
						try {
							if(fromServer.ready() == true) {
								servermsg = fromServer.readLine();
								System.out.println(servermsg);
							}
							
						} catch (IOException outputerr) {
							System.err.println("Unable to read server input");
							System.err.println(outputerr);
							System.exit(1);
						} 
						
					}     
					//increment the clock up to 500 then reset it
					if (clock < 500) {
						clock++;
					} else {
						clock = 0;
					}
				}
				
			}
			
		});
		
		stdInThread.start();
		clientThread.start();
		
		//thread for handling ctrl+c
		Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	try {
					finalSock.close();
				} catch (IOException e) {
					System.err.println("Failed to close socket");
					System.err.println(e);
					System.exit(1);
				}
                System.out.println("Exiting client, shutting ports");
            }
        });
	}
}