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
package org.kie.server.integrationtests.cluster.management;

import java.net.MalformedURLException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.controller.client.exception.UnexpectedResponseCodeException;

public class ClusterManagementIntegrationTest extends ClusterManagementBaseTest {

    @Test
    public void testGetContainerFromOneTemplate() {
        ContainerSpec containerSpec = createDefaultContainer();
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerSpec);

        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(kieServerTemplate.getId(), containerResponseEntity.getServerTemplateKey().getId());
        assertEquals(kieServerTemplate.getName(), containerResponseEntity.getServerTemplateKey().getName());

    }


    @Test
    public void testGetNotExistingContainer() {
        try {
            mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
            fail("Should throw exception about container not existing container on this server template.");
        } catch (UnexpectedResponseCodeException ex) {
            assertEquals(404, ex.getResponseCode());
        }
    }

    @Test
    public void testStartRemoveServerInstaceStopContainer() {
        //deploy container
        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        //get container
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient, primaryClient);

        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        //turnOffServer(kieServerAlpha);
        turnOffSecondaryServer();
        //logger.warn(kieServerAlpha.getKieServerInfo().toString());
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        //stop
        mgmtControllerClient.stopContainer(kieServerTemplate.getId(), CONTAINER_ID);
        //check container not deployed on server instances
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, primaryClient);
    }

    @Test
    public void testStartAddServerInstanceStopContainer() {
        //turnOffServer(kieServerBravo);
        turnOffSecondaryServer();

        //deploy container
        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        //get container
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, primaryClient);

        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        //add server instance
        try {
            turnOnSecondaryServer();
        } catch(InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        //maybe need timeout
        //waitForAllKieServerSynchronization(1,primaryClient,secondaryClient);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        mgmtControllerClient.stopContainer(kieServerTemplate.getId(), CONTAINER_ID);
        //check container not deployed on server instances
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient, primaryClient);
    }

    @Test
    public void testStartContainerAndRemoveAllServerInstace() {
        //deploy container
        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        //get container
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient, primaryClient);

        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        turnOffPrimaryServer();

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, secondaryClient);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        turnOffSecondaryServer();

        //get container
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED); //not sure in status type
        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        //try to stop container??
        mgmtControllerClient.stopContainer(kieServerTemplate.getId(), CONTAINER_ID);
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);
    }

    @Test
    public void testRemoveAndAddNewInstanceWithContainer() {
        turnOffSecondaryServer();

        //deploy and get container
        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, primaryClient);

        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);
        //check container deployed on server instances
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        turnOffPrimaryServer();

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);

        try {
        turnOnSecondaryServer();
        } catch(InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, secondaryClient);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
    }

    @Test
    @Ignore
    public void testAddServerInstencesAfterStartOfContainer() throws Exception {
        turnOffSecondaryServer();
        turnOffPrimaryServer();

        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        
        try {
            //start server and check if contianer is deployed
            turnOnSecondaryServer();
        } catch (InterruptedException | MalformedURLException ex) {
           fail("Server was not started due:\n" + ex);
        }
        checkContainerSpec(mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, secondaryClient);
        
        try {
            turnOnPrimaryServer();
        } catch (InterruptedException |MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }
        //bug after start second server (first can't be founded)
        checkContainerSpec(mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, secondaryClient,primaryClient);
    }
    
    

}
