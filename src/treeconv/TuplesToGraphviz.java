package treeconv;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import utils.FileUtil;


public class TuplesToGraphviz {
	
	public static String treeToGraphviz(Tree tree){
		StringBuilder sb = new StringBuilder();
		Map<Integer, Label> labelMap = new HashMap<>();
		Map<Integer, Type> typeMap = new HashMap<>();
		
		Queue<Tree> queue = new ArrayDeque<>();
		
		queue.add(tree);
		
		while(!queue.isEmpty()){
			Tree target = queue.poll();
			
			Integer labelId = null;
			for(Integer l : labelMap.keySet()){
				if(target.label.equals(labelMap.get(l))){
					labelId = l;
				}
			}
			if(labelId == null){
				labelId = labelMap.size();
				labelMap.put(labelMap.size(), target.label);
			}
			
			Integer typeId = null;
			for(Integer t : typeMap.keySet()){
				if(target.type.equals(typeMap.get(t))){
					typeId = t;
				}
			}
			if(typeId == null){
				typeId = typeMap.size();
				typeMap.put(typeMap.size(), target.type);
			}
			
			if(target.parent!=null){
				sb.append(String.format("%d %d %d %d %d\n", target.parent.id, target.id,
						target.parent.children.indexOf(target), labelId, typeId));
			}
			
			queue.addAll(target.children);
		}
		
		return tuplesToGraphviz(sb.toString(), labelMap, typeMap);
	}
	
	public static String tuplesToGraphviz(String tuples, Map<Integer, Label> labelMap, Map<Integer, Type> typeMap){
		
		if(labelMap == null){
			labelMap = new HashMap<>();
		}
		if(typeMap == null){
			typeMap = new HashMap<>();
		}
		
		List<String[]> sorted_tuples = new ArrayList<String[]>();
		for(String tuple : tuples.split("\n\r|\r\n|\r|\n")){
			tuple = tuple.trim();
			if(tuple.isEmpty() || tuple.startsWith("#")){
				continue;
			}
			
			sorted_tuples.add(tuple.split(" "));
		}
		Collections.sort(sorted_tuples, new Comparator<String[]>() {

			@Override
			public int compare(String[] o1, String[] o2) {
				return Integer.parseInt(o1[2]) - Integer.parseInt(o2[2]);
			}
		});
		Collections.sort(sorted_tuples, new Comparator<String[]>() {

			@Override
			public int compare(String[] o1, String[] o2) {
				return Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]);
			}
		});
		
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("digraph converted_tree {\n");
		int parent = -1;
		ArrayList<Integer> siblings = new ArrayList<>();
		for(String[] t : sorted_tuples){
			if(parent != Integer.parseInt(t[0])){
				sb.append("\tsubgraph {\n");
				sb.append("\trank=\"same\";\n");
				sb.append("\tedge[style=\"invisible\",dir=\"none\"];\n");
				Integer prev = null;
				for(Integer s : siblings){
					if(prev == null){
						prev = s;
					} else {
						sb.append(String.format("\t\t%d -> %d\n", prev, s));
						prev = s;
					}
				}
				sb.append("}\n");
				parent = Integer.parseInt(t[0]);
				siblings.clear();
			}
			siblings.add(Integer.parseInt(t[1]));
			
			String label = t[3];
			if(labelMap.containsKey(Integer.parseInt(t[3]))){
				label = graphvizEscape(labelMap.get(Integer.parseInt(t[3])).label);
			}
			String type = t[4];
			if(typeMap.containsKey(Integer.parseInt(t[4]))){
				type = graphvizEscape(typeMap.get(Integer.parseInt(t[4])).type);
			}
			sb.append(String.format("\t%s [label=\"{{%s|%s}|%s}\", shape=record]\n", t[1], type, label, t[1]));
			sb.append(String.format("\t%s -> %s [label=\"%s\"]\n", t[0], t[1], t[2]));
		}
		
		sb.append("}");
		
		
		return sb.toString();
	}
	
	public static String graphvizEscape(String str){
		StringBuilder sb = new StringBuilder();
		
		String escapingLetter = "<>{}|";
		for(int i = 0; i < str.length(); i++){
			char c = str.charAt(i);
			if(escapingLetter.indexOf(c) >= 0){
				sb.append("\\").append(c);
			} else {
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
}
