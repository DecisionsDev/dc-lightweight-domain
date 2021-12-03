package com.ibm.rules.domainProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ilog.rules.brl.syntaxtree.IlrSyntaxTree;
import ilog.rules.brl.value.info.IlrAbstractValueProvider;
import ilog.rules.teamserver.model.IlrSession;

/**
 * @author Frederic Mercier
 *
 */
public class LightweightDomainValueProvider extends IlrAbstractValueProvider 
{
	static final String CLASS_FQN = LightweightDomainValueProvider.class.getCanonicalName();
	static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);

	private LightweightDomainValueInfo valueInfo;
	private Map<IlrSession, IlrLightweightDomainValueProvider> domainMap = Collections.synchronizedMap(new HashMap<IlrSession, IlrLightweightDomainValueProvider>());
	 
	public LightweightDomainValueProvider(LightweightDomainValueInfo valueInfo) {
		this.valueInfo = valueInfo;
	}

	private IlrSession getSession() 
	{
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = (requestAttributes == null ? null : requestAttributes.getRequest());
		return request == null ? null : (IlrSession) request.getSession().getAttribute("session");
	}

	private IlrLightweightDomainValueProvider getDomain() 
	{
		IlrSession session = getSession();
		if (session == null) {
			LOGGER.warning("null session");
		}
		return session == null ? null : domainMap.get(session);
	}

	/*
	 * Dispose of the value provider.
	 * remove any allocated resource
	 */
	@Override
	public void dispose() 
	{
		IlrSession session = getSession();
		if (session != null) {
			domainMap.remove(session);		
		}
	}

	/*
	 * @return the array of values
	 */
	@Override
	public Object[] getValues() 
	{	
		Object[] tab = valueInfo.getResourceMgr().getLabels(getDomain());

		if (LOGGER.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder("getValues()");
			if (LOGGER.isLoggable(Level.FINEST)) {
				sb.append(" = ")
				  .append(Arrays.toString(tab));
				LOGGER.finest(sb.toString());
			} else {
				LOGGER.finer(sb.toString());
			}
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
		IlrSession session = getSession();

		try {
			IlrLightweightDomainValueProvider domain = valueInfo.getResourceMgr().findBomlessDomain(node, true);
			if (session != null) {
				domainMap.put(session, domain);		
			}
		} 
		catch (IlrLightweightDomainException e) {
			LOGGER.log(Level.WARNING, e.getParameter() == null ? e.getReason() : e.getReason() + " : " + e.getParameter(), e);
		}
	}

	/*
     * @param element the element
     * @param locale the locale
     * @return the way the element will appear in the proposed list of values.
     * In BOM-less domains, elements are not localized.
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
     * In BOM-less domains, elements are not localized.
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
