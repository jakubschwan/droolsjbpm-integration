/*
 * Copyright 2017 JBoss by Red Hat.
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
package org.kie.server.integrationtests.cluster.controller;

import java.net.MalformedURLException;
import java.net.URL;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.DeployableMonitor;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.deployer.DeployerWatchdog;
import org.codehaus.cargo.container.weblogic.WebLogicPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployer.DefaultDeployerFactory;
import org.kie.server.integrationtests.config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieServerClusterController {

    protected static Logger logger = LoggerFactory.getLogger(KieServerClusterController.class);

    private Configuration configuration;
    private Container container;
    private Deployer deployer;

    private WAR kieServerWar;
    private boolean isDeployStarted;
    private DeployableMonitor monitor;
    private DeployerWatchdog watchdog;

    String warFilePath;
    //TODO: add some loggers

    //TODO: add ClusterControllerFactory to allow only eap and wildfly builds!
    public KieServerClusterController(String cargoContainerId, String containerPort, String managmentPort, String warFilePath, String serverUrl, String configHome) {

        //set configuration for reomte server
        configuration = setCongiguration(cargoContainerId, containerPort, managmentPort, configHome);

        container = (Container) new DefaultContainerFactory().createContainer(
                cargoContainerId, ContainerType.INSTALLED, configuration);

        deployer = new DefaultDeployerFactory().createDeployer(container, DeployerType.INSTALLED); //if not work create wildfly10 deployer instance

        kieServerWar = (WAR) new DefaultDeployableFactory().createDeployable(container.getId(), warFilePath, DeployableType.WAR);
        //kieServerWar.setContext(System.getProperty("kie.server.context")); //use this by default
        this.warFilePath = warFilePath;

        try {
            //TODO: get rid of try catch block
            System.out.println("Kie Server URL: " + serverUrl);
            monitor = new URLDeployableMonitor(new URL(serverUrl), 60000);
        } catch (MalformedURLException ex) {
            logger.error("Wrong URL", ex);
        }

        watchdog = new DeployerWatchdog(monitor);

        isDeployStarted = true;

    }

    private Configuration setCongiguration(String cargoContainerId, String containerPort, String managmentPort, String configHome) {
        Configuration config = new DefaultConfigurationFactory().createConfiguration(
                cargoContainerId, ContainerType.INSTALLED, ConfigurationType.EXISTING, configHome);
        config.setProperty("cargo.jboss.management-http.port", managmentPort);

        config.setProperty(ServletPropertySet.PORT, containerPort);

        config.setProperty("cargo.jboss.configuration", "standalone-full");

        if (TestConfig.isCargoRemoteUsernameProvided()) {
            config.setProperty(RemotePropertySet.USERNAME, TestConfig.getCargoRemoteUsername());
        } else {
            config.setProperty(RemotePropertySet.USERNAME, TestConfig.getUsername());
        }
        
        if (TestConfig.isCargoRemotePasswordProvided()) {
            config.setProperty(RemotePropertySet.PASSWORD, TestConfig.getCargoRemotePassword());
        } else {
            config.setProperty(RemotePropertySet.PASSWORD, TestConfig.getPassword());
        }

        if (TestConfig.isWebLogicHomeProvided()) {
            String wlserverHome = TestConfig.getWebLogicHome().matches(".*/wlserver")
                    ? TestConfig.getWebLogicHome() : TestConfig.getWebLogicHome() + "/wlserver";
            configuration.setProperty(WebLogicPropertySet.LOCAL_WEBLOGIC_HOME, wlserverHome);
        }

        System.out.println("Properties:\n" + config.getProperties());
        return config;
    }

    public boolean isContainerStarted() {
        return container.getState().equals(State.STARTED);
    }

    public boolean isContainerStopped() {
        return container.getState().equals(State.STOPPED);
    }

    /**
     * Deploy KieServer on container
     */
    public void startDeploy() {
//        deployer.start(kieServerWar,monitor); //not supported

        kieServerWar = (WAR) new DefaultDeployableFactory().createDeployable(container.getId(), warFilePath, DeployableType.WAR);

        logger.info("Deploying {}", kieServerWar);
        System.out.println("Deploying " + kieServerWar);
        System.out.println(kieServerWar.getContext());
        System.out.println(kieServerWar.getExtraClasspath());
        System.out.println(kieServerWar.getName());
        System.out.println(kieServerWar.getType());
        System.out.println(kieServerWar.getFile());
        deployer.deploy(kieServerWar, monitor);

        watchdog.watchForAvailability();
        isDeployStarted = true;
    }

    public void restartDeploy() {
        logger.info("Reddeploying {}", kieServerWar);
        deployer.redeploy(kieServerWar, monitor);
        watchdog.watchForAvailability();
        isDeployStarted = true;
    }

    public void stopDeploy() {
//        deployer.stop(kieServerWar, monitor); //not supported

        logger.info("Undeploying {}", kieServerWar);
        System.out.println("Undeploying " + kieServerWar);
        System.out.println(kieServerWar.getContext());
        System.out.println(kieServerWar.getExtraClasspath());
        System.out.println(kieServerWar.getName());
        System.out.println(kieServerWar.getType());
        System.out.println(kieServerWar.getFile());
        deployer.undeploy(kieServerWar, monitor);

        watchdog.watchForUnavailability();
        isDeployStarted = false;
    }

    public boolean isDeployStarted() {
        return isDeployStarted;
    }

    private void undeployWarFile(String context, String warFilePath) {
        WAR deployable = (WAR) new DefaultDeployableFactory().createDeployable(container.getId(), warFilePath, DeployableType.WAR);
        deployable.setContext(context); //context = "kie-server-services"
        deployer.undeploy(deployable);
    }

    private void deployWarFile(String context, String warFilePath) {
        WAR deployable = (WAR) new DefaultDeployableFactory().createDeployable(container.getId(), warFilePath, DeployableType.WAR);
        deployable.setContext(context);
        deployer.deploy(deployable, monitor);
    }

}
