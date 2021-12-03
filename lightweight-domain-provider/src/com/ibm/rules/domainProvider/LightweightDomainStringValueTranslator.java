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
	static final String CLASS_FQN = LightweightDomainStringValueTranslator.class.getCanonicalName();
	static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);

	private LightweightDomainValueInfo valueInfo;
	
	LightweightDomainStringValueTranslator (LightweightDomainValueInfo valueInfo) {
		this.valueInfo = valueInfo;
	}
	
	public boolean allowValueWrapping() {
		return true;
	}

	/*
	 * 	returns a string like 
	 * 		"return \"XYZ\";"
	 */
	@Override
	public String translateValue(String value, IlrConcept concept, Node node, IlrVocabulary vocabulary) {
		IlrLightweightDomainValueProvider domain;
		try {
			domain = valueInfo.getResourceMgr().findBomlessDomain(node);
			String b2x = valueInfo.getResourceMgr().getB2x(value, domain);
			String s = "\"" + (null == b2x ? "" : b2x.replaceAll("\"","\\\\\"")) + "\"";
			if (LOGGER.isLoggable(Level.FINER)) {	
				LOGGER.finer("translateValue (" + value + ") = " + s);
			}
			return s;
		} 
		catch (IlrLightweightDomainException e) {
			LOGGER.log(Level.SEVERE, e.getReason() + " : " + e.getParameter(), e);
		}
		return null;
	}
}
