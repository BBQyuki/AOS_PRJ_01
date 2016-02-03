package edu.utdallas.aos.prj1;

public class Initiator {
	public static void main(String[] args){
		Site s = new Site(Integer.parseInt(args[0]));
		try {
			s.startSite();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
