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
    union => [],
    union_fields => {},
    joint => [],
    joint_fields => {},
  }, $class;
};


# Clone the object
sub clone {
  my $self = shift;
  my $clone = {};

  if ($self->{union}) {
    $clone->{union} = [map { $_->clone } @{$self->{union}}];
  };

  if ($self->{joint}) {
    $clone->{joint} = [map { $_->clone } @{$self->{joint}}];
  };

  if ($self->{union_fields}) {
    $clone->{union_fields} = {%{$self->{union_fields}}};
  };

  if ($self->{joint_fields}) {
    $clone->{joint_fields} = {%{$self->{joint_fields}}};
  };

  bless $clone, __PACKAGE__;
};

# Define an operand to be "or"ed
sub union {
  my $self = shift;
  push @{$self->{union}}, shift;
};


# Define a field that should be "or"ed
sub union_field {
  my $self = shift;
  my $field = shift;
  push @{$self->{union_fields}->{$field}}, shift;
};

# Define an operand to be "and"ed
sub joint {
  my $self = shift;
  push @{$self->{joint}}, shift;
};


# Define a field that should be "and"ed
sub joint_field {
  my $self = shift;
  my $field = shift;
  push @{$self->{joint_fields}->{$field}}, shift;
};


# VC contains only union fields
sub only_union_fields {
  my $self = shift;

  if (keys %{$self->{joint_fields}} || @{$self->{union}} || @{$self->{joint}}) {
    return 0;
  };

  return 1;
};


# VC contains only joints
sub only_joint {
  my $self = shift;

  if (keys %{$self->{union_fields}} || @{$self->{union}}) {
    return 0;
  };

  return 1;
};


# Restrict VC by date
sub from {
  my $self = shift;
  my ($year, $month) = @_;
  my $doc = KorAP::VirtualCorpus::Doc->new('createDate')
    ->type('date')
    ->match('geq')
    ->value($year . '-' . $month);
  $self->joint($doc);
};


# Restrict VC by date
sub to {
  my $self = shift;
  my ($year, $month) = @_;
  my $doc = KorAP::VirtualCorpus::Doc->new('createDate')
    ->type('date')
    ->match('leq')
    ->value($year . '-' . $month);
  $self->joint($doc);
};


# Create a document vector field
sub _doc_vec {
  my $field = shift;
  my $vec = shift;

  if (@$vec == 0) {
    return '{}';
  };

  my $json = '{';
  $json .= '"@type":"koral:doc",';
  $json .= '"key":"' . $field . '",';
  $json .= '"match":"match:eq",';
  $json .= '"value":';
  if (@$vec == 1) {
    $json .= '"' . $vec->[0] . '"';
  } else {
    $json .= '[';
    $json .= join(',', map { '"' . $_ . '"' } @$vec);
    $json .=  ']';
  };
  $json .= '}';
  return $json;
}

# Serialize or-Operands
sub _to_fragment_or_ops {
  my $self = shift;
  my $json = '';

  if ($self->{union}) {
    foreach my $op (@{$self->{union}}) {

      # The embedded VC has only extending fields
      if ($op->only_union_fields) {

        $self->comment('embed:[' . $op->_comment_to_string . ']');

        foreach my $k (keys %{$op->{union_fields}}) {
          foreach my $v (@{$op->{union_fields}->{$k}}) {
            $self->union_field($k, $v);
          };
        };
      }

      # Embed complex VC
      else {
        $json .= $op->_to_fragment . ',';
      };
    };
  };

  if (keys %{$self->{union_fields}}) {
    foreach my $field (sort keys %{$self->{union_fields}}) {
      unless (@{$self->{union_fields}->{$field}}) {
        next;
      };
      $json .= _doc_vec($field, $self->{union_fields}->{$field}) . ',';
    };
  };

  return $json;
};


