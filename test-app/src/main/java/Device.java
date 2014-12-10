import cz.cuni.mff.d3s.deeco.annotations.Component;
import cz.cuni.mff.d3s.deeco.annotations.In;
import cz.cuni.mff.d3s.deeco.annotations.PeriodicScheduling;
import cz.cuni.mff.d3s.deeco.annotations.Process;
import javafx.application.Platform;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Component
public class Device implements Serializable {

    public String name;
    public Set<String> otherDevices = new HashSet<>();

    public Device(String name) {
        this.name = name;
    }

    @Process
    @PeriodicScheduling(period = 1000)
    public static void update(@In("name") String name, @In("otherDevices") Set<String> otherDevices) {

        StringBuilder othersBuilder = new StringBuilder();
        for (String s : otherDevices) {
            othersBuilder.append(s + " ");
        }
        String others = othersBuilder.toString();

        Platform.runLater(() -> {
            MainApp.name.setText(name);
            MainApp.others.setText(others);
        });
    }
}
