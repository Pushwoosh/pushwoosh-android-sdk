/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.inapp.mapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.pushwoosh.internal.utils.PWLog;

public class InAppTagFormatModifier {
	// String modificators
	private static final String MODIFICATOR_LOWERCASE = "lowercase";
	private static final String MODIFICATOR_UPPERCASE = "UPPERCASE";
	private static final String MODIFICATOR_CAPITALIZE_ALL_FIRST = "CapitalizeAllFirst";
	private static final String MODIFICATOR_CAPITALIZE_FIRST = "CapitalizeFirst";

	// Int/Date modificators
	private static final String MODIFICATOR_CENT = "cent";
	private static final String MODIFICATOR_DOLLAR = "dollar";
	private static final String MODIFICATOR_COMMA = "comma";
	private static final String MODIFICATOR_EURO = "euro";
	private static final String MODIFICATOR_JPY = "jpy";
	private static final String MODIFICATOR_LIRA = "lira";

	private static final String MODIFICATOR_DATE1 = "M-d-y";
	private static final String MODIFICATOR_DATE2 = "m-d-y";
	private static final String MODIFICATOR_DATE3 = "M d y";
	private static final String MODIFICATOR_DATE4 = "M d Y";
	private static final String MODIFICATOR_DATE5 = "l";
	private static final String MODIFICATOR_DATE6 = "M d";
	private static final String MODIFICATOR_DATE7 = "H:i";
	private static final String MODIFICATOR_DATE8 = "m-d-y H:i";

	private InAppTagFormatModifier() {
	}

	private static final HashMap<String, String> COUNTRY_CODES = new HashMap<>();

