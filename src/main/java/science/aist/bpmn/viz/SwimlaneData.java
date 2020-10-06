/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

import com.mxgraph.model.mxCell;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

/**
 * <p>Object which contains all information for swimlanes.</p>
 *
 * @author Clemens Toegel
 */

@Getter
@Setter
@RequiredArgsConstructor
public class SwimlaneData {
    private final String id;
    private HashMap<Integer, mxCell> dataHierarchy = new HashMap<>();
}
