/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz.impl;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;
import org.omg.spec.bpmn.model.*;
import science.aist.bpmn.viz.BpmnAutoLayout;
import science.aist.bpmn.viz.BpmnProcessor;
import science.aist.bpmn.viz.BpmnProcessorData;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static science.aist.bpmn.viz.GraphCreator.STYLE_EVENT;
import static science.aist.bpmn.viz.GraphCreator.STYLE_GATEWAY;
import static science.aist.bpmn.viz.VisualizationConstants.*;
import static science.aist.bpmn.viz.VisualizationUtils.*;

/**
 * <p>Processes the elements of either a TProcess or TSubProcess of a BPMN model.</p>
 * <p>At the moment it can handle:</p>
 * <ul>
 *     <li>TSequenceFlow elements</li>
 *     <li>TEvent elements</li>
 *     <li>TGateway elements</li>
 *     <li>The following TActivity elements:
 *     <ul>
 *         <li>TTask elements</li>
 *         <li>TCallActivity elements</li>
 *         <li>TSubProcess elements</li>
 *         <li>The following attributes of such elements:
 *         <ul>
 *             <li>TProperty</li>
 *             <li>TDataInput</li>
 *             <li>TDataOutput</li>
 *             <li>TDataInputAssociation</li>
 *             <li>TDataOutputAssociation</li>
 *         </ul></li>
 *     </ul>
 *     </li>
 *     <li>TDataObjectReference elements</li>
 *      <li>TDataStoreReference elements</li>
 * </ul>
 *
 * @author Clemens Toegel
 */

public class ElementBpmnProcessorImpl implements BpmnProcessor {

    private final BpmnProcessorData bpmnProcessorData;
    private mxGraph graph;
    private mxCell graphElementContainer;
    private final List<JAXBElement<? extends TFlowElement>> flowElements;

    public ElementBpmnProcessorImpl(BpmnProcessorData bpmnProcessorData, List<JAXBElement<? extends TFlowElement>> flowElements) {
        this.bpmnProcessorData = bpmnProcessorData;
        this.flowElements = flowElements;
    }

