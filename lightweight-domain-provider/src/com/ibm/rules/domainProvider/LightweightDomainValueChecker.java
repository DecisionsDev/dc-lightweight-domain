/**
 * 
 */
package com.ibm.rules.domainProvider;

import ilog.rules.brl.syntaxtree.IlrSyntaxTree.Node;
import ilog.rules.brl.value.info.IlrValueChecker;
import ilog.rules.brl.value.info.IlrValueError;
import ilog.rules.teamserver.brm.IlrResource;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ilog.rules.brl.IlrBRLMarkerConstants;

/**
 * @author Frederic Mercier
 *
 */
public class LightweightDomainValueChecker implements IlrValueChecker 
{
	static final String CLASS_FQN = LightweightDomainValueChecker.class.getCanonicalName();
	static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);

	private LightweightDomainValueInfo valueInfo;
	
	public LightweightDomainValueChecker(LightweightDomainValueInfo valueInfo) {
		this.valueInfo = valueInfo;
	}

	/* (non-Javadoc)
	 * @see ilog.rules.brl.value.info.IlrValueChecker#check(java.lang.Object, ilog.rules.brl.syntaxtree.IlrSyntaxTree.Node, ilog.rules.brl.value.info.IlrValueError)
	 */
	@Override
	public boolean check(Object value, Node node, IlrValueError valueError) 
	{
		if (LOGGER.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder("check: ").append(value);
			LOGGER.finer(sb.toString());
		}
		
		IlrLightweightDomainValueProvider domain;
		try {
			domain = valueInfo.getResourceMgr().findBomlessDomain(node);
		} 
		catch (IlrLightweightDomainException e) 
		{
			LOGGER.log(Level.WARNING, e.getReason() + " : " + e.getParameter(), e);
			valueError.setReason(e.getParameter() == null ? e.getReason() : 	
															e.getReason() + " : " + e.getParameter());
			valueError.setSeverity(IlrBRLMarkerConstants.SEVERITY_ERROR);
			return false;
		}
		
		if (!valueInfo.getResourceMgr().existLabel(value.toString(), domain)) 
		{
			IlrResource resource = valueInfo.getResourceMgr().getResource(domain);
			String resourceFileName = resource.getName() + resource.getExtension();
			String resourceProjectName;
			try {
				resourceProjectName = resource.getProject().getName();
			} catch (IlrObjectNotFoundException e) {
				resourceProjectName = "<not found>";
			}
			
			valueError.setReason("it does not exist in domain file '" + resourceFileName + "' in project '" + resourceProjectName + "'");
			valueError.setSeverity(IlrBRLMarkerConstants.SEVERITY_ERROR);
			return false;
		}
		
		return true;
	}

}
