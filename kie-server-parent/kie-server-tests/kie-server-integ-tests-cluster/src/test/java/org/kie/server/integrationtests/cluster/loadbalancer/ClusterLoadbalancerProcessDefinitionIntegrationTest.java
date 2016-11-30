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
package org.kie.server.integrationtests.cluster.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.client.QueryServicesClient;
import static org.kie.server.integrationtests.cluster.ClusterTestConstants.*;

public class ClusterLoadbalancerProcessDefinitionIntegrationTest extends ClusterLoadbalancerBaseTest {

    @Test
    public void testListProcessDefinition() throws Exception {
        List<ProcessDefinition> processDefinitions = queryClient.findProcesses(0, 20);
        assertNotNull(processDefinitions);
        
        assertEquals(13, processDefinitions.size());
        List<String> processIds = collectDefinitions(processDefinitions);
        checkProcessDefinitions(processIds);

        processDefinitions = queryClient.findProcesses(0, 3, QueryServicesClient.SORT_BY_NAME, true);

        assertNotNull(processDefinitions);
        assertEquals(3, processDefinitions.size());
        processIds = collectDefinitions(processDefinitions);
        assertTrue(processIds.contains(PROCESS_ID_ASYNC_SCRIPT));
        assertTrue(processIds.contains(PROCESS_ID_SIGNAL_START));
        assertTrue(processIds.contains(PROCESS_ID_TIMER));

        processDefinitions = queryClient.findProcesses(0, 3, QueryServicesClient.SORT_BY_NAME, false);
        assertNotNull(processDefinitions);

        assertEquals(3, processDefinitions.size());
        processIds = collectDefinitions(processDefinitions);
        assertTrue(processIds.contains(PROCESS_ID_XYZ_TRANSLATIONS));
        assertTrue(processIds.contains(PROCESS_ID_USERTASK));
        assertTrue(processIds.contains(PROCESS_ID_SIMPLE_START));
    }
    
    private List<String> collectDefinitions(List<ProcessDefinition> definitions) {
        List<String> ids = new ArrayList<String>();

        for (ProcessDefinition definition : definitions) {
            ids.add(definition.getId());
        }
        return ids;
    }
    
    private void checkProcessDefinitions(List<String> processIds) {
        assertTrue(processIds.contains(PROCESS_ID_CALL_EVALUATION));
        assertTrue(processIds.contains(PROCESS_ID_EVALUATION));
        assertTrue(processIds.contains(PROCESS_ID_GROUPTASK));
        assertTrue(processIds.contains(PROCESS_ID_SIGNAL_PROCESS));
        assertTrue(processIds.contains(PROCESS_ID_SIMPLE_SIGNAL));
        assertTrue(processIds.contains(PROCESS_ID_USERTASK));
        assertTrue(processIds.contains(PROCESS_ID_CUSTOM_TASK));
        assertTrue(processIds.contains(PROCESS_ID_SIGNAL_START));
        assertTrue(processIds.contains(PROCESS_ID_SIMPLE_START));
        assertTrue(processIds.contains(PROCESS_ID_ASYNC_SCRIPT));
        assertTrue(processIds.contains(PROCESS_ID_TIMER));
        assertTrue(processIds.contains(PROCESS_ID_USERTASK_ESCALATION));
        assertTrue(processIds.contains(PROCESS_ID_XYZ_TRANSLATIONS));
        assertTrue(processIds.contains(PROCESS_ID_HIRING));
    }
}
