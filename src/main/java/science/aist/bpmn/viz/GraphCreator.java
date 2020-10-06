/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;

import java.util.HashMap;

/**
 * <p>Class which creates a graph and setup the style of it.</p>
 *
 * @author Clemens Toegel
 */

public class GraphCreator {

    // Style constants
    public static final String STYLE_EVENT = "styleEvent";
    public static final String STYLE_GATEWAY = "styleGateway";
    public static final String STYLE_SEQUENCE_FLOW = "styleSequenceFlow";
    public static final String STYLE_BOUNDARY_SEQUENCE_FLOW = "styleBoundarySequenceFlow";
    public static final String STYLE_MESSAGE_FLOW = "styleMessageFlow";
    public static final String STYLE_MESSAGE_FLOW_PARTICIPANT_TO_ELEMENT = "styleMessageFlowParticipantToElement";


    private mxGraph graph;
    private final boolean horizontal;

    public GraphCreator(boolean horizontal) {
        this.horizontal = horizontal;
    }

    public mxGraph createGraph() {
        this.graph = new mxGraph();
        // Here we also setup the graph styles as we now can be sure that graph is not null
        setupGraphStyle();
        return this.graph;
    }

    /**
     * Method to setup graph style. Considers the edge style for the different orientations.
     */
    private void setupGraphStyle() {
        // Styling for sequence flow
        HashMap<String, Object> sequenceFlowEdgeStyle = new HashMap<>();
        sequenceFlowEdgeStyle.put(mxConstants.STYLE_ORTHOGONAL, true);
        sequenceFlowEdgeStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.OrthConnector);
        if (horizontal) {
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 0.0);
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 0.5);
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_EXIT_X, 1.0);
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 0.5);
        } else {
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 0.5);
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 0.0);
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_EXIT_X, 0.5);
            sequenceFlowEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 1.0);
        }
        graph.getStylesheet().putCellStyle(STYLE_SEQUENCE_FLOW, sequenceFlowEdgeStyle);

        // Styling for boundary sequence flow/data associations
        HashMap<String, Object> boundaryEdgeStyle = new HashMap<>();
        if (horizontal) {
            boundaryEdgeStyle.put(mxConstants.STYLE_EXIT_X, 0.5);
            boundaryEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 1.0);
            boundaryEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 0.5);
            boundaryEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 1.0);
        } else {
            boundaryEdgeStyle.put(mxConstants.STYLE_EXIT_X, 1.0);
            boundaryEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 0.5);
            boundaryEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 1.0);
            boundaryEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 0.5);
        }
        boundaryEdgeStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.OrthConnector);
        graph.getStylesheet().putCellStyle(STYLE_BOUNDARY_SEQUENCE_FLOW, boundaryEdgeStyle);

        // Styling for events
        HashMap<String, Object> eventStyle = new HashMap<>();
        eventStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        graph.getStylesheet().putCellStyle(STYLE_EVENT, eventStyle);

        // Styling for gateways
        HashMap<String, Object> gateWayStyle = new HashMap<>();
        gateWayStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RHOMBUS);
        graph.getStylesheet().putCellStyle(STYLE_GATEWAY, gateWayStyle);

        // Styling for message flow if the source is a process element and the target is participant
        // or when source and targets are both participants
        HashMap<String, Object> messageFlowElementsEdgeStyle = new HashMap<>();
        messageFlowElementsEdgeStyle.put(mxConstants.STYLE_ORTHOGONAL, true);
        if (horizontal) {
            messageFlowElementsEdgeStyle.put(mxConstants.STYLE_EXIT_X, 0.5);
            messageFlowElementsEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 1.0);
        } else {
            messageFlowElementsEdgeStyle.put(mxConstants.STYLE_EXIT_X, 1.0);
            messageFlowElementsEdgeStyle.put(mxConstants.STYLE_EXIT_Y, 0.5);
        }
        graph.getStylesheet().putCellStyle(STYLE_MESSAGE_FLOW, messageFlowElementsEdgeStyle);

        // Styling for message flow if the source is a participant and the target is a process element
        HashMap<String, Object> messageFlowParticipantsEdgeStyle = new HashMap<>();
        messageFlowParticipantsEdgeStyle.put(mxConstants.STYLE_ORTHOGONAL, true);
        if (horizontal) {
            messageFlowParticipantsEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 0.5);
            messageFlowParticipantsEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 1.0);
        } else {
            messageFlowParticipantsEdgeStyle.put(mxConstants.STYLE_ENTRY_X, 1.0);
            messageFlowParticipantsEdgeStyle.put(mxConstants.STYLE_ENTRY_Y, 0.5);
        }
        graph.getStylesheet().putCellStyle(STYLE_MESSAGE_FLOW_PARTICIPANT_TO_ELEMENT, messageFlowParticipantsEdgeStyle);
    }

}
