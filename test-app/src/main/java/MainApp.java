import cz.cuni.mff.d3s.deeco.annotations.processor.AnnotationProcessor;
import cz.cuni.mff.d3s.deeco.annotations.processor.AnnotationProcessorException;
import cz.cuni.mff.d3s.deeco.knowledge.CloningKnowledgeManagerFactory;
import cz.cuni.mff.d3s.deeco.model.runtime.api.RuntimeMetadata;
import cz.cuni.mff.d3s.deeco.model.runtime.custom.RuntimeMetadataFactoryExt;
import cz.cuni.mff.d3s.deeco.runtime.RuntimeFramework;
import cz.kinst.jakub.diploma.udpbroadcast.JavaUDPBroadcast;
import cz.kinst.jakub.diploma.udpbroadcast.UDPRuntimeBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.SocketException;

public class MainApp extends Application {

    public static Label name, others;

    @Override
    public void start(Stage primaryStage) {
        name = new Label();
        name.setText("My Name");
        others = new Label();
        others.setText("Others");

        VBox root = new VBox();
        root.getChildren().add(name);
        root.getChildren().add(others);

        Scene scene = new Scene(root, 300, 250);

        primaryStage.setTitle("JDEECo UDP Test");
        primaryStage.setScene(scene);
        primaryStage.show();

        try {
            startUDPTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static JavaUDPBroadcast mUdpBroadcast;
    private static RuntimeFramework mDEECoRuntime;

    void startUDPTest() throws SocketException, AnnotationProcessorException {
        mUdpBroadcast = new JavaUDPBroadcast();

        UDPRuntimeBuilder builder = new UDPRuntimeBuilder();

        RuntimeMetadata model = RuntimeMetadataFactoryExt.eINSTANCE.createRuntimeMetadata();
        AnnotationProcessor processor = new AnnotationProcessor(RuntimeMetadataFactoryExt.eINSTANCE, model, new CloningKnowledgeManagerFactory());

        processor.process(
                new Device(mUdpBroadcast.getMyIpAddress()), // Components
                DeviceNetworkEnsemble.class // Ensembles
        );
        mDEECoRuntime = builder.build(mUdpBroadcast.getMyIpAddress(), model, mUdpBroadcast);
        mDEECoRuntime.start();

        mUdpBroadcast.startReceivingInBackground();
    }


}