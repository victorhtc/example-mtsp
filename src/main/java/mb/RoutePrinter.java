package mb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.io.FileUtils;
import org.primefaces.event.map.MarkerDragEvent;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;
import org.primefaces.model.map.Polyline;

import br.com.vcosta.mtsp.MultipleVehicleRouteSolver;
import br.com.vcosta.mtsp.Place;
import br.com.vcosta.mtsp.clustering.ClusteringMultipleVehicleRouteSolver;

@ManagedBean
@SessionScoped
public class RoutePrinter {

    @Getter @Setter String center = "-16.717362338206847,-49.275906921418176";
    @Getter @Setter MapModel mapModel = new DefaultMapModel();
    @Getter @Setter List<String> colors = new ArrayList<String>();
    @Getter @Setter List<Place> occurrences;
    @Getter @Setter Place vehicle1;
    @Getter @Setter Place vehicle2;
    @Getter @Setter String vehicle1Plate = "KKK-1234";
    @Getter @Setter String vehicle2Plate = "LNP-0808";

    @PostConstruct
    public void pageLoad() {
        setColorsLineColors();
        loadOcurrences();
        setVehicles();
        drawRoutes();
    }

    public void onMarkerDrop(MarkerDragEvent event) {
        String plate = event.getMarker().getData().toString();
        if (plate.equals(vehicle1Plate)) {
            vehicle1 =
                    new Place(vehicle1Plate, event.getMarker().getLatlng().getLat(), event.getMarker().getLatlng()
                            .getLng());
        } else if (plate.equals(vehicle2Plate)) {
            vehicle2 =
                    new Place(vehicle2Plate, event.getMarker().getLatlng().getLat(), event.getMarker().getLatlng()
                            .getLng());
        }
        drawRoutes();
    }

    private void setColorsLineColors() {
        colors.add("#0000FF");
        colors.add("#FF0000");
        colors.add("#FF00FF");
        colors.add("#00FF00");
        colors.add("#CCFF00");
        colors.add("#FFFFFF");
    }

    private void loadOcurrences() {
        this.occurrences = new ArrayList<Place>();
        try {
            List<String> lines = FileUtils.readLines(new File(getClass().getResource("/occurrences.csv").toURI()));
            lines = lines.subList(0, 199);
            for (String line : lines) {
                String[] split = line.split(";");
                occurrences.add(new Place(Double.parseDouble(split[0]), (Double.parseDouble(split[1]))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.shuffle(occurrences);
    }

    private void setVehicles() {
        vehicle1 = new Place(vehicle1Plate, -16.723090595125004, -49.30794906144388);
        vehicle2 = new Place(vehicle2Plate, -16.668566529523396, -49.18289160319341);
    }

    private void drawRoutes() {
        mapModel.getMarkers().clear();
        mapModel.getPolylines().clear();

        Map<Place, List<Place>> routes = calculateRoutes();

        int count = 0;
        for (Entry<Place, List<Place>> entry : routes.entrySet()) {
            Polyline polyline = new Polyline();
            LatLng veiculo = new LatLng(entry.getKey().getLat(), entry.getKey().getLng());
            Marker marker =
                    new Marker(veiculo, entry.getKey().getData().toString(), entry.getKey().getData(), "car.png");
            marker.setDraggable(true);
            mapModel.addOverlay(marker);
            polyline.getPaths().add(veiculo);
            for (Place local : entry.getValue()) {
                LatLng ocorrencia = new LatLng(local.getLat(), local.getLng());
                mapModel.addOverlay(new Marker(ocorrencia));
                polyline.getPaths().add(ocorrencia);
            }
            polyline.setStrokeWeight(3);
            polyline.setStrokeColor(colors.get(count++));
            mapModel.addOverlay(polyline);
        }
    }

    private Map<Place, List<Place>> calculateRoutes() {
        MultipleVehicleRouteSolver mtsp = new ClusteringMultipleVehicleRouteSolver();
        mtsp.addVehicle(vehicle1);
        mtsp.addVehicle(vehicle2);
        mtsp.addAllOccurrences(occurrences);
        Map<Place, List<Place>> routes = mtsp.calculateRoutes();
        // Do not list the vehicle as an occurrence
        for (Entry<Place, List<Place>> entry : routes.entrySet())
            entry.getValue().remove(entry.getKey());
        return routes;
    }

}
