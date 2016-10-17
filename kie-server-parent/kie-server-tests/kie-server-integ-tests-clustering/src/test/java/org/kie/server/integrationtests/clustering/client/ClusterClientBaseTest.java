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

import org.junit.Before;
import org.kie.server.client.DocumentServicesClient;
import org.kie.server.client.JobServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.kie.server.integrationtests.clustering.ClusterBaseTest;

public abstract class ClusterClientBaseTest extends ClusterBaseTest {
    // set clients for deploying and excluding tasks

    protected static final String USER_YODA = "yoda";
    protected static final String USER_JOHN = "john";
    protected static final String USER_ADMINISTRATOR = "Administrator";
    protected static final String USER_MARY = "mary";

    protected static final String PERSON_CLASS_NAME = "org.jbpm.data.Person";

    protected static final String CONTAINER_ID = "definition-project";
    protected static final String CONTAINER_NAME = "definition-project";

    protected static final String PROCESS_ID_USERTASK = "definition-project.usertask";
    protected static final String PROCESS_ID_EVALUATION = "definition-project.evaluation";
    protected static final String PROCESS_ID_CALL_EVALUATION = "definition-project.call-evaluation";
    protected static final String PROCESS_ID_GROUPTASK = "definition-project.grouptask";
    protected static final String PROCESS_ID_ASYNC_SCRIPT = "AsyncScriptTask";
    protected static final String PROCESS_ID_TIMER = "definition-project.timer-process";
    protected static final String PROCESS_ID_SIGNAL_PROCESS = "definition-project.signalprocess";
    protected static final String PROCESS_ID_SIGNAL_START = "signal-start";
    protected static final String PROCESS_ID_CUSTOM_TASK = "customtask";
    protected static final String PROCESS_ID_USERTASK_ESCALATION = "humanTaskEscalation";
    protected static final String PROCESS_ID_XYZ_TRANSLATIONS = "xyz-translations";

    protected static final long SERVICE_TIMEOUT = 30000;
    protected static final long TIMEOUT_BETWEEN_CALLS = 200;

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
    public void setupClusterClients() throws Exception {
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

    //copy from RestJmsBaseTest
    protected Object createInstance(String objectClassIdentifier, Object... constructorParameters) {
        Class<?>[] parameterClasses = new Class[constructorParameters.length];
        for (int i = 0; i < constructorParameters.length; i++) {
            parameterClasses[i] = constructorParameters[i].getClass();
        }

        try {
            Class<?> clazz = extraClasses.get(objectClassIdentifier);
            if (clazz != null) {
                Object object = clazz.getConstructor(parameterClasses).newInstance(constructorParameters);
                return object;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create object due " + e.getMessage(), e);
        }
        throw new RuntimeException("Instantiated class isn't defined in extraClasses set. Please define it first.");
    }
}
