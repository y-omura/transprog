package treeconv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Type {
	private static int next_id = 1;
	public static Map<String, Type> typeMap = new HashMap<>();
	
	public int id;
	public Type parent;
	public List<Type> children = new ArrayList<Type>();
	public String type;
	public String typeType;

	public static Type UNKNOWN = new Type("_", "", null);
	
	public Type(String type, String typeType, Type parent){
		if(type == null){
			throw new NullPointerException();
		}
		
		if(typeMap.containsKey(type)){
			throw new IllegalArgumentException();
		}
		
		this.id = next_id++;
		if(typeType == null){
			typeType = "";
		}
		this.type = type;
		this.typeType = typeType;
		
		this.parent = parent;
		
		typeMap.put(type, this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result
				+ ((typeType == null) ? 0 : typeType.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Type other = (Type) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (typeType == null) {
			if (other.typeType != null)
				return false;
		} else if (!typeType.equals(other.typeType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Type [type=" + type + ", typeType=" + typeType + "]";
	}
}
