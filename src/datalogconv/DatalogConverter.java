package datalogconv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static datalogconv.VariadicUtils.*;
import datalogconv.RuleConv.Rule;
import treeconv.Label;
import treeconv.TreeConverter;
import treeconv.TuplesToGraphviz;
import treeconv.Type;
import utils.FileUtil;

public class DatalogConverter {
	public static final int LOOP = 10;
	
	public static void main(String[] args) {
		String filename = "test";
		String tempfilename = "temp";
		File temp = new File(tempfilename);
		temp.mkdirs();
		for(File f : temp.listFiles()){
			f.delete();
		}
		
		Map<Integer, Label> labelMap = TreeConverter.readLabelMap(FileUtil.readFromFile(filename+".labelmap"));
		Map<Integer, Type> typeMap = TreeConverter.readTypeMap(FileUtil.readFromFile(filename+".typemap"));
		
		String init = FileUtil.readFromFile(filename+".init");
		String typeTree = FileUtil.readFromFile(filename+".typetree");
		String tree = FileUtil.readFromFile(filename+".tree");
		String lib = FileUtil.readFromFile(filename+".lib");
		String rules = FileUtil.readFromFile(filename+".rules");
		StringBuilder sbRules = new StringBuilder();
		List<Map<Rule, Integer>> ruleUsage = RuleConv.processRules(rules, sbRules);
		
		for(int i = 0; i < LOOP; i++){
			StringBuilder sb = new StringBuilder();
			sb.append(init);
			sb.append(typeTree);
			sb.append(tree);
			sb.append(lib);
			sb.append(sbRules);
			sb.append("\n");
			genConvertedTree(ruleUsage, sb);
			
			sb = replaceLabelsAndTypes(sb, labelMap, typeMap);
			
			FileUtil.writeToFile(tempfilename+"/"+filename+".dlg", sb.toString());
			
			Runtime runtime = Runtime.getRuntime();
			try {
				// Execute
				Process process = runtime.exec("java -jar -Dfile.encoding=UTF-8 bddbddb-full.jar "+tempfilename+"/"+filename+".dlg");
				new InputStreamThread(process.getInputStream()).start();
				new InputStreamThread(process.getErrorStream()).start();
				
				int retcode = process.waitFor();
				if(retcode != 0){
					break;
				}
				
				tree = FileUtil.readFromFile(tempfilename+"/converted_tree.tuples");
				FileUtil.writeToFile(tempfilename+"/"+filename+"_"+i+".dot", TuplesToGraphviz.tuplesToGraphviz(tree, labelMap, typeMap));
				
				String is_applying = FileUtil.readFromFile(tempfilename+"/is_applying.tuples");
				if(is_applying.trim().equals("")){
					System.out.printf("\n\nDONE. loop count: %d.\n", i);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tree = tuplesToTree(tree);
		}
	}
	
	private static StringBuilder replaceLabelsAndTypes(StringBuilder sb, Map<Integer, Label> labelMap, Map<Integer, Type> typeMap) {
		StringBuilder ret = new StringBuilder();
		
		Map<String, List<Pair<Integer, Label>>> inLabelMap = new HashMap<>();
		for(Integer i : labelMap.keySet()){
			if(inLabelMap.containsKey(labelMap.get(i).label)){
				List<Pair<Integer, Label>> l = inLabelMap.get(labelMap.get(i).label);
				l.add(new ImmutablePair<Integer, Label>(i, labelMap.get(i)));
			} else {
				List<Pair<Integer, Label>> l = new ArrayList<>();
				l.add(new ImmutablePair<Integer, Label>(i, labelMap.get(i)));
				inLabelMap.put(labelMap.get(i).label, l);
			}
		}
		
		Map<String, Integer> inTypeMap = new HashMap<>();
		for(Integer i : typeMap.keySet()){
			inTypeMap.put(typeMap.get(i).type, i);
		}
		
		Pattern pLabel = Pattern.compile("\\{(.+?)\\}");
		Pattern pType = Pattern.compile("\\[(.+?)\\]");
		for(String line : sb.toString().split("\n\r|\r\n|\r|\n")){
			line = line.trim();
			if(line.equals("") || line.startsWith("#")){
				ret.append(line).append("\n");
				continue;
			}
			
			line = convertWithLabelMap(pLabel, inLabelMap, line);
			line = convertWithMap(pType, inTypeMap, line);
			ret.append(line).append("\n");
		}
		
		return ret;
	}
	
	private static String convertWithMap(Pattern p, Map<String, Integer> inMap, String line){
		StringBuilder converted = new StringBuilder();
		Matcher m = p.matcher(line);
		int s = 0;
		while(m.find(s)){
			converted.append(line.substring(s, m.start()));
			if(!inMap.containsKey(m.group(1))){
				throw new IllegalArgumentException("label not found: "+m.group(1));
			}
			converted.append(inMap.get(m.group(1)));
			s = m.end();
		}
		converted.append(line.substring(s));
		return converted.toString();
	}
	
	private static String convertWithLabelMap(Pattern p, Map<String, List<Pair<Integer, Label>>> inLabelMap, String line){
		StringBuilder converted = new StringBuilder();
		Matcher m = p.matcher(line);
		int s = 0;
		while(m.find(s)){
			converted.append(line.substring(s, m.start()));
			boolean symbolMode = false;
			String token = m.group(1);
			if(token.startsWith("#")){
				symbolMode = true;
				token = token.substring(1);
			}
			if(!inLabelMap.containsKey(token)){
				throw new IllegalArgumentException("label not found: "+token);
			}
			List<Pair<Integer, Label>> labels = inLabelMap.get(token);
			Integer control = null;
			Integer symbol = null;
			for(Pair<Integer, Label> pair : labels){
				if(pair.getRight().labelType.equals("symbol or literal")){
					symbol = pair.getLeft();
				} else {
					control = pair.getLeft();
				}
			}
			
			Integer i = control;
			if(symbolMode || i == null){
				if(symbol != null){
					i = symbol;
				}
			}
			converted.append(i);
			s = m.end();
		}
		converted.append(line.substring(s));
		return converted.toString();
	}

	private static String tuplesToTree(String tuples) {
		StringBuilder sb = new StringBuilder();
		sb.append("tree(p: Node, c: Node, i: Index, l: Label, t: Type)\n");
		
		for(String tuple : tuples.split("\n")){
			if(tuple.startsWith("#")){
				continue;
			} else if("".equals(tuple)){
				
			} else {
				String[] elem = tuple.split(" ");
				sb.append(String.format("tree(%s, %s, %s, %s, %s).\n", elem[0], elem[1], elem[2], elem[3], elem[4]));
			}
		}
		return sb.toString();
	}
	
	private static void genConvertedTree(List<Map<Rule, Integer>> ruleUsage, StringBuilder sb){
		
		//ルールの選択
		sb.append("Rule ").append(ruleUsage.size()).append("\n");
		sb.append("has_rule(r: Rule) outputtuples\n");
		for(int i = 0; i < ruleUsage.size(); i++){
			String prefix = new StringBuilder().append("rule").append(i).append("_").toString();
			
			if(ruleUsage.get(i).get(Rule.REMOVE) > 0){
				sb.append("has_rule(r) :- r = ").append(i).append(", ");
				sb.append(prefix).append("remove_to_remove(_).\n");
			}
			if(ruleUsage.get(i).get(Rule.REPLACE) > 0){
				sb.append("has_rule(r) :- r = ").append(i).append(", ");
				sb.append(prefix).append("remove_to_replace(_).\n");
			}
			if(ruleUsage.get(i).get(Rule.ADD) > 0){
				sb.append("has_rule(r) :- r = ").append(i).append(", ");
				sb.append(prefix).append("addition_nodes(_).\n");
			}
		}
		sb.append("has_smaller_rule(r: Rule)\n");
		sb.append("has_smaller_rule(r) :- has_rule(r), s < r, has_rule(s).\n");
		sb.append("is_min_rule(r: Rule) outputtuples\n");
		sb.append("is_min_rule(r) :- has_rule(r), !has_smaller_rule(r).\n");
		sb.append("is_applying(b: Boolean) outputtuples\n");
		sb.append("is_applying(b) :- b = 1, is_min_rule(r).\n");
		
		
		sb.append("converted_tree(p: Node, c: Node, i: Index, l: Label, t: Type) outputtuples\n");
		
		//addのノード番号変換
		sb.append("addition_nodes(r: Rule, n: Node) outputtuples\n");
		for(int i = 0; i < ruleUsage.size(); i++){
			if(ruleUsage.get(i).get(Rule.ADD) > 0){
				sb.append("addition_nodes(r, n) :- ").append(String.format("r = %d, rule%d_addition_nodes(n).\n", i, i));
			}
		}
		sb.append("lt_addition_nodes(r: Rule, n: Node, r_: Rule, n: Node) outputtuples\n");
		sb.append("lt_addition_nodes(r, n, r_, n_) :- r < r_, addition_nodes(r, n), addition_nodes(r_, n_).\n");
		sb.append("lt_addition_nodes(r, n, r_, n_) :- r = r_, n < n_, addition_nodes(r, n), addition_nodes(r_, n_).\n");
		sb.append("has_smaller_addition_nodes(r: Rule, n: Node)\n");
		sb.append("has_smaller_addition_nodes(r, n) :- lt_addition_nodes(r, n, r_, n_), addition_nodes(r_, _), addition_nodes(r, n).\n");
		sb.append("min_addition_nodes(r: Rule, n: Node) outputtuples\n");
		sb.append("min_addition_nodes(r, n) :- addition_nodes(r, n), !has_smaller_addition_nodes(r, n).\n");
		sb.append("has_smaller_addition_nodes_gt(r: Rule, n: Node, r_: Rule, n_: Node)\n");
		sb.append("has_smaller_addition_nodes_gt(r, n, r_, n_) :- lt_addition_nodes(r, n, rr, nn), lt_addition_nodes(rr, nn, r_, n_), addition_nodes(rr, _), addition_nodes(r, n), addition_nodes(r_, n_).\n");
		sb.append("next_addition_nodes(r: Rule, n: Node, r_: Rule, n_: Node)\n");
		sb.append("next_addition_nodes(r, n, r_, n_) :- lt_addition_nodes(r, n, r_, n_), !has_smaller_addition_nodes_gt(r, n, r_, n_), addition_nodes(r, n), addition_nodes(r_, n_).\n");
		sb.append("tree_nodes(n: Node)\n");
		sb.append("tree_nodes(n) :- tree(_, n, _, _, _).\n");
		sb.append("tree_nodes(n) :- tree(n, _, _, _, _).\n");
		sb.append("not_tree_nodes(n: Node)\n");
		sb.append("not_tree_nodes(n) :- !tree_nodes(n).\n");
		sb.append("has_smaller_not_tree_node(n: Node)\n");
		sb.append("has_smaller_not_tree_node(n) :- not_tree_nodes(n), not_tree_nodes(n_), n_ < n.\n");
		sb.append("min_node_not_in_tree(n: Node) outputtuples\n");
		sb.append("min_node_not_in_tree(n) :- not_tree_nodes(n), !has_smaller_not_tree_node(n).\n");
		sb.append("has_smaller_node_not_in_tree_gt(n: Node, n_: Node)\n");
		sb.append("has_smaller_node_not_in_tree_gt(n, n_) :- n < nn, nn < n_, not_tree_nodes(nn), not_tree_nodes(n), not_tree_nodes(n_).\n");
		sb.append("next_node_not_in_tree(n: Node, n_: Node)\n");
		sb.append("next_node_not_in_tree(n, n_) :- n < n_, !has_smaller_node_not_in_tree_gt(n, n_), not_tree_nodes(n), not_tree_nodes(n_).\n");
		sb.append("map_node(r: Rule, subtree: Node, maintree: Node) outputtuples\n");
		sb.append("map_node(r, s, m) :- min_addition_nodes(r, s), min_node_not_in_tree(m).\n");
		sb.append("map_node(r, s, m) :- map_node(rr, ss, mm), next_addition_nodes(rr, ss, r, s), next_node_not_in_tree(mm, m).\n");
		
		//既存ルール
		for(int i = 0; i < ruleUsage.size(); i++) {
			sb.append(String.format("converted_tree(p, c, i, l, t) :- is_min_rule(%d), tree(p, c, i, l, t), ", i));
			if(ruleUsage.get(i).get(Rule.REMOVE) > 0){
				sb.append(String.format("!rule%d_remove_to_remove(c), ", i));
			}
			if(ruleUsage.get(i).get(Rule.REPLACE) > 0){
				sb.append(String.format("!rule%d_remove_to_replace(c), ", i));
			}
			sb.delete(sb.length()-2, sb.length());
			sb.append(".\n");
		}
		sb.append(String.format("converted_tree(p, c, i, l, t) :- !is_applying(1), tree(p, c, i, l, t).\n"));
		
		//追加ルール
		for(int i = 0; i < ruleUsage.size(); i++) {
			if(ruleUsage.get(i).get(Rule.REPLACE) > 0){
				sb.append(String.format("converted_tree(p, c, i, l, t) :- is_min_rule(%d), rule%d_replace(p, c, i, l, t, ", i, i));
				if(ruleUsage.get(i).get(Rule.SEARCH) > 0){
					sb.append(genXList(ruleUsage.get(i).get(Rule.SEARCH)));
					sb.append(String.format("), rule%d_is_min_search(", i));
					sb.append(genXList(ruleUsage.get(i).get(Rule.SEARCH)));
				} else {
					sb.delete(sb.length()-2, sb.length());
				}
				sb.append(").\n");
			}
			if(ruleUsage.get(i).get(Rule.ADD) > 0){
				sb.append(String.format("converted_tree(p, c, i, l, t) :- is_min_rule(%d), rule%d_add(p_, c_, i, l, t, 0, ", i, i));
				if(ruleUsage.get(i).get(Rule.SEARCH) > 0){
					sb.append(genXList(ruleUsage.get(i).get(Rule.SEARCH)));
					sb.append(String.format("), rule%d_is_min_search(", i));
					sb.append(genXList(ruleUsage.get(i).get(Rule.SEARCH)));
				} else {
					sb.delete(sb.length()-2, sb.length());
				}
				sb.append(String.format("), map_node(%d, p_, p), map_node(%d, c_, c", i, i));
				sb.append(").\n");

				sb.append(String.format("converted_tree(p, c, i, l, t) :- is_min_rule(%d), rule%d_add(p, c_, i, l, t, 1, ", i, i));
				if(ruleUsage.get(i).get(Rule.SEARCH) > 0){
					sb.append(genXList(ruleUsage.get(i).get(Rule.SEARCH)));
					sb.append(String.format("), rule%d_is_min_search(", i));
					sb.append(genXList(ruleUsage.get(i).get(Rule.SEARCH)));
				} else {
					sb.delete(sb.length()-2, sb.length());
				}
				sb.append(String.format("), map_node(%d, c_, c", i, i));
				sb.append(").\n");
			}
		}
	}
}
