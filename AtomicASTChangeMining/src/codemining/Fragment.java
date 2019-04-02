package codemining;

import java.util.Map;
import java.util.Set;

public interface Fragment {

	String getId();

	Map<? extends Feature, Integer> getVector();

	double getVectorLength();

	Set<Bucket> getBuckets();

	void setBuckets(Set<Bucket> buckets);

	Set<Fragment> getClones();

	void setClones(Set<Fragment> clones);

	double distance(Fragment other);

	int getSize();
}
