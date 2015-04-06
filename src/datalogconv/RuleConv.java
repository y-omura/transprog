package datalogconv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static datalogconv.VariadicUtils.*;

public class RuleConv {


	/**
	 * 
	 * @param rules
	 * @param sb
	 * @return usage of rule[remove, replace]
	 */
	public static List<Map<Rule, Integer>> processRules(String rules, StringBuilder sb){
		/**
		 * [SEARCH]: searchの引数の数 (0の場合は存在しない)
		 * [Rule.REMOVE]: remove(..) :- . のルール数 (0の場合は存在しない)
		 * [Rule.REPLACE]: replace(..) :- . のルール数 (0の場合は存在しない)
		 */
		List<Map<Rule, Integer>> ruleUsage = new ArrayList<>();
		for(String line: rules.split("\n")){
			String trimed = line.trim();
			
			int rule_no = ruleUsage.size() - 1;
			
			if("".equals(trimed)){
				sb.append("\n");
			} else if(trimed.startsWith("##")){
				String command = trimed.substring(2).trim();
				if(command.equals("rule")){
					ruleEndAction(sb, ruleUsage);
					
					ruleUsage.add(new HashMap<Rule, Integer>());
					for(Rule rule : Rule.values()){
						ruleUsage.get(ruleUsage.size()-1).put(rule, 0);
					}
					sb.append("\n");
				} else {
					System.err.println("Syntax Error (Unknown Command): " + command);
				}
			} else if(trimed.startsWith("#")){
				sb.append(trimed).append("\n");
			} else if(trimed.startsWith("search(") || trimed.startsWith("remove(") || trimed.startsWith("replace(") || trimed.startsWith("add(")){
				
				Map<Rule, Integer> r = ruleUsage.get(ruleUsage.size()-1);
				String prefix = new StringBuilder().append("rule").append(rule_no).append("_").toString();
				for(Rule rule : new Rule[]{Rule.REMOVE, Rule.REPLACE, Rule.ADD}){
					if(trimed.startsWith(rule.getString()+"(")){
						if(r.get(rule) == 0){
							sb.append(prefix).append(rule.getString());
							if(rule == Rule.REMOVE){
								sb.append("(n: Node, ");
							} else if(rule == Rule.REPLACE){
								sb.append("(p: Node, c: Node, i: Index, l: Label, t: Type, ");
							} else if(rule == Rule.ADD) {
								sb.append("(p: Node, c: Node, i: Index, l:Label, t:Type, is_root: Boolean, ");
							} else {
								assert false;
							}
							sb.append(genXNodeList(r.get(Rule.SEARCH)));
							sb.append(") outputtuples\n");
						}
						r.put(rule, r.get(rule)+1);
					}
				}
				if(trimed.startsWith("search(")){
					if(r.get(Rule.SEARCH) == 0){
						if(r.get(Rule.REMOVE) > 0 || r.get(Rule.REPLACE) > 0 || r.get(Rule.ADD) > 0){
							System.err.println("Syntax Error(Must Define 'search' before 'remove' and 'replace' and 'add'): "+ trimed);
						}
						Pattern p = Pattern.compile("search\\((.+?)\\)");
						Matcher m = p.matcher(trimed);
						if(!m.find() || m.groupCount() != 1){
							System.err.println("Syntax Error: "+trimed);
						} else {
							r.put(Rule.SEARCH, m.group(1).split(",").length);
							sb.append(prefix).append("search(");
							sb.append(genXNodeList(r.get(Rule.SEARCH)));
							sb.append(") outputtuples\n");
						}
					}
				}
				trimed = trimed
					.replaceAll("remove\\(", prefix+"remove(")
					.replaceAll("replace\\(", prefix+"replace(")
					.replaceAll("add\\(", prefix+"add(")
					.replaceAll("search\\(", prefix+"search(");
				sb.append(trimed).append("\n");
			} else {
				System.err.println("Maybe This is Library: "+trimed);
				sb.append(trimed).append("\n");
			}
		}
		ruleEndAction(sb, ruleUsage);
		
		return ruleUsage;
	}

