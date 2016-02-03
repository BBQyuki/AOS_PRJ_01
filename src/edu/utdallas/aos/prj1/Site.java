//Every site has one "site" process.
//The site process control flow has four main step:
//The 1st step: initiation: process will new two thread named InitConnector and InitListener.
//These two thread will build a connection network map 
//that every pair of two sites is connected by one socket.
//There is a Receiver thread and a Sender thread in each site. The Receiver keeps looping and 
//and receive message from all socket.The Sender checks the "ready to be delivered" FIFO buffer, drop and send the first msg.
//
//The 2ed step: sending and receiving message. Sender threads and the Receiver thread
//will firstly check the causal ordering condition and then send or receive message.
//
//The 3rd step: termination. Termination condition will be detected in 2ed step. 
//If condition reached, Sender and Receiver threads will be interrupted by Thread.interrupt().
//
//The 4th step: collection result date. Some parameters are monitered in 2ed step. 
//In the last step, process will out put these data results.

package edu.utdallas.aos.prj1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.Date;

public class Site {
	private static int ID = -1;// site id
	private static int sentDone = 0;// This variable is a flag and only
									// meaningful for site0. Site++ for two
									// case: When other sites sent 100 messages,
									// they will send a token to site0, or site0
									// sent 100 messages.
	private LinkedList<Message> buffer = new LinkedList<Message>();// If a
																	// message
																	// is not
																	// causal
																	// ordering,
																	// it will
																	// be
																	// buffered
																	// in a FIFO
																	// link
																	// list.
	int[] rec = new int[10]; // causal dependency info
	int[][] sent = new int[10][10];// causal dependency info
	protected Socket[] sockets = new Socket[10];// Site
												// should
												// records
												// other
												// sites'
												// connection
												// sockets.
	static private int countSentMsg = 0;// count the number of message sent.

	// all sites' address
	protected static String[] ADRESS = new String[] { "dc20.utdallas.edu 23333", "dc21.utdallas.edu 23333",
			"dc22.utdallas.edu 23333", "dc23.utdallas.edu 23333", "dc24.utdallas.edu 23333", "dc25.utdallas.edu 23333",
			"dc26.utdallas.edu 23333", "dc27.utdallas.edu 23333", "dc28.utdallas.edu 23333",
			"dc29.utdallas.edu 23333" };
	protected static String[] ADRESS1 = new String[] { "127.0.0.1 23330", "127.0.0.1 23331", "127.0.0.1 23332",
			"127.0.0.1 23333", "127.0.0.1 23334", "127.0.0.1 23335", "127.0.0.1 23336", "127.0.0.1 23337",
			"127.0.0.1 23338", "127.0.0.1 23339" };

	// this array is used in initial step. Site must hold the knowledge, using
	// this array, that whether it has connected to all other site or not.
	protected int[] checkExistance = new int[10];

	// calculate and store data results.
	public Collector outputResults = new Collector();

	// constructor
	public Site(int id) {
		ID = id;
		// add initial message
		if (ID == 0)
			for (int i = 1; i < 10; i++)
				buffer.add(new Message(0, i));
	}

	// multiple threads can use sockets array list concurrently. A mutex method
	// for threads updating list.
	protected synchronized void addSocket(int i, Socket s) {
		sockets[i] = s;
	}

	protected synchronized void setExistance(int i) {
		checkExistance[i] = 1;
	}

	protected synchronized boolean checkSocketList() {
		for (int i = 0; i < 10; i++) {
			if (i != ID && (checkExistance[i] == 0)) {
				return false;
			}
		}
		return true;
	}

	// one of two initial thread. It passively keeps listening for other site
	// initiate connections.
	private class InitListener extends Thread {
		public void run() {
			System.out.println("InitListener: Start listening\n");
			while (!checkSocketList()) {
				// System.out.println("InitListener: listening\n");
				try {
					String[] tempAdress = ADRESS[ID].split(" ");
					ServerSocket listener = new ServerSocket(Integer.parseInt(tempAdress[1]));
					Socket socket = listener.accept();
					// System.out.println("1");
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					System.out.println("1");
					try {
						int i = Integer.parseInt(reader.readLine());// after
																	// initialization,
																	// the first
																	// msg must
																	// be
																	// receiver's
																	// ID
						System.out.println(
								"InitListener: Site #" + ID + " received connection request from site #" + i + ".\n");
						addSocket(i, socket);// synchronized
						setExistance(i);// synchronized
					} catch (Exception e) {
						e.getStackTrace();
					}
				} catch (Exception e) {

				}
			}
			System.out.println("InitListener: End listening\n");
		}
	}

