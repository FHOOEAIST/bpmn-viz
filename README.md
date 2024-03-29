# BPMN Visualization

[![javadoc](https://javadoc.io/badge2/science.aist/bpmn-viz/javadoc.svg)](https://javadoc.io/doc/science.aist/bpmn-viz) 
[![Maven Central](https://img.shields.io/maven-central/v/science.aist/bpmn-viz.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22science.aist%22%20a:%22bpmn-viz%22)
[![GitHub release](https://img.shields.io/github/v/release/fhooeaist/bpmn-viz.svg)](https://github.com/fhooeaist/bpmn-viz/releases)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![DOI](https://zenodo.org/badge/301400051.svg)](https://zenodo.org/badge/latestdoi/301400051)

BPMN visualization provides a functionality to generate the diagramm interchange information for a given
BPMN model. 

## Getting Started

To use the bpmn visualization, simply include the maven dependency on the project.

```xml
<dependency>
    <groupId>science.aist</groupId>
    <artifactId>bpmn-viz</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope> <!-- Note: this is default -->
</dependency>
```

Then you are able to load generate the diagramm interchange information the following way:

```java
// Create the repository to load the definition elements
XMLRepository<TDefinitions> repository = new BPMNTDefinitionsRepository();
// Load the root element
JAXBElement<TDefinitions> tDefinitionsJAXBElement = repository.load(...);
// Create the processing data
BpmnProcessorData bpmnProcessorData = new BpmnProcessorData(tDefinitionsJAXBElement, horizontalLayout);
// create the auto layouter
BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnProcessorData);
// run the auto layouting
bpmnAutoLayout.execute();
// save the element back again (note: the org. element is manipulated)
repository.save(tDefinitionsJAXBElement, ...);
``` 

### Building the BPMN visualization yourself

If you want to build the project yourself, just checkout this git repo, make sure you have jdk 11 or above installed as
well as maven 3.6.0 or above and build the project by running the maven command: `mvn package`. This results in a 
jar-file inside the target folder, which can be used as a dependency in other projects.

## FAQ

If you have any questions, please checkout our [FAQ](https://fhooeaist.github.io/bpmn-viz/faq.html) section.

## Contributing

**First make sure to read our [general contribution guidelines](https://fhooeaist.github.io/CONTRIBUTING.html).**
   
## Licence

Copyright (c) 2020 the original author or authors.
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES.

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

The following code is under different licence and copyright: 

| Licence | Filepaths |
|-|-|
| **Apache 2.0**<br>see LICENCE_APACHE_2_0 | src/main/java/science/aist/bpmn/viz/BpmnAutoLayout.java |
| **jgraph mxgraph**<br>see LICENSE_JGRAPH_MXGRAPH | dependency: com.mxgraph:mxgraph-all:3.7.4 |

## Research

If you are going to use this project as part of a research paper, we would ask you to reference this project by citing
it. [![DOI](https://zenodo.org/badge/301400051.svg)](https://zenodo.org/badge/latestdoi/301400051)
