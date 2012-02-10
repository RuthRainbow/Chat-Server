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
	private static int port = 3333;
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
							//System.out.println("std is not null :) " + usermsg);
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
				
				//I want to: read user input, queue this, send to server, receive from server
				//but i can do reading the input and reading from the socket at the same time
				while (true) {
					if (clock < 250) {
						//write to the socket whilst the list has stuff to write
						synchronized(userInput) {
							//currently does this until any user input
							while((userInput.isEmpty())==false) {
								//System.out.println("user input was not null :) " + userInput);
								toServer.println(userInput.remove(0));
							}			
						} 
					}
					else {
						//read from the socket while the input isn't null print the message
						try {
							//do i need this is another thread with interrupts? It would work...
							if(fromServer.ready() == true) {
								servermsg = fromServer.readLine();
								//System.out.println("server input was not null :) " + servermsg);
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

	}
}