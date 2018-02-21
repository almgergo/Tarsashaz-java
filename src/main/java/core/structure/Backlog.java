package core.structure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import core.ExcelLoggable;
import core.InterestInformation;
import core.PaymentInformation;
import lombok.Data;

@Data
public class Backlog implements HasDate {
	private Calendar date;
	private Double requiredPayment;
	private Double paidAmount;
	private Double monthlyBalance;

	private Double originalRequiredPayment;
	private Map<Double, Double> surcharges = new HashMap<>();
	private Double surcharge;
	private Double interestRate;

	private List<ExcelLoggable> loggables = new ArrayList<>();

	public Backlog(Calendar date, Double requiredPayment, Double paidAmount, Double monthlyBalance) {
		super();
		this.date = date;
		this.requiredPayment = requiredPayment;
		this.paidAmount = paidAmount;
		this.monthlyBalance = monthlyBalance;

		this.originalRequiredPayment = requiredPayment;
		this.surcharge = 0.0;
		this.interestRate = 1.0;
	}

	public void usePayment(Payment payment) {
		// System.out.println("\r\n");
		updateInterest(payment.getDate());
		doPay(payment);
	}

	private void doPay(Payment payment) {
		// System.out.println("surcharge: " + this.surcharge + ", payment: " +
		// payment.getAmount());

		PaymentInformation pi = new PaymentInformation();
		pi.setDate(payment.getDate().getTime());

		pi.setPaymentAmountBefore(payment.getAmount());
		paySurcharge(payment, pi);

		// System.out.println("surcharge: " + this.surcharge + ", payment: " +
		// payment.getAmount());
		// System.out.println("requiredPayment: " + this.requiredPayment + ", payment: "
		// + payment.getAmount());

		payBacklog(payment, pi);
		pi.setPaymentAmountAfter(payment.getAmount());

		this.loggables.add(pi);
		// System.out.println("requiredPayment: " + this.requiredPayment + ", payment: "
		// + payment.getAmount());

	}

	private void payBacklog(Payment payment, PaymentInformation pi) {
		pi.setBacklogBefore(this.requiredPayment);

		if (payment.getAmount() > 0 && this.requiredPayment > 0) {

			this.requiredPayment -= payment.getAmount();

			if (this.requiredPayment > 0) {
				payment.setAmount(0.0);
			} else {
				payment.setAmount(Math.abs(this.requiredPayment));
				this.requiredPayment = 0.0;
			}
		}
		pi.setBacklogAfter(this.requiredPayment);

	}

	private void paySurcharge(Payment payment, PaymentInformation pi) {
		pi.setSurchargeBefore(this.surcharge);
		if (this.surcharge > 0) {
			this.surcharge -= payment.getAmount();

			if (this.surcharge > 0) {
				payment.setAmount(0.0);
			} else {
				payment.setAmount(Math.abs(this.surcharge));
				this.surcharge = 0.0;
			}
		}

		pi.setSurchargeAfter(this.surcharge);
	}

	public void updateInterest(Calendar paymentDate) {
		long start = this.date.getTimeInMillis();
		long end = paymentDate.getTimeInMillis();
		Long dayDelta = TimeUnit.MILLISECONDS.toDays(end - start);

		if (dayDelta < 31) {
		} else if (31 <= dayDelta && dayDelta <= 45) {
			setInterest(1.2, 31);
		} else if (46 <= dayDelta && dayDelta <= 90) {
			setInterest(1.4, 46);
		} else {
			setInterest(1.6, 91);
		}
	}

	private void setInterest(double rate, int dayDiff) {
		if (rate > this.interestRate) {
			double oldSurcharge = this.surcharge;
			double newSurcharge = Math.round((rate - this.interestRate) * this.requiredPayment);

			this.surcharges.put(rate, newSurcharge);
			this.surcharge += newSurcharge;

			double oldInterestRate = this.interestRate;
			double newInterestRate = rate;

			Calendar c2 = Calendar.getInstance();
			c2.set(Calendar.YEAR, this.getDate().get(Calendar.YEAR));
			c2.set(Calendar.MONTH, this.getDate().get(Calendar.MONTH));
			c2.set(Calendar.DAY_OF_MONTH, this.getDate().get(Calendar.DAY_OF_MONTH));
			c2.set(Calendar.MINUTE, this.getDate().get(Calendar.MINUTE));
			c2.set(Calendar.HOUR_OF_DAY, this.getDate().get(Calendar.HOUR_OF_DAY));
			c2.set(Calendar.SECOND, this.getDate().get(Calendar.SECOND));
			c2.set(Calendar.MILLISECOND, this.getDate().get(Calendar.MILLISECOND));

			c2.add(Calendar.DAY_OF_YEAR, dayDiff);

			this.loggables.add(new InterestInformation(oldInterestRate, newInterestRate, oldSurcharge, newSurcharge,
					this.requiredPayment, c2.getTime()));

			this.interestRate = newInterestRate;

		}
	}

	public void logToExcel(XSSFSheet sheet) {
		int rowNum = sheet.getLastRowNum() + 1;

		if (this.originalRequiredPayment > 0) {

			if (loggables.size() > 0) {

				Row loggableRow = sheet.createRow(rowNum++);
				boolean loggedPaymentInformation = false;

				for (ExcelLoggable loggable : loggables) {

					if (loggable instanceof InterestInformation) {
						loggableRow = sheet.createRow(rowNum++);
						loggedPaymentInformation = false;
					} else if (loggable instanceof PaymentInformation) {
						if (loggedPaymentInformation) {
							loggableRow = sheet.createRow(rowNum++);
							loggedPaymentInformation = false;
						} else {
							loggedPaymentInformation = true;
						}
					}

					loggable.printRow(loggableRow);
				}

				sheet.createRow(rowNum++);
			}
		}

	}

	public int createHeader(XSSFSheet sheet, int rowNum) {
		XSSFRow row = sheet.createRow(rowNum++);
		int cellNum = 0;

		Cell headerCell = null;

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Hátralék");

		for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {

		}

		row = sheet.createRow(rowNum++);
		cellNum = 0;
		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Dátum");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue(this.getDate().getTime());
		headerCell.setCellStyle(Person.DATE_CELL_STYLE);

		cellNum++;
		cellNum++;
		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Befizetések után fennmaradó hátralék");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue(this.getRequiredPayment());
		headerCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		row = sheet.createRow(rowNum++);
		cellNum = 0;
		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Havi befizetendő hátralék");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue(this.getOriginalRequiredPayment());
		headerCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		cellNum++;
		cellNum++;
		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Pótdíj");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue(this.getSurcharge());
		headerCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		row = sheet.createRow(rowNum++);
		cellNum = 4;
		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue("Összesen");

		headerCell = row.createCell(cellNum++);
		headerCell.setCellValue(this.getSurcharge() + this.getRequiredPayment());
		headerCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		return rowNum;
	}

	public void sumMainInfo(Row row) {
		int cellNum = 0;

		Cell sumCell = null;

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.getDate().getTime());
		sumCell.setCellStyle(Person.DATE_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.getOriginalRequiredPayment());
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.getRequiredPayment());
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.getSurcharge());
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

		sumCell = row.createCell(cellNum++);
		sumCell.setCellValue(this.getSurcharge() + this.getRequiredPayment());
		sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);
	}

}
