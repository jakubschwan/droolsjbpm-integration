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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.controller.client.exception.UnexpectedResponseCodeException;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

public class ClusterManagementIntegrationTest extends ClusterManagementBaseTest {

    @Test
    public void testGetContainerFromOneTemplate() {
        ContainerSpec containerSpec = createDefaultContainer();
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerSpec);

        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(templateOne.getId(), containerResponseEntity.getServerTemplateKey().getId());
        assertEquals(templateOne.getName(), containerResponseEntity.getServerTemplateKey().getName());

        try {
            containerResponseEntity = mgmtControllerClient.getContainerInfo(templateTwo.getId(), CONTAINER_ID);
            fail("Should throw exception about container not existing container on this server template.");
        } catch (UnexpectedResponseCodeException ex) {
            assertEquals(404, ex.getResponseCode());
        }

    }

    @Test
    @Category(Smoke.class)
    public void testGetContainerFromBothTemplates() {
        ContainerSpec containerSpec = createDefaultContainer();
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerSpec);
        mgmtControllerClient.saveContainerSpec(templateTwo.getId(), containerSpec);

        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(templateOne.getId(), containerResponseEntity.getServerTemplateKey().getId());
        assertEquals(templateOne.getName(), containerResponseEntity.getServerTemplateKey().getName());

        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateTwo.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(templateTwo.getId(), containerResponseEntity.getServerTemplateKey().getId());
        assertEquals(templateTwo.getName(), containerResponseEntity.getServerTemplateKey().getName());

    }

    @Test
    public void testGetNotExistingContainer() {
        try {
            mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
            fail("Should throw exception about container not existing container on this server template.");
        } catch (UnexpectedResponseCodeException ex) {
            assertEquals(404, ex.getResponseCode());
        }
        try {
            mgmtControllerClient.getContainerInfo(templateTwo.getId(), CONTAINER_ID);
            fail("Should throw exception about container not existing container on this server template.");
        } catch (UnexpectedResponseCodeException ex) {
            assertEquals(404, ex.getResponseCode());
        }
    }

    @Test
    public void testStartRemoveServerInstaceStopContainer() {
        //deploy container
        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        //get container
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha, clientBravo, clientCharlie);

        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie, clientBravo);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);

        //turnOffServer(kieServerAlpha);
        turnOffBravoServer();
        //logger.warn(kieServerAlpha.getKieServerInfo().toString());
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie);

        //stop
        mgmtControllerClient.stopContainer(templateOne.getId(), CONTAINER_ID);
        //check container not deployed on server instances
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientCharlie, clientAlpha);
    }

    @Test
    public void testStartAddServerInstanceStopContainer() {
        //turnOffServer(kieServerBravo);
        turnOffBravoServer();

        //deploy container
        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        //get container
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha, clientCharlie);

        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie);

        //add server instance
        try {
            turnOnBravoServer();
        } catch(InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        //maybe need timeout
        //waitForAllKieServerSynchronization(1,clientAlpha,clientBravo);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie, clientBravo);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);

        mgmtControllerClient.stopContainer(templateOne.getId(), CONTAINER_ID);
        //check container not deployed on server instances
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha, clientBravo, clientCharlie);
    }

    @Test
    public void testStartContainerAndRemoveAllServerInstace() {
        //deploy container
        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        //get container
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha, clientBravo, clientCharlie);

        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie, clientBravo);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);

        turnOffCharlieServer();

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientBravo);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);

        turnOffBravoServer();

        //get container
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED); //not sure in status type
        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);

        //try to stop container??
        mgmtControllerClient.stopContainer(templateOne.getId(), CONTAINER_ID);
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);
    }

    @Test
    public void testRemoveAndAddNewInstanceWithContainer() {
        turnOffBravoServer();

        //deploy and get container
        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STOPPED);

        //check container not deployed on server instances
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha, clientCharlie);

        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
        //check container deployed on server instances
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie);

        turnOffCharlieServer();

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);

        try {
        turnOnBravoServer();
        } catch(InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }

        //check container deployed on server
        containerResponseEntity = mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID);
        checkContainerSpec(containerResponseEntity, KieContainerStatus.STARTED);

        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientBravo);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
    }

    @Test
    public void testAddServerInstencesAfterStartOfContainer() throws Exception {
        turnOffBravoServer();
        turnOffCharlieServer();

        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
        
        try {
            //start server and check if contianer is deployed
            turnOnBravoServer();
        } catch (InterruptedException | MalformedURLException ex) {
           fail("Server was not started due:\n" + ex);
        }
        KieServerSynchronization.waitForKieServerSynchronization(clientBravo, 1);
        checkContainerSpec(mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientBravo);
        
        try {
            turnOnCharlieServer();
        } catch (InterruptedException |MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }
        KieServerSynchronization.waitForKieServerSynchronization(clientCharlie, 1);
        checkContainerSpec(mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientBravo,clientCharlie);
    }
    
    

}
