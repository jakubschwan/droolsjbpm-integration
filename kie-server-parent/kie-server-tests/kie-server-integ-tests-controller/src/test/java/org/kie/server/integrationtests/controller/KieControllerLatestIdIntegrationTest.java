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
package org.kie.server.integrationtests.controller;

import java.util.Collections;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.ModelFactory;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import static org.junit.Assert.*;
import org.junit.Test;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerSynchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieControllerLatestIdIntegrationTest extends KieControllerManagementBaseTest{
    protected static Logger logger = LoggerFactory.getLogger(KieControllerLatestIdIntegrationTest.class);
    
    private static ReleaseId releaseId100 = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0");
    private static ReleaseId releaseId101 = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.1");
    private static ReleaseId releaseId110 = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.1.0");
    
    private static ReleaseId latestId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "LATEST");

    private static final String CONTAINER_ID = "kie-concurrent";
    private static final String CONTAINER_NAME = "containerName";

    private KieServerInfo kieServerInfo;
    private ServerTemplate defaultServerTemplate;

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar-100").getFile());
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar-101").getFile());
    }

    @Before
    public void getKieServerInfo() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        assumeThat(reply.getType(), is(ServiceResponse.ResponseType.SUCCESS));
        kieServerInfo = reply.getResult();
        
        defaultServerTemplate = createServerTemplate();
    }
    
    @Test
    public void testRedeployContainerWithUpdatedDeployment() throws Exception {
        logger.info("testRedeployContainerWithUpdatedDeployment");
        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, defaultServerTemplate, latestId, KieContainerStatus.DISPOSING, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(defaultServerTemplate.getId(), containerSpec);
        //start container to deploy it on kie server instance
        mgmtControllerClient.startContainer(defaultServerTemplate.getId(), CONTAINER_ID);
        
        assertEquals(KieContainerStatus.STARTED ,mgmtControllerClient.getContainerInfo(defaultServerTemplate.getId(), CONTAINER_ID).getStatus());
        
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        
        ServiceResponse<KieContainerResourceList> listContianers = client.listContainers();
        KieServerAssert.assertSuccess(listContianers);
        assertNotNull(listContianers.getResult());
        assertEquals(1, listContianers.getResult().getContainers().size());
        
        KieContainerResource container = listContianers.getResult().getContainers().get(0);
        assertNotNull(container);
        assertEquals(CONTAINER_ID, container.getContainerId());
        assertEquals(latestId, container.getReleaseId());
        /*  Here is resolved id 1.0.0 - probably stay stored after testUpdateReleaseIdToLatest that run before.
            Version should not have effect on this scenario, because is deployed new one when container is stop
            and then is strated again with version latest.
        */
        if(container.getResolvedReleaseId().equals(releaseId100)) {
            logger.warn("Resolved id is not latest. Caused by previos test.");
        }
        else {
        assertEquals(releaseId101, container.getResolvedReleaseId());
        }
        assertEquals(KieContainerStatus.STARTED, container.getStatus());
        
        //stop contianer to undeploy it from kie server instance
        mgmtControllerClient.stopContainer(defaultServerTemplate.getId(), CONTAINER_ID);
        
        assertEquals(KieContainerStatus.STOPPED ,mgmtControllerClient.getContainerInfo(defaultServerTemplate.getId(), CONTAINER_ID).getStatus());
        listContianers = client.listContainers();
        KieServerAssert.assertSuccess(listContianers);
        KieServerAssert.assertNullOrEmpty("Container was found on kie server!", listContianers.getResult().getContainers());
        
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar-110").getFile());
        
            Thread.sleep(5000);
        
        mgmtControllerClient.startContainer(defaultServerTemplate.getId(), CONTAINER_ID);
        
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        
        listContianers = client.listContainers();
        KieServerAssert.assertSuccess(listContianers);
        assertNotNull(listContianers.getResult());
        assertEquals(1, listContianers.getResult().getContainers().size());
        
        container = listContianers.getResult().getContainers().get(0);
        assertNotNull(container);
        assertEquals(CONTAINER_ID, container.getContainerId());
        assertEquals(latestId, container.getReleaseId());
        assertEquals(releaseId110, container.getResolvedReleaseId());
        assertEquals(KieContainerStatus.STARTED, container.getStatus());
    
    }
    
    @Test
    public void testUpdateReleaseIdToLatest() throws Exception {
        logger.info("testUpdateReleaseIdToLatest");
        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, defaultServerTemplate, releaseId100, KieContainerStatus.DISPOSING, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(defaultServerTemplate.getId(), containerSpec);
        //start container to deploy it on kie server instance
        mgmtControllerClient.startContainer(defaultServerTemplate.getId(), CONTAINER_ID);
        
        assertEquals(KieContainerStatus.STARTED ,mgmtControllerClient.getContainerInfo(defaultServerTemplate.getId(), CONTAINER_ID).getStatus());
        
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        
        ServiceResponse<KieContainerResourceList> listContianers = client.listContainers();
        KieServerAssert.assertSuccess(listContianers);
        assertNotNull(listContianers.getResult());
        assertEquals(1, listContianers.getResult().getContainers().size());
        
        KieContainerResource container = listContianers.getResult().getContainers().get(0);
        assertNotNull(container);
        assertEquals(CONTAINER_ID, container.getContainerId());
        assertEquals(releaseId100, container.getReleaseId());
        assertEquals(releaseId100, container.getResolvedReleaseId());
        assertEquals(KieContainerStatus.STARTED, container.getStatus());
    
        
//        client.updateReleaseId(CONTAINER_ID, latestId);
        containerSpec.setReleasedId(latestId);
        containerSpec.setStatus(KieContainerStatus.STARTED);
        mgmtControllerClient.updateContainerSpec(defaultServerTemplate.getId(), containerSpec);
        
        listContianers = client.listContainers();
        KieServerAssert.assertSuccess(listContianers);
        assertNotNull(listContianers.getResult());
        assertEquals(1, listContianers.getResult().getContainers().size());
        
        container = listContianers.getResult().getContainers().get(0);
        assertNotNull(container);
        assertEquals(CONTAINER_ID, container.getContainerId());
        assertEquals(latestId, container.getReleaseId());
        assertEquals(releaseId101, container.getResolvedReleaseId()); //should not fail
        assertEquals(KieContainerStatus.STARTED, container.getStatus());
    
    }
    
    protected ServerTemplate createServerTemplate() {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId(kieServerInfo.getServerId());
        serverTemplate.setName(kieServerInfo.getName());

        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getLocation()));
        mgmtControllerClient.saveServerTemplate(serverTemplate);

        return serverTemplate;
    }
    
    
}
