package siani.districting.readers;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import siani.districting.architecture.geometry.geoshapes.GeoToolsGeoShape;
import siani.districting.architecture.geometry.GeoShape;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  ShapefileReader {

    private ShapefileReader() {}

    public static State read(String path,
                             String name,
                             PrecinctInfoContainer container,
                             String populationCsvPath) throws IOException {
        DataStore store = FileDataStoreFinder.getDataStore(new File(path));
        if (store == null) throw new IOException("can not find store for " + path);
        return createState(store, name, container, populationCsvPath);
    }

    private static State createState(DataStore store,
                                     String name,
                                     PrecinctInfoContainer container,
                                     String populationCsvPath) throws IOException {
        Map<Integer, List<Precinct>> precintsMap = createPrecincts(store, container);
        assignPopulation(precintsMap, populationCsvPath);
        List<District> districtsList = buildDistricts(precintsMap);
        return new State(name, districtsList);
    }

    private static List<District> buildDistricts(Map<Integer, List<Precinct>> precintsMap) {
        List<District> districtsList = new ArrayList<District>();
        precintsMap.keySet().forEach(districtId -> {
            District newDistrict = new District(districtId, precintsMap.get(districtId));
            districtsList.add(newDistrict);
        });
        return districtsList;
    }

    private static void assignPopulation(Map<Integer, List<Precinct>> precinctsMap, String populationCsvPath) throws IOException {
        Map<Object, Object> districtPopulationMap = CsvToMapReader.read(populationCsvPath, true);
        for (Map.Entry<Integer, List<Precinct>> entry : precinctsMap.entrySet()){
            Integer districtId = entry.getKey();
            List<Precinct> precincts = entry.getValue();
            List<Integer> population = PopulationGenerator.generatePopulationPerPrecinct(
                    precincts.size(),
                    (Integer) districtPopulationMap.get(districtId),
                    districtId
            );

            for (int i = 0; i < precincts.size(); i++) {
                precincts.get(i).setPopulation(population.get(i));
            }
        }
    }

    private static Map<Integer, List<Precinct>> createPrecincts(DataStore store, PrecinctInfoContainer container) throws IOException {
        Map<Integer, List<Precinct>> precinctsMap = new HashMap<>();
        SimpleFeatureCollection collection = getCollectionFrom(store);
        try (SimpleFeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                createPrecinct(iterator, precinctsMap, feature);
                for (Property property : feature.getProperties()) {
                    if (propertyIsNotValid(property)) continue;
                    String uniqueId = feature.getAttribute("UNIQUE_ID").toString();
                    String propertyName = property.getName().toString();
                    Integer value = toInt(property.getValue().toString());
                    container.insert(uniqueId, propertyName, value);
                }
            }
        } finally {
            store.dispose();
        }
        return precinctsMap;

    }

    private static boolean propertyIsNotValid(Property property) {
        return property.getName().toString().equals("CONG_DIST") ||
                property.getName().toString().equals("UNIQUE_ID") ||
                property instanceof GeometryAttribute ||
                propertyIsNotNumber(property.getValue().toString());
    }

    private static boolean propertyIsNotNumber(String string) {
        return !string.matches("-?\\d+");
    }

    private static void createPrecinct(SimpleFeatureIterator iterator, Map<Integer, List<Precinct>> precinctsMap, SimpleFeature feature) {
        String uniqueId = feature.getAttribute("UNIQUE_ID").toString();
        int districtId = toInt(feature.getAttribute("CONG_DIST").toString());
        GeoShape geoShape = GeoToolsGeoShape.of((Geometry) feature.getDefaultGeometry());
        Precinct precinct = new Precinct(uniqueId, geoShape);
        putPrecinctInMap(precinct, districtId, precinctsMap);
    }

    private static int toInt(String string) {
        return Integer.parseInt(string);
    }

    private static void putPrecinctInMap(Precinct precinct, int districtId, Map<Integer, List<Precinct>> precinctsMap) {
        if (precinctsMap.get(districtId) == null) {
            List<Precinct> precinctList = new ArrayList<>();
            precinctList.add(precinct);
            precinctsMap.put(districtId, precinctList);
        } else precinctsMap.get(districtId).add(precinct);
    }

    private static SimpleFeatureCollection getCollectionFrom(DataStore store) throws IOException {

        String layerName = store.getTypeNames()[0];
        return store.getFeatureSource(layerName).getFeatures();

    }

}
