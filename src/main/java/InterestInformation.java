import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InterestInformation implements ExcelLoggable {

	private static final int START_CELL = 0;
	double interestBefore;
	double interestAfter;
	double surchargeBefore;
	double surchargeAfter;
	double backlog;
	Date date;

	// public static void printEmptyRow(Row row) {
	// Cell c;
	//
	// int rowNum = START_CELL;
	//
	// // c = row.createCell(rowNum++);
	// // c.setCellValue("Pótdíj növekedés");
	//
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// c = row.createCell(rowNum++);
	// c.setCellValue("-");
	// }

	@Override
	public void printRow(Row row) {
		Cell c;

		int rowNum = START_CELL;

		// c = row.createCell(rowNum++);
		// c.setCellValue("Pótdíj növekedés");

		c = row.createCell(rowNum++);
		c.setCellValue(date);
		c.setCellStyle(Person.DATE_CELL_STYLE);

		c = row.createCell(rowNum++);
		c.setCellValue(backlog);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		c = row.createCell(rowNum++);
		c.setCellValue(interestBefore);
		c.setCellStyle(Person.PERCENT_CELL_STYLE);

		c = row.createCell(rowNum++);
		c.setCellValue(surchargeBefore);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		/******/
		c = row.createCell(rowNum++);
		c.setCellValue("-->");
		c.setCellStyle(Person.CENTER_CELL_STYLE);
		/******/

		c = row.createCell(rowNum++);
		c.setCellValue(interestAfter);
		c.setCellStyle(Person.PERCENT_CELL_STYLE);

		c = row.createCell(rowNum++);
		c.setCellValue(surchargeAfter);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

	}

	public static void printHeader(Row row) {
		int cellNum = START_CELL;

		row.createCell(cellNum++).setCellValue("Pótdíj növekedések");

		// row.createCell(cellNum++).setCellValue("Dátum");
		row.createCell(cellNum++).setCellValue("Hátralék");
		row.createCell(cellNum++).setCellValue("Kamat");
		row.createCell(cellNum++).setCellValue("Pótdíj");

		row.createCell(cellNum++);
		// row.createCell(cellNum++).setCellValue("Törlesztés");

		row.createCell(cellNum++).setCellValue("Kamat");
		row.createCell(cellNum++).setCellValue("Pótdíj");
	}

}
