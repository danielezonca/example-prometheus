package com.company;

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

    private static Logger logger = LoggerFactory.getLogger(ApplicationClient.class);

    private final int parallelism;
    private final Controller controller;
    private DMNServicesClient dmnClient;
    private KieServicesConfiguration configuration;

    private ArrayList<Future<Long>> tasks;

    ApplicationClient(int parallelism, Controller controller) {
        this.parallelism = parallelism;
        this.controller = controller;
        configuration = KieServicesFactory.newRestConfiguration("http://localhost:8090/rest/server", "wbadmin", "wbadmin");
    }

    private void setupClients(KieServicesClient kieServicesClient) {
        this.dmnClient = kieServicesClient.getServicesClient(DMNServicesClient.class);
    }

    private KieServicesClient createDefaultClient() {
        configuration.setTimeout(3000);
        configuration.setMarshallingFormat(MarshallingFormat.JSON);

        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(configuration);

        setupClients(kieServicesClient);
        return kieServicesClient;
    }

    String CONTAINER_1_ID = "function-definition";

    public void run() throws Exception {
        logger.info("Starting kie-server requests");

        KieServicesClient client = createDefaultClient();

        ReleaseId kjar1 = new ReleaseId(
                "com.company", "example-prometheus-kjar",
                "1.0-SNAPSHOT");

        KieContainerResource containerResource = new KieContainerResource(CONTAINER_1_ID, kjar1);

        client.deactivateContainer(CONTAINER_1_ID);
        client.disposeContainer(CONTAINER_1_ID);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_1_ID, containerResource);

        final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        final CyclicBarrier started = new CyclicBarrier(parallelism);
        final Callable<Long> task = () -> {
            started.await();
            final Thread current = Thread.currentThread();
            long executions = 0;
            while (!current.isInterrupted()) {
                ApplicationClient.this.evaluateDMNWithPause();
                executions++;
                if (executions % 1000 == 0) {
                    logger.info(executions + " requests sent");
                }
            }
            return executions;
        };
        tasks = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; i++) {
            tasks.add(executor.submit(task));
        }
    }

    void stop() {
        tasks.forEach(future -> future.cancel(true));
    }

    private void evaluateDMNWithPause() {
        DMNContext dmnContext = dmnClient.newContext();

        ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();

        int a = salaryRandom.nextInt(1000, 100000 / 12);
        int b = salaryRandom.nextInt(1000, 100000 / 12);

        dmnContext.set("a", a);
        dmnContext.set("b", b);
        ServiceResponse<DMNResult> evaluateAll = dmnClient.evaluateAll(CONTAINER_1_ID, dmnContext);

        DMNResult result = evaluateAll.getResult();
//        DMNDecisionResult sum = result.getDecisionResultById("Sum");
//        System.out.println("sum = " + sum);
//        logger.info(sum.toString());
    }
}
