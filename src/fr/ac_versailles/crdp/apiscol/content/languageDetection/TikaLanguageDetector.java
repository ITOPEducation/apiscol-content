package fr.ac_versailles.crdp.apiscol.content.languageDetection;

import org.apache.tika.language.LanguageIdentifier;

public class TikaLanguageDetector extends AbstractLanguageDetector {

	@Override
	public String detectLanguage(String texte) {
		String iso639_1 = new LanguageIdentifier(texte).getLanguage();
		return Iso639Converter.getIso639_2(iso639_1);
	}

}
