package treeconv;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import utils.FileUtil;

public class TreeConverter {

	public static void main(String[] args) {

		//type init
		String types = FileUtil.readFromFile("test.types");
		toTypeTree(types);
		StringBuilder inverseTypeMapSb = new StringBuilder();
		for(Type type : Type.typeMap.values()){
			inverseTypeMapSb.append(String.format("%02d %s\n", type.id, type.type));
		}
		FileUtil.writeToFile("test.typemap", inverseTypeMapSb.toString());
		
		//Tree init
		String str = FileUtil.readFromFile("tree.txt");
		Tree tree = toTree(str);
		
		//Label init
		StringBuilder inverseLabelMapSb = new StringBuilder();
		Map<Label, Integer> labelMap = generateLabelMap(tree, FileUtil.readFromFile("test.labels"));
		Label[] inverseLabelMap = new Label[labelMap.size()];
		for(Label key : labelMap.keySet()) {
			inverseLabelMap[labelMap.get(key)] = key;
		}
		for(int i = 0; i < inverseLabelMap.length; i++){
			inverseLabelMapSb.append(String.format("%02d %s %s\n", i, inverseLabelMap[i].label, inverseLabelMap[i].labelType));
		}
		FileUtil.writeToFile("test.labelmap", inverseLabelMapSb.toString());
		
		String datalog = toDatalog(tree, labelMap);
		FileUtil.writeToFile("test.tree", datalog.toString());
		
		FileUtil.writeToFile("test.typetree", toTypeDatalog());
	}
	
	public static Map<Label, Integer> generateLabelMap(Tree tree, String labels) {
		Map<Label, Integer> map = new HashMap<Label, Integer>();
		int id = 0;
		map.put(new Label("_", "meta"), id++);
		for(String line : labels.split("\r\n|\n\r|\r|\n")){
			if(line.trim().equals("")){
				continue;
			}
			String[] t = line.split("\\|");
			map.put(new Label(t[0], t.length>=2?t[1]:toLabelType(t[0])), id++);
		}
		
		generateLabelMapImpl(tree, map, id);
		
		return map;
	}

	private static int generateLabelMapImpl(Tree tree, Map<Label, Integer> map, int id) {
		if(!map.containsKey(tree.label)){
			map.put(tree.label, id++);
		}
		for(Tree t : tree.children){
			id = generateLabelMapImpl(t, map, id);
		}
		return id;
	}
	
	public static String toDatalog(Tree tree, Map<Label, Integer> labelMap) {
		StringBuilder sb = new StringBuilder();
		sb.append("tree(parent: Node, child: Node, index: Index, child_label: Label, child_type: Type)\n");
		
		toDatalogImpl(tree.children.get(0), labelMap, sb, 0);
		
		return sb.toString();
	}
	private static void toDatalogImpl(Tree tree, Map<Label, Integer> labelMap, StringBuilder sb, int index) {
		sb.append(String.format("tree(%d, %d, %d, %d, %d).\n",
				tree.parent.id, tree.id, index, labelMap.get(tree.label), tree.type.id));
		
		for(int i = 0; i < tree.children.size(); i++){
			toDatalogImpl(tree.children.get(i), labelMap, sb, i);
		}
		
	}
	
	public static String toTypeDatalog(){
		StringBuilder sb = new StringBuilder();
		sb.append("type_tree(parent: Type, child: Type)\n");
		
		for(Type t : Type.typeMap.values()){
			sb.append(String.format("type_tree(%d, %d).\n", t.parent==null?0:t.parent.id, t.id));
		}
		
		return sb.toString();
	}
	
	public static Map<Integer, Label> readLabelMap(String labels){
		Map<Integer, Label> map = new HashMap<>();
		
		for(String line : labels.split("\n\r|\r\n|\n|\r")){
			if(line.trim().equals("")){
				continue;
			}
			String[] tokens = line.split(" ", 3);
			map.put(Integer.parseInt(tokens[0]), new Label(tokens[1], tokens[2]));
		}
		
		return map;
	}
	
	public static Map<Integer, Type> readTypeMap(String types){
		Map<Integer, Type> map = new HashMap<>();

		for(String line : types.split("\n\r|\r\n|\n|\r")){
			if(line.trim().equals("")){
				continue;
			}
			String[] tokens = line.split(" ", 2);
			if(Integer.parseInt(tokens[0]) == 1){
				map.put(1, Type.UNKNOWN);
			} else {
				map.put(Integer.parseInt(tokens[0]), new Type(tokens[1], null, null));
			}
		}
		
		return map;
	}
	
	static Tree toTree(String str){
		
		int id = 0;
		Tree root = new Tree(id++, "root", toLabelType("root"), "_", null);
		
		int tabs = -1;
		Tree target = root;
		for(String line : str.split("\n")){
			int t = line.lastIndexOf("\t")+1;
			
			String[] s = line.trim().split("\\|");
			String label = s[0];
			String type = s.length >= 2? s[1]: null;
			
			
			//child
			if(tabs < t) {
				tabs = t;
				Tree tree = new Tree(id++, label, toLabelType(label), type, target);
				target.children.add(tree);
				target = tree;
			}
			//parent or sibling
			else {
				int gap = tabs - t;
				tabs = t;
				for(int i = 0; i < gap; i++) {
					target = target.parent;
				}
				target = target.parent;

				Tree tree = new Tree(id++, label, toLabelType(label), type, target);
				target.children.add(tree);
				target = tree;
			}
		}
		
		
		return root;
	}
	

	public static Type toTypeTree(String str){
		
		Type root = new Type("object", null, null);
		
		int tabs = -1;
		Type target = root;
		for(String line : str.split("\n")){
			int t = line.lastIndexOf("\t")+1;
			
			String[] s = line.trim().split("\\|");
			String type = s.length >= 1? s[0]: null;
			String typeType = s.length >= 2? s[1]: null;
			
			
			//child
			if(tabs < t) {
				tabs = t;
				Type tree = new Type(type, typeType, target);
				target.children.add(tree);
				target = tree;
			}
			//parent or sibling
			else {
				int gap = tabs - t;
				tabs = t;
				for(int i = 0; i < gap; i++) {
					target = target.parent;
				}
				target = target.parent;

				Type tree = new Type(type, typeType, target);
				target.children.add(tree);
				target = tree;
			}
		}
		
		
		return root;
	}
	
	
	public static String toLabelType(String label){
		switch(label){
			case "root":
				return "root";
			case "block":
			case "if":
			case "switch":
			case "switchGroup":
			case "return":
				return "control_block";
			case "def":
				return "def";
			case "int":
				return "type";
			case ".":
			case "=":
			case "*":
			case "/":
			case "+":
			case "-":
			case "new":
			case "==":
			case "<":
			case ">":
			case "<=":
			case ">=":
				return "operator";
			default:
				return "symbol_or_literal";
		}
	}

}
