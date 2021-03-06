/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.analysis;

import org.dom4j.Document;
import org.junit.Test;
import org.orbeon.oxf.common.Version;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorUtils;
import org.orbeon.oxf.processor.test.TestExternalContext;
import org.orbeon.oxf.test.ResourceManagerTestBase;
import org.orbeon.oxf.xforms.XFormsStaticState;
import org.orbeon.oxf.xforms.analysis.model.Model;
import org.orbeon.oxf.xml.SAXStore;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.dom4j.LocationDocumentResult;

import javax.xml.transform.sax.TransformerHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

public class XFormsStaticStateTest extends ResourceManagerTestBase {

    @Test
    public void testLHHAAnalysis() {
//        if (Version.instance().isPE()) { // only test this feature if we are the PE version
//            final XFormsStaticState staticState = getStaticState("oxf:/org/orbeon/oxf/xforms/analysis/lhha.xml");
//            final Map<String, String> namespaces = new HashMap<String, String>();
//            namespaces.put("", "");
//
//            // Hold the current list of changes
//            final Set<String> currentChanges = new HashSet<String>();
//
//            final PathMapUIDependencies dependencies = new PathMapUIDependencies(staticState.getIndentedLogger(), staticState) {
//                @Override
//                protected Set<String> getModifiedPaths() {
//                    return currentChanges;
//                }
//            };
//
//            staticState.dumpAnalysis();
//        }
    }

    @Test
    public void testBindAnalysis() {
        if (Version.isPE()) { // only test this feature if we are the PE version
            final XFormsStaticState staticState = getStaticState("oxf:/org/orbeon/oxf/xforms/analysis/binds.xml");

            staticState.dumpAnalysis();

            // TODO: test computedBindExpressionsInstances and validationBindInstances
            {
                final Model model1 = staticState.getModel("model1");
                assertTrue(model1.figuredBindAnalysis);

                assertTrue(model1.bindInstances.contains("instance11"));
                assertTrue(model1.bindInstances.contains("instance12"));
                assertTrue(model1.bindInstances.contains("instance13"));
            }
            {
                final Model model2 = staticState.getModel("model2");
                assertTrue(model2.figuredBindAnalysis);

                assertFalse(model2.bindInstances.contains("instance21"));
            }
            {
                final Model model3 = staticState.getModel("model3");
                assertTrue(model3.figuredBindAnalysis);

                assertFalse(model3.bindInstances.contains("instance31"));
                assertTrue(model3.bindInstances.contains("instance32"));
            }
            {
                final Model model4 = staticState.getModel("model4");
                assertTrue(model4.figuredBindAnalysis);

                assertTrue(model4.bindInstances.contains("instance41"));
                assertFalse(model4.bindInstances.contains("instance42"));
            }
            {
                final Model model5 = staticState.getModel("model5");
                assertTrue(model5.figuredBindAnalysis);

                assertTrue(model5.validationBindInstances.contains("instance51"));
                assertFalse(model5.computedBindExpressionsInstances.contains("instance51"));

                assertFalse(model5.validationBindInstances.contains("instance52"));
                assertTrue(model5.computedBindExpressionsInstances.contains("instance52"));
            }
        }
    }

