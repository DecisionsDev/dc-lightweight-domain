package com.ibm.rules.domainProvider;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import ilog.rules.brl.syntaxtree.IlrSyntaxTree;
import ilog.rules.brl.value.info.IlrAbstractValueProvider;

/**
 * @author Frederic Mercier
 *
 */
public class LightweightDomainValueProvider extends IlrAbstractValueProvider 
{
	private static final String CLASS_FQN = LightweightDomainValueProvider.class.getCanonicalName();
	private static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);

	private static final ThreadLocal<IlrLightweightDomainValueProvider> latestDomain = new ThreadLocal<IlrLightweightDomainValueProvider>();

	private LightweightDomainValueInfo valueInfo;
	 
	public LightweightDomainValueProvider(LightweightDomainValueInfo valueInfo) {
		this.valueInfo = valueInfo;
	}

	private IlrLightweightDomainValueProvider getDomain() {
		return latestDomain.get();
	}
	
	private void setDomain(IlrLightweightDomainValueProvider domain) {
		latestDomain.set(domain);
	}
	
	/*
	 * @return the array of values
	 */
	@Override
	public Object[] getValues() 
	{	
		LOGGER.finer("getValues()");

		Object[] tab = valueInfo.getResourceMgr().getLabels(getDomain());

		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest(new StringBuilder("getValues()")
							.append(" = ")
							.append(Arrays.toString(tab))
							.toString());
		}
		return tab;
	}
	
	/*
	 * From the syntax tree node, we retrieve the rule (BRL language rule);
	 * and from the rule, we can retrieve the rule properties.
	 * @param node the syntax tree node.
	 */
	@Override
	public void prepare(IlrSyntaxTree.Node node) 
	{
		LOGGER.finest("prepare");

		try {
			IlrLightweightDomainValueProvider domain = valueInfo.getResourceMgr().findBomlessDomain(node, true);
			setDomain(domain);
		} 
		catch (IlrLightweightDomainException e) {
			LOGGER.log(Level.WARNING, e.getParameter() == null ? e.getReason() : e.getReason() + " : " + e.getParameter(), e);
			setDomain(null);
		}
	}

	/*
     * @param element the element
     * @param locale the locale
     * @return the way the element will appear in the proposed list of values.
     * In light-weight domains, elements are not localized.
	 */
	@Override
	public String getDisplayText(Object element, Locale locale) 
	{
		return element.toString();
	}

	/*
     * @param element the element
     * @param locale the locale
     * @return the way the element will appear in the rule definition.
     * In light-weight domains, elements are not localized.
     * Note: It is surrounded with double quotes because our value descriptor
     *       is IlrStringValueDescriptor.
	 */
	@Override
	public String getText(Object element, Locale locale) 
	{
		StringBuilder sb = new StringBuilder("\"").append(element).append("\"");
		return sb.toString();
	}

}
