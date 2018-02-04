package core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	public static CellStyle DATE_CELL_STYLE = null;
	public static CellStyle CURRENCY_CELL_STYLE = null;
	public static CellStyle CENTER_CELL_STYLE = null;
	public static CellStyle PERCENT_CELL_STYLE = null;

	private String identifier;
	private String name;
	private Double startBalance;
	private Double existingBacklog;
	private Double paymentTotal;
	private Double balance;

	List<Backlog> backlogs = new LinkedList<>();
	List<Payment> payments = new LinkedList<>();

	public void processPerson() {
		List<Backlog> unpaidBacklogs = this.backlogs;
		List<Payment> remainingPayments = this.payments;

		XSSFWorkbook workbook = new XSSFWorkbook();

		int rowNum = 0;
		// System.out.println("Creating excel");

		processBacklogs(unpaidBacklogs, remainingPayments);

		createCellStyles(workbook);
		logBacklogs(unpaidBacklogs, workbook);

		printWorkbookToFile(workbook);
	}

	private void printWorkbookToFile(XSSFWorkbook workbook) {
		try {
			File theDir = new File("eredmenyek");

			if (!theDir.exists()) {
				boolean result = false;

				try {
					theDir.mkdir();
					result = true;
				} catch (SecurityException se) {
					// handle it
				}
			}

			FileOutputStream outputStream = new FileOutputStream("eredmenyek/" + this.name.trim().replace("/", ", ")
					+ " " + this.identifier.trim().replace("/", ", ") + ".xlsx");
			workbook.write(outputStream);
			workbook.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void logBacklogs(List<Backlog> unpaidBacklogs, XSSFWorkbook workbook) {
		double unpaidReqSum = 0.0;
		double surchargeSum = 0.0;

		int mainRowId = 0;
		XSSFSheet mainSheet = workbook.createSheet("Összegzés");

		mainRowId = personHeader(mainRowId, mainSheet);
		logMainSheetHeader(mainSheet.createRow(mainRowId++));

		for (Backlog backlog : unpaidBacklogs) {
			if (backlog.getOriginalRequiredPayment() > 0) {

				Row mainRow = mainSheet.createRow(mainRowId++);
				unpaidReqSum += backlog.getRequiredPayment();
				surchargeSum += backlog.getSurcharge();

				backlog.sumMainInfo(mainRow);

				// System.out.println("LOGGING BACKLOG: " + backlog.getDate().getTime());

				XSSFSheet sheet = workbook.createSheet(
						backlog.getDate().get(Calendar.YEAR) + "." + (backlog.getDate().get(Calendar.MONTH) + 1) + "."
								+ backlog.getDate().get(Calendar.DAY_OF_MONTH));

				int rowNum = 0;
				rowNum = backlog.createHeader(sheet, rowNum);

				if (backlog.getLoggables().size() > 0) {
					rowNum++;
					Row infoHeaderRow = sheet.createRow(rowNum++);
					InterestInformation.printHeader(infoHeaderRow);
					PaymentInformation.printHeader(infoHeaderRow);

					backlog.logToExcel(sheet);
				}

				for (int i = 0; i < 200; i++) {
					sheet.autoSizeColumn(i);
				}

			}

		}

		mainRowId = logSumData(mainSheet, mainRowId + 2, unpaidReqSum, surchargeSum);

		for (int i = 0; i < 200; i++) {
			mainSheet.autoSizeColumn(i);
		}
	}

	private int personHeader(int mainRowId, XSSFSheet mainSheet) {
		Row row = mainSheet.createRow(mainRowId++);
		int cellId = 0;
		row.createCell(cellId++).setCellValue(this.name);

		cellId++;
		row.createCell(cellId++).setCellValue("Nyitó egyenleg");

		Cell c = row.createCell(cellId++);
		c.setCellValue(this.startBalance);
		c.setCellStyle(Person.CURRENCY_CELL_STYLE);

		return ++mainRowId;
	}

	private int logSumData(XSSFSheet mainSheet, int mainRowId, double unpaidReqSum, double surchargeSum) {

		Row row = null;
		Cell sumCell = null;
		int cellNum = 0;

		row = mainSheet.createRow(mainRowId++);
		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Összes fennmaradó hátralék");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(unpaidReqSum);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		row = mainSheet.createRow(mainRowId++);
		cellNum = 0;
		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Összes pótdíj");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(surchargeSum);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		row = mainSheet.createRow(mainRowId++);
		cellNum = 0;
		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Összes tartozás");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(unpaidReqSum + surchargeSum);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		cellNum++;
		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Összes tartozás, nyitóegyenleggel");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(unpaidReqSum + surchargeSum + this.startBalance * -1.0);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		return mainRowId;
	}

	private void logMainSheetHeader(XSSFRow row) {
		Cell headerCell = null;
		int cellNum = 0;

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Dátum");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Havi előirányzat");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Fennmaradó hátralék");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Pótdíj");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Összesen");

	}

	private void processBacklogs(List<Backlog> unpaidBacklogs, List<Payment> remainingPayments) {
		Calendar now = Calendar.getInstance();

		for (Backlog backlog : unpaidBacklogs) {
			// System.out.println("\r\nNEW BACKLOG: " + backlog.getDate().getTime());

			for (Payment payment : remainingPayments) {
				backlog.usePayment(payment);
				if (backlog.getRequiredPayment() <= 0) {
					break;
				}
			}

			remainingPayments = remainingPayments.stream().filter(p -> p.getAmount() > 0).collect(Collectors.toList());

			if (backlog.getRequiredPayment() > 0) {
				backlog.updateInterest(now);
			}
		}
	}

	private void createCellStyles(XSSFWorkbook workbook) {
		DATE_CELL_STYLE = workbook.createCellStyle();
		CreationHelper createHelper = workbook.getCreationHelper();

		DATE_CELL_STYLE.setDataFormat(createHelper.createDataFormat().getFormat("yyyy/mm/dd"));

		CURRENCY_CELL_STYLE = workbook.createCellStyle();
		CURRENCY_CELL_STYLE
				.setDataFormat(createHelper.createDataFormat().getFormat("#,##0 [$Ft-40E];-#,##0 [$Ft-40E]"));

		PERCENT_CELL_STYLE = workbook.createCellStyle();
		PERCENT_CELL_STYLE.setDataFormat(createHelper.createDataFormat().getFormat("0%"));

		CENTER_CELL_STYLE = workbook.createCellStyle();
		CENTER_CELL_STYLE.setAlignment(HorizontalAlignment.CENTER);

	}

	private String getSheetNameFromDate(Calendar calendar) {
		return calendar.get(Calendar.YEAR) + "." + calendar.get(Calendar.MONTH) + "."
				+ calendar.get(Calendar.DAY_OF_MONTH);
	}
}
