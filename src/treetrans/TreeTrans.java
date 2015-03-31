package treetrans;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import treeconv.AntlrTreeConv;
import treeconv.Label;
import treeconv.Tree;
import treeconv.TreeConverter;
import treeconv.TuplesToGraphviz;
import treeconv.Type;
import utils.FileUtil;
import antlr.JavaLexer;
import antlr.JavaParser;

public class TreeTrans {

	public static void main(String[] args) throws Exception {
		JavaLexer lexer = new JavaLexer(new ANTLRFileStream("../antlrtest/src/Test.java"));
		final JavaParser parser = new JavaParser(new CommonTokenStream(lexer));
		final ParseTree antlrtree = parser.compilationUnit();
		
		int id = 0;
		Tree tree = new Tree(id++, "root", "control_block", null, null);
		tree.children.add(new AntlrTreeConv(id, Arrays.asList(parser.getRuleNames())).convAntlrToTree(tree, antlrtree));
		
		TreeTrans trans = new TreeTrans();
		trans.parseRule(FileUtil.readFromFile("rule"));
		tree = trans.trans(0, null, tree).getRight();
		FileUtil.writeToFile("debug.dot", TuplesToGraphviz.treeToGraphviz(tree));
		
		//Label init
		StringBuilder inverseLabelMapSb = new StringBuilder();
		Map<Label, Integer> labelMap = TreeConverter.generateLabelMap(tree, FileUtil.readFromFile("test.labels"));
		Label[] inverseLabelMap = new Label[labelMap.size()];
		for(Label key : labelMap.keySet()) {
			inverseLabelMap[labelMap.get(key)] = key;
		}
		for(int i = 0; i < inverseLabelMap.length; i++){
			inverseLabelMapSb.append(String.format("%02d %s %s\n", i, inverseLabelMap[i].label, inverseLabelMap[i].labelType));
		}
		FileUtil.writeToFile("test.labelmap", inverseLabelMapSb.toString());
		
		String datalog = TreeConverter.toDatalog(tree, labelMap);
		FileUtil.writeToFile("test.tree", datalog.toString());
		
		FileUtil.writeToFile("test.typetree", TreeConverter.toTypeDatalog());
	}
	
	private List<TransRule> rules = new ArrayList<TransRule>();
	
	public void parseRule(String ruleString){
		
		for(String line : ruleString.split("\n\r|\r\n|\n|\r")){
			String[] rule = line.split("->");
			
			RuleElement query = parseRuleElement(rule[0].trim(), false);
			RuleElement result = parseRuleElement(rule[1].trim(), true);
			
			//TODO validate query
			
			rules.add(new TransRule(query, result));
		}
	}
	
	public Pair<Integer, Tree> trans(int id, Tree parent, Tree before){
		for(TransRule rule : rules){
			Map<Integer, Tree[]> vars = apply(before, rule.query);
			if(vars == null){
				continue;
			}
			
			Pair<Integer, Tree[]> t = trans(id, parent, rule.result, vars);
			
			return new ImmutablePair<Integer, Tree>(t.getLeft(), t.getRight()[0]);
		}
		throw new IllegalArgumentException("適用するルールがありません :"+before.toString());
	}
	
	private Pair<Integer, Tree[]> trans(int id, Tree parent, RuleElement result, Map<Integer, Tree[]> vars){
		Tree[] tree;
		switch(result.type){
		case CONTROL_BLOCK:
			tree = new Tree[]{new Tree(id++, result.label, TreeConverter.toLabelType(result.label), null, parent)};
			break;
		case SYMBOL_OR_LITERAL:
			tree = new Tree[]{new Tree(id++, result.label, "symbol_or_literal", null, parent)};
			break;
		case AS_IS:
			Label l = vars.get(result.index)[0].label;
			tree = new Tree[]{new Tree(id++, l.label, l.labelType, null, parent)};
			break;
		case VAR:
			if(result.index == 0){
				Pair<Integer, Tree> t = trans(id, parent, vars.get(result.var)[0]);
				tree = new Tree[]{t.getRight()};
				id = t.getLeft();
			} else {
				Tree[] ts = vars.get(result.var);
				tree = new Tree[(ts.length-1)/result.index+1];
				for(int i = 0; i < tree.length; i++){
					Pair<Integer, Tree> t = trans(id, parent, vars.get(result.var)[i*result.index]);
					tree[i] = t.getRight();
					id = t.getLeft();
				}
			}
			break;
		default:
			throw new IllegalArgumentException();
		
		}
		
		for(RuleElement r : result.children){
			Pair<Integer, Tree[]> t = trans(id, tree[0], r, vars);
			id = t.getLeft();
			tree[0].children.addAll(Arrays.asList(t.getRight()));
		}
		
		return new ImmutablePair<>(id, tree);
	}
	
