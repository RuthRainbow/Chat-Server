
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.net.Socket;
import java.io.IOException;

class Protocol {
	private String nick;
	private String destNick;
	private Socket proSock;
	private String delims = "[ ]+";
	private String[] text = null;
	private String password;
	
	//enum to hold possible commands
	private enum commands {
		IDENTIFY, LOGOUT, MSG, NEW, NOOP, USER, QUIT, WHO, HELP
	}
	private commands command;
	
	//Storing current connections - nick and associated socket
	private static Hashtable<String, Socket> onlineNicks = new Hashtable<String, Socket>();
	//Storing all known nicks - nick, password and queue of messages waiting for user to come online (ie have an entry in the other hash table)
	private static Hashtable<String, PwdList> allNicks = new Hashtable<String, PwdList>();

	Protocol(Socket currSock) {
		this.nick = null;
		this.proSock = currSock;
		this.password = null;
	}
	
	public void process(String message) throws IOException {
			System.out.println("processing " + message);
			if (message == null) {
				write("no message");
			}
			String[] tokens = null;
			tokens = message.split(delims);
			this.command = commands.valueOf(tokens[1]);

			switch(this.command){ 
				case IDENTIFY: identify(); break;
				case LOGOUT: logout(); break;
				case MSG: sendMessage(tokens); break;
				case NEW: newUser(tokens); break;
				case NOOP: write("Acknowledged"); break;
				case QUIT: quit(); break;
				case USER: user(tokens); break;
				case WHO: whosOnline(); break;
				case HELP: help(); break;
				default: write("Invalid command. For a list of commands, type 'HELP'");
			}
	}
	
	private void identify() throws IOException {
		//Display the username currently online on this client
		if(onlineNicks.containsKey(this.nick)) {
			write(this.nick);
		}
		else write("Not currently signed in");
	}
	
	private void logout() throws IOException {
		//logout the current user (remove them from online nicks)
		if(this.nick != null) {
			onlineNicks.remove(this.nick);
			write("Signing out " + this.nick);
		}
		else write("No user currently signed in");
		this.nick = null;
		this.password = null;
	}
	
	private void sendMessage(String[] tokens) throws IOException {
		//send a message to the desintation nick: MSG <nick> <message>
		if(tokens.length > 2) { 
			destNick = tokens[1];
			//fill the text array with the message to be sent
			if(tokens.length > 302) {
				write("message too long");
			}
			else if((this.nick == null) || !onlineNicks.containsKey(this.nick)) {
				write("Please sign in to send a message");
			}
			else {
				text = new String[tokens.length-2];
				for (int i = 2; i < tokens.length; i++) {
					text[i-2] = tokens[i];
				}
				//look for dest nick in storage table
				if(!onlineNicks.containsKey(destNick)) {
					//add message to queue in allNicks 
					if(!allNicks.containsKey(destNick)) {
						write("Nick not found");
					}
					else {
						//COULD BE THIS BIT NOT WORKING
						//add message to list of user's offline messages
						List<String[]> msgs = allNicks.get(destNick).getList();
						msgs.add(text);
						allNicks.put(this.nick, new PwdList(allNicks.get(destNick).getPassword(), msgs));
						write("User offline, message sent");
					}
				}
				else { //send message from prosock -> destnick's sock
						Socket destSock = onlineNicks.get(destNick);
						destSock.getOutputStream().write((this.nick + ": " + arrayToString(text) + "\r\n").getBytes());
					}
			}
		}
		else write("usage: MSG <nick> <message>");
	}
	
	private void newUser(String[] tokens) throws IOException {
		//create a new user account with: tokens[1] = nick i want, tokens[2] = password
		if(tokens.length >= 3) {
			if(tokens[1]!=null && tokens[2]!= null) {
				if(allNicks.containsKey(tokens[1])) {
					write("nick already exists");
				}
				else {
					List<String[]> msgs = new ArrayList<String[]>();
					allNicks.put(tokens[1], new PwdList(tokens[2], msgs));
					write("created new nick");
				}
			}
		}
		else write("usage: NEW <nick> <password>");
	}
	
	private void quit() throws IOException {
		//remove the nick from the hash table if actually signed in
		if(this.nick != null) {
			onlineNicks.remove(this.nick);
			write("Signing out " + this.nick);
		}
		//close the conn
		try {
			write("Closing server connection");
			this.proSock.close();
		} catch (IOException close) {
			System.out.println("Failed to close the socket connection");
			System.out.println(close);
			System.exit(1);
		}
	}
	
	private void user(String[] tokens) throws IOException {
		//log in a user
		//move nick and socket to onlineNicks, if they exist in allNicks
		if(this.nick != null) {
			write("Already signed in, please sign out first");
		}
		else {
			if(tokens.length == 3) {
				this.nick = tokens[1];
				this.password = tokens[2];
				//check nick exists and password is valid
				if(!validateUser()){
					write("Please enter a valid username and password");
					this.nick=null;
					this.password=null;
				}
				else {
					onlineNicks.put(this.nick, this.proSock);
					sendList();
					write("Hello " + this.nick);
				}
			}
			else write("Usage: USER <nick> <password>");
		}
	}
	
	private boolean validateUser() {
		//check the nick exists
		if(!allNicks.containsKey(this.nick)) return false;
		//check the password is correct
		if(!allNicks.get(this.nick).getPassword().equals(this.password)) return false;
		else return true;
	}
	
	private void whosOnline() throws IOException {
		//displays the users currently online
		String online = onlineNicks.keySet().toString();
		if(online.length() > 0 ) {
			String trimmed = online.substring(1, online.length()-1);
			write("Online: " + trimmed);
		}
		else write("No users online");
	}
	
	private void help() throws IOException {
		//writes command info.
		write("IDENTIFY - displays which user account you are currently signed in as");
		write("LOGOUT - logs out the current user");
		write("MSG <nick> <msg> - sends a message to the nick provided");
		write("NEW <nick> <password> - creates a new user account");
		write("NOOP - check the server is responding correctly (used for debugging)");
		write("QUIT - closes connection to server");
		write("USER <nick> <password> - identfies and signs in a user account");
		write("WHO - displays who is currently online");
	}
	
	public void write(String msg) throws IOException {
		//method for writing to the user's socket
		if((proSock.isClosed()) == false) {
			this.proSock.getOutputStream().write((msg + "\r\n").getBytes());
		}
		else System.out.println("Error - trying to write to closed socket");

	}
	
	public void sendList() throws IOException {
		//write the user's list of offline messages
		List<String[]> msgs = (allNicks.get(this.nick)).getList();
		String listToString = "";
		for(int i = 0; i < msgs.size(); i++) {
			listToString = listToString + msgs.get(i);
		}
		if(msgs.size() > 0) write("Offline messages: " + listToString);
		msgs.clear();
		PwdList newPwdList = new PwdList(this.password, msgs);
		//update their msgs list to empty
		allNicks.put(this.nick, newPwdList);
	}
	
	public String arrayToString(Object[] arrMsg) {
		String strMsg = "";
		for(int i = 0; i<arrMsg.length; i++) {
			strMsg = strMsg + " " + arrMsg[i];
		}
		return strMsg;
	}
	
	private class PwdList {
		//structure to hold user's message list and their password for the allNicks table
		public List<String[]> userList;
		public String userPwd;
		
		PwdList(String p, List<String[]> l) {
			this.userList = l;
			this.userPwd = p;
		}
		
		public String getPassword() {
			return this.userPwd;
		}
		
		public List<String[]> getList() {
			return this.userList;
		}
		
	}
	
	
}