#!/usr/bin/env perl
use strict;
use warnings;
use lib 'lib';
use KorAP::VirtualCorpus::Group;

# 2020-05-20
#   Preliminary support for C2 def-files.
# 2020-05-29
#   Introduce optimizable object system.

our @ARGV;

unless (@ARGV) {
  print <<'HELP';
Convert a line-separated list of corpus sigles, doc sigles or
text sigles into a virtual corpus query.

  $ perl list2vc.pl my_vc.txt | gzip -vc > my_vc.jsonld.gz
  $ cat my_vc.txt | perl list2vc.pl - | gzip -vc > my_vc.jsonld.gz

HELP
exit 0;
};


# Shorten long strings for logging
sub _shorten ($) {
  my $line = shift;
  if (length($line) < 20) {
    return $line;
  }
  else {
    return substr($line,0,17) . '...';
  };
};


my $fh;
if ($ARGV[0] eq '-') {
  $fh = *STDIN;
} elsif (!open($fh, '<' . $ARGV[0])) {
  warn $ARGV[0] . " can't be opened";
  exit(0);
};

# Initial VC group
my $vc;

# Create an intensional and an extensional VC
my $vc_ext = KorAP::VirtualCorpus::Group->new;
my $vc_int = KorAP::VirtualCorpus::Group->new;

# Load ext initially
$$vc = $vc_ext;

# Collect all virtual corpora
my %all_vcs;

my $frozen = 0;