    @Override
    public mxGraph process(mxGraph graph) {
        this.graph = graph;
        setupProcessElements();
        // Create a vertex which only contains the BPMN flow elements and corresponding edges, so no data object or other stuff
        mxCell graphRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());
        graphElementContainer = (mxCell) graph.insertVertex(graphRootNode, generateRandomId(ELEMENT_CONTAINER_PREFIX), "", 0, 0, 0, 0, "");
        bpmnProcessorData.setGraphElementContainerId(graphElementContainer.getId());
        handleElements(flowElements);
        return this.graph;
    }

    /**
     * Method to setup all necessary object maps/lists which belong to a process
     */
    protected void setupProcessElements() {
        bpmnProcessorData.setDataObjectReferences(new HashMap<>());// TDataObjectReference are gathered and processed afterwards,because we must be sure we already found source and target
        bpmnProcessorData.setDataInputAssociations(new ArrayList<>()); // TDataInputAssociations are gathered and processed afterwards,because we must be sure we already found source and target
        bpmnProcessorData.setDataOutputAssociations(new HashMap<>()); // TDataOutputAssociations are gathered and processed afterwards,because we must be sure we already found source and target
        bpmnProcessorData.setProperties(new HashMap<>()); // Properties of TActivity elements
        bpmnProcessorData.setIoSpecification(new HashMap<>());
        bpmnProcessorData.setDataStoreReference(new HashMap<>());
    }

    private void handleElements(List<JAXBElement<? extends TFlowElement>> flowElements) {
        // Process elements
        for (JAXBElement<? extends TFlowElement> flowElement : flowElements) {
            if (flowElement.getValue() instanceof TSequenceFlow) {
                handleSequenceFlow((TSequenceFlow) flowElement.getValue());
            } else if (flowElement.getValue() instanceof TEvent) {
                TEvent tEvent = (TEvent) flowElement.getValue();
                handleEvent(tEvent);
                // If the element has got property we save them in the HashMap with the corresponding vertex
                for (TProperty tProperty : tEvent.getProperty()) {
                    bpmnProcessorData.getProperties().put(tProperty.getId(), tEvent.getId());
                }
                // The specific event TCatchEvent might have TDataOutputAssociations elements therefore we react to them
                if (tEvent instanceof TCatchEvent) {
                    // TDataOutputAssociation should have the key of the parent as sourceRef
                    bpmnProcessorData.getDataOutputAssociations().put(tEvent.getId(), ((TCatchEvent) tEvent).getDataOutputAssociation());
                }
                // Add all TDataOutputAssociations from a TStartEvent, logically it can't have TDataInputAssociations
                if (tEvent instanceof TStartEvent) {
                    bpmnProcessorData.getDataOutputAssociations().put(tEvent.getId(), ((TStartEvent) tEvent).getDataOutputAssociation());
                }
                // Add all TDataInputAssociation from a TEndEvent, logically it can't have TDataOutputAssociations
                if (tEvent instanceof TEndEvent) {
                    bpmnProcessorData.getDataInputAssociations().addAll(((TEndEvent) tEvent).getDataInputAssociation());
                }
            } else if (flowElement.getValue() instanceof TGateway) {
                createGatewayVertex(flowElement.getValue());
            } else if (flowElement.getValue() instanceof TActivity) {
                TActivity tActivity = (TActivity) flowElement.getValue();
                if (tActivity instanceof TTask || tActivity instanceof TCallActivity) {
                    handleActivity(tActivity);
                } else if (flowElement.getValue() instanceof TSubProcess) {
                    handleSubProcess(tActivity);
                }
                // If the element has got property we save them in the HashMap with the corresponding vertex
                for (TProperty tProperty : tActivity.getProperty()) {
                    bpmnProcessorData.getProperties().put(tProperty.getId(), tActivity.getId());
                }
                // If the element has got an ioSpecification we save them in the HashMap with the corresponding vertex
                if (tActivity.getIoSpecification() != null) {
                    for (TDataInput tDataInput : tActivity.getIoSpecification().getDataInput()) {
                        bpmnProcessorData.getIoSpecification().put(tDataInput.getId(), tActivity.getId());
                    }
                    for (TDataOutput tDataOutput : tActivity.getIoSpecification().getDataOutput()) {
                        bpmnProcessorData.getIoSpecification().put(tDataOutput.getId(), tActivity.getId());
                    }
                }
                bpmnProcessorData.getDataInputAssociations().addAll(tActivity.getDataInputAssociation());
                // TDataOutputAssociation should have the key of the parent as sourceRef
                if (!tActivity.getDataOutputAssociation().isEmpty()) {
                    bpmnProcessorData.getDataOutputAssociations().put(tActivity.getId(), tActivity.getDataOutputAssociation());
                }
            } else if (flowElement.getValue() instanceof TDataObjectReference) {
                handleDataObjectReference((TDataObjectReference) flowElement.getValue());
            } else if (flowElement.getValue() instanceof TDataStoreReference) {
                bpmnProcessorData.getDataStoreReference().put(flowElement.getValue().getId(), (TDataStoreReference) flowElement.getValue());
            }
        }
        // Boundary events are gathered and processed afterwards, because we must be sure we have its parent
        handleBoundaryEvents();
    }

    private void handleSequenceFlow(TSequenceFlow sequenceFlow) {
        if (sequenceFlow.getId() == null) sequenceFlow.setId(generateRandomId("sequenceFlow"));
        if (sequenceFlow.getConditionExpression() != null) {
            bpmnProcessorData.getSequenceFlowCondition().add(sequenceFlow.getId());
        }
        bpmnProcessorData.getSequenceFlows().put(sequenceFlow.getId(), sequenceFlow);
    }

    private void handleEvent(TFlowElement flowElement) {
        // Boundary events are an exception to the general way of drawing an event
        if (flowElement instanceof TBoundaryEvent) {
            bpmnProcessorData.getBoundaryEvents().add((TBoundaryEvent) flowElement);
        } else {
            createEventVertex(flowElement);
        }
    }

    private void handleActivity(TActivity activityElement) {
        graph.insertVertex(graphElementContainer, activityElement.getId(), "", 0, 0, TASK_WIDTH, TASK_HEIGHT);
    }

    private void handleDataObjectReference(TDataObjectReference tDataObjectReference) {
        if (tDataObjectReference.getId() == null)
            tDataObjectReference.setId(generateRandomId(DATA_OBJECT_REFERENCE_PREFIX));
        bpmnProcessorData.getDataObjectReferences().put(tDataObjectReference.getId(), tDataObjectReference);
    }

    private void handleSubProcess(TActivity activityElememt) {
        BpmnProcessorData subProcessData = new BpmnProcessorData(bpmnProcessorData.getTDefinitionsJAXBElement(), bpmnProcessorData.isHorizontal());
        // New instance needs the diagramElements so that we can add the DI information
        subProcessData.setDiagramElements(bpmnProcessorData.getDiagramElements());
        subProcessData.setBpmnPlaneId(bpmnProcessorData.getBpmnPlaneId());
        // Create new instance of a BpmnAutoLayout for a TSubProcess, as each process element is layouted individually
        BpmnAutoLayout bpmnAutoLayoutSubProcess = new BpmnAutoLayout(subProcessData);
        bpmnAutoLayoutSubProcess.layoutHandler(activityElememt);
        bpmnAutoLayoutSubProcess.handleSequenceFlow();
        // Saves the sub processes of a process, so that we can later manipulate their origin in respect to the final graph
        bpmnProcessorData.getBpmnSubProcesses().put(activityElememt.getId(), bpmnAutoLayoutSubProcess);

        // Create a boundary box for the sub process in the parent graph
        double subProcessWidth = bpmnAutoLayoutSubProcess.getGraph().getView().getGraphBounds().getWidth();
        double subProcessHeight = bpmnAutoLayoutSubProcess.getGraph().getView().getGraphBounds().getHeight();
        graph.insertVertex(graphElementContainer, activityElememt.getId(), "", 0, 0, (subProcessWidth + MARGIN), (subProcessHeight + MARGIN));
    }

    /**
     * Method to create graph cell of event vertex
     *
     * @param flowElement TEvent element
     */
    private void createEventVertex(TFlowElement flowElement) {
        // Add vertex representing event to graph
        graph.insertVertex(graphElementContainer, flowElement.getId(), "", 0, 0, EVENT_SIZE, EVENT_SIZE, STYLE_EVENT);
    }

    /**
     * Method to create graph cell of gateway vertex
     *
     * @param flowElement TGateway element
     */
    private void createGatewayVertex(TFlowElement flowElement) {
        // Create gateway node
        graph.insertVertex(graphElementContainer, flowElement.getId(), "", 0, 0, GATEWAY_SIZE, GATEWAY_SIZE, STYLE_GATEWAY);
    }

    private void handleBoundaryEvents() {
        for (TBoundaryEvent boundaryEvent : bpmnProcessorData.getBoundaryEvents()) {
            mxGeometry geometry = new mxGeometry(0.8, 1.0, EVENT_SIZE, EVENT_SIZE);
            geometry.setOffset(new mxPoint(-(EVENT_SIZE / 2), -(EVENT_SIZE / 2)));
            geometry.setRelative(true);
            mxCell boundaryPort = new mxCell(null, geometry, "shape=ellipse;perimeter=ellipsePerimeter");
            boundaryPort.setId("boundary-event-" + boundaryEvent.getId());
            boundaryPort.setVertex(true);
            Object portParent;
            if (boundaryEvent.getAttachedToRef() != null) {
                portParent = ((mxGraphModel) graph.getModel()).getCell(boundaryEvent.getAttachedToRef().getLocalPart());
            } else {
                boundaryEvent.getAttachedToRef();
                throw new RuntimeException("Could not generate DI: boundaryEvent '" + boundaryEvent.getId() + "' has no attachedToRef");
            }
            graph.addCell(boundaryPort, portParent);
        }
    }
}
