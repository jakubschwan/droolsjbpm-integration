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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.shared.KieServerAssert;

public class ClusterDeployingIntegrationTest extends ClusterManagementBaseTest {

    @Before
    public void deployContainer() {
        KieServerAssert.assertNullOrEmpty("Container found before test execution!", mgmtControllerClient.listContainerSpec(kieServerTemplate.getId()));
    }

    @Test
    public void deployContainerOnNoneServer() {
        turnOffSecondaryServer();
        turnOffPrimaryServer();

        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, primaryClient, secondaryClient);
    }

    @Test
    public void deployContainerOnOneServer() {
        turnOffSecondaryServer();

        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, primaryClient);
        checkContainerNotDeployedOnServerInstances(CONTAINER_ID, secondaryClient);
    }

    @Test
    @Category(Smoke.class)
    public void deployContainerOnTwoServers() {
        ContainerSpec containerToDeploy = createDefaultContainer(kieServerTemplate);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);

        checkContainerSpec(mgmtControllerClient.getContainerInfo(kieServerTemplate.getId(), CONTAINER_ID), KieContainerStatus.STARTED);
        checkContainerDeployedOnServerInstances(CONTAINER_ID, secondaryClient, primaryClient);
    }

}
