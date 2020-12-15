/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.runtime;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.core.common.spring.security.LocalSpringSecurityManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
@DirtiesContext
public class ModelsContextTest {

    private final Logger logger = LoggerFactory.getLogger(ModelsContextTest.class);

    public static final String PROJECT_MODEL_DEFINITION_KEY = "Process_BKx8PFXad";
    public static final String SUBPROJECT_MODEL_DEFINITION_KEY = "Process_i5ugxdvpu";
    public static final String FILE_MODEL_DEFINITION_KEY = "Process_t1-1M1g1T";

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private SecurityManager securityManager;

    @Test
    public void contextLoads() {
        // Проверка того, правильно ли мы инциализировались
        // и можем получать пользователей из памяти
        assertThat(securityManager).isInstanceOf(LocalSpringSecurityManager.class);
    }

    @Test
    public void getModels() throws Exception {
        securityUtil.logInAs("system");

        // Проверка того, что в памяти найдены известные модели
        Page<ProcessDefinition> processDefinitionPage = processRuntime.processDefinitions(Pageable.of(0, 10));
        for (ProcessDefinition pd : processDefinitionPage.getContent()) {
            logger.info("> Found Process: " + pd);
            switch (pd.getName()) {
                case "project-model":
                    assertThat(pd.getKey()).isEqualTo(PROJECT_MODEL_DEFINITION_KEY);
                    break;
                case "subproject-model":
                    assertThat(pd.getKey()).isEqualTo(SUBPROJECT_MODEL_DEFINITION_KEY);
                    break;
                case "file-model":
                    assertThat(pd.getKey()).isEqualTo(FILE_MODEL_DEFINITION_KEY);
                    break;
            }
        }
    }
}
