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
package org.kie.server.integrationtests.cluster.loadbalancer.jbpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.cluster.loadbalancer.ClusterLoadbalancerBaseTest;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.*;
import org.kie.server.integrationtests.config.TestConfig;

public class ClusterLoadbalancerProcessServiceIntegrationTest extends ClusterLoadbalancerBaseTest {

    @Test
    @Category(Smoke.class)
    public void testGetProcessInstance() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            ProcessInstance expectedInstance = ProcessInstance.builder()
                    .id(processInstanceId)
                    .state(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)
                    .processId(PROCESS_ID_SIMPLE_SIGNAL)
                    .processName(PROCESS_NAME_SIMPLE_SIGNAL)
                    .processVersion("1.0")
                    .containerId(CONTAINER_ID)
                    .processInstanceDescription(PROCESS_NAME_SIMPLE_SIGNAL)
                    .initiator(TestConfig.getUsername())
                    .parentInstanceId(-1l)
                    .build();

            assertNotNull(processInstance);
            assertEquals(expectedInstance.getId(), processInstance.getId());
            assertEquals(expectedInstance.getState(), processInstance.getState());
            assertEquals(expectedInstance.getProcessId(), processInstance.getProcessId());
            assertEquals(expectedInstance.getProcessName(), processInstance.getProcessName());
            assertEquals(expectedInstance.getProcessVersion(), processInstance.getProcessVersion());
            assertEquals(expectedInstance.getContainerId(), processInstance.getContainerId());
            assertEquals(expectedInstance.getProcessInstanceDescription(), processInstance.getProcessInstanceDescription());
            assertEquals(expectedInstance.getInitiator(), processInstance.getInitiator());
            assertEquals(expectedInstance.getParentId(), processInstance.getParentId());
            assertNotNull(processInstance.getCorrelationKey());
            assertNotNull(processInstance.getDate());

            Map<String, Object> variables = processInstance.getVariables();
            assertNull(variables);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testSignalProcessInstances() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);
        assertNotNull(processInstanceId2);
        assertTrue(processInstanceId2.longValue() > 0);

        List<Long> processInstanceIds = new ArrayList<Long>();
        processInstanceIds.add(processInstanceId);
        processInstanceIds.add(processInstanceId2);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);
            checkAvailableSignals(CONTAINER_ID, processInstanceId2);

            processClient.signalProcessInstances(CONTAINER_ID, processInstanceIds, "event", null);
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
            e.printStackTrace();
            fail(e.getMessage());
        }

    }
    
    @Test
    public void testSignalContainer() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIMPLE_SIGNAL);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            processClient.signal(CONTAINER_ID, "event", "");

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }
    
    @Test
    @Ignore //add for test kjar with start timer process
    public void testSignalStartProcess() throws Exception {
        try {

            List<Integer> status = new ArrayList<Integer>();
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

            List<ProcessInstance> processInstances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, status, 0, 10);
            int initial = processInstances.size();

           // Object person = createPersonInstance(USER_JOHN);
            processClient.signal(CONTAINER_ID, "start-process", "");

            processInstances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, status, 0, 10);
            assertNotNull(processInstances);
            assertEquals(initial + 1, processInstances.size());

        } catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    private void checkAvailableSignals(String containerId, Long processInstanceId) {
        List<String> availableSignals = processClient.getAvailableSignals(containerId, processInstanceId);
        assertNotNull(availableSignals);
        assertEquals(1, availableSignals.size());
        assertTrue(availableSignals.contains("event"));
    }
    
    
}
