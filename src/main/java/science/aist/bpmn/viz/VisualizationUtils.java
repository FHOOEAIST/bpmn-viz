/*
 * Copyright (c) 2020 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package science.aist.bpmn.viz;

import org.omg.spec.bpmn.model.ObjectFactory;

import java.util.UUID;

/**
 * <p>Visualizations utils which are used by the BPMN layouting process.</p>
 *
 * @author Clemens Toegel
 */
public class VisualizationUtils {

    private VisualizationUtils() {}

    // Object factory to create BPMN diagram elements
    public static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    /**
     * Method to generate element ID if not present
     * We really must have ids for
     * - TSequenceFlow
     * - TArtifacts
     * - TDataObjectReference
     * to be able to generate stuff
     *
     * @param element represents type of element
     */
    public static String generateRandomId(String element) {
        return element.concat("-").concat(UUID.randomUUID().toString());
    }

}
