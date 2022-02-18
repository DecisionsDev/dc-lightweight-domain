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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Row;

import ilog.rules.bom.IlrMember;
import ilog.rules.shared.util.IlrExcelHelper;

public abstract class IlrLightweightAbstractExcelDomainProvider extends IlrLightweightDomainResourceProvider {

	private static final String CLASS_FQN = IlrLightweightAbstractExcelDomainProvider.class.getCanonicalName();
	private static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);
	
	private static Pattern pattern = Pattern.compile("return[ \t]+\"(.*)\"[ \t]*;");

	public static String DOMAIN_PROVIDER_XL = "com.ibm.rules.domainProvider.msexcel";

	/*
	 *  custom properties
	 *  
	 *  	either use:
	 *  	1) the dynamic domain Excel provider properties
	 *  	2) or the new ones
	 *  
	 *  	In the latter case, some properties are optional
	 */
	
	// 1) dynamic domain custom properties
	public static String DOMAIN_PROVIDER = "domainValueProviderName";
	public static String DOMAIN_PROVIDER_RESOURCE = "domainProviderResource";
	public static String DOMAIN_XL_LABEL_COL   = DOMAIN_PROVIDER_XL + ".labelColumn";
	public static String DOMAIN_XL_B2X_COL 	   = DOMAIN_PROVIDER_XL + ".b2xColumn";
	public static String DOMAIN_XL_HAS_HEADER  = DOMAIN_PROVIDER_XL + ".hasHeader";
	public static String DOMAIN_XL_SHEET_INDEX = DOMAIN_PROVIDER_XL + ".sheetIndex";

	// 2.1) mandatory new custom properties
	public static String LIGHTWEIGHT_RESOURCE = "resource";
	
	// 2.2) optional  new custom properties
	public static String LIGHTWEIGHT_FORMAT      = "format";
	public static String LIGHTWEIGHT_LABEL_COL   = "labelColumn";
	public static String LIGHTWEIGHT_B2X_COL     = "b2xColumn";
	public static String LIGHTWEIGHT_HAS_HEADER  = "hasHeader";
	public static String LIGHTWEIGHT_SHEET_INDEX = "sheetIndex";
	
	// expected values
	public static String EXCEL_2003_PROVIDER = "com.ibm.rules.domainProvider.msexcel2003";
	public static String EXCEL_2007_PROVIDER = "com.ibm.rules.domainProvider.msexcel2007";
	public static String LIGHTWEIGHT_EXCEL_2003 = "msexcel2003";
	public static String LIGHTWEIGHT_EXCEL_2007 = "msexcel2007";
	public static String LIGHTWEIGHT_EXCEL_PROVIDERS[] = {LIGHTWEIGHT_EXCEL_2003, LIGHTWEIGHT_EXCEL_2007};
	public static String EXCEL_PROVIDERS[] = {EXCEL_2003_PROVIDER, LIGHTWEIGHT_EXCEL_2003,
											  EXCEL_2007_PROVIDER, LIGHTWEIGHT_EXCEL_2007};

	// default values
	public static int COL_0 = 0;
	public static int COL_1 = 1;
	public static int SHEET_0 = 0;
	public static boolean HEADER_PRESENT = true;
	
	private String[] labelsTab;
	private ArrayList<String> labels;
	private HashMap<String, String> b2xs;
	
	@Override
	public Collection<String> getLabels(IlrMember member, boolean bNewProperties) throws IlrLightweightDomainException {

		int colLabel;
		int colB2X;
		int sheet;
		boolean hasHeader;
		
		if (bNewProperties)
		{
			colLabel  = getIntPropertyValue    (member, LIGHTWEIGHT_LABEL_COL,   true, COL_0);
			colB2X    = getIntPropertyValue    (member, LIGHTWEIGHT_B2X_COL,     true, COL_1);
			sheet     = getIntPropertyValue    (member, LIGHTWEIGHT_SHEET_INDEX, true, SHEET_0);
			hasHeader = getBooleanPropertyValue(member, LIGHTWEIGHT_HAS_HEADER,        HEADER_PRESENT);
		}
		else
		{			
			colLabel  = getIntPropertyValue    (member, DOMAIN_XL_LABEL_COL,  false);
			colB2X    = getIntPropertyValue    (member, DOMAIN_XL_B2X_COL,     true);
			sheet     = getIntPropertyValue    (member, DOMAIN_XL_SHEET_INDEX, true);
			hasHeader = getBooleanPropertyValue(member, DOMAIN_XL_HAS_HEADER);
		}
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(getData());
		Iterator<Row> rowIte;		
		try {
			rowIte = getRowIterator(inputStream, sheet);
		} catch (IOException e) {
			throw new IlrLightweightDomainException(e, "unexpected error reading Excel file");
		}
		
		Set<String> uniqueLabels = new LinkedHashSet<String>();
		Set<String> uniqueB2xs   = new LinkedHashSet<String>();
		labels = new ArrayList<String>();
		b2xs = new HashMap<String, String>();

		if (hasHeader && rowIte.hasNext()) {
			rowIte.next();
		}
		while (rowIte.hasNext()) {
			Row row = rowIte.next();
			
			String b2x = IlrExcelHelper.GetStringCellValue(row.getCell(colB2X));
			if (b2x.isEmpty()) {
				report(Level.INFO, "skip empty B2X cell");
			    continue;
			}
			if (! bNewProperties)
			{
				/*
				 *   when dealing with dynamic domain resouce file, the BOM2XOM cells contain values such as:
				 *   
				 *   	return "XYZ";
				 *   
				 *   we want to extract from the example below:
				 *   
				 *   	XYZ
				 */
				b2x = extract(b2x);
			}
			if (!uniqueB2xs.add(b2x)) {
				report(Level.WARNING, "skip duplicate value in B2X cell", b2x);
				continue;
			}

			String label = IlrExcelHelper.GetStringCellValue(row.getCell(colLabel));
			if (label.isEmpty()) {
				report(Level.INFO, "skip empty Label cell");
			    continue;
			}
			if (!uniqueLabels.add(label)) {
				report(Level.WARNING, "skip value in Label cell", label);
				continue;
			}

			labels.add(label);
			b2xs.put(label, b2x);
		}
		uniqueLabels.clear();
		uniqueB2xs.clear();

		AtomicInteger idx = new AtomicInteger(0);
		labelsTab = new String[labels.size()];		
		labels.stream().forEach(s -> labelsTab[idx.getAndIncrement()] = s);
		
		return labels;
	}

	public boolean existLabel(String label) {
		return labels.contains(label);
	}

	@Override
	public String[] getLabels() {
		return labelsTab;
	}

	@Override
	public String getBOM2XOMMapping(String label) {
		return (String) b2xs.get(label);
	}

	@Override
	public void dispose() {
		setData(null);
	}

	protected abstract Iterator<Row> getRowIterator(ByteArrayInputStream inputStream, int sheetIndex) throws IOException;
	
	private int getIntPropertyValue (IlrMember member, String property, boolean exactMatch, boolean mandatory, int defaultValue) throws IlrLightweightDomainException {
		String sValue = (String) member.getPropertyValue(property, null);
		if (null == sValue && !exactMatch) {
			Iterator<?> it = member.propertyNames();
			while (it.hasNext()) {
				String propertyName = (String)it.next();
				if (propertyName.startsWith(property)) {
					sValue = (String) member.getPropertyValue(propertyName, null);
					if (sValue != null) {
						property = propertyName;
						break;
					}
				}
			}
		}
		if (null == sValue) {
			if (mandatory) {
				throw new IlrLightweightDomainException("missing or empty custom property", property);
			} else {
				return defaultValue;
			}
		}
		try {
			return Integer.parseInt(sValue);
		} catch (NumberFormatException e) {
			throw new IlrLightweightDomainException(e, property);			
		}
	}

	private int getIntPropertyValue (IlrMember member, String property, boolean exactMatch, int defaultValue) throws IlrLightweightDomainException {
		return getIntPropertyValue (member, property, exactMatch, false, defaultValue);
	}

	private int getIntPropertyValue (IlrMember member, String property, boolean exactMatch) throws IlrLightweightDomainException {
		return getIntPropertyValue (member, property, exactMatch, true, -1);
	}
	
	private boolean getBooleanPropertyValue (IlrMember member, String property, boolean mandatory, boolean defaultValue) throws IlrLightweightDomainException {
		String sValue = (String) member.getPropertyValue(property, null);
		if (null == sValue) {
			if (mandatory) {
				throw new IlrLightweightDomainException("missing or empty custom property", property);
			} else {
				return defaultValue;
			}
		}
		return Boolean.parseBoolean(sValue);
	}

	private boolean getBooleanPropertyValue (IlrMember member, String property, boolean defaultValue) throws IlrLightweightDomainException {
		return getBooleanPropertyValue (member, property, false, defaultValue);
	}

	private boolean getBooleanPropertyValue (IlrMember member, String property) throws IlrLightweightDomainException {
		return getBooleanPropertyValue (member, property, true, false);
	}

	/*
	 *   input:   return "XYZ";
	 *   output:  XYZ
	 */
	private String extract (String b2xWithReturn) {
		Matcher matcher = pattern.matcher(b2xWithReturn);
		return matcher.find() ? matcher.group(1) : b2xWithReturn;
	}
	
	private void report (Level level, String msg, String value) {
		StringBuilder b = new StringBuilder(msg);
		if (value != null) {
			b.append(" (")
			 .append(value)
			 .append(")");
		}
		b.append(" in ")
		.append(getResource().getName())
	    .append(getResource().getExtension());
		
		LOGGER.log(level, b.toString());
	}
	private void report (Level level, String msg) {
		report(level, msg, null);
	}
}
