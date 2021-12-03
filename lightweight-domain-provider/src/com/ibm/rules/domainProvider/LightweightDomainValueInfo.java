/**
 * 
 */
package com.ibm.rules.domainProvider;

import java.util.Locale;
import java.util.logging.Logger;

import ilog.rules.brl.translation.codegen.IlrValueTranslator;
import ilog.rules.brl.value.descriptor.IlrValueDescriptorConstants;
import ilog.rules.brl.value.info.IlrValueChecker;
import ilog.rules.brl.value.info.IlrValueInfo;
import ilog.rules.brl.value.info.IlrValueProvider;

/**
 * @author Frederic Mercier
 *
 */
public class LightweightDomainValueInfo implements IlrValueInfo 
{	
	static final String CLASS_FQN = LightweightDomainValueInfo.class.getCanonicalName();
	static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);
	
	private LightweightDomainValueChecker  valueChecker;
	private LightweightDomainValueProvider valueProvider;
	private LightweightDomainStringValueTranslator valueTranslator;
	private LightweightDomainResourceMgr resourceMgr;

	public LightweightDomainValueInfo() 
	{
		LOGGER.finer("init");
	}
	
	public LightweightDomainResourceMgr getResourceMgr()
	{
		if (null == resourceMgr) {
			resourceMgr = new LightweightDomainResourceMgr();
		}
		return resourceMgr;
	}

	public String getDisplayName(Locale locale) 
	{
		return CLASS_FQN;
	}

	public IlrValueChecker getValueChecker() 
	{
		if (null == valueChecker){
			valueChecker = new LightweightDomainValueChecker(this);
		}
		return valueChecker;
	}

	public String getValueDescriptor() 
	{
		return IlrValueDescriptorConstants.STRING;
	}

	public String getValueEditor() 
	{
		return null;
	}

	public IlrValueProvider getValueProvider() 
	{
		if (null == valueProvider){
			valueProvider = new LightweightDomainValueProvider(this);
		}
		return valueProvider;
	}

	public IlrValueTranslator getValueTranslator(String target) 
	{
		if (null == valueTranslator){
			valueTranslator = new LightweightDomainStringValueTranslator(this);
		}
		return valueTranslator;
	}

}
