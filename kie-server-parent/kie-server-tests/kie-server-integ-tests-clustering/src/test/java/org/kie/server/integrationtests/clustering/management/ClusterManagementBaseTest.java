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

import java.util.HashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ServerTemplateKey;
import org.kie.server.integrationtests.clustering.ClusterBaseTest;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClusterManagementBaseTest extends ClusterBaseTest {

    protected static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0-SNAPSHOT");

    protected final String CONTAINER_ID = "kie-concurrent";
    protected final String CONTAINER_NAME = "containerName";

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.createAndDeployKJar(releaseId);
    }

    protected static Logger logger = LoggerFactory.getLogger(ClusterManagementBaseTest.class);

    @Override
    protected void additionalConfiguration(KieServicesConfiguration configuration) throws Exception {
        super.additionalConfiguration(configuration);
        configuration.setTimeout(60000);
    }

    protected void checkContainerSpec(ContainerSpec container, KieContainerStatus status) {
        assertNotNull(container);
        assertEquals(CONTAINER_ID, container.getId());
        assertEquals(CONTAINER_NAME, container.getContainerName());
        assertEquals(releaseId, container.getReleasedId());
        assertEquals(status, container.getStatus());
    }

    protected void checkContainerNotDeployedOnServerInstances(String containerId, KieServicesClient... clients) {
        for (KieServicesClient client : clients) {
            ServiceResponse<KieContainerResource> clientResponse = client.getContainerInfo(containerId);
            //fail, cause container is not started -> not deployed
            checkFailedServiceResponse(clientResponse);
        }
    }

    protected void checkContainerDeployedOnServerInstances(String containerId, KieServicesClient... clients) {
        for (KieServicesClient client : clients) {
            ServiceResponse<KieContainerResource> clientResponse = client.getContainerInfo(containerId);
            checkSuccessServiceResponse(clientResponse, KieContainerStatus.STARTED);
        }
    }

    protected void checkSuccessServiceResponse(ServiceResponse<KieContainerResource> containerInfo, KieContainerStatus status) {
        assertNotNull(containerInfo);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
        assertNotNull(containerInfo.getResult());
        assertEquals(CONTAINER_ID, containerInfo.getResult().getContainerId());
        assertEquals(status, containerInfo.getResult().getStatus());
        assertEquals(releaseId, containerInfo.getResult().getReleaseId());
    }

    protected void checkFailedServiceResponse(ServiceResponse<KieContainerResource> containerInfo) {
        assertEquals(ServiceResponse.ResponseType.FAILURE, containerInfo.getType());
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");
    }

    protected ContainerSpec createDefaultContainer() {
        return createDefaultContainer(null);
    }

    protected ContainerSpec createDefaultContainer(ServerTemplateKey templateKey) {
        return new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, templateKey, releaseId, KieContainerStatus.STOPPED, new HashMap());
    }
}