	private static Map<String, String> getCountyCodes() {
		if (COUNTRY_CODES.isEmpty()) {
			COUNTRY_CODES.put("AD", "Andorra");
			COUNTRY_CODES.put("AE", "United Arab Emirates");
			COUNTRY_CODES.put("AF", "Afghanistan");
			COUNTRY_CODES.put("AG", "Antigua and Barbuda");
			COUNTRY_CODES.put("AI", "Anguilla");
			COUNTRY_CODES.put("AL", "Albania");
			COUNTRY_CODES.put("AM", "Armenia");
			COUNTRY_CODES.put("AO", "Angola");
			COUNTRY_CODES.put("AP", "Asia/Pacific Region");
			COUNTRY_CODES.put("AQ", "Antarctica");
			COUNTRY_CODES.put("AR", "Argentina");
			COUNTRY_CODES.put("AS", "American Samoa");
			COUNTRY_CODES.put("AT", "Austria");
			COUNTRY_CODES.put("AU", "Australia");
			COUNTRY_CODES.put("AW", "Aruba");
			COUNTRY_CODES.put("AX", "Aland Islands");
			COUNTRY_CODES.put("AZ", "Azerbaijan");
			COUNTRY_CODES.put("BA", "Bosnia and Herzegovina");
			COUNTRY_CODES.put("BB", "Barbados");
			COUNTRY_CODES.put("BD", "Bangladesh");
			COUNTRY_CODES.put("BE", "Belgium");
			COUNTRY_CODES.put("BF", "Burkina Faso");
			COUNTRY_CODES.put("BG", "Bulgaria");
			COUNTRY_CODES.put("BH", "Bahrain");
			COUNTRY_CODES.put("BI", "Burundi");
			COUNTRY_CODES.put("BJ", "Benin");
			COUNTRY_CODES.put("BL", "Saint Bartelemey");
			COUNTRY_CODES.put("BM", "Bermuda");
			COUNTRY_CODES.put("BN", "Brunei Darussalam");
			COUNTRY_CODES.put("BO", "Bolivia");
			COUNTRY_CODES.put("BQ", "Bonaire, Saint Eustatius and Saba");
			COUNTRY_CODES.put("BR", "Brazil");
			COUNTRY_CODES.put("BS", "Bahamas");
			COUNTRY_CODES.put("BT", "Bhutan");
			COUNTRY_CODES.put("BV", "Bouvet Island");
			COUNTRY_CODES.put("BW", "Botswana");
			COUNTRY_CODES.put("BY", "Belarus");
			COUNTRY_CODES.put("BZ", "Belize");
			COUNTRY_CODES.put("CA", "Canada");
			COUNTRY_CODES.put("CC", "Cocos (Keeling) Islands");
			COUNTRY_CODES.put("CD", "Congo, The Democratic Republic of the");
			COUNTRY_CODES.put("CF", "Central African Republic");
			COUNTRY_CODES.put("CG", "Congo");
			COUNTRY_CODES.put("CH", "Switzerland");
			COUNTRY_CODES.put("CI", "Cote d'Ivoire");
			COUNTRY_CODES.put("CK", "Cook Islands");
			COUNTRY_CODES.put("CL", "Chile");
			COUNTRY_CODES.put("CM", "Cameroon");
			COUNTRY_CODES.put("CN", "China");
			COUNTRY_CODES.put("CO", "Colombia");
			COUNTRY_CODES.put("CR", "Costa Rica");
			COUNTRY_CODES.put("CU", "Cuba");
			COUNTRY_CODES.put("CV", "Cape Verde");
			COUNTRY_CODES.put("CW", "Curacao");
			COUNTRY_CODES.put("CX", "Christmas Island");
			COUNTRY_CODES.put("CY", "Cyprus");
			COUNTRY_CODES.put("CZ", "Czech Republic");
			COUNTRY_CODES.put("DE", "Germany");
			COUNTRY_CODES.put("DJ", "Djibouti");
			COUNTRY_CODES.put("DK", "Denmark");
			COUNTRY_CODES.put("DM", "Dominica");
			COUNTRY_CODES.put("DO", "Dominican Republic");
			COUNTRY_CODES.put("DZ", "Algeria");
			COUNTRY_CODES.put("EC", "Ecuador");
			COUNTRY_CODES.put("EE", "Estonia");
			COUNTRY_CODES.put("EG", "Egypt");
			COUNTRY_CODES.put("EH", "Western Sahara");
			COUNTRY_CODES.put("ER", "Eritrea");
			COUNTRY_CODES.put("ES", "Spain");
			COUNTRY_CODES.put("ET", "Ethiopia");
			COUNTRY_CODES.put("EU", "Europe");
			COUNTRY_CODES.put("FI", "Finland");
			COUNTRY_CODES.put("FJ", "Fiji");
			COUNTRY_CODES.put("FK", "Falkland Islands (Malvinas)");
			COUNTRY_CODES.put("FM", "Micronesia, Federated States of");
			COUNTRY_CODES.put("FO", "Faroe Islands");
			COUNTRY_CODES.put("FR", "France");
			COUNTRY_CODES.put("GA", "Gabon");
			COUNTRY_CODES.put("GB", "United Kingdom");
			COUNTRY_CODES.put("GD", "Grenada");
			COUNTRY_CODES.put("GE", "Georgia");
			COUNTRY_CODES.put("GF", "French Guiana");
			COUNTRY_CODES.put("GG", "Guernsey");
			COUNTRY_CODES.put("GH", "Ghana");
			COUNTRY_CODES.put("GI", "Gibraltar");
			COUNTRY_CODES.put("GL", "Greenland");
			COUNTRY_CODES.put("GM", "Gambia");
			COUNTRY_CODES.put("GN", "Guinea");
			COUNTRY_CODES.put("GP", "Guadeloupe");
			COUNTRY_CODES.put("GQ", "Equatorial Guinea");
			COUNTRY_CODES.put("GR", "Greece");
			COUNTRY_CODES.put("GS", "South Georgia and the South Sandwich Islands");
			COUNTRY_CODES.put("GT", "Guatemala");
			COUNTRY_CODES.put("GU", "Guam");
			COUNTRY_CODES.put("GW", "Guinea-Bissau");
			COUNTRY_CODES.put("GY", "Guyana");
			COUNTRY_CODES.put("HK", "Hong Kong");
			COUNTRY_CODES.put("HM", "Heard Island and McDonald Islands");
			COUNTRY_CODES.put("HN", "Honduras");
			COUNTRY_CODES.put("HR", "Croatia");
			COUNTRY_CODES.put("HT", "Haiti");
			COUNTRY_CODES.put("HU", "Hungary");
			COUNTRY_CODES.put("ID", "Indonesia");
			COUNTRY_CODES.put("IE", "Ireland");
			COUNTRY_CODES.put("IL", "Israel");
			COUNTRY_CODES.put("IM", "Isle of Man");
			COUNTRY_CODES.put("IN", "India");
			COUNTRY_CODES.put("IO", "British Indian Ocean Territory");
			COUNTRY_CODES.put("IQ", "Iraq");
			COUNTRY_CODES.put("IR", "Iran, Islamic Republic of");
			COUNTRY_CODES.put("IS", "Iceland");
			COUNTRY_CODES.put("IT", "Italy");
			COUNTRY_CODES.put("JE", "Jersey");
			COUNTRY_CODES.put("JM", "Jamaica");
			COUNTRY_CODES.put("JO", "Jordan");
			COUNTRY_CODES.put("JP", "Japan");
			COUNTRY_CODES.put("KE", "Kenya");
			COUNTRY_CODES.put("KG", "Kyrgyzstan");
			COUNTRY_CODES.put("KH", "Cambodia");
			COUNTRY_CODES.put("KI", "Kiribati");
			COUNTRY_CODES.put("KM", "Comoros");
			COUNTRY_CODES.put("KN", "Saint Kitts and Nevis");
			COUNTRY_CODES.put("KP", "Korea, Democratic People's Republic of");
			COUNTRY_CODES.put("KR", "Korea, Republic of");
			COUNTRY_CODES.put("KW", "Kuwait");
			COUNTRY_CODES.put("KY", "Cayman Islands");
			COUNTRY_CODES.put("KZ", "Kazakhstan");
			COUNTRY_CODES.put("LA", "Lao People's Democratic Republic");
			COUNTRY_CODES.put("LB", "Lebanon");
			COUNTRY_CODES.put("LC", "Saint Lucia");
			COUNTRY_CODES.put("LI", "Liechtenstein");
			COUNTRY_CODES.put("LK", "Sri Lanka");
			COUNTRY_CODES.put("LR", "Liberia");
			COUNTRY_CODES.put("LS", "Lesotho");
			COUNTRY_CODES.put("LT", "Lithuania");
			COUNTRY_CODES.put("LU", "Luxembourg");
			COUNTRY_CODES.put("LV", "Latvia");
			COUNTRY_CODES.put("LY", "Libyan Arab Jamahiriya");
			COUNTRY_CODES.put("MA", "Morocco");
			COUNTRY_CODES.put("MC", "Monaco");
			COUNTRY_CODES.put("MD", "Moldova, Republic of");
			COUNTRY_CODES.put("ME", "Montenegro");
			COUNTRY_CODES.put("MF", "Saint Martin");
			COUNTRY_CODES.put("MG", "Madagascar");
			COUNTRY_CODES.put("MH", "Marshall Islands");
			COUNTRY_CODES.put("MK", "Macedonia");
			COUNTRY_CODES.put("ML", "Mali");
			COUNTRY_CODES.put("MM", "Myanmar");
			COUNTRY_CODES.put("MN", "Mongolia");
			COUNTRY_CODES.put("MO", "Macao");
			COUNTRY_CODES.put("MP", "Northern Mariana Islands");
			COUNTRY_CODES.put("MQ", "Martinique");
			COUNTRY_CODES.put("MR", "Mauritania");
			COUNTRY_CODES.put("MS", "Montserrat");
			COUNTRY_CODES.put("MT", "Malta");
			COUNTRY_CODES.put("MU", "Mauritius");
			COUNTRY_CODES.put("MV", "Maldives");
			COUNTRY_CODES.put("MW", "Malawi");
			COUNTRY_CODES.put("MX", "Mexico");
			COUNTRY_CODES.put("MY", "Malaysia");
			COUNTRY_CODES.put("MZ", "Mozambique");
			COUNTRY_CODES.put("NA", "Namibia");
			COUNTRY_CODES.put("NC", "New Caledonia");
			COUNTRY_CODES.put("NE", "Niger");
			COUNTRY_CODES.put("NF", "Norfolk Island");
			COUNTRY_CODES.put("NG", "Nigeria");
			COUNTRY_CODES.put("NI", "Nicaragua");
			COUNTRY_CODES.put("NL", "Netherlands");
			COUNTRY_CODES.put("NO", "Norway");
			COUNTRY_CODES.put("NP", "Nepal");
			COUNTRY_CODES.put("NR", "Nauru");
			COUNTRY_CODES.put("NU", "Niue");
			COUNTRY_CODES.put("NZ", "New Zealand");
			COUNTRY_CODES.put("OM", "Oman");
			COUNTRY_CODES.put("PA", "Panama");
			COUNTRY_CODES.put("PE", "Peru");
			COUNTRY_CODES.put("PF", "French Polynesia");
			COUNTRY_CODES.put("PG", "Papua New Guinea");
			COUNTRY_CODES.put("PH", "Philippines");
			COUNTRY_CODES.put("PK", "Pakistan");
			COUNTRY_CODES.put("PL", "Poland");
			COUNTRY_CODES.put("PM", "Saint Pierre and Miquelon");
			COUNTRY_CODES.put("PN", "Pitcairn");
			COUNTRY_CODES.put("PR", "Puerto Rico");
			COUNTRY_CODES.put("PS", "Palestinian Territory");
			COUNTRY_CODES.put("PT", "Portugal");
			COUNTRY_CODES.put("PW", "Palau");
			COUNTRY_CODES.put("PY", "Paraguay");
			COUNTRY_CODES.put("QA", "Qatar");
			COUNTRY_CODES.put("RE", "Reunion");
			COUNTRY_CODES.put("RO", "Romania");
			COUNTRY_CODES.put("RS", "Serbia");
			COUNTRY_CODES.put("RU", "Russian Federation");
			COUNTRY_CODES.put("RW", "Rwanda");
			COUNTRY_CODES.put("SA", "Saudi Arabia");
			COUNTRY_CODES.put("SB", "Solomon Islands");
			COUNTRY_CODES.put("SC", "Seychelles");
			COUNTRY_CODES.put("SD", "Sudan");
			COUNTRY_CODES.put("SE", "Sweden");
			COUNTRY_CODES.put("SG", "Singapore");
			COUNTRY_CODES.put("SH", "Saint Helena");
			COUNTRY_CODES.put("SI", "Slovenia");
			COUNTRY_CODES.put("SJ", "Svalbard and Jan Mayen");
			COUNTRY_CODES.put("SK", "Slovakia");
			COUNTRY_CODES.put("SL", "Sierra Leone");
			COUNTRY_CODES.put("SM", "San Marino");
			COUNTRY_CODES.put("SN", "Senegal");
			COUNTRY_CODES.put("SO", "Somalia");
			COUNTRY_CODES.put("SR", "Suriname");
			COUNTRY_CODES.put("SS", "South Sudan");
			COUNTRY_CODES.put("ST", "Sao Tome and Principe");
			COUNTRY_CODES.put("SV", "El Salvador");
			COUNTRY_CODES.put("SX", "Sint Maarten");
			COUNTRY_CODES.put("SY", "Syrian Arab Republic");
			COUNTRY_CODES.put("SZ", "Swaziland");
			COUNTRY_CODES.put("TC", "Turks and Caicos Islands");
			COUNTRY_CODES.put("TD", "Chad");
			COUNTRY_CODES.put("TF", "French Southern Territories");
			COUNTRY_CODES.put("TG", "Togo");
			COUNTRY_CODES.put("TH", "Thailand");
			COUNTRY_CODES.put("TJ", "Tajikistan");
			COUNTRY_CODES.put("TK", "Tokelau");
			COUNTRY_CODES.put("TL", "Timor-Leste");
			COUNTRY_CODES.put("TM", "Turkmenistan");
			COUNTRY_CODES.put("TN", "Tunisia");
			COUNTRY_CODES.put("TO", "Tonga");
			COUNTRY_CODES.put("TR", "Turkey");
			COUNTRY_CODES.put("TT", "Trinidad and Tobago");
			COUNTRY_CODES.put("TV", "Tuvalu");
			COUNTRY_CODES.put("TW", "Taiwan");
			COUNTRY_CODES.put("TZ", "Tanzania, United Republic of");
			COUNTRY_CODES.put("UA", "Ukraine");
			COUNTRY_CODES.put("UG", "Uganda");
			COUNTRY_CODES.put("UM", "United States Minor Outlying Islands");
			COUNTRY_CODES.put("US", "United States");
			COUNTRY_CODES.put("UY", "Uruguay");
			COUNTRY_CODES.put("UZ", "Uzbekistan");
			COUNTRY_CODES.put("VA", "Holy See (Vatican City State)");
			COUNTRY_CODES.put("VC", "Saint Vincent and the Grenadines");
			COUNTRY_CODES.put("VE", "Venezuela");
			COUNTRY_CODES.put("VG", "Virgin Islands, British");
			COUNTRY_CODES.put("VI", "Virgin Islands, U.S.");
			COUNTRY_CODES.put("VN", "Vietnam");
			COUNTRY_CODES.put("VU", "Vanuatu");
			COUNTRY_CODES.put("WF", "Wallis and Futuna");
			COUNTRY_CODES.put("WS", "Samoa");
			COUNTRY_CODES.put("YE", "Yemen");
			COUNTRY_CODES.put("YT", "Mayotte");
			COUNTRY_CODES.put("ZA", "South Africa");
			COUNTRY_CODES.put("ZM", "Zambia");
			COUNTRY_CODES.put("ZW", "Zimbabwe");
		}

		return COUNTRY_CODES;
	}

