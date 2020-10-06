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
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import org.omg.spec.bpmn.model.TBaseElement;
import org.omg.spec.bpmn.model.TLane;
import org.omg.spec.bpmn.model.TLaneSet;
import science.aist.bpmn.viz.BpmnProcessor;
import science.aist.bpmn.viz.BpmnProcessorData;
import science.aist.bpmn.viz.SwimlaneData;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static science.aist.bpmn.viz.VisualizationConstants.*;
import static science.aist.bpmn.viz.VisualizationUtils.*;

/**
 * <p>Processes swimlanes and nested swimlanes of a BPMN model.</p>
 *
 * @author Clemens Toegel
 */

public class SwimlaneBpmnProcessorImpl implements BpmnProcessor {

    private mxGraph graph;
    private final TLaneSet tLaneSet;
    private final BpmnProcessorData bpmnProcessorData;

    // Map to associate swimlane id with layer value
    private final HashMap<String, Integer> swimlaneLayerMap = new HashMap<>();
    // Saves the layer deepness of the swimlanes, ergo how many nested swimlanes exists
    private int swimlaneLayerMapDeepness = 1;

    public SwimlaneBpmnProcessorImpl(BpmnProcessorData bpmnProcessorData, TLaneSet tLaneSet) {
        this.bpmnProcessorData = bpmnProcessorData;
        this.tLaneSet = tLaneSet;
    }

