/*
 * Copyright 2016 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.server.integrationtests.cluster;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.codehaus.cargo.container.deployer.DeployableMonitor;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServerStateInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesException;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.cluster.controller.KieServerClusterController;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.controller.ContainerRemoteController;
import org.kie.server.integrationtests.controller.client.KieServerMgmtControllerClient;
import org.kie.server.integrationtests.shared.basetests.RestOnlyBaseIntegrationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClusterBaseTest extends RestOnlyBaseIntegrationTest {

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    private static final long SERVICE_TIMEOUT = 30000L;
    private static final long TIMEOUT_BETWEEN_CALLS = 200L;

    protected static Logger logger = LoggerFactory.getLogger(ClusterBaseTest.class);

    protected KieServicesClient primaryClient, secondaryClient;
    protected static String primaryKieServerUrl, secondaryKieServerUrl;
    protected static String serversUrl;

    protected ServerTemplate kieServerTemplate;

    protected KieServerMgmtControllerClient mgmtControllerClient;

    private static boolean secondaryKieServerIsDeployed = true;
    private static boolean primaryKieServerIsDeployed = true;
    
    protected static KieServerClusterController primaryServerController, secondaryServerController;

    @BeforeClass
    public static void configureServersUrls() {
        primaryKieServerUrl = TestConfig.getKieServerHttpUrl();
        secondaryKieServerUrl = System.getProperty(ClusterTestConstants.SECONDARY_URL_PROPERTY);
        serversUrl = primaryKieServerUrl + "|" + secondaryKieServerUrl;
        
        //TODO: create better constructor
        primaryServerController = new KieServerClusterController("wildfly10x", Integer.toString(9990),"38080", System.getProperty("cluster.configuration.dir") + "/default-kie-server/deployments/kie-server-services.war", primaryKieServerUrl, System.getProperty("cluster.configuration.dir")+"/default-kie-server");
        secondaryServerController =  new KieServerClusterController("wildfly10x", Integer.toString(9990 + 150),"38230",System.getProperty("cluster.configuration.dir") + "/cluster-kie-server/deployments/kie-server-services.war", secondaryKieServerUrl, System.getProperty("cluster.configuration.dir")+"/cluster-kie-server");
    }

    @Before
    public void setupClustering() throws Exception {

        setMgmtControllerClient();
        setServerTeplates();

        disposeAllContainersInMgmtClient();
        createClientsForServerInstances();
        disposeContainersInAllClinets();
    }
    
    private void setMgmtControllerClient() {
        if (TestConfig.isLocalServer()) {
            mgmtControllerClient = new KieServerMgmtControllerClient(TestConfig.getControllerHttpUrl(), null, null);
        } else {
            mgmtControllerClient = new KieServerMgmtControllerClient(TestConfig.getControllerHttpUrl(), TestConfig.getUsername(), TestConfig.getPassword());
        }
        mgmtControllerClient.setMarshallingFormat(marshallingFormat);
    }
    
    private void setServerTeplates() {
        Collection<ServerTemplate> serverTemplates = mgmtControllerClient.listServerTemplates();
        if (serverTemplates.size() == 1) {
            kieServerTemplate = serverTemplates.iterator().next();
        }
    }

    private void disposeAllContainersInMgmtClient() {
        Collection<ServerTemplate> serverTemplates = mgmtControllerClient.listServerTemplates();
        serverTemplates.stream().forEach((serverTemplate) -> {
            Collection<ContainerSpec> containersSpec = mgmtControllerClient.listContainerSpec(serverTemplate.getId());
            containersSpec.stream().forEach((containerSpec) -> {
                mgmtControllerClient.deleteContainerSpec(serverTemplate.getId(), containerSpec.getId());
            });
        });
    }

    protected void createClientWithDefaultLoadBalancer() throws Exception {
        //cient use defaul load balancer and is used on template one
        client = createDefaultClient(serversUrl);
    }

    protected void createClientsForServerInstances() throws Exception {
        primaryClient = createDefaultClient(primaryKieServerUrl);
        secondaryClient = createDefaultClient(secondaryKieServerUrl);
    }

    @After
    public void tearDownClustering() {
        mgmtControllerClient.close();
        if(!primaryServerController.isDeployStarted()) {
            primaryServerController.startDeploy();
        }
        if(!secondaryServerController.isDeployStarted()) {
            secondaryServerController.startDeploy();
        }
    }

    protected void disposeContainersInClinets(KieServicesClient... clients) {
        for (KieServicesClient kieServiceClient : clients) {
            try {
            ServiceResponse<KieContainerResourceList> response = kieServiceClient.listContainers();
            assertEquals(ServiceResponse.ResponseType.SUCCESS, response.getType());
            List<KieContainerResource> containers = response.getResult().getContainers();
            if (containers != null) {
                for (KieContainerResource container : containers) {
                    kieServiceClient.disposeContainer(container.getContainerId());
                }
            }
            } catch (KieServicesException ex) {
                assertTrue("Failed: "+ex.getMessage(), ex.getMessage().contains("404"));
            }
        }
    }

    protected void disposeContainersInAllClinets() {
        disposeContainersInClinets(primaryClient, secondaryClient);
    }

    
    //TODO: turn off / on should use defaul server controller inicialized in @Before
    //can be replaced by KieServerCluseterController + start  & stop??
    protected void turnOnSecondaryServer() throws InterruptedException, MalformedURLException {
        logger.info("Turning on {}", secondaryKieServerUrl);System.out.println("\n***** Turn on BRAVO\n");
        secondaryServerController.startDeploy();
        
        try {
            KieServerInfo result = secondaryClient.getServerInfo().getResult();
            if (result == null) {
                fail("Server info is null");
            }
            System.out.println("Results: " + result);
        } catch (Exception ex) {
            fail("Server is not running");
        }
        
        
//        if (!secondaryKieServerIsDeployed) {
//            System.out.println("\n***** Turn on BRAVO\n");
//            DeployableMonitor dm = new URLDeployableMonitor(new URL(secondaryKieServerUrl), 60000);
//            ContainerRemoteController remoteControllerBravo = new ContainerRemoteController("wildfly10x", Integer.toString(9990 + 150));
//            remoteControllerBravo.deployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/cluster-kie-server/deployments/kie-server-services.war", dm);
//            secondaryKieServerIsDeployed = true;
//            startSynchronization(secondaryKieServerUrl, secondaryClient);
//        }
    }

    protected void turnOffSecondaryServer() {
        logger.info("Turning off {}", secondaryKieServerUrl); System.out.println("\n***** Turn off BRAVO\n");
        secondaryServerController.stopDeploy();
        try {
            KieServerInfo result = secondaryClient.getServerInfo().getResult();
            if (result != null) {
                fail("Server is running");
            }
        } catch (Exception ex) {
            //OK
        }
        
//        if (secondaryKieServerIsDeployed) {
//            System.out.println("\n***** Turn off BRAVO\n");
//            ContainerRemoteController remoteControllerBravo = new ContainerRemoteController("wildfly10x", Integer.toString(9990 + 150));
//            remoteControllerBravo.undeployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/cluster-kie-server/deployments/kie-server-services.war");
//            secondaryKieServerIsDeployed = false;
//        }
    }

    protected void turnOnPrimaryServer() throws InterruptedException, MalformedURLException {
        logger.info("Turning on {}", primaryKieServerUrl);System.out.println("\n***** Turn on CHARLIE\n");
        primaryServerController.startDeploy();
        
        try {
            KieServerInfo result = secondaryClient.getServerInfo().getResult();
            if (result == null) {
                fail("Server info is null");
            }
            System.out.println("Results: " + result);
        } catch (Exception ex) {
            fail("Server is not running");
        }
        
        
//        if (!primaryKieServerIsDeployed) {
//            System.out.println("\n***** Turn on CHARLIE\n");
//            
//            DeployableMonitor dm = new URLDeployableMonitor(new URL(primaryKieServerUrl), 60000);
//            ContainerRemoteController remoteControllerCharlie = new ContainerRemoteController("wildfly10x", Integer.toString(9990));
//            remoteControllerCharlie.deployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/default-kie-server/deployments/kie-server-services.war", dm);
//            primaryKieServerIsDeployed = true;
//            startSynchronization(primaryKieServerUrl, primaryClient);
//        }
    }

    protected void turnOffPrimaryServer() {
        logger.info("Turning off {}", primaryKieServerUrl);System.out.println("\n***** Turn off CHARLIE\n");
        primaryServerController.stopDeploy();
        
        try {
            KieServerInfo result = primaryClient.getServerInfo().getResult();
            if (result != null) {
                fail("Server is running");
            }
        } catch (Exception ex) {
            //OK
        }
        
//        if (primaryKieServerIsDeployed) {
//            System.out.println("\n***** Turn off CHARLIE\n");
//            ContainerRemoteController remoteControllerCharlie = new ContainerRemoteController("wildfly10x", Integer.toString(9990));
//            remoteControllerCharlie.undeployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/default-kie-server/deployments/kie-server-services.war");
//            primaryKieServerIsDeployed = false;
//        }
    }


    protected KieServicesClient createDefaultClient(String url) throws Exception {
        KieServicesConfiguration config;
        if (TestConfig.isLocalServer()) {
            config = KieServicesFactory.newRestConfiguration(url, null, null);
        } else {
            config = KieServicesFactory.newRestConfiguration(url, TestConfig.getUsername(), TestConfig.getPassword());
        }
        addExtraCustomClasses(extraClasses);
        KieServicesClient client = waitToClientCreated(primaryClient, config);
        return client;//createDefaultClient(config, marshallingFormat);
    }

    
    //TODO: remove wainting loops
    private KieServicesClient waitToClientCreated(KieServicesClient client, KieServicesConfiguration conf) throws InterruptedException, TimeoutException {
        long timeoutTime = Calendar.getInstance().getTimeInMillis() + SERVICE_TIMEOUT;
        while (Calendar.getInstance().getTimeInMillis() < timeoutTime) {
            try {
                client = createDefaultClient(conf, marshallingFormat);
            } catch (Exception ex) {
                Thread.sleep(TIMEOUT_BETWEEN_CALLS);
                continue;
            }
            if (client != null) {
                return client;
            }
            Thread.sleep(TIMEOUT_BETWEEN_CALLS);
        }
        throw new TimeoutException("Synchronization failed for defined timeout: " + SERVICE_TIMEOUT + " milliseconds.");

    }
}
