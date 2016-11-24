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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.task.model.Status;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.*;

public class ClusterTasksServiceIntegrationTest extends ClusterClientBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    @Before
    public void deployContainer() {
        ContainerSpec containerToDeploy = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, templateOne, releaseId, KieContainerStatus.STOPPED, new HashMap());
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerToDeploy);
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
    }

    @Test
    public void testStartAndStopTask() {
        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);
        try {
            List<TaskSummary> taskList = taskCharlieClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            TaskSummary taskSummary = taskList.get(0);
            checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);

            taskList = taskBravoClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);

            taskCharlieClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

            taskList = taskCharlieClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            checkTaskNameAndStatus(taskSummary, "First task", Status.InProgress);

            taskList = taskBravoClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            checkTaskNameAndStatus(taskSummary, "First task", Status.InProgress);

            //turn off server and complete task on other one
            turnOffCharlieServer();

            taskBravoClient.stopTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

            taskList = taskBravoClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);

            try {
                //turn on back server and check if task is stopped
                turnOnCharlieServer();
            } catch (InterruptedException |MalformedURLException ex) {
            fail("Server was not started due:\n" + ex);
            }

            taskList = taskCharlieClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);
        } finally {
            processBravoClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    @Ignore
    public void testFailTask() {
        Long processInstanceId = processCharlieClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);
        try {
            List<TaskSummary> taskList = taskCharlieClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            TaskSummary taskSummary = taskList.get(0);
            assertEquals("First task", taskSummary.getName());

            // startTask and completeTask task
            taskCharlieClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

            turnOffCharlieServer();

            Map<String, Object> taskOutcome = new HashMap<String, Object>();
            taskOutcome.put("string_", "my custom data");
//            taskOutcome.put("person_", createInstance(PERSON_CLASS_NAME, USER_MARY));

            taskBravoClient.failTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, taskOutcome);

            taskList = taskBravoClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            assertEquals("Second task", taskSummary.getName());

            try {
                turnOnCharlieServer();
            } catch (InterruptedException|MalformedURLException ex) {
               fail("Server was not started due:\n" + ex);
            }

            taskList = taskCharlieClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertNotNull(taskList);
            assertEquals(1, taskList.size());
            taskSummary = taskList.get(0);
            assertEquals("Second task", taskSummary.getName());
        } finally {
            processBravoClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    @Ignore
    public void testTaskWithEscalation() throws Exception {
        //separate class
        turnOffCharlieServer();
        
        Long processInstanceId = processBravoClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK_ESCALATION); //make a longer version
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);
        
        turnOffBravoServer();
        //wait a while for task escalation
        Thread.sleep(5000l);
        //start other server
        turnOnCharlieServer();
        List<TaskSummary> taskList = taskCharlieClient.findTasksByStatusByProcessInstanceId(processInstanceId, null, 0, 10);
        assertNotNull(taskList);
        assertEquals(1, taskList.size());
        TaskSummary taskSummary = taskList.get(0);
        assertEquals(CONTAINER_ID, taskSummary.getContainerId());
        assertEquals(processInstanceId, taskSummary.getProcessInstanceId());
        assertEquals("Failed" /* or "fail" */, taskSummary.getStatus()); //search for better one
        
        ProcessInstance pi = processCharlieClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(pi);
        assertEquals(processInstanceId, pi.getId());
        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_SUSPENDED, pi.getState().intValue());
        
        //human task with escalation
        //start process and tunr of server instances
        //after restart task should be escalated -> ended
    }

    private void checkTaskNameAndStatus(TaskSummary taskSummary, String name, Status status) {
        assertNotNull(taskSummary);
        assertEquals(name, taskSummary.getName());
        assertEquals(status.toString(), taskSummary.getStatus());
    }
}
