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
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class IlrLightweightExcel2007DomainProvider extends IlrLightweightAbstractExcelDomainProvider {

	protected Iterator<Row> getRowIterator(ByteArrayInputStream inputStream, int sheetIndex) throws IOException {
		XSSFWorkbook wb = null;
		try {
			wb = new XSSFWorkbook(inputStream);
			XSSFSheet sheet = wb.getSheetAt(sheetIndex);
			return sheet.rowIterator();			
		}
		finally {
			try {
				wb.close();
			} catch (Throwable e) {
				// ignore noSuchMethodError that may happen with older versions of the POI libraries because the close method is not implemented 
			}
		}
	}
}
