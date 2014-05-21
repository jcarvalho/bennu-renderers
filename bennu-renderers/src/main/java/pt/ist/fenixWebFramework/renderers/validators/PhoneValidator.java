package pt.ist.fenixWebFramework.renderers.validators;

import java.util.Locale;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;

import com.google.common.base.Strings;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class PhoneValidator extends HtmlValidator {

    public PhoneValidator() {
        super();
    }

    public PhoneValidator(HtmlChainValidator htmlChainValidator) {
        super(htmlChainValidator);
    }

    @Override
    public void performValidation() {
        setValid(isValidNumber(getComponent().getValue()));
    }

    @Override
    public String getErrorMessage() {
        return RenderUtils.getResourceString("renderers.validator.phone.number");
    }

    public static boolean isValidNumber(String numberText) {
        final PhoneNumber phoneNumber = getPhoneNumber(numberText);
        return phoneNumber != null;
    }

    public static PhoneNumber getPhoneNumber(String numberText) {
        if (!Strings.isNullOrEmpty(numberText)) {
            if (numberText.startsWith("00")) {
                numberText = numberText.replaceFirst("00", "+");
            }
            try {
                final PhoneNumber phoneNumber = PhoneNumberUtil.getInstance().parse(numberText, Locale.getDefault().getCountry());
                if (PhoneNumberUtil.getInstance().isValidNumber(phoneNumber)) {
                    return phoneNumber;
                }
            } catch (NumberParseException e) {
                return null;
            }
        }
        return null;
    }

}
