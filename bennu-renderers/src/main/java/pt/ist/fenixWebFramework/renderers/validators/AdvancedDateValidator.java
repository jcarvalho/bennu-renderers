/**
 * 
 */
package pt.ist.fenixWebFramework.renderers.validators;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Predicate;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
public class AdvancedDateValidator extends DateValidator {

    private String validationPeriod;

    public AdvancedDateValidator() {
        super();
    }

    public AdvancedDateValidator(HtmlChainValidator htmlChainValidator) {
        super(htmlChainValidator);
    }

    public AdvancedDateValidator(HtmlChainValidator htmlChainValidator, String dateFormat) {
        super(htmlChainValidator, dateFormat);
    }

    @Override
    public void performValidation() {
        super.performValidation();

        if (isValid()) {
            try {
                DateTime dateTime = new DateTime(parse(getDateFormat(), getComponent().getValue()).getTime());
                setValid(getValidationPeriodType().evaluateDate(dateTime));
            } catch (ParseException e) {
                setValid(false);
                e.printStackTrace();
            }
        }
    }

    public static Date parse(final String format, final String dateString) throws ParseException {
        return new SimpleDateFormat(format).parse(dateString);
    }

    public String getValidationPeriod() {
        return validationPeriod;
    }

    public void setValidationPeriod(String validationPeriod) {
        this.validationPeriod = validationPeriod;
        setMessage("renderers.validator.advancedDate." + getValidationPeriod());
    }

    public ValidationPeriodType getValidationPeriodType() {
        if (this.validationPeriod != null) {
            return ValidationPeriodType.valueOf(getValidationPeriod().toUpperCase());
        }
        return null;
    }

    private static Predicate<DateTime> pastPredicate = dateTime -> dateTime.isBeforeNow();

    private static Predicate<DateTime> pastOrTodayPredicate = dateTime -> dateTime.isBeforeNow()
            || dateTime.toLocalDate().isEqual(new LocalDate());

    private static Predicate<DateTime> futurePredicate = dateTime -> dateTime.isAfterNow();

    private enum ValidationPeriodType {

        PAST(pastPredicate), PASTORTODAY(pastOrTodayPredicate), FUTURE(futurePredicate);

        private Predicate<DateTime> predicate;

        private ValidationPeriodType(Predicate<DateTime> predicate) {
            this.predicate = predicate;
        }

        protected boolean evaluateDate(DateTime dateTime) {
            return this.predicate.apply(dateTime);
        }

    }

}