    @Test
    public void testXPathAnalysis() {
        if (Version.isPE()) { // only test this feature if we are the PE version
            final XFormsStaticState staticState = getStaticState("oxf:/org/orbeon/oxf/xforms/analysis/form.xml");
            final Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.put("", "");

            // Hold the current list of changes
            final Set<String> currentChanges = new HashSet<String>();

            final PathMapXPathDependencies dependencies = new PathMapXPathDependencies(staticState.getIndentedLogger(), staticState) {
                @Override
                protected Set<String> getModifiedPaths() {
                    return currentChanges;
                }
            };

            staticState.dumpAnalysis();

            // == Value change to default ==================================================================================
            currentChanges.clear();
            currentChanges.add(XPathAnalysis.getInternalPath(namespaces, "instance('default')/a"));

            assertFalse(dependencies.requireBindingUpdate("trigger1"));
            assertFalse(dependencies.requireBindingUpdate("trigger2"));

            assertFalse(dependencies.requireBindingUpdate("select1"));
            assertTrue(dependencies.requireValueUpdate("select1"));

            assertTrue(dependencies.requireBindingUpdate("group2"));
    //        assertTrue(dependencies.requireValueUpdate("group2"));

            assertTrue(dependencies.requireBindingUpdate("select2"));
            assertTrue(dependencies.requireValueUpdate("select2"));


            assertFalse(dependencies.requireBindingUpdate("group3"));
    //        assertFalse(dependencies.requireValueUpdate("group3"));

            assertFalse(dependencies.requireBindingUpdate("select3"));
            assertFalse(dependencies.requireValueUpdate("select3"));

            assertFalse(dependencies.requireBindingUpdate("group4"));
    //        assertFalse(dependencies.requireValueUpdate("group4"));

            assertFalse(dependencies.requireBindingUpdate("select4"));
            assertFalse(dependencies.requireValueUpdate("select4"));

            dependencies.refreshDone();

            // == Value change to default ==================================================================================
            currentChanges.clear();
            currentChanges.add(XPathAnalysis.getInternalPath(namespaces, "instance('default')/b"));

            assertFalse(dependencies.requireBindingUpdate("trigger1"));
            assertFalse(dependencies.requireBindingUpdate("trigger2"));

            assertFalse(dependencies.requireBindingUpdate("select1"));
            assertFalse(dependencies.requireValueUpdate("select1"));

            assertFalse(dependencies.requireBindingUpdate("group2"));
    //        assertFalse(dependencies.requireValueUpdate("group2"));

            assertFalse(dependencies.requireBindingUpdate("select2"));
            assertTrue(dependencies.requireValueUpdate("select2"));


            assertFalse(dependencies.requireBindingUpdate("group3"));
    //        assertFalse(dependencies.requireValueUpdate("group3"));

            assertFalse(dependencies.requireBindingUpdate("select3"));
            assertFalse(dependencies.requireValueUpdate("select3"));

            assertFalse(dependencies.requireBindingUpdate("group4"));
    //        assertFalse(dependencies.requireValueUpdate("group4"));

            assertFalse(dependencies.requireBindingUpdate("select4"));
            assertFalse(dependencies.requireValueUpdate("select4"));

            dependencies.refreshDone();

            // == Value change to instance2 ================================================================================
            currentChanges.clear();
            currentChanges.add(XPathAnalysis.getInternalPath(namespaces, "instance('instance2')/a"));

            assertFalse(dependencies.requireBindingUpdate("trigger1"));
            assertFalse(dependencies.requireBindingUpdate("trigger2"));

            assertFalse(dependencies.requireBindingUpdate("select1"));
            assertFalse(dependencies.requireValueUpdate("select1"));

            assertFalse(dependencies.requireBindingUpdate("group2"));
    //        assertFalse(dependencies.requireValueUpdate("group2"));

            assertFalse(dependencies.requireBindingUpdate("select2"));
            assertFalse(dependencies.requireValueUpdate("select2"));


            assertFalse(dependencies.requireBindingUpdate("group3"));
    //        assertFalse(dependencies.requireValueUpdate("group3"));

            assertFalse(dependencies.requireBindingUpdate("select3"));
            assertTrue(dependencies.requireValueUpdate("select3"));

            assertTrue(dependencies.requireBindingUpdate("group4"));
    //        assertFalse(dependencies.requireValueUpdate("group4"));

            assertTrue(dependencies.requireBindingUpdate("select4"));
            assertTrue(dependencies.requireValueUpdate("select4"));

            dependencies.refreshDone();

            // == Value change to instance2 ================================================================================
            currentChanges.clear();
            currentChanges.add(XPathAnalysis.getInternalPath(namespaces, "instance('instance2')/b"));

            assertFalse(dependencies.requireBindingUpdate("trigger1"));
            assertFalse(dependencies.requireBindingUpdate("trigger2"));

            assertFalse(dependencies.requireBindingUpdate("select1"));
            assertFalse(dependencies.requireValueUpdate("select1"));

            assertFalse(dependencies.requireBindingUpdate("group2"));
    //        assertFalse(dependencies.requireValueUpdate("group2"));

            assertFalse(dependencies.requireBindingUpdate("select2"));
            assertFalse(dependencies.requireValueUpdate("select2"));


            assertFalse(dependencies.requireBindingUpdate("group3"));
    //        assertFalse(dependencies.requireValueUpdate("group3"));

            assertFalse(dependencies.requireBindingUpdate("select3"));
            assertFalse(dependencies.requireValueUpdate("select3"));

            assertFalse(dependencies.requireBindingUpdate("group4"));
    //        assertFalse(dependencies.requireValueUpdate("group4"));

            assertFalse(dependencies.requireBindingUpdate("select4"));
            assertTrue(dependencies.requireValueUpdate("select4"));

            dependencies.refreshDone();

            // == Structural change to model1 ==============================================================================
            currentChanges.clear();
            dependencies.markStructuralChange("model1");

            assertTrue(dependencies.requireBindingUpdate("trigger1"));
            assertFalse(dependencies.requireBindingUpdate("trigger2"));

            assertTrue(dependencies.requireBindingUpdate("select1"));
            assertTrue(dependencies.requireValueUpdate("select1"));

            assertTrue(dependencies.requireBindingUpdate("group2"));
    //        assertTrue(dependencies.requireValueUpdate("group2"));

            assertTrue(dependencies.requireBindingUpdate("select2"));
            assertTrue(dependencies.requireValueUpdate("select2"));


            assertFalse(dependencies.requireBindingUpdate("group3"));
    //        assertFalse(dependencies.requireValueUpdate("group3"));

            assertFalse(dependencies.requireBindingUpdate("select3"));
            assertFalse(dependencies.requireValueUpdate("select3"));

            assertFalse(dependencies.requireBindingUpdate("group4"));
    //        assertFalse(dependencies.requireValueUpdate("group4"));

            assertFalse(dependencies.requireBindingUpdate("select4"));
            assertFalse(dependencies.requireValueUpdate("select4"));

            dependencies.refreshDone();

            // == Structural change to model2 ==============================================================================
            currentChanges.clear();
            dependencies.markStructuralChange("model2");

            assertFalse(dependencies.requireBindingUpdate("trigger1"));
            assertTrue(dependencies.requireBindingUpdate("trigger2"));

            assertFalse(dependencies.requireBindingUpdate("select1"));
            assertFalse(dependencies.requireValueUpdate("select1"));

            assertFalse(dependencies.requireBindingUpdate("group2"));
    //        assertFalse(dependencies.requireValueUpdate("group2"));

            assertFalse(dependencies.requireBindingUpdate("select2"));
            assertFalse(dependencies.requireValueUpdate("select2"));


            assertTrue(dependencies.requireBindingUpdate("group3"));
    //        assertTrue(dependencies.requireValueUpdate("group3"));

            assertTrue(dependencies.requireBindingUpdate("select3"));
            assertTrue(dependencies.requireValueUpdate("select3"));

            assertTrue(dependencies.requireBindingUpdate("group4"));
    //        assertTrue(dependencies.requireValueUpdate("group4"));

            assertTrue(dependencies.requireBindingUpdate("select4"));
            assertTrue(dependencies.requireValueUpdate("select4"));

            dependencies.refreshDone();
        }
    }

