package core.structure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import core.InterestInformation;
import core.PaymentInformation;
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
    private Double originalStartBalance;
    private Double existingBacklog;
    private Double paymentTotal;
    private Double balance;

    private Double remainingPaymentBalance;

    List<Backlog> backlogs = new LinkedList<>();
    List<Payment> payments = new LinkedList<>();
    LinkedList<Month> months = new LinkedList<>();

    public void processPerson() {
        final XSSFWorkbook workbook = new XSSFWorkbook();
        this.createCellStyles(workbook);

        for ( final Month m : this.months ) {
            m.processMonth(this);
            m.logMonth(this, workbook);
        }

        // this.months.getLast().updateInterestForDate(Calendar.getInstance());

        for ( final Month m : this.months ) {

        }

        for ( int i = 1; i < this.months.size(); i++ ) {
            workbook.setSheetHidden(i, true);

            // System.out.println(i + " " + workbook.getSheetAt(i).getSheetName() + " " + workbook.isSheetHidden(i));
        }

        workbook.setActiveSheet(0);
        this.printWorkbookToFile(workbook);
    }

    private void printWorkbookToFile( final XSSFWorkbook workbook ) {
        try {
            final File theDir = new File("eredmenyek");

            if ( !theDir.exists() ) {
                boolean result = false;

                try {
                    theDir.mkdir();
                    result = true;
                } catch (final SecurityException se) {
                    // handle it
                }
            }

            final FileOutputStream outputStream =
                new FileOutputStream("eredmenyek/" + this.name.trim().replace("/", ", ") + " " + this.identifier.trim().replace("/", ", ") + ".xlsx");
            workbook.write(outputStream);
            workbook.close();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void logBacklogs( final List<Backlog> unpaidBacklogs, final XSSFWorkbook workbook ) {
        double unpaidReqSum = 0.0;
        double surchargeSum = 0.0;

        int mainRowId = 0;
        final XSSFSheet mainSheet = workbook.createSheet("Összegzés");

        mainRowId = this.personHeader(mainRowId, mainSheet);

        int paymentMainRowId = mainRowId;
        this.logMainSheetHeader(mainSheet.createRow(mainRowId++));

        for ( final Backlog backlog : unpaidBacklogs ) {
            if ( backlog.getOriginalRequiredPayment() > 0 ) {

                final Row mainRow = mainSheet.createRow(mainRowId++);
                unpaidReqSum += backlog.getRequiredPayment();
                surchargeSum += backlog.getSurcharge();

                backlog.sumMainInfo(mainRow);

                // System.out.println("LOGGING BACKLOG: " + backlog.getDate().getTime());

                final XSSFSheet sheet = workbook.createSheet(backlog.getDate().get(Calendar.YEAR) + "." + (backlog.getDate().get(Calendar.MONTH) + 1)
                    + "." + backlog.getDate().get(Calendar.DAY_OF_MONTH));

                int rowNum = 0;
                rowNum = backlog.createHeader(sheet, rowNum);

                if ( backlog.getLoggables().size() > 0 ) {
                    rowNum++;
                    final Row infoHeaderRow = sheet.createRow(rowNum++);
                    InterestInformation.printHeader(infoHeaderRow);
                    PaymentInformation.printHeader(infoHeaderRow);

                    backlog.logToExcel(sheet);
                }

                for ( int i = 0; i < 200; i++ ) {
                    sheet.autoSizeColumn(i);
                }

            }

        }

        Payment.logTitle(this.getOrCreateRow(mainSheet, paymentMainRowId - 1));
        Payment.sumMainInfoHeader(this.getOrCreateRow(mainSheet, paymentMainRowId++));
        for ( final Payment payment : this.payments ) {
            payment.sumMainInfo(this.getOrCreateRow(mainSheet, paymentMainRowId++));
        }

        mainRowId = this.logSumData(mainSheet, mainRowId + 2, unpaidReqSum, surchargeSum);

        for ( int i = 0; i < 200; i++ ) {
            mainSheet.autoSizeColumn(i);
        }
    }

    private XSSFRow getOrCreateRow( final XSSFSheet mainSheet, int paymentMainRowId ) {
        XSSFRow row = mainSheet.getRow(paymentMainRowId++);
        if ( row == null ) {
            row = mainSheet.createRow(paymentMainRowId);
        }
        return row;
    }

    private int personHeader( int mainRowId, final XSSFSheet mainSheet ) {
        final Row row = mainSheet.createRow(mainRowId++);
        int cellId = 0;
        row.createCell(cellId++).setCellValue(this.name);

        cellId++;
        row.createCell(cellId++).setCellValue("Nyitó egyenleg");

        final Cell c = row.createCell(cellId++);
        c.setCellValue(this.startBalance);
        c.setCellStyle(Person.CURRENCY_CELL_STYLE);

        return ++mainRowId;
    }

    private int logSumData( final XSSFSheet mainSheet, int mainRowId, final double unpaidReqSum, final double surchargeSum ) {

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
        sumCell.setCellValue(unpaidReqSum + surchargeSum - this.remainingPaymentBalance);
        sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        row = mainSheet.createRow(mainRowId++);
        cellNum = 0;
        sumCell = row.createCell(cellNum++);
        sumCell.setCellValue("Megmaradt befizetés");

        sumCell = row.createCell(cellNum++);
        sumCell.setCellValue(this.remainingPaymentBalance);
        sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        cellNum++;
        sumCell = row.createCell(cellNum++);
        sumCell.setCellValue("Összes tartozás, nyitóegyenleggel");

        sumCell = row.createCell(cellNum++);
        sumCell.setCellValue(unpaidReqSum + surchargeSum + this.startBalance * -1.0 - this.remainingPaymentBalance);
        sumCell.setCellStyle(Person.CURRENCY_CELL_STYLE);

        return mainRowId;
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
        headerCell.setCellValue("Havi egyenleg");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("31-45 nap kamata: 20%");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("46-90 nap kamata 40%");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("91. naptól 60%");

        headerCell = row.createCell(cellNum++);
        headerCell.setCellValue("közös költség + számított kamat");

    }

    private void processBacklogs( final List<Backlog> unpaidBacklogs, final List<Payment> remainingPayments ) {
        final Calendar now = Calendar.getInstance();

        double paymentBalance = 0.0;
        for ( final Payment p : remainingPayments ) {
            paymentBalance += p.getAmount();
        }

        this.remainingPaymentBalance = paymentBalance;

    }

    private void createCellStyles( final XSSFWorkbook workbook ) {
        DATE_CELL_STYLE = workbook.createCellStyle();
        final CreationHelper createHelper = workbook.getCreationHelper();

        DATE_CELL_STYLE.setDataFormat(createHelper.createDataFormat().getFormat("yyyy/mm/dd"));

        CURRENCY_CELL_STYLE = workbook.createCellStyle();
        CURRENCY_CELL_STYLE.setDataFormat(createHelper.createDataFormat().getFormat("#,##0 [$Ft-40E];-#,##0 [$Ft-40E]"));

        PERCENT_CELL_STYLE = workbook.createCellStyle();
        PERCENT_CELL_STYLE.setDataFormat(createHelper.createDataFormat().getFormat("0%"));

        CENTER_CELL_STYLE = workbook.createCellStyle();
        CENTER_CELL_STYLE.setAlignment(HorizontalAlignment.CENTER);

    }

    private String getSheetNameFromDate( final Calendar calendar ) {
        return calendar.get(Calendar.YEAR) + "." + calendar.get(Calendar.MONTH) + "." + calendar.get(Calendar.DAY_OF_MONTH);
    }
}
