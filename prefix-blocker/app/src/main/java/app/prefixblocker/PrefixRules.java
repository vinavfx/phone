package app.prefixblocker;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Set;

public final class PrefixRules {
    private PrefixRules() {}

    public static String normalizePrefix(String value) {
        String prefix = value.replaceAll("[\\s().-]", "");
        return prefix.matches("\\+?[0-9]{1,15}") ? prefix : "";
    }

    public static boolean blocks(String number, String region, Set<String> prefixes) {
        if (number == null || prefixes.isEmpty()) {
            return false;
        }
        String digits = number.replaceAll("[^0-9]", "");
        String national = digits;
        String international = number.startsWith("+") ? "+" + digits : "";
        PhoneNumberUtil phoneNumbers = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber parsed = phoneNumbers.parse(number, region);
            national = phoneNumbers.getNationalSignificantNumber(parsed);
            international = phoneNumbers.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ignored) {
        }
        for (String prefix : prefixes) {
            if (prefix.startsWith("+")) {
                if (international.startsWith(prefix)) {
                    return true;
                }
            } else if (national.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