    @Test
    public void testVariables() {
        if (Version.isPE()) { // only test this feature if we are the PE version
            final XFormsStaticState staticState = getStaticState("oxf:/org/orbeon/oxf/xforms/analysis/variables.xml");
            final Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.put("", "");

            // Hold the current list of changes
            final Set<String> currentChanges = new HashSet<String>();

            final PathMapXPathDependencies dependencies = new PathMapXPathDependencies(staticState.getIndentedLogger(), staticState) {
                @Override
                protected Set<String> getModifiedPaths() {
                    return currentChanges;
                }
            };

            staticState.dumpAnalysis();

            // == Value change to default ==================================================================================
            currentChanges.clear();
            currentChanges.add(XPathAnalysis.getInternalPath(namespaces, "instance('default')/value"));

            assertFalse(dependencies.requireBindingUpdate("values"));
            assertTrue(dependencies.requireValueUpdate("values"));

            assertFalse(dependencies.requireBindingUpdate("repeat"));

            assertFalse(dependencies.requireBindingUpdate("value"));
            assertTrue(dependencies.requireValueUpdate("value"));

            assertFalse(dependencies.requireBindingUpdate("input"));
            assertTrue(dependencies.requireValueUpdate("input"));
        }
    }

    /**
     * Return an analyzed static state for the given XForms document URL.
     *
     * @param documentURL   URL to read and analyze
     * @return              static state
     */
    public static XFormsStaticState getStaticState(String documentURL) {
        final PipelineContext pipelineContext = new PipelineContext();
        final Document requestDocument = ProcessorUtils.createDocumentFromURL("oxf:/org/orbeon/oxf/xforms/analysis/request.xml", null);
        final ExternalContext externalContext = new TestExternalContext(pipelineContext, requestDocument);


        final TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();

        final LocationDocumentResult documentResult = new LocationDocumentResult();
        identity.setResult(documentResult);

        final XFormsAnnotatorContentHandler.Metadata metadata = new XFormsAnnotatorContentHandler.Metadata();
        final SAXStore annotatedSAXStore = new SAXStore(new XFormsExtractorContentHandler(externalContext, identity, metadata));

        // Read the input through the annotator and gather namespace mappings
        XMLUtils.urlToSAX(documentURL, new XFormsAnnotatorContentHandler(annotatedSAXStore, externalContext, metadata), false, false);

        // Get static state document and create static state object
        final Document staticStateDocument = documentResult.getDocument();
        final XFormsStaticState staticState = new XFormsStaticState(pipelineContext, staticStateDocument, metadata, annotatedSAXStore);
        staticState.analyzeIfNecessary(pipelineContext);
        return staticState;
    }
}
