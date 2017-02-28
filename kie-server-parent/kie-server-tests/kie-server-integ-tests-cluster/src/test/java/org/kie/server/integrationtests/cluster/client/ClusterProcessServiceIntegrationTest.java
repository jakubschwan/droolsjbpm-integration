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
package org.kie.server.integrationtests.cluster.client;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.internal.runtime.conf.MergeMode;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.*;

public class ClusterProcessServiceIntegrationTest extends ClusterClientBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");
    private static ReleaseId startTimerReleaseId = new ReleaseId("org.kie.server.testing", "start-timer",
            "1.0.0.Final");

    private String startTimerContainerId = new String("start-timer");

    private final List<Integer> statusList = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED, org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/start-timer").getFile());
    }

    //fail over and normal
    
    @Before
    public void deployContainer() throws Exception {
        Map<Capability, ContainerConfig> config = new HashMap<>();
        config.put(Capability.PROCESS, new ProcessConfig(RuntimeStrategy.SINGLETON.toString(), "", "", MergeMode.MERGE_COLLECTIONS.toString()));

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, kieServerTemplate, releaseId, KieContainerStatus.STOPPED, config);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(kieServerTemplate.getId(), CONTAINER_ID);

        KieServerSynchronization.waitForKieServerSynchronization(primaryClient, 1);
        KieServerSynchronization.waitForKieServerSynchronization(secondaryClient, 1);

        abortAllActiveProcessInstances();
    }

    @After
    public void cleantContainer() {
        mgmtControllerClient.stopContainer(kieServerTemplate.getId(), CONTAINER_ID);
    }

    @Test
    @Category(Smoke.class)
    public void testCompleteProcess() {
        Long processInstanceId = primaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ProcessInstance pi = primaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());
        pi = secondaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        try {
            primaryProcessClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "event", null);

            pi = secondaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            secondaryProcessClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testStartSignalProcessInstance() {
        List<ProcessInstance> processInstances = primaryQueryClient.findProcessInstancesByProcessId(PROCESS_ID_SIMPLE_START, statusList, 0, 50);
        int initial = processInstances.size();
        processInstances = secondaryQueryClient.findProcessInstancesByProcessId(PROCESS_ID_SIMPLE_START, statusList, 0, 50);
        assertEquals(initial, processInstances.size());

        primaryProcessClient.signal(CONTAINER_ID, "signal-start", "signal-start");
        checkSignalStartProcessInstances(initial + 1);

        secondaryProcessClient.signal(CONTAINER_ID, "signal-start", "signal-start");
        checkSignalStartProcessInstances(initial + 2);
    }

    @Test
    @Ignore //need quartz tables to run OK
    public void testProcessInstanceWithTimer() {
        Long processInstanceId = primaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ProcessInstance pi = primaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());
        pi = secondaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        turnOffPrimaryServer();

        try {
            KieServerSynchronization.waitForProcessInstanceToFinish(secondaryProcessClient, CONTAINER_ID, processInstanceId);

            pi = secondaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            secondaryProcessClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testProcessTimer() {
        Long processInstanceId = primaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ProcessInstance pi = primaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        try {
            KieServerSynchronization.waitForProcessInstanceToFinish(primaryProcessClient, CONTAINER_ID, processInstanceId);

            pi = primaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            primaryProcessClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testStartMultipleProcessInstacesAndAbortThem() {
        List<ProcessInstance> startProcesses = secondaryQueryClient.findProcessInstances(0, 10);
        KieServerAssert.assertNullOrEmpty("Before start there are some process instances!", startProcesses);
        startProcesses = primaryQueryClient.findProcessInstances(0, 10);
        KieServerAssert.assertNullOrEmpty("Before start there are some process instances!", startProcesses);

        List<Long> processInstancesId = new ArrayList<Long>();
        processInstancesId.add(secondaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        processInstancesId.add(primaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        processInstancesId.add(secondaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        processInstancesId.add(primaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));

        List<ProcessInstance> processInstances = secondaryQueryClient.findProcessInstances(0, 10);
        assertNotNull(processInstances);
        assertEquals(processInstancesId.size(), processInstances.size());
        for (ProcessInstance processInstance : processInstances) {
            assertTrue(processInstancesId.contains(processInstance.getId()));
        }
        processInstances = primaryQueryClient.findProcessInstances(0, 10);
        assertNotNull(processInstances);
        assertEquals(processInstancesId.size(), processInstances.size());
        for (ProcessInstance processInstance : processInstances) {
            assertTrue(processInstancesId.contains(processInstance.getId()));
        }

        primaryProcessClient.abortProcessInstances(CONTAINER_ID, processInstancesId);

        processInstances = primaryQueryClient.findProcessInstances(0, 10);
        KieServerAssert.assertNullOrEmpty("ProcessInstances were not corectly aborted", processInstances);
        processInstances = secondaryQueryClient.findProcessInstances(0, 10);
        KieServerAssert.assertNullOrEmpty("ProcessInstances were not corectly aborted", processInstances);
    }

    @Test
    @Ignore
    public void testSignalContainer() {
        Long processInstanceId1 = secondaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);
        Long processInstanceId2 = primaryProcessClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);

        assertNotNull(processInstanceId1);
        assertTrue(processInstanceId1.longValue() > 0);
        assertNotNull(processInstanceId2);
        assertTrue(processInstanceId2.longValue() > 0);

        try {
            //send signals to container from diferent client
            secondaryProcessClient.signal(CONTAINER_ID, "evnet", "evnet"); //is it right??
            Thread.sleep(2000);
            //all process, that expect signal should be completed
            ProcessInstance pi = secondaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId2);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
            pi = primaryProcessClient.getProcessInstance(CONTAINER_ID, processInstanceId1);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            secondaryProcessClient.abortProcessInstance(CONTAINER_ID, processInstanceId1);
            primaryProcessClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    @Ignore //issue with starting processes, after stop process still creating new instances -- RHBPMS-4389
    public void startProcessInstanceWithTimer() throws Exception {
        Map<Capability, ContainerConfig> config = new HashMap<>();
        config.put(Capability.PROCESS, new ProcessConfig(RuntimeStrategy.SINGLETON.toString(), "", "", MergeMode.MERGE_COLLECTIONS.toString()));

        ContainerSpec containerToDeploy = new ContainerSpec(startTimerContainerId, "start timer", kieServerTemplate, startTimerReleaseId, KieContainerStatus.STOPPED, config);
        mgmtControllerClient.saveContainerSpec(kieServerTemplate.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(kieServerTemplate.getId(), startTimerContainerId);
        long startTimeInMillis = System.currentTimeMillis();
        int processInstanceCount = 0;

        KieServerSynchronization.waitForKieServerSynchronization(primaryClient, 1);
        KieServerSynchronization.waitForKieServerSynchronization(secondaryClient, 1);

        turnOffPrimaryServer();

        List<ProcessInstance> processInstancesList = secondaryQueryClient.findProcessInstances(0, 50);
        assertNotNull(processInstancesList);
        assertTrue(processInstanceCount < processInstancesList.size());
        processInstanceCount = processInstancesList.size();
        //normal starting on other template
        try {
            turnOnPrimaryServer();
        } catch (InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }
        turnOffSecondaryServer();

        processInstancesList = secondaryQueryClient.findProcessInstances(0, 50);
        assertNotNull(processInstancesList);
        assertTrue(processInstanceCount < processInstancesList.size());
        processInstanceCount = processInstancesList.size();

        turnOffPrimaryServer();

        try {
            turnOnSecondaryServer();
        } catch (InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }

        processInstancesList = secondaryQueryClient.findProcessInstances(0, 50);
        assertNotNull(processInstancesList);
        assertTrue(processInstanceCount < processInstancesList.size());
        processInstanceCount = processInstancesList.size();

        turnOffSecondaryServer();
        mgmtControllerClient.stopContainer(kieServerTemplate.getId(), startTimerContainerId);
        long stopTimeInMillis = System.currentTimeMillis();

        try {
            turnOnSecondaryServer();
        } catch (InterruptedException | MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
        }

        processInstancesList = secondaryQueryClient.findProcessInstances(0, 50);
        assertNotNull(processInstancesList);
        assertTrue(processInstanceCount == processInstancesList.size());

        long timeRunOfTestInMillis = stopTimeInMillis - startTimeInMillis;
        long countOfProcessByTime = (timeRunOfTestInMillis / 1000) / 10; //to sec then cound of sec between start of processses

        processInstancesList = secondaryQueryClient.findProcessInstances(0, 50);
        assertNotNull(processInstancesList);
        assertEquals(countOfProcessByTime, processInstancesList.size());

        //separete project with only this process deploying process evry 5/10 seconds (decide from speed of tests)
        //need new process - update timer
        //processes strated by timer (5 instances evry second)
        //undeploy one server instance (if ok)
        // other scenario - undeploy both after redeploy check if process are completed / still starting
    }

    private void assertProcessInstance(ProcessInstance expected, ProcessInstance actual) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
        assertEquals(expected.getProcessId(), actual.getProcessId());
        assertEquals(expected.getProcessName(), actual.getProcessName());
        assertEquals(expected.getProcessVersion(), actual.getProcessVersion());
        assertEquals(expected.getContainerId(), actual.getContainerId());
        assertEquals(expected.getProcessInstanceDescription(), actual.getProcessInstanceDescription());
        assertEquals(expected.getInitiator(), actual.getInitiator());
        assertEquals(expected.getParentId(), actual.getParentId());
        assertNotNull(actual.getCorrelationKey());
        assertNotNull(actual.getDate());
    }

    private void checkSignalStartProcessInstances(int expected) {
        List<ProcessInstance> processInstances = primaryQueryClient.findProcessInstancesByProcessId(PROCESS_ID_SIMPLE_START, statusList, 0, 50);
        assertNotNull(processInstances);
        assertEquals(expected, processInstances.size());
        processInstances = secondaryQueryClient.findProcessInstancesByProcessId(PROCESS_ID_SIMPLE_START, statusList, 0, 50);
        assertNotNull(processInstances);
        assertEquals(expected, processInstances.size());
    }

    private void abortAllActiveProcessInstances() {
        List<ProcessInstance> activeInstances = secondaryQueryClient.findProcessInstances(0, 10);
        if (!activeInstances.isEmpty()) {
            for (ProcessInstance pi : activeInstances) {
                secondaryProcessClient.abortProcessInstance(CONTAINER_ID, pi.getId());
            }
        }
        activeInstances = primaryQueryClient.findProcessInstances(0, 10);
        if (!activeInstances.isEmpty()) {
            for (ProcessInstance pi : activeInstances) {
                primaryProcessClient.abortProcessInstance(CONTAINER_ID, pi.getId());
            }
        }

    }
}
