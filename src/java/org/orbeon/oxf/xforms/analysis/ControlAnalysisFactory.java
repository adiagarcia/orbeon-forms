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

import org.dom4j.Element;
import org.orbeon.oxf.util.PropertyContext;
import org.orbeon.oxf.xforms.XFormsStaticState;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.analysis.controls.*;
import org.orbeon.oxf.xforms.control.XFormsControlFactory;
import org.orbeon.oxf.xforms.xbl.XBLBindings;
import org.orbeon.saxon.dom4j.DocumentWrapper;

import java.util.Map;

public class ControlAnalysisFactory {

    public static ControlAnalysis create(PropertyContext propertyContext, XFormsStaticState staticState, DocumentWrapper controlsDocumentInfo,
                                           XBLBindings.Scope controlScope, Element controlElement, int index, boolean isContainer,
                                           ContainerAnalysis parentControlAnalysis, Map<String, ControlAnalysis> inScopeVariables) {

        final String controlName = controlElement.getName();
        final String controlURI = controlElement.getNamespaceURI();

        ControlAnalysis controlAnalysis;
        if (controlName.equals("repeat")) {
            // Repeat container
            controlAnalysis = new RepeatAnalysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index,
                    XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        } else if (staticState.getXBLBindings().hasBinding(controlScope.getPrefixedIdForStaticId(XFormsUtils.getElementStaticId(controlElement)))) {
            // Control with XBL binding
            controlAnalysis = new ComponentAnalysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index,
                    XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        } else if (isContainer) {
            // Other container
            controlAnalysis = new ContainerAnalysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index,
                    XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        } else if (controlName.equals("select") || controlName.equals("select1")) {
            controlAnalysis = new Select1Analysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index,
                    XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        } else if ("variable".equals(controlName)) {
            controlAnalysis = new VariableAnalysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index,
                    XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        } else if ("attribute".equals(controlName)) {
            controlAnalysis = new AttributeAnalysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index,
                    XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        } else {
            // Default
            controlAnalysis = new ControlAnalysis(propertyContext, staticState, controlsDocumentInfo, controlScope,
                    controlElement, index, XFormsControlFactory.isValueControl(controlURI, controlName), parentControlAnalysis, inScopeVariables);
        }
        return controlAnalysis;
    }
}
