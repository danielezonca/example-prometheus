package com.company.client;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
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

    public static String SERVER_URL = "serverURL";
    public static String USERNAME = "username";
    public static String PASSWORD = "password";

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

    static String CONTAINER_1_ID = "function-definition";

    public static void main(String[] args) throws Exception {
        logger.info("Starting kie-server requests");

        ApplicationClient applicationClient = new ApplicationClient();

        KieServicesClient client = applicationClient.createDefaultClient();

        ReleaseId kjar1 = new ReleaseId(
                "com.company", "example-prometheus-kjar",
                "1.0-SNAPSHOT");

        KieContainerResource containerResource = new KieContainerResource(CONTAINER_1_ID, kjar1);

        client.deactivateContainer(CONTAINER_1_ID);
        client.disposeContainer(CONTAINER_1_ID);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_1_ID, containerResource);

        final int parallelism = Integer.valueOf(args[0]);
        final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        final CyclicBarrier started = new CyclicBarrier(parallelism);
        final Callable<Long> task = () -> {
            started.await();
            final Thread current = Thread.currentThread();
            long executions = 0;
            while (!current.isInterrupted()) {
                evaluateDMNWithPause(applicationClient.getDmnClient());
                executions++;
                if(executions % 1000 == 0) {
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

    private static void evaluateDMNWithPause(DMNServicesClient dmnClient) {
        DMNContext dmnContext = dmnClient.newContext();

        ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();

        int a = salaryRandom.nextInt(1000, 100000 / 12);
        int b = salaryRandom.nextInt(1000, 100000 / 12);

        dmnContext.set("a", a);
        dmnContext.set("b", b);
        ServiceResponse<DMNResult> evaluateAll = dmnClient.evaluateAll(CONTAINER_1_ID, dmnContext);
//        logger.info("result" + evaluateAll.getMsg());
    }
}
