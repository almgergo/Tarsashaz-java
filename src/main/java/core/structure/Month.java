package core.structure;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
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
@AllArgsConstructor
@NoArgsConstructor
public class Month {

	private Calendar date;
	private Backlog backlog;

	@Builder.Default
	private LinkedList<Payment> payments = new LinkedList<>();
	@Builder.Default
	private LinkedList<Month> previousMonths = new LinkedList<>();

	private Double monthlyBalance;
	private Double allSurcharge;
	private Double totalPayment;
	private Double totalRequiredAmount;
	private Double totalBacklog;

	public void processMonth(Person person) {
		List<Backlog> backlogs = getAllBacklogs();
		List<Payment> allPayments = getAllPayments();

		processBacklogs(backlogs, allPayments);
		updateStartBalance(person, allPayments);
		calculateMonthlyBalance(person, backlogs, allPayments);
	}

	private void calculateMonthlyBalance(Person person, List<Backlog> backlogs, List<Payment> allPayments) {
		monthlyBalance = 0.0;
		totalBacklog = 0.0;

		for (Backlog backlog : backlogs) {
			totalBacklog += backlog.getOriginalRequiredPayment();

			if (backlog.getRequiredPayment() > 0) {
				monthlyBalance -= backlog.getRequiredPayment();
			}

		}

		for (Payment payment : allPayments) {
			if (payment.getAmount() > 0) {
				monthlyBalance += payment.getAmount();
			}
		}

		if (person.getStartBalance() < 0) {
			monthlyBalance += person.getStartBalance();
		}
	}

	private Double calculateSumSurcharge() {
		allSurcharge = 0.0;

		for (Entry<Double, Double> entry : this.backlog.getSurcharges().entrySet()) {
			allSurcharge += entry.getValue();
		}

		return allSurcharge;
	}

	private List<Backlog> getAllBacklogs() {
		List<Backlog> backlogs = previousMonths.stream().map(m -> m.getBacklog()).collect(Collectors.toList());
		backlogs.add(this.backlog);
		return backlogs;
	}

	private List<Payment> getAllPayments() {
		totalPayment = 0.0;

		List<Payment> allPayments = previousMonths.stream().map(m -> m.getPayments()).flatMap(p -> p.stream())
				.collect(Collectors.toList());
		allPayments.addAll(payments);

		allPayments.forEach(p -> totalPayment += p.getOriginalAmount());

		return allPayments;
	}

	private void processBacklogs(List<Backlog> backlogs, List<Payment> allPayments) {
		for (Backlog backlog : backlogs) {

			for (Payment payment : allPayments) {
				if (payment.getAmount() > 0) {
					backlog.usePayment(payment);
					if (backlog.getRequiredPayment() <= 0) {
						break;
					}
				}
			}

			if (backlog.getRequiredPayment() > 0) {
				backlog.updateInterest(date);
			}
		}
	}

	private void updateStartBalance(Person person, List<Payment> allPayments) {
		double startBalance = person.getStartBalance();
		if (startBalance < 0) {
			for (Payment payment : allPayments) {
				if (payment.getAmount() > 0) {
					startBalance += payment.getAmount();
					if (startBalance >= 0) {
						payment.setAmount(startBalance);
						startBalance = 0;
						break;
					} else {
						payment.setAmount(0.0);
					}
				}
			}
		}

		person.setStartBalance(startBalance);
	}

	public void logMonth(Person person, XSSFWorkbook workbook) {
		XSSFSheet monthlySheet = workbook.createSheet(this.date.get(Calendar.YEAR) + "_"
				+ (this.date.get(Calendar.MONTH) + 1) + "_" + this.date.get(Calendar.DAY_OF_MONTH));
		int rowNum = 0;

		calculateTotalRequiredAmount(person);

		rowNum = logPersonHeader(person, monthlySheet, rowNum);
		logMainSheetHeader(monthlySheet.createRow(rowNum++));

		for (Month m : previousMonths) {
			m.logRow(person, monthlySheet.createRow(rowNum++));
		}
		this.logRow(person, monthlySheet.createRow(rowNum++));

		for (int i = 0; i < 200; i++) {
			monthlySheet.autoSizeColumn(i);
		}
	}

	private void calculateTotalRequiredAmount(Person person) {
		totalRequiredAmount = 0.0;
		for (Month m : previousMonths) {
			totalRequiredAmount += m.backlog.getOriginalRequiredPayment() + m.calculateSumSurcharge();
		}
		totalRequiredAmount += this.backlog.getOriginalRequiredPayment() + this.calculateSumSurcharge();

		if (person.getStartBalance() < 0) {
			totalRequiredAmount -= person.getStartBalance();
		}
	}

	private int logPersonHeader(Person p, XSSFSheet sheet, int rowNum) {
		Row row = null;
		Cell sumCell = null;
		int cellNum = 0;

		row = sheet.createRow(rowNum++);
		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Alb. azonosító");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Tulajdonos");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Nyitó egyenleg 2017");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Előirányzat");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Befizetés");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Összes közös költség + számított kamat");

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue("Egyenleg");

		row = sheet.createRow(rowNum++);
		cellNum = 0;

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(p.getIdentifier());

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(p.getName());

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(p.getOriginalStartBalance());
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(totalBacklog);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(totalPayment);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(totalRequiredAmount);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(totalPayment - totalRequiredAmount);
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		rowNum++;
		rowNum++;

		return rowNum;

	}

	private void logMainSheetHeader(XSSFRow row) {
		Cell headerCell = null;
		int cellNum = 0;

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Dátum");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Előirányzat");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Befizetve");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Havi egyenleg");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Rendezetlen előirányzat");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("31-45 nap kamata: 20%");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("46-90 nap kamata 40%");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("91. naptól 60%");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("közös költség + számított kamat");

	}

	public void logRow(Person person, Row row) {
		Cell cell = null;
		int cellNum = 0;

		cell = row.createCell(cellNum++);
		cell.setCellValue(date.getTime());
		cell.setCellStyle(Person.DATE_CELL_STYLE);

		cell = row.createCell(cellNum++);
		cell.setCellValue(this.backlog.getOriginalRequiredPayment());
		cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		cell = row.createCell(cellNum++);
		cell.setCellValue(this.backlog.getPaidAmount());
		cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		cell = row.createCell(cellNum++);
		cell.setCellValue(this.monthlyBalance);
		cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		cell = row.createCell(cellNum++);
		cell.setCellValue(this.backlog.getRequiredPayment());
		cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		cell = row.createCell(cellNum++);
		printSurcharge(cell, 1.2);
		cell = row.createCell(cellNum++);
		printSurcharge(cell, 1.4);
		cell = row.createCell(cellNum++);
		printSurcharge(cell, 1.6);

		cell = row.createCell(cellNum++);
		cell.setCellValue(this.backlog.getOriginalRequiredPayment() + calculateSumSurcharge());
		cell.setCellStyle(Person.CURRENCY_CELL_STYLE);
	}

	private void printSurcharge(Cell cell, double interestRate) {
		if (this.backlog.getSurcharges().get(interestRate) != null
				&& this.backlog.getSurcharges().get(interestRate) > 0) {
			cell.setCellValue(this.backlog.getSurcharges().get(interestRate));
			cell.setCellStyle(Person.CURRENCY_CELL_STYLE);
		}
	}
}