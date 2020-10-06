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
import com.mxgraph.view.mxGraph;
import org.omg.spec.bpmn.model.TArtifact;
import org.omg.spec.bpmn.model.TAssociation;
import org.omg.spec.bpmn.model.TTextAnnotation;
import science.aist.bpmn.viz.BpmnProcessor;
import science.aist.bpmn.viz.BpmnProcessorData;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

import static science.aist.bpmn.viz.VisualizationConstants.*;
import static science.aist.bpmn.viz.VisualizationUtils.*;

/**
 * <p>Processes TArtifcats of a BPMN model.</p>
 * <p>At the moment it can handle:</p>
 * <ul>
 *     <li>TTextAnnotations elements</li>
 *     <li>TAssociation</li>
 * </ul>
 *
 * @author Clemens Toegel
 */

public class ArtifactBpmnProcessorImpl implements BpmnProcessor {

    private final BpmnProcessorData bpmnProcessorData;
    private final List<JAXBElement<? extends TArtifact>> artifacts;
    private final boolean hasSwimlanes;
    private mxGraph graph;
    // Array to check which swimlanes were already moved
    private List<String> movedSwimlanesId;


    public ArtifactBpmnProcessorImpl(BpmnProcessorData bpmnProcessorData, List<JAXBElement<? extends TArtifact>> artifacts, boolean hasSwimlanes) {
        this.bpmnProcessorData = bpmnProcessorData;
        this.artifacts = artifacts;
        this.hasSwimlanes = hasSwimlanes;
    }


    @Override
    public mxGraph process(mxGraph graph) {
        this.graph = graph;
        if (hasSwimlanes) {
            movedSwimlanesId = new ArrayList<>();
        }
        handleArtifacts();
        return this.graph;
    }

    private void handleArtifacts() {
        // process artifacts
        for (JAXBElement<? extends TArtifact> artifact : artifacts) {
            if (artifact.getValue() instanceof TAssociation) {
                handleAssociation((TAssociation) artifact.getValue());
            } else if (artifact.getValue() instanceof TTextAnnotation) {
                handleTextAnnotation((TTextAnnotation) artifact.getValue());
            }
        }
        // After gathering the elements it goes through the associations. Here we know, that the association source must be
        // already in the graph and the target must be created by us as it is the missing TTextAnnotation
        for (TAssociation tAssociation : bpmnProcessorData.getAssociations()) {
            mxCell sourceCell = (mxCell) ((mxGraphModel) graph.getModel()).getCell(tAssociation.getSourceRef().getLocalPart());
            double startY = bpmnProcessorData.isHorizontal() ? sourceCell.getGeometry().getY() - TEXT_ANNOTATION_MARGIN : sourceCell.getGeometry().getY() - (TEXT_ANNOTATION_HEIGHT + 10);
            double startX = sourceCell.getGeometry().getX() + sourceCell.getGeometry().getWidth();
            // Check if the graph has swimlanes, because then we need to make the swimlanes bigger, so that
            // the annotations fit into them
            if (hasSwimlanes) {
                mxCell swimlane = (mxCell) sourceCell.getParent();
                if (!movedSwimlanesId.contains(swimlane.getId())) {
                    mxCell rootOfSwimlane = (mxCell) swimlane.getParent();
                    boolean move = false;
                    ArrayList<mxCell> lanesToMove = new ArrayList<>();
                    // Get the childs (swimlanes) which needs to be moved down as the annotations are above an element
                    for (Object child : graph.getChildCells(rootOfSwimlane)) {
                        if (child.equals(swimlane)) {
                            move = true;
                        } else {
                            if (move) {
                                lanesToMove.add((mxCell) child);
                            }
                        }
                    }
                    if (!lanesToMove.isEmpty()) {
                        if (bpmnProcessorData.isHorizontal()) {
                            // First we increase the height of the root of the swimlane by the value we want to move the cells
                            rootOfSwimlane.getGeometry().setHeight(rootOfSwimlane.getGeometry().getHeight() + TEXT_ANNOTATION_MARGIN);
                            // Then we move the cells
                            graph.moveCells(lanesToMove.toArray(), 0, TEXT_ANNOTATION_MARGIN);
                        } else {
                            // First we increase the width of the root of the swimlane by the value we want to move the cells
                            rootOfSwimlane.getGeometry().setWidth(rootOfSwimlane.getGeometry().getWidth() + TEXT_ANNOTATION_MARGIN);
                            // Then we move the cells
                            graph.moveCells(lanesToMove.toArray(), TEXT_ANNOTATION_MARGIN, 0);
                        }
                    }
                    // Depending on the orientation we resize cells and move some.
                    if (bpmnProcessorData.isHorizontal()) {
                        // Then we also increase the height of the swimlane
                        swimlane.getGeometry().setHeight(swimlane.getGeometry().getHeight() + TEXT_ANNOTATION_MARGIN);
                        // Then we move the cells
                        graph.moveCells(graph.getChildCells(swimlane), 0, TEXT_ANNOTATION_MARGIN);
                    } else {
                        // We increase the width of the swimlane
                        swimlane.getGeometry().setWidth(swimlane.getGeometry().getWidth() + TEXT_ANNOTATION_MARGIN);
                    }
                    movedSwimlanesId.add(swimlane.getId());
                }
                if (bpmnProcessorData.isHorizontal()) {
                    // Now, we can also set the y value to 20 so that the text annotation is on top with a bit of a margin
                    startY = 20;
                }
            }
            mxCell targetCell = (mxCell) graph.insertVertex(sourceCell.getParent(), tAssociation.getTargetRef().getLocalPart(), null, startX, startY, TEXT_ANNOTATION_WIDTH, TEXT_ANNOTATION_HEIGHT);
            mxCell graphElementContainer = (mxCell) ((mxGraphModel) graph.getModel()).getCell(bpmnProcessorData.getGraphElementContainerId());
            graph.insertEdge(graphElementContainer, tAssociation.getId(), null, sourceCell, targetCell);
        }
    }

    private void handleTextAnnotation(TTextAnnotation artifact) {
        if (artifact.getId() == null)
            artifact.setId(generateRandomId(ARTIFACT_PREFIX));
        bpmnProcessorData.getTextAnnotations().put(artifact.getId(), artifact);
    }

    private void handleAssociation(TAssociation association) {
        if (association.getId() == null) association.setId(generateRandomId(ARTIFACT_PREFIX));
        bpmnProcessorData.getAssociations().add(association);
    }
}
