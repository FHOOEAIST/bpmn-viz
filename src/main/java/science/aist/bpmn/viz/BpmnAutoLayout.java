/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright 2018 Flowable
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

package science.aist.bpmn.viz;

import com.mxgraph.layout.hierarchical.model.mxGraphHierarchyNode;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import lombok.Getter;
import org.omg.spec.bpmn.model.*;
import science.aist.bpmn.model.BPMNHelper;
import science.aist.bpmn.viz.impl.*;

import javax.swing.*;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static science.aist.bpmn.viz.GraphCreator.*;
import static science.aist.bpmn.viz.VisualizationConstants.*;
import static science.aist.bpmn.viz.VisualizationUtils.OBJECT_FACTORY;

/**
 * <p>Auto layouts a BPMN model, with the help of different BPMN processors.</p>
 * <p>Modified from <a href="https://github.com/flowable/flowable-engine">Flowable Engine</a></p>
 * <p>Note that in the Javadoc a lot of different process are mentioned. The following list will clarify the different meanings.</p>
 * <ul>
 *     <li>Process element: Can be any element which describes a process in BPMN. So, in the most cases it can be a process or sub process</li>
 *     <li>TProcess: The BPMN element of a process</li>
 *     <li>TSubProcess: The BPMN element of a sub process</li>
 * </ul>
 *
 * @author Joram Barrez
 * @author Clemens Toegel
 */
public class BpmnAutoLayout {

    // Data which belongs to a BPMN model
    private final BpmnProcessorData bpmnProcessorData;

    @Getter
    private mxGraph graph;

    public BpmnAutoLayout(BpmnProcessorData bpmnProcessorData) {
        this.bpmnProcessorData = bpmnProcessorData;
    }

    public BpmnAutoLayout(JAXBElement<TDefinitions> tDefinitionsJAXBElement, boolean horizontal) {
            this(new BpmnProcessorData(tDefinitionsJAXBElement, horizontal));
    }

    /**
     * Method to setup the visualization of a given BPMN file
     */
    public void execute() {
        // In the first step we handle the root elements. We know that we dont manipulate the graph, therefore we dont need to react to the return value
        new RootElementBpmnProcessorImpl(bpmnProcessorData).process(graph);
        if (bpmnProcessorData.getTCollaboration() != null) {
            setupLayoutingElements();
        }
        for (TProcess tProcess : bpmnProcessorData.getTProcesses()) {
            // Layout the TProcess
            layoutHandler(tProcess);
        }
        // In the end, we handle all BPMN models just like collaborations, so that the layout looks good
        layoutTCollaboration();
        // Handle the connections as last step, otherwise JGraphX would make the connections look weird
        handleSequenceFlow();
        // After layouting the BPMN model we add the message flow edges if available
        if (bpmnProcessorData.getMessageFlow() != null && !bpmnProcessorData.getMessageFlow().isEmpty()) {
            handleMessageFlow();
        }
        // Can only be done after all participants or processes are modelled
        generateDiagramInterchangeElements(false);
    }