# Iterate over the whole list
while (!eof $fh) {
  my $line = readline($fh);
  chomp $line;


  # Skip empty lines
  if (!$line || length($line) == 0 || $line =~ /^[\s\t\n]*$/) {
    # empty
    next;
  };

  my ($key, $value, $desc);

  # Line-Type: <e>c</a>
  if ($line =~ /^\s*<([^>]+)>\s*([^<]*)\s*<\/\1>\s*$/) {
    $key = $1;
    $value = $2 // undef;
  }

  # Line-Type: <e>c
  elsif($line =~ /^\s*<([^>]+)>\s*([^<]+)\s*$/) {
    $key = $1;
    $value = $2;
  }

  # Get text sigles
  elsif ($line =~ m!^(?:\w+\/){2}\w+$!) {
    $key = 'text';
    $value = $line;
  }

  # Get doc sigles
  elsif ($line =~ m!^(\w+\/\w+?)(?:\s.+?)?$!) {
    $key = 'doc';
    $value = $1;
  }

  # Get corpus sigles
  elsif ($line !~ m!(?:\/|\s)!) {
    $key = 'corpus';
    $value = $line;
  }

  # Not known
  else {
    warn _shorten($line) . q! isn't a valid VC definition!;
    next;
  };

  # Add text field
  if ($key eq 'text') {

    # Convert C2 sigle to KorAP form
    $value =~ s!^([^/]+?/[^\.]+?)\.(.+?)$!$1\/$2!;
    ${$vc}->union_field(textSigle => $value);
  }

  # Add doc field
  elsif ($key eq 'doc') {
    ${$vc}->union_field(docSigle => $value);
  }

  # Add corpus field
  elsif ($key eq 'corpus') {
    ${$vc}->union_field(corpusSigle => $value);
  }

  # Add corpus field
  elsif ($key eq 'cn') {
    # Korpussigle, z.B. 'F97 Frankfurter Allgemeine 1997'
    if ($value =~ m!^([^\/\s]+)(?:\s.+?)?$!) {
      ${$vc}->union_field(corpusSigle => $1);
    };
  }

  # Mark the vc as frozen
  # This means that an extended VC area is expected
  elsif ($key eq 'frozen') {
    $frozen = 1;
  }

  # Start/End intended VC area
  elsif ($key eq 'intended') {
    if ($value eq 'start') {
      $$vc = $vc_int;
    }
    elsif ($value ne 'end') {
      warn 'Unknown intension value ' . $value;
    };
  }

  # Start/End extended VC area
  elsif ($key eq 'extended') {
    if ($value eq 'start') {
      $$vc = $vc_ext;
    }
    elsif ($value ne 'end') {
      warn 'Unknown extension value ' . $value;
    };
  }

  # Set VC name
  elsif ($key eq 'name') {
    # "Name des virt. Korpus, der angezeigt wird.
    # Wird auch intern zur Korpusbildung referenziert, z.B. für <and>,
    # <add>, <sub>"

    # No global name defined yet
    if ($$vc && !$$vc->name) {
      $vc_ext->name($value);
      $vc_int->name($value);
      next;
    };

    ${$vc} = KorAP::VirtualCorpus::Group->new;
    ${$vc}->name($value);
  }

  # End VC def
  elsif ($key eq 'end') {
    $all_vcs{${$vc}->name} = $$vc;
    # $vc = undef;
  }

  # Add VC definition
  elsif ($key eq 'add') {
    unless (defined $all_vcs{$value}) {
      #       warn 'VC ' . $value . ' not defined';
      # exit(1);
      next;
    };

    $$vc->union($all_vcs{$value}->clone->to_koral);
  }

  # AND definition
  elsif ($key eq 'and') {
    unless (defined $all_vcs{$value}) {
      #       warn 'VC ' . $value . ' not defined';
      # exit(1);
      next;
    };

    $$vc->joint($all_vcs{$value}->clone->to_koral);
  }

  # Source of the corpus
  elsif ($key eq 'ql') {
    # Quellenname, z.B. "Neue Zürcher Zeitung"
    $$vc->union_field(corpusTitle => $value);
  }

  elsif ($key eq 'sub') {
    # "Sub" is the difference - it is the "and not" operation.
    warn $key . ' is not yet supported';
  }

  elsif ($key eq 'co') {
    # Country,	z.B. DE für Text in Deutschland erschienen
    warn $key . ' is not yet supported';
  }

  elsif ($key eq 'tl') {
    # Textlength, Bereich von Texten der angegebenen Länge [in Anz. Wörtern]
    warn $key . ' is not yet supported';
  }

  elsif ($key eq 'ts') {
    # Textsorte, 	z.B. "Bericht"
    warn $key . ' is not yet supported';
  }

  elsif ($key eq 'th') {
    # Thema, z.B. "Sport - Fußball"
    warn $key . ' is not yet supported';
  }

  elsif ($key eq 'red') {
    # Reduktionsfaktor
    # Wert zw. 1-99%: virt. Korpus wird auf diesen Wert
    # reduziert. Modus: feste Reduzierung, nicht variabel.
    warn $key . ' is not yet supported';
  }

  elsif ($key eq 'thprob') {
    # ThemaProbability
    # Wert, der für <th>Thema verwendet wird um zu bestimmen, ab welchem
    # Zuverläßigkeitswert ein Thema übernommen wird
  }


  # Add reduction value as a comment
  elsif ($key eq 'redabs') {
    # "red. Anz. Texte
    # absoluter Wert der durch Reduktion zu erzielende Anzahl Texte"
    $$vc->comment('redabs:' . $value);
    warn $key . ' is not yet supported';
  }

  # Add reduction value as a comment
  elsif ($key eq 'date') {
    # Supports two pattern schemes:
    # m1=Year1/Month1 bis Year2/Month2
    #   Datumsbereich Schema 1: z.B. "2000/01 bis 2010/12"

    # Schema 1
    if ($value =~ m!^(?:m1\s*=\s*)?\s*(\d+)\/(\d+) bis (\d+)\/(\d+)\s*$!s) {
      my ($y1, $m1, $y2, $m2) = ($1, $2, $3, $4);
      if ($m1 < 10) {
        $m1 = '0' . (0+$m1);
      };
      if ($m2 < 10) {
        $m2 = '0' . (0+$m2);
      };
      $$vc->from($y1, $m1);
      $$vc->to($y2, $m2);
    }

    # Scheme 2
    elsif ($value =~ m!^\s*\d{4}-\d{4}\s+und\s+\d{1,2}-\d{1,2}\s*$!) {
      # m2=Year1-Year2 und Month1-Month2
      #   Datumsbereich Schema 2: z.B. "1990-2000 und 06-06"

      warn 'Second date scheme not yet supported!'
    }

    else {
      warn 'Unknown date scheme ' . $value;
    };
  }

  # Unknown
  else {
    warn $key . ' is an unknown field';
  };
};

close($fh);

# Stringify current (extended?) virtual corpus
print $$vc->to_string;
