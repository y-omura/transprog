package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileUtil {
	public static String readFromFile(String filename){
		try(
				FileInputStream fis = new FileInputStream(filename);
				InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
				BufferedReader br = new BufferedReader(isr);
				){
			StringBuilder sb = new StringBuilder();
			
			while(true){
				String line = br.readLine();
				if(line == null){
					break;
				}
				sb.append(line);
				sb.append("\n");
			}
			
			return sb.toString();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static void writeToFile(String filename, String content){
		try(
				FileOutputStream fos = new FileOutputStream(filename);
				OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
				BufferedWriter bw = new BufferedWriter(osw);
				){
			
			bw.write(content);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
