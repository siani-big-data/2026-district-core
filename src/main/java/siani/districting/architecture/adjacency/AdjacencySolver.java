package siani.districting.architecture.adjacency;

import org.locationtech.jts.index.strtree.STRtree;
import siani.districting.architecture.geometry.GeoTree;
import siani.districting.architecture.geometry.geotrees.GeoToolsGeoTree;
import siani.districting.architecture.model.Precinct;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AdjacencySolver {
    private final List<Precinct> precincts;
    private GeoTree tree;
    private Set<String> adjacencySet;

    private record Adjacency(String firstId, String secondId) {
        boolean contains(String precinctId) {
            return firstId.equals(precinctId) || secondId.equals(precinctId);
        }

        String otherId(String precinctId) {
            if (firstId.equals(precinctId)) return secondId;
            if (secondId.equals(precinctId)) return firstId;
            throw new IllegalArgumentException("Precinct " + precinctId + " is not part of this adjacency.");
        }
    }
    
    public AdjacencySolver(List<Precinct> precincts) {
        this.precincts = precincts;
        tree = new GeoToolsGeoTree(new STRtree());
        adjacencySet = ConcurrentHashMap.newKeySet();
        buildTree();
        calculateAdjacency();
    }

    public AdjacencySolver(List<Precinct> precincts, Set<String> adjacencySet) {
        this.precincts = precincts;
        tree = new GeoToolsGeoTree(new STRtree());
        this.adjacencySet = adjacencySet;
        buildTree();
    }

    public List<String> getAdjacentsIds(Precinct precinct) {
        return getAdjacentsIds(precinct.id());
    }

    public List<Precinct> getAdjacents(Precinct precinct) {

        List<String> precinctsIds = adjacencySet.stream()
                .map(this::parseAdjacency)
                .filter(adjacency -> adjacency.contains(precinct.id()))
                .map(adjacency -> adjacency.otherId(precinct.id()))
                .toList();

        return precincts.stream().filter(p -> precinctsIds.contains(p.id())).toList();
    }

    public List<String> getAdjacentsIds(String precinctId) {
        return adjacencySet.stream()
                .map(this::parseAdjacency)
                .filter(adjacency -> adjacency.contains(precinctId))
                .map(adjacency -> adjacency.otherId(precinctId))
                .toList();
    }

    private void buildTree() {

        for (Precinct precinct : precincts) {
            this.tree.insert( precinct.boundaries().getBoundingBox(),  precinct);
        }
        tree.build();
    }

    private void calculateAdjacency() {

        precincts.parallelStream().forEach(precinct -> {

            List<Precinct> candidates = (List<Precinct>) tree.query(precinct.boundaries().getBoundingBox());

            for (Precinct candidate : candidates) {

                if (candidate.equals(precinct)) continue;

                if (precinctsIntersects(precinct, candidate)) adjacencySet.add( sortIds(precinct, candidate) );

            }

        });

    }

    private boolean precinctsIntersects(Precinct precinct, Precinct candidate) {
        return precinct.boundaries().intersects(candidate.boundaries());
    }

    private String sortIds(Precinct precinct, Precinct candidate) {
        return precinct.id().compareTo(candidate.id()) < 0
                ? precinct.id() + "," + candidate.id()
                : candidate.id() + "," + precinct.id();
    }


    public boolean areAdjacents(String precinct1, String precinct2) {
        return  adjacencySet.contains( sortIds(precinct1, precinct2) );
    }

    private String sortIds(String precinct1, String precinct2) {
        return precinct1.compareTo(precinct2) < 0
                ? precinct1 + "," + precinct2
                : precinct2 + "," + precinct1;
    }

    private Adjacency parseAdjacency(String adjacency) {
        String[] ids = adjacency.split(",", 2);
        if (ids.length != 2) {
            throw new IllegalArgumentException("Invalid adjacency entry: " + adjacency);
        }
        return new Adjacency(ids[0], ids[1]);
    }

    public Set<String> getAdjacencySet() {
        return adjacencySet;
    }

}
