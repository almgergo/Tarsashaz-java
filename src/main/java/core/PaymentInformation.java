package core;

import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import core.structure.Person;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInformation implements ExcelLoggable {
	private static final int START_CELL = 8;
	double surchargeBefore;
	double surchargeAfter;
	double backlogBefore;
	double backlogAfter;
	double paymentAmountBefore;
	double paymentAmountAfter;
	Date date;

	@Override
	public void printRow(Row row) {
		Cell c;

		int cellNum = START_CELL;

		// c = row.createCell(cellNum++);
		// c.setCellValue("Befizetés");

		c = row.createCell(cellNum++);
		c.setCellValue(date);
		c.setCellStyle(Person.DATE_CELL_STYLE);

		c = row.createCell(cellNum++);
		c.setCellValue(paymentAmountBefore);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		c = row.createCell(cellNum++);
		c.setCellValue(backlogBefore);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		c = row.createCell(cellNum++);
		c.setCellValue(surchargeBefore);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		/*****/
		c = row.createCell(cellNum++);
		c.setCellValue("-->");
		c.setCellStyle(Person.CENTER_CELL_STYLE);
		/*****/
		// cellNum++;
		c = row.createCell(cellNum++);
		c.setCellValue(paymentAmountAfter);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		c = row.createCell(cellNum++);
		c.setCellValue(backlogAfter);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		c = row.createCell(cellNum++);
		c.setCellValue(surchargeAfter);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

	}

	public static void printHeader(Row row) {
		int cellNum = START_CELL;

		row.createCell(cellNum++).setCellValue("Befizetések");

		// row.createCell(cellNum++).setCellValue("Dátum");
		row.createCell(cellNum++).setCellValue("Egyenleg");
		row.createCell(cellNum++).setCellValue("Hátralék");
		row.createCell(cellNum++).setCellValue("Pótdíj");

		row.createCell(cellNum++);
		// row.createCell(cellNum++).setCellValue("Törlesztés");

		row.createCell(cellNum++).setCellValue("Egyenleg");
		row.createCell(cellNum++).setCellValue("Hátralék");
		row.createCell(cellNum++).setCellValue("Pótdíj");
	}
}
