package com.company.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.DMNServicesClient;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationClient {

    static Logger logger = LoggerFactory.getLogger(ApplicationClient.class);

    protected DMNServicesClient dmnClient;
    public KieServicesConfiguration configuration;
    public static String containerId;
    public static String namespace;
    public static String name;
    public static List<String[]> sampleData = new ArrayList<>();
    public static AtomicInteger counter = new AtomicInteger(0);

    public static String SERVER_URL = "serverURL";
    public static String USERNAME = "username";
    public static String PASSWORD = "password";
    public static String CONTAINER_ID_PARAMETER_KEY = "containerId";
    public static String NAMESPACE_PARAMETER_KEY = "namespace";
    public static String NAME_PARAMETER_KEY = "name";

    protected void setupClients(KieServicesClient kieServicesClient) {
        this.dmnClient = kieServicesClient.getServicesClient(DMNServicesClient.class);
    }

    public ApplicationClient() {
        String url = System.getProperty(SERVER_URL, "http://localhost:8090/rest/server");
        String username = System.getProperty(USERNAME, "wbadmin");
        String password = System.getProperty(PASSWORD, "wbadmin");

        configuration = KieServicesFactory.newRestConfiguration(url, username, password);
    }

    protected KieServicesClient createDefaultClient() throws Exception {

        configuration.setTimeout(3000);
        configuration.setMarshallingFormat(MarshallingFormat.JSON);

        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(configuration);

        setupClients(kieServicesClient);
        return kieServicesClient;
    }

    public DMNServicesClient getDmnClient() {
        return dmnClient;
    }

    public static void main(String[] args) throws Exception {
        logger.info("Starting kie-server requests");

        ApplicationClient applicationClient = new ApplicationClient();

        containerId = Optional.ofNullable(System.getProperty(CONTAINER_ID_PARAMETER_KEY))
                .orElseThrow(() -> new IllegalStateException("containerId parameter is mandatory"));

        namespace = Optional.ofNullable(System.getProperty(NAMESPACE_PARAMETER_KEY))
                .orElseThrow(() -> new IllegalStateException("namespace parameter is mandatory"));

        name = Optional.ofNullable(System.getProperty(NAME_PARAMETER_KEY))
                .orElseThrow(() -> new IllegalStateException("name parameter is mandatory"));


        loadSampleData();

        applicationClient.createDefaultClient();

        final int parallelism = args.length > 0 ? Integer.valueOf(args[0]) : 1;
        final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        final CyclicBarrier started = new CyclicBarrier(parallelism);
        final Callable<Long> task = () -> {
            started.await();
            final Thread current = Thread.currentThread();
            long executions = 0;
            while (!current.isInterrupted()) {
                evaluateDMNWithPause(applicationClient.getDmnClient());
                executions++;
                if (executions % 1000 == 0) {
                    logger.info(executions + " requests sent");
                }
            }
            return executions;
        };
        final ArrayList<Future<Long>> tasks = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; i++) {
            tasks.add(executor.submit(task));
        }
        executor.shutdown();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tasks.forEach(future -> future.cancel(true));
        }));
    }

    private static void loadSampleData() {
        String text = new Scanner(ApplicationClient.class.getResourceAsStream("sampleData"), "UTF-8").useDelimiter("\\Z").next();

        sampleData = Arrays.stream(text.split("\\n"))
                .map(row -> row.split(","))
                .collect(Collectors.toList());
    }

    private static void evaluateDMNWithPause(DMNServicesClient dmnClient) {
        DMNContext dmnContext = dmnClient.newContext();

        int index = sampleData.size() % counter.getAndIncrement();
        String[] sampleDataRow = sampleData.get(index);

        double fraudAmount = Double.valueOf(sampleDataRow[0]);
        String cardHolderStatus = sampleDataRow[1];
        double incidentCount = Double.valueOf(sampleDataRow[2]);
        double age = Double.valueOf(sampleDataRow[3]);

        dmnContext.set("Fraud Amount", fraudAmount);
        dmnContext.set("Cardholder Status", cardHolderStatus);
        dmnContext.set("Incident Count", incidentCount);
        dmnContext.set("Age", age);
        ServiceResponse<DMNResult> evaluateAll = dmnClient.evaluateAll(containerId, namespace, name, dmnContext);
//        logger.info("result" + evaluateAll.getMsg());
    }
}
