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
package org.kie.server.integrationtests.cluster;

public class ClusterTestConstants {

    public static final String SECONDARY_URL_PROPERTY = "kie.server.cluster.offset.http.url";

    public static final String USER_YODA = "yoda";
    public static final String USER_JOHN = "john";
    public static final String USER_ADMINISTRATOR = "Administrator";
    public static final String USER_MARY = "mary";

    public static final String PERSON_CLASS_NAME = "org.jbpm.data.Person";

    public static final String CONTAINER_ID = "definition-project";
    public static final String CONTAINER_NAME = "definitionproject";

    public static final String PROCESS_ID_USERTASK = "definition-project.usertask";
    public static final String PROCESS_ID_EVALUATION = "definition-project.evaluation";
    public static final String PROCESS_ID_CALL_EVALUATION = "definition-project.call-evaluation";
    public static final String PROCESS_ID_GROUPTASK = "definition-project.grouptask";
    public static final String PROCESS_ID_ASYNC_SCRIPT = "AsyncScriptTask";
    public static final String PROCESS_ID_TIMER = "definition-project.timer-process";
    public static final String PROCESS_ID_SIGNAL_PROCESS = "definition-project.signalprocess";
    public static final String PROCESS_ID_SIMPLE_SIGNAL = "definition-project.simple-signal-process";
    public static final String PROCESS_ID_SIGNAL_START = "signal-start";
    public static final String PROCESS_ID_SIMPLE_START = "definition-project.simple-start-signal-process";
    public static final String PROCESS_ID_CUSTOM_TASK = "customtask";
    public static final String PROCESS_ID_USERTASK_ESCALATION = "humanTaskEscalation";
    public static final String PROCESS_ID_XYZ_TRANSLATIONS = "xyz-translations";

    public static final String PROCESS_NAME_SIMPLE_SIGNAL = "simple-signal-process";
    public static final String PROCESS_NAME_SIMPLE_START_SIGNAL = "simple-start-signal-process";
    
    public static final String PROCESS_ID_HIRING = "hiring";

    public static final String CONFIGURATION_DIR = "cluster.configuration.dir";

    public static final long SERVICE_TIMEOUT = 30000;
    public static final long TIMEOUT_BETWEEN_CALLS = 200;
}
