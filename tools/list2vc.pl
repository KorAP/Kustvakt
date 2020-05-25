#!/usr/bin/env perl
package KorAP::VirtualCorpus;
use strict;
use warnings;


# Get or set name of the VC
sub name {
  my $self = shift;
  unless (@_) {
    return $self->{name};
  };
  $self->{name} = shift;
  return $self;
};


# Comment
sub comment {
  my $self = shift;
  unless (@_) {
    return $self->{comment};
  };
  $self->{comment} //= [];

  push @{$self->{comment}}, shift;
  return $self;
};


# Quote utility function
sub quote {
  shift;
  my $str = shift;
  $str =~ s/(["\\])/\\$1/g;
  return qq{"$str"};
};


# Escaped quote utility function
sub equote {
  shift;
  my $str = shift;
  $str =~ s/(["\\])/\\$1/g;
  $str =~ s/(["\\])/\\$1/g;
  return '\\"' . $str . '\\"';
};


sub _commentparam_to_string {
  my $self = shift;
  my $comment = $self->_comment_to_string;
  if ($comment) {
    return qq!,"comment":"$comment"!;
  };
  return '';
};


sub _comment_to_string {
  my $self = shift;
  if (!$self->name && !$self->comment) {
    return '';
  };

  my $json = '';
  $json .= 'name:' . $self->equote($self->name) if $self->name;
  if ($self->name && $self->comment) {
    $json .= ','
  };
  $json .= join(',', @{$self->{comment}}) if $self->{comment};

  return $json;
};


# Stringify globally
sub to_string {
  my $self = shift;
  ## Create collection object

  my $json = '{';
  $json .= '"@context":"http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",';
  $json .= '"collection":';
  $json .= $self->_to_fragment;
  # Set at the end, when all comments are done
  $json .= $self->_commentparam_to_string;
  return $json .= '}';
};


package KorAP::VirtualCorpus::Group;
use strict;
use warnings;
use base 'KorAP::VirtualCorpus';


# Construct a new VC group
sub new {
  my $class = shift;
  bless {
    with => [],
    with_fields => {},
    without => [],
    without_fields => {},
  }, $class;
};


# Define an operand to be "or"ed
sub with {
  my $self = shift;
  push @{$self->{with}}, shift;
};


# Define a field that should be "or"ed
sub with_field {
  my $self = shift;
  my $field = shift;
  push @{$self->{with_fields}->{$field}}, shift;
};

# Define an operand to be "and"ed
sub without {
  my $self = shift;
  push @{$self->{without}}, shift;
};


# Define a field that should be "and"ed
sub without_field {
  my $self = shift;
  my $field = shift;
  push @{$self->{without_fields}->{$field}}, shift;
};


# VC contains only with fields
sub only_with_fields {
  my $self = shift;

  if (keys %{$self->{without_fields}} || @{$self->{with}} || @{$self->{without}}) {
    return 0;
  };

  return 1;
};


# Create a document vector field
sub _doc_vec {
  my $field = shift;
  my $vec = shift;
  my $json = '{';
  $json .= '"@type":"koral:doc",';
  $json .= '"key":"' . $field . '",';
  $json .= '"match":"match:eq",';
  $json .= '"value":[';
  $json .= join ',', map { '"' . $_ . '"' } @$vec;
  $json .=  ']';
  $json .= '},';
  return $json;
}


# Stringify fragment
sub _to_fragment {
  my $self = shift;

  my $json = '{';
  $json .= '"@type":"koral:docGroup",';

  # Make the outer group "and"
  if (keys %{$self->{without_fields}}) {
    $json .= '"operation":"operation:and",';
    $json .= '"operands":[';

    foreach my $field (sort keys %{$self->{without_fields}}) {
      unless (@{$self->{without_fields}->{$field}}) {
        next;
      };
      $json .= _doc_vec($field, $self->{without_fields}->{$field});
    };

    # Remove the last comma
    chop $json;

    $json .= ']';
  }

  elsif (keys %{$self->{with_fields}} || @{$self->{with}}) {
    $json .= '"operation":"operation:or",';

    $json .= '"operands":[';

    # Flatten embedded "or"-VCs
    foreach my $op (@{$self->{with}}) {

      # The embedded VC has only extending fields
      if ($op->only_with_fields) {

        $self->comment('embed:[' . $op->_comment_to_string . ']');

        foreach my $k (keys %{$op->{with_fields}}) {
          foreach my $v (@{$op->{with_fields}->{$k}}) {
            $self->with_field($k, $v);
          };
        };
      }

      # Embed complex VC
      else {
        $json .= $op->_to_fragment . ',';
      };
    };

    foreach my $field (sort keys %{$self->{with_fields}}) {
      unless (@{$self->{with_fields}->{$field}}) {
        next;
      };
      $json .= _doc_vec($field, $self->{with_fields}->{$field});
    };

    # Remove the last comma
    chop $json;

    $json .= ']';
  }

  # No operands in the group
  else {
    # Remove the last comma after the comment
    chop $json;
  };

  # Set at the end, when all comments are done
  $json .= $self->_commentparam_to_string;
  return $json . '}';
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
    ${$vc}->with_field(textSigle => $value);
  }

  # Add doc field
  elsif ($key eq 'doc') {
    ${$vc}->with_field(docSigle => $value);
  }

  # Add corpus field
  elsif ($key eq 'corpus') {
    ${$vc}->with_field(corpusSigle => $value);
  }

  # Add corpus field
  elsif ($key eq 'cn') {
    # Korpussigle, z.B. 'F97 Frankfurter Allgemeine 1997'
    if ($value =~ m!^([^\/\s]+)(?:\s.+?)?$!) {
      ${$vc}->with_field(corpusSigle => $1);
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

    $$vc->with($all_vcs{$value});
  }

  # Add reduction value as a comment
  elsif ($key eq 'redabs') {
    # "red. Anz. Texte
    # absoluter Wert der durch Reduktion zu erzielende Anzahl Texte"
    $$vc->comment('redabs:' . $value);
  }

  # Unknown
  else {
    # warn $key . ' is an unknown field';
  };
};

close($fh);

# Stringify current (extended?) virtual corpus
print $$vc->to_string;