    /**
     * Method to layout a TCollaboration
     */
    private void layoutTCollaboration() {
        // Own layout logic, basically layouts three participants in one row (horizontal orientation) or column (vertical orientation),
        // after 3 participants it adds one row (vertical orientation) or column (horizontal orientation)
        Object[] participants = graph.getChildCells(graph.getDefaultParent());
        double xPosition = 0;
        double yPosition = 0;
        double maxHeight = 0;
        double maxWidth = 0;
        double lowestYPosition = 0;
        double mostRightXPosition = 0;
        for (int i = 0; i < participants.length; i++) {
            mxCell participantElements = (mxCell) participants[i];
            participantElements.getGeometry().setX(xPosition);
            participantElements.getGeometry().setY(yPosition);
            double participantWidth = participantElements.getGeometry().getWidth();
            double participantHeight = participantElements.getGeometry().getHeight();
            if (maxWidth < participantWidth) {
                maxWidth = participantWidth;
            }
            if (maxHeight < participantHeight) {
                maxHeight = participantHeight;
            }
            // After dealing with three participants we make a new row (vertical orientation) or column (horizontal orientation)
            if ((i + 1) % 3 == 0) {
                if (bpmnProcessorData.isHorizontal()) {
                    xPosition += maxWidth + (2 * MARGIN);
                    yPosition = 0;
                } else {
                    yPosition += maxHeight + (2 * MARGIN);
                    xPosition = 0;
                }
            } else {
                if (bpmnProcessorData.isHorizontal()) {
                    yPosition += participantElements.getGeometry().getHeight() + (2 * MARGIN);
                } else {
                    xPosition += participantElements.getGeometry().getWidth() + (2 * MARGIN);
                }
            }
            if (mostRightXPosition < xPosition) {
                mostRightXPosition = xPosition;
            } else if (lowestYPosition < yPosition) {
                lowestYPosition = yPosition;
            }
        }
        // Checks if black box participants exists
        if (bpmnProcessorData.getBlackBoxParticipants() != null) {
            // Based on the orientation we would place them below or on the right side of all diagrams
            for (TParticipant tParticipant : bpmnProcessorData.getBlackBoxParticipants()) {
                if (bpmnProcessorData.isHorizontal()) {
                    graph.insertVertex(graph.getDefaultParent(), tParticipant.getId(), "", 0, lowestYPosition, xPosition + maxWidth, BLACKBOX_PARTICIPANT_SIZE);
                } else {
                    graph.insertVertex(graph.getDefaultParent(), tParticipant.getId(), "", mostRightXPosition, 0, BLACKBOX_PARTICIPANT_SIZE, yPosition + maxHeight);
                }
            }
        }
        // After updating the model we need to refresh the graph before generating the DI information
        graph.refresh();
    }


    /**
     * Method to create all necessary element maps/lists to auto layout a BPMN file
     */
    private void setupLayoutingElements() {
        bpmnProcessorData.setSequenceFlows(new HashMap<>());
        bpmnProcessorData.setBoundaryEvents(new ArrayList<>());
        bpmnProcessorData.setTextAnnotations(new HashMap<>());
        bpmnProcessorData.setAssociations(new ArrayList<>());
        bpmnProcessorData.setSequenceFlowCondition(new ArrayList<>());
        graph = new GraphCreator(bpmnProcessorData.isHorizontal()).createGraph();
    }

    /**
     * Method to handle different flow element containers
     *
     * @param flowElementsContainer the container of flow elements -> can be either TProcess or TSubProcess at the moment
     */
    public void layoutHandler(TBaseElement flowElementsContainer) {
        // Create lists for the elements of a process element
        List<JAXBElement<? extends TFlowElement>> flowElements = null;
        List<JAXBElement<? extends TArtifact>> artifacts = null;
        List<TLaneSet> laneSet = null;
        // Checks if the TBaseElement is a process element, if not we skip the whole layouting part
        if (flowElementsContainer instanceof TProcess) {
            flowElements = ((TProcess) flowElementsContainer).getFlowElement();
            artifacts = ((TProcess) flowElementsContainer).getArtifact();
            laneSet = ((TProcess) flowElementsContainer).getLaneSet();
        } else if (flowElementsContainer instanceof TSubProcess) {
            flowElements = ((TSubProcess) flowElementsContainer).getFlowElement();
            artifacts = ((TSubProcess) flowElementsContainer).getArtifact();
            laneSet = ((TSubProcess) flowElementsContainer).getLaneSet();
        }
        if (flowElements != null) {
            // Check if graph is null, if the graph is not null we don't have a TCollaboration or handling a TSubProcess
            if (graph == null) {
                // Sub processes are handled in a new instance of BpmnAutoLayout, hence the new setup of layouting elements
                setupLayoutingElements();
            }
            // Get the corresponding TParticipant element if exists
            if (bpmnProcessorData.getParticipantProcessMap() != null) {
                TParticipant currentTParticipant = bpmnProcessorData.getParticipantProcessMap().get(flowElementsContainer.getId());
                if (currentTParticipant != null) {
                    // If we have the cell parent corresponding to a process we set it as root node
                    graph.insertVertex(graph.getDefaultParent(), currentTParticipant.getId(), "", 0, 0, 0, 0);
                    bpmnProcessorData.setGraphRootNodeId(currentTParticipant.getId());
                    bpmnProcessorData.getParticipantProcessMap().remove(flowElementsContainer.getId());
                }
            }
            // If a process does not belong to a participant we create a root node
            if (bpmnProcessorData.getGraphRootNodeId() == null) {
                graph.insertVertex(graph.getDefaultParent(), flowElementsContainer.getId(), "", 0, 0, 0, 0);
                bpmnProcessorData.setGraphRootNodeId(flowElementsContainer.getId());
            }
            layout(flowElements, artifacts, laneSet);
        }
    }