	/**
	 * 
	 * @param tree
	 * @param query
	 * @return 当てはまらなかったらnull
	 */
	private Map<Integer, Tree[]> apply(Tree tree, RuleElement query){
		Map<Integer, Tree[]> result = new HashMap<>();
		Queue<Tree> queue = new ArrayDeque<>();
		Queue<RuleElement> queryQueue = new ArrayDeque<>();
		
		int r = query.matches(tree);
		if(r < 0){
			return null;
		} else {
			if(r > 0){
				result.put(query.var, new Tree[]{tree});
				return result;
			}
		}
		
		queue.add(tree);
		queryQueue.add(query);
		while(!queue.isEmpty()) {
			Tree t = queue.poll();
			RuleElement q = queryQueue.poll();
			
			if(q.children.size() > t.children.size()){
				return null;
			}
			
			//前から見ていく
			int left = 0;
			while(left < t.children.size()){
				if(left >= q.children.size()){
					return null;
				}
				int res = q.children.get(left).matches(t.children.get(left));
				if(res < 0){
					return null;
				} else if(res == 0){
					queue.add(t.children.get(left));
					queryQueue.add(q.children.get(left));
				} else if(res == 1){
					result.put(q.children.get(left).var, new Tree[]{t.children.get(left)});
				} else if(res == 2){
					break;
				}
				
				left++;
			}
			
			// 処理しきれていない場合は後ろから見ていく(可変長がある場合)
			int right = 0;
			while(left <= t.children.size()-1-right){
				int res = q.children.get(q.children.size()-1-right).matches(t.children.get(t.children.size()-1-right));
				if(res < 0){
					return null;
				} else if(res == 0){
					queue.add(t.children.get(t.children.size()-1-right));
					queryQueue.add(q.children.get(q.children.size()-1-right));
				} else if(res == 1){
					result.put(q.children.get(q.children.size()-1-right).var, new Tree[]{t.children.get(right)});
				} else if(res == 2){
					break;
				}
				
				right++;
			}
			
			//可変長があればそれを入れる
			if(left + right + 1 < q.children.size()){
				throw new IllegalArgumentException("可変長変数が2つ以上あります"+q);
			} else if(left + right + 1 == q.children.size()){
				result.put(q.children.get(left).var, t.children.subList(left, t.children.size()-right).toArray(new Tree[0]));
			}
			
		}
		
		return result;
	}
	
	private RuleElement parseRuleElement(String ruleElementString, boolean isResult){
		
		Tokenizer tokenizer = new Tokenizer(ruleElementString);
		
		RuleElement root = null;
		RuleElement targetParent = null;
		Pair<String, Integer> token;
		while((token = tokenizer.nextToken(isResult))!=null) {
			if(token.getRight() == 3){
				if(root == null){
					throw new IllegalArgumentException();
				}
				switch(token.getLeft().charAt(0)){
				case '[':
					if(targetParent == null){
						targetParent = root;
					} else {
						if(targetParent.children.isEmpty()){
							throw new IllegalArgumentException();
						}
						targetParent = targetParent.children.get(targetParent.children.size()-1);
					}
					break;
				case ']':
					if(targetParent.children.isEmpty()){
						throw new IllegalArgumentException();
					}
					targetParent = targetParent.parent;
					break;
				case '+':
					RuleElement last;
					if(targetParent != null){
						last = targetParent.children.get(targetParent.children.size()-1);
					} else {
						last = root;
					}
					if(last.type != RuleType.VAR){
						throw new IllegalArgumentException();
					}
					last.index = 1;
					break;
				}
				continue;
			}

			RuleElement target;
			switch(token.getRight()){
			case 0:
				boolean realControlBlock = false;
				for(String cbs : JavaParser.ruleNames){
					if(cbs.equals(token.getLeft())){
						realControlBlock = true;
					}
				}
				for(String cbs : JavaParser.tokenNames){
					if(cbs.startsWith("'")){
						cbs = cbs.substring(1, cbs.length()-1);
					} else {
						continue;
					}
					if(cbs.equals(token.getLeft())){
						realControlBlock = true;
					}
				}
				for(String cbs : new String[]{
						"<EOF>",
						"root",
				}){
					if(cbs.equals(token.getLeft())){
						realControlBlock = true;
					}
				}
				
				
				if(realControlBlock){
					target = new RuleElement(targetParent, token.getLeft(), RuleType.CONTROL_BLOCK);
				} else {
					target = new RuleElement(targetParent, token.getLeft(), RuleType.SYMBOL_OR_LITERAL);
				}
				break;
			case 1:
				target = new RuleElement(targetParent, token.getLeft(), RuleType.SYMBOL_OR_LITERAL);
				break;
			case 2:
				target = new RuleElement(targetParent, token.getLeft(), RuleType.AS_IS, Integer.parseInt(token.getLeft()));
				break;
			case 4:
				target = new RuleElement(targetParent, token.getLeft(), RuleType.VAR, Integer.parseInt(token.getLeft()));
				break;
			case 5:
				String index = token.getLeft();
				if(index.isEmpty()){
					index = "1";
				}
				RuleElement last;
				if(targetParent != null){
					last = targetParent.children.get(targetParent.children.size()-1);
				} else {
					last = root;
				}
				if(last.type != RuleType.VAR){
					throw new IllegalArgumentException();
				}
				last.index = Integer.parseInt(index);
				continue;
			default:
				throw new IllegalArgumentException();
			}
			if(root == null){
				root = target;
			} else {
				targetParent.children.add(target);
			}
		}
		return root;
	}
	
