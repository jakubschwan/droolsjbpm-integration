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
package org.kie.server.integrationtests.clustering.management;

import org.junit.Before;
import org.junit.Test;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.shared.KieServerAssert;

public class ClusterDeployingIntegrationTest extends ClusterManagementBaseTest {

    @Before
    public void deployContainer() throws InterruptedException {
        KieServerAssert.assertNullOrEmpty("Container found before test execution!", mgmtControllerClient.listContainerSpec(templateOne.getId()));
    }

    @Test
    public void deployContainerOnNoneServer() throws Exception {
        turnOffBravoServer();
        turnOffCharlieServer();

        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
    }

    @Test
    public void deployContainerOnOneServer() throws Exception {
        turnOffBravoServer();

        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientCharlie);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
    }

    @Test
    public void deployContainerOnTwoServers() {
        ContainerSpec containerToDeploy = createDefaultContainer(templateOne);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(templateOne.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientBravo, clientCharlie);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
    }

    @Test
    public void deployContainerOnOtherTemplate() {
        ContainerSpec containerToDeploy = createDefaultContainer(templateTwo);
        mgmtControllerClient.saveContainerSpec(templateTwo.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateTwo.getId(), CONTAINER_ID);

        KieServerAssert.assertNullOrEmpty("Container was found on wrong template.", mgmtControllerClient.listContainerSpec(templateOne.getId()));
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, clientBravo, clientCharlie);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, clientAlpha);
    }

}
