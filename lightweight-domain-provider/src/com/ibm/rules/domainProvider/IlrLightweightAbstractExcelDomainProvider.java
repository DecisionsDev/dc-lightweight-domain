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
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Row;

import ilog.rules.bom.IlrMember;
import ilog.rules.shared.util.IlrExcelHelper;

public abstract class IlrLightweightAbstractExcelDomainProvider extends IlrLightweightDomainResourceProvider {

	static final String CLASS_FQN = IlrLightweightAbstractExcelDomainProvider.class.getCanonicalName();
	static final Logger LOGGER    = Logger.getLogger(CLASS_FQN);

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
		labels = new ArrayList<String>();
		b2xs = new HashMap<String, String>();

		if (hasHeader && rowIte.hasNext()) {
			rowIte.next();
		}
		while (rowIte.hasNext()) {
			Row row = rowIte.next();
			
			String b2x = IlrExcelHelper.GetStringCellValue(row.getCell(colB2X));
			if (b2x.isEmpty()) {
				LOGGER.finest("ignore empty B2X cell");
			    continue;
			}

			String label = IlrExcelHelper.GetStringCellValue(row.getCell(colLabel));
			if (label.isEmpty()) {
				LOGGER.finest("ignore empty Label cell");
			    continue;
			}
			if (!uniqueLabels.add(label)) {
				LOGGER.finer("ignore duplicate Label: " + label);
				continue;
			}

			labels.add(label);
			b2xs.put(label, b2x);
		}
		uniqueLabels.clear();

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
}
