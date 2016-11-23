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
package org.kie.server.integrationtests.clustering;

import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.codehaus.cargo.container.deployer.DeployableMonitor;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieServerStateInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesException;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.controller.ContainerRemoteController;
import org.kie.server.integrationtests.controller.client.KieServerMgmtControllerClient;
import org.kie.server.integrationtests.shared.basetests.RestOnlyBaseIntegrationTest;
import org.kie.server.services.impl.storage.KieServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClusterBaseTest extends RestOnlyBaseIntegrationTest {

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    protected static Logger logger = LoggerFactory.getLogger(ClusterBaseTest.class);

    //only 3 servers, holded in own object (no collection)
    protected KieServicesClient clientAlpha, clientBravo, clientCharlie;
    protected static String kieServerUrlAlpha, kieServerUrlBravo, kieServerUrlCharlie;
    protected static String serversUrl;

    protected ServerTemplate templateOne, templateTwo;

    protected KieServerMgmtControllerClient mgmtControllerClient;

    @BeforeClass
    public static void startServers() {
        kieServerUrlAlpha = TestConfig.getKieServerHttpUrl();
        kieServerUrlBravo = System.getProperty("kie.server.cluster.one.http.url");
        kieServerUrlCharlie = System.getProperty("kie.server.cluster.two.http.url");
        serversUrl = kieServerUrlBravo + "|" + kieServerUrlCharlie;
    }

    @Before
    public void setupClustering() throws Exception {
        turnOnBravoServer();
        turnOnCharlieServer();
        startSynchronization(kieServerUrlBravo, clientBravo);
        startSynchronization(kieServerUrlCharlie, clientCharlie);

        setMgmtControllerClient();
        setServerTeplates();

        disposeAllContainersInCluster();
        createClientsForServers();
        disposeAllClusterContainers();
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
        if (serverTemplates.size() == 2) {
            Iterator<ServerTemplate> iterator = serverTemplates.iterator();
            ServerTemplate serverTemplate = iterator.next();
            if (serverTemplate.getId().equals("templateOne")) {
                templateOne = serverTemplate;
            } else {
                templateTwo = serverTemplate;
            }
            if (templateOne == null) {
                templateOne = iterator.next();
            } else {
                templateTwo = iterator.next();
            }
        }
    }

    private void disposeAllContainersInCluster() {
        Collection<ServerTemplate> serverTemplates = mgmtControllerClient.listServerTemplates();
        serverTemplates.stream().forEach((serverTemplate) -> {
            Collection<ContainerSpec> containersSpec = mgmtControllerClient.listContainerSpec(serverTemplate.getId());
            containersSpec.stream().forEach((containerSpec) -> {
                mgmtControllerClient.deleteContainerSpec(serverTemplate.getId(), containerSpec.getId());
            });
        });
    }

    protected void createClientWithDefaultLoadBalancer() throws Exception {
        //cient use defaul load balancer and is used on template A
        client = createDefaultClient(serversUrl);
    }

    protected void createClientsForServers() throws Exception {
        clientAlpha = createDefaultClient(kieServerUrlAlpha);
        clientBravo = createDefaultClient(kieServerUrlBravo);
        clientCharlie = createDefaultClient(kieServerUrlCharlie);
    }

    @After
    public void tearDownClustering() {
        mgmtControllerClient.close();
    }

    //delete this??
    protected void disposeAllClusterContainers(KieServicesClient... clients) {
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
                assertTrue(ex.getMessage().startsWith("404"));
            }
        }
    }

    protected void disposeAllClusterContainers() {
        disposeAllClusterContainers(clientAlpha, clientBravo, clientCharlie);
    }

    private static boolean bravoKieServerIsDeployed = true;
    private static boolean charliKieServerIsDeployed = true;

    protected void turnOnBravoServer() throws InterruptedException, TimeoutException, Exception {
        if (!bravoKieServerIsDeployed) {
            System.out.println("\n***** Turn on BRAVO\n");
            DeployableMonitor dm = new URLDeployableMonitor(new URL(kieServerUrlBravo), 60000);
            ContainerRemoteController remoteControllerBravo = new ContainerRemoteController("wildfly10x", Integer.toString(9990 + 150));
            remoteControllerBravo.deployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/cluster1/deployments/kie-server-services.war", dm);
            bravoKieServerIsDeployed = true;
            startSynchronization(kieServerUrlBravo, clientBravo);
        }
    }

    protected void turnOffBravoServer() throws Exception {
        if (bravoKieServerIsDeployed) {
            System.out.println("\n***** Turn off BRAVO\n");
            ContainerRemoteController remoteControllerBravo = new ContainerRemoteController("wildfly10x", Integer.toString(9990 + 150));
            remoteControllerBravo.undeployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/cluster1/deployments/kie-server-services.war");
            bravoKieServerIsDeployed = false;
        }
    }

    protected void turnOnCharlieServer() throws Exception {
        if (!charliKieServerIsDeployed) {
            System.out.println("\n***** Turn on CHARLIE\n");
            DeployableMonitor dm = new URLDeployableMonitor(new URL(kieServerUrlCharlie), 60000);
            ContainerRemoteController remoteControllerCharlie = new ContainerRemoteController("wildfly10x", Integer.toString(9990 + 300));
            remoteControllerCharlie.deployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/cluster2/deployments/kie-server-services.war", dm);
            charliKieServerIsDeployed = true;
            startSynchronization(kieServerUrlCharlie, clientCharlie);
        }
    }

    protected void turnOffCharlieServer() throws Exception {
        if (charliKieServerIsDeployed) {
            System.out.println("\n***** Turn off CHARLIE\n");
            ContainerRemoteController remoteControllerCharlie = new ContainerRemoteController("wildfly10x", Integer.toString(9990 + 300));
            remoteControllerCharlie.undeployWarFile("kie-server-services", System.getProperty("cluster.configuration.dir") + "/cluster2/deployments/kie-server-services.war");
            charliKieServerIsDeployed = false;
        }
    }

    private void startSynchronization(String serverUrl, KieServicesClient serverClient) throws Exception {
        long SERVICE_TIMEOUT = 30000;
        long timeoutTime = Calendar.getInstance().getTimeInMillis() + SERVICE_TIMEOUT;
        while (Calendar.getInstance().getTimeInMillis() < timeoutTime) {
            if (serverClient == null) {
                try {
                    serverClient = createDefaultClient(serverUrl);
                } catch (KieServicesException ex) {
                    Thread.sleep(1000l);
                    continue;
                }
            }
            ServiceResponse<KieServerStateInfo> response = serverClient.getServerState();
            if(response.getType().equals(ServiceResponse.ResponseType.FAILURE) || response.getResult() == null) {
                Thread.sleep(1000l);
                continue;
            }
            else {
                try {
                    serverClient = createDefaultClient(serverUrl);
                } catch (KieServicesException ex) {
                    Thread.sleep(1000l);
                    continue;
                }
            }
            if (!serverClient.getServerInfo().getMsg().startsWith("404")) {
                return;
            }
            Thread.sleep(1000l);
        }
    }

    protected KieServicesClient createDefaultClient(String url) throws Exception {
        KieServicesConfiguration config;
        if (TestConfig.isLocalServer()) {
            config = KieServicesFactory.newRestConfiguration(url, null, null);
        } else {
            config = KieServicesFactory.newRestConfiguration(url, TestConfig.getUsername(), TestConfig.getPassword());
        }
        addExtraCustomClasses(extraClasses);
        return createDefaultClient(config, marshallingFormat);
    }
    
    /***??
    protected static void repeatIfNotFound(Callable<?> func) throws Exception {
        try {
            func.call();
        } catch (KieServicesException ex ) {
            if(ex.getMessage().startsWith("404")) {
                func.call();
            }
        }
    }
*/
}
