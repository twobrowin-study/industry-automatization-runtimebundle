/*
 * Copyright 2010-2020 Alfresco Software, Ltd.
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


import com.github.fridujo.rabbitmq.mock.compatibility.MockConnectionFactoryFactory;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.runtime.shared.security.*;
import org.activiti.core.common.spring.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Configuration
public class TestConfiguration {

    private final Logger logger = LoggerFactory.getLogger(TestConfiguration.class);

    @Bean
    public CachingConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(MockConnectionFactoryFactory.build());
    }

    @Bean
    public GrantedAuthoritiesResolver grantedAuthoritiesResolver() {
        return new SimpleGrantedAuthoritiesResolver();
    }

    @Bean
    public GrantedAuthoritiesGroupsMapper grantedAuthoritiesGroupsMapper() {
        return new SimpleGrantedAuthoritiesGroupsMapper();
    };

    @Bean
    public GrantedAuthoritiesRolesMapper grantedAuthoritiesRolesMapper() {
        return new SimpleGrantedAuthoritiesRolesMapper();
    }

    @Bean
    public SecurityContextPrincipalProvider securityContextPrincipalProvider() {
        return new LocalSpringSecurityContextPrincipalProvider();
    }

    @Bean
    public PrincipalIdentityProvider principalIdentityProvider() {
        return new AuthenticationPrincipalIdentityProvider();
    }

    @Bean
    public PrincipalGroupsProvider principalGroupsProvider(GrantedAuthoritiesResolver grantedAuthoritiesResolver,
                                                           GrantedAuthoritiesGroupsMapper grantedAuthoritiesGroupsMapper) {
        return new AuthenticationPrincipalGroupsProvider(grantedAuthoritiesResolver,
                grantedAuthoritiesGroupsMapper);
    }

    @Bean
    public PrincipalRolesProvider principalRolessProvider(GrantedAuthoritiesResolver grantedAuthoritiesResolver,
                                                          GrantedAuthoritiesRolesMapper grantedAuthoritiesRolesMapper) {
        return new AuthenticationPrincipalRolesProvider(grantedAuthoritiesResolver,
                grantedAuthoritiesRolesMapper);
    }

    @Bean
    public SecurityManager securityManager(SecurityContextPrincipalProvider securityContextPrincipalProvider,
                                           PrincipalIdentityProvider principalIdentityProvider,
                                           PrincipalGroupsProvider principalGroupsProvider,
                                           PrincipalRolesProvider principalRolessProvider) {
        return new LocalSpringSecurityManager(securityContextPrincipalProvider,
                principalIdentityProvider,
                principalGroupsProvider,
                principalRolessProvider);
    }

    @Bean
    public UserDetailsService myUserDetailsService() {

        InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager();

        String[][] usersGroupsAndRoles = {
                {"bob", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam"},
                {"john", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam"},
                {"hannah", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam"},
                {"other", "password", "ROLE_ACTIVITI_USER", "GROUP_otherTeam"},
                {"system", "password", "ROLE_ACTIVITI_USER"},
                {"admin", "password", "ROLE_ACTIVITI_ADMIN"},
        };

        for (String[] user : usersGroupsAndRoles) {
            List<String> authoritiesStrings = asList(Arrays.copyOfRange(user, 2, user.length));
            logger.info("> Registering new user: " + user[0] + " with the following Authorities[" + authoritiesStrings + "]");
            inMemoryUserDetailsManager.createUser(new User(user[0], passwordEncoder().encode(user[1]),
                    authoritiesStrings.stream().map(s -> new SimpleGrantedAuthority(s)).collect(Collectors.toList())));
        }

        return inMemoryUserDetailsManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}