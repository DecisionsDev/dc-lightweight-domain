/*
 *
 *   Copyright IBM Corp. 2022
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.ibm.rules.domainProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

import ilog.rules.brl.syntaxtree.IlrSyntaxTree.Node;
import ilog.rules.brl.translation.codegen.IlrAbstractValueTranslator;
import ilog.rules.vocabulary.model.IlrConcept;
import ilog.rules.vocabulary.model.IlrVocabulary;

/**
 * @author Frederic Mercier
 *
 */
public class LightweightDomainStringValueTranslator extends IlrAbstractValueTranslator
{
	private static final String CLASS_FQN = LightweightDomainStringValueTranslator.class.getCanonicalName();
	private static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);

	private LightweightDomainValueInfo valueInfo;
	
	LightweightDomainStringValueTranslator (LightweightDomainValueInfo valueInfo) {
		this.valueInfo = valueInfo;
	}
	
	public boolean allowValueWrapping() {
		return true;
	}

	/*
	 * 	returns a string like 
	 * 		"\"XYZ\""
	 */
	@Override
	public String translateValue(String value, IlrConcept concept, Node node, IlrVocabulary vocabulary) {
		IlrLightweightDomainValueProvider domain;
		try {
			domain = valueInfo.getResourceMgr().findBomlessDomain(node, true);
			String b2x = valueInfo.getResourceMgr().getB2x(value, domain);
			String s = new StringBuilder("\"")
					.append(b2x.replaceAll("\"","\\\\\""))
					.append("\"")
					.toString();
			if (LOGGER.isLoggable(Level.FINER)) {	
				LOGGER.finer(new StringBuilder(value).append(" -> ").append(s).toString());
			}
			return s;
		} 
		catch (IlrLightweightDomainException e) {
			LOGGER.log(Level.SEVERE, e.getReason() + " : " + e.getParameter(), e);
		}
		return null;
	}
}
