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
package org.kie.server.integrationtests.clustering.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.runtime.conf.MergeMode;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.KieServicesException;
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

public class ClusterProcessServiceIntegrationTest extends ClusterClientBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");
    protected static KieContainer kieContainer;

    private final List<Integer> statusList = Arrays.asList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED, org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Before
    public void deployContainer() throws Exception {
        Map<Capability, ContainerConfig> config = new HashMap<>();
        config.put(Capability.PROCESS, new ProcessConfig(RuntimeStrategy.SINGLETON.toString(), "", "", MergeMode.MERGE_COLLECTIONS.toString()));

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, templateOne, releaseId, KieContainerStatus.STOPPED, config);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);

        KieServerSynchronization.waitForKieServerSynchronization(clientCharlie, 1);
        KieServerSynchronization.waitForKieServerSynchronization(clientBravo, 1);
    }

    private void deployContainerOnTemplateTwo() throws Exception {
        Map<Capability, ContainerConfig> config = new HashMap<>();
        config.put(Capability.PROCESS, new ProcessConfig(RuntimeStrategy.SINGLETON.toString(), "", "", MergeMode.MERGE_COLLECTIONS.toString()));

        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, templateTwo, releaseId, KieContainerStatus.STOPPED, config);
        mgmtControllerClient.saveContainerSpec(templateTwo.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateTwo.getId(), CONTAINER_ID);

        KieServerSynchronization.waitForKieServerSynchronization(clientAlpha, 1);
    }

    @Test
    @Category(Smoke.class)
    public void testGetProcessInstance() throws Exception {
        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            ProcessInstance processInstance = processCharlieClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            ProcessInstance expectedInstance = ProcessInstance.builder()
                    .id(processInstanceId)
                    .state(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)
                    .processId(PROCESS_ID_SIGNAL_PROCESS)
                    .processName("signalprocess")
                    .processVersion("1.0")
                    .containerId(CONTAINER_ID)
                    .processInstanceDescription("signalprocess")
                    .initiator(TestConfig.getUsername())
                    .parentInstanceId(-1l)
                    .build();

            assertProcessInstance(expectedInstance, processInstance);
            processInstance = processBravoClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertProcessInstance(expectedInstance, processInstance);

        } finally {
            processCharlieClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    @Category(Smoke.class)
    public void testCompleteProcess() {
        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ProcessInstance pi = processCharlieClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());
        pi = processBravoClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        Object person = createInstance(PERSON_CLASS_NAME, USER_JOHN);
        try {
            processCharlieClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", person);
            processBravoClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal2", "My custom string event");

            pi = processBravoClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            processBravoClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testStartSignalProcessInstance() {
        try {

            List<ProcessInstance> processInstances = queryCharlieClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, statusList, 0, 50);
            int initial = processInstances.size();
            processInstances = queryBravoClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, statusList, 0, 50);
            assertEquals(initial, processInstances.size());

            Object person = createInstance(PERSON_CLASS_NAME, USER_JOHN);
            processCharlieClient.signal(CONTAINER_ID, "start-process", person);
            checkSignalStartProcessInstancesCount(initial + 1);

            processBravoClient.signal(CONTAINER_ID, "start-process", person);
            checkSignalStartProcessInstancesCount(initial + 2);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void startProcessInstanceWithTimer() {

    }

    @Test
    public void testSentSignalOnOtherTemplate() {
        try {
            deployContainerOnTemplateTwo();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Not able to deploy container on template two due " + ex.getMessage());
        }

        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ProcessServicesClient processAlphaClient = clientAlpha.getServicesClient(ProcessServicesClient.class);
        Long otherProcessInstanceId = processAlphaClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(otherProcessInstanceId);
        assertTrue(otherProcessInstanceId.longValue() > 0);

        ProcessInstance pi = processCharlieClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        pi = processAlphaClient.getProcessInstance(CONTAINER_ID, otherProcessInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        Object person = createInstance(PERSON_CLASS_NAME, USER_JOHN);
        try {
            processAlphaClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", person);
            processAlphaClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal2", "My custom string event");
            //check that process on templateOne still running
            pi = processBravoClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());
            pi = processCharlieClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());
            //check that process on templateTwo is completed
            pi = processAlphaClient.getProcessInstance(CONTAINER_ID, otherProcessInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            processBravoClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processAlphaClient.abortProcessInstance(CONTAINER_ID, otherProcessInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test(timeout = 60000)
    public void testProcessInstanceWithTimer() throws Exception {
        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ProcessInstance pi = processCharlieClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());
        pi = processBravoClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, pi.getState().intValue());

        turnOffCharlieServer();

        try {
            KieServerSynchronization.waitForProcessInstanceToFinish(processBravoClient, CONTAINER_ID, processInstanceId);

            pi = processBravoClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            processBravoClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }
    
    @Test
    public void testStartMultipleProcessInstacesAndAbortThem() {
        List<Long> processInstancesId = new ArrayList<Long>();
        processInstancesId.add(processBravoClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        processInstancesId.add(processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        processInstancesId.add(processBravoClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        processInstancesId.add(processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION));
        
        List<ProcessInstance> processInstances = queryBravoClient.findProcessInstances(0, 10);
        assertNotNull(processInstances);
        assertEquals(processInstancesId.size(), processInstances.size());
        for (ProcessInstance processInstance : processInstances) {
            assertTrue(processInstancesId.contains(processInstance.getId()));
        }
        processInstances = queryCharlieClient.findProcessInstances(0, 10);
        assertNotNull(processInstances);
        assertEquals(processInstancesId.size(), processInstances.size());
        for (ProcessInstance processInstance : processInstances) {
            assertTrue(processInstancesId.contains(processInstance.getId()));
        }

        processCharlieClient.abortProcessInstances(CONTAINER_ID, processInstancesId);

        processInstances = queryCharlieClient.findProcessInstances(0, 10);
        KieServerAssert.assertNullOrEmpty("ProcessInstances were not corectly aborted", processInstances);
        processInstances = queryBravoClient.findProcessInstances(0, 10);
        KieServerAssert.assertNullOrEmpty("ProcessInstances were not corectly aborted", processInstances);
    }
    
    @Test
    public void testManipulateProcessVariable() throws Exception {
        turnOffBravoServer();
        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            Object personVar = null;
            try {
                personVar = processCharlieClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertNull(personVar);

            personVar = createInstance(PERSON_CLASS_NAME, USER_JOHN);
            processCharlieClient.setProcessVariable(CONTAINER_ID, processInstanceId, "personData", personVar);

            personVar = processCharlieClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
            assertNotNull(personVar);
            assertEquals(USER_JOHN, valueOf(personVar, "name"));

turnOnBravoServer();
            
            processBravoClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "custom value");

            String stringVar = (String) processBravoClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
            assertNotNull(personVar);
            assertEquals("custom value", stringVar);

        } finally {
            processCharlieClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    @Ignore
    public void testProcessWithSubProcess() {

    }

    @Test
    @Ignore
    public void testProcessWithFailedSubProcess() {

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

    private void checkSignalStartProcessInstancesCount(int expected) throws Exception {
        List<ProcessInstance> processInstances = queryCharlieClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, statusList, 0, 50);
        assertNotNull(processInstances);
        assertEquals(expected, processInstances.size());
        processInstances = queryBravoClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, statusList, 0, 50);
        assertNotNull(processInstances);
        assertEquals(expected, processInstances.size());
    }
    
    private Object valueOf(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception e) {
            return null;
        }
    }
}
