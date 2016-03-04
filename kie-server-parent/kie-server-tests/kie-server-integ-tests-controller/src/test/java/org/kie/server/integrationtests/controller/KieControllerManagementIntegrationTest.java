/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.ModelFactory;
import org.kie.server.controller.api.model.KieServerInstance;
import org.kie.server.controller.api.model.KieServerInstanceInfo;
import org.kie.server.controller.api.model.KieServerInstanceList;
import org.kie.server.controller.api.model.KieServerStatus;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.client.exception.UnexpectedResponseCodeException;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.kie.server.integrationtests.category.Smoke;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.controller.api.model.spec.RuleConfig;
import org.kie.server.controller.api.model.spec.ServerTemplateKey;

public class KieControllerManagementIntegrationTest extends KieControllerManagementBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0-SNAPSHOT");

    private static final String CONTAINER_ID = "kie-concurrent";

    private KieServerInfo kieServerInfo;

    @BeforeClass
    public static void initialize() throws Exception {
        createAndDeployKJar(releaseId);
    }

    @Before
    public void getKieServerInfo() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        assumeThat(reply.getType(), is(ServiceResponse.ResponseType.SUCCESS));
        kieServerInfo = reply.getResult();
    }

    @Test
    @Category(Smoke.class)
    public void testCreateKieServerInstance() {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId(kieServerInfo.getServerId());
        serverTemplate.setName(kieServerInfo.getName());
        controllerClient.saveServerTemplate(serverTemplate);

        ServerTemplate storedServerTemplate = controllerClient.getServerTemplate(serverTemplate.getId());
        assertNotNull(storedServerTemplate);
        assertEquals(serverTemplate.getId(), storedServerTemplate.getId());
        assertEquals(serverTemplate.getName(), storedServerTemplate.getName());

        Collection<ServerTemplate> serverTemplates = controllerClient.listServerTemplates();
        assertNotNull(serverTemplates);
        assertEquals(1, serverTemplates.size());

        storedServerTemplate = serverTemplates.iterator().next();
        assertNotNull(storedServerTemplate);
        assertEquals(serverTemplate.getId(), storedServerTemplate.getId());
        assertEquals(serverTemplate.getName(), storedServerTemplate.getName());

    }

    @Test
    public void testDeleteKieServerInstance() {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId(kieServerInfo.getServerId());
        serverTemplate.setName(kieServerInfo.getName());
        controllerClient.saveServerTemplate(serverTemplate);

        Collection<ServerTemplate> serverTemplates = controllerClient.listServerTemplates();
        assertNotNull(serverTemplates);
        assertEquals(1, serverTemplates.size());

        // Delete created kie server instance.
        controllerClient.deleteServerTemplate(serverTemplate.getId());

        // There are no kie server instances in controller now.
        serverTemplates = controllerClient.listServerTemplates();
        assertNullOrEmpty("Active kie server instance found!", serverTemplates);
    }

    @Test
    public void testDeleteNotExistingKieServerInstance() {
        try {
            // Try to delete not existing kie server instance.
            controllerClient.deleteServerTemplate("not existing");
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    @Category(Smoke.class)
    public void testGetKieServerInstance() {
        // Create kie server instance in controller.
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId(kieServerInfo.getServerId());
        serverTemplate.setName(kieServerInfo.getName());

        // define server instance for this template
        ServerInstanceKey serverInstanceKey = ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getLocation());
        serverTemplate.addServerInstance(serverInstanceKey);

        controllerClient.saveServerTemplate(serverTemplate);

        // Get kie server instance.
        ServerTemplate serverInstance = controllerClient.getServerTemplate(serverTemplate.getId());

        assertNotNull(serverInstance);
        assertEquals(kieServerInfo.getServerId(), serverInstance.getId());
        assertEquals(kieServerInfo.getName(), serverInstance.getName());

        assertNotNull("Kie server instance isn't managed!", serverInstance.getServerInstanceKeys());
        assertEquals(1, serverInstance.getServerInstanceKeys().size());

        ServerInstanceKey managedInstance = serverInstance.getServerInstanceKeys().iterator().next();
        assertNotNull(managedInstance);
//        assertArrayEquals(kieServerInfo.getCapabilities().toArray(), managedInstance.getCapabilities().toArray());
        assertEquals(kieServerInfo.getLocation(), managedInstance.getUrl());
        assertEquals(serverTemplate.getId(), managedInstance.getServerTemplateId());
        assertEquals(serverInstanceKey.getServerName(), managedInstance.getServerName());
    }

    @Test
    public void testCreateDuplicitKieServerInstance() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);
        try {
            controllerClient.saveServerTemplate(serverTemplate);
            fail("Should throw exception about kie server instance existing. size: " + controllerClient.listServerTemplates().size());
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testGetNotExistingKieServerInstance() {
        try {
            controllerClient.getServerTemplate("not existing");
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testListKieServerInstances() {
        Collection<ServerTemplate> serverTemplateList = controllerClient.listServerTemplates();
        assertNullOrEmpty("Found some Kie server instances", serverTemplateList);

        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        serverTemplateList = controllerClient.listServerTemplates();
        assertNotNull(serverTemplateList);
        assertEquals(1, serverTemplateList.size());

        ServerTemplate serverInstance = serverTemplateList.iterator().next();
        assertNotNull(serverInstance);
        assertEquals(kieServerInfo.getServerId(), serverInstance.getId());
        assertEquals(kieServerInfo.getName(), serverInstance.getName());

        assertNotNull("Kie server instance isn't managed!", serverInstance.getServerInstanceKeys());
        assertEquals(1, serverInstance.getServerInstanceKeys().size());
    }

    @Test
    public void testContainerHandling() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        ContainerSpec containerSpec = createContainerSpec(serverTemplate);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        // Check that container is deployed.
        ContainerSpec deployedContainer = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(deployedContainer);
        assertEquals(CONTAINER_ID, deployedContainer.getId());
        assertEquals(releaseId, deployedContainer.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, deployedContainer.getStatus());

        // Container is in stopped state, so there are no containers deployed in kie server.
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containersList.getType());
        assertNullOrEmpty("Active containers found!", containersList.getResult().getContainers());

        // Undeploy container for kie server instance.
        controllerClient.deleteContainerSpec(serverTemplate.getId(), containerSpec.getId());

        // Check that container is disposed.
        try {
            controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
            fail("Should throw exception about container info not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }

    }

    @Test
    public void testCreateContainerOnNotExistingKieServerInstance() {
        ContainerSpec containerSpec = new ContainerSpec();
        try {
            controllerClient.saveContainerSpec("not existing", containerSpec);
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testCreateDuplicitContainer() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        ContainerSpec containerSpec = createContainerSpec(serverTemplate);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);
        try {
            controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);
            fail("Should throw exception about container existing. size: " + controllerClient.listContainerSpec(serverTemplate.getId()).size());
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testDeleteNotExistingContainer() {
        try {
            // Try to delete not existing kie server instance.
            controllerClient.deleteContainerSpec("not existing", "not existing");
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }

        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        try {
            controllerClient.deleteContainerSpec(serverTemplate.getId(), "not existing");
            fail("Should throw exception about container not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testGetContainer() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        ContainerSpec containerSpec = createContainerSpec(serverTemplate);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        ContainerSpec containerInstance = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(containerInstance);
        assertEquals(CONTAINER_ID, containerInstance.getId());
        assertEquals(releaseId, containerInstance.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerInstance.getStatus());
        assertEquals(containerSpec.getContainerName(), containerInstance.getContainerName());
        assertEquals(serverTemplate.getId(), containerInstance.getServerTemplateKey().getId());
    }

    @Test
    public void testListContainer() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        Collection<ContainerSpec> containerList = controllerClient.listContainerSpec(serverTemplate.getId());
        assertNullOrEmpty("Found some containers.", containerList); //better msg

        ContainerSpec containerSpec = createContainerSpec(serverTemplate);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        containerList = controllerClient.listContainerSpec(serverTemplate.getId());
        assertNotNull(containerList);
        assertEquals(1, containerList.size());

        ContainerSpec containerInstance = containerList.iterator().next();
        assertNotNull(containerInstance);
        assertEquals(CONTAINER_ID, containerInstance.getId());
        assertEquals(releaseId, containerInstance.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerInstance.getStatus());
        assertEquals(containerSpec.getContainerName(), containerInstance.getContainerName());
        assertEquals(serverTemplate.getId(), containerInstance.getServerTemplateKey().getId());
    }

    @Test
    public void testStartAndStopContainer() throws Exception {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        ContainerSpec containerSpec = createContainerSpec(serverTemplate);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(containerResponseEntity);
        assertEquals(CONTAINER_ID, containerResponseEntity.getId());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerResponseEntity.getStatus());

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(containerSpec.getId());
        assertEquals(ServiceResponse.ResponseType.FAILURE, containerInfo.getType());
        assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");

        controllerClient.startContainer(serverTemplate.getId(), containerSpec.getId());

        // Get container using kie controller.
        containerResponseEntity = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(containerResponseEntity);
        assertEquals(CONTAINER_ID, containerResponseEntity.getId());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
        assertEquals(KieContainerStatus.STARTED, containerResponseEntity.getStatus());

        // Check that container is deployed in kie server.
        waitForKieServerSynchronization(1);
        containerInfo = client.getContainerInfo(containerSpec.getId());
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
        assertEquals(CONTAINER_ID, containerInfo.getResult().getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerInfo.getResult().getStatus());
        assertEquals(releaseId, containerInfo.getResult().getReleaseId());

        controllerClient.stopContainer(kieServerInfo.getServerId(), CONTAINER_ID);

        containerResponseEntity = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(containerResponseEntity);
        assertEquals(CONTAINER_ID, containerResponseEntity.getId());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerResponseEntity.getStatus());

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        waitForKieServerSynchronization(0);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.FAILURE, containerInfo.getType());
        assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");
    }

    @Test
    public void testStartNotExistingContainer() throws Exception {
        try {
            controllerClient.startContainer("not existing", "not existing");
            fail("Should throw exception about server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }

        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        try {
            controllerClient.startContainer(serverTemplate.getId(), "not existing");
            fail("Should throw exception about container not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testStopNotExistingContainer() throws Exception {
        try {
            controllerClient.stopContainer("not existing", "not existing");
            fail("Should throw exception about server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }

        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        try {
            controllerClient.stopContainer(serverTemplate.getId(), "not existing");
            fail("Should throw exception about container not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testGetNotExistingContainer() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);
        try {
            ContainerSpec containerSpec = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec("not existing");
            if (containerSpec != null) {
                fail("Should throw exception about container not existing.");
            } else {
                fail("is NULL");
            }
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testUpgradeContainerConfig() {
        ServerTemplate serverTemplate = createServerTemplateWithServerInstance();
        controllerClient.saveServerTemplate(serverTemplate);

        ContainerSpec containerSpec = createContainerSpec(serverTemplate);
        controllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        ContainerSpec containerInstance = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(containerInstance);
        assertEquals(CONTAINER_ID, containerInstance.getId());
        assertEquals(releaseId, containerInstance.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerInstance.getStatus());
        // assertEquals(containerSpec.getConfigs().get(Capability.RULE), containerInstance.getConfigs().get(Capability.RULE));

        ContainerConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        try {
            controllerClient.updateContainerConfig(serverTemplate.getId(), containerSpec.getId(), Capability.PROCESS, processConfig);
        } catch (UnexpectedResponseCodeException e) {
            e.printStackTrace();
            throw e;
        }

        ContainerConfig ruleConfig = new RuleConfig(500l, KieScannerStatus.SCANNING);
        try {
            controllerClient.updateContainerConfig(serverTemplate.getId(), containerSpec.getId(), Capability.RULE, ruleConfig);
        } catch (UnexpectedResponseCodeException e) {
            e.printStackTrace();
            throw e;
        }
        containerInstance = controllerClient.getServerTemplate(serverTemplate.getId()).getContainerSpec(containerSpec.getId());
        assertNotNull(containerInstance);
        assertEquals(CONTAINER_ID, containerInstance.getId());
        assertEquals(releaseId, containerInstance.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerInstance.getStatus());
        assertEquals(ruleConfig, containerInstance.getConfigs().get(Capability.RULE));

    }

    private ServerTemplate createServerTemplateWithServerInstance() {
        ServerTemplate serverTemplate = new ServerTemplate(kieServerInfo.getServerId(), kieServerInfo.getName());
        ServerInstanceKey serverInstanceKey = ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getLocation());
        serverTemplate.addServerInstance(serverInstanceKey);
        return serverTemplate;
    }

    private ContainerSpec createContainerSpec(ServerTemplate serverTemplate) {
        Map<Capability, ContainerConfig> config = new HashMap();
        RuleConfig ruleConfig = new RuleConfig();
        ruleConfig.setPollInterval(1000l);
        ruleConfig.setScannerStatus(KieScannerStatus.STARTED);
        config.put(Capability.RULE, ruleConfig);

        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setKBase("defaultKieBase");
        processConfig.setKSession("defaultKieSession");
        processConfig.setMergeMode("MERGE_COLLECTION");
        processConfig.setRuntimeStrategy("PER_PROCESS_INSTANCE");
        config.put(Capability.PROCESS, processConfig);

        ContainerSpec containerSpec = new ContainerSpec();
        containerSpec.setId(CONTAINER_ID);
        containerSpec.setReleasedId(releaseId);
        containerSpec.setServerTemplateKey(new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()));
        containerSpec.setStatus(KieContainerStatus.STOPPED);
        containerSpec.setConfigs(config);
        return containerSpec;
    }
}
