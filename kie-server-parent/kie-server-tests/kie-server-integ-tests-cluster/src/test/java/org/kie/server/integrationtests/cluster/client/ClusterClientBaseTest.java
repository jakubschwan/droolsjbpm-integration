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

import org.junit.Before;
import org.kie.api.runtime.KieContainer;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.integrationtests.cluster.ClusterBaseTest;

public abstract class ClusterClientBaseTest extends ClusterBaseTest {

    protected static KieContainer kieContainer;

    protected ProcessServicesClient processCharlieClient;
    protected UserTaskServicesClient taskCharlieClient;
    protected QueryServicesClient queryCharlieClient;
    protected JobServicesClient jobServicesCharlieClient;
    protected DocumentServicesClient documentCharlieClient;

    protected ProcessServicesClient processBravoClient;
    protected UserTaskServicesClient taskBravoClient;
    protected QueryServicesClient queryBravoClient;
    protected JobServicesClient jobServicesBravoClient;
    protected DocumentServicesClient documentBravoClient;

    @Before
    public void setupClusterClients() {
        processCharlieClient = clientCharlie.getServicesClient(ProcessServicesClient.class);
        taskCharlieClient = clientCharlie.getServicesClient(UserTaskServicesClient.class);
        queryCharlieClient = clientCharlie.getServicesClient(QueryServicesClient.class);
        jobServicesCharlieClient = clientCharlie.getServicesClient(JobServicesClient.class);
        documentCharlieClient = clientCharlie.getServicesClient(DocumentServicesClient.class);

        processBravoClient = clientBravo.getServicesClient(ProcessServicesClient.class);
        taskBravoClient = clientBravo.getServicesClient(UserTaskServicesClient.class);
        queryBravoClient = clientBravo.getServicesClient(QueryServicesClient.class);
        jobServicesBravoClient = clientBravo.getServicesClient(JobServicesClient.class);
        documentBravoClient = clientBravo.getServicesClient(DocumentServicesClient.class);
    }
}
