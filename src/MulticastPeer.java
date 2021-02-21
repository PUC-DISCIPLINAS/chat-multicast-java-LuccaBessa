import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MulticastPeer {
	public static String serverName = "localhost";
	public static int serverPort = 6789;
	static String userName;
	static boolean finished;

	public static void clearScreen() {
		System.out.print("\033[H\033[2J");
		System.out.flush();
	}

	public static String sendMessage(String message) {
		try {
			DatagramSocket socket = new DatagramSocket();
			InetAddress host = InetAddress.getByName(serverName);

			DatagramPacket request = new DatagramPacket(message.getBytes(), message.length(), host, serverPort);
			socket.send(request);

			byte[] buffer = new byte[100000];

			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
			socket.receive(reply);
			socket.close();
			message = new String(reply.getData()).trim();

			return message;
		}catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
			return "Error";
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
			return "Error";
		}
	}

	public static String enterRoom(String roomName, String userName) {
		String message = "2"+ roomName + "---" + userName;
		return sendMessage(message);
	}

	public static String createRoom(String roomName, String userName) {
		String message = "3"+ roomName + "---" + userName;
		return sendMessage(message);
	}

	public static String listRoomMembers(String roomName) {
		System.out.println(roomName);
		String message = "4"+ roomName;
		return sendMessage(message);
	}

	public static void leaveRoom(String roomName, String userName) {
		String message = "5"+ roomName + "---" + userName;
		sendMessage(message);
	}

	public static void main(String[] args) {
		MulticastSocket socket = null;
		Scanner scanner = new Scanner(System.in);
		String answer;

		try {
			String op;
			String roomName;

			while(true){
				System.out.println("----- Options: -----\n\n1- List Chat Rooms\n2- Enter Chat Room\n3- Create Chat Room\n4- List Room Members");
				op = scanner.nextLine();

				if(op.compareTo("1") == 0) {
					clearScreen();
					System.out.println("---- Rooms: ----\n\n" + sendMessage("1") + "\n");
				}else if(op.compareTo("2") == 0) {
					System.out.println("---- Enter Room -----\n");
					System.out.print("Enter Chat Room name: ");
					roomName = scanner.nextLine();
					System.out.print("\nEnter your username: ");
					userName = scanner.nextLine();
					answer = enterRoom(roomName, userName);

					if (answer.compareTo("No rooms") == 0) {
						clearScreen();
						System.out.println("No rooms\n");
					}else if(answer.compareTo("No room found with that name\n") == 0){
						clearScreen();
						System.out.println("No room found\n");
					}else if(answer.compareTo("User already on Chat Room") == 0){
						clearScreen();
						System.out.println("User already on Chat Room\n");
					}else {
						break;
					}
				}else if(op.compareTo("3") == 0) {
					clearScreen();
					System.out.println("---- Create Room -----\n");
					System.out.print("Enter Chat Room name: ");
					roomName = scanner.nextLine();
					System.out.print("\nEnter your username: ");
					userName = scanner.nextLine();
					answer = createRoom(roomName, userName);

					if (answer.compareTo("Maximum chat room limit") == 0) {
						clearScreen();
						System.out.println("Maximum chat room limit\n");
					}else if(answer.compareTo("Room already created\n") == 0){
						clearScreen();
						System.out.println("Room already created\n");
					}else {
						break;
					}
				}else if(op.compareTo("4") == 0) {
					System.out.println("---- List Room Members -----\n");
					System.out.print("Enter Chat Room name: ");
					roomName = scanner.nextLine();
					answer = listRoomMembers(roomName);

					if (answer.compareTo("No rooms") == 0) {
						clearScreen();
						System.out.println("No room found with that name\n");
					} else {
						clearScreen();
						System.out.println("---- " + roomName +" Members: ----\n\n" + answer + "\n");
					}
				}
			}

			clearScreen();

			InetAddress groupIp = InetAddress.getByName(answer);
			socket = new MulticastSocket(6790);

			// Since we are deploying
			socket.setTimeToLive(0);
			//this on localhost only (For a subnet set it as 1)

			socket.joinGroup(groupIp);
			Thread t = new Thread(new ReadThread(socket,groupIp, 6790));

			// Spawn a thread for reading messages
			t.start();

			// sent to the current group
			System.out.println("------ "+ roomName +" (Type /exit to Exit) ------\n");
			while(true) {
				String message;
				message = scanner.nextLine();
				if(message.equalsIgnoreCase("/exit")) {
					finished = true;
					leaveRoom(roomName, userName);
					socket.leaveGroup(groupIp);
					socket.close();
					break;
				}
				message = userName + ": " + message;
				byte[] buffer = message.getBytes();
				DatagramPacket datagram = new
						DatagramPacket(buffer,buffer.length,groupIp, 6790);
				socket.send(datagram);
			}

			while (true) {
				byte[] buffer = new byte[100000];
				DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
				socket.receive(messageIn);
				System.out.println(new String(messageIn.getData()).trim());

				System.out.println("Type a message (/exit to leave):");
				String messageString = scanner.nextLine();

				if(messageString.compareTo("/exit") == 0) {
					socket.leaveGroup(groupIp);
					leaveRoom(roomName, userName);
					break;
				}

				messageString = userName.concat(": ").concat(messageString);

				byte[] message = messageString.getBytes(StandardCharsets.UTF_8);
				DatagramPacket messageOut = new DatagramPacket(message, message.length, groupIp, 6789);
				socket.send(messageOut);
			}
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (socket != null)
				socket.close();
		}
	}
}

class ReadThread implements Runnable {
	private final MulticastSocket socket;
	private final InetAddress group;
	private final int port;
	private static final int MAX_LEN = 1000;
	ReadThread(MulticastSocket socket,InetAddress group,int port)
	{
		this.socket = socket;
		this.group = group;
		this.port = port;
	}

	@Override
	public void run() {
		while(!MulticastPeer.finished) {
			byte[] buffer = new byte[ReadThread.MAX_LEN];
			DatagramPacket datagram = new
					DatagramPacket(buffer,buffer.length,group,port);
			String message;
			try {
				socket.receive(datagram);
				message = new
						String(buffer,0,datagram.getLength(), StandardCharsets.UTF_8);
				if(!message.startsWith(MulticastPeer.userName))
					System.out.println(message);
			} catch(IOException e) {
				System.out.println("Leaving Chat...");
			}
		}
	}
}