package treeconv;

import java.util.ArrayList;
import java.util.List;

public class Tree {
	public int id;
	public Label label;
	public Type type;
	public Tree parent;
	public List<Tree> children = new ArrayList<Tree>();
	
	public Tree(int id, String label, String labelType, String type, Tree parent){
		this.id = id;
		this.label = new Label(label, labelType);
		if(type == null){
			this.type = Type.UNKNOWN;
		} else {
			this.type = Type.typeMap.get(type);
			if(this.type == null){
				throw new IllegalArgumentException("No such type: "+type);
			}
		}
		this.parent = parent;
	}
	
	/**
	 * copy constructor with new id and parent and empty children
	 * @param id
	 * @param tree
	 */
	public Tree(int id, Tree tree, Tree parent){
		this.id = id;
		this.label = tree.label;
		this.type = tree.type;
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "Tree [id=" + id + ", label=" + label + ", type=" + type + ", children=" + children
				+ "]";
	}
}