    /**
     * Method to layout a process element
     *
     * @param flowElements all elements of a process element
     * @param tLaneSets    all lane sets of a process element
     */
    private void layout(List<JAXBElement<? extends TFlowElement>> flowElements, List<JAXBElement<? extends TArtifact>> artifacts, List<TLaneSet> tLaneSets) {
        // First we will handle basic BPMN elements
        graph = new ElementBpmnProcessorImpl(bpmnProcessorData, flowElements).process(graph);

        // Sequence flow are gathered and processed afterwards,because we must be sure we already found source and target
        // Create the edges for the BPMN process, so that the hierarchical layouter works
        handleSequenceFlow();

        mxCell graphElementContainer = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphElementContainerId());
        mxCell graphRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());

        // All elements are now put in the graph. Let's layout them!
        // Now depending on the wished orientation we create a auto layouter
        int direction;
        if (bpmnProcessorData.isHorizontal()) {
            direction = SwingConstants.WEST;
        } else {
            direction = SwingConstants.VERTICAL;
        }
        CustomHierarchicalLayout hierarchicalLayouter = new CustomHierarchicalLayout(graph, direction);
        hierarchicalLayouter.setIntraCellSpacing(100.0);
        hierarchicalLayouter.setResizeParent(true);
        hierarchicalLayouter.setFineTuning(true);
        hierarchicalLayouter.setMoveParent(true);
        hierarchicalLayouter.setDisableEdgeStyle(false);
        hierarchicalLayouter.setUseBoundingBox(true);
        hierarchicalLayouter.execute(graphElementContainer);
        // Just to be sure to work with the origin (0,0)
        graphElementContainer.getGeometry().setX(0);
        graphElementContainer.getGeometry().setY(0);

        // We save the hierarchy value of the nodes, which we need later on for the data flow
        // The lowest rank is the end of the diagram, which means the far most right node has the value 0
        // We invert the order that we will start from left to right with the value 0,
        // so that it is easier later on with the data flow
        HashMap<String, Integer> nodeHierarchyRank = new HashMap<>();
        int hierarchySize = hierarchicalLayouter.getModel().getVertexMapper().values().size();
        for (mxGraphHierarchyNode hierarchyNode : hierarchicalLayouter.getModel().getVertexMapper().values()) {
            int invertedRank = hierarchySize - (hierarchyNode.getRankValue() + 1);
            nodeHierarchyRank.put(((mxCell) hierarchyNode.cell).getId(), invertedRank);
        }
        bpmnProcessorData.setNodeHierarchyRank(nodeHierarchyRank);
        // We can be sure to erase all created edges, as they will not be considered
        // while we move the other diagram elements in the next steps. They would only result in extra empty spaces in the diagram later
        ArrayList<Object> edges = new ArrayList<>();
        for (String key : bpmnProcessorData.getSequenceFlows().keySet()) {
            edges.add(((mxGraphModel) graph.getModel()).getCell(key));
        }
        graph.removeCells(edges.toArray());
        // Process swimlanes
        // Must be after the layouting as we now just move cells and create a swimlane as parent for them
        if (!tLaneSets.isEmpty()) {
            for (TLaneSet tLaneSet : tLaneSets) {
                graph = new SwimlaneBpmnProcessorImpl(bpmnProcessorData, tLaneSet).process(graph);
            }
        }
        if (!artifacts.isEmpty()) {
            graph = new ArtifactBpmnProcessorImpl(bpmnProcessorData, artifacts, !tLaneSets.isEmpty()).process(graph);
        }
        // Here we need to update the graph
        if (!tLaneSets.isEmpty() || !artifacts.isEmpty()) {
            graph.updateGroupBounds(new Object[]{graphElementContainer, graphRootNode});
        }
        if (tLaneSets.isEmpty()) {
            // If we didnt handled swimlanes we get a margin on the whole graph
            graph.updateGroupBounds(graph.getChildCells(graphRootNode, true, true), BPMN_IO_MARGIN);
            graph.updateGroupBounds(new Object[]{graphRootNode}, BPMN_IO_MARGIN);
        }
        graph.refresh();
        // Data associations need to be handled differently and as last elements, as the whole graph must be already drawn
        if (!bpmnProcessorData.getDataObjectReferences().isEmpty() || !bpmnProcessorData.getDataStoreReference().isEmpty()) {
            // If the orientation is vertical we have to add some margin
            if (!bpmnProcessorData.isHorizontal()) {
                graphElementContainer.getGeometry().setWidth(graphElementContainer.getGeometry().getWidth() + MARGIN);
            }
            graph.refresh();
            graph = new DataBpmnProcessorImpl(bpmnProcessorData).process(graph);
        }
        if (bpmnProcessorData.getTCollaboration() != null) {
            // Specific change for www.bpmn.io BPMN viewer so that the labels are not overlapping
            Object[] graphRootChilds = graph.getChildCells(graphRootNode, true, true);
            graph.moveCells(graphRootChilds, BPMN_IO_MARGIN, 0, false, graphRootNode, new java.awt.Point(BPMN_IO_MARGIN, 0));
        }
    }

    /**
     * Method to handle the sequence flow
     */
    public void handleSequenceFlow() {
        mxCell graphElementContainer = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphElementContainerId());
        for (TSequenceFlow sequenceFlow : bpmnProcessorData.getSequenceFlows().values()) {
            Object sourceVertex = ((mxGraphModel) graph.getModel()).getCell(((TBaseElement) sequenceFlow.getSourceRef()).getId());
            Object targetVertex = ((mxGraphModel) graph.getModel()).getCell(((TBaseElement) sequenceFlow.getTargetRef()).getId());

            String style;

            if (sequenceFlow.getSourceRef() instanceof TBoundaryEvent) {
                // Sequence flow out of boundary events are handled in a different way,
                // to make them visually appealing for the eye of the dear end user.
                style = STYLE_BOUNDARY_SEQUENCE_FLOW;
            } else {
                style = STYLE_SEQUENCE_FLOW;
            }
            graph.insertEdge(graphElementContainer, sequenceFlow.getId(), "", sourceVertex, targetVertex, style);
        }
    }

    /**
     * Method to handle the message flow
     */
    private void handleMessageFlow() {
        mxCell graphElementRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());
        for (TMessageFlow tMessageFlow : bpmnProcessorData.getMessageFlow()) {

            Object sourceVertex = ((mxGraphModel) graph.getModel()).getCell(tMessageFlow.getSourceRef().getLocalPart());
            Object targetVertex = ((mxGraphModel) graph.getModel()).getCell(tMessageFlow.getTargetRef().getLocalPart());
            String style;
            // We check if the source vertex is a participant or not and if the target is no participant
            if (((mxCell) sourceVertex).getParent().getId().equals(((mxCell) graph.getDefaultParent()).getId()) &&
                    !((mxCell) targetVertex).getParent().getId().equals(((mxCell) graph.getDefaultParent()).getId())) {
                // If it would be the case then we have to use a different edge style
                style = STYLE_MESSAGE_FLOW_PARTICIPANT_TO_ELEMENT;
            } else {
                // Otherwise we take the normal one
                style = STYLE_MESSAGE_FLOW;
            }
            graph.insertEdge(graphElementRootNode, tMessageFlow.getId(), "", sourceVertex, targetVertex, style);
        }
    }

    /**
     * Method to create DI information of a process
     */
    private void generateDiagramInterchangeElements(boolean isSubProcess) {
        // We recursively generate the DI information, so that our sub processes are positioned correctly in respect to the ancestors
        // For each process element we get its sub processes
        while (bpmnProcessorData.getBpmnSubProcesses().entrySet().iterator().hasNext()) {
            // Get the basic sub process information
            Map.Entry<String, BpmnAutoLayout> bpmnSubProcessEntry = bpmnProcessorData.getBpmnSubProcesses().entrySet().iterator().next();
            mxGraph subProcessGraph = bpmnSubProcessEntry.getValue().graph;

            // We need to find out where the sub process is located in its parent graph and get its position information
            Object subProcessCell = ((mxGraphModel) graph.getModel()).getCell(bpmnSubProcessEntry.getKey());
            mxGeometry subProcessGraphGeometry = graph.getCellGeometry(subProcessCell);
            mxPoint absolutePoint = graph.getView().getState(subProcessCell).getOrigin();

            // Then we set the origin of the sub process graph to the coordinates of the previously found coordinates
            // This will make sure that the origin of the sub process is positioned correctly in respect to the ancestors
            subProcessGraphGeometry.setX(absolutePoint.getX() + MARGIN);
            subProcessGraphGeometry.setY(absolutePoint.getY() + MARGIN);
            subProcessGraph.getModel().setGeometry(subProcessGraph.getModel().getRoot(), subProcessGraphGeometry);

            // After having set the right origin for the sub process we go one step deeper
            // So, we check if the sub process has got sub processes
            bpmnSubProcessEntry.getValue().generateDiagramInterchangeElements(true);
            // After handling all sub processes of a sub process we remove the sub process from the parent process
            bpmnProcessorData.getBpmnSubProcesses().remove(bpmnSubProcessEntry.getKey());
        }
        // After handling all sub processes of a process element we create the DI information,
        // for all elements of the current process element
        // If it is a sub process we have to take a different root node, otherwise we would create some elements multiple times
        if (isSubProcess) {
            mxCell graphElementRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());
            generate(graphElementRootNode);
        } else {
            generate((mxCell) graph.getDefaultParent());
        }
    }

    /**
     * Method to generate DI information recursively
     *
     * @param parentCell the current parent cell
     */
    private void generate(mxCell parentCell) {
        // Get all child cells of the parent cell
        for (Object child : graph.getChildCells(parentCell)) {
            // Each child will be recursively handled
            generate(((mxCell) child));
            // Here we check if the graph element is artifically created by us (these containers do help us during the layout,
            // but should not be generated as DI information)
            if (!((mxCell) child).getId().startsWith(DATA_OBJECTS_LANE_PREFIX) && !((mxCell) child).getId().startsWith(HIERARCHY_NODE_PREFIX)
                    && !((mxCell) child).getId().startsWith(ELEMENT_CONTAINER_PREFIX) &&
                    !((mxCell) child).getId().startsWith(DATA_STORE_LANE_PREFIX)) {
                // Check if the child is a vertex or an edge
                if (((mxCell) child).isVertex()) {
                    // Check if the cell is not the BPMN Plane
                    if (!bpmnProcessorData.getBpmnPlaneId().equals(((mxCell) child).getId())) {
                        mxCellState cellState = graph.getView().getState(child);
                        createDiagramInterchangeInformation(((mxCell) child).getId(), (int) cellState.getX(), (int) cellState.getY(), (int) cellState.getWidth(),
                                (int) cellState.getHeight());
                    }
                } else {
                    List<mxPoint> points = graph.getView().getState(child).getAbsolutePoints();
                    createDiagramInterchangeInformation(((mxCell) child).getId(), optimizeEdgePoints(points));
                }
            }
        }
    }

    /**
     * Method to remove points which were created by JGraphX that are visually not really necessary
     *
     * @param unoptimizedPointsList list of points from an edge which need optimization
     * @return optimized points
     */
    private static List<mxPoint> optimizeEdgePoints(List<mxPoint> unoptimizedPointsList) {
        List<mxPoint> optimizedPointsList = new ArrayList<>();
        for (int i = 0; i < unoptimizedPointsList.size(); i++) {
            boolean keepPoint = true;
            mxPoint currentPoint = unoptimizedPointsList.get(i);
            // When three points are on the same x-axis with same y value, the middle point can be removed
            if (i > 0 && i != unoptimizedPointsList.size() - 1) {
                mxPoint previousPoint = unoptimizedPointsList.get(i - 1);
                mxPoint nextPoint = unoptimizedPointsList.get(i + 1);

                if (currentPoint.getX() >= previousPoint.getX() && currentPoint.getX() <= nextPoint.getX() && currentPoint.getY() == previousPoint.getY() && currentPoint.getY() == nextPoint.getY() ||
                        currentPoint.getY() >= previousPoint.getY() && currentPoint.getY() <= nextPoint.getY() && currentPoint.getX() == previousPoint.getX() && currentPoint.getX() == nextPoint.getX()) {
                    keepPoint = false;
                }
            }
            if (keepPoint) {
                optimizedPointsList.add(currentPoint);
            }
        }
        return optimizedPointsList;
    }

    /**
     * Method to create the BPMN DI information from elements which need a shape.
     *
     * @param id     of the BPMN element
     * @param x      position of the shape
     * @param y      position of the shape
     * @param width  of the shape
     * @param height of the shape
     */
    private void createDiagramInterchangeInformation(String id, int x, int y, int width, int height) {
        // First we create a BPMN shape for the diagram and set the corresponding BPMN element
        BPMNShape bpmnShapeValue = OBJECT_FACTORY.createBPMNShape();
        bpmnShapeValue.setBpmnElement(BPMNHelper.createQName(id));
        // Create the bounds of the BPMN shape
        Bounds bounds = OBJECT_FACTORY.createBounds();
        bounds.setX(x);
        bounds.setY(y);
        bounds.setHeight(height);
        bounds.setWidth(width);
        bpmnShapeValue.setBounds(bounds);
        bpmnShapeValue.setIsExpanded(true);
        bpmnShapeValue.setIsHorizontal(bpmnProcessorData.isHorizontal());
        // Add the DI information of the current shape element to the diagramElements list
        bpmnProcessorData.getDiagramElements().add(OBJECT_FACTORY.createBPMNShape(bpmnShapeValue));
    }

    /**
     * Method to create edges from which we do not have the element but only the ID
     *
     * @param id        of the BPMN element
     * @param waypoints of the edge
     */
    private void createDiagramInterchangeInformation(String id, List<mxPoint> waypoints) {
        // First we create a BPMN Edge for the diagram and set the corresponding BPMN element
        BPMNEdge bpmnEdgeValue = OBJECT_FACTORY.createBPMNEdge();
        bpmnEdgeValue.setBpmnElement(BPMNHelper.createQName(id));
        // Create the arrows of the BPMN edge
        double centerXValue = 0;
        double labelYValue = 0;
        for (mxPoint waypoint : waypoints) {
            Point point = OBJECT_FACTORY.createPoint();
            point.setX(waypoint.getX());
            centerXValue += waypoint.getX();
            point.setY(waypoint.getY());
            labelYValue += waypoint.getY();
            bpmnEdgeValue.getWaypoint().add(point);
        }
        if (bpmnProcessorData.getSequenceFlowCondition().contains(id)) {
            BPMNLabel bpmnLabel = OBJECT_FACTORY.createBPMNLabel();
            Bounds bounds = OBJECT_FACTORY.createBounds();
            bounds.setX(centerXValue / waypoints.size() - LABEL_WIDTH);
            if (bpmnProcessorData.isHorizontal()) {
                bounds.setY(labelYValue / waypoints.size());
            } else {
                bounds.setY((labelYValue / waypoints.size()) - LABEL_HEIGHT);
            }
            bounds.setWidth(LABEL_WIDTH);
            bounds.setHeight(LABEL_HEIGHT);
            bpmnLabel.setBounds(bounds);
            bpmnEdgeValue.setBPMNLabel(bpmnLabel);
        }
        // Add the DI information of the current edge element to the diagramElements list
        bpmnProcessorData.getDiagramElements().add(OBJECT_FACTORY.createBPMNEdge(bpmnEdgeValue));
    }


    /**
     * Due to a bug (see
     * http://forum.jgraph.com/questions/5952/mxhierarchicallayout-not-correct-when-using-child-vertex)
     * We must extend the default hierarchical layout to tweak it a bit (see url
     * link) otherwise the layouting crashes.
     * <p>
     * Verify again with a later release if fixed (ie the mxHierarchicalLayout can be used directly)
     * <p>
     * The bug is that the normal mxHierarchicalLayout would traverse ancestors which is not necessary, otherwise the whole layout would get messed up
     * (However, with our current examples it does not mess up anything)
     */
    public static class CustomHierarchicalLayout extends mxHierarchicalLayout {

        public CustomHierarchicalLayout(mxGraph graph, int orientation) {
            super(graph, orientation);
            this.traverseAncestors = false;
        }
    }
}