/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.omg.spec.bpmn.model.*;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Object which contains all information for instances of BpmnProcessor.</p>
 *
 * @author Clemens Toegel
 */

@Getter
@Setter
@RequiredArgsConstructor
public class BpmnProcessorData {

    private final JAXBElement<TDefinitions> tDefinitionsJAXBElement;
    private final boolean horizontal;

    // All diagram elements of a BPMN file
    private List<JAXBElement<? extends DiagramElement>> diagramElements;
    private String bpmnPlaneId;
    // All sub processes of a process element
    private Map<String, BpmnAutoLayout> bpmnSubProcesses = new HashMap<>();

    private String graphElementContainerId;
    private String graphRootNodeId;

    // Collaboration if exits
    private TCollaboration tCollaboration;
    private ArrayList<TProcess> tProcesses = new ArrayList<>();
    // If a collaboration exists we need to map the TParticipant element to the ID of the corresponding TProcess
    private Map<String, TParticipant> participantProcessMap;
    // TParticipant which are a Black Box
    private ArrayList<TParticipant> blackBoxParticipants;

    // BPMN elements to which we react
    private HashMap<String, TSequenceFlow> sequenceFlows;
    private HashMap<String, TDataObjectReference> dataObjectReferences;
    private List<TDataInputAssociation> dataInputAssociations;
    private HashMap<String, List<TDataOutputAssociation>> dataOutputAssociations;
    private HashMap<String, TDataStoreReference> dataStoreReference;
    private List<TBoundaryEvent> boundaryEvents;
    private Map<String, TTextAnnotation> textAnnotations;
    private List<TAssociation> associations;
    // List of sequence flows with conditions
    private ArrayList<String> sequenceFlowCondition;
    // Associate a property to its parent, by saving the parent ID to the property ID
    private HashMap<String, String> properties;
    // Associate TDataInput or TDataOutput to its parent, by saving the parent ID to the given ID
    private HashMap<String, String> ioSpecification;
    private List<TMessageFlow> messageFlow;

    // Hierarchy of the graph, where the key is the id of a cell
    private HashMap<String, Integer> nodeHierarchyRank;
}
