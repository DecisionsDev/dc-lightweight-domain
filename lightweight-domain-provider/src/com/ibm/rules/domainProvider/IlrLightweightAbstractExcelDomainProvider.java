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

	// custom properties
	public static String DOMAIN_PROVIDER = "domainValueProviderName";
	public static String DOMAIN_PROVIDER_RESOURCE = "domainProviderResource";
	public static String DOMAIN_XL_LABEL_COL   = DOMAIN_PROVIDER_XL + ".labelColumn";
	public static String DOMAIN_XL_B2X_COL 	   = DOMAIN_PROVIDER_XL + ".b2xColumn";
	public static String DOMAIN_XL_HAS_HEADER  = DOMAIN_PROVIDER_XL + ".hasHeader";
	public static String DOMAIN_XL_SHEET_INDEX = DOMAIN_PROVIDER_XL + ".sheetIndex";

	// provider names (class names)
	public static String EXCEL_2003_PROVIDER = "com.ibm.rules.domainProvider.msexcel2003";
	public static String EXCEL_2007_PROVIDER = "com.ibm.rules.domainProvider.msexcel2007";
	public static String LIGHTWEIGHT_EXCEL_2003_PROVIDER = "com.ibm.rules.lightweightdomainProvider.msexcel2003";
	public static String LIGHTWEIGHT_EXCEL_2007_PROVIDER = "com.ibm.rules.lightweightdomainProvider.msexcel2007";
	public static String EXCEL_PROVIDERS[] = {EXCEL_2003_PROVIDER, LIGHTWEIGHT_EXCEL_2003_PROVIDER,
											  EXCEL_2007_PROVIDER, LIGHTWEIGHT_EXCEL_2007_PROVIDER};

	private String[] labelsTab;
	private ArrayList<String> labels;
	private HashMap<String, String> b2xs;
	
	@Override
	public Collection<String> getLabels(IlrMember member) throws IlrLightweightDomainException {

		int colLabel = getIntPropertyValue(member, DOMAIN_XL_LABEL_COL,  false);
		int colB2X   = getIntPropertyValue(member, DOMAIN_XL_B2X_COL, 	  true);
		int sheet    = getIntPropertyValue(member, DOMAIN_XL_SHEET_INDEX, true);
		boolean hasHeader = getBooleanPropertyValue(member, DOMAIN_XL_HAS_HEADER);
		
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
	
	private int getIntPropertyValue (IlrMember member, String property, boolean exactMatch) throws IlrLightweightDomainException {
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
			throw new IlrLightweightDomainException("missing or empty custom property", property);
		}
		try {
			return Integer.parseInt(sValue);
		} catch (NumberFormatException e) {
			throw new IlrLightweightDomainException(e, property);			
		}
	}
	
	private boolean getBooleanPropertyValue (IlrMember member, String property) throws IlrLightweightDomainException {
		String sValue = (String) member.getPropertyValue(property, null);
		if (null == sValue) {
			throw new IlrLightweightDomainException("missing or empty custom property", property);
		}
		return Boolean.parseBoolean(sValue);
	}
}
