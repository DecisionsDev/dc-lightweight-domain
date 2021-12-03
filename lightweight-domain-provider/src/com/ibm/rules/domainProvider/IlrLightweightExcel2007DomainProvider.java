package com.ibm.rules.domainProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class IlrLightweightExcel2007DomainProvider extends IlrLightweightAbstractExcelDomainProvider {

	protected Iterator<Row> getRowIterator(ByteArrayInputStream inputStream, int sheetIndex) throws IOException {
		try (XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
			XSSFSheet sheet = wb.getSheetAt(sheetIndex);
			return sheet.rowIterator();			
		}
	}
}
