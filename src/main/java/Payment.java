import java.util.Calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Payment implements HasDate {
	private Calendar date;
	private String payingPerson;
	private String subject;
	private Double amount;
}
