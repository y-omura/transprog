package treeconv;

import java.util.List;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class AntlrTreeConv {

	
	private int id;
	private List<String> ruleNames;
	
	public AntlrTreeConv(int id, List<String> ruleNames){
		this.id = id;
		this.ruleNames = ruleNames;
	}
	public Tree convAntlrToTree(Tree parent, ParseTree antlr){
		Tree tree;
		if(antlr instanceof RuleContext){
			RuleContext rc_antlr = (RuleContext)antlr;
			tree = new Tree(id++, ruleNames.get(rc_antlr.getRuleIndex()), "control_block", null, parent);
		} else {
			TerminalNode n = (TerminalNode)antlr;
			int type = n.getSymbol().getType();
			if(type == 100 || (type >= 51 && type <= 56)){
				tree = new Tree(id++, antlr.getText(), "symbol_or_literal", null, parent);
			} else {
				tree = new Tree(id++, antlr.getText(), "control_block", null, parent);
			}
		}
		
		for(int i = 0; i < antlr.getChildCount(); i++){
			tree.children.add(convAntlrToTree(tree, antlr.getChild(i)));
		}
		return tree;
	}
}