	// the other initial thread. It actively connects to other sites.
	// to avoid duplicated connections:
	// site0 connects to site 1-9;
	// site1 connects to site 2-9;
	// site2 connects to site 3-9;
	// site3 connects to site 4-9;
	// site4 connects to site 5-9;
	// site5 connects to site 6-9;
	// site6 connects to site 7-9;
	// site7 connects to site 8-9;
	// site8 connects to site 9;
	// site9 connects to no site.
	private class InitConnector extends Thread {
		public void run() {
			try {
				for (int i = ID + 1; i < 10; i++) {
					System.out.println("InitConnector: Site #" + ID + " tries to connect site #" + i + ".\n");
					String[] tempAdrs = ADRESS[i].split(" ");
					String ipaddress = tempAdrs[0];
					int port = Integer.parseInt(tempAdrs[1]);
					Socket socket = new Socket(ipaddress, port);// blocked??
					System.out.println("InitConnector: Site #" + ID + " has connected to site #" + i + ".\n");
					Writer writer = new OutputStreamWriter(socket.getOutputStream());
					writer.write(ID + "\n");
					writer.flush();
					addSocket(i, socket);// synchronized
					setExistance(i);// synchronized
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.out.println("InitConnector:  End connecting.\n");
				interrupt();
			}
		}
	}

	// thread for sending message(after initiation)
	protected class Sender extends Thread {

		private ObjectOutputStream out;
		private Random rdm = new Random(233);

		public void run() {
			try {
				while (true) {
					if (countSentMsg == 100) {
						if (ID != 0) {
							ObjectOutputStream w = new ObjectOutputStream(sockets[0].getOutputStream());
							w.writeObject(new Integer(0));
						} else
							sentDone++;
						System.out.println(
								"site " + ID + " has sent 100 messages, and it will be interrupted right now!\n");
						interrupt();
					} // check if send thread complete

					// first, send all messages in buffer(buffer is a FIFO
					// buffer)
					sleep(3000);

					while (!buffer.isEmpty()) {
						Message tempMsg = buffer.removeFirst();

						// causal ordering algorithm implementation
						out = new ObjectOutputStream(sockets[tempMsg.to].getOutputStream());
						sleep(rdm.nextInt(81) + 20);
						tempMsg.timestamp = new Date();
						sent[ID][tempMsg.to]++;
						tempMsg.sent = sent;
						out.writeObject(tempMsg);
						System.out.println("Site#" + ID + ": Sent a msg to site#" + tempMsg.to + ".\n");
						countSentMsg++;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// do nothing
			}
		}
	}

	// thread for receiving message(after initiation)
	protected class Receiver extends Thread {
		private ObjectInputStream in;
		private LinkedList<Message> buffer1 = new LinkedList<Message>();
		private Random rdm1 = new Random(233);

		public void run() {
			while (true) {
				try {
					if (ID == 0 && sentDone == 10) {
						System.out.println(
								"Site 0: All sites have sent 100 messages. Receiver thread will be interrupted right now!\n");
						System.out.println(outputResults);// output data
															// results.
						interrupt();
					}

					for (int i = 0; i != ID && i < 10; i++) {
						in = new ObjectInputStream(sockets[i].getInputStream());
						Object temp = in.readObject();
						if (((Integer) temp).intValue() == 0) // if received a
																// flag msg
																// which means
																// other one
																// site has sent
																// 100 msgs.
							sentDone++;
						else
							buffer1.addLast((Message) temp);
					}

					if (buffer1.isEmpty())
						continue;
					Message msgTemp = buffer1.removeFirst();
					sleep(rdm1.nextInt(151) + 50); // requirement t

					// causal ordering algorithm implementation
					int[][] sentTemp = msgTemp.sent;
					if ((rec[msgTemp.from] + 1) == sentTemp[msgTemp.from][ID])
						for (int k = 0; k < 10 && k != msgTemp.from; k++)
							if (rec[k] >= sentTemp[k][ID]) {

								for (int i = 0; i != ID && i < 10; i++)// broadcast
									buffer.addLast(new Message(msgTemp, ID, i));

								/*
								 * //multi-cast int rdmAmount =
								 * rdm1.nextInt(10)+1; int[] checkDupl = new
								 * int[10]; int tmpCounter = 0; while(tmpCounter
								 * <= rdmAmount){ int ttemp=rdm1.nextInt(10);
								 * if(ttemp != ID&& checkDupl[ttemp]!=1){
								 * tmpCounter++; checkDupl[ttemp] = 1;
								 * buffer.addLast(new Message(msgTemp, ID,
								 * ttemp)); } }
								 */
								System.out.println("Site#" + ID + ": A msg is ready to be delivered.\n");
								rec[msgTemp.from]++;
								for (int i = 0; i < 10; i++)
									for (int j = 0; j < 10; j++)
										sent[i][j] = (sent[i][j] < sentTemp[i][j]) ? sentTemp[i][j] : sent[i][j];
							} else
								buffer1.addLast(msgTemp);
					else
						buffer1.addLast(msgTemp);
				} catch (Exception e) {

				}

			}
		}

	}

	// this function is the entry of the whole "site" class control flow.
	public void startSite() throws InterruptedException {
		Thread t1 = new InitListener();
		Scanner sc = new Scanner(System.in);
		Thread t2 = new InitConnector();
		t1.start();
		while (sc.nextInt() != 1) {
		}
		t2.start();
		while (!t1.isInterrupted()) {
		}
		System.out.println("Site#" + ID + ": End initialization.\n");
		new Sender().start();
		new Receiver().start();
		System.out.println(outputResults);
	}
}