	private class Tokenizer{
		public String ruleString;
		public int index = 0;
		
		private Pattern stringRegex = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]+|^\"([^\"]|\\\\\")+\"");
		//symbol or literal指定する場合は#開始
		//ラベルをそのままにする場合は##
		private Pattern typeRegex = Pattern.compile("^#?[a-zA-Z][a-zA-Z0-9]+|^#?\"([^\"]|\\\\\")+\"|^##[0-9]+");
		private Pattern indexRegex = Pattern.compile("^<[0-9]*n>");
		private Pattern numberRegex = Pattern.compile("^[0-9]+");
		
		public Tokenizer(String ruleString){
			this.ruleString = ruleString;
		}
		/**
		 * @arg RuleResultの場合はisTypeを指定
		 * @return String: token,
		 * Integer: 
		 * 	0 if the token is control_block, 
		 * 	1 if the token is symbol_or_literal,
		 * 	2 if the token is symbol_or_literal and its label will be inherited,
		 *  3 if the token is other operators,
		 *  4 if the token is number,
		 *  5 if the token is index,
		 */
		private Pair<String, Integer> nextToken(boolean isType){
			if(index >= ruleString.length()){
				return null;
			}
			
			Pattern p;
			if(isType){
				p = typeRegex;
			} else {
				p = stringRegex;
			}
			
			Matcher m = p.matcher(ruleString.substring(index));
			if(m.find()){
				index += m.end();
				String token = m.group(0);
				
				boolean symbol_or_literal = false;
				if(token.charAt(0) == '#'){
					symbol_or_literal = true;
					token = token.substring(1);
				}
				if(token.charAt(0) == '#'){
					return new ImmutablePair<>(token.substring(1), 2);
				}
				if(token.charAt(0) == '"'){
					token = token.substring(1, token.length()-1);
					return new ImmutablePair<>(token, symbol_or_literal?1:0);
				}
				return new ImmutablePair<>(token, symbol_or_literal?1:0);
			}
			m = numberRegex.matcher(ruleString.substring(index));
			if(m.find()){
				index += m.end();
				String token = m.group(0);
				return new ImmutablePair<>(token, 4);
			}
			m = indexRegex.matcher(ruleString.substring(index));
			if(m.find()){
				index += m.end();
				String token = m.group(0);
				return new ImmutablePair<>(token.substring(1, token.length()-2), 5);
			}
			
			index++;
			return new ImmutablePair<>(ruleString.substring(index-1, index), 3);
		}
	}
	
	private static class TransRule {
		public RuleElement query;
		public RuleElement result;
		public TransRule(RuleElement query, RuleElement result) {
			super();
			this.query = query;
			this.result = result;
		}
	}
	
	private static class RuleElement {
		public RuleElement parent;
		public List<RuleElement> children = new ArrayList<>();
		public String label;
		public RuleType type;
		public int var = -1;
		public int index = 0;
		
		public RuleElement(RuleElement parent, String label, RuleType type){
			this(parent, label, type, -1);
		}
		public RuleElement(RuleElement parent, String label, RuleType type, int var) {
			this.parent = parent;
			this.label = label;
			this.type = type;
			this.var = var;
		}
		@Override
		public String toString() {
			return "<"+label + (index!=0?"{"+index+"n}":"") +">" + (children.isEmpty()?"":children);
		}
		
		/**
		 * 
		 * @param tree
		 * @param force
		 * @return -1: not match, 0: match, 1: 変数でのマッチ, 2:可変長でのマッチ
		 */
		public int matches(Tree tree){
			if(type == RuleType.CONTROL_BLOCK){
				return tree.label.label.equals(label) && (tree.label.labelType.equals("control_block") || tree.label.labelType.equals("operator"))?0:-1;
			}
			if(type == RuleType.SYMBOL_OR_LITERAL){
				return tree.label.label.equals(label) && tree.label.labelType.equals("symbol_or_literal")?0:-1;
			}
			if(type == RuleType.VAR){
				if(index == 0){
					return 1;
				}
				return 2;
			}
			return -1;
		}
	}
	
	private enum RuleType {
		CONTROL_BLOCK, SYMBOL_OR_LITERAL, VAR, AS_IS, ;
	}
}
