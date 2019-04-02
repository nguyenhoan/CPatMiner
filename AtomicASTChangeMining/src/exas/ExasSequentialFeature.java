package exas;

import java.util.ArrayList;
import java.util.List;

public class ExasSequentialFeature extends ExasFeature {
	private ArrayList<ExasSingleFeature> sequence = new ArrayList<ExasSingleFeature>();
	
	/*public ExasSequentialFeature(ArrayList<ExasSingleFeature> sequence) {
		this.sequence = new ArrayList<ExasSingleFeature>(sequence);
		ExasFeature pre = sequence.get(0);
		int i = 1;
		while (i < sequence.size())
		{
			ExasSingleFeature s = sequence.get(i);
			if (pre.next.containsKey(s))
			{
				pre = pre.next.get(s);
			}
			else
			{
				pre = new ExasSequentialFeature(sequence.subList(0, i+1), pre, s);
			}
			i++;
		}
	}*/
	
	public ExasSequentialFeature(List<ExasSingleFeature> sequence, ExasFeature pre, ExasSingleFeature s) {
		super();
		this.sequence = new ArrayList<ExasSingleFeature>(sequence);
		pre.next.put(s, this);
		if (pre.next.size() > numOfBranches)
			numOfBranches = pre.next.size();
	}

	public ArrayList<ExasSingleFeature> getSequence() {
		return sequence;
	}

	public void setSequence(ArrayList<ExasSingleFeature> sequence) {
		this.sequence = sequence;
	}

	@Override
	public int getFeatureLength() {
		return sequence.size();
	}

	@Override
	public String toString() {
		return sequence.toString();
	}

	@Override
    public int hashCode() {
        if (hash == 0 && sequence.size() > 0) {
            for (int i = 0; i < sequence.size(); i++) {
                hash = 31 * hash + sequence.get(i).hashCode();
            }
        }
        return hash;
    }

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj instanceof ExasSequentialFeature) {
			ExasSequentialFeature other = (ExasSequentialFeature) obj;
			if (this.getFeatureLength() != other.getFeatureLength())
				return false;
			for (int i = 0; i < getFeatureLength(); i++) {
				if (!this.sequence.get(i).equals(other.sequence.get(i)))
					return false;
			}
			return true;
		}	
		return false;
	}
}
