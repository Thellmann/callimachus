package org.callimachusproject.engine.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;

public class ClusterCounter extends RDFEventPipe {
	private final Map<String, Set<String>> clusters = new HashMap<String, Set<String>>();

	public ClusterCounter(RDFEventReader reader) {
		super(reader);
	}

	public int getNumberOfVariableClusters(String... excluding) {
		HashSet<Set<String>> set = new HashSet<Set<String>>(clusters.size());
		loop: for (Set<String> cluster : clusters.values()) {
			for (String exclude : excluding) {
				if (cluster.contains(exclude))
					continue loop;
			}
			set.add(cluster);
		}
		return set.size();
	}

	public Set<Set<String>> getClusters() {
		return new HashSet<Set<String>>(clusters.values());
	}

	public Set<String> getSmallestCluster(String... excluding) {
		if (clusters.isEmpty())
			return Collections.emptySet();
		Set<String> smallest = null;
		loop: for (Set<String> cluster : new HashSet<Set<String>>(clusters.values())) {
			for (String exclude : excluding) {
				if (cluster.contains(exclude))
					continue loop;
			}
			if (smallest == null || cluster.size() < smallest.size()) {
				smallest = cluster;
			}
		}
		return new TreeSet<String>(smallest);
	}

	@Override
	public String toString() {
		return getClusters().toString();
	}

	@Override
	protected void process(RDFEvent event) throws RDFParseException {
		add(event);
		if (event.isTriplePattern()) {
			TriplePattern tp = event.asTriplePattern();
			Set<String> vars = new HashSet<String>(2);
			vars.add(tp.getSubject().stringValue());
			if (!tp.getObject().isLiteral()) {
				vars.add(tp.getObject().stringValue());
			}
			Set<String> cluster = new HashSet<String>();
			for (String var : vars) {
				Set<String> set = clusters.get(var);
				if (cluster == null && set == null) {
					cluster = new HashSet<String>();
					cluster.add(var);
					clusters.put(var, cluster);
				} else if (cluster == null) {
					cluster = set;
				} else if (set == null) {
					cluster.add(var);
					clusters.put(var, cluster);
				} else{
					cluster.addAll(set);
					for (String key : set) {
						clusters.put(key, cluster);
					}
				}
			}
		}
	}

}
