package com.lssservlet.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    static public Boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    static public Boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static Long parseAmount(String amountStr) {
        if (amountStr != null) {
            Long amount = Long.parseLong(amountStr.replace(".", ""));
            if (amount != null)
                return amount;
        }
        return null;
    }

    public static double parseDouble(Float value) {
        if (value != null) {
            return value;
        } else {
            return 0.0;
        }
    }

    public static String parseAmount(Long amount) {
        if (amount != null && amount > 0) {
            StringBuilder sb = new StringBuilder(amount.toString());
            if (sb != null && sb.length() > 0) {
                if (sb.length() >= 3)
                    return sb.insert(sb.length() - 2, ".").toString();
                else if (sb.length() == 2)
                    return sb.insert(0, "0.").toString();
                if (sb.length() == 1)
                    return sb.insert(0, "0.0").toString();
            }
        }
        return "0";
    }

    public static String expDate(String content) {
        final String slash = "/";
        Pattern p = Pattern.compile(slash);
        Matcher m = p.matcher(content);
        return m.replaceAll("").trim();
    }

    public static String txtToHtml(String s) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch (c) {
            case '<':
                builder.append("&lt;");
                break;
            case '>':
                builder.append("&gt;");
                break;
            case '&':
                builder.append("&amp;");
                break;
            case '"':
                builder.append("&quot;");
                break;
            case '\n':
                builder.append("<br>");
                break;
            case '\t':
                builder.append("&nbsp; &nbsp; &nbsp; &nbsp;");
                break;
            default:
                builder.append(c);

            }
        }
        String converted = builder.toString();
        String str = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))";
        Pattern patt = Pattern.compile(str);
        Matcher matcher = patt.matcher(converted);
        converted = matcher.replaceAll("<a href=\"$1\">$1</a>");
        return converted;
    }
}