	public static String format(String value, String modifier) {
		if (value == null || modifier == null) {
			return "";
		}

		try {
			if (MODIFICATOR_LOWERCASE.equals(modifier)) {
				return value.toLowerCase();
			} else if (MODIFICATOR_UPPERCASE.equals(modifier)) {
				return value.toUpperCase();
			} else if (MODIFICATOR_CAPITALIZE_ALL_FIRST.equals(modifier)) {
				return capitalizeAll(value);
			} else if (MODIFICATOR_CAPITALIZE_FIRST.equals(modifier)) {
				return capitalizeFirst(value);
			} else if (MODIFICATOR_CENT.equals(modifier)) {
				return toCent(value);
			} else if (MODIFICATOR_DOLLAR.equals(modifier)) {
				return toDollar(value);
			} else if (MODIFICATOR_COMMA.equals(modifier)) {
				return toComma(value);
			} else if (MODIFICATOR_EURO.equals(modifier)) {
				return toEuro(value);
			} else if (MODIFICATOR_JPY.equals(modifier)) {
				return toJpy(value);
			} else if (MODIFICATOR_LIRA.equals(modifier)) {
				return toLira(value);
			} else if (MODIFICATOR_DATE1.equals(modifier)) {
				return toDate1(value);
			} else if (MODIFICATOR_DATE2.equals(modifier)) {
				return toDate2(value);
			} else if (MODIFICATOR_DATE3.equals(modifier)) {
				return toDate3(value);
			} else if (MODIFICATOR_DATE4.equals(modifier)) {
				return toDate4(value);
			} else if (MODIFICATOR_DATE5.equals(modifier)) {
				return toDate5(value);
			} else if (MODIFICATOR_DATE6.equals(modifier)) {
				return toDate6(value);
			} else if (MODIFICATOR_DATE7.equals(modifier)) {
				return toDate7(value);
			} else if (MODIFICATOR_DATE8.equals(modifier)) {
				return toDate8(value);
			}
		} catch (Exception e) {
			PWLog.exception(e);
			return value;
		}

		// Do not modify by default
		return value;
	}

