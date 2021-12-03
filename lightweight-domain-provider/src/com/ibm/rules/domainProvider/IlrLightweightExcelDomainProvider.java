package com.ibm.rules.domainProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

public class IlrLightweightExcelDomainProvider extends IlrLightweightAbstractExcelDomainProvider {

	protected Iterator<Row> getRowIterator(ByteArrayInputStream inputStream, int sheetIndex) throws IOException {
		try (HSSFWorkbook wb = new HSSFWorkbook(inputStream)) {
			HSSFSheet sheet = wb.getSheetAt(sheetIndex);
			return sheet.rowIterator();
		}
	}
}
