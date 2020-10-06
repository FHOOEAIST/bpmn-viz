/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz.impl;

import com.mxgraph.layout.mxStackLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import org.omg.spec.bpmn.model.*;
import science.aist.bpmn.viz.BpmnProcessor;
import science.aist.bpmn.viz.BpmnProcessorData;
import science.aist.bpmn.viz.SwimlaneData;

import javax.xml.bind.JAXBElement;
import java.util.*;

import static science.aist.bpmn.viz.VisualizationConstants.*;
import static science.aist.bpmn.viz.VisualizationUtils.*;

/**
 * <p>Processes TDataObject of a BPMN model.</p>
 * <p>However, it does not directly handle TDataObject elements, but rather:</p>
 * <ul>
 *     <li>TDataObjectReference: Serves in our case as TDataObject as its target always must be a TDataObject.</li>
 *     <li>TDataStoreReference: Serves in our case as TDataStore as its target always must be a TDataStore.</li>
 *     <li>TDataOutputAssociation elements</li>
 *     <li>TDataInputAssociation elements</li>
 * </ul>
 *
 * @author Clemens Toegel
 */

public class DataBpmnProcessorImpl implements BpmnProcessor {

    private mxGraph graph;
    private final BpmnProcessorData bpmnProcessorData;

    public DataBpmnProcessorImpl(BpmnProcessorData bpmnProcessorData) {
        this.bpmnProcessorData = bpmnProcessorData;
    }

    @Override
    public mxGraph process(mxGraph graph) {
        this.graph = graph;
        // Elements must be created beofre handling associations
        createDataElements();
        if (!bpmnProcessorData.getDataObjectReferences().isEmpty()) {
            handleDataObjectReferencesAssociations();
        }
        if (!bpmnProcessorData.getDataStoreReference().isEmpty()) {
            // Must be handled after TDataObjectReference elements as it will be placed below
            handleDataStoreReferencesAssociations();
        }
        // Must be done at last as we need TDataObjectReferences and TDataStore already created
        handleDataInputAssociations();
        return this.graph;
    }