	// City and Country tags are returned in unappropriate format in getTags response:
	// Country : in
	// City : in, trivandrum
	//
	// Warning: thread unsafe
	public static void convertGeoTags(Map<String, Object> tags) {
		try {
			if (tags.containsKey("Country")) {
				String country = tags.get("Country").toString();
				country = convertCountryTag(country);
				if (country != null) {
					tags.put("Country", country);
				} else {
					tags.remove("Country");
				}
			}

			if (tags.containsKey("City")) {
				String city = tags.get("City").toString();
				tags.put("City", convertCityTag(city));

			}
		} catch (Exception e) {
			PWLog.error("Failed converting geoTags", e);
		}
	}

	private static String convertCountryTag(String country) {
		country = country.toUpperCase();
		Map<String, String> codes = getCountyCodes();
		if (codes.containsKey(country)) {
			return codes.get(country);
		}

		return null;
	}

	private static String convertCityTag(String city) {
		String[] components = city.split(", ");
		return components[components.length - 1];
	}

	private static String capitalizeFirst(String string) {
		if (string.length() == 0) {
			return "";
		}

		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

	private static String capitalizeAll(String string) {
		char[] chars = string.toCharArray();
		boolean found = true; // capitalize first
		for (int i = 0; i < chars.length; i++) {
			if (found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
			}

			found = Character.isWhitespace(chars[i]); // capitalize after whitespace
		}
		return String.valueOf(chars);
	}

	private static String toCent(String string) {
		// expand
		if (string.length() == 0) {
			return "$.00";
		} else if (string.length() == 1) {
			string = "0" + string;
		}

		String cents = string.substring(string.length() - 2, string.length());
		String dollars = string.substring(0, string.length() - 2);
		return "$" + dollars + "." + cents;
	}

	private static String toDollar(String string) {
		if (string.length() == 0) {
			return "$0";
		}

		return "$" + toComma(string);
	}

	private static String toEuro(String string) {
		if (string.length() == 0) {
			return "€0";
		}

		return "€" + toComma(string);
	}

	private static String toJpy(String string) {
		if (string.length() == 0) {
			return "¥0";
		}

		return "¥" + toComma(string);
	}

	private static String toLira(String string) {
		if (string.length() == 0) {
			return "₤0";
		}

		return "₤" + toComma(string);
	}

	private static String toComma(String string) {
		if (string.length() == 0) {
			return "";
		}

		String result = "";

		int left = string.length();
		while (left > 0) {
			left = left - 3;
			result = string.substring(Math.max(left, 0), left + 3) + "," + result;
		}

		return result.substring(0, result.length() - 1);
	}

	private static String toDate1(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("MMM-dd-yy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate2(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate3(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("MMM dd yy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate4(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate5(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("EEEE");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate6(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("MMM dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate7(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("hh:mm");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}

	private static String toDate8(String date) {
		Long dateL = Long.parseLong(date);
		Date dateValue = new Date(dateL.longValue() * 1000L);
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy hh:mm");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
		return dateFormat.format(dateValue);
	}
}
