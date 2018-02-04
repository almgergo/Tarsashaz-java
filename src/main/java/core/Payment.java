package core;

import java.util.Calendar;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Payment implements HasDate {
	private static final int MAIN_SHEET_START_COL = 6;
	private Calendar date;
	private String payingPerson;
	private String subject;
	private Double amount;

	public void sumMainInfo(XSSFRow row) {
		int cellNum = MAIN_SHEET_START_COL;

		Cell sumCell = null;

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.getDate().getTime());
		sumCell.setCellStyle(Person.DATE_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.payingPerson);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.amount);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

	}

	public static void sumMainInfoHeader(XSSFRow row) {
		Cell headerCell = null;
		int cellNum = MAIN_SHEET_START_COL;

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Dátum");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Befizető");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Összeg");

	}

	@Override
	public Payment clone() {
		Payment p = new Payment();

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date.getTimeInMillis());
		p.setDate(c);

		p.setPayingPerson(payingPerson);
		p.setSubject(subject);
		p.setAmount(amount);

		return p;
	}

	public static void logTitle(XSSFRow row) {
		Cell headerCell = null;
		int cellNum = MAIN_SHEET_START_COL;

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Befizetések");

	}
}