    /**
     * Method to create Data elements: TDataObjectReferences, TDataStores
     */
    private void createDataElements() {
        mxCell graphRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());
        if (!bpmnProcessorData.getDataObjectReferences().isEmpty()) {
            for (TDataObjectReference tDataObjectReference : bpmnProcessorData.getDataObjectReferences().values()) {
                graph.insertVertex(graphRootNode, tDataObjectReference.getId(), "", 0, 0, DATA_OBJECT_WIDTH, DATA_OBJECT_HEIGHT);
            }
        }
        if (!bpmnProcessorData.getDataStoreReference().isEmpty()) {
            for (TDataStoreReference tDataStoreReference : bpmnProcessorData.getDataStoreReference().values()) {
                graph.insertVertex(graphRootNode, tDataStoreReference.getId(), "", 0, 0, DATA_STORE_WIDTH, DATA_STORE_HEIGHT);
            }
        }
    }

    /**
     * Method to handle and layout TDataObjectReference elements and also some TDataOutputAssociations, as we create them along with the TDataObjectReferences elements.
     * TDataObjectReferences will be placed below or next to the current diagram. For each graph layers (hierarchy is from left to right)
     * lanes will be placed below, so that corresponding TDataObjectReferences will be placed in there.
     * However, TDataObjectReferences which have no "origin" will be placed above the current diagram.
     * TDataObjectReferences with no "origin" are in this case TDataObjectReferences which are not mentioned in any TDataOutputAssociation,
     * so they are elements which have no direct origin.
     * During the steps we also check if a TDataObjectReference element belong to a vertex in a swimlane. If so, we place the TDataObjectReference
     * in the corresponding swimlane. However, the swimlane association does not work for TDataObjectReferences with no "origin".
     */
    private void handleDataObjectReferencesAssociations() {
        mxCell graphElementContainer = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphElementContainerId());
        double lowestPointInGraph = graphElementContainer.getGeometry().getHeight();
        double mostRightPointInGraph = graphElementContainer.getGeometry().getWidth();
        double dataFlowLanesStartPoint = bpmnProcessorData.isHorizontal() ? lowestPointInGraph : mostRightPointInGraph;
        mxCell graphRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());
        // Needed to later on position the hierarchy nodes on the x or y axis according to their corresponding nodes in the graph
        HashMap<String, Double> hierarchyNodesCenterValue = new HashMap<>();
        ArrayList<mxCell> swimlanes = new ArrayList<>();
        // Root cell of the hierarchy logic, is just a place holder
        // Set the position depending on whether it should be horizontal or not
        double rootXPosition = bpmnProcessorData.isHorizontal() ? 0 : dataFlowLanesStartPoint;
        double rootYPosition = bpmnProcessorData.isHorizontal() ? dataFlowLanesStartPoint : 0;
        mxCell dataObjectReferenceRootBottom = (mxCell) graph.insertVertex(graphRootNode, generateRandomId(DATA_OBJECTS_LANE_PREFIX), "", rootXPosition, rootYPosition, 0, 0);
        for (int i = 0; i < bpmnProcessorData.getNodeHierarchyRank().values().size(); i++) {
            graph.insertVertex(dataObjectReferenceRootBottom, generateRandomId(HIERARCHY_NODE_PREFIX), "", 0, 0, 0, 0);
        }
        // First, we will go through all TDataOutputAssociations and create the vertex for TDataObjectReference which have an origin
        for (Map.Entry<String, List<TDataOutputAssociation>> tDataOutputAssociations : bpmnProcessorData.getDataOutputAssociations().entrySet()) {
            mxCell sourceVertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(tDataOutputAssociations.getKey());
            // One sourceVertex can have multiple TDataOutputAssociations
            for (TDataOutputAssociation tDataOutputAssociation : tDataOutputAssociations.getValue()) {
                // We get the corresponding TDataObjectReference from the TDataOutputAssociation and remove it from our list
                String targetId = ((TBaseElement) tDataOutputAssociation.getTargetRef()).getId();
                // If the target is not a TDataStoreReference we know it must be a TDataObjectReference at the moment
                if (!bpmnProcessorData.getDataStoreReference().containsKey(targetId)) {
                    TDataObjectReference tDataObjectReference = bpmnProcessorData.getDataObjectReferences().get(targetId);
                    bpmnProcessorData.getDataObjectReferences().remove(tDataObjectReference.getId());
                    // We get the current location of the source vertex
                    mxCellState cellState = graph.getView().getState(sourceVertex);
                    // We get the hierarchy node in which we insert the TDataObjectReference
                    int hierarchyKey = bpmnProcessorData.getNodeHierarchyRank().get(tDataOutputAssociations.getKey());
                    mxICell hierarchyNode = dataObjectReferenceRootBottom.getChildAt(hierarchyKey);
                    // We will save the center coordinate of the source depending on the orientation
                    double hierarchyNodeCenterPosition;
                    if (bpmnProcessorData.isHorizontal()) {
                        hierarchyNodeCenterPosition = cellState.getX() + ((cellState.getWidth() / 2) - (DATA_OBJECT_WIDTH / 2));
                    } else {
                        hierarchyNodeCenterPosition = cellState.getY() + ((cellState.getHeight() / 2) - (DATA_OBJECT_HEIGHT / 2));
                    }
                    // We will save the center value to the corresponding hierarchy node
                    hierarchyNodesCenterValue.put(hierarchyNode.getId(), hierarchyNodeCenterPosition);
                    // We get the TDataStoreReference and associate it to the correct hierachy lane
                    Object dataObjectReferenceVertex = ((mxGraphModel) graph.getModel()).getCell(tDataObjectReference.getId());
                    mxCell parentCell = dataObjectReferenceRootBottom;
                    if (sourceVertex.getParent().getValue() instanceof SwimlaneData) {
                        // If the source vertex is a swimlane (marked with a SwimlaneData object as value)
                        // We add the TDataObjectReference vertex to the swimlane hierarchy ndoes
                        mxCell swimlane = (mxCell) sourceVertex.getParent();
                        SwimlaneData swimlaneData = (SwimlaneData) swimlane.getValue();
                        swimlaneData.getDataHierarchy().computeIfAbsent(hierarchyKey, k -> (mxCell) hierarchyNode);
                        graph.addCell(dataObjectReferenceVertex, swimlaneData.getDataHierarchy().get(hierarchyKey));
                        if (!swimlanes.contains(swimlane)) {
                            // Also add the swimlanes to the array so we can later on react to that
                            swimlanes.add(swimlane);
                        }
                        // Change the parent cell as it is now the swimlane
                        parentCell = swimlane;
                    } else {
                        graph.addCell(dataObjectReferenceVertex, hierarchyNode);
                    }
                    // We will also create the edges immediately
                    graph.insertEdge(parentCell, tDataOutputAssociation.getId(), "", sourceVertex, dataObjectReferenceVertex);
                }
            }
        }
        // After inserting all TDataOutputAssociations, we will delete the hierarchy lanes we dont need and layout all remaining
        // with a simple stack layout. Here we set the y coordinate to -40 as the auto layouting would automatically set a margin of 40
        ArrayList<mxCell> cellsToRemove = new ArrayList<>();
        mxStackLayout stackLayoutForDataObject = new mxStackLayout(graph, bpmnProcessorData.isHorizontal(), 15, 0, -40, 0);
        for (int i = bpmnProcessorData.getNodeHierarchyRank().values().size() - 1; i >= 0; i--) {
            if (dataObjectReferenceRootBottom.getChildAt(i).getChildCount() == 0) {
                // If a hierarchy is empty we will save it so we can delete it later on
                cellsToRemove.add((mxCell) dataObjectReferenceRootBottom.getChildAt(i));
            } else {
                stackLayoutForDataObject.execute(dataObjectReferenceRootBottom.getChildAt(i));
            }
        }
        // Delete all unused hierarchies
        graph.removeCells(cellsToRemove.toArray());
        // Then we need to adjust the origin of the hierarchy lanes, so that they are approximately centered according to their source
        for (int i = 0; i < dataObjectReferenceRootBottom.getChildCount(); i++) {
            // The hierarchy node we want to update
            mxICell hierarchyNode = dataObjectReferenceRootBottom.getChildAt(i);
            mxGeometry hierarchyNodeGeometry = hierarchyNode.getGeometry();
            // The other value will be just the data object width/height multiplied by i added the margin for each level
            double xPosition = bpmnProcessorData.isHorizontal() ? hierarchyNodesCenterValue.get(hierarchyNode.getId()) : (DATA_OBJECT_WIDTH * (i + 1)) + (MARGIN * i);
            double yPosition = bpmnProcessorData.isHorizontal() ? (DATA_OBJECT_HEIGHT * i) + (MARGIN * (i + 1)) : hierarchyNodesCenterValue.get(hierarchyNode.getId());
            hierarchyNodeGeometry.setX(xPosition);
            hierarchyNodeGeometry.setY(yPosition);
        }
        // After having handled all TDataObjectReferences with an origin (so which are mentionend in TDataOutputAssociations)
        // we update the bounds of their parents
        int boundsBorderSpace = 20;
        graph.updateGroupBounds(graph.getChildCells(dataObjectReferenceRootBottom));
        graph.updateGroupBounds(new Object[]{dataObjectReferenceRootBottom}, boundsBorderSpace, true);
        // We need to insert all TDataObjectReferences and some do not have an origin, so we have to iterate over them
        // They will be placed above the current graph, so we have to move down the whole graph and change the style of the data
        if (!bpmnProcessorData.getDataObjectReferences().isEmpty()) {
            // Before adding the remaining TDataObjectReferences we will move the whole graph depending on the orientation
            // We have to move it the size of a data object element added the margin from the bounding box
            if (bpmnProcessorData.isHorizontal()) {
                graphElementContainer.getGeometry().setY(DATA_OBJECT_HEIGHT + (boundsBorderSpace * 2));
                dataObjectReferenceRootBottom.getGeometry().setY(dataFlowLanesStartPoint + DATA_OBJECT_HEIGHT + (boundsBorderSpace * 2));
            } else {
                graphElementContainer.getGeometry().setX(DATA_OBJECT_WIDTH + (boundsBorderSpace * 2));
                dataObjectReferenceRootBottom.getGeometry().setX(dataFlowLanesStartPoint + DATA_OBJECT_WIDTH + (boundsBorderSpace * 2));
            }
            mxCell dataObjectReferenceRootTop = (mxCell) graph.insertVertex(graphRootNode, generateRandomId(DATA_OBJECTS_LANE_PREFIX), "", 0, 0, 0, 0);
            // The first one will be placed at the margin, and then we will get some spacing between them
            double startPosition = MARGIN;
            double xPosition;
            double yPosition;
            for (TDataObjectReference tDataObjectReference : bpmnProcessorData.getDataObjectReferences().values()) {
                xPosition = bpmnProcessorData.isHorizontal() ? startPosition : 0;
                yPosition = bpmnProcessorData.isHorizontal() ? 0 : startPosition;
                mxCell dataObjectReferenceVertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(tDataObjectReference.getId());
                dataObjectReferenceVertex.getGeometry().setX(xPosition);
                dataObjectReferenceVertex.getGeometry().setY(yPosition);
                graph.addCell(dataObjectReferenceVertex, dataObjectReferenceRootTop);
                startPosition += (bpmnProcessorData.isHorizontal() ? DATA_OBJECT_WIDTH : DATA_OBJECT_HEIGHT) + MARGIN;
            }
            graph.updateGroupBounds(new Object[]{dataObjectReferenceRootTop}, boundsBorderSpace);
            graph.refresh();
            // Order the data object lane to the top of the graph
            graph.orderCells(true, new Object[]{dataObjectReferenceRootTop});
        }
        // Check if we have to move some TDataObjectReference elements to swimlanes
        if (!swimlanes.isEmpty()) {
            for (mxCell swimlane : swimlanes) {
                // For each swimlane we deal with its TDataObjectReferences which are saved in hierarchy nodes,
                // to have a hierarchical structure
                SwimlaneData swimlaneData = (SwimlaneData) swimlane.getValue();
                // First of all we sort the hierarchy nodes
                ArrayList<Integer> sortedKeys = new ArrayList<>(swimlaneData.getDataHierarchy().keySet());
                Collections.sort(sortedKeys);
                // Then we get the original size of the swimlane depending on the orientation
                double originalSize = bpmnProcessorData.isHorizontal() ? swimlane.getGeometry().getHeight() : swimlane.getGeometry().getWidth();
                // After that we will go through the hierarchies
                for (int j = 0; j < sortedKeys.size(); j++) {
                    mxICell hierarchyNode = swimlaneData.getDataHierarchy().get(sortedKeys.get(j));
                    // Here we also would layout the hierarchy nodes of the swimlane
                    stackLayoutForDataObject.execute(hierarchyNode);
                    mxGeometry hierarchyNodeGeometry = hierarchyNode.getGeometry();
                    // The other value will be just the data object width/height multiplied by i added the margin for each level
                    // Of course when handling with swimlanes we also have to consider there original start position subtracted by the margin
                    double xPosition = bpmnProcessorData.isHorizontal() ? hierarchyNodesCenterValue.get(hierarchyNode.getId()) : (originalSize - MARGIN) + (DATA_OBJECT_WIDTH * (j + 1)) + (MARGIN * j);
                    double yPosition = bpmnProcessorData.isHorizontal() ? (DATA_OBJECT_HEIGHT * j) + (MARGIN * (j + 1)) + (originalSize - MARGIN) : hierarchyNodesCenterValue.get(hierarchyNode.getId());
                    // Furthermore, we must subtract the start position of the swimlane so the hierarchy lanes are almost centered
                    if (bpmnProcessorData.isHorizontal()) {
                        xPosition -= graph.getView().getState(swimlane).getX();
                    } else {
                        yPosition -= graph.getView().getState(swimlane).getY();
                    }
                    hierarchyNodeGeometry.setX(xPosition);
                    hierarchyNodeGeometry.setY(yPosition);
                }
                // Then, we can add all the hierarchy nodes to the swimlane
                graph.addCells(swimlaneData.getDataHierarchy().values().toArray(), swimlane);
                graph.refresh();
                // Now, we edit the size of the swimlane so that it respects the added TDataObjectReference elements
                if (bpmnProcessorData.isHorizontal()) {
                    swimlane.getGeometry().setHeight(swimlane.getGeometry().getHeight() + MARGIN);
                } else {
                    swimlane.getGeometry().setWidth(swimlane.getGeometry().getWidth() + MARGIN);
                }
                // Then we have to calculate the value with which we must move the other cells which are placed after the current swimlane,
                // so that nothing is overlapping
                double valueToMove = bpmnProcessorData.isHorizontal() ? swimlane.getGeometry().getHeight() : swimlane.getGeometry().getWidth();
                valueToMove -= originalSize;
                int xValue = bpmnProcessorData.isHorizontal() ? 0 : (int) valueToMove;
                int yValue = bpmnProcessorData.isHorizontal() ? (int) valueToMove : 0;
                moveSwimlanes(swimlane, xValue, yValue);
                graph.refresh();
            }
            if (dataObjectReferenceRootBottom.getChildCount() == 0) {
                // If we have moved all TDataObjectReference elements to swimlanes, we don't need the placeholder for TDataObjectReferences anymore,
                // therefore we delete it
                graph.removeCells(new Object[]{dataObjectReferenceRootBottom});
            }
            graph.updateGroupBounds(new Object[]{graphElementContainer, graphRootNode});
            graph.refresh();
        }
    }

    /**
     * Method to recursively move the swimlanes which come after a swimlane to which we have added TDataObjectReferences
     * @param swimlane swimlane which we deal with
     * @param xValue how much they must be moved on the x axis
     * @param yValue how much they must be moved on the x axis
     */
    private void moveSwimlanes(mxCell swimlane, int xValue, int yValue) {
        ArrayList<mxCell> swimlanesToMove = new ArrayList<>();
        boolean moveCells = false;
        // Go through the childs of the swimlane parent
        for (int i = 0; i < swimlane.getParent().getChildCount(); i++) {
            if (swimlane.getParent().getChildAt(i).getId().equals(swimlane.getId())) {
                // As soon as we have seen the current swimlane we know that we can from now on move the coming swimlanes
                moveCells = true;
                continue;
            }
            if (moveCells) {
                // Save the swimlanes which must be moved
                swimlanesToMove.add((mxCell) swimlane.getParent().getChildAt(i));
            }
        }
        if (!swimlane.getParent().getId().startsWith(ELEMENT_CONTAINER_PREFIX)) {
            // If the parent is not the graph element container we must handle the next layer of swimlanes
            moveSwimlanes((mxCell) swimlane.getParent(), xValue, yValue);
        }
        graph.moveCells(swimlanesToMove.toArray(), xValue, yValue, false, swimlane.getParent(), new java.awt.Point(xValue, yValue));
    }

    /**
     * Method to handle and layout TDataStoreReference elements and also some TDataOutputAssociations, as we create them along with the TDataStoreReference elements.
     * TDataObjectReferences will be placed below or next to the current diagram.
     */
    private void handleDataStoreReferencesAssociations() {
        mxCell graphRootNode = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphRootNodeId());
        // Set the position depending on whether it should be horizontal or not
        double rootXPosition = bpmnProcessorData.isHorizontal() ? 30 : graphRootNode.getGeometry().getWidth();
        double rootYPosition = bpmnProcessorData.isHorizontal() ? graphRootNode.getGeometry().getHeight() : 30;
        double width = bpmnProcessorData.isHorizontal() ? 0 : DATA_STORE_HEIGHT + (2 * MARGIN);
        double height = bpmnProcessorData.isHorizontal() ? DATA_STORE_WIDTH + (2 * MARGIN) : 0;
        mxCell dataStoreRoot = (mxCell) graph.insertVertex(graphRootNode, generateRandomId(DATA_STORE_LANE_PREFIX), "", rootXPosition, rootYPosition, width, height);
        double xPosition = bpmnProcessorData.isHorizontal() ? 0 : MARGIN;
        double yPosition = bpmnProcessorData.isHorizontal() ? MARGIN : 0;
        // First, we will go through all TDataOutputAssociations and create the vertex for TDataStoreReference elements which have an origin
        for (Map.Entry<String, List<TDataOutputAssociation>> tDataOutputAssociations : bpmnProcessorData.getDataOutputAssociations().entrySet()) {
            mxCell sourceVertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(tDataOutputAssociations.getKey());
            // One sourceVertex can have multiple TDataOutputAssociations
            for (TDataOutputAssociation tDataOutputAssociation : tDataOutputAssociations.getValue()) {
                String targetId = ((TBaseElement) tDataOutputAssociation.getTargetRef()).getId();
                if (bpmnProcessorData.getDataStoreReference().containsKey(targetId)) {
                    // We get the corresponding TDataStoreReference from the TDataOutputAssociation and remove it from our list
                    TDataStoreReference tDataStoreReference = bpmnProcessorData.getDataStoreReference().get(targetId);
                    bpmnProcessorData.getDataStoreReference().remove(targetId);
                    // We get the TDataStoreReference and associate it to its parent
                    mxCell dataObjectReferenceVertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(tDataStoreReference.getId());
                    dataObjectReferenceVertex.getGeometry().setX(xPosition);
                    dataObjectReferenceVertex.getGeometry().setY(yPosition);
                    graph.addCell(dataObjectReferenceVertex, dataStoreRoot);
                    // We will also create the edges immediately
                    graph.insertEdge(dataStoreRoot, tDataOutputAssociation.getId(), "", sourceVertex, dataObjectReferenceVertex);
                    if (bpmnProcessorData.isHorizontal()) {
                        xPosition += DATA_STORE_WIDTH + MARGIN;
                    } else {
                        yPosition += DATA_STORE_HEIGHT + MARGIN;
                    }
                }
            }
        }
        // We need to insert all TDataStoreReference and some do not have an origin, so we have to iterate over them
        if (!bpmnProcessorData.getDataStoreReference().isEmpty()) {
            for (TDataStoreReference tDataStoreReference : bpmnProcessorData.getDataStoreReference().values()) {
                mxCell dataObjectReferenceVertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(tDataStoreReference.getId());
                dataObjectReferenceVertex.getGeometry().setX(xPosition);
                dataObjectReferenceVertex.getGeometry().setY(yPosition);
                graph.addCell(dataObjectReferenceVertex, dataStoreRoot);
                if (bpmnProcessorData.isHorizontal()) {
                    xPosition += DATA_STORE_WIDTH + MARGIN;
                } else {
                    yPosition += DATA_STORE_HEIGHT + MARGIN;
                }
            }
        }
        graph.updateCellSize(graphRootNode);
        graph.refresh();
    }

    /**
     * Method to create edges for TDataInputAssociation
     */
    private void handleDataInputAssociations() {
        // Then, after creating all TDataObjectReferences we can handle the TDataInputAssociations
        for (TDataInputAssociation tDataInputAssociation : bpmnProcessorData.getDataInputAssociations()) {
            // We get the target ID of the TDataInputAssociations, sometimes they can be a property, so we need to react to that too.
            String targetID = (((TBaseElement) tDataInputAssociation.getTargetRef()).getId());
            Object targetVertex = findVertex(targetID);
            // TDataInputAssociation can have more than one sourceRef
            for (JAXBElement<Object> sourceRef : tDataInputAssociation.getSourceRef()) {
                String sourceId = ((TBaseElement) sourceRef.getValue()).getId();
                mxCell sourceVertex = findVertex(sourceId);
                graph.insertEdge(sourceVertex.getParent(), tDataInputAssociation.getId(), "", sourceVertex, targetVertex);
            }
        }
    }

    /**
     * Method to find a vertex. It can be that we need the TPropery or IOSpecification of a BPMN element, they would
     * not be a standalone vertex, but they are associated with their BPMN element vertex
     * @param id of the vertex to search (or TProperty or IOSpecification)
     * @return the vertex
     */
    private mxCell findVertex(String id) {
        mxCell vertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(id);
        // If the target is not a created vertex (TActivity element) itslef, it is a property of such
        if (vertex == null) {
            String parentId;
            parentId = bpmnProcessorData.getProperties().get(id);
            if (parentId == null) {
                parentId = bpmnProcessorData.getIoSpecification().get(id);
            }
            vertex = (mxCell) ((mxGraphModel) graph.getModel()).getCell(parentId);
        }
        return vertex;
    }
}
