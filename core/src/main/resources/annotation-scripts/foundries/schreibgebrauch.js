define(["hint/foundries","hint/foundries/stts"], function (ah, sttsArray) {
//  var sgbrSttsArray = sttsArray.slice(0);

  // Push specific information for Schreibgebrauch
//  sgbrSttsArray.push(
//    ["NNE", "NNE", "Normal Nomina with Named Entity"],
//    ["ADVART","ADVART",   "Adverb with Article"],
//    ["EMOASC","EMOASC",   "ASCII emoticon"],
//    ["EMOIMG","EMOIMG",   "Graphic emoticon"],
//    ["ERRTOK","ERRTOK",   "Tokenisation Error"],
//    ["HST",     "HST",      "Hashtag"],
//    ["KOUSPPER","KOUSPPER", "Subordinating Conjunction (with Sentence) with Personal Pronoun"],
//    ["ONO",     "ONO",      "Onomatopoeia"],
//    ["PPERPPER","PPERPPER", "Personal Pronoun with Personal Pronoun"],
//    ["URL",     "URL",      "Uniform Resource Locator"],
//    ["VAPPER",  "VAPPER",   "Finite Auxiliary Verb with Personal Pronoun"],
//    ["VMPPER",  "VMPPER",   "Fintite Modal Verb with Personal Pronoun"],
//    ["VVPPER",  "VVPPER",   "Finite Full Verb with Personal Pronoun"],
//    ["AW", "AW", "Interaction Word"],
//    ["ADR", "ADR", "Addressing Term"],
//    ["AWIND", "AWIND", "Punctuation Indicating Addressing Term"],
//    ["ERRAW","ERRAW", "Part of Erroneously Separated Compound"]
//    /*
//      As KorAP currently doesn't support these tags, they could also be ommited
//      ["_KOMMA", "_KOMMA", "Comma"],
//      ["_SONST", "_SONST", "Intrasentential Punctuation Mark"],
//      ["_ENDE", "_ENDE", "Punctuation Mark at the end of the Sentence"]
//    */
//  );

  // Sort by tag
  sgbrSttsArray.sort(function (a,b) { return a[0].localeCompare(b[0]) });

  
  ah["-"].push(
    ["Schreibgebrauch", "sgbr/", "Lemma, Lemma Variants, Part-of-Speech"]
  );

  ah["sgbr/"] = [
    ["Lemma", "l="],
    ["Lemma Variants", "lv="],
    ["Part-of-Speech", "p="]
  ];

//  ah["sgbr/p="] = sgbrSttsArray;
  ah["sgbr/p="] = [
    ["ADJA","ADJA ", "Attributive Adjective"],
    ["ADJD","ADJD ", "Predicative Adjective"],
    ["ADV","ADV ", "Adverb"],
    ["APPO","APPO ", "Postposition"],
    ["APPR","APPR ", "Preposition"],
    ["APPRART","APPRART ", "Preposition with Determiner"],
    ["APZR","APZR ","Right Circumposition"],
    ["ART","ART ", "Determiner"],
    ["CARD","CARD ", "Cardinal Number"],
    ["FM","FM ", "Foreign Material"],
    ["ITJ","ITJ ", "Interjection"],
    ["KOKOM","KOKOM ", "Comparison Particle"],
    ["KON","KON ", "Coordinating Conjuncion"],
    ["KOUI","KOUI ", "Subordinating Conjunction with 'zu'"],
    ["KOUS","KOUS ", "Subordinating Conjunction with Sentence"],
    ["NE","NE ", "Named Entity"],
    ["NN","NN ", "Normal Nomina"],
    ["PAV", "PAV ", "Pronominal Adverb"],
    ["PDAT","PDAT ","Attributive Demonstrative Pronoun"],
    ["PDS","PDS ", "Substitutive Demonstrative Pronoun"],
    ["PIAT","PIAT ", "Attributive Indefinite Pronoun without Determiner"],
    ["PIDAT","PIDAT ", "Attributive Indefinite Pronoun with Determiner"],
    ["PIS","PIS ", "Substitutive Indefinite Pronoun"],
    ["PPER","PPER ", "Personal Pronoun"],
    ["PPOSAT","PPOSAT ", "Attributive Possessive Pronoun"],
    ["PPOSS","PPOSS ", "Substitutive Possessive Pronoun"],
    ["PRELAT","PRELAT ", "Attributive Relative Pronoun"],
    ["PRELS","PRELS ", "Substitutive Relative Pronoun"],
    ["PRF","PRF ", "Reflexive Pronoun"],
    ["PROAV","PROAV ", "Pronominal Adverb"],
    ["PTKA","PTKA ","Particle with Adjective"],
    ["PTKANT","PTKANT ", "Answering Particle"],
    ["PTKNEG","PTKNEG ", "Negation Particle"],
    ["PTKVZ","PTKVZ ", "Separated Verbal Particle"],
    ["PTKZU","PTKZU ", "'zu' Particle"],
    ["PWAT","PWAT ", "Attributive Interrogative Pronoun"],
    ["PWAV","PWAV ", "Adverbial Interrogative Pronoun"],
    ["PWS","PWS ", "Substitutive Interrogative Pronoun"],
    ["TRUNC","TRUNC ","Truncated"],
    ["VAFIN","VAFIN ", "Auxiliary Finite Verb"],
    ["VAIMP","VAIMP ", "Auxiliary Finite Imperative Verb"],
    ["VAINF","VAINF ", "Auxiliary Infinite Verb"],
    ["VAPP","VAPP ", "Auxiliary Perfect Participle"],
    ["VMFIN","VMFIN ", "Modal Finite Verb"],
    ["VMINF","VMINF ", "Modal Infinite Verb"],
    ["VMPP","VMPP ", "Modal Perfect Participle"],
    ["VVFIN","VVFIN ","Finite Verb"],
    ["VVIMP","VVIMP ", "Finite Imperative Verb"],
    ["VVINF","VVINF ", "Infinite Verb"],
    ["VVIZU","VVIZU ", "Infinite Verb with 'zu'"],
    ["VVPP","VVPP ", "Perfect Participle"],
    ["XY", "XY ", "Non-Word"],
    ["NNE", "NNE", "Normal Nomina with Named Entity"],
    ["ADVART","ADVART",   "Adverb with Article"],
    ["EMOASC","EMOASC",   "ASCII emoticon"],
    ["EMOIMG","EMOIMG",   "Graphic emoticon"],
    ["ERRTOK","ERRTOK",   "Tokenisation Error"],
    ["HST",     "HST",      "Hashtag"],
    ["KOUSPPER","KOUSPPER", "Subordinating Conjunction (with Sentence) with Personal Pronoun"],
    ["ONO",     "ONO",      "Onomatopoeia"],
    ["PPERPPER","PPERPPER", "Personal Pronoun with Personal Pronoun"],
    ["URL",     "URL",      "Uniform Resource Locator"],
    ["VAPPER",  "VAPPER",   "Finite Auxiliary Verb with Personal Pronoun"],
    ["VMPPER",  "VMPPER",   "Fintite Modal Verb with Personal Pronoun"],
    ["VVPPER",  "VVPPER",   "Finite Full Verb with Personal Pronoun"],
    ["AW", "AW", "Interaction Word"],
    ["ADR", "ADR", "Addressing Term"],
    ["AWIND", "AWIND", "Punctuation Indicating Addressing Term"],
    ["ERRAW","ERRAW", "Part of Erroneously Separated Compound"]
  ];
});