	private static void ruleEndAction(StringBuilder sb, List<Map<Rule, Integer>> ruleUsage) {
		if(ruleUsage.size() <= 0){
			return;
		}
		int rule_no = ruleUsage.size()-1;
		String prefix = new StringBuilder().append("rule").append(rule_no).append("_").toString();
		Map<Rule, Integer> r = ruleUsage.get(rule_no);
		
		//has_smaller_search
		if(r.get(Rule.SEARCH) > 0) {
			sb.append(prefix).append("has_smaller_search(");
			sb.append(genXNodeList(r.get(Rule.SEARCH)));
			sb.append(")\n");
			
			for(int k = 0; k < r.get(Rule.SEARCH); k++){
				sb.append(prefix).append("has_smaller_search(");
				sb.append(genXList(r.get(Rule.SEARCH)));
				sb.append(") :- ").append(prefix).append("search(");
				sb.append(genXList(r.get(Rule.SEARCH)));
				sb.append("), ");

				sb.append(prefix).append("search(");
				sb.append(genList("y", r.get(Rule.SEARCH)));
				sb.append("), ");
	
				for(int i = 0; i < k+1; i++){
					if(i == k){
						sb.append(String.format("y%d < x%d, ", i, i));
					} else {
						sb.append(String.format("y%d = x%d, ", i, i));
					}
				}
				sb.delete(sb.length()-2, sb.length());
				sb.append(".\n");
			}
			
			//is_min_search
			sb.append(prefix).append("is_min_search(");
			sb.append(genXNodeList(r.get(Rule.SEARCH)));
			sb.append(") outputtuples\n");
			
			sb.append(prefix).append("is_min_search(");
			sb.append(genXList(r.get(Rule.SEARCH)));
			sb.append(") :- ").append(prefix).append("search(");
			sb.append(genXList(r.get(Rule.SEARCH)));
			sb.append("), !").append(prefix).append("has_smaller_search(");
			sb.append(genXList(r.get(Rule.SEARCH)));
			sb.append(").\n");
		}
		
		for(Rule rule : new Rule[]{Rule.REMOVE, Rule.REPLACE}){
			if(ruleUsage.get(rule_no).get(rule) > 0){
				sb.append(prefix).append("remove_to_").append(rule.getString()).append("(n: Node) outputtuples\n");
				sb.append(prefix).append("remove_to_").append(rule.getString());
				sb.append("(n) :- ");
				sb.append(prefix).append(rule.getString());
				if(rule == Rule.REMOVE) {
					sb.append("(n, ");
				} else if(rule == Rule.REPLACE){
					sb.append("(_, n, _, _, _, ");
				} else {
					assert false;
				}
				sb.append(genXList(r.get(Rule.SEARCH)));
				sb.append(")");
				if(r.get(Rule.SEARCH) > 0){
					sb.append(", ");
					sb.append(prefix).append("is_min_search(");
					sb.append(genXList(r.get(Rule.SEARCH)));
					sb.append(")");
				}
				sb.append(".\n");
			}
		}
		
		if(ruleUsage.get(rule_no).get(Rule.ADD) > 0){
			sb.append(prefix).append("addition_nodes(n: Node) outputtuples\n");
			sb.append(prefix).append("addition_nodes(n) :- ");
			sb.append(prefix).append("add(_, n, _, _, _, _,").append(genXList(r.get(Rule.SEARCH))).append(")");
			if(r.get(Rule.SEARCH) > 0){
				sb.append(", ");
				sb.append(prefix).append("is_min_search(");
				sb.append(genXList(r.get(Rule.SEARCH)));
				sb.append(")");
			}
			sb.append(".\n");
		}
	}
	
	static enum Rule {
		SEARCH("search"), REMOVE("remove"), REPLACE("replace"), ADD("add");
		
		private String string;
		
		private Rule(String string) {
			this.string = string;
		}
		
		public String getString() {
			return string;
		}
		
		public boolean isSepecial(){
			return this == SEARCH;
		}
	}
}
