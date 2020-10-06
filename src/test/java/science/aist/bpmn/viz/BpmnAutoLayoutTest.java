/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

import lombok.SneakyThrows;
import org.omg.spec.bpmn.model.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import science.aist.bpmn.model.XMLRepository;
import science.aist.bpmn.model.impl.BPMNTDefinitionsRepository;

import javax.xml.bind.JAXBElement;
import java.io.FileOutputStream;

/**
 * <p>Test class to test the generation of BPMN diagrams</p>
 *
 * @author Andreas Pointner
 * @author Clemens Toegel
 */
public class BpmnAutoLayoutTest {

    private final XMLRepository<TDefinitions> repository = new BPMNTDefinitionsRepository();
    // Orientation of the BPMN diagram layout
    private final boolean horizontalLayout = true;

    @SneakyThrows
    @Test
    public void testCpgWithAnnotations() {
        // given
        String testFileName = "/cpg-common-registration-with-annotations.bpmn";

        // when
        JAXBElement<TDefinitions> tDefinitionsJAXBElement = repository.load(getClass().getResourceAsStream(testFileName));
        BpmnProcessorData bpmnProcessorData = new BpmnProcessorData(tDefinitionsJAXBElement, horizontalLayout);
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnProcessorData);
        bpmnAutoLayout.execute();

