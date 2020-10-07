/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

import com.mxgraph.view.mxGraph;

/**
 * <p>Interface for processing the graph</p>
 *
 * @author Clemens Toegel
 */
public interface BpmnProcessor {

    mxGraph process(mxGraph graph);

}