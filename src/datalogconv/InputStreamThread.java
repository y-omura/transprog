package datalogconv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InputStreamThread extends Thread{
	private InputStream is;
	public InputStreamThread(InputStream is) {
		this.is = is;
	}
	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
	
			while(true){
				String line = br.readLine();
				if(line == null){
					break;
				}
				System.out.println(line);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
