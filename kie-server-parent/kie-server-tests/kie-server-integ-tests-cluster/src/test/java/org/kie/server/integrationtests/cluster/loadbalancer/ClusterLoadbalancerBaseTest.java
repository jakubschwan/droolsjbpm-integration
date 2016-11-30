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
package org.kie.server.integrationtests.cluster.loadbalancer;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.runtime.conf.MergeMode;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.SolverServicesClient;
import org.kie.server.client.UIServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.integrationtests.cluster.ClusterBaseTest;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.CONTAINER_ID;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.CONTAINER_NAME;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

public abstract class ClusterLoadbalancerBaseTest extends ClusterBaseTest {

    protected static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project", "1.0.0.Final");
    protected static KieContainer kieContainer;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);
    }
    
    @Before
    public void beforeTests() throws Exception {
        Map<Capability, ContainerConfig> config = new HashMap<>();
        config.put(Capability.PROCESS, new ProcessConfig(RuntimeStrategy.SINGLETON.toString(), "", "", MergeMode.MERGE_COLLECTIONS.toString()));

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, templateOne, releaseId, KieContainerStatus.STOPPED, config);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerSpec);

        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);

        //make sure, that contianer is deployed on both servers
        KieServerSynchronization.waitForKieServerSynchronization(clientBravo, 1);
        KieServerSynchronization.waitForKieServerSynchronization(clientCharlie, 1);
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
    }

    //client with defaultLoadBalancer
    protected ProcessServicesClient processClient;
    protected UserTaskServicesClient taskClient;
    protected QueryServicesClient queryClient;
    protected JobServicesClient jobServicesClient;
    protected DocumentServicesClient documentClient;
    protected UIServicesClient uiServicesClient;
    protected SolverServicesClient solverClient;

    @Before
    public void setupClusterClients() throws Exception {
        client = createDefaultClient(serversUrl);
        processClient = client.getServicesClient(ProcessServicesClient.class);
        taskClient = client.getServicesClient(UserTaskServicesClient.class);
        queryClient = client.getServicesClient(QueryServicesClient.class);
        jobServicesClient = client.getServicesClient(JobServicesClient.class);
        documentClient = client.getServicesClient(DocumentServicesClient.class);
        uiServicesClient = client.getServicesClient(UIServicesClient.class);
        solverClient = client.getServicesClient(SolverServicesClient.class);
    }
}
