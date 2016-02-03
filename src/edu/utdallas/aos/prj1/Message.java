package edu.utdallas.aos.prj1;

import java.util.Date;
import java.io.*;

//message class. Message objects contain timestamp and causal dependency info.
public class Message implements Serializable {
	int[][] sent = new int[10][10];
	String msg;
	Date timestamp;
	int from;
	int to;

	public Message(int from, int to) {
		this.msg = "I gonna go HELL for this project!";
		this.from = from;
		this.to = to;
	}
	public Message(Message msg, int from, int to){
		this.sent = msg.sent;
		this.msg = msg.msg;
		this.timestamp = msg.timestamp;
		this.from = from;
		this.to = to;
	}

}
