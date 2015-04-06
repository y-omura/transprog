package treeconv;

public class Label {
	public String label;
	public String labelType;
	
	
	public Label(String label, String labelType) {
		super();
		this.label = label;
		this.labelType = labelType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((labelType == null) ? 0 : labelType.hashCode());
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
		Label other = (Label) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (labelType == null) {
			if (other.labelType != null)
				return false;
		} else if (!labelType.equals(other.labelType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Label [label=" + label + ", labelType=" + labelType + "]";
	}
}
