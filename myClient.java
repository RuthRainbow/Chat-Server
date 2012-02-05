import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;


public class myClient {
	private static int port = 3333;
	private static String host = "localhost";
	
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

		
		Thread clientThread = new Thread(new Runnable() {
			
			private PrintWriter output = null;
			private BufferedReader input = null;
			private BufferedReader stdIn = null;
			private Socket clientSock = finalSock;
			
			ArrayList<String> userInput = new ArrayList<String>();
			int clock = 0;
			
			@Override
			public void run() {
				
				//set up the communication streams with the server
				try {
					input = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
					output = new PrintWriter(clientSock.getOutputStream(), true);
					stdIn = new BufferedReader(new InputStreamReader(System.in));
				} catch (IOException streamerr) {
					System.err.println("error creating streams to/from server");
					System.err.println(streamerr);
					System.exit(1);
				} 
				
				//to hold user input
				String usermsg = null;
				//to hold server input
				String servermsg = null;
				
				//I want to: read user input, queue this, send to server, receive from server
				//but i can do reading the input and reading from the socket at the same time
				while (true) {
					
					try {
						//read from stdin, add to list when not null input
						if((usermsg = stdIn.readLine())!=null) {
							System.out.println("std is not null :) " + usermsg);
							userInput.add(usermsg);
						}
					}
					catch (IOException stdInerr) {
						System.err.println("Unable to read user input");
						System.err.println(stdInerr);
						System.exit(1);
					}
					
					//PROBLEM: DOESN@T GET HERE UNTIL CLIENT HAS QUIT
					System.out.println("now looking at the clock");
					if(clock < 250){
						//write to the socket whilst the list has stuff to write
						while((userInput.isEmpty())==false) {
							System.out.println("user input was not null :) " + userInput);
							output.println(userInput.remove(0));
						}
					}
					else {
						//read from the socket while the input isn't null print the message
						try {
							while((servermsg = input.readLine())!= null) {
								System.out.println("server input was not null :) " + servermsg);
								System.out.println(servermsg);
							}
						} catch (IOException outputerr) {
							System.err.println("Unable to read server input");
							System.err.println(outputerr);
							System.exit(1);
						}
					}
					
					if(clock < 300)
						clock++;
					else clock = 0;
					System.out.println(clock);
				}        
				
			}
			
		});
		
		clientThread.start();
	}
}