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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ilog.rules.bom.IlrClass;
import ilog.rules.bom.IlrMember;
import ilog.rules.brl.syntaxtree.IlrSyntaxTree;
import ilog.rules.brl.util.IlrPropertyManager;
import ilog.rules.brl.util.IlrPropertyManager.Property;
import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrBrmPackage;
import ilog.rules.teamserver.brm.IlrPackageKind;
import ilog.rules.teamserver.brm.IlrResource;
import ilog.rules.teamserver.brm.IlrRulePackage;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrElementSummary;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelperEx;
import ilog.rules.teamserver.model.permissions.IlrRoleRestrictedPermissionException;
import ilog.rules.vocabulary.model.IlrConcept;
import ilog.rules.vocabulary.model.IlrVocabulary;
import ilog.rules.vocabulary.model.bom.IlrBOMVocabulary;

public class LightweightDomainResourceMgr {

	static final String CLASS_FQN = LightweightDomainResourceMgr.class.getCanonicalName();
	static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);
	
	static long PERIOD_CHECK_IF_MODIFIED              =  600L;	// seconds
	static long PERIOD_CHECK_IF_MODIFIED_WHEN_EDITING =   60L;	// seconds
	static long MIN_TIME_IN_CACHE                     =   60L;	// seconds
	
	static final String PROP_DOMAIN = "domain";
	static final int INITIAL_MAX_CACHE_SIZE = 5;

	@SuppressWarnings("serial")
	static Map<String, IlrLightweightDomainValueProvider> cache_resources = Collections.synchronizedMap(new LinkedHashMap<String,IlrLightweightDomainValueProvider>(INITIAL_MAX_CACHE_SIZE) {
		int limit = INITIAL_MAX_CACHE_SIZE;
		
	    @SuppressWarnings("rawtypes")
		@Override
	    protected boolean removeEldestEntry(Map.Entry eldest) {
			if (((IlrLightweightDomainResourceProvider) eldest.getValue()).olderThan(MIN_TIME_IN_CACHE) == false && size() > limit) {
				// if we are about to remove a domain that was inserted less than MIN_TIME_IN_CACHE in the cache, then expand the cache
				limit++;
			}
			return size() > limit;
	    }		
	});

	@SuppressWarnings("serial")
	static Map<String, DomainWrapper> cache_members = Collections.synchronizedMap(new LinkedHashMap<String, DomainWrapper>(INITIAL_MAX_CACHE_SIZE) {
		int limit = INITIAL_MAX_CACHE_SIZE;
		
		@SuppressWarnings("rawtypes")
		@Override
		protected boolean removeEldestEntry(Map.Entry eldest) {
			if (((DomainWrapper) eldest.getValue()).olderThan(MIN_TIME_IN_CACHE) == false && size() > limit) {
				// if we are about to remove a domain that was inserted less than MIN_TIME_IN_CACHE in the cache, then expand the cache
				limit++;
			}
			return size() > limit;
		}		
	});

	String id;
	String projectAndBranch;

	class DomainWrapper {
		IlrLightweightDomainValueProvider domain;
		private LocalTime refTime;
		private LocalTime creationTime;
		
		public DomainWrapper (IlrLightweightDomainValueProvider domain) {
			this.domain = domain;
			creationTime = LocalTime.now();
			reset();
		}
		public void reset () {
			refTime = LocalTime.now();
		}
		public boolean recent (long periodInSeconds) {
			return LocalTime.now().isBefore(refTime.plusSeconds(periodInSeconds));
		}
		public boolean olderThan(long durationInSeconds) {
			return LocalTime.now().isAfter(creationTime.plusSeconds(durationInSeconds));			
		}
	}
	
	public LightweightDomainResourceMgr() {
		String id = this.toString();
		int i = id.lastIndexOf('@');
		this.id = id.substring(i);
		LOGGER.fine("init " + this.id);
	}
	
	public boolean existLabel(String label, IlrLightweightDomainValueProvider domain) {
		return (domain == null ? null : domain.existLabel(label));
	}

	public String getB2x(String value, IlrLightweightDomainValueProvider domain) throws IlrLightweightDomainException {

		if (null == domain) {
			throw new IlrLightweightDomainException("null domain", "cannot translate " + value);
		}
		if (null == value) {
			throw new IlrLightweightDomainException("cannot translate null value", null);
		}

		return domain.getBOM2XOMMapping(value.replaceAll("\\\\\"","\""));
	}

	public Object[] getLabels(IlrLightweightDomainValueProvider domain) {	
		return (domain == null ? null : domain.getLabels());
	}
	
	public IlrResource getResource (IlrLightweightDomainValueProvider domain) {
		return (domain == null ? null : ((IlrLightweightDomainResourceProvider) domain).getResource());
	}
	public String getResourceFQN (IlrLightweightDomainValueProvider domain) {
		IlrResource resource = getResource(domain);
		if (resource == null)
			return null;
		
		StringBuilder sb = new StringBuilder(resource.getName()).append(resource.getExtension());
		IlrRulePackage rp = null;
		try {
			rp = resource.getRulePackage();
		} catch (IlrObjectNotFoundException e1) {
		}
		while (rp != null) {
			sb.insert(0, "/").insert(0, rp.getName());
			try {
				rp = rp.getParent();
			} catch (IlrObjectNotFoundException e) {
				break;
			}
		}
		return sb.toString();
	}
	
	public IlrLightweightDomainValueProvider findBomlessDomain(IlrSyntaxTree.Node node, boolean reload) throws IlrLightweightDomainException 
	{
		if (null == node) {
			throw new IlrLightweightDomainException("unexpected null node", null);
		}

		IlrSyntaxTree st = node.getSyntaxTree();
		IlrBOMVocabulary bomvoc = null;
		if (st.getVocabulary() instanceof IlrBOMVocabulary) {
			bomvoc = (IlrBOMVocabulary) st.getVocabulary();
		}

		if (LOGGER.isLoggable(Level.FINEST)) { 
			LOGGER.finest(examine (node.getSuperNode(), "findBomlessDomain (" + node.getNodePath() + ")\n", 0, bomvoc));
		}

		IlrMember member = findMember(node);
		if (null == member)
			throw new IlrLightweightDomainException("bomless domain not found", "check the 'valueInfo' custom parameter");
		
		IlrLightweightDomainValueProvider domain = getDomain(member, reload);
		LOGGER.finer("domain resource file = " + getResourceFQN(domain));
		return domain;
	}

	public IlrLightweightDomainValueProvider findBomlessDomain(IlrSyntaxTree.Node node) throws IlrLightweightDomainException 
	{
		return findBomlessDomain(node, false);
	}
	
	private IlrMember findMember(IlrSyntaxTree.Node node) 
    {
		IlrMember member;
		IlrSyntaxTree.Node sibling, child;
		
		while (node != null) 
		{
			IlrSyntaxTree.Iterator previousSibling = new IlrSyntaxTree.Iterator(node, IlrSyntaxTree.LEFT);
			IlrSyntaxTree.Iterator     nextSibling = new IlrSyntaxTree.Iterator(node, IlrSyntaxTree.RIGHT);
			
			while ((sibling = previousSibling.nextNode()) != null) 
			{
				IlrSyntaxTree.Iterator siblingAndChildren = new IlrSyntaxTree.Iterator(sibling, IlrSyntaxTree.DOWN);
				while ((child = siblingAndChildren.nextNode()) != null) {
					if ((member = getMember(child)) != null) 
						return member;
				}
			}
			
			while ((sibling = nextSibling.nextNode()) != null) 
			{
				IlrSyntaxTree.Iterator siblingAndChildren = new IlrSyntaxTree.Iterator(sibling, IlrSyntaxTree.DOWN);
				while ((child = siblingAndChildren.nextNode()) != null) {
					if ((member = getMember(child)) != null) 
						return member;
				}
			}
			
			node = node.getSuperNode();
		}
		return null;
    }

	private IlrMember getMember(IlrSyntaxTree.Node node)
	{		
		if (LOGGER.isLoggable(Level.FINEST)) {
			StringBuilder s = new StringBuilder("getMember(").append(node.getNodePath()).append(") = ");
			s.append(node.getName().replaceAll("\r|\n", ""));
			if (node.hasContents()) {
				s.append(" = ");
				s.append(node.getContents().replaceAll("\r|\n", ""));
			}
			LOGGER.finest(s.toString());
		}

		if (!node.getType().equals("T-voc-element-fqn"))
			return null;

		String sTerm = node.getContents();
		if (null == sTerm)
			return null;

		int idx1 = sTerm.indexOf('/');
		if (idx1 <= 0)
			return null;
		
		String sClassName = sTerm.substring(0, idx1);

		int idx2 = sTerm.indexOf('/', idx1+1);
		if (idx2 == -1 || idx2 == idx1 + 1)
			return null;
		
		String sMemberName = sTerm.substring(idx1+1, idx2);
		
		IlrVocabulary voc = node.getSyntaxTree().getVocabulary();
		IlrConcept concept = voc.getConcept(sClassName);
		if (null == concept)
			return null;
		
		IlrClass  bomClass = ((IlrBOMVocabulary) voc).getClass(concept);
		IlrMember member;
		
		if (null != (member = bomClass.getAttribute(sMemberName)) && hasExpectedProps(member) ||
		    null != (member = bomClass.getMethod(sMemberName))    && hasExpectedProps(member)) 
		{
			if (LOGGER.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder("getMember => ").append(member.getFullyQualifiedName());
				LOGGER.finer(sb.toString());
			}
			return member;
		}
		return null;
	}

	private boolean hasExpectedProps (IlrMember member) {
		return LightweightDomainValueInfo.KEY.equals((String) member.getPropertyValue("valueInfo"));
	}

	private IlrLightweightDomainValueProvider getDomain (IlrMember member, boolean editing) throws IlrLightweightDomainException
	{
		IlrLightweightDomainValueProvider domain;
		String valueProviderName;
		String providerClassName;
		boolean bNewProperties = false;

		String resourceName = (String) member.getPropertyValue(IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_RESOURCE);	
		if (resourceName != null)
		{
			bNewProperties = true;

			if (resourceName.trim().isEmpty()) {
				throw getException (member, "empty custom property", IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_RESOURCE);
			}
			
			valueProviderName = (String) member.getPropertyValue(IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_FORMAT);
			if (valueProviderName == null)
			{// guess the format from the extension of the resource file
				int i = resourceName.lastIndexOf('.');
				if (i > 0) {					
					String extension = resourceName.substring(i+1).toLowerCase();
					     if ("xls". equals(extension)) valueProviderName = IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_EXCEL_2003;
					else if ("xlsx".equals(extension)) valueProviderName = IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_EXCEL_2007;
				}
			}
			if (valueProviderName == null || valueProviderName.equals(IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_EXCEL_2007))
			{
				providerClassName = IlrLightweightExcel2007DomainProvider.class.getCanonicalName();
			}
			else if (valueProviderName.equals(IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_EXCEL_2003))
			{
				providerClassName = IlrLightweightExcelDomainProvider.class.getCanonicalName();
			}
			else {
				throw getException (member, "Invalid value for custom property", IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_FORMAT);
			}
		}
		else
		{
			resourceName = (String) member.getPropertyValue(IlrLightweightAbstractExcelDomainProvider.DOMAIN_PROVIDER_RESOURCE);		
			if (resourceName == null) {
				// if neither LIGHTWEIGHT_RESOURCE nor DOMAIN_PROVIDER_RESOURCE are defined, report that the preferred one LIGHTWEIGHT_RESOURCE is missing
				throw getException (member, "missing custom property", IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_RESOURCE);

			}
			if (resourceName.trim().isEmpty()) {
				throw getException (member, "empty custom property", IlrLightweightAbstractExcelDomainProvider.DOMAIN_PROVIDER_RESOURCE);
			}
			valueProviderName = (String) member.getPropertyValue(IlrLightweightAbstractExcelDomainProvider.DOMAIN_PROVIDER);
			if (valueProviderName == null) {
				throw getException (member, "missing custom property", IlrLightweightAbstractExcelDomainProvider.DOMAIN_PROVIDER);
			}
			if (!Arrays.asList(IlrLightweightAbstractExcelDomainProvider.EXCEL_PROVIDERS).contains(valueProviderName)) {
				throw getException (member, "Invalid value for custom property", IlrLightweightAbstractExcelDomainProvider.DOMAIN_PROVIDER);
			}
			providerClassName = (valueProviderName.equals(IlrLightweightAbstractExcelDomainProvider.LIGHTWEIGHT_EXCEL_2007) ||
									    valueProviderName.equals(IlrLightweightAbstractExcelDomainProvider.EXCEL_2007_PROVIDER)) ?
									    		IlrLightweightExcel2007DomainProvider.class.getCanonicalName() :
									    		IlrLightweightExcelDomainProvider.class.getCanonicalName();
		}

		IlrSession session = getSession();
		DomainWrapper domainWrapper = (DomainWrapper) cache_members.get(member.getFullyQualifiedName() + id);
		if (domainWrapper != null) {
			if (domainWrapper.recent(editing ? PERIOD_CHECK_IF_MODIFIED_WHEN_EDITING : PERIOD_CHECK_IF_MODIFIED) || session == null) {
				// for performance reason, return the domain if it was inserted recently (to avoid the resource lookup)
				return domainWrapper.domain;
			}
		}
		else if (session == null) {
			throw new IlrLightweightDomainException("null session", null);
		}

		IlrResource resource = findResource(resourceName, session);

		if (projectAndBranch == null) {
			try {
				IlrRuleProject project = resource.getProject();
				IlrBaseline branch = session.getWorkingBaseline();
				projectAndBranch = new StringBuilder(project.getName()).append('(').append(branch.getName()).append(')').toString();
			} catch (IlrObjectNotFoundException e) {}
		}

		String uuid = resource.getUuid();
		if ((domain = (IlrLightweightDomainValueProvider) cache_resources.get(uuid + id)) != null) {
			/*
			 *   have we already checked recently if the content of the resource was modified ?
			 */
			if (((IlrLightweightDomainResourceProvider) domain).recentlyCheckedIfModified (editing ? PERIOD_CHECK_IF_MODIFIED_WHEN_EDITING : PERIOD_CHECK_IF_MODIFIED))
				return domain;
			
			
			/*
			 *   check if the resource file is unchanged
			 */
			if (((IlrLightweightDomainResourceProvider) domain).sameContent (resource))
			{
				LOGGER.finer( new StringBuilder(projectAndBranch).append(": resource file '").append(resourceName).append("' has not changed").toString() );
				
				if (domainWrapper != null) {
					domainWrapper.reset();
				} else {
					cache_members.put(member.getFullyQualifiedName() + id, this.new DomainWrapper(domain));
				}
				
				return domain;
			}
			LOGGER.fine( new StringBuilder(projectAndBranch).append(": resource file '").append(resourceName).append("' has changed and needs to be reloaded").toString() );
		}

        try {
            Class<?> clazz = Class.forName(providerClassName);
            domain = (IlrLightweightDomainValueProvider) clazz.newInstance();
        	((IlrLightweightDomainResourceProvider) domain).setResource(resource);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
            throw new IlrLightweightDomainException(e, providerClassName);
        }
        
        // read the Excel file
        Collection<String> labels = domain.getLabels(member, bNewProperties);

        // set the 2 caches
		cache_resources.put(uuid + id, domain);
		cache_members.put(member.getFullyQualifiedName() + id, this.new DomainWrapper(domain));
		
		if (LOGGER.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder(projectAndBranch)
				.append(": resource file '")
				.append(resourceName)
				.append("' loaded size=")
				.append(labels.size())
				.append(", cache(")
				.append(cache_resources.size())
				.append(")=")
				.append(cache_resources.keySet().toString());
				LOGGER.fine(sb.toString());
		}
		return domain;
	}

	private IlrLightweightDomainException getException (IlrMember member, String msg, String property) {
		return new IlrLightweightDomainException(new StringBuilder(msg)
				.append(" '")
				.append(property)
				.append("' in ")
				.append(member.getFullyQualifiedName())
				.toString(),
				"Please fix the BOM");
	}
	
	private IlrResource findResource (String resourceName, IlrSession session) throws IlrLightweightDomainException
	{		
		IlrBrmPackage brm = session.getBrmPackage();

		// split the full path name
		StringTokenizer st = new StringTokenizer(resourceName.trim(), "/");
		ArrayList<String> list = new ArrayList<>();
		while (st.hasMoreTokens()) {
			list.add(st.nextToken());
		}
		String[] s = (String[]) list.toArray(new String[0]);

		// name only
		String nameAndExtension = s[s.length - 1];

		// look for the handle of the package containing the resource
		IlrElementSummary rulePackage = null;
		if (s.length > 1) 
		{ 	
			// path only
			String[] s2 = new String[s.length - 1];
			System.arraycopy(s, 0, s2, 0, s2.length);

			try {
				rulePackage = IlrSessionHelperEx.getHierarchyFromPath(session, session.getWorkingBaseline(), brm.getRulePackage(), s2, true, null, IlrPackageKind.RESOURCE_LITERAL).get(0);
			} catch (IlrObjectNotFoundException e) {
				throw new IlrLightweightDomainException(e, "cannot find the resource file '" + resourceName + "'");
			}
		}

		// look for the resource
		int indexOf = nameAndExtension.length();
		while (indexOf != -1)
		{
			indexOf = nameAndExtension.lastIndexOf(".", indexOf - 1);
			String name      = indexOf == -1 ? nameAndExtension : nameAndExtension.substring(0, indexOf);
			String extension = indexOf == -1 ? ""               : nameAndExtension.substring(indexOf);
			
			IlrDefaultSearchCriteria search = new IlrDefaultSearchCriteria(brm.getResource(),
													Arrays.asList(new Object[] { brm.getPackageElement_RulePackage(), brm.getModelElement_Name(), brm.getResource_Extension() }), 
													Arrays.asList(new Object[] { rulePackage,                         name,                       extension }));
			try {
				List<IlrElementDetails> packageElements = session.findElementDetails(search);
				if (packageElements.size() == 1)
					return (IlrResource) packageElements.get(0);
				
			} catch (IlrRoleRestrictedPermissionException | IlrObjectNotFoundException e) {
				throw new IlrLightweightDomainException(e, "cannot find the resource file '" + resourceName + "'");
			}
		}
		
		throw new IlrLightweightDomainException("cannot find the resource file '" + resourceName + "'", "");
	}
	
	private IlrSession getSession() {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = (requestAttributes == null ? null : requestAttributes.getRequest());
		return request == null ? null : (IlrSession) request.getSession().getAttribute("session");
	}

	private void examine(IlrSyntaxTree.Node node, String sOffset, int depth, IlrBOMVocabulary voc, StringBuilder s) {
		for (int i=0; i<depth; i++)
			s.append("  ");
		s.append(node.getType());
		s.append(": ");
		s.append(node.getName().replaceAll("\r|\n", ""));
		if (node.hasContents()) {
			s.append(" = ");
			s.append(node.getContents().replaceAll("\r|\n", ""));
		}

		java.util.Iterator<?> itProp = node.properties();
		if (itProp.hasNext()) {
			IlrPropertyManager.Property prop = (Property) itProp.next();
			if (! "scope".equals(prop.getName())) {
				s.append(", ");
				s.append(prop.getName());
				s.append(" = ");
				s.append(prop.getValue().toString());
			}
		}
		
		s.append('\n');
		
		IlrSyntaxTree.Iterator it = node.iterator(IlrSyntaxTree.SUBNODES);
		while(it.hasNext()) {
			examine((IlrSyntaxTree.Node) it.next(), sOffset, depth + 1, voc, s);
		}
	}
	private String examine(IlrSyntaxTree.Node node, String sOffset, int depth, IlrBOMVocabulary voc) {
		StringBuilder s = new StringBuilder(sOffset);
		examine(node, sOffset, depth, voc, s);
		return s.toString();
	}

}
