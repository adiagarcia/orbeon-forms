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
package org.orbeon.oxf.xforms.analysis.controls;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.Text;
import org.orbeon.oxf.util.PropertyContext;
import org.orbeon.oxf.util.XPathCache;
import org.orbeon.oxf.xforms.XFormsConstants;
import org.orbeon.oxf.xforms.XFormsProperties;
import org.orbeon.oxf.xforms.XFormsStaticState;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.analysis.XPathAnalysis;
import org.orbeon.oxf.xforms.xbl.XBLBindings;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import org.orbeon.oxf.xml.dom4j.ExtendedLocationData;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.orbeon.saxon.dom4j.DocumentWrapper;
import org.orbeon.saxon.om.NodeInfo;

import java.util.*;

/**
 * Hold the static analysis for an XForms control.
 */
public class ControlAnalysis extends SimpleAnalysis {

    public final int index;
    
    private final LHHAAnalysis nestedLabel;
    private final LHHAAnalysis nestedHelp;
    private final LHHAAnalysis nestedHint;
    private final LHHAAnalysis nestedAlert;

    private LHHAAnalysis externalLabel;
    private LHHAAnalysis externalHelp;
    private LHHAAnalysis externalHint;
    private LHHAAnalysis externalAlert;

    private String classes;

    public class LHHAAnalysis extends SimpleAnalysis {
        public final Element element;
        public final boolean isLocal;
        public final boolean hasStaticValue;

        public final XPathAnalysis valueAnalysis;

        public LHHAAnalysis(PropertyContext propertyContext, XFormsStaticState staticState, XBLBindings.Scope scope,
                            Map<String, ControlAnalysis> inScopeVariables,
                            DocumentWrapper controlsDocumentInfo, Element element, boolean isLocal) {
            super(staticState, scope, element, ControlAnalysis.this, inScopeVariables, true);
            this.element = element;
            this.isLocal = isLocal;

            this.hasStaticValue = hasStaticValue(propertyContext, controlsDocumentInfo);
            // TODO: model change

            if (!this.hasStaticValue && staticState.isXPathAnalysis()) {
                final String lhhaPrefixedId = XFormsUtils.getRelatedEffectiveId(prefixedId, XFormsUtils.getElementStaticId(element));
                if (element.attribute("value") != null || element.attribute("ref") != null) {
                    // 1. E.g. <xforms:label model="..." context="..." value|ref="..."/>
                    final XPathAnalysis bindingAnalysis = computeBindingAnalysis(element);
                    this.valueAnalysis = analyzeValueXPath(bindingAnalysis, element, lhhaPrefixedId);
                } else {
                    // 2. E.g. <xforms:label>...<xforms:output value|ref=""/>...</xforms:label>

                    // TODO: handle model/context on enclosing LHHA element

                    final Set<XPathAnalysis> analyses = new LinkedHashSet<XPathAnalysis>();

                    final boolean hostLanguageAVTs = XFormsProperties.isHostLanguageAVTs();
                    Dom4jUtils.visitSubtree(element, new Dom4jUtils.VisitorListener() {
                        public void startElement(Element element) {
                            if (element.getQName().equals(XFormsConstants.XFORMS_OUTPUT_QNAME)) {
                                // Add dependencies
                                final XPathAnalysis bindingAnalysis = computeBindingAnalysis(element);
                                analyses.add(analyzeValueXPath(bindingAnalysis, element, lhhaPrefixedId));
                            } else {
                                final List<Attribute> attributes = element.attributes();
                                if (attributes.size() > 0) {
                                    for (final Attribute currentAttribute: attributes) {
                                        final String currentAttributeValue = currentAttribute.getValue();

                                        if (hostLanguageAVTs && currentAttributeValue.indexOf('{') != -1) {
                                            // TODO: check for AVTs
                                            analyses.add(null);
                                        }
                                    }
                                }
                            }
                        }

                        public void endElement(Element element) {}
                        public void text(Text text) {}
                    });

                    if (analyses.size() > 0) {
                        final Iterator<XPathAnalysis> i = analyses.iterator();
                        XPathAnalysis combinedAnalysis = i.next();
                        if (combinedAnalysis != null && combinedAnalysis.figuredOutDependencies) {
                            while (i.hasNext()) {
                                final XPathAnalysis nextAnalysis = i.next();
                                if (nextAnalysis != null && nextAnalysis.figuredOutDependencies) {
                                    combinedAnalysis.combine(nextAnalysis);
                                } else {
                                    combinedAnalysis = null;
                                    break;
                                }
                            }

                            this.valueAnalysis = combinedAnalysis;
                        } else {
                            this.valueAnalysis = null;
                        }

                    } else {
                        this.valueAnalysis = null;
                    }
                }
            } else {
                // Value of LHHA is 100% static
                this.valueAnalysis = null;
                // TODO: get static value, and figure out whether to allow HTML or not (could default to true?)
//                XFormsUtils.getStaticChildElementValue(element, true, null);
            }
        }

        @Override
        protected XPathAnalysis computeValueAnalysis() {
            return super.computeValueAnalysis();
        }

