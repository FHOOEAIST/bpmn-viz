/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz.impl;

import com.mxgraph.view.mxGraph;
import org.omg.spec.bpmn.model.*;
import science.aist.bpmn.model.BPMNHelper;
import science.aist.bpmn.viz.BpmnProcessor;
import science.aist.bpmn.viz.BpmnProcessorData;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;

import static science.aist.bpmn.viz.VisualizationConstants.*;
import static science.aist.bpmn.viz.VisualizationUtils.*;

/**
 * <p>Processes the root elements of a BPMN model.</p>
 * <ul>
 *     <li>TCollaboration elements<ul>
 *         <li>TParticipants (also black box participants, which means they have no processRef attribute)</li>
 *         <li>TMessageFlow</li>
 *     </ul></li>
 *     <li>TProcesses elements</li>
 * </ul>
 *
 * @author Clemens Toegel
 */

public class RootElementBpmnProcessorImpl implements BpmnProcessor {

    private final BpmnProcessorData bpmnProcessorData;

    public RootElementBpmnProcessorImpl(BpmnProcessorData bpmnProcessorData) {
        this.bpmnProcessorData = bpmnProcessorData;
    }

    @Override
    public mxGraph process(mxGraph graph) {
        handleRootElements();
        // We don't need to return a graph as we don't manipulate it
        return null;
    }

    private void handleRootElements() {
        // We will setup a new BPMN diagram, it does not matter if one already exists as the standard states that "Multiple BPMN diagrams can be exchanged referencing model elements from the same BPMN model".
        BPMNDiagram bpmnDiagram = OBJECT_FACTORY.createBPMNDiagram();
        // Extract the collaboration and all its participants if possible
        // Must be done alone, so that we already know the information before going through the processes
        // At the moment we can only react to one TCollaboration, if there are more we would always override the graph with the next TCollaboration
        for (JAXBElement<? extends TRootElement> tRootElement : bpmnProcessorData.getTDefinitionsJAXBElement().getValue().getRootElement()) {
            if (tRootElement.getValue() instanceof TCollaboration) {
                bpmnProcessorData.setDiagramElements(setupBpmnPlane(bpmnDiagram, tRootElement.getValue()).getDiagramElement());
                bpmnProcessorData.setTCollaboration((TCollaboration) tRootElement.getValue());
                bpmnProcessorData.setParticipantProcessMap(new HashMap<>());
                bpmnProcessorData.setBlackBoxParticipants(new ArrayList<>());
                for (TParticipant tParticipant : bpmnProcessorData.getTCollaboration().getParticipant()) {
                    QName processRef = tParticipant.getProcessRef();
                    if (processRef != null) {
                        // Map the TParticipant element to the process ID
                        bpmnProcessorData.getParticipantProcessMap().put(tParticipant.getProcessRef().getLocalPart(), tParticipant);
                    } else {
                        // Black Box participant
                        bpmnProcessorData.getBlackBoxParticipants().add(tParticipant);
                    }
                }
                bpmnProcessorData.setMessageFlow(bpmnProcessorData.getTCollaboration().getMessageFlow());
            }
            // Checks if the root element is a TProcess
            if (tRootElement.getValue() instanceof TProcess) {
                TProcess tProcess = (TProcess) tRootElement.getValue();
                // If we don't have a TCollaboration we have to setup a BPMN plane
                if (bpmnProcessorData.getDiagramElements() == null) {
                    // We can only have one BPMN plane, so if we have multiple processes we can only setup one BPMN plane for all
                    bpmnProcessorData.setDiagramElements(setupBpmnPlane(bpmnDiagram, tProcess).getDiagramElement());
                }
                bpmnProcessorData.getTProcesses().add(tProcess);
            }
        }
    }

    /**
     * Method to create a BPMN Plane. Will be created for the TCollaboration or if no collaboration exists for the first TProcess element
     *
     * @param bpmnDiagram the BPMN diagram element
     * @param rootElement either TCollaboration element, otherwise TProcess element
     * @return the corresponding BPMN plane on which all diagram elements will be placed
     */
    private BPMNPlane setupBpmnPlane(BPMNDiagram bpmnDiagram, TRootElement rootElement) {
        BPMNPlane bpmnPlane = OBJECT_FACTORY.createBPMNPlane();
        bpmnPlane.setBpmnElement(BPMNHelper.createQName(rootElement.getId()));
        bpmnDiagram.setBPMNPlane(bpmnPlane);
        bpmnProcessorData.setBpmnPlaneId(rootElement.getId());
        bpmnProcessorData.getTDefinitionsJAXBElement().getValue().getBPMNDiagram().add(bpmnDiagram);
        return bpmnPlane;
    }
}