# Stringify fragment
sub _to_fragment {
  my $self = shift;

  my $json = '{';
  $json .= '"@type":"koral:docGroup",';

  # Make the outer group "and"
  if (keys %{$self->{joint_fields}} || @{$self->{joint}}) {
    $json .= '"operation":"operation:and",';
    $json .= '"operands":[';

    # Integrate embedded VCs
    foreach my $op (@{$self->{joint}}) {

#      # Embed joints
#      if ($op->only_joint) {
#        $self->comment('embed:[' . $op->_comment_to_string . ']');
#
#        foreach (@{$op->{joint}}) {
#          $json .= $op->_to_fragment . ',';
#        };
#
#        # TODO: Delete joint!
#
#        foreach my $k (keys %{$op->{joint_fields}}) {
#          foreach my $v (@{$op->{joint_fields}->{$k}}) {
#            $self->joint_field($k, $v);
#          };
#        };
#      }
#
#      else {
        $json .= $op->_to_fragment . ',';
#      };
    };

    foreach my $field (sort keys %{$self->{joint_fields}}) {
      unless (@{$self->{joint_fields}->{$field}}) {
        next;
      };
      $json .= _doc_vec($field, $self->{joint_fields}->{$field}) . ',';
    };

    if (keys %{$self->{union_fields}} || @{$self->{union}}) {
      $json .= '{';
      $json .= '"@type":"koral:docGroup",';
      $json .= '"operation":"operation:or",';
      $json .= '"operands":[';

      $json .= $self->_to_fragment_or_ops;

      chop $json;

      $json .= ']';
      $json .= '}';
      $json .= ',';
    };

    # Remove the last comma
    chop $json;

    $json .= ']';
  }

  elsif (keys %{$self->{union_fields}} || @{$self->{union}}) {
    $json .= '"operation":"operation:or",';
    $json .= '"operands":[';

    $json .= $self->_to_fragment_or_ops;

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

package KorAP::VirtualCorpus::Doc;
use strict;
use warnings;
use base 'KorAP::VirtualCorpus';

# Constructor
sub new {
  my $class = shift;
  bless {
    key => shift,
    match => 'eq',
    type => 'string',
    value => ''
  }, $class;
};


# Clone document VC
sub clone {
  my $self = shift;
  bless {
    key =>   $self->{key},
    match => $self->{match},
    type =>  $self->{type},
    value => $self->{value},
  }, __PACKAGE__;
};


# Normalize the object
sub normalize {
  return shift;
}


# Get or set type
sub type {
  my $self = shift;
  if (@_) {
    $self->{type} = shift;
    return $self;
  };
  return $self->{type};
};


# Get or set match
sub match {
  my $self = shift;
  if (@_) {
    $self->{match} = shift;
    return $self;
  };
  return $self->{match};
};


# Get or set key
sub key {
  my $self = shift;
  if (@_) {
    $self->{key} = shift;
    return $self;
  };
  return $self->{key};
};


# Get or set value
sub value {
  my $self = shift;
  if (@_) {
    $self->{value} = shift;
    return $self;
  };
  return $self->{value};
};


# Stringify fragment
sub _to_fragment {
  my $self = shift;
  my $json = '{';
  $json .= '"@type":"koral:doc",';
  $json .= '"type":"type:' . $self->type . '",';
  $json .= '"match":"match:' . $self->match . '",';
  $json .= '"key":"' . $self->key . '",';
  $json .= '"value":' . $self->quote($self->value);
  return $json . '}';
};


# VC contains only union fields
sub only_union_fields {
  return 0;
};

# VC contains only joints
sub only_joint {
  0;
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

    $$vc->union($all_vcs{$value}->clone);
  }

  # AND definition
  elsif ($key eq 'and') {
    unless (defined $all_vcs{$value}) {
      #       warn 'VC ' . $value . ' not defined';
      # exit(1);
      next;
    };

    $$vc->joint($all_vcs{$value}->clone);
  }

  # Source of the corpus
  elsif ($key eq 'ql') {
    # Quellenname, z.B. "Neue Zürcher Zeitung"
    $$vc->union_field(corpusTitle => $value);
  }

  # Add reduction value as a comment
  elsif ($key eq 'redabs') {
    # "red. Anz. Texte
    # absoluter Wert der durch Reduktion zu erzielende Anzahl Texte"
    $$vc->comment('redabs:' . $value);
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
    # warn $key . ' is an unknown field';
  };
};

close($fh);

# Stringify current (extended?) virtual corpus
print $$vc->to_string;
