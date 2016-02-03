package edu.utdallas.aos.prj1;

//class for collect results.
public class Collector {
	public int totalSentNum = 100;
	public double averageCommLatency = 0;
	public double standardDiviation = 0;
	public int bufferedMsgNum = 0;
	public double maxTimeBuffered = 0;
	public double minTimeBuffered = 0;
	public int maxBufferedMsgNum = 0;
	public double bufferedMsgNumPerS = 0;
	public int notBufferMsg = 0;
	public String toString(){
		return "totalSentNum: " + totalSentNum + "\n" +
				"averageCommLatency: " + averageCommLatency + "\n" +
				"standardDiviation: " + standardDiviation + "\n" +
				"bufferedMsgNum: " + bufferedMsgNum + "\n" +
				"maxTimeBuffered: " + maxTimeBuffered + "\n" +
				"minTimeBuffered: " + minTimeBuffered + "\n" +
				"maxBufferedMsgNum: " + maxBufferedMsgNum + "\n" +
				"bufferedMsgNumPerS: " + bufferedMsgNumPerS + "\n" +
				"notBufferMsg: " + notBufferMsg + "\n";
	}
}