    public mxGraph process(mxGraph graph) {
        this.graph = graph;
        mxCell graphElementContainer = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphElementContainerId());
        double originalWidth = graphElementContainer.getGeometry().getWidth();
        double originalHeight = graphElementContainer.getGeometry().getHeight();
        handleSwimlanes(tLaneSet, graphElementContainer, originalWidth, originalHeight, 0);
        postProcessSwimlanes();
        return this.graph;
    }

    /**
     * Method to handle swimlanes. We go through all swimlanes and if they have child swimlanes we recursively add them.
     * @param tLaneSet the current lane set (can also be a TChildLaneSet)
     * @param parentCell the parent cell of the current swimlane
     * @param originalWidth width of the current container
     * @param originalHeight height of the current container
     * @param swimlaneLayer the layer  of the current swimlane (if it is a nested swimlane the layer increases)
     */
    private void handleSwimlanes(TLaneSet tLaneSet, mxCell parentCell, double originalWidth, double originalHeight, int swimlaneLayer) {
        double xPosition = 0;
        double yPosition = 0;
        for (TLane tLane : tLaneSet.getLane()) {
            boolean isOneLine = true;
            ArrayList<Integer> hierarchyArray = new ArrayList<>();
            double contentSize = 0;
            ArrayList<mxCell> swimLaneChilds = new ArrayList<>();
            for (JAXBElement<Object> flowNodeRef : tLane.getFlowNodeRef()) {
                mxCell swimLaneChild = (mxCell) ((mxGraphModel) graph.getModel()).getCell(((TBaseElement) flowNodeRef.getValue()).getId());
                swimLaneChilds.add(swimLaneChild);
                // Here we check if the hierarchy value is already in our array, if so we know that we have two objects with the same hierarchy,
                // which means the elements of the swimlane can not be in one line
                if (hierarchyArray.contains(bpmnProcessorData.getNodeHierarchyRank().get(swimLaneChild.getId()))) {
                    isOneLine = false;
                }
                // contentSize is the maximum height/width value of the elements in the swimlane
                if (contentSize < (bpmnProcessorData.isHorizontal() ? swimLaneChild.getGeometry().getHeight() : swimLaneChild.getGeometry().getWidth())) {
                    contentSize = (bpmnProcessorData.isHorizontal() ? swimLaneChild.getGeometry().getHeight() : swimLaneChild.getGeometry().getWidth());
                }
                hierarchyArray.add(bpmnProcessorData.getNodeHierarchyRank().get(swimLaneChild.getId()));
            }
            mxRectangle boundingBoxFromSwimlaneChilds = graph.getBoundingBoxFromGeometry(swimLaneChilds.toArray());
            // If the layout is horizontal we the swimlane object must have a width of the whole size with a margin, if not it will be the size of its childs with margin to the right and left
            // For vertical layout it is just inverted
            double width = bpmnProcessorData.isHorizontal() ? (originalWidth + MARGIN) : boundingBoxFromSwimlaneChilds.getWidth() + (2 * MARGIN);
            double height = bpmnProcessorData.isHorizontal() ? boundingBoxFromSwimlaneChilds.getHeight() + (2 * MARGIN) : (originalHeight + MARGIN);
            // If all elements of the swimlane are in one line we can set the height/width to the biggest element and add a margin
            if (isOneLine) {
                if (bpmnProcessorData.isHorizontal()) {
                    height = contentSize + (2 * MARGIN);
                } else {
                    width = contentSize + (2 * MARGIN);
                }
            }
            mxCell swimlane = (mxCell) graph.insertVertex(parentCell, tLane.getId(), "", xPosition, yPosition, width, height);
            // Go through each cell and set the correct start value so that the elements are centered
            for (mxCell swimLaneChild : swimLaneChilds) {
                double relativeCenterPosition;
                double calibratedPosition;
                if (bpmnProcessorData.isHorizontal()) {
                    // If the swimlane only consists of only elements which are in one line we can center them
                    if (isOneLine) {
                        calibratedPosition = (height / 2) - (swimLaneChild.getGeometry().getHeight() / 2);
                    } else {
                        // Otherwise we calculate the position for the swimlane
                        relativeCenterPosition = swimLaneChild.getGeometry().getCenterY() / originalHeight;
                        calibratedPosition = ((height - 2 * MARGIN) * relativeCenterPosition);
                        calibratedPosition -= swimLaneChild.getGeometry().getHeight() / 2;
                    }
                    swimLaneChild.getGeometry().setY(calibratedPosition);
                } else {
                    if (isOneLine) {
                        calibratedPosition = (width / 2) - (swimLaneChild.getGeometry().getWidth() / 2);
                    } else {
                        relativeCenterPosition = swimLaneChild.getGeometry().getCenterX() / originalWidth;
                        calibratedPosition = ((width - 2 * MARGIN) * relativeCenterPosition);
                        calibratedPosition -= swimLaneChild.getGeometry().getWidth() / 2;
                    }
                    swimLaneChild.getGeometry().setX(calibratedPosition);
                }
            }
            // Here we add all elements to a swimlane
            graph.addCells(swimLaneChilds.toArray(), swimlane);
            // If the parent cell is not the element container of the graph we set the correct parent cell to a swimlane cell
            if (!parentCell.getId().equals(bpmnProcessorData.getGraphElementContainerId())) {
                graph.addCell(swimlane, parentCell);
            }
            // When a swimlane is not a one liner, we have to move all childs again by the margin,
            // so that they are centered as we have positioned the childs without respecting the margin
            if (!isOneLine) {
                if (bpmnProcessorData.isHorizontal()) {
                    graph.moveCells(swimLaneChilds.toArray(), 0, BPMN_IO_MARGIN, false, swimlane, new java.awt.Point(0, BPMN_IO_MARGIN));
                } else {
                    graph.moveCells(swimLaneChilds.toArray(), BPMN_IO_MARGIN, 0, false, swimlane, new java.awt.Point(BPMN_IO_MARGIN, 0));
                }
            }
            double swimLaneWidth = swimlane.getGeometry().getWidth();
            double swimLaneHeight = swimlane.getGeometry().getHeight();
            if (tLane.getChildLaneSet() != null) {
                if (bpmnProcessorData.isHorizontal()) {
                    swimLaneWidth -= MARGIN;
                } else {
                    swimLaneHeight -= MARGIN;
                }
                // We update the deepness of the swimlanes
                swimlaneLayerMapDeepness++;
                // We mark the swimlane so that we know it has nested swimlanes
                swimlane.setValue(new SwimlaneData(NESTED_SWIMLANE));
                handleSwimlanes(tLane.getChildLaneSet(), swimlane, swimLaneWidth, swimLaneHeight, swimlaneLayer + 1);
                // We realize the new changes
                graph.refresh();
                // We need the bounding box and need to subtract the y value from the height or the x value from the width,
                // otherwise we would have extra spacing
                mxRectangle swimLaneBounds = graph.getBoundingBox(swimlane);
                if (bpmnProcessorData.isHorizontal()) {
                    swimlane.getGeometry().setHeight(swimlane.getGeometry().getHeight() - swimLaneBounds.getY());
                    swimLaneHeight = swimlane.getGeometry().getHeight();
                } else {
                    swimlane.getGeometry().setWidth(swimlane.getGeometry().getWidth() - swimLaneBounds.getX());
                    // Here we also have to already add the label margin which we will add later on
                    swimLaneWidth = swimlane.getGeometry().getWidth() + ((swimlane.getChildCount() - 1) * BPMN_IO_MARGIN);
                }
            }
            // Mark the swimlane as swimlane if it isnt already marked as nested swimlane
            if (swimlane.getValue().equals("")) {
                swimlane.setValue(new SwimlaneData(SWIMLANE));
            }
            swimlaneLayerMap.put(swimlane.getId(), swimlaneLayer);
            // Depending on the orientation we set the new start value
            if (bpmnProcessorData.isHorizontal()) {
                yPosition += swimLaneHeight;
            } else {
                xPosition += swimLaneWidth;
            }
        }
        // We need update cell size as it could be that the graph reduced it size
        graph.updateCellSize(parentCell);
        graph.refresh();
    }

    /**
     * Method to postprocess swimlanes: Specific change for www.bpmn.io BPMN viewer so that the labels are not overlapping
     */
    void postProcessSwimlanes() {
        // Here we go through all swimlanes and move the, so that the label is not overlapped
        for (Map.Entry<String, Integer> swimlaneHierarchyEntry : swimlaneLayerMap.entrySet()) {
            mxCell swimlane = (mxCell) ((mxGraphModel) graph.getModel()).getCell(swimlaneHierarchyEntry.getKey());
            // We will move the cells of a swimlane by the bpmn.io margin times the amount of nested swimlanes subtracted by the hierarchy value
            double xValuesToMove = (swimlaneLayerMapDeepness - swimlaneHierarchyEntry.getValue()) * BPMN_IO_MARGIN;
            if (((SwimlaneData)swimlane.getValue()).getId().equals(NESTED_SWIMLANE) && bpmnProcessorData.isHorizontal()) {
                // However, if we see that the swimlane has got nested swimlanes and the orientation is horizontal we
                // only add the bpmn.io margin
                xValuesToMove = BPMN_IO_MARGIN;
            } else if (!bpmnProcessorData.isHorizontal()) {
                // If we see that the orientation is vertical we often need no the full bpmn.io margin and therefore
                // subtract it by the start x value
                xValuesToMove = BPMN_IO_MARGIN - graph.getBoundingBoxFromGeometry(graph.getChildCells(swimlane)).getX();
            }
            if (!bpmnProcessorData.isHorizontal() && !((SwimlaneData)swimlane.getValue()).getId().equals(NESTED_SWIMLANE)) {
                graph.moveCells(graph.getChildCells(swimlane), 0, MARGIN, false, swimlane, new java.awt.Point(0, MARGIN));
            }
            graph.moveCells(graph.getChildCells(swimlane), xValuesToMove, 0, false, swimlane, new java.awt.Point((int) xValuesToMove, 0));
        }
        mxCell graphElementContainer = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphElementContainerId());
        // If the orientation is horizontal we must update the width of the child cells again, so that all are equally long
        // if the orientation is vertical we must update the height
        double sizeToChange = bpmnProcessorData.isHorizontal() ? graphElementContainer.getGeometry().getWidth() + BPMN_IO_MARGIN :
                graphElementContainer.getGeometry().getHeight() + MARGIN;
        for (Map.Entry<String, Integer> swimlaneEntry : swimlaneLayerMap.entrySet()) {
            mxCell swimlane = (mxCell) ((mxGraphModel) graph.getModel()).getCell(swimlaneEntry.getKey());
            if (bpmnProcessorData.isHorizontal()) {
                // We must set the width of the swimlane to the graphElementContainer width subtracted by
                // the bpmn.io margin times the hierarchy of the swimlane
                // with that we ensure that we always have some margin and all swimlanes are equally long.
                double parentWidth = sizeToChange - (BPMN_IO_MARGIN * swimlaneEntry.getValue());
                swimlane.getGeometry().setWidth(parentWidth);
            } else {
                // We must set the height of the swimlane to the graphElementContainer height subtracted by
                // the margin with that we ensure that we always have some margin and all swimlanes are equally long.
                swimlane.getGeometry().setHeight(sizeToChange);
            }
        }
        graph.refresh();
    }
}