        @Override
        protected XPathAnalysis computeBindingAnalysis(Element element) {
            return super.computeBindingAnalysis(element);
        }

        private boolean hasStaticValue(PropertyContext propertyContext, DocumentWrapper controlsDocumentInfo) {

            // Gather itemset information
            final NodeInfo lhhaNodeInfo = controlsDocumentInfo.wrap(element);

            // Try to figure out if we have a dynamic LHHA element. This attempts to cover all cases, including nested
            // xforms:output controls. Also check for AVTs ion @class and @style.
            return (Boolean) XPathCache.evaluateSingle(propertyContext, lhhaNodeInfo,
                    "exists(descendant-or-self::xforms:*[@ref or @nodeset or @bind or @value or (@class, @style)[contains(., '{')]])",
                    XFormsStaticState.BASIC_NAMESPACE_MAPPINGS, null, null, null, null, getLocationData());
        }

        public LocationData getLocationData() {
            return new ExtendedLocationData((LocationData) element.getData(), "gathering static control information", element);
        }
    }

    public ControlAnalysis(PropertyContext propertyContext, XFormsStaticState staticState, DocumentWrapper controlsDocumentInfo,
                           XBLBindings.Scope scope, Element element, int index, boolean isValueControl,
                           ContainerAnalysis parentControlAnalysis, Map<String, ControlAnalysis> inScopeVariables) {

        super(staticState, scope, element, parentControlAnalysis, inScopeVariables, isValueControl);

        this.index = index;

        this.nestedLabel = findNestedLHHA(propertyContext, controlsDocumentInfo, XFormsConstants.LABEL_QNAME);
        this.nestedHelp = findNestedLHHA(propertyContext, controlsDocumentInfo, XFormsConstants.HELP_QNAME);
        this.nestedHint = findNestedLHHA(propertyContext, controlsDocumentInfo, XFormsConstants.HINT_QNAME);
        this.nestedAlert = findNestedLHHA(propertyContext, controlsDocumentInfo, XFormsConstants.ALERT_QNAME);
    }

    private LHHAAnalysis findNestedLHHA(PropertyContext propertyContext, DocumentWrapper controlsDocumentInfo, QName qName) {
        final Element e = findNestedLHHAElement(propertyContext, controlsDocumentInfo, qName);
        return (e != null) ? new LHHAAnalysis(propertyContext, staticState, scope, inScopeVariables, controlsDocumentInfo, e, true) : null;
    }

    protected Element findNestedLHHAElement(PropertyContext propertyContext, DocumentWrapper controlsDocumentInfo, QName qName) {
        return element.element(qName);
    }

    public void setExternalLHHA(PropertyContext propertyContext, DocumentWrapper controlsDocumentInfo, Element lhhaElement) {
        assert lhhaElement != null;
        final String name = lhhaElement.getName();
        if (XFormsConstants.LABEL_QNAME.getName().equals(name)) {
            externalLabel = new LHHAAnalysis(propertyContext, staticState, scope, inScopeVariables, controlsDocumentInfo, lhhaElement, false);
        } else if (XFormsConstants.HELP_QNAME.getName().equals(name)) {
            externalHelp = new LHHAAnalysis(propertyContext, staticState, scope, inScopeVariables, controlsDocumentInfo, lhhaElement, false);
        } else if (XFormsConstants.HINT_QNAME.getName().equals(name)) {
            externalHint = new LHHAAnalysis(propertyContext, staticState, scope, inScopeVariables, controlsDocumentInfo, lhhaElement, false);
        } else if (XFormsConstants.ALERT_QNAME.getName().equals(name)) {
            externalAlert = new LHHAAnalysis(propertyContext, staticState, scope, inScopeVariables, controlsDocumentInfo, lhhaElement, false);
        }
    }

    public LHHAAnalysis getLabel() {
        return (nestedLabel != null) ? nestedLabel : externalLabel;
    }

    public LHHAAnalysis getHelp() {
        return (nestedHelp != null) ? nestedHelp : externalHelp;
    }

    public LHHAAnalysis getHint() {
        return (nestedHint != null) ? nestedHint : externalHint;
    }

    public LHHAAnalysis getAlert() {
        return (nestedAlert != null) ? nestedAlert : externalAlert;
    }

    @Override
    protected XPathAnalysis computeValueAnalysis() {
        if (element != null && hasValue && !element.getQName().equals(XFormsConstants.XXFORMS_ATTRIBUTE_QNAME)) {
            return super.computeValueAnalysis();
        } else {
            return null;
        }
    }

    public void addClasses(String classes) {
        if (this.classes == null) {
            // Set
            this.classes = classes;
        } else {
            // Append
            this.classes = this.classes + ' ' + classes;
        }
    }

    public String getClasses() {
        return classes;
    }

    public RepeatAnalysis getAncestorRepeat() {
        SimpleAnalysis currentParent = parentControlAnalysis;
        while (currentParent != null) {
            if (currentParent instanceof RepeatAnalysis)
                return (RepeatAnalysis) currentParent;
            currentParent = currentParent.parentControlAnalysis;
        }
        return null;
    }
}