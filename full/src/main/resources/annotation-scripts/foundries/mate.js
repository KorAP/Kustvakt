define(["hint/foundries","hint/foundries/stts"], function (ah, sttsArray) {
//  var mateSttsArray = sttsArray.slice(0);
//  mateSttsArray.push(
//    ["<root-POS>","<root-POS>","Root Part of Speech"]
//  );

  var ah = KorAP.annotationHelper = KorAP.annotationHelper || { "-" : [] };

  ah["-"].push(
    ["Mate", "mate/", "Lemma, Morphology, Part-of-Speech"]
  );

  ah["mate/"] = [
    // Inactive: "d" : ["d=", "Dependency"],
    ["Lemma", "l="],
    ["Morphology", "m="],
    ["Part-of-Speech", "p="]
  ];

  // Inactive: mate/d=
  ah["mate/p="] = [
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
    ["<root-POS>","<root-POS>","Root Part of Speech"]
  ];

  ah["mate/m="] = [
    ["Case", "case:"],
    ["Degree", "degree:"],
    ["Gender", "gender:"],
    ["Mood", "mood:"],
    ["Number", "number:"],
    ["Person", "person:"],
    ["Tense","tense:"],
    ["No type", "<no-type> "]
  ];

  ah["mate/m=case:"] = [
    ["acc", "acc ", "Accusative"],
    ["dat","dat ", "Dative"],
    ["gen", "gen ","Genitive"],
    ["nom","nom ", "Nominative"],
    ["*","* ", "Undefined"]
  ];

  ah["mate/m=degree:"] = [
    ["comp","comp ", "Comparative"],
    ["pos","pos ", "Positive"],
    ["sup","sup ", "Superative"]
  ];

  ah["mate/m=gender:"] = [
    ["fem", "fem ", "Feminium"],
    ["masc", "masc ", "Masculinum"],
    ["neut","neut ", "Neuter"],
    ["*","* ","Undefined"]
  ];

  ah["mate/m=mood:"] = [
    ["imp","imp ", "Imperative"],
    ["ind","ind ", "Indicative"],
    ["subj","subj ", "Subjunctive"]
  ];

  ah["mate/m=number:"] = [
    ["pl","pl ","Plural"],
    ["sg","sg ","Singular"],
    ["*","* ","Undefined"]
  ];

  ah["mate/m=person:"] = [
    ["1","1 ", "First Person"],
    ["2","2 ", "Second Person"],
    ["3","3 ", "Third Person"]
  ];
  ah["mate/m=tense:"] = [
    ["past","past ", "Past"],
    ["pres","pres ", "Present"]
  ];
});
