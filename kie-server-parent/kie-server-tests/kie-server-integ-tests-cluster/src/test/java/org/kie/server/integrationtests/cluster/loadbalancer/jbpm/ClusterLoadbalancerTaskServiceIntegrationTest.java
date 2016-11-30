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

import java.net.MalformedURLException;

import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.task.model.Status;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.integrationtests.cluster.loadbalancer.ClusterLoadbalancerBaseTest;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.CONTAINER_ID;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.PROCESS_ID_USERTASK;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.USER_YODA;

public class ClusterLoadbalancerTaskServiceIntegrationTest extends ClusterLoadbalancerBaseTest {

        private Long processInstanceId;
        
    @Before
    public void startProcess() throws Exception {
        processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);
    }

    @After
    public void afterTestStopProcess() {
        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
    }

    @Test
    public void testStartAndStopTask() {
        List<TaskSummary> taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertNotNull(taskList);
        assertEquals(1, taskList.size());
        TaskSummary taskSummary = taskList.get(0);
        checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);

        taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

        taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertNotNull(taskList);
        assertEquals(1, taskList.size());
        taskSummary = taskList.get(0);
        checkTaskNameAndStatus(taskSummary, "First task", Status.InProgress);

//        //turn off server and complete task on other one
//        turnOffCharlieServer();

        taskClient.stopTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);

        taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertNotNull(taskList);
        assertEquals(1, taskList.size());
        taskSummary = taskList.get(0);
        checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);

//        try {
//            //turn on back server and check if task is stopped
//            turnOnCharlieServer();
//        } catch (InterruptedException | MalformedURLException ex) {
//            fail("Server was not started due:\n" + ex);
//        }

        taskList = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertNotNull(taskList);
        assertEquals(1, taskList.size());
        taskSummary = taskList.get(0);
        checkTaskNameAndStatus(taskSummary, "First task", Status.Reserved);
    }

    private void checkTaskNameAndStatus(TaskSummary taskSummary, String name, Status status) {
        assertNotNull(taskSummary);
        assertEquals(name, taskSummary.getName());
        assertEquals(status.toString(), taskSummary.getStatus());
    }
}
