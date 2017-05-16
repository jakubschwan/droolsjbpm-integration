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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.ModelFactory;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.controller.api.model.spec.RuleConfig;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.api.model.spec.ServerTemplateKey;
import org.kie.server.integrationtests.controller.client.exception.UnexpectedResponseCodeException;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

public class KieControllerManagementIntegrationTest extends KieControllerManagementBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.0.0-SNAPSHOT");
    private static ReleaseId releaseId101 = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "1.1.0-SNAPSHOT");

    private static final String CONTAINER_ID = "kie-concurrent";
    private static final String CONTAINER_NAME = "containerName";

    private KieServerInfo kieServerInfo;

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.createAndDeployKJar(releaseId);
        KieServerDeployer.createAndDeployKJar(releaseId101);
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
    public void testCreateKieServerTemplate() {
        ServerTemplate serverTemplate = createServerTemplate();

        ServerTemplate storedServerTemplate = mgmtControllerClient.getServerTemplate(serverTemplate.getId());
        checkServerTemplate(storedServerTemplate);

        Collection<ServerTemplate> serverTemplates = mgmtControllerClient.listServerTemplates();
        assertNotNull(serverTemplates);
        assertEquals(1, serverTemplates.size());

        storedServerTemplate = serverTemplates.iterator().next();
        checkServerTemplate(storedServerTemplate);
    }

    @Test
    public void testCreateDuplicateServerTemplate() {
        // Create kie server template.
        ServerTemplate serverTemplate = createServerTemplate();

        try {
            // Try to create same kie server instance.
            mgmtControllerClient.saveServerTemplate(serverTemplate);
            fail("Template already created.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testDeleteServerTemplate() {
        ServerTemplate serverTemplate = createServerTemplate();

        Collection<ServerTemplate> serverTemplates = mgmtControllerClient.listServerTemplates();
        assertNotNull(serverTemplates);
        assertEquals(1, serverTemplates.size());

        // Delete created server template.
        mgmtControllerClient.deleteServerTemplate(serverTemplate.getId());

        // There are no kie server instances in controller now.
        serverTemplates = mgmtControllerClient.listServerTemplates();
        KieServerAssert.assertNullOrEmpty("Active kie server instance found!", serverTemplates);
    }

    @Test
    public void testDeleteNotExistingServerTemplate() {
        try {
            // Try to delete not existing server template.
            mgmtControllerClient.deleteServerTemplate("not existing");
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    @Category(Smoke.class)
    public void testGetKieServerInstance() {
        // Create kie server instance in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        ServerInstanceKey serverInstanceKey = serverTemplate.getServerInstanceKeys().iterator().next();

        // Get kie server instance.
        ServerTemplate serverInstance = mgmtControllerClient.getServerTemplate(serverTemplate.getId());

        checkServerTemplate(serverInstance);

        assertNotNull("Kie server instance isn't managed!", serverInstance.getServerInstanceKeys());
        assertEquals(1, serverInstance.getServerInstanceKeys().size());

        ServerInstanceKey managedInstance = serverInstance.getServerInstanceKeys().iterator().next();
        assertNotNull(managedInstance);
        assertEquals(kieServerInfo.getLocation(), managedInstance.getUrl());
        assertEquals(serverTemplate.getId(), managedInstance.getServerTemplateId());
        assertEquals(serverInstanceKey.getServerName(), managedInstance.getServerName());
    }

    @Test
    public void testGetNotExistingServerTemplate() {
        try {
            // Try to get not existing kie server template.
            mgmtControllerClient.getServerTemplate(kieServerInfo.getServerId());
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testListKieServerInstances() {
        // Create kie server instance in controller.
        createServerTemplate();

        // List kie server instances.
        Collection<ServerTemplate> instanceList = mgmtControllerClient.listServerTemplates();

        assertNotNull(instanceList);
        assertEquals(1, instanceList.size());

        ServerTemplate serverInstance = instanceList.iterator().next();
        checkServerTemplate(serverInstance);

        assertNotNull("Kie server instance isn't managed!", serverInstance.getServerInstanceKeys());
        assertEquals(1, serverInstance.getServerInstanceKeys().size());

        ServerInstanceKey managedInstance = serverInstance.getServerInstanceKeys().iterator().next();
        assertNotNull(managedInstance);
        assertEquals(kieServerInfo.getLocation(), managedInstance.getUrl());

    }

    @Test
    public void testEmptyListServerTemplates() throws Exception {
        Collection<ServerTemplate> instanceList = mgmtControllerClient.listServerTemplates();
        KieServerAssert.assertNullOrEmpty("Server templates found!", instanceList);
    }

    @Test
    public void testContainerHandling() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Check that container is deployed.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Container is in stopped state, so there are no containers deployed in kie server.
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containersList.getType());
        KieServerAssert.assertNullOrEmpty("Active containers found!", containersList.getResult().getContainers());

        // Undeploy container for kie server instance.
        mgmtControllerClient.deleteContainerSpec(serverTemplate.getId(), CONTAINER_ID);

        // Check that container is disposed.
        try {
            mgmtControllerClient.getContainerInfo(serverTemplate.getId(), CONTAINER_ID);
            fail("Should throw exception about container info not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testCreateContainerAutoStart() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Check that container is deployed.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Container is in started state, so it should already be in kie server
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containersList.getType());
        assertEquals(1, containersList.getResult().getContainers().size());

        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
        assertEquals(CONTAINER_ID, containerInfo.getResult().getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerInfo.getResult().getStatus());
        assertEquals(releaseId, containerInfo.getResult().getReleaseId());

    }


    @Test
    public void testCreateServerTemplateWithContainersAutoStart() throws Exception {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId( kieServerInfo.getServerId() );
        serverTemplate.setName( kieServerInfo.getName() );

        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), kieServerInfo.getLocation()));

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, new ServerTemplateKey(serverTemplate.getId(), serverTemplate.getName()), releaseId, KieContainerStatus.STARTED, new HashMap());
        serverTemplate.addContainerSpec(containerToDeploy);

        mgmtControllerClient.saveServerTemplate(serverTemplate);

        // Check that container is deployed.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Container is in started state, so it should already be in kie server
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        ServiceResponse<KieContainerResourceList> containersList = client.listContainers();
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containersList.getType());
        assertEquals(1, containersList.getResult().getContainers().size());

        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
        assertEquals(CONTAINER_ID, containerInfo.getResult().getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerInfo.getResult().getStatus());
        assertEquals(releaseId, containerInfo.getResult().getReleaseId());

    }
    @Test
    public void testCreateContainerOnNotExistingKieServerInstance() {
        // Try to create container using kie controller without created kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, new ServerTemplate(), releaseId, KieContainerStatus.STOPPED, new HashMap());
        try {

            mgmtControllerClient.saveContainerSpec(kieServerInfo.getServerId(), containerToDeploy);
            fail("Should throw exception about kie server instance not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testCreateDuplicitContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        try {
            // Try to create same container.
            mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);
            fail("Should throw exception about container being created already.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
        }
    }

    @Test
    public void testDeleteNotExistingContainer() {
        // Try to dispose not existing container using kie controller without created kie server instance.
        try {
            mgmtControllerClient.deleteContainerSpec(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about kie server instance not exists.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        createServerTemplate();
        // Try to dispose not existing container using kie controller with created kie server instance.
        try {
            mgmtControllerClient.deleteContainerSpec(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container not exists.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testGetContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
    }

    @Test
    public void testGetAndUpdateContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());

        containerToDeploy.setReleasedId(releaseId101);
        mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        assertNotNull(containerResponseEntity);
        assertEquals(CONTAINER_ID, containerResponseEntity.getId());
        assertEquals(KieContainerStatus.STOPPED, containerResponseEntity.getStatus());
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(releaseId101, containerResponseEntity.getReleasedId());
    }

    @Test
    public void testStartAndStopContainer() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.FAILURE, containerInfo.getType());
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");

        mgmtControllerClient.startContainer(kieServerInfo.getServerId(), CONTAINER_ID);

        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Check that container is deployed in kie server.
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
        assertEquals(CONTAINER_ID, containerInfo.getResult().getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerInfo.getResult().getStatus());
        assertEquals(releaseId, containerInfo.getResult().getReleaseId());

        mgmtControllerClient.stopContainer(kieServerInfo.getServerId(), CONTAINER_ID);

        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        KieServerSynchronization.waitForKieServerSynchronization(client, 0);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.FAILURE, containerInfo.getType());
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");
    }

    @Test
    public void testStartNotExistingContainer() throws Exception {
        // Try to start not existing container using kie controller without created kie server instance.
        try {
            mgmtControllerClient.startContainer(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        createServerTemplate();
        // Try to start not existing container using kie controller with created kie server instance.
        try {
            mgmtControllerClient.startContainer(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testStopNotExistingContainer() throws Exception {
        // Try to stop not existing container using kie controller without created kie server instance.
        try {
            mgmtControllerClient.stopContainer(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        createServerTemplate();
        // Try to stop not existing container using kie controller with created kie server instance.
        try {
            mgmtControllerClient.stopContainer(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testGetNotExistingContainer() {
        // Try to get not existing container using kie controller without created kie server instance.
        try {
            mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container info not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        createServerTemplate();
        // Try to get not existing container using kie controller with created kie server instance.
        try {
            mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
            fail("Should throw exception about container info not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testListContainers() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        Collection<ContainerSpec> containerList = mgmtControllerClient.listContainerSpec(kieServerInfo.getServerId());

        assertNotNull(containerList);
        assertEquals(1, containerList.size());

        ContainerSpec containerResponseEntity = containerList.iterator().next();
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
    }

    @Test
    public void testEmptyListContainers() {
        try {
            Collection<ContainerSpec> emptyList = mgmtControllerClient.listContainerSpec(kieServerInfo.getServerId());
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        // Create kie server instance connection in controller.
        createServerTemplate();

        Collection<ContainerSpec> emptyList = mgmtControllerClient.listContainerSpec(kieServerInfo.getServerId());
        KieServerAssert.assertNullOrEmpty("Active containers found!", emptyList);
    }

    @Test
    public void testUpdateContainerConfig() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        Map<Capability, ContainerConfig> containerConfigMap = new HashMap();

        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        containerConfigMap.put(Capability.PROCESS, processConfig);

        RuleConfig ruleConfig = new RuleConfig(500l, KieScannerStatus.SCANNING);
        containerConfigMap.put(Capability.RULE, ruleConfig);

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, containerConfigMap);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);

        // Check process config and rule config
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);

        processConfig = new ProcessConfig("SINGLETON", "defaultKieBase", "defaultKieSession", "OVERRIDE_ALL");
        mgmtControllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, processConfig);

        // Check process config and rule config
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);

        ruleConfig = new RuleConfig(1000l, KieScannerStatus.STOPPED);
        mgmtControllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.RULE, ruleConfig);

        // Check process config and rule config
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);
    }

    @Test
    public void testUpdateNotExistingContainerConfig() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerSpec);

        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        KieServerAssert.assertNullOrEmpty("Config is not empty.", containerResponseEntity.getConfigs().values());

        // Try update not existing ProcessConfig
        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        mgmtControllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, processConfig);


        // Try update not existing RuleConfig
        RuleConfig ruleConfig = new RuleConfig(500l, KieScannerStatus.SCANNING);
        mgmtControllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.RULE, ruleConfig);
    }

    @Test
    public void testUpdateContainerConfigOnNotExistingContainer() {
        ProcessConfig config = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        try {
            mgmtControllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, config);
            fail("Should throw exception about kie server instance not existing.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        // Create kie server instance connection in controller.
        createServerTemplate();

        try {
            mgmtControllerClient.updateContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, Capability.PROCESS, config);
            fail("Should throw exception about container info not found.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }
    }

    @Test
    public void testTemplateKeyChangeDuringUpdate() {
        ServerTemplate serverTemplate = createServerTemplate();

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());

        // setting new template to container
        ServerTemplate secondTemplate = createServerTemplate("st-id", "st-id", kieServerInfo.getLocation());
        containerToDeploy.setServerTemplateKey(secondTemplate);

        try {
            mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);
            fail("Template key should not be allowed to be changed during update.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
            KieServerAssert.assertResultContainsString(e.getMessage(), "Cannot change container template key during update.");
        }

        assertEquals(2, mgmtControllerClient.listServerTemplates().size());
        // Check that on other server template is not any container.
        KieServerAssert.assertNullOrEmpty("Found container in second server template.", mgmtControllerClient.listContainerSpec(secondTemplate.getId()));
        assertEquals(1, mgmtControllerClient.listContainerSpec(serverTemplate.getId()).size());

        // Check that container is not changed
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(serverTemplate.getId(), containerResponseEntity.getServerTemplateKey().getId());
        assertEquals(serverTemplate.getName(), containerResponseEntity.getServerTemplateKey().getName());
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
    }

    @Test
    public void testUpdateContainerWithDifferrentID() {
        ServerTemplate serverTemplate = createServerTemplate();
        final String ONE_ID = "one";
        final String TWO_ID = "two";

        // Deploy container for kie server instance.
        ContainerSpec containerOneToDeploy = new ContainerSpec(ONE_ID, ONE_ID, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        ContainerSpec containerTwoToDeploy = new ContainerSpec(TWO_ID, TWO_ID, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerOneToDeploy);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerTwoToDeploy);

        containerOneToDeploy.setReleasedId(releaseId101);

        try {
            mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerTwoToDeploy.getId(), containerOneToDeploy);
            fail("Container one was updated from container two REST endpoint.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
            KieServerAssert.assertResultContainsString(e.getMessage(), "Cannot update container " + containerOneToDeploy.getId() + " on container " + containerTwoToDeploy.getId());
        }

        // Check container that are not changed
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(serverTemplate.getId(), ONE_ID);
        assertEquals(ONE_ID, containerResponseEntity.getId());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerResponseEntity.getStatus());

        containerResponseEntity = mgmtControllerClient.getContainerInfo(serverTemplate.getId(), TWO_ID);
        assertEquals(TWO_ID, containerResponseEntity.getId());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
        assertEquals(KieContainerStatus.STOPPED, containerResponseEntity.getStatus());
    }

    @Test
    public void testUpdateContainerNonValidReleaseId() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STARTED, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());

        // Check that container is deployed in kie server.
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        ServiceResponse<KieContainerResource> containerInfoResponse = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfoResponse.getType());
        KieContainerResource containerResource = containerInfoResponse.getResult();
        assertEquals(CONTAINER_ID, containerResource.getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerResource.getStatus());
        assertEquals(releaseId, containerResource.getReleaseId());

        assertEquals(1, containerResource.getMessages().size());
        Collection<String> oldMessages = containerResource.getMessages().get(0).getMessages();
        assertEquals(1, oldMessages.size());
        String oldMessage = oldMessages.iterator().next();

        // Update container
        ReleaseId nonValidReleaseId = new ReleaseId("org.kie.server.testing", "stateless-session-kjar", "2.0.0-SNAPSHOT");
        containerToDeploy.setReleasedId(nonValidReleaseId);

        // We can update container to new version, but if can't be found, then deployed container is not changed.
        mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller. Container has changed ReleaseId
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        assertEquals(CONTAINER_ID, containerResponseEntity.getId());
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(KieContainerStatus.STARTED, containerResponseEntity.getStatus());
        assertEquals(nonValidReleaseId, containerResponseEntity.getReleasedId());

        // Check deployed container
        containerInfoResponse = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfoResponse.getType());
        containerResource = containerInfoResponse.getResult();

        // Check that Kie Container has message about error durining updating
        // Kie Container store last message
        KieServerSynchronization.waitForKieServerMessage(client, CONTAINER_ID, oldMessage, "Error updating releaseId for container");
        System.out.println("*****Synchronized\n");
        assertEquals(1, containerResource.getMessages().size());
        Collection<String> messages = containerResource.getMessages().get(0).getMessages();
        assertEquals(1, messages.size());
        KieServerAssert.assertResultContainsString(messages.iterator().next(), "Error updating releaseId for container");

        assertEquals(CONTAINER_ID, containerResource.getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerResource.getStatus());
        assertEquals(releaseId, containerResource.getReleaseId());
    }

    @Test
    public void testStartContainerByUpdateKieContainerStatus() throws Exception {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());

        // Check that container is not deployed in kie server (as container is in STOPPED state).
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.FAILURE, containerInfo.getType());
        KieServerAssert.assertResultContainsString(containerInfo.getMsg(), "Container " + CONTAINER_ID + " is not instantiated.");

        // Update container
        containerToDeploy.setStatus(KieContainerStatus.STARTED);
        mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STARTED);

        // Check that container is deployed in kie server.
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertEquals(ServiceResponse.ResponseType.SUCCESS, containerInfo.getType());
        assertEquals(CONTAINER_ID, containerInfo.getResult().getContainerId());
        assertEquals(KieContainerStatus.STARTED, containerInfo.getResult().getStatus());
        assertEquals(releaseId, containerInfo.getResult().getReleaseId());
    }

    @Test
    public void testCreateContainerByUpdateContainer() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        try {
            mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);
            fail("Container was created by update command - REST Post method.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(404, e.getResponseCode());
        }

        KieServerAssert.assertNullOrEmpty("Found deployed container.", mgmtControllerClient.listContainerSpec(serverTemplate.getId()));
    }

    @Test
    public void testUpdateContainerId() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, Collections.EMPTY_MAP);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());

        containerToDeploy.setId("newID");
        try {
            mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), CONTAINER_ID, containerToDeploy);
            fail("Container has updated id.");
        } catch (UnexpectedResponseCodeException e) {
            assertEquals(400, e.getResponseCode());
            KieServerAssert.assertResultContainsString(e.getMessage(), "Cannot update container newID on container " + CONTAINER_ID);
        }
    }

    @Test
    public void testUpdateContainerWitoutContainerConfig() {
        // Create kie server instance connection in controller.
        ServerTemplate serverTemplate = createServerTemplate();

        // Create container configMap
        Map<Capability, ContainerConfig> containerConfigMap = new HashMap();

        ProcessConfig processConfig = new ProcessConfig("PER_PROCESS_INSTANCE", "kieBase", "kieSession", "MERGE_COLLECTION");
        containerConfigMap.put(Capability.PROCESS, processConfig);
        RuleConfig ruleConfig = new RuleConfig(5000l, KieScannerStatus.SCANNING);
        containerConfigMap.put(Capability.RULE, ruleConfig);

        // Deploy container for kie server instance.
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, serverTemplate, releaseId, KieContainerStatus.STOPPED, containerConfigMap);
        mgmtControllerClient.saveContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        ContainerSpec containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());

        // Check process config and rule config.
        checkContainerConfig(kieServerInfo.getServerId(), CONTAINER_ID, processConfig, ruleConfig);

        // Update container
        containerToDeploy.setConfigs(Collections.EMPTY_MAP);
        mgmtControllerClient.updateContainerSpec(serverTemplate.getId(), containerToDeploy);

        // Get container using kie controller.
        containerResponseEntity = mgmtControllerClient.getContainerInfo(kieServerInfo.getServerId(), CONTAINER_ID);
        checkContainer(containerResponseEntity, KieContainerStatus.STOPPED);
        assertEquals(CONTAINER_NAME, containerResponseEntity.getContainerName());
        assertEquals(releaseId, containerResponseEntity.getReleasedId());
        KieServerAssert.assertNullOrEmpty("Container configuration was found.", containerResponseEntity.getConfigs().keySet());
    }

    protected ServerTemplate createServerTemplate() {
        return createServerTemplate(kieServerInfo.getServerId(), kieServerInfo.getName(), kieServerInfo.getLocation());
    }

    protected ServerTemplate createServerTemplate(String id, String name, String location) {
        ServerTemplate serverTemplate = new ServerTemplate();
        serverTemplate.setId( id );
        serverTemplate.setName( name );

        serverTemplate.addServerInstance(ModelFactory.newServerInstanceKey(serverTemplate.getId(), location));
        mgmtControllerClient.saveServerTemplate(serverTemplate);

        return serverTemplate;
    }

    protected void checkContainer(ContainerSpec container, KieContainerStatus status) {
        assertNotNull(container);
        assertEquals(CONTAINER_ID, container.getId());
        assertEquals(releaseId, container.getReleasedId());
        assertEquals(status, container.getStatus());
    }

    protected void checkContainerConfig(String serverTemplateId, String containerId, ContainerConfig... configs) {
        Map<Capability, ContainerConfig> configMap = mgmtControllerClient.getContainerInfo(serverTemplateId, containerId).getConfigs();
        assertNotNull(configMap);

        for (ContainerConfig config : configs) {
            if (config instanceof ProcessConfig) {
                ProcessConfig pc = (ProcessConfig) config;
                ProcessConfig processConfig = (ProcessConfig) configMap.get(Capability.PROCESS);
                assertNotNull(processConfig);
                assertEquals(pc.getKBase(), processConfig.getKBase());
                assertEquals(pc.getKSession(), processConfig.getKSession());
                assertEquals(pc.getMergeMode(), processConfig.getMergeMode());
                assertEquals(pc.getRuntimeStrategy(), processConfig.getRuntimeStrategy());
            } else if (config instanceof RuleConfig) {
                RuleConfig rc = (RuleConfig) config;
                RuleConfig ruleConfig = (RuleConfig) configMap.get(Capability.RULE);
                assertNotNull(ruleConfig);
                assertEquals(rc.getPollInterval(), ruleConfig.getPollInterval());
                assertEquals(rc.getScannerStatus(), ruleConfig.getScannerStatus());
            }
        }
    }

    protected void checkServerTemplate(ServerTemplate actual) {
        assertNotNull(actual);
        assertEquals(kieServerInfo.getServerId(), actual.getId());
        assertEquals(kieServerInfo.getName(), actual.getName());
    }
}
