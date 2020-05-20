#!/usr/bin/env perl
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


sub shorten ($) {
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


my %data = (
  corpus => [],
  doc => [],
  text => []
);

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
  elsif ($line =~ m!^(?:[^\/\s]+\/){2}[^\/\s]+$!) {
    $key = 'text';
    $value = $line;
  }

  # Get doc sigles
  elsif ($line =~ m!^([^\/\s]+\/[^\/\s]+?)(?:\s.+?)?$!) {
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
    warn shorten($line) . q! isn't a valid sigle!;
    next;
  };

  if ($key eq 'text') {

    # Convert C2 sigle to KorAP form
    $value =~ s!^([^/]+?/[^\.]+?)\.(.+?)$!$1\/$2!;
    push @{$data{text}}, $value;
  }

  elsif ($key eq 'doc') {
    push @{$data{doc}}, $value;
  }

  elsif ($key eq 'corpus') {
    push @{$data{corpus}}, $value;
  };
};

# Create collection object
my $json = '{';
$json .= '"@context":"http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",';
$json .= '"collection":{';

unless (@{$data{corpus}} || @{$data{doc}} || @{$data{text}}) {
  $json .= '}}';
  close($fh);
  print $json;
  exit(0);
};

$json .= '"@type":"koral:docGroup",';
$json .= '"operation":"operation:or",';
$json .= '"operands":[';

foreach my $type (qw/corpus doc text/) {
  unless (@{$data{$type}}) {
    next;
  };
  $json .= '{';
  $json .= '"@type":"koral:doc",';
  $json .= '"key":"' . $type . 'Sigle",';
  $json .= '"match":"match:eq",';
  $json .= '"value":[';
  $json .= join ',', map { '"' . $_ . '"' } @{$data{$type}};
  $json .=  ']';
  $json .= '},';
};

# Remove the last comma
chop $json;

$json .= ']}}';

close($fh);

print $json;