        // then
        Assert.assertNotNull(tDefinitionsJAXBElement);
        TDefinitions value = tDefinitionsJAXBElement.getValue();
        Assert.assertNotNull(value);
        // Check if TDefinition has got diagrams
        Assert.assertNotNull(value.getBPMNDiagram());
        // Checks if TDefinitions has got the right amount of diagrams (for each process one diagram)
        // To do this we first need to count the TProcesses in TDefinition
        int tProcesses = 0;
        for (JAXBElement<? extends TRootElement> process : tDefinitionsJAXBElement.getValue().getRootElement()) {
            // Checks if the root element is a TProcess
            if (process.getValue() instanceof TProcess) {
                tProcesses++;
            }
        }
        Assert.assertEquals(tProcesses, value.getBPMNDiagram().size());
        // Check if BPMN diagram has got the specific a BPMN plane with the right name for the first process
        // If the bpmnElement of the BPMN plane equals the id of the process this is true
        BPMNPlane bpmnPlane = value.getBPMNDiagram().get(0).getBPMNPlane();
        Assert.assertEquals(bpmnPlane.getBpmnElement().getLocalPart(), "PlanDefinition_CPG_Common_Registration");
        // Saves the new TDefinitions for visualization purposes
        repository.save(tDefinitionsJAXBElement, new FileOutputStream("target/test-".concat(testFileName.substring(1))));
    }

    @SneakyThrows
    @Test
    public void testAncContact() {
        // given
        String testFileName = "/anc-contact.bpmn";

        // when
        JAXBElement<TDefinitions> tDefinitionsJAXBElement = repository.load(getClass().getResourceAsStream(testFileName));
        BpmnProcessorData bpmnProcessorData = new BpmnProcessorData(tDefinitionsJAXBElement, horizontalLayout);
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnProcessorData);
        bpmnAutoLayout.execute();

        // then
        Assert.assertNotNull(tDefinitionsJAXBElement);
        TDefinitions value = tDefinitionsJAXBElement.getValue();
        Assert.assertNotNull(value);
        // Check if TDefinition has got diagrams
        Assert.assertNotNull(value.getBPMNDiagram());
        // Checks if TDefinitions has got the right amount of diagrams (for each process one diagram)
        // To do this we first need to count the TProcesses in TDefinition
        int actualTProcesses = 0;
        for (JAXBElement<? extends TRootElement> process : tDefinitionsJAXBElement.getValue().getRootElement()) {
            // Checks if the root element is a TProcess
            if (process.getValue() instanceof TProcess) {
                actualTProcesses++;
            }
        }
        Assert.assertEquals(actualTProcesses, value.getBPMNDiagram().size());
        // Check if BPMN diagram has got the specific a BPMN plane with the right name for the first process
        // If the bpmnElement of the BPMN plane equals the id of the process this is true
        BPMNPlane bpmnPlane = value.getBPMNDiagram().get(0).getBPMNPlane();
        Assert.assertEquals(bpmnPlane.getBpmnElement().getLocalPart(), "PlanDefinition_ANC_Contact");
        // Saves the new TDefinitions for visualization purposes
        repository.save(tDefinitionsJAXBElement, new FileOutputStream("target/test-".concat(testFileName.substring(1))));
    }

    @SneakyThrows
    @Test
    public void testSwimlane() {
        // given
        String testFileName = "/swimlane.bpmn";

        // when
        JAXBElement<TDefinitions> tDefinitionsJAXBElement = repository.load(getClass().getResourceAsStream(testFileName));
        BpmnProcessorData bpmnProcessorData = new BpmnProcessorData(tDefinitionsJAXBElement, horizontalLayout);
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnProcessorData);
        bpmnAutoLayout.execute();

        // then
        Assert.assertNotNull(tDefinitionsJAXBElement);
        TDefinitions value = tDefinitionsJAXBElement.getValue();
        Assert.assertNotNull(value);
        // Check if TDefinition has got diagrams
        Assert.assertNotNull(value.getBPMNDiagram());
        // Get the root BPMN Plane of the BPMN diagram which should be associated with the TCollaboration element
        TCollaboration rootBpmnPlane = null;
        for (JAXBElement<? extends TRootElement> process : tDefinitionsJAXBElement.getValue().getRootElement()) {
            // Gets the TCollaboration root, as we know there is only one we get the right one
            if (process.getValue() instanceof TCollaboration) {
                rootBpmnPlane = (TCollaboration) process.getValue();
            }
        }
        // After that we should also have our root element TCollaboration which represents the BPMN plane then
        Assert.assertNotNull(rootBpmnPlane);
        // Check if BPMN diagram has got the specific a BPMN plane with the right name for the TCollaboration element
        // If the bpmnElement of the BPMN plane equals the id of the collaboration this is true
        BPMNPlane bpmnPlane = value.getBPMNDiagram().get(0).getBPMNPlane();
        Assert.assertEquals(bpmnPlane.getBpmnElement().getLocalPart(), "Collaboration_17xtm2e");
        // Count the number of TLane elements with the right ID and check if we have all TLane elements in the diagram
        // We know the must have the same ID as in the BPMN model as they have to reference correctly
        int expectedTLanes = 3;
        int actualTLanes = 0;
        for (JAXBElement<? extends DiagramElement> diagramElement : bpmnPlane.getDiagramElement()) {
            if (diagramElement.getValue() instanceof BPMNShape) {
                BPMNShape currentShape = (BPMNShape) diagramElement.getValue();
                if (currentShape.getBpmnElement().getLocalPart().equals("Lane_1y9ay9a") ||
                        currentShape.getBpmnElement().getLocalPart().equals("Lane_180etu4") ||
                        currentShape.getBpmnElement().getLocalPart().equals("Lane_003y0sj")) {
                    actualTLanes++;
                }
            }
        }
        Assert.assertEquals(actualTLanes, expectedTLanes);
        // Count the number of TDataObjectReference elements with the right ID and check if we have all TDataObjectReference elements in the diagram
        // We know the must have the same ID as in the BPMN model as they have to reference correctly
        int expectedTDataObjectReferences = 1;
        int actualTDataObjectReferences = 0;
        for (JAXBElement<? extends DiagramElement> diagramElement : bpmnPlane.getDiagramElement()) {
            if (diagramElement.getValue() instanceof BPMNShape) {
                BPMNShape currentShape = (BPMNShape) diagramElement.getValue();
                if (currentShape.getBpmnElement().getLocalPart().equals("DataObjectReference_0okgaod")) {
                    actualTDataObjectReferences++;
                }
            }
        }
        Assert.assertEquals(actualTDataObjectReferences, expectedTDataObjectReferences);
        // Saves the new TDefinitions for visualization purposes
        repository.save(tDefinitionsJAXBElement, new FileOutputStream("target/test-".concat(testFileName.substring(1))));
    }

    @SneakyThrows
    @Test
    public void testMultipleParticipants() {
        // given
        String testFileName = "/multiple-participants.bpmn";

        // when
        JAXBElement<TDefinitions> tDefinitionsJAXBElement = repository.load(getClass().getResourceAsStream(testFileName));
        BpmnProcessorData bpmnProcessorData = new BpmnProcessorData(tDefinitionsJAXBElement, horizontalLayout);
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnProcessorData);
        bpmnAutoLayout.execute();

        // then
       Assert.assertNotNull(tDefinitionsJAXBElement);
        TDefinitions value = tDefinitionsJAXBElement.getValue();
        Assert.assertNotNull(value);
        // Check if TDefinition has got diagrams
        Assert.assertNotNull(value.getBPMNDiagram());
        // Get the root BPMN Plane of the BPMN diagram which should be associated with the TCollaboration element
        TCollaboration rootBpmnPlane = null;
        for (JAXBElement<? extends TRootElement> process : tDefinitionsJAXBElement.getValue().getRootElement()) {
            // Gets the TCollaboration root, as we know there is only one we get the right one
            if (process.getValue() instanceof TCollaboration) {
                rootBpmnPlane = (TCollaboration) process.getValue();
            }
        }
        // After that we should also have our root element TCollaboration which represents the BPMN plane then
        Assert.assertNotNull(rootBpmnPlane);
        // Check if BPMN diagram has got the specific a BPMN plane with the right name for the TCollaboration element
        // If the bpmnElement of the BPMN plane equals the id of the collaboration this is true
        BPMNPlane bpmnPlane = value.getBPMNDiagram().get(0).getBPMNPlane();
        Assert.assertEquals(bpmnPlane.getBpmnElement().getLocalPart(), "Collaboration_060lez2");
        // Count the number of TParticipant elements with the right ID and check if we have all TParticipant elements in the diagram
        // We know the must have the same ID as in the BPMN model as they have to reference correctly
        int expectedTParticipants = 4;
        int actualTParticipants = 0;
        for (JAXBElement<? extends DiagramElement> diagramElement : bpmnPlane.getDiagramElement()) {
            if (diagramElement.getValue() instanceof BPMNShape) {
                BPMNShape currentShape = (BPMNShape) diagramElement.getValue();
                if (currentShape.getBpmnElement().getLocalPart().equals("Participant_1i2ydfg") ||
                        currentShape.getBpmnElement().getLocalPart().equals("Participant_0isdj8m") ||
                        currentShape.getBpmnElement().getLocalPart().equals("Participant_1xoy8vu") ||
                        currentShape.getBpmnElement().getLocalPart().equals("Participant_0nuyvyi")) {
                    actualTParticipants++;
                }
            }
        }
        Assert.assertEquals(actualTParticipants, expectedTParticipants);
        // Saves the new TDefinitions for visualization purposes
        repository.save(tDefinitionsJAXBElement, new FileOutputStream("target/test-".concat(testFileName.substring(1))));
    }


    @SneakyThrows
    @Test
    public void testBlackBoxParticipant() {
        // given
        String testFileName = "/black-box-participant.bpmn";

        // when
        JAXBElement<TDefinitions> tDefinitionsJAXBElement = repository.load(getClass().getResourceAsStream(testFileName));
        BpmnProcessorData bpmnProcessorData = new BpmnProcessorData(tDefinitionsJAXBElement, horizontalLayout);
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnProcessorData);
        bpmnAutoLayout.execute();

        // then
        Assert.assertNotNull(tDefinitionsJAXBElement);
        TDefinitions value = tDefinitionsJAXBElement.getValue();
        Assert.assertNotNull(value);
        // Check if TDefinition has got diagrams
        Assert.assertNotNull(value.getBPMNDiagram());
        TCollaboration rootBpmnPlane = null;
        for (JAXBElement<? extends TRootElement> process : tDefinitionsJAXBElement.getValue().getRootElement()) {
            // Gets the TCollaboration root, as we know there is only one we get the right one
            if (process.getValue() instanceof TCollaboration) {
                rootBpmnPlane = (TCollaboration) process.getValue();
            }
        }
        // After that we should also have our root element TCollaboration which represents the BPMN plane then
        Assert.assertNotNull(rootBpmnPlane);
        // Check if BPMN diagram has got the specific a BPMN plane with the right name for the TCollaboration element
        // If the bpmnElement of the BPMN plane equals the id of the collaboration this is true
        BPMNPlane bpmnPlane = value.getBPMNDiagram().get(0).getBPMNPlane();
        Assert.assertEquals(bpmnPlane.getBpmnElement().getLocalPart(), "sid-54cad3ae-6fa6-4829-a5ef-f4aa3ea715e4");
        // Saves the new TDefinitions for visualization purposes
        repository.save(tDefinitionsJAXBElement, new FileOutputStream("target/test-".concat(testFileName.substring(1))));
    }
}