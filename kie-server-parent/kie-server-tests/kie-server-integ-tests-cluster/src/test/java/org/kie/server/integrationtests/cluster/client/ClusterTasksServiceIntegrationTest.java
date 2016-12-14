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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.task.model.Status;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.*;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class ClusterTasksServiceIntegrationTest extends ClusterClientBaseTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());
    }

    @Before
    public void deployContainer() throws Exception {
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


    private void checkTaskNameAndStatus(TaskSummary taskSummary, String name, Status status) {
        assertNotNull(taskSummary);
        assertEquals(name, taskSummary.getName());
        assertEquals(status.toString(), taskSummary.getStatus());
    }
}
