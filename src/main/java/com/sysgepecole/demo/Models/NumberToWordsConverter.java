package com.sysgepecole.demo.Models;

public class NumberToWordsConverter {

	private static final String[] units = {
	        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", 
	        "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"
	    };
	    private static final String[] tens = {
	        "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"
	    };

	    public static String convert(final int n) {
	        if (n < 0) {
	            return "moins " + convert(-n);
	        }

	        if (n < 20) {
	            return units[n];
	        }

	        if (n < 100) {
	            return tens[n / 10] + ((n % 10 != 0) ? " " + units[n % 10] : "");
	        }

	        if (n < 1000) {
	            return units[n / 100] + " cent" + ((n % 100 != 0) ? " " + convert(n % 100) : "");
	        }

	        if (n < 1000000) {
	            return convert(n / 1000) + " mille" + ((n % 1000 != 0) ? " " + convert(n % 1000) : "");
	        }

	        return convert(n / 1000000) + " million" + ((n % 1000000 != 0) ? " " + convert(n % 1000000) : "");
	    }
}
