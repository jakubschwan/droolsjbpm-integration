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
package org.kie.server.integrationtests.clustering.client.loadbalancer;

//load via client all process definition by list

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.kie.internal.runtime.conf.MergeMode;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.controller.api.model.spec.Capability;
import org.kie.server.controller.api.model.spec.ContainerConfig;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ProcessConfig;
import org.kie.server.integrationtests.clustering.ClusterTestConstants;
import static org.kie.server.integrationtests.clustering.ClusterTestConstants.CONTAINER_ID;
import static org.kie.server.integrationtests.clustering.ClusterTestConstants.CONTAINER_NAME;
import static org.kie.server.integrationtests.clustering.client.loadbalancer.ClusterLoadbalancerBaseTest.releaseId;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

public class ClusterLoadbalancerProcessDefinitionIntegrationTest extends ClusterLoadbalancerBaseTest {
    @Before
    public void beforeTests() throws Exception {
        Map<Capability, ContainerConfig> config = new HashMap<>();
        config.put(Capability.PROCESS, new ProcessConfig(RuntimeStrategy.SINGLETON.toString(), "", "", MergeMode.MERGE_COLLECTIONS.toString()));

        ContainerSpec containerSpec = new ContainerSpec(CONTAINER_ID, CONTAINER_NAME, templateOne, releaseId, KieContainerStatus.STOPPED, config);
        mgmtControllerClient.saveContainerSpec(templateOne.getId(), containerSpec);
    }
    
    public void testListProcessDefinition() throws Exception {
        List<ProcessDefinition> processDefinitions = queryClient.findProcesses(0, 20);
        KieServerAssert.assertNullOrEmpty("Founded process definition, but no containers are deployed", processDefinitions);
        
        mgmtControllerClient.startContainer(templateOne.getId(), CONTAINER_ID);
        KieServerSynchronization.waitForKieServerSynchronization(client, 1);
        
        processDefinitions = queryClient.findProcesses(0, 20);
        assertNotNull(processDefinitions);
        
        assertEquals(11, processDefinitions.size());
        List<String> processIds = collectDefinitions(processDefinitions);
        checkProcessDefinitions(processIds);

        // test paging of the result
        processDefinitions = queryClient.findProcesses(0, 3, QueryServicesClient.SORT_BY_NAME, true);

        assertNotNull(processDefinitions);
        assertEquals(3, processDefinitions.size());
        processIds = collectDefinitions(processDefinitions);
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_ASYNC_SCRIPT));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_SIGNAL_START));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_TIMER));

        processDefinitions = queryClient.findProcesses(0, 3, QueryServicesClient.SORT_BY_NAME, false);
        assertNotNull(processDefinitions);

        assertEquals(3, processDefinitions.size());
        processIds = collectDefinitions(processDefinitions);
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_XYZ_TRANSLATIONS));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_USERTASK));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_SIGNAL_PROCESS));
    }
    
    private List<String> collectDefinitions(List<ProcessDefinition> definitions) {
        List<String> ids = new ArrayList<String>();

        for (ProcessDefinition definition : definitions) {
            ids.add(definition.getId());
        }
        return ids;
    }
    
    private void checkProcessDefinitions(List<String> processIds) {
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_CALL_EVALUATION));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_EVALUATION));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_GROUPTASK));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_SIGNAL_PROCESS));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_USERTASK));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_CUSTOM_TASK));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_SIGNAL_START));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_ASYNC_SCRIPT));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_TIMER));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_USERTASK_ESCALATION));
        assertTrue(processIds.contains(ClusterTestConstants.PROCESS_ID_XYZ_TRANSLATIONS));
    }
}
