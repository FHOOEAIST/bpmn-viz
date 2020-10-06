/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

/**
 * <p>Visualizations constants which are used by the BPMN layouting process.</p>
 *
 * @author Clemens Toegel
 */
public class VisualizationConstants {

    private VisualizationConstants() {}

    // Graph container names
    public static final String ELEMENT_CONTAINER_PREFIX = "elementContainer";
    public static final String DATA_OBJECTS_LANE_PREFIX = "dataObjectsLane";
    public static final String DATA_STORE_LANE_PREFIX = "dataStoreLane";
    public static final String HIERARCHY_NODE_PREFIX = "hierarchyNode";
    public static final String DATA_OBJECT_REFERENCE_PREFIX = "dataObjectReference";
    public static final String ARTIFACT_PREFIX = "artifact";
    public static final String NESTED_SWIMLANE = "nestedSwimlane";
    public static final String SWIMLANE = "swimlane";
    // BPMN  diagram elements sizes
    public static final float EVENT_SIZE = 30;
    public static final int GATEWAY_SIZE = 40;
    public static final int TASK_WIDTH = 100;
    public static final int TASK_HEIGHT = 80;
    public static final int DATA_OBJECT_WIDTH = 36;
    public static final int DATA_OBJECT_HEIGHT = 50;
    public static final int DATA_STORE_WIDTH = 50;
    public static final int DATA_STORE_HEIGHT = 50;
    public static final int MARGIN = 30;
    public static final int TEXT_ANNOTATION_HEIGHT = 30;
    public static final int TEXT_ANNOTATION_WIDTH = 100;
    public static final int TEXT_ANNOTATION_MARGIN = 80;
    public static final int BLACKBOX_PARTICIPANT_SIZE = 50;
    public static final int LABEL_WIDTH = 90;
    public static final int LABEL_HEIGHT = 30;
    // Specific margin for www.bpmn.io BPMN viewer
    public static final int BPMN_IO_MARGIN = 30;

}
