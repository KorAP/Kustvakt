define(["hint/foundries"], function (ah) {
  ah["-"].push(
    ["Base Annotation", "base/", "Structure"]
  );
  
  ah["base/"] = [
	["Structure", "s="]
  ];
  
  ah["base/s="] = [
    ["s", "s", "Sentence"],
    ["p", "p", "Paragraph"],
    ["t", "t", "Text"]
  ];
});
