#!/usr/bin/env perl
package KorAP::VirtualCorpus::Group;
use strict;
use warnings;

# Construct a new VC group
sub new {
  my $class = shift;
  bless {
    op => shift,
    fields => {}
  }, $class;
};


# Add field information to group
sub add_field {
  my $self = shift;
  my $field = shift;
  push @{$self->{fields}->{$field}}, shift;
};


# Stringify
sub to_string {
  my $self = shift;
  ## Create collection object
  my $json = '{';
  $json .= '"@context":"http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",';
  $json .= '"collection":{';

  unless (keys %{$self->{fields}}) {
    return $json . '}}';
  };

  $json .= '"@type":"koral:docGroup",';
  $json .= '"operation":"operation:' . $self->{op} . '",';
  $json .= '"operands":[';

  foreach my $field (sort keys %{$self->{fields}}) {
    unless (@{$self->{fields}->{$field}}) {
      next;
    };
    $json .= '{';
    $json .= '"@type":"koral:doc",';
    $json .= '"key":"' . $field . '",';
    $json .= '"match":"match:eq",';
    $json .= '"value":[';
    $json .= join ',', map { '"' . $_ . '"' } @{$self->{fields}->{$field}};
    $json .=  ']';
    $json .= '},';
  };

  # Remove the last comma
  chop $json;

  $json .= ']}}';
  return $json;
};


package main;
use strict;
use warnings;

# 2020-05-20
#   Preliminary support for C2 def-files.

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


# Create an intensional and an extensional VC
my $vc_ext = KorAP::VirtualCorpus::Group->new('or');
my $vc_int = KorAP::VirtualCorpus::Group->new('or');

# Initial VC group
my $vc = \$vc_ext;

my ($frozen) = 0;

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
    ${$vc}->add_field(textSigle => $value);
  }

  # Add doc field
  elsif ($key eq 'doc') {
    ${$vc}->add_field(docSigle => $value);
  }

  # Add corpus field
  elsif ($key eq 'corpus') {
    ${$vc}->add_field(corpusSigle => $value);
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
      warn 'Unknown extension value ' . $value;
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
};

close($fh);

# Stringify current (extended) virtual corpus
print $$vc->to_string;
