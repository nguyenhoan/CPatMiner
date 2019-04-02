package codemining;

import java.util.HashMap;

public class ChangeLocation {
	private static HashMap<Integer, HashMap<Integer, HashMap<Integer, ChangeLocation>>> changeLocations = new HashMap<>();

	private int project, revision, location;

	public ChangeLocation(int project, int revision, int location) {
		this.project = project;
		this.revision = revision;
		this.location = location;
	}

	public static ChangeLocation getChangeLocation(int project, int revision,
			int location) {
		HashMap<Integer, HashMap<Integer, ChangeLocation>> map1 = changeLocations
				.get(project);
		if (map1 == null) {
			map1 = new HashMap<>();
			changeLocations.put(project, map1);
		}
		HashMap<Integer, ChangeLocation> map2 = map1.get(revision);
		if (map2 == null) {
			map2 = new HashMap<>();
			map1.put(revision, map2);
		}
		ChangeLocation cl = map2.get(location);
		if (cl == null)
			cl = new ChangeLocation(project, revision, location);
		map2.put(location, cl);
		return cl;
	}

	@Override
	public String toString() {
		return this.project + " " + this.revision + " " + this.location;
	}
}
