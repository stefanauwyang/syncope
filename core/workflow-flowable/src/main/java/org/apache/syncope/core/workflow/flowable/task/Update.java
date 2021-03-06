/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.workflow.flowable.task;

import java.util.Set;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.workflow.flowable.FlowableUserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Update extends AbstractFlowableServiceTask {

    @Autowired
    private UserDataBinder dataBinder;

    @Autowired
    private UserDAO userDAO;

    @Override
    protected void doExecute(final String executionId) {
        User user = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.USER, User.class);
        UserPatch userPatch = engine.getRuntimeService().
                getVariable(executionId, FlowableUserWorkflowAdapter.USER_PATCH, UserPatch.class);

        user = userDAO.save(user);
        UserTO original = dataBinder.getUserTO(user, true);

        PropagationByResource propByRes = dataBinder.update(user, userPatch);
        PasswordPatch password = userPatch.getPassword();
        Set<AttrTO> virAttrs = userPatch.getVirAttrs();

        UserTO updated = dataBinder.getUserTO(user.getKey());
        userPatch = AnyOperations.diff(updated, original, false);
        userPatch.setPassword(password);
        userPatch.getVirAttrs().clear();
        userPatch.getVirAttrs().addAll(virAttrs);

        // report updated user and propagation by resource as result
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.USER, user);
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.USER_TO, updated);
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.USER_PATCH, userPatch);
        engine.getRuntimeService().setVariable(executionId, FlowableUserWorkflowAdapter.PROP_BY_RESOURCE, propByRes);
    }
}
