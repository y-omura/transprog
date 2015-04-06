package datalogconv;

public class VariadicUtils {
	
	private VariadicUtils(){
	}

	/**
	 * return "x0: Node, x1: Node, ..."
	 * @param x
	 * @return
	 */
	public static String genXNodeList(int x){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < x; i++){
			sb.append("x").append(i).append(": Node, ");
		}
		if(sb.length() > 2)sb.delete(sb.length()-2, sb.length());
		return sb.toString();
	}
	
	/**
	 * return "x0, x1, ..."
	 * @param x
	 * @return
	 */
	public static String genXList(int x){
		return genList("x", x);
	}
	
	public static String genList(String string, int x) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < x; i++){
			sb.append(string).append(i).append(", ");
		}
		if(sb.length() > 2)sb.delete(sb.length()-2, sb.length());
		return sb.toString();
	}
}
