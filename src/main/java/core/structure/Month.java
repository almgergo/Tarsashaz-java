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

    public void processMonth( final Person person ) {
        final List<Backlog> backlogs = this.getAllBacklogs();
        final List<Payment> allPayments = this.getAllPayments();

        this.processBacklogs(backlogs, allPayments);
        this.updateStartBalance(person, allPayments);
        this.calculateMonthlyBalance(person, backlogs, allPayments);

        if ( this.itIsCurrentMonth() ) {
            this.updateInterestForDate(Calendar.getInstance());
        }
    }

    private void calculateMonthlyBalance( final Person person, final List<Backlog> backlogs, final List<Payment> allPayments ) {
        this.monthlyBalance = 0.0;
        this.totalBacklog = 0.0;

        for ( final Backlog backlog : backlogs ) {
            this.totalBacklog += backlog.getOriginalRequiredPayment();

            if ( backlog.getRequiredPayment() > 0 ) {
                this.monthlyBalance -= backlog.getRequiredPayment();
            }

            if ( backlog.getSurcharge() > 0 ) {
                this.monthlyBalance -= backlog.getSurcharge();
            }

        }

        for ( final Payment payment : allPayments ) {
            if ( payment.getAmount() > 0 ) {
                this.monthlyBalance += payment.getAmount();
            }
        }

        if ( person.getStartBalance() < 0 ) {
            this.monthlyBalance += person.getStartBalance();
        }
    }

    private Double calculateSumSurcharge() {
        this.allSurcharge = 0.0;

        for ( final Entry<Double, Double> entry : this.backlog.getSurcharges().entrySet() ) {
            this.allSurcharge += entry.getValue();
        }

        return this.allSurcharge;
    }

    private List<Backlog> getAllBacklogs() {
        final List<Backlog> backlogs = this.previousMonths.stream().map(m -> m.getBacklog()).collect(Collectors.toList());
        backlogs.add(this.backlog);
        return backlogs;
    }

    private List<Payment> getAllPayments() {
        this.totalPayment = 0.0;

        final List<Payment> allPayments =
            this.previousMonths.stream().map(m -> m.getPayments()).flatMap(p -> p.stream()).collect(Collectors.toList());
        allPayments.addAll(this.payments);

        allPayments.forEach(p -> this.totalPayment += p.getOriginalAmount());

        return allPayments;
    }

    private void processBacklogs( final List<Backlog> backlogs, final List<Payment> allPayments ) {
        for ( final Backlog backlog : backlogs ) {

            for ( final Payment payment : allPayments ) {
                if ( payment.getAmount() > 0 ) {
                    backlog.usePayment(payment);
                    if ( backlog.getRequiredPayment() <= 0 ) {
                        break;
                    }
                }
            }

            this.updateInterestForBacklog(this.date, backlog);
        }
    }

    public void updateInterestForDate( final Calendar calendar ) {
        // if ( date.before(this.getCurrentMonthEnd()) && !date.before(this.getCurrentMonthStart()) ) {
        for ( final Backlog backlog : this.getAllBacklogs() ) {
            this.updateInterestForBacklog(calendar, backlog);
        }
        // }
    }

    private void updateInterestForBacklog( final Calendar date, final Backlog backlog ) {
        if ( backlog.getRequiredPayment() > 0 ) {
            backlog.updateInterest(date);
        }
    }

    private void updateStartBalance( final Person person, final List<Payment> allPayments ) {
        double startBalance = person.getStartBalance();
        if ( startBalance < 0 ) {
            for ( final Payment payment : allPayments ) {
                if ( payment.getAmount() > 0 ) {
                    startBalance += payment.getAmount();
                    if ( startBalance >= 0 ) {
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

    public void logMonth( final Person person, final XSSFWorkbook workbook ) {
        final XSSFSheet monthlySheet = workbook
            .createSheet(this.date.get(Calendar.YEAR) + "_" + (this.date.get(Calendar.MONTH) + 1) + "_" + this.date.get(Calendar.DAY_OF_MONTH));
        int rowNum = 0;

        this.calculateTotalRequiredAmount(person);

        rowNum = this.logPersonHeader(person, monthlySheet, rowNum);
        this.logMainSheetHeader(monthlySheet.createRow(rowNum++));

        for ( final Month m : this.previousMonths ) {
            m.logRow(person, monthlySheet.createRow(rowNum++));
        }
        this.logRow(person, monthlySheet.createRow(rowNum++));

        this.logSummary(person, monthlySheet.createRow(rowNum++));

        for ( int i = 0; i < 200; i++ ) {
            monthlySheet.autoSizeColumn(i);
        }

        this.setSheetOrder(workbook, monthlySheet.getSheetName());

    }

    public void setSheetOrder( final XSSFWorkbook workbook, final String sheetName ) {
        if ( this.itIsCurrentMonth() ) {
            workbook.setSheetOrder(sheetName, 0);
        }
    }

    private boolean itIsCurrentMonth() {
        return !this.date.before(getCurrentMonthStart()) && this.date.before(getCurrentMonthEnd());
    }

    public void hideSheet( final XSSFWorkbook workbook, final int sheetIx ) {
        final Calendar startOfMonth = getCurrentMonthStart();
        final Calendar endOfMonth = getCurrentMonthEnd();

        if ( this.getDate().before(startOfMonth) || !this.getDate().before(endOfMonth) ) {
            workbook.setSheetHidden(sheetIx, true);
        }
    }

    private static Calendar getCurrentMonthEnd() {
        final Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.add(Calendar.MONTH, 1);
        endOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        endOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        endOfMonth.set(Calendar.MINUTE, 0);
        endOfMonth.set(Calendar.SECOND, 0);
        endOfMonth.set(Calendar.MILLISECOND, 0);
        return endOfMonth;
    }

    private static Calendar getCurrentMonthStart() {
        final Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);
        return startOfMonth;
    }

    private void calculateTotalRequiredAmount( final Person person ) {
        this.totalRequiredAmount = 0.0;
        for ( final Month m : this.previousMonths ) {
            this.totalRequiredAmount += m.backlog.getOriginalRequiredPayment() + m.calculateSumSurcharge();
        }
        this.totalRequiredAmount += this.backlog.getOriginalRequiredPayment() + this.calculateSumSurcharge();

        if ( person.getStartBalance() < 0 ) {
            this.totalRequiredAmount -= person.getOriginalStartBalance();
        }
    }

    private int logPersonHeader( final Person p, final XSSFSheet sheet, int rowNum ) {
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

        // sumCell = row.createCell(cellNum++);
        // sumCell.setCellValue("Összes közös költség + számított kamat + nyitó
        // egyenleg");

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
        sumCell.setCellValue(this.totalBacklog);
        sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        sumCell = row.createCell(cellNum++);
        sumCell.setCellValue(this.totalPayment);
        sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        // sumCell = row.createCell(cellNum++);
        // sumCell.setCellValue(totalRequiredAmount);
        // sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        rowNum++;
        rowNum++;

        return rowNum;

    }

    private void logMainSheetHeader( final XSSFRow row ) {
        Cell headerCell = null;
        int cellNum = 0;

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Dátum");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Előirányzat");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Befizetve");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Rendezetlen előirányzat");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("31-45 nap kamata: 20%");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("46-90 nap kamata: 40%");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("91. naptól kamat: 60%");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Összes kamat");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Közös költség + számított kamat");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("Havi egyenleg kamattal");

    }

    public void logRow( final Person person, final Row row ) {
        Cell cell = null;
        int cellNum = 0;

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.date.getTime());
        cell.setCellStyle(Person.DATE_CELL_STYLE);

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.backlog.getOriginalRequiredPayment());
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.backlog.getPaidAmount());
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.backlog.getRequiredPayment());
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        this.printSurcharge(cell, 1.2);
        cell = row.createCell(cellNum++);
        this.printSurcharge(cell, 1.2, 1.4);
        cell = row.createCell(cellNum++);
        this.printSurcharge(cell, 1.2, 1.4, 1.6);

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.getAllSurchargeForMonth());
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.backlog.getOriginalRequiredPayment() + this.calculateSumSurcharge());
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        cell.setCellValue(this.monthlyBalance);
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);
    }

    public void logSummary( final Person person, final Row row ) {
        Cell cell = null;
        int cellNum = 0;

        cell = row.createCell(cellNum++);
        // cell.setCellValue(date.getTime());
        // cell.setCellStyle(Person.DATE_CELL_STYLE);

        cell = row.createCell(cellNum++);
        // cell.setCellValue(this.backlog.getOriginalRequiredPayment());
        // cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        // cell.setCellValue(this.backlog.getPaidAmount());
        // cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        // cell.setCellValue(this.backlog.getRequiredPayment());
        // cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        // printSurcharge(cell, 1.2);
        cell = row.createCell(cellNum++);
        // printSurcharge(cell, 1.4);
        cell = row.createCell(cellNum++);
        // printSurcharge(cell, 1.6);
        cell = row.createCell(cellNum++);
        cell.setCellValue(this.getAllSurcharge());
        cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        // cell.setCellValue(this.backlog.getOriginalRequiredPayment() +
        // calculateSumSurcharge());
        // cell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cell = row.createCell(cellNum++);
        // cell.setCellValue(this.monthlyBalance);
        // cell.setCellStyle(Person.CURRENCY_CELL_STYLE);
    }

    private double getAllSurcharge() {
        double sumSurcharge = 0;
        for ( final Month month : this.previousMonths ) {
            sumSurcharge += month.getAllSurchargeForMonth();
        }

        sumSurcharge += this.getAllSurchargeForMonth();

        return sumSurcharge;
    }

    private double getAllSurchargeForMonth() {
        double sumSurcharge = 0;
        for ( final Entry<Double, Double> surcharge : this.backlog.getSurcharges().entrySet() ) {
            sumSurcharge += surcharge.getValue();
        }

        return sumSurcharge;
    }

    private void printSurcharge( final Cell cell, final double... interestRates ) {
        if ( this.backlog.getSurcharges().get(interestRates[interestRates.length - 1]) != null
            && this.backlog.getSurcharges().get(interestRates[interestRates.length - 1]) > 0 ) {
            double sumInterest = 0.0;

            for ( final double interestRate : interestRates ) {
                sumInterest += this.backlog.getSurcharges().get(interestRate);
            }

            cell.setCellValue(sumInterest);
            cell.setCellStyle(Person.CURRENCY_CELL_STYLE);
        }
    }
}
